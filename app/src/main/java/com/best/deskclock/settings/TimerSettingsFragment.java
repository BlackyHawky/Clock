// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesKeys.*;

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
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.AppExecutors;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.dialogfragment.AutoSilenceDurationDialogFragment;
import com.best.deskclock.dialogfragment.TimerAddTimeButtonDialogFragment;
import com.best.deskclock.dialogfragment.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.settings.custompreference.AlarmVolumePreference;
import com.best.deskclock.settings.custompreference.AutoSilenceDurationPreference;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.settings.custompreference.TimerAddTimeButtonValuePreference;
import com.best.deskclock.settings.custompreference.VolumeCrescendoDurationPreference;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.Utils;

public class TimerSettingsFragment extends ScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private AudioManager mAudioManager;
    private AudioDeviceCallback mAudioDeviceCallback;

    private boolean mHasExternalAudioDeviceConnected;
    private boolean mIsAlarmTabHidden;

    Preference mTimerDisplayCustomizationPref;
    Preference mTimerDurationFontPref;
    ListPreference mTimerCreationViewStylePref;
    Preference mTimerRingtonePref;
    AlarmVolumePreference mAlarmVolumePref;
    SwitchPreferenceCompat mAdvancedAudioPlaybackPref;
    SwitchPreferenceCompat mAutoRoutingToExternalAudioDevicePref;
    SwitchPreferenceCompat mSystemMediaVolume;
    CustomSliderPreference mExternalAudioDeviceVolumePref;
    SwitchPreferenceCompat mTimerVibratePref;
    SwitchPreferenceCompat mTimerVolumeButtonsActionPref;
    SwitchPreferenceCompat mTimerPowerButtonActionPref;
    SwitchPreferenceCompat mTimerFlipActionPref;
    SwitchPreferenceCompat mTimerShakeActionPref;
    CustomSliderPreference mTimerShakeIntensityPref;
    ListPreference mSortTimerPref;
    SwitchPreferenceCompat mDisplayWarningBeforeDeletingTimerPref;
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

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = Utils.toSafeFileName(FILE_TIMER_FONT);
            String oldFontPath = mPrefs.getString(KEY_TIMER_DURATION_FONT, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old font if it exists
                clearFile(oldFontPath);

                // Copy the new font to the device's protected storage
                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_TIMER_DURATION_FONT, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, R.string.custom_font_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, "Error importing font");
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_timer);

        mTimerDisplayCustomizationPref = findPreference(KEY_TIMER_DISPLAY_CUSTOMIZATION);
        mTimerDurationFontPref = findPreference(KEY_TIMER_DURATION_FONT);
        mTimerCreationViewStylePref = findPreference(KEY_TIMER_CREATION_VIEW_STYLE);
        mTimerRingtonePref = findPreference(KEY_TIMER_RINGTONE);
        mAlarmVolumePref = findPreference(KEY_ALARM_VOLUME_SETTING);
        mAdvancedAudioPlaybackPref = findPreference(KEY_ADVANCED_AUDIO_PLAYBACK);
        mAutoRoutingToExternalAudioDevicePref = findPreference(KEY_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE);
        mSystemMediaVolume = findPreference(KEY_SYSTEM_MEDIA_VOLUME);
        mExternalAudioDeviceVolumePref = findPreference(KEY_EXTERNAL_AUDIO_DEVICE_VOLUME);
        mTimerVibratePref = findPreference(KEY_TIMER_VIBRATE);
        mTimerVolumeButtonsActionPref = findPreference(KEY_TIMER_VOLUME_BUTTONS_ACTION);
        mTimerPowerButtonActionPref = findPreference(KEY_TIMER_POWER_BUTTON_ACTION);
        mTimerFlipActionPref = findPreference(KEY_TIMER_FLIP_ACTION);
        mTimerShakeActionPref = findPreference(KEY_TIMER_SHAKE_ACTION);
        mTimerShakeIntensityPref = findPreference(KEY_TIMER_SHAKE_INTENSITY);
        mSortTimerPref = findPreference(KEY_SORT_TIMER);
        mDisplayWarningBeforeDeletingTimerPref = findPreference(KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER);
        mDisplayLowAlarmVolumeWarningPref = findPreference(KEY_DISPLAY_LOW_ALARM_VOLUME_WARNING);

        mIsAlarmTabHidden = !SettingsDAO.isAlarmTabVisible(mPrefs);

        if (mIsAlarmTabHidden) {
            mAudioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
            mHasExternalAudioDeviceConnected = RingtoneUtils.hasExternalAudioDeviceConnected(requireContext(), mPrefs);
        }

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

        mTimerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());

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
            mAlarmVolumePref, mAdvancedAudioPlaybackPref, mAutoRoutingToExternalAudioDevicePref, mSystemMediaVolume,
            mExternalAudioDeviceVolumePref, mTimerVibratePref, mTimerVolumeButtonsActionPref, mTimerPowerButtonActionPref,
            mTimerFlipActionPref, mTimerShakeActionPref, mTimerShakeIntensityPref, mSortTimerPref, mDisplayWarningBeforeDeletingTimerPref,
            mDisplayLowAlarmVolumeWarningPref);

        super.onDestroy();

        mAudioManager = null;

        nullifyAllPrefs();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_TIMER_CREATION_VIEW_STYLE, KEY_SORT_TIMER -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_TIMER_RINGTONE -> mTimerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());

            case KEY_ADVANCED_AUDIO_PLAYBACK -> {
                stopRingtonePreview();
                mAutoRoutingToExternalAudioDevicePref.setVisible(mIsAlarmTabHidden && (boolean) newValue);
                mSystemMediaVolume.setVisible(mIsAlarmTabHidden
                    && (boolean) newValue
                    && SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs));
                mExternalAudioDeviceVolumePref.setVisible(mIsAlarmTabHidden
                    && (boolean) newValue
                    && SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs)
                    && SettingsDAO.shouldUseCustomMediaVolume(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE -> {
                stopRingtonePreview();
                mSystemMediaVolume.setVisible(mIsAlarmTabHidden && (boolean) newValue);
                mExternalAudioDeviceVolumePref.setVisible(mIsAlarmTabHidden
                    && (boolean) newValue
                    && SettingsDAO.shouldUseCustomMediaVolume(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_SYSTEM_MEDIA_VOLUME -> {
                stopRingtonePreview();
                mExternalAudioDeviceVolumePref.setVisible(mIsAlarmTabHidden && !(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_TIMER_SHAKE_ACTION -> {
                mTimerShakeIntensityPref.setVisible((boolean) newValue);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_TIMER_VIBRATE, KEY_TIMER_VOLUME_BUTTONS_ACTION, KEY_TIMER_POWER_BUTTON_ACTION, KEY_TIMER_FLIP_ACTION,
                 KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER, KEY_DISPLAY_LOW_ALARM_VOLUME_WARNING ->
                Utils.setVibrationTime(requireContext(), 50);
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
                SettingsDAO.getTimerDurationFont(mPrefs), KEY_TIMER_DURATION_FONT, true, null);

            case KEY_TIMER_RINGTONE -> startActivity(RingtonePickerActivity.createTimerRingtonePickerIntent(context));
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
        } else if (pref instanceof TimerAddTimeButtonValuePreference timerAddTimeButtonValuePreference) {
            int currentValue = timerAddTimeButtonValuePreference.getAddTimeButtonValue();
            TimerAddTimeButtonDialogFragment dialogFragment = TimerAddTimeButtonDialogFragment.newInstance(pref.getKey(), currentValue);
            TimerAddTimeButtonDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    private void setupPreferences() {
        final boolean isAdvancedAudioPlaybackEnabled = SettingsDAO.isAdvancedAudioPlaybackEnabled(mPrefs);
        final boolean isAutoRoutingToExternalAudioDevice = SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs);

        mTimerDisplayCustomizationPref.setOnPreferenceClickListener(this);

        mTimerDurationFontPref.setTitle(getString(SettingsDAO.getTimerDurationFont(mPrefs) == null
            ? R.string.custom_font_title
            : R.string.custom_font_title_variant));
        mTimerDurationFontPref.setOnPreferenceClickListener(this);

        mTimerCreationViewStylePref.setOnPreferenceChangeListener(this);
        mTimerCreationViewStylePref.setSummary(mTimerCreationViewStylePref.getEntry());

        mTimerRingtonePref.setOnPreferenceClickListener(this);

        mAlarmVolumePref.setVisible(mIsAlarmTabHidden);
        if (mAlarmVolumePref.isVisible()) {
            mAlarmVolumePref.setEnabled(!mHasExternalAudioDeviceConnected);
        }

        mAdvancedAudioPlaybackPref.setVisible(mIsAlarmTabHidden);
        mAdvancedAudioPlaybackPref.setOnPreferenceChangeListener(this);

        mAutoRoutingToExternalAudioDevicePref.setVisible(mIsAlarmTabHidden && isAdvancedAudioPlaybackEnabled);
        mAutoRoutingToExternalAudioDevicePref.setOnPreferenceChangeListener(this);

        mSystemMediaVolume.setVisible(mIsAlarmTabHidden && isAdvancedAudioPlaybackEnabled && isAutoRoutingToExternalAudioDevice);
        mSystemMediaVolume.setOnPreferenceChangeListener(this);

        mExternalAudioDeviceVolumePref.setVisible(mIsAlarmTabHidden
            && isAdvancedAudioPlaybackEnabled
            && isAutoRoutingToExternalAudioDevice
            && SettingsDAO.shouldUseCustomMediaVolume(mPrefs));
        mExternalAudioDeviceVolumePref.setEnabled(mExternalAudioDeviceVolumePref.isVisible() && mHasExternalAudioDeviceConnected);

        mTimerVibratePref.setVisible(DeviceUtils.hasVibrator(requireContext()));
        mTimerVibratePref.setOnPreferenceChangeListener(this);

        mTimerVolumeButtonsActionPref.setOnPreferenceChangeListener(this);

        mTimerPowerButtonActionPref.setOnPreferenceChangeListener(this);

        SensorManager sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            mTimerFlipActionPref.setChecked(false);
            mTimerShakeActionPref.setChecked(false);
            mTimerFlipActionPref.setVisible(false);
            mTimerShakeActionPref.setVisible(false);
        } else {
            mTimerFlipActionPref.setOnPreferenceChangeListener(this);
            mTimerShakeActionPref.setOnPreferenceChangeListener(this);
            mTimerShakeIntensityPref.setVisible(SettingsDAO.isShakeActionForTimersEnabled(mPrefs));
        }

        mSortTimerPref.setOnPreferenceChangeListener(this);
        mSortTimerPref.setSummary(mSortTimerPref.getEntry());

        mDisplayWarningBeforeDeletingTimerPref.setOnPreferenceChangeListener(this);

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
        if (!mIsAlarmTabHidden) {
            return;
        }

        if (mHasExternalAudioDeviceConnected) {
            mExternalAudioDeviceVolumePref.stopRingtonePreviewForExternalAudioDevices();
        } else {
            mAlarmVolumePref.stopRingtonePreview();
        }
    }

    private void nullifyAllPrefs() {
        mTimerDisplayCustomizationPref = null;
        mTimerDurationFontPref = null;
        mTimerCreationViewStylePref = null;
        mTimerRingtonePref = null;
        mAlarmVolumePref = null;
        mAdvancedAudioPlaybackPref = null;
        mAutoRoutingToExternalAudioDevicePref = null;
        mSystemMediaVolume = null;
        mExternalAudioDeviceVolumePref = null;
        mTimerVibratePref = null;
        mTimerVolumeButtonsActionPref = null;
        mTimerPowerButtonActionPref = null;
        mTimerFlipActionPref = null;
        mTimerShakeActionPref = null;
        mTimerShakeIntensityPref = null;
        mSortTimerPref = null;
        mDisplayWarningBeforeDeletingTimerPref = null;
        mDisplayLowAlarmVolumeWarningPref = null;
    }

}
