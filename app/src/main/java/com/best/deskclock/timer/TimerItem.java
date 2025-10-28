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
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
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

    /** The container of TimerCircleView and TimerTextController */
    private CircleButtonsLayout mCircleContainer;

    /** Formats and displays the text in the timer. */
    private TimerTextController mTimerTextController;

    /** Displays timer progress as a color circle that changes from white to red. */
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

    /** The last state of the timer that was rendered; used to avoid expensive operations. */
    private Timer.State mLastState;

    /** The timer duration text that appears when the timer is reset */
    private TextView mTimerTotalDurationText;

    private boolean mIsTablet;
    private boolean mIsPortrait;

    public TimerItem(Context context) {
        this(context, null);
    }

    public TimerItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mContext = getContext();
        mIsTablet = ThemeUtils.isTablet();
        mIsPortrait = ThemeUtils.isPortrait();

        setBackground(ThemeUtils.cardBackground(mContext));

        mLabelView = findViewById(R.id.timer_label);
        mResetButton = findViewById(R.id.reset);
        mAddTimeButton = findViewById(R.id.timer_add_time_button);
        mPlayPauseButton = findViewById(R.id.play_pause);
        mCircleContainer = findViewById(R.id.circle_container);
        mCircleView = findViewById(R.id.timer_time);
        // Displays the remaining time or time since expiration.
        // Timer text serves as a virtual start/stop button.
        mTimerText = findViewById(R.id.timer_time_text);
        final int colorAccent = MaterialColors.getColor(
                mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mTimerText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
                new int[][]{{-state_activated, -state_pressed}, {}},
                new int[]{textColorPrimary, colorAccent});
        mTimerText.setTextColor(timeTextColor);
        mTimerTextController = new TimerTextController(mTimerText);
        mTimerEditNewDurationButton = findViewById(R.id.timer_edit_new_duration_button);
        // Needed to avoid the null pointer exception, as only phones in portrait mode with
        // multiple timers have this text
        if (isPortraitPhoneWithMultipleTimers()) {
            mTimerTotalDurationText = findViewById(R.id.timer_total_duration);
        }
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
            mCircleView.animate()
                    .alpha(targetAlpha)
                    .setDuration(300)
                    .start();

            // Update circle only if visible
            if (!isBlinking || !blinkOff) {
                mCircleView.update(timer);
            }
        }

        final float textTargetAlpha = (!timer.isPaused() || !blinkOff || mTimerText.isPressed()) ? 1f : 0f;
        mTimerText.animate()
                .alpha(textTargetAlpha)
                .setDuration(200)
                .start();
    }

    /**
     * Initializes the {@code timer} static visual elements when binding to a ViewHolder.
     */
    void bindTimer(Timer timer) {
        // Initialize the time.
        mTimerTextController.setTimeString(timer.getRemainingTime());

        // Initialize text for timer total duration
        if (isPortraitPhoneWithMultipleTimers() && mTimerTotalDurationText != null) {
            mTimerTotalDurationText.setText(timer.getTotalDuration());
        }

        // Initialize the label
        final String label = timer.getLabel();
        if (label.isEmpty()) {
            mLabelView.setText(null);
            mLabelView.setTypeface(Typeface.DEFAULT);
        } else {
            mLabelView.setText(label);
            mLabelView.setTypeface(Typeface.DEFAULT_BOLD);
            mLabelView.setAlpha(1f);
        }

        // Initialize the circle
        if (mCircleView != null) {
            mCircleView.setAlpha(1f);
            mCircleView.update(timer);
        }

        // Initialize the alpha value of the time text color
        mTimerText.setAlpha(1f);

        // Initialize the time value to add to timer in the "Add time" button
        String buttonTime = timer.getButtonTime();
        long totalSeconds = Long.parseLong(buttonTime);
        long buttonTimeMinutes = (totalSeconds) / 60;
        long buttonTimeSeconds = totalSeconds % 60;
        String buttonTimeFormatted = String.format(
                Locale.getDefault(),
                buttonTimeMinutes < 10 ? "%d:%02d" : "%02d:%02d",
                buttonTimeMinutes,
                buttonTimeSeconds);

        mAddTimeButton.setText(mContext.getString(R.string.timer_add_custom_time, buttonTimeFormatted));

        String buttonContentDescription = buttonTimeSeconds == 0
                ? mContext.getString(R.string.timer_add_custom_time_description, String.valueOf(buttonTimeMinutes))
                : mContext.getString(R.string.timer_add_custom_time_with_seconds_description,
                    String.valueOf(buttonTimeMinutes),
                    String.valueOf(buttonTimeSeconds));
        mAddTimeButton.setContentDescription(buttonContentDescription);

        // For tablets in portrait mode with single timer, adjust the size of the "Add time" and
        // "Play/Pause" buttons
        final ConstraintLayout.LayoutParams addTimeButtonParams =
                (ConstraintLayout.LayoutParams) mAddTimeButton.getLayoutParams();
        final ConstraintLayout.LayoutParams playPauseButtonParams =
                (ConstraintLayout.LayoutParams) mPlayPauseButton.getLayoutParams();

        if (mIsTablet && mIsPortrait && DataModel.getDataModel().getTimers().size() == 1) {
            addTimeButtonParams.matchConstraintMaxHeight = ThemeUtils.convertDpToPixels(200, mContext);
            playPauseButtonParams.matchConstraintMaxHeight = ThemeUtils.convertDpToPixels(200, mContext);
        }

        // Initialize some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState) {
            final String resetDesc = mContext.getString(R.string.reset);

            mResetButton.setVisibility(VISIBLE);
            mResetButton.setContentDescription(resetDesc);
            mAddTimeButton.setVisibility(VISIBLE);
            mLastState = timer.getState();

            // For phones in portrait mode, when there are multiple timers and they are reset,
            // adjust the constraints, margins and paddings of the "Play/Pause" button to have
            // a suitable reduced view
            if (isPortraitPhoneWithMultipleTimers()) {
                if (mLastState.equals(Timer.State.RESET)) {
                    playPauseButtonParams.width = LayoutParams.WRAP_CONTENT;

                    playPauseButtonParams.startToStart = ConstraintLayout.LayoutParams.UNSET;
                    playPauseButtonParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                    playPauseButtonParams.topToBottom = mLabelView.getId();
                    playPauseButtonParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;

                    playPauseButtonParams.rightMargin = ThemeUtils.convertDpToPixels(12, mContext);
                    playPauseButtonParams.topMargin = ThemeUtils.convertDpToPixels(10, mContext);

                    mPlayPauseButton.setPadding(0, 0, 0, 0);
                } else {
                    int playPauseButtonPadding = ThemeUtils.convertDpToPixels(24, mContext);

                    playPauseButtonParams.width = 0;

                    playPauseButtonParams.startToStart = mAddTimeButton.getId();
                    playPauseButtonParams.endToEnd = mAddTimeButton.getId();
                    playPauseButtonParams.topToBottom = ConstraintLayout.LayoutParams.UNSET;
                    playPauseButtonParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;

                    playPauseButtonParams.rightMargin = 0;
                    playPauseButtonParams.topMargin = 0;

                    mPlayPauseButton.setPadding(playPauseButtonPadding, playPauseButtonPadding,
                            playPauseButtonPadding, playPauseButtonPadding);
                }
            }

            switch (mLastState) {
                case RESET -> {
                    mResetButton.setVisibility(GONE);
                    mResetButton.setContentDescription(null);
                    mAddTimeButton.setVisibility(INVISIBLE);
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_play));

                    if (isTabletOrLandscapePhone() || isPhoneWithSingleTimer()) {
                        mTimerEditNewDurationButton.setVisibility(VISIBLE);
                    }

                    if (isPortraitPhoneWithMultipleTimers()) {
                        mCircleContainer.setVisibility(GONE);
                        mTimerTotalDurationText.setVisibility(VISIBLE);
                    }
                }

                case PAUSED -> {
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_play));

                    if (isTabletOrLandscapePhone() || isPhoneWithSingleTimer()) {
                        mTimerEditNewDurationButton.setVisibility(GONE);
                    }

                    if (isPortraitPhoneWithMultipleTimers()) {
                        mCircleContainer.setVisibility(VISIBLE);
                        mTimerTotalDurationText.setVisibility(GONE);
                    }
                }

                case RUNNING -> {
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_pause));

                    if (isTabletOrLandscapePhone() || isPhoneWithSingleTimer()) {
                        mTimerEditNewDurationButton.setVisibility(GONE);
                    }

                    if (isPortraitPhoneWithMultipleTimers()) {
                        mCircleContainer.setVisibility(VISIBLE);
                        mTimerTotalDurationText.setVisibility(GONE);
                    }
                }

                case EXPIRED, MISSED -> {
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_stop));

                    if (isTabletOrLandscapePhone() || isPhoneWithSingleTimer()) {
                        mTimerEditNewDurationButton.setVisibility(GONE);
                    }

                    if (isPortraitPhoneWithMultipleTimers()) {
                        mCircleContainer.setVisibility(VISIBLE);
                        mTimerTotalDurationText.setVisibility(GONE);
                    }

                    mResetButton.setVisibility(GONE);
                }
            }
        }
    }

    /**
     * @return {@code true} if the device is a phone in portrait mode with multiple timers displayed.
     * {@code false} otherwise.
     */
    private boolean isPortraitPhoneWithMultipleTimers() {
        return !mIsTablet && mIsPortrait && DataModel.getDataModel().getTimers().size() > 1;
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
    private boolean isPhoneWithSingleTimer() {
        return !mIsTablet && DataModel.getDataModel().getTimers().size() == 1;
    }
}