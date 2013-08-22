/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.android.deskclock.Log;

/**
 * Helper class for opening the database from multiple providers.  Also provides
 * some common functionality.
 */
class ClockDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "alarms.db";
    private static final String TABLE_NAME = "alarms";
    private static final int DATABASE_VERSION = 5;

    public ClockDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                   Alarm.Columns._ID + " INTEGER PRIMARY KEY," +
                   Alarm.Columns.HOUR + " INTEGER, " +
                   Alarm.Columns.MINUTES + " INTEGER, " +
                   Alarm.Columns.DAYS_OF_WEEK + " INTEGER, " +
                   Alarm.Columns.ALARM_TIME + " INTEGER, " +
                   Alarm.Columns.ENABLED + " INTEGER, " +
                   Alarm.Columns.VIBRATE + " INTEGER, " +
                   Alarm.Columns.MESSAGE + " TEXT, " +
                   Alarm.Columns.ALERT + " TEXT);");

        // insert default alarms
        String cs = ", "; //coma and space
        String insertMe = "INSERT INTO " + TABLE_NAME +
                " (" + Alarm.Columns.HOUR + cs + Alarm.Columns.MINUTES + cs +
                Alarm.Columns.DAYS_OF_WEEK + cs + Alarm.Columns.ALARM_TIME + cs +
                Alarm.Columns.ENABLED + cs + Alarm.Columns.VIBRATE + cs +
                Alarm.Columns.MESSAGE + cs + Alarm.Columns.ALERT + ") VALUES ";
        db.execSQL(insertMe + "(8, 30, 31, 0, 0, 1, '', '');");
        db.execSQL(insertMe + "(9, 00, 96, 0, 0, 1, '', '');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,
            int currentVersion) {
        if (Log.LOGV) Log.v(
                "Upgrading alarms database from version " +
                oldVersion + " to " + currentVersion +
                ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    Uri commonInsert(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long rowId = -1;
        try {
            // Check if we are trying to re-use an existing id.
            Object value = values.get(Alarm.Columns._ID);
            if (value != null) {
                int id = (Integer) value;
                if (id > -1) {
                    final Cursor cursor = db
                            .query(TABLE_NAME, new String[]{Alarm.Columns._ID},
                                    Alarm.Columns._ID + " = ?",
                                    new String[]{id + ""}, null, null, null);
                    if (cursor.moveToFirst()) {
                        // Record exists. Remove the id so sqlite can generate a new one.
                        values.putNull(Alarm.Columns._ID);
                    }
                }
            }

            rowId = db.insert(TABLE_NAME, Alarm.Columns.MESSAGE, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (rowId < 0) {
            throw new SQLException("Failed to insert row");
        }
        if (Log.LOGV) Log.v("Added alarm rowId = " + rowId);

        return ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, rowId);
    }
}
