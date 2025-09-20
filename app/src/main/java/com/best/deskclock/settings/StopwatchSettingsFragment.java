// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_DOWN_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_UP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.best.deskclock.R;

public class StopwatchSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener {

    ListPreference mVolumeUpActionPref;
    ListPreference mVolumeUpActionAfterLongPressPref;
    ListPreference mVolumeDownActionPref;
    ListPreference mVolumeDownActionAfterLongPressPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.stopwatch_channel);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_stopwatch);

        mVolumeUpActionPref = findPreference(KEY_SW_VOLUME_UP_ACTION);
        mVolumeUpActionAfterLongPressPref = findPreference(KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS);
        mVolumeDownActionPref = findPreference(KEY_SW_VOLUME_DOWN_ACTION);
        mVolumeDownActionAfterLongPressPref = findPreference(KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS);

        setupPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_SW_VOLUME_UP_ACTION, KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS,
                 KEY_SW_VOLUME_DOWN_ACTION, KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }
        }

        return true;
    }

    private void setupPreferences() {
        mVolumeUpActionPref.setOnPreferenceChangeListener(this);
        mVolumeUpActionPref.setSummary(mVolumeUpActionPref.getEntry());

        mVolumeUpActionAfterLongPressPref.setOnPreferenceChangeListener(this);
        mVolumeUpActionAfterLongPressPref.setSummary(mVolumeUpActionAfterLongPressPref.getEntry());

        mVolumeDownActionPref.setOnPreferenceChangeListener(this);
        mVolumeDownActionPref.setSummary(mVolumeDownActionPref.getEntry());

        mVolumeDownActionAfterLongPressPref.setOnPreferenceChangeListener(this);
        mVolumeDownActionAfterLongPressPref.setSummary(mVolumeDownActionAfterLongPressPref.getEntry());
    }

}
