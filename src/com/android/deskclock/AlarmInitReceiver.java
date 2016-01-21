/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager.WakeLock;

import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;

public class AlarmInitReceiver extends BroadcastReceiver {
    /**
     * This receiver handles a variety of actions:
     *
     * <ul>
     *     <li>Clean up backup data that was recently restored to this device on
     *     ACTION_COMPLETE_RESTORE.</li>
     *     <li>Reset timers and stopwatch on ACTION_BOOT_COMPLETED</li>
     *     <li>Fix alarm states on ACTION_BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED,
     *     and LOCALE_CHANGED</li>
     *     <li>Rebuild notifications on MY_PACKAGE_REPLACED</li>
     * </ul>
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        LogUtils.i("AlarmInitReceiver " + action);

        final PendingResult result = goAsync();
        final WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();

        // We need to increment the global id out of the async task to prevent race conditions
        AlarmStateManager.updateGlobalIntentId(context);

        // Clear stopwatch data and reset timers because they rely on elapsed real-time values
        // which are meaningless after a device reboot.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            DataModel.getDataModel().clearLaps();
            DataModel.getDataModel().resetStopwatch();
            Events.sendStopwatchEvent(R.string.action_reset, R.string.label_reboot);
            DataModel.getDataModel().resetTimers(R.string.label_reboot);
        }

        // Notifications are canceled by the system on application upgrade. This broadcast signals
        // that the new app is free to rebuild the notifications using the existing data.
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            DataModel.getDataModel().updateAllNotifications();
        }

        AsyncHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // Process restored data if any exists
                    if (!DeskClockBackupAgent.processRestoredData(context)) {
                        // Update all the alarm instances on time change event
                        AlarmStateManager.fixAlarmInstances(context);
                    }
                } finally {
                    result.finish();
                    wl.release();
                    LogUtils.v("AlarmInitReceiver finished");
                }
            }
        });
    }
}