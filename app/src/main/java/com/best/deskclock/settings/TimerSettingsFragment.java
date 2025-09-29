// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_NEVER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SORT_TIMER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_CREATION_VIEW_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_FLIP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_POWER_BUTTON_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_RINGTONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHAKE_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_VIBRATE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_VOLUME_BUTTONS_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.AutoSilenceDurationDialogFragment;
import com.best.deskclock.R;
import com.best.deskclock.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.timer.TimerAddTimeButtonDialogFragment;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.Utils;

public class TimerSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    ListPreference mSortTimerPref;
    ListPreference mTimerCreationViewStylePref;
    Preference mTimerRingtonePref;
    Preference mTimerVibratePref;
    SwitchPreferenceCompat mTimerVolumeButtonsActionPref;
    SwitchPreferenceCompat mTimerPowerButtonActionPref;
    SwitchPreferenceCompat mTimerFlipActionPref;
    SwitchPreferenceCompat mTimerShakeActionPref;
    CustomSeekbarPreference mTimerShakeIntensityPref;
    SwitchPreferenceCompat mTransparentBackgroundPref;
    SwitchPreferenceCompat mDisplayWarningBeforeDeletingTimerPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.timer_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_timer);

        mTimerRingtonePref = findPreference(KEY_TIMER_RINGTONE);
        mTimerVibratePref = findPreference(KEY_TIMER_VIBRATE);
        mTimerVolumeButtonsActionPref = findPreference(KEY_TIMER_VOLUME_BUTTONS_ACTION);
        mTimerPowerButtonActionPref = findPreference(KEY_TIMER_POWER_BUTTON_ACTION);
        mTimerFlipActionPref = findPreference(KEY_TIMER_FLIP_ACTION);
        mTimerShakeActionPref = findPreference(KEY_TIMER_SHAKE_ACTION);
        mTimerShakeIntensityPref = findPreference(KEY_TIMER_SHAKE_INTENSITY);
        mSortTimerPref = findPreference(KEY_SORT_TIMER);
        mTransparentBackgroundPref = findPreference(KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER);
        mDisplayWarningBeforeDeletingTimerPref = findPreference(KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER);
        mTimerCreationViewStylePref = findPreference(KEY_TIMER_CREATION_VIEW_STYLE);

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
            case KEY_TIMER_RINGTONE -> mTimerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());

            case KEY_TIMER_CREATION_VIEW_STYLE, KEY_SORT_TIMER -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_TIMER_SHAKE_ACTION -> {
                mTimerShakeIntensityPref.setVisible((boolean) newValue);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_TIMER_VIBRATE, KEY_TIMER_VOLUME_BUTTONS_ACTION, KEY_TIMER_POWER_BUTTON_ACTION,
                 KEY_TIMER_FLIP_ACTION, KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER,
                 KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER ->
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
        if (pref.getKey().equals(KEY_TIMER_RINGTONE)) {
            startActivity(RingtonePickerActivity.createTimerRingtonePickerIntent(context));
            return true;
        }

        return false;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference pref) {
        if (pref instanceof AutoSilenceDurationPreference autoSilenceDurationPreference) {
            int currentValue = autoSilenceDurationPreference.getAutoSilenceDuration();
            AutoSilenceDurationDialogFragment dialogFragment =
                    AutoSilenceDurationDialogFragment.newInstance(pref.getKey(), currentValue,
                            currentValue == TIMEOUT_END_OF_RINGTONE,
                            currentValue == TIMEOUT_NEVER, true);
            AutoSilenceDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VolumeCrescendoDurationPreference volumeCrescendoDurationPreference) {
            int currentDelay = volumeCrescendoDurationPreference.getVolumeCrescendoDuration();
            VolumeCrescendoDurationDialogFragment dialogFragment =
                    VolumeCrescendoDurationDialogFragment.newInstance(pref.getKey(), currentDelay,
                            currentDelay == DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION);
            VolumeCrescendoDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof TimerAddTimeButtonValuePreference timerAddTimeButtonValuePreference) {
            int currentValue = timerAddTimeButtonValuePreference.getAddTimeButtonValue();
            TimerAddTimeButtonDialogFragment dialogFragment =
                    TimerAddTimeButtonDialogFragment.newInstance(pref.getKey(), currentValue);
            TimerAddTimeButtonDialogFragment.show(getParentFragmentManager(), dialogFragment);
        }
        else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    private void setupPreferences() {
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

        mTimerCreationViewStylePref.setOnPreferenceChangeListener(this);
        mTimerCreationViewStylePref.setSummary(mTimerCreationViewStylePref.getEntry());

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

        mTransparentBackgroundPref.setOnPreferenceChangeListener(this);

        mDisplayWarningBeforeDeletingTimerPref.setOnPreferenceChangeListener(this);
    }

}
