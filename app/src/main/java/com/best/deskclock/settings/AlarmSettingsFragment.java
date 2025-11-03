// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_SNOOZE_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_VOLUME;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_AUTO_SILENCE_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_MISSED_ALARM_REPEAT_LIMIT;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VIBRATION_START_DELAY;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_NEVER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DISPLAY_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_NOTIFICATION_REMINDER_TIME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SNOOZE_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_VOLUME_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_VOLUME_SETTING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_ROUTING_TO_BLUETOOTH_DEVICE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_SILENCE_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_BLUETOOTH_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DEFAULT_ALARM_RINGTONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_DISMISS_BUTTON;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_ENABLED_ALARMS_FIRST;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_ALARM_FAB_LONG_PRESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_AUTO_SILENCE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_SNOOZE_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_FLIP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_DATE_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_TIME_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MISSED_ALARM_REPEAT_LIMIT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_POWER_BUTTON;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ADVANCED_AUDIO_PLAYBACK;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SORT_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SYSTEM_MEDIA_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VIBRATION_PATTERN;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VIBRATION_START_DELAY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VOLUME_BUTTONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_WEEK_START;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.AlarmSnoozeDurationDialogFragment;
import com.best.deskclock.AutoSilenceDurationDialogFragment;
import com.best.deskclock.R;
import com.best.deskclock.VibrationPatternDialogFragment;
import com.best.deskclock.VibrationStartDelayDialogFragment;
import com.best.deskclock.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class AlarmSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private AudioManager mAudioManager;
    private AudioDeviceCallback mAudioDeviceCallback;
    private AlarmUpdateHandler mAlarmUpdateHandler;
    private List<Alarm> mAlarmList;

    Preference mAlarmRingtonePref;
    SwitchPreferenceCompat mEnablePerAlarmAutoSilencePref;
    Preference mAlarmAutoSilencePref;
    SwitchPreferenceCompat mEnablePerAlarmSnoozeDurationPref;
    Preference mAlarmSnoozeDurationPref;
    SwitchPreferenceCompat mEnablePerAlarmMissedRepeatLimitPref;
    ListPreference mMissedAlarmRepeatLimitPref;
    SwitchPreferenceCompat mEnablePerAlarmVolumePref;
    AlarmVolumePreference mAlarmVolumePref;
    SwitchPreferenceCompat mEnablePerAlarmVolumeCrescendoDurationPref;
    Preference mAlarmVolumeCrescendoDurationPref;
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
    SwitchPreferenceCompat mEnableAlarmFabLongPressPref;
    ListPreference mWeekStartPref;
    SwitchPreferenceCompat mDisplayDismissButtonPref;
    ListPreference mAlarmNotificationReminderTimePref;
    Preference mVibrationPatternPref;
    Preference mVibrationStartDelayPref;
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

        final ContentResolver contentResolver = requireContext().getContentResolver();
        mAlarmUpdateHandler = new AlarmUpdateHandler(requireContext(), null, null);
        mAlarmList = Alarm.getAlarms(contentResolver, null);

        addPreferencesFromResource(R.xml.settings_alarm);

        mAlarmRingtonePref = findPreference(KEY_DEFAULT_ALARM_RINGTONE);
        mEnablePerAlarmAutoSilencePref = findPreference(KEY_ENABLE_PER_ALARM_AUTO_SILENCE);
        mAlarmAutoSilencePref = findPreference(KEY_AUTO_SILENCE_DURATION);
        mEnablePerAlarmSnoozeDurationPref = findPreference(KEY_ENABLE_PER_ALARM_SNOOZE_DURATION);
        mAlarmSnoozeDurationPref = findPreference(KEY_ALARM_SNOOZE_DURATION);
        mEnablePerAlarmMissedRepeatLimitPref = findPreference(KEY_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT);
        mMissedAlarmRepeatLimitPref = findPreference(KEY_MISSED_ALARM_REPEAT_LIMIT);
        mEnablePerAlarmVolumePref = findPreference(KEY_ENABLE_PER_ALARM_VOLUME);
        mAlarmVolumePref = findPreference(KEY_ALARM_VOLUME_SETTING);
        mEnablePerAlarmVolumeCrescendoDurationPref = findPreference(KEY_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION);
        mAlarmVolumeCrescendoDurationPref = findPreference(KEY_ALARM_VOLUME_CRESCENDO_DURATION);
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
        mEnableAlarmFabLongPressPref = findPreference(KEY_ENABLE_ALARM_FAB_LONG_PRESS);
        mWeekStartPref = findPreference(KEY_WEEK_START);
        mDisplayDismissButtonPref = findPreference(KEY_DISPLAY_DISMISS_BUTTON);
        mAlarmNotificationReminderTimePref = findPreference(KEY_ALARM_NOTIFICATION_REMINDER_TIME);
        mVibrationPatternPref = findPreference(KEY_VIBRATION_PATTERN);
        mVibrationStartDelayPref = findPreference(KEY_VIBRATION_START_DELAY);
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
            case KEY_DISPLAY_ENABLED_ALARMS_FIRST, KEY_ENABLE_ALARM_FAB_LONG_PRESS,
                 KEY_DISPLAY_DISMISS_BUTTON, KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT,
                 KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS,
                 KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM,
                 KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT ->
                    Utils.setVibrationTime(requireContext(), 50);

            case KEY_ENABLE_PER_ALARM_AUTO_SILENCE -> {
                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    mAlarmAutoSilencePref.setVisible(false);

                    for (Alarm alarm : mAlarmList) {
                        alarm.autoSilenceDuration = DEFAULT_AUTO_SILENCE_DURATION;
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                } else {
                    showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_auto_silence_dialog_message,
                            KEY_ENABLE_PER_ALARM_AUTO_SILENCE, mEnablePerAlarmAutoSilencePref,
                            mAlarmAutoSilencePref, alarm ->
                                    alarm.autoSilenceDuration = SettingsDAO.getAlarmTimeout(mPrefs));

                    return false;
                }
            }

            case KEY_ENABLE_PER_ALARM_SNOOZE_DURATION -> {
                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    mAlarmSnoozeDurationPref.setVisible(false);

                    for (Alarm alarm : mAlarmList) {
                        alarm.snoozeDuration = DEFAULT_ALARM_SNOOZE_DURATION;
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                } else {
                    showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_snooze_duration_dialog_message,
                            KEY_ENABLE_PER_ALARM_SNOOZE_DURATION, mEnablePerAlarmSnoozeDurationPref,
                            mAlarmSnoozeDurationPref, alarm ->
                                    alarm.snoozeDuration = SettingsDAO.getSnoozeLength(mPrefs));

                    return false;
                }
            }

            case KEY_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT -> {
                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    mMissedAlarmRepeatLimitPref.setVisible(false);

                    for (Alarm alarm : mAlarmList) {
                        alarm.missedAlarmRepeatLimit = Integer.parseInt(DEFAULT_MISSED_ALARM_REPEAT_LIMIT);
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                } else {
                    showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_missed_repeat_limit_dialog_message,
                            KEY_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT, mEnablePerAlarmMissedRepeatLimitPref,
                            mMissedAlarmRepeatLimitPref, alarm ->
                                    alarm.missedAlarmRepeatLimit = SettingsDAO.getMissedAlarmRepeatLimit(mPrefs));

                    return false;
                }
            }

            case KEY_MISSED_ALARM_REPEAT_LIMIT -> {
                final int index = mMissedAlarmRepeatLimitPref.findIndexOfValue((String) newValue);
                mMissedAlarmRepeatLimitPref.setSummary(mMissedAlarmRepeatLimitPref.getEntries()[index]);

                for (Alarm alarm : mAlarmList) {
                    alarm.missedAlarmRepeatLimit = Integer.parseInt((String) newValue);
                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                }
            }

            case KEY_ENABLE_PER_ALARM_VOLUME -> {
                stopRingtonePreview();

                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    mAlarmVolumePref.setVisible(false);

                    for (Alarm alarm : mAlarmList) {
                        alarm.alarmVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                } else {
                    showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_volume_dialog_message,
                            KEY_ENABLE_PER_ALARM_VOLUME, mEnablePerAlarmVolumePref, mAlarmVolumePref,
                            alarm -> alarm.alarmVolume = DEFAULT_ALARM_VOLUME);

                    return false;
                }
            }

            case KEY_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION -> {
                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    mAlarmVolumeCrescendoDurationPref.setVisible(false);

                    for (Alarm alarm : mAlarmList) {
                        alarm.crescendoDuration = DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION;
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                } else {
                    showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_crescendo_duration_dialog_message,
                            KEY_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION, mEnablePerAlarmVolumeCrescendoDurationPref,
                            mAlarmVolumeCrescendoDurationPref, alarm ->
                                    alarm.crescendoDuration = SettingsDAO.getAlarmVolumeCrescendoDuration(mPrefs));

                    return false;
                }
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
                 KEY_SORT_ALARM, KEY_VIBRATION_PATTERN -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_ALARM_NOTIFICATION_REMINDER_TIME -> {
                final int index = mAlarmNotificationReminderTimePref.findIndexOfValue((String) newValue);
                mAlarmNotificationReminderTimePref.setSummary(mAlarmNotificationReminderTimePref.getEntries()[index]);

                for (Alarm alarm : mAlarmList) {
                    if (alarm.enabled) {
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, false);
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
                            currentValue == TIMEOUT_END_OF_RINGTONE,
                            currentValue == TIMEOUT_NEVER, false);
            AutoSilenceDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof AlarmSnoozeDurationPreference alarmSnoozeDurationPreference) {
            int currentValue = alarmSnoozeDurationPreference.getSnoozeDuration();
            AlarmSnoozeDurationDialogFragment dialogFragment =
                    AlarmSnoozeDurationDialogFragment.newInstance(pref.getKey(), currentValue,
                            currentValue == ALARM_SNOOZE_DURATION_DISABLED);
            AlarmSnoozeDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VolumeCrescendoDurationPreference volumeCrescendoDurationPreference) {
            int currentValue = volumeCrescendoDurationPreference.getVolumeCrescendoDuration();
            VolumeCrescendoDurationDialogFragment dialogFragment =
                    VolumeCrescendoDurationDialogFragment.newInstance(pref.getKey(), currentValue,
                            currentValue == DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION);
            VolumeCrescendoDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VibrationPatternPreference vibrationPatternPreference) {
            String currentValue = vibrationPatternPreference.getPattern();
            VibrationPatternDialogFragment dialogFragment =
                    VibrationPatternDialogFragment.newInstance(pref.getKey(), currentValue);
            VibrationPatternDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VibrationStartDelayPreference vibrationStartDelayPreference) {
            int currentValue = vibrationStartDelayPreference.getVibrationStartDelay();
            VibrationStartDelayDialogFragment dialogFragment =
                    VibrationStartDelayDialogFragment.newInstance(pref.getKey(), currentValue,
                            currentValue == DEFAULT_VIBRATION_START_DELAY);
            VibrationStartDelayDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    private void setupPreferences() {
        final boolean hasVibrator = DeviceUtils.hasVibrator(requireContext());
        mAudioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

        mAlarmRingtonePref.setOnPreferenceClickListener(this);

        mEnablePerAlarmAutoSilencePref.setOnPreferenceChangeListener(this);

        // Alarm auto silence duration preference
        mAlarmAutoSilencePref.setVisible(!SettingsDAO.isPerAlarmAutoSilenceEnabled(mPrefs));
        getParentFragmentManager().setFragmentResultListener(AutoSilenceDurationDialogFragment.REQUEST_KEY,
                this, (requestKey, bundle) -> {
            String key = bundle.getString(AutoSilenceDurationDialogFragment.RESULT_PREF_KEY);
            int newValue = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);

            if (key != null) {
                AutoSilenceDurationPreference pref = findPreference(key);
                if (pref != null) {
                    pref.setAutoSilenceDuration(newValue);
                    pref.setSummary(pref.getSummary());
                    mEnablePerAlarmMissedRepeatLimitPref.setVisible(newValue != TIMEOUT_NEVER);
                    mMissedAlarmRepeatLimitPref.setVisible(newValue != TIMEOUT_NEVER);
                    for (Alarm alarm : mAlarmList) {
                        alarm.autoSilenceDuration = newValue;
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                }
            }
        });

        mEnablePerAlarmSnoozeDurationPref.setOnPreferenceChangeListener(this);

        // Alarm snooze duration preference
        mAlarmSnoozeDurationPref.setVisible(!SettingsDAO.isPerAlarmSnoozeDurationEnabled(mPrefs));
        getParentFragmentManager().setFragmentResultListener(AlarmSnoozeDurationDialogFragment.REQUEST_KEY,
                this, (requestKey, bundle) -> {
            String key = bundle.getString(AlarmSnoozeDurationDialogFragment.RESULT_PREF_KEY);
            int newValue = bundle.getInt(AlarmSnoozeDurationDialogFragment.ALARM_SNOOZE_DURATION_VALUE);

            if (key != null) {
                AlarmSnoozeDurationPreference pref = findPreference(key);
                if (pref != null) {
                    pref.setSnoozeDuration(newValue);
                    pref.setSummary(pref.getSummary());
                    for (Alarm alarm : mAlarmList) {
                        alarm.snoozeDuration = newValue;
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                }
            }
        });

        mEnablePerAlarmMissedRepeatLimitPref.setVisible(SettingsDAO.getAlarmTimeout(mPrefs) != TIMEOUT_NEVER);
        mEnablePerAlarmMissedRepeatLimitPref.setOnPreferenceChangeListener(this);

        mMissedAlarmRepeatLimitPref.setVisible(SettingsDAO.getAlarmTimeout(mPrefs) != TIMEOUT_NEVER
                && !SettingsDAO.isPerAlarmMissedRepeatLimitEnabled(mPrefs));
        mMissedAlarmRepeatLimitPref.setOnPreferenceChangeListener(this);
        mMissedAlarmRepeatLimitPref.setSummary(mMissedAlarmRepeatLimitPref.getEntry());

        mEnablePerAlarmVolumePref.setOnPreferenceChangeListener(this);

        mAlarmVolumePref.setVisible(!SettingsDAO.isPerAlarmVolumeEnabled(mPrefs));
        if (mAlarmVolumePref.isVisible()) {
            mAlarmVolumePref.setEnabled(!RingtoneUtils.hasBluetoothDeviceConnected(requireContext(), mPrefs));
        }

        mEnablePerAlarmVolumeCrescendoDurationPref.setOnPreferenceChangeListener(this);

        // Alarm volume crescendo duration preference
        mAlarmVolumeCrescendoDurationPref.setVisible(!SettingsDAO.isPerAlarmCrescendoDurationEnabled(mPrefs));
        getParentFragmentManager().setFragmentResultListener(VolumeCrescendoDurationDialogFragment.REQUEST_KEY,
                this, (requestKey, bundle) -> {
            String key = bundle.getString(VolumeCrescendoDurationDialogFragment.RESULT_PREF_KEY);
            int newValue = bundle.getInt(VolumeCrescendoDurationDialogFragment.VOLUME_CRESCENDO_DURATION_VALUE);

            if (key != null) {
                VolumeCrescendoDurationPreference pref = findPreference(key);
                if (pref != null) {
                    pref.setVolumeCrescendoDuration(newValue);
                    pref.setSummary(pref.getSummary());
                    for (Alarm alarm : mAlarmList) {
                        alarm.crescendoDuration = newValue;
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
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

        mEnableAlarmFabLongPressPref.setOnPreferenceChangeListener(this);

        // Set the default first day of the week programmatically
        final Weekdays.Order weekdayOrder = SettingsDAO.getWeekdayOrder(mPrefs);
        final Integer firstDay = weekdayOrder.getCalendarDays().get(0);
        final String value = String.valueOf(firstDay);
        final int index = mWeekStartPref.findIndexOfValue(value);
        mWeekStartPref.setValueIndex(index);
        mWeekStartPref.setSummary(mWeekStartPref.getEntries()[index]);
        mWeekStartPref.setOnPreferenceChangeListener(this);

        mDisplayDismissButtonPref.setOnPreferenceChangeListener(this);

        mAlarmNotificationReminderTimePref.setOnPreferenceChangeListener(this);
        mAlarmNotificationReminderTimePref.setSummary(mAlarmNotificationReminderTimePref.getEntry());

        mVibrationPatternPref.setVisible(hasVibrator);
        getParentFragmentManager().setFragmentResultListener(VibrationPatternDialogFragment.REQUEST_KEY,
                this, (requestKey, bundle) -> {
            String key = bundle.getString(VibrationPatternDialogFragment.RESULT_PREF_KEY);
            String newValue = bundle.getString(VibrationPatternDialogFragment.RESULT_PATTERN_KEY);

            if (key != null) {
                VibrationPatternPreference pref = findPreference(key);
                if (pref != null) {
                    pref.setPattern(newValue);
                    pref.setSummary(pref.getSummary());
                }
            }
        });

        mVibrationStartDelayPref.setVisible(hasVibrator);
        getParentFragmentManager().setFragmentResultListener(VibrationStartDelayDialogFragment.REQUEST_KEY,
                this, (requestKey, bundle) -> {
            String key = bundle.getString(VibrationStartDelayDialogFragment.RESULT_PREF_KEY);
            int newValue = bundle.getInt(VibrationStartDelayDialogFragment.VIBRATION_DELAY_VALUE);

            if (key != null) {
                VibrationStartDelayPreference pref = findPreference(key);
                if (pref != null) {
                    pref.setVibrationStartDelay(newValue);
                    pref.setSummary(pref.getSummary());
                }
            }
        });

        mEnableAlarmVibrationsByDefaultPref.setVisible(hasVibrator);
        mEnableAlarmVibrationsByDefaultPref.setOnPreferenceChangeListener(this);

        mEnableSnoozedOrDismissedAlarmVibrationsPref.setVisible(hasVibrator);
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

    private void showDisablePerAlarmSettingDialog(@StringRes int messageResId, String prefKey,
                                                  SwitchPreferenceCompat switchPref, Preference dependentPref,
                                                  AlarmUpdater alarmUpdater) {

        final Drawable icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error);
        if (icon != null) {
            icon.setTint(MaterialColors.getColor(
                    requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
        }

        String confirmAction = requireContext().getString(R.string.confirm_action_prompt);
        String message = requireContext().getString(messageResId, confirmAction);

        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(icon)
                .setTitle(R.string.warning)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    for (Alarm alarm : mAlarmList) {
                        alarmUpdater.update(alarm);
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }

                    mPrefs.edit().putBoolean(prefKey, false).apply();
                    switchPref.setChecked(false);
                    dependentPref.setVisible(true);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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

    /**
     * Interface for updating alarm properties when pressing the OK button in the dialog box
     * that appears when the "per alarm" settings are disabled.
     */
    private interface AlarmUpdater {
        void update(Alarm alarm);
    }

}
