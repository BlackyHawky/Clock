// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.DeskClock.REQUEST_CHANGE_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DISPLAY_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_NOTIFICATION_REMINDER_TIME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SNOOZE_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_VOLUME_SETTING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_ROUTING_TO_BLUETOOTH_DEVICE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_SILENCE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DEFAULT_ALARM_RINGTONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_FLIP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_DATE_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_TIME_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_POWER_BUTTON;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VOLUME_BUTTONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_WEEK_START;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.Utils;

import java.util.List;

public class AlarmSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    Preference mAlarmRingtonePref;
    ListPreference mAutoSilencePref;
    ListPreference mAlarmSnoozePref;
    AlarmVolumePreference mAlarmVolumePreference;
    ListPreference mAlarmCrescendoPref;
    SwitchPreferenceCompat mAutoRoutingToBluetoothDevice;
    ListPreference mVolumeButtonsPref;
    ListPreference mPowerButtonPref;
    ListPreference mFlipActionPref;
    ListPreference mShakeActionPref;
    CustomSeekbarPreference mShakeIntensityPref;
    ListPreference mWeekStartPref;
    ListPreference mAlarmNotificationReminderTimePref;
    SwitchPreferenceCompat mEnableAlarmVibrationsByDefaultPref;
    SwitchPreferenceCompat mEnableSnoozedOrDismissedAlarmVibrationsPref;
    SwitchPreferenceCompat mTurnOnBackFlashForTriggeredAlarmPref;
    SwitchPreferenceCompat mDeleteOccasionalAlarmByDefaultPref;
    ListPreference mMaterialTimePickerStylePref;
    ListPreference mMaterialDatePickerStylePref;
    Preference mAlarmDisplayCustomizationPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.alarm_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_alarm);

        mAlarmRingtonePref = findPreference(KEY_DEFAULT_ALARM_RINGTONE);
        mAutoSilencePref = findPreference(KEY_AUTO_SILENCE);
        mAlarmSnoozePref = findPreference(KEY_ALARM_SNOOZE_DURATION);
        mAlarmVolumePreference = findPreference(KEY_ALARM_VOLUME_SETTING);
        mAlarmCrescendoPref = findPreference(KEY_ALARM_CRESCENDO_DURATION);
        mAutoRoutingToBluetoothDevice = findPreference(KEY_AUTO_ROUTING_TO_BLUETOOTH_DEVICE);
        mVolumeButtonsPref = findPreference(KEY_VOLUME_BUTTONS);
        mPowerButtonPref = findPreference(KEY_POWER_BUTTON);
        mFlipActionPref = findPreference(KEY_FLIP_ACTION);
        mShakeActionPref = findPreference(KEY_SHAKE_ACTION);
        mShakeIntensityPref = findPreference(KEY_SHAKE_INTENSITY);
        mWeekStartPref = findPreference(KEY_WEEK_START);
        mAlarmNotificationReminderTimePref = findPreference(KEY_ALARM_NOTIFICATION_REMINDER_TIME);
        mEnableAlarmVibrationsByDefaultPref = findPreference(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT);
        mEnableSnoozedOrDismissedAlarmVibrationsPref = findPreference(KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS);
        mTurnOnBackFlashForTriggeredAlarmPref = findPreference(KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM);
        mDeleteOccasionalAlarmByDefaultPref = findPreference(KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT);
        mMaterialTimePickerStylePref = findPreference(KEY_MATERIAL_TIME_PICKER_STYLE);
        mMaterialDatePickerStylePref = findPreference(KEY_MATERIAL_DATE_PICKER_STYLE);
        mAlarmDisplayCustomizationPref = findPreference(KEY_ALARM_DISPLAY_CUSTOMIZATION);

        setupPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();

        mAlarmRingtonePref.setSummary(DataModel.getDataModel().getAlarmRingtoneTitle());
    }

    @Override
    public void onStop() {
        super.onStop();

        mAlarmVolumePreference.stopRingtonePreview(requireContext());
        mAlarmVolumePreference.stopListeningToPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_AUTO_SILENCE -> {
                final String delay = (String) newValue;
                updateAutoSnoozeSummary((ListPreference) pref, delay);
            }

            case KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS,
                 KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM, KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT,
                 KEY_AUTO_ROUTING_TO_BLUETOOTH_DEVICE ->
                    Utils.setVibrationTime(requireContext(), 50);

            case KEY_ALARM_SNOOZE_DURATION, KEY_ALARM_CRESCENDO_DURATION, KEY_VOLUME_BUTTONS,
                 KEY_POWER_BUTTON, KEY_FLIP_ACTION, KEY_MATERIAL_TIME_PICKER_STYLE,
                 KEY_MATERIAL_DATE_PICKER_STYLE -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_ALARM_NOTIFICATION_REMINDER_TIME -> {
                final int index = mAlarmNotificationReminderTimePref.findIndexOfValue((String) newValue);
                mAlarmNotificationReminderTimePref.setSummary(mAlarmNotificationReminderTimePref.getEntries()[index]);

                ContentResolver cr = requireContext().getContentResolver();
                AlarmUpdateHandler alarmUpdateHandler = new AlarmUpdateHandler(requireContext(), null, null);
                List<Alarm> alarms = Alarm.getAlarms(cr, null);

                for (Alarm alarm : alarms) {
                    if (alarm.enabled) {
                        alarmUpdateHandler.asyncUpdateAlarm(alarm, false, false);
                    }
                }
            }

            case KEY_SHAKE_ACTION -> {
                final int index = mShakeActionPref.findIndexOfValue((String) newValue);
                mShakeActionPref.setSummary(mShakeActionPref.getEntries()[index]);
                // index == 2 --> Nothing
                mShakeIntensityPref.setVisible(index != 2);
            }

            case KEY_WEEK_START -> {
                final int index = mWeekStartPref.findIndexOfValue((String) newValue);
                mWeekStartPref.setSummary(mWeekStartPref.getEntries()[index]);
                // Set result so DeskClock knows to refresh itself
                requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
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

        switch (pref.getKey()) {
            case KEY_DEFAULT_ALARM_RINGTONE ->
                    startActivity(RingtonePickerActivity.createAlarmRingtonePickerIntentForSettings(context));

            case KEY_ALARM_DISPLAY_CUSTOMIZATION -> animateAndShowFragment(new AlarmDisplayCustomizationFragment());
        }

        return true;
    }

    private void setupPreferences() {
        mAlarmRingtonePref.setOnPreferenceClickListener(this);

        String delay = mAutoSilencePref.getValue();
        updateAutoSnoozeSummary(mAutoSilencePref, delay);
        mAutoSilencePref.setOnPreferenceChangeListener(this);

        mAlarmSnoozePref.setOnPreferenceChangeListener(this);
        mAlarmSnoozePref.setSummary(mAlarmSnoozePref.getEntry());

        mAlarmCrescendoPref.setOnPreferenceChangeListener(this);
        mAlarmCrescendoPref.setSummary(mAlarmCrescendoPref.getEntry());

        mAutoRoutingToBluetoothDevice.setOnPreferenceChangeListener(this);

        mVolumeButtonsPref.setOnPreferenceChangeListener(this);
        mVolumeButtonsPref.setSummary(mVolumeButtonsPref.getEntry());

        mPowerButtonPref.setOnPreferenceChangeListener(this);
        mPowerButtonPref.setSummary(mPowerButtonPref.getEntry());

        mEnableAlarmVibrationsByDefaultPref.setVisible(Utils.hasVibrator(requireContext()));
        mEnableSnoozedOrDismissedAlarmVibrationsPref.setVisible(Utils.hasVibrator(requireContext()));

        SensorManager sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            mFlipActionPref.setValue("0");
            mShakeActionPref.setValue("0");
            mFlipActionPref.setVisible(false);
            mShakeActionPref.setVisible(false);
        } else {
            mFlipActionPref.setSummary(mFlipActionPref.getEntry());
            mFlipActionPref.setOnPreferenceChangeListener(this);
            mShakeActionPref.setSummary(mShakeActionPref.getEntry());
            mShakeActionPref.setOnPreferenceChangeListener(this);

            // shakeActionIndex == 2 --> Nothing
            final int shakeActionIndex = mShakeActionPref.findIndexOfValue(String.valueOf(SettingsDAO.getShakeAction(mPrefs)));
            mShakeIntensityPref.setVisible(shakeActionIndex != 2);
        }

        // Set the default first day of the week programmatically
        final Weekdays.Order weekdayOrder = SettingsDAO.getWeekdayOrder(mPrefs);
        final Integer firstDay = weekdayOrder.getCalendarDays().get(0);
        final String value = String.valueOf(firstDay);
        final int index = mWeekStartPref.findIndexOfValue(value);
        mWeekStartPref.setValueIndex(index);
        mWeekStartPref.setSummary(mWeekStartPref.getEntries()[index]);
        mWeekStartPref.setOnPreferenceChangeListener(this);

        mAlarmNotificationReminderTimePref.setOnPreferenceChangeListener(this);
        mAlarmNotificationReminderTimePref.setSummary(mAlarmNotificationReminderTimePref.getEntry());

        mEnableAlarmVibrationsByDefaultPref.setOnPreferenceChangeListener(this);

        mEnableSnoozedOrDismissedAlarmVibrationsPref.setOnPreferenceChangeListener(this);

        mTurnOnBackFlashForTriggeredAlarmPref.setVisible(AlarmUtils.hasBackFlash(requireContext()));
        mTurnOnBackFlashForTriggeredAlarmPref.setOnPreferenceChangeListener(this);

        mDeleteOccasionalAlarmByDefaultPref.setOnPreferenceChangeListener(this);

        mMaterialTimePickerStylePref.setOnPreferenceChangeListener(this);
        mMaterialTimePickerStylePref.setSummary(mMaterialTimePickerStylePref.getEntry());

        mMaterialDatePickerStylePref.setOnPreferenceChangeListener(this);
        mMaterialDatePickerStylePref.setSummary(mMaterialDatePickerStylePref.getEntry());

        mAlarmDisplayCustomizationPref.setOnPreferenceClickListener(this);
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

}
