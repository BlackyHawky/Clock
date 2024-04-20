/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import android.content.Context;
import android.os.SystemClock;
import android.text.format.DateFormat;

import java.util.Calendar;

/**
 * All time data is accessed via this model. This model exists so that time can be mocked for
 * testing purposes.
 */
final class TimeModel {

    private final Context mContext;

    TimeModel(Context context) {
        mContext = context;
    }

    /**
     * @return the current time in milliseconds
     */
    long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * @return milliseconds since boot, including time spent in sleep
     */
    long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * @return {@code true} if 24 hour time format is selected; {@code false} otherwise
     */
    boolean is24HourFormat() {
        return DateFormat.is24HourFormat(mContext);
    }

    /**
     * @return a new Calendar with the {@link #currentTimeMillis}
     */
    Calendar getCalendar() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis());
        return calendar;
    }
}
