/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static com.best.deskclock.bedtime.BedtimeFragment.BEDLABEL;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.widget.toast.SnackbarManager;
import com.best.deskclock.widget.toast.ToastManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.Locale;

/**
 * Static utility methods for Alarms.
 */
public class AlarmUtils {

    public static String getFormattedTime(Context context, Calendar time) {
        final String skeleton = DateFormat.is24HourFormat(context) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        if (context instanceof ScreensaverActivity || context instanceof Screensaver) {
            // Add a "Thin Space" (\u2009) at the end of the next alarm to prevent its display from being cut off on some devices.
            // (The display of the next alarm is only cut off at the end if it is defined in italics in the screensaver settings).
            final boolean isItalicDate = DataModel.getDataModel().getScreensaverItalicDate();
            final boolean isItalicNextAlarm = DataModel.getDataModel().getScreensaverItalicNextAlarm();
            if (isItalicDate) {
                // A "Thin Space" (\u2009) is also added at the beginning to correctly center the date,
                // alarm icon and next alarm only when the date is in italics.
                pattern = "\u2009" + DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton) + "\u2009";
            } else if (isItalicNextAlarm) {
                pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton) + "\u2009";
            }
        }
        return (String) DateFormat.format(pattern, time);
    }

    public static String getFormattedTime(Context context, long timeInMillis) {
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeInMillis);
        return getFormattedTime(context, c);
    }

    public static String getAlarmText(Context context, AlarmInstance instance,
                                      boolean includeLabel) {
        String alarmTimeStr = getFormattedTime(context, instance.getAlarmTime());
        return (instance.mLabel.isEmpty() || !includeLabel)
                ? alarmTimeStr
                : alarmTimeStr + " - " + (instance.mLabel.equals(BEDLABEL) ? context.getString(R.string.wakeup_alarm_label_visible) : instance.mLabel);
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
