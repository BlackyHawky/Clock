// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.ThemeController.Setting.CHANGED;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public static final String BLACK_ACCENT_COLOR = "8";
    public static final String PURPLE_ACCENT_COLOR = "9";
    public static final String YELLOW_ACCENT_COLOR = "10";
    public static final String KEY_AUTO_NIGHT_ACCENT_COLOR = "key_auto_night_accent_color";
    public static final String KEY_NIGHT_ACCENT_COLOR = "key_night_accent_color";
    public static final String DEFAULT_NIGHT_ACCENT_COLOR = "0";
    public static final String BLUE_GRAY_NIGHT_ACCENT_COLOR = "1";
    public static final String BROWN_NIGHT_ACCENT_COLOR = "2";
    public static final String GREEN_NIGHT_ACCENT_COLOR = "3";
    public static final String INDIGO_NIGHT_ACCENT_COLOR = "4";
    public static final String ORANGE_NIGHT_ACCENT_COLOR = "5";
    public static final String PINK_NIGHT_ACCENT_COLOR = "6";
    public static final String RED_NIGHT_ACCENT_COLOR = "7";
    public static final String BLACK_NIGHT_ACCENT_COLOR = "8";
    public static final String PURPLE_NIGHT_ACCENT_COLOR = "9";
    public static final String YELLOW_NIGHT_ACCENT_COLOR = "10";
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
        SwitchPreferenceCompat mAutoNightAccentColorPref;
        ListPreference mNightAccentColorPref;
        SwitchPreferenceCompat mCardBackgroundPref;
        SwitchPreferenceCompat mCardBorderPref;
        SwitchPreferenceCompat mVibrationPref;
        SwitchPreferenceCompat mTabIndicatorPref;
        SwitchPreferenceCompat mFadeTransitionsPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_interface_customization);

            mThemePref = findPreference(KEY_THEME);
            mDarkModePref = findPreference(KEY_DARK_MODE);
            mAccentColorPref = findPreference(KEY_ACCENT_COLOR);
            mAutoNightAccentColorPref = findPreference(KEY_AUTO_NIGHT_ACCENT_COLOR);
            mNightAccentColorPref = findPreference(KEY_NIGHT_ACCENT_COLOR);
            mCardBackgroundPref = findPreference(KEY_CARD_BACKGROUND);
            mCardBorderPref = findPreference(KEY_CARD_BORDER);
            mVibrationPref = findPreference(KEY_VIBRATIONS);
            mTabIndicatorPref = findPreference(KEY_TAB_INDICATOR);
            mFadeTransitionsPref = findPreference(KEY_FADE_TRANSITIONS);

            setupPreferences();

            sortListPreference(mAccentColorPref);
            if (mNightAccentColorPref.isShown()) {
                sortListPreference(mNightAccentColorPref);
            }
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
                        ThemeController.setNewSetting(CHANGED);
                    }
                }

                case KEY_ACCENT_COLOR -> {
                    final ListPreference accentColorPref = (ListPreference) pref;
                    final int index = accentColorPref.findIndexOfValue((String) newValue);
                    accentColorPref.setSummary(accentColorPref.getEntries()[index]);
                    ThemeController.setNewSetting(CHANGED);
                }

                case KEY_AUTO_NIGHT_ACCENT_COLOR -> {
                    ThemeController.setNewSettingWithDelay(CHANGED);
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_NIGHT_ACCENT_COLOR -> {
                    final ListPreference nightAccentColorPref = (ListPreference) pref;
                    final int index = nightAccentColorPref.findIndexOfValue((String) newValue);
                    nightAccentColorPref.setSummary(nightAccentColorPref.getEntries()[index]);
                    if (Utils.isNight(requireContext().getResources())) {
                        ThemeController.setNewSetting(CHANGED);
                    }
                }

                case KEY_CARD_BACKGROUND -> {
                    final TwoStatePreference cardBackgroundPref = (TwoStatePreference) pref;
                    cardBackgroundPref.setChecked(DataModel.getDataModel().isCardBackgroundDisplayed());
                    ThemeController.setNewSettingWithDelay(CHANGED);
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_CARD_BORDER -> {
                    final TwoStatePreference cardBorderPref = (TwoStatePreference) pref;
                    cardBorderPref.setChecked(DataModel.getDataModel().isCardBorderDisplayed());
                    ThemeController.setNewSettingWithDelay(CHANGED);
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
                    ThemeController.setNewSettingWithDelay(CHANGED);
                    Utils.setVibrationTime(requireContext(), 50);
                    requireActivity().setResult(RESULT_OK);
                }

                case KEY_FADE_TRANSITIONS -> {
                    final TwoStatePreference fadeTransitionsPref = (TwoStatePreference) pref;
                    fadeTransitionsPref.setChecked(DataModel.getDataModel().isFadeTransitionsEnabled());
                    ThemeController.setNewSettingWithDelay(CHANGED);
                    Utils.setVibrationTime(requireContext(), 50);
                }
            }

            return true;
        }

        private void setupPreferences() {
            final Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            mVibrationPref.setVisible(vibrator.hasVibrator());

            mAutoNightAccentColorPref.setChecked(DataModel.getDataModel().isAutoNightAccentColorEnabled());
            mNightAccentColorPref.setVisible(!mAutoNightAccentColorPref.isChecked());
            if (mAutoNightAccentColorPref.isChecked()) {
                mAccentColorPref.setTitle(requireContext().getString(R.string.title_accent_color));
                mAccentColorPref.setDialogTitle(requireContext().getString(R.string.title_accent_color));
            } else {
                mAccentColorPref.setTitle(requireContext().getString(R.string.day_accent_color_title));
                mAccentColorPref.setDialogTitle(requireContext().getString(R.string.day_accent_color_title));
            }
        }

        private void refresh() {
            mThemePref.setSummary(mThemePref.getEntry());
            mThemePref.setOnPreferenceChangeListener(this);

            mDarkModePref.setSummary(mDarkModePref.getEntry());
            mDarkModePref.setOnPreferenceChangeListener(this);

            mAccentColorPref.setSummary(mAccentColorPref.getEntry());
            mAccentColorPref.setOnPreferenceChangeListener(this);

            mAutoNightAccentColorPref.setChecked(DataModel.getDataModel().isAutoNightAccentColorEnabled());
            mAutoNightAccentColorPref.setOnPreferenceChangeListener(this);

            mNightAccentColorPref.setSummary(mNightAccentColorPref.getEntry());
            mNightAccentColorPref.setOnPreferenceChangeListener(this);

            mCardBackgroundPref.setOnPreferenceChangeListener(this);

            mCardBorderPref.setOnPreferenceChangeListener(this);

            mVibrationPref.setOnPreferenceChangeListener(this);

            mTabIndicatorPref.setOnPreferenceChangeListener(this);

            mFadeTransitionsPref.setOnPreferenceChangeListener(this);
        }

        private void sortListPreference(ListPreference listPreference) {
            if (listPreference != null) {

                CharSequence[] entries = listPreference.getEntries();
                CharSequence[] values = listPreference.getEntryValues();

                if (entries != null && values != null && entries.length > 1) {
                    // Create a list of (entry, value) pairs to sort
                    List<Pair> entryValuePairs = new ArrayList<>();

                    // Add the first entry and value that should not be sorted
                    entryValuePairs.add(new Pair(entries[0], values[0]));

                    // Add the rest of the entries and values to sort (starting from the second element)
                    for (int i = 1; i < entries.length; i++) {
                        entryValuePairs.add(new Pair(entries[i], values[i]));
                    }

                    // Sort elements starting from second (index 1)
                    List<Pair> remainingPairs = entryValuePairs.subList(1, entryValuePairs.size());
                    Collections.sort(remainingPairs, (pair1, pair2) ->
                        CharSequence.compare(pair1.entry.toString(), pair2.entry.toString()));

                    CharSequence[] sortedEntries = new CharSequence[entries.length];
                    CharSequence[] sortedValues = new CharSequence[values.length];

                    // Place first entry and value (unsorted)
                    sortedEntries[0] = entryValuePairs.get(0).entry;
                    sortedValues[0] = entryValuePairs.get(0).value;

                    // Copy sorted items
                    for (int i = 1; i < entryValuePairs.size(); i++) {
                        sortedEntries[i] = entryValuePairs.get(i).entry;
                        sortedValues[i] = entryValuePairs.get(i).value;
                    }

                    // Update entries and sorted values in the ListPreference
                    listPreference.setEntries(sortedEntries);
                    listPreference.setEntryValues(sortedValues);
                }
            }
        }

        /**
         * Internal class to store entry/value pairs
         */
        private record Pair(CharSequence entry, CharSequence value) {
        }
    }

}
