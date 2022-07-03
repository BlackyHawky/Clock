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

package com.best.deskclock.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.DataModel.ThemeButtonBehavior;

/**
 * Settings for Clock screen saver
 */
public final class ScreensaverSettingsActivity extends AppCompatActivity {
    public static final String KEY_CLOCK_STYLE = "screensaver_clock_style";
    public static final String KEY_NIGHT_MODE = "screensaver_night_mode";
    private ThemeButtonBehavior mThemeBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeBehavior = DataModel.getDataModel().getThemeButtonBehavior();
        if (mThemeBehavior == DataModel.ThemeButtonBehavior.DARK) {
            getTheme().applyStyle(R.style.Theme_DeskClock_Actionbar_Dark, true);
        }
        if (mThemeBehavior == DataModel.ThemeButtonBehavior.LIGHT) {
            getTheme().applyStyle(R.style.Theme_DeskClock_Actionbar_Light, true);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screensaver_settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public static class PrefsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {

        @Override
        @TargetApi(Build.VERSION_CODES.N)
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (Utils.isNOrLater()) {
                getPreferenceManager().setStorageDeviceProtected();
            }
            addPreferencesFromResource(R.xml.screensaver_settings);
        }

        @Override
        public void onResume() {
            super.onResume();
            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            if (KEY_CLOCK_STYLE.equals(pref.getKey())) {
                final ListPreference clockStylePref = (ListPreference) pref;
                final int index = clockStylePref.findIndexOfValue((String) newValue);
                clockStylePref.setSummary(clockStylePref.getEntries()[index]);
            }
            return true;
        }

        private void refresh() {
            final ListPreference clockStylePref = (ListPreference) findPreference(KEY_CLOCK_STYLE);
            clockStylePref.setSummary(clockStylePref.getEntry());
            clockStylePref.setOnPreferenceChangeListener(this);
        }
    }
}
