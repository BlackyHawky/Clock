// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Represents a combined system of weekdays and specific dates for alarm scheduling.
 * <p>
 * When weekdays are selected, specific dates can be <b>deselected</b> from that pattern
 * (e.g., to skip holidays). When no weekdays are selected, specific dates can be
 * <b>individually selected</b> as one-time alarm triggers.
 * <p>
 * The data is serialized to/from a JSON string for database storage:
 * <pre>
 * {
 *   "selectedDates": ["2024-01-12", "2024-01-22"],
 *   "deselectedDates": ["2024-01-10"]
 * }
 * </pre>
 */
public final class CombinedDays implements Parcelable {

    public static final String EMPTY_JSON = "";
    private static final String KEY_SELECTED_DATES = "selectedDates";
    private static final String KEY_DESELECTED_DATES = "deselectedDates";
    private static final String KEY_DISMISSED_DATES = "dismissedDates";
    private static final String DATE_FORMAT = "%04d-%02d-%02d";

    private static final CombinedDays EMPTY = new CombinedDays();

    private final List<String> mSelectedDates;
    private final List<String> mDeselectedDates;
    private final List<String> mDismissedDates;

    public CombinedDays() {
        mSelectedDates = new ArrayList<>();
        mDeselectedDates = new ArrayList<>();
        mDismissedDates = new ArrayList<>();
    }

    private CombinedDays(@NonNull List<String> selectedDates, @NonNull List<String> deselectedDates) {
        this(selectedDates, deselectedDates, new ArrayList<>());
    }

    private CombinedDays(@NonNull List<String> selectedDates, @NonNull List<String> deselectedDates,
                         @NonNull List<String> dismissedDates) {
        mSelectedDates = new ArrayList<>(selectedDates);
        mDeselectedDates = new ArrayList<>(deselectedDates);
        mDismissedDates = new ArrayList<>(dismissedDates);
    }

    /**
     * Creates a CombinedDays from a JSON string stored in the database.
     *
     * @param json the JSON string, or null/empty for an empty CombinedDays
     * @return a CombinedDays instance
     */
    @NonNull
    public static CombinedDays fromJson(@Nullable String json) {
        if (json == null || json.isEmpty()) {
            return EMPTY;
        }

        try {
            JSONObject obj = new JSONObject(json);
            List<String> selected = jsonArrayToList(obj.optJSONArray(KEY_SELECTED_DATES));
            List<String> deselected = jsonArrayToList(obj.optJSONArray(KEY_DESELECTED_DATES));
            List<String> dismissed = jsonArrayToList(obj.optJSONArray(KEY_DISMISSED_DATES));
            return new CombinedDays(selected, deselected, dismissed);
        } catch (JSONException e) {
            return EMPTY;
        }
    }

    /**
     * Converts this instance to a JSON string for database storage.
     *
     * @return the JSON string, or {@link #EMPTY_JSON} if both lists are empty
     */
    @NonNull
    public String toJson() {
        if (mSelectedDates.isEmpty() && mDeselectedDates.isEmpty() && mDismissedDates.isEmpty()) {
            return EMPTY_JSON;
        }

        try {
            JSONObject obj = new JSONObject();
            obj.put(KEY_SELECTED_DATES, listToJsonArray(mSelectedDates));
            obj.put(KEY_DESELECTED_DATES, listToJsonArray(mDeselectedDates));
            obj.put(KEY_DISMISSED_DATES, listToJsonArray(mDismissedDates));
            return obj.toString();
        } catch (JSONException e) {
            return EMPTY_JSON;
        }
    }

    /**
     * Creates a date string key in the format "YYYY-MM-DD" from calendar fields.
     *
     * @param year  the year
     * @param month the month (0-based, as in {@link Calendar#MONTH})
     * @param day   the day of month
     * @return a date key string
     */
    @NonNull
    public static String dateKey(int year, int month, int day) {
        return String.format(java.util.Locale.ROOT, DATE_FORMAT, year, month + 1, day);
    }

    /**
     * Parses a date key string back into year, month (0-based), and day.
     *
     * @param dateKey a date key in "YYYY-MM-DD" format
     * @return a 3-element array: {year, month (0-based), day}
     * @throws IllegalArgumentException if the dateKey is malformed
     */
    public static int[] parseDateKey(@NonNull String dateKey) {
        String[] parts = dateKey.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid date key: " + dateKey);
        }
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1; // Convert back to 0-based
        int day = Integer.parseInt(parts[2]);
        return new int[]{year, month, day};
    }

    /**
     * Adds a date to the selected dates list (for one-time date alarms).
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return a new CombinedDays with the date added
     */
    @NonNull
    public CombinedDays addSelectedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        if (mSelectedDates.contains(key)) {
            return this;
        }
        List<String> newSelected = new ArrayList<>(mSelectedDates);
        newSelected.add(key);
        return new CombinedDays(newSelected, mDeselectedDates);
    }

    /**
     * Removes a date from the selected dates list.
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return a new CombinedDays with the date removed
     */
    @NonNull
    public CombinedDays removeSelectedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        List<String> newSelected = new ArrayList<>(mSelectedDates);
        newSelected.remove(key);
        return new CombinedDays(newSelected, mDeselectedDates);
    }

    /**
     * Adds a date to the deselected dates list (to skip a weekday).
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return a new CombinedDays with the date added to deselected
     */
    @NonNull
    public CombinedDays addDeselectedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        if (mDeselectedDates.contains(key)) {
            return this;
        }
        List<String> newDeselected = new ArrayList<>(mDeselectedDates);
        newDeselected.add(key);
        return new CombinedDays(mSelectedDates, newDeselected);
    }

    /**
     * Removes a date from the deselected dates list (to re-enable a weekday).
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return a new CombinedDays with the date removed from deselected
     */
    @NonNull
    public CombinedDays removeDeselectedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        List<String> newDeselected = new ArrayList<>(mDeselectedDates);
        newDeselected.remove(key);
        return new CombinedDays(mSelectedDates, newDeselected);
    }

    /**
     * Toggles a date's selection state for one-time date selection.
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return a new CombinedDays with the date toggled
     */
    @NonNull
    public CombinedDays toggleSelectedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        List<String> newSelected = new ArrayList<>(mSelectedDates);
        if (newSelected.contains(key)) {
            newSelected.remove(key);
        } else {
            newSelected.add(key);
        }
        return new CombinedDays(newSelected, mDeselectedDates);
    }

    /**
     * Toggles a date's deselection state for weekday-based deselection.
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return a new CombinedDays with the date toggled
     */
    @NonNull
    public CombinedDays toggleDeselectedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        List<String> newDeselected = new ArrayList<>(mDeselectedDates);
        if (newDeselected.contains(key)) {
            newDeselected.remove(key);
        } else {
            newDeselected.add(key);
        }
        return new CombinedDays(mSelectedDates, newDeselected);
    }

    /**
     * Adds a date to the dismissed dates list (a transient skip used when an upcoming
     * occurrence of an active weekday is preemptively dismissed). These are not part of the
     * user's calendar selection and are cleared when the alarm is re-enabled.
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return a new CombinedDays with the date added to dismissed
     */
    @NonNull
    public CombinedDays addDismissedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        if (mDismissedDates.contains(key)) {
            return this;
        }
        List<String> newDismissed = new ArrayList<>(mDismissedDates);
        newDismissed.add(key);
        return new CombinedDays(mSelectedDates, mDeselectedDates, newDismissed);
    }

    /**
     * Removes a date from the dismissed dates list.
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return a new CombinedDays with the date removed from dismissed
     */
    @NonNull
    public CombinedDays removeDismissedDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        List<String> newDismissed = new ArrayList<>(mDismissedDates);
        newDismissed.remove(key);
        return new CombinedDays(mSelectedDates, mDeselectedDates, newDismissed);
    }

    /**
     * Removes all dismissed dates. Used when the alarm is re-enabled so that all weekday
     * occurrences are scheduled again.
     *
     * @return a new CombinedDays with an empty dismissed list
     */
    @NonNull
    public CombinedDays clearDismissed() {
        return new CombinedDays(mSelectedDates, mDeselectedDates);
    }

    /**
     * @return true if there are any dismissed dates
     */
    public boolean hasDismissedDates() {
        return !mDismissedDates.isEmpty();
    }

    /**
     * Checks if a specific date is in the dismissed dates list.
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return true if the date is dismissed
     */
    public boolean isDateDismissed(int year, int month, int day) {
        return mDismissedDates.contains(dateKey(year, month, day));
    }

    /**
     * @return an unmodifiable list of dismissed date keys
     */
    @NonNull
    public List<String> getDismissedDates() {
        return List.copyOf(mDismissedDates);
    }

    /**
     * Clears all selected and deselected dates.
     *
     * @return an empty CombinedDays
     */
    @NonNull
    public CombinedDays clear() {
        return EMPTY;
    }

    /**
     * Clears only the deselected dates.
     *
     * @return a new CombinedDays with empty deselected list
     */
    @NonNull
    public CombinedDays clearDeselected() {
        return new CombinedDays(mSelectedDates, new ArrayList<>());
    }

    /**
     * Clears only the selected dates.
     *
     * @return a new CombinedDays with empty selected list
     */
    @NonNull
    public CombinedDays clearSelected() {
        return new CombinedDays(new ArrayList<>(), mDeselectedDates);
    }

    /**
     * Removes redundant overrides based on the current weekday selection.
     * <p>
     * A deselected date is redundant if its weekday is not currently selected (the alarm
     * wouldn't fire on that day anyway, so excluding it is pointless).
     * <p>
     * A selected date is redundant if its weekday IS currently selected (the alarm would
     * already fire on that day, so explicitly selecting it is pointless).
     *
     * @param weekdays the current weekday bitmask
     * @return a new CombinedDays with redundant overrides removed
     */
    @NonNull
    public CombinedDays cleanup(@NonNull Weekdays weekdays) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        List<String> cleanedDeselected = new ArrayList<>();
        for (String dateKey : mDeselectedDates) {
            try {
                int[] parsed = parseDateKey(dateKey);
                cal.clear();
                cal.set(parsed[0], parsed[1], parsed[2]);
                int calendarDay = cal.get(Calendar.DAY_OF_WEEK);
                if (weekdays.isBitOn(calendarDay)) {
                    cleanedDeselected.add(dateKey);
                }
            } catch (IllegalArgumentException e) {
                // Skip malformed date key
            }
        }

        List<String> cleanedSelected = new ArrayList<>();
        for (String dateKey : mSelectedDates) {
            try {
                int[] parsed = parseDateKey(dateKey);
                cal.clear();
                cal.set(parsed[0], parsed[1], parsed[2]);
                int calendarDay = cal.get(Calendar.DAY_OF_WEEK);
                if (!weekdays.isBitOn(calendarDay)) {
                    cleanedSelected.add(dateKey);
                }
            } catch (IllegalArgumentException e) {
                // Skip malformed date key
            }
        }

        List<String> cleanedDismissed = new ArrayList<>();
        for (String dateKey : mDismissedDates) {
            try {
                int[] parsed = parseDateKey(dateKey);
                cal.clear();
                cal.set(parsed[0], parsed[1], parsed[2]);
                int calendarDay = cal.get(Calendar.DAY_OF_WEEK);
                if (weekdays.isBitOn(calendarDay)) {
                    cleanedDismissed.add(dateKey);
                }
            } catch (IllegalArgumentException e) {
                // Skip malformed date key
            }
        }

        return new CombinedDays(cleanedSelected, cleanedDeselected, cleanedDismissed);
    }

    /**
     * Checks if there are any redundant overrides that could be cleaned up.
     *
     * @param weekdays the current weekday bitmask
     * @return true if any overrides are redundant
     */
    public boolean hasRedundantOverrides(@NonNull Weekdays weekdays) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        for (String dateKey : mDeselectedDates) {
            try {
                int[] parsed = parseDateKey(dateKey);
                cal.clear();
                cal.set(parsed[0], parsed[1], parsed[2]);
                int calendarDay = cal.get(Calendar.DAY_OF_WEEK);
                if (!weekdays.isBitOn(calendarDay)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // Skip malformed date key
            }
        }
        for (String dateKey : mSelectedDates) {
            try {
                int[] parsed = parseDateKey(dateKey);
                cal.clear();
                cal.set(parsed[0], parsed[1], parsed[2]);
                int calendarDay = cal.get(Calendar.DAY_OF_WEEK);
                if (weekdays.isBitOn(calendarDay)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // Skip malformed date key
            }
        }
        return false;
    }

    /**
     * Checks if a specific date is in the selected dates list.
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return true if the date is selected
     */
    public boolean isDateSelected(int year, int month, int day) {
        return mSelectedDates.contains(dateKey(year, month, day));
    }

    /**
     * Checks if a specific date is in the deselected dates list.
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return true if the date is deselected
     */
    public boolean isDateDeselected(int year, int month, int day) {
        return mDeselectedDates.contains(dateKey(year, month, day));
    }

    /**
     * Determines if the alarm will fire on a specific date given the current weekday selection.
     * <p>
     * A date is active if its weekday is selected and the date is not in the deselected list,
     * or if its weekday is not selected but the date IS in the selected list. This matches the
     * logic used to display the inline calendar.
     *
     * @param year     the year
     * @param month    the month (0-based)
     * @param day      the day of month
     * @param weekdays the current weekday bitmask
     * @return true if the alarm fires on this date
     */
    public boolean isDateActive(int year, int month, int day, @NonNull Weekdays weekdays) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year, month, day);
        int calendarDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        boolean matchesWeekday = weekdays.isBitOn(calendarDayOfWeek);
        boolean isDeselected = mDeselectedDates.contains(dateKey(year, month, day));
        boolean isSelected = mSelectedDates.contains(dateKey(year, month, day));

        return matchesWeekday ? !isDeselected : isSelected;
    }

    /**
     * @return an unmodifiable list of selected date keys
     */
    @NonNull
    public List<String> getSelectedDates() {
        return List.copyOf(mSelectedDates);
    }

    /**
     * @return an unmodifiable list of deselected date keys
     */
    @NonNull
    public List<String> getDeselectedDates() {
        return List.copyOf(mDeselectedDates);
    }

    /**
     * @return true if there are any selected dates
     */
    public boolean hasSelectedDates() {
        return !mSelectedDates.isEmpty();
    }

    /**
     * @return true if there are any deselected dates
     */
    public boolean hasDeselectedDates() {
        return !mDeselectedDates.isEmpty();
    }

    /**
     * @return true if this CombinedDays has no data at all
     */
    public boolean isEmpty() {
        return mSelectedDates.isEmpty() && mDeselectedDates.isEmpty() && mDismissedDates.isEmpty();
    }

    /**
     * Returns the next selected date that is on or after the given calendar date.
     * Dismissed dates are skipped so that a dismissed occurrence does not get scheduled again.
     *
     * @param from the starting date
     * @return the next selected date as a Calendar (with only year/month/day set), or null if none found within 365 days
     */
    @Nullable
    public Calendar getNextSelectedDate(@NonNull Calendar from) {
        Calendar search = (Calendar) from.clone();
        for (int i = 0; i < 366; i++) {
            int year = search.get(Calendar.YEAR);
            int month = search.get(Calendar.MONTH);
            int day = search.get(Calendar.DAY_OF_MONTH);
            if (isDateSelected(year, month, day) && !isDateDismissed(year, month, day)) {
                Calendar result = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                result.clear();
                result.set(year, month, day);
                return result;
            }
            search.add(Calendar.DAY_OF_MONTH, 1);
        }
        return null;
    }

    /**
     * @return the number of selected dates
     */
    public int getSelectedDateCount() {
        return mSelectedDates.size();
    }

    /**
     * @return the number of deselected dates
     */
    public int getDeselectedDateCount() {
        return mDeselectedDates.size();
    }

    /**
     * Removes a specific date from both selected and deselected lists.
     *
     * @param year  the year
     * @param month the month (0-based)
     * @param day   the day of month
     * @return a new CombinedDays with the date removed
     */
    @NonNull
    public CombinedDays removeDate(int year, int month, int day) {
        String key = dateKey(year, month, day);
        List<String> newSelected = new ArrayList<>(mSelectedDates);
        newSelected.remove(key);
        List<String> newDeselected = new ArrayList<>(mDeselectedDates);
        newDeselected.remove(key);
        List<String> newDismissed = new ArrayList<>(mDismissedDates);
        newDismissed.remove(key);
        return new CombinedDays(newSelected, newDeselected, newDismissed);
    }

    /**
     * Removes all dates (both selected and deselected) that are in the past relative to today.
     *
     * @return a new CombinedDays with past dates removed
     */
    @NonNull
    public CombinedDays removePastDates() {
        return removePastDates(-1, -1);
    }

    /**
     * Removes all dates (both selected and deselected) that are in the past relative to today.
     * If hour and minute are provided (>= 0), today is also removed if its alarm time has passed.
     *
     * @param alarmHour   the alarm hour (0-23), or -1 to ignore time check
     * @param alarmMinute the alarm minute (0-59), or -1 to ignore time check
     * @return a new CombinedDays with past dates removed
     */
    @NonNull
    public CombinedDays removePastDates(int alarmHour, int alarmMinute) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        boolean todayIsPast = false;
        if (alarmHour >= 0 && alarmMinute >= 0) {
            Calendar alarmToday = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            alarmToday.set(Calendar.HOUR_OF_DAY, alarmHour);
            alarmToday.set(Calendar.MINUTE, alarmMinute);
            alarmToday.set(Calendar.SECOND, 0);
            alarmToday.set(Calendar.MILLISECOND, 0);
            todayIsPast = now.getTimeInMillis() >= alarmToday.getTimeInMillis();
        }

        List<String> newSelected = new ArrayList<>();
        for (String key : mSelectedDates) {
            try {
                int[] parsed = parseDateKey(key);
                Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                date.set(parsed[0], parsed[1], parsed[2], 0, 0, 0);
                date.set(Calendar.MILLISECOND, 0);
                boolean isToday = date.equals(today);
                if (isToday && todayIsPast) continue;
                if (!date.before(today)) {
                    newSelected.add(key);
                }
            } catch (IllegalArgumentException e) {
                // Skip malformed date key
            }
        }

        List<String> newDeselected = new ArrayList<>();
        for (String key : mDeselectedDates) {
            try {
                int[] parsed = parseDateKey(key);
                Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                date.set(parsed[0], parsed[1], parsed[2], 0, 0, 0);
                date.set(Calendar.MILLISECOND, 0);
                boolean isToday = date.equals(today);
                if (isToday && todayIsPast) continue;
                if (!date.before(today)) {
                    newDeselected.add(key);
                }
            } catch (IllegalArgumentException e) {
                // Skip malformed date key
            }
        }

        List<String> newDismissed = new ArrayList<>();
        for (String key : mDismissedDates) {
            try {
                int[] parsed = parseDateKey(key);
                Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                date.set(parsed[0], parsed[1], parsed[2], 0, 0, 0);
                date.set(Calendar.MILLISECOND, 0);
                boolean isToday = date.equals(today);
                if (isToday && todayIsPast) continue;
                if (!date.before(today)) {
                    newDismissed.add(key);
                }
            } catch (IllegalArgumentException e) {
                // Skip malformed date key
            }
        }

        return new CombinedDays(newSelected, newDeselected, newDismissed);
    }

    // Parcelable implementation

    protected CombinedDays(@NonNull Parcel in) {
        mSelectedDates = new ArrayList<>();
        in.readStringList(mSelectedDates);
        mDeselectedDates = new ArrayList<>();
        in.readStringList(mDeselectedDates);
        mDismissedDates = new ArrayList<>();
        in.readStringList(mDismissedDates);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(mSelectedDates);
        dest.writeStringList(mDeselectedDates);
        dest.writeStringList(mDismissedDates);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CombinedDays> CREATOR = new Creator<>() {
        @NonNull
        @Override
        public CombinedDays createFromParcel(@NonNull Parcel in) {
            return new CombinedDays(in);
        }

        @NonNull
        @Override
        public CombinedDays[] newArray(int size) {
            return new CombinedDays[size];
        }
    };

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CombinedDays that = (CombinedDays) o;
        return Objects.equals(mSelectedDates, that.mSelectedDates)
            && Objects.equals(mDeselectedDates, that.mDeselectedDates)
            && Objects.equals(mDismissedDates, that.mDismissedDates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSelectedDates, mDeselectedDates, mDismissedDates);
    }

    @NonNull
    @Override
    public String toString() {
        return "CombinedDays{" +
            "selected=" + mSelectedDates +
            ", deselected=" + mDeselectedDates +
            '}';
    }

    // Private helpers

    @NonNull
    private static List<String> jsonArrayToList(@Nullable JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                list.add(array.optString(i));
            }
        }
        return list;
    }

    @NonNull
    private static JSONArray listToJsonArray(@NonNull List<String> list) {
        JSONArray array = new JSONArray();
        for (String item : list) {
            array.put(item);
        }
        return array;
    }

}
