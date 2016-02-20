/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.alarms.utils;

import android.content.Context;

import com.android.deskclock.Utils;
import com.android.deskclock.provider.DaysOfWeek;

import java.util.Calendar;

/**
 * Contains information about the order that days of the week are shown in the UI.
 */
public final class DayOrderUtils {

    // A reference used to create mDayOrder
    private static final int[] DAY_ORDER = {
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
    };

    public static int[] getDayOrder(Context context) {
        // Value from preferences corresponds to Calendar.<WEEKDAY> value
        // -1 in order to correspond to DAY_ORDER indexing
        final int startDay = Utils.getZeroIndexedFirstDayOfWeek(context);
        final int[] dayOrder = new int[DaysOfWeek.DAYS_IN_A_WEEK];

        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; ++i) {
            dayOrder[i] = DAY_ORDER[(startDay + i) % DaysOfWeek.DAYS_IN_A_WEEK];
        }
        return dayOrder;
    }

}
