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
import android.support.annotation.VisibleForTesting;
import android.util.ArrayMap;

import com.android.deskclock.R;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.FRIDAY;
import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;
import static java.util.Calendar.THURSDAY;
import static java.util.Calendar.TUESDAY;
import static java.util.Calendar.WEDNESDAY;

/**
 * This class is responsible for encoding a weekly repeat cycle in a {@link #getBits bitset}. It
 * also converts between those bits and the {@link Calendar#DAY_OF_WEEK} values for easier mutation
 * and querying.
 */
public final class Weekdays {

    /**
     * The preferred starting day of the week can differ by locale. This enumerated value is used to
     * describe the preferred ordering.
     */
    public enum Order {
        SAT_TO_FRI(SATURDAY, SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
        SUN_TO_SAT(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY),
        MON_TO_SUN(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY);

        private final List<Integer> mCalendarDays;

        Order(Integer... calendarDays) {
            mCalendarDays = Arrays.asList(calendarDays);
        }

        public List<Integer> getCalendarDays() {
            return mCalendarDays;
        }
    }

    /** All valid bits set. */
    private static final int ALL_DAYS = 0x7F;

    /** An instance with all weekdays in the weekly repeat cycle. */
    public static final Weekdays ALL = Weekdays.fromBits(ALL_DAYS);

    /** An instance with no weekdays in the weekly repeat cycle. */
    public static final Weekdays NONE = Weekdays.fromBits(0);

    /** Maps calendar weekdays to the bit masks that represent them in this class. */
    private static final Map<Integer, Integer> sCalendarDayToBit;
    static {
        final Map<Integer, Integer> map = new ArrayMap<>(7);
        map.put(MONDAY,    0x01);
        map.put(TUESDAY,   0x02);
        map.put(WEDNESDAY, 0x04);
        map.put(THURSDAY,  0x08);
        map.put(FRIDAY,    0x10);
        map.put(SATURDAY,  0x20);
        map.put(SUNDAY,    0x40);
        sCalendarDayToBit = Collections.unmodifiableMap(map);
    }

    /** An encoded form of a weekly repeat schedule. */
    private final int mBits;

    private Weekdays(int bits) {
        // Mask off the unused bits.
        mBits = ALL_DAYS & bits;
    }

    /**
     * @param bits {@link #getBits bits} representing the encoded weekly repeat schedule
     * @return a Weekdays instance representing the same repeat schedule as the {@code bits}
     */
    public static Weekdays fromBits(int bits) {
        return new Weekdays(bits);
    }

    /**
     * @param calendarDays an array containing any or all of the following values
     *                     <ul>
     *                     <li>{@link Calendar#SUNDAY}</li>
     *                     <li>{@link Calendar#MONDAY}</li>
     *                     <li>{@link Calendar#TUESDAY}</li>
     *                     <li>{@link Calendar#WEDNESDAY}</li>
     *                     <li>{@link Calendar#THURSDAY}</li>
     *                     <li>{@link Calendar#FRIDAY}</li>
     *                     <li>{@link Calendar#SATURDAY}</li>
     *                     </ul>
     * @return a Weekdays instance representing the given {@code calendarDays}
     */
    public static Weekdays fromCalendarDays(int... calendarDays) {
        int bits = 0;
        for (int calendarDay : calendarDays) {
            final Integer bit = sCalendarDayToBit.get(calendarDay);
            if (bit != null) {
                bits = bits | bit;
            }
        }
        return new Weekdays(bits);
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
     * @param on {@code true} if the {@code calendarDay} is on; {@code false} otherwise
     * @return a WeekDays instance with the {@code calendarDay} mutated
     */
    public Weekdays setBit(int calendarDay, boolean on) {
        final Integer bit = sCalendarDayToBit.get(calendarDay);
        if (bit == null) {
            return this;
        }
        return new Weekdays(on ? (mBits | bit) : (mBits & ~bit));
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
     * @return {@code true} if the given {@code calendarDay}
     */
    public boolean isBitOn(int calendarDay) {
        final Integer bit = sCalendarDayToBit.get(calendarDay);
        if (bit == null) {
            throw new IllegalArgumentException(calendarDay + " is not a valid weekday");
        }
        return (mBits & bit) > 0;
    }

    /**
     * @return the weekly repeat schedule encoded as an integer
     */
    public int getBits() { return mBits; }

    /**
     * @return {@code true} iff at least one weekday is enabled in the repeat schedule
     */
    public boolean isRepeating() { return mBits != 0; }

    /**
     * Note: only the day-of-week is read from the {@code time}. The time fields
     * are not considered in this computation.
     *
     * @param time a timestamp relative to which the answer is given
     * @return the number of days between the given {@code time} and the previous enabled weekday
     *      which is always between 1 and 7 inclusive; {@code -1} if no weekdays are enabled
     */
    public int getDistanceToPreviousDay(Calendar time) {
        int calendarDay = time.get(DAY_OF_WEEK);
        for (int count = 1; count <= 7; count++) {
            calendarDay--;
            if (calendarDay < Calendar.SUNDAY) {
                calendarDay = Calendar.SATURDAY;
            }
            if (isBitOn(calendarDay)) {
                return count;
            }
        }

        return -1;
    }

    /**
     * Note: only the day-of-week is read from the {@code time}. The time fields
     * are not considered in this computation.
     *
     * @param time a timestamp relative to which the answer is given
     * @return the number of days between the given {@code time} and the next enabled weekday which
     *      is always between 0 and 6 inclusive; {@code -1} if no weekdays are enabled
     */
    public int getDistanceToNextDay(Calendar time) {
        int calendarDay = time.get(DAY_OF_WEEK);
        for (int count = 0; count < 7; count++) {
            if (isBitOn(calendarDay)) {
                return count;
            }

            calendarDay++;
            if (calendarDay > Calendar.SATURDAY) {
                calendarDay = Calendar.SUNDAY;
            }
        }

        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Weekdays weekdays = (Weekdays) o;
        return mBits == weekdays.mBits;
    }

    @Override
    public int hashCode() {
        return mBits;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(19);
        builder.append("[");
        if (isBitOn(MONDAY)) {
            builder.append(builder.length() > 1 ? " M" : "M");
        }
        if (isBitOn(TUESDAY)) {
            builder.append(builder.length() > 1 ? " T" : "T");
        }
        if (isBitOn(WEDNESDAY)) {
            builder.append(builder.length() > 1 ? " W" : "W");
        }
        if (isBitOn(THURSDAY)) {
            builder.append(builder.length() > 1 ? " Th" : "Th");
        }
        if (isBitOn(FRIDAY)) {
            builder.append(builder.length() > 1 ? " F" : "F");
        }
        if (isBitOn(SATURDAY)) {
            builder.append(builder.length() > 1 ? " Sa" : "Sa");
        }
        if (isBitOn(SUNDAY)) {
            builder.append(builder.length() > 1 ? " Su" : "Su");
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * @param context for accessing resources
     * @param order the order in which to present the weekdays
     * @return the enabled weekdays in the given {@code order}
     */
    public String toString(Context context, Order order) {
        return toString(context, order, false /* forceLongNames */);
    }

    /**
     * @param context for accessing resources
     * @param order the order in which to present the weekdays
     * @return the enabled weekdays in the given {@code order} in a manner that
     *      is most appropriate for talk-back
     */
    public String toAccessibilityString(Context context, Order order) {
        return toString(context, order, true /* forceLongNames */);
    }

    @VisibleForTesting
    int getCount() {
        int count = 0;
        for (int calendarDay = SUNDAY; calendarDay <= SATURDAY; calendarDay++) {
            if (isBitOn(calendarDay)) {
                count++;
            }
        }
        return count;
    }

    /**
     * @param context for accessing resources
     * @param order the order in which to present the weekdays
     * @param forceLongNames if {@code true} the un-abbreviated weekdays are used
     * @return the enabled weekdays in the given {@code order}
     */
    private String toString(Context context, Order order, boolean forceLongNames) {
        if (!isRepeating()) {
            return "";
        }

        if (mBits == ALL_DAYS) {
            return context.getString(R.string.every_day);
        }

        final boolean longNames = forceLongNames || getCount() <= 1;
        final DateFormatSymbols dfs = new DateFormatSymbols();
        final String[] weekdays = longNames ? dfs.getWeekdays() : dfs.getShortWeekdays();

        final String separator = context.getString(R.string.day_concat);

        final StringBuilder builder = new StringBuilder(40);
        for (int calendarDay : order.getCalendarDays()) {
            if (isBitOn(calendarDay)) {
                if (builder.length() > 0) {
                    builder.append(separator);
                }
                builder.append(weekdays[calendarDay]);
            }
        }
        return builder.toString();
    }
}