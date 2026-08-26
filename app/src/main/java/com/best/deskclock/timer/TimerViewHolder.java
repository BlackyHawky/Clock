/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.databinding.TimerItemBinding;
import com.best.deskclock.databinding.TimerItemCompactBinding;
import com.best.deskclock.utils.Utils;
import com.google.android.material.button.MaterialButton;

public class TimerViewHolder extends RecyclerView.ViewHolder {

    private int mTimerId;
    private final TimerAdapter mAdapter;
    public TimerItem mTimerItem;
    public TimerItemCompact mTimerItemCompact;
    public final MaterialButton addTimeButton;
    public final View circleContainer;
    public final TextView timerTimeText;

    private final int mViewType;
    private final boolean mIsTablet;
    private final boolean mIsLandscape;

    public TimerViewHolder(@NonNull View view, @NonNull TimerAdapter timerAdapter, @NonNull TimerClickHandler timerClickHandler,
                           int viewType, @NonNull Typeface regular, @NonNull Typeface bold, boolean isTablet, boolean isLandscape) {

        super(view);

        mAdapter = timerAdapter;
        mViewType = viewType;
        mIsTablet = isTablet;
        mIsLandscape = isLandscape;

        final MaterialButton playPauseButton;
        final MaterialButton resetButton;

        switch (viewType) {
            case TimerAdapter.SINGLE_TIMER, TimerAdapter.MULTIPLE_TIMERS -> {
                mTimerItem = (TimerItem) view;
                mTimerItem.setGeneralFonts(regular, bold);

                TimerItemBinding binding = TimerItemBinding.bind(view);

                resetButton = binding.resetButton;
                addTimeButton = binding.timerAddTimeButton;
                circleContainer = binding.circleContainer;
                timerTimeText = binding.timerTimeText;
                playPauseButton = binding.playPauseButton;
            }
            case TimerAdapter.MULTIPLE_TIMERS_COMPACT -> {
                mTimerItemCompact = (TimerItemCompact) view;
                mTimerItemCompact.setGeneralFonts(regular, bold);

                TimerItemCompactBinding compactBinding = TimerItemCompactBinding.bind(view);

                resetButton = compactBinding.resetButton;
                addTimeButton = compactBinding.timerAddTimeButton;
                timerTimeText = compactBinding.timerTimeText;
                playPauseButton = compactBinding.playPauseButton;
                circleContainer = null;
            }
            default -> throw new IllegalArgumentException("Unknown ViewType: " + viewType);
        }

        itemView.setOnClickListener(v -> timerClickHandler.displayBottomSheetDialog(getTimer()));

        View.OnClickListener circleListener = v -> {
            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            timerClickHandler.onCircleClicked(getTimer());
        };

        resetButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            timerClickHandler.onResetClicked(getTimer());
        });

        addTimeButton.setOnClickListener(v -> {
            if (getTimer().isReset()) {
                return;
            }

            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.CLOCK_TICK);
            timerClickHandler.onAddTimeClicked(getTimer(), v);
        });

        if (circleContainer != null) {
            circleContainer.setOnClickListener(circleListener);
            circleContainer.setOnTouchListener(new Utils.CircleTouchListener());
        } else {
            timerTimeText.setOnClickListener(circleListener);
        }

        playPauseButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            timerClickHandler.onPlayPauseClicked(getTimer());
        });
    }

    public void applySettings(@NonNull TimerSettings settings) {
        if (mTimerItem != null) {
            mTimerItem.setTimerTimeFont(settings.timerTimeTypeface);
            mTimerItem.setTimerEndTimeFormatPattern(settings.timerEndTimeFormatPattern);
            mTimerItem.displayTimerEndTime(settings.isTimerEndTimeDisplayed);
            mTimerItem.setButtonPosition(
                settings.areTimerButtonPositionsInverted, mIsTablet, mIsLandscape, mViewType == TimerAdapter.SINGLE_TIMER);
            mTimerItem.setIndicatorColors(settings.colorPaused, settings.colorRunning, settings.colorExpired, settings.colorMissed);
            mTimerItem.setIndicatorStateDisplay(settings.isIndicatorStateDisplay);
        } else if (mTimerItemCompact != null) {
            mTimerItemCompact.setTimerTimeFont(settings.timerTimeTypeface);
            mTimerItemCompact.setTimerEndTimeFormatPattern(settings.timerEndTimeFormatPattern);
            mTimerItemCompact.displayTimerEndTime(settings.isTimerEndTimeDisplayed);
            mTimerItemCompact.setButtonPosition(settings.areTimerButtonPositionsInverted);
            mTimerItemCompact.setIndicatorColors(settings.colorPaused, settings.colorRunning, settings.colorExpired, settings.colorMissed);
            mTimerItemCompact.setIndicatorStateDisplay(settings.isIndicatorStateDisplay);
        }
    }

    public void onBind(int timerId, boolean animate) {
        mTimerId = timerId;

        final Timer timer = getTimer();
        if (timer != null) {
            if (mTimerItem != null) {
                mTimerItem.bindTimer(timer, animate);
            } else if (mTimerItemCompact != null) {
                mTimerItemCompact.bindTimer(timer, animate);
            }
        }

        updateBackground();
    }

    public void updateBackground() {
        int position = getBindingAdapterPosition();

        if (position != RecyclerView.NO_POSITION && mAdapter != null) {
            int totalCount = mAdapter.getItemCount();
            Drawable.ConstantState bgState;

            if (mAdapter.isTablet() || totalCount <= 1) {
                bgState = mAdapter.getBgStandard();
            } else if (position == 0) {
                bgState = mAdapter.getBgStart();
            } else if (position == totalCount - 1) {
                bgState = mAdapter.getBgEnd();
            } else {
                bgState = mAdapter.getBgMiddle();
            }

            if (bgState != null) {
                itemView.setBackground(bgState.newDrawable());
            }
        }
    }

    int getTimerId() {
        return mTimerId;
    }

    Timer getTimer() {
        return DataModel.getDataModel().getTimer(getTimerId());
    }

    /**
     * A periodic task that updates the timer display based on its current state.
     * <p>
     * This runnable checks the associated {@link Timer} and refreshes its visual representation
     * using {@code updateTimeDisplay(timer)}. It dynamically adjusts its update interval:
     * <ul>
     *   <li>500 ms if the timer is paused (to enable blinking effect)</li>
     *   <li>1000 ms otherwise</li>
     * </ul>
     * The task reschedules itself using {@code postDelayed()} until explicitly stopped.
     */
    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            final Timer timer = getTimer();
            if (timer == null || timer.isReset()) {
                return;
            }

            // Use a 500 ms delay for paused, expired, or missed timers to ensure
            // more frequent updates needed for smooth blinking (based on a 500 ms interval).
            // For running timers, a 1000 ms delay is sufficient to save resources.
            long delay;

            if (timer.isPaused() || timer.isExpired() || timer.isMissed()) {
                delay = 500;
            } else {
                long remainingTime = timer.getRemainingTime();
                delay = remainingTime % 1000;
                if (delay == 0) {
                    delay = 1000;
                }
            }

            if (mTimerItemCompact != null) {
                mTimerItemCompact.updateTimeDisplay(timer, true);
                mTimerItemCompact.postDelayed(this, delay);
            } else if (mTimerItem != null) {
                mTimerItem.updateTimeDisplay(timer, true);
                mTimerItem.postDelayed(this, delay);
            }
        }
    };

    /**
     * Starts the timer update cycle if it is not already running.
     * <p>
     * This method ensures that only one instance of the update runnable is active.
     * and posts the runnable to begin periodic updates.
     */
    public void startUpdating() {
        stopUpdating();
        if (mTimerItemCompact != null) {
            mTimerItemCompact.post(mUpdateRunnable);
        } else if (mTimerItem != null) {
            mTimerItem.post(mUpdateRunnable);
        }
    }

    /**
     * Stops the timer update cycle.
     * <p>
     * This method cancels any pending executions of the update runnable.
     */
    public void stopUpdating() {
        if (mTimerItemCompact != null) {
            mTimerItemCompact.removeCallbacks(mUpdateRunnable);
        } else if (mTimerItem != null) {
            mTimerItem.removeCallbacks(mUpdateRunnable);
        }
    }

}
