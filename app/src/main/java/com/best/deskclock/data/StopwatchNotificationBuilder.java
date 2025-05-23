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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import androidx.core.app.NotificationCompat.Builder;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.events.Events;
import com.best.deskclock.stopwatch.StopwatchService;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds notification to reflect the latest state of the stopwatch and recorded laps.
 */
class StopwatchNotificationBuilder {

    public Notification build(Context context, NotificationModel nm, Stopwatch stopwatch) {
        @StringRes final int eventLabel = R.string.label_notification;

        // Intent to load the app when the notification is tapped.
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(StopwatchService.ACTION_SHOW_STOPWATCH)
                .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        // Compute some values required below.
        final boolean running = stopwatch.isRunning();
        final long base = SystemClock.elapsedRealtime() - stopwatch.getTotalTime();

        final RemoteViews content = new RemoteViews(context.getPackageName(), R.layout.chronometer_notif_content);
        content.setTextViewText(R.id.title, context.getString(R.string.stopwatch_channel));
        content.setChronometer(R.id.chronometer, base, null, running);

        final List<Action> actions = new ArrayList<>(2);

        if (running) {
            // Left button: Pause
            final Intent pause = new Intent(context, StopwatchService.class)
                    .setAction(StopwatchService.ACTION_PAUSE_STOPWATCH)
                    .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

            @DrawableRes final int icon1 = R.drawable.ic_fab_pause;
            final CharSequence title1 = context.getText(R.string.sw_pause_button);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, pause);
            actions.add(new Action.Builder(icon1, title1, intent1).build());

            // Right button: Add Lap
            if (DataModel.getDataModel().canAddMoreLaps()) {
                final Intent lap = new Intent(context, StopwatchService.class)
                        .setAction(StopwatchService.ACTION_LAP_STOPWATCH)
                        .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

                @DrawableRes final int icon2 = R.drawable.ic_sw_lap;
                final CharSequence title2 = context.getText(R.string.sw_lap_button);
                final PendingIntent intent2 = Utils.pendingServiceIntent(context, lap);
                actions.add(new Action.Builder(icon2, title2, intent2).build());
            }

            // Show the current lap number if any laps have been recorded.
            final int lapCount = DataModel.getDataModel().getLaps().size();
            if (lapCount > 0) {
                final int lapNumber = lapCount + 1;
                final String lap = context.getString(R.string.sw_notification_lap_number, lapNumber);
                content.setTextViewText(R.id.state, lap);
                content.setViewVisibility(R.id.state, VISIBLE);
            } else {
                content.setViewVisibility(R.id.state, GONE);
            }
        } else {
            // Left button: Start
            final Intent start = new Intent(context, StopwatchService.class)
                    .setAction(StopwatchService.ACTION_START_STOPWATCH)
                    .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

            @DrawableRes final int icon1 = R.drawable.ic_fab_play;
            final CharSequence title1 = context.getText(R.string.sw_start_button);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, start);
            actions.add(new Action.Builder(icon1, title1, intent1).build());

            // Right button: Reset (dismisses notification and resets stopwatch)
            final Intent reset = new Intent(context, StopwatchService.class)
                    .setAction(StopwatchService.ACTION_RESET_STOPWATCH)
                    .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

            @DrawableRes final int icon2 = R.drawable.ic_reset;
            final CharSequence title2 = context.getText(R.string.reset);
            final PendingIntent intent2 = Utils.pendingServiceIntent(context, reset);
            actions.add(new Action.Builder(icon2, title2, intent2).build());

            // Indicate the stopwatch is paused.
            content.setTextViewText(R.id.state, context.getString(R.string.swn_paused));
            content.setViewVisibility(R.id.state, VISIBLE);
        }

        final Builder notification = new Builder(context, STOPWATCH_NOTIFICATION_CHANNEL_ID)
                .setLocalOnly(true)
                .setOngoing(running)
                .setCustomContentView(content)
                .setContentIntent(pendingShowApp)
                .setAutoCancel(stopwatch.isPaused())
                .setPriority(SdkUtils.isAtLeastAndroid7()
                        ? NotificationManager.IMPORTANCE_LOW
                        : Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_tab_stopwatch_static)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setColor(context.getColor(R.color.md_theme_primary))
                .setGroup(nm.getStopwatchNotificationGroupKey());

        for (Action action : actions) {
            notification.addAction(action);
        }

        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.createChannel(context, STOPWATCH_NOTIFICATION_CHANNEL_ID);
        }
        return notification.build();
    }
}
