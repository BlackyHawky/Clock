// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.DeskClock.REQUEST_CHANGE_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CARD_BACKGROUND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CARD_BORDER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CUSTOM_LANGUAGE_CODE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DARK_MODE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TAB_TITLE_VISIBILITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TAB_TO_DISPLAY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_FADE_TRANSITIONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TAB_INDICATOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_THEME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TOOLBAR_TITLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VIBRATIONS;
import static com.best.deskclock.utils.Utils.ACTION_LANGUAGE_CODE_CHANGED;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.alarmclock.WidgetUtils;
import com.best.alarmclock.materialyouwidgets.MaterialYouDigitalAppWidgetProvider;
import com.best.alarmclock.materialyouwidgets.MaterialYouNextAlarmAppWidgetProvider;
import com.best.alarmclock.materialyouwidgets.MaterialYouVerticalDigitalAppWidgetProvider;
import com.best.alarmclock.standardwidgets.DigitalAppWidgetProvider;
import com.best.alarmclock.standardwidgets.NextAlarmAppWidgetProvider;
import com.best.alarmclock.standardwidgets.VerticalDigitalAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InterfaceCustomizationFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener {

    private static boolean isLanguageChanged = false;

    ListPreference mThemePref;
    ListPreference mDarkModePref;
    ListPreference mAccentColorPref;
    SwitchPreferenceCompat mAutoNightAccentColorPref;
    ListPreference mNightAccentColorPref;
    SwitchPreferenceCompat mCardBackgroundPref;
    SwitchPreferenceCompat mCardBorderPref;
    ListPreference mCustomLanguageCodePref;
    ListPreference mTabToDisplayPref;
    SwitchPreferenceCompat mVibrationPref;
    SwitchPreferenceCompat mToolbarTitlePref;
    ListPreference mTabTitleVisibilityPref;
    SwitchPreferenceCompat mTabIndicatorPref;
    SwitchPreferenceCompat mFadeTransitionsPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.interface_customization_settings);
    }

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
        mCustomLanguageCodePref = findPreference(KEY_CUSTOM_LANGUAGE_CODE);
        mTabToDisplayPref = findPreference(KEY_TAB_TO_DISPLAY);
        mVibrationPref = findPreference(KEY_VIBRATIONS);
        mToolbarTitlePref = findPreference(KEY_TOOLBAR_TITLE);
        mTabTitleVisibilityPref = findPreference(KEY_TAB_TITLE_VISIBILITY);
        mTabIndicatorPref = findPreference(KEY_TAB_INDICATOR);
        mFadeTransitionsPref = findPreference(KEY_FADE_TRANSITIONS);

        setupPreferences();

        sortListPreference(mAccentColorPref);
        if (mNightAccentColorPref.isShown()) {
            sortListPreference(mNightAccentColorPref);
        }
        sortListPreference(mCustomLanguageCodePref);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isLanguageChanged) {
            updateAllDigitalWidgets(requireContext());
            isLanguageChanged = false;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        final boolean isNight = ThemeUtils.isNight(requireActivity().getResources());
        switch (pref.getKey()) {
            case KEY_THEME -> {
                final int index = mThemePref.findIndexOfValue((String) newValue);
                mThemePref.setSummary(mThemePref.getEntries()[index]);
                switch (index) {
                    case 0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    case 1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    case 2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
            }

            case KEY_DARK_MODE, KEY_NIGHT_ACCENT_COLOR -> {
                final ListPreference listPreference = (ListPreference) pref;
                final int darkModeIndex = listPreference.findIndexOfValue((String) newValue);
                listPreference.setSummary(listPreference.getEntries()[darkModeIndex]);
                if (isNight) {
                    recreateActivity();
                }
            }

            case KEY_ACCENT_COLOR -> {
                final int index = mAccentColorPref.findIndexOfValue((String) newValue);
                mAccentColorPref.setSummary(mAccentColorPref.getEntries()[index]);
                recreateActivity();
            }

            case KEY_AUTO_NIGHT_ACCENT_COLOR, KEY_CARD_BACKGROUND, KEY_CARD_BORDER,
                 KEY_FADE_TRANSITIONS -> {
                recreateActivity();
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_CUSTOM_LANGUAGE_CODE -> {
                final int index = mCustomLanguageCodePref.findIndexOfValue((String) newValue);
                mCustomLanguageCodePref.setSummary(mCustomLanguageCodePref.getEntries()[index]);
                requireContext().sendBroadcast(new Intent(ACTION_LANGUAGE_CODE_CHANGED));
                isLanguageChanged = true;
                recreateActivity();
            }

            case KEY_TAB_TO_DISPLAY -> {
                final int index = mTabToDisplayPref.findIndexOfValue((String) newValue);
                mTabToDisplayPref.setSummary(mTabToDisplayPref.getEntries()[index]);
                // Set result so DeskClock knows to refresh itself
                requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
            }

            case KEY_VIBRATIONS, KEY_TOOLBAR_TITLE -> Utils.setVibrationTime(requireContext(), 50);

            case KEY_TAB_TITLE_VISIBILITY -> {
                final int index = mTabTitleVisibilityPref.findIndexOfValue((String) newValue);
                mTabTitleVisibilityPref.setSummary(mTabTitleVisibilityPref.getEntries()[index]);
                // Set result so DeskClock knows to refresh itself
                requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
            }

            case KEY_TAB_INDICATOR -> {
                Utils.setVibrationTime(requireContext(), 50);
                // Set result so DeskClock knows to refresh itself
                requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
            }

        }

        return true;
    }

    private void setupPreferences() {
        mThemePref.setSummary(mThemePref.getEntry());
        mThemePref.setOnPreferenceChangeListener(this);

        mDarkModePref.setSummary(mDarkModePref.getEntry());
        mDarkModePref.setOnPreferenceChangeListener(this);

        mAccentColorPref.setSummary(mAccentColorPref.getEntry());
        mAccentColorPref.setOnPreferenceChangeListener(this);
        if (SettingsDAO.isAutoNightAccentColorEnabled(mPrefs)) {
            mAccentColorPref.setTitle(requireContext().getString(R.string.title_accent_color));
            mAccentColorPref.setDialogTitle(requireContext().getString(R.string.title_accent_color));
        } else {
            mAccentColorPref.setTitle(requireContext().getString(R.string.day_accent_color_title));
            mAccentColorPref.setDialogTitle(requireContext().getString(R.string.day_accent_color_title));
        }

        mAutoNightAccentColorPref.setOnPreferenceChangeListener(this);

        mNightAccentColorPref.setVisible(!SettingsDAO.isAutoNightAccentColorEnabled(mPrefs));
        mNightAccentColorPref.setSummary(mNightAccentColorPref.getEntry());
        mNightAccentColorPref.setOnPreferenceChangeListener(this);

        mCardBackgroundPref.setOnPreferenceChangeListener(this);

        mCardBorderPref.setOnPreferenceChangeListener(this);

        mToolbarTitlePref.setOnPreferenceChangeListener(this);

        mTabTitleVisibilityPref.setSummary(mTabTitleVisibilityPref.getEntry());
        mTabTitleVisibilityPref.setOnPreferenceChangeListener(this);

        mTabIndicatorPref.setOnPreferenceChangeListener(this);

        mCustomLanguageCodePref.setSummary(mCustomLanguageCodePref.getEntry());
        mCustomLanguageCodePref.setOnPreferenceChangeListener(this);

        mTabToDisplayPref.setSummary(mTabToDisplayPref.getEntry());
        mTabToDisplayPref.setOnPreferenceChangeListener(this);

        final Vibrator vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
        mVibrationPref.setVisible(vibrator.hasVibrator());
        mVibrationPref.setOnPreferenceChangeListener(this);

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
     * Helper method to update all digital widgets.
     */
    private void updateAllDigitalWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        WidgetUtils.updateWidget(context, appWidgetManager, DigitalAppWidgetProvider.class);
        WidgetUtils.updateWidget(context, appWidgetManager, NextAlarmAppWidgetProvider.class);
        WidgetUtils.updateWidget(context, appWidgetManager, VerticalDigitalAppWidgetProvider.class);
        WidgetUtils.updateWidget(context, appWidgetManager, MaterialYouDigitalAppWidgetProvider.class);
        WidgetUtils.updateWidget(context, appWidgetManager, MaterialYouNextAlarmAppWidgetProvider.class);
        WidgetUtils.updateWidget(context, appWidgetManager, MaterialYouVerticalDigitalAppWidgetProvider.class);
    }

    /**
     * Internal class to store entry/value pairs
     */
    private record Pair(CharSequence entry, CharSequence value) {
    }

}
