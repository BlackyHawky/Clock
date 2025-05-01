// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_TIME_PICKER_STYLE;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.data.SettingsDAO;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

/**
 * Utility class to show a Material Design time picker dialog.
 */
public class MaterialTimePickerDialog {

    /**
     * Displays a dialog to select the hour and minutes and AM/PM for 12-hour mode.
     */
    public static void show(Context context, FragmentManager fragmentManager, String tag, int initialHour,
                            int initialMinute, SharedPreferences prefs, @NonNull OnTimeSetListener listener) {

        @TimeFormat int clockFormat;
        boolean isSystem24Hour = DateFormat.is24HourFormat(context);
        clockFormat = isSystem24Hour ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;

        String style = SettingsDAO.getMaterialTimePickerStyle(prefs);
        int inputMode = style.equals(DEFAULT_TIME_PICKER_STYLE)
                ? MaterialTimePicker.INPUT_MODE_CLOCK
                : MaterialTimePicker.INPUT_MODE_KEYBOARD;

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setInputMode(inputMode)
                .setHour(initialHour)
                .setMinute(initialMinute)
                .build();

        picker.show(fragmentManager, tag);

        picker.addOnPositiveButtonClickListener(dialog ->
                listener.onTimeSet(picker.getHour(), picker.getMinute()));
    }
}
