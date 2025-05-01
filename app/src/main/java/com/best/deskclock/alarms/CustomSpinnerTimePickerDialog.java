// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.app.Dialog;
import android.content.Context;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.best.deskclock.R;

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
public class CustomSpinnerTimePickerDialog {

    /**
     * Displays the time selection dialog.
     */
    public static void show(Context context, Fragment fragment, int hour, int minute, OnTimeSetListener listener) {
        boolean is24Hour = DateFormat.is24HourFormat(context);

        showCustomSpinnerTimePicker(context, fragment, hour, minute, is24Hour, listener);
    }

    /**
     * Displays a dialog to select the hour and minutes (and AM/PM indicator for 12-hour mode).
     */
    private static void showCustomSpinnerTimePicker(Context context, Fragment fragment, int hour, int minute,
                                                    boolean is24Hour, OnTimeSetListener listener) {

        LayoutInflater inflater = fragment.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_spinner_time_picker, null);

        NumberPicker hourPicker = dialogView.findViewById(R.id.hour);
        NumberPicker minutePicker = dialogView.findViewById(R.id.minute);
        NumberPicker amPmPicker = dialogView.findViewById(R.id.amPm);

        setupNumberPickers(context, dialogView, hour, minute, is24Hour, hourPicker, minutePicker, amPmPicker);

        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.time_picker_dialog_title)
                .setIcon(R.drawable.ic_calendar_clock)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        alertDialog.setOnShowListener(dialog -> {
            Button okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            okButton.setOnClickListener(v -> {
                int selectedHour = hourPicker.getValue();
                int selectedMinute = minutePicker.getValue();
                int amPm = amPmPicker.getValue();

                if (!is24Hour) {
                    if (amPm == Calendar.PM && selectedHour < 12) selectedHour += 12;
                    if (amPm == Calendar.AM && selectedHour == 12) selectedHour = 0;
                }

                listener.onTimeSet(selectedHour, selectedMinute);
                alertDialog.dismiss();
            });

            enableEditTextInput(hourPicker, InputType.TYPE_CLASS_NUMBER, EditorInfo.IME_ACTION_NEXT,
                    alertDialog, hourPicker, minutePicker, amPmPicker, is24Hour, listener);
            int imeActionForMinute = is24Hour ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT;
            enableEditTextInput(minutePicker, InputType.TYPE_CLASS_NUMBER, imeActionForMinute,
                    alertDialog, hourPicker, minutePicker, amPmPicker, is24Hour, listener);
            enableEditTextInput(amPmPicker, InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE,
                    alertDialog, hourPicker, minutePicker, amPmPicker, is24Hour, listener);
        });

        alertDialog.show();
    }

    /**
     * Configure the NumberPickers for the hour, minutes, and optionally the AM/PM format.
     * <p>
     * This method initializes the value ranges, display formats, default values, and specific behaviors
     * depending on the time format (24h or 12h).
     * It also manages the layout of the AM/PM picker and the navigation behavior between fields,
     * as well as the logic to prevent the hour from changing when the minutes change from 59 to 0 or vice versa.
     */
    private static void setupNumberPickers(Context context, View dialogView, int hour, int minute, boolean is24Hour,
                                           NumberPicker hourPicker, NumberPicker minutePicker, NumberPicker amPmPicker) {

        // Hours setup
        if (is24Hour) {
            hourPicker.setMinValue(0);
            hourPicker.setMaxValue(23);
            hourPicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));
            hourPicker.setValue(hour);
        } else {
            hourPicker.setMinValue(1);
            hourPicker.setMaxValue(12);
            hourPicker.setValue(hour % 12 == 0 ? 12 : hour % 12);
        }

        // Minutes setup
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));
        minutePicker.setValue(minute);

        // Prevent hours from changing when the minutes change from 59 to 0 or from 0 to 59
        final int[] lastHour = {hour};
        minutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if ((oldVal == 59 && newVal == 0) || (oldVal == 0 && newVal == 59)) {
                hourPicker.setValue(lastHour[0]);
            } else {
                lastHour[0] = hourPicker.getValue();
            }
        });

        // AM/PM setup
        if (!is24Hour) {
            // Dynamic positioning of AM/PM
            LinearLayout layout = dialogView.findViewById(R.id.timePickerLayout);
            layout.removeView(amPmPicker);
            if (isAmPmAtStart()) {
                layout.addView(amPmPicker, 0);
            } else {
                layout.addView(amPmPicker);
            }

            amPmPicker.setMinValue(Calendar.AM);
            amPmPicker.setMaxValue(Calendar.PM);
            amPmPicker.setDisplayedValues(getAmPmStrings(context));
            amPmPicker.setValue(hour >= 12 ? Calendar.PM : Calendar.AM);
            amPmPicker.setVisibility(View.VISIBLE);

            hourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                if ((oldVal == 11 && newVal == 12) || (oldVal == 12 && newVal == 11)) {
                    int currentAmPm = amPmPicker.getValue();
                    amPmPicker.setValue(currentAmPm == Calendar.AM ? Calendar.PM : Calendar.AM);
                }
            });
        } else {
            amPmPicker.setVisibility(View.GONE);
        }

        // Divider setup
        TextView divider = dialogView.findViewById(R.id.divider);
        divider.setText(getTimeSeparator(is24Hour));

        // Set up the correct focus navigation order
        hourPicker.setNextFocusForwardId(R.id.minute);
        if (!is24Hour) {
            minutePicker.setNextFocusForwardId(R.id.amPm);
            amPmPicker.setNextFocusForwardId(View.NO_ID);
        } else {
            minutePicker.setNextFocusForwardId(View.NO_ID);
        }
        divider.setNextFocusForwardId(View.NO_ID);
    }

    /**
     * This method checks the locale configuration of the device and returns an array containing the
     * AM and PM strings in the appropriate format. The method also ensures that narrow representations
     * (if available) are used when applicable. If no narrow forms are available, the standard AM/PM strings
     * are returned.
     */
    static String[] getAmPmStrings(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        DateFormatSymbols dfs = DateFormatSymbols.getInstance(locale);
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
    private static boolean isAmPmAtStart() {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hm");
        return pattern.startsWith("a");
    }

    /**
     * This method checks the appropriate date-time pattern for the current locale and returns
     * the character or string used to separate the hours and minutes. It ensures that the separator
     * matches the format used in the system's locale and time format preferences (24-hour or 12-hour).
     */
    private static String getTimeSeparator(boolean is24Hour) {
        String skeleton = is24Hour ? "Hm" : "hm";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);

        int hourIndex = pattern.lastIndexOf('H');
        if (hourIndex == -1) {
            hourIndex = pattern.lastIndexOf('h');
        }

        if (hourIndex == -1) {
            return ":"; // fallback
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
    private static void enableEditTextInput(NumberPicker numberPicker, int inputType, int imeOptions,
                                            Dialog dialog, NumberPicker hourPicker,
                                            NumberPicker minutePicker, NumberPicker amPmPicker,
                                            boolean is24Hour, OnTimeSetListener listener) {

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

                        int selectedHour = hourPicker.getValue();
                        int selectedMinute = minutePicker.getValue();

                        if (!is24Hour && numberPicker == amPmPicker) {
                            int amPm = amPmPicker.getValue();

                            if (amPm == Calendar.PM && selectedHour < 12) {
                                selectedHour += 12;
                            } else if (amPm == Calendar.AM && selectedHour == 12) {
                                selectedHour = 0;
                            }

                            listener.onTimeSet(selectedHour, selectedMinute);
                            dialog.dismiss();
                        } else if (numberPicker == minutePicker) {
                            listener.onTimeSet(selectedHour, selectedMinute);
                            dialog.dismiss();
                        }

                        return true;
                    }

                    return false;
                });
            }
        }
    }
}
