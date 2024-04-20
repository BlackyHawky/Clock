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
package com.best.deskclock.data;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static androidx.core.app.NotificationCompat.Action;
import static androidx.core.app.NotificationCompat.Builder;
import static com.best.deskclock.NotificationUtils.FIRING_NOTIFICATION_CHANNEL_ID;
import static com.best.deskclock.NotificationUtils.TIMER_MODEL_NOTIFICATION_CHANNEL_ID;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;

import com.best.deskclock.AlarmUtils;
import com.best.deskclock.DeskClock;
import com.best.deskclock.NotificationUtils;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.events.Events;
import com.best.deskclock.timer.ExpiredTimersActivity;
import com.best.deskclock.timer.TimerService;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds notifications to reflect the latest state of the timers.
 */
class TimerNotificationBuilder {

    private static final int REQUEST_CODE_UPCOMING = 0;

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

    public Notification build(Context context, NotificationModel nm, List<Timer> unexpired) {
        final Timer timer = unexpired.get(0);
        final int count = unexpired.size();

        // Compute some values required below.
        final boolean running = timer.isRunning();

        final long base = getChronometerBase(timer);

        final List<Action> actions = new ArrayList<>(2);

        final CharSequence titleText;

        final CharSequence stateText;
        if (count == 1) {
            if (TextUtils.isEmpty(timer.getLabel())) {
                titleText = context.getString(R.string.timer_notification_label);
            } else {
                titleText = timer.getLabel();
            }

            if (running) {
                stateText = null;
                // Left button: Pause
                final Intent pause = new Intent(context, TimerService.class)
                        .setAction(TimerService.ACTION_PAUSE_TIMER)
                        .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId());

                @DrawableRes final int icon1 = R.drawable.ic_fab_pause;
                final CharSequence title1 = context.getText(R.string.timer_pause);
                final PendingIntent intent1 = Utils.pendingServiceIntent(context, pause);
                actions.add(new Action.Builder(icon1, title1, intent1).build());

                // Right Button: +1 Minute
                final Intent addMinute = new Intent(context, TimerService.class)
                        .setAction(TimerService.ACTION_ADD_MINUTE_TIMER)
                        .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId());

                @DrawableRes final int icon2 = R.drawable.ic_add;
                final CharSequence title2 = context.getText(R.string.timer_plus_1_min);
                final PendingIntent intent2 = Utils.pendingServiceIntent(context, addMinute);
                actions.add(new Action.Builder(icon2, title2, intent2).build());

            } else {
                // Single timer is paused.
                stateText = context.getString(R.string.timer_paused);

                // Left button: Start
                final Intent start = new Intent(context, TimerService.class)
                        .setAction(TimerService.ACTION_START_TIMER)
                        .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId());

                @DrawableRes final int icon1 = R.drawable.ic_fab_play;
                final CharSequence title1 = context.getText(R.string.sw_resume_button);
                final PendingIntent intent1 = Utils.pendingServiceIntent(context, start);
                actions.add(new Action.Builder(icon1, title1, intent1).build());

                // Right Button: Reset
                final Intent reset = new Intent(context, TimerService.class)
                        .setAction(TimerService.ACTION_RESET_TIMER)
                        .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId());

                @DrawableRes final int icon2 = R.drawable.ic_reset;
                final CharSequence title2 = context.getText(R.string.sw_reset_button);
                final PendingIntent intent2 = Utils.pendingServiceIntent(context, reset);
                actions.add(new Action.Builder(icon2, title2, intent2).build());
            }
        } else {
            if (running) {
                // At least one timer is running.
                titleText = context.getString(R.string.timers_in_use, count);
            } else {
                // All timers are paused.
                titleText = context.getString(R.string.timers_stopped, count);
            }

            stateText = null;

            final Intent reset = TimerService.createResetUnexpiredTimersIntent(context);

            @DrawableRes final int icon1 = R.drawable.ic_reset;
            final CharSequence title1 = context.getText(R.string.timer_reset_all);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, reset);
            actions.add(new Action.Builder(icon1, title1, intent1).build());
        }

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(TimerService.ACTION_SHOW_TIMER)
                .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId())
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        final Builder notification = new Builder(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setContentIntent(pendingShowApp)
                .setPriority(Notification.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSmallIcon(R.drawable.ic_tab_timer_static)
                .setSortKey(nm.getTimerNotificationSortKey())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setColor(context.getColor(R.color.md_theme_primary));

        for (Action action : actions) {
            notification.addAction(action);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.setCustomContentView(buildChronometer(context.getPackageName(), base, running, titleText, stateText))
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

            notification.setContentTitle(titleText).setContentText(contentTextPreN);

            final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            final Intent updateNotification = TimerService.createUpdateNotificationIntent(context);
            final long remainingTime = timer.getRemainingTime();
            if (timer.isRunning() && remainingTime > MINUTE_IN_MILLIS) {
                // Schedule a callback to update the time-sensitive information of the running timer
                final PendingIntent pi = PendingIntent.getService(context, REQUEST_CODE_UPCOMING, updateNotification,
                                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                final long nextMinuteChange = remainingTime % MINUTE_IN_MILLIS;
                final long triggerTime = SystemClock.elapsedRealtime() + nextMinuteChange;
                TimerModel.schedulePendingIntent(am, triggerTime, pi);
            } else {
                // Cancel the update notification callback.
                final PendingIntent pi = PendingIntent.getService(context, 0, updateNotification,
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                if (pi != null) {
                    am.cancel(pi);
                    pi.cancel();
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID);
        }
        return notification.build();
    }

    Notification buildHeadsUp(Context context, List<Timer> expired) {
        final Timer timer = expired.get(0);

        // First action intent is to reset all timers.
        @DrawableRes final int icon1 = R.drawable.ic_fab_stop;
        final Intent reset = TimerService.createResetExpiredTimersIntent(context);
        final PendingIntent intent1 = Utils.pendingServiceIntent(context, reset);

        // Generate some descriptive text, a title, and an action name based on the timer count.
        final CharSequence titleText;
        final String label = timer.getLabel();

        final CharSequence stateText;
        final int count = expired.size();
        final List<Action> actions = new ArrayList<>(2);
        if (count == 1) {
            if (TextUtils.isEmpty(label)) {
                titleText = context.getString(R.string.timer_notification_label);
            } else {
                titleText = label;
            }

            stateText = context.getString(R.string.timer_times_up);

            // Left button: Reset single timer
            final CharSequence title1 = context.getString(R.string.timer_stop);
            actions.add(new Action.Builder(icon1, title1, intent1).build());

            // Right button: Add minute
            final Intent addTime = TimerService.createAddMinuteTimerIntent(context, timer.getId());
            final PendingIntent intent2 = Utils.pendingServiceIntent(context, addTime);
            @DrawableRes final int icon2 = R.drawable.ic_add;
            final CharSequence title2 = context.getString(R.string.timer_plus_1_min);
            actions.add(new Action.Builder(icon2, title2, intent2).build());
        } else {
            titleText = context.getString(R.string.timer_multi_times_up, count);
            stateText = null;

            // Left button: Reset all timers
            final CharSequence title1 = context.getString(R.string.timer_stop_all);
            actions.add(new Action.Builder(icon1, title1, intent1).build());
        }

        final long base = getChronometerBase(timer);

        // Content intent shows the timer full screen when clicked.
        final Intent content = new Intent(context, ExpiredTimersActivity.class);
        final PendingIntent contentIntent = Utils.pendingActivityIntent(context, content);

        // Full screen intent has flags so it is different than the content intent.
        final Intent fullScreen = new Intent(context, ExpiredTimersActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        final PendingIntent pendingFullScreen = Utils.pendingActivityIntent(context, fullScreen);

        final Builder notification = new Builder(context, FIRING_NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setContentIntent(contentIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setSmallIcon(R.drawable.ic_tab_timer_static)
                .setFullScreenIntent(pendingFullScreen, true)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setColor(context.getColor(R.color.md_theme_primary));

        for (Action action : actions) {
            notification.addAction(action);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.setCustomContentView(buildChronometer(context.getPackageName(), base, true, titleText, stateText));
        } else {
            final CharSequence contentTextPreN = count == 1
                    ? context.getString(R.string.timer_times_up)
                    : null;

            notification.setContentTitle(titleText).setContentText(contentTextPreN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(context, FIRING_NOTIFICATION_CHANNEL_ID);
        }
        return notification.build();
    }

    Notification buildMissed(Context context, NotificationModel nm, List<Timer> missedTimers) {
        final Timer timer = missedTimers.get(0);
        final int count = missedTimers.size();

        // Compute some values required below.
        final long base = getChronometerBase(timer);
        final Resources res = context.getResources();

        final Action action;

        final CharSequence titleText;
        final String label = timer.getLabel();
        final CharSequence stateText;
        if (count == 1) {
            // Single timer is missed.
            if (TextUtils.isEmpty(label)) {
                titleText = context.getString(R.string.timer_notification_label);
            } else {
                titleText = label;
            }

            stateText = res.getString(R.string.missed_named_timer_notification_label, label);

            // Reset button
            final Intent reset = new Intent(context, TimerService.class)
                    .setAction(TimerService.ACTION_RESET_TIMER)
                    .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId());

            @DrawableRes final int icon1 = R.drawable.ic_reset;
            final CharSequence title1 = res.getText(R.string.timer_reset);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, reset);
            action = new Action.Builder(icon1, title1, intent1).build();
        } else {
            // Multiple missed timers.
            titleText = res.getString(R.string.timer_multi_missed, count);

            stateText = null;

            final Intent reset = TimerService.createResetMissedTimersIntent(context);

            @DrawableRes final int icon1 = R.drawable.ic_reset;
            final CharSequence title1 = res.getText(R.string.timer_reset_all);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, reset);
            action = new Action.Builder(icon1, title1, intent1).build();
        }

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(TimerService.ACTION_SHOW_TIMER)
                .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId())
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        final Builder notification = new Builder(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setContentIntent(pendingShowApp)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSmallIcon(R.drawable.ic_tab_timer_static)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSortKey(nm.getTimerNotificationMissedSortKey())
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .addAction(action)
                .setColor(context.getColor(R.color.md_theme_primary));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.setCustomContentView(buildChronometer(context.getPackageName(), base, true, titleText, stateText))
                    .setGroup(nm.getTimerNotificationGroupKey());
        } else {
            final CharSequence contentText = AlarmUtils.getFormattedTime(context, timer.getWallClockExpirationTime());
            notification.setContentTitle(titleText).setContentText(contentText);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID);
        }
        return notification.build();
    }

    private RemoteViews buildChronometer(String packageName, long base, boolean running, CharSequence titleText,
                                         CharSequence stateText) {

        final RemoteViews content = new RemoteViews(packageName, R.layout.chronometer_notif_content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            content.setChronometerCountDown(R.id.chronometer, true);
        }
        content.setChronometer(R.id.chronometer, base, null, running);
        content.setTextViewText(R.id.title, titleText);
        content.setTextViewText(R.id.state, stateText);
        return content;
    }
}
