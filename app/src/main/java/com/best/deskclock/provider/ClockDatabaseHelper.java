/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.best.deskclock.utils.LogUtils;

import java.util.Calendar;

/**
 * Helper class for opening the database from multiple providers.  Also provides
 * some common functionality.
 */
class ClockDatabaseHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "alarms.db";
    static final String ALARMS_TABLE_NAME = "alarm_templates";
    static final String INSTANCES_TABLE_NAME = "alarm_instances";

    private static final int DATABASE_VERSION = 21;
    private static final int MINIMUM_SUPPORTED_VERSION = 15;

    public ClockDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static void createAlarmsTable(SQLiteDatabase db, String alarmsTableName) {
        db.execSQL("CREATE TABLE " + alarmsTableName + " (" +
                ClockContract.AlarmsColumns._ID + " INTEGER PRIMARY KEY," +
                ClockContract.AlarmsColumns.YEAR + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.MONTH + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.DAY + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.HOUR + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.MINUTES + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.DAYS_OF_WEEK + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.ENABLED + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.VIBRATE + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.FLASH + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.LABEL + " TEXT NOT NULL, " +
                ClockContract.AlarmsColumns.RINGTONE + " TEXT, " +
                ClockContract.AlarmsColumns.DELETE_AFTER_USE + " INTEGER NOT NULL DEFAULT 0, " +
                ClockContract.AlarmsColumns.AUTO_SILENCE_DURATION + " INTEGER NOT NULL DEFAULT 10, " +
                ClockContract.AlarmsColumns.SNOOZE_DURATION + " INTEGER NOT NULL DEFAULT 10, " +
                ClockContract.AlarmsColumns.CRESCENDO_DURATION + " INTEGER NOT NULL DEFAULT 0, " +
                ClockContract.AlarmsColumns.ALARM_VOLUME + " INTEGER NOT NULL DEFAULT 11);");

        LogUtils.i("Alarms Table created");
    }

    private static void createInstanceTable(SQLiteDatabase db, String instanceTableName) {
        db.execSQL("CREATE TABLE " + instanceTableName + " (" +
                ClockContract.InstancesColumns._ID + " INTEGER PRIMARY KEY," +
                ClockContract.InstancesColumns.YEAR + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.MONTH + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.DAY + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.HOUR + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.MINUTES + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.VIBRATE + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.FLASH + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.LABEL + " TEXT NOT NULL, " +
                ClockContract.InstancesColumns.RINGTONE + " TEXT, " +
                ClockContract.InstancesColumns.ALARM_STATE + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.AUTO_SILENCE_DURATION + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.SNOOZE_DURATION + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.CRESCENDO_DURATION + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.ALARM_VOLUME + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.ALARM_ID + " INTEGER REFERENCES " +
                ALARMS_TABLE_NAME + "(" + ClockContract.AlarmsColumns._ID + ") " +
                "ON UPDATE CASCADE ON DELETE CASCADE);");

        LogUtils.i("Instance table created");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAlarmsTable(db, ALARMS_TABLE_NAME);
        createInstanceTable(db, INSTANCES_TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        if (oldVersion < MINIMUM_SUPPORTED_VERSION) {
            throw new IllegalStateException(
                    "Database version too old (" + oldVersion + "). " +
                            "Minimum supported version is " + MINIMUM_SUPPORTED_VERSION + "."
            );
        }

        LogUtils.v("Upgrading alarms database from version %d to %d", oldVersion, currentVersion);

        // Add the ability to set an alarm on a specific date
        if (oldVersion < 16) {
            int year = Calendar.getInstance().get(Calendar.YEAR);
            int month = Calendar.getInstance().get(Calendar.MONTH);
            int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

            db.execSQL("ALTER TABLE " + ALARMS_TABLE_NAME + " ADD COLUMN year INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE " + ALARMS_TABLE_NAME + " ADD COLUMN month INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE " + ALARMS_TABLE_NAME + " ADD COLUMN day INTEGER NOT NULL DEFAULT 0;");

            db.execSQL("UPDATE " + ALARMS_TABLE_NAME + " SET year = " + year + ", month = " + month + ", day = " + day + ";");

            LogUtils.i("Added jear, month and day columns to alarm table for version 16 upgrade.");
        }

        // Add the ability to set the volume crescendo duration per alarm
        if (oldVersion < 17) {
            db.execSQL("ALTER TABLE " + ALARMS_TABLE_NAME + " ADD COLUMN crescendoDuration" + " INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME + " ADD COLUMN crescendoDuration" + " INTEGER NOT NULL DEFAULT 0;");

            LogUtils.i("Added crescendoDuration column for version 17 upgrade.");
        }

        // Add the ability to set the snooze duration per alarm
        if (oldVersion < 18) {
            db.execSQL("ALTER TABLE " + ALARMS_TABLE_NAME + " ADD COLUMN snoozeDuration" + " INTEGER NOT NULL DEFAULT 10;");
            db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME + " ADD COLUMN snoozeDuration" + " INTEGER NOT NULL DEFAULT 10;");

            LogUtils.i("Added snoozeDuration column for version 18 upgrade.");
        }

        // Add the ability to set the auto silence duration per alarm
        if (oldVersion < 19) {
            db.execSQL("ALTER TABLE " + ALARMS_TABLE_NAME + " ADD COLUMN autoSilenceDuration" + " INTEGER NOT NULL DEFAULT 10;");
            db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME + " ADD COLUMN autoSilenceDuration" + " INTEGER NOT NULL DEFAULT 10;");

            LogUtils.i("Added autoSilenceDuration column for version 19 upgrade.");
        }

        // Add the ability to set the alarm volume per alarm
        if (oldVersion < 20) {
            db.execSQL("ALTER TABLE " + ALARMS_TABLE_NAME + " ADD COLUMN alarmVolume" + " INTEGER NOT NULL DEFAULT 11;");
            db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME + " ADD COLUMN alarmVolume" + " INTEGER NOT NULL DEFAULT 11;");

            LogUtils.i("Added alarmVolume column for version 20 upgrade.");
        }

        // Remove "Dismiss alarm when ringtone ends", "Alarm snooze action" and "Increasing volume"
        if (oldVersion < 21) {
            LogUtils.i("Copying alarms to temporary table");
            final String TEMP_ALARMS_TABLE_NAME = ALARMS_TABLE_NAME + "_temp";
            final String TEMP_INSTANCES_TABLE_NAME = INSTANCES_TABLE_NAME + "_temp";
            createAlarmsTable(db, TEMP_ALARMS_TABLE_NAME);
            createInstanceTable(db, TEMP_INSTANCES_TABLE_NAME);
            final String[] OLD_TABLE_COLUMNS = {
                    ClockContract.AlarmsColumns._ID,
                    ClockContract.AlarmsColumns.YEAR,
                    ClockContract.AlarmsColumns.MONTH,
                    ClockContract.AlarmsColumns.DAY,
                    ClockContract.AlarmsColumns.HOUR,
                    ClockContract.AlarmsColumns.MINUTES,
                    ClockContract.AlarmsColumns.DAYS_OF_WEEK,
                    ClockContract.AlarmsColumns.ENABLED,
                    ClockContract.AlarmsColumns.VIBRATE,
                    ClockContract.AlarmsColumns.FLASH,
                    ClockContract.AlarmsColumns.LABEL,
                    ClockContract.AlarmsColumns.RINGTONE,
                    ClockContract.AlarmsColumns.DELETE_AFTER_USE,
                    ClockContract.AlarmsColumns.AUTO_SILENCE_DURATION,
                    ClockContract.AlarmsColumns.SNOOZE_DURATION,
                    ClockContract.AlarmsColumns.CRESCENDO_DURATION,
                    ClockContract.AlarmsColumns.ALARM_VOLUME
            };

            try (Cursor cursor = db.query(ALARMS_TABLE_NAME, OLD_TABLE_COLUMNS,
                    null, null, null, null, null)) {
                final Calendar currentTime = Calendar.getInstance();
                while (cursor.moveToNext()) {
                    final Alarm alarm = new Alarm(cursor);
                    // Save new version of alarm and create alarm instance for it
                    db.insert(TEMP_ALARMS_TABLE_NAME, null, Alarm.createContentValues(alarm));
                    if (alarm.enabled) {
                        AlarmInstance newInstance = alarm.createInstanceAfter(currentTime);
                        db.insert(TEMP_INSTANCES_TABLE_NAME, null,
                                AlarmInstance.createContentValues(newInstance));
                    }
                }
            }
            db.execSQL("DROP TABLE IF EXISTS " + ALARMS_TABLE_NAME + ";");
            db.execSQL("DROP TABLE IF EXISTS " + INSTANCES_TABLE_NAME + ";");
            db.execSQL("ALTER TABLE " + TEMP_ALARMS_TABLE_NAME + " RENAME TO " + ALARMS_TABLE_NAME + ";");
            db.execSQL("ALTER TABLE " + TEMP_INSTANCES_TABLE_NAME + " RENAME TO " + INSTANCES_TABLE_NAME + ";");

            LogUtils.i("dismissAlarmWhenRingtoneEnds, alarmSnoozeActions and increasingVolume" +
                    " columns removed for version 21 upgrade.");
        }
    }

    long fixAlarmInsert(ContentValues values) {
        // Why are we doing this? Is this not a programming bug if we try to
        // insert an already used id?
        final SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long rowId;
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
