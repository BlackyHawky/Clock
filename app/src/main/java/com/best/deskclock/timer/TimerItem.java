/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static android.R.attr.state_activated;
import static android.R.attr.state_pressed;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.databinding.TimerItemBinding;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

import java.util.Locale;

/**
 * This view is a visual representation of a {@link Timer}.
 */
public class TimerItem extends ConstraintLayout {

    private TimerItemBinding mBinding;

    Context mContext;
    SharedPreferences mPrefs;
    Typeface mRegularTypeface;
    Typeface mBoldTypeface;
    Typeface mTimerTimeTypeface;
    DisplayMetrics mDisplayMetrics;

    private boolean mIsTablet;
    private boolean mIsPortrait;
    private boolean mIsIndicatorStateDisplay;

    private int mMaxTabletButtonHeight;
    private int mMargin12;
    private int mMargin10;
    private int mPadding24;

    private Drawable mIconPlay;
    private Drawable mIconPause;
    private Drawable mIconStop;

    private int mColorPaused;
    private int mColorRunning;
    private int mColorExpired;
    private int mColorMissed;

    private String mLastButtonTimeRaw;
    private String mCachedAddButtonText;
    private String mCachedAddButtonContentDesc;
    private String mLastLabel = null;
    private String mLastTotalDuration = null;
    private int mLastTimerCount = -1;

    /**
     * Formats and displays the text in the timer.
     */
    private TimerTextController mTimerTextController;

    /**
     * Drawable used to style the timer state indicator as a circle with dynamic fill.
     */
    private GradientDrawable mGradientDrawable;

    /**
     * The last state of the timer that was rendered; used to avoid expensive operations.
     */
    private Timer.State mLastState;

    public TimerItem(Context context) {
        this(context, null);
    }

    public TimerItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (isInEditMode()) {
            // Skip logic during Android Studio preview
            return;
        }

        mBinding = TimerItemBinding.bind(this);

        mContext = getContext();
        mPrefs = getDefaultSharedPreferences(mContext);
        String generalFontPath = SettingsDAO.getGeneralFont(mPrefs);
        mRegularTypeface = ThemeUtils.loadFont(generalFontPath);
        mBoldTypeface = ThemeUtils.boldTypeface(generalFontPath);
        mTimerTimeTypeface = ThemeUtils.loadFont(SettingsDAO.getTimerDurationFont(mPrefs));
        mDisplayMetrics = getResources().getDisplayMetrics();
        mIsTablet = ThemeUtils.isTablet();
        mIsPortrait = ThemeUtils.isPortrait();
        mIsIndicatorStateDisplay = SettingsDAO.isTimerStateIndicatorDisplayed(mPrefs);
        mTimerTextController = new TimerTextController(mBinding.timerTimeText);

        final Drawable drawable = ThemeUtils.circleDrawable();
        mBinding.timerIndicatorState.setBackground(drawable);
        mGradientDrawable = (GradientDrawable) mBinding.timerIndicatorState.getBackground();

        mMaxTabletButtonHeight = (int) dpToPx(200, mDisplayMetrics);
        mMargin12 = (int) dpToPx(12, mDisplayMetrics);
        mMargin10 = (int) dpToPx(10, mDisplayMetrics);
        mPadding24 = (int) dpToPx(24, mDisplayMetrics);

        mIconPlay = AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_play);
        mIconPause = AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_pause);
        mIconStop = AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_stop);

        mColorPaused = SettingsDAO.getPausedTimerIndicatorColor(mPrefs);
        mColorRunning = SettingsDAO.getRunningTimerIndicatorColor(mPrefs);
        mColorExpired = SettingsDAO.getExpiredTimerIndicatorColor(mPrefs);
        mColorMissed = SettingsDAO.getMissedTimerIndicatorColor(mPrefs);

        final int colorAccent = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mBinding.timerTimeText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
            new int[][]{{-state_activated, -state_pressed}, {}},
            new int[]{textColorPrimary, colorAccent});
        mBinding.timerTimeText.setTextColor(timeTextColor);
    }

    /**
     * Injects preloaded typefaces into the view to optimize performance.
     * This avoids expensive disk I/O operations when the view is inflated or recycled.
     *
     * @param regular   The standard typeface used for regular text (e.g., empty labels).
     * @param bold      The bold typeface used for active labels and buttons.
     * @param timerTime The specific typeface used for the main timer countdown display.
     */
    public void setCachedFonts(Typeface regular, Typeface bold, Typeface timerTime) {
        mRegularTypeface = regular;
        mBoldTypeface = bold;
        mTimerTimeTypeface = timerTime;

        mBinding.timerTimeText.setTypeface(mTimerTimeTypeface);

        if (mBinding.timerTotalDurationText != null) {
            mBinding.timerTotalDurationText.setTypeface(mTimerTimeTypeface);
        }

        mBinding.timerAddTimeButton.setTypeface(mBoldTypeface);
    }

    /**
     * Dynamically updates the {@code timer} display based on its current state.
     */
    void updateTimeDisplay(Timer timer) {
        final boolean blinkOff = SystemClock.elapsedRealtime() % 1000 < 500;

        mTimerTextController.setTimeString(timer.getRemainingTime());

        if (mBinding.timerCircleView != null) {
            final boolean isBlinking = timer.isExpired() || timer.isMissed();
            final float targetAlpha = isBlinking
                ? (blinkOff ? 0f : 1f)
                : 1f;

            // Apply circle blinking
            if (mBinding.timerCircleView.getAlpha() != targetAlpha) {
                mBinding.timerCircleView.animate()
                    .alpha(targetAlpha)
                    .setDuration(300)
                    .start();
            }

            // Update circle only if visible
            if (!isBlinking || !blinkOff) {
                mBinding.timerCircleView.update(timer);
            }
        }

        final float textTargetAlpha = (!timer.isPaused() || !blinkOff || mBinding.timerTimeText.isPressed()) ? 1f : 0f;
        if (mBinding.timerTimeText.getAlpha() != textTargetAlpha) {
            mBinding.timerTimeText.animate()
                .alpha(textTargetAlpha)
                .setDuration(200)
                .start();
        }
    }

    /**
     * Initializes the {@code timer} static visual elements when binding to a ViewHolder.
     */
    public void bindTimer(Timer timer) {
        int currentTimerCount = DataModel.getDataModel().getTimers().size();

        // Initialize the time.
        mTimerTextController.setTimeString(timer.getRemainingTime());

        // Initialize text for timer total duration
        if (mBinding.timerTotalDurationText != null) {
            String totalDuration = timer.getTotalDuration();
            if (!totalDuration.equals(mLastTotalDuration)) {
                mLastTotalDuration = totalDuration;
                mBinding.timerTotalDurationText.setText(totalDuration);
            }
        }

        // Initialize the label
        final String label = timer.getLabel();

        if (!label.equals(mLastLabel)) {
            mLastLabel = label;

            if (label.isEmpty()) {
                mBinding.timerLabel.setText(null);
                mBinding.timerLabel.setTypeface(mRegularTypeface);
            } else {
                mBinding.timerLabel.setText(label);
                mBinding.timerLabel.setTypeface(mBoldTypeface);
                mBinding.timerLabel.setAlpha(1f);
            }
        }

        // Initialize the circle
        if (mBinding.timerCircleView != null) {
            mBinding.timerCircleView.animate().cancel();
            mBinding.timerCircleView.setAlpha(1f);
            mBinding.timerCircleView.update(timer);
        }

        // Initialize the alpha value of the time text color
        mBinding.timerTimeText.animate().cancel();
        mBinding.timerTimeText.setAlpha(1f);

        // Initialize the time value to add to timer in the "Add time" button
        String buttonTime = timer.getButtonTime();

        if (!buttonTime.equals(mLastButtonTimeRaw)) {
            mLastButtonTimeRaw = buttonTime;

            long totalSeconds = Long.parseLong(buttonTime);
            long buttonTimeMinutes = (totalSeconds) / 60;
            long buttonTimeSeconds = totalSeconds % 60;

            String buttonTimeFormatted = String.format(
                Locale.getDefault(),
                buttonTimeMinutes < 10 ? "%d:%02d" : "%02d:%02d",
                buttonTimeMinutes,
                buttonTimeSeconds);

            mCachedAddButtonText = mContext.getString(R.string.timer_add_custom_time, buttonTimeFormatted);

            mCachedAddButtonContentDesc = buttonTimeSeconds == 0
                ? mContext.getString(R.string.timer_add_custom_time_description, String.valueOf(buttonTimeMinutes))
                : mContext.getString(R.string.timer_add_custom_time_with_seconds_description,
                String.valueOf(buttonTimeMinutes),
                String.valueOf(buttonTimeSeconds));
        }

        mBinding.timerAddTimeButton.setText(mCachedAddButtonText);
        mBinding.timerAddTimeButton.setContentDescription(mCachedAddButtonContentDesc);

        // For tablets in portrait mode with single timer, adjust the size of the "Add time" and
        // "Play/Pause" buttons
        final ConstraintLayout.LayoutParams addTimeButtonParams =
            (ConstraintLayout.LayoutParams) mBinding.timerAddTimeButton.getLayoutParams();
        final ConstraintLayout.LayoutParams playPauseButtonParams =
            (ConstraintLayout.LayoutParams) mBinding.playPauseButton.getLayoutParams();

        if (currentTimerCount != mLastTimerCount) {
            mLastTimerCount = currentTimerCount;

            if (mIsTablet && mIsPortrait) {
                if (currentTimerCount == 1) {
                    addTimeButtonParams.matchConstraintMaxHeight = mMaxTabletButtonHeight;
                    playPauseButtonParams.matchConstraintMaxHeight = mMaxTabletButtonHeight;
                }

                mBinding.timerAddTimeButton.setLayoutParams(addTimeButtonParams);
                mBinding.playPauseButton.setLayoutParams(playPauseButtonParams);
            }
        }

        // Initialize some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState) {
            boolean isReset = timer.getState() == Timer.State.RESET;

            final String resetDesc = mContext.getString(R.string.reset);

            mBinding.resetButton.setVisibility(VISIBLE);
            mBinding.resetButton.setContentDescription(resetDesc);
            mBinding.timerAddTimeButton.setVisibility(VISIBLE);
            mLastState = timer.getState();

            // For phones in portrait mode, when there are multiple timers, and they are reset,
            // adjust the constraints, margins and paddings of the "Play/Pause" button to have
            // a suitable reduced view
            if (isPortraitPhoneWithMultipleTimers(currentTimerCount)) {
                if (isReset) {
                    playPauseButtonParams.width = LayoutParams.WRAP_CONTENT;

                    playPauseButtonParams.startToStart = ConstraintLayout.LayoutParams.UNSET;
                    playPauseButtonParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                    playPauseButtonParams.topToBottom = mBinding.timerLabel.getId();
                    playPauseButtonParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;

                    playPauseButtonParams.rightMargin = mMargin12;
                    playPauseButtonParams.topMargin = mMargin10;

                    mBinding.playPauseButton.setPadding(0, 0, 0, 0);
                } else {
                    playPauseButtonParams.width = 0;

                    playPauseButtonParams.startToStart = mBinding.timerAddTimeButton.getId();
                    playPauseButtonParams.endToEnd = mBinding.timerAddTimeButton.getId();
                    playPauseButtonParams.topToBottom = ConstraintLayout.LayoutParams.UNSET;
                    playPauseButtonParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;

                    playPauseButtonParams.rightMargin = 0;
                    playPauseButtonParams.topMargin = 0;

                    mBinding.playPauseButton.setPadding(mPadding24, mPadding24, mPadding24, mPadding24);
                }

                mBinding.playPauseButton.setLayoutParams(playPauseButtonParams);
            }

            switch (mLastState) {
                case RESET -> {
                    mBinding.resetButton.setVisibility(GONE);
                    mBinding.resetButton.setContentDescription(null);
                    mBinding.timerAddTimeButton.setVisibility(INVISIBLE);
                    mBinding.playPauseButton.setIcon(mIconPlay);
                    mBinding.timerIndicatorState.setVisibility(GONE);
                }

                case PAUSED -> {
                    mBinding.playPauseButton.setIcon(mIconPlay);
                    updateIndicatorState(mColorPaused);
                }

                case RUNNING -> {
                    mBinding.playPauseButton.setIcon(mIconPause);
                    updateIndicatorState(mColorRunning);
                }

                case EXPIRED, MISSED -> {
                    mBinding.playPauseButton.setIcon(mIconStop);
                    mBinding.resetButton.setVisibility(GONE);
                    updateIndicatorState(mLastState == Timer.State.EXPIRED ? mColorExpired : mColorMissed);
                }
            }

            if (isTabletOrLandscapePhone() || isPhoneWithSingleTimer(currentTimerCount)) {
                mBinding.timerEditNewDurationButton.setVisibility(isReset ? VISIBLE : GONE);
            }

            if (isPortraitPhoneWithMultipleTimers(currentTimerCount)) {
                if (mBinding.circleContainer != null) {
                    mBinding.circleContainer.setVisibility(isReset ? GONE : VISIBLE);
                }

                if (mBinding.timerTotalDurationText != null) {
                    mBinding.timerTotalDurationText.setVisibility(isReset ? VISIBLE : GONE);
                }
            }
        }
    }

    /**
     * @return {@code true} if the device is a phone in portrait mode with multiple timers displayed.
     * {@code false} otherwise.
     */
    private boolean isPortraitPhoneWithMultipleTimers(int timerCount) {
        return !mIsTablet && mIsPortrait && timerCount > 1;
    }

    /**
     * @return {@code true} if the device is a tablet or phone in landscape mode.
     * {@code false} otherwise.
     */
    private boolean isTabletOrLandscapePhone() {
        return (mIsTablet || !mIsPortrait);
    }

    /**
     * @return {@code true} if the device is a phone with a single timer displayed.
     */
    private boolean isPhoneWithSingleTimer(int timerCount) {
        return !mIsTablet && timerCount == 1;
    }

    private void updateIndicatorState(int color) {
        if (!mIsIndicatorStateDisplay) {
            mBinding.timerIndicatorState.setVisibility(GONE);
            return;
        }

        mGradientDrawable.setColor(color);
        mBinding.timerIndicatorState.setVisibility(VISIBLE);
    }
}
