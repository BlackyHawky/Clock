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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager.WakeLock;

import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.alarms.AlarmNotifications;
import com.android.deskclock.controller.Controller;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.NotificationUtils;
import com.android.deskclock.provider.AlarmInstance;

import java.util.Calendar;
import java.util.List;

public class AlarmInitReceiver extends BroadcastReceiver {

    private static final String ACTION_UPDATE_ALARM_STATUS =
            "org.codeaurora.poweroffalarm.action.UPDATE_ALARM";

    private static final int SNOOZE_STATUS = 2;
    private static final int DISMISS_STATUS = 3;

    private static final String STATUS = "status";
    private static final String TIME = "time";
    private static final String SNOOZE_TIME = "snooze_time";

    /**
     * When running on N devices, we're interested in the boot completed event that is sent while
     * the user is still locked, so that we can schedule alarms.
     */
    @SuppressLint("InlinedApi")
    private static final String ACTION_BOOT_COMPLETED = Utils.isNOrLater()
            ? Intent.ACTION_LOCKED_BOOT_COMPLETED : Intent.ACTION_BOOT_COMPLETED;

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
        DataModel.getDataModel().updateGlobalIntentId();

        // Updates stopwatch and timer data after a device reboot so they are as accurate as
        // possible.
        if (ACTION_BOOT_COMPLETED.equals(action)) {
            DataModel.getDataModel().updateAfterReboot();
            // Stopwatch and timer data need to be updated on time change so the reboot
            // functionality works as expected.
        } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
            DataModel.getDataModel().updateAfterTimeSet();
        }

        // Update shortcuts so they exist for the user.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            Controller.getController().updateShortcuts();
            NotificationUtils.updateNotificationChannels(context);
        }

        // Notifications are canceled by the system on application upgrade. This broadcast signals
        // that the new app is free to rebuild the notifications using the existing data.
        // Additionally on new app installs, make sure to enable shortcuts immediately as opposed
        // to waiting for system reboot.
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            DataModel.getDataModel().updateAllNotifications();
            Controller.getController().updateShortcuts();
        }

        // Update alarm status once receive the status update broadcast
        if (ACTION_UPDATE_ALARM_STATUS.equals(action)) {
            long alarmTime = intent.getLongExtra(TIME, 0L);
            int alarmStatus = intent.getIntExtra(STATUS, 0);

            if (alarmTime != 0) {
                ContentResolver cr = context.getContentResolver();
                List<AlarmInstance> alarmInstances = AlarmInstance.getInstances(cr, null);
                AlarmInstance alarmInstance = null;
                for (AlarmInstance instance : alarmInstances) {
                    if (instance.getAlarmTime().getTimeInMillis() == alarmTime) {
                        alarmInstance = instance;
                        break;
                    }
                }

                if (alarmInstance != null) {
                    // Update alarm status if the alarm instance is not null
                    if (alarmStatus == DISMISS_STATUS) {
                        AlarmStateManager.setDismissState(context, alarmInstance);
                    } else if (alarmStatus == SNOOZE_STATUS) {
                        long snoozeTime = intent.getLongExtra(SNOOZE_TIME, 0L);
                        if (snoozeTime > System.currentTimeMillis()) {
                            AlarmNotifications.clearNotification(context, alarmInstance);
                            Calendar c = Calendar.getInstance();
                            c.setTimeInMillis(snoozeTime);
                            alarmInstance.setAlarmTime(c);
                            alarmInstance.mAlarmState = AlarmInstance.SNOOZE_STATE;
                            AlarmInstance.updateInstance(cr, alarmInstance);
                        }
                    }
                }
            }
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
