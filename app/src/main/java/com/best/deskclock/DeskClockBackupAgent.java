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

package com.best.deskclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.best.deskclock.alarms.AlarmStateManager;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class DeskClockBackupAgent extends BackupAgent {

    public static final String ACTION_COMPLETE_RESTORE =
            "com.best.deskclock.action.COMPLETE_RESTORE";
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("DeskClockBackupAgent");

    /**
     * @param context a context to access resources and services
     * @return {@code true} if restore data was processed; {@code false} otherwise.
     */
    public static boolean processRestoredData(Context context) {
        // If data was not recently restored, there is nothing to do.
        if (!DataModel.getDataModel().isRestoreBackupFinished()) {
            return false;
        }

        LOGGER.i("processRestoredData() started");

        // Now that alarms have been restored, schedule new instances in AlarmManager.
        final ContentResolver contentResolver = context.getContentResolver();
        final List<Alarm> alarms = Alarm.getAlarms(contentResolver, null);

        final Calendar now = Calendar.getInstance();
        for (Alarm alarm : alarms) {
            // Remove any instances that may currently exist for the alarm;
            // these aren't relevant on the restore device and we'll recreate them below.
            AlarmStateManager.deleteAllInstances(context, alarm.id);

            if (alarm.enabled) {
                // Create the next alarm instance to schedule.
                AlarmInstance alarmInstance = alarm.createInstanceAfter(now);

                // Add the next alarm instance to the database.
                AlarmInstance.addInstance(contentResolver, alarmInstance);

                // Schedule the next alarm instance in AlarmManager.
                AlarmStateManager.registerInstance(context, alarmInstance, true);
                LOGGER.i("DeskClockBackupAgent scheduled alarm instance: %s", alarmInstance);
            }
        }

        // Remove the preference to avoid executing this logic multiple times.
        DataModel.getDataModel().setRestoreBackupFinished(false);

        LOGGER.i("processRestoredData() completed");
        return true;
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
    }

    @Override
    public void onRestoreFile(@NonNull ParcelFileDescriptor data, long size, File destination,
                              int type, long mode, long mtime) throws IOException {
        // The preference file on the backup device may not be the same on the restore device.
        // Massage the file name here before writing it.
        if (destination.getName().endsWith("_preferences.xml")) {
            final String prefFileName = getPackageName() + "_preferences.xml";
            destination = new File(destination.getParentFile(), prefFileName);
        }

        super.onRestoreFile(data, size, destination, type, mode, mtime);
    }

    /**
     * When this method is called during backup/restore, the application is executing in a
     * "minimalist" state. Because of this, the application's ContentResolver cannot be used.
     * Consequently, the work of scheduling alarms on the restore device cannot be done here.
     * Instead, a future callback to DeskClock is used as a signal to reschedule the alarms. The
     * future callback may take the form of ACTION_BOOT_COMPLETED if the device is not yet fully
     * booted (i.e. the restore occurred as part of the setup wizard). If the device is booted, an
     * ACTION_COMPLETE_RESTORE broadcast is scheduled 10 seconds in the future to give
     * backup/restore enough time to kill the Clock process. Both of these future callbacks result
     * in the execution of {@link #processRestoredData(Context)}.
     */
    @Override
    public void onRestoreFinished() {
        // TODO: migrate restored database and preferences over into
        // the device-encrypted storage area

        // Indicate a data restore has been completed.
        DataModel.getDataModel().setRestoreBackupFinished(true);

        // Create an Intent to send into DeskClock indicating restore is complete.
        final PendingIntent restoreIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_COMPLETE_RESTORE).setClass(this, AlarmInitReceiver.class),
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);

        // Deliver the Intent 10 seconds from now.
        final long triggerAtMillis = SystemClock.elapsedRealtime() + 10000;

        // Schedule the Intent delivery in AlarmManager.
        final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, restoreIntent);

        LOGGER.i("Waiting for %s to complete the data restore", ACTION_COMPLETE_RESTORE);
    }
}
