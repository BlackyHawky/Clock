/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.deskclock.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import com.android.deskclock.LogUtils;

import java.util.Calendar;

/**
 * Helper class for opening the database from multiple providers.  Also provides
 * some common functionality.
 */
class ClockDatabaseHelper extends SQLiteOpenHelper {
    /**
     * Original Clock Database.
     **/
    private static final int VERSION_5 = 5;

    /**
     * Added alarm_instances table
     * Added selected_cities table
     * Added DELETE_AFTER_USE column to alarms table
     */
    private static final int VERSION_6 = 6;

    /**
     * Added alarm settings to instance table.
     */
    private static final int VERSION_7 = 7;

    /**
     * Removed selected_cities table.
     */
    private static final int VERSION_8 = 8;

    // This creates a default alarm at 8:30 for every Mon,Tue,Wed,Thu,Fri
    private static final String DEFAULT_ALARM_1 = "(8, 30, 31, 0, 1, '', NULL, 0);";

    // This creates a default alarm at 9:30 for every Sat,Sun
    private static final String DEFAULT_ALARM_2 = "(9, 00, 96, 0, 1, '', NULL, 0);";

    // Database and table names
    static final String DATABASE_NAME = "alarms.db";
    static final String OLD_ALARMS_TABLE_NAME = "alarms";
    static final String ALARMS_TABLE_NAME = "alarm_templates";
    static final String INSTANCES_TABLE_NAME = "alarm_instances";
    private static final String SELECTED_CITIES_TABLE_NAME = "selected_cities";

    private static void createAlarmsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ALARMS_TABLE_NAME + " (" +
                ClockContract.AlarmsColumns._ID + " INTEGER PRIMARY KEY," +
                ClockContract.AlarmsColumns.HOUR + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.MINUTES + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.DAYS_OF_WEEK + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.ENABLED + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.VIBRATE + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.LABEL + " TEXT NOT NULL, " +
                ClockContract.AlarmsColumns.RINGTONE + " TEXT, " +
                ClockContract.AlarmsColumns.DELETE_AFTER_USE + " INTEGER NOT NULL DEFAULT 0);");
        LogUtils.i("Alarms Table created");
    }

    private static void createInstanceTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + INSTANCES_TABLE_NAME + " (" +
                ClockContract.InstancesColumns._ID + " INTEGER PRIMARY KEY," +
                ClockContract.InstancesColumns.YEAR + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.MONTH + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.DAY + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.HOUR + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.MINUTES + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.VIBRATE + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.LABEL + " TEXT NOT NULL, " +
                ClockContract.InstancesColumns.RINGTONE + " TEXT, " +
                ClockContract.InstancesColumns.ALARM_STATE + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.ALARM_ID + " INTEGER REFERENCES " +
                    ALARMS_TABLE_NAME + "(" + ClockContract.AlarmsColumns._ID + ") " +
                    "ON UPDATE CASCADE ON DELETE CASCADE" +
                ");");
        LogUtils.i("Instance table created");
    }

    public ClockDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION_8);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAlarmsTable(db);
        createInstanceTable(db);

        // insert default alarms
        LogUtils.i("Inserting default alarms");
        String cs = ", "; //comma and space
        String insertMe = "INSERT INTO " + ALARMS_TABLE_NAME + " (" +
                ClockContract.AlarmsColumns.HOUR + cs +
                ClockContract.AlarmsColumns.MINUTES + cs +
                ClockContract.AlarmsColumns.DAYS_OF_WEEK + cs +
                ClockContract.AlarmsColumns.ENABLED + cs +
                ClockContract.AlarmsColumns.VIBRATE + cs +
                ClockContract.AlarmsColumns.LABEL + cs +
                ClockContract.AlarmsColumns.RINGTONE + cs +
                ClockContract.AlarmsColumns.DELETE_AFTER_USE + ") VALUES ";
        db.execSQL(insertMe + DEFAULT_ALARM_1);
        db.execSQL(insertMe + DEFAULT_ALARM_2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        LogUtils.v("Upgrading alarms database from version %d to %d", oldVersion, currentVersion);

        if (oldVersion <= VERSION_7) {
            // This was not used in VERSION_7 or prior, so we can just drop it.
            db.execSQL("DROP TABLE IF EXISTS " + SELECTED_CITIES_TABLE_NAME + ";");
        }

        if (oldVersion <= VERSION_6) {
            // This was not used in VERSION_6 or prior, so we can just drop it.
            db.execSQL("DROP TABLE IF EXISTS " + INSTANCES_TABLE_NAME + ";");

            // Create new alarms table and copy over the data
            createAlarmsTable(db);
            createInstanceTable(db);

            LogUtils.i("Copying old alarms to new table");
            final String[] OLD_TABLE_COLUMNS = {
                    "_id",
                    "hour",
                    "minutes",
                    "daysofweek",
                    "enabled",
                    "vibrate",
                    "message",
                    "alert",
            };
            try (Cursor cursor = db.query(OLD_ALARMS_TABLE_NAME, OLD_TABLE_COLUMNS,
                    null, null, null, null, null)) {
                final Calendar currentTime = Calendar.getInstance();
                while (cursor != null && cursor.moveToNext()) {
                    final Alarm alarm = new Alarm();
                    alarm.id = cursor.getLong(0);
                    alarm.hour = cursor.getInt(1);
                    alarm.minutes = cursor.getInt(2);
                    alarm.daysOfWeek = new DaysOfWeek(cursor.getInt(3));
                    alarm.enabled = cursor.getInt(4) == 1;
                    alarm.vibrate = cursor.getInt(5) == 1;
                    alarm.label = cursor.getString(6);

                    final String alertString = cursor.getString(7);
                    if ("silent".equals(alertString)) {
                        alarm.alert = Alarm.NO_RINGTONE_URI;
                    } else {
                        alarm.alert =
                                TextUtils.isEmpty(alertString) ? null : Uri.parse(alertString);
                    }

                    // Save new version of alarm and create alarm instance for it
                    db.insert(ALARMS_TABLE_NAME, null, Alarm.createContentValues(alarm));
                    if (alarm.enabled) {
                        AlarmInstance newInstance = alarm.createInstanceAfter(currentTime);
                        db.insert(INSTANCES_TABLE_NAME, null,
                                AlarmInstance.createContentValues(newInstance));
                    }
                }
            }

            LogUtils.i("Dropping old alarm table");
            db.execSQL("DROP TABLE IF EXISTS " + OLD_ALARMS_TABLE_NAME + ";");
        }
    }

    long fixAlarmInsert(ContentValues values) {
        // Why are we doing this? Is this not a programming bug if we try to
        // insert an already used id?
        final SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long rowId = -1;
        try {
            // Check if we are trying to re-use an existing id.
            final Object value = values.get(ClockContract.AlarmsColumns._ID);
            if (value != null) {
                long id = (Long) value;
                if (id > -1) {
                    final String[] columns = {ClockContract.AlarmsColumns._ID};
                    final String selection = ClockContract.AlarmsColumns._ID + " = ?";
                    final String[] selectionArgs = {String.valueOf(id)};
                    try (Cursor cursor = db.query(ALARMS_TABLE_NAME, columns, selection,
                            selectionArgs, null, null, null)) {
                        if (cursor.moveToFirst()) {
                            // Record exists. Remove the id so sqlite can generate a new one.
                            values.putNull(ClockContract.AlarmsColumns._ID);
                        }
                    }
                }
            }

            rowId = db.insert(ALARMS_TABLE_NAME, ClockContract.AlarmsColumns.RINGTONE, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (rowId < 0) {
            throw new SQLException("Failed to insert row");
        }
        LogUtils.v("Added alarm rowId = " + rowId);

        return rowId;
    }
}
