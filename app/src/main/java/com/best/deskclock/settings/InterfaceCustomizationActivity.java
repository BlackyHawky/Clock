// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

import java.util.Objects;

public class InterfaceCustomizationActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "customization_interface_fragment";

    public static final String KEY_THEME = "key_theme";
    public static final String SYSTEM_THEME = "0";
    public static final String LIGHT_THEME = "1";
    public static final String DARK_THEME = "2";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_DEFAULT_DARK_MODE = "0";
    public static final String KEY_AMOLED_DARK_MODE = "1";
    public static final String KEY_ACCENT_COLOR = "key_accent_color";
    public static final String DEFAULT_ACCENT_COLOR = "0";
    public static final String BLUE_GRAY_ACCENT_COLOR = "1";
    public static final String BROWN_ACCENT_COLOR = "2";
    public static final String GREEN_ACCENT_COLOR = "3";
    public static final String INDIGO_ACCENT_COLOR = "4";
    public static final String ORANGE_ACCENT_COLOR = "5";
    public static final String PINK_ACCENT_COLOR = "6";
    public static final String RED_ACCENT_COLOR = "7";
    public static final String KEY_CARD_BACKGROUND = "key_card_background";
    public static final String KEY_CARD_BACKGROUND_BORDER = "key_card_background_border";
    public static final String KEY_MISCELLANEOUS_CATEGORY = "key_miscellaneous_category";
    public static final String KEY_VIBRATIONS = "key_vibrations";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PrefsFragment(), PREFS_FRAGMENT_TAG)
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static class PrefsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getPreferenceManager().setStorageDeviceProtected();
            }
            addPreferencesFromResource(R.xml.settings_interface_customization);
            hidePreferences();
        }

        @Override
        public void onResume() {
            super.onResume();
            int bottomPadding = Utils.toPixel(20, requireContext());
            getListView().setPadding(0, 0, 0, bottomPadding);

            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_THEME -> {
                    final ListPreference themePref = (ListPreference) pref;
                    final int index = themePref.findIndexOfValue((String) newValue);
                    themePref.setSummary(themePref.getEntries()[index]);
                    switch (index) {
                        case 0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        case 1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        case 2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    }
                }

                case KEY_DARK_MODE -> {
                    final ListPreference amoledPref = (ListPreference) pref;
                    final int darkModeIndex = amoledPref.findIndexOfValue((String) newValue);
                    amoledPref.setSummary(amoledPref.getEntries()[darkModeIndex]);
                    if (Utils.isNight(requireActivity().getResources())) {
                        switch (darkModeIndex) {
                            case 0 -> ThemeController.applyDarkMode(ThemeController.DarkMode.DEFAULT_DARK_MODE);
                            case 1 -> ThemeController.applyDarkMode(ThemeController.DarkMode.AMOLED);
                        }
                    }
                }

                case KEY_ACCENT_COLOR -> {
                    final ListPreference themePref = (ListPreference) pref;
                    final int index = themePref.findIndexOfValue((String) newValue);
                    themePref.setSummary(themePref.getEntries()[index]);
                    switch (index) {
                        case 0 -> ThemeController.applyAccentColor(ThemeController.AccentColor.DEFAULT);
                        case 1 -> ThemeController.applyAccentColor(ThemeController.AccentColor.BLUE_GRAY);
                        case 2 -> ThemeController.applyAccentColor(ThemeController.AccentColor.BROWN);
                        case 3 -> ThemeController.applyAccentColor(ThemeController.AccentColor.GREEN);
                        case 4 -> ThemeController.applyAccentColor(ThemeController.AccentColor.INDIGO);
                        case 5 -> ThemeController.applyAccentColor(ThemeController.AccentColor.ORANGE);
                        case 6 -> ThemeController.applyAccentColor(ThemeController.AccentColor.PINK);
                        case 7 -> ThemeController.applyAccentColor(ThemeController.AccentColor.RED);
                    }
                }

                case KEY_CARD_BACKGROUND -> {
                    final TwoStatePreference cardBackgroundPref = (TwoStatePreference) pref;
                    cardBackgroundPref.setChecked(DataModel.getDataModel().isCardBackgroundDisplayed());
                    // Set result so DeskClock knows to refresh itself
                    requireActivity().setResult(RESULT_OK);
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_CARD_BACKGROUND_BORDER -> {
                    final TwoStatePreference cardBackgroundBorderPref = (TwoStatePreference) pref;
                    cardBackgroundBorderPref.setChecked(
                            DataModel.getDataModel().isCardBackgroundBorderDisplayed()
                    );
                    // Set result so DeskClock knows to refresh itself
                    requireActivity().setResult(RESULT_OK);
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_VIBRATIONS -> {
                    final TwoStatePreference vibrationsPref = (TwoStatePreference) pref;
                    vibrationsPref.setChecked(DataModel.getDataModel().isVibrationsEnabled());
                    Utils.setVibrationTime(requireContext(), 50);
                }
            }

            return true;
        }

        private void hidePreferences() {
            // Don't hide this category if we add others settings in the future;
            // only the “Enable vibrations” setting will have to be hidden if the device doesn't have a vibrator.
            PreferenceCategory miscellaneousCategory = findPreference(KEY_MISCELLANEOUS_CATEGORY);
            final Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);

            Objects.requireNonNull(miscellaneousCategory).setVisible(vibrator.hasVibrator());
        }

        private void refresh() {
            final ListPreference themePref = findPreference(KEY_THEME);
            Objects.requireNonNull(themePref).setSummary(themePref.getEntry());
            themePref.setOnPreferenceChangeListener(this);

            final ListPreference amoledModePref = findPreference(KEY_DARK_MODE);
            Objects.requireNonNull(amoledModePref).setSummary(amoledModePref.getEntry());
            amoledModePref.setOnPreferenceChangeListener(this);

            final ListPreference colorPref = findPreference(KEY_ACCENT_COLOR);
            Objects.requireNonNull(colorPref).setSummary(colorPref.getEntry());
            colorPref.setOnPreferenceChangeListener(this);

            final SwitchPreferenceCompat cardBackgroundPref = findPreference(KEY_CARD_BACKGROUND);
            Objects.requireNonNull(cardBackgroundPref).setOnPreferenceChangeListener(this);

            final SwitchPreferenceCompat cardBackgroundBorderPref = findPreference(KEY_CARD_BACKGROUND_BORDER);
            Objects.requireNonNull(cardBackgroundBorderPref).setOnPreferenceChangeListener(this);

            final SwitchPreferenceCompat vibrationPref = findPreference(KEY_VIBRATIONS);
            Objects.requireNonNull(vibrationPref).setOnPreferenceChangeListener(this);
        }
    }

}
