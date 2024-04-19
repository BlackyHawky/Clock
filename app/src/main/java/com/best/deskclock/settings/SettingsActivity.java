/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.ListPreferenceDialogFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.TimeZones;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

import java.util.Objects;

/**
 * Settings for the Alarm Clock.
 */
public final class SettingsActivity extends CollapsingToolbarBaseActivity {
    public static final String KEY_THEME = "key_theme";
    public static final String SYSTEM_THEME = "0";
    public static final String LIGHT_THEME = "1";
    public static final String DARK_THEME = "2";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_DEFAULT_DARK_MODE = "default";
    public static final String KEY_AMOLED_DARK_MODE = "amoled";
    public static final String KEY_DEFAULT_ALARM_RINGTONE = "default_alarm_ringtone";
    public static final String KEY_ALARM_SNOOZE = "snooze_duration";
    public static final String KEY_ALARM_CRESCENDO = "alarm_crescendo_duration";
    public static final String KEY_TIMER_CRESCENDO = "timer_crescendo_duration";
    public static final String KEY_TIMER_RINGTONE = "timer_ringtone";
    public static final String KEY_TIMER_VIBRATE = "timer_vibrate";
    public static final String KEY_AUTO_SILENCE = "auto_silence";
    public static final String KEY_CLOCK_STYLE = "clock_style";
    public static final String KEY_CLOCK_DISPLAY_SECONDS = "display_clock_seconds";
    public static final String KEY_HOME_TZ = "home_time_zone";
    public static final String KEY_AUTO_HOME_CLOCK = "automatic_home_clock";
    public static final String KEY_DATE_TIME = "date_time";
    public static final String KEY_SS_SETTINGS = "screensaver_settings";
    public static final String KEY_VOLUME_BUTTONS = "volume_button_setting";
    public static final String KEY_POWER_BUTTONS = "power_button";
    public static final String KEY_WEEK_START = "week_start";
    public static final String KEY_FLIP_ACTION = "flip_action";
    public static final String KEY_SHAKE_ACTION = "shake_action";
    public static final String DEFAULT_VOLUME_BEHAVIOR = "0";
    public static final String VOLUME_BEHAVIOR_SNOOZE = "1";
    public static final String VOLUME_BEHAVIOR_DISMISS = "2";
    public static final String DEFAULT_POWER_BEHAVIOR = "0";
    public static final String POWER_BEHAVIOR_SNOOZE = "1";
    public static final String POWER_BEHAVIOR_DISMISS = "2";
    public static final String KEY_PERMISSIONS_MANAGEMENT = "permissions_management";
    public static final String PREFS_FRAGMENT_TAG = "prefs_fragment";
    public static final String PREFERENCE_DIALOG_FRAGMENT_TAG = "preference_dialog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the prefs fragment in code to ensure it's created before PreferenceDialogFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PrefsFragment(), PREFS_FRAGMENT_TAG)
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Menu.NONE, 0, R.string.about_title)
                .setIcon(R.drawable.ic_about).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            final Intent settingIntent = new Intent(getApplicationContext(), AboutActivity.class);
            startActivity(settingIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PrefsFragment extends PreferenceFragmentCompat implements
            Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getPreferenceManager().setStorageDeviceProtected();
            }
            addPreferencesFromResource(R.xml.settings);
            final Preference timerVibrate = findPreference(KEY_TIMER_VIBRATE);
            final boolean hasVibrator = ((Vibrator) Objects.requireNonNull(timerVibrate).getContext()
                    .getSystemService(VIBRATOR_SERVICE)).hasVibrator();
            timerVibrate.setVisible(hasVibrator);
            loadTimeZoneList();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // By default, do not recreate the DeskClock activity
            requireActivity().setResult(RESULT_CANCELED);
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
                        switch (darkModeIndex) {
                            case 0 -> DarkModeController.applyDarkMode(DarkModeController.DarkMode.DEFAULT_DARK_MODE);
                            case 1 -> DarkModeController.applyDarkMode(DarkModeController.DarkMode.AMOLED);
                        }
                    }
                }
                case KEY_CLOCK_STYLE, KEY_ALARM_CRESCENDO, KEY_HOME_TZ, KEY_ALARM_SNOOZE,
                        KEY_TIMER_CRESCENDO, KEY_VOLUME_BUTTONS, KEY_POWER_BUTTONS, KEY_FLIP_ACTION,
                        KEY_SHAKE_ACTION, KEY_WEEK_START -> {
                    final ListPreference preference = (ListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                }
                case KEY_CLOCK_DISPLAY_SECONDS -> DataModel.getDataModel().setDisplayClockSeconds((boolean) newValue);
                case KEY_AUTO_SILENCE -> {
                    final String delay = (String) newValue;
                    updateAutoSnoozeSummary((ListPreference) pref, delay);
                }
                case KEY_AUTO_HOME_CLOCK -> {
                    final boolean autoHomeClockEnabled = ((TwoStatePreference) pref).isChecked();
                    final Preference homeTimeZonePref = findPreference(KEY_HOME_TZ);
                    Objects.requireNonNull(homeTimeZonePref).setEnabled(!autoHomeClockEnabled);
                }
                case KEY_TIMER_VIBRATE -> {
                    final TwoStatePreference timerVibratePref = (TwoStatePreference) pref;
                    DataModel.getDataModel().setTimerVibrate(timerVibratePref.isChecked());
                }
                case KEY_DEFAULT_ALARM_RINGTONE -> pref.setSummary(DataModel.getDataModel().getAlarmRingtoneTitle());
                case KEY_TIMER_RINGTONE -> pref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());
            }
            // Set result so DeskClock knows to refresh itself
            requireActivity().setResult(RESULT_OK);
            return true;
        }


        @Override
        public boolean onPreferenceClick(@NonNull Preference pref) {
            final Context context = getActivity();
            if (context == null) {
                return false;
            }

            switch (pref.getKey()) {
                case KEY_SS_SETTINGS -> {
                    final Intent screensaverSettingsIntent = new Intent(context, ScreensaverSettingsActivity.class);
                    startActivity(screensaverSettingsIntent);
                    return true;
                }
                case KEY_DATE_TIME -> {
                    final Intent dialogIntent = new Intent(Settings.ACTION_DATE_SETTINGS);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogIntent);
                    return true;
                }
                case KEY_DEFAULT_ALARM_RINGTONE -> {
                    startActivity(RingtonePickerActivity.createAlarmRingtonePickerIntentForSettings(context));
                    return true;
                }
                case KEY_TIMER_RINGTONE -> {
                    startActivity(RingtonePickerActivity.createTimerRingtonePickerIntent(context));
                    return true;
                }
                case KEY_PERMISSIONS_MANAGEMENT -> {
                    final Intent permissionsManagementIntent = new Intent(context, PermissionsManagementActivity.class);
                    startActivity(permissionsManagementIntent);
                    requireActivity().setResult(RESULT_OK);
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            // Only single-selection lists are currently supported.
            final PreferenceDialogFragmentCompat f;
            if (preference instanceof ListPreference) {
                f = ListPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            } else {
                throw new IllegalArgumentException("Unsupported DialogPreference type");
            }
            showDialog(f);
        }

        private void showDialog(PreferenceDialogFragmentCompat fragment) {
            // Don't show dialog if one is already shown.
            if (getParentFragmentManager().findFragmentByTag(PREFERENCE_DIALOG_FRAGMENT_TAG) != null) {
                return;
            }
            // Always set the target fragment, this is required by PreferenceDialogFragment
            // internally.
            fragment.setTargetFragment(this, 0);
            // Don't use getChildFragmentManager(), it causes issues on older platforms when the
            // target fragment is being restored after an orientation change.
            fragment.show(getParentFragmentManager(), PREFERENCE_DIALOG_FRAGMENT_TAG);
        }

        /**
         * Reconstruct the timezone list.
         */
        private void loadTimeZoneList() {
            final TimeZones timezones = DataModel.getDataModel().getTimeZones();
            final ListPreference homeTimezonePref = findPreference(KEY_HOME_TZ);
            Objects.requireNonNull(homeTimezonePref).setEntryValues(timezones.getTimeZoneIds());
            homeTimezonePref.setEntries(timezones.getTimeZoneNames());
            homeTimezonePref.setSummary(homeTimezonePref.getEntry());
            homeTimezonePref.setOnPreferenceChangeListener(this);
        }

        private void refresh() {
            final ListPreference themePref = findPreference(KEY_THEME);
            Objects.requireNonNull(themePref).setSummary(themePref.getEntry());
            themePref.setOnPreferenceChangeListener(this);

            final ListPreference amoledModePref = findPreference(KEY_DARK_MODE);
            Objects.requireNonNull(amoledModePref).setSummary(amoledModePref.getEntry());
            amoledModePref.setOnPreferenceChangeListener(this);

            final ListPreference autoSilencePref = findPreference(KEY_AUTO_SILENCE);
            String delay = Objects.requireNonNull(autoSilencePref).getValue();
            updateAutoSnoozeSummary(autoSilencePref, delay);
            autoSilencePref.setOnPreferenceChangeListener(this);

            final ListPreference clockStylePref = findPreference(KEY_CLOCK_STYLE);
            Objects.requireNonNull(clockStylePref).setSummary(clockStylePref.getEntry());
            clockStylePref.setOnPreferenceChangeListener(this);

            final ListPreference volumeButtonsPref = findPreference(KEY_VOLUME_BUTTONS);
            Objects.requireNonNull(volumeButtonsPref).setSummary(volumeButtonsPref.getEntry());
            volumeButtonsPref.setOnPreferenceChangeListener(this);

            final ListPreference powerButtonsPref = findPreference(KEY_POWER_BUTTONS);
            Objects.requireNonNull(powerButtonsPref).setSummary(powerButtonsPref.getEntry());
            powerButtonsPref.setOnPreferenceChangeListener(this);

            final Preference clockSecondsPref = findPreference(KEY_CLOCK_DISPLAY_SECONDS);
            Objects.requireNonNull(clockSecondsPref).setOnPreferenceChangeListener(this);

            final Preference autoHomeClockPref = findPreference(KEY_AUTO_HOME_CLOCK);
            final boolean autoHomeClockEnabled =
                    ((TwoStatePreference) Objects.requireNonNull(autoHomeClockPref)).isChecked();
            autoHomeClockPref.setOnPreferenceChangeListener(this);

            final ListPreference homeTimezonePref = findPreference(KEY_HOME_TZ);
            Objects.requireNonNull(homeTimezonePref).setEnabled(autoHomeClockEnabled);
            refreshListPreference(homeTimezonePref);

            refreshListPreference(Objects.requireNonNull(findPreference(KEY_ALARM_CRESCENDO)));
            refreshListPreference(Objects.requireNonNull(findPreference(KEY_TIMER_CRESCENDO)));
            refreshListPreference(Objects.requireNonNull(findPreference(KEY_ALARM_SNOOZE)));

            final Preference dateAndTimeSetting = findPreference(KEY_DATE_TIME);
            Objects.requireNonNull(dateAndTimeSetting).setOnPreferenceClickListener(this);

            final Preference screensaverSettings = findPreference(KEY_SS_SETTINGS);
            Objects.requireNonNull(screensaverSettings).setOnPreferenceClickListener(this);

            final ListPreference weekStartPref = findPreference(KEY_WEEK_START);
            // Set the default value programmatically
            final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
            final Integer firstDay = weekdayOrder.getCalendarDays().get(0);
            final String value = String.valueOf(firstDay);
            final int idx = Objects.requireNonNull(weekStartPref).findIndexOfValue(value);
            weekStartPref.setValueIndex(idx);
            weekStartPref.setSummary(weekStartPref.getEntries()[idx]);
            weekStartPref.setOnPreferenceChangeListener(this);

            final Preference alarmRingtonePref = findPreference(KEY_DEFAULT_ALARM_RINGTONE);
            Objects.requireNonNull(alarmRingtonePref).setOnPreferenceClickListener(this);
            alarmRingtonePref.setSummary(DataModel.getDataModel().getAlarmRingtoneTitle());

            final Preference timerRingtonePref = findPreference(KEY_TIMER_RINGTONE);
            Objects.requireNonNull(timerRingtonePref).setOnPreferenceClickListener(this);
            timerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());

            final ListPreference flipActionPref = findPreference(KEY_FLIP_ACTION);
            setupFlipOrShakeAction(flipActionPref);

            final ListPreference shakeActionPref = findPreference(KEY_SHAKE_ACTION);
            setupFlipOrShakeAction(shakeActionPref);

            final Preference permissionsManagement = findPreference(KEY_PERMISSIONS_MANAGEMENT);
            Objects.requireNonNull(permissionsManagement).setOnPreferenceClickListener(this);
        }

        private void setupFlipOrShakeAction(ListPreference preference) {
            if (preference != null) {
                SensorManager sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
                if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
                    preference.setValue("0");  // Turn it off
                    preference.setVisible(false);
                } else {
                    preference.setSummary(preference.getEntry());
                    preference.setOnPreferenceChangeListener(this);
                }
            }
        }

        private void refreshListPreference(ListPreference preference) {
            preference.setSummary(preference.getEntry());
            preference.setOnPreferenceChangeListener(this);
        }

        private void updateAutoSnoozeSummary(ListPreference listPref, String delay) {
            int i = Integer.parseInt(delay);
            if (i == -1) {
                listPref.setSummary(R.string.auto_silence_never);
            } else {
                listPref.setSummary(Utils.getNumberFormattedQuantityString(requireActivity(),
                        R.plurals.auto_silence_summary, i));
            }
        }
    }
}
