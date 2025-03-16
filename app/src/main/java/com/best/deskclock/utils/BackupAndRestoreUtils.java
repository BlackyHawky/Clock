/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Inspired by Heliboard (https://github.com/Helium314/HeliBoard/blob/main/app/src/main/java/helium314/keyboard/latin/settings/AdvancedSettingsFragment.kt)
 */

package com.best.deskclock.utils;

import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * This class lists all settings that can be saved or restored.
 */
public class BackupAndRestoreUtils {

    /**
     * Read and export values in SharedPreferences to a file.
     */
    public static void settingsToJsonStream(Map<String, ?> settings, OutputStream out, SharedPreferences prefs, Context context) throws IOException {

        Map<String, Boolean> booleans = new HashMap<>();
        Map<String, String> strings = new HashMap<>();
        Map<String, Integer> ints = new HashMap<>();

        for (Map.Entry<String, ?> entry : settings.entrySet()) {
            if (entry.getKey() != null) {
                if (entry.getValue() instanceof Boolean) {
                    // InterfaceCustomizationFragment
                    booleans.put(KEY_AUTO_NIGHT_ACCENT_COLOR, prefs.getBoolean(
                            KEY_AUTO_NIGHT_ACCENT_COLOR, DEFAULT_AUTO_NIGHT_ACCENT_COLOR));
                    booleans.put(KEY_CARD_BACKGROUND, prefs.getBoolean(
                            KEY_CARD_BACKGROUND, DEFAULT_CARD_BACKGROUND));
                    booleans.put(KEY_CARD_BORDER, prefs.getBoolean(
                            KEY_CARD_BORDER, DEFAULT_CARD_BORDER));
                    booleans.put(KEY_VIBRATIONS, prefs.getBoolean(
                            KEY_VIBRATIONS, DEFAULT_VIBRATIONS));
                    booleans.put(KEY_TAB_INDICATOR, prefs.getBoolean(
                            KEY_TAB_INDICATOR, DEFAULT_TAB_INDICATOR));
                    booleans.put(KEY_FADE_TRANSITIONS, prefs.getBoolean(
                            KEY_FADE_TRANSITIONS, DEFAULT_FADE_TRANSITIONS));

                    // ClockSettingsFragment
                    booleans.put(KEY_CLOCK_DISPLAY_SECONDS, prefs.getBoolean(
                            KEY_CLOCK_DISPLAY_SECONDS, DEFAULT_CLOCK_DISPLAY_SECONDS));
                    booleans.put(KEY_AUTO_HOME_CLOCK, prefs.getBoolean(
                            KEY_AUTO_HOME_CLOCK, DEFAULT_AUTO_HOME_CLOCK));

                    // AlarmSettingsFragment
                    booleans.put(KEY_SWIPE_ACTION, prefs.getBoolean(
                            KEY_SWIPE_ACTION, DEFAULT_SWIPE_ACTION));
                    booleans.put(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, prefs.getBoolean(
                            KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, DEFAULT_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT));
                    booleans.put(KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS, prefs.getBoolean(
                            KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS, DEFAULT_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS));
                    booleans.put(KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM, prefs.getBoolean(
                            KEY_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM, DEFAULT_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM));
                    booleans.put(KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT, prefs.getBoolean(
                            KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT, DEFAULT_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT));

                    // AlarmDisplayCustomizationFragment
                    booleans.put(KEY_DISPLAY_ALARM_SECONDS_HAND, prefs.getBoolean(
                            KEY_DISPLAY_ALARM_SECONDS_HAND, DEFAULT_DISPLAY_ALARM_SECONDS_HAND));
                    booleans.put(KEY_DISPLAY_RINGTONE_TITLE, prefs.getBoolean(
                            KEY_DISPLAY_RINGTONE_TITLE, DEFAULT_DISPLAY_RINGTONE_TITLE));

                    // TimerSettingsFragment
                    booleans.put(KEY_TIMER_VIBRATE, prefs.getBoolean(
                            KEY_TIMER_VIBRATE, DEFAULT_TIMER_VIBRATE));
                    booleans.put(KEY_TIMER_VOLUME_BUTTONS_ACTION, prefs.getBoolean(
                            KEY_TIMER_VOLUME_BUTTONS_ACTION, DEFAULT_TIMER_VOLUME_BUTTONS_ACTION));
                    booleans.put(KEY_TIMER_POWER_BUTTON_ACTION, prefs.getBoolean(
                            KEY_TIMER_POWER_BUTTON_ACTION, DEFAULT_TIMER_POWER_BUTTON_ACTION));
                    booleans.put(KEY_TIMER_FLIP_ACTION, prefs.getBoolean(
                            KEY_TIMER_FLIP_ACTION, DEFAULT_TIMER_FLIP_ACTION));
                    booleans.put(KEY_TIMER_SHAKE_ACTION, prefs.getBoolean(
                            KEY_TIMER_SHAKE_ACTION, DEFAULT_TIMER_SHAKE_ACTION));
                    booleans.put(KEY_KEEP_TIMER_SCREEN_ON, prefs.getBoolean(
                            KEY_KEEP_TIMER_SCREEN_ON, DEFAULT_KEEP_TIMER_SCREEN_ON));
                    booleans.put(KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER, prefs.getBoolean(
                            KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER, DEFAULT_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER));
                    booleans.put(KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER, prefs.getBoolean(
                            KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER, DEFAULT_DISPLAY_WARNING_BEFORE_DELETING_TIMER));

                    // ScreensaverSettingsFragment
                    booleans.put(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS, prefs.getBoolean(
                            KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS, DEFAULT_DISPLAY_SCREENSAVER_CLOCK_SECONDS));
                    booleans.put(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS, prefs.getBoolean(
                            KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS, DEFAULT_SCREENSAVER_CLOCK_DYNAMIC_COLORS));
                    booleans.put(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, prefs.getBoolean(
                            KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, DEFAULT_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD));
                    booleans.put(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC, prefs.getBoolean(
                            KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC, DEFAULT_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC));
                    booleans.put(KEY_SCREENSAVER_DATE_IN_BOLD, prefs.getBoolean(
                            KEY_SCREENSAVER_DATE_IN_BOLD, DEFAULT_SCREENSAVER_DATE_IN_BOLD));
                    booleans.put(KEY_SCREENSAVER_DATE_IN_ITALIC, prefs.getBoolean(
                            KEY_SCREENSAVER_DATE_IN_ITALIC, DEFAULT_SCREENSAVER_DATE_IN_ITALIC));
                    booleans.put(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD, prefs.getBoolean(
                            KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD, DEFAULT_SCREENSAVER_NEXT_ALARM_IN_BOLD));
                    booleans.put(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC, prefs.getBoolean(
                            KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC, DEFAULT_SCREENSAVER_NEXT_ALARM_IN_ITALIC));

                    // DigitalWidgetSettingsFragment
                    booleans.put(KEY_DIGITAL_WIDGET_DISPLAY_SECONDS, prefs.getBoolean(
                            KEY_DIGITAL_WIDGET_DISPLAY_SECONDS, DEFAULT_DIGITAL_WIDGET_DISPLAY_SECONDS));
                    booleans.put(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, prefs.getBoolean(
                            KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND));
                    booleans.put(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, prefs.getBoolean(
                            KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, DEFAULT_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED));
                    booleans.put(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, prefs.getBoolean(
                            KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, prefs.getBoolean(
                            KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, prefs.getBoolean(
                            KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, prefs.getBoolean(
                            KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, prefs.getBoolean(
                            KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));

                    // VerticalDigitalWidgetSettingsFragment
                    booleans.put(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND, prefs.getBoolean(
                            KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND, DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND));
                    booleans.put(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, prefs.getBoolean(
                            KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, prefs.getBoolean(
                            KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR, prefs.getBoolean(
                            KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, prefs.getBoolean(
                            KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));

                    // NextAlarmWidgetSettingsFragment
                    booleans.put(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND, prefs.getBoolean(
                            KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND, DEFAULT_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND));
                    booleans.put(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, prefs.getBoolean(
                            KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, prefs.getBoolean(
                            KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, prefs.getBoolean(
                            KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));

                    // MaterialYouDigitalWidgetSettingsFragment
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, prefs.getBoolean(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED));
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));

                    // MaterialYouVerticalDigitalWidgetSettingsFragment
                    booleans.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));

                    // MaterialYouNextAlarmWidgetSettingsFragment
                    booleans.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));
                    booleans.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, prefs.getBoolean(
                            KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR));

                } else if (entry.getValue() instanceof String) {
                    // InterfaceCustomizationFragment
                    strings.put(KEY_CUSTOM_LANGUAGE_CODE, prefs.getString(
                            KEY_CUSTOM_LANGUAGE_CODE, DEFAULT_SYSTEM_LANGUAGE_CODE));
                    strings.put(KEY_THEME, prefs.getString(
                            KEY_THEME, SYSTEM_THEME));
                    strings.put(KEY_DARK_MODE, prefs.getString(
                            KEY_DARK_MODE, DEFAULT_DARK_MODE));
                    strings.put(KEY_ACCENT_COLOR, prefs.getString(
                            KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR));
                    strings.put(KEY_NIGHT_ACCENT_COLOR, prefs.getString(
                            KEY_NIGHT_ACCENT_COLOR, DEFAULT_NIGHT_ACCENT_COLOR));

                    // ClockSettingsFragment
                    strings.put(KEY_CLOCK_STYLE, prefs.getString(
                            KEY_CLOCK_STYLE, DEFAULT_CLOCK_STYLE));
                    strings.put(KEY_HOME_TIME_ZONE, prefs.getString(
                            KEY_HOME_TIME_ZONE, DEFAULT_HOME_TIME_ZONE));

                    // AlarmSettingsFragment
                    // Todo: possible to backup the alarm ringtone?
                    //strings.put(KEY_DEFAULT_ALARM_RINGTONE, prefs.getString(KEY_DEFAULT_ALARM_RINGTONE, null));
                    strings.put(KEY_AUTO_SILENCE, prefs.getString(
                            KEY_AUTO_SILENCE, DEFAULT_AUTO_SILENCE));
                    strings.put(KEY_ALARM_SNOOZE_DURATION, prefs.getString(
                            KEY_ALARM_SNOOZE_DURATION, DEFAULT_ALARM_SNOOZE_DURATION));
                    strings.put(KEY_ALARM_CRESCENDO_DURATION, prefs.getString(
                            KEY_ALARM_CRESCENDO_DURATION, DEFAULT_ALARM_CRESCENDO_DURATION));
                    strings.put(KEY_VOLUME_BUTTONS, prefs.getString(
                            KEY_VOLUME_BUTTONS, DEFAULT_VOLUME_BEHAVIOR));
                    strings.put(KEY_POWER_BUTTON, prefs.getString(
                            KEY_POWER_BUTTON, DEFAULT_POWER_BEHAVIOR));
                    strings.put(KEY_FLIP_ACTION, prefs.getString(
                            KEY_FLIP_ACTION, DEFAULT_FLIP_ACTION));
                    strings.put(KEY_SHAKE_ACTION, prefs.getString(
                            KEY_SHAKE_ACTION, DEFAULT_SHAKE_ACTION));
                    strings.put(KEY_WEEK_START, prefs.getString(
                            KEY_WEEK_START, DEFAULT_WEEK_START));
                    strings.put(KEY_ALARM_NOTIFICATION_REMINDER_TIME, prefs.getString(
                            KEY_ALARM_NOTIFICATION_REMINDER_TIME, DEFAULT_ALARM_NOTIFICATION_REMINDER_TIME));
                    strings.put(KEY_MATERIAL_TIME_PICKER_STYLE, prefs.getString(
                            KEY_MATERIAL_TIME_PICKER_STYLE, DEFAULT_TIME_PICKER_STYLE));

                    // AlarmDisplayCustomizationFragment
                    strings.put(KEY_ALARM_CLOCK_STYLE, prefs.getString(
                            KEY_ALARM_CLOCK_STYLE, DEFAULT_CLOCK_STYLE));
                    strings.put(KEY_ALARM_CLOCK_FONT_SIZE, prefs.getString(
                            KEY_ALARM_CLOCK_FONT_SIZE, DEFAULT_ALARM_CLOCK_FONT_SIZE));
                    strings.put(KEY_ALARM_TITLE_FONT_SIZE, prefs.getString(
                            KEY_ALARM_TITLE_FONT_SIZE, DEFAULT_ALARM_TITLE_FONT_SIZE));

                    // TimerSettingsFragment
                    // Todo: possible to backup the timer ringtone?
                    //strings.put(KEY_DEFAULT_ALARM_RINGTONE, prefs.getString(KEY_DEFAULT_ALARM_RINGTONE, null));
                    strings.put(KEY_TIMER_AUTO_SILENCE, prefs.getString(
                            KEY_TIMER_AUTO_SILENCE, DEFAULT_TIMER_AUTO_SILENCE));
                    strings.put(KEY_TIMER_CRESCENDO_DURATION, prefs.getString(
                            KEY_TIMER_CRESCENDO_DURATION, DEFAULT_TIMER_CRESCENDO_DURATION));
                    strings.put(KEY_SORT_TIMER, prefs.getString(
                            KEY_SORT_TIMER, DEFAULT_SORT_TIMER_MANUALLY));
                    strings.put(KEY_DEFAULT_TIME_TO_ADD_TO_TIMER, prefs.getString(
                            KEY_DEFAULT_TIME_TO_ADD_TO_TIMER, DEFAULT_TIME_TO_ADD_TO_TIMER));

                    // StopwatchSettingsFragment
                    strings.put(KEY_SW_VOLUME_UP_ACTION, prefs.getString(
                            KEY_SW_VOLUME_UP_ACTION, DEFAULT_SW_ACTION));
                    strings.put(KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS, prefs.getString(
                            KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS, DEFAULT_SW_ACTION));
                    strings.put(KEY_SW_VOLUME_DOWN_ACTION, prefs.getString(
                            KEY_SW_VOLUME_DOWN_ACTION, DEFAULT_SW_ACTION));
                    strings.put(KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS, prefs.getString(
                            KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS, DEFAULT_SW_ACTION));

                    // ScreensaverSettingsFragment
                    strings.put(KEY_SCREENSAVER_CLOCK_STYLE, prefs.getString(
                            KEY_SCREENSAVER_CLOCK_STYLE, DEFAULT_CLOCK_STYLE));

                    // DigitalWidgetSettingsFragment
                    strings.put(KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, prefs.getString(
                            KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, DEFAULT_DIGITAL_WIDGET_FONT_SIZE));

                    // VerticalDigitalWidgetSettingsFragment
                    strings.put(KEY_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, prefs.getString(
                            KEY_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE));

                    // NextAlarmWidgetSettingsFragment
                    strings.put(KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE, prefs.getString(
                            KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE));

                    // MaterialYouDigitalWidgetSettingsFragment
                    strings.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, prefs.getString(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_FONT_SIZE));

                    // MaterialYouVerticalDigitalWidgetSettingsFragment
                    strings.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, prefs.getString(
                            KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE));

                    // MaterialYouNextAlarmWidgetSettingsFragment
                    strings.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE, prefs.getString(
                            KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE));

                } else if (entry.getValue() instanceof Integer) {
                    // AlarmSettingsFragment
                    ints.put(KEY_SHAKE_INTENSITY, prefs.getInt(
                            KEY_SHAKE_INTENSITY, DEFAULT_SHAKE_INTENSITY));

                    // AlarmDisplayCustomizationFragment
                    ints.put(KEY_ALARM_BACKGROUND_COLOR, prefs.getInt(
                            KEY_ALARM_BACKGROUND_COLOR, DEFAULT_ALARM_BACKGROUND_COLOR));
                    ints.put(KEY_ALARM_BACKGROUND_AMOLED_COLOR, prefs.getInt(
                            KEY_ALARM_BACKGROUND_AMOLED_COLOR, DEFAULT_ALARM_BACKGROUND_AMOLED_COLOR));
                    ints.put(KEY_ALARM_CLOCK_COLOR, prefs.getInt(
                            KEY_ALARM_CLOCK_COLOR, DEFAULT_ALARM_CLOCK_COLOR));
                    ints.put(KEY_ALARM_SECONDS_HAND_COLOR, prefs.getInt(
                            KEY_ALARM_SECONDS_HAND_COLOR, getDefaultAlarmSecondsHandColor(context)));
                    ints.put(KEY_ALARM_TITLE_COLOR, prefs.getInt(
                            KEY_ALARM_TITLE_COLOR, DEFAULT_ALARM_TITLE_COLOR));
                    ints.put(KEY_SNOOZE_BUTTON_COLOR, prefs.getInt(
                            KEY_SNOOZE_BUTTON_COLOR, DEFAULT_SNOOZE_BUTTON_COLOR));
                    ints.put(KEY_DISMISS_BUTTON_COLOR, prefs.getInt(
                            KEY_DISMISS_BUTTON_COLOR, DEFAULT_DISMISS_BUTTON_COLOR));
                    ints.put(KEY_ALARM_BUTTON_COLOR, prefs.getInt(
                            KEY_ALARM_BUTTON_COLOR, DEFAULT_ALARM_BUTTON_COLOR));
                    ints.put(KEY_PULSE_COLOR, prefs.getInt(
                            KEY_PULSE_COLOR, DEFAULT_PULSE_COLOR));

                    // ScreensaverSettingsFragment
                    ints.put(KEY_SCREENSAVER_CLOCK_COLOR_PICKER, prefs.getInt(
                            KEY_SCREENSAVER_CLOCK_COLOR_PICKER, DEFAULT_SCREENSAVER_CUSTOM_COLOR));
                    ints.put(KEY_SCREENSAVER_DATE_COLOR_PICKER, prefs.getInt(
                            KEY_SCREENSAVER_DATE_COLOR_PICKER, DEFAULT_SCREENSAVER_CUSTOM_COLOR));
                    ints.put(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER, prefs.getInt(
                            KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER, DEFAULT_SCREENSAVER_CUSTOM_COLOR));
                    ints.put(KEY_SCREENSAVER_BRIGHTNESS, prefs.getInt(
                            KEY_SCREENSAVER_BRIGHTNESS, DEFAULT_SCREENSAVER_BRIGHTNESS));

                    // DigitalWidgetSettingsFragment
                    ints.put(KEY_DIGITAL_WIDGET_BACKGROUND_COLOR, prefs.getInt(
                            KEY_DIGITAL_WIDGET_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR));
                    ints.put(KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, prefs.getInt(
                            KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, prefs.getInt(
                            KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, prefs.getInt(
                            KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, prefs.getInt(
                            KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, prefs.getInt(
                            KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));

                    // VerticalDigitalWidgetSettingsFragment
                    ints.put(KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR, prefs.getInt(
                            KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR));
                    ints.put(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, prefs.getInt(
                            KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, prefs.getInt(
                            KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, prefs.getInt(
                            KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, prefs.getInt(
                            KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));

                    // NextAlarmWidgetSettingsFragment
                    ints.put(KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR, prefs.getInt(
                            KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR));
                    ints.put(KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, prefs.getInt(
                            KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, prefs.getInt(
                            KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, prefs.getInt(
                            KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));

                    // MaterialYouDigitalWidgetSettingsFragment
                    ints.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));

                    // MaterialYouVerticalDigitalWidgetSettingsFragment
                    ints.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));

                    // MaterialYouNextAlarmWidgetSettingsFragment
                    ints.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                    ints.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, prefs.getInt(
                            KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR));
                }
            }
        }

        StringBuilder jsonBuilder = new StringBuilder();

        // Write booleans settings
        jsonBuilder.append("boolean settings\n");
        jsonBuilder.append("{");
        boolean firstBoolean = true;
        for (Map.Entry<String, Boolean> entry : booleans.entrySet()) {
            if (!firstBoolean) {
                jsonBuilder.append(",");
            }
            firstBoolean = false;
            jsonBuilder.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
        }
        jsonBuilder.append("}\n");

        // Write strings settings
        jsonBuilder.append("string settings\n");
        jsonBuilder.append("{");
        boolean firstString = true;
        for (Map.Entry<String, String> entry : strings.entrySet()) {
            if (!firstString) {
                jsonBuilder.append(",");
            }
            firstString = false;
            jsonBuilder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
        }
        jsonBuilder.append("}\n");

        // Write ints settings
        jsonBuilder.append("integer settings\n");
        jsonBuilder.append("{");
        boolean firstInteger = true;
        for (Map.Entry<String, Integer> entry : ints.entrySet()) {
            if (!firstInteger) {
                jsonBuilder.append(",");
            }
            firstInteger = false;
            jsonBuilder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
        }
        jsonBuilder.append("}");

        // Write everything in the output stream
        out.write(jsonBuilder.toString().getBytes(StandardCharsets.UTF_8));
        out.close();
    }

    /**
     * Read and apply values to restore in SharedPreferences.
     */
    public static void readJsonLines(InputStream inputStream, SharedPreferences prefs) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        SharedPreferences.Editor editor = prefs.edit();

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                switch (line) {
                    case "boolean settings" -> {
                        line = reader.readLine(); // Go to the next line
                        if (line != null) {
                            Map<String, Boolean> booleans = parseBooleanJson(line);
                            for (Map.Entry<String, Boolean> entry : booleans.entrySet()) {
                                editor.putBoolean(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    case "string settings" -> {
                        line = reader.readLine(); // Go to the next line
                        if (line != null) {
                            Map<String, String> strings = parseStringJson(line);
                            for (Map.Entry<String, String> entry : strings.entrySet()) {
                                editor.putString(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    case "integer settings" -> {
                        line = reader.readLine(); // Go to the next line
                        if (line != null) {
                            Map<String, Integer> ints = parseIntegerJson(line);
                            for (Map.Entry<String, Integer> entry : ints.entrySet()) {
                                editor.putInt(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
            }

            editor.apply();

        } catch (IOException e) {
            Log.w(BackupAndRestoreUtils.class.getName(), "error during restore");
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Log.w(BackupAndRestoreUtils.class.getName(), "error during restore");
            }
        }
    }

    /**
     * Parse the list of Boolean to obtain the key name and value.
     * @param json strings to be parsed and formatted.
     * @return the boolean key name and value.
     */
    private static Map<String, Boolean> parseBooleanJson(String json) {
        Map<String, Boolean> result = new HashMap<>();
        json = json.trim();

        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] entries = json.split(",");
            for (String entry : entries) {
                String[] keyValue = entry.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    Boolean value = Boolean.parseBoolean(keyValue[1].trim());
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    /**
     * Parse the list of String to obtain the key name and value.
     * @param json strings to be parsed and formatted.
     * @return the string key name and value.
     */
    private static Map<String, String> parseStringJson(String json) {
        Map<String, String> result = new HashMap<>();
        json = json.trim();

        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] entries = json.split(",");
            for (String entry : entries) {
                String[] keyValue = entry.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    /**
     * Parse the list of Integer to obtain the key name and value.
     * @param json strings to be parsed and formatted.
     * @return the integer key name and value.
     */
    private static Map<String, Integer> parseIntegerJson(String json) {
        Map<String, Integer> result = new HashMap<>();
        json = json.trim();

        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] entries = json.split(",");
            for (String entry : entries) {
                String[] keyValue = entry.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    Integer value = Integer.parseInt(keyValue[1].trim().replace("\"", ""));
                    result.put(key, value);
                }
            }
        }

        return result;
    }
}
