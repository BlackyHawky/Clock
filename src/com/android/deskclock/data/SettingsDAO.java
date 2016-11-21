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
import android.provider.Settings;

import com.android.deskclock.R;
import com.android.deskclock.data.DataModel.CitySort;
import com.android.deskclock.data.DataModel.ClockStyle;
import com.android.deskclock.settings.ScreensaverSettingsActivity;
import com.android.deskclock.settings.SettingsActivity;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static com.android.deskclock.data.Weekdays.Order.MON_TO_SUN;
import static com.android.deskclock.data.Weekdays.Order.SAT_TO_FRI;
import static com.android.deskclock.data.Weekdays.Order.SUN_TO_SAT;
import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;

/**
 * This class encapsulates the storage of application preferences in {@link SharedPreferences}.
 */
final class SettingsDAO {

    /** Key to a preference that stores the preferred sort order of world cities. */
    private static final String KEY_SORT_PREFERENCE = "sort_preference";

    /** Key to a preference that stores the default ringtone for new alarms. */
    private static final String KEY_DEFAULT_ALARM_RINGTONE_URI = "default_alarm_ringtone_uri";

    private SettingsDAO() {}

    /**
     * @return an enumerated value indicating the order in which cities are ordered
     */
    static CitySort getCitySort() {
        final int defaultSortOrdinal = CitySort.NAME.ordinal();
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        final int citySortOrdinal = prefs.getInt(KEY_SORT_PREFERENCE, defaultSortOrdinal);
        return CitySort.values()[citySortOrdinal];
    }

    /**
     * Adjust the sort order of cities.
     */
    static void toggleCitySort() {
        final CitySort oldSort = getCitySort();
        final CitySort newSort = oldSort == CitySort.NAME ? CitySort.UTC_OFFSET : CitySort.NAME;
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        prefs.edit().putInt(KEY_SORT_PREFERENCE, newSort.ordinal()).apply();
    }

    /**
     * @return {@code true} if a clock for the user's home timezone should be automatically
     *      displayed when it doesn't match the current timezone
     */
    static boolean getAutoShowHomeClock() {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        return prefs.getBoolean(SettingsActivity.KEY_AUTO_HOME_CLOCK, false);
    }

    /**
     * @return the user's home timezone
     */
    static TimeZone getHomeTimeZone() {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        final String defaultTimeZoneId = TimeZone.getDefault().getID();
        final String timeZoneId = prefs.getString(SettingsActivity.KEY_HOME_TZ, defaultTimeZoneId);
        return TimeZone.getTimeZone(timeZoneId);
    }

    /**
     * Sets the user's home timezone to the current system timezone if no home timezone is yet set.
     *
     * @param homeTimeZone the timezone to set as the user's home timezone if necessary
     */
    static void setDefaultHomeTimeZone(TimeZone homeTimeZone) {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        final String homeTimeZoneId = prefs.getString(SettingsActivity.KEY_HOME_TZ, null);
        if (homeTimeZoneId == null) {
            prefs.edit().putString(SettingsActivity.KEY_HOME_TZ, homeTimeZone.getID()).apply();
        }
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed in the app
     */
    static ClockStyle getClockStyle(Context context) {
        return getClockStyle(context, SettingsActivity.KEY_CLOCK_STYLE);
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed in the app
     */
    static boolean getDisplayClockSeconds() {
       return DataModel.getSharedPreferences().getBoolean(
               SettingsActivity.KEY_CLOCK_DISPLAY_SECONDS, false);
    }

    /**
     * @param displaySeconds whether or not to display seconds on main clock
     */
    static void setDisplayClockSeconds(boolean displaySeconds) {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        prefs.edit().putBoolean(SettingsActivity.KEY_CLOCK_DISPLAY_SECONDS, displaySeconds).apply();
    }

    /**
     * Sets the user's display seconds preference based on the currently selected clock if one has
     * not yet been manually chosen.
     */
    static void setDefaultDisplayClockSeconds(Context context) {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        if (!prefs.contains(SettingsActivity.KEY_CLOCK_DISPLAY_SECONDS)) {
            // If on analog clock style on upgrade, default to true. Otherwise, default to false.
            final boolean isAnalog = getClockStyle(context) == ClockStyle.ANALOG;
            setDisplayClockSeconds(isAnalog);
        }
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed on the screensaver
     */
    static ClockStyle getScreensaverClockStyle(Context context) {
        return getClockStyle(context, ScreensaverSettingsActivity.KEY_CLOCK_STYLE);
    }

    /**
     * @return {@code true} if the screen saver should be dimmed for lower contrast at night
     */
    static boolean getScreensaverNightModeOn() {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        return prefs.getBoolean(ScreensaverSettingsActivity.KEY_NIGHT_MODE, false);
    }

    /**
     * @return the uri of the selected ringtone or the {@code defaultUri} if no explicit selection
     *      has yet been made
     */
    static Uri getTimerRingtoneUri(Uri defaultUri) {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        final String uriString = prefs.getString(SettingsActivity.KEY_TIMER_RINGTONE, null);
        return uriString == null ? defaultUri : Uri.parse(uriString);
    }

    /**
     * @return whether timer vibration is enabled. false by default.
     */
    static boolean getTimerVibrate() {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        return prefs.getBoolean(SettingsActivity.KEY_TIMER_VIBRATE, false);
    }

    /**
     * @param enabled whether vibration will be turned on for all timers.
     */
    static void setTimerVibrate(boolean enabled) {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        prefs.edit().putBoolean(SettingsActivity.KEY_TIMER_VIBRATE, enabled).apply();
    }

    /**
     * @param uri the uri of the ringtone to play for all timers
     */
    static void setTimerRingtoneUri(Uri uri) {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        prefs.edit().putString(SettingsActivity.KEY_TIMER_RINGTONE, uri.toString()).apply();
    }

    /**
     * @return the uri of the selected ringtone or the {@code defaultUri} if no explicit selection
     *      has yet been made
     */
    static Uri getDefaultAlarmRingtoneUri() {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        final String uriString = prefs.getString(KEY_DEFAULT_ALARM_RINGTONE_URI, null);
        return uriString == null ? Settings.System.DEFAULT_ALARM_ALERT_URI : Uri.parse(uriString);
    }

    /**
     * @param uri identifies the default ringtone to play for new alarms
     */
    static void setDefaultAlarmRingtoneUri(Uri uri) {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        prefs.edit().putString(KEY_DEFAULT_ALARM_RINGTONE_URI, uri.toString()).apply();
    }

    /**
     * @return the display order of the weekdays, which can start with {@link Calendar#SATURDAY},
     *      {@link Calendar#SUNDAY} or {@link Calendar#MONDAY}
     */
    static Weekdays.Order getWeekdayOrder() {
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        final String defaultValue = String.valueOf(Calendar.getInstance().getFirstDayOfWeek());
        final String value = prefs.getString(SettingsActivity.KEY_WEEK_START, defaultValue);
        final int firstCalendarDay = Integer.parseInt(value);
        switch (firstCalendarDay) {
            case SATURDAY: return SAT_TO_FRI;
            case SUNDAY: return SUN_TO_SAT;
            case MONDAY: return MON_TO_SUN;
            default:
                throw new IllegalArgumentException("Unknown weekday: " + firstCalendarDay);
        }
    }

    private static ClockStyle getClockStyle(Context context, String prefKey) {
        final String defaultStyle = context.getString(R.string.default_clock_style);
        final SharedPreferences prefs = DataModel.getSharedPreferences();
        final String clockStyle = prefs.getString(prefKey, defaultStyle);
        // Use hardcoded locale to perform toUpperCase, because in some languages toUpperCase adds
        // accent to character, which breaks the enum conversion.
        return ClockStyle.valueOf(clockStyle.toUpperCase(Locale.US));
    }
}