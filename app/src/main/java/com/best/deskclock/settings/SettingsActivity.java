/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
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
 * Application settings
 */
public final class SettingsActivity extends CollapsingToolbarBaseActivity {

    public static final String PREFS_FRAGMENT_TAG = "settings_prefs_fragment";

    public static final String KEY_PERMISSION_MESSAGE = "key_permission_message";
    public static final String KEY_INTERFACE_CUSTOMIZATION = "key_interface_customization";
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
    public static final String KEY_DIGITAL_WIDGET_CUSTOMIZATION =
            "key_digital_widget_customization";
    public static final String KEY_DIGITAL_WIDGET_MATERIAL_YOU_CUSTOMIZATION =
            "key_digital_widget_material_you_customization";
    public static final String KEY_PERMISSIONS_MANAGEMENT = "permissions_management";

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

        Preference mPermissionMessage;
        Preference mTimerVibrate;
        ListPreference mHomeTimeZonePref;

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getPreferenceManager().setStorageDeviceProtected();
            }
            addPreferencesFromResource(R.xml.settings);
            hidePreferences();
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
                case KEY_CLOCK_STYLE, KEY_ALARM_CRESCENDO, KEY_HOME_TZ, KEY_ALARM_SNOOZE,
                        KEY_TIMER_CRESCENDO, KEY_VOLUME_BUTTONS, KEY_POWER_BUTTONS, KEY_FLIP_ACTION,
                        KEY_SHAKE_ACTION, KEY_WEEK_START -> {
                    final ListPreference preference = (ListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                }

                case KEY_CLOCK_DISPLAY_SECONDS -> {
                    DataModel.getDataModel().setDisplayClockSeconds((boolean) newValue);
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_DEFAULT_ALARM_RINGTONE ->
                        pref.setSummary(DataModel.getDataModel().getAlarmRingtoneTitle());

                case KEY_AUTO_SILENCE -> {
                    final String delay = (String) newValue;
                    updateAutoSnoozeSummary((ListPreference) pref, delay);
                }

                case KEY_AUTO_HOME_CLOCK -> {
                    final boolean autoHomeClockEnabled = ((TwoStatePreference) pref).isChecked();
                    final Preference homeTimeZonePref = findPreference(KEY_HOME_TZ);
                    Objects.requireNonNull(homeTimeZonePref).setEnabled(!autoHomeClockEnabled);
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_TIMER_VIBRATE -> {
                    final TwoStatePreference timerVibratePref = (TwoStatePreference) pref;
                    DataModel.getDataModel().setTimerVibrate(timerVibratePref.isChecked());
                    Utils.setVibrationTime(requireContext(), 50);
                }

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
                case KEY_INTERFACE_CUSTOMIZATION -> {
                    final Intent InterfaceCustomizationIntent =
                            new Intent(context, InterfaceCustomizationActivity.class);
                    startActivity(InterfaceCustomizationIntent);
                    // Set result so DeskClock knows to refresh itself
                    requireActivity().setResult(RESULT_OK);
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

                case KEY_SS_SETTINGS -> {
                    final Intent screensaverSettingsIntent =
                            new Intent(context, ScreensaverSettingsActivity.class);
                    startActivity(screensaverSettingsIntent);
                    return true;
                }

                case KEY_DIGITAL_WIDGET_CUSTOMIZATION -> {
                    final Intent digitalWidgetCustomizationIntent =
                            new Intent(context, DigitalWidgetCustomizationActivity.class);
                    startActivity(digitalWidgetCustomizationIntent);
                    return true;
                }

                case KEY_DIGITAL_WIDGET_MATERIAL_YOU_CUSTOMIZATION -> {
                    final Intent digitalWidgetMaterialYouCustomizationIntent =
                            new Intent(context, DigitalWidgetMaterialYouCustomizationActivity.class);
                    startActivity(digitalWidgetMaterialYouCustomizationIntent);
                    return true;
                }

                case KEY_PERMISSION_MESSAGE, KEY_PERMISSIONS_MANAGEMENT -> {
                    final Intent permissionsManagementIntent =
                            new Intent(context, PermissionsManagementActivity.class);
                    startActivity(permissionsManagementIntent);
                    return true;
                }
            }

            return false;
        }

        private void hidePreferences() {
            mPermissionMessage = findPreference(KEY_PERMISSION_MESSAGE);
            mTimerVibrate = findPreference(KEY_TIMER_VIBRATE);

            assert mTimerVibrate != null;
            final boolean hasVibrator = ((Vibrator) mTimerVibrate.getContext()
                    .getSystemService(VIBRATOR_SERVICE)).hasVibrator();
            mTimerVibrate.setVisible(hasVibrator);

            if (mPermissionMessage != null) {
                mPermissionMessage.setVisible(
                        PermissionsManagementActivity.areEssentialPermissionsNotGranted(requireContext())
                );
            }
        }

        /**
         * Reconstruct the timezone list.
         */
        private void loadTimeZoneList() {
            final TimeZones timezones = DataModel.getDataModel().getTimeZones();
            mHomeTimeZonePref = findPreference(KEY_HOME_TZ);
            assert mHomeTimeZonePref != null;
            mHomeTimeZonePref.setEntryValues(timezones.getTimeZoneIds());
            mHomeTimeZonePref.setEntries(timezones.getTimeZoneNames());
            mHomeTimeZonePref.setSummary(mHomeTimeZonePref.getEntry());
        }

        private void refresh() {
            mPermissionMessage.setVisible(
                    PermissionsManagementActivity.areEssentialPermissionsNotGranted(requireContext())
            );
            if (mPermissionMessage.isShown()) {
                final SpannableStringBuilder builderPermissionMessage = new SpannableStringBuilder();
                final String messagePermission = requireContext().getString(R.string.settings_permission_message);
                final Spannable spannableMessagePermission = new SpannableString(messagePermission);
                spannableMessagePermission.setSpan(
                        new ForegroundColorSpan(Color.RED), 0, messagePermission.length(), 0);
                spannableMessagePermission.setSpan(
                        new StyleSpan(Typeface.BOLD), 0, messagePermission.length(), 0);
                builderPermissionMessage.append(spannableMessagePermission);
                mPermissionMessage.setTitle(builderPermissionMessage);
                mPermissionMessage.setOnPreferenceClickListener(this);
            }

            final Preference interfaceCustomizationPref = findPreference(KEY_INTERFACE_CUSTOMIZATION);
            Objects.requireNonNull(interfaceCustomizationPref).setOnPreferenceClickListener(this);

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

            mHomeTimeZonePref.setEnabled(autoHomeClockEnabled);
            refreshListPreference(mHomeTimeZonePref);

            refreshListPreference(Objects.requireNonNull(findPreference(KEY_ALARM_CRESCENDO)));
            refreshListPreference(Objects.requireNonNull(findPreference(KEY_TIMER_CRESCENDO)));
            refreshListPreference(Objects.requireNonNull(findPreference(KEY_ALARM_SNOOZE)));

            final Preference dateAndTimeSetting = findPreference(KEY_DATE_TIME);
            Objects.requireNonNull(dateAndTimeSetting).setOnPreferenceClickListener(this);

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

            mTimerVibrate.setOnPreferenceChangeListener(this);

            final Preference screensaverSettings = findPreference(KEY_SS_SETTINGS);
            Objects.requireNonNull(screensaverSettings).setOnPreferenceClickListener(this);

            final Preference digitalWidgetCustomizationPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOMIZATION);
            Objects.requireNonNull(digitalWidgetCustomizationPref).setOnPreferenceClickListener(this);

            final Preference digitalWidgetMaterialYouCustomizationPref =
                    findPreference(KEY_DIGITAL_WIDGET_MATERIAL_YOU_CUSTOMIZATION);
            Objects.requireNonNull(digitalWidgetMaterialYouCustomizationPref).setOnPreferenceClickListener(this);

            final Preference permissionsManagement = findPreference(KEY_PERMISSIONS_MANAGEMENT);
            Objects.requireNonNull(permissionsManagement).setOnPreferenceClickListener(this);
        }

        private void setupFlipOrShakeAction(ListPreference preference) {
            if (preference != null) {
                SensorManager sensorManager = (SensorManager) requireActivity()
                        .getSystemService(Context.SENSOR_SERVICE);
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
