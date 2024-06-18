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
import android.net.Uri;
import android.text.TextUtils;

import com.best.deskclock.LogUtils;
import com.best.deskclock.data.Weekdays;

import java.util.Calendar;

/**
 * Helper class for opening the database from multiple providers.  Also provides
 * some common functionality.
 */
class ClockDatabaseHelper extends SQLiteOpenHelper {
    // Database and table names
    static final String DATABASE_NAME = "alarms.db";
    static final String OLD_ALARMS_TABLE_NAME = "alarms";
    static final String ALARMS_TABLE_NAME = "alarm_templates";
    static final String INSTANCES_TABLE_NAME = "alarm_instances";

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
     * Added increasing alarm volume mode
     */
    private static final int VERSION_9 = 10;

    /**
     * Added change profile
     */
    private static final int VERSION_10 = 11;

    /**
     * Removed change profile
     */
    private static final int VERSION_11 = 12;

    private static final String SELECTED_CITIES_TABLE_NAME = "selected_cities";

    public ClockDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION_11);
    }

    private static void createAlarmsTable(SQLiteDatabase db, String alarmsTableName) {
        db.execSQL("CREATE TABLE " + alarmsTableName + " (" +
                ClockContract.AlarmsColumns._ID + " INTEGER PRIMARY KEY," +
                ClockContract.AlarmsColumns.HOUR + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.MINUTES + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.DAYS_OF_WEEK + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.ENABLED + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.STOP_ALARM_WHEN_RINGTONE_ENDS + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.DO_NOT_REPEAT_ALARM + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.VIBRATE + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.LABEL + " TEXT NOT NULL, " +
                ClockContract.AlarmsColumns.RINGTONE + " TEXT, " +
                ClockContract.AlarmsColumns.DELETE_AFTER_USE + " INTEGER NOT NULL DEFAULT 0, " +
                ClockContract.AlarmsColumns.INCREASING_VOLUME + " INTEGER NOT NULL DEFAULT 0);");
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
                ClockContract.InstancesColumns.STOP_ALARM_WHEN_RINGTONE_ENDS + " INTEGER NOT NULL, " +
                ClockContract.AlarmsColumns.DO_NOT_REPEAT_ALARM + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.VIBRATE + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.LABEL + " TEXT NOT NULL, " +
                ClockContract.InstancesColumns.RINGTONE + " TEXT, " +
                ClockContract.InstancesColumns.ALARM_STATE + " INTEGER NOT NULL, " +
                ClockContract.InstancesColumns.ALARM_ID + " INTEGER REFERENCES " +
                ALARMS_TABLE_NAME + "(" + ClockContract.AlarmsColumns._ID + ") " +
                "ON UPDATE CASCADE ON DELETE CASCADE, " +
                ClockContract.InstancesColumns.INCREASING_VOLUME + " INTEGER NOT NULL DEFAULT 0);");
        LogUtils.i("Instance table created");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAlarmsTable(db, ALARMS_TABLE_NAME);
        createInstanceTable(db, INSTANCES_TABLE_NAME);
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
            createAlarmsTable(db, ALARMS_TABLE_NAME);
            createInstanceTable(db, INSTANCES_TABLE_NAME);

            LogUtils.i("Copying old alarms to new table");
            final String[] OLD_TABLE_COLUMNS = {
                    "_id",
                    "hour",
                    "minutes",
                    "daysofweek",
                    "enabled",
                    "stopAlarmWhenRingtoneEnds",
                    "doNotRepeatAlarm",
                    "vibrate",
                    "message",
                    "alert",
                    "incvol"
            };
            try (Cursor cursor = db.query(OLD_ALARMS_TABLE_NAME, OLD_TABLE_COLUMNS,
                    null, null, null, null, null)) {
                final Calendar currentTime = Calendar.getInstance();
                while (cursor != null && cursor.moveToNext()) {
                    final Alarm alarm = new Alarm();
                    alarm.id = cursor.getLong(0);
                    alarm.hour = cursor.getInt(1);
                    alarm.minutes = cursor.getInt(2);
                    alarm.daysOfWeek = Weekdays.fromBits(cursor.getInt(3));
                    alarm.enabled = cursor.getInt(4) == 1;
                    alarm.stopAlarmWhenRingtoneEnds = cursor.getInt(5) == 1;
                    alarm.doNotRepeatAlarm = cursor.getInt(6) == 1;
                    alarm.vibrate = cursor.getInt(7) == 1;
                    alarm.label = cursor.getString(8);

                    final String alertString = cursor.getString(8);
                    if ("silent".equals(alertString)) {
                        alarm.alert = Alarm.NO_RINGTONE_URI;
                    } else {
                        alarm.alert = TextUtils.isEmpty(alertString) ? null : Uri.parse(alertString);
                    }
                    alarm.increasingVolume = cursor.getInt(9) == 1;

                    // Save new version of alarm and create alarm instance for it
                    db.insert(ALARMS_TABLE_NAME, null, Alarm.createContentValues(alarm));
                    if (alarm.enabled) {
                        AlarmInstance newInstance = alarm.createInstanceAfter(currentTime);
                        db.insert(INSTANCES_TABLE_NAME, null, AlarmInstance.createContentValues(newInstance));
                    }
                }
            }

            LogUtils.i("Dropping old alarm table");
            db.execSQL("DROP TABLE IF EXISTS " + OLD_ALARMS_TABLE_NAME + ";");
            return;
        }

        if (oldVersion < VERSION_9) {
            db.execSQL("ALTER TABLE " + ALARMS_TABLE_NAME
                    + " ADD COLUMN " + ClockContract.AlarmsColumns.INCREASING_VOLUME
                    + " INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME
                    + " ADD COLUMN " + ClockContract.InstancesColumns.INCREASING_VOLUME
                    + " INTEGER NOT NULL DEFAULT 0;");
        }

        if (oldVersion < VERSION_10) {
            db.execSQL("ALTER TABLE " + ALARMS_TABLE_NAME
                    + " ADD COLUMN profile"
                    + " TEXT NOT NULL DEFAULT '';");
            db.execSQL("ALTER TABLE " + INSTANCES_TABLE_NAME
                    + " ADD COLUMN profile"
                    + " TEXT NOT NULL DEFAULT '';");
        }

        if (oldVersion < VERSION_11) {
            LogUtils.i("Copying alarms to temporary table");
            final String TEMP_ALARMS_TABLE_NAME = ALARMS_TABLE_NAME + "_temp";
            final String TEMP_INSTANCES_TABLE_NAME = INSTANCES_TABLE_NAME + "_temp";
            createAlarmsTable(db, TEMP_ALARMS_TABLE_NAME);
            createInstanceTable(db, TEMP_INSTANCES_TABLE_NAME);
            final String[] OLD_TABLE_COLUMNS = {
                    ClockContract.AlarmsColumns._ID,
                    ClockContract.AlarmsColumns.HOUR,
                    ClockContract.AlarmsColumns.MINUTES,
                    ClockContract.AlarmsColumns.DAYS_OF_WEEK,
                    ClockContract.AlarmsColumns.ENABLED,
                    ClockContract.AlarmsColumns.STOP_ALARM_WHEN_RINGTONE_ENDS,
                    ClockContract.AlarmsColumns.DO_NOT_REPEAT_ALARM,
                    ClockContract.AlarmsColumns.VIBRATE,
                    ClockContract.AlarmsColumns.LABEL,
                    ClockContract.AlarmsColumns.RINGTONE,
                    ClockContract.AlarmsColumns.DELETE_AFTER_USE,
                    ClockContract.AlarmsColumns.INCREASING_VOLUME
            };

            try (Cursor cursor = db.query(ALARMS_TABLE_NAME, OLD_TABLE_COLUMNS,
                    null, null, null, null, null)) {
                final Calendar currentTime = Calendar.getInstance();
                while (cursor != null && cursor.moveToNext()) {
                    final Alarm alarm = new Alarm(cursor);
                    // Save new version of alarm and create alarm instance for it
                    db.insert(TEMP_ALARMS_TABLE_NAME, null,
                            Alarm.createContentValues(alarm));
                    if (alarm.enabled) {
                        AlarmInstance newInstance = alarm.createInstanceAfter(currentTime);
                        db.insert(TEMP_INSTANCES_TABLE_NAME, null,
                                AlarmInstance.createContentValues(newInstance));
                    }
                }
            }
            db.execSQL("DROP TABLE IF EXISTS " + ALARMS_TABLE_NAME + ";");
            db.execSQL("DROP TABLE IF EXISTS " + INSTANCES_TABLE_NAME + ";");
            db.execSQL("ALTER TABLE " + TEMP_ALARMS_TABLE_NAME
                    + " RENAME TO " + ALARMS_TABLE_NAME + ";");
            db.execSQL("ALTER TABLE " + TEMP_INSTANCES_TABLE_NAME
                    + " RENAME TO " + INSTANCES_TABLE_NAME + ";");
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
