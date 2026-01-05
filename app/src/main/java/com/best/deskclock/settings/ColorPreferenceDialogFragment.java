// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BATTERY_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DATE_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;

import com.best.deskclock.utils.Utils;
import com.rarepebble.colorpicker.ColorPickerView;

/**
 * DialogFragment related to the {@link ColorPickerPreference} that allows a custom font
 * to be displayed in the dialog box.
 */
public class ColorPreferenceDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of ColorPreferenceDialogFragment in the fragment manager.
     */
    private static final String TAG = "color_picker_dialog";

    private static final String ARG_PREF_KEY = "arg_pref_key";

    private ColorPickerPreference preference;

    public static ColorPreferenceDialogFragment newInstance(ColorPickerPreference pref) {
        Bundle args = new Bundle();
        args.putString(ARG_PREF_KEY, pref.getKey());

        ColorPreferenceDialogFragment frag = new ColorPreferenceDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    /**
     * Displays {@link ColorPreferenceDialogFragment}.
     */
    public static void show(FragmentManager manager, ColorPreferenceDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();

        resolvePreferenceIfNeeded();

        ColorPickerView colorPickerView = getColorPickerView(context);

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));
        EditText hexEdit = colorPickerView.findViewById(com.rarepebble.colorpicker.R.id.hexEdit);

        if (hexEdit != null && typeface != null) {
            hexEdit.setTypeface(typeface);
        }

        return CustomDialog.create(
                context,
                null,
                null,
                preference.getTitle(),
                null,
                colorPickerView,
                getString(android.R.string.ok),
                (d, w) -> {
                    int color = colorPickerView.getColor();
                    if (preference.callChangeListener(color)) {
                        preference.setColor(color);
                    }
                },
                getString(android.R.string.cancel),
                null,
                getString(R.string.label_default),
                (d, w) -> {
                    if (preference.callChangeListener(null)) {
                        preference.setColor(null);
                    }
                },
                null,
                CustomDialog.SoftInputMode.NONE
        );
    }

    @NonNull
    private ColorPickerView getColorPickerView(Context context) {
        ColorPickerView colorPickerView = new ColorPickerView(context);

        colorPickerView.setColor(preference.getColor());

        // Don't display transparency for the screensaver color settings, as this has no effect.
        colorPickerView.showAlpha(!preference.getKey().equals(KEY_SCREENSAVER_CLOCK_COLOR_PICKER)
                && !preference.getKey().equals(KEY_SCREENSAVER_BATTERY_COLOR_PICKER)
                && !preference.getKey().equals(KEY_SCREENSAVER_DATE_COLOR_PICKER)
                && !preference.getKey().equals(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER));

        colorPickerView.showHex(true);

        colorPickerView.showPreview(true);

        return colorPickerView;
    }

    private void resolvePreferenceIfNeeded() {
        if (preference != null) {
            return;
        }

        String key = requireArguments().getString(ARG_PREF_KEY);
        Fragment parent = getParentFragment();
        if (!(parent instanceof PreferenceFragmentCompat preferenceFragmentCompat) || key == null) {
            return;
        }

        Preference pref = preferenceFragmentCompat.findPreference(key);
        if (pref instanceof ColorPickerPreference colorPickerPreference) {
            preference = colorPickerPreference;
        }
    }

}
