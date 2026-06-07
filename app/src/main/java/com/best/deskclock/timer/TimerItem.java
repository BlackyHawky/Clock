/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static android.R.attr.state_activated;
import static android.R.attr.state_pressed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.util.AttributeSet;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.Timer;
import com.best.deskclock.databinding.TimerItemBinding;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

import java.util.Locale;

/**
 * This view is a visual representation of a {@link Timer}.
 */
public class TimerItem extends ConstraintLayout {

    private TimerItemBinding mBinding;

    Typeface mRegularTypeface;
    Typeface mBoldTypeface;

    private boolean mIsIndicatorStateDisplayed;

    private Drawable mIconPlay;
    private Drawable mIconPause;
    private Drawable mIconStop;
    private Drawable mIconEdit;
    private Drawable mIconReset;

    private int mColorPaused;
    private int mColorRunning;
    private int mColorExpired;
    private int mColorMissed;

    private String mLastButtonTimeRaw;
    private String mCachedAddButtonText;
    private String mCachedAddButtonContentDesc;
    private String mLastLabel = null;

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

        mTimerTextController = new TimerTextController(mBinding.timerTimeText);

        final Drawable drawable = ThemeUtils.circleDrawable();
        mBinding.timerIndicatorState.setBackground(drawable);
        mGradientDrawable = (GradientDrawable) mBinding.timerIndicatorState.getBackground();

        mIconPlay = AppCompatResources.getDrawable(getContext(), R.drawable.ic_fab_play);
        mIconPause = AppCompatResources.getDrawable(getContext(), R.drawable.ic_fab_pause);
        mIconStop = AppCompatResources.getDrawable(getContext(), R.drawable.ic_fab_stop);
        mIconEdit = AppCompatResources.getDrawable(getContext(), R.drawable.ic_edit);
        mIconReset = AppCompatResources.getDrawable(getContext(), R.drawable.ic_reset);

        final int colorAccent = MaterialColors.getColor(getContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mBinding.timerTimeText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
            new int[][]{{-state_activated, -state_pressed}, {}},
            new int[]{textColorPrimary, colorAccent});
        mBinding.timerTimeText.setTextColor(timeTextColor);
    }

    public void setGeneralFonts(Typeface regular, Typeface bold) {
        mRegularTypeface = regular;
        mBoldTypeface = bold;

        mBinding.timerAddTimeButton.setTypeface(bold);
    }

    public void setTimerTimeFont(Typeface timerTime) {
        mBinding.timerTimeText.setTypeface(timerTime);
    }

    public void setButtonPosition(boolean areTimerButtonPositionsInverted) {
        if (areTimerButtonPositionsInverted) {
            mBinding.timerControlsContainer.setLayoutDirection(ThemeUtils.isRTL(getContext())
                ? LAYOUT_DIRECTION_LTR
                : LAYOUT_DIRECTION_RTL
            );
        } else {
            mBinding.timerControlsContainer.setLayoutDirection(LAYOUT_DIRECTION_LOCALE);
        }
    }

    public void setIndicatorStateDisplay(boolean isIndicatorStateDisplayed) {
        mIsIndicatorStateDisplayed = isIndicatorStateDisplayed;
    }

    public void setIndicatorColors(int colorPaused, int colorRunning, int colorExpired, int colorMissed) {
        mColorPaused = colorPaused;
        mColorRunning = colorRunning;
        mColorExpired = colorExpired;
        mColorMissed = colorMissed;
    }

    /**
     * Dynamically updates the {@code timer} display based on its current state.
     */
    void updateTimeDisplay(Timer timer, boolean animateProgress) {
        final boolean blinkOff = SystemClock.elapsedRealtime() % 1000 < 500;

        mTimerTextController.setTimeString(timer.getRemainingTime());

        if (mBinding.circularProgressIndicator != null) {
            final boolean isBlinking = timer.isExpired() || timer.isMissed();
            final float targetAlpha = isBlinking
                ? (blinkOff ? 0f : 1f)
                : 1f;

            // Apply circle blinking
            if (mBinding.circularProgressIndicator.getAlpha() != targetAlpha) {
                mBinding.circularProgressIndicator.animate()
                    .alpha(targetAlpha)
                    .setDuration(300)
                    .start();
            }

            // Update circle only if visible
            if (!isBlinking || !blinkOff) {
                long totalLength = timer.getTotalLength();

                if (totalLength > 0) {
                    int progress = (int) ((timer.getRemainingTime() * 1000) / totalLength);

                    progress = Math.max(0, Math.min(1000, progress));

                    if (mBinding.circularProgressIndicator.getProgress() != progress) {
                        if (SdkUtils.isAtLeastAndroid7()) {
                            mBinding.circularProgressIndicator.setProgress(progress, animateProgress);
                        } else {
                            mBinding.circularProgressIndicator.setProgressCompat(progress, animateProgress);
                        }
                    }
                } else {
                    if (mBinding.circularProgressIndicator.getProgress() != 0) {
                        if (SdkUtils.isAtLeastAndroid7()) {
                            mBinding.circularProgressIndicator.setProgress(0, animateProgress);
                        } else {
                            mBinding.circularProgressIndicator.setProgressCompat(0, animateProgress);
                        }
                    }
                }
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
    public void bindTimer(Timer timer, boolean animate) {
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
        if (mBinding.circularProgressIndicator != null) {
            mBinding.circularProgressIndicator.animate().cancel();
            mBinding.circularProgressIndicator.setAlpha(1f);
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

            mCachedAddButtonText = getContext().getString(R.string.timer_add_custom_time, buttonTimeFormatted);

            mCachedAddButtonContentDesc = buttonTimeSeconds == 0
                ? getContext().getString(R.string.timer_add_custom_time_description, String.valueOf(buttonTimeMinutes))
                : getContext().getString(R.string.timer_add_custom_time_with_seconds_description,
                String.valueOf(buttonTimeMinutes),
                String.valueOf(buttonTimeSeconds));
        }

        mBinding.timerAddTimeButton.setText(mCachedAddButtonText);
        mBinding.timerAddTimeButton.setContentDescription(mCachedAddButtonContentDesc);

        // Initialize some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState) {
            mBinding.resetOrEditButton.setVisibility(VISIBLE);

            mLastState = timer.getState();

            switch (mLastState) {
                case RESET -> {
                    mBinding.resetOrEditButton.setIcon(mIconEdit);
                    mBinding.playPauseButton.setIcon(mIconPlay);
                }

                case PAUSED -> {
                    mBinding.resetOrEditButton.setIcon(mIconReset);
                    mBinding.playPauseButton.setIcon(mIconPlay);
                }

                case RUNNING -> {
                    mBinding.resetOrEditButton.setIcon(mIconReset);
                    mBinding.playPauseButton.setIcon(mIconPause);
                }

                case EXPIRED, MISSED -> {
                    mBinding.resetOrEditButton.setVisibility(INVISIBLE);
                    mBinding.playPauseButton.setIcon(mIconStop);
                }
            }
        }

        updateIndicator(timer.getState());

        updateTimeDisplay(timer, animate);
    }

    private void updateIndicator(Timer.State state) {
        if (!mIsIndicatorStateDisplayed || state == Timer.State.RESET) {
            mBinding.timerIndicatorState.setVisibility(GONE);
            return;
        }

        int color = switch (state) {
            case PAUSED -> mColorPaused;
            case RUNNING -> mColorRunning;
            case EXPIRED -> mColorExpired;
            case MISSED -> mColorMissed;
            default -> Color.TRANSPARENT;
        };

        mGradientDrawable.setColor(color);
        mBinding.timerIndicatorState.setVisibility(VISIBLE);
    }

}
