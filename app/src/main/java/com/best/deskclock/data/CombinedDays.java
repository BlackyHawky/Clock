/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

/**
 * Immutable date overrides for an alarm's weekly schedule.
 *
 * <p>Selected dates add an occurrence outside the selected weekdays; deselected dates skip an
 * otherwise-active weekday occurrence. Dismissed dates are temporary skips used by the
 * pre-emptive dismiss action and are cleared when the alarm is re-enabled.</p>
 *
 * <p>Dates are stored as ISO civil-date keys, independent of time zone. Database values use a
 * compact JSON string; alarm backups use a nested JSON object.</p>
 */
public final class CombinedDays {

    public static final String EMPTY_JSON = "";

    private static final String KEY_VERSION = "version";
    private static final String KEY_SELECTED_DATES = "selectedDates";
    private static final String KEY_DESELECTED_DATES = "deselectedDates";
    private static final String KEY_DISMISSED_DATES = "dismissedDates";
    private static final int VERSION = 1;
    private static final CombinedDays EMPTY =
        new CombinedDays(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

    private final Set<String> mSelectedDates;
    private final Set<String> mDeselectedDates;
    private final Set<String> mDismissedDates;

    public CombinedDays() {
        this(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
    }

    private CombinedDays(@NonNull Collection<String> selectedDates,
                         @NonNull Collection<String> deselectedDates,
                         @NonNull Collection<String> dismissedDates) {
        mSelectedDates = immutableDateKeys(selectedDates);
        mDeselectedDates = immutableDateKeys(deselectedDates);
        mDismissedDates = immutableDateKeys(dismissedDates);
    }

    /**
     * Creates a date key from a year, zero-based month, and day.
     *
     * @throws IllegalArgumentException if the supplied civil date is invalid
     */
    @NonNull
    public static String dateKey(int year, int month, int day) {
        validateDate(year, month, day);
        return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day);
    }

    /**
     * Parses an ISO date key into {@code {year, zeroBasedMonth, day}}.
     *
     * @throws IllegalArgumentException if the key is malformed or is not a real date
     */
    @NonNull
    public static int[] parseDateKey(@NonNull String dateKey) {
        if (!dateKey.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Invalid date key: " + dateKey);
        }
        try {
            int year = Integer.parseInt(dateKey.substring(0, 4));
            int month = Integer.parseInt(dateKey.substring(5, 7)) - 1;
            int day = Integer.parseInt(dateKey.substring(8, 10));
            validateDate(year, month, day);
            return new int[]{year, month, day};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid date key: " + dateKey, e);
        }
    }

    private static void validateDate(int year, int month, int day) {
        if (year < 1 || year > 9999 || month < Calendar.JANUARY || month > Calendar.DECEMBER) {
            throw new IllegalArgumentException("Invalid date: " + year + "-" + (month + 1) + "-" + day);
        }
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.setLenient(false);
        calendar.set(year, month, day);
        try {
            calendar.getTimeInMillis();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid date: " + year + "-" + (month + 1) + "-" + day, e);
        }
    }

    private static Set<String> immutableDateKeys(@NonNull Collection<String> values) {
        Set<String> keys = new TreeSet<>();
        for (String value : values) {
            try {
                int[] date = parseDateKey(value);
                keys.add(dateKey(date[0], date[1], date[2]));
            } catch (IllegalArgumentException ignored) {
                // Invalid values from old or hand-edited backups are ignored.
            }
        }
        return Collections.unmodifiableSet(keys);
    }
}
