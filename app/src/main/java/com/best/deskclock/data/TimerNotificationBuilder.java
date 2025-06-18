/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static androidx.core.app.NotificationCompat.Action;
import static androidx.core.app.NotificationCompat.Builder;
import static com.best.deskclock.utils.NotificationUtils.FIRING_NOTIFICATION_CHANNEL_ID;
import static com.best.deskclock.utils.NotificationUtils.TIMER_MODEL_NOTIFICATION_CHANNEL_ID;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.events.Events;
import com.best.deskclock.timer.ExpiredTimersActivity;
import com.best.deskclock.timer.TimerService;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

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

    public Notification build(Context context, NotificationModel nm, Timer timer) {
        // Compute some values required below.
        final boolean running = timer.isRunning();

        final long base = getChronometerBase(timer);

        final List<Action> actions = new ArrayList<>(2);

        final CharSequence titleText;
        final CharSequence stateText;
        final CharSequence contentTitle;

        final int timerId = timer.getId();

        if (TextUtils.isEmpty(timer.getLabel())) {
            titleText = context.getString(R.string.timer_notification_label);
            contentTitle = context.getString(R.string.timer_notification_label);
        } else {
            titleText = timer.getLabel();
            contentTitle = timer.getLabel();
        }

        if (running) {
            stateText = null;
            // Left button: Pause
            final Intent pause = new Intent(context, TimerService.class)
                    .setAction(TimerService.ACTION_PAUSE_TIMER)
                    .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

            @DrawableRes final int icon1 = R.drawable.ic_fab_pause;
            final CharSequence title1 = context.getText(R.string.timer_pause);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, pause, timerId);
            actions.add(new Action.Builder(icon1, title1, intent1).build());

            // Right Button: +x Minutes
            final Intent addMinute = new Intent(context, TimerService.class)
                    .setAction(TimerService.ACTION_ADD_CUSTOM_TIME_TO_TIMER)
                    .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

            @DrawableRes final int icon2 = R.drawable.ic_add;
            int customTimeToAdd = Integer.parseInt(timer.getButtonTime());
            int minutesToAdd = customTimeToAdd / 60;
            int secondsToAdd = customTimeToAdd % 60;

            final CharSequence title2 = secondsToAdd == 0
                    ? context.getString(R.string.timer_add_custom_time_for_notification,
                        String.valueOf(minutesToAdd))
                    : context.getString(R.string.timer_add_custom_time_with_seconds_for_notification,
                        String.valueOf(minutesToAdd),
                        String.valueOf(secondsToAdd));

            final PendingIntent intent2 = Utils.pendingServiceIntent(context, addMinute, timerId);
            actions.add(new Action.Builder(icon2, title2, intent2).build());
        } else {
            // Timer is paused.
            stateText = context.getString(R.string.timer_paused);

            // Left button: Start
            final Intent start = new Intent(context, TimerService.class)
                    .setAction(TimerService.ACTION_START_TIMER)
                    .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

            @DrawableRes final int icon1 = R.drawable.ic_fab_play;
            final CharSequence title1 = context.getText(R.string.sw_resume_button);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, start, timerId);
            actions.add(new Action.Builder(icon1, title1, intent1).build());

            // Right Button: Reset
            final Intent reset = new Intent(context, TimerService.class)
                    .setAction(TimerService.ACTION_RESET_TIMER)
                    .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

            @DrawableRes final int icon2 = R.drawable.ic_reset;
            final CharSequence title2 = context.getText(R.string.reset);
            final PendingIntent intent2 = Utils.pendingServiceIntent(context, reset, timerId);
            actions.add(new Action.Builder(icon2, title2, intent2).build());
        }

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(TimerService.ACTION_SHOW_TIMER)
                .putExtra(TimerService.EXTRA_TIMER_ID, timerId)
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        final Builder notification = new Builder(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setContentTitle(contentTitle)
                .setContentText(timer.getTotalDuration())
                .setContentIntent(pendingShowApp)
                .setPriority(SdkUtils.isAtLeastAndroid7()
                        ? NotificationManager.IMPORTANCE_LOW
                        : Notification.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSmallIcon(R.drawable.ic_hourglass_bottom)
                .setSortKey(nm.getTimerNotificationSortKey())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setColor(context.getColor(R.color.md_theme_primary));

        for (Action action : actions) {
            notification.addAction(action);
        }

        if (SdkUtils.isAtLeastAndroid7()) {
            notification.setCustomContentView(buildChronometer(context.getPackageName(), base,
                            running, titleText, stateText)).setGroup(nm.getTimerNotificationGroupKey());
        } else {
            final CharSequence contentText = stateText != null
                    ? stateText
                    : TimerStringFormatter.formatTimeRemaining(context, timer.getRemainingTime(), false);

            notification.setContentTitle(titleText).setContentText(contentText);

            final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            final Intent updateNotification = TimerService.createUpdateNotificationIntent(context);
            final long remainingTime = timer.getRemainingTime();
            if (timer.isRunning() && remainingTime > MINUTE_IN_MILLIS) {
                // Schedule a callback to update the time-sensitive information of the running timer
                final PendingIntent pi = PendingIntent.getService(context, REQUEST_CODE_UPCOMING, updateNotification,
                                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT
                                        | PendingIntent.FLAG_IMMUTABLE);

                final long nextMinuteChange = remainingTime % MINUTE_IN_MILLIS;
                final long triggerTime = SystemClock.elapsedRealtime() + nextMinuteChange;
                TimerModel.schedulePendingIntent(am, triggerTime, pi);
            } else {
                // Cancel the update notification callback.
                final PendingIntent pi = PendingIntent.getService(context, 0, updateNotification,
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_NO_CREATE
                                | PendingIntent.FLAG_IMMUTABLE);
                if (pi != null) {
                    am.cancel(pi);
                    pi.cancel();
                }
            }
        }

        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.createChannel(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID);
        }
        return notification.build();
    }

    Notification buildHeadsUp(Context context, List<Timer> expired) {
        final Timer timer = expired.get(0);
        final int timerId = timer.getId();

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

            // Right Button: +x Minutes
            final Intent addTime = TimerService.createAddCustomTimeToTimerIntent(context, timerId);
            final PendingIntent intent2 = Utils.pendingServiceIntent(context, addTime, timerId);
            @DrawableRes final int icon2 = R.drawable.ic_add;
            int customTimeToAdd = Integer.parseInt(timer.getButtonTime());
            int minutesToAdd = customTimeToAdd / 60;
            int secondsToAdd = customTimeToAdd % 60;

            final CharSequence title2 = secondsToAdd == 0
                    ? context.getString(R.string.timer_add_custom_time_for_notification,
                        String.valueOf(minutesToAdd))
                    : context.getString(R.string.timer_add_custom_time_with_seconds_for_notification,
                        String.valueOf(minutesToAdd),
                        String.valueOf(secondsToAdd));

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
                .setPriority(SdkUtils.isAtLeastAndroid7()
                        ? NotificationManager.IMPORTANCE_HIGH
                        : Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setSmallIcon(R.drawable.ic_hourglass_bottom)
                .setFullScreenIntent(pendingFullScreen, true)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setColor(context.getColor(R.color.md_theme_primary));

        for (Action action : actions) {
            notification.addAction(action);
        }

        if (SdkUtils.isAtLeastAndroid7()) {
            notification.setCustomContentView(buildChronometer(context.getPackageName(), base,
                    true, titleText, stateText));
        } else {
            final CharSequence contentTextPreN = count == 1
                    ? context.getString(R.string.timer_times_up)
                    : null;

            notification.setContentTitle(titleText).setContentText(contentTextPreN);
        }

        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.createChannel(context, FIRING_NOTIFICATION_CHANNEL_ID);
        }

        // Stop and reset the timer if user clears notification.
        Intent dismissIntent = new Intent(context, TimerService.class);
            dismissIntent.setAction(TimerService.ACTION_RESET_EXPIRED_TIMERS);
            dismissIntent.putExtra(TimerService.EXTRA_TIMER_ID, timerId);
            PendingIntent deletePendingIntent = Utils.pendingServiceIntent(context, dismissIntent, timerId);
            notification.setDeleteIntent(deletePendingIntent);

        return notification.build();
    }

    Notification buildMissed(Context context, NotificationModel nm, List<Timer> missedTimers) {
        final Timer timer = missedTimers.get(0);
        final int count = missedTimers.size();
        final int timerId = timer.getId();

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
                    .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

            @DrawableRes final int icon1 = R.drawable.ic_reset;
            final CharSequence title1 = res.getText(R.string.reset);
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
                .putExtra(TimerService.EXTRA_TIMER_ID, timerId)
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        final Builder notification = new Builder(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setContentIntent(pendingShowApp)
                .setPriority(SdkUtils.isAtLeastAndroid7()
                        ? NotificationManager.IMPORTANCE_HIGH
                        : Notification.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSmallIcon(R.drawable.ic_hourglass_bottom)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSortKey(nm.getTimerNotificationMissedSortKey())
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .addAction(action)
                .setColor(context.getColor(R.color.md_theme_primary));

        if (SdkUtils.isAtLeastAndroid7()) {
            notification.setCustomContentView(buildChronometer(context.getPackageName(), base,
                    true, titleText, stateText)).setGroup(nm.getTimerNotificationGroupKey());
        } else {
            final CharSequence contentText = AlarmUtils.getFormattedTime(context, timer.getWallClockExpirationTime());
            notification.setContentTitle(titleText).setContentText(contentText);
        }

        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.createChannel(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID);
        }
        return notification.build();
    }

    public Notification buildSummaryNotification(Context context, NotificationModel nm) {
        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(TimerService.ACTION_SHOW_TIMER)
                .putExtra(TimerService.EXTRA_TIMER_ID, -1)
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.createChannel(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID);
        }

        return new NotificationCompat.Builder(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_hourglass_bottom)
                .setGroup(nm.getTimerNotificationGroupKey())
                .setGroupSummary(true)
                .setOngoing(true)
                .setContentIntent(pendingShowApp)
                .setPriority(SdkUtils.isAtLeastAndroid7()
                        ? NotificationManager.IMPORTANCE_LOW
                        : Notification.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true)
                .setColor(context.getColor(R.color.md_theme_primary))
                .build();
    }

    private RemoteViews buildChronometer(String packageName, long base, boolean running, CharSequence titleText,
                                         CharSequence stateText) {

        final RemoteViews content = new RemoteViews(packageName, R.layout.chronometer_notif_content);
        if (SdkUtils.isAtLeastAndroid7()) {
            content.setChronometerCountDown(R.id.chronometer, true);
        }
        content.setChronometer(R.id.chronometer, base, null, running);
        content.setTextViewText(R.id.title, titleText);
        content.setTextViewText(R.id.state, stateText);
        return content;
    }
}
