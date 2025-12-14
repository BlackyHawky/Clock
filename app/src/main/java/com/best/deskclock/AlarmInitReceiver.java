/*
 * Copyright (C) 2007 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager.WakeLock;

import com.best.deskclock.alarms.AlarmNotifications;
import com.best.deskclock.alarms.AlarmStateManager;
import com.best.deskclock.controller.Controller;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.SdkUtils;

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
    private static final String ACTION_BOOT_COMPLETED = SdkUtils.isAtLeastAndroid7()
            ? Intent.ACTION_LOCKED_BOOT_COMPLETED : Intent.ACTION_BOOT_COMPLETED;

    /**
     * This receiver handles a variety of actions:
     *
     * <ul>
     *     <li>Update timers and stopwatch datas on ACTION_BOOT_COMPLETED, TIME_SET
     *     and TIMEZONE_CHANGED</li>
     *     <li>Rebuild notifications on ACTION_BOOT_COMPLETED and LOCALE_CHANGED</li>
     *     <li>Fix alarm states on ACTION_BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED,
     *     and LOCALE_CHANGED</li>
     * </ul>
     */

    @SuppressLint({"WakelockTimeout", "Wakelock"})
    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        LogUtils.i("AlarmInitReceiver " + action);

        final PendingResult result = goAsync();
        final WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();

        // We need to increment the global id out of the async task to prevent race conditions
        SettingsDAO.updateGlobalIntentId(getDefaultSharedPreferences(context));

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
            DataModel.getDataModel().updateAllNotifications();

            if (SdkUtils.isAtLeastAndroid8()) {
                NotificationUtils.updateNotificationChannels(context);
            }
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
                            alarmInstance.updateInstance(cr);
                        }
                    }
                }
            }
        }

        AsyncHandler.post(() -> {
            try {
                // Update all the alarm instances
                AlarmStateManager.fixAlarmInstances(context);
            } finally {
                result.finish();
                wl.release();
                LogUtils.v("AlarmInitReceiver finished");
            }
        });
    }
}
