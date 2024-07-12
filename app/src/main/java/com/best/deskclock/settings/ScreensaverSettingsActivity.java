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
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.ScreensaverActivity;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.Events;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.rarepebble.colorpicker.ColorPreference;

/**
 * Settings for Clock screensaver
 */
public final class ScreensaverSettingsActivity extends CollapsingToolbarBaseActivity {
    public static final String KEY_CLOCK_STYLE = "screensaver_clock_style";
    public static final String KEY_CLOCK_DYNAMIC_COLORS = "screensaver_clock_dynamic_colors";
    public static final String KEY_CLOCK_COLOR_PICKER = "key_clock_color_picker";
    public static final String KEY_DATE_COLOR_PICKER = "key_date_color_picker";
    public static final String KEY_NEXT_ALARM_COLOR_PICKER = "key_next_alarm_color_picker";
    public static final String KEY_SS_BRIGHTNESS = "screensaver_brightness";
    public static final String KEY_SS_CLOCK_DISPLAY_SECONDS = "display_screensaver_clock_seconds";
    public static final String KEY_BOLD_DIGITAL_CLOCK = "screensaver_bold_digital_clock";
    public static final String KEY_ITALIC_DIGITAL_CLOCK = "screensaver_italic_digital_clock";
    public static final String KEY_BOLD_DATE = "screensaver_bold_date";
    public static final String KEY_ITALIC_DATE = "screensaver_italic_date";
    public static final String KEY_BOLD_NEXT_ALARM = "screensaver_bold_next_alarm";
    public static final String KEY_ITALIC_NEXT_ALARM = "screensaver_italic_next_alarm";
    public static final String KEY_SS_PREVIEW = "screensaver_preview";
    public static final String KEY_SS_DAYDREAM_SETTINGS = "screensaver_daydream_settings";
    private static final String PREFS_FRAGMENT_TAG = "screensaver_prefs_fragment";

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
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        ColorPreference mClockColorPref;
        ColorPreference mDateColorPref;
        ColorPreference mNextAlarmColorPref;
        ListPreference mClockStyle;
        String[] mClockStyleValues;
        String mDigitalClock;
        SwitchPreferenceCompat mBoldDigitalClockPref;
        SwitchPreferenceCompat mClockDynamicColorPref;
        SwitchPreferenceCompat mItalicDigitalClockPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getPreferenceManager().setStorageDeviceProtected();
            }

            mClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
            mDigitalClock = mClockStyleValues[1];
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.screensaver_settings);

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
        public boolean onPreferenceClick(Preference pref) {
            final Context context = requireActivity();

            switch (pref.getKey()) {
                case KEY_SS_PREVIEW -> {
                    context.startActivity(new Intent(context, ScreensaverActivity.class)
                            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));
                    return true;
                }

                case KEY_SS_DAYDREAM_SETTINGS -> {
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

                case KEY_CLOCK_STYLE -> {
                    final int clockIndex = mClockStyle.findIndexOfValue((String) newValue);
                    mClockStyle.setSummary(mClockStyle.getEntries()[clockIndex]);
                    mBoldDigitalClockPref.setVisible(newValue.equals(mDigitalClock));
                    mItalicDigitalClockPref.setVisible(newValue.equals(mDigitalClock));
                }

                case KEY_SS_CLOCK_DISPLAY_SECONDS, KEY_BOLD_DIGITAL_CLOCK, KEY_ITALIC_DIGITAL_CLOCK, KEY_BOLD_DATE, KEY_ITALIC_DATE,
                        KEY_BOLD_NEXT_ALARM, KEY_ITALIC_NEXT_ALARM
                         -> Utils.setVibrationTime(requireContext(), 50);

                case KEY_CLOCK_DYNAMIC_COLORS -> {
                    if (mClockDynamicColorPref.getSharedPreferences() != null) {
                        final boolean isNotDynamicColors = mClockDynamicColorPref.getSharedPreferences()
                                .getBoolean(KEY_CLOCK_DYNAMIC_COLORS, false);
                        mClockColorPref.setVisible(isNotDynamicColors);
                        mDateColorPref.setVisible(isNotDynamicColors);
                        mNextAlarmColorPref.setVisible(isNotDynamicColors);
                    }
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_SS_BRIGHTNESS -> {
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

        private void hidePreferences() {
            mClockStyle = findPreference(KEY_CLOCK_STYLE);
            mClockDynamicColorPref = findPreference(KEY_CLOCK_DYNAMIC_COLORS);
            mClockColorPref = findPreference(KEY_CLOCK_COLOR_PICKER);
            mDateColorPref = findPreference(KEY_DATE_COLOR_PICKER);
            mNextAlarmColorPref = findPreference(KEY_NEXT_ALARM_COLOR_PICKER);
            mBoldDigitalClockPref = findPreference(KEY_BOLD_DIGITAL_CLOCK);
            mItalicDigitalClockPref = findPreference(KEY_ITALIC_DIGITAL_CLOCK);

            final String digitalClockStyle = DataModel.getDataModel().getScreensaverClockStyle().toString().toLowerCase();
            mBoldDigitalClockPref.setVisible(mClockStyle.getValue().equals(digitalClockStyle));
            mItalicDigitalClockPref.setVisible(mClockStyle.getValue().equals(digitalClockStyle));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mClockDynamicColorPref.setVisible(true);
                mClockDynamicColorPref.setChecked(DataModel.getDataModel().getScreensaverClockDynamicColors());
                mClockColorPref.setVisible(!mClockDynamicColorPref.isChecked());
                mDateColorPref.setVisible(!mClockDynamicColorPref.isChecked());
                mNextAlarmColorPref.setVisible(!mClockDynamicColorPref.isChecked());
            }
        }

        private void refresh() {
            final SeekBarPreference screensaverBrightness = findPreference(KEY_SS_BRIGHTNESS);
            final SwitchPreferenceCompat displaySecondsPref = findPreference(KEY_SS_CLOCK_DISPLAY_SECONDS);
            final SwitchPreferenceCompat boldDatePref = findPreference(KEY_BOLD_DATE);
            final SwitchPreferenceCompat italicDatePref = findPreference(KEY_ITALIC_DATE);
            final SwitchPreferenceCompat boldNextAlarmPref = findPreference(KEY_BOLD_NEXT_ALARM);
            final SwitchPreferenceCompat italicNextAlarmPref = findPreference(KEY_ITALIC_NEXT_ALARM);
            final Preference screensaverPreview = findPreference(KEY_SS_PREVIEW);
            final Preference screensaverMainSettings = findPreference(KEY_SS_DAYDREAM_SETTINGS);

            final int index = mClockStyle.findIndexOfValue(DataModel.getDataModel()
                    .getScreensaverClockStyle().toString().toLowerCase());
            mClockStyle.setValueIndex(index);
            mClockStyle.setSummary(mClockStyle.getEntries()[index]);
            mClockStyle.setOnPreferenceChangeListener(this);

            mClockDynamicColorPref.setChecked(DataModel.getDataModel().getScreensaverClockDynamicColors());
            mClockDynamicColorPref.setOnPreferenceChangeListener(this);

            if (screensaverBrightness != null) {
                final int percentage = DataModel.getDataModel().getScreensaverBrightness();
                screensaverBrightness.setValue(percentage);
                screensaverBrightness.setSummary(percentage + "%");
                screensaverBrightness.setOnPreferenceChangeListener(this);
                screensaverBrightness.setUpdatesContinuously(true);
            }

            if (displaySecondsPref != null) {
                displaySecondsPref.setChecked(DataModel.getDataModel().getDisplayScreensaverClockSeconds());
                displaySecondsPref.setOnPreferenceChangeListener(this);
            }

            mBoldDigitalClockPref.setChecked(DataModel.getDataModel().getScreensaverBoldDigitalClock());
            mBoldDigitalClockPref.setOnPreferenceChangeListener(this);

            mItalicDigitalClockPref.setChecked(DataModel.getDataModel().getScreensaverItalicDigitalClock());
            mItalicDigitalClockPref.setOnPreferenceChangeListener(this);

            if (boldDatePref != null) {
                boldDatePref.setChecked(DataModel.getDataModel().getScreensaverBoldDate());
                boldDatePref.setOnPreferenceChangeListener(this);
            }

            if (italicDatePref != null) {
                italicDatePref.setChecked(DataModel.getDataModel().getScreensaverItalicDate());
                italicDatePref.setOnPreferenceChangeListener(this);
            }

            if (boldNextAlarmPref != null) {
                boldNextAlarmPref.setChecked(DataModel.getDataModel().getScreensaverBoldNextAlarm());
                boldNextAlarmPref.setOnPreferenceChangeListener(this);
            }

            if (italicNextAlarmPref != null) {
                italicNextAlarmPref.setChecked(DataModel.getDataModel().getScreensaverItalicNextAlarm());
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
