/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.best.deskclock.settings;

import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;

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

/**
 * Settings for Clock screensaver
 */
public final class ScreensaverSettingsActivity extends CollapsingToolbarBaseActivity {
    public static final String KEY_CLOCK_STYLE = "screensaver_clock_style";
    public static final String KEY_CLOCK_DYNAMIC_COLORS = "screensaver_clock_dynamic_colors";
    public static final String KEY_CLOCK_PRESET_COLORS = "screensaver_clock_preset_colors";
    public static final String KEY_DATE_PRESET_COLORS = "screensaver_date_preset_colors";
    public static final String KEY_NEXT_ALARM_PRESET_COLORS = "screensaver_next_alarm_preset_colors";
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
    private static final String PREFS_FRAGMENT_TAG = "prefs_fragment";

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

    @Override
    protected void onResume() {
        super.onResume();

        getSupportFragmentManager().findFragmentById(R.id.main);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public static class PrefsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        ListPreference mClockPresetColorsPref;
        ListPreference mClockStyle;
        ListPreference mDatePresetColorsPref;
        ListPreference mNextAlarmPresetColorsPref;
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
            refresh();
        }

        @Override
        public boolean onPreferenceClick(Preference pref) {
            final Context context = requireActivity();

            switch (pref.getKey()) {
                case KEY_SS_PREVIEW -> {
                    context.startActivity(new Intent(context, ScreensaverActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

                case KEY_CLOCK_DYNAMIC_COLORS -> {
                    if (mClockDynamicColorPref.getSharedPreferences() != null) {
                        final boolean isNotDynamicColors = mClockDynamicColorPref.getSharedPreferences()
                                .getBoolean(KEY_CLOCK_DYNAMIC_COLORS, false);
                        mClockPresetColorsPref.setVisible(isNotDynamicColors);
                        mDatePresetColorsPref.setVisible(isNotDynamicColors);
                        mNextAlarmPresetColorsPref.setVisible(isNotDynamicColors);
                    }
                }

                case KEY_CLOCK_PRESET_COLORS, KEY_DATE_PRESET_COLORS, KEY_NEXT_ALARM_PRESET_COLORS -> {
                    final ListPreference clockPref = (ListPreference) pref;
                    final int clockIndex = clockPref.findIndexOfValue((String) newValue);
                    clockPref.setSummary(clockPref.getEntries()[clockIndex]);
                }

                case KEY_SS_BRIGHTNESS -> {
                    final SeekBarPreference clockBrightness = (SeekBarPreference) pref;
                    final String progress = newValue + "%";
                    clockBrightness.setSummary(progress);
                }
            }

            return true;
        }

        private void hidePreferences() {
            mClockStyle = findPreference(KEY_CLOCK_STYLE);
            mClockDynamicColorPref = findPreference(KEY_CLOCK_DYNAMIC_COLORS);
            mClockPresetColorsPref = findPreference(KEY_CLOCK_PRESET_COLORS);
            mDatePresetColorsPref = findPreference(KEY_DATE_PRESET_COLORS);
            mNextAlarmPresetColorsPref = findPreference(KEY_NEXT_ALARM_PRESET_COLORS);
            mBoldDigitalClockPref = findPreference(KEY_BOLD_DIGITAL_CLOCK);
            mItalicDigitalClockPref = findPreference(KEY_ITALIC_DIGITAL_CLOCK);

            final String digitalClockStyle = DataModel.getDataModel().getScreensaverClockStyle().toString().toLowerCase();
            mBoldDigitalClockPref.setVisible(mClockStyle.getValue().equals(digitalClockStyle));
            mItalicDigitalClockPref.setVisible(mClockStyle.getValue().equals(digitalClockStyle));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mClockDynamicColorPref.setVisible(true);
                mClockDynamicColorPref.setChecked(DataModel.getDataModel().getScreensaverClockDynamicColors());
                mClockPresetColorsPref.setVisible(!mClockDynamicColorPref.isChecked());
                mDatePresetColorsPref.setVisible(!mClockDynamicColorPref.isChecked());
                mNextAlarmPresetColorsPref.setVisible(!mClockDynamicColorPref.isChecked());
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

            final int indexPresetColors = mClockPresetColorsPref.findIndexOfValue(DataModel.getDataModel()
                    .getScreensaverClockPresetColors());
            mClockPresetColorsPref.setValueIndex(indexPresetColors);
            mClockPresetColorsPref.setSummary(mClockPresetColorsPref.getEntries()[indexPresetColors]);
            mClockPresetColorsPref.setOnPreferenceChangeListener(this);

            final int indexDateColor = mDatePresetColorsPref.findIndexOfValue(DataModel.getDataModel()
                    .getScreensaverDatePresetColors());
            mDatePresetColorsPref.setValueIndex(indexDateColor);
            mDatePresetColorsPref.setSummary(mClockPresetColorsPref.getEntries()[indexDateColor]);
            mDatePresetColorsPref.setOnPreferenceChangeListener(this);

            final int indexAlarmColor = mNextAlarmPresetColorsPref.findIndexOfValue(DataModel.getDataModel()
                    .getScreensaverNextAlarmPresetColors());
            if (Utils.getNextAlarm(requireActivity()) == null) {
                mNextAlarmPresetColorsPref.setEnabled(false);
                mNextAlarmPresetColorsPref.setSummary(R.string.screensaver_no_alarm_set);
            } else {
                mNextAlarmPresetColorsPref.setSummary(mClockPresetColorsPref.getEntries()[indexAlarmColor]);
            }

            mNextAlarmPresetColorsPref.setValueIndex(indexAlarmColor);
            mNextAlarmPresetColorsPref.setOnPreferenceChangeListener(this);

            if (screensaverBrightness != null) {
                final int percentage = DataModel.getDataModel().getScreensaverBrightness();
                screensaverBrightness.setValue(percentage);
                screensaverBrightness.setSummary(percentage + "%");
                screensaverBrightness.setOnPreferenceChangeListener(this);
                screensaverBrightness.setUpdatesContinuously(true);
            }

            if (displaySecondsPref != null) {
                displaySecondsPref.setChecked(DataModel.getDataModel().getDisplayScreensaverClockSeconds());
            }

            mBoldDigitalClockPref.setChecked(DataModel.getDataModel().getScreensaverBoldDigitalClock());

            mItalicDigitalClockPref.setChecked(DataModel.getDataModel().getScreensaverItalicDigitalClock());

            if (boldDatePref != null) {
                boldDatePref.setChecked(DataModel.getDataModel().getScreensaverBoldDate());
            }

            if (italicDatePref != null) {
                italicDatePref.setChecked(DataModel.getDataModel().getScreensaverItalicDate());
            }

            if (boldNextAlarmPref != null) {
                if (Utils.getNextAlarm(requireActivity()) == null) {
                    boldNextAlarmPref.setEnabled(false);
                    boldNextAlarmPref.setSummary(R.string.screensaver_no_alarm_set);
                }

                boldNextAlarmPref.setChecked(DataModel.getDataModel().getScreensaverBoldNextAlarm());
            }

            if (italicNextAlarmPref != null) {
                if (Utils.getNextAlarm(requireActivity()) == null) {
                    italicNextAlarmPref.setEnabled(false);
                    italicNextAlarmPref.setSummary(R.string.screensaver_no_alarm_set);
                }

                italicNextAlarmPref.setChecked(DataModel.getDataModel().getScreensaverItalicNextAlarm());
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
