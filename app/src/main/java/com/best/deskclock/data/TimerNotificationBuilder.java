/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMER_TIME_BUTTON_VALUE_ZERO;
import static com.best.deskclock.utils.NotificationUtils.FIRING_NOTIFICATION_CHANNEL_ID;
import static com.best.deskclock.utils.NotificationUtils.TIMER_MISSED_NOTIFICATION_CHANNEL_ID;
import static com.best.deskclock.utils.NotificationUtils.TIMER_MODEL_NOTIFICATION_CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.events.Events;
import com.best.deskclock.timer.ExpiredTimersActivity;
import com.best.deskclock.timer.TimerService;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds notifications to reflect the latest state of the timers.
 */
class TimerNotificationBuilder {

    private static final String URI_SCHEME_TIMER_START = "timer:start:";
    private static final String URI_SCHEME_TIMER_PAUSE = "timer:pause:";
    private static final String URI_SCHEME_TIMER_RESET = "timer:reset:";
    private static final String URI_SCHEME_TIMER_SHOW = "timer:show:";
    private static final String URI_SCHEME_TIMER_ADD = "timer:add:";
    private static final String URI_SCHEME_TIMER_REMOVE = "timer:remove:";

    private static final int REQUEST_CODE_UPCOMING = 0;

    /**
     * @param timer the timer on which to base the chronometer display
     * @return the time at which the chronometer will/did reach 0:00 in realtime
     */
    private static long getChronometerBase(@NonNull Timer timer) {
        // The in-app timer display rounds *up* to the next second for positive timer values. Mirror
        // that behavior in the notification's Chronometer by padding in an extra second as needed.
        final long remaining = timer.getRemainingTime();

        // If the timer is paused, align the notification's Chronometer with the in-app
        // display's rounded-up value. This prevents render delay desynchronization.
        if (timer.isPaused() && remaining > 0) {
            long displaySeconds = (long) Math.ceil(remaining / 1000.0);

            // Add a 500ms buffer to absorb system rendering delays, ensuring the
            // Chronometer (which truncates downwards) displays the correct rounded second.
            return SystemClock.elapsedRealtime() + (displaySeconds * 1000) + 500;
        }

        // The in-app timer display rounds *up* to the next second for positive timer values. Mirror
        // that behavior in the notification's Chronometer by padding in an extra second as needed.
        final long adjustedRemaining = remaining < 0 ? remaining : remaining + SECOND_IN_MILLIS;

        // Chronometer will/did reach 0:00 adjustedRemaining milliseconds from now.
        return SystemClock.elapsedRealtime() + adjustedRemaining;
    }

    /**
     * @return the live update notification for running timers.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public Notification buildLiveUpdateNotification(@NonNull Context context, @NonNull NotificationModel nm, @NonNull Timer timer,
                                                    @NonNull String languageCode, boolean isPrimary) {

        final Context localizedContext = Utils.getLocalizedContext(context, languageCode);
        final long base = getChronometerBase(timer);
        final boolean running = timer.isRunning();

        final CharSequence contentTitle = TextUtils.isEmpty(timer.getLabel())
            ? localizedContext.getString(R.string.timer_notification_label)
            : timer.getLabel();

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
            .setAction(TimerService.ACTION_SHOW_TIMER)
            .setData(Uri.parse(URI_SCHEME_TIMER_SHOW + timer.getId()))
            .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId())
            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setLocalOnly(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setContentTitle(contentTitle)
            .setContentIntent(pendingShowApp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setSortKey(nm.getTimerNotificationSortKey())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(ContextCompat.getColor(context, R.color.notificationColor))
            .setGroup(nm.getTimerNotificationGroupKey());

        if (isPrimary) {
            builder.setRequestPromotedOngoing(true);
        }

        for (NotificationCompat.Action action : buildNotificationActions(context, localizedContext, timer)) {
            builder.addAction(action);
        }

        builder.addExtras(buildSharedExtras(context, timer, base, running));

        if (running) {
            long offset = System.currentTimeMillis() - SystemClock.elapsedRealtime();

            builder.setWhen(base + offset);
            builder.setUsesChronometer(true);
            builder.setChronometerCountDown(true);
            builder.setSmallIcon(R.drawable.ic_tab_timer_static);
        } else {
            long remainingMillis = base - SystemClock.elapsedRealtime();
            long remainingSeconds = remainingMillis / 1000;
            String pausedTimeString = DateUtils.formatElapsedTime(remainingSeconds);
            String stateText = localizedContext.getString(R.string.timer_paused);

            builder.setUsesChronometer(false);
            builder.setContentText(stateText + " - " + pausedTimeString);
            builder.setShortCriticalText(pausedTimeString);
            builder.setSmallIcon(R.drawable.ic_fab_pause);
        }

        long totalLength = timer.getTotalLength();
        if (totalLength > 0) {
            long elapsedTime = totalLength - timer.getRemainingTime();

            int progressPercent = (int) ((elapsedTime * 100) / totalLength);
            progressPercent = Math.max(0, Math.min(100, progressPercent));

            NotificationCompat.ProgressStyle progressStyle = new NotificationCompat.ProgressStyle()
                .setProgress(progressPercent)
                .setProgressPoints(Arrays.asList(
                    new NotificationCompat.ProgressStyle.Point(25),
                    new NotificationCompat.ProgressStyle.Point(50),
                    new NotificationCompat.ProgressStyle.Point(75)
                ));

            builder.setStyle(progressStyle);
        }

        return builder.build();
    }

    /**
     * @return the notification for running timers.
     */
    public Notification build(@NonNull Context context, @NonNull NotificationModel nm, @NonNull Timer timer, @NonNull String languageCode) {
        final Context localizedContext = Utils.getLocalizedContext(context, languageCode);
        final boolean running = timer.isRunning();
        final long base = getChronometerBase(timer);
        final CharSequence titleText;
        final CharSequence contentText;

        final CharSequence stateText = running ? null : localizedContext.getString(R.string.timer_paused);
        final CharSequence contentTitle = localizedContext.getString(R.string.timer_notification_label);

        if (TextUtils.isEmpty(timer.getLabel())) {
            titleText = localizedContext.getString(R.string.timer_notification_label);
            contentText = timer.getTotalDuration();
        } else {
            titleText = timer.getLabel();
            contentText = timer.getLabel() + " - " + timer.getTotalDuration();
        }

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
            .setAction(TimerService.ACTION_SHOW_TIMER)
            .setData(Uri.parse(URI_SCHEME_TIMER_SHOW + timer.getId()))
            .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId())
            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setLocalOnly(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(pendingShowApp)
            .setDefaults(0) // No sound on Android 7 and earlier versions
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setSmallIcon(R.drawable.ic_tab_timer_static)
            .setSortKey(nm.getTimerNotificationSortKey())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
            .setColor(ContextCompat.getColor(context, R.color.notificationColor))
            .setGroup(nm.getTimerNotificationGroupKey());

        if (running) {
            long offset = System.currentTimeMillis() - SystemClock.elapsedRealtime();

            builder.setWhen(base + offset);
        }

        for (NotificationCompat.Action action : buildNotificationActions(context, localizedContext, timer)) {
            builder.addAction(action);
        }

        if (SdkUtils.isAtLeastAndroid7()) {
            builder.setCustomContentView(buildChronometer(context, base, running, titleText, stateText));
        } else {
            final CharSequence preNText = stateText != null
                ? stateText
                : TimerStringFormatter.formatTimeRemaining(context, timer.getRemainingTime(), false);

            builder.setContentTitle(titleText).setContentText(preNText);

            final AlarmManager am = context.getApplicationContext().getSystemService(AlarmManager.class);
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

        builder.addExtras(buildSharedExtras(context, timer, base, running));

        return builder.build();
    }

    /**
     * @return the notification for expired timers.
     */
    Notification buildHeadsUp(@NonNull Context context, @NonNull List<Timer> expired, @NonNull String languageCode,
                              @NonNull NotificationModel nm, boolean isSingleTimerMode) {

        final Context localizedContext = Utils.getLocalizedContext(context, languageCode);
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
        final List<NotificationCompat.Action> actions = new ArrayList<>(2);

        if (count == 1) {
            if (TextUtils.isEmpty(label)) {
                titleText = localizedContext.getString(R.string.timer_notification_label) + " - " + timer.getTotalDuration();
            } else {
                titleText = timer.getLabel();
            }

            stateText = localizedContext.getString(R.string.timer_times_up);

            // Left button: Reset single timer
            final CharSequence title1 = localizedContext.getString(isSingleTimerMode || timer.getDeleteAfterUse()
                ? R.string.delete
                : R.string.timer_stop);
            actions.add(new NotificationCompat.Action.Builder(icon1, title1, intent1).build());

            // Right Button: +x Minutes
            if (!TIMER_TIME_BUTTON_VALUE_ZERO.equals(timer.getButtonAddTime())) {
                final Intent addTime = TimerService.createAddCustomTimeToTimerIntent(context, timerId);
                final PendingIntent intent2 = Utils.pendingServiceIntent(context, addTime, timerId);
                @DrawableRes final int icon2 = R.drawable.ic_add;
                final CharSequence title2 = formatButtonTimeText(localizedContext, timer, true);

                actions.add(new NotificationCompat.Action.Builder(icon2, title2, intent2).build());
            }
        } else {
            titleText = localizedContext.getString(R.string.timer_multi_times_up, count);
            stateText = null;

            // Left button: Reset all timers
            final CharSequence title1 = localizedContext.getString(R.string.timer_stop_all);
            actions.add(new NotificationCompat.Action.Builder(icon1, title1, intent1).build());
        }

        final long base = getChronometerBase(timer);

        // Content intent shows the timer full screen when clicked.
        final Intent content = new Intent(context, ExpiredTimersActivity.class);
        final PendingIntent contentIntent = Utils.pendingActivityIntent(context, content);

        // Full screen intent has flags so it is different from the content intent.
        final Intent fullScreen = new Intent(context, ExpiredTimersActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        final PendingIntent pendingFullScreen = Utils.pendingActivityIntent(context, fullScreen);

        @SuppressLint("FullScreenIntentPolicy")
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FIRING_NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setLocalOnly(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setContentTitle(titleText)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_tab_timer_static)
            .setFullScreenIntent(pendingFullScreen, true)
            .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
            .setColor(ContextCompat.getColor(context, R.color.notificationColor))
            .setGroup(nm.getExpiredTimerNotificationGroupKey())
            .setSortKey(nm.getExpiredTimerNotificationSortKey());

        for (NotificationCompat.Action action : actions) {
            builder.addAction(action);
        }

        if (SdkUtils.isAtLeastAndroid7()) {
            builder.setCustomContentView(buildChronometer(context, base, true, titleText, stateText));
        } else {
            final CharSequence contentTextPreN = count == 1
                ? localizedContext.getString(R.string.timer_times_up)
                : null;

            builder.setContentTitle(titleText).setContentText(contentTextPreN);
        }

        // Stop and reset the timer if user clears notification.
        builder.setDeleteIntent(resetTimerIntent(context, timerId, false));

        Bundle extras = buildSharedExtras(context, timer, base, true);
        extras.putBoolean(context.getPackageName() + ".timerIsExpired", true);
        builder.addExtras(extras);

        return builder.build();
    }

    /**
     * @return the notification for missed timers.
     */
    Notification buildMissed(@NonNull Context context, @NonNull NotificationModel nm, @NonNull Timer timer, @NonNull String languageCode,
                             boolean isSingleTimerMode) {

        final Context localizedContext = Utils.getLocalizedContext(context, languageCode);
        final int timerId = timer.getId();
        final long base = getChronometerBase(timer);
        final NotificationCompat.Action action;
        final CharSequence contentTitle;
        String label = timer.getLabel();
        final CharSequence contentText;

        contentTitle = localizedContext.getString(R.string.timer_notification_label);

        if (TextUtils.isEmpty(label)) {
            label = timer.getTotalDuration();
        }

        contentText = localizedContext.getString(R.string.missed_named_timer_notification_label, label);

        // Reset button
        final Intent reset = new Intent(context, TimerService.class)
            .setAction(TimerService.ACTION_RESET_TIMER)
            .setData(Uri.parse(URI_SCHEME_TIMER_RESET + timerId))
            .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
            .setAction(TimerService.ACTION_SHOW_TIMER)
            .setData(Uri.parse(URI_SCHEME_TIMER_SHOW + timerId))
            .putExtra(TimerService.EXTRA_TIMER_ID, timerId)
            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        @DrawableRes final int icon = R.drawable.ic_reset;
        final CharSequence title = localizedContext.getText(isSingleTimerMode || timer.getDeleteAfterUse()
            ? R.string.delete
            : R.string.reset);
        final PendingIntent intent = Utils.pendingServiceIntent(context, reset);
        action = new NotificationCompat.Action.Builder(icon, title, intent).build();

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TIMER_MISSED_NOTIFICATION_CHANNEL_ID)
            .setLocalOnly(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(pendingShowApp)
            .setDefaults(0) // No sound on Android 7 and earlier versions
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSmallIcon(R.drawable.ic_tab_timer_static)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSortKey(nm.getTimerNotificationMissedSortKey())
            .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
            .addAction(action)
            .setColor(ContextCompat.getColor(context, R.color.notificationColor))
            .setGroup(nm.getTimerNotificationGroupKey());

        if (SdkUtils.isAtLeastAndroid7()) {
            builder.setCustomContentView(buildChronometer(context, base, true, contentTitle, contentText));
        } else {
            final CharSequence preNText = AlarmUtils.getFormattedTime(localizedContext, timer.getWallClockExpirationTime());
            builder.setContentTitle(contentTitle).setContentText(preNText);
        }

        // Reset the timer if user clears notification.
        builder.setDeleteIntent(resetTimerIntent(context, timerId, true));

        Bundle extras = buildSharedExtras(context, timer, base, true);
        extras.putBoolean(context.getPackageName() + ".timerIsMissed", true);
        builder.addExtras(extras);

        return builder.build();
    }

    public Notification buildSummaryNotification(@NonNull Context context, @NonNull NotificationModel nm) {
        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
            .setAction(TimerService.ACTION_SHOW_TIMER)
            .setData(Uri.parse(URI_SCHEME_TIMER_SHOW + "-1"))
            .putExtra(TimerService.EXTRA_TIMER_ID, -1)
            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        return new NotificationCompat.Builder(context, TIMER_MODEL_NOTIFICATION_CHANNEL_ID)
            .setShowWhen(true)
            .setSmallIcon(R.drawable.ic_tab_timer_static)
            .setGroup(nm.getTimerNotificationGroupKey())
            .setGroupSummary(true)
            .setOngoing(true)
            .setContentIntent(pendingShowApp)
            .setDefaults(0) // No sound on Android 7 and earlier versions
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLocalOnly(true)
            .setColor(ContextCompat.getColor(context, R.color.notificationColor))
            .build();
    }

    private PendingIntent resetTimerIntent(@NonNull Context context, int timerId, boolean isMissedTimer) {
        Intent dismissIntent = new Intent(context, TimerService.class)
            .setAction(isMissedTimer ? TimerService.ACTION_RESET_MISSED_TIMERS : TimerService.ACTION_RESET_EXPIRED_TIMERS)
            .setData(Uri.parse(URI_SCHEME_TIMER_RESET + timerId))
            .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

        return Utils.pendingServiceIntent(context, dismissIntent, timerId);
    }

    /**
     * Builds the list of actions (buttons) for the timer notification.
     */
    private List<NotificationCompat.Action> buildNotificationActions(@NonNull Context context, @NonNull Context localizedContext,
                                                                     @NonNull Timer timer) {

        final List<NotificationCompat.Action> actions = new ArrayList<>(2);
        final boolean running = timer.isRunning();
        final int timerId = timer.getId();

        if (running) {
            // Left button: Pause
            final Intent pause = new Intent(context, TimerService.class)
                .setAction(TimerService.ACTION_PAUSE_TIMER)
                .setData(Uri.parse(URI_SCHEME_TIMER_PAUSE + timerId))
                .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

            final PendingIntent intent1 = Utils.pendingServiceIntent(context, pause, timerId);
            actions.add(new NotificationCompat.Action.Builder(
                R.drawable.ic_fab_pause, localizedContext.getText(R.string.timer_pause), intent1).build());

            // Center Button: -x Minutes
            if (!TIMER_TIME_BUTTON_VALUE_ZERO.equals(timer.getButtonRemoveTime())) {
                final Intent removeMinute = new Intent(context, TimerService.class)
                    .setAction(TimerService.ACTION_REMOVE_CUSTOM_TIME_TO_TIMER)
                    .setData(Uri.parse(URI_SCHEME_TIMER_REMOVE + timerId))
                    .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

                final PendingIntent intent2 = Utils.pendingServiceIntent(context, removeMinute, timerId);
                actions.add(new NotificationCompat.Action.Builder(
                    R.drawable.ic_minus, formatButtonTimeText(localizedContext, timer, false), intent2).build());
            }

            // Right Button: +x Minutes
            if (!TIMER_TIME_BUTTON_VALUE_ZERO.equals(timer.getButtonAddTime())) {
                final Intent addMinute = new Intent(context, TimerService.class)
                    .setAction(TimerService.ACTION_ADD_CUSTOM_TIME_TO_TIMER)
                    .setData(Uri.parse(URI_SCHEME_TIMER_ADD + timerId))
                    .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

                final PendingIntent intent3 = Utils.pendingServiceIntent(context, addMinute, timerId);
                actions.add(new NotificationCompat.Action.Builder(
                    R.drawable.ic_add, formatButtonTimeText(localizedContext, timer, true), intent3).build());
            }
        } else {
            // Left button: Start
            final Intent start = new Intent(context, TimerService.class)
                .setAction(TimerService.ACTION_START_TIMER)
                .setData(Uri.parse(URI_SCHEME_TIMER_START + timerId))
                .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

            final PendingIntent intent1 = Utils.pendingServiceIntent(context, start, timerId);
            actions.add(new NotificationCompat.Action.Builder(
                R.drawable.ic_fab_play, localizedContext.getText(R.string.sw_resume_button), intent1).build());

            // Right Button: Reset
            final Intent reset = new Intent(context, TimerService.class)
                .setAction(TimerService.ACTION_RESET_TIMER)
                .setData(Uri.parse(URI_SCHEME_TIMER_RESET + timerId))
                .putExtra(TimerService.EXTRA_TIMER_ID, timerId);

            final PendingIntent intent = Utils.pendingServiceIntent(context, reset, timerId);
            actions.add(new NotificationCompat.Action.Builder(
                R.drawable.ic_reset, localizedContext.getText(R.string.reset), intent).build());
        }

        return actions;
    }

    /**
     * Creates the shared extras bundle for third-party apps to display active timers.
     */
    private Bundle buildSharedExtras(@NonNull Context context, @NonNull Timer timer, long base, boolean running) {
        Bundle extras = new Bundle();

        if (running) {
            extras.putLong("android.chronometerBase", base);
            extras.putBoolean("android.chronometerCountDown", true);
            extras.putBoolean(Notification.EXTRA_SHOW_CHRONOMETER, true);
        } else {
            extras.putBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false);
        }

        extras.putBoolean(context.getPackageName() + ".timerIsRunning", running);
        extras.putLong(context.getPackageName() + ".timerRemainingMs", timer.getRemainingTime());

        return extras;
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.N)
    private RemoteViews buildChronometer(@NonNull Context context, long base, boolean running, @NonNull CharSequence titleText,
                                         @Nullable CharSequence stateText) {

        final RemoteViews content = new RemoteViews(context.getPackageName(), R.layout.chronometer_notif_content);

        if (running) {
            content.setChronometerCountDown(R.id.chronometer, true);
            content.setChronometer(R.id.chronometer, base, null, true);
        } else {
            long remainingMillis = base - SystemClock.elapsedRealtime();
            long remainingSeconds = remainingMillis / 1000;
            String pausedTimeString = DateUtils.formatElapsedTime(remainingSeconds);

            if (remainingMillis < 0) {
                pausedTimeString = "− " + DateUtils.formatElapsedTime(Math.abs(remainingSeconds));
            }

            content.setTextViewText(R.id.chronometer, pausedTimeString);
        }

        content.setTextViewText(R.id.title, titleText);
        content.setTextViewText(R.id.state, stateText);
        return content;
    }

    @NonNull
    private static String formatButtonTimeText(@NonNull Context context, @NonNull Timer timer, boolean isAddButton) {
        int customTime = Integer.parseInt(isAddButton ? timer.getButtonAddTime() : timer.getButtonRemoveTime());
        int minutes = customTime / 60;
        int seconds = customTime % 60;
        final String minText = FormattedTextUtils.getNumberFormattedQuantityString(context, R.plurals.minutes_short, minutes);
        final String prefix = isAddButton ? "+ " : "- ";

        if (seconds == 0) {
            return prefix + minText;
        }

        final String minSecText = minText + " " + seconds;

        return prefix + minSecText;
    }

}
