// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_HOME_CLOCK;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CLOCK_DIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CLOCK_DIAL_MATERIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CLOCK_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CLOCK_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DATE_TIME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_CLOCK_SECONDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_CITY_NOTE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_HOME_TIME_ZONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SORT_CITIES;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.TimeZones;
import com.best.deskclock.utils.Utils;

public class ClockSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    String[] mClockStyleValues;
    String mAnalogClock;
    String mMaterialAnalogClock;

    ListPreference mClockStylePref;
    ListPreference mClockDialPref;
    ListPreference mClockDialMaterialPref;
    ListPreference mClockSecondHandPref;
    SwitchPreferenceCompat mDisplayClockSecondsPref;
    SwitchPreferenceCompat mEnableCityNotePref;
    ListPreference mSortCitiesPref;
    SwitchPreferenceCompat mAutoHomeClockPref;
    ListPreference mHomeTimeZonePref;
    Preference mDateTimePref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.clock_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_clock);

        mClockStylePref = findPreference(KEY_CLOCK_STYLE);
        mClockDialPref = findPreference(KEY_CLOCK_DIAL);
        mClockDialMaterialPref = findPreference(KEY_CLOCK_DIAL_MATERIAL);
        mDisplayClockSecondsPref = findPreference(KEY_DISPLAY_CLOCK_SECONDS);
        mClockSecondHandPref = findPreference(KEY_CLOCK_SECOND_HAND);
        mEnableCityNotePref = findPreference(KEY_ENABLE_CITY_NOTE);
        mSortCitiesPref = findPreference(KEY_SORT_CITIES);
        mAutoHomeClockPref = findPreference(KEY_AUTO_HOME_CLOCK);
        mHomeTimeZonePref = findPreference(KEY_HOME_TIME_ZONE);
        mDateTimePref = findPreference(KEY_DATE_TIME);

        mClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
        mAnalogClock = mClockStyleValues[0];
        mMaterialAnalogClock = mClockStyleValues[1];

        setupPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_CLOCK_STYLE -> {
                final int clockIndex = mClockStylePref.findIndexOfValue((String) newValue);
                mClockStylePref.setSummary(mClockStylePref.getEntries()[clockIndex]);
                mClockDialPref.setVisible(newValue.equals(mAnalogClock));
                mClockDialMaterialPref.setVisible(newValue.equals(mMaterialAnalogClock));
                mClockSecondHandPref.setVisible(newValue.equals(mAnalogClock)
                        && SettingsDAO.areClockSecondsDisplayed(mPrefs));
            }

            case KEY_CLOCK_DIAL, KEY_CLOCK_DIAL_MATERIAL, KEY_CLOCK_SECOND_HAND, KEY_HOME_TIME_ZONE,
                 KEY_SORT_CITIES -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_DISPLAY_CLOCK_SECONDS -> {
                mClockSecondHandPref.setVisible((boolean) newValue
                        && SettingsDAO.getClockStyle(mPrefs) == DataModel.ClockStyle.ANALOG);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_AUTO_HOME_CLOCK -> {
                mHomeTimeZonePref.setEnabled((boolean) newValue);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ENABLE_CITY_NOTE -> Utils.setVibrationTime(requireContext(), 50);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        final Context context = getActivity();
        if (context == null) {
            return false;
        }

        if (pref.getKey().equals(KEY_DATE_TIME)) {
            final Intent dialogIntent = new Intent(Settings.ACTION_DATE_SETTINGS);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialogIntent);
            return true;
        }

        return false;
    }

    private void setupPreferences() {
        mClockStylePref.setSummary(mClockStylePref.getEntry());
        mClockStylePref.setOnPreferenceChangeListener(this);

        mClockDialPref.setVisible(mClockStylePref.getValue().equals(mAnalogClock));
        mClockDialPref.setSummary(mClockDialPref.getEntry());
        mClockDialPref.setOnPreferenceChangeListener(this);

        mClockDialMaterialPref.setVisible(mClockStylePref.getValue().equals(mMaterialAnalogClock));
        mClockDialMaterialPref.setSummary(mClockDialMaterialPref.getEntry());
        mClockDialMaterialPref.setOnPreferenceChangeListener(this);

        mDisplayClockSecondsPref.setOnPreferenceChangeListener(this);

        mClockSecondHandPref.setVisible(mClockStylePref.getValue().equals(mAnalogClock)
                && SettingsDAO.areClockSecondsDisplayed(mPrefs));
        mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
        mClockSecondHandPref.setOnPreferenceChangeListener(this);

        mSortCitiesPref.setSummary(mSortCitiesPref.getEntry());
        mSortCitiesPref.setOnPreferenceChangeListener(this);

        mEnableCityNotePref.setOnPreferenceChangeListener(this);

        mAutoHomeClockPref.setOnPreferenceChangeListener(this);

        mHomeTimeZonePref.setEnabled(SettingsDAO.getAutoShowHomeClock(mPrefs));
        // Reconstruct the timezone list.
        final TimeZones timezones = SettingsDAO.getTimeZones(requireContext(), System.currentTimeMillis());
        mHomeTimeZonePref.setEntryValues(timezones.getTimeZoneIds());
        mHomeTimeZonePref.setEntries(timezones.getTimeZoneNames());
        mHomeTimeZonePref.setSummary(mHomeTimeZonePref.getEntry());
        mHomeTimeZonePref.setOnPreferenceChangeListener(this);

        mDateTimePref.setOnPreferenceClickListener(this);
    }

}