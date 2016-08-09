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

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemClock;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;

import com.android.deskclock.HandleDeskClockApiCalls;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.stopwatch.StopwatchService;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * Builds KK, L, or M-style notifications to reflect the latest state of the stopwatch and
 * recorded laps.
 */
class StopwatchNotificationBuilderPreN implements StopwatchModel.NotificationBuilder {

    @Override
    public Notification build(Context context, NotificationModel nm, Stopwatch stopwatch) {
        @StringRes final int eventLabel = R.string.label_notification;

        // Intent to load the app when the notification is tapped.
        final Intent showApp = new Intent(context, HandleDeskClockApiCalls.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(HandleDeskClockApiCalls.ACTION_SHOW_STOPWATCH)
                .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = PendingIntent.getActivity(context, 0, showApp,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        // Compute some values required below.
        final boolean running = stopwatch.isRunning();
        final String pname = context.getPackageName();
        final Resources res = context.getResources();
        final long base = SystemClock.elapsedRealtime() - stopwatch.getTotalTime();

        final RemoteViews collapsed = new RemoteViews(pname, R.layout.stopwatch_notif_collapsed);
        collapsed.setChronometer(R.id.swn_collapsed_chronometer, base, null, running);
        collapsed.setOnClickPendingIntent(R.id.swn_collapsed_hitspace, pendingShowApp);
        collapsed.setImageViewResource(R.id.notification_icon, R.drawable.stat_notify_stopwatch);

        final RemoteViews expanded = new RemoteViews(pname, R.layout.stopwatch_notif_expanded);
        expanded.setChronometer(R.id.swn_expanded_chronometer, base, null, running);
        expanded.setOnClickPendingIntent(R.id.swn_expanded_hitspace, pendingShowApp);
        expanded.setImageViewResource(R.id.notification_icon, R.drawable.stat_notify_stopwatch);

        @IdRes final int leftButtonId = R.id.swn_left_button;
        @IdRes final int rightButtonId = R.id.swn_right_button;
        if (running) {
            // Left button: Pause
            expanded.setTextViewText(leftButtonId, res.getText(R.string.sw_pause_button));
            setTextViewDrawable(expanded, leftButtonId, R.drawable.ic_pause_24dp);
            final Intent pause = new Intent(context, StopwatchService.class)
                    .setAction(HandleDeskClockApiCalls.ACTION_PAUSE_STOPWATCH)
                    .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);
            final PendingIntent pendingPause = Utils.pendingServiceIntent(context, pause);
            expanded.setOnClickPendingIntent(leftButtonId, pendingPause);

            // Right button: Add Lap
            if (DataModel.getDataModel().canAddMoreLaps()) {
                expanded.setTextViewText(rightButtonId, res.getText(R.string.sw_lap_button));
                setTextViewDrawable(expanded, rightButtonId, R.drawable.ic_sw_lap_24dp);

                final Intent lap = new Intent(context, StopwatchService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_LAP_STOPWATCH)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);
                final PendingIntent pendingLap = Utils.pendingServiceIntent(context, lap);
                expanded.setOnClickPendingIntent(rightButtonId, pendingLap);
                expanded.setViewVisibility(rightButtonId, VISIBLE);
            } else {
                expanded.setViewVisibility(rightButtonId, INVISIBLE);
            }

            // Show the current lap number if any laps have been recorded.
            final int lapCount = DataModel.getDataModel().getLaps().size();
            if (lapCount > 0) {
                final int lapNumber = lapCount + 1;
                final String lap = res.getString(R.string.sw_notification_lap_number, lapNumber);
                collapsed.setTextViewText(R.id.swn_collapsed_laps, lap);
                collapsed.setViewVisibility(R.id.swn_collapsed_laps, VISIBLE);
                expanded.setTextViewText(R.id.swn_expanded_laps, lap);
                expanded.setViewVisibility(R.id.swn_expanded_laps, VISIBLE);
            } else {
                collapsed.setViewVisibility(R.id.swn_collapsed_laps, GONE);
                expanded.setViewVisibility(R.id.swn_expanded_laps, GONE);
            }
        } else {
            // Left button: Start
            expanded.setTextViewText(leftButtonId, res.getText(R.string.sw_start_button));
            setTextViewDrawable(expanded, leftButtonId, R.drawable.ic_start_24dp);
            final Intent start = new Intent(context, StopwatchService.class)
                    .setAction(HandleDeskClockApiCalls.ACTION_START_STOPWATCH)
                    .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);
            final PendingIntent pendingStart = Utils.pendingServiceIntent(context, start);
            expanded.setOnClickPendingIntent(leftButtonId, pendingStart);

            // Right button: Reset (dismisses notification and resets stopwatch)
            expanded.setViewVisibility(rightButtonId, VISIBLE);
            expanded.setTextViewText(rightButtonId, res.getText(R.string.sw_reset_button));
            setTextViewDrawable(expanded, rightButtonId, R.drawable.ic_reset_24dp);
            final Intent reset = new Intent(context, StopwatchService.class)
                    .setAction(HandleDeskClockApiCalls.ACTION_RESET_STOPWATCH)
                    .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);
            final PendingIntent pendingReset = Utils.pendingServiceIntent(context, reset);
            expanded.setOnClickPendingIntent(rightButtonId, pendingReset);

            // Indicate the stopwatch is paused.
            collapsed.setTextViewText(R.id.swn_collapsed_laps, res.getString(R.string.swn_paused));
            collapsed.setViewVisibility(R.id.swn_collapsed_laps, VISIBLE);
            expanded.setTextViewText(R.id.swn_expanded_laps, res.getString(R.string.swn_paused));
            expanded.setViewVisibility(R.id.swn_expanded_laps, VISIBLE);
        }

        // Swipe away will reset the stopwatch without bringing forward the app.
        final Intent reset = new Intent(context, StopwatchService.class)
                .setAction(HandleDeskClockApiCalls.ACTION_RESET_STOPWATCH)
                .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);

        final Notification notification = new NotificationCompat.Builder(context)
                .setLocalOnly(true)
                .setOngoing(running)
                .setContent(collapsed)
                .setAutoCancel(stopwatch.isPaused())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.stat_notify_stopwatch)
                .setDeleteIntent(Utils.pendingServiceIntent(context, reset))
                .setColor(ContextCompat.getColor(context, R.color.default_background))
                .build();
        notification.bigContentView = expanded;
        return notification;
    }

    private static void setTextViewDrawable(RemoteViews rv, int viewId, int drawableId) {
        rv.setTextViewCompoundDrawablesRelative(viewId, drawableId, 0, 0, 0);
    }
}