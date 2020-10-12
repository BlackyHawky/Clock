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

import android.net.Uri;
import android.provider.BaseColumns;

import com.android.deskclock.BuildConfig;

/**
 * <p>
 * The contract between the clock provider and desk clock. Contains
 * definitions for the supported URIs and data columns.
 * </p>
 * <h3>Overview</h3>
 * <p>
 * ClockContract defines the data model of clock related information.
 * This data is stored in a number of tables:
 * </p>
 * <ul>
 * <li>The {@link AlarmsColumns} table holds the user created alarms</li>
 * <li>The {@link InstancesColumns} table holds the current state of each
 * alarm in the AlarmsColumn table.
 * </li>
 * </ul>
 */
public final class ClockContract {
    /**
     * This authority is used for writing to or querying from the clock
     * provider.
     */
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID;

    /**
     * This utility class cannot be instantiated
     */
    private ClockContract() {}

    /**
     * Constants for tables with AlarmSettings.
     */
    private interface AlarmSettingColumns extends BaseColumns {
        /**
         * This string is used to indicate no ringtone.
         */
        Uri NO_RINGTONE_URI = Uri.EMPTY;

        /**
         * This string is used to indicate no ringtone.
         */
        String NO_RINGTONE = NO_RINGTONE_URI.toString();

        /**
         * True if alarm should vibrate
         * <p>Type: BOOLEAN</p>
         */
        String VIBRATE = "vibrate";

        /**
         * Alarm label.
         *
         * <p>Type: STRING</p>
         */
        String LABEL = "label";

        /**
         * Audio alert to play when alarm triggers. Null entry
         * means use system default and entry that equal
         * Uri.EMPTY.toString() means no ringtone.
         *
         * <p>Type: STRING</p>
         */
        String RINGTONE = "ringtone";

        /**
         * True if alarm should start off quiet and slowly increase volume
         * <P>Type: BOOLEAN</P>
         */
        public static final String INCREASING_VOLUME = "incvol";
    }

    /**
     * Constants for the Alarms table, which contains the user created alarms.
     */
    protected interface AlarmsColumns extends AlarmSettingColumns, BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/alarms");

        /**
         * The content:// style URL for the alarms with instance tables, which is used to get the
         * next firing instance and the current state of an alarm.
         */
        Uri ALARMS_WITH_INSTANCES_URI = Uri.parse("content://" + AUTHORITY
                + "/alarms_with_instances");

        /**
         * Hour in 24-hour localtime 0 - 23.
         * <p>Type: INTEGER</p>
         */
        String HOUR = "hour";

        /**
         * Minutes in localtime 0 - 59.
         * <p>Type: INTEGER</p>
         */
        String MINUTES = "minutes";

        /**
         * Days of the week encoded as a bit set.
         * <p>Type: INTEGER</p>
         *
         * {@link com.android.deskclock.data.Weekdays}
         */
        String DAYS_OF_WEEK = "daysofweek";

        /**
         * True if alarm is active.
         * <p>Type: BOOLEAN</p>
         */
        String ENABLED = "enabled";

        /**
         * Determine if alarm is deleted after it has been used.
         * <p>Type: INTEGER</p>
         */
        String DELETE_AFTER_USE = "delete_after_use";
    }

    /**
     * Constants for the Instance table, which contains the state of each alarm.
     */
    protected interface InstancesColumns extends AlarmSettingColumns, BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/instances");

        /**
         * Alarm state when to show no notification.
         *
         * Can transitions to:
         * LOW_NOTIFICATION_STATE
         */
        int SILENT_STATE = 0;

        /**
         * Alarm state to show low priority alarm notification.
         *
         * Can transitions to:
         * HIDE_NOTIFICATION_STATE
         * HIGH_NOTIFICATION_STATE
         * DISMISSED_STATE
         */
        int LOW_NOTIFICATION_STATE = 1;

        /**
         * Alarm state to hide low priority alarm notification.
         *
         * Can transitions to:
         * HIGH_NOTIFICATION_STATE
         */
        int HIDE_NOTIFICATION_STATE = 2;

        /**
         * Alarm state to show high priority alarm notification.
         *
         * Can transitions to:
         * DISMISSED_STATE
         * FIRED_STATE
         */
        int HIGH_NOTIFICATION_STATE = 3;

        /**
         * Alarm state when alarm is in snooze.
         *
         * Can transitions to:
         * DISMISSED_STATE
         * FIRED_STATE
         */
        int SNOOZE_STATE = 4;

        /**
         * Alarm state when alarm is being fired.
         *
         * Can transitions to:
         * DISMISSED_STATE
         * SNOOZED_STATE
         * MISSED_STATE
         */
        int FIRED_STATE = 5;

        /**
         * Alarm state when alarm has been missed.
         *
         * Can transitions to:
         * DISMISSED_STATE
         */
        int MISSED_STATE = 6;

        /**
         * Alarm state when alarm is done.
         */
        int DISMISSED_STATE = 7;

        /**
         * Alarm state when alarm has been dismissed before its intended firing time.
         */
        int PREDISMISSED_STATE = 8;

        /**
         * Alarm year.
         *
         * <p>Type: INTEGER</p>
         */
        String YEAR = "year";

        /**
         * Alarm month in year.
         *
         * <p>Type: INTEGER</p>
         */
        String MONTH = "month";

        /**
         * Alarm day in month.
         *
         * <p>Type: INTEGER</p>
         */
        String DAY = "day";

        /**
         * Alarm hour in 24-hour localtime 0 - 23.
         * <p>Type: INTEGER</p>
         */
        String HOUR = "hour";

        /**
         * Alarm minutes in localtime 0 - 59
         * <p>Type: INTEGER</p>
         */
        String MINUTES = "minutes";

        /**
         * Foreign key to Alarms table
         * <p>Type: INTEGER (long)</p>
         */
        String ALARM_ID = "alarm_id";

        /**
         * Alarm state
         * <p>Type: INTEGER</p>
         */
        String ALARM_STATE = "alarm_state";
    }
}
