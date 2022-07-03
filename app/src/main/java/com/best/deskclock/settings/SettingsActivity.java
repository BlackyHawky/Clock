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
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.ListPreferenceDialogFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.best.deskclock.BaseActivity;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.best.deskclock.actionbarmenu.NavUpMenuItemController;
import com.best.deskclock.actionbarmenu.OptionsMenuManager;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.DataModel.ThemeButtonBehavior;
import com.best.deskclock.data.TimeZones;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.ringtone.RingtonePickerActivity;

import java.util.List;
import java.util.Objects;

/**
 * Settings for the Alarm Clock.
 */
public final class SettingsActivity extends BaseActivity {
    public static final String KEY_THEME = "key_theme";
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
    public static final String KEY_VOLUME_BUTTONS = "volume_button_setting";
    public static final String KEY_POWER_BUTTONS = "power_button";
    public static final String KEY_WEEK_START = "week_start";
    public static final String KEY_FLIP_ACTION = "flip_action";
    public static final String KEY_SHAKE_ACTION = "shake_action";
    public static final String DEFAULT_VOLUME_BEHAVIOR = "0";
    public static final String VOLUME_BEHAVIOR_SNOOZE = "1";
    public static final String VOLUME_BEHAVIOR_DISMISS = "2";
    public static final String SYSTEM_THEME_BEHAVIOR = "0";
    public static final String THEME_BEHAVIOR_DARK = "1";
    public static final String THEME_BEHAVIOR_LIGHT = "2";
    public static final String DEFAULT_POWER_BEHAVIOR = "0";
    public static final String POWER_BEHAVIOR_SNOOZE = "1";
    public static final String POWER_BEHAVIOR_DISMISS = "2";
    public static final String PREFS_FRAGMENT_TAG = "prefs_fragment";
    public static final String PREFERENCE_DIALOG_FRAGMENT_TAG = "preference_dialog";
    private final OptionsMenuManager mOptionsMenuManager = new OptionsMenuManager();
    private ThemeButtonBehavior mThemeBehavior;

    /**
     * The controller that shows the drop shadow when content is not scrolled to the top.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeBehavior = DataModel.getDataModel().getThemeButtonBehavior();
        if (mThemeBehavior == DataModel.ThemeButtonBehavior.DARK) {
            getTheme().applyStyle(R.style.Theme_DeskClock_Actionbar_Dark, true);
        }
        if (mThemeBehavior == DataModel.ThemeButtonBehavior.LIGHT) {
            getTheme().applyStyle(R.style.Theme_DeskClock_Actionbar_Light, true);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);

        mOptionsMenuManager.addMenuItemController(new NavUpMenuItemController(this))
                .addMenuItemController(MenuItemControllerFactory.getInstance()
                        .buildMenuItemControllers(this));

        // Create the prefs fragment in code to ensure it's created before PreferenceDialogFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new PrefsFragment(), PREFS_FRAGMENT_TAG)
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
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mOptionsMenuManager.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mOptionsMenuManager.onOptionsItemSelected(item)
                || super.onOptionsItemSelected(item);
    }

    public static class PrefsFragment extends PreferenceFragmentCompat implements
            Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
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
                case KEY_THEME:
                case KEY_ALARM_CRESCENDO:
                case KEY_HOME_TZ:
                case KEY_ALARM_SNOOZE:
                case KEY_TIMER_CRESCENDO:
                    final ListPreference preference = (ListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                    break;
                case KEY_CLOCK_STYLE:
                case KEY_WEEK_START:
                case KEY_VOLUME_BUTTONS:
                case KEY_FLIP_ACTION:
                case KEY_POWER_BUTTONS:
                case KEY_SHAKE_ACTION:
                    final DropDownPreference simpleMenuPreference = (DropDownPreference) pref;
                    final int i = simpleMenuPreference.findIndexOfValue((String) newValue);
                    pref.setSummary(simpleMenuPreference.getEntries()[i]);
                    break;
                case KEY_CLOCK_DISPLAY_SECONDS:
                    DataModel.getDataModel().setDisplayClockSeconds((boolean) newValue);
                    break;
                case KEY_AUTO_SILENCE:
                    final String delay = (String) newValue;
                    updateAutoSnoozeSummary((ListPreference) pref, delay);
                    break;
                case KEY_AUTO_HOME_CLOCK:
                    final boolean autoHomeClockEnabled = ((TwoStatePreference) pref).isChecked();
                    final Preference homeTimeZonePref = findPreference(KEY_HOME_TZ);
                    Objects.requireNonNull(homeTimeZonePref).setEnabled(!autoHomeClockEnabled);
                    break;
                case KEY_TIMER_VIBRATE:
                    final TwoStatePreference timerVibratePref = (TwoStatePreference) pref;
                    DataModel.getDataModel().setTimerVibrate(timerVibratePref.isChecked());
                    break;
                case KEY_TIMER_RINGTONE:
                    pref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());
                    break;
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
                case KEY_DATE_TIME:
                    final Intent dialogIntent = new Intent(Settings.ACTION_DATE_SETTINGS);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogIntent);
                    return true;
                case KEY_TIMER_RINGTONE:
                    startActivity(RingtonePickerActivity.createTimerRingtonePickerIntent(context));
                    return true;
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
            if (requireFragmentManager().findFragmentByTag(PREFERENCE_DIALOG_FRAGMENT_TAG) != null) {
                return;
            }
            // Always set the target fragment, this is required by PreferenceDialogFragment
            // internally.
            fragment.setTargetFragment(this, 0);
            // Don't use getChildFragmentManager(), it causes issues on older platforms when the
            // target fragment is being restored after an orientation change.
            fragment.show(getFragmentManager(), PREFERENCE_DIALOG_FRAGMENT_TAG);
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
            final ListPreference autoSilencePref =
                    findPreference(KEY_AUTO_SILENCE);
            String delay = Objects.requireNonNull(autoSilencePref).getValue();
            updateAutoSnoozeSummary(autoSilencePref, delay);
            autoSilencePref.setOnPreferenceChangeListener(this);

            final DropDownPreference clockStylePref = findPreference(KEY_CLOCK_STYLE);
            Objects.requireNonNull(clockStylePref).setSummary(clockStylePref.getEntry());
            clockStylePref.setOnPreferenceChangeListener(this);

            final DropDownPreference volumeButtonsPref = findPreference(KEY_VOLUME_BUTTONS);
            Objects.requireNonNull(volumeButtonsPref).setSummary(volumeButtonsPref.getEntry());
            volumeButtonsPref.setOnPreferenceChangeListener(this);

            final DropDownPreference themeButtonsPref = findPreference(KEY_THEME);
            Objects.requireNonNull(themeButtonsPref).setSummary(themeButtonsPref.getEntry());
            themeButtonsPref.setOnPreferenceChangeListener(this);

            final DropDownPreference powerButtonsPref = findPreference(KEY_POWER_BUTTONS);
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

            final DropDownPreference weekStartPref = findPreference(KEY_WEEK_START);
            // Set the default value programmatically
            final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
            final Integer firstDay = weekdayOrder.getCalendarDays().get(0);
            final String value = String.valueOf(firstDay);
            final int idx = Objects.requireNonNull(weekStartPref).findIndexOfValue(value);
            weekStartPref.setValueIndex(idx);
            weekStartPref.setSummary(weekStartPref.getEntries()[idx]);
            weekStartPref.setOnPreferenceChangeListener(this);

            final Preference timerRingtonePref = findPreference(KEY_TIMER_RINGTONE);
            Objects.requireNonNull(timerRingtonePref).setOnPreferenceClickListener(this);
            timerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());

            SensorManager sensorManager = (SensorManager)
                    requireActivity().getSystemService(Context.SENSOR_SERVICE);

            final DropDownPreference flipActionPref =
                    findPreference(KEY_FLIP_ACTION);
            if (flipActionPref != null) {
                List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
                if (sensorList.size() < 1) { // This will be true if no orientation sensor
                    flipActionPref.setValue("0"); // Turn it off
                } else {
                    flipActionPref.setSummary(flipActionPref.getEntry());
                    flipActionPref.setOnPreferenceChangeListener(this);
                }
            }

            final DropDownPreference shakeActionPref =
                    findPreference(KEY_SHAKE_ACTION);
            if (shakeActionPref != null) {
                List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
                if (sensorList.size() < 1) { // This will be true if no accelerometer sensor
                    shakeActionPref.setValue("0"); // Turn it off
                } else {
                    shakeActionPref.setSummary(shakeActionPref.getEntry());
                    shakeActionPref.setOnPreferenceChangeListener(this);
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
