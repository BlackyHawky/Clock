/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v14.preference.PreferenceDialogFragment;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.deskclock.BaseActivity;
import com.android.deskclock.DropShadowController;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.RingtonePickerDialogFragment;
import com.android.deskclock.Utils;
import com.android.deskclock.actionbarmenu.OptionsMenuManager;
import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NavUpMenuItemController;
import com.android.deskclock.data.DataModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * Settings for the Alarm Clock.
 */
public final class SettingsActivity extends BaseActivity {

    public static final String KEY_ALARM_SNOOZE = "snooze_duration";
    public static final String KEY_ALARM_CRESCENDO = "alarm_crescendo_duration";
    public static final String KEY_TIMER_CRESCENDO = "timer_crescendo_duration";
    public static final String KEY_TIMER_RINGTONE = "timer_ringtone";
    public static final String KEY_TIMER_VIBRATE = "timer_vibrate";
    public static final String KEY_AUTO_SILENCE = "auto_silence";
    public static final String KEY_CLOCK_STYLE = "clock_style";
    public static final String KEY_HOME_TZ = "home_time_zone";
    public static final String KEY_AUTO_HOME_CLOCK = "automatic_home_clock";
    public static final String KEY_DATE_TIME = "date_time";
    public static final String KEY_VOLUME_BUTTONS = "volume_button_setting";
    public static final String KEY_WEEK_START = "week_start";

    public static final String DEFAULT_VOLUME_BEHAVIOR = "0";
    public static final String VOLUME_BEHAVIOR_SNOOZE = "1";
    public static final String VOLUME_BEHAVIOR_DISMISS = "2";

    public static final String PREFS_FRAGMENT_TAG = "prefs_fragment";
    public static final String PREFERENCE_DIALOG_FRAGMENT_TAG = "preference_dialog";

    private final OptionsMenuManager mOptionsMenuManager = new OptionsMenuManager();

    /**
     * The controller that shows the drop shadow when content is not scrolled to the top.
     */
    private DropShadowController mDropShadowController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        mOptionsMenuManager.addMenuItemController(new NavUpMenuItemController(this))
                .addMenuItemController(MenuItemControllerFactory.getInstance()
                        .buildMenuItemControllers(this));

        // Create the prefs fragment in code to ensure it's created before PreferenceDialogFragment
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.main, new PrefsFragment(), PREFS_FRAGMENT_TAG)
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final View dropShadow = findViewById(R.id.drop_shadow);
        final PrefsFragment fragment =
                (PrefsFragment) getFragmentManager().findFragmentById(R.id.main);
        mDropShadowController = new DropShadowController(dropShadow, fragment.getListView());
    }

    @Override
    protected void onPause() {
        mDropShadowController.stop();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mOptionsMenuManager.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mOptionsMenuManager.onOptionsItemSelected(item)
                || super.onOptionsItemSelected(item);
    }

    public static class PrefsFragment extends PreferenceFragment implements
            RingtonePickerDialogFragment.OnRingtoneSelectedListener,
            Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener {

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
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
        public void onDisplayPreferenceDialog(Preference preference) {
            final String key = preference.getKey();
            switch (key) {
                case KEY_ALARM_SNOOZE:
                    showDialog(SnoozeLengthDialogFragment.newInstance(preference));
                    break;
                case KEY_ALARM_CRESCENDO:
                case KEY_TIMER_CRESCENDO:
                    showDialog(CrescendoLengthDialogFragment.newInstance(preference));
                    break;
                default:
                    super.onDisplayPreferenceDialog(preference);
            }
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            final int idx;
            switch (pref.getKey()) {
                case KEY_AUTO_SILENCE:
                    String delay = (String) newValue;
                    updateAutoSnoozeSummary((ListPreference) pref, delay);
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
                    final boolean autoHomeClockEnabled = ((TwoStatePreference) pref).isChecked();
                    final Preference homeTimeZonePref = findPreference(KEY_HOME_TZ);
                    homeTimeZonePref.setEnabled(!autoHomeClockEnabled);
                    break;
                case KEY_VOLUME_BUTTONS:
                    final ListPreference volumeButtonsPref = (ListPreference) pref;
                    final int index = volumeButtonsPref.findIndexOfValue((String) newValue);
                    volumeButtonsPref.setSummary(volumeButtonsPref.getEntries()[index]);
                    break;
                case KEY_WEEK_START:
                    final ListPreference weekStartPref = (ListPreference) pref;
                    idx = weekStartPref.findIndexOfValue((String) newValue);
                    weekStartPref.setSummary(weekStartPref.getEntries()[idx]);
                    break;
                case KEY_TIMER_VIBRATE:
                    final TwoStatePreference timerVibratePref = (TwoStatePreference) pref;
                    DataModel.getDataModel().setTimerVibrate(timerVibratePref.isChecked());
                    break;
                case KEY_TIMER_RINGTONE:
                    pref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());
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
                case KEY_TIMER_RINGTONE:
                    new RingtonePickerDialogFragment.Builder()
                            .setTitle(R.string.timer_ringtone_title)
                            .setDefaultRingtoneTitle(R.string.default_timer_ringtone_title)
                            .setDefaultRingtoneUri(
                                    DataModel.getDataModel().getDefaultTimerRingtoneUri())
                            .setExistingRingtoneUri(DataModel.getDataModel().getTimerRingtoneUri())
                            .show(getChildFragmentManager(), PREFERENCE_DIALOG_FRAGMENT_TAG);
                default:
                    return false;
            }
        }

        @Override
        public void onRingtoneSelected(String tag, Uri ringtoneUri) {
            DataModel.getDataModel().setTimerRingtoneUri(ringtoneUri);

            // Manually call onPreferenceChange since PreferenceFragment doesn't listen to
            // external changes via SharedPreferences.
            onPreferenceChange(findPreference(KEY_TIMER_RINGTONE), ringtoneUri);
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
            final boolean autoHomeClockEnabled =
                    ((TwoStatePreference) autoHomeClockPref).isChecked();
            autoHomeClockPref.setOnPreferenceChangeListener(this);

            final ListPreference homeTimezonePref = (ListPreference) findPreference(KEY_HOME_TZ);
            homeTimezonePref.setEnabled(autoHomeClockEnabled);
            homeTimezonePref.setSummary(homeTimezonePref.getEntry());
            homeTimezonePref.setOnPreferenceChangeListener(this);

            final ListPreference volumeButtonsPref =
                    (ListPreference) findPreference(KEY_VOLUME_BUTTONS);
            volumeButtonsPref.setSummary(volumeButtonsPref.getEntry());
            volumeButtonsPref.setOnPreferenceChangeListener(this);

            ((SnoozeLengthDialogPreference) findPreference(KEY_ALARM_SNOOZE)).updateSummary();
            ((CrescendoLengthDialogPreference) findPreference(KEY_ALARM_CRESCENDO)).updateSummary();
            ((CrescendoLengthDialogPreference) findPreference(KEY_TIMER_CRESCENDO)).updateSummary();

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

            final Preference timerRingtonePref = findPreference(KEY_TIMER_RINGTONE);
            timerRingtonePref.setOnPreferenceClickListener(this);
            timerRingtonePref.setSummary(DataModel.getDataModel().getTimerRingtoneTitle());
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

        private void showDialog(PreferenceDialogFragment fragment) {
            // Always set the target fragment, this is required by PreferenceDialogFragment
            // internally.
            fragment.setTargetFragment(this, 0);
            // Don't use getChildFragmentManager(), it causes issues on older platforms when the
            // target fragment is being restored after an orientation change.
            fragment.show(getFragmentManager(), PREFERENCE_DIALOG_FRAGMENT_TAG);
        }

        private static class TimeZoneRow implements Comparable<TimeZoneRow> {

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
            public int compareTo(@NonNull TimeZoneRow another) {
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
                return name.toString();
            }
        }
    }
}
