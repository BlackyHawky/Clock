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
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.best.deskclock.CircleButtonsLayout;
import com.best.deskclock.R;
import com.best.deskclock.TimerTextController;
import com.best.deskclock.Utils;
import com.best.deskclock.data.Timer;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

/**
 * This view is a visual representation of a {@link Timer}.
 */
public class TimerItem extends ConstraintLayout {

    /**
     * The container of TimerCircleView and TimerTextController
     */
    private CircleButtonsLayout mCircleContainer;

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

    /** A button that adds a minute to the timer. */
    private MaterialButton mAddButton;

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

    public TimerItem(Context context) {
        this(context, null);
    }

    public TimerItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLabelView = findViewById(R.id.timer_label);
        mResetButton = findViewById(R.id.reset);
        mAddButton = findViewById(R.id.add_one_min);
        mCircleView = findViewById(R.id.timer_time);
        // Displays the remaining time or time since expiration. Timer text serves as a virtual start/stop button.
        mTimerText = findViewById(R.id.timer_time_text);
        final int colorAccent = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mTimerText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
                new int[][]{{-state_activated, -state_pressed}, {}},
                new int[]{textColorPrimary, colorAccent});
        mTimerText.setTextColor(timeTextColor);
        mTimerTextController = new TimerTextController(mTimerText);
        mPlayPauseButton = findViewById(R.id.play_pause);
        mCircleContainer = findViewById(R.id.circle_container);
        // Necessary to avoid the null pointer exception, as only the timer_item layout for portrait mode has these attributes
        if (!Utils.isTablet(getContext()) && !Utils.isLandscape(getContext())) {
            mTimerTotalDurationText = findViewById(R.id.timer_total_duration);
        }
    }

    /**
     * Updates this view to display the latest state of the {@code timer}.
     */
    void update(Timer timer) {
        // Update the time.
        mTimerTextController.setTimeString(timer.getRemainingTime());

        if (!Utils.isTablet(getContext()) && !Utils.isLandscape(getContext())) {
            final String totalDuration = timer.getTotalDuration();
            mTimerTotalDurationText.setText(getContext().getString(R.string.timer_total_duration_text, totalDuration));
        }

        // Update the label if it changed.
        final String label = timer.getLabel();
        if (label.isEmpty()) {
            mLabelView.setText(getContext().getString(R.string.add_label));
            mLabelView.setAlpha(0.63f);
        } else {
            mLabelView.setText(label);
            mLabelView.setAlpha(1f);
        }

        // Update visibility of things that may blink.
        final boolean blinkOff = SystemClock.elapsedRealtime() % 1000 < 500;
        if (mCircleView != null) {
            final boolean hideCircle = (timer.isExpired() || timer.isMissed()) && blinkOff;
            mCircleView.setVisibility(hideCircle ? INVISIBLE : VISIBLE);

            if (!hideCircle) {
                // Update the progress of the circle.
                mCircleView.update(timer);
            }
        }

        if (!timer.isPaused() || !blinkOff || mTimerText.isPressed()) {
            mTimerText.setAlpha(1f);
        } else {
            mTimerText.setAlpha(0f);
        }

        // Update some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState) {
            final Context context = getContext();
            final String resetDesc = context.getString(R.string.timer_reset);
            mResetButton.setVisibility(View.VISIBLE);
            mResetButton.setContentDescription(resetDesc);
            mAddButton.setVisibility(View.VISIBLE);
            mLastState = timer.getState();

            if (!Utils.isTablet(context) && !Utils.isLandscape(context)) {
                final ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mPlayPauseButton.getLayoutParams();
                params.topMargin = Utils.toPixel(timer.getState().equals(Timer.State.RESET) ? 20 : 0, context);
                mPlayPauseButton.setLayoutParams(params);
            }

            switch (mLastState) {
                case RESET -> {
                    mResetButton.setVisibility(View.GONE);
                    mResetButton.setContentDescription(null);
                    mAddButton.setVisibility(View.INVISIBLE);
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_fab_play));
                    if (!Utils.isTablet(context) && !Utils.isLandscape(context)) {
                        mCircleContainer.setVisibility(GONE);
                        mTimerTotalDurationText.setVisibility(VISIBLE);
                    }
                }
                case PAUSED -> {
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_fab_play));
                    if (!Utils.isTablet(context) && !Utils.isLandscape(context)) {
                        mCircleContainer.setVisibility(VISIBLE);
                        mTimerTotalDurationText.setVisibility(GONE);
                    }
                }
                case RUNNING -> {
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_fab_pause));
                    if (!Utils.isTablet(context) && !Utils.isLandscape(context)) {
                        mCircleContainer.setVisibility(VISIBLE);
                        mTimerTotalDurationText.setVisibility(GONE);
                    }
                }
                case EXPIRED, MISSED -> {
                    mResetButton.setVisibility(View.GONE);
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_fab_stop));
                }
            }
        }
    }
}