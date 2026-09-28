// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.databinding.SpinnerDatePickerBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.Utils;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Custom component to display a date selection dialog using spinners.
 */
public class SpinnerDatePickerDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of {@link SpinnerDatePickerDialogFragment} in the fragment manager.
     */
    public static final String TAG = "spinner_date_picker_dialog";


    public static final String REQUEST_KEY = "spinner_date_picker_request_key";
    public static final String BUNDLE_KEY_YEAR = "bundle_key_year";
    public static final String BUNDLE_KEY_MONTH = "bundle_key_month";
    public static final String BUNDLE_KEY_DAY = "bundle_key_day";

    private static final String ARG_MIN_DATE = "arg_min_date";
    private static final String ARG_YEAR = "arg_year";
    private static final String ARG_MONTH = "arg_month";
    private static final String ARG_DAY = "arg_day";

    private SpinnerDatePickerBinding mBinding;

    /**
     * Creates a new instance of {@link SpinnerDatePickerDialogFragment} for use in the alarm editing panel.
     *
     * @param year  The selected hours.
     * @param month The selected month.
     * @param day   The selected day.
     */
    @NonNull
    public static SpinnerDatePickerDialogFragment newInstance(long minDate, int year, int month, int day) {
        Bundle args = new Bundle();
        args.putLong(ARG_MIN_DATE, minDate);
        args.putInt(ARG_YEAR, year);
        args.putInt(ARG_MONTH, month);
        args.putInt(ARG_DAY, day);

        SpinnerDatePickerDialogFragment fragment = new SpinnerDatePickerDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Displays {@link SpinnerDatePickerDialogFragment}.
     */
    public static void show(@NonNull FragmentManager manager, @NonNull SpinnerDatePickerDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // As long as the dialog exists, save its state.
        outState.putLong(ARG_MIN_DATE, mBinding.spinnerDatePicker.getMinDate());
        outState.putInt(ARG_YEAR, mBinding.spinnerDatePicker.getYear());
        outState.putInt(ARG_MONTH, mBinding.spinnerDatePicker.getMonth());
        outState.putInt(ARG_DAY, mBinding.spinnerDatePicker.getDayOfMonth());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        long minDate = args.getLong(ARG_MIN_DATE);
        int year = args.getInt(ARG_YEAR);
        int month = args.getInt(ARG_MONTH);
        int day = args.getInt(ARG_DAY);

        if (savedInstanceState != null) {
            minDate = savedInstanceState.getLong(ARG_MIN_DATE, minDate);
            year = savedInstanceState.getInt(ARG_YEAR, year);
            month = savedInstanceState.getInt(ARG_MONTH, month);
            day = savedInstanceState.getInt(ARG_DAY, day);
        }

        mBinding = SpinnerDatePickerBinding.inflate(getLayoutInflater());

        final Calendar minCal = Calendar.getInstance();
        minCal.clear();
        minCal.setTimeInMillis(minDate);
        final int minYear = minCal.get(Calendar.YEAR);
        final int minMonth = minCal.get(Calendar.MONTH);
        final int minDay = minCal.get(Calendar.DAY_OF_MONTH);

        mBinding.spinnerDatePicker.setMinDate(minDate);
        mBinding.spinnerDatePicker.init(year, month, day, (view, pickedYear, pickedMonth, pickedDay) -> {
            clampWheelsToMinimum(mBinding.spinnerDatePicker, minYear, minMonth, minDay,
                pickedYear, pickedMonth);
        });
        clampWheelsToMinimum(mBinding.spinnerDatePicker, minYear, minMonth, minDay, year, month);

        return CustomDialog.create(
            requireContext(),
            R.style.SpinnerDialogTheme,
            null,
            getString(R.string.date_picker_dialog_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> setDate(
                mBinding.spinnerDatePicker.getYear(),
                mBinding.spinnerDatePicker.getMonth(),
                mBinding.spinnerDatePicker.getDayOfMonth()
            ),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    @Override
    public void onDestroyView() {
        mBinding = null;

        super.onDestroyView();
    }

    private void setDate(int year, int month, int dayOfMonth) {
        Bundle result = new Bundle();

        result.putInt(BUNDLE_KEY_YEAR, year);
        result.putInt(BUNDLE_KEY_MONTH, month);
        result.putInt(BUNDLE_KEY_DAY, dayOfMonth);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    /**
     * Normalizes the spinner wheels. The day and month wheels are kept linear (no wrap) so a
     * circular adjacency never shows an out-of-range value (e.g. the last day of the month
     * right above day 1). While the selected date lies inside the minimum's month, the wheels
     * are additionally clamped to the minimum so no past value (yesterday / previous month)
     * is ever offered or visible.
     * <p>
     * The month wheel renders localized month names indexed from its minimum value, so the
     * name array has to be re-indexed exactly like the framework does in
     * {@code DatePickerSpinnerDelegate.updateSpinners()} - otherwise the labels shift.
     *
     * @param picker    the active spinner picker
     * @param minYear   the minimum year (inclusive)
     * @param minMonth  the minimum month (inclusive)
     * @param minDay    the minimum day of month (inclusive)
     * @param pickYear  the currently selected year
     * @param pickMonth the currently selected month
     */
    private void clampWheelsToMinimum(DatePicker picker, int minYear, int minMonth, int minDay,
                                      int pickYear, int pickMonth) {
        try {
            Resources res = Resources.getSystem();
            NumberPicker day = picker.findViewById(res.getIdentifier("day", "id", "android"));
            NumberPicker month = picker.findViewById(res.getIdentifier("month", "id", "android"));
            NumberPicker year = picker.findViewById(res.getIdentifier("year", "id", "android"));
            if (day == null || month == null || year == null) {
                return;
            }
            // Keep the wheels linear (no wrap) in every month so a circular adjacency never
            // shows an out-of-range value - e.g. at day 1 the previous value of the generic
            // wrapped wheel is the month's last day ("30" right above "1"), which looks like a
            // ghost. The framework enables wrap in its generic range; we always turn it off.
            day.setWrapSelectorWheel(false);
            month.setWrapSelectorWheel(false);
            if (pickYear != minYear || pickMonth != minMonth) {
                return;
            }
            Calendar firstOfMonth = Calendar.getInstance();
            firstOfMonth.clear();
            firstOfMonth.set(pickYear, pickMonth, 1);
            day.setMinValue(minDay);
            day.setMaxValue(firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
            if (day.getValue() < minDay) {
                day.setValue(minDay);
            }
            String[] monthNames = new DateFormatSymbols().getShortMonths();
            month.setDisplayedValues(null);
            month.setMinValue(minMonth);
            month.setMaxValue(11);
            month.setDisplayedValues(Arrays.copyOfRange(monthNames, minMonth, month.getMaxValue() + 1));
            if (month.getValue() < minMonth) {
                month.setValue(minMonth);
            }
            year.setMinValue(minYear);
        } catch (RuntimeException ignored) {
        }
    }
}
