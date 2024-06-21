/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.events.Events;

/**
 * <p>This service exists solely to allow {@link android.app.AlarmManager} and timer notifications
 * to alter the state of timers without disturbing the notification shade. If an activity were used
 * instead (even one that is not displayed) the notification manager implicitly closes the
 * notification shade which clashes with the use case of starting/pausing/resetting timers without
 * disturbing the notification shade.</p>
 *
 * <p>The service has a second benefit. It is used to start heads-up notifications for expired
 * timers in the foreground. This keeps the entire application in the foreground and thus prevents
 * the operating system from killing it while expired timers are firing.</p>
 */
public final class TimerService extends Service {

    /**
     * Extra for many actions specific to a given timer.
     */
    public static final String EXTRA_TIMER_ID = "com.best.deskclock.extra.TIMER_ID";
    private static final String ACTION_PREFIX = "com.best.deskclock.action.";

    /**
     * Shows the tab with timers; scrolls to a specific timer.
     */
    public static final String ACTION_SHOW_TIMER = ACTION_PREFIX + "SHOW_TIMER";

    /**
     * Pauses running timers; resets expired timers.
     */
    public static final String ACTION_PAUSE_TIMER = ACTION_PREFIX + "PAUSE_TIMER";

    /**
     * Starts the sole timer.
     */
    public static final String ACTION_START_TIMER = ACTION_PREFIX + "START_TIMER";

    /**
     * Resets the timer.
     */
    public static final String ACTION_RESET_TIMER = ACTION_PREFIX + "RESET_TIMER";

    /**
     * Adds minutes or hour to the timer.
     */
    public static final String ACTION_ADD_CUSTOM_TIME_TO_TIMER = ACTION_PREFIX + "ADD_CUSTOM_TIME_TO_TIMER";
    private static final String ACTION_TIMER_EXPIRED = ACTION_PREFIX + "TIMER_EXPIRED";
    private static final String ACTION_UPDATE_NOTIFICATION = ACTION_PREFIX + "UPDATE_NOTIFICATION";
    private static final String ACTION_RESET_EXPIRED_TIMERS = ACTION_PREFIX + "RESET_EXPIRED_TIMERS";
    private static final String ACTION_RESET_UNEXPIRED_TIMERS = ACTION_PREFIX + "RESET_UNEXPIRED_TIMERS";
    private static final String ACTION_RESET_MISSED_TIMERS = ACTION_PREFIX + "RESET_MISSED_TIMERS";

    public static Intent createTimerExpiredIntent(Context context, Timer timer) {
        final int timerId = timer == null ? -1 : timer.getId();
        return new Intent(context, TimerService.class)
                .setAction(ACTION_TIMER_EXPIRED)
                .putExtra(EXTRA_TIMER_ID, timerId);
    }

    public static Intent createResetExpiredTimersIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_RESET_EXPIRED_TIMERS);
    }

    public static Intent createResetUnexpiredTimersIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_RESET_UNEXPIRED_TIMERS);
    }

    public static Intent createResetMissedTimersIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_RESET_MISSED_TIMERS);
    }


    public static Intent createAddCustomTimeToTimerIntent(Context context, int timerId) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_ADD_CUSTOM_TIME_TO_TIMER)
                .putExtra(EXTRA_TIMER_ID, timerId);
    }

    public static Intent createUpdateNotificationIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_UPDATE_NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            final String action = intent.getAction();
            final int label = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, R.string.label_intent);
            if (action != null) {
                switch (action) {
                    case ACTION_UPDATE_NOTIFICATION -> {
                        DataModel.getDataModel().updateTimerNotification();
                        return START_NOT_STICKY;
                    }
                    case ACTION_RESET_EXPIRED_TIMERS -> {
                        DataModel.getDataModel().resetOrDeleteExpiredTimers(label);
                        return START_NOT_STICKY;
                    }
                    case ACTION_RESET_UNEXPIRED_TIMERS -> {
                        DataModel.getDataModel().resetUnexpiredTimers(label);
                        return START_NOT_STICKY;
                    }
                    case ACTION_RESET_MISSED_TIMERS -> {
                        DataModel.getDataModel().resetMissedTimers(label);
                        return START_NOT_STICKY;
                    }
                }
            }

            // Look up the timer in question.
            final int timerId = intent.getIntExtra(EXTRA_TIMER_ID, -1);
            final Timer timer = DataModel.getDataModel().getTimer(timerId);

            // If the timer cannot be located, ignore the action.
            if (timer == null) {
                return START_NOT_STICKY;
            }

            // Perform the action on the timer.
            if (action != null) {
                switch (action) {
                    case ACTION_START_TIMER -> {
                        Events.sendTimerEvent(R.string.action_start, label);
                        DataModel.getDataModel().startTimer(this, timer);
                    }
                    case ACTION_PAUSE_TIMER -> {
                        Events.sendTimerEvent(R.string.action_pause, label);
                        DataModel.getDataModel().pauseTimer(timer);
                    }
                    case ACTION_ADD_CUSTOM_TIME_TO_TIMER -> {
                        Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, label);
                        DataModel.getDataModel().addCustomTimeToTimer(timer);
                    }
                    case ACTION_RESET_TIMER -> DataModel.getDataModel().resetOrDeleteTimer(timer, label);
                    case ACTION_TIMER_EXPIRED -> {
                        Events.sendTimerEvent(R.string.action_fire, label);
                        DataModel.getDataModel().expireTimer(this, timer);
                    }
                }
            }
        } finally {
            // This service is foreground when expired timers exist and stopped when none exist.
            if (DataModel.getDataModel().getExpiredTimers().isEmpty()) {
                stopSelf();
            }
        }

        return START_NOT_STICKY;
    }
}
