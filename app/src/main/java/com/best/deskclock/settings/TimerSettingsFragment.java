// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.content.Context.VIBRATOR_SERVICE;

import static com.best.deskclock.DeskClock.REQUEST_CHANGE_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DEFAULT_TIME_TO_ADD_TO_TIMER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_KEEP_TIMER_SCREEN_ON;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SORT_TIMER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_AUTO_SILENCE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_FLIP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_POWER_BUTTON_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_RINGTONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHAKE_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_VIBRATE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_VOLUME_BUTTONS_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.utils.Utils;

public class TimerSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private int mRecyclerViewPosition = -1;

    ListPreference mTimerAutoSilencePref;
    ListPreference mTimerCrescendoPref;
    ListPreference mSortTimerPref;
    ListPreference mDefaultMinutesToAddToTimerPref;
    Preference mTimerRingtonePref;
    Preference mTimerVibratePref;
    SwitchPreferenceCompat mTimerVolumeButtonsActionPref;
    SwitchPreferenceCompat mTimerPowerButtonActionPref;
    SwitchPreferenceCompat mTimerFlipActionPref;
    SwitchPreferenceCompat mTimerShakeActionPref;
    SwitchPreferenceCompat mKeepTimerScreenOnPref;
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
        mTimerCrescendoPref = findPreference(KEY_TIMER_CRESCENDO_DURATION);
        mTimerVibratePref = findPreference(KEY_TIMER_VIBRATE);
        mTimerVolumeButtonsActionPref = findPreference(KEY_TIMER_VOLUME_BUTTONS_ACTION);
        mTimerPowerButtonActionPref = findPreference(KEY_TIMER_POWER_BUTTON_ACTION);
        mTimerFlipActionPref = findPreference(KEY_TIMER_FLIP_ACTION);
        mTimerShakeActionPref = findPreference(KEY_TIMER_SHAKE_ACTION);
        mSortTimerPref = findPreference(KEY_SORT_TIMER);
        mDefaultMinutesToAddToTimerPref = findPreference(KEY_DEFAULT_TIME_TO_ADD_TO_TIMER);
        mKeepTimerScreenOnPref = findPreference(KEY_KEEP_TIMER_SCREEN_ON);
        mTransparentBackgroundPref = findPreference(KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER);
        mDisplayWarningBeforeDeletingTimerPref = findPreference(KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER);

        setupPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mRecyclerViewPosition != -1) {
            mLinearLayoutManager.scrollToPosition(mRecyclerViewPosition);
            mAppBarLayout.setExpanded(mRecyclerViewPosition == 0, true);
        }
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mLinearLayoutManager != null) {
            mRecyclerViewPosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_TIMER_RINGTONE -> pref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());

            case KEY_TIMER_AUTO_SILENCE, KEY_TIMER_CRESCENDO_DURATION, KEY_DEFAULT_TIME_TO_ADD_TO_TIMER -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_TIMER_VIBRATE -> {
                final TwoStatePreference timerVibratePref = (TwoStatePreference) pref;
                DataModel.getDataModel().setTimerVibrate(timerVibratePref.isChecked());
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_SORT_TIMER -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
                // Set result so DeskClock knows to refresh itself
                requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
            }

            case KEY_TIMER_VOLUME_BUTTONS_ACTION, KEY_TIMER_POWER_BUTTON_ACTION,
                 KEY_TIMER_FLIP_ACTION, KEY_TIMER_SHAKE_ACTION, KEY_KEEP_TIMER_SCREEN_ON,
                 KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER,
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

    private void setupPreferences() {
        final boolean hasVibrator = ((Vibrator) requireActivity().getSystemService(VIBRATOR_SERVICE)).hasVibrator();
        mTimerVibratePref.setVisible(hasVibrator);

        SensorManager sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            mTimerFlipActionPref.setChecked(false);
            mTimerShakeActionPref.setChecked(false);
            mTimerFlipActionPref.setVisible(false);
            mTimerShakeActionPref.setVisible(false);
        } else {
            mTimerFlipActionPref.setChecked(DataModel.getDataModel().isFlipActionForTimersEnabled());
            mTimerFlipActionPref.setOnPreferenceChangeListener(this);
            mTimerShakeActionPref.setChecked(DataModel.getDataModel().isShakeActionForTimersEnabled());
            mTimerShakeActionPref.setOnPreferenceChangeListener(this);
        }
    }

    private void refresh() {
        mTimerRingtonePref.setOnPreferenceClickListener(this);
        mTimerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());

        mTimerAutoSilencePref.setOnPreferenceChangeListener(this);
        mTimerAutoSilencePref.setSummary(mTimerAutoSilencePref.getEntry());

        mTimerCrescendoPref.setOnPreferenceChangeListener(this);
        mTimerCrescendoPref.setSummary(mTimerCrescendoPref.getEntry());

        mTimerVibratePref.setOnPreferenceChangeListener(this);

        mTimerVolumeButtonsActionPref.setOnPreferenceChangeListener(this);

        mTimerPowerButtonActionPref.setOnPreferenceChangeListener(this);

        mSortTimerPref.setOnPreferenceChangeListener(this);
        mSortTimerPref.setSummary(mSortTimerPref.getEntry());

        mDefaultMinutesToAddToTimerPref.setOnPreferenceChangeListener(this);
        mDefaultMinutesToAddToTimerPref.setSummary(mDefaultMinutesToAddToTimerPref.getEntry());

        mKeepTimerScreenOnPref.setChecked(DataModel.getDataModel().shouldTimerDisplayRemainOn());
        mKeepTimerScreenOnPref.setOnPreferenceChangeListener(this);

        mTransparentBackgroundPref.setChecked(DataModel.getDataModel().isTimerBackgroundTransparent());
        mTransparentBackgroundPref.setOnPreferenceChangeListener(this);

        mDisplayWarningBeforeDeletingTimerPref.setChecked(
                DataModel.getDataModel().isWarningDisplayedBeforeDeletingTimer()
        );
        mDisplayWarningBeforeDeletingTimerPref.setOnPreferenceChangeListener(this);
    }

}
