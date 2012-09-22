/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock;

import android.app.ActionBar;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Settings for the Alarm Clock.
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    private static final int ALARM_STREAM_TYPE_BIT =
            1 << AudioManager.STREAM_ALARM;

    static final String KEY_ALARM_IN_SILENT_MODE =
            "alarm_in_silent_mode";
    static final String KEY_ALARM_SNOOZE =
            "snooze_duration";
    static final String KEY_VOLUME_BEHAVIOR =
            "volume_button_setting";
    static final String KEY_AUTO_SILENCE =
            "auto_silence";
    static final String KEY_CLOCK_STYLE =
            "clock_style";
    static final String KEY_HOME_TZ =
            "home_time_zone";
    static final String KEY_AUTO_HOME_CLOCK =
            "automatic_home_clock";
    static final String KEY_VOLUME_BUTTONS =
            "volume_button_setting";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

    /*    final AlarmPreference ringtone =
                (AlarmPreference) findPreference(KEY_DEFAULT_RINGTONE);
        Uri alert = RingtoneManager.getActualDefaultRingtoneUri(this,
                RingtoneManager.TYPE_ALARM);
        if (alert != null) {
            ringtone.setAlert(alert);
        }
        ringtone.setChangeDefault();*/
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (KEY_ALARM_IN_SILENT_MODE.equals(preference.getKey())) {
            CheckBoxPreference pref = (CheckBoxPreference) preference;
            int ringerModeStreamTypes = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);

            if (pref.isChecked()) {
                ringerModeStreamTypes &= ~ALARM_STREAM_TYPE_BIT;
            } else {
                ringerModeStreamTypes |= ALARM_STREAM_TYPE_BIT;
            }

            Settings.System.putInt(getContentResolver(),
                    Settings.System.MODE_RINGER_STREAMS_AFFECTED,
                    ringerModeStreamTypes);

            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (KEY_AUTO_SILENCE.equals(pref.getKey())) {
            final ListPreference listPref = (ListPreference) pref;
            String delay = (String) newValue;
            updateAutoSnoozeSummary(listPref, delay);
        } else if (KEY_CLOCK_STYLE.equals(pref.getKey())) {
            final ListPreference listPref = (ListPreference) pref;
            final int idx = listPref.findIndexOfValue((String) newValue);
            listPref.setSummary(listPref.getEntries()[idx]);
        } else if (KEY_HOME_TZ.equals(pref.getKey())) {
            final ListPreference listPref = (ListPreference) pref;
            final int idx = listPref.findIndexOfValue((String) newValue);
            listPref.setSummary(listPref.getEntries()[idx]);
        } else if (KEY_AUTO_HOME_CLOCK.equals(pref.getKey())) {
            boolean state =((CheckBoxPreference) pref).isChecked();
            Preference homeTimeZone = findPreference(KEY_HOME_TZ);
            homeTimeZone.setEnabled(!state);
        } else if (KEY_VOLUME_BUTTONS.equals(pref.getKey())) {
            final ListPreference listPref = (ListPreference) pref;
            final int idx = listPref.findIndexOfValue((String) newValue);
            listPref.setSummary(listPref.getEntries()[idx]);
        }
        return true;
    }

    private void updateAutoSnoozeSummary(ListPreference listPref,
            String delay) {
        int i = Integer.parseInt(delay);
        if (i == -1) {
            listPref.setSummary(R.string.auto_silence_never);
        } else {
            listPref.setSummary(getString(R.string.auto_silence_summary, i));
        }
    }

    private void refresh() {
        ListPreference listPref = (ListPreference) findPreference(KEY_AUTO_SILENCE);
        String delay = listPref.getValue();
        updateAutoSnoozeSummary(listPref, delay);
        listPref.setOnPreferenceChangeListener(this);

        listPref = (ListPreference) findPreference(KEY_CLOCK_STYLE);
        listPref.setSummary(listPref.getEntry());
        listPref.setOnPreferenceChangeListener(this);

        listPref = (ListPreference) findPreference(KEY_HOME_TZ);
        listPref.setSummary(listPref.getEntry());
        listPref.setOnPreferenceChangeListener(this);

        Preference pref = findPreference(KEY_AUTO_HOME_CLOCK);
        boolean state =((CheckBoxPreference) pref).isChecked();
        pref.setOnPreferenceChangeListener(this);

        listPref = (ListPreference)findPreference(KEY_HOME_TZ);
        listPref.setEnabled(state);
        listPref.setSummary(listPref.getEntry());

        listPref = (ListPreference) findPreference(KEY_VOLUME_BUTTONS);
        listPref.setSummary(listPref.getEntry());
        listPref.setOnPreferenceChangeListener(this);

        SnoozeLengthDialog snoozePref = (SnoozeLengthDialog) findPreference(KEY_ALARM_SNOOZE);
        snoozePref.setSummary();
    }

}
