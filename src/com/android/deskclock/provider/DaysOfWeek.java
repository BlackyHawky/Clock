/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.deskclock.provider;

import android.content.Context;

import com.android.deskclock.R;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;

/*
 * Days of week code as a single int.
 * 0x00: no day
 * 0x01: Monday
 * 0x02: Tuesday
 * 0x04: Wednesday
 * 0x08: Thursday
 * 0x10: Friday
 * 0x20: Saturday
 * 0x40: Sunday
 */
public final class DaysOfWeek {
    // Number if days in the week.
    public static final int DAYS_IN_A_WEEK = 7;

    // Value when all days are set
    public static final int ALL_DAYS_SET = 0x7f;

    // Value when no days are set
    public static final int NO_DAYS_SET = 0;

    /**
     * Need to have monday start at index 0 to be backwards compatible. This converts
     * Calendar.DAY_OF_WEEK constants to our internal bit structure.
     */
    static int convertDayToBitIndex(int day) {
        return (day + 5) % DAYS_IN_A_WEEK;
    }

    /**
     * Need to have monday start at index 0 to be backwards compatible. This converts
     * our bit structure to Calendar.DAY_OF_WEEK constant value.
     */
    static int convertBitIndexToDay(int bitIndex) {
        return (bitIndex + 1) % DAYS_IN_A_WEEK + 1;
    }

    // Bitmask of all repeating days
    private int mBitSet;

    public DaysOfWeek(int bitSet) {
        mBitSet = bitSet;
    }

    public String toString(Context context, int firstDay) {
        return toString(context, firstDay, false /* forAccessibility */);
    }

    public String toAccessibilityString(Context context, int firstDay) {
        return toString(context, firstDay, true /* forAccessibility */);
    }

    private String toString(Context context, int firstDay, boolean forAccessibility) {
        StringBuilder ret = new StringBuilder();

        // no days
        if (mBitSet == NO_DAYS_SET) {
            return "";
        }

        // every day
        if (mBitSet == ALL_DAYS_SET) {
            return context.getText(R.string.every_day).toString();
        }

        // count selected days
        int dayCount = 0;
        int bitSet = mBitSet;
        while (bitSet > 0) {
            if ((bitSet & 1) == 1) dayCount++;
            bitSet >>= 1;
        }

        // short or long form?
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] dayList = (forAccessibility || dayCount <= 1) ?
                dfs.getWeekdays() :
                dfs.getShortWeekdays();

        // In this system, Mon = 0, Sun = 6, etc.
        // startDay is stored corresponding to Calendar.DAY_OF_WEEK where Sun = 0, Mon = 2, etc.
        final int startDay = convertDayToBitIndex(firstDay);

        // selected days, starting from user-selected start day of week
        // iterate starting from user-selected start of day
        for (int bitIndex = startDay; bitIndex < DAYS_IN_A_WEEK + startDay; ++bitIndex) {
            if ((mBitSet & (1 << (bitIndex % DAYS_IN_A_WEEK))) != 0) {
                ret.append(dayList[convertBitIndexToDay(bitIndex)]);
                dayCount -= 1;
                if (dayCount > 0) ret.append(context.getText(R.string.day_concat));
            }
        }
        return ret.toString();
    }

    /**
     * Enables or disable certain days of the week.
     *
     * @param daysOfWeek Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, etc.
     */
    public void setDaysOfWeek(boolean value, int ... daysOfWeek) {
        for (int day : daysOfWeek) {
            setBit(convertDayToBitIndex(day), value);
        }
    }

    private boolean isBitEnabled(int bitIndex) {
        return ((mBitSet & (1 << bitIndex)) > 0);
    }

    private void setBit(int bitIndex, boolean set) {
        if (set) {
            mBitSet |= (1 << bitIndex);
        } else {
            mBitSet &= ~(1 << bitIndex);
        }
    }

    public void setBitSet(int bitSet) {
        mBitSet = bitSet;
    }

    public int getBitSet() {
        return mBitSet;
    }

    /**
     * Returns set of Calendar.MONDAY, Calendar.TUESDAY, etc based on the current mBitSet value
     */
    public HashSet<Integer> getSetDays() {
        final HashSet<Integer> result = new HashSet<Integer>();
        for (int bitIndex = 0; bitIndex < DAYS_IN_A_WEEK; bitIndex++) {
            if (isBitEnabled(bitIndex)) {
                result.add(convertBitIndexToDay(bitIndex));
            }
        }
        return result;
    }

    public boolean isRepeating() {
        return mBitSet != NO_DAYS_SET;
    }

    /**
     * Returns number of days backwards from today to previous alarm.
     * ex:
     * Daily alarm, current = Tuesday -> 1
     * Weekly alarm on Wednesday, current = Tuesday -> 6
     * One time alarm -> -1
     *
     * @param current must be set to today
     */
    public int calculateDaysToPreviousAlarm(Calendar current) {
        if (!isRepeating()) {
            return -1;
        }

        // We only use this on preemptively dismissed alarms, and alarms can only fire once a day,
        // so there is no chance that the previous fire time is on the same day. Start dayCount on
        // previous day.
        int dayCount = -1;
        int currentDayIndex = convertDayToBitIndex(current.get(Calendar.DAY_OF_WEEK));
        for (; dayCount >= -DAYS_IN_A_WEEK; dayCount--) {
            int previousAlarmBitIndex = (currentDayIndex + dayCount);
            if (previousAlarmBitIndex < 0) {
                // Ex. previousAlarmBitIndex = -1 means the day before index 0 = index 6
                previousAlarmBitIndex = previousAlarmBitIndex + DAYS_IN_A_WEEK;
            }
            if (isBitEnabled(previousAlarmBitIndex)) {
                break;
            }
        }
        // return a positive value
        return dayCount * -1;
    }

    /**
     * Returns number of days from today until next alarm.
     *
     * @param current must be set to today or the day after the currentTime
     */
    public int calculateDaysToNextAlarm(Calendar current) {
        if (!isRepeating()) {
            return -1;
        }

        int dayCount = 0;
        int currentDayIndex = convertDayToBitIndex(current.get(Calendar.DAY_OF_WEEK));
        for (; dayCount < DAYS_IN_A_WEEK; dayCount++) {
            int nextAlarmBitIndex = (currentDayIndex + dayCount) % DAYS_IN_A_WEEK;
            if (isBitEnabled(nextAlarmBitIndex)) {
                break;
            }
        }
        return dayCount;
    }

    public void clearAllDays() {
        mBitSet = NO_DAYS_SET;
    }

    @Override
    public String toString() {
        return "DaysOfWeek{" +
                "mBitSet=" + mBitSet +
                '}';
    }
}
