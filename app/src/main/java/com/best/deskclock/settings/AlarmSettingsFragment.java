// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.base.BaseSettingsScreenFragment;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.dialogfragment.AlarmMathHardnessLevelDialogFragment;
import com.best.deskclock.dialogfragment.AlarmNotificationReminderDialogFragment;
import com.best.deskclock.dialogfragment.AlarmSnoozeDurationDialogFragment;
import com.best.deskclock.dialogfragment.AutoSilenceDurationDialogFragment;
import com.best.deskclock.dialogfragment.VibrationPatternDialogFragment;
import com.best.deskclock.dialogfragment.VibrationStartDelayDialogFragment;
import com.best.deskclock.dialogfragment.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.ClockContract;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.settings.custompreference.AlarmMathHardnessLevelPreference;
import com.best.deskclock.settings.custompreference.AlarmNotificationReminderPreference;
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
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.PermissionUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AlarmSettingsFragment extends BaseSettingsScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String KEY_PENDING_DIALOG_PREF_KEY = "pending_dialog_pref_key";

    private String mPendingDialogPrefKey = null;

    private AudioManager mAudioManager;

    private final AudioDeviceCallback mAudioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(@NonNull AudioDeviceInfo[] addedDevices) {
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
        public void onAudioDevicesRemoved(@NonNull AudioDeviceInfo[] removedDevices) {
            super.onAudioDevicesRemoved(removedDevices);
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

    private boolean mHasExternalAudioDeviceConnected;

    private AlarmUpdateHandler mAlarmUpdateHandler;

    Preference mAlarmDisplayCustomizationPref;
    Preference mAlarmFontPref;
    ListPreference mMaterialTimePickerStylePref;
    ListPreference mMaterialDatePickerStylePref;
    Preference mAlarmRingtonePref;
    ListPreference mRepeatMissedAlarmPref;
    AlarmVolumePreference mAlarmVolumePref;
    SwitchPreferenceCompat mAdvancedAudioPlaybackPref;
    SwitchPreferenceCompat mAutoRoutingToExternalAudioDevicePref;
    SwitchPreferenceCompat mSystemMediaVolumePref;
    CustomSliderPreference mExternalAudioDeviceVolumePref;
    VibrationPatternPreference mVibrationPatternPref;
    SwitchPreferenceCompat mEnableAlarmVibrationsByDefaultPref;
    VibrationStartDelayPreference mVibrationStartDelayPref;
    SwitchPreferenceCompat mEnableSnoozedOrDismissedAlarmVibrationsPref;
    ListPreference mVolumeButtonsPref;
    ListPreference mPowerButtonPref;
    ListPreference mHeadphonesButtonPref;
    ListPreference mFlipActionPref;
    ListPreference mShakeActionPref;
    ListPreference mSortAlarmPref;
    SwitchPreferenceCompat mDisplayEnabledAlarmsFirstPref;
    SwitchPreferenceCompat mDisplayLockedAlarmsFirstPref;
    SwitchPreferenceCompat mEnableAlarmSortingByCategoryPref;
    ListPreference mDefaultCategoryPref;
    SwitchPreferenceCompat mShouldSwitchToNewCategoryPref;
    SwitchPreferenceCompat mDisplayEnabledAlarmBadgePref;
    SwitchPreferenceCompat mDeleteAlarmsWithCategoryPref;
    MultiSelectListPreference mFeaturesToDisplayPref;
    SwitchPreferenceCompat mEnableAlarmFabLongPressPref;
    ListPreference mWeekStartPref;
    SwitchPreferenceCompat mDisplayDismissButtonPref;
    SwitchPreferenceCompat mAlarmProximitySensorPref;
    SwitchPreferenceCompat mDisableDNDForTriggeredAlarmPref;
    SwitchPreferenceCompat mTurnOnBackFlashForTriggeredAlarmPref;
    SwitchPreferenceCompat mDeleteOccasionalAlarmByDefaultPref;
    SwitchPreferenceCompat mDisplayLowAlarmVolumeWarningPref;

    private final ActivityResultLauncher<Intent> fontPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (isAdded() && mIsSearchMode) {
                blockSearchResetForAWhile();
            }

            if (result.getResultCode() != RESULT_OK) {
                return;
            }

            Intent intent = result.getData();
            final Uri sourceUri = intent == null ? null : intent.getData();
            if (sourceUri == null) {
                return;
            }

            final Context appContext = requireContext().getApplicationContext();
            final int style = getAccentStyle();
            final Typeface font = getGeneralTypeface();
            final SharedPreferences prefs = getPrefs();

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = FileUtils.toSafeFileName(FILE_ALARM_FONT);
            String oldFontPath = prefs.getString(KEY_ALARM_FONT, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old font if it exists
                FileUtils.clearFile(oldFontPath);

                // Clear the font cache
                ThemeUtils.removeFontFromCache(oldFontPath);

                // Copy the new font to the device's protected storage
                Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    prefs.edit().putString(KEY_ALARM_FONT, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, style, font, R.string.custom_font_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, style, font, R.string.font_message_error);
                    }

                    if (!isAdded() || mAlarmFontPref == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        mAlarmFontPref.setTitle(getString(R.string.custom_font_title_variant));
                    }else {
                        mAlarmFontPref.setTitle(getString(R.string.custom_font_title));
                    }
                });
            });
        });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.alarm_settings);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAudioManager = requireContext().getApplicationContext().getSystemService(AudioManager.class);
        mHasExternalAudioDeviceConnected = RingtoneUtils.hasExternalAudioDeviceConnected(
            requireContext(), SettingsDAO.isAutoRoutingToExternalAudioDevice(getPrefs()));
        mAlarmUpdateHandler = new AlarmUpdateHandler(
            requireContext(), getPrefs(), getGeneralTypeface(), null, null, isVibrationsEnabled());

        addPreferencesFromResource(R.xml.settings_alarm);

        mAlarmDisplayCustomizationPref = findPreference(KEY_ALARM_DISPLAY_CUSTOMIZATION);
        mAlarmFontPref = findPreference(KEY_ALARM_FONT);
        mMaterialTimePickerStylePref = findPreference(KEY_MATERIAL_TIME_PICKER_STYLE);
        mMaterialDatePickerStylePref = findPreference(KEY_MATERIAL_DATE_PICKER_STYLE);
        mAlarmRingtonePref = findPreference(KEY_DEFAULT_ALARM_RINGTONE);
        mRepeatMissedAlarmPref = findPreference(KEY_REPEAT_MISSED_ALARM);
        mAlarmVolumePref = findPreference(KEY_ALARM_VOLUME_SETTING);
        mAdvancedAudioPlaybackPref = findPreference(KEY_ADVANCED_AUDIO_PLAYBACK);
        mAutoRoutingToExternalAudioDevicePref = findPreference(KEY_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE);
        mSystemMediaVolumePref = findPreference(KEY_SYSTEM_MEDIA_VOLUME);
        mExternalAudioDeviceVolumePref = findPreference(KEY_EXTERNAL_AUDIO_DEVICE_VOLUME);
        mEnableAlarmVibrationsByDefaultPref = findPreference(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT);
        mVibrationPatternPref = findPreference(KEY_VIBRATION_PATTERN);
        mVibrationStartDelayPref = findPreference(KEY_VIBRATION_START_DELAY);
        mEnableSnoozedOrDismissedAlarmVibrationsPref = findPreference(KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS);
        mVolumeButtonsPref = findPreference(KEY_VOLUME_BUTTONS);
        mPowerButtonPref = findPreference(KEY_POWER_BUTTON);
        mHeadphonesButtonPref = findPreference(KEY_HEADPHONES_BUTTON);
        mFlipActionPref = findPreference(KEY_FLIP_ACTION);
        mShakeActionPref = findPreference(KEY_SHAKE_ACTION);
        mSortAlarmPref = findPreference(KEY_SORT_ALARM);
        mDisplayEnabledAlarmsFirstPref = findPreference(KEY_DISPLAY_ENABLED_ALARMS_FIRST);
        mDisplayLockedAlarmsFirstPref = findPreference(KEY_DISPLAY_LOCKED_ALARMS_FIRST);
        mEnableAlarmSortingByCategoryPref = findPreference(KEY_ENABLE_ALARM_SORTING_BY_CATEGORY);
        mDefaultCategoryPref = findPreference(KEY_DEFAULT_ALARM_CATEGORY);
        mShouldSwitchToNewCategoryPref = findPreference(KEY_SWITCH_TO_NEW_CATEGORY);
        mDisplayEnabledAlarmBadgePref = findPreference(KEY_DISPLAY_ENABLED_ALARM_BADGE);
        mDeleteAlarmsWithCategoryPref = findPreference(KEY_DELETE_ALARMS_WITH_CATEGORY);
        mFeaturesToDisplayPref = findPreference(KEY_ALARM_FEATURES_TO_DISPLAY);
        mEnableAlarmFabLongPressPref = findPreference(KEY_ENABLE_ALARM_FAB_LONG_PRESS);
        mWeekStartPref = findPreference(KEY_WEEK_START);
        mDisplayDismissButtonPref = findPreference(KEY_DISPLAY_DISMISS_BUTTON);
        mAlarmProximitySensorPref = findPreference(KEY_ENABLE_ALARM_PROXIMITY_SENSOR);
        mDisableDNDForTriggeredAlarmPref = findPreference(KEY_DISABLE_DND_FOR_TRIGGERED_ALARM);
        mTurnOnBackFlashForTriggeredAlarmPref = findPreference(KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM);
        mDeleteOccasionalAlarmByDefaultPref = findPreference(KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT);
        mDisplayLowAlarmVolumeWarningPref = findPreference(KEY_DISPLAY_LOW_ALARM_VOLUME_WARNING);

        if (savedInstanceState != null) {
            mPendingDialogPrefKey = savedInstanceState.getString(KEY_PENDING_DIALOG_PREF_KEY, null);
        }

        setupPreferences();

        setupFeaturesPreference();

        invalidatePreferencesVisibility();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mAudioManager != null) {
            mAudioManager.registerAudioDeviceCallback(mAudioDeviceCallback, null);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_PENDING_DIALOG_PREF_KEY, mPendingDialogPrefKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupFragmentResultListeners();
    }

    @Override
    public void onResume() {
        super.onResume();

        mHasExternalAudioDeviceConnected = RingtoneUtils.hasExternalAudioDeviceConnected(
            requireContext(), SettingsDAO.isAutoRoutingToExternalAudioDevice(getPrefs()));

        updateExternalAudioDeviceState();

        if (mPendingDialogPrefKey != null && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            triggerDisableSettingDialog(mPendingDialogPrefKey);
        }

        restoreCustomFileDialogIfNeeded(KEY_ALARM_FONT, mAlarmFontPref, fontPickerLauncher, null);

        updateRingtonePreferences();

        invalidatePreferencesVisibility();
    }

    @Override
    public void onStop() {
        super.onStop();

        stopRingtonePreview();

        if (mAudioManager != null) {
            mAudioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
        }
    }

    @Override
    public void onDestroy() {
        mAlarmUpdateHandler = null;

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        final Context appContext = requireContext().getApplicationContext();
        final ContentResolver cr = appContext.getContentResolver();

        switch (pref.getKey()) {
            case KEY_DISPLAY_LOW_ALARM_VOLUME_WARNING, KEY_DISPLAY_ENABLED_ALARMS_FIRST, KEY_DISPLAY_LOCKED_ALARMS_FIRST,
                 KEY_SWITCH_TO_NEW_CATEGORY, KEY_DISPLAY_ENABLED_ALARM_BADGE, KEY_DELETE_ALARMS_WITH_CATEGORY,
                 KEY_ENABLE_ALARM_FAB_LONG_PRESS, KEY_DISPLAY_DISMISS_BUTTON, KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS,
                 KEY_ENABLE_ALARM_SORTING_BY_CATEGORY, KEY_ENABLE_ALARM_PROXIMITY_SENSOR ->
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            case KEY_ADVANCED_AUDIO_PLAYBACK, KEY_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE, KEY_SYSTEM_MEDIA_VOLUME -> {
                stopRingtonePreview();
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_REPEAT_MISSED_ALARM -> {
                final int index = mRepeatMissedAlarmPref.findIndexOfValue((String) newValue);
                mRepeatMissedAlarmPref.setSummary(mRepeatMissedAlarmPref.getEntries()[index]);

                if (!SettingsDAO.getAlarmFeaturesToDisplay(getPrefs()).contains(VISIBLE_MISSED_ALARM_REPEAT_LIMIT)) {
                    final int missedAlarmRepeatLimit = Integer.parseInt((String) newValue);

                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                        for (Alarm alarm : currentAlarms) {
                            if (!alarm.lock) {
                                alarm.missedAlarmRepeatLimit = missedAlarmRepeatLimit;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                }
            }

            case KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                        for (Alarm alarm : currentAlarms) {
                            if (!alarm.lock) {
                                alarm.vibrate = true;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT);
                    return false;
                }
            }

            case KEY_VOLUME_BUTTONS, KEY_POWER_BUTTON, KEY_HEADPHONES_BUTTON, KEY_FLIP_ACTION, KEY_MATERIAL_TIME_PICKER_STYLE,
                 KEY_MATERIAL_DATE_PICKER_STYLE, KEY_SORT_ALARM, KEY_VIBRATION_PATTERN -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_SHAKE_ACTION -> {
                final int index = mShakeActionPref.findIndexOfValue((String) newValue);
                mShakeActionPref.setSummary(mShakeActionPref.getEntries()[index]);
            }

            case KEY_WEEK_START -> {
                final int index = mWeekStartPref.findIndexOfValue((String) newValue);
                mWeekStartPref.setSummary(mWeekStartPref.getEntries()[index]);
            }

            case KEY_ALARM_FEATURES_TO_DISPLAY -> {
                @SuppressWarnings("unchecked")
                Set<String> newSelectedSecondaryFeatures = (Set<String>) newValue;
                Set<String> oldSelectedSecondaryFeatures = SettingsDAO.getAlarmFeaturesToDisplay(getPrefs());

                boolean timeZoneHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_TIME_ZONE)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_TIME_ZONE);

                boolean scheduleHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_SCHEDULE_ALARM)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_SCHEDULE_ALARM);

                boolean pauseHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_PAUSE_ALARM)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_PAUSE_ALARM);

                boolean autoSilenceHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_AUTO_SILENCE)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_AUTO_SILENCE);

                boolean snoozeDurationHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_SNOOZE_DURATION)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_SNOOZE_DURATION);

                boolean missedAlarmRepeatLimitHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_MISSED_ALARM_REPEAT_LIMIT)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_MISSED_ALARM_REPEAT_LIMIT);

                boolean volumeHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_VOLUME)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_VOLUME);

                boolean crescendoDurationHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_CRESCENDO_DURATION)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_CRESCENDO_DURATION);

                boolean vibrationPatternHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_VIBRATION_PATTERN)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_VIBRATION_PATTERN);

                boolean mathHardnessHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_MATH_HARDNESS_LEVEL)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_MATH_HARDNESS_LEVEL);

                boolean backgroundImageHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_BACKGROUND_IMAGE)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_BACKGROUND_IMAGE);

                boolean taskHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_TASK)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_TASK);

                boolean lockHidden = oldSelectedSecondaryFeatures.contains(VISIBLE_LOCK)
                    && !newSelectedSecondaryFeatures.contains(VISIBLE_LOCK);

                if (timeZoneHidden || scheduleHidden || pauseHidden || autoSilenceHidden || snoozeDurationHidden
                    || missedAlarmRepeatLimitHidden || volumeHidden || crescendoDurationHidden || vibrationPatternHidden
                    || mathHardnessHidden || backgroundImageHidden || taskHidden || lockHidden) {

                    final int alarmVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);

                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);

                        boolean imageDeleted = false;

                        for (Alarm alarm : currentAlarms) {
                            boolean alarmChanged = false;

                            // First check if the alarm is locked and that the functionality is not hidden.
                            if (alarm.lock) {
                                if (lockHidden) {
                                    alarm.lock = false;
                                    alarmChanged = true;
                                } else {
                                    continue;
                                }
                            }

                            // IMPORTANT: timeZone must be reset BEFORE schedule/date calculations,
                            // because the time zone directly dictates what the "current date" is.
                            if (timeZoneHidden && !Objects.equals("", alarm.timeZone)) {
                                alarm.timeZone = "";
                                alarmChanged = true;
                            }

                            if (scheduleHidden && alarm.isSpecifiedDate()) {
                                Calendar currentCalendar = Calendar.getInstance(alarm.getTimeZone());
                                alarm.year = currentCalendar.get(Calendar.YEAR);
                                alarm.month = currentCalendar.get(Calendar.MONTH);
                                alarm.day = currentCalendar.get(Calendar.DAY_OF_MONTH);
                                alarmChanged = true;
                            }

                            if (pauseHidden && alarm.isPauseSet()) {
                                alarm.pauseStartDate = 0;
                                alarm.pauseEndDate = 0;
                                alarmChanged = true;
                            }

                            if (autoSilenceHidden && alarm.autoSilenceDuration != SettingsDAO.getAlarmTimeout(getPrefs())) {
                                alarm.autoSilenceDuration = SettingsDAO.getAlarmTimeout(getPrefs());
                                alarmChanged = true;
                            }

                            if (snoozeDurationHidden && alarm.snoozeDuration != SettingsDAO.getSnoozeLength(getPrefs())) {
                                alarm.snoozeDuration = SettingsDAO.getSnoozeLength(getPrefs());
                                alarmChanged = true;
                            }

                            if (missedAlarmRepeatLimitHidden
                                && alarm.missedAlarmRepeatLimit != SettingsDAO.getMissedAlarmRepeatLimit(getPrefs())) {
                                alarm.missedAlarmRepeatLimit = SettingsDAO.getMissedAlarmRepeatLimit(getPrefs());
                                alarmChanged = true;
                            }

                            if (volumeHidden && alarm.alarmVolume != alarmVolume) {
                                alarm.alarmVolume = alarmVolume;
                                alarmChanged = true;
                            }

                            if (crescendoDurationHidden
                                && alarm.crescendoDuration != SettingsDAO.getAlarmVolumeCrescendoDuration(getPrefs())) {
                                alarm.crescendoDuration = SettingsDAO.getAlarmVolumeCrescendoDuration(getPrefs());
                                alarmChanged = true;
                            }

                            if (vibrationPatternHidden
                                && !Objects.equals(alarm.vibrationPattern, SettingsDAO.getVibrationPattern(getPrefs()))) {
                                alarm.vibrationPattern = SettingsDAO.getVibrationPattern(getPrefs());
                                alarmChanged = true;
                            }

                            if (mathHardnessHidden
                                && !Objects.equals(alarm.mathHardnessLevel, SettingsDAO.getAlarmMathHardnessLevel(getPrefs()))) {
                                alarm.mathHardnessLevel = SettingsDAO.getAlarmMathHardnessLevel(getPrefs());
                                alarmChanged = true;
                            }

                            if (backgroundImageHidden
                                && (!Objects.equals(alarm.backgroundImage, SettingsDAO.getAlarmBackgroundImage(getPrefs()))
                                || alarm.blurIntensity != SettingsDAO.getAlarmBlurIntensity(getPrefs()))) {
                                // Delete only specific background images
                                if (!TextUtils.isEmpty(alarm.backgroundImage)
                                    && alarm.backgroundImage.contains(FILE_SPECIFIC_ALARM_BACKGROUND)) {
                                    FileUtils.clearFile(alarm.backgroundImage);
                                    imageDeleted = true;
                                }

                                alarm.backgroundImage = SettingsDAO.getAlarmBackgroundImage(getPrefs());
                                alarm.blurIntensity = SettingsDAO.getAlarmBlurIntensity(getPrefs());
                                alarmChanged = true;
                            }

                            if (taskHidden && !TextUtils.isEmpty(alarm.taskUrl)) {
                                alarm.taskName = "";
                                alarm.taskUrl = "";
                                alarmChanged = true;
                            }

                            if (alarmChanged) {
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, false);
                            }
                        }

                        if (imageDeleted) {
                            AppExecutors.getMainThread().post(() -> CustomToast.show(
                                appContext, getAccentStyle(), getGeneralTypeface(), R.string.background_image_toast_message_deleted)
                            );
                        }
                    });
                }

                updateFeaturesSummaries(newSelectedSecondaryFeatures);
            }

            case KEY_DISABLE_DND_FOR_TRIGGERED_ALARM -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                        for (Alarm alarm : currentAlarms) {
                            if (!alarm.lock) {
                                alarm.disableDND = true;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_DISABLE_DND_FOR_TRIGGERED_ALARM);
                    return false;
                }
            }

            case KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                        for (Alarm alarm : currentAlarms) {
                            if (!alarm.lock) {
                                alarm.flash = true;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM);
                    return false;
                }
            }

            case KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                        for (Alarm alarm : currentAlarms) {
                            if (!alarm.lock) {
                                alarm.deleteAfterUse = true;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT);
                    return false;
                }
            }
        }

        blockSearchResetForAWhile();

        if (getView() != null) {
            getView().post(() -> {
                if (isAdded()) {
                    invalidatePreferencesVisibility();
                }
            });
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

            case KEY_ALARM_FONT -> selectCustomFile(mAlarmFontPref, fontPickerLauncher, SettingsDAO.getAlarmFont(getPrefs()),
                KEY_ALARM_FONT, true, false, null);

            case KEY_DEFAULT_ALARM_RINGTONE -> startActivity(RingtonePickerActivity.createAlarmRingtonePickerIntentForSettings(context));
        }

        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference pref) {
        if (pref instanceof AutoSilenceDurationPreference autoSilenceDurationPreference) {
            int currentValue = autoSilenceDurationPreference.getAutoSilenceDuration();
            AutoSilenceDurationDialogFragment dialogFragment = AutoSilenceDurationDialogFragment.newInstance(pref.getKey(), currentValue);
            AutoSilenceDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof AlarmSnoozeDurationPreference alarmSnoozeDurationPreference) {
            int currentValue = alarmSnoozeDurationPreference.getSnoozeDuration();
            AlarmSnoozeDurationDialogFragment dialogFragment = AlarmSnoozeDurationDialogFragment.newInstance(pref.getKey(), currentValue);
            AlarmSnoozeDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VolumeCrescendoDurationPreference volumeCrescendoDurationPreference) {
            int currentValue = volumeCrescendoDurationPreference.getVolumeCrescendoDuration();
            VolumeCrescendoDurationDialogFragment dialogFragment =
                VolumeCrescendoDurationDialogFragment.newInstance(pref.getKey(), currentValue);
            VolumeCrescendoDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VibrationPatternPreference vibrationPatternPreference) {
            String currentValue = vibrationPatternPreference.getPattern();
            VibrationPatternDialogFragment dialogFragment = VibrationPatternDialogFragment.newInstance(pref.getKey(), currentValue);
            VibrationPatternDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VibrationStartDelayPreference vibrationStartDelayPreference) {
            int currentValue = vibrationStartDelayPreference.getVibrationStartDelay();
            VibrationStartDelayDialogFragment dialogFragment = VibrationStartDelayDialogFragment.newInstance(pref.getKey(), currentValue,
                currentValue == DEFAULT_VIBRATION_START_DELAY);
            VibrationStartDelayDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof AlarmMathHardnessLevelPreference alarmMathHardnessLevelPreference) {
            String currentValue = alarmMathHardnessLevelPreference.getMathHardnessLevel();
            AlarmMathHardnessLevelDialogFragment dialogFragment =
                AlarmMathHardnessLevelDialogFragment.newInstance(pref.getKey(), currentValue);
            AlarmMathHardnessLevelDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof AlarmNotificationReminderPreference alarmNotificationReminderPreference) {
            int currentValue = alarmNotificationReminderPreference.getAlarmNotificationReminderTime();
            AlarmNotificationReminderDialogFragment dialogFragment =
                AlarmNotificationReminderDialogFragment.newInstance(pref.getKey(), currentValue);
            AlarmNotificationReminderDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    @Override
    protected boolean isPreferenceVisible(@NonNull Preference pref, @NonNull String key) {
        SensorManager sensorManager = requireContext().getApplicationContext().getSystemService(SensorManager.class);

        return switch (key) {
            case KEY_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE -> SettingsDAO.isAdvancedAudioPlaybackEnabled(getPrefs());

            case KEY_SYSTEM_MEDIA_VOLUME -> SettingsDAO.isAdvancedAudioPlaybackEnabled(getPrefs())
                && SettingsDAO.isAutoRoutingToExternalAudioDevice(getPrefs());

            case KEY_EXTERNAL_AUDIO_DEVICE_VOLUME -> SettingsDAO.isAdvancedAudioPlaybackEnabled(getPrefs())
                && SettingsDAO.isAutoRoutingToExternalAudioDevice(getPrefs())
                && SettingsDAO.shouldUseCustomMediaVolume(getPrefs());

            case KEY_ALARM_VIBRATION_CATEGORY -> DeviceUtils.hasVibrator(requireContext());

            case KEY_VIBRATION_PATTERN, KEY_VIBRATION_START_DELAY -> SettingsDAO.areAlarmVibrationsEnabledByDefault(getPrefs());

            case KEY_FLIP_ACTION, KEY_SHAKE_ACTION -> sensorManager != null
                && sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;

            case KEY_SHAKE_INTENSITY -> {
                boolean hasAccelerometer = sensorManager != null && sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
                yield hasAccelerometer && SettingsDAO.getShakeAction(getPrefs()) != 0;
            }

            case KEY_DEFAULT_ALARM_CATEGORY -> {
                boolean isCategoryEnabled = SettingsDAO.areAlarmsSortedByCategory(getPrefs());
                boolean hasCustomCategories = mDefaultCategoryPref != null
                    && mDefaultCategoryPref.getEntries() != null
                    && mDefaultCategoryPref.getEntries().length > 1;
                yield isCategoryEnabled && hasCustomCategories;
            }

            case KEY_SWITCH_TO_NEW_CATEGORY, KEY_DISPLAY_ENABLED_ALARM_BADGE, KEY_DELETE_ALARMS_WITH_CATEGORY ->
                SettingsDAO.areAlarmsSortedByCategory(getPrefs());

            case KEY_REPEAT_MISSED_ALARM -> SettingsDAO.getAlarmTimeout(getPrefs()) != TIMEOUT_NEVER
                && SettingsDAO.getSnoozeLength(getPrefs()) != ALARM_SNOOZE_DURATION_DISABLED;

            case KEY_ENABLE_ALARM_PROXIMITY_SENSOR -> sensorManager != null && sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null;

            case KEY_DISABLE_DND_FOR_TRIGGERED_ALARM -> PermissionUtils.isDNDPermissionEnabled(requireContext());

            case KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM -> DeviceUtils.hasBackFlash(requireContext());

            default -> super.isPreferenceVisible(pref, key);
        };
    }

    private void updateExternalAudioDeviceState() {
        mAlarmVolumePref.setEnabled(!mHasExternalAudioDeviceConnected);

        if (mHasExternalAudioDeviceConnected) {
            mAlarmVolumePref.setTitle(R.string.disconnect_external_audio_device_title);
            mExternalAudioDeviceVolumePref.setTitle(R.string.external_audio_device_volume_title);
        } else {
            mAlarmVolumePref.setTitle(R.string.alarm_volume_title);
            mExternalAudioDeviceVolumePref.setTitle(R.string.connect_external_audio_device_title);
        }

        mExternalAudioDeviceVolumePref.setEnabled(mHasExternalAudioDeviceConnected);
    }

    private void setupPreferences() {
        mAlarmDisplayCustomizationPref.setOnPreferenceClickListener(this);

        mAlarmFontPref.setTitle(getString(SettingsDAO.getAlarmFont(getPrefs()) == null
            ? R.string.custom_font_title
            : R.string.custom_font_title_variant));
        mAlarmFontPref.setOnPreferenceClickListener(this);

        mMaterialTimePickerStylePref.setOnPreferenceChangeListener(this);
        mMaterialTimePickerStylePref.setSummary(mMaterialTimePickerStylePref.getEntry());

        mMaterialDatePickerStylePref.setOnPreferenceChangeListener(this);
        mMaterialDatePickerStylePref.setSummary(mMaterialDatePickerStylePref.getEntry());

        mAlarmRingtonePref.setOnPreferenceClickListener(this);

        mRepeatMissedAlarmPref.setOnPreferenceChangeListener(this);
        mRepeatMissedAlarmPref.setSummary(mRepeatMissedAlarmPref.getEntry());

        mAdvancedAudioPlaybackPref.setOnPreferenceChangeListener(this);

        mAutoRoutingToExternalAudioDevicePref.setOnPreferenceChangeListener(this);

        mSystemMediaVolumePref.setOnPreferenceChangeListener(this);

        mEnableAlarmVibrationsByDefaultPref.setOnPreferenceChangeListener(this);

        mEnableSnoozedOrDismissedAlarmVibrationsPref.setOnPreferenceChangeListener(this);

        mVolumeButtonsPref.setOnPreferenceChangeListener(this);
        mVolumeButtonsPref.setSummary(mVolumeButtonsPref.getEntry());

        mPowerButtonPref.setOnPreferenceChangeListener(this);
        mPowerButtonPref.setSummary(mPowerButtonPref.getEntry());

        mHeadphonesButtonPref.setOnPreferenceChangeListener(this);
        mHeadphonesButtonPref.setSummary(mHeadphonesButtonPref.getEntry());

        SensorManager sensorManager = requireContext().getApplicationContext().getSystemService(SensorManager.class);
        if (sensorManager == null || sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            // DEFAULT_SHAKE_ACTION --> Nothing
            mFlipActionPref.setValue(DEFAULT_SHAKE_ACTION);
            mShakeActionPref.setValue(DEFAULT_SHAKE_ACTION);
        } else {
            mFlipActionPref.setSummary(mFlipActionPref.getEntry());
            mFlipActionPref.setOnPreferenceChangeListener(this);
            mShakeActionPref.setSummary(mShakeActionPref.getEntry());
            mShakeActionPref.setOnPreferenceChangeListener(this);
        }

        mSortAlarmPref.setOnPreferenceChangeListener(this);
        mSortAlarmPref.setSummary(mSortAlarmPref.getEntry());

        mDisplayEnabledAlarmsFirstPref.setOnPreferenceChangeListener(this);

        mDisplayLockedAlarmsFirstPref.setOnPreferenceChangeListener(this);

        mEnableAlarmSortingByCategoryPref.setOnPreferenceChangeListener(this);

        setupDefaultCategoryPref();

        mShouldSwitchToNewCategoryPref.setOnPreferenceChangeListener(this);

        mDisplayEnabledAlarmBadgePref.setOnPreferenceChangeListener(this);

        mDeleteAlarmsWithCategoryPref.setOnPreferenceChangeListener(this);

        mFeaturesToDisplayPref.setOnPreferenceChangeListener(this);

        mEnableAlarmFabLongPressPref.setOnPreferenceChangeListener(this);

        // Set the default first day of the week programmatically
        final Weekdays.Order weekdayOrder = SettingsDAO.getWeekdayOrder(getPrefs());
        final Integer firstDay = weekdayOrder.getCalendarDays().get(0);
        final String value = String.valueOf(firstDay);
        final int index = mWeekStartPref.findIndexOfValue(value);
        mWeekStartPref.setValueIndex(index);
        mWeekStartPref.setSummary(mWeekStartPref.getEntries()[index]);
        mWeekStartPref.setOnPreferenceChangeListener(this);

        mDisplayDismissButtonPref.setOnPreferenceChangeListener(this);

        mAlarmProximitySensorPref.setOnPreferenceChangeListener(this);

        mDisableDNDForTriggeredAlarmPref.setOnPreferenceChangeListener(this);

        mTurnOnBackFlashForTriggeredAlarmPref.setOnPreferenceChangeListener(this);

        mDeleteOccasionalAlarmByDefaultPref.setOnPreferenceChangeListener(this);

        mDisplayLowAlarmVolumeWarningPref.setOnPreferenceChangeListener(this);

        updateFeaturesSummaries(SettingsDAO.getAlarmFeaturesToDisplay(getPrefs()));
    }

    private void setupDefaultCategoryPref() {
        mDefaultCategoryPref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

        Context appContext = requireContext().getApplicationContext();
        ContentResolver cr = appContext.getContentResolver();

        AppExecutors.getDiskIO().execute(() -> {
            List<CharSequence> entriesList = new ArrayList<>();
            List<CharSequence> valuesList = new ArrayList<>();

            // Add the "Default" category in the first position
            entriesList.add(getString(R.string.label_default));
            valuesList.add(String.valueOf(Alarm.DEFAULT_CATEGORY_ID));

            // Retrieve custom categories
            try (Cursor cursor = cr.query(
                ClockContract.CategoryColumns.CONTENT_URI,
                new String[]{ClockContract.CategoryColumns._ID, ClockContract.CategoryColumns.NAME},
                null,
                null,
                ClockContract.CategoryColumns.SORT_ORDER + " ASC"
            )) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idIndex = cursor.getColumnIndexOrThrow(ClockContract.CategoryColumns._ID);
                    int nameIndex = cursor.getColumnIndexOrThrow(ClockContract.CategoryColumns.NAME);

                    do {
                        entriesList.add(cursor.getString(nameIndex));
                        valuesList.add(String.valueOf(cursor.getLong(idIndex)));
                    } while (cursor.moveToNext());
                }
            }

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    boolean hasCustomCategories = entriesList.size() > 1;

                    if (hasCustomCategories) {
                        CharSequence[] entries = entriesList.toArray(new CharSequence[0]);
                        CharSequence[] entryValues = valuesList.toArray(new CharSequence[0]);

                        mDefaultCategoryPref.setEntries(entries);
                        mDefaultCategoryPref.setEntryValues(entryValues);

                        // If the category no longer exists, it reverts to "Default".
                        String currentValue = mDefaultCategoryPref.getValue();
                        boolean valueExists = false;
                        for (CharSequence value : entryValues) {
                            if (value.toString().equals(currentValue)) {
                                valueExists = true;
                                break;
                            }
                        }

                        if (!valueExists) {
                            mDefaultCategoryPref.setValue(String.valueOf(Alarm.DEFAULT_CATEGORY_ID));
                        }
                    }

                    invalidatePreferencesVisibility();
                }
            });
        });
    }

    private void setupFeaturesPreference() {
        boolean hasFlash = DeviceUtils.hasBackFlash(requireContext());
        boolean hasVibrator = DeviceUtils.hasVibrator(requireContext());

        if (!hasFlash || !hasVibrator) {
            CharSequence[] currentEntries = mFeaturesToDisplayPref.getEntries();
            CharSequence[] currentValues = mFeaturesToDisplayPref.getEntryValues();

            if (currentEntries != null && currentValues != null) {
                List<CharSequence> newEntries = new ArrayList<>();
                List<CharSequence> newValues = new ArrayList<>();

                for (int i = 0; i < currentValues.length; i++) {
                    String value = currentValues[i].toString();

                    boolean skipFlash = !hasFlash && VISIBLE_ALARM_FLASH.equals(value);
                    boolean skipVibrate = !hasVibrator && VISIBLE_ALARM_VIBRATE.equals(value);

                    if (!skipFlash && !skipVibrate) {
                        newEntries.add(currentEntries[i]);
                        newValues.add(currentValues[i]);
                    }
                }

                mFeaturesToDisplayPref.setEntries(newEntries.toArray(new CharSequence[0]));
                mFeaturesToDisplayPref.setEntryValues(newValues.toArray(new CharSequence[0]));
            }
        }

        sortMultiSelectListPreference(mFeaturesToDisplayPref);
    }

    private void sortMultiSelectListPreference(@Nullable MultiSelectListPreference multiSelectListPreference) {
        if (multiSelectListPreference != null) {
            CharSequence[] entries = multiSelectListPreference.getEntries();
            CharSequence[] values = multiSelectListPreference.getEntryValues();

            if (entries != null && values != null && entries.length > 1) {
                List<Pair> entryValuePairs = new ArrayList<>();

                for (int i = 0; i < entries.length; i++) {
                    entryValuePairs.add(new Pair(entries[i], values[i]));
                }

                Collections.sort(entryValuePairs, (pair1, pair2) ->
                    String.CASE_INSENSITIVE_ORDER.compare(pair1.entry().toString(), pair2.entry().toString())
                );

                CharSequence[] sortedEntries = new CharSequence[entries.length];
                CharSequence[] sortedValues = new CharSequence[values.length];

                for (int i = 0; i < entryValuePairs.size(); i++) {
                    sortedEntries[i] = entryValuePairs.get(i).entry();
                    sortedValues[i] = entryValuePairs.get(i).value();
                }

                multiSelectListPreference.setEntries(sortedEntries);
                multiSelectListPreference.setEntryValues(sortedValues);
            }
        }
    }

    private void updateFeaturesSummaries(@NonNull Set<String> selectedFeatures) {
        String generalSummary = getString(R.string.alarm_settings_general_summary);

        mEnableAlarmVibrationsByDefaultPref.setSummary(
            selectedFeatures.contains(VISIBLE_ALARM_VIBRATE) ? generalSummary : null
        );

        mTurnOnBackFlashForTriggeredAlarmPref.setSummary(
            selectedFeatures.contains(VISIBLE_ALARM_FLASH) ? generalSummary : null
        );

        mDeleteOccasionalAlarmByDefaultPref.setSummary(
            selectedFeatures.contains(VISIBLE_DELETE_OCCASIONAL_ALARM) ? generalSummary : null
        );

        mDisableDNDForTriggeredAlarmPref.setSummary(
            selectedFeatures.contains(VISIBLE_DISABLE_DND) ? generalSummary : null
        );
    }

    private void setupFragmentResultListeners() {
        final FragmentManager parentFragmentManager = getParentFragmentManager();
        final LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        final Context appContext = requireContext().getApplicationContext();
        final ContentResolver cr = appContext.getContentResolver();

        // Alarm auto silence duration preference
        parentFragmentManager.setFragmentResultListener(AutoSilenceDurationDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(AutoSilenceDurationDialogFragment.RESULT_PREF_KEY);
                int newValue = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);

                if (key != null) {
                    AutoSilenceDurationPreference pref = findPreference(key);
                    if (pref != null) {
                        pref.setAutoSilenceDuration(newValue);

                        blockSearchResetForAWhile();

                        if (getView() != null) {
                            getView().post(this::invalidatePreferencesVisibility);
                        }

                        if (!SettingsDAO.getAlarmFeaturesToDisplay(getPrefs()).contains(VISIBLE_AUTO_SILENCE)) {
                            AppExecutors.getDiskIO().execute(() -> {
                                List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                                for (Alarm alarm : currentAlarms) {
                                    if (!alarm.lock) {
                                        alarm.autoSilenceDuration = newValue;
                                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                                    }
                                }
                            });
                        }
                    }
                }
            });

        // Alarm snooze duration preference
        parentFragmentManager.setFragmentResultListener(AlarmSnoozeDurationDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(AlarmSnoozeDurationDialogFragment.RESULT_PREF_KEY);
                int newValue = bundle.getInt(AlarmSnoozeDurationDialogFragment.ALARM_SNOOZE_DURATION_VALUE);

                if (key != null) {
                    AlarmSnoozeDurationPreference pref = findPreference(key);
                    if (pref != null) {
                        pref.setSnoozeDuration(newValue);

                        blockSearchResetForAWhile();

                        if (getView() != null) {
                            getView().post(this::invalidatePreferencesVisibility);
                        }

                        if (!SettingsDAO.getAlarmFeaturesToDisplay(getPrefs()).contains(VISIBLE_SNOOZE_DURATION)) {
                            AppExecutors.getDiskIO().execute(() -> {
                                List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                                for (Alarm alarm : currentAlarms) {
                                    if (!alarm.lock) {
                                        alarm.snoozeDuration = newValue;
                                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                                    }
                                }
                            });
                        }
                    }
                }
            });

        // Alarm volume crescendo duration preference
        parentFragmentManager.setFragmentResultListener(VolumeCrescendoDurationDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(VolumeCrescendoDurationDialogFragment.RESULT_PREF_KEY);
                int newValue = bundle.getInt(VolumeCrescendoDurationDialogFragment.VOLUME_CRESCENDO_DURATION_VALUE);

                if (key != null) {
                    VolumeCrescendoDurationPreference pref = findPreference(key);
                    if (pref != null) {
                        pref.setVolumeCrescendoDuration(newValue);

                        if (!SettingsDAO.getAlarmFeaturesToDisplay(getPrefs()).contains(VISIBLE_CRESCENDO_DURATION)) {
                            AppExecutors.getDiskIO().execute(() -> {
                                List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                                for (Alarm alarm : currentAlarms) {
                                    if (!alarm.lock) {
                                        alarm.crescendoDuration = newValue;
                                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                                    }
                                }
                            });
                        }
                    }
                }
            });

        // Vibration pattern preference
        parentFragmentManager.setFragmentResultListener(VibrationPatternDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(VibrationPatternDialogFragment.RESULT_PREF_KEY);
                String newValue = bundle.getString(VibrationPatternDialogFragment.RESULT_PATTERN_KEY);

                if (key != null && newValue != null) {
                    VibrationPatternPreference pref = findPreference(key);
                    if (pref != null) {
                        pref.setPattern(newValue);

                        if (!SettingsDAO.getAlarmFeaturesToDisplay(getPrefs()).contains(VISIBLE_VIBRATION_PATTERN)) {
                            AppExecutors.getDiskIO().execute(() -> {
                                List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                                for (Alarm alarm : currentAlarms) {
                                    if (!alarm.lock) {
                                        alarm.vibrationPattern = newValue;
                                        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                                    }
                                }
                            });
                        }
                    }
                }
            });

        // Vibration start delay preference
        parentFragmentManager.setFragmentResultListener(VibrationStartDelayDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(VibrationStartDelayDialogFragment.RESULT_PREF_KEY);
                int newValue = bundle.getInt(VibrationStartDelayDialogFragment.VIBRATION_DELAY_VALUE);

                if (key != null) {
                    VibrationStartDelayPreference pref = findPreference(key);
                    if (pref != null) {
                        pref.setVibrationStartDelay(newValue);
                    }
                }
            });

        // Math hardness preference
        parentFragmentManager.setFragmentResultListener(AlarmMathHardnessLevelDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(AlarmMathHardnessLevelDialogFragment.RESULT_PREF_KEY);
                String newValue = bundle.getString(AlarmMathHardnessLevelDialogFragment.RESULT_MATH_HARDNESS_LEVEL);

                if (key != null && newValue != null) {
                    AlarmMathHardnessLevelPreference pref = findPreference(key);
                    if (pref != null) {
                        pref.setMathHardnessLevel(newValue);
                    }

                    if (!SettingsDAO.getAlarmFeaturesToDisplay(getPrefs()).contains(VISIBLE_MATH_HARDNESS_LEVEL)) {
                        AppExecutors.getDiskIO().execute(() -> {
                            List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                            for (Alarm alarm : currentAlarms) {
                                if (!alarm.lock) {
                                    alarm.mathHardnessLevel = newValue;
                                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                                }
                            }
                        });
                    }
                }
            });

        // Notification reminder preference
        parentFragmentManager.setFragmentResultListener(AlarmNotificationReminderDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(AlarmNotificationReminderDialogFragment.RESULT_PREF_KEY);
                int newValue = bundle.getInt(AlarmNotificationReminderDialogFragment.ALARM_NOTIFICATION_REMINDER_VALUE);

                if (key != null) {
                    AlarmNotificationReminderPreference pref = findPreference(key);
                    if (pref != null) {
                        pref.setAlarmNotificationReminderTime(newValue);

                        AppExecutors.getDiskIO().execute(() -> {
                            List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                            for (Alarm alarm : currentAlarms) {
                                if (alarm.enabled) {
                                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, false);
                                }
                            }
                        });
                    }
                }
            });
    }

    private void triggerDisableSettingDialog(@NonNull String prefKey) {
        final Context appContext = requireContext().getApplicationContext();
        final ContentResolver cr = appContext.getContentResolver();

        AppExecutors.getDiskIO().execute(() -> {
            boolean hasAlarms = false;

            try (Cursor cursor = cr.query(
                Alarm.CONTENT_URI,
                new String[]{Alarm._ID},
                null,
                null,
                null)) {

                hasAlarms = cursor != null && cursor.getCount() > 0;

            } catch (Exception e) {
                LogUtils.e("Error checking for the presence of alarms", e);
            }

            final boolean finalHasAlarms = hasAlarms;

            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || isDetached()) {
                    return;
                }

                if (finalHasAlarms) {
                    mPendingDialogPrefKey = prefKey;

                    switch (prefKey) {
                        case KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT ->
                            showDisablePerAlarmSettingDialog(R.string.enable_alarm_vibrations_by_default_dialog_message,
                                KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, mEnableAlarmVibrationsByDefaultPref,
                                alarm -> alarm.vibrate = false);

                        case KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT ->
                            showDisablePerAlarmSettingDialog(R.string.enable_delete_occasional_alarm_by_default_dialog_message,
                                KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT, mDeleteOccasionalAlarmByDefaultPref,
                                alarm -> alarm.deleteAfterUse = false);

                        case KEY_DISABLE_DND_FOR_TRIGGERED_ALARM ->
                            showDisablePerAlarmSettingDialog(R.string.disable_dnd_for_triggered_alarm_dialog_message,
                                KEY_DISABLE_DND_FOR_TRIGGERED_ALARM, mDisableDNDForTriggeredAlarmPref,
                                alarm -> alarm.disableDND = false);

                        case KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM ->
                            showDisablePerAlarmSettingDialog(R.string.turn_on_back_flash_for_triggered_alarm_dialog_message,
                                KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM, mTurnOnBackFlashForTriggeredAlarmPref,
                                alarm -> alarm.flash = false);
                    }
                } else {
                    getPrefs().edit().putBoolean(prefKey, false).apply();

                    Preference pref = findPreference(prefKey);
                    if (pref instanceof SwitchPreferenceCompat switchPreferenceCompat) {
                        switchPreferenceCompat.setChecked(false);
                    }
                }
            });
        });
    }

    private void showDisablePerAlarmSettingDialog(@StringRes int messageResId, @NonNull String prefKey,
                                                  @NonNull SwitchPreferenceCompat switchPref, @NonNull AlarmUpdater alarmUpdater) {

        String confirmAction = getString(R.string.confirm_action_prompt);

        mActiveDialog = CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error),
            getString(R.string.warning),
            getString(messageResId, confirmAction),
            null,
            getString(android.R.string.ok),
            (d, w) -> {
                final Context appContext = requireContext().getApplicationContext();
                final ContentResolver cr = appContext.getContentResolver();

                AppExecutors.getDiskIO().execute(() -> {
                    List<Alarm> currentAlarms = Alarm.getAlarms(cr, null);
                    for (Alarm alarm : currentAlarms) {
                        if (!alarm.lock) {
                            alarmUpdater.update(alarm);
                            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                        }
                    }
                });

                getPrefs().edit().putBoolean(prefKey, false).apply();

                switchPref.setChecked(false);

                if (KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT.equals(prefKey)) {
                    mVibrationPatternPref.setVisible(false);
                    mVibrationStartDelayPref.setVisible(false);
                }
            },
            getString(android.R.string.cancel),
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mPendingDialogPrefKey = null)),
            CustomDialog.SoftInputMode.NONE
        );

        mActiveDialog.show();
    }

    private void stopRingtonePreview() {
        if (mHasExternalAudioDeviceConnected) {
            mExternalAudioDeviceVolumePref.stopRingtonePreviewForExternalAudioDevices();
        } else {
            mAlarmVolumePref.stopRingtonePreview();
        }
    }

    private void updateRingtonePreferences() {
        mAlarmRingtonePref.setSummary(getDataModel().getAlarmRingtoneTitle());
        mAlarmRingtonePref.setIntent(RingtonePickerActivity.createAlarmRingtonePickerIntentForSettings(requireContext()));
    }

    /**
     * Interface for updating alarm properties when pressing the OK button in the dialog box
     * that appears when the "per alarm" settings are disabled.
     */
    private interface AlarmUpdater {
        void update(@NonNull Alarm alarm);
    }

    /**
     * Internal class to store entry/value pairs
     */
    private record Pair(@NonNull CharSequence entry, @NonNull CharSequence value) {
    }

}
