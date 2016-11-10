/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.deskclock.data;

import android.content.Context;
import android.support.annotation.StringRes;

import com.android.deskclock.R;
import com.android.deskclock.Utils;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

public class TimerStringFormatter {

    /**
     * Format "7 hours 52 minutes 14 seconds remaining"
     */
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
                    hours = roundedHours;
                } else {
                    minutes = roundedMinutes;
                    hours = roundedHours;
                }
            } else {
                seconds = roundedSeconds;
                minutes = roundedMinutes;
                hours = roundedHours;
            }
        } else {
            // Already perfect precision, or we don't want to consider seconds at all.
            seconds = roundedSeconds;
            minutes = roundedMinutes;
            hours = roundedHours;
        }

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
