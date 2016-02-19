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

package com.android.deskclock.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.android.deskclock.BaseActivity;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.actionbarmenu.ActionBarMenuManager;
import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NavUpMenuItemController;
import com.android.deskclock.data.DataModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Settings for the Alarm Clock.
 */
public final class SettingsActivity extends BaseActivity {

    public static final String KEY_ALARM_SNOOZE = "snooze_duration";
    public static final String KEY_ALARM_VOLUME = "volume_setting";
    public static final String KEY_ALARM_CRESCENDO = "alarm_crescendo_duration";
    public static final String KEY_TIMER_CRESCENDO = "timer_crescendo_duration";
    public static final String KEY_TIMER_RINGTONE = "timer_ringtone";
    public static final String KEY_AUTO_SILENCE = "auto_silence";
    public static final String KEY_CLOCK_STYLE = "clock_style";
    public static final String KEY_HOME_TZ = "home_time_zone";
    public static final String KEY_AUTO_HOME_CLOCK = "automatic_home_clock";
    public static final String KEY_DATE_TIME = "date_time";
    public static final String KEY_VOLUME_BUTTONS = "volume_button_setting";
    public static final String KEY_WEEK_START = "week_start";

    public static final String TIMEZONE_LOCALE = "tz_locale";

    public static final String DEFAULT_VOLUME_BEHAVIOR = "0";
    public static final String VOLUME_BEHAVIOR_SNOOZE = "1";
    public static final String VOLUME_BEHAVIOR_DISMISS = "2";

    private final ActionBarMenuManager mActionBarMenuManager = new ActionBarMenuManager(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_ALARM);
        setContentView(R.layout.settings);
        mActionBarMenuManager.addMenuItemController(new NavUpMenuItemController(this))
            .addMenuItemController(MenuItemControllerFactory.getInstance()
                    .buildMenuItemControllers(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mActionBarMenuManager.createOptionsMenu(menu, getMenuInflater());
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mActionBarMenuManager.prepareShowMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mActionBarMenuManager.handleMenuItemClick(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PrefsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings);
            loadTimeZoneList();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // By default, do not recreate the DeskClock activity
            getActivity().setResult(RESULT_CANCELED);

        }

        @Override
        public void onResume() {
            super.onResume();

            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            final int idx;
            switch (pref.getKey()) {
                case KEY_AUTO_SILENCE:
                    final ListPreference autoSilencePref = (ListPreference) pref;
                    String delay = (String) newValue;
                    updateAutoSnoozeSummary(autoSilencePref, delay);
                    break;
                case KEY_CLOCK_STYLE:
                    final ListPreference clockStylePref = (ListPreference) pref;
                    idx = clockStylePref.findIndexOfValue((String) newValue);
                    clockStylePref.setSummary(clockStylePref.getEntries()[idx]);
                    break;
                case KEY_HOME_TZ:
                    final ListPreference homeTimezonePref = (ListPreference) pref;
                    idx = homeTimezonePref.findIndexOfValue((String) newValue);
                    homeTimezonePref.setSummary(homeTimezonePref.getEntries()[idx]);
                    break;
                case KEY_AUTO_HOME_CLOCK:
                    final boolean autoHomeClockEnabled = ((SwitchPreference) pref).isChecked();
                    final Preference homeTimeZonePref = findPreference(KEY_HOME_TZ);
                    homeTimeZonePref.setEnabled(!autoHomeClockEnabled);
                    break;
                case KEY_VOLUME_BUTTONS:
                    final ListPreference volumeButtonsPref = (ListPreference) pref;
                    final int index = volumeButtonsPref.findIndexOfValue((String) newValue);
                    volumeButtonsPref.setSummary(volumeButtonsPref.getEntries()[index]);
                    break;
                case KEY_WEEK_START:
                    final ListPreference weekStartPref = (ListPreference)
                            findPreference(KEY_WEEK_START);
                    idx = weekStartPref.findIndexOfValue((String) newValue);
                    weekStartPref.setSummary(weekStartPref.getEntries()[idx]);
                    break;
                case KEY_TIMER_RINGTONE:
                    final RingtonePreference timerRingtonePref = (RingtonePreference)
                            findPreference(KEY_TIMER_RINGTONE);
                    timerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());
                    break;
            }
            // Set result so DeskClock knows to refresh itself
            getActivity().setResult(RESULT_OK);
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference pref) {
            final Activity activity = getActivity();
            if (activity == null) {
                return false;
            }

            switch (pref.getKey()) {
                case KEY_DATE_TIME:
                    final Intent dialogIntent = new Intent(Settings.ACTION_DATE_SETTINGS);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogIntent);
                    return true;
                case KEY_ALARM_VOLUME:
                    final AudioManager audioManager =
                            (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
                    audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM,
                            AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Reconstruct the timezone list.
         */
        private void loadTimeZoneList() {
            final CharSequence[][] timezones = getAllTimezones();
            final ListPreference homeTimezonePref = (ListPreference) findPreference(KEY_HOME_TZ);
            homeTimezonePref.setEntryValues(timezones[0]);
            homeTimezonePref.setEntries(timezones[1]);
            homeTimezonePref.setSummary(homeTimezonePref.getEntry());
            homeTimezonePref.setOnPreferenceChangeListener(this);
        }

        /**
         * Returns an array of ids/time zones. This returns a double indexed array
         * of ids and time zones for Calendar. It is an inefficient method and
         * shouldn't be called often, but can be used for one time generation of
         * this list.
         *
         * @return double array of tz ids and tz names
         */
        public CharSequence[][] getAllTimezones() {
            final Resources res = getResources();
            final String[] ids = res.getStringArray(R.array.timezone_values);
            final String[] labels = res.getStringArray(R.array.timezone_labels);

            int minLength = ids.length;
            if (ids.length != labels.length) {
                minLength = Math.min(minLength, labels.length);
                LogUtils.e("Timezone ids and labels have different length!");
            }

            final long currentTimeMillis = System.currentTimeMillis();
            final List<TimeZoneRow> timezones = new ArrayList<>(minLength);
            for (int i = 0; i < minLength; i++) {
                timezones.add(new TimeZoneRow(ids[i], labels[i], currentTimeMillis));
            }
            Collections.sort(timezones);

            final CharSequence[][] timeZones = new CharSequence[2][timezones.size()];
            int i = 0;
            for (TimeZoneRow row : timezones) {
                timeZones[0][i] = row.mId;
                timeZones[1][i++] = row.mDisplayName;
            }
            return timeZones;
        }

        private void refresh() {
            final ListPreference autoSilencePref =
                    (ListPreference) findPreference(KEY_AUTO_SILENCE);
            String delay = autoSilencePref.getValue();
            updateAutoSnoozeSummary(autoSilencePref, delay);
            autoSilencePref.setOnPreferenceChangeListener(this);

            final ListPreference clockStylePref = (ListPreference) findPreference(KEY_CLOCK_STYLE);
            clockStylePref.setSummary(clockStylePref.getEntry());
            clockStylePref.setOnPreferenceChangeListener(this);

            final Preference autoHomeClockPref = findPreference(KEY_AUTO_HOME_CLOCK);
            final boolean autoHomeClockEnabled = ((SwitchPreference) autoHomeClockPref).isChecked();
            autoHomeClockPref.setOnPreferenceChangeListener(this);

            final ListPreference homeTimezonePref = (ListPreference) findPreference(KEY_HOME_TZ);
            homeTimezonePref.setEnabled(autoHomeClockEnabled);
            homeTimezonePref.setSummary(homeTimezonePref.getEntry());
            homeTimezonePref.setOnPreferenceChangeListener(this);

            final ListPreference volumeButtonsPref =
                    (ListPreference) findPreference(KEY_VOLUME_BUTTONS);
            volumeButtonsPref.setSummary(volumeButtonsPref.getEntry());
            volumeButtonsPref.setOnPreferenceChangeListener(this);

            final Preference volumePref = findPreference(KEY_ALARM_VOLUME);
            volumePref.setOnPreferenceClickListener(this);

            final SnoozeLengthDialog snoozePref =
                    (SnoozeLengthDialog) findPreference(KEY_ALARM_SNOOZE);
            snoozePref.setSummary();

            final CrescendoLengthDialog alarmCrescendoPref =
                    (CrescendoLengthDialog) findPreference(KEY_ALARM_CRESCENDO);
            alarmCrescendoPref.setSummary();

            final CrescendoLengthDialog timerCrescendoPref =
                    (CrescendoLengthDialog) findPreference(KEY_TIMER_CRESCENDO);
            timerCrescendoPref.setSummary();

            final Preference dateAndTimeSetting = findPreference(KEY_DATE_TIME);
            dateAndTimeSetting.setOnPreferenceClickListener(this);

            final ListPreference weekStartPref = (ListPreference) findPreference(KEY_WEEK_START);
            // Set the default value programmatically
            final String value = weekStartPref.getValue();
            final int idx = weekStartPref.findIndexOfValue(
                    value == null ? String.valueOf(Utils.DEFAULT_WEEK_START) : value);
            weekStartPref.setValueIndex(idx);
            weekStartPref.setSummary(weekStartPref.getEntries()[idx]);
            weekStartPref.setOnPreferenceChangeListener(this);

            final RingtonePreference timerRingtonePref =
                    (RingtonePreference) findPreference(KEY_TIMER_RINGTONE);
            timerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());
            timerRingtonePref.setOnPreferenceChangeListener(this);
        }

        private void updateAutoSnoozeSummary(ListPreference listPref, String delay) {
            int i = Integer.parseInt(delay);
            if (i == -1) {
                listPref.setSummary(R.string.auto_silence_never);
            } else {
                listPref.setSummary(Utils.getNumberFormattedQuantityString(getActivity(),
                        R.plurals.auto_silence_summary, i));
            }
        }

        private static class TimeZoneRow implements Comparable<TimeZoneRow> {

            private static final boolean SHOW_DAYLIGHT_SAVINGS_INDICATOR = false;

            public final String mId;
            public final String mDisplayName;
            public final int mOffset;

            public TimeZoneRow(String id, String name, long currentTimeMillis) {
                final TimeZone tz = TimeZone.getTimeZone(id);
                final boolean useDaylightTime = tz.useDaylightTime();
                mId = id;
                mOffset = tz.getOffset(currentTimeMillis);
                mDisplayName = buildGmtDisplayName(name, useDaylightTime);
            }

            @Override
            public int compareTo(TimeZoneRow another) {
                return mOffset - another.mOffset;
            }

            public String buildGmtDisplayName(String displayName, boolean useDaylightTime) {
                final int p = Math.abs(mOffset);
                final StringBuilder name = new StringBuilder("(GMT");
                name.append(mOffset < 0 ? '-' : '+');

                name.append(p / DateUtils.HOUR_IN_MILLIS);
                name.append(':');

                int min = p / 60000;
                min %= 60;

                if (min < 10) {
                    name.append('0');
                }
                name.append(min);
                name.append(") ");
                name.append(displayName);
                if (useDaylightTime && SHOW_DAYLIGHT_SAVINGS_INDICATOR) {
                    name.append(" \u2600"); // Sun symbol
                }
                return name.toString();
            }
        }
    }
}
