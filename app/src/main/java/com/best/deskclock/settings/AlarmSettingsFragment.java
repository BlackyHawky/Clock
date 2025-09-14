// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.DeskClock.REQUEST_CHANGE_SETTINGS;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DISPLAY_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_NOTIFICATION_REMINDER_TIME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_VOLUME_SETTING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_ROUTING_TO_BLUETOOTH_DEVICE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_BLUETOOTH_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DEFAULT_ALARM_RINGTONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_ENABLED_ALARMS_FIRST;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_FLIP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_DATE_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_TIME_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_POWER_BUTTON;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ADVANCED_AUDIO_PLAYBACK;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SORT_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SYSTEM_MEDIA_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VOLUME_BUTTONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_WEEK_START;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.AlarmSnoozeDurationDialogFragment;
import com.best.deskclock.AutoSilenceDurationDialogFragment;
import com.best.deskclock.R;
import com.best.deskclock.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.Utils;

import java.util.List;

public class AlarmSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private AudioManager mAudioManager;
    private AudioDeviceCallback mAudioDeviceCallback;

    Preference mAlarmRingtonePref;
    SwitchPreferenceCompat mEnablePerAlarmVolumePref;
    AlarmVolumePreference mAlarmVolumePref;
    SwitchPreferenceCompat mAdvancedAudioPlaybackPref;
    SwitchPreferenceCompat mAutoRoutingToBluetoothDevicePref;
    SwitchPreferenceCompat mSystemMediaVolume;
    CustomSeekbarPreference mBluetoothVolumePref;
    ListPreference mVolumeButtonsPref;
    ListPreference mPowerButtonPref;
    ListPreference mFlipActionPref;
    ListPreference mShakeActionPref;
    CustomSeekbarPreference mShakeIntensityPref;
    ListPreference mSortAlarmPref;
    SwitchPreferenceCompat mDisplayEnabledAlarmsFirstPref;
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
        mEnablePerAlarmVolumePref = findPreference(KEY_ENABLE_PER_ALARM_VOLUME);
        mAlarmVolumePref = findPreference(KEY_ALARM_VOLUME_SETTING);
        mAdvancedAudioPlaybackPref = findPreference(KEY_ADVANCED_AUDIO_PLAYBACK);
        mAutoRoutingToBluetoothDevicePref = findPreference(KEY_AUTO_ROUTING_TO_BLUETOOTH_DEVICE);
        mSystemMediaVolume = findPreference(KEY_SYSTEM_MEDIA_VOLUME);
        mBluetoothVolumePref = findPreference(KEY_BLUETOOTH_VOLUME);
        mVolumeButtonsPref = findPreference(KEY_VOLUME_BUTTONS);
        mPowerButtonPref = findPreference(KEY_POWER_BUTTON);
        mFlipActionPref = findPreference(KEY_FLIP_ACTION);
        mShakeActionPref = findPreference(KEY_SHAKE_ACTION);
        mShakeIntensityPref = findPreference(KEY_SHAKE_INTENSITY);
        mSortAlarmPref = findPreference(KEY_SORT_ALARM);
        mDisplayEnabledAlarmsFirstPref = findPreference(KEY_DISPLAY_ENABLED_ALARMS_FIRST);
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

        if (RingtoneUtils.hasBluetoothDeviceConnected(requireContext(), mPrefs)) {
            mAlarmVolumePref.setTitle(R.string.disconnect_bluetooth_device_title);
            mBluetoothVolumePref.setTitle(R.string.bluetooth_volume_title);
        } else {
            mAlarmVolumePref.setTitle(R.string.alarm_volume_title);
            mBluetoothVolumePref.setTitle(R.string.connect_bluetooth_device_title);
        }

        if (mAudioDeviceCallback == null) {
            initAudioDeviceCallback();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        stopRingtonePreview();

        if (mAudioDeviceCallback != null) {
            mAudioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
            mAudioDeviceCallback = null;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_DISPLAY_ENABLED_ALARMS_FIRST, KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT,
                 KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS,
                 KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM,
                 KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT ->
                    Utils.setVibrationTime(requireContext(), 50);

            case KEY_ENABLE_PER_ALARM_VOLUME -> {
                stopRingtonePreview();
                mAlarmVolumePref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
                // Set result so DeskClock knows to refresh itself
                requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
            }

            case KEY_ADVANCED_AUDIO_PLAYBACK -> {
                stopRingtonePreview();
                mAutoRoutingToBluetoothDevicePref.setVisible((boolean) newValue);
                mSystemMediaVolume.setVisible((boolean) newValue
                        && SettingsDAO.isAutoRoutingToBluetoothDeviceEnabled(mPrefs));
                mBluetoothVolumePref.setVisible((boolean) newValue
                        && SettingsDAO.isAutoRoutingToBluetoothDeviceEnabled(mPrefs)
                        && SettingsDAO.shouldUseCustomMediaVolume(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_AUTO_ROUTING_TO_BLUETOOTH_DEVICE -> {
                stopRingtonePreview();
                mSystemMediaVolume.setVisible((boolean) newValue);
                mBluetoothVolumePref.setVisible((boolean) newValue && SettingsDAO.shouldUseCustomMediaVolume(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_SYSTEM_MEDIA_VOLUME -> {
                stopRingtonePreview();
                mBluetoothVolumePref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VOLUME_BUTTONS, KEY_POWER_BUTTON, KEY_FLIP_ACTION,
                 KEY_MATERIAL_TIME_PICKER_STYLE, KEY_MATERIAL_DATE_PICKER_STYLE,
                 KEY_SORT_ALARM -> {
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

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference pref) {
        if (pref instanceof AutoSilenceDurationPreference autoSilenceDurationPreference) {
            int currentValue = autoSilenceDurationPreference.getAutoSilenceDuration();
            AutoSilenceDurationDialogFragment dialogFragment =
                    AutoSilenceDurationDialogFragment.newInstance(pref.getKey(), currentValue,
                            currentValue == ALARM_TIMEOUT_END_OF_RINGTONE);
            AutoSilenceDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof AlarmSnoozeDurationPreference alarmSnoozeDurationPreference) {
            int currentValue = alarmSnoozeDurationPreference.getSnoozeDuration();
            AlarmSnoozeDurationDialogFragment dialogFragment =
                    AlarmSnoozeDurationDialogFragment.newInstance(pref.getKey(), currentValue);
            AlarmSnoozeDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VolumeCrescendoDurationPreference volumeCrescendoDurationPreference) {
            int currentValue = volumeCrescendoDurationPreference.getVolumeCrescendoDuration();
            VolumeCrescendoDurationDialogFragment dialogFragment =
                    VolumeCrescendoDurationDialogFragment.newInstance(pref.getKey(), currentValue);
            VolumeCrescendoDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    private void setupPreferences() {
        mAudioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

        mAlarmRingtonePref.setOnPreferenceClickListener(this);

        // Alarm auto silence duration preference
        getParentFragmentManager().setFragmentResultListener(AutoSilenceDurationDialogFragment.REQUEST_KEY,
                this, (requestKey, bundle) -> {
            String key = bundle.getString(AutoSilenceDurationDialogFragment.RESULT_PREF_KEY);
            int newValue = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);

            if (key != null) {
                AutoSilenceDurationPreference pref = findPreference(key);
                if (pref != null) {
                    pref.setAutoSilenceDuration(newValue);
                    pref.setSummary(pref.getSummary());
                }
            }
        });

        // Alarm snooze duration preference
        getParentFragmentManager().setFragmentResultListener(AlarmSnoozeDurationDialogFragment.REQUEST_KEY,
                this, (requestKey, bundle) -> {
            String key = bundle.getString(AlarmSnoozeDurationDialogFragment.RESULT_PREF_KEY);
            int newValue = bundle.getInt(AlarmSnoozeDurationDialogFragment.ALARM_SNOOZE_DURATION_VALUE);

            if (key != null) {
                AlarmSnoozeDurationPreference pref = findPreference(key);
                if (pref != null) {
                    pref.setSnoozeDuration(newValue);
                    pref.setSummary(pref.getSummary());
                }
            }
        });

        mEnablePerAlarmVolumePref.setOnPreferenceChangeListener(this);

        mAlarmVolumePref.setVisible(!SettingsDAO.isPerAlarmVolumeEnabled(mPrefs));
        if (mAlarmVolumePref.isVisible()) {
            mAlarmVolumePref.setEnabled(!RingtoneUtils.hasBluetoothDeviceConnected(requireContext(), mPrefs));
        }

        // Alarm volume crescendo duration preference
        getParentFragmentManager().setFragmentResultListener(VolumeCrescendoDurationDialogFragment.REQUEST_KEY,
                this, (requestKey, bundle) -> {
            String key = bundle.getString(VolumeCrescendoDurationDialogFragment.RESULT_PREF_KEY);
            int newValue = bundle.getInt(VolumeCrescendoDurationDialogFragment.VOLUME_CRESCENDO_DURATION_VALUE);

            if (key != null) {
                VolumeCrescendoDurationPreference pref = findPreference(key);
                if (pref != null) {
                    pref.setVolumeCrescendoDuration(newValue);
                    pref.setSummary(pref.getSummary());
                }
            }
        });

        mAdvancedAudioPlaybackPref.setOnPreferenceChangeListener(this);

        mAutoRoutingToBluetoothDevicePref.setVisible(SettingsDAO.isAdvancedAudioPlaybackEnabled(mPrefs));
        mAutoRoutingToBluetoothDevicePref.setOnPreferenceChangeListener(this);

        mSystemMediaVolume.setVisible(SettingsDAO.isAdvancedAudioPlaybackEnabled(mPrefs)
                && SettingsDAO.isAutoRoutingToBluetoothDeviceEnabled(mPrefs));
        mSystemMediaVolume.setOnPreferenceChangeListener(this);

        mBluetoothVolumePref.setVisible(SettingsDAO.isAdvancedAudioPlaybackEnabled(mPrefs)
                && SettingsDAO.isAutoRoutingToBluetoothDeviceEnabled(mPrefs)
                && SettingsDAO.shouldUseCustomMediaVolume(mPrefs));
        mBluetoothVolumePref.setEnabled(mBluetoothVolumePref.isVisible()
                && RingtoneUtils.hasBluetoothDeviceConnected(requireContext(), mPrefs));

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

        mSortAlarmPref.setOnPreferenceChangeListener(this);
        mSortAlarmPref.setSummary(mSortAlarmPref.getEntry());

        mDisplayEnabledAlarmsFirstPref.setOnPreferenceChangeListener(this);

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

    private void initAudioDeviceCallback() {
        if (mAudioDeviceCallback != null) {
            return;
        }

        mAudioDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                super.onAudioDevicesAdded(addedDevices);

                mAlarmVolumePref.stopRingtonePreview();

                for (AudioDeviceInfo device : addedDevices) {
                    if (RingtoneUtils.isBluetoothDevice(device)) {
                        mAlarmVolumePref.setEnabled(false);
                        mAlarmVolumePref.setTitle(R.string.disconnect_bluetooth_device_title);
                        mBluetoothVolumePref.setEnabled(true);
                        mBluetoothVolumePref.setTitle(R.string.bluetooth_volume_title);
                    }
                }
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {

                mBluetoothVolumePref.stopRingtonePreviewForBluetoothDevices();

                for (AudioDeviceInfo device : removedDevices) {
                    if (RingtoneUtils.isBluetoothDevice(device)) {
                        mAlarmVolumePref.setEnabled(true);
                        mAlarmVolumePref.setTitle(R.string.alarm_volume_title);
                        mBluetoothVolumePref.setEnabled(false);
                        mBluetoothVolumePref.setTitle(R.string.connect_bluetooth_device_title);
                    }
                }
            }
        };

        mAudioManager.registerAudioDeviceCallback(mAudioDeviceCallback, new Handler(Looper.getMainLooper()));
    }

    private void stopRingtonePreview() {
        if (RingtoneUtils.hasBluetoothDeviceConnected(requireContext(), mPrefs)) {
            mBluetoothVolumePref.stopRingtonePreviewForBluetoothDevices();
        } else {
            mAlarmVolumePref.stopRingtonePreview();
        }
    }

}
