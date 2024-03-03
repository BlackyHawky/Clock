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
    public static final String KEY_CLOCK_COLOR = "screensaver_clock_color";
    public static final String KEY_DATE_COLOR = "screensaver_date_color";
    public static final String KEY_NEXT_ALARM_COLOR = "screensaver_next_alarm_color";
    public static final String KEY_SS_BRIGHTNESS = "screensaver_brightness";
    public static final String KEY_SS_CLOCK_DISPLAY_SECONDS = "display_screensaver_clock_seconds";
    public static final String KEY_BOLD_DIGITAL_ALARM = "screensaver_bold_digital_clock";
    public static final String KEY_BOLD_DATE = "screensaver_bold_date";
    public static final String KEY_BOLD_NEXT_ALARM = "screensaver_bold_next_alarm";
    public static final String KEY_SS_PREVIEW = "screensaver_preview";
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

        getWindow().setNavigationBarColor(getColor(R.color.md_theme_background));
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

        String[] mClockStyleValues;
        String mDigitalClock;

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
        }

        @Override
        public void onResume() {
            super.onResume();
            refresh();
        }

        @Override
        public boolean onPreferenceClick(Preference pref) {
            final Context context = requireActivity();

            if (pref.getKey().equals(KEY_SS_PREVIEW)) {
                context.startActivity(new Intent(context, ScreensaverActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));
                return true;
            }

            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {

                case KEY_CLOCK_STYLE -> {
                    final ListPreference clockPref = (ListPreference) pref;
                    final SwitchPreferenceCompat boldDigitalClockPref = findPreference(KEY_BOLD_DIGITAL_ALARM);
                    final int clockIndex = clockPref.findIndexOfValue((String) newValue);

                    clockPref.setSummary(clockPref.getEntries()[clockIndex]);

                    if (boldDigitalClockPref != null) {
                        if (!newValue.equals(mDigitalClock)) {
                            boldDigitalClockPref.setEnabled(false);
                            boldDigitalClockPref.setSummary(R.string.screensaver_digital_clock_not_selected);
                        } else {
                            boldDigitalClockPref.setEnabled(true);
                            boldDigitalClockPref.setSummary(null);
                        }
                    }
                }

                case KEY_CLOCK_COLOR, KEY_DATE_COLOR, KEY_NEXT_ALARM_COLOR -> {
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

        private void refresh() {
            final ListPreference clockStylePref = findPreference(KEY_CLOCK_STYLE);
            final ListPreference clockColorPref = findPreference(KEY_CLOCK_COLOR);
            final ListPreference dateColorPref = findPreference(KEY_DATE_COLOR);
            final ListPreference nextAlarmColorPref = findPreference(KEY_NEXT_ALARM_COLOR);
            final SeekBarPreference screensaverBrightness = findPreference(KEY_SS_BRIGHTNESS);
            final SwitchPreferenceCompat displaySecondsPref = findPreference(KEY_SS_CLOCK_DISPLAY_SECONDS);
            final SwitchPreferenceCompat boldDigitalClockPref = findPreference(KEY_BOLD_DIGITAL_ALARM);
            final SwitchPreferenceCompat boldDatePref = findPreference(KEY_BOLD_DATE);
            final SwitchPreferenceCompat boldNextAlarmPref = findPreference(KEY_BOLD_NEXT_ALARM);
            final Preference screensaverPreview = findPreference(KEY_SS_PREVIEW);

            if (clockStylePref != null) {
                final int index = clockStylePref.findIndexOfValue(DataModel.getDataModel()
                        .getScreensaverClockStyle().toString().toLowerCase());
                clockStylePref.setValueIndex(index);
                clockStylePref.setSummary(clockStylePref.getEntries()[index]);
                clockStylePref.setOnPreferenceChangeListener(this);
            }

            if (clockColorPref != null) {
                final int indexColor = clockColorPref.findIndexOfValue(DataModel.getDataModel().getScreensaverClockColor());
                clockColorPref.setValueIndex(indexColor);
                clockColorPref.setSummary(clockColorPref.getEntries()[indexColor]);
                clockColorPref.setOnPreferenceChangeListener(this);
            }

            if (dateColorPref != null && clockColorPref != null) {
                final int indexColor = dateColorPref.findIndexOfValue(DataModel.getDataModel().getScreensaverDateColor());
                dateColorPref.setValueIndex(indexColor);
                dateColorPref.setSummary(clockColorPref.getEntries()[indexColor]);
                dateColorPref.setOnPreferenceChangeListener(this);
            }

            if (nextAlarmColorPref != null && clockColorPref != null) {
                final int indexColor = nextAlarmColorPref.findIndexOfValue(DataModel.getDataModel().getScreensaverNextAlarmColor());
                if (Utils.getNextAlarm(requireActivity()) == null) {
                    nextAlarmColorPref.setEnabled(false);
                    nextAlarmColorPref.setSummary(R.string.screensaver_no_alarm_set);
                } else {
                    nextAlarmColorPref.setSummary(clockColorPref.getEntries()[indexColor]);
                }

                nextAlarmColorPref.setValueIndex(indexColor);
                nextAlarmColorPref.setOnPreferenceChangeListener(this);
            }

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

            if (boldDigitalClockPref != null && clockStylePref != null) {
                if (!clockStylePref.getValue().equals(mDigitalClock)) {
                    boldDigitalClockPref.setEnabled(false);
                    boldDigitalClockPref.setSummary(R.string.screensaver_digital_clock_not_selected);
                }

                boldDigitalClockPref.setChecked(DataModel.getDataModel().getScreensaverBoldDigitalClock());
            }

            if (boldDatePref != null) {
                boldDatePref.setChecked(DataModel.getDataModel().getScreensaverBoldDate());
            }

            if (boldNextAlarmPref != null) {
                if (Utils.getNextAlarm(requireActivity()) == null) {
                    boldNextAlarmPref.setEnabled(false);
                    boldNextAlarmPref.setSummary(R.string.screensaver_no_alarm_set);
                }

                boldNextAlarmPref.setChecked(DataModel.getDataModel().getScreensaverBoldNextAlarm());
            }

            if (screensaverPreview != null) {
                screensaverPreview.setOnPreferenceClickListener(this);
            }
        }
    }
}
