/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.stopwatch;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.Events;

/**
 * This service exists solely to allow the stopwatch notification to alter the state of the
 * stopwatch without disturbing the notification shade. If an activity were used instead (even one
 * that is not displayed) the notification manager implicitly closes the notification shade which
 * clashes with the use case of starting/pausing/lapping/resetting the stopwatch without
 * disturbing the notification shade.
 */
public final class StopwatchService extends Service {

    private static final String ACTION_PREFIX = "com.best.deskclock.action.";

    // shows the tab with the stopwatch
    public static final String ACTION_SHOW_STOPWATCH = ACTION_PREFIX + "SHOW_STOPWATCH";
    // starts the current stopwatch
    public static final String ACTION_START_STOPWATCH = ACTION_PREFIX + "START_STOPWATCH";
    // pauses the current stopwatch that's currently running
    public static final String ACTION_PAUSE_STOPWATCH = ACTION_PREFIX + "PAUSE_STOPWATCH";
    // laps the stopwatch that's currently running
    public static final String ACTION_LAP_STOPWATCH = ACTION_PREFIX + "LAP_STOPWATCH";
    // resets the stopwatch if it's stopped
    public static final String ACTION_RESET_STOPWATCH = ACTION_PREFIX + "RESET_STOPWATCH";

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        final String action = intent.getAction();
        final int label = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, R.string.label_intent);
        final DataModel dataModel = DataModel.getDataModel();

        if (action != null) {
            switch (action) {
                case ACTION_START_STOPWATCH -> {
                    Events.sendStopwatchEvent(R.string.action_start, label);
                    dataModel.startStopwatch();
                }
                case ACTION_PAUSE_STOPWATCH -> {
                    Events.sendStopwatchEvent(R.string.action_pause, label);
                    dataModel.pauseStopwatch();
                }
                case ACTION_RESET_STOPWATCH -> {
                    Events.sendStopwatchEvent(R.string.action_reset, label);
                    dataModel.resetStopwatch();
                }
                case ACTION_LAP_STOPWATCH -> {
                    Events.sendStopwatchEvent(R.string.action_lap, label);
                    dataModel.addLap();
                }
            }
        }

        stopSelf(startId);

        return START_NOT_STICKY;
    }
}
