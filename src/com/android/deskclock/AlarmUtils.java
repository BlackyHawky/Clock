/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.deskclock;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.util.Calendar;
import java.util.Locale;

/**
 * Static utility methods for Alarms.
 */
public class AlarmUtils {
    public static final String FRAG_TAG_TIME_PICKER = "time_dialog";

    public static String getFormattedTime(Context context, Calendar time) {
        String pattern;
        if (Utils.isJBMR2OrLater()) {
            final String skeleton = DateFormat.is24HourFormat(context) ? "EHm" : "Ehma";
            pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
            return (String) DateFormat.format(pattern, time);
        } else {
            pattern = DateFormat.is24HourFormat(context)
                    ? context.getString(R.string.weekday_time_format_24_mode)
                    : context.getString(R.string.weekday_time_format_12_mode);
        }
        return (String) DateFormat.format(pattern, time);
    }

    public static String getAlarmText(Context context, AlarmInstance instance) {
        String alarmTimeStr = getFormattedTime(context, instance.getAlarmTime());
        return !instance.mLabel.isEmpty() ? alarmTimeStr + " - " + instance.mLabel
                : alarmTimeStr;
    }

    // show time picker dialog for pre-L devices
    public static void showTimeEditDialog(FragmentManager manager, final Alarm alarm,
          com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener listener,
          boolean is24HourMode) {

        final int hour, minutes;
        if (alarm == null) {
            hour = 0;
            minutes = 0;
        } else {
            hour = alarm.hour;
            minutes = alarm.minutes;
        }
        com.android.datetimepicker.time.TimePickerDialog dialog =
                com.android.datetimepicker.time.TimePickerDialog.newInstance(listener,
                    hour, minutes, is24HourMode);
        dialog.setThemeDark(true);

        // Make sure the dialog isn't already added.
        manager.executePendingTransactions();
        final FragmentTransaction ft = manager.beginTransaction();
        final Fragment prev = manager.findFragmentByTag(FRAG_TAG_TIME_PICKER);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();

        if (!dialog.isAdded()) {
            dialog.show(manager, FRAG_TAG_TIME_PICKER);
        }
    }

    /**
     * Show the time picker dialog for post-L devices.
     * This is called from AlarmClockFragment to set alarm.
     * @param fragment The calling fragment (which is also a onTimeSetListener),
     *                 we use it as the target fragment of the TimePickerFragment, so later the
     *                 latter can retrieve it and set it as its onTimeSetListener when the fragment
     *                 is recreated.
     * @param alarm The clicked alarm, it can be null if user was clicking the fab instead.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void showTimeEditDialog(Fragment fragment, final Alarm alarm) {
        final FragmentManager manager = fragment.getFragmentManager();
        final FragmentTransaction ft = manager.beginTransaction();
        final Fragment prev = manager.findFragmentByTag(FRAG_TAG_TIME_PICKER);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();
        final TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setTargetFragment(fragment, 0);
        timePickerFragment.setOnTimeSetListener((TimePickerDialog.OnTimeSetListener) fragment);
        timePickerFragment.setAlarm(alarm);
        timePickerFragment.show(manager, FRAG_TAG_TIME_PICKER);
    }

    /**
     * @return {@code true} iff the user has granted permission to read the ringtone at the given
     *      uri or no permission is required to read the ringtone
     */
    public static boolean hasPermissionToDisplayRingtoneTitle(Context context, Uri ringtoneUri) {
        final PackageManager pm = context.getPackageManager();
        final String packageName = context.getPackageName();

        // If the default alarm alert ringtone URI is given, resolve it to the actual URI.
        if (Settings.System.DEFAULT_ALARM_ALERT_URI.equals(ringtoneUri)) {
            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context,
                    RingtoneManager.TYPE_ALARM);
        }

        // If no ringtone is specified, return true.
        if (ringtoneUri == null || ringtoneUri == Alarm.NO_RINGTONE_URI) {
            return true;
        }

        // If the permission is already granted, return true.
        if (pm.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, packageName)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        // If the ringtone is internal, return true;
        // external ringtones require the permission to see their title
        return ringtoneUri.toString().startsWith("content://media/internal/");
    }

    /**
     * format "Alarm set for 2 days, 7 hours, and 53 minutes from now."
     */
    @VisibleForTesting
    static String formatElapsedTimeUntilAlarm(Context context, long delta) {
        // If the alarm will ring within 60 seconds, just report "less than a minute."
        final String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        if (delta < DateUtils.MINUTE_IN_MILLIS) {
            return formats[0];
        }

        // Otherwise, format the remaining time until the alarm rings.

        // Round delta upwards to the nearest whole minute. (e.g. 7m 58s -> 8m)
        final long remainder = delta % DateUtils.MINUTE_IN_MILLIS;
        delta += remainder == 0 ? 0 : (DateUtils.MINUTE_IN_MILLIS - remainder);

        int hours = (int) delta / (1000 * 60 * 60);
        final int minutes = (int) delta / (1000 * 60) % 60;
        final int days = hours / 24;
        hours = hours % 24;

        String daySeq = Utils.getNumberFormattedQuantityString(context, R.plurals.days, days);
        String minSeq = Utils.getNumberFormattedQuantityString(context, R.plurals.minutes, minutes);
        String hourSeq = Utils.getNumberFormattedQuantityString(context, R.plurals.hours, hours);

        final boolean showDays = days > 0;
        final boolean showHours = hours > 0;
        final boolean showMinutes = minutes > 0;

        // Compute the index of the most appropriate time format based on the time delta.
        final int index = (showDays ? 1 : 0) | (showHours ? 2 : 0) | (showMinutes ? 4 : 0);

        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }

    public static void popAlarmSetToast(Context context, long alarmTime) {
        final long alarmTimeDelta = alarmTime - System.currentTimeMillis();
        final String text = formatElapsedTimeUntilAlarm(context, alarmTimeDelta);
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        ToastMaster.setToast(toast);
        toast.show();
    }
}
