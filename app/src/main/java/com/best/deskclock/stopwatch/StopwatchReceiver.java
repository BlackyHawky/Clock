// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.stopwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.Events;

/**
 * This receiver exists solely to allow the stopwatch notification to alter the state of the
 * stopwatch without disturbing the notification shade. If an activity were used instead (even one
 * that is not displayed) the notification manager implicitly closes the notification shade which
 * clashes with the use case of starting/pausing/lapping/resetting the stopwatch without
 * disturbing the notification shade.
 */
public final class StopwatchReceiver extends BroadcastReceiver {

    private static final String ACTION_PREFIX = "com.best.deskclock.action.";

    /** Displays the Stopwatch tab */
    public static final String ACTION_SHOW_STOPWATCH = ACTION_PREFIX + "SHOW_STOPWATCH";

    /** Starts the current stopwatch */
    public static final String ACTION_START_STOPWATCH = ACTION_PREFIX + "START_STOPWATCH";

    /** Pauses the current stopwatch that's currently running */
    public static final String ACTION_PAUSE_STOPWATCH = ACTION_PREFIX + "PAUSE_STOPWATCH";

    /** Laps the stopwatch that's currently running */
    public static final String ACTION_LAP_STOPWATCH = ACTION_PREFIX + "LAP_STOPWATCH";

    /** Resets the stopwatch if it's stopped */
    public static final String ACTION_RESET_STOPWATCH = ACTION_PREFIX + "RESET_STOPWATCH";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        }

        final int label = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, R.string.label_intent);
        final DataModel dataModel = DataModel.getDataModel();

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

}
