/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS;
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
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
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
        String mMaterialAnalogClock;
        String mDigitalClock;

        ColorPreference mClockColorPref;
        ColorPreference mDateColorPref;
        ColorPreference mNextAlarmColorPref;
        ListPreference mClockStyle;
        SeekBarPreference mScreensaverBrightness;
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

            addPreferencesFromResource(R.xml.screensaver_settings);

            mClockStyle = findPreference(KEY_SCREENSAVER_CLOCK_STYLE);
            mDisplaySecondsPref = findPreference(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS);
            mClockDynamicColorPref = findPreference(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS);
            mClockColorPref = findPreference(KEY_SCREENSAVER_CLOCK_COLOR_PICKER);
            mDateColorPref = findPreference(KEY_SCREENSAVER_DATE_COLOR_PICKER);
            mNextAlarmColorPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER);
            mScreensaverBrightness = findPreference(KEY_SCREENSAVER_BRIGHTNESS);
            mBoldDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD);
            mItalicDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC);
            mBoldDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_BOLD);
            mItalicDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_ITALIC);
            mBoldNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD);
            mItalicNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC);
            mScreensaverPreview = findPreference(KEY_SCREENSAVER_PREVIEW);
            mScreensaverMainSettings = findPreference(KEY_SCREENSAVER_DAYDREAM_SETTINGS);

            mClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
            mMaterialAnalogClock = mClockStyleValues[1];
            mDigitalClock = mClockStyleValues[2];

            setupPreferences();
        }

        @Override
        public void onResume() {
            super.onResume();

            refresh();
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
                    final int clockIndex = mClockStyle.findIndexOfValue((String) newValue);
                    mClockStyle.setSummary(mClockStyle.getEntries()[clockIndex]);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        mClockDynamicColorPref.setVisible(!newValue.equals(mMaterialAnalogClock));
                        mClockDynamicColorPref.setChecked(SettingsDAO.areScreensaverClockDynamicColors(mPrefs));
                        mClockColorPref.setVisible(!newValue.equals(mMaterialAnalogClock) && !mClockDynamicColorPref.isChecked());
                        if (mClockDynamicColorPref.isChecked()) {
                            mDateColorPref.setVisible(newValue.equals(mMaterialAnalogClock));
                            mNextAlarmColorPref.setVisible(newValue.equals(mMaterialAnalogClock));
                        }
                    } else {
                        mClockColorPref.setVisible(!newValue.equals(mMaterialAnalogClock));
                    }
                    mBoldDigitalClockPref.setVisible(newValue.equals(mDigitalClock));
                    mItalicDigitalClockPref.setVisible(newValue.equals(mDigitalClock));
                }

                case KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD,
                     KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC,
                     KEY_SCREENSAVER_DATE_IN_BOLD, KEY_SCREENSAVER_DATE_IN_ITALIC,
                     KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD,
                     KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC ->
                        Utils.setVibrationTime(requireContext(), 50);

                case KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS -> {
                    final boolean areNotDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(mPrefs);
                    mClockColorPref.setVisible(areNotDynamicColors);
                    mDateColorPref.setVisible(areNotDynamicColors);
                    mNextAlarmColorPref.setVisible(areNotDynamicColors);
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_SCREENSAVER_BRIGHTNESS -> {
                    final SeekBarPreference clockBrightness = (SeekBarPreference) pref;
                    final String progress = newValue + "%";
                    clockBrightness.setSummary(progress);
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
            final int screensaverClockIndex = mClockStyle.findIndexOfValue(
                    SettingsDAO.getScreensaverClockStyle(mPrefs).toString().toLowerCase());
            // screensaverClockIndex == 0 --> analog
            // screensaverClockIndex == 1 --> analog (material)
            // screensaverClockIndex == 2 --> digital
            mBoldDigitalClockPref.setVisible(screensaverClockIndex == 2);
            mItalicDigitalClockPref.setVisible(screensaverClockIndex == 2);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mClockDynamicColorPref.setVisible(screensaverClockIndex != 1);
                mClockDynamicColorPref.setChecked(SettingsDAO.areScreensaverClockDynamicColors(mPrefs));
                mClockColorPref.setVisible(!mClockDynamicColorPref.isChecked() && screensaverClockIndex != 1);
                mDateColorPref.setVisible(!mClockDynamicColorPref.isChecked()
                        || (screensaverClockIndex == 1 && mClockDynamicColorPref.isChecked()));
                mNextAlarmColorPref.setVisible(!mClockDynamicColorPref.isChecked()
                        || (screensaverClockIndex == 1 && mClockDynamicColorPref.isChecked()));
            } else {
                mClockColorPref.setVisible(screensaverClockIndex != 1);
            }
        }

        private void refresh() {
            mClockStyle.setSummary(mClockStyle.getEntry());
            mClockStyle.setOnPreferenceChangeListener(this);

            mClockDynamicColorPref.setChecked(SettingsDAO.areScreensaverClockDynamicColors(mPrefs));
            mClockDynamicColorPref.setOnPreferenceChangeListener(this);

            final int brightnessPercentage = SettingsDAO.getScreensaverBrightness(mPrefs);
            mScreensaverBrightness.setValue(brightnessPercentage);
            mScreensaverBrightness.setSummary(brightnessPercentage + "%");
            mScreensaverBrightness.setOnPreferenceChangeListener(this);
            mScreensaverBrightness.setUpdatesContinuously(true);

            mDisplaySecondsPref.setChecked(SettingsDAO.areScreensaverClockSecondsDisplayed(mPrefs));
            mDisplaySecondsPref.setOnPreferenceChangeListener(this);

            mBoldDigitalClockPref.setChecked(SettingsDAO.isScreensaverDigitalClockInBold(mPrefs));
            mBoldDigitalClockPref.setOnPreferenceChangeListener(this);

            mItalicDigitalClockPref.setChecked(SettingsDAO.isScreensaverDigitalClockInItalic(mPrefs));
            mItalicDigitalClockPref.setOnPreferenceChangeListener(this);

            mBoldDatePref.setChecked(SettingsDAO.isScreensaverDateInBold(mPrefs));
            mBoldDatePref.setOnPreferenceChangeListener(this);

            mItalicDatePref.setChecked(SettingsDAO.isScreensaverDateInItalic(mPrefs));
            mItalicDatePref.setOnPreferenceChangeListener(this);

            mBoldNextAlarmPref.setChecked(SettingsDAO.isScreensaverNextAlarmInBold(mPrefs));
            mBoldNextAlarmPref.setOnPreferenceChangeListener(this);

            mItalicNextAlarmPref.setChecked(SettingsDAO.isScreensaverNextAlarmInItalic(mPrefs));
            mItalicNextAlarmPref.setOnPreferenceChangeListener(this);

            mScreensaverPreview.setOnPreferenceClickListener(this);

            mScreensaverMainSettings.setOnPreferenceClickListener(this);
        }

    }
    
}
