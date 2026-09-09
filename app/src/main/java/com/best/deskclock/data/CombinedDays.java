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
    private static final String DATE_FORMAT = "%04d-%02d-%02d";

    private static final CombinedDays EMPTY = new CombinedDays();

    private final List<String> mSelectedDates;
    private final List<String> mDeselectedDates;

    public CombinedDays() {
        mSelectedDates = new ArrayList<>();
        mDeselectedDates = new ArrayList<>();
    }

    private CombinedDays(@NonNull List<String> selectedDates, @NonNull List<String> deselectedDates) {
        mSelectedDates = new ArrayList<>(selectedDates);
        mDeselectedDates = new ArrayList<>(deselectedDates);
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
            return new CombinedDays(selected, deselected);
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
        if (mSelectedDates.isEmpty() && mDeselectedDates.isEmpty()) {
            return EMPTY_JSON;
        }

        try {
            JSONObject obj = new JSONObject();
            obj.put(KEY_SELECTED_DATES, listToJsonArray(mSelectedDates));
            obj.put(KEY_DESELECTED_DATES, listToJsonArray(mDeselectedDates));
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
        return mSelectedDates.isEmpty() && mDeselectedDates.isEmpty();
    }

    /**
     * Returns the next selected date that is on or after the given calendar date.
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
            if (isDateSelected(year, month, day)) {
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

    // Parcelable implementation

    protected CombinedDays(@NonNull Parcel in) {
        mSelectedDates = new ArrayList<>();
        in.readStringList(mSelectedDates);
        mDeselectedDates = new ArrayList<>();
        in.readStringList(mDeselectedDates);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(mSelectedDates);
        dest.writeStringList(mDeselectedDates);
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
            && Objects.equals(mDeselectedDates, that.mDeselectedDates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSelectedDates, mDeselectedDates);
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
