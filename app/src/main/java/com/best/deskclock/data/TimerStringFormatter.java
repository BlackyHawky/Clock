/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.StringRes;

import com.best.deskclock.R;
import com.best.deskclock.Utils;

public class TimerStringFormatter {

    /**
     * Format "7 hours 52 minutes 14 seconds remaining"
     */
    @SuppressLint("StringFormatInvalid")
    public static String formatTimeRemaining(Context context, long remainingTime,
                                             boolean shouldShowSeconds) {
        int roundedHours = (int) (remainingTime / HOUR_IN_MILLIS);
        int roundedMinutes = (int) (remainingTime / MINUTE_IN_MILLIS % 60);
        int roundedSeconds = (int) (remainingTime / SECOND_IN_MILLIS % 60);

        final int seconds;
        final int minutes;
        final int hours;
        if ((remainingTime % SECOND_IN_MILLIS != 0) && shouldShowSeconds) {
            // Add 1 because there's a partial second.
            roundedSeconds += 1;
            if (roundedSeconds == 60) {
                // Wind back and fix the hours and minutes as needed.
                seconds = 0;
                roundedMinutes += 1;
                if (roundedMinutes == 60) {
                    minutes = 0;
                    roundedHours += 1;
                } else {
                    minutes = roundedMinutes;
                }
            } else {
                seconds = roundedSeconds;
                minutes = roundedMinutes;
            }
        } else {
            // Already perfect precision, or we don't want to consider seconds at all.
            seconds = roundedSeconds;
            minutes = roundedMinutes;
        }
        hours = roundedHours;

        final String minSeq = Utils.getNumberFormattedQuantityString(context, R.plurals.minutes,
                minutes);
        final String hourSeq = Utils.getNumberFormattedQuantityString(context, R.plurals.hours,
                hours);
        final String secSeq = Utils.getNumberFormattedQuantityString(context, R.plurals.seconds,
                seconds);

        // The verb "remaining" may have to change tense for singular subjects in some languages.
        final String remainingSuffix = context.getString((minutes > 1 || hours > 1 || seconds > 1)
                ? R.string.timer_remaining_multiple
                : R.string.timer_remaining_single);

        final boolean showHours = hours > 0;
        final boolean showMinutes = minutes > 0;
        final boolean showSeconds = (seconds > 0) && shouldShowSeconds;

        int formatStringId = -1;
        if (showHours) {
            if (showMinutes) {
                if (showSeconds) {
                    formatStringId = R.string.timer_notifications_hours_minutes_seconds;
                } else {
                    formatStringId = R.string.timer_notifications_hours_minutes;
                }
            } else if (showSeconds) {
                formatStringId = R.string.timer_notifications_hours_seconds;
            } else {
                formatStringId = R.string.timer_notifications_hours;
            }
        } else if (showMinutes) {
            if (showSeconds) {
                formatStringId = R.string.timer_notifications_minutes_seconds;
            } else {
                formatStringId = R.string.timer_notifications_minutes;
            }
        } else if (showSeconds) {
            formatStringId = R.string.timer_notifications_seconds;
        } else if (!shouldShowSeconds) {
            formatStringId = R.string.timer_notifications_less_min;
        }

        if (formatStringId == -1) {
            return null;
        }
        return String.format(context.getString(formatStringId), hourSeq, minSeq, remainingSuffix,
                secSeq);
    }

    public static String formatString(Context context, @StringRes int stringResId, long currentTime,
                                      boolean shouldShowSeconds) {
        return String.format(context.getString(stringResId),
                formatTimeRemaining(context, currentTime, shouldShowSeconds));
    }
}
