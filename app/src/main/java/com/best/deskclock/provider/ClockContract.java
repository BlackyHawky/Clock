/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import com.best.deskclock.BuildConfig;

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
    private ClockContract() {
    }

    /**
     * Constants for tables with AlarmSettings.
     */
    private interface AlarmSettingColumns extends BaseColumns {

        /**
         * True if alarm should vibrate
         * <p>Type: BOOLEAN</p>
         */
        String VIBRATE = "vibrate";

        /**
         * True if flash should turn on
         * <p>Type: BOOLEAN</p>
         */
        String FLASH = "flash";

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
         * Alarm auto silence duration.
         * <p>Type: INTEGER</p>
         */
        String AUTO_SILENCE_DURATION = "autoSilenceDuration";

        /**
         * Alarm snooze duration.
         * <p>Type: INTEGER</p>
         */
        String SNOOZE_DURATION = "snoozeDuration";

        /**
         * Alarm crescendo duration.
         * <p>Type: INTEGER</p>
         */
        String CRESCENDO_DURATION = "crescendoDuration";

        /**
         * Alarm crescendo duration.
         * <p>Type: INTEGER</p>
         */
        String ALARM_VOLUME = "alarmVolume";
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
        Uri ALARMS_WITH_INSTANCES_URI = Uri.parse("content://" + AUTHORITY + "/alarms_with_instances");

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
         * <p>
         * {@link com.best.deskclock.data.Weekdays}
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
         * <p>
         * Can transitions to:
         * NOTIFICATION_STATE
         */
        int SILENT_STATE = 0;

        /**
         * Alarm state to show alarm notification.
         * <p>
         * Can transitions to:
         * DISMISSED_STATE
         * FIRED_STATE
         */
        int NOTIFICATION_STATE = 1;

        /**
         * Alarm state when alarm is in snooze.
         * <p>
         * Can transitions to:
         * DISMISSED_STATE
         * FIRED_STATE
         */
        int SNOOZE_STATE = 2;

        /**
         * Alarm state when alarm is being fired.
         * <p>
         * Can transitions to:
         * DISMISSED_STATE
         * SNOOZED_STATE
         * MISSED_STATE
         */
        int FIRED_STATE = 3;

        /**
         * Alarm state when alarm has been missed.
         * <p>
         * Can transitions to:
         * DISMISSED_STATE
         */
        int MISSED_STATE = 4;

        /**
         * Alarm state when alarm is done.
         */
        int DISMISSED_STATE = 5;

        /**
         * Alarm state when alarm has been dismissed before its intended firing time.
         */
        int PREDISMISSED_STATE = 6;

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
