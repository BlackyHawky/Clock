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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public final class AlarmInstance implements ClockContract.InstancesColumns {
    /**
     * AlarmInstances start with an invalid id when it hasn't been saved to the database.
     */
    public static final long INVALID_ID = -1;

    /**
     * Alarm state when alarm is ready to be fired.
     */
    public static final int ACTIVE_STATE = 0;

    /**
     * Alarm state when alarm is being fired.
     */
    public static final int FIRED_STATE = 1;

    /**
     * Alarm state when alarm is in snooze.
     */
    public static final int SNOOZE_STATE = 2;

    private static final String[] QUERY_COLUMNS = {
            _ID,
            YEAR,
            MONTH,
            DAY,
            HOUR,
            MINUTES,
            ALARM_ID,
            ALARM_STATE
    };

    /**
     * These save calls to cursor.getColumnIndexOrThrow()
     * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
     */
    private static final int ID_INDEX = 0;
    private static final int YEAR_INDEX = 1;
    private static final int MONTH_INDEX = 2;
    private static final int DAY_INDEX = 3;
    private static final int HOUR_INDEX = 4;
    private static final int MINUTES_INDEX = 5;
    private static final int ALARM_ID_INDEX = 6;
    private static final int ALARM_STATE_INDEX = 7;

    private static final int COLUMN_COUNT = ALARM_STATE_INDEX + 1;

    public static ContentValues createContentValues(AlarmInstance instance) {
        ContentValues values = new ContentValues(COLUMN_COUNT);
        if (instance.mId != INVALID_ID) {
            values.put(_ID, instance.mId);
        }

        values.put(YEAR, instance.mYear);
        values.put(MONTH, instance.mMonth);
        values.put(DAY, instance.mDay);
        values.put(HOUR, instance.mHour);
        values.put(MINUTES, instance.mMinute);
        values.put(ALARM_ID, instance.mAlarmId);
        values.put(ALARM_STATE, instance.mAlarmState);
        return values;
    }

    public static long getId(Uri contentUri) {
        return ContentUris.parseId(contentUri);
    }

    /**
     * Get alarm instance from instanceId.
     *
     * @param contentResolver to perform the query on.
     * @param instanceId for the desired instance.
     * @return instance if found, null otherwise
     */
    public static AlarmInstance getInstance(ContentResolver contentResolver, long instanceId) {
        Cursor cursor = contentResolver.query(ContentUris.withAppendedId(CONTENT_URI, instanceId),
                QUERY_COLUMNS, null, null, null);
        AlarmInstance result = null;
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                result = new AlarmInstance(cursor);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    /**
     * Get an alarm instance by alarmId.
     *
     * @param contentResolver to perform the query on.
     * @param alarmId for the desired instance.
     * @return instance if found, null otherwise
     */
    public static AlarmInstance getInstanceByAlarmId(ContentResolver contentResolver, long alarmId) {
        List<AlarmInstance> result = getInstances(contentResolver, ALARM_ID + "=" + alarmId);
        return !result.isEmpty() ? result.get(0) : null;
    }

    /**
     * Get a list of instances given selection.
     *
     * @param contentResolver to perform the query on.
     * @param selection A filter declaring which rows to return, formatted as an
     *         SQL WHERE clause (excluding the WHERE itself). Passing null will
     *         return all rows for the given URI.
     * @param selectionArgs You may include ?s in selection, which will be
     *         replaced by the values from selectionArgs, in the order that they
     *         appear in the selection. The values will be bound as Strings.
     * @return list of alarms matching where clause or empty list if none found.
     */
    public static List<AlarmInstance> getInstances(ContentResolver contentResolver,
            String selection, String ... selectionArgs) {
        Cursor cursor  = contentResolver.query(CONTENT_URI, QUERY_COLUMNS,
                selection, selectionArgs, null);
        List<AlarmInstance> result = new LinkedList<AlarmInstance>();
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    result.add(new AlarmInstance(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    // Public fields
    public long mId;
    public int mYear;
    public int mMonth;
    public int mDay;
    public int mHour;
    public int mMinute;
    public long mAlarmId;
    public int mAlarmState;

    // Creates a default alarm at the current time.
    public AlarmInstance(Alarm alarm) {
        //TODO: Find better way to get year and day
        this(Calendar.getInstance(), alarm.hour, alarm.minutes, alarm.id);
    }

    public AlarmInstance(Calendar calendar, int hour, int minute, long alarmId) {
        mId = INVALID_ID;
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH);
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHour = hour;
        mMinute = minute;
        mAlarmId = alarmId;
        mAlarmState = 0;
    }

    public AlarmInstance(Cursor c) {
        mId = c.getLong(ID_INDEX);
        mYear = c.getInt(YEAR_INDEX);
        mMonth = c.getInt(MONTH_INDEX);
        mDay = c.getInt(DAY_INDEX);
        mHour = c.getInt(HOUR_INDEX);
        mMinute = c.getInt(MINUTES_INDEX);
        mAlarmId = c.getLong(ALARM_ID_INDEX);
        mAlarmState = c.getInt(ALARM_STATE_INDEX);
    }

    public long calculateTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, mYear);
        calendar.set(Calendar.DAY_OF_MONTH, mDay);
        calendar.set(Calendar.HOUR, mHour);
        calendar.set(Calendar.MINUTE, mMinute);
        return calendar.getTimeInMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AlarmInstance)) return false;
        final AlarmInstance other = (AlarmInstance) o;
        return mId == other.mId;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(mId).hashCode();
    }

    @Override
    public String toString() {
        return "AlarmInstance{" +
                "mId=" + mId +
                ", mYear=" + mYear +
                ", mMonth=" + mMonth +
                ", mDay=" + mDay +
                ", mHour=" + mHour +
                ", mMinute=" + mMinute +
                ", mAlarmId=" + mAlarmId +
                ", mAlarmState=" + mAlarmState +
                '}';
    }
}
