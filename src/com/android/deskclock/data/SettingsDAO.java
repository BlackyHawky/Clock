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

package com.android.deskclock.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.android.deskclock.R;
import com.android.deskclock.ScreensaverSettingsActivity;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.data.DataModel.CitySort;
import com.android.deskclock.data.DataModel.ClockStyle;

import java.util.TimeZone;

/**
 * This class encapsulates the storage of application preferences in {@link SharedPreferences}.
 */
final class SettingsDAO {

    private static final String KEY_SORT_PREFERENCE = "sort_preference";

    // Lazily instantiated and cached for the life of the application.
    private static SharedPreferences sPrefs;

    private SettingsDAO() {}

    /**
     * @return an enumerated value indicating the order in which cities are ordered
     */
    static CitySort getCitySort(Context context) {
        final int defaultSortOrdinal = CitySort.NAME.ordinal();
        final SharedPreferences prefs = getSharedPreferences(context);
        final int citySortOrdinal = prefs.getInt(KEY_SORT_PREFERENCE, defaultSortOrdinal);
        return CitySort.values()[citySortOrdinal];
    }

    /**
     * Adjust the sort order of cities.
     */
    static void toggleCitySort(Context context) {
        final CitySort oldSort = getCitySort(context);
        final CitySort newSort = oldSort == CitySort.NAME ? CitySort.UTC_OFFSET : CitySort.NAME;
        final SharedPreferences prefs = getSharedPreferences(context);
        prefs.edit().putInt(KEY_SORT_PREFERENCE, newSort.ordinal()).apply();
    }

    /**
     * @return {@code true} if a clock for the user's home timezone should be automatically
     *      displayed when it doesn't match the current timezone
     */
    static boolean getAutoShowHomeClock(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getBoolean(SettingsActivity.KEY_AUTO_HOME_CLOCK, false);
    }

    /**
     * @return the user's home timezone
     */
    static TimeZone getHomeTimeZone(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);
        final String defaultTimeZoneId = TimeZone.getDefault().getID();
        final String timeZoneId = prefs.getString(SettingsActivity.KEY_HOME_TZ, defaultTimeZoneId);
        return TimeZone.getTimeZone(timeZoneId);
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed in the app
     */
    static ClockStyle getClockStyle(Context context) {
        return getClockStyle(context, SettingsActivity.KEY_CLOCK_STYLE);
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed on the screensaver
     */
    static ClockStyle getScreensaverClockStyle(Context context) {
        return getClockStyle(context, ScreensaverSettingsActivity.KEY_CLOCK_STYLE);
    }

    /**
     * @return the uri of the selected ringtone or the {@code defaultUri} if no explicit selection
     *      has yet been made
     */
    static Uri getTimerRingtoneUri(Context context, Uri defaultUri) {
        final SharedPreferences prefs = getSharedPreferences(context);
        final String uriString = prefs.getString(SettingsActivity.KEY_TIMER_RINGTONE, null);
        return uriString == null ? defaultUri : Uri.parse(uriString);
    }

    private static ClockStyle getClockStyle(Context context, String prefKey) {
        final String defaultStyle = context.getString(R.string.default_clock_style);
        final SharedPreferences prefs = getSharedPreferences(context);
        final String clockStyle = prefs.getString(prefKey, defaultStyle);
        return ClockStyle.valueOf(clockStyle.toUpperCase());
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        }

        return sPrefs;
    }
}