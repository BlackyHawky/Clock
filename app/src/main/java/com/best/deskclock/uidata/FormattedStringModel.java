/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uidata;

import static java.util.Calendar.JULY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.ArrayMap;
import android.util.SparseArray;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

/**
 * All formatted strings that are cached for performance are accessed via this model.
 */
final class FormattedStringModel {

    /**
     * Clears data structures containing data that is locale-sensitive.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /**
     * Caches formatted numbers in the current locale padded with zeroes to requested lengths.
     * The first level of the cache maps length to the second level of the cache.
     * The second level of the cache maps an integer to a formatted String in the current locale.
     */
    private final SparseArray<SparseArray<String>> mNumberFormatCache = new SparseArray<>(3);

    /**
     * Single-character version of weekday names; e.g.: 'S', 'M', 'T', 'W', 'T', 'F', 'S'
     */
    private Map<Integer, String> mShortWeekdayNames;

    /**
     * Full weekday names; e.g.: 'Sunday', 'Monday', 'Tuesday', etc.
     */
    private Map<Integer, String> mLongWeekdayNames;

    FormattedStringModel(Context context) {
        // Clear caches affected by locale when locale changes.
        final IntentFilter localeBroadcastFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter);
        }
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
        return getFormattedNumber(value, length);
    }

    /**
     * This method is intended to be used when formatting numbers occurs in a hotspot such as the
     * update loop of a timer or stopwatch. It returns cached results when possible in order to
     * provide speed and limit garbage to be collected by the virtual machine.
     *
     * @param value  a positive integer to format as a String
     * @param length the length of the String; zeroes are padded to match this length. If
     *               {@code negative} is {@code true} the return value will contain a minus sign and a total
     *               length of {@code length + 1}.
     * @return the {@code value} formatted as a String in the current locale and padded to the
     * requested {@code length}
     * @throws IllegalArgumentException if {@code value} is negative
     */
    String getFormattedNumber(int value, int length) {
        if (value < 0) {
            throw new IllegalArgumentException("value may not be negative: " + value);
        }

        // Look up the value cache using the length; -ve and +ve values are cached separately.
        SparseArray<String> valueCache = mNumberFormatCache.get(length);
        if (valueCache == null) {
            valueCache = new SparseArray<>((int) Math.pow(10, length));
            mNumberFormatCache.put(length, valueCache);
        }

        // Look up the cached formatted value using the value.
        String formatted = valueCache.get(value);
        if (formatted == null) {
            final String sign = "";
            formatted = String.format(Locale.getDefault(), sign + "%0" + length + "d", value);
            valueCache.put(value, formatted);
        }

        return formatted;
    }

    /**
     * @param calendarDay any of the following values
     *                    <ul>
     *                    <li>{@link Calendar#SUNDAY}</li>
     *                    <li>{@link Calendar#MONDAY}</li>
     *                    <li>{@link Calendar#TUESDAY}</li>
     *                    <li>{@link Calendar#WEDNESDAY}</li>
     *                    <li>{@link Calendar#THURSDAY}</li>
     *                    <li>{@link Calendar#FRIDAY}</li>
     *                    <li>{@link Calendar#SATURDAY}</li>
     *                    </ul>
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
     *                    <ul>
     *                    <li>{@link Calendar#SUNDAY}</li>
     *                    <li>{@link Calendar#MONDAY}</li>
     *                    <li>{@link Calendar#TUESDAY}</li>
     *                    <li>{@link Calendar#WEDNESDAY}</li>
     *                    <li>{@link Calendar#THURSDAY}</li>
     *                    <li>{@link Calendar#FRIDAY}</li>
     *                    <li>{@link Calendar#SATURDAY}</li>
     *                    </ul>
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
