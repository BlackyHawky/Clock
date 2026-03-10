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
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.uicomponents.CircleButtonsLayout;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.Locale;

/**
 * This view is a visual representation of a {@link Timer}.
 */
public class TimerItem extends ConstraintLayout {

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

    private String mLastButtonTimeRaw;
    private String mCachedAddButtonText;
    private String mCachedAddButtonContentDesc;
    private String mLastLabel = null;
    private String mLastTotalDuration = null;
    private int mLastTimerCount = -1;

    /** The container of TimerCircleView and TimerTextController */
    private CircleButtonsLayout mCircleContainer;

    /** Formats and displays the text in the timer. */
    private TimerTextController mTimerTextController;

    /** Displays timer progress as a color circle. */
    private TimerCircleView mCircleView;

    /** Displays the remaining time or time since expiration. */
    private TextView mTimerText;

    /** A button that resets the timer. */
    private ImageButton mResetButton;

    /** A button that edits a new duration to the timer. */
    private ImageButton mTimerEditNewDurationButton;

    /** A button that adds time to the timer. */
    private MaterialButton mAddTimeButton;

    /** Displays the label associated with the timer. Tapping it presents an edit dialog. */
    private TextView mLabelView;

    /** A button to start / stop the timer */
    private MaterialButton mPlayPauseButton;

    /** View representing the visual indicator of the timer state (running, paused, expired). */
    private View mIndicatorState;

    /** Drawable used to style the timer state indicator as a circle with dynamic fill. */
    private GradientDrawable mGradientDrawable;

    /** The last state of the timer that was rendered; used to avoid expensive operations. */
    private Timer.State mLastState;

    /** The timer duration text that appears when the timer is reset */
    private TextView mTimerTotalDurationText;

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

        mLabelView = findViewById(R.id.timer_label);
        mResetButton = findViewById(R.id.reset);
        mAddTimeButton = findViewById(R.id.timer_add_time_button);
        mPlayPauseButton = findViewById(R.id.play_pause);
        mCircleContainer = findViewById(R.id.circle_container);
        mCircleView = findViewById(R.id.timer_time);
        mTimerText = findViewById(R.id.timer_time_text);
        mTimerTextController = new TimerTextController(mTimerText);
        mTimerEditNewDurationButton = findViewById(R.id.timer_edit_new_duration_button);

        mTimerTotalDurationText = findViewById(R.id.timer_total_duration);
        if (mTimerTotalDurationText != null) {
            mTimerTotalDurationText.setTypeface(mTimerTimeTypeface);
        }

        mIndicatorState = findViewById(R.id.indicator_state);
        final Drawable drawable = ThemeUtils.circleDrawable();
        mIndicatorState.setBackground(drawable);
        mGradientDrawable = (GradientDrawable) mIndicatorState.getBackground();

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

        final int colorAccent = MaterialColors.getColor(
                mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mTimerText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
                new int[][]{{-state_activated, -state_pressed}, {}},
                new int[]{textColorPrimary, colorAccent});
        mTimerText.setTextColor(timeTextColor);
        mTimerText.setTypeface(mTimerTimeTypeface);

        mAddTimeButton.setTypeface(mBoldTypeface);
    }

    /**
     * Dynamically updates the {@code timer} display based on its current state.
     */
    void updateTimeDisplay(Timer timer) {
        final boolean blinkOff = SystemClock.elapsedRealtime() % 1000 < 500;

        mTimerTextController.setTimeString(timer.getRemainingTime());

        if (mCircleView != null) {
            final boolean isBlinking = timer.isExpired() || timer.isMissed();
            final float targetAlpha = isBlinking
                    ? (blinkOff ? 0f : 1f)
                    : 1f;

            // Apply circle blinking
            if (mCircleView.getAlpha() != targetAlpha) {
                mCircleView.animate()
                        .alpha(targetAlpha)
                        .setDuration(300)
                        .start();
            }

            // Update circle only if visible
            if (!isBlinking || !blinkOff) {
                mCircleView.update(timer);
            }
        }

        final float textTargetAlpha = (!timer.isPaused() || !blinkOff || mTimerText.isPressed()) ? 1f : 0f;
        if (mTimerText.getAlpha() != textTargetAlpha) {
            mTimerText.animate()
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
        if (mTimerTotalDurationText != null) {
            String totalDuration = timer.getTotalDuration();
            if (!totalDuration.equals(mLastTotalDuration)) {
                mLastTotalDuration = totalDuration;
                mTimerTotalDurationText.setText(totalDuration);
            }
        }

        // Initialize the label
        final String label = timer.getLabel();

        if (!label.equals(mLastLabel)) {
            mLastLabel = label;

            if (label.isEmpty()) {
                mLabelView.setText(null);
                mLabelView.setTypeface(mRegularTypeface);
            } else {
                mLabelView.setText(label);
                mLabelView.setTypeface(mBoldTypeface);
                mLabelView.setAlpha(1f);
            }
        }

        // Initialize the circle
        if (mCircleView != null) {
            mCircleView.animate().cancel();
            mCircleView.setAlpha(1f);
            mCircleView.update(timer);
        }

        // Initialize the alpha value of the time text color
        mTimerText.animate().cancel();
        mTimerText.setAlpha(1f);

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

        mAddTimeButton.setText(mCachedAddButtonText);
        mAddTimeButton.setContentDescription(mCachedAddButtonContentDesc);

        // For tablets in portrait mode with single timer, adjust the size of the "Add time" and
        // "Play/Pause" buttons
        final ConstraintLayout.LayoutParams addTimeButtonParams =
                (ConstraintLayout.LayoutParams) mAddTimeButton.getLayoutParams();
        final ConstraintLayout.LayoutParams playPauseButtonParams =
                (ConstraintLayout.LayoutParams) mPlayPauseButton.getLayoutParams();

        if (currentTimerCount != mLastTimerCount) {
            mLastTimerCount = currentTimerCount;

            if (mIsTablet && mIsPortrait) {
                if (currentTimerCount == 1) {
                    addTimeButtonParams.matchConstraintMaxHeight = mMaxTabletButtonHeight;
                    playPauseButtonParams.matchConstraintMaxHeight = mMaxTabletButtonHeight;
                }

                mAddTimeButton.setLayoutParams(addTimeButtonParams);
                mPlayPauseButton.setLayoutParams(playPauseButtonParams);
            }
        }

        // Initialize some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState) {
            boolean isReset = timer.getState() == Timer.State.RESET;

            final String resetDesc = mContext.getString(R.string.reset);

            mResetButton.setVisibility(VISIBLE);
            mResetButton.setContentDescription(resetDesc);
            mAddTimeButton.setVisibility(VISIBLE);
            mLastState = timer.getState();

            // For phones in portrait mode, when there are multiple timers and they are reset,
            // adjust the constraints, margins and paddings of the "Play/Pause" button to have
            // a suitable reduced view
            if (isPortraitPhoneWithMultipleTimers(currentTimerCount)) {
                if (isReset) {
                    playPauseButtonParams.width = LayoutParams.WRAP_CONTENT;

                    playPauseButtonParams.startToStart = ConstraintLayout.LayoutParams.UNSET;
                    playPauseButtonParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                    playPauseButtonParams.topToBottom = mLabelView.getId();
                    playPauseButtonParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;

                    playPauseButtonParams.rightMargin = mMargin12;
                    playPauseButtonParams.topMargin = mMargin10;

                    mPlayPauseButton.setPadding(0, 0, 0, 0);
                } else {
                    playPauseButtonParams.width = 0;

                    playPauseButtonParams.startToStart = mAddTimeButton.getId();
                    playPauseButtonParams.endToEnd = mAddTimeButton.getId();
                    playPauseButtonParams.topToBottom = ConstraintLayout.LayoutParams.UNSET;
                    playPauseButtonParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;

                    playPauseButtonParams.rightMargin = 0;
                    playPauseButtonParams.topMargin = 0;

                    mPlayPauseButton.setPadding(mPadding24, mPadding24, mPadding24, mPadding24);
                }

                mPlayPauseButton.setLayoutParams(playPauseButtonParams);
            }

            switch (mLastState) {
                case RESET -> {
                    mResetButton.setVisibility(GONE);
                    mResetButton.setContentDescription(null);
                    mAddTimeButton.setVisibility(INVISIBLE);
                    mPlayPauseButton.setIcon(mIconPlay);
                    mIndicatorState.setVisibility(GONE);
                }

                case PAUSED -> {
                    mPlayPauseButton.setIcon(mIconPlay);
                    updateIndicatorState(mColorPaused);
                }

                case RUNNING -> {
                    mPlayPauseButton.setIcon(mIconPause);
                    updateIndicatorState(mColorRunning);
                }

                case EXPIRED, MISSED -> {
                    mPlayPauseButton.setIcon(mIconStop);
                    mResetButton.setVisibility(GONE);
                    updateIndicatorState(mColorExpired);
                }
            }

            if (isTabletOrLandscapePhone() || isPhoneWithSingleTimer(currentTimerCount)) {
                mTimerEditNewDurationButton.setVisibility(isReset ? VISIBLE : GONE);
            }

            if (isPortraitPhoneWithMultipleTimers(currentTimerCount)) {
                mCircleContainer.setVisibility(isReset ? GONE : VISIBLE);
                mTimerTotalDurationText.setVisibility(isReset ? VISIBLE : GONE);
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
            mIndicatorState.setVisibility(GONE);
            return;
        }

        mGradientDrawable.setColor(color);
        mIndicatorState.setVisibility(VISIBLE);
    }
}