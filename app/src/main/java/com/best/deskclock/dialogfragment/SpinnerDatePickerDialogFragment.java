// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.databinding.SpinnerDatePickerBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.Utils;

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
    public static void show(FragmentManager manager, SpinnerDatePickerDialogFragment fragment) {
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

        mBinding.spinnerDatePicker.setMinDate(minDate);
        mBinding.spinnerDatePicker.init(year, month, day, null);

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
}
