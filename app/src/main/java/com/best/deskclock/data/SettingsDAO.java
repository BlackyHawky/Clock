/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import static com.best.deskclock.data.DataModel.PowerButtonBehavior.DISMISS;
import static com.best.deskclock.data.DataModel.PowerButtonBehavior.NOTHING;
import static com.best.deskclock.data.DataModel.PowerButtonBehavior.SNOOZE;
import static com.best.deskclock.data.DataModel.VolumeButtonBehavior.CHANGE_VOLUME;
import static com.best.deskclock.data.DataModel.VolumeButtonBehavior.DISMISS_ALARM;
import static com.best.deskclock.data.DataModel.VolumeButtonBehavior.DO_NOTHING;
import static com.best.deskclock.data.DataModel.VolumeButtonBehavior.SNOOZE_ALARM;
import static com.best.deskclock.data.Weekdays.Order.MON_TO_SUN;
import static com.best.deskclock.data.Weekdays.Order.SAT_TO_FRI;
import static com.best.deskclock.data.Weekdays.Order.SUN_TO_SAT;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.DEFAULT_ALARM_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.DEFAULT_ALARM_TITLE_FONT_SIZE;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_ALARM_BACKGROUND_AMOLED_COLOR;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_ALARM_BACKGROUND_COLOR;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_ALARM_BUTTON_COLOR;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_ALARM_CLOCK_COLOR;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_ALARM_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_ALARM_CLOCK_STYLE;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_ALARM_SECONDS_HAND_COLOR;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_ALARM_TITLE_FONT_SIZE;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_DISMISS_BUTTON_COLOR;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_DISPLAY_ALARM_SECONDS_HAND;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_DISPLAY_RINGTONE_TITLE;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_PULSE_COLOR;
import static com.best.deskclock.settings.AlarmDisplayCustomizationActivity.KEY_SNOOZE_BUTTON_COLOR;
import static com.best.deskclock.settings.AlarmSettingsActivity.DEFAULT_POWER_BEHAVIOR;
import static com.best.deskclock.settings.AlarmSettingsActivity.DEFAULT_VOLUME_BEHAVIOR;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ALARM_CRESCENDO;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ALARM_NOTIFICATION_REMINDER_TIME;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ALARM_SNOOZE;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_AUTO_SILENCE;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_DEFAULT_ALARM_RINGTONE;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_FLIP_ACTION;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_MATERIAL_TIME_PICKER_STYLE;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_POWER_BUTTON;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_SHAKE_ACTION;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_SWIPE_ACTION;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_VOLUME_BUTTONS;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_WEEK_START;
import static com.best.deskclock.settings.AlarmSettingsActivity.MATERIAL_TIME_PICKER_ANALOG_STYLE;
import static com.best.deskclock.settings.AlarmSettingsActivity.POWER_BEHAVIOR_DISMISS;
import static com.best.deskclock.settings.AlarmSettingsActivity.POWER_BEHAVIOR_SNOOZE;
import static com.best.deskclock.settings.AlarmSettingsActivity.VOLUME_BEHAVIOR_CHANGE_VOLUME;
import static com.best.deskclock.settings.AlarmSettingsActivity.VOLUME_BEHAVIOR_DISMISS;
import static com.best.deskclock.settings.AlarmSettingsActivity.VOLUME_BEHAVIOR_SNOOZE;
import static com.best.deskclock.settings.ClockSettingsActivity.KEY_AUTO_HOME_CLOCK;
import static com.best.deskclock.settings.ClockSettingsActivity.KEY_CLOCK_DISPLAY_SECONDS;
import static com.best.deskclock.settings.ClockSettingsActivity.KEY_CLOCK_STYLE;
import static com.best.deskclock.settings.ClockSettingsActivity.KEY_HOME_TIME_ZONE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.DEFAULT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.DEFAULT_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_AUTO_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_CARD_BACKGROUND;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_CARD_BORDER;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_DARK_MODE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_DEFAULT_DARK_MODE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_FADE_TRANSITIONS;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_TAB_INDICATOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_VIBRATIONS;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.SYSTEM_THEME;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_CLOCK_COLOR_PICKER;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_CLOCK_STYLE;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_DATE_COLOR_PICKER;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_DATE_IN_BOLD;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_DATE_IN_ITALIC;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC;
import static com.best.deskclock.settings.StopwatchSettingsActivity.KEY_SW_DEFAULT_ACTION;
import static com.best.deskclock.settings.StopwatchSettingsActivity.KEY_SW_VOLUME_DOWN_ACTION;
import static com.best.deskclock.settings.StopwatchSettingsActivity.KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS;
import static com.best.deskclock.settings.StopwatchSettingsActivity.KEY_SW_VOLUME_UP_ACTION;
import static com.best.deskclock.settings.StopwatchSettingsActivity.KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_DEFAULT_TIME_TO_ADD_TO_TIMER;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_KEEP_TIMER_SCREEN_ON;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_SORT_TIMER;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_SORT_TIMER_MANUALLY;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_AUTO_SILENCE;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_CRESCENDO;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_FLIP_ACTION;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_POWER_BUTTON_ACTION;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_RINGTONE;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_SHAKE_ACTION;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_VIBRATE;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_VOLUME_BUTTONS_ACTION;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER;

import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel.CitySort;
import com.best.deskclock.data.DataModel.ClockStyle;
import com.best.deskclock.data.DataModel.PowerButtonBehavior;
import com.best.deskclock.data.DataModel.VolumeButtonBehavior;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class encapsulates the storage of application preferences in {@link SharedPreferences}.
 */
final class SettingsDAO {

    /**
     * Key to a preference that stores the preferred sort order of world cities.
     */
    private static final String KEY_SORT_PREFERENCE = "sort_preference";

    /**
     * Key to a preference that stores the ringtone of an existing alarm.
     */
    private static final String KEY_SELECTED_ALARM_RINGTONE_URI = "selected_alarm_ringtone_uri";

    /**
     * Key to a preference that stores the global broadcast id.
     */
    private static final String KEY_ALARM_GLOBAL_ID = "intent.extra.alarm.global.id";

    /**
     * Key to a preference that indicates whether restore (of backup and restore) has completed.
     */
    private static final String KEY_RESTORE_BACKUP_FINISHED = "restore_finished";

    private SettingsDAO() {
    }

    /**
     * @return the id used to discriminate relevant AlarmManager callbacks from defunct ones
     */
    static int getGlobalIntentId(SharedPreferences prefs) {
        return prefs.getInt(KEY_ALARM_GLOBAL_ID, -1);
    }

    /**
     * Update the id used to discriminate relevant AlarmManager callbacks from defunct ones
     */
    static void updateGlobalIntentId(SharedPreferences prefs) {
        final int globalId = prefs.getInt(KEY_ALARM_GLOBAL_ID, -1) + 1;
        prefs.edit().putInt(KEY_ALARM_GLOBAL_ID, globalId).apply();
    }

    /**
     * @return an enumerated value indicating the order in which cities are ordered
     */
    static CitySort getCitySort(SharedPreferences prefs) {
        final int defaultSortOrdinal = CitySort.NAME.ordinal();
        final int citySortOrdinal = prefs.getInt(KEY_SORT_PREFERENCE, defaultSortOrdinal);
        return CitySort.values()[citySortOrdinal];
    }

    /**
     * Adjust the sort order of cities.
     */
    static void toggleCitySort(SharedPreferences prefs) {
        final CitySort oldSort = getCitySort(prefs);
        final CitySort newSort = oldSort == CitySort.NAME ? CitySort.UTC_OFFSET : CitySort.NAME;
        prefs.edit().putInt(KEY_SORT_PREFERENCE, newSort.ordinal()).apply();
    }

    /**
     * @return {@code true} if a clock for the user's home timezone should be automatically
     * displayed when it doesn't match the current timezone
     */
    static boolean getAutoShowHomeClock(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_clock.xml
        return prefs.getBoolean(KEY_AUTO_HOME_CLOCK, true);
    }

    /**
     * @return the user's home timezone
     */
    static TimeZone getHomeTimeZone(Context context, SharedPreferences prefs, TimeZone defaultTZ) {
        String timeZoneId = prefs.getString(KEY_HOME_TIME_ZONE, null);

        // If the recorded home timezone is legal, use it.
        final TimeZones timeZones = getTimeZones(context, System.currentTimeMillis());
        if (timeZones.contains(timeZoneId)) {
            return TimeZone.getTimeZone(timeZoneId);
        }

        // No legal home timezone has yet been recorded, attempt to record the default.
        timeZoneId = defaultTZ.getID();
        if (timeZones.contains(timeZoneId)) {
            prefs.edit().putString(KEY_HOME_TIME_ZONE, timeZoneId).apply();
        }

        // The timezone returned here may be valid or invalid. When it matches TimeZone.getDefault()
        // the Home city will not show, regardless of its validity.
        return defaultTZ;
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed in the app
     */
    public static ClockStyle getClockStyle(Context context, SharedPreferences prefs) {
        return getClockStyle(context, prefs, KEY_CLOCK_STYLE);
    }

    /**
     * @return the theme applied.
     */
    static String getTheme(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getString(KEY_THEME, SYSTEM_THEME);
    }

    /**
     * @return the accent color applied.
     */
    static String getAccentColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR);
    }

    /**
     * @return {@code true} if auto night accent color is enabled. {@code false} otherwise.
     */
    static boolean isAutoNightAccentColorEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_AUTO_NIGHT_ACCENT_COLOR, true);
    }

    /**
     * @return the night accent color applied.
     */
    static String getNightAccentColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getString(KEY_NIGHT_ACCENT_COLOR, DEFAULT_NIGHT_ACCENT_COLOR);
    }

    /**
     * @return the dark mode of the applied theme.
     */
    static String getDarkMode(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getString(KEY_DARK_MODE, KEY_DEFAULT_DARK_MODE);
    }

    /**
     * @return whether or not the background should be displayed in a view.
     */
    static boolean isCardBackgroundDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_CARD_BACKGROUND, true);
    }

    /**
     * @return whether or not the border should be displayed in a view.
     */
    static boolean isCardBorderDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_CARD_BORDER, false);
    }

    /**
     * @return whether or not the vibrations are enabled for the buttons.
     */
    static boolean isVibrationsEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_VIBRATIONS, false);
    }

    /**
     * @return whether or not the tab indicator is displayed in the bottom navigation menu.
     */
    static boolean isTabIndicatorDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_TAB_INDICATOR, true);
    }

    /**
     * @return whether or not the fade transitions are enabled.
     */
    static boolean isFadeTransitionsEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_FADE_TRANSITIONS, false);
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed in the app
     */
    static boolean getDisplayClockSeconds(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_clock.xml
        return prefs.getBoolean(KEY_CLOCK_DISPLAY_SECONDS, false);
    }

    /**
     * @param displaySeconds whether or not to display seconds on main clock
     */
    static void setDisplayClockSeconds(SharedPreferences prefs, boolean displaySeconds) {
        prefs.edit().putBoolean(KEY_CLOCK_DISPLAY_SECONDS, displaySeconds).apply();
    }

    /**
     * Sets the user's display seconds preference based on the currently selected clock if one has
     * not yet been manually chosen.
     */
    static void setDefaultDisplayClockSeconds(Context context, SharedPreferences prefs) {
        if (!prefs.contains(KEY_CLOCK_DISPLAY_SECONDS)) {
            // If on analog clock style on upgrade, default to true. Otherwise, default to false.
            final boolean isAnalog = getClockStyle(context, prefs) == ClockStyle.ANALOG;
            setDisplayClockSeconds(prefs, isAnalog);
        }
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed on the screensaver
     */
    static ClockStyle getScreensaverClockStyle(Context context, SharedPreferences prefs) {
        return getClockStyle(context, prefs, KEY_SCREENSAVER_CLOCK_STYLE);
    }

    /**
     * @return a value indicating whether analog or digital clock dynamic colors are displayed
     */
    static boolean areScreensaverClockDynamicColors(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS, false);
    }

    /**
     * @return a value indicating the color of the clock of the screensaver
     */
    static int getScreensaverClockColorPicker(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getInt(KEY_SCREENSAVER_CLOCK_COLOR_PICKER, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return a value indicating the color of the date of the screensaver
     */
    static int getScreensaverDateColorPicker(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getInt(KEY_SCREENSAVER_DATE_COLOR_PICKER, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return a value indicating the color of the next alarm of the screensaver
     */
    static int getScreensaverNextAlarmColorPicker(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getInt(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code int} the screen saver brightness level at night
     */
    static int getScreensaverBrightness(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getInt(KEY_SCREENSAVER_BRIGHTNESS, 40);
    }

    /**
     * @return a value indicating whether analog or digital clock seconds are displayed
     */
    static boolean areScreensaverClockSecondsDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS, false);
    }

    /**
     * @return {@code true} if the screen saver should show the clock in bold
     */
    static boolean isScreensaverDigitalClockInBold(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, false);
    }

    /**
     * @return {@code true} if the screen saver should show the clock in italic
     */
    static boolean isScreensaverDigitalClockInItalic(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC, false);
    }

    /**
     * @return {@code true} if the screen saver should show the date in bold
     */
    static boolean isScreensaverDateInBold(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_DATE_IN_BOLD, true);
    }

    /**
     * @return {@code true} if the screen saver should show the date in italic
     */
    static boolean isScreensaverDateInItalic(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_DATE_IN_ITALIC, false);
    }

    /**
     * @return {@code true} if the screen saver should show the next alarm in bold
     */
    static boolean isScreensaverNextAlarmInBold(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD, true);
    }

    /**
     * @return {@code true} if the screen saver should show the next alarm in italic
     */
    static boolean isScreensaverNextAlarmInItalic(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC, false);
    }

    /**
     * @return the uri of the selected ringtone or the {@code defaultUri} if no explicit selection
     * has yet been made
     */
    static Uri getTimerRingtoneUri(SharedPreferences prefs, Uri defaultUri) {
        final String uriString = prefs.getString(KEY_TIMER_RINGTONE, null);
        return uriString == null ? defaultUri : Uri.parse(uriString);
    }

    /**
     * @return the duration for which a timer can ring before expiring and being reset
     */
    static long getTimerAutoSilenceDuration(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        final String string = prefs.getString(KEY_TIMER_AUTO_SILENCE, "30");
        return Long.parseLong(string);
    }

    /**
     * @return whether timer vibration is enabled. {@code false} otherwise.
     */
    static boolean getTimerVibrate(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TIMER_VIBRATE, false);
    }

    /**
     * @param enabled whether vibration will be turned on for all timers.
     */
    static void setTimerVibrate(SharedPreferences prefs, boolean enabled) {
        prefs.edit().putBoolean(KEY_TIMER_VIBRATE, enabled).apply();
    }

    /**
     * @return whether the expired timer is reset with the volume buttons. {@code false} otherwise.
     */
    static boolean isExpiredTimerResetWithVolumeButtons(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TIMER_VOLUME_BUTTONS_ACTION, false);
    }

    /**
     * @return whether the expired timer is reset with the power button. {@code false} otherwise.
     */
    static boolean isExpiredTimerResetWithPowerButton(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TIMER_POWER_BUTTON_ACTION, false);
    }

    /**
     * @return whether flip action for timers is enabled. {@code false} otherwise.
     */
    static boolean isFlipActionForTimersEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TIMER_FLIP_ACTION, false);
    }

    /**
     * @return whether shake action for timers is enabled. {@code false} otherwise.
     */
    static boolean isShakeActionForTimersEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TIMER_SHAKE_ACTION, false);
    }

    /**
     * @return the timer sorting manually, in ascending order of duration, in descending order of duration or by name
     */
    static String getTimerSortingPreference(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getString(KEY_SORT_TIMER, KEY_SORT_TIMER_MANUALLY);
    }

    /**
     * @return the default minutes or hour to add to timer when the "Add Minute Or Hour" button is clicked.
     */
    static int getDefaultTimeToAddToTimer(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        final String string = prefs.getString(KEY_DEFAULT_TIME_TO_ADD_TO_TIMER, "1");
        return Integer.parseInt(string);
    }

    /**
     * @return {@code true} if the timer display must remain on. {@code false} otherwise.
     */
    static boolean shouldTimerDisplayRemainOn(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_timer.xml
        return pref.getBoolean(KEY_KEEP_TIMER_SCREEN_ON, true);
    }

    /**
     * @return {@code true} if the timer background must be transparent. {@code false} otherwise.
     */
    static boolean isTimerBackgroundTransparent(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER, false);
    }

    /**
     * @return {@code true} if a warning is displayed before deleting a timer. {@code false} otherwise.
     */
    static boolean isWarningDisplayedBeforeDeletingTimer(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER, false);
    }

    /**
     * @param uri the uri of the ringtone to play for all timers
     */
    static void setTimerRingtoneUri(SharedPreferences prefs, Uri uri) {
        prefs.edit().putString(KEY_TIMER_RINGTONE, uri.toString()).apply();
    }

    /**
     * @return the uri of the ringtone from the settings to play for all alarms
     */
    static Uri getAlarmRingtoneUriFromSettings(SharedPreferences prefs, Uri defaultUri) {
        final String uriString = prefs.getString(KEY_DEFAULT_ALARM_RINGTONE, null);
        return uriString == null ? defaultUri : Uri.parse(uriString);
    }

    /**
     * @param uri the uri of the ringtone from the settings to play for all alarms
     */
    static void setAlarmRingtoneUriFromSettings(SharedPreferences prefs, Uri uri) {
        prefs.edit().putString(KEY_DEFAULT_ALARM_RINGTONE, uri.toString()).apply();
    }

    /**
     * @param uri identifies the ringtone to play of an existing alarm
     */
    static void setSelectedAlarmRingtoneUri(SharedPreferences prefs, Uri uri) {
        prefs.edit().putString(KEY_SELECTED_ALARM_RINGTONE_URI, uri.toString()).apply();
    }

    /**
     * @return the duration, in milliseconds, of the crescendo to apply to alarm ringtone playback;
     * {@code 0} implies no crescendo should be applied
     */
    static long getAlarmCrescendoDuration(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String crescendoSeconds = prefs.getString(KEY_ALARM_CRESCENDO, "0");
        return Integer.parseInt(crescendoSeconds) * DateUtils.SECOND_IN_MILLIS;
    }

    /**
     * @return the duration, in milliseconds, of the crescendo to apply to timer ringtone playback;
     * {@code 0} implies no crescendo should be applied
     */
    static long getTimerCrescendoDuration(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        final String crescendoSeconds = prefs.getString(KEY_TIMER_CRESCENDO, "0");
        return Integer.parseInt(crescendoSeconds) * DateUtils.SECOND_IN_MILLIS;
    }

    /**
     * @return {@code true} if swipe action is enabled to dismiss or snooze alarms. {@code false} otherwise.
     */
    static boolean isSwipeActionEnabled(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getBoolean(KEY_SWIPE_ACTION, true);
    }

    /**
     * @return the display order of the weekdays, which can start with {@link Calendar#SATURDAY},
     * {@link Calendar#SUNDAY} or {@link Calendar#MONDAY}
     */
    static Weekdays.Order getWeekdayOrder(SharedPreferences prefs) {
        final String defaultValue = String.valueOf(Calendar.getInstance().getFirstDayOfWeek());
        final String value = prefs.getString(KEY_WEEK_START, defaultValue);
        final int firstCalendarDay = Integer.parseInt(value);
        return switch (firstCalendarDay) {
            case SATURDAY -> SAT_TO_FRI;
            case SUNDAY -> SUN_TO_SAT;
            case MONDAY -> MON_TO_SUN;
            default -> throw new IllegalArgumentException("Unknown weekday: " + firstCalendarDay);
        };
    }

    /**
     * @return {@code true} if the restore process (of backup and restore) has completed
     */
    static boolean isRestoreBackupFinished(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_RESTORE_BACKUP_FINISHED, false);
    }

    /**
     * @param finished {@code true} means the restore process (of backup and restore) has completed
     */
    static void setRestoreBackupFinished(SharedPreferences prefs, boolean finished) {
        if (finished) {
            prefs.edit().putBoolean(KEY_RESTORE_BACKUP_FINISHED, true).apply();
        } else {
            prefs.edit().remove(KEY_RESTORE_BACKUP_FINISHED).apply();
        }
    }

    /**
     * @return the behavior to execute when volume button is pressed while firing an alarm
     */
    static VolumeButtonBehavior getAlarmVolumeButtonBehavior(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String value = prefs.getString(KEY_VOLUME_BUTTONS, DEFAULT_VOLUME_BEHAVIOR);
        return switch (value) {
            case DEFAULT_VOLUME_BEHAVIOR -> DO_NOTHING;
            case VOLUME_BEHAVIOR_CHANGE_VOLUME -> CHANGE_VOLUME;
            case VOLUME_BEHAVIOR_SNOOZE -> SNOOZE_ALARM;
            case VOLUME_BEHAVIOR_DISMISS -> DISMISS_ALARM;
            default -> throw new IllegalArgumentException("Unknown volume button behavior: " + value);
        };
    }

    /**
     * @return the behavior to execute when power button is pressed while firing an alarm
     */
    static PowerButtonBehavior getAlarmPowerButtonBehavior(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String value = prefs.getString(KEY_POWER_BUTTON, DEFAULT_POWER_BEHAVIOR);
        return switch (value) {
            case DEFAULT_POWER_BEHAVIOR -> NOTHING;
            case POWER_BEHAVIOR_SNOOZE -> SNOOZE;
            case POWER_BEHAVIOR_DISMISS -> DISMISS;
            default -> throw new IllegalArgumentException("Unknown power button behavior: " + value);
        };
    }

    /**
     * @return the number of minutes an alarm may ring before it has timed out and becomes missed
     */
    static int getAlarmTimeout(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(KEY_AUTO_SILENCE, "10");
        return Integer.parseInt(string);
    }

    /**
     * @return the number of minutes an alarm will remain snoozed before it rings again
     */
    static int getSnoozeLength(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(KEY_ALARM_SNOOZE, "10");
        return Integer.parseInt(string);
    }

    /**
     * @param currentTime timezone offsets created relative to this time
     * @return a description of the time zones available for selection
     */
    static TimeZones getTimeZones(Context context, long currentTime) {
        final Locale locale = Locale.getDefault();
        final Resources resources = context.getResources();
        final String[] timeZoneIds = resources.getStringArray(R.array.timezone_values);
        final String[] timeZoneNames = resources.getStringArray(R.array.timezone_labels);

        // Verify the data is consistent.
        if (timeZoneIds.length != timeZoneNames.length) {
            final String message = String.format(Locale.US,
                    "id count (%d) does not match name count (%d) for locale %s",
                    timeZoneIds.length, timeZoneNames.length, locale);
            throw new IllegalStateException(message);
        }

        // Create TimeZoneDescriptors for each TimeZone so they can be sorted.
        final TimeZoneDescriptor[] descriptors = new TimeZoneDescriptor[timeZoneIds.length];
        for (int i = 0; i < timeZoneIds.length; i++) {
            final String id = timeZoneIds[i];
            final String name = timeZoneNames[i].replaceAll("\"", "");
            descriptors[i] = new TimeZoneDescriptor(locale, id, name, currentTime);
        }
        Arrays.sort(descriptors);

        // Transfer the TimeZoneDescriptors into parallel arrays for easy consumption by the caller.
        final CharSequence[] tzIds = new CharSequence[descriptors.length];
        final CharSequence[] tzNames = new CharSequence[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            final TimeZoneDescriptor descriptor = descriptors[i];
            tzIds[i] = descriptor.mTimeZoneId;
            tzNames[i] = descriptor.mTimeZoneName;
        }

        return new TimeZones(tzIds, tzNames);
    }

    static int getFlipAction(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(KEY_FLIP_ACTION, "0");
        return Integer.parseInt(string);
    }

    static int getShakeAction(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(KEY_SHAKE_ACTION, "0");
        return Integer.parseInt(string);
    }

    /**
     * @return the number of minutes before the upcoming alarm notification appears
     */
    static int getAlarmNotificationReminderTime(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(KEY_ALARM_NOTIFICATION_REMINDER_TIME, "30");
        return Integer.parseInt(string);
    }

    /**
     * @return {@code true} if alarm vibrations are enabled when creating alarms. {@code false} otherwise.
     */
    static boolean areAlarmVibrationsEnabledByDefault(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getBoolean(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, false);
    }

    /**
     * @return {@code true} if vibrations are enabled to indicate whether the alarm is snoozed or dismissed.
     * {@code false} otherwise.
     */
    static boolean areSnoozedOrDismissedAlarmVibrationsEnabled(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getBoolean(KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS, false);
    }

    /**
     * @return {@code true} if occasional alarm should be deleted by default. {@code false} otherwise.
     */
    static boolean isOccasionalAlarmDeletedByDefault(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getBoolean(KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT, false);
    }

    /**
     * @return the time picker style.
     */
    static String getMaterialTimePickerStyle(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getString(KEY_MATERIAL_TIME_PICKER_STYLE, MATERIAL_TIME_PICKER_ANALOG_STYLE);
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed on the alarm.
     */
    static ClockStyle getAlarmClockStyle(Context context, SharedPreferences prefs) {
        return getClockStyle(context, prefs, KEY_ALARM_CLOCK_STYLE);
    }

    /**
     * @return a value indicating whether analog clock seconds hand is displayed on the alarm.
     */
    static boolean isAlarmSecondsHandDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getBoolean(KEY_DISPLAY_ALARM_SECONDS_HAND, true);
    }

    /**
     * @return a value indicating the alarm background color.
     */
    static int getAlarmBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_BACKGROUND_COLOR, Color.parseColor("#FF191C1E"));
    }

    /**
     * @return a value indicating the alarm background amoled color.
     */
    static int getAlarmBackgroundAmoledColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_BACKGROUND_AMOLED_COLOR, Color.parseColor("#FF000000"));
    }

    /**
     * @return a value indicating the alarm clock color.
     */
    static int getAlarmClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_CLOCK_COLOR, Color.parseColor("#FF8A9297"));
    }

    /**
     * @return a value indicating the alarm seconds hand color.
     */
    static int getAlarmSecondsHandColor(SharedPreferences prefs, Context context) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_SECONDS_HAND_COLOR, context.getColor(R.color.md_theme_primary));
    }

    /**
     * @return a value indicating the alarm title color.
     */
    static int getAlarmTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_TITLE_COLOR, Color.parseColor("#FF8A9297"));
    }

    /**
     * @return a value indicating the snooze button color.
     */
    static int getSnoozeButtonColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_SNOOZE_BUTTON_COLOR, Color.parseColor("#FF8A9297"));
    }

    /**
     * @return a value indicating the dismiss button color.
     */
    static int getDismissButtonColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_DISMISS_BUTTON_COLOR, Color.parseColor("#FF8A9297"));
    }

    /**
     * @return a value indicating the alarm button color.
     */
    static int getAlarmButtonColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_BUTTON_COLOR, Color.parseColor("#FF8A9297"));
    }

    /**
     * @return a value indicating the pulse color.
     */
    static int getPulseColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_PULSE_COLOR, Color.parseColor("#FFC0C7CD"));
    }

    /**
     * @return the font size applied to the alarm clock.
     */
    static String getAlarmClockFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getString(KEY_ALARM_CLOCK_FONT_SIZE, DEFAULT_ALARM_CLOCK_FONT_SIZE);
    }

    /**
     * @return the font size applied to the alarm title.
     */
    static String getAlarmTitleFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getString(KEY_ALARM_TITLE_FONT_SIZE, DEFAULT_ALARM_TITLE_FONT_SIZE);
    }

    /**
     * @return {@code true} if the ringtone title should be displayed on the lock screen when the alarm is triggered.
     * {@code false} otherwise.
     */
    static boolean isRingtoneTitleDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getBoolean(KEY_DISPLAY_RINGTONE_TITLE, false);
    }

    private static ClockStyle getClockStyle(Context context, SharedPreferences prefs, String key) {
        final String defaultStyle = context.getString(R.string.default_clock_style);
        final String clockStyle = prefs.getString(key, defaultStyle);
        // Use hardcoded locale to perform toUpperCase, because in some languages toUpperCase adds
        // accent to character, which breaks the enum conversion.
        return ClockStyle.valueOf(clockStyle.toUpperCase(Locale.US));
    }

    /**
     * @return the action to execute when volume up button is pressed for the stopwatch
     */
    static String getVolumeUpActionForStopwatch(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_stopwatch.xml
        return prefs.getString(KEY_SW_VOLUME_UP_ACTION, KEY_SW_DEFAULT_ACTION);
    }

    /**
     * @return the action to execute when volume up button is long pressed for the stopwatch
     */
    static String getVolumeUpActionAfterLongPressForStopwatch(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_stopwatch.xml
        return prefs.getString(KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS, KEY_SW_DEFAULT_ACTION);
    }

    /**
     * @return the action to execute when volume down button is pressed for the stopwatch
     */
    static String getVolumeDownActionForStopwatch(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_stopwatch.xml
        return prefs.getString(KEY_SW_VOLUME_DOWN_ACTION, KEY_SW_DEFAULT_ACTION);
    }

    /**
     * @return the action to execute when volume down button is long pressed for the stopwatch
     */
    static String getVolumeDownActionAfterLongPressForStopwatch(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_stopwatch.xml
        return prefs.getString(KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS, KEY_SW_DEFAULT_ACTION);
    }

    /**
     * These descriptors have a natural order from furthest ahead of GMT to furthest behind GMT.
     */
    private static class TimeZoneDescriptor implements Comparable<TimeZoneDescriptor> {

        private final int mOffset;
        private final String mTimeZoneId;
        private final String mTimeZoneName;

        private TimeZoneDescriptor(Locale locale, String id, String name, long currentTime) {
            mTimeZoneId = id;

            final TimeZone tz = TimeZone.getTimeZone(id);
            mOffset = tz.getOffset(currentTime);

            final char sign = mOffset < 0 ? '-' : '+';
            final int absoluteGMTOffset = Math.abs(mOffset);
            final long hour = absoluteGMTOffset / HOUR_IN_MILLIS;
            final long minute = (absoluteGMTOffset / MINUTE_IN_MILLIS) % 60;
            mTimeZoneName = String.format(locale, "(GMT%s%d:%02d) %s", sign, hour, minute, name);
        }

        @Override
        public int compareTo(@NonNull TimeZoneDescriptor other) {
            return mOffset - other.mOffset;
        }
    }

}
