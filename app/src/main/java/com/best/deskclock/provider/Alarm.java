/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.provider;

import static com.best.deskclock.settings.PreferencesDefaultValues.*;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ParcelCompat;
import androidx.loader.content.CursorLoader;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.RingtoneUtils;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

public final class Alarm implements Parcelable, ClockContract.AlarmsColumns {
    /**
     * Alarms start with an invalid id when it hasn't been saved to the database.
     */
    public static final long INVALID_ID = -1;

    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<>() {
        @NonNull
        public Alarm createFromParcel(@NonNull Parcel p) {
            return new Alarm(p);
        }

        @NonNull
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    /**
     * The default sort order for this table
     */
    private static final String DEFAULT_SORT_ORDER =
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + HOUR + " ASC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MINUTES + " ASC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns._ID + " DESC";

    /**
     * The default sort order for this table with enabled alarms first
     */
    private static final String DEFAULT_SORT_ORDER_WITH_ENABLED_FIRST =
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ENABLED + " DESC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + HOUR + " ASC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MINUTES + " ASC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns._ID + " DESC";

    /**
     * The sort order by descending ID to display oldest alarms last.
     */
    private static final String SORT_ORDER_BY_DESCENDING_CREATION =
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + _ID + " DESC";

    /**
     * The sort order that places enabled alarms first, then sorts alarms by descending ID
     * with the oldest last.
     */
    private static final String SORT_ORDER_BY_DESCENDING_CREATION_WITH_ENABLED_FIRST =
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ENABLED + " DESC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + _ID + " DESC";

    /**
     * The sort order by ascending ID to display oldest alarms first.
     */
    private static final String SORT_ORDER_BY_ASCENDING_CREATION =
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + _ID + " ASC";

    /**
     * The sort order that places enabled alarms first, then sorts alarms by ascending ID
     * with the oldest first.
     */
    private static final String SORT_ORDER_BY_ASCENDING_CREATION_WITH_ENABLED_FIRST =
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ENABLED + " DESC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + _ID + " ASC";

    /**
     * The sort order by ascending sort_order to display manually sorted alarms.
     */
    private static final String SORT_ORDER_MANUALLY_ASC =
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MANUAL_SORT_ORDER + " ASC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + HOUR + " ASC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MINUTES + " ASC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + _ID + " DESC";

    /**
     * The sort order that places enabled alarms first, then sorts manually.
     */
    private static final String SORT_ORDER_MANUALLY_WITH_ENABLED_FIRST =
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ENABLED + " DESC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MANUAL_SORT_ORDER + " ASC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + HOUR + " ASC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MINUTES + " ASC, " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + _ID + " DESC";

    private static final String[] QUERY_COLUMNS = {
        _ID,
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MINUTES,
        DAYS_OF_WEEK,
        ENABLED,
        VIBRATE,
        VIBRATION_PATTERN,
        FLASH,
        LABEL,
        SYNC_BY_LABEL,
        RINGTONE,
        DELETE_AFTER_USE,
        AUTO_SILENCE_DURATION,
        SNOOZE_DURATION,
        MISSED_ALARM_REPEAT_LIMIT,
        CRESCENDO_DURATION,
        ALARM_VOLUME,
        MANUAL_SORT_ORDER,
        PAUSE_START_DATE,
        PAUSE_END_DATE,
        BACKGROUND_IMAGE,
        BLUR_INTENSITY,
        MATH_HARDNESS_LEVEL
    };
    private static final String[] QUERY_ALARMS_WITH_INSTANCES_COLUMNS = {
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + _ID,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + YEAR,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MONTH,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + DAY,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + HOUR,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MINUTES,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + DAYS_OF_WEEK,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ENABLED,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + VIBRATE,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + VIBRATION_PATTERN,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + FLASH,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + LABEL,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + SYNC_BY_LABEL,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + RINGTONE,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + DELETE_AFTER_USE,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + AUTO_SILENCE_DURATION,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + SNOOZE_DURATION,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MISSED_ALARM_REPEAT_LIMIT,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + CRESCENDO_DURATION,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ALARM_VOLUME,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MANUAL_SORT_ORDER,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + PAUSE_START_DATE,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + PAUSE_END_DATE,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + BACKGROUND_IMAGE,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + BLUR_INTENSITY,
        ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MATH_HARDNESS_LEVEL,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.ALARM_STATE,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns._ID,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.YEAR,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MONTH,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.DAY,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.HOUR,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MINUTES,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.LABEL,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.SYNC_BY_LABEL,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.VIBRATE,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.VIBRATION_PATTERN,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.FLASH,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.AUTO_SILENCE_DURATION,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.SNOOZE_DURATION,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MISSED_ALARM_REPEAT_COUNT,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MISSED_ALARM_REPEAT_LIMIT,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.CRESCENDO_DURATION,
        ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.ALARM_VOLUME
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
    private static final int DAYS_OF_WEEK_INDEX = 6;
    private static final int ENABLED_INDEX = 7;
    private static final int VIBRATE_INDEX = 8;
    private static final int VIBRATION_PATTERN_INDEX = 9;
    private static final int FLASH_INDEX = 10;
    private static final int LABEL_INDEX = 11;
    private static final int SYNC_BY_LABEL_INDEX = 12;
    private static final int RINGTONE_INDEX = 13;
    private static final int DELETE_AFTER_USE_INDEX = 14;
    private static final int AUTO_SILENCE_DURATION_INDEX = 15;
    private static final int SNOOZE_DURATION_INDEX = 16;
    private static final int MISSED_ALARM_REPEAT_LIMIT_INDEX = 17;
    private static final int CRESCENDO_DURATION_INDEX = 18;
    private static final int ALARM_VOLUME_INDEX = 19;
    private static final int MANUAL_SORT_ORDER_INDEX = 20;
    private static final int PAUSE_START_DATE_INDEX = 21;
    private static final int PAUSE_END_DATE_INDEX = 22;
    private static final int BACKGROUND_IMAGE_INDEX = 23;
    private static final int BLUR_INTENSITY_INDEX = 24;
    private static final int MATH_HARDNESS_LEVEL_INDEX = 25;

    private static final int INSTANCE_STATE_INDEX = 26;
    public static final int INSTANCE_ID_INDEX = 27;
    public static final int INSTANCE_YEAR_INDEX = 28;
    public static final int INSTANCE_MONTH_INDEX = 29;
    public static final int INSTANCE_DAY_INDEX = 30;
    public static final int INSTANCE_HOUR_INDEX = 31;
    public static final int INSTANCE_MINUTE_INDEX = 32;
    public static final int INSTANCE_LABEL_INDEX = 33;
    public static final int INSTANCE_SYNC_BY_LABEL_INDEX = 34;
    public static final int INSTANCE_VIBRATE_INDEX = 35;
    public static final int INSTANCE_VIBRATION_PATTERN_INDEX = 36;
    public static final int INSTANCE_FLASH_INDEX = 37;
    public static final int INSTANCE_AUTO_SILENCE_DURATION_INDEX = 38;
    public static final int INSTANCE_SNOOZE_DURATION_INDEX = 39;
    public static final int INSTANCE_MISSED_ALARM_REPEAT_COUNT_INDEX = 40;
    public static final int INSTANCE_MISSED_ALARM_REPEAT_LIMIT_INDEX = 41;
    public static final int INSTANCE_CRESCENDO_DURATION_INDEX = 42;
    public static final int INSTANCE_ALARM_VOLUME_INDEX = 43;

    private static final int COLUMN_COUNT = MATH_HARDNESS_LEVEL_INDEX + 1;
    private static final int ALARM_JOIN_INSTANCE_COLUMN_COUNT = INSTANCE_ALARM_VOLUME_INDEX + 1;
    // Public fields
    public long id;
    public boolean enabled;
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minutes;
    public Weekdays daysOfWeek;
    public boolean vibrate;
    public String vibrationPattern;
    public boolean flash;
    public String label;
    public boolean syncByLabel;
    public Uri alert;
    public boolean deleteAfterUse;
    public int autoSilenceDuration;
    public int snoozeDuration;
    public int missedAlarmRepeatLimit;
    public int crescendoDuration;
    // Alarm volume level in steps; not a percentage
    public int alarmVolume;
    public int manualSortOrder;
    public long pauseStartDate;
    public long pauseEndDate;
    public int instanceState;
    public String backgroundImage;
    public int blurIntensity;
    public String mathHardnessLevel;

    // Creates a default alarm at the current time.
    public Alarm() {
        this(Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
            0,
            0);
    }

    public Alarm(int year, int month, int day, int hour, int minutes) {
        this.id = INVALID_ID;
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minutes = minutes;
        this.vibrate = true;
        this.vibrationPattern = DEFAULT_VIBRATION_PATTERN;
        this.flash = true;
        this.daysOfWeek = Weekdays.NONE;
        this.label = "";
        this.syncByLabel = false;
        this.alert = DataModel.getDataModel().getAlarmRingtoneUriFromSettings();
        this.deleteAfterUse = DEFAULT_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT;
        this.autoSilenceDuration = DEFAULT_AUTO_SILENCE_DURATION;
        this.snoozeDuration = DEFAULT_ALARM_SNOOZE_DURATION;
        this.missedAlarmRepeatLimit = Integer.parseInt(DEFAULT_MISSED_ALARM_REPEAT_LIMIT);
        this.crescendoDuration = DEFAULT_VOLUME_CRESCENDO_DURATION;
        this.alarmVolume = DEFAULT_ALARM_VOLUME;
        this.manualSortOrder = 0;
        this.pauseStartDate = 0;
        this.pauseEndDate = 0;
        this.backgroundImage = DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
        this.blurIntensity = DEFAULT_BLUR_INTENSITY;
        this.mathHardnessLevel = DEFAULT_MATH_HARDNESS_LEVEL;
    }

    // Used to back up/restore the alarm
    public Alarm(long id, boolean enabled, int year, int month, int day, int hour, int minutes, boolean vibrate,
                 @NonNull String vibrationPattern, boolean flash, @NonNull Weekdays daysOfWeek, @NonNull String label, boolean syncByLabel,
                 @NonNull String alert, boolean deleteAfterUse, int autoSilenceDuration, int snoozeDuration, int missedAlarmRepeatLimit,
                 int crescendoDuration, int alarmVolume, int manualSortOrder, long pauseStartDate, long pauseEndDate,
                 @NonNull String backgroundImage, int blurIntensity, @NonNull String mathHardnessLevel) {

        this.id = id;
        this.enabled = enabled;
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minutes = minutes;
        this.vibrate = vibrate;
        this.vibrationPattern = vibrationPattern;
        this.flash = flash;
        this.daysOfWeek = daysOfWeek;
        this.label = label;
        this.syncByLabel = syncByLabel;
        this.alert = Uri.parse(alert);
        this.deleteAfterUse = deleteAfterUse;
        this.autoSilenceDuration = autoSilenceDuration;
        this.snoozeDuration = snoozeDuration;
        this.missedAlarmRepeatLimit = missedAlarmRepeatLimit;
        this.crescendoDuration = crescendoDuration;
        this.alarmVolume = alarmVolume;
        this.manualSortOrder = manualSortOrder;
        this.pauseStartDate = pauseStartDate;
        this.pauseEndDate = pauseEndDate;
        this.backgroundImage = backgroundImage;
        this.blurIntensity = blurIntensity;
        this.mathHardnessLevel = mathHardnessLevel;
    }

    // Used to create a clone of the given alarm
    public Alarm(@NonNull Alarm original) {
        this.id = original.id;
        this.enabled = original.enabled;
        this.year = original.year;
        this.month = original.month;
        this.day = original.day;
        this.hour = original.hour;
        this.minutes = original.minutes;
        this.vibrate = original.vibrate;
        this.vibrationPattern = original.vibrationPattern;
        this.flash = original.flash;
        this.daysOfWeek = Weekdays.fromBits(original.daysOfWeek.getBits());
        this.label = original.label;
        this.syncByLabel = original.syncByLabel;
        this.alert = original.alert;
        this.deleteAfterUse = original.deleteAfterUse;
        this.autoSilenceDuration = original.autoSilenceDuration;
        this.snoozeDuration = original.snoozeDuration;
        this.missedAlarmRepeatLimit = original.missedAlarmRepeatLimit;
        this.crescendoDuration = original.crescendoDuration;
        this.alarmVolume = original.alarmVolume;
        this.instanceState = original.instanceState;
        this.manualSortOrder = original.manualSortOrder;
        this.pauseStartDate = original.pauseStartDate;
        this.pauseEndDate = original.pauseEndDate;
        this.backgroundImage = original.backgroundImage;
        this.blurIntensity = original.blurIntensity;
        this.mathHardnessLevel = original.mathHardnessLevel;
    }

    public Alarm(@NonNull Cursor c) {
        id = c.getLong(ID_INDEX);
        enabled = c.getInt(ENABLED_INDEX) == 1;
        year = c.getInt(YEAR_INDEX);
        month = c.getInt(MONTH_INDEX);
        day = c.getInt(DAY_INDEX);
        hour = c.getInt(HOUR_INDEX);
        minutes = c.getInt(MINUTES_INDEX);
        daysOfWeek = Weekdays.fromBits(c.getInt(DAYS_OF_WEEK_INDEX));
        vibrate = c.getInt(VIBRATE_INDEX) == 1;
        vibrationPattern = c.getString(VIBRATION_PATTERN_INDEX);
        flash = c.getInt(FLASH_INDEX) == 1;
        label = c.getString(LABEL_INDEX);
        syncByLabel = c.getInt(SYNC_BY_LABEL_INDEX) == 1;
        deleteAfterUse = c.getInt(DELETE_AFTER_USE_INDEX) == 1;
        autoSilenceDuration = c.getInt(AUTO_SILENCE_DURATION_INDEX);
        snoozeDuration = c.getInt(SNOOZE_DURATION_INDEX);
        missedAlarmRepeatLimit = c.getInt(MISSED_ALARM_REPEAT_LIMIT_INDEX);
        crescendoDuration = c.getInt(CRESCENDO_DURATION_INDEX);
        alarmVolume = c.getInt(ALARM_VOLUME_INDEX);
        manualSortOrder = c.getInt(MANUAL_SORT_ORDER_INDEX);
        pauseStartDate = c.getLong(PAUSE_START_DATE_INDEX);
        pauseEndDate = c.getLong(PAUSE_END_DATE_INDEX);
        backgroundImage = c.getString(BACKGROUND_IMAGE_INDEX);
        blurIntensity = c.getInt(BLUR_INTENSITY_INDEX);
        mathHardnessLevel = c.getString(MATH_HARDNESS_LEVEL_INDEX);

        if (c.getColumnCount() == ALARM_JOIN_INSTANCE_COLUMN_COUNT) {
            instanceState = c.getInt(INSTANCE_STATE_INDEX);
        }

        if (c.isNull(RINGTONE_INDEX)) {
            // Should we be saving this with the current ringtone or leave it null
            // so it changes when user changes default ringtone?
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            alert = Uri.parse(c.getString(RINGTONE_INDEX));
        }
    }

    Alarm(@NonNull Parcel p) {
        id = p.readLong();
        enabled = p.readInt() == 1;
        year = p.readInt();
        month = p.readInt();
        day = p.readInt();
        hour = p.readInt();
        minutes = p.readInt();
        daysOfWeek = Weekdays.fromBits(p.readInt());
        vibrate = p.readInt() == 1;
        vibrationPattern = p.readString();
        flash = p.readInt() == 1;
        label = p.readString();
        syncByLabel = p.readInt() == 1;
        alert = ParcelCompat.readParcelable(p, getClass().getClassLoader(), Uri.class);
        deleteAfterUse = p.readInt() == 1;
        autoSilenceDuration = p.readInt();
        snoozeDuration = p.readInt();
        missedAlarmRepeatLimit = p.readInt();
        crescendoDuration = p.readInt();
        alarmVolume = p.readInt();
        manualSortOrder = p.readInt();
        pauseStartDate = p.readLong();
        pauseEndDate = p.readLong();
        backgroundImage = p.readString();
        blurIntensity = p.readInt();
        mathHardnessLevel = p.readString();
    }

    @NonNull
    public ContentValues createContentValues() {
        ContentValues values = new ContentValues(COLUMN_COUNT);
        if (id != INVALID_ID) {
            values.put(ClockContract.AlarmsColumns._ID, id);
        }

        values.put(ENABLED, enabled ? 1 : 0);
        values.put(YEAR, year);
        values.put(MONTH, month);
        values.put(DAY, day);
        values.put(HOUR, hour);
        values.put(MINUTES, minutes);
        values.put(DAYS_OF_WEEK, daysOfWeek.getBits());
        values.put(VIBRATE, vibrate ? 1 : 0);
        values.put(VIBRATION_PATTERN, vibrationPattern);
        values.put(FLASH, flash ? 1 : 0);
        values.put(LABEL, label);
        values.put(SYNC_BY_LABEL, syncByLabel ? 1 : 0);
        values.put(DELETE_AFTER_USE, deleteAfterUse ? 1 : 0);
        values.put(AUTO_SILENCE_DURATION, autoSilenceDuration);
        values.put(SNOOZE_DURATION, snoozeDuration);
        values.put(MISSED_ALARM_REPEAT_LIMIT, missedAlarmRepeatLimit);
        values.put(CRESCENDO_DURATION, crescendoDuration);
        values.put(ALARM_VOLUME, alarmVolume);
        values.put(MANUAL_SORT_ORDER, manualSortOrder);
        values.put(PAUSE_START_DATE, pauseStartDate);
        values.put(PAUSE_END_DATE, pauseEndDate);
        values.put(BACKGROUND_IMAGE, backgroundImage);
        values.put(BLUR_INTENSITY, blurIntensity);
        values.put(MATH_HARDNESS_LEVEL, mathHardnessLevel);

        if (alert == null) {
            // We want to put null, so default alarm changes
            values.putNull(RINGTONE);
        } else {
            values.put(RINGTONE, alert.toString());
        }

        return values;
    }

    public void writeToParcel(@NonNull Parcel p, int flags) {
        p.writeLong(id);
        p.writeInt(enabled ? 1 : 0);
        p.writeInt(year);
        p.writeInt(month);
        p.writeInt(day);
        p.writeInt(hour);
        p.writeInt(minutes);
        p.writeInt(daysOfWeek.getBits());
        p.writeInt(vibrate ? 1 : 0);
        p.writeString(vibrationPattern);
        p.writeInt(flash ? 1 : 0);
        p.writeString(label);
        p.writeInt(syncByLabel ? 1 : 0);
        p.writeParcelable(alert, flags);
        p.writeInt(deleteAfterUse ? 1 : 0);
        p.writeInt(autoSilenceDuration);
        p.writeInt(snoozeDuration);
        p.writeInt(missedAlarmRepeatLimit);
        p.writeInt(crescendoDuration);
        p.writeInt(alarmVolume);
        p.writeInt(manualSortOrder);
        p.writeLong(pauseStartDate);
        p.writeLong(pauseEndDate);
        p.writeString(backgroundImage);
        p.writeInt(blurIntensity);
        p.writeString(mathHardnessLevel);
    }

    public int describeContents() {
        return 0;
    }

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull Class<?> cls, long alarmId) {
        return new Intent(context, cls).setData(getContentUri(alarmId));
    }

    @NonNull
    public static Uri getContentUri(long alarmId) {
        return ContentUris.withAppendedId(CONTENT_URI, alarmId);
    }

    public static long getId(@NonNull Uri contentUri) {
        return ContentUris.parseId(contentUri);
    }

    /**
     * Get alarm cursor loader for all alarms.
     *
     * @param context               to query the database.
     * @param areEnabledAlarmsFirst {@code true} if enabled alarms are placed at the top of the list; {@code false} otherwise.
     * @param sortingPref           the alarm sorting.
     * @return cursor loader with all the alarms.
     */
    @NonNull
    public static CursorLoader getAlarmsCursorLoader(@NonNull Context context, boolean areEnabledAlarmsFirst, @NonNull String sortingPref) {
        String sortOrder = DEFAULT_SORT_ORDER;

        switch (sortingPref) {
            case DEFAULT_SORT_BY_ALARM_TIME -> {
                if (areEnabledAlarmsFirst) {
                    sortOrder = DEFAULT_SORT_ORDER_WITH_ENABLED_FIRST;
                }
            }

            case SORT_ALARM_BY_DESCENDING_CREATION_ORDER -> {
                if (areEnabledAlarmsFirst) {
                    sortOrder = SORT_ORDER_BY_DESCENDING_CREATION_WITH_ENABLED_FIRST;
                } else {
                    sortOrder = SORT_ORDER_BY_DESCENDING_CREATION;
                }
            }

            case SORT_ALARM_BY_ASCENDING_CREATION_ORDER -> {
                if (areEnabledAlarmsFirst) {
                    sortOrder = SORT_ORDER_BY_ASCENDING_CREATION_WITH_ENABLED_FIRST;
                } else {
                    sortOrder = SORT_ORDER_BY_ASCENDING_CREATION;
                }
            }

            case SORT_ALARM_MANUALLY -> {
                if (areEnabledAlarmsFirst) {
                    sortOrder = SORT_ORDER_MANUALLY_WITH_ENABLED_FIRST;
                } else {
                    sortOrder = SORT_ORDER_MANUALLY_ASC;
                }
            }
        }

        return new CursorLoader(context, ALARMS_WITH_INSTANCES_URI, QUERY_ALARMS_WITH_INSTANCES_COLUMNS, null, null, sortOrder) {
            @Override
            public Cursor loadInBackground() {
                // Prime the ringtone title cache for later access. Most alarms will refer to
                // system ringtones.
                DataModel.getDataModel().loadRingtoneTitles();

                return super.loadInBackground();
            }
        };
    }

    /**
     * Get alarm by id.
     *
     * @param cr      provides access to the content model
     * @param alarmId for the desired alarm.
     * @return alarm if found, null otherwise
     */
    @Nullable
    public static Alarm getAlarm(@NonNull ContentResolver cr, long alarmId) {
        try (Cursor cursor = cr.query(getContentUri(alarmId), QUERY_COLUMNS, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return new Alarm(cursor);
            }
        }

        return null;
    }

    /**
     * Get all alarms given conditions.
     *
     * @param cr            provides access to the content model
     * @param selection     A filter declaring which rows to return, formatted as an
     *                      SQL WHERE clause (excluding the WHERE itself). Passing null will
     *                      return all rows for the given URI.
     * @param selectionArgs You may include ?s in selection, which will be
     *                      replaced by the values from selectionArgs, in the order that they
     *                      appear in the selection. The values will be bound as Strings.
     * @return list of alarms matching where clause or empty list if none found.
     */
    @NonNull
    public static List<Alarm> getAlarms(@NonNull ContentResolver cr, @Nullable String selection, @NonNull String... selectionArgs) {
        final List<Alarm> result = new LinkedList<>();
        try (Cursor cursor = cr.query(CONTENT_URI, QUERY_COLUMNS, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    result.add(new Alarm(cursor));
                } while (cursor.moveToNext());
            }
        }

        return result;
    }

    /**
     * @return a list of enabled alarms.
     */
    @NonNull
    public static List<Alarm> getEnabledAlarms(@NonNull Context context) {
        final String selection = String.format("%s=?", Alarm.ENABLED);
        final String[] args = {"1"};
        return Alarm.getAlarms(context.getContentResolver(), selection, args);
    }

    public Alarm addAlarm(@NonNull ContentResolver contentResolver) {
        ContentValues values = createContentValues();
        Uri uri = contentResolver.insert(CONTENT_URI, values);

        if (uri == null) {
            throw new IllegalStateException("Failed to insert alarm into ContentResolver");
        }

        id = getId(uri);
        return this;
    }

    public void updateAlarm(@NonNull ContentResolver contentResolver) {
        if (id == Alarm.INVALID_ID) {
            return;
        }
        ContentValues values = createContentValues();
        contentResolver.update(getContentUri(id), values, null, null);
    }

    public static boolean deleteAlarm(@NonNull ContentResolver contentResolver, long alarmId) {
        if (alarmId == INVALID_ID) {
            return false;
        }
        int deletedRows = contentResolver.delete(getContentUri(alarmId), "", null);
        return deletedRows == 1;
    }

    public boolean isDeleteAfterUse() {
        return !daysOfWeek.isRepeating() && deleteAfterUse;
    }

    public String getLabelOrDefault(@NonNull Context context) {
        return label.isEmpty() ? context.getString(R.string.default_label) : label;
    }

    /**
     * Determines whether the alarm is eligible to show a preemptive dismiss button.
     * <p>
     * The behavior depends on user settings and the current alarm state:</p>
     * <ul>
     *      <li>If the dismiss button is configured to be shown when the alarm is enabled,
     *          the method returns {@code true} if the alarm is enabled or currently snoozed.</li>
     *      <li>Otherwise, it returns true only if the alarm is in SNOOZE_STATE or NOTIFICATION_STATE.</li>
     * </ul>
     *
     * @param isDismissButtonDisplayed {@code true} if the "Dismiss" button is displayed; {@code false} otherwise.
     * @return {@code true} if the alarm can show a preemptive dismiss button; {@code false} otherwise.
     */
    public boolean canPreemptivelyDismiss(boolean isDismissButtonDisplayed) {
        if (isDismissButtonDisplayed) {
            return enabled || instanceState == AlarmInstance.SNOOZE_STATE;
        } else {
            return instanceState == AlarmInstance.SNOOZE_STATE || instanceState == AlarmInstance.NOTIFICATION_STATE;
        }
    }

    /**
     * Checks if the user has modified the time, date, or repeat days.
     * These changes are considered "major" because they require recalculating and recreating
     * the alarm instance.
     *
     * @param other The original alarm to compare against.
     * @return {@code true} if a major time-related field has changed, {@code false} otherwise.
     */
    public boolean hasTimeChanged(@Nullable Alarm other) {
        if (other == null) {
            return false;
        }

        return year != other.year
            || month != other.month
            || day != other.day
            || hour != other.hour
            || minutes != other.minutes
            || daysOfWeek.getBits() != other.daysOfWeek.getBits()
            || pauseStartDate != other.pauseStartDate
            || pauseEndDate != other.pauseEndDate;
    }

    /**
     * Checks if the user has modified the alarm's behavior settings.
     * These changes are considered "minor" because they can be applied directly to the existing
     * instance without having to cancel and recreate the schedule in the {@code AlarmManager}.
     *
     * @param other The original alarm to compare against.
     * @return {@code true} if a minor behavior-related field has changed, {@code false} otherwise.
     */
    public boolean hasMinorFieldsChanged(@Nullable Alarm other) {
        if (other == null) return false;

        return !Objects.equals(label, other.label)
            || syncByLabel != other.syncByLabel
            || vibrate != other.vibrate
            || !Objects.equals(vibrationPattern, other.vibrationPattern)
            || flash != other.flash
            || !Objects.equals(alert, other.alert)
            || deleteAfterUse != other.deleteAfterUse
            || autoSilenceDuration != other.autoSilenceDuration
            || snoozeDuration != other.snoozeDuration
            || missedAlarmRepeatLimit != other.missedAlarmRepeatLimit
            || crescendoDuration != other.crescendoDuration
            || alarmVolume != other.alarmVolume
            || !Objects.equals(backgroundImage, other.backgroundImage)
            || blurIntensity != other.blurIntensity
            || !Objects.equals(mathHardnessLevel, other.mathHardnessLevel);
    }

    public boolean isTomorrow(@NonNull Calendar now) {
        if (instanceState == AlarmInstance.SNOOZE_STATE) {
            return false;
        }

        final int totalAlarmMinutes = hour * 60 + minutes;
        final int totalNowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        return totalAlarmMinutes <= totalNowMinutes;
    }

    public boolean isDateInThePast() {
        Calendar alarmDate = Calendar.getInstance();
        alarmDate.clear();
        alarmDate.set(year, month, day);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return alarmDate.before(today);
    }

    public boolean isSpecifiedDate() {
        Calendar now = Calendar.getInstance();
        // Set this variable to avoid lint warning
        int currentMonth = now.get(Calendar.MONTH);

        return year != now.get(Calendar.YEAR)
            || month != currentMonth
            || day != now.get(Calendar.DAY_OF_MONTH);
    }

    public static boolean isSpecifiedDateTomorrow(int alarmYear, int alarmMonth, int alarmDayOfMonth) {
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = (Calendar) today.clone();

        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        // Set this variable to avoid lint warning
        int nextDayMonth = tomorrow.get(Calendar.MONTH);

        return alarmYear == tomorrow.get(Calendar.YEAR) &&
            alarmMonth == nextDayMonth &&
            alarmDayOfMonth == tomorrow.get(Calendar.DAY_OF_MONTH);
    }

    public boolean isTimeBeforeOrEqual(@NonNull Calendar referenceTime) {
        int currentHour = referenceTime.get(Calendar.HOUR_OF_DAY);
        int currentMinute = referenceTime.get(Calendar.MINUTE);

        return hour < currentHour || (hour == currentHour && minutes <= currentMinute);
    }

    public boolean isScheduledForToday(@NonNull Calendar reference) {
        int currentMonth = reference.get(Calendar.MONTH);
        return year == reference.get(Calendar.YEAR)
            && month == currentMonth
            && day == reference.get(Calendar.DAY_OF_MONTH);
    }

    public boolean isPauseSet() {
        return pauseStartDate > 0 && pauseEndDate > 0;
    }

    private boolean isDatePaused(@NonNull Calendar instanceTime) {
        if (!isPauseSet()) {
            return false;
        }

        // Convert the local alarm time to UTC midnight to compare it exactly with the output from the DatePicker.
        Calendar utcTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcTime.clear();
        utcTime.set(
            instanceTime.get(Calendar.YEAR),
            instanceTime.get(Calendar.MONTH),
            instanceTime.get(Calendar.DAY_OF_MONTH)
        );

        long timeMillis = utcTime.getTimeInMillis();
        return timeMillis >= pauseStartDate && timeMillis <= pauseEndDate;
    }

    public void clearPauseIfExpired() {
        if (isPauseSet() && AlarmUtils.isPauseExpired(pauseEndDate)) {
            pauseStartDate = 0;
            pauseEndDate = 0;
        }
    }

    /**
     * Ensures that an alarm scheduled for a specific date is not set in the past.
     * If it is, the date is reset to today.
     */
    public void fixDateIfPast() {
        if (daysOfWeek.isRepeating()) {
            return;
        }

        if (this.isDateInThePast()) {
            Calendar currentCalendar = Calendar.getInstance();
            year = currentCalendar.get(Calendar.YEAR);
            month = currentCalendar.get(Calendar.MONTH);
            day = currentCalendar.get(Calendar.DAY_OF_MONTH);
        }
    }

    @NonNull
    public AlarmInstance createInstanceAfter(@NonNull Calendar time) {
        Calendar nextInstanceTime = getNextAlarmTime(time);
        AlarmInstance result = new AlarmInstance(nextInstanceTime, id);
        result.mVibrate = vibrate;
        result.mVibrationPattern = vibrationPattern;
        result.mFlash = flash;
        result.mLabel = label;
        result.mSyncByLabel = syncByLabel;
        result.mRingtone = RingtoneUtils.isRandomRingtone(alert)
            ? RingtoneUtils.getRandomRingtoneUri()
            : RingtoneUtils.isRandomCustomRingtone(alert)
            ? RingtoneUtils.getRandomCustomRingtoneUri()
            : alert;
        result.mAutoSilenceDuration = autoSilenceDuration;
        result.mSnoozeDuration = snoozeDuration;
        result.mMissedAlarmRepeatLimit = missedAlarmRepeatLimit;
        result.mCrescendoDuration = crescendoDuration;
        result.mAlarmVolume = alarmVolume;
        return result;
    }

    /**
     * @param currentTime the current time
     * @return previous firing time, or null if this is a one-time alarm.
     */
    @Nullable
    public Calendar getPreviousAlarmTime(@NonNull Calendar currentTime) {
        final Calendar previousInstanceTime = Calendar.getInstance(currentTime.getTimeZone());
        previousInstanceTime.set(Calendar.YEAR, year);
        previousInstanceTime.set(Calendar.MONTH, month);
        previousInstanceTime.set(Calendar.DAY_OF_MONTH, day);
        previousInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
        previousInstanceTime.set(Calendar.MINUTE, minutes);
        previousInstanceTime.set(Calendar.SECOND, 0);
        previousInstanceTime.set(Calendar.MILLISECOND, 0);

        final int subtractDays = daysOfWeek.getDistanceToPreviousDay(previousInstanceTime);
        if (subtractDays > 0) {
            previousInstanceTime.add(Calendar.DAY_OF_WEEK, -subtractDays);
            return previousInstanceTime;
        } else {
            return null;
        }
    }

    /**
     * Calculates the next scheduled occurrence time.
     *
     * <p>This method determines when the alarm should trigger again based on its
     * configuration. It handles both repeating alarms (with specific days of the week)
     * and one-time alarms (with a fixed date). Daylight Savings Time (DST) adjustments
     * are also taken into account by resetting the hour and minute after shifting days.
     *
     * @return a {@link Calendar} instance representing the next valid alarm time.
     * <p>- For repeating alarms: the next valid day of the week at the configured hour/minute.</p>
     * <p>- For one-time alarms: the configured date and time, or the following day if the
     * specified time has already passed relative to {@code currentTime}.</p>
     */
    @NonNull
    public Calendar getNextAlarmTime(@NonNull Calendar currentTime) {
        final Calendar nextInstanceTime = Calendar.getInstance(currentTime.getTimeZone());
        nextInstanceTime.set(Calendar.SECOND, 0);
        nextInstanceTime.set(Calendar.MILLISECOND, 0);

        if (daysOfWeek.isRepeating()) {
            nextInstanceTime.setTimeInMillis(currentTime.getTimeInMillis());
            nextInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
            nextInstanceTime.set(Calendar.MINUTE, minutes);

            // If we are still behind the passed in currentTime, then add a day
            if (nextInstanceTime.getTimeInMillis() <= currentTime.getTimeInMillis()) {
                nextInstanceTime.add(Calendar.DAY_OF_YEAR, 1);
            }

            // The day of the week might be invalid, so find next valid one
            final int addDays = daysOfWeek.getDistanceToNextDay(nextInstanceTime);
            if (addDays > 0) {
                nextInstanceTime.add(Calendar.DAY_OF_WEEK, addDays);
            }

            // Daylight Savings Time can alter the hours and minutes when adjusting the day above.
            // Reset the desired hour and minute now that the correct day has been chosen.
            nextInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
            nextInstanceTime.set(Calendar.MINUTE, minutes);

            if (isDatePaused(nextInstanceTime)) {
                // The alarm goes off during the pause: retrieve the end time of the pause
                Calendar endOfPauseUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                endOfPauseUtc.setTimeInMillis(pauseEndDate);

                // Create a fake local "current date" that corresponds to the very end of the pause, exactly at the scheduled alarm time.
                Calendar localEndOfPause = Calendar.getInstance(currentTime.getTimeZone());
                localEndOfPause.clear();
                localEndOfPause.set(Calendar.YEAR, endOfPauseUtc.get(Calendar.YEAR));
                localEndOfPause.set(Calendar.MONTH, endOfPauseUtc.get(Calendar.MONTH));
                localEndOfPause.set(Calendar.DAY_OF_MONTH, endOfPauseUtc.get(Calendar.DAY_OF_MONTH));
                localEndOfPause.set(Calendar.HOUR_OF_DAY, hour);
                localEndOfPause.set(Calendar.MINUTE, minutes);

                // Restart the search. The system will see that the alarm time has “already passed or is equal to” that day,
                // and will automatically add one day and then search for the next valid day of the week.
                return getNextAlarmTime(localEndOfPause);
            }
        } else {
            nextInstanceTime.set(Calendar.YEAR, year);
            nextInstanceTime.set(Calendar.MONTH, month);
            nextInstanceTime.set(Calendar.DAY_OF_MONTH, day);
            nextInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
            nextInstanceTime.set(Calendar.MINUTE, minutes);

            // If we are still behind the passed in currentTime, then add a day
            if (nextInstanceTime.getTimeInMillis() <= currentTime.getTimeInMillis()) {
                nextInstanceTime.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        return nextInstanceTime;
    }

    /**
     * Returns the day of the week (as Calendar.DAY_OF_WEEK) when the alarm will next trigger.
     *
     * <p>If a valid {@link AlarmInstance} is provided and its scheduled time is in the future,
     * that time is used to determine the next alarm day.
     * Otherwise, the method calculates the next scheduled alarm time based on the current time
     * and the alarm's repeat settings.</p>
     *
     * @param alarmInstance the current {@link AlarmInstance}, or null if not yet created
     * @return the day of the week (e.g., {@link Calendar#MONDAY}, {@link Calendar#TUESDAY}, ...)
     */
    public int getNextAlarmDayOfWeek(@NonNull AlarmInstance alarmInstance) {
        return getNextAlarmTimeCalendar(alarmInstance).get(Calendar.DAY_OF_WEEK);
    }

    /**
     * Retrieves the exact {@link Calendar} date and time when the alarm will next trigger.
     *
     * <p>This method ensures temporal accuracy by verifying the provided {@link AlarmInstance}.
     * If the instance exists and is scheduled in the future, its time is returned directly.
     * If the instance is null or its scheduled time has already passed (e.g., the alarm just fired),
     * this method dynamically calculates the next valid chronological trigger time based on the
     * current time and the alarm's recurrence rules.</p>
     *
     * @param alarmInstance the current {@link AlarmInstance} associated with this alarm, or null.
     * @return a {@link Calendar} object representing the next valid upcoming alarm time.
     */
    public Calendar getNextAlarmTimeCalendar(@Nullable AlarmInstance alarmInstance) {
        Calendar referenceTime = Calendar.getInstance();
        if (alarmInstance != null && alarmInstance.getAlarmTime().after(referenceTime)) {
            return alarmInstance.getAlarmTime();
        } else {
            return getNextAlarmTime(referenceTime);
        }
    }

    /**
     * Returns the next alarm time for sorting purposes.
     */
    public Calendar getSortableNextAlarmTime(@Nullable AlarmInstance instance, @NonNull Calendar now) {
        Calendar result = Calendar.getInstance(now.getTimeZone());
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);

        if (daysOfWeek.isRepeating()) {
            // If a future instance exists (e.g. after Dismiss), use it.
            // Otherwise, compute the next valid occurrence from "now".
            if (instance != null && instance.getAlarmTime().getTimeInMillis() > now.getTimeInMillis()) {
                return instance.getAlarmTime();
            }

            return getNextAlarmTime(now);
        } else {
            if (isSpecifiedDate()) {
                if (isDateInThePast()) {
                    // Expired specific date → anchor to today at the alarm's time
                    result.set(Calendar.YEAR, now.get(Calendar.YEAR));
                    result.set(Calendar.MONTH, now.get(Calendar.MONTH));
                    result.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
                    result.set(Calendar.HOUR_OF_DAY, hour);
                    result.set(Calendar.MINUTE, minutes);

                    // If the time has already passed today, shift to tomorrow
                    if (result.getTimeInMillis() < now.getTimeInMillis()) {
                        result.add(Calendar.DAY_OF_YEAR, 1);
                    }
                } else {
                    // Future or today’s specified date → respect the defined date/time
                    result.set(Calendar.YEAR, year);
                    result.set(Calendar.MONTH, month);
                    result.set(Calendar.DAY_OF_MONTH, day);
                    result.set(Calendar.HOUR_OF_DAY, hour);
                    result.set(Calendar.MINUTE, minutes);
                }

                return result;
            }
        }

        // Alarms with no date and no repetition → today at the alarm time,
        // and if the time has passed, shift to tomorrow
        result.set(Calendar.YEAR, now.get(Calendar.YEAR));
        result.set(Calendar.MONTH, now.get(Calendar.MONTH));
        result.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
        result.set(Calendar.HOUR_OF_DAY, hour);
        result.set(Calendar.MINUTE, minutes);

        if (result.getTimeInMillis() < now.getTimeInMillis()) {
            result.add(Calendar.DAY_OF_YEAR, 1);
        }

        return result;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof final Alarm other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "Alarm{" +
            "alert=" + alert +
            ", id=" + id +
            ", enabled=" + enabled +
            ", year=" + year +
            ", month=" + month +
            ", day=" + day +
            ", hour=" + hour +
            ", minutes=" + minutes +
            ", daysOfWeek=" + daysOfWeek +
            ", vibrate=" + vibrate +
            ", vibrationPattern=" + vibrationPattern +
            ", flash=" + flash +
            ", label='" + label + '\'' +
            ", syncByLabel=" + syncByLabel +
            ", deleteAfterUse=" + deleteAfterUse +
            ", autoSilenceDuration=" + autoSilenceDuration +
            ", snoozeDuration=" + snoozeDuration +
            ", missedAlarmRepeatLimit=" + missedAlarmRepeatLimit +
            ", crescendoDuration=" + crescendoDuration +
            ", alarmVolume=" + alarmVolume +
            ", manualSortOrder=" + manualSortOrder +
            ", pauseStartDate=" + pauseStartDate +
            ", pauseEndDate=" + pauseEndDate +
            ", backgroundImage=" + backgroundImage +
            ", blurIntensity=" + blurIntensity +
            ", mathHardnessLevel=" + mathHardnessLevel +
            '}';
    }

}
