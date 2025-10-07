/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerStringFormatter;
import com.best.deskclock.events.Events;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TimerViewHolder extends RecyclerView.ViewHolder {

    private int mTimerId;
    private final TimerItem mTimerItem;
    private final TimerClickHandler mTimerClickHandler;

    public TimerViewHolder(View view, TimerClickHandler timerClickHandler) {
        super(view);

        final Context context = view.getContext();
        mTimerItem = (TimerItem) view;
        mTimerClickHandler = timerClickHandler;

        View timerLabel = view.findViewById(R.id.timer_label);
        View resetButton = view.findViewById(R.id.reset);
        View timerTotalDuration = view.findViewById(R.id.timer_total_duration);
        View timerEditNewDurationButton = view.findViewById(R.id.timer_edit_new_duration_button);
        View addTimeButton = view.findViewById(R.id.timer_add_time_button);
        View circleContainer = view.findViewById(R.id.circle_container);
        View timerTimeText = view.findViewById(R.id.timer_time_text);
        View playPauseButton = view.findViewById(R.id.play_pause);
        View deleteButton = view.findViewById(R.id.delete_timer);

        View.OnClickListener playPauseListener = v -> {
            Utils.setVibrationTime(context, 50);
            if (getTimer().isPaused() || getTimer().isReset()) {
                DataModel.getDataModel().startTimer(getTimer());
            } else if (getTimer().isRunning()) {
                DataModel.getDataModel().pauseTimer(getTimer());
            } else if (getTimer().isExpired() || getTimer().isMissed()) {
                DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_deskclock);
            }
        };

        timerLabel.setOnClickListener(v -> mTimerClickHandler.onEditLabelClicked(getTimer()));

        resetButton.setOnClickListener(v -> {
            DataModel.getDataModel().resetOrDeleteTimer(getTimer(), R.string.label_deskclock);
            Utils.setVibrationTime(context, 10);
        });

        addTimeButton.setOnClickListener(v -> {
            DataModel.getDataModel().addCustomTimeToTimer(getTimer());
            Utils.setVibrationTime(context, 10);
            Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, R.string.label_deskclock);

            // Must use getTimer() because old timer is no longer accurate.
            final long currentTime = getTimer().getRemainingTime();
            final String buttonTime = getTimer().getButtonTime();
            if (currentTime > 0) {
                v.announceForAccessibility(TimerStringFormatter.formatString(context,
                        R.string.timer_accessibility_custom_time_added, buttonTime, currentTime, true));
            }
        });

        addTimeButton.setOnLongClickListener(v -> {
            mTimerClickHandler.onEditAddTimeButtonLongClicked(getTimer());
            return true;
        });

        // Only possible for portrait mode phones with multiple timers
        if (timerTotalDuration != null) {
            timerTotalDuration.setOnClickListener(v -> {
                if (!getTimer().isReset()) {
                    return;
                }

                mTimerClickHandler.onDurationClicked(getTimer());
            });
        }

        // Only possible for tablets, landscape phones or when there is only one timer
        if (timerEditNewDurationButton != null) {
            timerEditNewDurationButton.setOnClickListener(v -> {
                if (!getTimer().isReset()) {
                    return;
                }

                mTimerClickHandler.onDurationClicked(getTimer());
            });
        }

        if (circleContainer != null) {
            circleContainer.setOnClickListener(playPauseListener);
            circleContainer.setOnTouchListener(new Utils.CircleTouchListener());
        }

        if (!ThemeUtils.isTablet() && ThemeUtils.isLandscape()) {
            timerTimeText.setOnClickListener(playPauseListener);
        }

        playPauseButton.setOnClickListener(playPauseListener);

        deleteButton.setOnClickListener(v -> {
            Utils.setVibrationTime(context, 10);

            if (SettingsDAO.isWarningDisplayedBeforeDeletingTimer(getDefaultSharedPreferences(context))) {
                final Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_delete);
                if (drawable != null) {
                    drawable.setTint(MaterialColors.getColor(
                            context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
                }
                // Get the title of the timer if there is one; otherwise, get the total duration.
                final String dialogMessage;
                if (getTimer().getLabel().isEmpty()) {
                    dialogMessage = context.getString(R.string.warning_dialog_message, getTimer().getTotalDuration());
                } else {
                    dialogMessage = context.getString(R.string.warning_dialog_message, getTimer().getLabel());
                }

                new MaterialAlertDialogBuilder(context)
                        .setIcon(drawable)
                        .setTitle(R.string.warning_dialog_title)
                        .setMessage(dialogMessage)
                        .setPositiveButton(android.R.string.ok, (dialog1, which) ->
                                DataModel.getDataModel().removeTimer(getTimer()))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                DataModel.getDataModel().removeTimer(getTimer());
            }
        });
    }

    public void onBind(int timerId) {
        mTimerId = timerId;

        final Timer timer = getTimer();
        if (timer != null) {
            mTimerItem.bindTimer(timer);
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

            mTimerItem.updateTimeDisplay(timer);

            // Use a 500 ms delay for paused, expired, or missed timers to ensure
            // more frequent updates needed for smooth blinking (based on a 500 ms interval).
            // For running timers, a 1000 ms delay is sufficient to save resources.
            final long delay = timer.isPaused() || timer.isExpired() || timer.isMissed()
                    ? 500
                    : 1000;
            mTimerItem.postDelayed(this, delay);
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
        mTimerItem.post(mUpdateRunnable);
    }

    /**
     * Stops the timer update cycle.
     * <p>
     * This method cancels any pending executions of the update runnable.
     */
    public void stopUpdating() {
        mTimerItem.removeCallbacks(mUpdateRunnable);
    }

}
