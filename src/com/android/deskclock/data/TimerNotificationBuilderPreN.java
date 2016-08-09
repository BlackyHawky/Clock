/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.data;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.android.deskclock.HandleDeskClockApiCalls;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.timer.ExpiredTimersActivity;
import com.android.deskclock.timer.TimerService;

import java.util.List;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

/**
 * Builds KK, L, or M-style notifications to reflect the latest state of the unexpired timers.
 */
class TimerNotificationBuilderPreN implements TimerModel.NotificationBuilder {

    @Override
    public Notification build(Context context, NotificationModel nm, List<Timer> unexpired) {
        final Timer timer = unexpired.get(0);
        final long remainingTime = timer.getRemainingTime();

        // Generate some descriptive text, a title, and some actions based on timer states.
        final String contentText;
        final String contentTitle;
        @DrawableRes int firstActionIconId, secondActionIconId = 0;
        @StringRes int firstActionTitleId, secondActionTitleId = 0;
        Intent firstActionIntent, secondActionIntent = null;

        if (unexpired.size() == 1) {
            contentText = formatElapsedTimeUntilExpiry(context, remainingTime);

            if (timer.isRunning()) {
                // Single timer is running.
                if (TextUtils.isEmpty(timer.getLabel())) {
                    contentTitle = context.getString(R.string.timer_notification_label);
                } else {
                    contentTitle = timer.getLabel();
                }

                firstActionIconId = R.drawable.ic_pause_24dp;
                firstActionTitleId = R.string.timer_pause;
                firstActionIntent = new Intent(context, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_PAUSE_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());

                secondActionIconId = R.drawable.ic_add_24dp;
                secondActionTitleId = R.string.timer_plus_1_min;
                secondActionIntent = new Intent(context, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_ADD_MINUTE_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());
            } else {
                // Single timer is paused.
                contentTitle = context.getString(R.string.timer_paused);

                firstActionIconId = R.drawable.ic_start_24dp;
                firstActionTitleId = R.string.sw_resume_button;
                firstActionIntent = new Intent(context, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_START_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());

                secondActionIconId = R.drawable.ic_reset_24dp;
                secondActionTitleId = R.string.sw_reset_button;
                secondActionIntent = new Intent(context, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_RESET_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());
            }
        } else {
            if (timer.isRunning()) {
                // At least one timer is running.
                final String timeRemaining = formatElapsedTimeUntilExpiry(context, remainingTime);
                contentText = context.getString(R.string.next_timer_notif, timeRemaining);
                contentTitle = context.getString(R.string.timers_in_use, unexpired.size());
            } else {
                // All timers are paused.
                contentText = context.getString(R.string.all_timers_stopped_notif);
                contentTitle = context.getString(R.string.timers_stopped, unexpired.size());
            }

            firstActionIconId = R.drawable.ic_reset_24dp;
            firstActionTitleId = R.string.timer_reset_all;
            firstActionIntent = TimerService.createResetUnexpiredTimersIntent(context);
        }

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, HandleDeskClockApiCalls.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(HandleDeskClockApiCalls.ACTION_SHOW_TIMERS)
                .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId())
                .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = PendingIntent.getActivity(context, 0, showApp,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setOngoing(true)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setContentText(contentText)
                .setContentTitle(contentTitle)
                .setContentIntent(pendingShowApp)
                .setSmallIcon(R.drawable.stat_notify_timer)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(ContextCompat.getColor(context, R.color.default_background));

        final PendingIntent action1 = Utils.pendingServiceIntent(context, firstActionIntent);
        final String action1Title = context.getString(firstActionTitleId);
        builder.addAction(firstActionIconId, action1Title, action1);

        if (secondActionIntent != null) {
            final PendingIntent action2 = Utils.pendingServiceIntent(context, secondActionIntent);
            final String action2Title = context.getString(secondActionTitleId);
            builder.addAction(secondActionIconId, action2Title, action2);
        }

        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final Intent updateNotification = TimerService.createUpdateNotificationIntent(context);
        if (timer.isRunning() && remainingTime > MINUTE_IN_MILLIS) {
            // Schedule a callback to update the time-sensitive information of the running timer.
            final PendingIntent pi = PendingIntent.getService(context, 0, updateNotification,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

            final long nextMinuteChange = remainingTime % MINUTE_IN_MILLIS;
            final long triggerTime = SystemClock.elapsedRealtime() + nextMinuteChange;
            TimerModel.schedulePendingIntent(am, triggerTime, pi);
        } else {
            // Cancel the update notification callback.
            final PendingIntent pi = PendingIntent.getService(context, 0, updateNotification,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_NO_CREATE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }

        return builder.build();
    }

    @Override
    public Notification buildHeadsUp(Context context, List<Timer> expired) {
        // Generate some descriptive text, a title, and an action name based on the timer count.
        final int timerId;
        final String contentText;
        final String contentTitle;
        final String resetActionTitle;
        final int count = expired.size();

        if (count == 1) {
            final Timer timer = expired.get(0);
            timerId = timer.getId();
            resetActionTitle = context.getString(R.string.timer_stop);
            contentText = context.getString(R.string.timer_times_up);

            final String label = timer.getLabel();
            if (TextUtils.isEmpty(label)) {
                contentTitle = context.getString(R.string.timer_notification_label);
            } else {
                contentTitle = label;
            }
        } else {
            timerId = -1;
            contentText = context.getString(R.string.timer_multi_times_up, count);
            contentTitle = context.getString(R.string.timer_notification_label);
            resetActionTitle = context.getString(R.string.timer_stop_all);
        }

        // Content intent shows the expired timers full screen when clicked.
        final Intent content = new Intent(context, ExpiredTimersActivity.class);
        final PendingIntent pendingContent = Utils.pendingActivityIntent(context, content);

        // Full screen intent has flags so it is different than the content intent.
        final Intent fullScreen = new Intent(context, ExpiredTimersActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        final PendingIntent pendingFullScreen = Utils.pendingActivityIntent(context, fullScreen);

        // Left button: Reset timer / Reset all timers
        final Intent reset = TimerService.createResetExpiredTimersIntent(context);
        final PendingIntent pendingReset = Utils.pendingServiceIntent(context, reset);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setOngoing(true)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setContentText(contentText)
                .setContentTitle(contentTitle)
                .setContentIntent(pendingContent)
                .setColor(ContextCompat.getColor(context, R.color.default_background))
                .setSmallIcon(R.drawable.stat_notify_timer)
                .setFullScreenIntent(pendingFullScreen, true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .addAction(R.drawable.ic_stop_24dp, resetActionTitle, pendingReset);

        // Add a second action if only a single timer is expired.
        if (count == 1) {
            // Right button: Add minute
            final Intent addMinute = TimerService.createAddMinuteTimerIntent(context, timerId);
            final PendingIntent pendingAddMinute = Utils.pendingServiceIntent(context, addMinute);
            final String addMinuteTitle = context.getString(R.string.timer_plus_1_min);
            builder.addAction(R.drawable.ic_add_24dp, addMinuteTitle, pendingAddMinute);
        }

        return builder.build();
    }

    /**
     * Format "7 hours 52 minutes remaining"
     */
    @VisibleForTesting
    static String formatElapsedTimeUntilExpiry(Context context, long remainingTime) {
        final int hours = (int) remainingTime / (int) HOUR_IN_MILLIS;
        final int minutes = (int) remainingTime / ((int) MINUTE_IN_MILLIS) % 60;

        String minSeq = Utils.getNumberFormattedQuantityString(context, R.plurals.minutes, minutes);
        String hourSeq = Utils.getNumberFormattedQuantityString(context, R.plurals.hours, hours);

        // The verb "remaining" may have to change tense for singular subjects in some languages.
        final String verb = context.getString((minutes > 1 || hours > 1)
                ? R.string.timer_remaining_multiple
                : R.string.timer_remaining_single);

        final boolean showHours = hours > 0;
        final boolean showMinutes = minutes > 0;

        int formatStringId;
        if (showHours) {
            if (showMinutes) {
                formatStringId = R.string.timer_notifications_hours_minutes;
            } else {
                formatStringId = R.string.timer_notifications_hours;
            }
        } else if (showMinutes) {
            formatStringId = R.string.timer_notifications_minutes;
        } else {
            formatStringId = R.string.timer_notifications_less_min;
        }
        return String.format(context.getString(formatStringId), hourSeq, minSeq, verb);
    }
}
