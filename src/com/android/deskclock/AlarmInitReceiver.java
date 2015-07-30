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
import android.content.SharedPreferences;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

import com.android.deskclock.alarms.AlarmStateManager;

import com.android.deskclock.timer.TimerObj;

public class AlarmInitReceiver extends BroadcastReceiver {

    // A flag that indicates that switching the volume button default was done
    private static final String PREF_VOLUME_DEF_DONE = "vol_def_done";

    /**
     * This receiver handles a variety of actions:
     *
     * <ul>
     *     <li>Clean up backup data that was recently restored to this device on
     *     ACTION_COMPLETE_RESTORE.</li>
     *     <li>Clean up backup data that was recently restored to this device and reset timers and
     *     clear stopwatch on ACTION_BOOT_COMPLETED</li>
     *     <li>Fix alarm states on ACTION_BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED,
     *     and LOCALE_CHANGED</li>
     * </ul>
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        LogUtils.v("AlarmInitReceiver " + action);

        final PendingResult result = goAsync();
        final WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();

        // We need to increment the global id out of the async task to prevent race conditions
        AlarmStateManager.updateGlobalIntentId(context);

        AsyncHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                        // Clear stopwatch and timers data
                        final SharedPreferences prefs =
                                PreferenceManager.getDefaultSharedPreferences(context);
                        LogUtils.v("AlarmInitReceiver - Reset timers and clear stopwatch data");
                        TimerObj.resetTimersInSharedPrefs(prefs);
                        Utils.clearSwSharedPref(prefs);

                        if (!prefs.getBoolean(PREF_VOLUME_DEF_DONE, false)) {
                            // Fix the default
                            LogUtils.v("AlarmInitReceiver - resetting volume button default");
                            switchVolumeButtonDefault(prefs);
                        }
                    }

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

    private void switchVolumeButtonDefault(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
            SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);

        // Make sure we do it only once
        editor.putBoolean(PREF_VOLUME_DEF_DONE, true);
        editor.apply();
    }
}
