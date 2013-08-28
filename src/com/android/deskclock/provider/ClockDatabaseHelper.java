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

import com.android.deskclock.Log;

/**
 * Helper class for opening the database from multiple providers.  Also provides
 * some common functionality.
 */
class ClockDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 6;

    // This creates a default alarm at 8:30 for every Mon,Tue,Wed,Thu,Fri
    private static final String DEFAULT_ALARM_1 = "(8, 30, 31, 0, 0, 1, '', '', 0);";

    // This creates a default alarm at 9:30 for every Sat,Sun
    private static final String DEFAULT_ALARM_2 = "(9, 00, 96, 0, 0, 1, '', '', 0);";

    // Database and table names
    static final String DATABASE_NAME = "alarms.db";
    static final String ALARMS_TABLE_NAME = "alarms";
    static final String INSTANCES_TABLE_NAME = "alarm_instances";
    static final String CITIES_TABLE_NAME = "selected_cities";

    private static void createAlarmsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ALARMS_TABLE_NAME + " (" +
                ClockContract.AlarmsColumns._ID + " INTEGER PRIMARY KEY," +
                ClockContract.AlarmsColumns.HOUR + " INTEGER, " +
                ClockContract.AlarmsColumns.MINUTES + " INTEGER, " +
                ClockContract.AlarmsColumns.DAYS_OF_WEEK + " INTEGER, " +
                ClockContract.AlarmsColumns.ALARM_TIME + " INTEGER, " +
                ClockContract.AlarmsColumns.ENABLED + " INTEGER, " +
                ClockContract.AlarmsColumns.VIBRATE + " INTEGER, " +
                ClockContract.AlarmsColumns.MESSAGE + " TEXT, " +
                ClockContract.AlarmsColumns.ALERT + " TEXT, " +
                ClockContract.AlarmsColumns.DELETE_AFTER_USE + " INTEGER DEFAULT 0);");
        Log.i("Alarms Table created");
    }

    private static void createInstanceTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + INSTANCES_TABLE_NAME + " (" +
                ClockContract.InstancesColumns._ID + " INTEGER PRIMARY KEY," +
                ClockContract.InstancesColumns.YEAR + " INTEGER, " +
                ClockContract.InstancesColumns.MONTH + " INTEGER, " +
                ClockContract.InstancesColumns.DAY + " INTEGER, " +
                ClockContract.InstancesColumns.HOUR + " INTEGER, " +
                ClockContract.InstancesColumns.MINUTES + " INTEGER, " +
                ClockContract.InstancesColumns.ALARM_STATE + " INTEGER, " +
                ClockContract.InstancesColumns.ALARM_ID + " INTEGER REFERENCES " +
                    ALARMS_TABLE_NAME + "(" + ClockContract.AlarmsColumns._ID + ") " +
                    "ON UPDATE CASCADE ON DELETE CASCADE" +
                ");");
        Log.i("Instance table created");
    }

    private static void createCitiesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + CITIES_TABLE_NAME + " (" +
                ClockContract.CitiesColumns.CITY_ID + " TEXT PRIMARY KEY," +
                ClockContract.CitiesColumns.CITY_NAME + " TEXT, " +
                ClockContract.CitiesColumns.TIMEZONE_NAME + " TEXT, " +
                ClockContract.CitiesColumns.TIMEZONE_OFFSET + " INTEGER);");
        Log.i("Cities table created");
    }

    public ClockDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAlarmsTable(db);
        createInstanceTable(db);
        createCitiesTable(db);

        // insert default alarms
        String cs = ", "; //comma and space
        String insertMe = "INSERT INTO " + ALARMS_TABLE_NAME + " (" +
                ClockContract.AlarmsColumns.HOUR + cs +
                ClockContract.AlarmsColumns.MINUTES + cs +
                ClockContract.AlarmsColumns.DAYS_OF_WEEK + cs +
                ClockContract.AlarmsColumns.ALARM_TIME + cs +
                ClockContract.AlarmsColumns.ENABLED + cs +
                ClockContract.AlarmsColumns.VIBRATE + cs +
                ClockContract.AlarmsColumns.MESSAGE + cs +
                ClockContract.AlarmsColumns.ALERT + cs +
                ClockContract.AlarmsColumns.DELETE_AFTER_USE + ") VALUES ";
        db.execSQL(insertMe + DEFAULT_ALARM_1);
        db.execSQL(insertMe + DEFAULT_ALARM_2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        if (Log.LOGV) {
            Log.v("Upgrading alarms database from version " + oldVersion + " to " + currentVersion);
        }

        if (currentVersion >= DATABASE_VERSION) {
            // Add new delete after use column
            db.execSQL("ALTER TABLE " + ALARMS_TABLE_NAME + " ADD COLUMN " +
                    ClockContract.AlarmsColumns.DELETE_AFTER_USE + " INTEGER DEFAULT 0;");
            Log.i("Added " + ClockContract.AlarmsColumns.DELETE_AFTER_USE + " column to "
                    + ALARMS_TABLE_NAME);

            // Add new tables
            createInstanceTable(db);
            createCitiesTable(db);
        }
    }

    long fixAlarmInsert(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long rowId = -1;
        try {
            // Check if we are trying to re-use an existing id.
            Object value = values.get(ClockContract.AlarmsColumns._ID);
            if (value != null) {
                long id = (Long) value;
                if (id > -1) {
                    final Cursor cursor = db.query(ALARMS_TABLE_NAME,
                            new String[]{ClockContract.AlarmsColumns._ID},
                            ClockContract.AlarmsColumns._ID + " = ?",
                            new String[]{id + ""}, null, null, null);
                    if (cursor.moveToFirst()) {
                        // Record exists. Remove the id so sqlite can generate a new one.
                        values.putNull(ClockContract.AlarmsColumns._ID);
                    }
                }
            }

            rowId = db.insert(ALARMS_TABLE_NAME, ClockContract.AlarmsColumns.MESSAGE, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (rowId < 0) {
            throw new SQLException("Failed to insert row");
        }
        if (Log.LOGV) Log.v("Added alarm rowId = " + rowId);

        return rowId;
    }
}
