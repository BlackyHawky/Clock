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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;

import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.data.DataModel;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class AlarmInstance implements ClockContract.InstancesColumns {
    /**
     * Offset from alarm time to show low priority notification
     */
    public static final int LOW_NOTIFICATION_HOUR_OFFSET = -2;

    /**
     * Offset from alarm time to show high priority notification
     */
    public static final int HIGH_NOTIFICATION_MINUTE_OFFSET = -30;

    /**
     * Offset from alarm time to stop showing missed notification.
     */
    private static final int MISSED_TIME_TO_LIVE_HOUR_OFFSET = 12;

    /**
     * AlarmInstances start with an invalid id when it hasn't been saved to the database.
     */
    public static final long INVALID_ID = -1;

    private static final String[] QUERY_COLUMNS = {
            _ID,
            YEAR,
            MONTH,
            DAY,
            HOUR,
            MINUTES,
            LABEL,
            VIBRATE,
            RINGTONE,
            ALARM_ID,
            ALARM_STATE,
            INCREASING_VOLUME
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
    private static final int LABEL_INDEX = 6;
    private static final int VIBRATE_INDEX = 7;
    private static final int RINGTONE_INDEX = 8;
    private static final int ALARM_ID_INDEX = 9;
    private static final int ALARM_STATE_INDEX = 10;
    private static final int INCREASING_VOLUME_INDEX = 11;

    private static final int COLUMN_COUNT = INCREASING_VOLUME_INDEX + 1;

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
        values.put(LABEL, instance.mLabel);
        values.put(VIBRATE, instance.mVibrate ? 1 : 0);
        if (instance.mRingtone == null) {
            // We want to put null in the database, so we'll be able
            // to pick up on changes to the default alarm
            values.putNull(RINGTONE);
        } else {
            values.put(RINGTONE, instance.mRingtone.toString());
        }
        values.put(ALARM_ID, instance.mAlarmId);
        values.put(ALARM_STATE, instance.mAlarmState);
        values.put(INCREASING_VOLUME, instance.mIncreasingVolume ? 1 : 0);

        return values;
    }

    public static Intent createIntent(String action, long instanceId) {
        return new Intent(action).setData(getContentUri(instanceId));
    }

    public static Intent createIntent(Context context, Class<?> cls, long instanceId) {
        return new Intent(context, cls).setData(getContentUri(instanceId));
    }

    public static long getId(Uri contentUri) {
        return ContentUris.parseId(contentUri);
    }

    /**
     * @return the {@link Uri} identifying the alarm instance
     */
    public static Uri getContentUri(long instanceId) {
        return ContentUris.withAppendedId(CONTENT_URI, instanceId);
    }

    /**
     * Get alarm instance from instanceId.
     *
     * @param cr provides access to the content model
     * @param instanceId for the desired instance.
     * @return instance if found, null otherwise
     */
    public static AlarmInstance getInstance(ContentResolver cr, long instanceId) {
        try (Cursor cursor = cr.query(getContentUri(instanceId), QUERY_COLUMNS, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return new AlarmInstance(cursor, false /* joinedTable */);
            }
        }

        return null;
    }

    /**
     * Get alarm instance for the {@code contentUri}.
     *
     * @param cr provides access to the content model
     * @param contentUri the {@link #getContentUri deeplink} for the desired instance
     * @return instance if found, null otherwise
     */
    public static AlarmInstance getInstance(ContentResolver cr, Uri contentUri) {
        final long instanceId = ContentUris.parseId(contentUri);
        return getInstance(cr, instanceId);
    }

    /**
     * Get an alarm instances by alarmId.
     *
     * @param contentResolver provides access to the content model
     * @param alarmId of instances desired.
     * @return list of alarms instances that are owned by alarmId.
     */
    public static List<AlarmInstance> getInstancesByAlarmId(ContentResolver contentResolver,
            long alarmId) {
        return getInstances(contentResolver, ALARM_ID + "=" + alarmId);
    }

    /**
     * Get the next instance of an alarm given its alarmId
     * @param contentResolver provides access to the content model
     * @param alarmId of instance desired
     * @return the next instance of an alarm by alarmId.
     */
    public static AlarmInstance getNextUpcomingInstanceByAlarmId(ContentResolver contentResolver,
                                                                 long alarmId) {
        final List<AlarmInstance> alarmInstances = getInstancesByAlarmId(contentResolver, alarmId);
        if (alarmInstances.isEmpty()) {
            return null;
        }
        AlarmInstance nextAlarmInstance = alarmInstances.get(0);
        for (AlarmInstance instance : alarmInstances) {
            if (instance.getAlarmTime().before(nextAlarmInstance.getAlarmTime())) {
                nextAlarmInstance = instance;
            }
        }
        return nextAlarmInstance;
    }

    /**
     * Get alarm instance by id and state.
     */
    public static List<AlarmInstance> getInstancesByInstanceIdAndState(
            ContentResolver contentResolver, long alarmInstanceId, int state) {
        return getInstances(contentResolver, _ID + "=" + alarmInstanceId + " AND " + ALARM_STATE +
                "=" + state);
    }

    /**
     * Get alarm instances in the specified state.
     */
    public static List<AlarmInstance> getInstancesByState(
            ContentResolver contentResolver, int state) {
        return getInstances(contentResolver, ALARM_STATE + "=" + state);
    }

    /**
     * Get a list of instances given selection.
     *
     * @param cr provides access to the content model
     * @param selection A filter declaring which rows to return, formatted as an
     *         SQL WHERE clause (excluding the WHERE itself). Passing null will
     *         return all rows for the given URI.
     * @param selectionArgs You may include ?s in selection, which will be
     *         replaced by the values from selectionArgs, in the order that they
     *         appear in the selection. The values will be bound as Strings.
     * @return list of alarms matching where clause or empty list if none found.
     */
    public static List<AlarmInstance> getInstances(ContentResolver cr, String selection,
                                                   String... selectionArgs) {
        final List<AlarmInstance> result = new LinkedList<>();
        try (Cursor cursor = cr.query(CONTENT_URI, QUERY_COLUMNS, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    result.add(new AlarmInstance(cursor, false /* joinedTable */));
                } while (cursor.moveToNext());
            }
        }

        return result;
    }

    public static AlarmInstance addInstance(ContentResolver contentResolver,
            AlarmInstance instance) {
        // Make sure we are not adding a duplicate instances. This is not a
        // fix and should never happen. This is only a safe guard against bad code, and you
        // should fix the root issue if you see the error message.
        String dupSelector = AlarmInstance.ALARM_ID + " = " + instance.mAlarmId;
        for (AlarmInstance otherInstances : getInstances(contentResolver, dupSelector)) {
            if (otherInstances.getAlarmTime().equals(instance.getAlarmTime())) {
                LogUtils.i("Detected duplicate instance in DB. Updating " + otherInstances + " to "
                        + instance);
                // Copy over the new instance values and update the db
                instance.mId = otherInstances.mId;
                updateInstance(contentResolver, instance);
                return instance;
            }
        }

        ContentValues values = createContentValues(instance);
        Uri uri = contentResolver.insert(CONTENT_URI, values);
        instance.mId = getId(uri);
        return instance;
    }

    public static boolean updateInstance(ContentResolver contentResolver, AlarmInstance instance) {
        if (instance.mId == INVALID_ID) return false;
        ContentValues values = createContentValues(instance);
        long rowsUpdated = contentResolver.update(getContentUri(instance.mId), values, null, null);
        return rowsUpdated == 1;
    }

    public static boolean deleteInstance(ContentResolver contentResolver, long instanceId) {
        if (instanceId == INVALID_ID) return false;
        int deletedRows = contentResolver.delete(getContentUri(instanceId), "", null);
        return deletedRows == 1;
    }

    public static void deleteOtherInstances(Context context, ContentResolver contentResolver,
            long alarmId, long instanceId) {
        final List<AlarmInstance> instances = getInstancesByAlarmId(contentResolver, alarmId);
        for (AlarmInstance instance : instances) {
            if (instance.mId != instanceId) {
                AlarmStateManager.unregisterInstance(context, instance);
                deleteInstance(contentResolver, instance.mId);
            }
        }
    }

    // Public fields
    public long mId;
    public int mYear;
    public int mMonth;
    public int mDay;
    public int mHour;
    public int mMinute;
    public String mLabel;
    public boolean mVibrate;
    public Uri mRingtone;
    public Long mAlarmId;
    public int mAlarmState;
    public boolean mIncreasingVolume;

    public AlarmInstance(Calendar calendar, Long alarmId) {
        this(calendar);
        mAlarmId = alarmId;
    }

    public AlarmInstance(Calendar calendar) {
        mId = INVALID_ID;
        setAlarmTime(calendar);
        mLabel = "";
        mVibrate = false;
        mRingtone = null;
        mAlarmState = SILENT_STATE;
        mIncreasingVolume = false;
    }

    public AlarmInstance(AlarmInstance instance) {
         this.mId = instance.mId;
         this.mYear = instance.mYear;
         this.mMonth = instance.mMonth;
         this.mDay = instance.mDay;
         this.mHour = instance.mHour;
         this.mMinute = instance.mMinute;
         this.mLabel = instance.mLabel;
         this.mVibrate = instance.mVibrate;
         this.mRingtone = instance.mRingtone;
         this.mAlarmId = instance.mAlarmId;
         this.mAlarmState = instance.mAlarmState;
         this.mIncreasingVolume = instance.mIncreasingVolume;
    }

    public AlarmInstance(Cursor c, boolean joinedTable) {
        if (joinedTable) {
            mId = c.getLong(Alarm.INSTANCE_ID_INDEX);
            mYear = c.getInt(Alarm.INSTANCE_YEAR_INDEX);
            mMonth = c.getInt(Alarm.INSTANCE_MONTH_INDEX);
            mDay = c.getInt(Alarm.INSTANCE_DAY_INDEX);
            mHour = c.getInt(Alarm.INSTANCE_HOUR_INDEX);
            mMinute = c.getInt(Alarm.INSTANCE_MINUTE_INDEX);
            mLabel = c.getString(Alarm.INSTANCE_LABEL_INDEX);
            mVibrate = c.getInt(Alarm.INSTANCE_VIBRATE_INDEX) == 1;
        } else {
            mId = c.getLong(ID_INDEX);
            mYear = c.getInt(YEAR_INDEX);
            mMonth = c.getInt(MONTH_INDEX);
            mDay = c.getInt(DAY_INDEX);
            mHour = c.getInt(HOUR_INDEX);
            mMinute = c.getInt(MINUTES_INDEX);
            mLabel = c.getString(LABEL_INDEX);
            mVibrate = c.getInt(VIBRATE_INDEX) == 1;
        }
        if (c.isNull(RINGTONE_INDEX)) {
            // Should we be saving this with the current ringtone or leave it null
            // so it changes when user changes default ringtone?
            mRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            mRingtone = Uri.parse(c.getString(RINGTONE_INDEX));
        }

        if (!c.isNull(ALARM_ID_INDEX)) {
            mAlarmId = c.getLong(ALARM_ID_INDEX);
        }
        mAlarmState = c.getInt(ALARM_STATE_INDEX);
        mIncreasingVolume = c.getInt(INCREASING_VOLUME_INDEX) == 1;
    }

    /**
     * @return the deeplink that identifies this alarm instance
     */
    public Uri getContentUri() {
        return getContentUri(mId);
    }

    public String getLabelOrDefault(Context context) {
        return mLabel.isEmpty() ? context.getString(R.string.default_label) : mLabel;
    }

    public void setAlarmTime(Calendar calendar) {
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH);
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
    }

    /**
     * Return the time when a alarm should fire.
     *
     * @return the time
     */
    public Calendar getAlarmTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, mYear);
        calendar.set(Calendar.MONTH, mMonth);
        calendar.set(Calendar.DAY_OF_MONTH, mDay);
        calendar.set(Calendar.HOUR_OF_DAY, mHour);
        calendar.set(Calendar.MINUTE, mMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    /**
     * Return the time when a low priority notification should be shown.
     *
     * @return the time
     */
    public Calendar getLowNotificationTime() {
        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.HOUR_OF_DAY, LOW_NOTIFICATION_HOUR_OFFSET);
        return calendar;
    }

    /**
     * Return the time when a high priority notification should be shown.
     *
     * @return the time
     */
    public Calendar getHighNotificationTime() {
        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.MINUTE, HIGH_NOTIFICATION_MINUTE_OFFSET);
        return calendar;
    }

    /**
     * Return the time when a missed notification should be removed.
     *
     * @return the time
     */
    public Calendar getMissedTimeToLive() {
        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.HOUR, MISSED_TIME_TO_LIVE_HOUR_OFFSET);
        return calendar;
    }

    /**
     * Return the time when the alarm should stop firing and be marked as missed.
     *
     * @return the time when alarm should be silence, or null if never
     */
    public Calendar getTimeout() {
        final int timeoutMinutes = DataModel.getDataModel().getAlarmTimeout();

        // Alarm silence has been set to "None"
        if (timeoutMinutes < 0) {
            return null;
        }

        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.MINUTE, timeoutMinutes);
        return calendar;
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
                ", mLabel=" + mLabel +
                ", mVibrate=" + mVibrate +
                ", mRingtone=" + mRingtone +
                ", mAlarmId=" + mAlarmId +
                ", mAlarmState=" + mAlarmState +
                ", mIncreasingVolume=" + mIncreasingVolume +
                '}';
    }
}
