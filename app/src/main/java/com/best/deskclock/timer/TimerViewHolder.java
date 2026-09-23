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
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.Utils;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class TimerViewHolder extends RecyclerView.ViewHolder {

    private int mTimerId;
    private final TimerAdapter mAdapter;
    public final BaseTimerItem mTimerView;
    public final MaterialButton addTimeButton;
    public final View circleContainer;
    public final TextView timerTimeText;
    private final int mViewType;

    public TimerViewHolder(@NonNull View view, @NonNull TimerAdapter timerAdapter, int viewType) {

        super(view);

        mAdapter = timerAdapter;
        mViewType = viewType;
        mTimerView = (BaseTimerItem) view;
        UiConfig.Fonts fonts = mAdapter.getFonts();
        UiConfig.Haptics haptics = mAdapter.getHaptics();
        TimerClickHandler timerClickHandler = mAdapter.getTimerClickHandler();

        final MaterialButton playPauseButton;
        final MaterialButton resetButton;

        mTimerView.setGeneralFonts(fonts.general(), fonts.bold());

        switch (viewType) {
            case TimerAdapter.SINGLE_TIMER, TimerAdapter.MULTIPLE_TIMERS -> {
                TimerItemBinding binding = TimerItemBinding.bind(view);
                resetButton = binding.resetButton;
                addTimeButton = binding.timerAddTimeButton;
                circleContainer = binding.circleContainer;
                timerTimeText = binding.timerTimeText;
                playPauseButton = binding.playPauseButton;
            }
            case TimerAdapter.MULTIPLE_TIMERS_COMPACT -> {
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

        View.OnClickListener playPauseListener = v -> {
            Utils.performHapticFeedback(v, haptics.isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            timerClickHandler.onPlayPauseClicked(getTimer());
        };

        resetButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, haptics.isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            timerClickHandler.onResetClicked(getTimer());
        });

        addTimeButton.setOnClickListener(v -> {
            if (getTimer().isReset()) {
                return;
            }

            Utils.performHapticFeedback(v, haptics.isVibrationsEnabled(), HapticFeedbackConstantsCompat.CLOCK_TICK);
            timerClickHandler.onAddTimeClicked(getTimer(), v);
        });

        if (circleContainer != null) {
            circleContainer.setOnClickListener(playPauseListener);
            circleContainer.setOnTouchListener(new Utils.CircleTouchListener());
        } else {
            timerTimeText.setOnClickListener(playPauseListener);
        }

        playPauseButton.setOnClickListener(playPauseListener);
    }

    public void applySettings() {
        TimerSettings settings = mAdapter.getSettings();
        UiConfig.Fonts fonts = mAdapter.getFonts();
        UiConfig.Screen screen = mAdapter.getScreen();
        Locale appLocale = mAdapter.getLocale();
        Typeface typeface = fonts.timerFont() != null ? fonts.timerFont() : fonts.bold();

        mTimerView.checkIsLandscapePhone(screen.isLandscape() && !screen.isTablet());
        mTimerView.setTimerTimeFont(typeface);
        mTimerView.setLocale(appLocale);
        mTimerView.setTimerEndTimeFormatPattern(settings.timerEndTimeFormatPattern);
        mTimerView.displayTimerEndTime(settings.isTimerEndTimeDisplayed);
        mTimerView.setIndicatorColors(settings.colorPaused, settings.colorRunning, settings.colorExpired, settings.colorMissed);
        mTimerView.setIndicatorStateDisplay(settings.isIndicatorStateDisplay);

        if (mTimerView instanceof TimerItem item) {
            item.setButtonPosition(settings.areTimerButtonPositionsInverted, screen.isTablet(), screen.isLandscape(),
                mViewType == TimerAdapter.SINGLE_TIMER, screen.isRtl());
        } else if (mTimerView instanceof TimerItemCompact compactItem) {
            compactItem.setButtonPosition(settings.areTimerButtonPositionsInverted, screen.isRtl());
        }
    }

    public void onBind(int timerId, boolean animate) {
        mTimerId = timerId;

        final Timer timer = getTimer();
        if (timer != null) {
            mTimerView.bindTimer(timer, animate);
        }

        updateBackground();
    }

    public void updateBackground() {
        int position = getBindingAdapterPosition();

        if (position != RecyclerView.NO_POSITION && mAdapter != null) {
            int totalCount = mAdapter.getItemCount();
            Drawable.ConstantState bgState;

            if (mAdapter.getScreen().isTablet() || totalCount <= 1) {
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

            mTimerView.updateTimeDisplay(timer, true);
            mTimerView.postDelayed(this, delay);
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
        mTimerView.post(mUpdateRunnable);
    }

    /**
     * Stops the timer update cycle.
     * <p>
     * This method cancels any pending executions of the update runnable.
     */
    public void stopUpdating() {
        mTimerView.removeCallbacks(mUpdateRunnable);
    }

}
