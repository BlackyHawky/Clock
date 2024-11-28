// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.TimeZones;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

import java.util.Objects;

public class ClockSettingsActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "clock_settings_fragment";

    public static final String KEY_CLOCK_STYLE = "key_clock_style";
    public static final String KEY_CLOCK_DISPLAY_SECONDS = "key_display_clock_seconds";
    public static final String KEY_AUTO_HOME_CLOCK = "key_automatic_home_clock";
    public static final String KEY_HOME_TIME_ZONE = "key_home_time_zone";
    public static final String KEY_DATE_TIME = "key_date_time";

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

        ListPreference mHomeTimeZonePref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_clock);

            loadTimeZoneList();
        }

        @Override
        public void onResume() {
            super.onResume();

            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_CLOCK_STYLE, KEY_HOME_TIME_ZONE -> {
                    final ListPreference preference = (ListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                }

                case KEY_CLOCK_DISPLAY_SECONDS -> {
                    DataModel.getDataModel().setDisplayClockSeconds((boolean) newValue);
                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_AUTO_HOME_CLOCK -> {
                    final boolean autoHomeClockEnabled = ((TwoStatePreference) pref).isChecked();
                    final Preference homeTimeZonePref = findPreference(KEY_HOME_TIME_ZONE);
                    Objects.requireNonNull(homeTimeZonePref).setEnabled(!autoHomeClockEnabled);
                    Utils.setVibrationTime(requireContext(), 50);
                }
            }

            // Set result so DeskClock knows to refresh itself
            requireActivity().setResult(RESULT_OK);
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

        /**
         * Reconstruct the timezone list.
         */
        private void loadTimeZoneList() {
            final TimeZones timezones = DataModel.getDataModel().getTimeZones();
            mHomeTimeZonePref = findPreference(KEY_HOME_TIME_ZONE);
            assert mHomeTimeZonePref != null;
            mHomeTimeZonePref.setEntryValues(timezones.getTimeZoneIds());
            mHomeTimeZonePref.setEntries(timezones.getTimeZoneNames());
            mHomeTimeZonePref.setSummary(mHomeTimeZonePref.getEntry());
        }

        private void refresh() {
            final ListPreference clockStylePref = findPreference(KEY_CLOCK_STYLE);
            Objects.requireNonNull(clockStylePref).setSummary(clockStylePref.getEntry());
            clockStylePref.setOnPreferenceChangeListener(this);

            final Preference clockSecondsPref = findPreference(KEY_CLOCK_DISPLAY_SECONDS);
            Objects.requireNonNull(clockSecondsPref).setOnPreferenceChangeListener(this);

            final Preference autoHomeClockPref = findPreference(KEY_AUTO_HOME_CLOCK);
            final boolean autoHomeClockEnabled =
                    ((TwoStatePreference) Objects.requireNonNull(autoHomeClockPref)).isChecked();
            autoHomeClockPref.setOnPreferenceChangeListener(this);

            mHomeTimeZonePref.setEnabled(autoHomeClockEnabled);
            mHomeTimeZonePref.setSummary(mHomeTimeZonePref.getEntry());
            mHomeTimeZonePref.setOnPreferenceChangeListener(this);

            final Preference dateAndTimeSetting = findPreference(KEY_DATE_TIME);
            Objects.requireNonNull(dateAndTimeSetting).setOnPreferenceClickListener(this);
        }
    }
}
