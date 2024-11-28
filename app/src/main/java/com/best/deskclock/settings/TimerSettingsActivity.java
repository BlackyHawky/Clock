// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

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
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

public class TimerSettingsActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "timer_settings_fragment";

    public static final String KEY_TIMER_RINGTONE = "key_timer_ringtone";
    public static final String KEY_TIMER_AUTO_SILENCE = "key_timer_auto_silence";
    public static final String KEY_TIMER_CRESCENDO = "key_timer_crescendo_duration";
    public static final String KEY_TIMER_VIBRATE = "key_timer_vibrate";
    public static final String KEY_TIMER_VOLUME_BUTTONS_ACTION = "key_timer_volume_buttons_action";
    public static final String KEY_TIMER_POWER_BUTTON_ACTION = "key_timer_power_button_action";
    public static final String KEY_TIMER_FLIP_ACTION = "key_timer_flip_action";
    public static final String KEY_TIMER_SHAKE_ACTION = "key_timer_shake_action";
    public static final String KEY_SORT_TIMER = "key_sort_timer";
    public static final String KEY_SORT_TIMER_MANUALLY = "0";
    public static final String KEY_SORT_TIMER_BY_ASCENDING_DURATION = "1";
    public static final String KEY_SORT_TIMER_BY_DESCENDING_DURATION = "2";
    public static final String KEY_SORT_TIMER_BY_NAME = "3";
    public static final String KEY_DEFAULT_TIME_TO_ADD_TO_TIMER = "key_default_time_to_add_to_timer";
    public static final String KEY_KEEP_TIMER_SCREEN_ON = "key_keep_timer_screen_on";
    public static final String KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER =
            "key_transparent_background_for_expired_timer";
    public static final String KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER =
            "key_display_warning_before_deleting_timer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PrefsFragment(), PREFS_FRAGMENT_TAG)
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static class PrefsFragment extends ScreenFragment implements
            Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

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
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_timer);

            mTimerRingtonePref = findPreference(KEY_TIMER_RINGTONE);
            mTimerAutoSilencePref = findPreference(KEY_TIMER_AUTO_SILENCE);
            mTimerCrescendoPref = findPreference(KEY_TIMER_CRESCENDO);
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

            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_TIMER_RINGTONE -> pref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());

                case KEY_TIMER_AUTO_SILENCE, KEY_TIMER_CRESCENDO, KEY_DEFAULT_TIME_TO_ADD_TO_TIMER -> {
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
                    requireActivity().setResult(RESULT_OK);
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
            final boolean hasVibrator = ((Vibrator) mTimerVibratePref.getContext()
                    .getSystemService(VIBRATOR_SERVICE)).hasVibrator();
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
}
