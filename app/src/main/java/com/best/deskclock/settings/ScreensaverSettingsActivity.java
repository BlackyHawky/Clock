/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_DIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DATE_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DATE_IN_BOLD;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DATE_IN_ITALIC;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DAYDREAM_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_PREVIEW;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.rarepebble.colorpicker.ColorPreference;

/**
 * Settings for Clock screensaver
 */
public final class ScreensaverSettingsActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected String getActivityTitle() {
        // Already defined in the fragment.
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new ScreensaverSettingsFragment())
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static class ScreensaverSettingsFragment extends ScreenFragment
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        String[] mClockStyleValues;
        String mAnalogClock;
        String mMaterialAnalogClock;
        String mDigitalClock;

        ColorPreference mClockColorPref;
        ColorPreference mDateColorPref;
        ColorPreference mNextAlarmColorPref;
        ListPreference mClockStylePref;
        ListPreference mClockDialPref;
        ListPreference mClockDialMaterialPref;
        ListPreference mClockSecondHandPref;
        SwitchPreferenceCompat mDisplaySecondsPref;
        SwitchPreferenceCompat mBoldDigitalClockPref;
        SwitchPreferenceCompat mClockDynamicColorPref;
        SwitchPreferenceCompat mItalicDigitalClockPref;
        SwitchPreferenceCompat mBoldDatePref;
        SwitchPreferenceCompat mItalicDatePref;
        SwitchPreferenceCompat mBoldNextAlarmPref;
        SwitchPreferenceCompat mItalicNextAlarmPref;
        Preference mScreensaverPreview;
        Preference mScreensaverMainSettings;

        @Override
        protected String getFragmentTitle() {
            return getString(R.string.screensaver_settings_title);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_screensaver);

            mClockStylePref = findPreference(KEY_SCREENSAVER_CLOCK_STYLE);
            mClockDialPref = findPreference(KEY_SCREENSAVER_CLOCK_DIAL);
            mClockDialMaterialPref = findPreference(KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL);
            mDisplaySecondsPref = findPreference(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS);
            mClockSecondHandPref = findPreference(KEY_SCREENSAVER_CLOCK_SECOND_HAND);
            mClockDynamicColorPref = findPreference(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS);
            mClockColorPref = findPreference(KEY_SCREENSAVER_CLOCK_COLOR_PICKER);
            mDateColorPref = findPreference(KEY_SCREENSAVER_DATE_COLOR_PICKER);
            mNextAlarmColorPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER);
            mBoldDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD);
            mItalicDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC);
            mBoldDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_BOLD);
            mItalicDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_ITALIC);
            mBoldNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD);
            mItalicNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC);
            mScreensaverPreview = findPreference(KEY_SCREENSAVER_PREVIEW);
            mScreensaverMainSettings = findPreference(KEY_SCREENSAVER_DAYDREAM_SETTINGS);

            mClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
            mAnalogClock = mClockStyleValues[0];
            mMaterialAnalogClock = mClockStyleValues[1];
            mDigitalClock = mClockStyleValues[2];

            setupPreferences();
        }

        @Override
        public boolean onPreferenceClick(Preference pref) {
            final Context context = requireActivity();

            switch (pref.getKey()) {
                case KEY_SCREENSAVER_PREVIEW -> {
                    context.startActivity(new Intent(context, ScreensaverActivity.class)
                            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));
                    return true;
                }

                case KEY_SCREENSAVER_DAYDREAM_SETTINGS -> {
                    final Intent dialogSSMainSettingsIntent = new Intent(Settings.ACTION_DREAM_SETTINGS);
                    dialogSSMainSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogSSMainSettingsIntent);
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_SCREENSAVER_CLOCK_STYLE -> {
                    final int clockIndex = mClockStylePref.findIndexOfValue((String) newValue);
                    mClockStylePref.setSummary(mClockStylePref.getEntries()[clockIndex]);
                    if (SdkUtils.isAtLeastAndroid12()) {
                        mClockDynamicColorPref.setVisible(!newValue.equals(mMaterialAnalogClock));
                        mClockColorPref.setVisible(!newValue.equals(mMaterialAnalogClock)
                                && !SettingsDAO.areScreensaverClockDynamicColors(mPrefs));
                        if (SettingsDAO.areScreensaverClockDynamicColors(mPrefs)) {
                            mDateColorPref.setVisible(newValue.equals(mMaterialAnalogClock));
                            mNextAlarmColorPref.setVisible(newValue.equals(mMaterialAnalogClock));
                        }
                    } else {
                        mClockColorPref.setVisible(!newValue.equals(mMaterialAnalogClock));
                    }
                    mClockDialPref.setVisible(newValue.equals(mAnalogClock));
                    mClockDialMaterialPref.setVisible(newValue.equals(mMaterialAnalogClock));
                    mClockSecondHandPref.setVisible(newValue.equals(mAnalogClock)
                            && SettingsDAO.areScreensaverClockSecondsDisplayed(mPrefs));
                    mBoldDigitalClockPref.setVisible(newValue.equals(mDigitalClock));
                    mItalicDigitalClockPref.setVisible(newValue.equals(mDigitalClock));
                }

                case KEY_SCREENSAVER_CLOCK_DIAL, KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL,
                     KEY_SCREENSAVER_CLOCK_SECOND_HAND -> {
                    final ListPreference preference = (ListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                }

                case KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS -> {
                    mClockSecondHandPref.setVisible((boolean) newValue
                            && SettingsDAO.getScreensaverClockStyle(mPrefs) == DataModel.ClockStyle.ANALOG);

                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC,
                     KEY_SCREENSAVER_DATE_IN_BOLD, KEY_SCREENSAVER_DATE_IN_ITALIC,
                     KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD,
                     KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC ->
                        Utils.setVibrationTime(requireContext(), 50);

                case KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS -> {
                    mClockColorPref.setVisible(!((boolean) newValue));
                    mDateColorPref.setVisible(!((boolean) newValue));
                    mNextAlarmColorPref.setVisible(!((boolean) newValue));
                    Utils.setVibrationTime(requireContext(), 50);
                }
            }

            return true;
        }

        @Override
        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            if (preference instanceof ColorPreference) {
                ((ColorPreference) preference).showDialog(this, 0);
            } else super.onDisplayPreferenceDialog(preference);
        }

        private void setupPreferences() {
            mClockStylePref.setSummary(mClockStylePref.getEntry());
            mClockStylePref.setOnPreferenceChangeListener(this);

            mClockDialPref.setVisible(mClockStylePref.getValue().equals(mAnalogClock));
            mClockDialPref.setSummary(mClockDialPref.getEntry());
            mClockDialPref.setOnPreferenceChangeListener(this);

            mClockDialMaterialPref.setVisible(mClockStylePref.getValue().equals(mMaterialAnalogClock));
            mClockDialMaterialPref.setSummary(mClockDialMaterialPref.getEntry());
            mClockDialMaterialPref.setOnPreferenceChangeListener(this);

            mDisplaySecondsPref.setOnPreferenceChangeListener(this);

            mClockSecondHandPref.setVisible(mClockStylePref.getValue().equals(mAnalogClock)
                    && SettingsDAO.areScreensaverClockSecondsDisplayed(mPrefs));
            mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
            mClockSecondHandPref.setOnPreferenceChangeListener(this);

            if (SdkUtils.isAtLeastAndroid12()) {
                final boolean areScreensaverClockDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(mPrefs);
                mClockDynamicColorPref.setVisible(!mClockStylePref.getValue().equals(mMaterialAnalogClock));
                mClockDynamicColorPref.setOnPreferenceChangeListener(this);
                mClockColorPref.setVisible(!areScreensaverClockDynamicColors
                        && !mClockStylePref.getValue().equals(mMaterialAnalogClock));
                mDateColorPref.setVisible(!areScreensaverClockDynamicColors
                        || mClockStylePref.getValue().equals(mMaterialAnalogClock));
                mNextAlarmColorPref.setVisible(!areScreensaverClockDynamicColors
                        || mClockStylePref.getValue().equals(mMaterialAnalogClock));
            } else {
                mClockColorPref.setVisible(!mClockStylePref.getValue().equals(mMaterialAnalogClock));
            }

            mBoldDigitalClockPref.setVisible(mClockStylePref.getValue().equals(mDigitalClock));
            mItalicDigitalClockPref.setVisible(mClockStylePref.getValue().equals(mDigitalClock));

            mBoldDigitalClockPref.setOnPreferenceChangeListener(this);

            mItalicDigitalClockPref.setOnPreferenceChangeListener(this);

            mBoldDatePref.setOnPreferenceChangeListener(this);

            mItalicDatePref.setOnPreferenceChangeListener(this);

            mBoldNextAlarmPref.setOnPreferenceChangeListener(this);

            mItalicNextAlarmPref.setOnPreferenceChangeListener(this);

            mScreensaverPreview.setOnPreferenceClickListener(this);

            mScreensaverMainSettings.setOnPreferenceClickListener(this);
        }

    }
    
}
