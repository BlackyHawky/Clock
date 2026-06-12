/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

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
    private TimerSettings mSettings;
    private final TimerAdapter mAdapter;
    public TimerItem mTimerItem;
    public TimerItemCompact mTimerItemCompact;
    private final ImageButton mDeleteButton;
    private final MaterialButton mResetOrEditButton;
    public final MaterialButton addTimeButton;
    public final View circleContainer;
    public final TextView timerTimeText;

    public TimerViewHolder(View view, TimerAdapter timerAdapter, TimerClickHandler timerClickHandler, int viewType, Typeface regular,
                           Typeface bold) {

        super(view);

        mAdapter = timerAdapter;

        final TextView timerLabel;
        final MaterialButton playPauseButton;

        switch (viewType) {
            case TimerAdapter.SINGLE_TIMER, TimerAdapter.MULTIPLE_TIMERS -> {
                mTimerItem = (TimerItem) view;
                mTimerItem.setGeneralFonts(regular, bold);

                TimerItemBinding binding = TimerItemBinding.bind(view);

                timerLabel = binding.timerLabel;
                mResetOrEditButton = binding.resetOrEditButton;
                addTimeButton = binding.timerAddTimeButton;
                circleContainer = binding.circleContainer;
                timerTimeText = binding.timerTimeText;
                playPauseButton = binding.playPauseButton;
                mDeleteButton = binding.deleteTimerButton;
            }
            case TimerAdapter.MULTIPLE_TIMERS_COMPACT -> {
                mTimerItemCompact = (TimerItemCompact) view;
                mTimerItemCompact.setGeneralFonts(regular, bold);

                TimerItemCompactBinding compactBinding = TimerItemCompactBinding.bind(view);

                timerLabel = compactBinding.timerLabel;
                mResetOrEditButton = compactBinding.resetOrEditButton;
                addTimeButton = compactBinding.timerAddTimeButton;
                timerTimeText = compactBinding.timerTimeText;
                playPauseButton = compactBinding.playPauseButton;
                mDeleteButton = compactBinding.deleteTimerButton;
                circleContainer = null;
            }
            default -> throw new IllegalArgumentException("Unknown ViewType: " + viewType);
        }

        View.OnClickListener playPauseListener = v -> {
            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            timerClickHandler.onPlayPauseClicked(getTimer());
        };

        timerLabel.setOnClickListener(v -> timerClickHandler.onEditLabelClicked(getTimer()));

        mResetOrEditButton.setOnClickListener(v -> {
            if (getTimer().isReset()) {
                Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.CLOCK_TICK);
                timerClickHandler.onDurationClicked(getTimer());
            } else {
                Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                timerClickHandler.onResetClicked(getTimer());
            }
        });

        addTimeButton.setOnClickListener(v -> {
            if (getTimer().isReset()) {
                return;
            }

            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.CLOCK_TICK);
            timerClickHandler.onAddTimeClicked(getTimer(), v);
        });

        addTimeButton.setOnLongClickListener(v -> {
            timerClickHandler.onEditAddTimeButtonLongClicked(getTimer());
            return true;
        });

        if (circleContainer != null) {
            circleContainer.setOnClickListener(playPauseListener);
            circleContainer.setOnTouchListener(new Utils.CircleTouchListener());
        } else {
            timerTimeText.setOnClickListener(playPauseListener);
        }

        playPauseButton.setOnClickListener(playPauseListener);

        mDeleteButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            timerClickHandler.onDeleteTimerClicked(getTimer());
        });
    }

    public void applySettings(TimerSettings settings) {
        mSettings = settings;

        if (mTimerItem != null) {
            mTimerItem.setTimerTimeFont(settings.timerTimeTypeface);
            mTimerItem.setTimerEndTimeFormatPattern(settings.timerEndTimeFormatPattern);
            mTimerItem.displayTimerEndTime(settings.isTimerEndTimeDisplayed);
            mTimerItem.setButtonPosition(settings.areTimerButtonPositionsInverted);
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

            if (mSettings.isSingleTimerMode) {
                mDeleteButton.setVisibility(GONE);
                mResetOrEditButton.setVisibility(GONE);
            } else {
                mDeleteButton.setVisibility(VISIBLE);

                if (timer.getState() == Timer.State.EXPIRED || timer.getState() == Timer.State.MISSED) {
                    mResetOrEditButton.setVisibility(INVISIBLE);
                } else {
                    mResetOrEditButton.setVisibility(VISIBLE);
                }
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
