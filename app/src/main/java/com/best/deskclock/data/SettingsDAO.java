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
import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.*;

import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel.CitySort;
import com.best.deskclock.data.DataModel.ClockStyle;
import com.best.deskclock.data.DataModel.PowerButtonBehavior;
import com.best.deskclock.data.DataModel.VolumeButtonBehavior;
import com.best.deskclock.utils.Utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class encapsulates the storage of application preferences in {@link SharedPreferences}.
 */
public final class SettingsDAO {

    /**
     * Key to a preference that stores the preferred sort order of world cities.
     */
    private static final String KEY_SORT_PREFERENCE = "sort_preference";

    /**
     * Key to a preference that stores the ringtone of an existing alarm.
     */
    public static final String KEY_SELECTED_ALARM_RINGTONE_URI = "selected_alarm_ringtone_uri";

    /**
     * Key to a preference that stores the global broadcast id.
     */
    private static final String KEY_ALARM_GLOBAL_ID = "intent.extra.alarm.global.id";

    /**
     * Key to a preference that indicates whether restore (of backup and restore) has completed.
     */
    private static final String KEY_RESTORE_BACKUP_FINISHED = "restore_finished";

    /**
     * @return the id used to discriminate relevant AlarmManager callbacks from defunct ones
     */
    public static int getGlobalIntentId(SharedPreferences prefs) {
        return prefs.getInt(KEY_ALARM_GLOBAL_ID, -1);
    }

    /**
     * Update the id used to discriminate relevant AlarmManager callbacks from defunct ones
     */
    public static void updateGlobalIntentId(SharedPreferences prefs) {
        final int globalId = prefs.getInt(KEY_ALARM_GLOBAL_ID, -1) + 1;
        prefs.edit().putInt(KEY_ALARM_GLOBAL_ID, globalId).apply();
    }

    /**
     * @return an enumerated value indicating the order in which cities are ordered
     */
    public static CitySort getCitySort(SharedPreferences prefs) {
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
     * @return sorting of cities by time zone in ascending order, by name or manually.
     */
    public static String getCitySorting(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_clock.xml
        return prefs.getString(KEY_SORT_CITIES, DEFAULT_SORT_CITIES_BY_ASCENDING_TIME_ZONE);
    }

    /**
     * @return {@code true} if if a note can be added to the cities; {@code false} otherwise.
     */
    public static boolean isCityNoteEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_clock.xml
        return prefs.getBoolean(KEY_ENABLE_CITY_NOTE, DEFAULT_ENABLE_CITY_NOTE);
    }

    /**
     * @return {@code true} if a clock for the user's home timezone should be automatically
     * displayed when it doesn't match the current timezone
     */
    public static boolean getAutoShowHomeClock(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_clock.xml
        return prefs.getBoolean(KEY_AUTO_HOME_CLOCK, DEFAULT_AUTO_HOME_CLOCK);
    }

    /**
     * @return {@code true} if the users wants to automatically show a clock for their home timezone
     * when they have travelled outside of that timezone
     */
    public static boolean getShowHomeClock(Context context, SharedPreferences prefs) {
        if (!getAutoShowHomeClock(prefs)) {
            return false;
        }

        // Show the home clock if the current time and home time differ.
        // (By using UTC offset for this comparison the various DST rules are considered)
        final TimeZone defaultTZ = TimeZone.getDefault();
        final TimeZone homeTimeZone = SettingsDAO.getHomeTimeZone(context, prefs, defaultTZ);
        final long now = System.currentTimeMillis();
        return homeTimeZone.getOffset(now) != defaultTZ.getOffset(now);
    }

    /**
     * @return the user's home timezone
     */
    public static TimeZone getHomeTimeZone(Context context, SharedPreferences prefs, TimeZone defaultTZ) {
        String timeZoneId = prefs.getString(KEY_HOME_TIME_ZONE, DEFAULT_HOME_TIME_ZONE);

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
     * @return {@code true} if the Debug settings are displayed for release versions.
     * {@code false} otherwise.
     */
    public static boolean isDebugSettingsDisplayed(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DISPLAY_DEBUG_SETTINGS, DEFAULT_DISPLAY_DEBUG_SETTINGS);
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed in the app
     */
    public static ClockStyle getClockStyle(SharedPreferences prefs) {
        return getClockStyle(prefs, KEY_CLOCK_STYLE);
    }

    /**
     * @return the clock dial applied in the Clock tab.
     */
    public static String getClockDial(SharedPreferences prefs) {
        return prefs.getString(KEY_CLOCK_DIAL, DEFAULT_CLOCK_DIAL);
    }

    /**
     * @return the material clock dial applied in the Clock tab.
     */
    public static String getClockDialMaterial(SharedPreferences prefs) {
        return prefs.getString(KEY_CLOCK_DIAL_MATERIAL, DEFAULT_CLOCK_DIAL_MATERIAL);
    }

    /**
     * @return the clock second hand applied in the Clock tab.
     */
    public static String getClockSecondHand(SharedPreferences prefs) {
        return prefs.getString(KEY_CLOCK_SECOND_HAND, DEFAULT_CLOCK_SECOND_HAND);
    }

    /**
     * @return the theme applied.
     */
    public static String getTheme(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getString(KEY_THEME, SYSTEM_THEME);
    }

    /**
     * @return the accent color applied.
     */
    public static String getAccentColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR);
    }

    /**
     * @return {@code true} if auto night accent color is enabled. {@code false} otherwise.
     */
    public static boolean isAutoNightAccentColorEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_AUTO_NIGHT_ACCENT_COLOR, DEFAULT_AUTO_NIGHT_ACCENT_COLOR);
    }

    /**
     * @return the night accent color applied.
     */
    public static String getNightAccentColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getString(KEY_NIGHT_ACCENT_COLOR, DEFAULT_NIGHT_ACCENT_COLOR);
    }

    /**
     * @return the dark mode of the applied theme.
     */
    public static String getDarkMode(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getString(KEY_DARK_MODE, DEFAULT_DARK_MODE);
    }

    /**
     * @return {@code true} if the background should be displayed in a view. {@code false} otherwise.
     */
    public static boolean isCardBackgroundDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_CARD_BACKGROUND, DEFAULT_CARD_BACKGROUND);
    }

    /**
     * @return {@code true} if the border should be displayed in a view. {@code false} otherwise.
     */
    public static boolean isCardBorderDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_CARD_BORDER, DEFAULT_CARD_BORDER);
    }

    /**
     * @return the custom language code.
     */
    public static String getCustomLanguageCode(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getString(KEY_CUSTOM_LANGUAGE_CODE, DEFAULT_SYSTEM_LANGUAGE_CODE);
    }

    public static int getTabToDisplay(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        final String tabToDisplay = prefs.getString(KEY_TAB_TO_DISPLAY, DEFAULT_TAB_TO_DISPLAY);
        return Integer.parseInt(tabToDisplay);
    }

    /**
     * @return {@code true} if the vibrations are enabled for the buttons. {@code false} otherwise.
     */
    public static boolean isVibrationsEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_VIBRATIONS, DEFAULT_VIBRATIONS);
    }

    /**
     * @return {@code true} if the toolbar title is displayed. {@code false} otherwise.
     */
    public static boolean isToolbarTitleDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_TOOLBAR_TITLE, DEFAULT_TOOLBAR_TITLE);
    }

    /**
     * @return the tab title visibility.
     */
    public static String getTabTitleVisibility(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getString(KEY_TAB_TITLE_VISIBILITY, DEFAULT_TAB_TITLE_VISIBILITY);
    }

    /**
     * @return {@code true} if the tab indicator is displayed in the bottom navigation menu.
     * {@code false} otherwise.
     */
    public static boolean isTabIndicatorDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_TAB_INDICATOR, DEFAULT_TAB_INDICATOR);
    }

    /**
     * @return {@code true} if the fade transitions are enabled. {@code false} otherwise.
     */
    public static boolean isFadeTransitionsEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_FADE_TRANSITIONS, DEFAULT_FADE_TRANSITIONS);
    }

    /**
     * @return {@code true} if the screen should remain on. {@code false} otherwise.
     */
    public static boolean shouldScreenRemainOn(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_interface_customization.xml
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, DEFAULT_KEEP_SCREEN_ON);
    }

    /**
     * @return {@code true} if the seconds are displayed on the analog or digital clock.
     * {@code false} otherwise.
     */
    public static boolean areClockSecondsDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_clock.xml
        return prefs.getBoolean(KEY_DISPLAY_CLOCK_SECONDS, DEFAULT_DISPLAY_CLOCK_SECONDS);
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed on the screensaver.
     */
    public static ClockStyle getScreensaverClockStyle(SharedPreferences prefs) {
        return getClockStyle(prefs, KEY_SCREENSAVER_CLOCK_STYLE);
    }

    /**
     * @return the clock dial applied for the screensaver.
     */
    public static String getScreensaverClockDial(SharedPreferences prefs) {
        return prefs.getString(KEY_SCREENSAVER_CLOCK_DIAL, DEFAULT_CLOCK_DIAL);
    }

    /**
     * @return the material clock dial applied for the screensaver.
     */
    public static String getScreensaverClockDialMaterial(SharedPreferences prefs) {
        return prefs.getString(KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL, DEFAULT_CLOCK_DIAL_MATERIAL);
    }

    /**
     * @return the clock second hand applied for the screensaver.
     */
    public static String getScreensaverClockSecondHand(SharedPreferences prefs) {
        return prefs.getString(KEY_SCREENSAVER_CLOCK_SECOND_HAND, DEFAULT_CLOCK_SECOND_HAND);
    }

    /**
     * @return {@code true} if dynamic colors are applied to analog or digital clock.
     * {@code false} otherwise.
     */
    public static boolean areScreensaverClockDynamicColors(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS, DEFAULT_SCREENSAVER_CLOCK_DYNAMIC_COLORS);
    }

    /**
     * @return a value indicating the color of the clock of the screensaver
     */
    public static int getScreensaverClockColorPicker(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getInt(KEY_SCREENSAVER_CLOCK_COLOR_PICKER, DEFAULT_SCREENSAVER_CUSTOM_COLOR);
    }

    /**
     * @return a value indicating the color of the date of the screensaver
     */
    public static int getScreensaverDateColorPicker(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getInt(KEY_SCREENSAVER_DATE_COLOR_PICKER, DEFAULT_SCREENSAVER_CUSTOM_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm of the screensaver
     */
    public static int getScreensaverNextAlarmColorPicker(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getInt(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER, DEFAULT_SCREENSAVER_CUSTOM_COLOR);
    }

    /**
     * @return {@code int} the screensaver brightness level at night
     */
    public static int getScreensaverBrightness(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getInt(KEY_SCREENSAVER_BRIGHTNESS, DEFAULT_SCREENSAVER_BRIGHTNESS);
    }

    /**
     * @return {@code true} if the seconds are displayed on the analog or digital clock in the screensaver.
     * {@code false} otherwise.
     */
    public static boolean areScreensaverClockSecondsDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS, DEFAULT_DISPLAY_SCREENSAVER_CLOCK_SECONDS);
    }

    /**
     * @return {@code true} if the screensaver should show the clock in bold. {@code false} otherwise.
     */
    public static boolean isScreensaverDigitalClockInBold(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, DEFAULT_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD);
    }

    /**
     * @return {@code true} if the screensaver should show the clock in italic. {@code false} otherwise.
     */
    public static boolean isScreensaverDigitalClockInItalic(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC, DEFAULT_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC);
    }

    /**
     * @return {@code true} if the screensaver should show the date in bold. {@code false} otherwise.
     */
    public static boolean isScreensaverDateInBold(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_DATE_IN_BOLD, DEFAULT_SCREENSAVER_DATE_IN_BOLD);
    }

    /**
     * @return {@code true} if the screensaver should show the date in italic. {@code false} otherwise.
     */
    public static boolean isScreensaverDateInItalic(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_DATE_IN_ITALIC, DEFAULT_SCREENSAVER_DATE_IN_ITALIC);
    }

    /**
     * @return {@code true} if the screensaver should show the next alarm in bold. {@code false} otherwise.
     */
    public static boolean isScreensaverNextAlarmInBold(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD, DEFAULT_SCREENSAVER_NEXT_ALARM_IN_BOLD);
    }

    /**
     * @return {@code true} if the screensaver should show the next alarm in italic. {@code false} otherwise.
     */
    public static boolean isScreensaverNextAlarmInItalic(SharedPreferences prefs) {
        // Default value must match the one in res/xml/screensaver_settings.xml
        return prefs.getBoolean(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC, DEFAULT_SCREENSAVER_NEXT_ALARM_IN_ITALIC);
    }

    /**
     * @return the uri of the selected ringtone or the {@code defaultUri} if no explicit selection
     * has yet been made.
     */
    static Uri getTimerRingtoneUri(SharedPreferences prefs, Uri defaultUri) {
        final String uriString = prefs.getString(KEY_TIMER_RINGTONE, null);
        return uriString == null ? defaultUri : Uri.parse(uriString);
    }

    /**
     * @return the duration for which a timer can ring before expiring and being reset.
     */
    static int getTimerAutoSilenceDuration(SharedPreferences prefs) {
        return prefs.getInt(KEY_TIMER_AUTO_SILENCE_DURATION, DEFAULT_TIMER_AUTO_SILENCE_DURATION);
    }

    /**
     * @return {@code true} if the timer vibrations are enabled. {@code false} otherwise.
     */
    public static boolean isTimerVibrate(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TIMER_VIBRATE, DEFAULT_TIMER_VIBRATE);
    }

    /**
     * @return {@code true} if the expired timer is reset with the volume buttons. {@code false} otherwise.
     */
    public static boolean isExpiredTimerResetWithVolumeButtons(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TIMER_VOLUME_BUTTONS_ACTION, DEFAULT_TIMER_VOLUME_BUTTONS_ACTION);
    }

    /**
     * @return {@code true} if the expired timer is reset with the power button. {@code false} otherwise.
     */
    public static boolean isExpiredTimerResetWithPowerButton(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TIMER_POWER_BUTTON_ACTION, DEFAULT_TIMER_POWER_BUTTON_ACTION);
    }

    /**
     * @return {@code true} if the flip action for timers is enabled. {@code false} otherwise.
     */
    public static boolean isFlipActionForTimersEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TIMER_FLIP_ACTION, DEFAULT_TIMER_FLIP_ACTION);
    }

    /**
     * @return {@code true} if the shake action for timers is enabled. {@code false} otherwise.
     */
    public static boolean isShakeActionForTimersEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TIMER_SHAKE_ACTION, DEFAULT_TIMER_SHAKE_ACTION);
    }

    /**
     * @return the shake intensity value for timers.
     */
    public static int getTimerShakeIntensity(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_timer.xml
        return pref.getInt(KEY_TIMER_SHAKE_INTENSITY, DEFAULT_TIMER_SHAKE_INTENSITY);
    }

    /**
     * @return the timer sorting manually, in ascending order of duration, in descending order of duration or by name
     */
    public static String getTimerSortingPreference(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getString(KEY_SORT_TIMER, DEFAULT_SORT_TIMER_MANUALLY);
    }

    /**
     * @return the default minutes in seconds to add to timer when the "Add Minute" button is clicked.
     */
    public static int getDefaultTimeToAddToTimer(SharedPreferences prefs) {
        return prefs.getInt(KEY_TIMER_ADD_TIME_BUTTON_VALUE, DEFAULT_TIMER_ADD_TIME_BUTTON_VALUE);
    }

    /**
     * @return the timer creation view style.
     */
    public static String getTimerCreationViewStyle(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getString(KEY_TIMER_CREATION_VIEW_STYLE, DEFAULT_TIMER_CREATION_VIEW_STYLE);
    }

    /**
     * @return {@code true} if the timer background must be transparent. {@code false} otherwise.
     */
    public static boolean isTimerBackgroundTransparent(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER, DEFAULT_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER);
    }

    /**
     * @return {@code true} if a warning is displayed before deleting a timer. {@code false} otherwise.
     */
    public static boolean isWarningDisplayedBeforeDeletingTimer(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getBoolean(KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER, DEFAULT_DISPLAY_WARNING_BEFORE_DELETING_TIMER);
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
     * @return {@code true} if a custom volume can be set for each alarm.
     * {@code false} otherwise.
     */
    public static boolean isPerAlarmVolumeEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getBoolean(KEY_ENABLE_PER_ALARM_VOLUME, DEFAULT_ENABLE_PER_ALARM_VOLUME);
    }

    /**
     * @return {@code true} if a custom volume increase duration can be set for each alarm.
     * {@code false} otherwise.
     */
    public static boolean isPerAlarmCrescendoDurationEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getBoolean(KEY_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION,
                DEFAULT_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION);
    }

    /**
     * @return the duration, in seconds, of the crescendo to apply to alarm ringtone playback;
     * {@code 0} implies no crescendo should be applied.
     */
    public static int getAlarmVolumeCrescendoDuration(SharedPreferences prefs) {
        return prefs.getInt(KEY_ALARM_VOLUME_CRESCENDO_DURATION, DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION);
    }

    /**
     * @return {@code true} if advanced audio playback is enabled for the ringtone.
     * {@code false} otherwise.
     */
    public static boolean isAdvancedAudioPlaybackEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getBoolean(KEY_ADVANCED_AUDIO_PLAYBACK, DEFAULT_ADVANCED_AUDIO_PLAYBACK);
    }

    /**
     * @return {@code true} if the ringtone should be automatically routed to Bluetooth devices.
     * {@code false} otherwise.
     */
    public static boolean isAutoRoutingToBluetoothDeviceEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getBoolean(KEY_AUTO_ROUTING_TO_BLUETOOTH_DEVICE, DEFAULT_AUTO_ROUTING_TO_BLUETOOTH_DEVICE);
    }

    /**
     * @return {@code true} if a custom media volume should be applied instead of the
     * system media volume. {@code false} otherwise.
     */
    public static boolean shouldUseCustomMediaVolume(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return !prefs.getBoolean(KEY_SYSTEM_MEDIA_VOLUME, DEFAULT_SYSTEM_MEDIA_VOLUME);
    }

    /**
     * @return the volume applied to the ringtone when a Bluetooth device is connected.
     */
    public static int getBluetoothVolumeValue(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getInt(KEY_BLUETOOTH_VOLUME, DEFAULT_BLUETOOTH_VOLUME);
    }

    /**
     * @return the duration, in seconds, of the crescendo to apply to timer ringtone playback;
     * {@code 0} implies no crescendo should be applied.
     */
    public static int getTimerVolumeCrescendoDuration(SharedPreferences prefs) {
        return prefs.getInt(KEY_TIMER_VOLUME_CRESCENDO_DURATION, DEFAULT_TIMER_VOLUME_CRESCENDO_DURATION);
    }

    /**
     * @return {@code true} if swipe action is enabled to dismiss or snooze alarms. {@code false} otherwise.
     */
    public static boolean isSwipeActionEnabled(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getBoolean(KEY_SWIPE_ACTION, DEFAULT_SWIPE_ACTION);
    }

    /**
     * @return the alarm sorting by time, by time of next alarm and by name.
     */
    public static String getAlarmSorting(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        return prefs.getString(KEY_SORT_ALARM, DEFAULT_SORT_BY_ALARM_TIME);
    }

    /**
     * @return {@code true} if the enabled alarms are displayed first; {@code false} otherwise.
     */
    public static boolean areEnabledAlarmsDisplayedFirst(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getBoolean(KEY_DISPLAY_ENABLED_ALARMS_FIRST, DEFAULT_DISPLAY_ENABLED_ALARMS_FIRST);
    }

    /**
     * @return {@code true} if the long press on the alarm FAB is enabled; {@code false} otherwise.
     */
    public static boolean isAlarmFabLongPressEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getBoolean(KEY_ENABLE_ALARM_FAB_LONG_PRESS, DEFAULT_ENABLE_ALARM_FAB_LONG_PRESS);
    }

    /**
     * @return the display order of the weekdays, which can start with {@link Calendar#SATURDAY},
     * {@link Calendar#SUNDAY} or {@link Calendar#MONDAY}
     */
    public static Weekdays.Order getWeekdayOrder(SharedPreferences prefs) {
        final String value = prefs.getString(KEY_WEEK_START, DEFAULT_WEEK_START);
        final int firstCalendarDay = Integer.parseInt(value);
        return switch (firstCalendarDay) {
            case SATURDAY -> SAT_TO_FRI;
            case SUNDAY -> SUN_TO_SAT;
            case MONDAY -> MON_TO_SUN;
            default -> throw new IllegalArgumentException("Unknown weekday: " + firstCalendarDay);
        };
    }

    /**
     * @return {@code true} if the restore process (of backup and restore) has completed. {@code false} otherwise.
     */
    public static boolean isRestoreBackupFinished(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_RESTORE_BACKUP_FINISHED, false);
    }

    /**
     * @param finished {@code true} means the restore process (of backup and restore) has completed
     */
    public static void setRestoreBackupFinished(SharedPreferences prefs, boolean finished) {
        if (finished) {
            prefs.edit().putBoolean(KEY_RESTORE_BACKUP_FINISHED, true).apply();
        } else {
            prefs.edit().remove(KEY_RESTORE_BACKUP_FINISHED).apply();
        }
    }

    /**
     * @return the behavior to execute when volume button is pressed while firing an alarm
     */
    public static VolumeButtonBehavior getAlarmVolumeButtonBehavior(SharedPreferences prefs) {
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
    public static PowerButtonBehavior getAlarmPowerButtonBehavior(SharedPreferences prefs) {
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
     * @return {@code true} if a custom auto silence duration can be set for each alarm.
     * {@code false} otherwise.
     */
    public static boolean isPerAlarmAutoSilenceEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getBoolean(KEY_ENABLE_PER_ALARM_AUTO_SILENCE, DEFAULT_ENABLE_PER_ALARM_AUTO_SILENCE);
    }

    /**
     * @return the number of minutes an alarm may ring before it has timed out
     */
    public static int getAlarmTimeout(SharedPreferences prefs) {
        return prefs.getInt(KEY_AUTO_SILENCE_DURATION, DEFAULT_AUTO_SILENCE_DURATION);
    }

    /**
     * @return {@code true} if a custom snooze duration can be set for each alarm.
     * {@code false} otherwise.
     */
    public static boolean isPerAlarmSnoozeDurationEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getBoolean(KEY_ENABLE_PER_ALARM_SNOOZE_DURATION, DEFAULT_ENABLE_PER_ALARM_SNOOZE_DURATION);
    }

    /**
     * @return the number of minutes an alarm will remain snoozed before it rings again
     */
    public static int getSnoozeLength(SharedPreferences prefs) {
        return prefs.getInt(KEY_ALARM_SNOOZE_DURATION, DEFAULT_ALARM_SNOOZE_DURATION);
    }

    /**
     * @return {@code true} if a custom repeat limit can be set for each missed alarm.
     * {@code false} otherwise.
     */
    public static boolean isPerAlarmMissedRepeatLimitEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getBoolean(KEY_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT, DEFAULT_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT);
    }

    /**
     * @return the number of times a missed alarm can be repeated.
     */
    public static int getMissedAlarmRepeatLimit(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(KEY_MISSED_ALARM_REPEAT_LIMIT, DEFAULT_MISSED_ALARM_REPEAT_LIMIT);
        return Integer.parseInt(string);
    }

    /**
     * @param currentTime timezone offsets created relative to this time
     * @return a description of the time zones available for selection
     */
    public static TimeZones getTimeZones(Context context, long currentTime) {
        final Locale locale = Locale.getDefault();
        final Context localizedContext = Utils.getLocalizedContext(context);
        final Resources resources = localizedContext.getResources();
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

    /**
     * @return the action to be performed after flipping the device.
     */
    public static int getFlipAction(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(KEY_FLIP_ACTION, DEFAULT_FLIP_ACTION);
        return Integer.parseInt(string);
    }

    /**
     * @return the action to be performed after shaking the device.
     */
    public static int getShakeAction(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(KEY_SHAKE_ACTION, DEFAULT_SHAKE_ACTION);
        return Integer.parseInt(string);
    }

    /**
     * @return the shake intensity value.
     */
    public static int getShakeIntensity(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getInt(KEY_SHAKE_INTENSITY, DEFAULT_SHAKE_INTENSITY);
    }

    /**
     * @return {@code true} if the Dismiss button should appear as soon as the alarm is enabled.
     * {@code false} otherwise.
     */
    public static boolean isDismissButtonDisplayedWhenAlarmEnabled(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getBoolean(KEY_DISPLAY_DISMISS_BUTTON, DEFAULT_DISPLAY_DISMISS_BUTTON);
    }

    /**
     * @return the number of minutes before the upcoming alarm notification appears
     */
    public static int getAlarmNotificationReminderTime(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(KEY_ALARM_NOTIFICATION_REMINDER_TIME, DEFAULT_ALARM_NOTIFICATION_REMINDER_TIME);
        return Integer.parseInt(string);
    }

    /**
     * @return the vibration pattern applied to alarms.
     */
    public static String getVibrationPattern(SharedPreferences prefs) {
        return prefs.getString(KEY_VIBRATION_PATTERN, DEFAULT_VIBRATION_PATTERN);
    }

    /**
     * @return the vibration start delay applied to alarms.
     */
    public static int getVibrationStartDelay(SharedPreferences prefs) {
        return prefs.getInt(KEY_VIBRATION_START_DELAY, DEFAULT_VIBRATION_START_DELAY);
    }

    /**
     * @return {@code true} if alarm vibrations are enabled when creating alarms. {@code false} otherwise.
     */
    public static boolean areAlarmVibrationsEnabledByDefault(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getBoolean(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, DEFAULT_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT);
    }

    /**
     * @return {@code true} if vibrations are enabled to indicate whether the alarm is snoozed or dismissed.
     * {@code false} otherwise.
     */
    public static boolean areSnoozedOrDismissedAlarmVibrationsEnabled(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getBoolean(KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS, DEFAULT_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS);
    }

    /**
     * @return {@code true} if the back flash should turn on when the alarm is triggered.
     * {@code false} otherwise.
     */
    public static boolean shouldTurnOnBackFlashForTriggeredAlarm(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getBoolean(KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM, DEFAULT_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM);
    }

    /**
     * @return {@code true} if occasional alarm should be deleted by default. {@code false} otherwise.
     */
    public static boolean isOccasionalAlarmDeletedByDefault(SharedPreferences pref) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return pref.getBoolean(KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT, DEFAULT_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT);
    }

    /**
     * @return the time picker style.
     */
    public static String getMaterialTimePickerStyle(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getString(KEY_MATERIAL_TIME_PICKER_STYLE, DEFAULT_TIME_PICKER_STYLE);
    }

    /**
     * @return the date picker style.
     */
    public static String getMaterialDatePickerStyle(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getString(KEY_MATERIAL_DATE_PICKER_STYLE, DEFAULT_DATE_PICKER_STYLE);
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed on the alarm.
     */
    public static ClockStyle getAlarmClockStyle(SharedPreferences prefs) {
        return getClockStyle(prefs, KEY_ALARM_CLOCK_STYLE);
    }

    /**
     * @return the clock dial applied for alarms.
     */
    public static String getAlarmClockDial(SharedPreferences prefs) {
        return prefs.getString(KEY_ALARM_CLOCK_DIAL, DEFAULT_CLOCK_DIAL);
    }

    /**
     * @return the clock second hand applied for alarms.
     */
    public static String getAlarmClockSecondHand(SharedPreferences prefs) {
        return prefs.getString(KEY_ALARM_CLOCK_SECOND_HAND, DEFAULT_CLOCK_SECOND_HAND);
    }

    /**
     * @return the material clock dial applied for alarms.
     */
    public static String getAlarmClockDialMaterial(SharedPreferences prefs) {
        return prefs.getString(KEY_ALARM_CLOCK_DIAL_MATERIAL, DEFAULT_CLOCK_DIAL_MATERIAL);
    }

    /**
     * @return {@code true} if the second hand is displayed on analog clock for the alarm.
     * {@code false} otherwise.
     */
    public static boolean isAlarmSecondHandDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getBoolean(KEY_DISPLAY_ALARM_SECOND_HAND, DEFAULT_DISPLAY_ALARM_SECOND_HAND);
    }

    /**
     * @return a value indicating the alarm background color.
     */
    public static int getAlarmBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_BACKGROUND_COLOR, DEFAULT_ALARM_BACKGROUND_COLOR);
    }

    /**
     * @return a value indicating the alarm background amoled color.
     */
    public static int getAlarmBackgroundAmoledColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_BACKGROUND_AMOLED_COLOR, DEFAULT_ALARM_BACKGROUND_AMOLED_COLOR);
    }

    /**
     * @return a value indicating the alarm clock color.
     */
    public static int getAlarmClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_CLOCK_COLOR, DEFAULT_ALARM_CLOCK_COLOR);
    }

    /**
     * @return a value indicating the alarm second hand color.
     */
    public static int getAlarmSecondHandColor(SharedPreferences prefs, Context context) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_SECOND_HAND_COLOR, getDefaultAlarmInversePrimaryColor(context));
    }

    /**
     * @return a value indicating the alarm title color.
     */
    public static int getAlarmTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_TITLE_COLOR, DEFAULT_ALARM_TITLE_COLOR);
    }

    /**
     * @return a value indicating the slide zone color.
     */
    public static int getSlideZoneColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_SLIDE_ZONE_COLOR, DEFAULT_SLIDE_ZONE_COLOR);
    }

    /**
     * @return a value indicating the color of "Snooze" title.
     */
    public static int getSnoozeTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_SNOOZE_TITLE_COLOR, DEFAULT_SNOOZE_TITLE_COLOR);
    }

    /**
     * @return a value indicating the snooze button color.
     */
    public static int getSnoozeButtonColor(SharedPreferences prefs, Context context) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_SNOOZE_BUTTON_COLOR, getDefaultAlarmInversePrimaryColor(context));
    }

    /**
     * @return a value indicating the color of "Dismiss" title.
     */
    public static int getDismissTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_DISMISS_TITLE_COLOR, DEFAULT_DISMISS_TITLE_COLOR);
    }

    /**
     * @return a value indicating the dismiss button color.
     */
    public static int getDismissButtonColor(SharedPreferences prefs, Context context) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_DISMISS_BUTTON_COLOR, getDefaultAlarmInversePrimaryColor(context));
    }

    /**
     * @return a value indicating the alarm button color.
     */
    public static int getAlarmButtonColor(SharedPreferences prefs, Context context) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_BUTTON_COLOR, getDefaultAlarmInversePrimaryColor(context));
    }

    /**
     * @return the font size applied to the alarm digital clock.
     */
    public static int getAlarmDigitalClockFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE, DEFAULT_ALARM_DIGITAL_CLOCK_FONT_SIZE);
    }

    /**
     * @return the font size applied to the alarm title.
     */
    public static int getAlarmTitleFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_TITLE_FONT_SIZE_PREF, DEFAULT_ALARM_TITLE_FONT_SIZE_PREF);
    }

    /**
     * @return {@code true} if a shadow is displayed on the texts of the triggered alarm.
     * {@code false} otherwise.
     */
    public static boolean isAlarmTextShadowDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getBoolean(KEY_ALARM_DISPLAY_TEXT_SHADOW, DEFAULT_ALARM_DISPLAY_TEXT_SHADOW);
    }

    /**
     * @return a value indicating the shadow color displayed on the triggered alarm texts.
     */
    public static int getAlarmShadowColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_SHADOW_COLOR, DEFAULT_ALARM_SHADOW_COLOR);
    }

    /**
     * @return a value indicating the shadow offset for the triggered alarm texts.
     */
    public static int getAlarmShadowOffset(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_SHADOW_OFFSET, DEFAULT_ALARM_SHADOW_OFFSET);
    }

    /**
     * @return {@code true} if the ringtone title should be displayed on the lock screen when the alarm is triggered.
     * {@code false} otherwise.
     */
    public static boolean isRingtoneTitleDisplayed(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getBoolean(KEY_DISPLAY_RINGTONE_TITLE, DEFAULT_DISPLAY_RINGTONE_TITLE);
    }

    /**
     * @return a value indicating the ringtone title color.
     */
    public static int getRingtoneTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_RINGTONE_TITLE_COLOR, DEFAULT_RINGTONE_TITLE_COLOR);
    }

    /**
     * @return {@code true} if a background image can be selected for triggered alarms.
     * {@code false} otherwise.
     */
    public static boolean isAlarmBackgroundImageEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_ENABLE_ALARM_BACKGROUND_IMAGE, DEFAULT_ENABLE_BACKGROUND_IMAGE);
    }

    /**
     * @return the URI of the image to be displayed on the lock screen when the alarm is triggered.
     */
    public static String getAlarmBackgroundImage(SharedPreferences prefs) {
        return prefs.getString(KEY_ALARM_BACKGROUND_IMAGE, null);
    }

    /**
     * @return {@code true} if a blur effect should be applied to the image when the alarm is triggered.
     * {@code false} otherwise.
     */
    public static boolean isAlarmBlurEffectEnabled(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getBoolean(KEY_ENABLE_ALARM_BLUR_EFFECT, DEFAULT_ENABLE_BLUR_EFFECT);
    }

    /**
     * @return the blur intensity applied to the image when the alarm is triggered.
     */
    public static int getAlarmBlurIntensity(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm_display.xml
        return prefs.getInt(KEY_ALARM_BLUR_INTENSITY, DEFAULT_BLUR_INTENSITY);
    }

    private static ClockStyle getClockStyle(SharedPreferences prefs, String key) {
        final String clockStyle = prefs.getString(key, DEFAULT_CLOCK_STYLE);
        // Use hardcoded locale to perform toUpperCase, because in some languages toUpperCase adds
        // accent to character, which breaks the enum conversion.
        return ClockStyle.valueOf(clockStyle.toUpperCase(Locale.US));
    }

    /**
     * @return the action to execute when volume up button is pressed for the stopwatch
     */
    public static String getVolumeUpActionForStopwatch(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_stopwatch.xml
        return prefs.getString(KEY_SW_VOLUME_UP_ACTION, DEFAULT_SW_ACTION);
    }

    /**
     * @return the action to execute when volume up button is long pressed for the stopwatch
     */
    public static String getVolumeUpActionAfterLongPressForStopwatch(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_stopwatch.xml
        return prefs.getString(KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS, DEFAULT_SW_ACTION);
    }

    /**
     * @return the action to execute when volume down button is pressed for the stopwatch
     */
    public static String getVolumeDownActionForStopwatch(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_stopwatch.xml
        return prefs.getString(KEY_SW_VOLUME_DOWN_ACTION, DEFAULT_SW_ACTION);
    }

    /**
     * @return the action to execute when volume down button is long pressed for the stopwatch
     */
    public static String getVolumeDownActionAfterLongPressForStopwatch(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_stopwatch.xml
        return prefs.getString(KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS, DEFAULT_SW_ACTION);
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
