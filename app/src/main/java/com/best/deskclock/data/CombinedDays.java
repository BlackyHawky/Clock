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

    @NonNull
    public CombinedDays addSelectedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        if (mSelectedDates.contains(key) && !mDeselectedDates.contains(key)
            && !mDismissedDates.contains(key)) {
            return this;
        }
        Set<String> selected = mutableCopy(mSelectedDates);
        Set<String> deselected = mutableCopy(mDeselectedDates);
        Set<String> dismissed = mutableCopy(mDismissedDates);
        selected.add(key);
        deselected.remove(key);
        dismissed.remove(key);
        return new CombinedDays(selected, deselected, dismissed);
    }

    @NonNull
    public CombinedDays removeSelectedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        if (!mSelectedDates.contains(key)) {
            return this;
        }
        Set<String> selected = mutableCopy(mSelectedDates);
        Set<String> dismissed = mutableCopy(mDismissedDates);
        selected.remove(key);
        dismissed.remove(key);
        return new CombinedDays(selected, mDeselectedDates, dismissed);
    }

    @NonNull
    public CombinedDays addDeselectedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        if (mDeselectedDates.contains(key) && !mSelectedDates.contains(key)
            && !mDismissedDates.contains(key)) {
            return this;
        }
        Set<String> deselected = mutableCopy(mDeselectedDates);
        Set<String> selected = mutableCopy(mSelectedDates);
        Set<String> dismissed = mutableCopy(mDismissedDates);
        deselected.add(key);
        selected.remove(key);
        dismissed.remove(key);
        return new CombinedDays(selected, deselected, dismissed);
    }

    @NonNull
    public CombinedDays removeDeselectedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        if (!mDeselectedDates.contains(key)) {
            return this;
        }
        Set<String> deselected = mutableCopy(mDeselectedDates);
        Set<String> dismissed = mutableCopy(mDismissedDates);
        deselected.remove(key);
        dismissed.remove(key);
        return new CombinedDays(mSelectedDates, deselected, dismissed);
    }

    @NonNull
    public CombinedDays toggleSelectedDate(int year, int month, int day) {
        return isDateSelected(year, month, day)
            ? removeSelectedDate(year, month, day)
            : addSelectedDate(year, month, day);
    }

    @NonNull
    public CombinedDays toggleDeselectedDate(int year, int month, int day) {
        return isDateDeselected(year, month, day)
            ? removeDeselectedDate(year, month, day)
            : addDeselectedDate(year, month, day);
    }

    @NonNull
    public CombinedDays addDismissedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        if (mDismissedDates.contains(key)) {
            return this;
        }
        Set<String> dismissed = mutableCopy(mDismissedDates);
        dismissed.add(key);
        return new CombinedDays(mSelectedDates, mDeselectedDates, dismissed);
    }

    @NonNull
    public CombinedDays removeDismissedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        if (!mDismissedDates.contains(key)) {
            return this;
        }
        Set<String> dismissed = mutableCopy(mDismissedDates);
        dismissed.remove(key);
        return new CombinedDays(mSelectedDates, mDeselectedDates, dismissed);
    }

    @NonNull
    public CombinedDays clearDismissed() {
        return mDismissedDates.isEmpty()
            ? this
            : new CombinedDays(mSelectedDates, mDeselectedDates, Collections.emptySet());
    }

    @NonNull
    public CombinedDays clear() {
        return EMPTY;
    }

    @NonNull
    public CombinedDays clearDeselected() {
        return mDeselectedDates.isEmpty()
            ? this
            : new CombinedDays(mSelectedDates, Collections.emptySet(), mDismissedDates);
    }

    @NonNull
    public CombinedDays clearSelected() {
        return mSelectedDates.isEmpty()
            ? this
            : new CombinedDays(Collections.emptySet(), mDeselectedDates, mDismissedDates);
    }

    public boolean isDateSelected(int year, int month, int day) {
        return mSelectedDates.contains(dateKey(year, month, day));
    }

    public boolean isDateDeselected(int year, int month, int day) {
        return mDeselectedDates.contains(dateKey(year, month, day));
    }

    public boolean isDateDismissed(int year, int month, int day) {
        return mDismissedDates.contains(dateKey(year, month, day));
    }

    public boolean isDateActive(int year, int month, int day, @NonNull Weekdays weekdays) {
        String key = dateKey(year, month, day);
        if (mDismissedDates.contains(key)) {
            return false;
        }
        if (matchesWeekdays(key, weekdays)) {
            return !mDeselectedDates.contains(key);
        }
        return mSelectedDates.contains(key);
    }

    @NonNull
    private static Calendar calendarForDateKey(@NonNull String key, @NonNull TimeZone zone) {
        int[] date = parseDateKey(key);
        Calendar calendar = Calendar.getInstance(zone);
        calendar.clear();
        calendar.set(date[0], date[1], date[2]);
        return calendar;
    }

    private static boolean matchesWeekdays(@NonNull String key, @NonNull Weekdays weekdays) {
        int[] date = parseDateKey(key);
        Calendar calendar = calendarForDateKey(key, TimeZone.getTimeZone("UTC"));
        return weekdays.isBitOn(calendar.get(Calendar.DAY_OF_WEEK));
    }

    private static Set<String> mutableCopy(@NonNull Collection<String> values) {
        return new TreeSet<>(values);
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
