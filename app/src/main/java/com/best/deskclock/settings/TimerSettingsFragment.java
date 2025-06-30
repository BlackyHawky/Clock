// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.DeskClock.REQUEST_CHANGE_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DEFAULT_TIME_TO_ADD_TO_TIMER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SORT_TIMER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_AUTO_SILENCE;
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

import com.best.deskclock.R;
import com.best.deskclock.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.utils.Utils;

public class TimerSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    ListPreference mTimerAutoSilencePref;
    ListPreference mSortTimerPref;
    ListPreference mDefaultMinutesToAddToTimerPref;
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
        mTimerAutoSilencePref = findPreference(KEY_TIMER_AUTO_SILENCE);
        mTimerVibratePref = findPreference(KEY_TIMER_VIBRATE);
        mTimerVolumeButtonsActionPref = findPreference(KEY_TIMER_VOLUME_BUTTONS_ACTION);
        mTimerPowerButtonActionPref = findPreference(KEY_TIMER_POWER_BUTTON_ACTION);
        mTimerFlipActionPref = findPreference(KEY_TIMER_FLIP_ACTION);
        mTimerShakeActionPref = findPreference(KEY_TIMER_SHAKE_ACTION);
        mTimerShakeIntensityPref = findPreference(KEY_TIMER_SHAKE_INTENSITY);
        mSortTimerPref = findPreference(KEY_SORT_TIMER);
        mDefaultMinutesToAddToTimerPref = findPreference(KEY_DEFAULT_TIME_TO_ADD_TO_TIMER);
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

            case KEY_TIMER_AUTO_SILENCE, KEY_DEFAULT_TIME_TO_ADD_TO_TIMER,
                 KEY_TIMER_CREATION_VIEW_STYLE -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_SORT_TIMER -> {
                final int index = mSortTimerPref.findIndexOfValue((String) newValue);
                mSortTimerPref.setSummary(mSortTimerPref.getEntries()[index]);
                // Set result so DeskClock knows to refresh itself
                requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
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
        if (pref instanceof VolumeCrescendoDurationPreference volumeCrescendoDurationPreference) {
            int currentDelay = volumeCrescendoDurationPreference.getVolumeCrescendoDuration();
            VolumeCrescendoDurationDialogFragment dialogFragment =
                    VolumeCrescendoDurationDialogFragment.newInstance(pref.getKey(), currentDelay);
            VolumeCrescendoDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    private void setupPreferences() {
        mTimerRingtonePref.setOnPreferenceClickListener(this);

        mTimerAutoSilencePref.setOnPreferenceChangeListener(this);
        mTimerAutoSilencePref.setSummary(mTimerAutoSilencePref.getEntry());

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

        mTimerVibratePref.setVisible(Utils.hasVibrator(requireContext()));
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

        mDefaultMinutesToAddToTimerPref.setOnPreferenceChangeListener(this);
        mDefaultMinutesToAddToTimerPref.setSummary(mDefaultMinutesToAddToTimerPref.getEntry());

        mTransparentBackgroundPref.setOnPreferenceChangeListener(this);

        mDisplayWarningBeforeDeletingTimerPref.setOnPreferenceChangeListener(this);
    }

}
