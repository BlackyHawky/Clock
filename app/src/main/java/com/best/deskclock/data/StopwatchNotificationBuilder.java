/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.best.deskclock.utils.NotificationUtils.STOPWATCH_NOTIFICATION_CHANNEL_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.events.Events;
import com.best.deskclock.stopwatch.StopwatchService;
import com.best.deskclock.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds notification to reflect the latest state of the stopwatch and recorded laps.
 */
class StopwatchNotificationBuilder {

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public Notification buildLiveUpdateNotification(@NonNull Context context, @NonNull Stopwatch stopwatch, @NonNull String languageCode) {

        @StringRes final int eventLabel = R.string.label_notification;
        final Context localizedContext = Utils.getLocalizedContext(context, languageCode);
        final boolean running = stopwatch.isRunning();
        final long base = SystemClock.elapsedRealtime() - stopwatch.getTotalTime();

        // Intent to load the app when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
            .setAction(StopwatchService.ACTION_SHOW_STOPWATCH)
            .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, STOPWATCH_NOTIFICATION_CHANNEL_ID)
            .setLocalOnly(true)
            .setShowWhen(false)
            .setOngoing(true)
            .setContentTitle(localizedContext.getString(R.string.stopwatch_channel))
            .setContentIntent(pendingShowApp)
            .setAutoCancel(stopwatch.isPaused())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(ContextCompat.getColor(context, R.color.notificationColor))
            .setRequestPromotedOngoing(true);

        for (NotificationCompat.Action action : buildNotificationActions(context, localizedContext, stopwatch, eventLabel)) {
            builder.addAction(action);
        }

        builder.addExtras(buildSharedExtras(context, stopwatch, base, running));

        if (running) {
            long offset = System.currentTimeMillis() - SystemClock.elapsedRealtime();

            builder.setWhen(base + offset);
            builder.setUsesChronometer(true);
            builder.setChronometerCountDown(false);
            builder.setSmallIcon(R.drawable.ic_tab_stopwatch_static);

            final int lapCount = DataModel.getDataModel().getLaps().size();

            if (lapCount > 0) {
                final int lapNumber = lapCount + 1;
                builder.setContentText(localizedContext.getString(R.string.sw_notification_lap_number, lapNumber));
            }
        } else {
            builder.setUsesChronometer(false);
            String pausedTimeString = DateUtils.formatElapsedTime(stopwatch.getTotalTime() / 1000);
            String stateText = localizedContext.getString(R.string.swn_paused);

            builder.setContentText(stateText + " - " + pausedTimeString);
            builder.setShortCriticalText(pausedTimeString);
            builder.setSmallIcon(R.drawable.ic_fab_pause);
        }

        return builder.build();
    }

    public Notification build(@NonNull Context context, @NonNull Stopwatch stopwatch, @NonNull String languageCode) {

        @StringRes final int eventLabel = R.string.label_notification;
        final Context localizedContext = Utils.getLocalizedContext(context, languageCode);
        final boolean running = stopwatch.isRunning();
        final long base = SystemClock.elapsedRealtime() - stopwatch.getTotalTime();
        final String fallbackText = running ? "" : localizedContext.getString(R.string.swn_paused);

        // Intent to load the app when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
            .setAction(StopwatchService.ACTION_SHOW_STOPWATCH)
            .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        final RemoteViews content = new RemoteViews(context.getPackageName(), R.layout.chronometer_notif_content);
        content.setTextViewText(R.id.title, localizedContext.getString(R.string.stopwatch_channel));

        if (running) {
            content.setChronometer(R.id.chronometer, base, null, true);
            final int lapCount = DataModel.getDataModel().getLaps().size();

            if (lapCount > 0) {
                final int lapNumber = lapCount + 1;
                content.setTextViewText(R.id.state, localizedContext.getString(R.string.sw_notification_lap_number, lapNumber));
                content.setViewVisibility(R.id.state, VISIBLE);
            } else {
                content.setViewVisibility(R.id.state, GONE);
            }
        } else {
            String pausedTimeString = DateUtils.formatElapsedTime(stopwatch.getTotalTime() / 1000);
            content.setTextViewText(R.id.chronometer, pausedTimeString);
            content.setTextViewText(R.id.state, localizedContext.getString(R.string.swn_paused));
            content.setViewVisibility(R.id.state, VISIBLE);
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, STOPWATCH_NOTIFICATION_CHANNEL_ID)
            .setLocalOnly(true)
            .setShowWhen(false)
            .setOngoing(true)
            .setContentTitle(localizedContext.getString(R.string.stopwatch_channel))
            .setContentText(fallbackText)
            .setCustomContentView(content)
            .setContentIntent(pendingShowApp)
            .setAutoCancel(stopwatch.isPaused())
            .setDefaults(0) // No sound on Android 7 and earlier versions
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_tab_stopwatch_static)
            .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
            .setColor(ContextCompat.getColor(context, R.color.notificationColor));

        if (running) {
            long offset = System.currentTimeMillis() - SystemClock.elapsedRealtime();

            builder.setWhen(base + offset);
        }

        for (NotificationCompat.Action action : buildNotificationActions(context, localizedContext, stopwatch, eventLabel)) {
            builder.addAction(action);
        }

        builder.addExtras(buildSharedExtras(context, stopwatch, base, running));

        return builder.build();
    }

    /**
     * Builds the list of actions (buttons) for the stopwatch notification.
     */
    private List<NotificationCompat.Action> buildNotificationActions(@NonNull Context context, @NonNull Context localizedContext,
                                                                     @NonNull Stopwatch stopwatch, @StringRes int eventLabel) {

        final List<NotificationCompat.Action> actions = new ArrayList<>(2);
        final boolean running = stopwatch.isRunning();

        if (running) {
            // Left button: Pause
            final Intent pause = new Intent(context, StopwatchService.class)
                .setAction(StopwatchService.ACTION_PAUSE_STOPWATCH)
                .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

            final PendingIntent intent1 = Utils.pendingServiceIntent(context, pause);
            actions.add(new NotificationCompat.Action.Builder(
                R.drawable.ic_fab_pause, localizedContext.getText(R.string.sw_pause_button), intent1).build());

            // Right button: Add Lap
            if (DataModel.getDataModel().canAddMoreLaps()) {
                final Intent lap = new Intent(context, StopwatchService.class)
                    .setAction(StopwatchService.ACTION_LAP_STOPWATCH)
                    .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

                final PendingIntent intent2 = Utils.pendingServiceIntent(context, lap);
                actions.add(new NotificationCompat.Action.Builder(
                    R.drawable.ic_stopwatch_lap, localizedContext.getText(R.string.sw_lap_button), intent2).build());
            }
        } else {
            // Left button: Start
            final Intent start = new Intent(context, StopwatchService.class)
                .setAction(StopwatchService.ACTION_START_STOPWATCH)
                .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

            final PendingIntent intent1 = Utils.pendingServiceIntent(context, start);
            actions.add(new NotificationCompat.Action.Builder(
                R.drawable.ic_fab_play, localizedContext.getText(R.string.sw_start_button), intent1).build());

            // Right button: Reset
            final Intent reset = new Intent(context, StopwatchService.class)
                .setAction(StopwatchService.ACTION_RESET_STOPWATCH)
                .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

            final PendingIntent intent2 = Utils.pendingServiceIntent(context, reset);
            actions.add(new NotificationCompat.Action.Builder(
                R.drawable.ic_reset, localizedContext.getText(R.string.reset), intent2).build());
        }

        return actions;
    }

    /**
     * Creates the shared extras bundle for third-party apps to display stopwatch.
     */
    private Bundle buildSharedExtras(@NonNull Context context, @NonNull Stopwatch stopwatch, long base, boolean running) {
        Bundle extras = new Bundle();
        if (running) {
            extras.putLong("android.chronometerBase", base);
            extras.putBoolean("android.chronometerCountDown", false);
            extras.putBoolean(Notification.EXTRA_SHOW_CHRONOMETER, true);
        } else {
            extras.putBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false);
        }

        extras.putBoolean(context.getPackageName() + ".stopwatchIsRunning", running);
        extras.putLong(context.getPackageName() + ".stopwatchAccumulatedMs", stopwatch.getTotalTime());

        return extras;
    }

}
