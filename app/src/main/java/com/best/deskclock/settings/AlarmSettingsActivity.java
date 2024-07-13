// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

import java.util.Objects;

public class AlarmSettingsActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "alarm_settings_fragment";

    public static final String KEY_DEFAULT_ALARM_RINGTONE = "default_alarm_ringtone";
    public static final String KEY_AUTO_SILENCE = "auto_silence";
    public static final String KEY_ALARM_SNOOZE = "snooze_duration";
    public static final String KEY_ALARM_CRESCENDO = "alarm_crescendo_duration";
    public static final String KEY_SWIPE_ACTION = "key_swipe_action";
    public static final String KEY_VOLUME_BUTTONS = "volume_button_setting";
    public static final String DEFAULT_VOLUME_BEHAVIOR = "0";
    public static final String VOLUME_BEHAVIOR_SNOOZE = "1";
    public static final String VOLUME_BEHAVIOR_DISMISS = "2";
    public static final String KEY_POWER_BUTTONS = "power_button";
    public static final String DEFAULT_POWER_BEHAVIOR = "0";
    public static final String POWER_BEHAVIOR_SNOOZE = "1";
    public static final String POWER_BEHAVIOR_DISMISS = "2";
    public static final String KEY_FLIP_ACTION = "flip_action";
    public static final String KEY_SHAKE_ACTION = "shake_action";
    public static final String KEY_WEEK_START = "week_start";
    public static final String KEY_ALARM_NOTIFICATION_REMINDER_TIME = "key_alarm_notification_reminder_time";
    public static final String KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT = "key_enable_alarm_vibrations_by_default";
    public static final String KEY_MATERIAL_TIME_PICKER_STYLE = "key_material_time_picker_style";
    public static final String MATERIAL_TIME_PICKER_ANALOG_STYLE = "analog";

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

    public static class PrefsFragment extends PreferenceFragmentCompat implements
            Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        SwitchPreferenceCompat mEnableAlarmVibrationsByDefault;

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getPreferenceManager().setStorageDeviceProtected();
            }
            addPreferencesFromResource(R.xml.settings_alarm);
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
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_DEFAULT_ALARM_RINGTONE ->
                        pref.setSummary(DataModel.getDataModel().getAlarmRingtoneTitle());

                case KEY_AUTO_SILENCE -> {
                    final String delay = (String) newValue;
                    updateAutoSnoozeSummary((ListPreference) pref, delay);
                }

                case KEY_SWIPE_ACTION, KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT ->
                        Utils.setVibrationTime(requireContext(), 50);

                case KEY_ALARM_SNOOZE, KEY_ALARM_CRESCENDO, KEY_VOLUME_BUTTONS,
                        KEY_POWER_BUTTONS, KEY_FLIP_ACTION, KEY_SHAKE_ACTION,
                        KEY_ALARM_NOTIFICATION_REMINDER_TIME, KEY_MATERIAL_TIME_PICKER_STYLE -> {
                    final ListPreference preference = (ListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                }

                case KEY_WEEK_START -> {
                    final ListPreference preference = (ListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                    // Set result so DeskClock knows to refresh itself
                    requireActivity().setResult(RESULT_OK);
                }
            }

            return true;
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference pref) {
            final Context context = getActivity();
            if (context == null) {
                return false;
            }

            if (pref.getKey().equals(KEY_DEFAULT_ALARM_RINGTONE)) {
                startActivity(RingtonePickerActivity.createAlarmRingtonePickerIntentForSettings(context));
                return true;
            }

            return false;
        }

        private void hidePreferences() {
            mEnableAlarmVibrationsByDefault = findPreference(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT);
            assert mEnableAlarmVibrationsByDefault != null;
            final boolean hasVibrator = ((Vibrator) mEnableAlarmVibrationsByDefault.getContext()
                    .getSystemService(VIBRATOR_SERVICE)).hasVibrator();
            mEnableAlarmVibrationsByDefault.setVisible(hasVibrator);
        }

        private void refresh() {
            final Preference alarmRingtonePref = findPreference(KEY_DEFAULT_ALARM_RINGTONE);
            Objects.requireNonNull(alarmRingtonePref).setOnPreferenceClickListener(this);
            alarmRingtonePref.setSummary(DataModel.getDataModel().getAlarmRingtoneTitle());

            final ListPreference autoSilencePref = findPreference(KEY_AUTO_SILENCE);
            String delay = Objects.requireNonNull(autoSilencePref).getValue();
            updateAutoSnoozeSummary(autoSilencePref, delay);
            autoSilencePref.setOnPreferenceChangeListener(this);

            refreshListPreference(Objects.requireNonNull(findPreference(KEY_ALARM_SNOOZE)));

            refreshListPreference(Objects.requireNonNull(findPreference(KEY_ALARM_CRESCENDO)));

            final SwitchPreferenceCompat swipeActionPref = findPreference(KEY_SWIPE_ACTION);
            Objects.requireNonNull(swipeActionPref).setChecked(DataModel.getDataModel().isSwipeActionEnabled());
            swipeActionPref.setOnPreferenceChangeListener(this);

            final ListPreference volumeButtonsPref = findPreference(KEY_VOLUME_BUTTONS);
            Objects.requireNonNull(volumeButtonsPref).setSummary(volumeButtonsPref.getEntry());
            volumeButtonsPref.setOnPreferenceChangeListener(this);

            final ListPreference powerButtonsPref = findPreference(KEY_POWER_BUTTONS);
            Objects.requireNonNull(powerButtonsPref).setSummary(powerButtonsPref.getEntry());
            powerButtonsPref.setOnPreferenceChangeListener(this);

            final ListPreference flipActionPref = findPreference(KEY_FLIP_ACTION);
            setupFlipOrShakeAction(flipActionPref);

            final ListPreference shakeActionPref = findPreference(KEY_SHAKE_ACTION);
            setupFlipOrShakeAction(shakeActionPref);

            final ListPreference weekStartPref = findPreference(KEY_WEEK_START);
            // Set the default value programmatically
            final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
            final Integer firstDay = weekdayOrder.getCalendarDays().get(0);
            final String value = String.valueOf(firstDay);
            final int index = Objects.requireNonNull(weekStartPref).findIndexOfValue(value);
            weekStartPref.setValueIndex(index);
            weekStartPref.setSummary(weekStartPref.getEntries()[index]);
            weekStartPref.setOnPreferenceChangeListener(this);

            refreshListPreference(Objects.requireNonNull(findPreference(KEY_ALARM_NOTIFICATION_REMINDER_TIME)));

            mEnableAlarmVibrationsByDefault.setChecked(DataModel.getDataModel().areAlarmVibrationsEnabledByDefault());
            mEnableAlarmVibrationsByDefault.setOnPreferenceChangeListener(this);

            refreshListPreference(Objects.requireNonNull(findPreference(KEY_MATERIAL_TIME_PICKER_STYLE)));
        }

        private void updateAutoSnoozeSummary(ListPreference listPref, String delay) {
            int i = Integer.parseInt(delay);
            if (i == -1) {
                listPref.setSummary(R.string.auto_silence_never);
            } else if (i == -2) {
                listPref.setSummary(R.string.auto_silence_at_the_end_of_the_ringtone);
            } else {
                listPref.setSummary(Utils.getNumberFormattedQuantityString(requireActivity(),
                        R.plurals.auto_silence_summary, i));
            }
        }

        private void refreshListPreference(ListPreference preference) {
            preference.setSummary(preference.getEntry());
            preference.setOnPreferenceChangeListener(this);
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
    }
}
