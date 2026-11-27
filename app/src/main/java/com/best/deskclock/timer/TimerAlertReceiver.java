// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.events.Events;

/**
 * A {@link BroadcastReceiver} to safely launch {@link TimerService} when a timer expires.
 *
 * <p>Starting with Android 12 (API 31), the system prohibits background applications
 * from starting foreground services directly from an {@link android.app.AlarmManager} service intent.
 * This receiver intercepts the broadcast alarm event, inherits a temporary exemption window
 * from the operating system, and safely forwards the intent to the foreground service.</p>
 */
public class TimerAlertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        }

        final DataModel dataModel = DataModel.getDataModel();
        final int label = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, R.string.label_intent);

        switch (action) {
            case TimerService.ACTION_UPDATE_NOTIFICATION -> {
                dataModel.updateTimerNotification();
                return;
            }
            case TimerService.ACTION_RESET_EXPIRED_TIMERS -> {
                dataModel.resetOrDeleteExpiredTimers(label);
                return;
            }
            case TimerService.ACTION_RESET_MISSED_TIMERS -> {
                dataModel.resetOrDeleteMissedTimers(label);
                return;
            }
        }

        final int timerId = intent.getIntExtra(TimerService.EXTRA_TIMER_ID, -1);
        final Timer timer = dataModel.getTimer(timerId);
        if (timer == null) {
            return;
        }

        switch (action) {
            case TimerService.ACTION_START_TIMER -> {
                Events.sendTimerEvent(R.string.action_start, label);
                dataModel.startTimer(timer);
            }

            case TimerService.ACTION_PAUSE_TIMER -> {
                Events.sendTimerEvent(R.string.action_pause, label);
                dataModel.pauseTimer(timer);
            }

            case TimerService.ACTION_ADD_CUSTOM_TIME_TO_TIMER -> {
                Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, label);
                dataModel.addCustomTimeToTimer(timer);
            }

            case TimerService.ACTION_REMOVE_CUSTOM_TIME_TO_TIMER -> {
                Events.sendTimerEvent(R.string.action_remove_custom_time_to_timer, label);
                dataModel.removeCustomTimeFromTimer(timer);
            }

            case TimerService.ACTION_RESET_TIMER -> dataModel.resetTimer(timer, label);

            case TimerService.ACTION_TIMER_EXPIRED -> {
                Intent serviceIntent = new Intent(intent);
                serviceIntent.setClass(context, TimerService.class);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
    }
}
