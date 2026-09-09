// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.CombinedDays;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.ThemeUtils;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * A dialog fragment that allows the user to select or deselect multiple dates on a calendar.
 * <p>
 * When used with repeating alarms (weekdays selected), it allows <b>deselecting</b> specific dates
 * to skip the alarm on those days (e.g., holidays).
 * <p>
 * When used without repeating weekdays, it allows <b>selecting</b> specific dates for one-time
 * alarm triggers.
 * </p>
 */
public class CalendarPickerDialogFragment extends DialogFragment {

    private static final String TAG = "calendar_picker_dialog";

    private static final String ARG_MODE = "mode";
    private static final String ARG_EXISTING_DATES = "existing_dates";
    private static final String ARG_WEEKDAY_BITS = "weekday_bits";

    public static final String REQUEST_KEY = "calendar_picker_request_key";
    public static final String RESULT_DATES_JSON = "result_dates_json";
    public static final String RESULT_MODE = "result_mode";

    public static final int MODE_SELECT = 0;
    public static final int MODE_DESELECT = 1;

    private CalendarView mCalendarView;
    private final Set<String> mSelectedDateKeys = new HashSet<>();
    private int mMode;
    private Typeface mGeneralTypeface;
    private int mAccentStyle;
    private int mWeekdayBits;

    /**
     * Creates a new instance of CalendarPickerDialogFragment.
     *
     * @param mode          either {@link #MODE_SELECT} or {@link #MODE_DESELECT}
     * @param existingDates the existing combined days data
     * @param weekdayBits   the current weekday bitmask (used for display in deselect mode)
     * @return a new CalendarPickerDialogFragment
     */
    @NonNull
    public static CalendarPickerDialogFragment newInstance(int mode, @NonNull CombinedDays existingDates, int weekdayBits) {
        CalendarPickerDialogFragment fragment = new CalendarPickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode);
        args.putString(ARG_EXISTING_DATES, existingDates.toJson());
        args.putInt(ARG_WEEKDAY_BITS, weekdayBits);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Shows this dialog.
     */
    public static void show(@NonNull FragmentManager fragmentManager, @NonNull CalendarPickerDialogFragment fragment) {
        fragment.show(fragmentManager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMode = getArguments() != null ? getArguments().getInt(ARG_MODE, MODE_SELECT) : MODE_SELECT;
        mWeekdayBits = getArguments() != null ? getArguments().getInt(ARG_WEEKDAY_BITS, 0) : 0;

        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        mGeneralTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));
        mAccentStyle = ThemeUtils.getAccentStyle(requireContext(),
            SettingsDAO.isAutoNightAccentColorEnabled(prefs),
            SettingsDAO.getAccentColor(prefs),
            SettingsDAO.getNightAccentColor(prefs));

        // Load existing dates into our set
        if (savedInstanceState != null) {
            String json = savedInstanceState.getString(ARG_EXISTING_DATES, "");
            CombinedDays existing = CombinedDays.fromJson(json);
            if (mMode == MODE_SELECT) {
                mSelectedDateKeys.addAll(existing.getSelectedDates());
            } else {
                mSelectedDateKeys.addAll(existing.getDeselectedDates());
            }
        } else if (getArguments() != null) {
            String json = getArguments().getString(ARG_EXISTING_DATES, "");
            CombinedDays existing = CombinedDays.fromJson(json);
            if (mMode == MODE_SELECT) {
                mSelectedDateKeys.addAll(existing.getSelectedDates());
            } else {
                mSelectedDateKeys.addAll(existing.getDeselectedDates());
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        CombinedDays days = buildCombinedDays();
        outState.putString(ARG_EXISTING_DATES, days.toJson());
        outState.putInt(ARG_MODE, mMode);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.calendar_picker_dialog, null);

        mCalendarView = view.findViewById(R.id.calendar_picker_view);
        ThemeUtils.applyFontToTextViews(view, mGeneralTypeface);

        // Set the first day of the week from settings
        mCalendarView.setFirstDayOfWeek(SettingsDAO.getFirstDayOfWeek(getDefaultSharedPreferences(context)));

        // Mark already-selected dates on the calendar
        for (String dateKey : mSelectedDateKeys) {
            int[] parsed = CombinedDays.parseDateKey(dateKey);
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.clear();
            cal.set(parsed[0], parsed[1], parsed[2]);
            mCalendarView.setDate(cal.getTimeInMillis(), true, true);
        }

        mCalendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            String key = CombinedDays.dateKey(year, month, dayOfMonth);
            if (mSelectedDateKeys.contains(key)) {
                mSelectedDateKeys.remove(key);
            } else {
                mSelectedDateKeys.add(key);
            }
        });

        String title = mMode == MODE_SELECT
            ? getString(R.string.select_dates_title)
            : getString(R.string.exclude_dates_title);

        String confirmText = mMode == MODE_SELECT
            ? getString(R.string.select_dates_confirm)
            : getString(R.string.exclude_dates_confirm);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setView(view);
        builder.setPositiveButton(confirmText, (dialog, which) -> {
            CombinedDays result = buildCombinedDays();
            Bundle resultBundle = new Bundle();
            resultBundle.putString(RESULT_DATES_JSON, result.toJson());
            resultBundle.putInt(RESULT_MODE, mMode);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, resultBundle);
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    @NonNull
    private CombinedDays buildCombinedDays() {
        // Start with the full existing combined days
        String json = getArguments() != null ? getArguments().getString(ARG_EXISTING_DATES, "") : "";
        CombinedDays days = CombinedDays.fromJson(json);

        if (mMode == MODE_SELECT) {
            // Replace selected dates with our current selection
            days = days.clearSelected();
            for (String dateKey : mSelectedDateKeys) {
                int[] parsed = CombinedDays.parseDateKey(dateKey);
                days = days.addSelectedDate(parsed[0], parsed[1], parsed[2]);
            }
        } else {
            // Replace deselected dates with our current selection
            days = days.clearDeselected();
            for (String dateKey : mSelectedDateKeys) {
                int[] parsed = CombinedDays.parseDateKey(dateKey);
                days = days.addDeselectedDate(parsed[0], parsed[1], parsed[2]);
            }
        }
        return days;
    }
}
