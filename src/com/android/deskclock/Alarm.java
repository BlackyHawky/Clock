/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;

public final class Alarm implements Parcelable {
    //////////////////////////////
    // Parcelable apis
    //////////////////////////////
    public static final Parcelable.Creator<Alarm> CREATOR
            = new Parcelable.Creator<Alarm>() {
                public Alarm createFromParcel(Parcel p) {
                    return new Alarm(p);
                }

                public Alarm[] newArray(int size) {
                    return new Alarm[size];
                }
            };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(id);
        p.writeInt(enabled ? 1 : 0);
        p.writeInt(hour);
        p.writeInt(minutes);
        p.writeInt(daysOfWeek.getBitSet());
        // We don't need the alarmTime field anymore, but write 0 to be backwards compatible
        p.writeLong(0);
        p.writeInt(vibrate ? 1 : 0);
        p.writeString(label);
        p.writeParcelable(alert, flags);
        p.writeInt(silent ? 1 : 0);
    }

    //////////////////////////////
    // end Parcelable apis
    //////////////////////////////

    //////////////////////////////
    // Column definitions
    //////////////////////////////
    public static class Columns implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
                Uri.parse("content://com.android.deskclock/alarm");

        /**
         * Hour in 24-hour localtime 0 - 23.
         * <P>Type: INTEGER</P>
         */
        public static final String HOUR = "hour";

        /**
         * Minutes in localtime 0 - 59
         * <P>Type: INTEGER</P>
         */
        public static final String MINUTES = "minutes";

        /**
         * Days of week coded as integer
         * <P>Type: INTEGER</P>
         */
        public static final String DAYS_OF_WEEK = "daysofweek";

        /**
         * Alarm time in UTC milliseconds from the epoch.
         * <P>Type: INTEGER</P>
         */
        @Deprecated // Calculate this from the other fields
        public static final String ALARM_TIME = "alarmtime";

        /**
         * True if alarm is active
         * <P>Type: BOOLEAN</P>
         */
        public static final String ENABLED = "enabled";

        /**
         * True if alarm should vibrate
         * <P>Type: BOOLEAN</P>
         */
        public static final String VIBRATE = "vibrate";

        /**
         * Message to show when alarm triggers
         * Note: not currently used
         * <P>Type: STRING</P>
         */
        public static final String MESSAGE = "message";

        /**
         * Audio alert to play when alarm triggers
         * <P>Type: STRING</P>
         */
        public static final String ALERT = "alert";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER =
                HOUR + ", " + MINUTES + " ASC" + ", " + _ID + " DESC";

        // Used when filtering enabled alarms.
        public static final String WHERE_ENABLED = ENABLED + "=1";

        static final String[] ALARM_QUERY_COLUMNS = {
            _ID, HOUR, MINUTES, DAYS_OF_WEEK, ALARM_TIME,
            ENABLED, VIBRATE, MESSAGE, ALERT };

        /**
         * These save calls to cursor.getColumnIndexOrThrow()
         * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
         */
        public static final int ALARM_ID_INDEX = 0;
        public static final int ALARM_HOUR_INDEX = 1;
        public static final int ALARM_MINUTES_INDEX = 2;
        public static final int ALARM_DAYS_OF_WEEK_INDEX = 3;
        @Deprecated public static final int ALARM_TIME_INDEX = 4;
        public static final int ALARM_ENABLED_INDEX = 5;
        public static final int ALARM_VIBRATE_INDEX = 6;
        public static final int ALARM_MESSAGE_INDEX = 7;
        public static final int ALARM_ALERT_INDEX = 8;
    }
    //////////////////////////////
    // End column definitions
    //////////////////////////////

    // Public fields
    public int        id;
    public boolean    enabled;
    public int        hour;
    public int        minutes;
    public DaysOfWeek daysOfWeek;
    public boolean    vibrate;
    public String     label;
    public Uri        alert;
    public boolean    silent;

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
                '}';
    }

    public Alarm(Cursor c) {
        id = c.getInt(Columns.ALARM_ID_INDEX);
        enabled = c.getInt(Columns.ALARM_ENABLED_INDEX) == 1;
        hour = c.getInt(Columns.ALARM_HOUR_INDEX);
        minutes = c.getInt(Columns.ALARM_MINUTES_INDEX);
        daysOfWeek = new DaysOfWeek(c.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX));
        vibrate = c.getInt(Columns.ALARM_VIBRATE_INDEX) == 1;
        label = c.getString(Columns.ALARM_MESSAGE_INDEX);
        String alertString = c.getString(Columns.ALARM_ALERT_INDEX);
        if (Alarms.ALARM_ALERT_SILENT.equals(alertString)) {
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

    public Alarm(Parcel p) {
        id = p.readInt();
        enabled = p.readInt() == 1;
        hour = p.readInt();
        minutes = p.readInt();
        daysOfWeek = new DaysOfWeek(p.readInt());
        // Don't need the alarmTime field anymore, but do a readLong to be backwards compatible
        p.readLong();
        vibrate = p.readInt() == 1;
        label = p.readString();
        alert = (Uri) p.readParcelable(null);
        silent = p.readInt() == 1;
    }

    // Creates a default alarm at the current time.
    public Alarm() {
        this(0, 0);
    }

    public Alarm(int hour, int minutes) {
        this.id = -1;
        this.hour = hour;
        this.minutes = minutes;
        this.vibrate = true;
        this.daysOfWeek = new DaysOfWeek(0);
        this.label = "";
        this.alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    }

    public String getLabelOrDefault(Context context) {
        if (label == null || label.length() == 0) {
            return context.getString(R.string.default_label);
        }
        return label;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Alarm)) return false;
        final Alarm other = (Alarm) o;
        return id == other.id;
    }


    public long calculateAlarmTime() {
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
        return c.getTimeInMillis();
    }

    /*
     * Days of week code as a single int.
     * 0x00: no day
     * 0x01: Monday
     * 0x02: Tuesday
     * 0x04: Wednesday
     * 0x08: Thursday
     * 0x10: Friday
     * 0x20: Saturday
     * 0x40: Sunday
     */
    public static final class DaysOfWeek {
        // Number if days in the week.
        public static final int DAYS_IN_A_WEEK = 7;

        // Value when all days are set
        public static final int ALL_DAYS_SET = 0x7f;

        // Value when no days are set
        public static final int NO_DAYS_SET = 0;

        /**
         * Need to have monday start at index 0 to be backwards compatible. This converts
         * Calendar.DAY_OF_WEEK constants to our internal bit structure.
         */
        private static int convertDayToBitIndex(int day) {
            return (day + 5) % DAYS_IN_A_WEEK;
        }

        /**
         * Need to have monday start at index 0 to be backwards compatible. This converts
         * our bit structure to Calendar.DAY_OF_WEEK constant value.
         */
        private static int convertBitIndexToDay(int bitIndex) {
            return (bitIndex + 1) % DAYS_IN_A_WEEK + 1;
        }

        // Bitmask of all repeating days
        private int mBitSet;

        public DaysOfWeek(int bitSet) {
            mBitSet = bitSet;
        }

        public String toString(Context context, boolean showNever) {
            return toString(context, showNever, false);
        }

        public String toAccessibilityString(Context context) {
            return toString(context, false, true);
        }

        private String toString(Context context, boolean showNever, boolean forAccessibility) {
            StringBuilder ret = new StringBuilder();

            // no days
            if (mBitSet == NO_DAYS_SET) {
                return showNever ? context.getText(R.string.never).toString() : "";
            }

            // every day
            if (mBitSet == ALL_DAYS_SET) {
                return context.getText(R.string.every_day).toString();
            }

            // count selected days
            int dayCount = 0;
            int bitSet = mBitSet;
            while (bitSet > 0) {
                if ((bitSet & 1) == 1) dayCount++;
                bitSet >>= 1;
            }

            // short or long form?
            DateFormatSymbols dfs = new DateFormatSymbols();
            String[] dayList = (forAccessibility || dayCount <= 1) ?
                    dfs.getWeekdays() :
                    dfs.getShortWeekdays();

            // selected days
            for (int bitIndex = 0; bitIndex < DAYS_IN_A_WEEK; bitIndex++) {
                if ((mBitSet & (1 << bitIndex)) != 0) {
                    ret.append(dayList[convertBitIndexToDay(bitIndex)]);
                    dayCount -= 1;
                    if (dayCount > 0) ret.append(context.getText(R.string.day_concat));
                }
            }
            return ret.toString();
        }

        /**
         * Enables or disable certain days of the week.
         *
         * @param daysOfWeek Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, etc.
         */
        public void setDaysOfWeek(boolean value, int ... daysOfWeek) {
            for (int day : daysOfWeek) {
                setBit(convertDayToBitIndex(day), value);
            }
        }

        private boolean isBitEnabled(int bitIndex) {
            return ((mBitSet & (1 << bitIndex)) > 0);
        }

        private void setBit(int bitIndex, boolean set) {
            if (set) {
                mBitSet |= (1 << bitIndex);
            } else {
                mBitSet &= ~(1 << bitIndex);
            }
        }

        public void setBitSet(int bitSet) {
            mBitSet = bitSet;
        }

        public int getBitSet() {
            return mBitSet;
        }

        public HashSet<Integer> getSetDays() {
            final HashSet<Integer> set = new HashSet<Integer>();
            for (int bitIndex = 0; bitIndex < DAYS_IN_A_WEEK; bitIndex++) {
                if (isBitEnabled(bitIndex)) {
                    set.add(convertBitIndexToDay(bitIndex));
                }
            }
            return set;
        }

        public boolean isRepeating() {
            return mBitSet != NO_DAYS_SET;
        }

        /**
         * Returns number of days from today until next alarm.
         *
         * @param current must be set to today
         */
        public int calculateDaysToNextAlarm(Calendar current) {
            if (!isRepeating()) {
                return -1;
            }

            int dayCount = 0;
            int currentDayBit = convertDayToBitIndex(current.get(Calendar.DAY_OF_WEEK));
            for (; dayCount < DAYS_IN_A_WEEK; dayCount++) {
                int nextAlarmBit = (currentDayBit + dayCount) % DAYS_IN_A_WEEK;
                if (isBitEnabled(nextAlarmBit)) {
                    break;
                }
            }
            return dayCount;
        }

        public void clearAllDays() {
            mBitSet = 0;
        }

        @Override
        public String toString() {
            return "DaysOfWeek{" +
                    "mBitSet=" + mBitSet +
                    '}';
        }
    }
}
