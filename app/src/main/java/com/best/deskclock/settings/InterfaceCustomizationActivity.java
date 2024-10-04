// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
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
    public static final String KEY_DARK_MODE = "key_dark_mode";
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
    public static final String KEY_CARD_BORDER = "key_card_border";
    public static final String KEY_VIBRATIONS = "key_vibrations";
    public static final String KEY_TAB_INDICATOR = "key_tab_indicator";
    public static final String KEY_FADE_TRANSITIONS = "key_fade_transitions";

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

    public static class PrefsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

        ListPreference mThemePref;
        ListPreference mDarkModePref;
        ListPreference mAccentColorPref;
        SwitchPreferenceCompat mCardBackgroundPref;
        SwitchPreferenceCompat mCardBorderPref;
        SwitchPreferenceCompat mVibrationPref;
        SwitchPreferenceCompat mTabIndicatorPref;
        SwitchPreferenceCompat mFadeTransitions;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_interface_customization);

            mThemePref = findPreference(KEY_THEME);
            mDarkModePref = findPreference(KEY_DARK_MODE);
            mAccentColorPref = findPreference(KEY_ACCENT_COLOR);
            mCardBackgroundPref = findPreference(KEY_CARD_BACKGROUND);
            mCardBorderPref = findPreference(KEY_CARD_BORDER);
            mVibrationPref = findPreference(KEY_VIBRATIONS);
            mTabIndicatorPref = findPreference(KEY_TAB_INDICATOR);
            mFadeTransitions = findPreference(KEY_FADE_TRANSITIONS);

            hidePreferences();
        }

        @Override
        public void onResume() {
            super.onResume();

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
                    if (cardBackgroundPref.isChecked()) {
                        ThemeController.applyLayoutBackground(ThemeController.LayoutBackground.TRANSPARENT);
                    } else {
                        ThemeController.applyLayoutBackground(ThemeController.LayoutBackground.DEFAULT);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_CARD_BORDER -> {
                    final TwoStatePreference cardBorderPref = (TwoStatePreference) pref;
                    cardBorderPref.setChecked(DataModel.getDataModel().isCardBorderDisplayed());
                    if (cardBorderPref.isChecked()) {
                        ThemeController.applyLayoutBorderedSettings(ThemeController.LayoutBorder.BORDERED);
                    } else {
                        ThemeController.applyLayoutBorderedSettings(ThemeController.LayoutBorder.DEFAULT);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_VIBRATIONS -> {
                    final TwoStatePreference vibrationsPref = (TwoStatePreference) pref;
                    vibrationsPref.setChecked(DataModel.getDataModel().isVibrationsEnabled());
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_TAB_INDICATOR -> {
                    final TwoStatePreference tabIndicatorPref = (TwoStatePreference) pref;
                    tabIndicatorPref.setChecked(DataModel.getDataModel().isTabIndicatorDisplayed());
                    Utils.setVibrationTime(requireContext(), 50);
                    requireActivity().setResult(RESULT_OK);
                }

                case KEY_FADE_TRANSITIONS -> {
                    final TwoStatePreference fadeTransitionsPref = (TwoStatePreference) pref;
                    fadeTransitionsPref.setChecked(DataModel.getDataModel().isFadeTransitionsEnabled());
                    if (fadeTransitionsPref.isChecked()) {
                        ThemeController.enableFadeTransitions(ThemeController.FadeTransitions.ENABLED);
                    } else {
                        ThemeController.enableFadeTransitions(ThemeController.FadeTransitions.DISABLED);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                }
            }

            return true;
        }

        private void hidePreferences() {
            final Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            mVibrationPref.setVisible(vibrator.hasVibrator());
        }

        private void refresh() {
            mThemePref.setSummary(mThemePref.getEntry());
            mThemePref.setOnPreferenceChangeListener(this);

            mDarkModePref.setSummary(mDarkModePref.getEntry());
            mDarkModePref.setOnPreferenceChangeListener(this);

            mAccentColorPref.setSummary(mAccentColorPref.getEntry());
            mAccentColorPref.setOnPreferenceChangeListener(this);

            mCardBackgroundPref.setOnPreferenceChangeListener(this);

            mCardBorderPref.setOnPreferenceChangeListener(this);

            mVibrationPref.setOnPreferenceChangeListener(this);

            mTabIndicatorPref.setOnPreferenceChangeListener(this);

            mFadeTransitions.setOnPreferenceChangeListener(this);
        }
    }

}
