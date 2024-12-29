/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import android.os.Build;
import android.content.Context;
import android.content.Intent;
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

    private static final String PREFS_FRAGMENT_TAG = "screensaver_prefs_fragment";

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

    public static class PrefsFragment extends ScreenFragment
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        ColorPreference mClockColorPref;
        ColorPreference mDateColorPref;
        ColorPreference mNextAlarmColorPref;
        ListPreference mClockStyle;
        String[] mClockStyleValues;
        String mDigitalClock;
        SwitchPreferenceCompat mDisplaySecondsPref;
        SwitchPreferenceCompat mBoldDigitalClockPref;
        SwitchPreferenceCompat mClockDynamicColorPref;
        SwitchPreferenceCompat mItalicDigitalClockPref;

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
            mBoldDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD);
            mItalicDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC);

            mClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
            mDigitalClock = mClockStyleValues[1];

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
                    mDisplaySecondsPref.setChecked(DataModel.getDataModel().areScreensaverClockSecondsDisplayed());
                    mClockDynamicColorPref.setChecked(DataModel.getDataModel().areScreensaverClockDynamicColors());
                    mBoldDigitalClockPref.setVisible(newValue.equals(mDigitalClock));
                    mItalicDigitalClockPref.setVisible(newValue.equals(mDigitalClock));
                }

                case KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC,
                     KEY_SCREENSAVER_DATE_IN_BOLD, KEY_SCREENSAVER_DATE_IN_ITALIC, KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD,
                     KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC -> Utils.setVibrationTime(requireContext(), 50);

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mClockDynamicColorPref.setVisible(true);
                mClockDynamicColorPref.setChecked(DataModel.getDataModel().areScreensaverClockDynamicColors());
                mClockColorPref.setVisible(!mClockDynamicColorPref.isChecked());
                mDateColorPref.setVisible(!mClockDynamicColorPref.isChecked());
                mNextAlarmColorPref.setVisible(!mClockDynamicColorPref.isChecked());
            }
        }

        private void refresh() {
            final SeekBarPreference screensaverBrightness = findPreference(KEY_SCREENSAVER_BRIGHTNESS);
            final SwitchPreferenceCompat boldDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_BOLD);
            final SwitchPreferenceCompat italicDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_ITALIC);
            final SwitchPreferenceCompat boldNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD);
            final SwitchPreferenceCompat italicNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC);
            final Preference screensaverPreview = findPreference(KEY_SCREENSAVER_PREVIEW);
            final Preference screensaverMainSettings = findPreference(KEY_SCREENSAVER_DAYDREAM_SETTINGS);

            final int index = mClockStyle.findIndexOfValue(DataModel.getDataModel()
                    .getScreensaverClockStyle().toString().toLowerCase());
            mClockStyle.setValueIndex(index);
            mClockStyle.setSummary(mClockStyle.getEntries()[index]);
            mClockStyle.setOnPreferenceChangeListener(this);

            mClockDynamicColorPref.setChecked(DataModel.getDataModel().areScreensaverClockDynamicColors());
            mClockDynamicColorPref.setOnPreferenceChangeListener(this);

            if (screensaverBrightness != null) {
                final int brightnessPercentage = DataModel.getDataModel().getScreensaverBrightness();
                screensaverBrightness.setValue(brightnessPercentage);
                screensaverBrightness.setSummary(brightnessPercentage + "%");
                screensaverBrightness.setOnPreferenceChangeListener(this);
                screensaverBrightness.setUpdatesContinuously(true);
            }

            mDisplaySecondsPref.setChecked(DataModel.getDataModel().areScreensaverClockSecondsDisplayed());
            mDisplaySecondsPref.setOnPreferenceChangeListener(this);

            mBoldDigitalClockPref.setChecked(DataModel.getDataModel().isScreensaverDigitalClockInBold());
            mBoldDigitalClockPref.setOnPreferenceChangeListener(this);

            mItalicDigitalClockPref.setChecked(DataModel.getDataModel().isScreensaverDigitalClockInItalic());
            mItalicDigitalClockPref.setOnPreferenceChangeListener(this);

            if (boldDatePref != null) {
                boldDatePref.setChecked(DataModel.getDataModel().isScreensaverDateInBold());
                boldDatePref.setOnPreferenceChangeListener(this);
            }

            if (italicDatePref != null) {
                italicDatePref.setChecked(DataModel.getDataModel().isScreensaverDateInItalic());
                italicDatePref.setOnPreferenceChangeListener(this);
            }

            if (boldNextAlarmPref != null) {
                boldNextAlarmPref.setChecked(DataModel.getDataModel().isScreensaverNextAlarmInBold());
                boldNextAlarmPref.setOnPreferenceChangeListener(this);
            }

            if (italicNextAlarmPref != null) {
                italicNextAlarmPref.setChecked(DataModel.getDataModel().isScreensaverNextAlarmInItalic());
                italicNextAlarmPref.setOnPreferenceChangeListener(this);
            }

            if (screensaverPreview != null) {
                screensaverPreview.setOnPreferenceClickListener(this);
            }

            if (screensaverMainSettings != null) {
                screensaverMainSettings.setOnPreferenceClickListener(this);
            }
        }
    }
}
