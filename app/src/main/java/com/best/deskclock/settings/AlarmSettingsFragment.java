// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_VOLUME;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VIBRATION_START_DELAY;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_NEVER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ADVANCED_AUDIO_PLAYBACK;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DISPLAY_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_NOTIFICATION_REMINDER_TIME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SNOOZE_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_VIBRATION_CATEGORY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_VOLUME_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_VOLUME_SETTING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_SILENCE_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DEFAULT_ALARM_RINGTONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_DISMISS_BUTTON;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_ENABLED_ALARMS_FIRST;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_ALARM_FAB_LONG_PRESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_AUTO_SILENCE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_SNOOZE_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_VIBRATION_PATTERN;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_EXTERNAL_AUDIO_DEVICE_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_FLIP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_DATE_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_TIME_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_POWER_BUTTON;
import static com.best.deskclock.settings.PreferencesKeys.KEY_REPEAT_MISSED_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SORT_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SYSTEM_MEDIA_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VIBRATION_PATTERN;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VOLUME_BUTTONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_WEEK_START;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
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
import com.best.deskclock.settings.custompreference.AlarmSnoozeDurationPreference;
import com.best.deskclock.settings.custompreference.AlarmVolumePreference;
import com.best.deskclock.settings.custompreference.AutoSilenceDurationPreference;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.settings.custompreference.VibrationPatternPreference;
import com.best.deskclock.settings.custompreference.VibrationStartDelayPreference;
import com.best.deskclock.settings.custompreference.VolumeCrescendoDurationPreference;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.Utils;

import java.util.List;

public class AlarmSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private AudioManager mAudioManager;
    private AudioDeviceCallback mAudioDeviceCallback;
    private AlarmUpdateHandler mAlarmUpdateHandler;
    private List<Alarm> mAlarmList;
    private boolean mHasExternalAudioDeviceConnected;

    Preference mAlarmFontPref;
    Preference mAlarmRingtonePref;
    AutoSilenceDurationPreference mAlarmAutoSilencePref;
    SwitchPreferenceCompat mEnablePerAlarmAutoSilencePref;
    AlarmSnoozeDurationPreference mAlarmSnoozeDurationPref;
    SwitchPreferenceCompat mEnablePerAlarmSnoozeDurationPref;
    ListPreference mRepeatMissedAlarmPref;
    SwitchPreferenceCompat mEnablePerAlarmMissedRepeatLimitPref;
    AlarmVolumePreference mAlarmVolumePref;
    SwitchPreferenceCompat mEnablePerAlarmVolumePref;
    VolumeCrescendoDurationPreference mAlarmVolumeCrescendoDurationPref;
    SwitchPreferenceCompat mEnablePerAlarmVolumeCrescendoDurationPref;
    SwitchPreferenceCompat mAdvancedAudioPlaybackPref;
    SwitchPreferenceCompat mAutoRoutingToExternalAudioDevicePref;
    SwitchPreferenceCompat mSystemMediaVolume;
    CustomSliderPreference mExternalAudioDeviceVolumePref;
    PreferenceCategory mAlarmVibrationCategory;
    ListPreference mVolumeButtonsPref;
    ListPreference mPowerButtonPref;
    ListPreference mFlipActionPref;
    ListPreference mShakeActionPref;
    CustomSliderPreference mShakeIntensityPref;
    ListPreference mSortAlarmPref;
    SwitchPreferenceCompat mDisplayEnabledAlarmsFirstPref;
    SwitchPreferenceCompat mEnableAlarmFabLongPressPref;
    ListPreference mWeekStartPref;
    SwitchPreferenceCompat mDisplayDismissButtonPref;
    ListPreference mAlarmNotificationReminderTimePref;
    VibrationPatternPreference mVibrationPatternPref;
    SwitchPreferenceCompat mEnablePerAlarmVibrationPatternPref;
    SwitchPreferenceCompat mEnableAlarmVibrationsByDefaultPref;
    SwitchPreferenceCompat mEnableSnoozedOrDismissedAlarmVibrationsPref;
    SwitchPreferenceCompat mTurnOnBackFlashForTriggeredAlarmPref;
    SwitchPreferenceCompat mDeleteOccasionalAlarmByDefaultPref;
    ListPreference mMaterialTimePickerStylePref;
    ListPreference mMaterialDatePickerStylePref;
    Preference mAlarmDisplayCustomizationPref;

    private final ActivityResultLauncher<Intent> fontPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    return;
                }

                Intent intent = result.getData();
                final Uri sourceUri = intent == null ? null : intent.getData();
                if (sourceUri == null) {
                    return;
                }

                // Take persistent permission
                requireActivity().getContentResolver().takePersistableUriPermission(
                        sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                String safeTitle = Utils.toSafeFileName("alarm_font");

                // Delete the old font if it exists
                clearFile(mPrefs.getString(KEY_ALARM_FONT, null));

                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_ALARM_FONT, copiedUri.getPath()).apply();
                    mAlarmFontPref.setTitle(getString(R.string.custom_font_title_variant));

                    CustomToast.show(requireContext(), R.string.custom_font_toast_message_selected);
                } else {
                    CustomToast.show(requireContext(), "Error importing font");
                    mAlarmFontPref.setTitle(getString(R.string.custom_font_title));
                }
            });

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

        mAlarmDisplayCustomizationPref = findPreference(KEY_ALARM_DISPLAY_CUSTOMIZATION);
        mAlarmFontPref = findPreference(KEY_ALARM_FONT);
        mMaterialTimePickerStylePref = findPreference(KEY_MATERIAL_TIME_PICKER_STYLE);
        mMaterialDatePickerStylePref = findPreference(KEY_MATERIAL_DATE_PICKER_STYLE);
        mAlarmRingtonePref = findPreference(KEY_DEFAULT_ALARM_RINGTONE);
        mAlarmAutoSilencePref = findPreference(KEY_AUTO_SILENCE_DURATION);
        mEnablePerAlarmAutoSilencePref = findPreference(KEY_ENABLE_PER_ALARM_AUTO_SILENCE);
        mAlarmSnoozeDurationPref = findPreference(KEY_ALARM_SNOOZE_DURATION);
        mEnablePerAlarmSnoozeDurationPref = findPreference(KEY_ENABLE_PER_ALARM_SNOOZE_DURATION);
        mRepeatMissedAlarmPref = findPreference(KEY_REPEAT_MISSED_ALARM);
        mEnablePerAlarmMissedRepeatLimitPref = findPreference(KEY_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT);
        mAlarmVolumePref = findPreference(KEY_ALARM_VOLUME_SETTING);
        mEnablePerAlarmVolumePref = findPreference(KEY_ENABLE_PER_ALARM_VOLUME);
        mAlarmVolumeCrescendoDurationPref = findPreference(KEY_ALARM_VOLUME_CRESCENDO_DURATION);
        mEnablePerAlarmVolumeCrescendoDurationPref = findPreference(KEY_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION);
        mAdvancedAudioPlaybackPref = findPreference(KEY_ADVANCED_AUDIO_PLAYBACK);
        mAutoRoutingToExternalAudioDevicePref = findPreference(KEY_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE);
        mSystemMediaVolume = findPreference(KEY_SYSTEM_MEDIA_VOLUME);
        mExternalAudioDeviceVolumePref = findPreference(KEY_EXTERNAL_AUDIO_DEVICE_VOLUME);
        mAlarmVibrationCategory = findPreference(KEY_ALARM_VIBRATION_CATEGORY);
        mEnableAlarmVibrationsByDefaultPref = findPreference(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT);
        mVibrationPatternPref = findPreference(KEY_VIBRATION_PATTERN);
        mEnablePerAlarmVibrationPatternPref = findPreference(KEY_ENABLE_PER_ALARM_VIBRATION_PATTERN);
        mEnableSnoozedOrDismissedAlarmVibrationsPref = findPreference(KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS);
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
        mTurnOnBackFlashForTriggeredAlarmPref = findPreference(KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM);
        mDeleteOccasionalAlarmByDefaultPref = findPreference(KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT);

        setupPreferences();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupFragmentResultListeners();
    }

    @Override
    public void onResume() {
        super.onResume();

        mAlarmRingtonePref.setSummary(DataModel.getDataModel().getAlarmRingtoneTitle());

        if (mHasExternalAudioDeviceConnected) {
            mAlarmVolumePref.setTitle(R.string.disconnect_external_audio_device_title);
            mExternalAudioDeviceVolumePref.setTitle(R.string.external_audio_device_volume_title);
        } else {
            mAlarmVolumePref.setTitle(R.string.alarm_volume_title);
            mExternalAudioDeviceVolumePref.setTitle(R.string.connect_external_audio_device_title);
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
                 KEY_DISPLAY_DISMISS_BUTTON, KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS ->
                    Utils.setVibrationTime(requireContext(), 50);

            case KEY_ENABLE_PER_ALARM_AUTO_SILENCE -> {
                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    for (Alarm alarm : mAlarmList) {
                        alarm.autoSilenceDuration = SettingsDAO.getAlarmTimeout(mPrefs);
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
                    for (Alarm alarm : mAlarmList) {
                        alarm.snoozeDuration = SettingsDAO.getSnoozeLength(mPrefs);
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
                    for (Alarm alarm : mAlarmList) {
                        alarm.missedAlarmRepeatLimit = SettingsDAO.getMissedAlarmRepeatLimit(mPrefs);
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                } else {
                    showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_missed_repeat_limit_dialog_message,
                            KEY_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT, mEnablePerAlarmMissedRepeatLimitPref,
                            mRepeatMissedAlarmPref, alarm ->
                                    alarm.missedAlarmRepeatLimit = SettingsDAO.getMissedAlarmRepeatLimit(mPrefs));

                    return false;
                }
            }

            case KEY_REPEAT_MISSED_ALARM -> {
                final int index = mRepeatMissedAlarmPref.findIndexOfValue((String) newValue);
                mRepeatMissedAlarmPref.setSummary(mRepeatMissedAlarmPref.getEntries()[index]);

                if (SettingsDAO.isPerAlarmMissedRepeatLimitDisabled(mPrefs)) {
                    for (Alarm alarm : mAlarmList) {
                        alarm.missedAlarmRepeatLimit = Integer.parseInt((String) newValue);
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                }
            }

            case KEY_ENABLE_PER_ALARM_VOLUME -> {
                stopRingtonePreview();

                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    //mAlarmVolumePref.setVisible(false);

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
                    for (Alarm alarm : mAlarmList) {
                        alarm.crescendoDuration = SettingsDAO.getAlarmVolumeCrescendoDuration(mPrefs);
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

            case KEY_ENABLE_PER_ALARM_VIBRATION_PATTERN -> {
                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    for (Alarm alarm : mAlarmList) {
                        alarm.vibrationPattern = SettingsDAO.getVibrationPattern(mPrefs);
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                } else {
                    showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_vibration_pattern_dialog_message,
                            KEY_ENABLE_PER_ALARM_VIBRATION_PATTERN, mEnablePerAlarmVibrationPatternPref,
                            mVibrationPatternPref, alarm ->
                                    alarm.vibrationPattern = SettingsDAO.getVibrationPattern(mPrefs));

                    return false;
                }
            }

            case KEY_ADVANCED_AUDIO_PLAYBACK -> {
                stopRingtonePreview();
                mAutoRoutingToExternalAudioDevicePref.setVisible((boolean) newValue);
                mSystemMediaVolume.setVisible((boolean) newValue
                        && SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs));
                mExternalAudioDeviceVolumePref.setVisible((boolean) newValue
                        && SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs)
                        && SettingsDAO.shouldUseCustomMediaVolume(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE -> {
                stopRingtonePreview();
                mSystemMediaVolume.setVisible((boolean) newValue);
                mExternalAudioDeviceVolumePref.setVisible((boolean) newValue
                        && SettingsDAO.shouldUseCustomMediaVolume(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_SYSTEM_MEDIA_VOLUME -> {
                stopRingtonePreview();
                mExternalAudioDeviceVolumePref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT -> {
                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    for (Alarm alarm : mAlarmList) {
                        alarm.vibrate = true;
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                } else {
                    showDisablePerAlarmSettingDialog(R.string.enable_alarm_vibrations_by_default_dialog_message,
                            KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, mEnableAlarmVibrationsByDefaultPref,
                            null, alarm -> alarm.vibrate = false);

                    return false;
                }
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

            case KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM -> {
                Utils.setVibrationTime(requireContext(), 50);

                for (Alarm alarm : mAlarmList) {
                    alarm.flash = (boolean) newValue;
                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                }
            }

            case KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT -> {
                Utils.setVibrationTime(requireContext(), 50);

                if ((boolean) newValue) {
                    for (Alarm alarm : mAlarmList) {
                        alarm.deleteAfterUse = true;
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }
                } else {
                    showDisablePerAlarmSettingDialog(R.string.enable_delete_occasional_alarm_by_default_dialog_message,
                            KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT, mDeleteOccasionalAlarmByDefaultPref,
                            null, alarm -> alarm.deleteAfterUse = false);

                    return false;
                }
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
            case KEY_ALARM_DISPLAY_CUSTOMIZATION -> animateAndShowFragment(new AlarmDisplayCustomizationFragment());

            case KEY_ALARM_FONT -> selectCustomFile(mAlarmFontPref, fontPickerLauncher,
                    SettingsDAO.getAlarmFont(mPrefs), KEY_ALARM_FONT, true, null);

            case KEY_DEFAULT_ALARM_RINGTONE ->
                    startActivity(RingtonePickerActivity.createAlarmRingtonePickerIntentForSettings(context));
        }

        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference pref) {
        if (pref instanceof AutoSilenceDurationPreference autoSilenceDurationPreference) {
            int currentValue = autoSilenceDurationPreference.getAutoSilenceDuration();
            AutoSilenceDurationDialogFragment dialogFragment =
                    AutoSilenceDurationDialogFragment.newInstance(pref.getKey(), currentValue);
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
        mAudioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        mHasExternalAudioDeviceConnected = RingtoneUtils.hasExternalAudioDeviceConnected(requireContext(), mPrefs);
        final int alarmTimeout = SettingsDAO.getAlarmTimeout(mPrefs);
        final boolean isAdvancedAudioPlaybackEnabled = SettingsDAO.isAdvancedAudioPlaybackEnabled(mPrefs);
        final boolean isAutoRoutingToExternalAudioDevice = SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs);

        mAlarmDisplayCustomizationPref.setOnPreferenceClickListener(this);

        mAlarmFontPref.setTitle(getString(SettingsDAO.getAlarmFont(mPrefs) == null
                ? R.string.custom_font_title
                : R.string.custom_font_title_variant));
        mAlarmFontPref.setOnPreferenceClickListener(this);

        mMaterialTimePickerStylePref.setOnPreferenceChangeListener(this);
        mMaterialTimePickerStylePref.setSummary(mMaterialTimePickerStylePref.getEntry());

        mMaterialDatePickerStylePref.setOnPreferenceChangeListener(this);
        mMaterialDatePickerStylePref.setSummary(mMaterialDatePickerStylePref.getEntry());

        mAlarmRingtonePref.setOnPreferenceClickListener(this);

        mEnablePerAlarmAutoSilencePref.setOnPreferenceChangeListener(this);

        mEnablePerAlarmSnoozeDurationPref.setOnPreferenceChangeListener(this);

        mRepeatMissedAlarmPref.setVisible(alarmTimeout != TIMEOUT_NEVER);
        mRepeatMissedAlarmPref.setOnPreferenceChangeListener(this);
        mRepeatMissedAlarmPref.setSummary(mRepeatMissedAlarmPref.getEntry());

        mEnablePerAlarmMissedRepeatLimitPref.setVisible(alarmTimeout != TIMEOUT_NEVER);
        mEnablePerAlarmMissedRepeatLimitPref.setOnPreferenceChangeListener(this);

        if (mAlarmVolumePref.isVisible()) {
            mAlarmVolumePref.setEnabled(!mHasExternalAudioDeviceConnected);
        }

        mEnablePerAlarmVolumePref.setOnPreferenceChangeListener(this);

        mEnablePerAlarmVolumeCrescendoDurationPref.setOnPreferenceChangeListener(this);

        mAdvancedAudioPlaybackPref.setOnPreferenceChangeListener(this);

        mAutoRoutingToExternalAudioDevicePref.setVisible(isAdvancedAudioPlaybackEnabled);
        mAutoRoutingToExternalAudioDevicePref.setOnPreferenceChangeListener(this);

        mSystemMediaVolume.setVisible(isAdvancedAudioPlaybackEnabled && isAutoRoutingToExternalAudioDevice);
        mSystemMediaVolume.setOnPreferenceChangeListener(this);

        mExternalAudioDeviceVolumePref.setVisible(isAdvancedAudioPlaybackEnabled
                && isAutoRoutingToExternalAudioDevice
                && SettingsDAO.shouldUseCustomMediaVolume(mPrefs));
        mExternalAudioDeviceVolumePref.setEnabled(mExternalAudioDeviceVolumePref.isVisible()
                && mHasExternalAudioDeviceConnected);

        mAlarmVibrationCategory.setVisible(DeviceUtils.hasVibrator(requireContext()));

        mEnableAlarmVibrationsByDefaultPref.setOnPreferenceChangeListener(this);

        mEnablePerAlarmVibrationPatternPref.setOnPreferenceChangeListener(this);

        mEnableSnoozedOrDismissedAlarmVibrationsPref.setOnPreferenceChangeListener(this);

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

        mTurnOnBackFlashForTriggeredAlarmPref.setVisible(DeviceUtils.hasBackFlash(requireContext()));
        mTurnOnBackFlashForTriggeredAlarmPref.setOnPreferenceChangeListener(this);

        mDeleteOccasionalAlarmByDefaultPref.setOnPreferenceChangeListener(this);
    }

    private void setupFragmentResultListeners() {
        FragmentManager parentFragmentManager = getParentFragmentManager();
        LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();

        // Alarm auto silence duration preference
        parentFragmentManager.setFragmentResultListener(AutoSilenceDurationDialogFragment.REQUEST_KEY,
                viewLifecycleOwner, (requestKey, bundle) -> {
                    String key = bundle.getString(AutoSilenceDurationDialogFragment.RESULT_PREF_KEY);
                    int newValue = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);

                    if (key != null) {
                        AutoSilenceDurationPreference pref = findPreference(key);
                        if (pref != null) {
                            pref.setAutoSilenceDuration(newValue);
                            pref.setSummary(pref.getSummary());
                            mEnablePerAlarmMissedRepeatLimitPref.setVisible(newValue != TIMEOUT_NEVER);
                            mRepeatMissedAlarmPref.setVisible(newValue != TIMEOUT_NEVER);

                            if (SettingsDAO.isPerAlarmAutoSilenceDisabled(mPrefs)) {
                                for (Alarm alarm : mAlarmList) {
                                    alarm.autoSilenceDuration = newValue;
                                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                                }
                            }
                        }
                    }
                });

        // Alarm snooze duration preference
        parentFragmentManager.setFragmentResultListener(AlarmSnoozeDurationDialogFragment.REQUEST_KEY,
                viewLifecycleOwner, (requestKey, bundle) -> {
                    String key = bundle.getString(AlarmSnoozeDurationDialogFragment.RESULT_PREF_KEY);
                    int newValue = bundle.getInt(AlarmSnoozeDurationDialogFragment.ALARM_SNOOZE_DURATION_VALUE);

                    if (key != null) {
                        AlarmSnoozeDurationPreference pref = findPreference(key);
                        if (pref != null) {
                            pref.setSnoozeDuration(newValue);
                            pref.setSummary(pref.getSummary());

                            if (SettingsDAO.isPerAlarmSnoozeDurationDisabled(mPrefs)) {
                                for (Alarm alarm : mAlarmList) {
                                    alarm.snoozeDuration = newValue;
                                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                                }
                            }
                        }
                    }
                });

        // Alarm volume crescendo duration preference
        parentFragmentManager.setFragmentResultListener(VolumeCrescendoDurationDialogFragment.REQUEST_KEY,
                viewLifecycleOwner, (requestKey, bundle) -> {
                    String key = bundle.getString(VolumeCrescendoDurationDialogFragment.RESULT_PREF_KEY);
                    int newValue = bundle.getInt(VolumeCrescendoDurationDialogFragment.VOLUME_CRESCENDO_DURATION_VALUE);

                    if (key != null) {
                        VolumeCrescendoDurationPreference pref = findPreference(key);
                        if (pref != null) {
                            pref.setVolumeCrescendoDuration(newValue);
                            pref.setSummary(pref.getSummary());

                            if (SettingsDAO.isPerAlarmCrescendoDurationDisabled(mPrefs)) {
                                for (Alarm alarm : mAlarmList) {
                                    alarm.crescendoDuration = newValue;
                                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                                }
                            }
                        }
                    }
                });

        // Vibration pattern preference
        parentFragmentManager.setFragmentResultListener(VibrationPatternDialogFragment.REQUEST_KEY,
                viewLifecycleOwner, (requestKey, bundle) -> {
                    String key = bundle.getString(VibrationPatternDialogFragment.RESULT_PREF_KEY);
                    String newValue = bundle.getString(VibrationPatternDialogFragment.RESULT_PATTERN_KEY);

                    if (key != null) {
                        VibrationPatternPreference pref = findPreference(key);
                        if (pref != null) {
                            pref.setPattern(newValue);
                            pref.setSummary(pref.getSummary());

                            if (!SettingsDAO.isPerAlarmVibrationPatternEnabled(mPrefs)) {
                                for (Alarm alarm : mAlarmList) {
                                    alarm.vibrationPattern = newValue;
                                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                                }
                            }
                        }
                    }
                });

        // Vibration start delay preference
        parentFragmentManager.setFragmentResultListener(VibrationStartDelayDialogFragment.REQUEST_KEY,
                viewLifecycleOwner, (requestKey, bundle) -> {
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
    }

    private void showDisablePerAlarmSettingDialog(@StringRes int messageResId, String prefKey,
                                                  SwitchPreferenceCompat switchPref,
                                                  @Nullable Preference dependentPref,
                                                  AlarmUpdater alarmUpdater) {

        String confirmAction = requireContext().getString(R.string.confirm_action_prompt);

        AlertDialog dialog = CustomDialog.createSimpleDialog(
                requireContext(),
                R.drawable.ic_error,
                R.string.warning,
                getString(messageResId, confirmAction),
                android.R.string.ok,
                (d, w) -> {
                    for (Alarm alarm : mAlarmList) {
                        alarmUpdater.update(alarm);
                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    }

                    mPrefs.edit().putBoolean(prefKey, false).apply();
                    switchPref.setChecked(false);
                    if (dependentPref != null) {
                        dependentPref.setVisible(true);
                    }
                }
        );

        dialog.show();
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
                    if (RingtoneUtils.isExternalAudioDevice(device)) {
                        mAlarmVolumePref.setEnabled(false);
                        mAlarmVolumePref.setTitle(R.string.disconnect_external_audio_device_title);
                        mExternalAudioDeviceVolumePref.setEnabled(true);
                        mExternalAudioDeviceVolumePref.setTitle(R.string.external_audio_device_volume_title);
                    }
                }
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                mExternalAudioDeviceVolumePref.stopRingtonePreviewForExternalAudioDevices();

                for (AudioDeviceInfo device : removedDevices) {
                    if (RingtoneUtils.isExternalAudioDevice(device)) {
                        mAlarmVolumePref.setEnabled(true);
                        mAlarmVolumePref.setTitle(R.string.alarm_volume_title);
                        mExternalAudioDeviceVolumePref.setEnabled(false);
                        mExternalAudioDeviceVolumePref.setTitle(R.string.connect_external_audio_device_title);
                    }
                }
            }
        };

        mAudioManager.registerAudioDeviceCallback(mAudioDeviceCallback, new Handler(Looper.getMainLooper()));
    }

    private void stopRingtonePreview() {
        if (mHasExternalAudioDeviceConnected) {
            mExternalAudioDeviceVolumePref.stopRingtonePreviewForExternalAudioDevices();
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
