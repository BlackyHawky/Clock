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
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.Locale;

/**
 * This view is a visual representation of a {@link Timer}.
 */
public class TimerItem extends ConstraintLayout {

    /**
     * The container of TimerCircleView and TimerTextController
     */
    private FrameLayout mCircleContainer;

    /**
     * Formats and displays the text in the timer.
     */
    private TimerTextController mTimerTextController;

    /**
     * Displays timer progress as a color circle that changes from white to red.
     */
    private TimerCircleView mCircleView;

    /** Displays the remaining time or time since expiration. */
    private TextView mTimerText;

    /** A button that resets the timer. */
    private ImageButton mResetButton;

    /** A button that adds time to the timer. */
    private MaterialButton mAddTimeButton;

    /**
     * Displays the label associated with the timer. Tapping it presents an edit dialog.
     */
    private TextView mLabelView;

    /** A button to start / stop the timer */
    private MaterialButton mPlayPauseButton;

    /**
     * The last state of the timer that was rendered; used to avoid expensive operations.
     */
    private Timer.State mLastState;

    /**
     * The timer duration text that appears when the timer is reset
     */
    private TextView mTimerTotalDurationText;

    private boolean mIsTablet;
    private boolean mIsPortrait;
    private boolean mIsLandscape;

    public TimerItem(Context context) {
        this(context, null);
    }

    public TimerItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mIsTablet = ThemeUtils.isTablet();
        mIsPortrait = ThemeUtils.isPortrait();
        mIsLandscape = ThemeUtils.isLandscape();

        // To avoid creating a layout specifically for tablets, adjust the layout width and height here
        if (mIsTablet && mIsLandscape) {
            final ViewGroup.LayoutParams params = getLayoutParams();
            params.width = LayoutParams.MATCH_PARENT;
            params.height = LayoutParams.WRAP_CONTENT;
            setLayoutParams(params);
        }

        setBackground(ThemeUtils.cardBackground(getContext()));

        mLabelView = findViewById(R.id.timer_label);
        mResetButton = findViewById(R.id.reset);
        mAddTimeButton = findViewById(R.id.timer_add_time_button);
        mPlayPauseButton = findViewById(R.id.play_pause);
        mCircleContainer = findViewById(R.id.circle_container);
        mCircleView = mCircleContainer.findViewById(R.id.timer_time);
        // Displays the remaining time or time since expiration. Timer text serves as a virtual start/stop button.
        mTimerText = mCircleContainer.findViewById(R.id.timer_time_text);
        final int colorAccent = MaterialColors.getColor(getContext(),
                com.google.android.material.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mTimerText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
                new int[][]{{-state_activated, -state_pressed}, {}},
                new int[]{textColorPrimary, colorAccent});
        mTimerText.setTextColor(timeTextColor);
        mTimerTextController = new TimerTextController(mTimerText);

        // Necessary to avoid the null pointer exception,
        // as only the timer_item layout for portrait mode has these attributes
        if (isPortraitPhoneWithMultipleTimers()) {
            mTimerTotalDurationText = findViewById(R.id.timer_total_duration);
        }

        // The size of the Play/Pause and add time buttons are reduced for phones in landscape mode
        // due to the size of the timers unlike tablets
        if (!mIsTablet && mIsLandscape) {
            mAddTimeButton.setIncludeFontPadding(false);
            mAddTimeButton.setMinHeight(0);
            mAddTimeButton.setMinimumHeight(0);
            mAddTimeButton.setMinWidth(0);
            mAddTimeButton.setMinimumWidth(0);
            mAddTimeButton.setPadding(ThemeUtils.convertDpToPixels(10, getContext()), mAddTimeButton.getPaddingTop(),
                    ThemeUtils.convertDpToPixels(10, getContext()), mAddTimeButton.getPaddingBottom());

            mPlayPauseButton.setIncludeFontPadding(false);
            mPlayPauseButton.setMinHeight(0);
            mPlayPauseButton.setMinimumHeight(0);
            mPlayPauseButton.setMinWidth(0);
            mPlayPauseButton.setMinimumWidth(0);
            mPlayPauseButton.setPadding(ThemeUtils.convertDpToPixels(20, getContext()), mPlayPauseButton.getPaddingTop(),
                    ThemeUtils.convertDpToPixels(20, getContext()), mPlayPauseButton.getPaddingBottom());
        }
    }

    /**
     * Dynamically updates the {@code timer} display based on its current state.
     */
    void updateTimeDisplay(Timer timer) {
        final boolean blinkOff = SystemClock.elapsedRealtime() % 1000 < 500;

        mTimerTextController.setTimeString(timer.getRemainingTime());

        if (mCircleView != null) {
            final boolean hideCircle = ((timer.isExpired() || timer.isMissed()) && blinkOff)
                    || (!mIsTablet && mIsLandscape);

            mCircleView.setVisibility(hideCircle ? INVISIBLE : VISIBLE);

            if (!hideCircle) {
                mCircleView.update(timer);
            }
        }

        if (!timer.isPaused() || !blinkOff || mTimerText.isPressed()) {
            mTimerText.setAlpha(1f);
        } else {
            mTimerText.setAlpha(0f);
        }
    }

    /**
     * Initializes the {@code timer} static visual elements when binding to a ViewHolder.
     */
    void bindTimer(Timer timer) {
        // Initialize the time.
        mTimerTextController.setTimeString(timer.getRemainingTime());

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
        // (the circle is hidden for landscape phones because there is not enough space)
        if (mCircleView != null) {
            final boolean hideCircle = !mIsTablet && mIsLandscape;

            mCircleView.setVisibility(hideCircle ? INVISIBLE : VISIBLE);

            if (!hideCircle) {
                mCircleView.update(timer);
            }
        }

        mTimerText.setAlpha(1f);

        // Initialize the time value to add to timer in the "timer_add_time_button"
        String buttonTime = timer.getButtonTime();
        long totalSeconds = Long.parseLong(buttonTime);
        long buttonTimeMinutes = (totalSeconds) / 60;
        long buttonTimeSeconds = totalSeconds % 60;

        String buttonTimeFormatted = String.format(
                Locale.getDefault(),
                buttonTimeMinutes < 10 ? "%d:%02d" : "%02d:%02d",
                buttonTimeMinutes,
                buttonTimeSeconds);

        mAddTimeButton.setText(getContext().getString(R.string.timer_add_custom_time, buttonTimeFormatted));

        String buttonContentDescription = buttonTimeSeconds == 0
                ? getContext().getString(R.string.timer_add_custom_time_description, String.valueOf(buttonTimeMinutes))
                : getContext().getString(R.string.timer_add_custom_time_with_seconds_description,
                    String.valueOf(buttonTimeMinutes),
                    String.valueOf(buttonTimeSeconds));
        mAddTimeButton.setContentDescription(buttonContentDescription);

        // Initialize some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState) {
            final Context context = getContext();
            final String resetDesc = context.getString(R.string.reset);
            mResetButton.setVisibility(VISIBLE);
            mResetButton.setContentDescription(resetDesc);
            mAddTimeButton.setVisibility(VISIBLE);
            mLastState = timer.getState();

            // If the timer is reset, add a top margin so that the "Play" button is not stuck to the "Delete" button.
            if (isPortraitPhoneWithMultipleTimers()) {
                final ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mPlayPauseButton.getLayoutParams();
                params.topMargin = ThemeUtils.convertDpToPixels(timer.getState().equals(Timer.State.RESET) ? 10 : 0, context);
                mPlayPauseButton.setLayoutParams(params);
            }

            switch (mLastState) {
                case RESET -> {
                    mResetButton.setVisibility(GONE);
                    mResetButton.setContentDescription(null);
                    mAddTimeButton.setVisibility(INVISIBLE);
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_fab_play));

                    if (isPortraitPhoneWithMultipleTimers()) {
                        mCircleContainer.setVisibility(GONE);
                        mTimerTotalDurationText.setVisibility(VISIBLE);
                    }
                }

                case PAUSED -> {
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_fab_play));
                    if (isPortraitPhoneWithMultipleTimers()) {
                        mCircleContainer.setVisibility(VISIBLE);
                        mTimerTotalDurationText.setVisibility(GONE);
                    }
                }

                case RUNNING -> {
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_fab_pause));

                    if (isPortraitPhoneWithMultipleTimers()) {
                        mCircleContainer.setVisibility(VISIBLE);
                        mTimerTotalDurationText.setVisibility(GONE);
                    }
                }

                case EXPIRED, MISSED -> {
                    mResetButton.setVisibility(GONE);
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_fab_stop));
                }
            }
        }
    }

    private boolean isPortraitPhoneWithMultipleTimers() {
        return !mIsTablet && mIsPortrait && DataModel.getDataModel().getTimers().size() > 1;
    }
}