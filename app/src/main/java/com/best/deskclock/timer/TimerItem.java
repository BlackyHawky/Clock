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
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.Timer;
import com.best.deskclock.databinding.TimerItemBinding;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

import java.util.Locale;

/**
 * This view is a visual representation of a {@link Timer}.
 */
public class TimerItem extends ConstraintLayout {

    private TimerItemBinding mBinding;

    private CharSequence mTimerEndTimeFormatPattern;

    private boolean mIsTimerEndTimeDisplayed;
    private boolean mIsIndicatorStateDisplayed;
    private boolean mLastDeleteAfterUse;
    private boolean mIsAddTimeZero;

    private Drawable mIconPlay;
    private Drawable mIconPause;
    private Drawable mIconStop;
    private Drawable mIconDelete;

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

    public TimerItem(@NonNull Context context) {
        this(context, null);
    }

    public TimerItem(@NonNull Context context, @Nullable AttributeSet attrs) {
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
        mIconDelete = AppCompatResources.getDrawable(getContext(), R.drawable.ic_delete);

        final int colorAccent = MaterialColors.getColor(getContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mBinding.timerTimeText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
            new int[][]{{-state_activated, -state_pressed}, {}},
            new int[]{textColorPrimary, colorAccent});
        mBinding.timerTimeText.setTextColor(timeTextColor);
    }

    public void setGeneralFonts(@NonNull Typeface regular, @NonNull Typeface bold) {
        mBinding.timerLabel.setTypeface(bold);

        mBinding.timerAddTimeButton.setTypeface(bold);

        mBinding.timerEndTime.setTypeface(regular, Typeface.ITALIC);
    }

    public void setTimerTimeFont(@NonNull Typeface timerTime) {
        mBinding.timerTimeText.setTypeface(timerTime);
    }

    public void setTimerEndTimeFormatPattern(@NonNull CharSequence formatPattern) {
        mTimerEndTimeFormatPattern = formatPattern;
    }

    public void displayTimerEndTime(boolean isTimerEndTimeDisplayed) {
        mIsTimerEndTimeDisplayed = isTimerEndTimeDisplayed;
    }

    public void setButtonPosition(boolean areTimerButtonPositionsInverted, boolean isTablet, boolean isLandscape, boolean isSingleTimer) {
        if (areTimerButtonPositionsInverted) {
            mBinding.getRoot().setLayoutDirection(ThemeUtils.isRTL(getContext())
                ? LAYOUT_DIRECTION_LTR
                : LAYOUT_DIRECTION_RTL
            );
        } else {
            mBinding.getRoot().setLayoutDirection(LAYOUT_DIRECTION_LOCALE);
        }

        if ((!isTablet && isLandscape) || isSingleTimer) {
            mBinding.timerEndTime.setGravity(Gravity.CENTER);
        } else {
            mBinding.timerEndTime.setGravity(areTimerButtonPositionsInverted
                ? Gravity.START | Gravity.CENTER_VERTICAL
                : Gravity.END | Gravity.CENTER_VERTICAL);
        }

        mBinding.timerLabel.setLayoutDirection(LAYOUT_DIRECTION_LOCALE);
        mBinding.timerIndicatorState.setLayoutDirection(LAYOUT_DIRECTION_LOCALE);
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
    void updateTimeDisplay(@NonNull Timer timer, boolean animateProgress) {
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
                    .setDuration(AnimatorUtils.MEDIUM_ANIMATION_DURATION)
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
                .setDuration(AnimatorUtils.SHORT_ANIMATION_DURATION)
                .start();
        }
    }

    /**
     * Initializes the {@code timer} static visual elements when binding to a ViewHolder.
     */
    public void bindTimer(@NonNull Timer timer, boolean animate) {
        // Initialize the label
        final String label = timer.getLabel();

        if (!TextUtils.equals(label, mLastLabel)) {
            mLastLabel = label;

            if (TextUtils.isEmpty(label)) {
                mBinding.timerLabel.setVisibility(GONE);
            } else {
                mBinding.timerLabel.setText(label);
                mBinding.timerLabel.setAlpha(1f);
                mBinding.timerLabel.setVisibility(VISIBLE);
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
            mIsAddTimeZero = totalSeconds == 0;

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

        final boolean deleteAfterUse = timer.getDeleteAfterUse();

        // Initialize some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState || deleteAfterUse != mLastDeleteAfterUse) {
            mBinding.resetButton.setVisibility(VISIBLE);

            mLastState = timer.getState();
            mLastDeleteAfterUse = deleteAfterUse;

            switch (mLastState) {
                case RESET -> {
                    mBinding.resetButton.setVisibility(INVISIBLE);
                    mBinding.playPauseButton.setIcon(mIconPlay);
                }

                case PAUSED -> {
                    mBinding.resetButton.setVisibility(VISIBLE);
                    mBinding.playPauseButton.setIcon(mIconPlay);
                }

                case RUNNING -> {
                    mBinding.resetButton.setVisibility(VISIBLE);
                    mBinding.playPauseButton.setIcon(mIconPause);
                }

                case EXPIRED, MISSED -> {
                    mBinding.resetButton.setVisibility(INVISIBLE);
                    mBinding.playPauseButton.setIcon(deleteAfterUse ? mIconDelete : mIconStop);
                }
            }
        }

        updateAddTimeButtonDisplay(timer.getState());

        updateIndicator(timer.getState());

        updateEndTimeDisplay(timer);

        updateTimeDisplay(timer, animate);
    }

    private void updateAddTimeButtonDisplay(@NonNull Timer.State state) {
        if (state == Timer.State.RESET || mIsAddTimeZero) {
            mBinding.timerAddTimeButton.setVisibility(INVISIBLE);
            return;
        }

        mBinding.timerAddTimeButton.setText(mCachedAddButtonText);
        mBinding.timerAddTimeButton.setContentDescription(mCachedAddButtonContentDesc);
        mBinding.timerAddTimeButton.setVisibility(VISIBLE);
    }

    private void updateIndicator(@NonNull Timer.State state) {
        if (!mIsIndicatorStateDisplayed) {
            mBinding.timerIndicatorState.setVisibility(GONE);
            return;
        }

        if (state == Timer.State.RESET) {
            mBinding.timerIndicatorState.setVisibility(mLastLabel.isEmpty() ? INVISIBLE : GONE);
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

    private void updateEndTimeDisplay(@NonNull Timer timer) {
        if (!mIsTimerEndTimeDisplayed) {
            mBinding.timerEndTime.setVisibility(GONE);
            return;
        }

        if (timer.getState() == Timer.State.RUNNING) {
            long endTimeMillis = timer.getWallClockExpirationTime();

            CharSequence formattedTime;

            if (!DateUtils.isToday(endTimeMillis)) {
                String dayString = DateFormat.format("EEE", endTimeMillis).toString();
                String capitalizedDay = FormattedTextUtils.capitalizeFirstLetter(dayString, Locale.getDefault());
                CharSequence timeCharSequence = DateFormat.format(mTimerEndTimeFormatPattern, endTimeMillis);
                formattedTime = TextUtils.concat(capitalizedDay, ", ", timeCharSequence);
            } else {
                formattedTime = DateFormat.format(mTimerEndTimeFormatPattern, endTimeMillis);
            }

            CharSequence expandedText = TextUtils.expandTemplate(getContext().getText(R.string.timer_end_time_label), formattedTime);

            // Add a "No-Break Space" before and after the text to center it properly and prevent it
            // from being cut off at the end due to the italic formatting.
            CharSequence finalText = TextUtils.concat("\u00A0", expandedText, "\u00A0");

            mBinding.timerEndTime.setText(finalText);

            mBinding.timerEndTime.setVisibility(VISIBLE);
        } else {
            mBinding.timerEndTime.setVisibility(INVISIBLE);
        }
    }

}
