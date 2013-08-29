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
import android.content.CursorLoader;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.deskclock.Log;
import com.android.deskclock.R;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public final class Alarm implements Parcelable, ClockContract.AlarmsColumns {
    /**
     * Alarms start with an invalid id when it hasn't been saved to the database.
     */
    public static final long INVALID_ID = -1;

    /**
     * This string is used to indicate a silent alarm in the db.
     */
    public static final String ALARM_ALERT_SILENT = "silent";

    /**
     * The default sort order for this table
     */
    private static final String DEFAULT_SORT_ORDER =
            HOUR + ", " +
            MINUTES + " ASC" + ", " +
            _ID + " DESC";

    private static final String[] QUERY_COLUMNS = {
            _ID,
            HOUR,
            MINUTES,
            DAYS_OF_WEEK,
            ALARM_TIME,
            ENABLED,
            VIBRATE,
            MESSAGE,
            ALERT,
            DELETE_AFTER_USE
    };

    /**
     * These save calls to cursor.getColumnIndexOrThrow()
     * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
     */
    private static final int ID_INDEX = 0;
    private static final int HOUR_INDEX = 1;
    private static final int MINUTES_INDEX = 2;
    private static final int DAYS_OF_WEEK_INDEX = 3;
    @Deprecated //ALARM_TIME is not used anymore
    private static final int ALARM_TIME_INDEX = 4;
    private static final int ENABLED_INDEX = 5;
    private static final int VIBRATE_INDEX = 6;
    private static final int MESSAGE_INDEX = 7;
    private static final int ALERT_INDEX = 8;
    private static final int DELETE_AFTER_USE_INDEX = 9;

    private static final int COLUMN_COUNT = DELETE_AFTER_USE_INDEX + 1;

    public static ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(COLUMN_COUNT);
        if (alarm.id != INVALID_ID) {
            values.put(ClockContract.AlarmsColumns._ID, alarm.id);
        }

        values.put(ENABLED, alarm.enabled ? 1 : 0);
        values.put(HOUR, alarm.hour);
        values.put(MINUTES, alarm.minutes);
        values.put(ALARM_TIME, 0);
        values.put(DAYS_OF_WEEK, alarm.daysOfWeek.getBitSet());
        values.put(VIBRATE, alarm.vibrate);
        values.put(MESSAGE, alarm.label);
        values.put(DELETE_AFTER_USE, alarm.deleteAfterUse);

        // A null alert Uri indicates a silent alarm.
        values.put(ALERT, alarm.alert == null ? ALARM_ALERT_SILENT : alarm.alert.toString());

        return values;
    }

    public static long getId(Uri contentUri) {
        return ContentUris.parseId(contentUri);
    }

    /**
     * Get alarm cursor loader for all alarms.
     *
     * @param context to query the database.
     * @return cursor loader with all the alarms.
     */
    public static CursorLoader getAlarmsCursorLoader(Context context) {
        return new CursorLoader(context, ClockContract.AlarmsColumns.CONTENT_URI,
                QUERY_COLUMNS, null, null, DEFAULT_SORT_ORDER);
    }

    /**
     * Get alarm by id.
     *
     * @param contentResolver to perform the query on.
     * @param alarmId for the desired alarm.
     * @return alarm if found, null otherwise
     */
    public static Alarm getAlarm(ContentResolver contentResolver, long alarmId) {
        Cursor cursor = contentResolver.query(ContentUris.withAppendedId(CONTENT_URI, alarmId),
                QUERY_COLUMNS, null, null, null);
        Alarm result = null;
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                result = new Alarm(cursor);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    /**
     * Get all alarms given conditions.
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
    public static List<Alarm> getAlarms(ContentResolver contentResolver,
            String selection, String ... selectionArgs) {
        Cursor cursor  = contentResolver.query(CONTENT_URI, QUERY_COLUMNS,
                selection, selectionArgs, null);
        List<Alarm> result = new LinkedList<Alarm>();
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    result.add(new Alarm(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public static final Parcelable.Creator<Alarm> CREATOR
            = new Parcelable.Creator<Alarm>() {
        public Alarm createFromParcel(Parcel p) {
            return new Alarm(p);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    // Public fields
    public long id;
    public boolean enabled;
    public int hour;
    public int minutes;
    public DaysOfWeek daysOfWeek;
    public boolean vibrate;
    public String label;
    public Uri alert;
    public boolean silent;
    public boolean deleteAfterUse;

    // Creates a default alarm at the current time.
    public Alarm() {
        this(0, 0);
    }

    public Alarm(int hour, int minutes) {
        this.id = INVALID_ID;
        this.hour = hour;
        this.minutes = minutes;
        this.vibrate = true;
        this.daysOfWeek = new DaysOfWeek(0);
        this.label = "";
        this.alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        this.deleteAfterUse = false;
    }

    public Alarm(Cursor c) {
        id = c.getLong(ID_INDEX);
        enabled = c.getInt(ENABLED_INDEX) == 1;
        hour = c.getInt(HOUR_INDEX);
        minutes = c.getInt(MINUTES_INDEX);
        daysOfWeek = new DaysOfWeek(c.getInt(DAYS_OF_WEEK_INDEX));
        vibrate = c.getInt(VIBRATE_INDEX) == 1;
        label = c.getString(MESSAGE_INDEX);
        deleteAfterUse = c.getInt(DELETE_AFTER_USE_INDEX) == 1;

        String alertString = c.getString(ALERT_INDEX);
        if (ALARM_ALERT_SILENT.equals(alertString)) {
            if (Log.LOGV) {
                Log.v("Alarm is marked as silent");
            }
            silent = true;
        } else {
            if (alertString != null && alertString.length() != 0) {
                alert = Uri.parse(alertString);
            }

            // If the database alert is null or it failed to parse, use the
            // default alert.
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_ALARM);
            }
        }
    }

    Alarm(Parcel p) {
        id = p.readLong();
        enabled = p.readInt() == 1;
        hour = p.readInt();
        minutes = p.readInt();
        daysOfWeek = new DaysOfWeek(p.readInt());
        vibrate = p.readInt() == 1;
        label = p.readString();
        alert = (Uri) p.readParcelable(null);
        silent = p.readInt() == 1;
        deleteAfterUse = p.readInt() == 1;
    }

    public String getLabelOrDefault(Context context) {
        if (label == null || label.length() == 0) {
            return context.getString(R.string.default_label);
        }
        return label;
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeLong(id);
        p.writeInt(enabled ? 1 : 0);
        p.writeInt(hour);
        p.writeInt(minutes);
        p.writeInt(daysOfWeek.getBitSet());
        p.writeInt(vibrate ? 1 : 0);
        p.writeString(label);
        p.writeParcelable(alert, flags);
        p.writeInt(silent ? 1 : 0);
        p.writeInt(deleteAfterUse ? 1 : 0);
    }

    public int describeContents() {
        return 0;
    }

    public long calculateAlarmTime() {
        Calendar c = calculateAlarmCalendar();
        return c.getTimeInMillis();
    }

    public Calendar calculateAlarmCalendar() {
        // start with now
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // if alarm is behind current time, advance one day
        if ((hour < nowHour  || (hour == nowHour && minutes <= nowMinute))) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minutes);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int addDays = daysOfWeek.calculateDaysToNextAlarm(c);
        if (addDays > 0) {
            c.add(Calendar.DAY_OF_WEEK, addDays);
        }
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Alarm)) return false;
        final Alarm other = (Alarm) o;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode();
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "alert=" + alert +
                ", id=" + id +
                ", enabled=" + enabled +
                ", hour=" + hour +
                ", minutes=" + minutes +
                ", daysOfWeek=" + daysOfWeek +
                ", vibrate=" + vibrate +
                ", label='" + label + '\'' +
                ", silent=" + silent +
                ", deleteAfterUse=" + deleteAfterUse +
                '}';
    }
}
