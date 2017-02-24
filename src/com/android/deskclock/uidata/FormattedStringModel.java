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

package com.android.deskclock.uidata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.ArrayMap;
import android.util.SparseArray;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

import static java.util.Calendar.JULY;

/**
 * All formatted strings that are cached for performance are accessed via this model.
 */
final class FormattedStringModel {

    /** Clears data structures containing data that is locale-sensitive. */
    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /**
     * Caches formatted numbers in the current locale padded with zeroes to requested lengths.
     * The first level of the cache maps length to the second level of the cache.
     * The second level of the cache maps an integer to a formatted String in the current locale.
     */
    private final SparseArray<SparseArray<String>> mNumberFormatCache = new SparseArray<>(3);

    /** Single-character version of weekday names; e.g.: 'S', 'M', 'T', 'W', 'T', 'F', 'S' */
    private Map<Integer, String> mShortWeekdayNames;

    /** Full weekday names; e.g.: 'Sunday', 'Monday', 'Tuesday', etc. */
    private Map<Integer, String> mLongWeekdayNames;

    FormattedStringModel(Context context) {
        // Clear caches affected by locale when locale changes.
        final IntentFilter localeBroadcastFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        context.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter);
    }

    /**
     * This method is intended to be used when formatting numbers occurs in a hotspot such as the
     * update loop of a timer or stopwatch. It returns cached results when possible in order to
     * provide speed and limit garbage to be collected by the virtual machine.
     *
     * @param value a positive integer to format as a String
     * @return the {@code value} formatted as a String in the current locale
     * @throws IllegalArgumentException if {@code value} is negative
     */
    String getFormattedNumber(int value) {
        final int length = value == 0 ? 1 : ((int) Math.log10(value) + 1);
        return getFormattedNumber(false, value, length);
    }

    /**
     * This method is intended to be used when formatting numbers occurs in a hotspot such as the
     * update loop of a timer or stopwatch. It returns cached results when possible in order to
     * provide speed and limit garbage to be collected by the virtual machine.
     *
     * @param value a positive integer to format as a String
     * @param length the length of the String; zeroes are padded to match this length
     * @return the {@code value} formatted as a String in the current locale and padded to the
     *      requested {@code length}
     * @throws IllegalArgumentException if {@code value} is negative
     */
    String getFormattedNumber(int value, int length) {
        return getFormattedNumber(false, value, length);
    }

    /**
     * This method is intended to be used when formatting numbers occurs in a hotspot such as the
     * update loop of a timer or stopwatch. It returns cached results when possible in order to
     * provide speed and limit garbage to be collected by the virtual machine.
     *
     * @param negative force a minus sign (-) onto the display, even if {@code value} is {@code 0}
     * @param value a positive integer to format as a String
     * @param length the length of the String; zeroes are padded to match this length. If
     *      {@code negative} is {@code true} the return value will contain a minus sign and a total
     *      length of {@code length + 1}.
     * @return the {@code value} formatted as a String in the current locale and padded to the
     *      requested {@code length}
     * @throws IllegalArgumentException if {@code value} is negative
     */
    String getFormattedNumber(boolean negative, int value, int length) {
        if (value < 0) {
            throw new IllegalArgumentException("value may not be negative: " + value);
        }

        // Look up the value cache using the length; -ve and +ve values are cached separately.
        final int lengthCacheKey = negative ? -length : length;
        SparseArray<String> valueCache = mNumberFormatCache.get(lengthCacheKey);
        if (valueCache == null) {
            valueCache = new SparseArray<>((int) Math.pow(10, length));
            mNumberFormatCache.put(lengthCacheKey, valueCache);
        }

        // Look up the cached formatted value using the value.
        String formatted = valueCache.get(value);
        if (formatted == null) {
            final String sign = negative ? "−" : "";
            formatted = String.format(Locale.getDefault(), sign + "%0" + length + "d", value);
            valueCache.put(value, formatted);
        }

        return formatted;
    }

    /**
     * @param calendarDay any of the following values
     *                     <ul>
     *                     <li>{@link Calendar#SUNDAY}</li>
     *                     <li>{@link Calendar#MONDAY}</li>
     *                     <li>{@link Calendar#TUESDAY}</li>
     *                     <li>{@link Calendar#WEDNESDAY}</li>
     *                     <li>{@link Calendar#THURSDAY}</li>
     *                     <li>{@link Calendar#FRIDAY}</li>
     *                     <li>{@link Calendar#SATURDAY}</li>
     *                     </ul>
     * @return single-character weekday name; e.g.: 'S', 'M', 'T', 'W', 'T', 'F', 'S'
     */
    String getShortWeekday(int calendarDay) {
        if (mShortWeekdayNames == null) {
            mShortWeekdayNames = new ArrayMap<>(7);

            final SimpleDateFormat format = new SimpleDateFormat("ccccc", Locale.getDefault());
            for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
                final Calendar calendar = new GregorianCalendar(2014, JULY, 20 + i - 1);
                final String weekday = format.format(calendar.getTime());
                mShortWeekdayNames.put(i, weekday);
            }
        }

        return mShortWeekdayNames.get(calendarDay);
    }

    /**
     * @param calendarDay any of the following values
     *                     <ul>
     *                     <li>{@link Calendar#SUNDAY}</li>
     *                     <li>{@link Calendar#MONDAY}</li>
     *                     <li>{@link Calendar#TUESDAY}</li>
     *                     <li>{@link Calendar#WEDNESDAY}</li>
     *                     <li>{@link Calendar#THURSDAY}</li>
     *                     <li>{@link Calendar#FRIDAY}</li>
     *                     <li>{@link Calendar#SATURDAY}</li>
     *                     </ul>
     * @return full weekday name; e.g.: 'Sunday', 'Monday', 'Tuesday', etc.
     */
    String getLongWeekday(int calendarDay) {
        if (mLongWeekdayNames == null) {
            mLongWeekdayNames = new ArrayMap<>(7);

            final Calendar calendar = new GregorianCalendar(2014, JULY, 20);
            final SimpleDateFormat format = new SimpleDateFormat("EEEE", Locale.getDefault());
            for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
                final String weekday = format.format(calendar.getTime());
                mLongWeekdayNames.put(i, weekday);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        return mLongWeekdayNames.get(calendarDay);
    }

    /**
     * Cached information that is locale-sensitive must be cleared in response to locale changes.
     */
    private final class LocaleChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mNumberFormatCache.clear();
            mShortWeekdayNames = null;
            mLongWeekdayNames = null;
        }
    }
}