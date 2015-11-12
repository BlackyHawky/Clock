/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.SystemClock;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.RemoteViews;

import com.android.deskclock.HandleDeskClockApiCalls;
import com.android.deskclock.R;
import com.android.deskclock.stopwatch.StopwatchService;

import java.util.Collections;
import java.util.List;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * All {@link Stopwatch} data is accessed via this model.
 */
final class StopwatchModel {

    private final Context mContext;

    /** The model from which notification data are fetched. */
    private final NotificationModel mNotificationModel;

    /** Used to create and destroy system notifications related to the stopwatch. */
    private final NotificationManagerCompat mNotificationManager;

    /** Update stopwatch notification when locale changes. */
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /** The current state of the stopwatch. */
    private Stopwatch mStopwatch;

    /** A mutable copy of the recorded stopwatch laps. */
    private List<Lap> mLaps;

    StopwatchModel(Context context, NotificationModel notificationModel) {
        mContext = context;
        mNotificationModel = notificationModel;
        mNotificationManager = NotificationManagerCompat.from(context);

        // Update stopwatch notification when locale changes.
        final IntentFilter localeBroadcastFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter);
    }

    /**
     * @return the current state of the stopwatch
     */
    Stopwatch getStopwatch() {
        if (mStopwatch == null) {
            mStopwatch = StopwatchDAO.getStopwatch(mContext);
        }

        return mStopwatch;
    }

    /**
     * @param stopwatch the new state of the stopwatch
     */
    Stopwatch setStopwatch(Stopwatch stopwatch) {
        if (mStopwatch != stopwatch) {
            StopwatchDAO.setStopwatch(mContext, stopwatch);
            mStopwatch = stopwatch;

            // Refresh the stopwatch notification to reflect the latest stopwatch state.
            if (!mNotificationModel.isApplicationInForeground()) {
                updateNotification();
            }
        }

        return stopwatch;
    }

    /**
     * @return the laps recorded for this stopwatch
     */
    List<Lap> getLaps() {
        return Collections.unmodifiableList(getMutableLaps());
    }

    /**
     * @return a newly recorded lap completed now; {@code null} if no more laps can be added
     */
    Lap addLap() {
        if (!canAddMoreLaps()) {
            return null;
        }

        final long totalTime = getStopwatch().getTotalTime();
        final List<Lap> laps = getMutableLaps();

        final int lapNumber = laps.size() + 1;
        StopwatchDAO.addLap(mContext, lapNumber, totalTime);

        final long prevAccumulatedTime = laps.isEmpty() ? 0 : laps.get(0).getAccumulatedTime();
        final long lapTime = totalTime - prevAccumulatedTime;

        final Lap lap = new Lap(lapNumber, lapTime, totalTime);
        laps.add(0, lap);

        // Refresh the stopwatch notification to reflect the latest stopwatch state.
        if (!mNotificationModel.isApplicationInForeground()) {
            updateNotification();
        }

        return lap;
    }

    /**
     * Clears the laps recorded for this stopwatch.
     */
    void clearLaps() {
        StopwatchDAO.clearLaps(mContext);
        getMutableLaps().clear();
    }

    /**
     * @return {@code true} iff more laps can be recorded
     */
    boolean canAddMoreLaps() {
        return getLaps().size() < 98;
    }

    /**
     * @return the longest lap time of all recorded laps and the current lap
     */
    long getLongestLapTime() {
        long maxLapTime = 0;

        final List<Lap> laps = getLaps();
        if (!laps.isEmpty()) {
            // Compute the maximum lap time across all recorded laps.
            for (Lap lap : getLaps()) {
                maxLapTime = Math.max(maxLapTime, lap.getLapTime());
            }

            // Compare with the maximum lap time for the current lap.
            final Stopwatch stopwatch = getStopwatch();
            final long currentLapTime = stopwatch.getTotalTime() - laps.get(0).getAccumulatedTime();
            maxLapTime = Math.max(maxLapTime, currentLapTime);
        }

        return maxLapTime;
    }

    /**
     * @param time a point in time after the end of the last lap
     * @return the elapsed time between the given {@code time} and the end of the previous lap
     */
    long getCurrentLapTime(long time) {
        final Lap previousLap = getLaps().get(0);

        final long last = previousLap.getAccumulatedTime();
        final long lapTime = time - last;

        if (lapTime < 0) {
            final String message = String.format("time (%d) must exceed last lap (%d)", time, last);
            throw new IllegalArgumentException(message);
        }

        return lapTime;
    }

    /**
     * Updates the notification to reflect the latest state of the stopwatch and recorded laps.
     */
    void updateNotification() {
        final Stopwatch stopwatch = getStopwatch();

        // Notification should be hidden if the stopwatch has no time or the app is open.
        if (stopwatch.isReset() || mNotificationModel.isApplicationInForeground()) {
            mNotificationManager.cancel(mNotificationModel.getStopwatchNotificationId());
            return;
        }

        @StringRes final int eventLabel = R.string.label_notification;

        // Intent to load the app when the notification is tapped.
        final Intent showApp = new Intent(mContext, HandleDeskClockApiCalls.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(HandleDeskClockApiCalls.ACTION_SHOW_STOPWATCH)
                .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = PendingIntent.getActivity(mContext, 0, showApp,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        // Compute some values required below.
        final boolean running = stopwatch.isRunning();
        final String pname = mContext.getPackageName();
        final Resources res = mContext.getResources();
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
            final Intent pause = new Intent(mContext, StopwatchService.class)
                    .setAction(HandleDeskClockApiCalls.ACTION_PAUSE_STOPWATCH)
                    .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);
            expanded.setOnClickPendingIntent(leftButtonId, pendingServiceIntent(pause));

            // Right button: Add Lap
            if (canAddMoreLaps()) {
                expanded.setTextViewText(rightButtonId, res.getText(R.string.sw_lap_button));
                setTextViewDrawable(expanded, rightButtonId, R.drawable.ic_sw_lap_24dp);

                final Intent lap = new Intent(mContext, StopwatchService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_LAP_STOPWATCH)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);
                expanded.setOnClickPendingIntent(rightButtonId, pendingServiceIntent(lap));
                expanded.setViewVisibility(rightButtonId, VISIBLE);
            } else {
                expanded.setViewVisibility(rightButtonId, INVISIBLE);
            }

            // Show the current lap number if any laps have been recorded.
            final int lapCount = getLaps().size();
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
            final Intent start = new Intent(mContext, StopwatchService.class)
                    .setAction(HandleDeskClockApiCalls.ACTION_START_STOPWATCH)
                    .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);
            expanded.setOnClickPendingIntent(leftButtonId, pendingServiceIntent(start));

            // Right button: Reset (HandleDeskClockApiCalls will also bring forward the app)
            expanded.setViewVisibility(rightButtonId, VISIBLE);
            expanded.setTextViewText(rightButtonId, res.getText(R.string.sw_reset_button));
            setTextViewDrawable(expanded, rightButtonId, R.drawable.ic_reset_24dp);
            final Intent reset = new Intent(mContext, HandleDeskClockApiCalls.class)
                    .setAction(HandleDeskClockApiCalls.ACTION_RESET_STOPWATCH)
                    .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);
            expanded.setOnClickPendingIntent(rightButtonId, pendingActivityIntent(reset));

            // Indicate the stopwatch is paused.
            collapsed.setTextViewText(R.id.swn_collapsed_laps, res.getString(R.string.swn_paused));
            collapsed.setViewVisibility(R.id.swn_collapsed_laps, VISIBLE);
            expanded.setTextViewText(R.id.swn_expanded_laps, res.getString(R.string.swn_paused));
            expanded.setViewVisibility(R.id.swn_expanded_laps, VISIBLE);
        }

        // Swipe away will reset the stopwatch without bringing forward the app.
        final Intent reset = new Intent(mContext, StopwatchService.class)
                .setAction(HandleDeskClockApiCalls.ACTION_RESET_STOPWATCH)
                .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, eventLabel);

        final Notification notification = new NotificationCompat.Builder(mContext)
                .setLocalOnly(true)
                .setOngoing(running)
                .setContent(collapsed)
                .setAutoCancel(stopwatch.isPaused())
                .setPriority(Notification.PRIORITY_MAX)
                .setDeleteIntent(pendingServiceIntent(reset))
                .setSmallIcon(R.drawable.ic_tab_stopwatch_activated)
                .build();
        notification.bigContentView = expanded;
        mNotificationManager.notify(mNotificationModel.getStopwatchNotificationId(), notification);
    }

    private PendingIntent pendingServiceIntent(Intent intent) {
        return PendingIntent.getService(mContext, 0, intent, FLAG_UPDATE_CURRENT);
    }

    private PendingIntent pendingActivityIntent(Intent intent) {
        return PendingIntent.getActivity(mContext, 0, intent, FLAG_UPDATE_CURRENT);
    }

    private static void setTextViewDrawable(RemoteViews rv, int viewId, int drawableId) {
        rv.setTextViewCompoundDrawablesRelative(viewId, drawableId, 0, 0, 0);
    }

    private List<Lap> getMutableLaps() {
        if (mLaps == null) {
            mLaps = StopwatchDAO.getLaps(mContext);
        }

        return mLaps;
    }

    /**
     * Update the stopwatch notification in response to a locale change.
     */
    private final class LocaleChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNotification();
        }
    }
}