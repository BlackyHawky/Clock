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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationManagerCompat;

import com.android.deskclock.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /** The listeners to notify when the stopwatch or its laps change. */
    private final List<StopwatchListener> mStopwatchListeners = new ArrayList<>();

    /** Delegate that builds platform-specific stopwatch notifications. */
    private NotificationBuilder mNotificationBuilder;

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
     * @param stopwatchListener to be notified when stopwatch changes or laps are added
     */
    void addStopwatchListener(StopwatchListener stopwatchListener) {
        mStopwatchListeners.add(stopwatchListener);
    }

    /**
     * @param stopwatchListener to no longer be notified when stopwatch changes or laps are added
     */
    void removeStopwatchListener(StopwatchListener stopwatchListener) {
        mStopwatchListeners.remove(stopwatchListener);
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
        final Stopwatch before = getStopwatch();
        if (before != stopwatch) {
            StopwatchDAO.setStopwatch(mContext, stopwatch);
            mStopwatch = stopwatch;

            // Refresh the stopwatch notification to reflect the latest stopwatch state.
            if (!mNotificationModel.isApplicationInForeground()) {
                updateNotification();
            }

            // Resetting the stopwatch implicitly clears the recorded laps.
            if (stopwatch.isReset()) {
                clearLaps();
            }

            // Notify listeners of the stopwatch change.
            for (StopwatchListener stopwatchListener : mStopwatchListeners) {
                stopwatchListener.stopwatchUpdated(before, stopwatch);
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

        // Notify listeners of the new lap.
        for (StopwatchListener stopwatchListener : mStopwatchListeners) {
            stopwatchListener.lapAdded(lap);
        }

        return lap;
    }

    /**
     * Clears the laps recorded for this stopwatch.
     */
    @VisibleForTesting
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
     * In practice, {@code time} can be any value due to device reboots. When the real-time clock is
     * reset, there is no more guarantee that this time falls after the last recorded lap.
     *
     * @param time a point in time expected, but not required, to be after the end of the prior lap
     * @return the elapsed time between the given {@code time} and the end of the prior lap;
     *      negative elapsed times are normalized to {@code 0}
     */
    long getCurrentLapTime(long time) {
        final Lap previousLap = getLaps().get(0);
        final long currentLapTime = time - previousLap.getAccumulatedTime();
        return Math.max(0, currentLapTime);
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

        // Otherwise build and post a notification reflecting the latest stopwatch state.
        final Notification notification =
                getNotificationBuilder().build(mContext, mNotificationModel, stopwatch);
        mNotificationManager.notify(mNotificationModel.getStopwatchNotificationId(), notification);
    }

    private List<Lap> getMutableLaps() {
        if (mLaps == null) {
            mLaps = StopwatchDAO.getLaps(mContext);
        }

        return mLaps;
    }

    private NotificationBuilder getNotificationBuilder() {
        if (mNotificationBuilder == null) {
            if (Utils.isNOrLater()) {
                mNotificationBuilder = new StopwatchNotificationBuilderN();
            } else {
                mNotificationBuilder = new StopwatchNotificationBuilderPreN();
            }
        }

        return mNotificationBuilder;
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

    /**
     * An API for building platform-specific stopwatch notifications.
     */
    public interface NotificationBuilder {
        /**
         * @param context a context to use for fetching resources
         * @param nm from which notification data are fetched
         * @param stopwatch the stopwatch guaranteed to be running or paused
         * @return a notification reporting the state of the {@code stopwatch}
         */
        Notification build(Context context, NotificationModel nm, Stopwatch stopwatch);
    }
}