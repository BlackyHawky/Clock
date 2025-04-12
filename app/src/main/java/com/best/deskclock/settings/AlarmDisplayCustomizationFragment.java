// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BACKGROUND_AMOLED_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_CLOCK_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_CLOCK_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SECONDS_HAND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_ALARM_SECONDS_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_RINGTONE_TITLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_PREVIEW_ALARM;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;
import com.rarepebble.colorpicker.ColorPreference;

public class AlarmDisplayCustomizationFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    String[] mAlarmClockStyleValues;
    String mAnalogClock;
    String mMaterialAnalogClock;
    String mDigitalClock;

    ListPreference mAlarmClockStyle;
    SwitchPreferenceCompat mDisplaySecondsPref;
    ColorPreference mAlarmClockColor;
    ColorPreference mAlarmSecondsHandColorPref;
    ColorPreference mBackgroundColorPref;
    ColorPreference mBackgroundAmoledColorPref;
    CustomSeekbarPreference mAlarmDigitalClockFontSizePref;
    SwitchPreferenceCompat mDisplayRingtoneTitlePref;
    Preference mPreviewAlarmPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.alarm_display_customization_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_alarm_display);

        mAlarmClockStyle = findPreference(KEY_ALARM_CLOCK_STYLE);
        mDisplaySecondsPref = findPreference(KEY_DISPLAY_ALARM_SECONDS_HAND);
        mBackgroundColorPref = findPreference(KEY_ALARM_BACKGROUND_COLOR);
        mBackgroundAmoledColorPref = findPreference(KEY_ALARM_BACKGROUND_AMOLED_COLOR);
        mAlarmClockColor = findPreference(KEY_ALARM_CLOCK_COLOR);
        mAlarmSecondsHandColorPref = findPreference(KEY_ALARM_SECONDS_HAND_COLOR);
        mAlarmDigitalClockFontSizePref = findPreference(KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE);
        mDisplayRingtoneTitlePref = findPreference(KEY_DISPLAY_RINGTONE_TITLE);
        mPreviewAlarmPref = findPreference(KEY_PREVIEW_ALARM);

        mAlarmClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
        mAnalogClock = mAlarmClockStyleValues[0];
        mMaterialAnalogClock = mAlarmClockStyleValues[1];
        mDigitalClock = mAlarmClockStyleValues[2];

        setupPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_ALARM_CLOCK_STYLE -> {
                final int clockIndex = mAlarmClockStyle.findIndexOfValue((String) newValue);
                mAlarmClockStyle.setSummary(mAlarmClockStyle.getEntries()[clockIndex]);
                mAlarmClockColor.setVisible(!newValue.equals(mMaterialAnalogClock));
                mAlarmDigitalClockFontSizePref.setVisible(newValue.equals(mDigitalClock));
                mDisplaySecondsPref.setVisible(!newValue.equals(mDigitalClock));
                mAlarmSecondsHandColorPref.setVisible(newValue.equals(mAnalogClock)
                        && SettingsDAO.isAlarmSecondsHandDisplayed(mPrefs));
            }

            case KEY_DISPLAY_ALARM_SECONDS_HAND -> {
                mAlarmSecondsHandColorPref.setVisible((boolean) newValue
                        && SettingsDAO.getAlarmClockStyle(mPrefs) != DataModel.ClockStyle.ANALOG_MATERIAL);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DISPLAY_RINGTONE_TITLE -> Utils.setVibrationTime(requireContext(), 50);
        }

        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        final Context context = getActivity();
        if (context == null) {
            return false;
        }

        if (pref.getKey().equals(KEY_PREVIEW_ALARM)) {
            startActivity(new Intent(context, AlarmDisplayPreviewActivity.class));
            final boolean isFadeTransitionsEnabled = SettingsDAO.isFadeTransitionsEnabled(mPrefs);
            if (isFadeTransitionsEnabled) {
                requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            } else {
                requireActivity().overridePendingTransition(R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
            }
        }

        return true;
    }

    private void setupPreferences() {
        mAlarmClockStyle.setSummary(mAlarmClockStyle.getEntry());
        mAlarmClockStyle.setOnPreferenceChangeListener(this);

        final boolean isAmoledMode = ThemeUtils.isNight(getResources())
                && SettingsDAO.getDarkMode(mPrefs).equals(AMOLED_DARK_MODE);
        mBackgroundAmoledColorPref.setVisible(isAmoledMode);

        mBackgroundColorPref.setVisible(!isAmoledMode);

        mAlarmClockColor.setVisible(!mAlarmClockStyle.getValue().equals(mMaterialAnalogClock));

        mDisplaySecondsPref.setVisible(!mAlarmClockStyle.getValue().equals(mDigitalClock));
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mAlarmSecondsHandColorPref.setVisible(mAlarmClockStyle.getValue().equals(mAnalogClock)
                && SettingsDAO.isAlarmSecondsHandDisplayed(mPrefs));
        if (mAlarmSecondsHandColorPref.isVisible()) {
            mAlarmSecondsHandColorPref.setColor(
                    MaterialColors.getColor(requireContext(), android.R.attr.colorPrimary, Color.BLACK));
        }

        mAlarmDigitalClockFontSizePref.setVisible(mAlarmClockStyle.getValue().equals(mDigitalClock));

        mDisplayRingtoneTitlePref.setOnPreferenceChangeListener(this);

        mPreviewAlarmPref.setOnPreferenceClickListener(this);
    }

}
