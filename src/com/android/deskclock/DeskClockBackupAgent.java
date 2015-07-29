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

package com.android.deskclock;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;

import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class DeskClockBackupAgent extends BackupAgent {

    private static final String TAG = "DeskClockBackupAgent";

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) throws IOException { }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
            ParcelFileDescriptor newState) throws IOException { }

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

    @Override
    public void onRestoreFinished() {
        // Now that alarms have been restored, schedule them in AlarmManager.
        final List<Alarm> alarms = Alarm.getAlarms(getContentResolver(), null);

        final Calendar now = Calendar.getInstance();
        for (Alarm alarm : alarms) {
            // Remove any instances that may currently exist for the alarm;
            // these aren't relevant on the restore device and we'll recreate them below.
            AlarmStateManager.deleteAllInstances(this, alarm.id);

            if (alarm.enabled) {
                // Create the next alarm instance to schedule.
                AlarmInstance alarmInstance = alarm.createInstanceAfter(now);

                // Add the next alarm instance to the database.
                alarmInstance = AlarmInstance.addInstance(getContentResolver(), alarmInstance);

                // Schedule the next alarm instance in AlarmManager.
                AlarmStateManager.registerInstance(this, alarmInstance, true);
                LogUtils.i(TAG, "DeskClockBackupAgent scheduled alarm instance: %s", alarmInstance);
            }
        }
    }
}