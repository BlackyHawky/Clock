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

        private int mRecyclerViewPosition = -1;

        ColorPreference mClockColorPref;
        ColorPreference mDateColorPref;
        ColorPreference mNextAlarmColorPref;
        ListPreference mClockStyle;
        String[] mClockStyleValues;
        String mDigitalClock;
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
            mDigitalClock = mClockStyleValues[1];

            setupPreferences();
        }

        @Override
        public void onResume() {
            super.onResume();

            if (mRecyclerViewPosition != -1) {
                mLinearLayoutManager.scrollToPosition(mRecyclerViewPosition);
                mAppBarLayout.setExpanded(mRecyclerViewPosition == 0, true);
            }
            refresh();
        }

        @Override
        public void onPause() {
            super.onPause();

            if (mLinearLayoutManager != null) {
                mRecyclerViewPosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
            }
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
                    mDisplaySecondsPref.setChecked(SettingsDAO.areScreensaverClockSecondsDisplayed(mPrefs));
                    mClockDynamicColorPref.setChecked(SettingsDAO.areScreensaverClockDynamicColors(mPrefs));
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
                    if (mClockDynamicColorPref.getSharedPreferences() != null
                            && mDisplaySecondsPref.getSharedPreferences() != null) {

                        final boolean isNotDynamicColors = mClockDynamicColorPref.getSharedPreferences()
                                .getBoolean(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS, false);

                        mClockColorPref.setVisible(isNotDynamicColors);
                        mDateColorPref.setVisible(isNotDynamicColors);
                        mNextAlarmColorPref.setVisible(isNotDynamicColors);
                    }
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
            mDisplaySecondsPref.setChecked(SettingsDAO.areScreensaverClockSecondsDisplayed(mPrefs));
            // screensaverClockIndex == 1 --> digital
            mBoldDigitalClockPref.setVisible(screensaverClockIndex == 1);
            mItalicDigitalClockPref.setVisible(screensaverClockIndex == 1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mClockDynamicColorPref.setVisible(true);
                mClockDynamicColorPref.setChecked(SettingsDAO.areScreensaverClockDynamicColors(mPrefs));
                mClockColorPref.setVisible(!mClockDynamicColorPref.isChecked());
                mDateColorPref.setVisible(!mClockDynamicColorPref.isChecked());
                mNextAlarmColorPref.setVisible(!mClockDynamicColorPref.isChecked());
            }
        }

        private void refresh() {
            final int index = mClockStyle.findIndexOfValue(
                    SettingsDAO.getScreensaverClockStyle(mPrefs).toString().toLowerCase());
            mClockStyle.setValueIndex(index);
            mClockStyle.setSummary(mClockStyle.getEntries()[index]);
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
