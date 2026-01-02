// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_TIME_PICKER_STYLE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;
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

        picker.addOnPositiveButtonClickListener(dialog ->
                listener.onTimeSet(picker.getHour(), picker.getMinute()));

        picker.getViewLifecycleOwnerLiveData().observeForever(new Observer<>() {

            @Override
            public void onChanged(LifecycleOwner owner) {
                if (owner != null) {
                    PickerFonts fonts = loadFonts(prefs);
                    setupPicker(picker, fonts);
                    picker.getViewLifecycleOwnerLiveData().removeObserver(this);
                }
            }
        });

        picker.show(fragmentManager, tag);
    }

    /**
     * Holds both alarm and general fonts for convenience.
     */
    private record PickerFonts(Typeface alarm, Typeface general) {}

    /**
     * Loads the custom fonts used by the picker.
     *
     * @param prefs shared preferences containing font settings
     * @return a PickerFonts record containing alarm and general fonts
     */
    private static PickerFonts loadFonts(SharedPreferences prefs) {
        return new PickerFonts(
                ThemeUtils.loadFont(SettingsDAO.getAlarmFont(prefs)),
                ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs))
        );
    }

    /**
     * Applies fonts and installs listeners on the picker once its view is ready.
     *
     * @param picker the MaterialTimePicker instance
     * @param fonts the loaded custom fonts
     */
    private static void setupPicker(MaterialTimePicker picker, PickerFonts fonts) {
        View root = picker.getView();
        if (root == null) {
            return;
        }

        applyFont(root, fonts.alarm(),
                com.google.android.material.R.id.header_title,
                com.google.android.material.R.id.material_timepicker_ok_button,
                com.google.android.material.R.id.material_timepicker_cancel_button
        );

        installClockFaceListener(root, fonts.alarm());
        installModeSwitchListener(picker, fonts);
        installDialogFontListener(picker, fonts.general());
    }

    /**
     * Recursively applies a font to all TextViews inside a view hierarchy,
     * excluding specific view IDs.
     *
     * @param root the root view to scan
     * @param font the typeface to apply
     * @param excludedIds view IDs that must not receive the font
     */
    private static void applyFont(View root, Typeface font, int... excludedIds) {
        if (font == null || root == null) {
            return;
        }

        for (int id : excludedIds) {
            if (root.getId() == id)
                return;
        }

        if (root instanceof TextView textView) {
            textView.setTypeface(font);
        } else if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                applyFont(group.getChildAt(i), font, excludedIds);
            }
        }
    }

    /**
     * Reapplies the alarm font whenever the analog clock face is redrawn.
     *
     * @param root the picker root view
     * @param alarmFont the font used for clock numbers
     */
    private static void installClockFaceListener(View root, Typeface alarmFont) {
        View clockFace = root.findViewById(com.google.android.material.R.id.material_clock_face);
        if (clockFace == null) {
            return;
        }

        clockFace.addOnLayoutChangeListener((view, left, top, right, bottom,
                                             oldLeft, oldTop, oldRight, oldBottom) ->

                applyFont(view, alarmFont)
        );

        if (clockFace instanceof ViewGroup group) {
            applyFont(group, alarmFont);
        }
    }

    /**
     * Reapplies fonts when switching between clock mode and keyboard mode.
     *
     * @param picker the MaterialTimePicker instance
     * @param fonts the loaded custom fonts
     */
    @SuppressLint("ClickableViewAccessibility")
    private static void installModeSwitchListener(MaterialTimePicker picker, PickerFonts fonts) {
        View root = picker.getView();
        if (root == null) {
            return;
        }

        View modeButton = root.findViewById(com.google.android.material.R.id.material_timepicker_mode_button);
        if (modeButton == null) {
            return;
        }

        modeButton.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                view.postDelayed(() -> {
                    View newRoot = picker.getView();
                    if (newRoot != null) {
                        applyFont(newRoot, fonts.alarm(),
                                com.google.android.material.R.id.header_title,
                                com.google.android.material.R.id.material_timepicker_ok_button,
                                com.google.android.material.R.id.material_timepicker_cancel_button
                        );

                        installDialogFontListener(picker, fonts.general());
                    }
                }, 20);
            }

            return false;
        });
    }

    /**
     * Applies the general font to dialog buttons and title when the dialog is shown.
     *
     * @param picker the MaterialTimePicker instance
     * @param generalFont the font used for dialog UI elements
     */
    private static void installDialogFontListener(MaterialTimePicker picker, Typeface generalFont) {
        Dialog dialog = picker.getDialog();
        if (dialog == null) {
            return;
        }

        dialog.setOnShowListener(d -> {
            int[] ids = { com.google.android.material.R.id.material_timepicker_ok_button,
                    com.google.android.material.R.id.material_timepicker_cancel_button,
                    com.google.android.material.R.id.header_title
            };

            for (int id : ids) {
                View view = dialog.findViewById(id);

                if (view instanceof TextView textView) {
                    textView.setTypeface(generalFont);
                }
            }
        });
    }

}
