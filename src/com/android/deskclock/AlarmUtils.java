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

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.Snackbar;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Toast;

import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.toast.SnackbarManager;
import com.android.deskclock.widget.toast.ToastManager;

import java.util.Calendar;
import java.util.Locale;

/**
 * Static utility methods for Alarms.
 */
public class AlarmUtils {

    public static String getFormattedTime(Context context, Calendar time) {
        final String skeleton = DateFormat.is24HourFormat(context) ? "EHm" : "Ehma";
        final String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return (String) DateFormat.format(pattern, time);
    }

    public static String getAlarmText(Context context, AlarmInstance instance,
            boolean includeLabel) {
        String alarmTimeStr = getFormattedTime(context, instance.getAlarmTime());
        return (instance.mLabel.isEmpty() || !includeLabel)
                ? alarmTimeStr
                : alarmTimeStr + " - " + instance.mLabel;
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
        ToastManager.setToast(toast);
        toast.show();
    }

    public static void popAlarmSetSnackbar(View snackbarAnchor, long alarmTime) {
        final long alarmTimeDelta = alarmTime - System.currentTimeMillis();
        final String text = formatElapsedTimeUntilAlarm(
                snackbarAnchor.getContext(), alarmTimeDelta);
        SnackbarManager.show(Snackbar.make(snackbarAnchor, text, Snackbar.LENGTH_SHORT));
        snackbarAnchor.announceForAccessibility(text);
    }
}
