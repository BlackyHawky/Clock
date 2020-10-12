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

import static com.android.deskclock.NotificationUtils.FIRING_NOTIFICATION_CHANNEL_ID;
import static com.android.deskclock.NotificationUtils.TIMER_MODEL_NOTIFICATION_CHANNEL_ID;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.SystemClock;
import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.android.deskclock.AlarmUtils;
import com.android.deskclock.NotificationUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;
import com.android.deskclock.timer.ExpiredTimersActivity;
import com.android.deskclock.timer.TimerService;

import java.util.ArrayList;
import java.util.List;

import static androidx.core.app.NotificationCompat.Action;
import static androidx.core.app.NotificationCompat.Builder;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * Builds notifications to reflect the latest state of the timers.
 */
class TimerNotificationBuilder {

    private static final int REQUEST_CODE_UPCOMING = 0;
    private static final int REQUEST_CODE_MISSING = 1;

    public Notification build(Context context, NotificationModel nm, List<Timer> unexpired) {
        final Timer timer = unexpired.get(0);
        final int count = unexpired.size();

        // Compute some values required below.
        final boolean running = timer.isRunning();
        final Resources res = context.getResources();

        final long base = getChronometerBase(timer);
        final String pname = context.getPackageName();

        final List<Action> actions = new ArrayList<>(2);

        final CharSequence stateText;
        if (count == 1) {
            if (running) {
                // Single timer is running.
                if (TextUtils.isEmpty(timer.getLabel())) {
                    stateText = res.getString(R.string.timer_notification_label);
                } else {
                    stateText = timer.getLabel();
                }

                // Left button: Pause
                final Intent pause = new Intent(context, TimerService.class)
                        .setAction(TimerService.ACTION_PAUSE_TIMER)
                        .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId());

                @DrawableRes final int icon1 = R.drawable.ic_pause_24dp;
                final CharSequence title1 = res.getText(R.string.timer_pause);
                final PendingIntent intent1 = Utils.pendingServiceIntent(context, pause);
                actions.add(new Action.Builder(icon1, title1, intent1).build());

                // Right Button: +1 Minute
                final Intent addMinute = new Intent(context, TimerService.class)
                        .setAction(TimerService.ACTION_ADD_MINUTE_TIMER)
                        .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId());

                @DrawableRes final int icon2 = R.drawable.ic_add_24dp;
                final CharSequence title2 = res.getText(R.string.timer_plus_1_min);
                final PendingIntent intent2 = Utils.pendingServiceIntent(context, addMinute);
                actions.add(new Action.Builder(icon2, title2, intent2).build());

            } else {
                // Single timer is paused.
                stateText = res.getString(R.string.timer_paused);

                // Left button: Start
                final Intent start = new Intent(context, TimerService.class)
                        .setAction(TimerService.ACTION_START_TIMER)
                        .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId());

                @DrawableRes final int icon1 = R.drawable.ic_start_24dp;
                final CharSequence title1 = res.getText(R.string.sw_resume_button);
                final PendingIntent intent1 = Utils.pendingServiceIntent(context, start);
                actions.add(new Action.Builder(icon1, title1, intent1).build());

                // Right Button: Reset
                final Intent reset = new Intent(context, TimerService.class)
                        .setAction(TimerService.ACTION_RESET_TIMER)
                        .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId());

                @DrawableRes final int icon2 = R.drawable.ic_reset_24dp;
                final CharSequence title2 = res.getText(R.string.sw_reset_button);
                final PendingIntent intent2 = Utils.pendingServiceIntent(context, reset);
                actions.add(new Action.Builder(icon2, title2, intent2).build());
            }
        } else {
            if (running) {
                // At least one timer is running.
                stateText = res.getString(R.string.timers_in_use, count);
            } else {
                // All timers are paused.
                stateText = res.getString(R.string.timers_stopped, count);
            }

            final Intent reset = TimerService.createResetUnexpiredTimersIntent(context);

            @DrawableRes final int icon1 = R.drawable.ic_reset_24dp;
            final CharSequence title1 = res.getText(R.string.timer_reset_all);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, reset);
            actions.add(new Action.Builder(icon1, title1, intent1).build());
        }

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, TimerService.class)
                .setAction(TimerService.ACTION_SHOW_TIMER)
                .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId())
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp =
                PendingIntent.getService(context, REQUEST_CODE_UPCOMING, showApp,
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        final Builder notification = new NotificationCompat.Builder(
                context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID)
                        .setOngoing(true)
                        .setLocalOnly(true)
                        .setShowWhen(false)
                        .setAutoCancel(false)
                        .setContentIntent(pendingShowApp)
                        .setPriority(Notification.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setSmallIcon(R.drawable.stat_notify_timer)
                        .setSortKey(nm.getTimerNotificationSortKey())
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setColor(ContextCompat.getColor(context, R.color.default_background));

        for (Action action : actions) {
            notification.addAction(action);
        }

        if (Utils.isNOrLater()) {
            notification.setCustomContentView(buildChronometer(pname, base, running, stateText))
                    .setGroup(nm.getTimerNotificationGroupKey());
        } else {
            final CharSequence contentTextPreN;
            if (count == 1) {
                contentTextPreN = TimerStringFormatter.formatTimeRemaining(context,
                        timer.getRemainingTime(), false);
            } else if (running) {
                final String timeRemaining = TimerStringFormatter.formatTimeRemaining(context,
                        timer.getRemainingTime(), false);
                contentTextPreN = context.getString(R.string.next_timer_notif, timeRemaining);
            } else {
                contentTextPreN = context.getString(R.string.all_timers_stopped_notif);
            }

            notification.setContentTitle(stateText).setContentText(contentTextPreN);

            final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            final Intent updateNotification = TimerService.createUpdateNotificationIntent(context);
            final long remainingTime = timer.getRemainingTime();
            if (timer.isRunning() && remainingTime > MINUTE_IN_MILLIS) {
                // Schedule a callback to update the time-sensitive information of the running timer
                final PendingIntent pi =
                        PendingIntent.getService(context, REQUEST_CODE_UPCOMING, updateNotification,
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
        }

        NotificationUtils.createChannel(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID);
        return notification.build();
    }

    Notification buildHeadsUp(Context context, List<Timer> expired) {
        final Timer timer = expired.get(0);

        // First action intent is to reset all timers.
        @DrawableRes final int icon1 = R.drawable.ic_stop_24dp;
        final Intent reset = TimerService.createResetExpiredTimersIntent(context);
        final PendingIntent intent1 = Utils.pendingServiceIntent(context, reset);

        // Generate some descriptive text, a title, and an action name based on the timer count.
        final CharSequence stateText;
        final int count = expired.size();
        final List<Action> actions = new ArrayList<>(2);
        if (count == 1) {
            final String label = timer.getLabel();
            if (TextUtils.isEmpty(label)) {
                stateText = context.getString(R.string.timer_times_up);
            } else {
                stateText = label;
            }

            // Left button: Reset single timer
            final CharSequence title1 = context.getString(R.string.timer_stop);
            actions.add(new Action.Builder(icon1, title1, intent1).build());

            // Right button: Add minute
            final Intent addTime = TimerService.createAddMinuteTimerIntent(context, timer.getId());
            final PendingIntent intent2 = Utils.pendingServiceIntent(context, addTime);
            @DrawableRes final int icon2 = R.drawable.ic_add_24dp;
            final CharSequence title2 = context.getString(R.string.timer_plus_1_min);
            actions.add(new Action.Builder(icon2, title2, intent2).build());
        } else {
            stateText = context.getString(R.string.timer_multi_times_up, count);

            // Left button: Reset all timers
            final CharSequence title1 = context.getString(R.string.timer_stop_all);
            actions.add(new Action.Builder(icon1, title1, intent1).build());
        }

        final long base = getChronometerBase(timer);

        final String pname = context.getPackageName();

        // Content intent shows the timer full screen when clicked.
        final Intent content = new Intent(context, ExpiredTimersActivity.class);
        final PendingIntent contentIntent = Utils.pendingActivityIntent(context, content);

        // Full screen intent has flags so it is different than the content intent.
        final Intent fullScreen = new Intent(context, ExpiredTimersActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        final PendingIntent pendingFullScreen = Utils.pendingActivityIntent(context, fullScreen);

        final Builder notification = new NotificationCompat.Builder(
                context, FIRING_NOTIFICATION_CHANNEL_ID)
                        .setOngoing(true)
                        .setLocalOnly(true)
                        .setShowWhen(false)
                        .setAutoCancel(false)
                        .setContentIntent(contentIntent)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setDefaults(Notification.DEFAULT_LIGHTS)
                        .setSmallIcon(R.drawable.stat_notify_timer)
                        .setFullScreenIntent(pendingFullScreen, true)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setColor(ContextCompat.getColor(context, R.color.default_background));

        for (Action action : actions) {
            notification.addAction(action);
        }

        if (Utils.isNOrLater()) {
            notification.setCustomContentView(buildChronometer(pname, base, true, stateText));
        } else {
            final CharSequence contentTextPreN = count == 1
                    ? context.getString(R.string.timer_times_up)
                    : context.getString(R.string.timer_multi_times_up, count);

            notification.setContentTitle(stateText).setContentText(contentTextPreN);
        }

        NotificationUtils.createChannel(context, FIRING_NOTIFICATION_CHANNEL_ID);
        return notification.build();
    }

    Notification buildMissed(Context context, NotificationModel nm,
            List<Timer> missedTimers) {
        final Timer timer = missedTimers.get(0);
        final int count = missedTimers.size();

        // Compute some values required below.
        final long base = getChronometerBase(timer);
        final String pname = context.getPackageName();
        final Resources res = context.getResources();

        final Action action;

        final CharSequence stateText;
        if (count == 1) {
            // Single timer is missed.
            if (TextUtils.isEmpty(timer.getLabel())) {
                stateText = res.getString(R.string.missed_timer_notification_label);
            } else {
                stateText = res.getString(R.string.missed_named_timer_notification_label,
                        timer.getLabel());
            }

            // Reset button
            final Intent reset = new Intent(context, TimerService.class)
                    .setAction(TimerService.ACTION_RESET_TIMER)
                    .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId());

            @DrawableRes final int icon1 = R.drawable.ic_reset_24dp;
            final CharSequence title1 = res.getText(R.string.timer_reset);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, reset);
            action = new Action.Builder(icon1, title1, intent1).build();
        } else {
            // Multiple missed timers.
            stateText = res.getString(R.string.timer_multi_missed, count);

            final Intent reset = TimerService.createResetMissedTimersIntent(context);

            @DrawableRes final int icon1 = R.drawable.ic_reset_24dp;
            final CharSequence title1 = res.getText(R.string.timer_reset_all);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, reset);
            action = new Action.Builder(icon1, title1, intent1).build();
        }

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, TimerService.class)
                .setAction(TimerService.ACTION_SHOW_TIMER)
                .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId())
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp =
                PendingIntent.getService(context, REQUEST_CODE_MISSING, showApp,
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        final Builder notification = new NotificationCompat.Builder(
                context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID)
                        .setLocalOnly(true)
                        .setShowWhen(false)
                        .setAutoCancel(false)
                        .setContentIntent(pendingShowApp)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setSmallIcon(R.drawable.stat_notify_timer)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setSortKey(nm.getTimerNotificationMissedSortKey())
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .addAction(action)
                        .setColor(ContextCompat.getColor(context, R.color.default_background));

        if (Utils.isNOrLater()) {
            notification.setCustomContentView(buildChronometer(pname, base, true, stateText))
                    .setGroup(nm.getTimerNotificationGroupKey());
        } else {
            final CharSequence contentText = AlarmUtils.getFormattedTime(context,
                    timer.getWallClockExpirationTime());
            notification.setContentText(contentText).setContentTitle(stateText);
        }

        NotificationUtils.createChannel(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID);
        return notification.build();
    }

    /**
     * @param timer the timer on which to base the chronometer display
     * @return the time at which the chronometer will/did reach 0:00 in realtime
     */
    private static long getChronometerBase(Timer timer) {
        // The in-app timer display rounds *up* to the next second for positive timer values. Mirror
        // that behavior in the notification's Chronometer by padding in an extra second as needed.
        final long remaining = timer.getRemainingTime();
        final long adjustedRemaining = remaining < 0 ? remaining : remaining + SECOND_IN_MILLIS;

        // Chronometer will/did reach 0:00 adjustedRemaining milliseconds from now.
        return SystemClock.elapsedRealtime() + adjustedRemaining;
    }

    @TargetApi(Build.VERSION_CODES.N)
    private RemoteViews buildChronometer(String pname, long base, boolean running,
            CharSequence stateText) {
        final RemoteViews content = new RemoteViews(pname, R.layout.chronometer_notif_content);
        content.setChronometerCountDown(R.id.chronometer, true);
        content.setChronometer(R.id.chronometer, base, null, running);
        content.setTextViewText(R.id.state, stateText);
        return content;
    }
}
