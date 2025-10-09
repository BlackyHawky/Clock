// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.R;

import com.best.deskclock.data.DataModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

/**
 * Custom component to display a time selection dialog using spinners.
 * This custom implementation allows the user to pick the hour and minute
 * (and AM/PM indicator for 12-hour mode) through spinners.
 * <p>
 * The main differences from the default Android TimePicker are:
 * <ul>
 *  <li>The hours do not automatically change when the minutes change from 59 to 0 or from 0 to 59,
 * allowing for more precise control over the time selection;
 *  <li>Time can be set with the enter key on the keyboard.
 * </ul>
 */
public class SpinnerTimePickerDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of SpinnerTimePickerDialogFragment in the fragment manager.
     */
    private static final String TAG = "alarm_time_picker_dialog";
    private static final String ARG_HOURS = "arg_hours";
    private static final String ARG_MINUTES = "arg_minutes";
    private static final String ARG_AM_PM = "arg_am_pm";
    public static final String REQUEST_KEY = "alarm_result";
    public static final String BUNDLE_KEY_HOURS = "hours";
    public static final String BUNDLE_KEY_MINUTES = "minutes";

    private LinearLayout mLayout;
    private NumberPicker mHourPicker;
    private NumberPicker mMinutePicker;
    private NumberPicker mAmPmPicker;
    private TextView mDivider;

    /**
     * Creates a new instance of {@link SpinnerTimePickerDialogFragment} for use
     * in the alarm view, where the volume value is configured for a specific alarm.
     *
     * @param hours    The alarm hours.
     * @param minutes  The alarm minutes.
     */
    public static SpinnerTimePickerDialogFragment newInstance(int hours, int minutes) {
        final Bundle args = new Bundle();
        args.putInt(ARG_HOURS, hours);
        args.putInt(ARG_MINUTES, minutes);

        final SpinnerTimePickerDialogFragment frag = new SpinnerTimePickerDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Replaces any existing SpinnerTimePickerDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, SpinnerTimePickerDialogFragment fragment) {
        if (manager == null || manager.isDestroyed()) {
            return;
        }

        // Finish any outstanding fragment work.
        manager.executePendingTransactions();

        final FragmentTransaction tx = manager.beginTransaction();

        // Remove existing instance of SpinnerTimePickerDialogFragment if necessary.
        final Fragment existing = manager.findFragmentByTag(TAG);
        if (existing != null) {
            tx.remove(existing);
        }
        tx.addToBackStack(null);

        fragment.show(tx, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as the dialog exists, save its state.
        if (mHourPicker != null) {
            outState.putInt(ARG_HOURS, mHourPicker.getValue());
        }

        if (mMinutePicker != null) {
            outState.putInt(ARG_MINUTES, mMinutePicker.getValue());
        }

        if (!is24HourFormat() && mAmPmPicker != null) {
            outState.putInt(ARG_AM_PM, mAmPmPicker.getValue());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = requireArguments();
        int hourValue = args.getInt(ARG_HOURS, 0);
        int minuteValue = args.getInt(ARG_MINUTES, 0);
        int amPmValue = hourValue >= 12 ? Calendar.PM : Calendar.AM;

        if (savedInstanceState != null) {
            hourValue = savedInstanceState.getInt(ARG_HOURS, hourValue);
            minuteValue = savedInstanceState.getInt(ARG_MINUTES, minuteValue);
            if (!is24HourFormat()) {
                amPmValue = savedInstanceState.getInt(ARG_AM_PM, amPmValue);
            }
        }

        View view = getLayoutInflater().inflate(R.layout.alarm_spinner_time_picker, null);

        mLayout = view.findViewById(R.id.timePickerLayout);
        mHourPicker = view.findViewById(R.id.hour);
        mMinutePicker = view.findViewById(R.id.minute);
        mAmPmPicker = view.findViewById(R.id.amPm);
        mDivider = view.findViewById(R.id.divider);

        setupNumberPickers(hourValue, minuteValue, amPmValue);

        setupEditTextInput(mHourPicker, InputType.TYPE_CLASS_NUMBER, EditorInfo.IME_ACTION_NEXT);
        int imeActionForMinute = is24HourFormat() ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT;
        setupEditTextInput(mMinutePicker, InputType.TYPE_CLASS_NUMBER, imeActionForMinute);
        setupEditTextInput(mAmPmPicker, InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext(), R.style.SpinnerDialogTheme)
                .setTitle(R.string.time_picker_dialog_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    mHourPicker.clearFocus();
                    mMinutePicker.clearFocus();
                    if (!is24HourFormat()) {
                        mAmPmPicker.clearFocus();
                    }

                    int selectedHour = mHourPicker.getValue();
                    int selectedMinute = mMinutePicker.getValue();
                    int amPm = mAmPmPicker.getValue();

                    if (!is24HourFormat()) {
                        if (amPm == Calendar.PM && selectedHour < 12) {
                            selectedHour += 12;
                        }

                        if (amPm == Calendar.AM && selectedHour == 12) {
                            selectedHour = 0;
                        }
                    }

                    setAlarm(selectedHour, selectedMinute);
                })
                .setNegativeButton(android.R.string.cancel, null);

        return dialogBuilder.create();
    }

    private void setAlarm(int hours, int minutes) {
        Bundle result = new Bundle();
        result.putInt(BUNDLE_KEY_HOURS, hours);
        result.putInt(BUNDLE_KEY_MINUTES, minutes);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    /**
     * Configure the NumberPickers for the hour, minutes, and optionally the AM/PM format.
     * <p>
     * This method initializes the value ranges, display formats, default values, and specific behaviors
     * depending on the time format (24h or 12h).
     * <p>It also manages the layout of the AM/PM picker and the navigation behavior between fields,
     * as well as the logic to prevent the hour from changing when the minutes change from 59 to 0 or vice versa.</p>
     * <p>Finally, it sets a listener on the NumberPicker to trigger haptic feedback when the value changes.</p>
     */
    private void setupNumberPickers(int hour, int minute, int amPmValue) {
        // Hours setup
        if (is24HourFormat()) {
            mHourPicker.setMinValue(0);
            mHourPicker.setMaxValue(23);
            mHourPicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));
            mHourPicker.setValue(hour);
        } else {
            mHourPicker.setMinValue(1);
            mHourPicker.setMaxValue(12);
            mHourPicker.setValue(hour % 12 == 0 ? 12 : hour % 12);
        }
        mHourPicker.setOnValueChangedListener((picker, oldVal, newVal) ->
                picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK));

        // Minutes setup
        mMinutePicker.setMinValue(0);
        mMinutePicker.setMaxValue(59);
        mMinutePicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));
        mMinutePicker.setValue(minute);

        // Prevent hours from changing when the minutes change from 59 to 0 or from 0 to 59
        final int[] lastHour = {hour};
        mMinutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);

            if ((oldVal == 59 && newVal == 0) || (oldVal == 0 && newVal == 59)) {
                mHourPicker.setValue(lastHour[0]);
            } else {
                lastHour[0] = mHourPicker.getValue();
            }
        });

        // AM/PM setup
        if (!is24HourFormat()) {
            // Dynamic positioning of AM/PM
            mLayout.removeView(mAmPmPicker);
            if (isAmPmAtStart()) {
                mLayout.addView(mAmPmPicker, 0);
            } else {
                mLayout.addView(mAmPmPicker);
            }

            mAmPmPicker.setMinValue(Calendar.AM);
            mAmPmPicker.setMaxValue(Calendar.PM);
            mAmPmPicker.setDisplayedValues(getAmPmStrings());
            mAmPmPicker.setValue(amPmValue);
            mAmPmPicker.setVisibility(View.VISIBLE);
            mAmPmPicker.setOnValueChangedListener((picker, oldVal, newVal) ->
                    picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK));

            mHourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);

                if ((oldVal == 11 && newVal == 12) || (oldVal == 12 && newVal == 11)) {
                    int currentAmPm = mAmPmPicker.getValue();
                    mAmPmPicker.setValue(currentAmPm == Calendar.AM ? Calendar.PM : Calendar.AM);
                }
            });
        } else {
            mAmPmPicker.setVisibility(View.GONE);
        }

        // Divider setup
        mDivider.setText(getTimeSeparator(is24HourFormat()));

        // Set up the correct focus navigation order
        mHourPicker.setNextFocusForwardId(R.id.minute);
        if (!is24HourFormat()) {
            mMinutePicker.setNextFocusForwardId(R.id.amPm);
            mAmPmPicker.setNextFocusForwardId(View.NO_ID);
        } else {
            mMinutePicker.setNextFocusForwardId(View.NO_ID);
        }
        mDivider.setNextFocusForwardId(View.NO_ID);
    }

    /**
     * This method checks the locale configuration of the device and returns an array containing the
     * AM and PM strings in the appropriate format. The method also ensures that narrow representations
     * (if available) are used when applicable. If no narrow forms are available, the standard AM/PM strings
     * are returned.
     */
    private String[] getAmPmStrings() {
        DateFormatSymbols dfs = DateFormatSymbols.getInstance(Locale.getDefault());
        String[] amPm = dfs.getAmPmStrings();

        String[] result = new String[2];
        result[0] = amPm[0].length() > 4 ? amPm[0].substring(0, 1) : amPm[0];
        result[1] = amPm[1].length() > 4 ? amPm[1].substring(0, 1) : amPm[1];
        return result;
    }

    /**
     * This method checks the best date-time pattern for the current locale using the
     * "hm" skeleton (hour and minute). If the pattern starts with "a" (representing AM/PM),
     * it indicates that the AM/PM marker should be placed at the start (before the time),
     * otherwise, it will be placed after the time.
     *
     * @return {@code true} if the AM/PM indicator should appear before the time.
     * {@code false} if it should appear after.
     */
    private boolean isAmPmAtStart() {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hm");
        return pattern.startsWith("a");
    }

    /**
     * This method checks the appropriate date-time pattern for the current locale and returns
     * the character or string used to separate the hours and minutes. It ensures that the separator
     * matches the format used in the system's locale and time format preferences (24-hour or 12-hour).
     */
    private String getTimeSeparator(boolean is24Hour) {
        String skeleton = is24Hour ? "Hm" : "hm";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);

        int hourIndex = pattern.lastIndexOf('H');
        if (hourIndex == -1) {
            hourIndex = pattern.lastIndexOf('h');
        }

        if (hourIndex == -1) {
            return ":"; // Fallback
        } else {
            int minuteIndex = pattern.indexOf('m', hourIndex + 1);
            if (minuteIndex == -1) {
                return String.valueOf(pattern.charAt(hourIndex + 1));
            } else {
                return pattern.substring(hourIndex + 1, minuteIndex);
            }
        }
    }

    /**
     * This method is used to customize the keyboard input handling in a NumberPicker of type EditText for a time picker.
     * It configures the keyboard and validation behavior when the user changes the hour, minute, or AM/PM.
     */
    private void setupEditTextInput(NumberPicker numberPicker, int inputType, int imeOptions) {
        int count = numberPicker.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = numberPicker.getChildAt(i);
            if (child instanceof EditText editText) {
                editText.setFocusable(true);
                editText.setFocusableInTouchMode(true);
                editText.setClickable(true);
                editText.setLongClickable(true);
                editText.setInputType(inputType);
                editText.setImeOptions(imeOptions);
                editText.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        numberPicker.clearFocus();

                        int selectedHour = mHourPicker.getValue();
                        int selectedMinute = mMinutePicker.getValue();

                        if (!is24HourFormat() && numberPicker == mAmPmPicker) {
                            int amPm = mAmPmPicker.getValue();

                            if (amPm == Calendar.PM && selectedHour < 12) {
                                selectedHour += 12;
                            } else if (amPm == Calendar.AM && selectedHour == 12) {
                                selectedHour = 0;
                            }
                        }

                        setAlarm(selectedHour, selectedMinute);
                        dismiss();

                        return true;
                    }

                    return false;
                });
            }
        }
    }

    private boolean is24HourFormat() {
        return DataModel.getDataModel().is24HourFormat();
    }
}
