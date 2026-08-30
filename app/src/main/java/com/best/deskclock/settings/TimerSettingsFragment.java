// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.base.BaseSettingsScreenFragment;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.dialogfragment.AutoSilenceDurationDialogFragment;
import com.best.deskclock.dialogfragment.TimerAddTimeButtonDialogFragment;
import com.best.deskclock.dialogfragment.VibrationPatternDialogFragment;
import com.best.deskclock.dialogfragment.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.settings.custompreference.AlarmVolumePreference;
import com.best.deskclock.settings.custompreference.AutoSilenceDurationPreference;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.settings.custompreference.TimerAddTimeButtonValuePreference;
import com.best.deskclock.settings.custompreference.VibrationPatternPreference;
import com.best.deskclock.settings.custompreference.VolumeCrescendoDurationPreference;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class TimerSettingsFragment extends BaseSettingsScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String KEY_SHOW_SINGLE_TIMER_WARNING = "show_single_timer_warning";
    private static final String KEY_PENDING_SINGLE_MODE_VALUE = "pending_single_mode_value";
    private static final String KEY_PENDING_DIALOG_PREF_KEY = "pending_dialog_pref_key";

    private boolean mShowSingleTimerWarning = false;
    private boolean mPendingSingleModeValue = false;
    private String mPendingDialogPrefKey = null;

    private AudioManager mAudioManager;
    private AudioDeviceCallback mAudioDeviceCallback;

    private boolean mHasExternalAudioDeviceConnected;
    private boolean mIsAlarmTabHidden;

    Preference mTimerDisplayCustomizationPref;
    Preference mTimerDurationFontPref;
    ListPreference mTimerCreationViewStylePref;
    Preference mTimerRingtonePref;
    SwitchPreferenceCompat mEnablePerTimerAutoSilencePref;
    AlarmVolumePreference mAlarmVolumePref;
    SwitchPreferenceCompat mEnablePerTimerVolumeCrescendoDurationPref;
    PreferenceCategory mAdvancedAudioPlaybackCategoryPref;
    SwitchPreferenceCompat mAdvancedAudioPlaybackPref;
    SwitchPreferenceCompat mAutoRoutingToExternalAudioDevicePref;
    SwitchPreferenceCompat mSystemMediaVolumePref;
    CustomSliderPreference mExternalAudioDeviceVolumePref;
    PreferenceCategory mTimerVibrationCategory;
    SwitchPreferenceCompat mTimerVibratePref;
    SwitchPreferenceCompat mEnablePerTimerVibrationPatternPref;
    SwitchPreferenceCompat mTimerVolumeButtonsActionPref;
    SwitchPreferenceCompat mTimerPowerButtonActionPref;
    SwitchPreferenceCompat mTimerHeadphonesButtonActionPref;
    SwitchPreferenceCompat mTimerFlipActionPref;
    SwitchPreferenceCompat mTimerShakeActionPref;
    CustomSliderPreference mTimerShakeIntensityPref;
    SwitchPreferenceCompat mSingleTimerModePref;
    ListPreference mSortTimerPref;
    SwitchPreferenceCompat mTurnOnBackFlashForExpiredTimerPref;
    SwitchPreferenceCompat mDisplayLowAlarmVolumeWarningPref;

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

            final Context appContext = requireContext().getApplicationContext();
            final int style = getAccentStyle();
            final Typeface font = getGeneralTypeface();
            final SharedPreferences prefs = getPrefs();

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = FileUtils.toSafeFileName(FILE_TIMER_FONT);
            String oldFontPath = prefs.getString(KEY_TIMER_DURATION_FONT, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old font if it exists
                FileUtils.clearFile(oldFontPath);

                // Clear the font cache
                ThemeUtils.removeFontFromCache(oldFontPath);

                // Copy the new font to the device's protected storage
                Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    prefs.edit().putString(KEY_TIMER_DURATION_FONT, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, style, font, R.string.custom_font_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, style, font, R.string.font_message_error);
                    }

                    if (!isAdded() || mTimerDurationFontPref == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        mTimerDurationFontPref.setTitle(getString(R.string.custom_font_title_variant));
                    } else {
                        mTimerDurationFontPref.setTitle(getString(R.string.custom_font_title));
                    }
                });
            });
        });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.timer_settings);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_timer);

        mTimerDisplayCustomizationPref = findPreference(KEY_TIMER_DISPLAY_CUSTOMIZATION);
        mTimerDurationFontPref = findPreference(KEY_TIMER_DURATION_FONT);
        mTimerCreationViewStylePref = findPreference(KEY_TIMER_CREATION_VIEW_STYLE);
        mTimerRingtonePref = findPreference(KEY_TIMER_RINGTONE);
        mEnablePerTimerAutoSilencePref = findPreference(KEY_ENABLE_PER_TIMER_AUTO_SILENCE);
        mAlarmVolumePref = findPreference(KEY_ALARM_VOLUME_SETTING);
        mEnablePerTimerVolumeCrescendoDurationPref = findPreference(KEY_ENABLE_PER_TIMER_VOLUME_CRESCENDO_DURATION);
        mAdvancedAudioPlaybackCategoryPref = findPreference(KEY_TIMER_ADVANCED_AUDIO_PLAYBACK_CATEGORY);
        mAdvancedAudioPlaybackPref = findPreference(KEY_ADVANCED_AUDIO_PLAYBACK);
        mAutoRoutingToExternalAudioDevicePref = findPreference(KEY_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE);
        mSystemMediaVolumePref = findPreference(KEY_SYSTEM_MEDIA_VOLUME);
        mExternalAudioDeviceVolumePref = findPreference(KEY_EXTERNAL_AUDIO_DEVICE_VOLUME);
        mTimerVibrationCategory = findPreference(KEY_TIMER_VIBRATION_CATEGORY);
        mTimerVibratePref = findPreference(KEY_TIMER_VIBRATE);
        mEnablePerTimerVibrationPatternPref = findPreference(KEY_ENABLE_PER_TIMER_VIBRATION_PATTERN);
        mTimerVolumeButtonsActionPref = findPreference(KEY_TIMER_VOLUME_BUTTONS_ACTION);
        mTimerPowerButtonActionPref = findPreference(KEY_TIMER_POWER_BUTTON_ACTION);
        mTimerHeadphonesButtonActionPref = findPreference(KEY_TIMER_HEADPHONES_BUTTON_ACTION);
        mTimerFlipActionPref = findPreference(KEY_TIMER_FLIP_ACTION);
        mTimerShakeActionPref = findPreference(KEY_TIMER_SHAKE_ACTION);
        mTimerShakeIntensityPref = findPreference(KEY_TIMER_SHAKE_INTENSITY);
        mSingleTimerModePref = findPreference(KEY_SINGLE_TIMER_MODE);
        mSortTimerPref = findPreference(KEY_SORT_TIMER);
        mTurnOnBackFlashForExpiredTimerPref = findPreference(KEY_TURN_ON_BACK_FLASH_FOR_EXPIRED_TIMER);
        mDisplayLowAlarmVolumeWarningPref = findPreference(KEY_DISPLAY_LOW_ALARM_VOLUME_WARNING);

        mIsAlarmTabHidden = !SettingsDAO.isAlarmTabVisible(getPrefs());

        if (mIsAlarmTabHidden) {
            mAudioManager = requireContext().getApplicationContext().getSystemService(AudioManager.class);
            mHasExternalAudioDeviceConnected = RingtoneUtils.hasExternalAudioDeviceConnected(
                requireContext(), SettingsDAO.isAutoRoutingToExternalAudioDevice(getPrefs()));
        }

        if (savedInstanceState != null) {
            mShowSingleTimerWarning = savedInstanceState.getBoolean(KEY_SHOW_SINGLE_TIMER_WARNING, false);
            mPendingSingleModeValue = savedInstanceState.getBoolean(KEY_PENDING_SINGLE_MODE_VALUE, false);
            mPendingDialogPrefKey = savedInstanceState.getString(KEY_PENDING_DIALOG_PREF_KEY, null);
        }

        setupPreferences();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_SHOW_SINGLE_TIMER_WARNING, mShowSingleTimerWarning);
        outState.putBoolean(KEY_PENDING_SINGLE_MODE_VALUE, mPendingSingleModeValue);
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

        if (mActiveDialog == null || !mActiveDialog.isShowing()) {
            if (mShowSingleTimerWarning) {
                mActiveDialog = singleModeWarningDialog(mPendingSingleModeValue);
                mActiveDialog.show();
            } else if (mPendingDialogPrefKey != null) {
                triggerDisableSettingDialog(mPendingDialogPrefKey);
            }
        }

        restoreCustomFileDialogIfNeeded(KEY_TIMER_DURATION_FONT, mTimerDurationFontPref, fontPickerLauncher, null);

        updateRingtonePreferences();

        if (mIsAlarmTabHidden) {
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
    }

    @Override
    public void onStop() {
        super.onStop();

        stopRingtonePreview();

        if (mIsAlarmTabHidden && mAudioDeviceCallback != null) {
            mAudioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
            mAudioDeviceCallback = null;
        }
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mTimerDisplayCustomizationPref, mTimerDurationFontPref, mTimerCreationViewStylePref, mTimerRingtonePref,
            mEnablePerTimerAutoSilencePref, mAlarmVolumePref, mEnablePerTimerVolumeCrescendoDurationPref, mAdvancedAudioPlaybackPref,
            mAutoRoutingToExternalAudioDevicePref, mSystemMediaVolumePref, mExternalAudioDeviceVolumePref, mTimerVibrationCategory,
            mTimerVibratePref, mEnablePerTimerVibrationPatternPref, mTimerVolumeButtonsActionPref, mTimerPowerButtonActionPref,
            mTimerHeadphonesButtonActionPref, mTimerFlipActionPref, mTimerShakeActionPref, mTimerShakeIntensityPref, mSortTimerPref,
            mTurnOnBackFlashForExpiredTimerPref, mDisplayLowAlarmVolumeWarningPref);

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_TIMER_CREATION_VIEW_STYLE, KEY_TIMER_VIBRATION_PATTERN, KEY_SORT_TIMER -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_TIMER_RINGTONE -> mTimerRingtonePref.setSummary(getDataModel().getTimerRingtoneTitle());

            case KEY_ENABLE_PER_TIMER_AUTO_SILENCE -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                List<Timer> timerList = getDataModel().getTimers();

                if ((boolean) newValue) {
                    for (Timer timer : timerList) {
                        getDataModel().updateAllTimerSettings(
                            timer,
                            timer.getLabel(),
                            timer.getButtonTime(),
                            timer.getRingtoneUri(),
                            SettingsDAO.getTimerAutoSilenceDuration(getPrefs()),
                            timer.getVolumeCrescendoDuration(),
                            timer.isVibrate(),
                            timer.getVibrationPattern(),
                            timer.isFlashOn(),
                            timer.getTurnOffMedia(),
                            timer.getDeleteAfterUse()
                        );
                    }
                } else {
                    triggerDisableSettingDialog(KEY_ENABLE_PER_TIMER_AUTO_SILENCE);
                    return false;
                }
            }

            case KEY_ENABLE_PER_TIMER_VOLUME_CRESCENDO_DURATION -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                List<Timer> timerList = getDataModel().getTimers();

                if ((boolean) newValue) {
                    for (Timer timer : timerList) {
                        getDataModel().updateAllTimerSettings(
                            timer,
                            timer.getLabel(),
                            timer.getButtonTime(),
                            timer.getRingtoneUri(),
                            timer.getAutoSilence(),
                            SettingsDAO.getTimerVolumeCrescendoDuration(getPrefs()),
                            timer.isVibrate(),
                            timer.getVibrationPattern(),
                            timer.isFlashOn(),
                            timer.getTurnOffMedia(),
                            timer.getDeleteAfterUse()
                        );
                    }
                } else {
                    triggerDisableSettingDialog(KEY_ENABLE_PER_TIMER_VOLUME_CRESCENDO_DURATION);
                    return false;
                }
            }

            case KEY_ADVANCED_AUDIO_PLAYBACK -> {
                stopRingtonePreview();

                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isAdvancedAudioPlaybackEnabled = (boolean) newValue;

                mAutoRoutingToExternalAudioDevicePref.setVisible(isAdvancedAudioPlaybackEnabled);
                mSystemMediaVolumePref.setVisible(isAdvancedAudioPlaybackEnabled
                    && SettingsDAO.isAutoRoutingToExternalAudioDevice(getPrefs()));
                mExternalAudioDeviceVolumePref.setVisible(isAdvancedAudioPlaybackEnabled
                    && SettingsDAO.isAutoRoutingToExternalAudioDevice(getPrefs())
                    && SettingsDAO.shouldUseCustomMediaVolume(getPrefs()));
            }

            case KEY_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE -> {
                stopRingtonePreview();

                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isAutoRoutingToExternalAudioDevice = (boolean) newValue;

                mSystemMediaVolumePref.setVisible(isAutoRoutingToExternalAudioDevice);
                mExternalAudioDeviceVolumePref.setVisible(isAutoRoutingToExternalAudioDevice
                    && SettingsDAO.shouldUseCustomMediaVolume(getPrefs())
                );
            }

            case KEY_SYSTEM_MEDIA_VOLUME -> {
                stopRingtonePreview();
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mExternalAudioDeviceVolumePref.setVisible(!(boolean) newValue);
            }

            case KEY_TIMER_SHAKE_ACTION -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mTimerShakeIntensityPref.setVisible((boolean) newValue);
            }

            case KEY_TIMER_VIBRATE -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                List<Timer> timerList = getDataModel().getTimers();

                if ((boolean) newValue) {
                    for (Timer timer : timerList) {
                        getDataModel().updateAllTimerSettings(
                            timer,
                            timer.getLabel(),
                            timer.getButtonTime(),
                            timer.getRingtoneUri(),
                            timer.getAutoSilence(),
                            timer.getVolumeCrescendoDuration(),
                            true,
                            timer.getVibrationPattern(),
                            timer.isFlashOn(),
                            timer.getTurnOffMedia(),
                            timer.getDeleteAfterUse()
                        );
                    }
                } else {
                    triggerDisableSettingDialog(KEY_TIMER_VIBRATE);
                    return false;
                }
            }

            case KEY_ENABLE_PER_TIMER_VIBRATION_PATTERN -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                List<Timer> timerList = getDataModel().getTimers();

                if ((boolean) newValue) {
                    for (Timer timer : timerList) {
                        getDataModel().updateAllTimerSettings(
                            timer,
                            timer.getLabel(),
                            timer.getButtonTime(),
                            timer.getRingtoneUri(),
                            timer.getAutoSilence(),
                            timer.getVolumeCrescendoDuration(),
                            timer.isVibrate(),
                            SettingsDAO.getVibrationPattern(getPrefs()),
                            timer.isFlashOn(),
                            timer.getTurnOffMedia(),
                            timer.getDeleteAfterUse()
                        );
                    }
                } else {
                    triggerDisableSettingDialog(KEY_ENABLE_PER_TIMER_VIBRATION_PATTERN);
                    return false;
                }
            }

            case KEY_SINGLE_TIMER_MODE -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean newValueBool = (boolean) newValue;

                if (getDataModel().getTimers().isEmpty()) {
                    mSortTimerPref.setVisible(!newValueBool);
                } else {
                    mShowSingleTimerWarning = true;
                    mPendingSingleModeValue = newValueBool;

                    mActiveDialog = singleModeWarningDialog(newValueBool);
                    mActiveDialog.show();

                    return false;
                }
            }

            case KEY_TURN_ON_BACK_FLASH_FOR_EXPIRED_TIMER -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                List<Timer> timerList = getDataModel().getTimers();

                for (Timer timer : timerList) {
                    getDataModel().updateAllTimerSettings(
                        timer,
                        timer.getLabel(),
                        timer.getButtonTime(),
                        timer.getRingtoneUri(),
                        timer.getAutoSilence(),
                        timer.getVolumeCrescendoDuration(),
                        timer.isVibrate(),
                        timer.getVibrationPattern(),
                        (boolean) newValue,
                        timer.getTurnOffMedia(),
                        timer.getDeleteAfterUse()
                    );
                }
            }

            case KEY_TIMER_VOLUME_BUTTONS_ACTION, KEY_TIMER_POWER_BUTTON_ACTION, KEY_TIMER_HEADPHONES_BUTTON_ACTION, KEY_TIMER_FLIP_ACTION,
                 KEY_DISPLAY_LOW_ALARM_VOLUME_WARNING ->
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
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
            case KEY_TIMER_DISPLAY_CUSTOMIZATION -> animateAndShowFragment(new TimerDisplayCustomizationFragment());

            case KEY_TIMER_DURATION_FONT -> selectCustomFile(mTimerDurationFontPref, fontPickerLauncher,
                SettingsDAO.getTimerDurationFont(getPrefs()), KEY_TIMER_DURATION_FONT, true, null);

            case KEY_TIMER_RINGTONE -> startActivity(RingtonePickerActivity.createTimerRingtonePickerIntentForSettings(context));
        }

        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference pref) {
        if (pref instanceof AutoSilenceDurationPreference autoSilenceDurationPreference) {
            int currentValue = autoSilenceDurationPreference.getAutoSilenceDuration();
            AutoSilenceDurationDialogFragment dialogFragment = AutoSilenceDurationDialogFragment.newInstance(pref.getKey(), currentValue);
            AutoSilenceDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VolumeCrescendoDurationPreference volumeCrescendoDurationPreference) {
            int currentDelay = volumeCrescendoDurationPreference.getVolumeCrescendoDuration();
            VolumeCrescendoDurationDialogFragment dialogFragment =
                VolumeCrescendoDurationDialogFragment.newInstance(pref.getKey(), currentDelay);
            VolumeCrescendoDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VibrationPatternPreference vibrationPatternPreference) {
            String currentValue = vibrationPatternPreference.getPattern();
            VibrationPatternDialogFragment dialogFragment = VibrationPatternDialogFragment.newInstance(pref.getKey(), currentValue);
            VibrationPatternDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof TimerAddTimeButtonValuePreference timerAddTimeButtonValuePreference) {
            int currentValue = timerAddTimeButtonValuePreference.getAddTimeButtonValue();
            TimerAddTimeButtonDialogFragment dialogFragment = TimerAddTimeButtonDialogFragment.newInstance(pref.getKey(), currentValue);
            TimerAddTimeButtonDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    private void setupPreferences() {
        final boolean isAdvancedAudioPlaybackEnabled = SettingsDAO.isAdvancedAudioPlaybackEnabled(getPrefs());
        final boolean isAutoRoutingToExternalAudioDevice = SettingsDAO.isAutoRoutingToExternalAudioDevice(getPrefs());

        mTimerDisplayCustomizationPref.setOnPreferenceClickListener(this);

        mTimerDurationFontPref.setTitle(getString(SettingsDAO.getTimerDurationFont(getPrefs()) == null
            ? R.string.custom_font_title
            : R.string.custom_font_title_variant));
        mTimerDurationFontPref.setOnPreferenceClickListener(this);

        mTimerCreationViewStylePref.setOnPreferenceChangeListener(this);
        mTimerCreationViewStylePref.setSummary(mTimerCreationViewStylePref.getEntry());

        mTimerRingtonePref.setOnPreferenceClickListener(this);

        mEnablePerTimerAutoSilencePref.setOnPreferenceChangeListener(this);

        mAlarmVolumePref.setVisible(mIsAlarmTabHidden);
        if (mAlarmVolumePref.isVisible()) {
            mAlarmVolumePref.setEnabled(!mHasExternalAudioDeviceConnected);
        }

        mEnablePerTimerVolumeCrescendoDurationPref.setOnPreferenceChangeListener(this);

        mAdvancedAudioPlaybackCategoryPref.setVisible(mIsAlarmTabHidden);

        mAdvancedAudioPlaybackPref.setOnPreferenceChangeListener(this);

        mAutoRoutingToExternalAudioDevicePref.setVisible(isAdvancedAudioPlaybackEnabled);
        mAutoRoutingToExternalAudioDevicePref.setOnPreferenceChangeListener(this);

        mSystemMediaVolumePref.setVisible(isAdvancedAudioPlaybackEnabled && isAutoRoutingToExternalAudioDevice);
        mSystemMediaVolumePref.setOnPreferenceChangeListener(this);

        mExternalAudioDeviceVolumePref.setVisible(isAdvancedAudioPlaybackEnabled
            && isAutoRoutingToExternalAudioDevice
            && SettingsDAO.shouldUseCustomMediaVolume(getPrefs()));
        mExternalAudioDeviceVolumePref.setEnabled(mExternalAudioDeviceVolumePref.isVisible() && mHasExternalAudioDeviceConnected);

        mTimerVibrationCategory.setVisible(DeviceUtils.hasVibrator(requireContext()));

        mTimerVibratePref.setOnPreferenceChangeListener(this);

        mEnablePerTimerVibrationPatternPref.setOnPreferenceChangeListener(this);

        mTimerVolumeButtonsActionPref.setOnPreferenceChangeListener(this);

        mTimerPowerButtonActionPref.setOnPreferenceChangeListener(this);

        mTimerHeadphonesButtonActionPref.setVisible(SettingsDAO.isAdvancedAudioPlaybackEnabled(getPrefs())
            && SettingsDAO.isAutoRoutingToExternalAudioDevice(getPrefs()));
        mTimerHeadphonesButtonActionPref.setOnPreferenceChangeListener(this);

        SensorManager sensorManager = requireContext().getApplicationContext().getSystemService(SensorManager.class);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            mTimerFlipActionPref.setChecked(false);
            mTimerShakeActionPref.setChecked(false);
            mTimerFlipActionPref.setVisible(false);
            mTimerShakeActionPref.setVisible(false);
        } else {
            mTimerFlipActionPref.setOnPreferenceChangeListener(this);
            mTimerShakeActionPref.setOnPreferenceChangeListener(this);
            mTimerShakeIntensityPref.setVisible(SettingsDAO.isShakeActionForTimersEnabled(getPrefs()));
        }

        mSingleTimerModePref.setOnPreferenceChangeListener(this);

        mSortTimerPref.setVisible(!SettingsDAO.isSingleTimerModeEnabled(getPrefs()));
        mSortTimerPref.setOnPreferenceChangeListener(this);
        mSortTimerPref.setSummary(mSortTimerPref.getEntry());

        mTurnOnBackFlashForExpiredTimerPref.setVisible(DeviceUtils.hasBackFlash(requireContext()));
        mTurnOnBackFlashForExpiredTimerPref.setOnPreferenceChangeListener(this);

        mDisplayLowAlarmVolumeWarningPref.setOnPreferenceChangeListener(this);
    }

    private void setupFragmentResultListeners() {
        FragmentManager parentFragmentManager = getParentFragmentManager();
        LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();

        // Timer auto silence duration preference
        parentFragmentManager.setFragmentResultListener(AutoSilenceDurationDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(AutoSilenceDurationDialogFragment.RESULT_PREF_KEY);
                int newValue = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);

                if (key != null) {
                    AutoSilenceDurationPreference pref = findPreference(key);
                    if (pref != null) {
                        pref.setAutoSilenceDuration(newValue);

                        if (SettingsDAO.isPerTimerAutoSilenceDisabled(getPrefs())) {
                            List<Timer> timerList = getDataModel().getTimers();

                            for (Timer timer : timerList) {
                                getDataModel().updateAllTimerSettings(
                                    timer,
                                    timer.getLabel(),
                                    timer.getButtonTime(),
                                    timer.getRingtoneUri(),
                                    newValue,
                                    timer.getVolumeCrescendoDuration(),
                                    timer.isVibrate(),
                                    timer.getVibrationPattern(),
                                    timer.isFlashOn(),
                                    timer.getTurnOffMedia(),
                                    timer.getDeleteAfterUse()
                                );
                            }
                        }
                    }
                }
            });

        // Timer volume crescendo duration preference
        parentFragmentManager.setFragmentResultListener(VolumeCrescendoDurationDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(VolumeCrescendoDurationDialogFragment.RESULT_PREF_KEY);
                int newValue = bundle.getInt(VolumeCrescendoDurationDialogFragment.VOLUME_CRESCENDO_DURATION_VALUE);

                if (key != null) {
                    VolumeCrescendoDurationPreference pref = findPreference(key);
                    if (pref != null) {
                        pref.setVolumeCrescendoDuration(newValue);

                        if (SettingsDAO.isPerTimerCrescendoDurationDisabled(getPrefs())) {
                            List<Timer> timerList = getDataModel().getTimers();

                            for (Timer timer : timerList) {
                                getDataModel().updateAllTimerSettings(
                                    timer,
                                    timer.getLabel(),
                                    timer.getButtonTime(),
                                    timer.getRingtoneUri(),
                                    timer.getAutoSilence(),
                                    newValue,
                                    timer.isVibrate(),
                                    timer.getVibrationPattern(),
                                    timer.isFlashOn(),
                                    timer.getTurnOffMedia(),
                                    timer.getDeleteAfterUse()
                                );
                            }
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

                        if (SettingsDAO.isPerTimerVibrationPatternDisabled(getPrefs())) {
                            List<Timer> timerList = getDataModel().getTimers();

                            for (Timer timer : timerList) {
                                getDataModel().updateAllTimerSettings(
                                    timer,
                                    timer.getLabel(),
                                    timer.getButtonTime(),
                                    timer.getRingtoneUri(),
                                    timer.getAutoSilence(),
                                    timer.getVolumeCrescendoDuration(),
                                    timer.isVibrate(),
                                    newValue,
                                    timer.isFlashOn(),
                                    timer.getTurnOffMedia(),
                                    timer.getDeleteAfterUse()
                                );
                            }
                        }
                    }
                }
            });

        // Add time button value preference
        parentFragmentManager.setFragmentResultListener(TimerAddTimeButtonDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(TimerAddTimeButtonDialogFragment.RESULT_PREF_KEY);
                int newValue = bundle.getInt(TimerAddTimeButtonDialogFragment.ADD_TIME_BUTTON_VALUE);

                if (key != null) {
                    TimerAddTimeButtonValuePreference pref = findPreference(key);
                    if (pref != null) {
                        pref.setAddTimeButtonValue(newValue);
                    }
                }
            });
    }

    private void triggerDisableSettingDialog(@NonNull String prefKey) {
        if (!isAdded() || isDetached()) {
            return;
        }

        List<Timer> timerList = getDataModel().getTimers();

        mPendingDialogPrefKey = prefKey;

        if (!timerList.isEmpty()) {
            switch (prefKey) {
                case KEY_TIMER_VIBRATE -> showDisablePerTimerSettingDialog(R.string.timer_vibrate_dialog_message, KEY_TIMER_VIBRATE,
                    mTimerVibratePref, timer ->
                        getDataModel().updateAllTimerSettings(
                            timer,
                            timer.getLabel(),
                            timer.getButtonTime(),
                            timer.getRingtoneUri(),
                            timer.getAutoSilence(),
                            timer.getVolumeCrescendoDuration(),
                            false,
                            timer.getVibrationPattern(),
                            timer.isFlashOn(),
                            timer.getTurnOffMedia(),
                            timer.getDeleteAfterUse()
                        )
                );

                case KEY_ENABLE_PER_TIMER_VIBRATION_PATTERN -> showDisablePerTimerSettingDialog(
                    R.string.enable_per_alarm_vibration_pattern_dialog_message, KEY_ENABLE_PER_TIMER_VIBRATION_PATTERN,
                    mEnablePerTimerVibrationPatternPref, timer ->
                        getDataModel().updateAllTimerSettings(
                            timer,
                            timer.getLabel(),
                            timer.getButtonTime(),
                            timer.getRingtoneUri(),
                            timer.getAutoSilence(),
                            timer.getVolumeCrescendoDuration(),
                            timer.isVibrate(),
                            SettingsDAO.getTimerVibrationPattern(getPrefs()),
                            timer.isFlashOn(),
                            timer.getTurnOffMedia(),
                            timer.getDeleteAfterUse()
                        )
                );

                case KEY_ENABLE_PER_TIMER_AUTO_SILENCE -> showDisablePerTimerSettingDialog(
                    R.string.enable_per_alarm_auto_silence_dialog_message, KEY_ENABLE_PER_TIMER_AUTO_SILENCE,
                    mEnablePerTimerAutoSilencePref, timer ->
                        getDataModel().updateAllTimerSettings(
                            timer,
                            timer.getLabel(),
                            timer.getButtonTime(),
                            timer.getRingtoneUri(),
                            SettingsDAO.getTimerAutoSilenceDuration(getPrefs()),
                            timer.getVolumeCrescendoDuration(),
                            timer.isVibrate(),
                            timer.getVibrationPattern(),
                            timer.isFlashOn(),
                            timer.getTurnOffMedia(),
                            timer.getDeleteAfterUse()
                        )
                );

                case KEY_ENABLE_PER_TIMER_VOLUME_CRESCENDO_DURATION -> showDisablePerTimerSettingDialog(
                    R.string.enable_per_alarm_crescendo_duration_dialog_message, KEY_ENABLE_PER_TIMER_VOLUME_CRESCENDO_DURATION,
                    mEnablePerTimerVolumeCrescendoDurationPref, timer ->
                        getDataModel().updateAllTimerSettings(
                            timer,
                            timer.getLabel(),
                            timer.getButtonTime(),
                            timer.getRingtoneUri(),
                            timer.getAutoSilence(),
                            SettingsDAO.getTimerVolumeCrescendoDuration(getPrefs()),
                            timer.isVibrate(),
                            timer.getVibrationPattern(),
                            timer.isFlashOn(),
                            timer.getTurnOffMedia(),
                            timer.getDeleteAfterUse()
                        )
                );
            }
        } else {
            getPrefs().edit().putBoolean(prefKey, false).apply();

            Preference pref = findPreference(prefKey);
            if (pref instanceof SwitchPreferenceCompat switchPreferenceCompat) {
                switchPreferenceCompat.setChecked(false);
            }
        }
    }

    private void showDisablePerTimerSettingDialog(@StringRes int messageResId, @NonNull String prefKey,
                                                  @NonNull SwitchPreferenceCompat switchPref, @NonNull TimerUpdater timerUpdater) {

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
                List<Timer> timerList = getDataModel().getTimers();

                for (Timer timer : timerList) {
                    timerUpdater.update(timer);
                }

                getPrefs().edit().putBoolean(prefKey, false).apply();
                switchPref.setChecked(false);
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

    @NonNull
    private AlertDialog singleModeWarningDialog(boolean newValue) {
        String confirmAction = getString(R.string.confirm_action_prompt);

        return CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error),
            getString(R.string.warning),
            getString(R.string.single_timer_mode_warning_message, confirmAction),
            null,
            getString(android.R.string.ok),
            (d, w) -> {
                List<Timer> timersToDelete = new ArrayList<>(getDataModel().getTimers());

                for (Timer timer : timersToDelete) {
                    getDataModel().removeTimer(timer, R.string.label_deskclock);
                }

                mSortTimerPref.setVisible(!newValue);
                getPrefs().edit().putBoolean(KEY_SINGLE_TIMER_MODE, newValue).apply();
                mSingleTimerModePref.setChecked(newValue);

                mShowSingleTimerWarning = false;
            },
            getString(android.R.string.cancel),
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mShowSingleTimerWarning = false)),
            CustomDialog.SoftInputMode.NONE
        );
    }

    private void initAudioDeviceCallback() {
        if (mAudioDeviceCallback != null) {
            return;
        }

        mAudioDeviceCallback = new AudioDeviceCallback() {
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
        if (!mIsAlarmTabHidden) {
            return;
        }

        if (mHasExternalAudioDeviceConnected) {
            mExternalAudioDeviceVolumePref.stopRingtonePreviewForExternalAudioDevices();
        } else {
            mAlarmVolumePref.stopRingtonePreview();
        }
    }

    private void updateRingtonePreferences() {
        mTimerRingtonePref.setSummary(getDataModel().getTimerRingtoneTitle());
        mTimerRingtonePref.setIntent(RingtonePickerActivity.createTimerRingtonePickerIntentForSettings(requireContext()));
    }

    private void nullifyAllPrefs() {
        mTimerDisplayCustomizationPref = null;
        mTimerDurationFontPref = null;
        mTimerCreationViewStylePref = null;
        mTimerRingtonePref = null;
        mEnablePerTimerAutoSilencePref = null;
        mAlarmVolumePref = null;
        mEnablePerTimerVolumeCrescendoDurationPref = null;
        mAdvancedAudioPlaybackCategoryPref = null;
        mAdvancedAudioPlaybackPref = null;
        mAutoRoutingToExternalAudioDevicePref = null;
        mSystemMediaVolumePref = null;
        mExternalAudioDeviceVolumePref = null;
        mTimerVibrationCategory = null;
        mTimerVibratePref = null;
        mEnablePerTimerVibrationPatternPref = null;
        mTimerVolumeButtonsActionPref = null;
        mTimerPowerButtonActionPref = null;
        mTimerHeadphonesButtonActionPref = null;
        mTimerFlipActionPref = null;
        mTimerShakeActionPref = null;
        mTimerShakeIntensityPref = null;
        mSortTimerPref = null;
        mTurnOnBackFlashForExpiredTimerPref = null;
        mDisplayLowAlarmVolumeWarningPref = null;
    }

    /**
     * Interface for updating timer properties when pressing the OK button in the dialog box
     * that appears when the "per timer" settings are disabled.
     */
    private interface TimerUpdater {
        void update(Timer timer);
    }

}
