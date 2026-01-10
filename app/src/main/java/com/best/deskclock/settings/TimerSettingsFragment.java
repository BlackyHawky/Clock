// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VOLUME_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_NEVER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SORT_TIMER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_CREATION_VIEW_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_DISPLAY_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_DURATION_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_FLIP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_POWER_BUTTON_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_RINGTONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHAKE_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_VIBRATE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_VOLUME_BUTTONS_ACTION;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.best.deskclock.AutoSilenceDurationDialogFragment;
import com.best.deskclock.R;
import com.best.deskclock.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.settings.custompreference.AutoSilenceDurationPreference;
import com.best.deskclock.settings.custompreference.CustomListPreference;
import com.best.deskclock.settings.custompreference.CustomPreference;
import com.best.deskclock.settings.custompreference.CustomSeekbarPreference;
import com.best.deskclock.settings.custompreference.CustomSwitchPreference;
import com.best.deskclock.settings.custompreference.TimerAddTimeButtonValuePreference;
import com.best.deskclock.settings.custompreference.VolumeCrescendoDurationPreference;
import com.best.deskclock.timer.TimerAddTimeButtonDialogFragment;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.Utils;

public class TimerSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    CustomPreference mTimerDisplayCustomizationPref;
    CustomPreference mTimerDurationFontPref;
    CustomListPreference mTimerCreationViewStylePref;
    CustomPreference mTimerRingtonePref;
    CustomSwitchPreference mTimerVibratePref;
    CustomSwitchPreference mTimerVolumeButtonsActionPref;
    CustomSwitchPreference mTimerPowerButtonActionPref;
    CustomSwitchPreference mTimerFlipActionPref;
    CustomSwitchPreference mTimerShakeActionPref;
    CustomSeekbarPreference mTimerShakeIntensityPref;
    CustomListPreference mSortTimerPref;
    CustomSwitchPreference mDisplayWarningBeforeDeletingTimerPref;

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

                String safeTitle = Utils.toSafeFileName("timer_font");

                // Delete the old font if it exists
                clearFile(mPrefs.getString(KEY_TIMER_DURATION_FONT, null));

                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_TIMER_DURATION_FONT, copiedUri.getPath()).apply();
                    mTimerDurationFontPref.setTitle(getString(R.string.custom_font_title_variant));

                    CustomToast.show(requireContext(), R.string.custom_font_toast_message_selected);
                } else {
                    CustomToast.show(requireContext(), "Error importing font");
                    mTimerDurationFontPref.setTitle(getString(R.string.custom_font_title));
                }
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
        mTimerVibratePref = findPreference(KEY_TIMER_VIBRATE);
        mTimerVolumeButtonsActionPref = findPreference(KEY_TIMER_VOLUME_BUTTONS_ACTION);
        mTimerPowerButtonActionPref = findPreference(KEY_TIMER_POWER_BUTTON_ACTION);
        mTimerFlipActionPref = findPreference(KEY_TIMER_FLIP_ACTION);
        mTimerShakeActionPref = findPreference(KEY_TIMER_SHAKE_ACTION);
        mTimerShakeIntensityPref = findPreference(KEY_TIMER_SHAKE_INTENSITY);
        mSortTimerPref = findPreference(KEY_SORT_TIMER);
        mDisplayWarningBeforeDeletingTimerPref = findPreference(KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER);

        setupPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();

        mTimerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_TIMER_CREATION_VIEW_STYLE, KEY_SORT_TIMER -> {
                final CustomListPreference preference = (CustomListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_TIMER_RINGTONE ->
                    mTimerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());

            case KEY_TIMER_SHAKE_ACTION -> {
                mTimerShakeIntensityPref.setVisible((boolean) newValue);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_TIMER_VIBRATE, KEY_TIMER_VOLUME_BUTTONS_ACTION, KEY_TIMER_POWER_BUTTON_ACTION,
                 KEY_TIMER_FLIP_ACTION, KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER ->
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
            case KEY_TIMER_DISPLAY_CUSTOMIZATION ->
                    animateAndShowFragment(new TimerDisplayCustomizationFragment());

            case KEY_TIMER_DURATION_FONT -> selectCustomFile(mTimerDurationFontPref, fontPickerLauncher,
                    SettingsDAO.getTimerDurationFont(mPrefs), KEY_TIMER_DURATION_FONT, true, null);

            case KEY_TIMER_RINGTONE ->
                    startActivity(RingtonePickerActivity.createTimerRingtonePickerIntent(context));
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
                            currentValue == TIMEOUT_NEVER);
            AutoSilenceDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VolumeCrescendoDurationPreference volumeCrescendoDurationPreference) {
            int currentDelay = volumeCrescendoDurationPreference.getVolumeCrescendoDuration();
            VolumeCrescendoDurationDialogFragment dialogFragment =
                    VolumeCrescendoDurationDialogFragment.newInstance(pref.getKey(), currentDelay,
                            currentDelay == DEFAULT_VOLUME_CRESCENDO_DURATION);
            VolumeCrescendoDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof TimerAddTimeButtonValuePreference timerAddTimeButtonValuePreference) {
            int currentValue = timerAddTimeButtonValuePreference.getAddTimeButtonValue();
            TimerAddTimeButtonDialogFragment dialogFragment =
                    TimerAddTimeButtonDialogFragment.newInstance(pref.getKey(), currentValue);
            TimerAddTimeButtonDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    private void setupPreferences() {
        mTimerDisplayCustomizationPref.setOnPreferenceClickListener(this);

        mTimerDurationFontPref.setTitle(getString(SettingsDAO.getTimerDurationFont(mPrefs) == null
                ? R.string.custom_font_title
                : R.string.custom_font_title_variant));
        mTimerDurationFontPref.setOnPreferenceClickListener(this);

        mTimerCreationViewStylePref.setOnPreferenceChangeListener(this);
        mTimerCreationViewStylePref.setSummary(mTimerCreationViewStylePref.getEntry());

        mTimerRingtonePref.setOnPreferenceClickListener(this);

        // Timer auto silence duration preference
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

        // Timer volume crescendo duration preference
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

        // Add time button value preference
        getParentFragmentManager().setFragmentResultListener(TimerAddTimeButtonDialogFragment.REQUEST_KEY,
                this, (requestKey, bundle) -> {
            String key = bundle.getString(TimerAddTimeButtonDialogFragment.RESULT_PREF_KEY);
            int newValue = bundle.getInt(TimerAddTimeButtonDialogFragment.ADD_TIME_BUTTON_VALUE);

            if (key != null) {
                TimerAddTimeButtonValuePreference pref = findPreference(key);
                if (pref != null) {
                    pref.setAddTimeButtonValue(newValue);
                    pref.setSummary(pref.getSummary());
                }
            }

        });

        mDisplayWarningBeforeDeletingTimerPref.setOnPreferenceChangeListener(this);
    }

}
