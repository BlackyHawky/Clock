// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.content.Context.VIBRATOR_SERVICE;

import static com.best.deskclock.DeskClock.REQUEST_CHANGE_SETTINGS;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.Utils;

public class AlarmSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private int mRecyclerViewPosition = -1;

    public static final String KEY_DEFAULT_ALARM_RINGTONE = "key_default_alarm_ringtone";
    public static final String KEY_AUTO_SILENCE = "key_auto_silence";
    public static final String KEY_ALARM_SNOOZE = "key_snooze_duration";
    public static final String KEY_ALARM_VOLUME_SETTING = "key_volume_setting";
    public static final String KEY_ALARM_CRESCENDO = "key_alarm_crescendo_duration";
    public static final String KEY_SWIPE_ACTION = "key_swipe_action";
    public static final String KEY_VOLUME_BUTTONS = "key_volume_button_setting";
    public static final String DEFAULT_VOLUME_BEHAVIOR = "-1";
    public static final String VOLUME_BEHAVIOR_CHANGE_VOLUME = "0";
    public static final String VOLUME_BEHAVIOR_SNOOZE = "1";
    public static final String VOLUME_BEHAVIOR_DISMISS = "2";
    public static final String KEY_POWER_BUTTON = "key_power_button";
    public static final String DEFAULT_POWER_BEHAVIOR = "0";
    public static final String POWER_BEHAVIOR_SNOOZE = "1";
    public static final String POWER_BEHAVIOR_DISMISS = "2";
    public static final String KEY_FLIP_ACTION = "key_flip_action";
    public static final String KEY_SHAKE_ACTION = "key_shake_action";
    public static final String KEY_SHAKE_INTENSITY = "key_shake_intensity";
    public static final String KEY_WEEK_START = "key_week_start";
    public static final String KEY_ALARM_NOTIFICATION_REMINDER_TIME = "key_alarm_notification_reminder_time";
    public static final String KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT = "key_enable_alarm_vibrations_by_default";
    public static final String KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS = "key_enable_snoozed_or_dismissed_alarm_vibrations";
    public static final String KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM = "key_turn_on_back_flash_for_triggered_alarm";
    public static final String KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT = "key_enable_delete_occasional_alarm_by_default";
    public static final String KEY_MATERIAL_TIME_PICKER_STYLE = "key_material_time_picker_style";
    public static final String MATERIAL_TIME_PICKER_ANALOG_STYLE = "analog";
    public static final String KEY_ALARM_DISPLAY_CUSTOMIZATION = "key_alarm_display_customization";

    Preference mAlarmRingtonePref;
    ListPreference mAutoSilencePref;
    ListPreference mAlarmSnoozePref;
    AlarmVolumePreference mAlarmVolumePreference;
    ListPreference mAlarmCrescendoPref;
    SwitchPreferenceCompat mSwipeActionPref;
    ListPreference mVolumeButtonsPref;
    ListPreference mPowerButtonPref;
    ListPreference mFlipActionPref;
    ListPreference mShakeActionPref;
    SeekBarPreference mShakeIntensityPref;
    ListPreference mWeekStartPref;
    ListPreference mAlarmNotificationReminderTimePref;
    SwitchPreferenceCompat mEnableAlarmVibrationsByDefaultPref;
    SwitchPreferenceCompat mEnableSnoozedOrDismissedAlarmVibrationsPref;
    SwitchPreferenceCompat mTurnOnBackFlashForTriggeredAlarmPref;
    SwitchPreferenceCompat mDeleteOccasionalAlarmByDefaultPref;
    ListPreference mMaterialTimePickerStylePref;
    Preference mAlarmDisplayCustomizationPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.alarm_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_alarm);

        mAlarmRingtonePref = findPreference(KEY_DEFAULT_ALARM_RINGTONE);
        mAutoSilencePref = findPreference(KEY_AUTO_SILENCE);
        mAlarmSnoozePref = findPreference(KEY_ALARM_SNOOZE);
        mAlarmVolumePreference = findPreference(KEY_ALARM_VOLUME_SETTING);
        mAlarmCrescendoPref = findPreference(KEY_ALARM_CRESCENDO);
        mSwipeActionPref = findPreference(KEY_SWIPE_ACTION);
        mVolumeButtonsPref = findPreference(KEY_VOLUME_BUTTONS);
        mPowerButtonPref = findPreference(KEY_POWER_BUTTON);
        mFlipActionPref = findPreference(KEY_FLIP_ACTION);
        mShakeActionPref = findPreference(KEY_SHAKE_ACTION);
        mShakeIntensityPref = findPreference(KEY_SHAKE_INTENSITY);
        mWeekStartPref = findPreference(KEY_WEEK_START);
        mAlarmNotificationReminderTimePref = findPreference(KEY_ALARM_NOTIFICATION_REMINDER_TIME);
        mEnableAlarmVibrationsByDefaultPref = findPreference(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT);
        mEnableSnoozedOrDismissedAlarmVibrationsPref = findPreference(KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS);
        mTurnOnBackFlashForTriggeredAlarmPref = findPreference(KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM);
        mDeleteOccasionalAlarmByDefaultPref = findPreference(KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT);
        mMaterialTimePickerStylePref = findPreference(KEY_MATERIAL_TIME_PICKER_STYLE);
        mAlarmDisplayCustomizationPref = findPreference(KEY_ALARM_DISPLAY_CUSTOMIZATION);

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
    public void onStop() {
        super.onStop();

        mAlarmVolumePreference.stopRingtonePreview(requireContext());
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_DEFAULT_ALARM_RINGTONE ->
                    pref.setSummary(DataModel.getDataModel().getAlarmRingtoneTitle());

            case KEY_AUTO_SILENCE -> {
                final String delay = (String) newValue;
                updateAutoSnoozeSummary((ListPreference) pref, delay);
            }

            case KEY_SWIPE_ACTION, KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS,
                 KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM, KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT ->
                    Utils.setVibrationTime(requireContext(), 50);

            case KEY_ALARM_SNOOZE, KEY_ALARM_CRESCENDO, KEY_VOLUME_BUTTONS,
                 KEY_POWER_BUTTON, KEY_FLIP_ACTION, KEY_ALARM_NOTIFICATION_REMINDER_TIME,
                 KEY_MATERIAL_TIME_PICKER_STYLE -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_SHAKE_ACTION -> {
                final ListPreference shakeActionPref = (ListPreference) pref;
                final int index = shakeActionPref.findIndexOfValue((String) newValue);
                shakeActionPref.setSummary(shakeActionPref.getEntries()[index]);
                // index == 2 --> Nothing
                mShakeIntensityPref.setVisible(index != 2);
            }

            case KEY_WEEK_START -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
                // Set result so DeskClock knows to refresh itself
                requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
            }

            case KEY_SHAKE_INTENSITY -> {
                final int progress = (int) newValue;
                if (progress == 16) {
                    mShakeIntensityPref.setSummary(R.string.label_default);
                } else {
                    mShakeIntensityPref.setSummary(String.valueOf(progress - 15));
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
            case KEY_DEFAULT_ALARM_RINGTONE ->
                    startActivity(RingtonePickerActivity.createAlarmRingtonePickerIntentForSettings(context));

            case KEY_ALARM_DISPLAY_CUSTOMIZATION -> animateAndShowFragment(new AlarmDisplayCustomizationFragment());
        }

        return true;
    }

    private void setupPreferences() {
        final boolean hasVibrator = ((Vibrator) requireActivity().getSystemService(VIBRATOR_SERVICE)).hasVibrator();
        mEnableAlarmVibrationsByDefaultPref.setVisible(hasVibrator);
        mEnableSnoozedOrDismissedAlarmVibrationsPref.setVisible(hasVibrator);

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
            final int shakeActionIndex = mShakeActionPref.findIndexOfValue(
                    String.valueOf(DataModel.getDataModel().getShakeAction()));
            mShakeIntensityPref.setVisible(shakeActionIndex != 2);
            mShakeIntensityPref.setMin(16);
            if (mShakeIntensityPref.getMin() == 16) {
                mShakeIntensityPref.setSummary(R.string.label_default);
            }

        }

        mTurnOnBackFlashForTriggeredAlarmPref.setVisible(AlarmUtils.hasBackFlash(requireContext()));
    }

    private void refresh() {
        mAlarmRingtonePref.setOnPreferenceClickListener(this);
        mAlarmRingtonePref.setSummary(DataModel.getDataModel().getAlarmRingtoneTitle());

        String delay = mAutoSilencePref.getValue();
        updateAutoSnoozeSummary(mAutoSilencePref, delay);
        mAutoSilencePref.setOnPreferenceChangeListener(this);

        mAlarmSnoozePref.setOnPreferenceChangeListener(this);
        mAlarmSnoozePref.setSummary(mAlarmSnoozePref.getEntry());

        mAlarmCrescendoPref.setOnPreferenceChangeListener(this);
        mAlarmCrescendoPref.setSummary(mAlarmCrescendoPref.getEntry());

        mSwipeActionPref.setChecked(DataModel.getDataModel().isSwipeActionEnabled());
        mSwipeActionPref.setOnPreferenceChangeListener(this);

        mVolumeButtonsPref.setOnPreferenceChangeListener(this);
        mVolumeButtonsPref.setSummary(mVolumeButtonsPref.getEntry());

        mPowerButtonPref.setOnPreferenceChangeListener(this);
        mPowerButtonPref.setSummary(mPowerButtonPref.getEntry());

        final int intensity = DataModel.getDataModel().getShakeIntensity();
        mShakeIntensityPref.setValue(intensity);
        if (intensity == 16) {
            mShakeIntensityPref.setSummary(R.string.label_default);
        } else {
            mShakeIntensityPref.setSummary(String.valueOf(intensity - 15));
        }
        mShakeIntensityPref.setOnPreferenceChangeListener(this);
        mShakeIntensityPref.setUpdatesContinuously(true);

        // Set the default first day of the week programmatically
        final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
        final Integer firstDay = weekdayOrder.getCalendarDays().get(0);
        final String value = String.valueOf(firstDay);
        final int index = mWeekStartPref.findIndexOfValue(value);
        mWeekStartPref.setValueIndex(index);
        mWeekStartPref.setSummary(mWeekStartPref.getEntries()[index]);
        mWeekStartPref.setOnPreferenceChangeListener(this);

        mAlarmNotificationReminderTimePref.setOnPreferenceChangeListener(this);
        mAlarmNotificationReminderTimePref.setSummary(mAlarmNotificationReminderTimePref.getEntry());

        mEnableAlarmVibrationsByDefaultPref.setChecked(DataModel.getDataModel().areAlarmVibrationsEnabledByDefault());
        mEnableAlarmVibrationsByDefaultPref.setOnPreferenceChangeListener(this);

        mEnableSnoozedOrDismissedAlarmVibrationsPref.setChecked(DataModel.getDataModel().areSnoozedOrDismissedAlarmVibrationsEnabled());
        mEnableSnoozedOrDismissedAlarmVibrationsPref.setOnPreferenceChangeListener(this);

        mTurnOnBackFlashForTriggeredAlarmPref.setChecked(DataModel.getDataModel().shouldTurnOnBackFlashForTriggeredAlarm());
        mTurnOnBackFlashForTriggeredAlarmPref.setOnPreferenceChangeListener(this);

        mDeleteOccasionalAlarmByDefaultPref.setChecked(DataModel.getDataModel().isOccasionalAlarmDeletedByDefault());
        mDeleteOccasionalAlarmByDefaultPref.setOnPreferenceChangeListener(this);

        mMaterialTimePickerStylePref.setOnPreferenceChangeListener(this);
        mMaterialTimePickerStylePref.setSummary(mMaterialTimePickerStylePref.getEntry());

        mAlarmDisplayCustomizationPref.setOnPreferenceClickListener(this);
    }

    private void updateAutoSnoozeSummary(ListPreference listPref, String delay) {
        int i = Integer.parseInt(delay);
        if (i == -1) {
            listPref.setSummary(R.string.auto_silence_never);
        } else if (i == -2) {
            listPref.setSummary(R.string.auto_silence_at_the_end_of_the_ringtone);
        } else {
            listPref.setSummary(Utils.getNumberFormattedQuantityString(requireActivity(),
                    R.plurals.auto_silence_summary, i));
        }
    }

}
