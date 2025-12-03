// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_TIMER_RINGTONE_TITLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_TIMER_STATE_INDICATOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_EXPIRED_TIMER_INDICATOR_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_PAUSED_TIMER_INDICATOR_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_RUNNING_TIMER_INDICATOR_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_DISPLAY_TEXT_SHADOW;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_RINGTONE_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHADOW_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHADOW_OFFSET;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.Utils;

public class TimerDisplayCustomizationFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    SwitchPreferenceCompat mTransparentBackgroundPref;
    SwitchPreferenceCompat mDisplayTimerStateIndicatorPref;
    ColorPickerPreference mRunningTimerIndicatorColorPref;
    ColorPickerPreference mPausedTimerIndicatorColorPref;
    ColorPickerPreference mExpiredTimerIndicatorColorPref;
    SwitchPreferenceCompat mDisplayRingtoneTitlePref;
    ColorPickerPreference mRingtoneTitleColorPref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    ColorPickerPreference mShadowColorPref;
    Preference mShadowOffsetPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.display_settings_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_timer_display);

        mTransparentBackgroundPref = findPreference(KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER);
        mDisplayTimerStateIndicatorPref = findPreference(KEY_DISPLAY_TIMER_STATE_INDICATOR);
        mRunningTimerIndicatorColorPref = findPreference(KEY_RUNNING_TIMER_INDICATOR_COLOR);
        mPausedTimerIndicatorColorPref = findPreference(KEY_PAUSED_TIMER_INDICATOR_COLOR);
        mExpiredTimerIndicatorColorPref = findPreference(KEY_EXPIRED_TIMER_INDICATOR_COLOR);
        mDisplayRingtoneTitlePref = findPreference(KEY_DISPLAY_TIMER_RINGTONE_TITLE);
        mRingtoneTitleColorPref = findPreference(KEY_TIMER_RINGTONE_TITLE_COLOR);
        mDisplayTextShadowPref = findPreference(KEY_TIMER_DISPLAY_TEXT_SHADOW);
        mShadowColorPref = findPreference(KEY_TIMER_SHADOW_COLOR);
        mShadowOffsetPref = findPreference(KEY_TIMER_SHADOW_OFFSET);

        setupPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER ->
                    Utils.setVibrationTime(requireContext(), 50);

            case KEY_DISPLAY_TIMER_STATE_INDICATOR -> {
                boolean isTimerStateIndicatorDisplayed = (boolean) newValue;

                mRunningTimerIndicatorColorPref.setVisible(isTimerStateIndicatorDisplayed);
                mPausedTimerIndicatorColorPref.setVisible(isTimerStateIndicatorDisplayed);
                mExpiredTimerIndicatorColorPref.setVisible(isTimerStateIndicatorDisplayed);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DISPLAY_TIMER_RINGTONE_TITLE -> {
                boolean isRingtoneTitleDisplayed = (boolean) newValue;
                boolean isTextShadowDisplayed = SettingsDAO.isTimerTextShadowDisplayed(mPrefs);

                mRingtoneTitleColorPref.setVisible(isRingtoneTitleDisplayed);
                mDisplayTextShadowPref.setVisible(isRingtoneTitleDisplayed);
                mShadowColorPref.setVisible(isRingtoneTitleDisplayed && isTextShadowDisplayed);
                mShadowOffsetPref.setVisible(isRingtoneTitleDisplayed && isTextShadowDisplayed);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_TIMER_DISPLAY_TEXT_SHADOW -> {
                boolean displayTextShadow = (boolean) newValue;

                mShadowColorPref.setVisible(displayTextShadow);
                mShadowOffsetPref.setVisible(displayTextShadow);

                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference pref) {
        if (pref instanceof ColorPickerPreference colorPickerPref) {
            colorPickerPref.showDialog(this, 0);
        } else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    private void setupPreferences() {
        mTransparentBackgroundPref.setOnPreferenceChangeListener(this);

        mDisplayTimerStateIndicatorPref.setOnPreferenceChangeListener(this);

        mRunningTimerIndicatorColorPref.setVisible(SettingsDAO.isTimerStateIndicatorDisplayed(mPrefs));

        mPausedTimerIndicatorColorPref.setVisible(SettingsDAO.isTimerStateIndicatorDisplayed(mPrefs));

        mExpiredTimerIndicatorColorPref.setVisible(SettingsDAO.isTimerStateIndicatorDisplayed(mPrefs));

        mDisplayRingtoneTitlePref.setOnPreferenceChangeListener(this);

        final boolean isTimerRingtoneTitleDisplayed = SettingsDAO.isTimerRingtoneTitleDisplayed(mPrefs);
        final boolean isTimerTextShadowDisplayed = SettingsDAO.isTimerTextShadowDisplayed(mPrefs);

        mRingtoneTitleColorPref.setVisible(isTimerRingtoneTitleDisplayed);

        mDisplayTextShadowPref.setVisible(isTimerRingtoneTitleDisplayed);
        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mShadowColorPref.setVisible(isTimerRingtoneTitleDisplayed && isTimerTextShadowDisplayed);

        mShadowOffsetPref.setVisible(isTimerRingtoneTitleDisplayed && isTimerTextShadowDisplayed);
    }
}
