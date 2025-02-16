/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

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
import com.best.deskclock.data.DataModel;
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

        public static final String KEY_SCREENSAVER_CLOCK_STYLE = "key_screensaver_clock_style";
        public static final String KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS = "key_display_screensaver_clock_seconds";
        public static final String KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS = "key_screensaver_clock_dynamic_colors";
        public static final String KEY_SCREENSAVER_CLOCK_COLOR_PICKER = "key_screensaver_clock_color_picker";
        public static final String KEY_SCREENSAVER_DATE_COLOR_PICKER = "key_screensaver_date_color_picker";
        public static final String KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER = "key_screensaver_next_alarm_color_picker";
        public static final String KEY_SCREENSAVER_BRIGHTNESS = "key_screensaver_brightness";
        public static final String KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD = "key_screensaver_digital_clock_in_bold";
        public static final String KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC = "key_screensaver_digital_clock_in_italic";
        public static final String KEY_SCREENSAVER_DATE_IN_BOLD = "key_screensaver_date_in_bold";
        public static final String KEY_SCREENSAVER_DATE_IN_ITALIC = "key_screensaver_date_in_italic";
        public static final String KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD = "key_screensaver_next_alarm_in_bold";
        public static final String KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC = "key_screensaver_next_alarm_in_italic";
        public static final String KEY_SCREENSAVER_PREVIEW = "key_screensaver_preview";
        public static final String KEY_SCREENSAVER_DAYDREAM_SETTINGS = "key_screensaver_daydream_settings";

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
                    mDisplaySecondsPref.setChecked(DataModel.getDataModel().areScreensaverClockSecondsDisplayed());
                    mClockDynamicColorPref.setChecked(DataModel.getDataModel().areScreensaverClockDynamicColors());
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
            final int screensaverClockIndex = mClockStyle.findIndexOfValue(DataModel.getDataModel()
                    .getScreensaverClockStyle().toString().toLowerCase());
            mDisplaySecondsPref.setChecked(DataModel.getDataModel().areScreensaverClockSecondsDisplayed());
            // screensaverClockIndex == 1 --> digital
            mBoldDigitalClockPref.setVisible(screensaverClockIndex == 1);
            mItalicDigitalClockPref.setVisible(screensaverClockIndex == 1);

            mBoldNextAlarmPref.setEnabled(DataModel.getDataModel().isUpcomingAlarmDisplayed());
            if (!mBoldNextAlarmPref.isEnabled()) {
                mBoldNextAlarmPref.setSummary(R.string.warning_upcoming_alarm_setting_off);
            }

            mItalicNextAlarmPref.setEnabled(DataModel.getDataModel().isUpcomingAlarmDisplayed());
            if (!mItalicNextAlarmPref.isEnabled()) {
                mItalicNextAlarmPref.setSummary(R.string.warning_upcoming_alarm_setting_off);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mClockDynamicColorPref.setVisible(true);
                mClockDynamicColorPref.setChecked(DataModel.getDataModel().areScreensaverClockDynamicColors());
                mClockColorPref.setVisible(!mClockDynamicColorPref.isChecked());
                mDateColorPref.setVisible(!mClockDynamicColorPref.isChecked());
                mNextAlarmColorPref.setVisible(!mClockDynamicColorPref.isChecked());
                mNextAlarmColorPref.setEnabled(DataModel.getDataModel().isUpcomingAlarmDisplayed());
                if (!mNextAlarmColorPref.isEnabled()) {
                    mNextAlarmColorPref.setSummary(R.string.warning_upcoming_alarm_setting_off);
                }
            }
        }

        private void refresh() {
            final int index = mClockStyle.findIndexOfValue(DataModel.getDataModel()
                    .getScreensaverClockStyle().toString().toLowerCase());
            mClockStyle.setValueIndex(index);
            mClockStyle.setSummary(mClockStyle.getEntries()[index]);
            mClockStyle.setOnPreferenceChangeListener(this);

            mClockDynamicColorPref.setChecked(DataModel.getDataModel().areScreensaverClockDynamicColors());
            mClockDynamicColorPref.setOnPreferenceChangeListener(this);

            final int brightnessPercentage = DataModel.getDataModel().getScreensaverBrightness();
            mScreensaverBrightness.setValue(brightnessPercentage);
            mScreensaverBrightness.setSummary(brightnessPercentage + "%");
            mScreensaverBrightness.setOnPreferenceChangeListener(this);
            mScreensaverBrightness.setUpdatesContinuously(true);

            mDisplaySecondsPref.setChecked(DataModel.getDataModel().areScreensaverClockSecondsDisplayed());
            mDisplaySecondsPref.setOnPreferenceChangeListener(this);

            mBoldDigitalClockPref.setChecked(DataModel.getDataModel().isScreensaverDigitalClockInBold());
            mBoldDigitalClockPref.setOnPreferenceChangeListener(this);

            mItalicDigitalClockPref.setChecked(DataModel.getDataModel().isScreensaverDigitalClockInItalic());
            mItalicDigitalClockPref.setOnPreferenceChangeListener(this);

            mBoldDatePref.setChecked(DataModel.getDataModel().isScreensaverDateInBold());
            mBoldDatePref.setOnPreferenceChangeListener(this);

            mItalicDatePref.setChecked(DataModel.getDataModel().isScreensaverDateInItalic());
            mItalicDatePref.setOnPreferenceChangeListener(this);

            mBoldNextAlarmPref.setChecked(DataModel.getDataModel().isScreensaverNextAlarmInBold());
            mBoldNextAlarmPref.setOnPreferenceChangeListener(this);

            mItalicNextAlarmPref.setChecked(DataModel.getDataModel().isScreensaverNextAlarmInItalic());
            mItalicNextAlarmPref.setOnPreferenceChangeListener(this);

            mScreensaverPreview.setOnPreferenceClickListener(this);

            mScreensaverMainSettings.setOnPreferenceClickListener(this);
        }

    }
    
}
