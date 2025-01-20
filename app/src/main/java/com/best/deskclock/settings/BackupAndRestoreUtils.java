/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Inspired by Heliboard (https://github.com/Helium314/HeliBoard/blob/main/app/src/main/java/helium314/keyboard/latin/settings/AdvancedSettingsFragment.kt)
 */

package com.best.deskclock.settings;

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
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ALARM_CRESCENDO;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ALARM_NOTIFICATION_REMINDER_TIME;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ALARM_SNOOZE;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_AUTO_SILENCE;
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
import static com.best.deskclock.settings.ClockSettingsActivity.KEY_AUTO_HOME_CLOCK;
import static com.best.deskclock.settings.ClockSettingsActivity.KEY_CLOCK_DISPLAY_SECONDS;
import static com.best.deskclock.settings.ClockSettingsActivity.KEY_CLOCK_STYLE;
import static com.best.deskclock.settings.ClockSettingsActivity.KEY_HOME_TIME_ZONE;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_BACKGROUND_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_AUTO_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_CARD_BACKGROUND;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_CARD_BORDER;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_DARK_MODE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_FADE_TRANSITIONS;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_TAB_INDICATOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_VIBRATIONS;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE;
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
import static com.best.deskclock.settings.StopwatchSettingsActivity.KEY_SW_VOLUME_DOWN_ACTION;
import static com.best.deskclock.settings.StopwatchSettingsActivity.KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS;
import static com.best.deskclock.settings.StopwatchSettingsActivity.KEY_SW_VOLUME_UP_ACTION;
import static com.best.deskclock.settings.StopwatchSettingsActivity.KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_DEFAULT_TIME_TO_ADD_TO_TIMER;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_KEEP_TIMER_SCREEN_ON;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_SORT_TIMER;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_AUTO_SILENCE;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_CRESCENDO;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_FLIP_ACTION;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_POWER_BUTTON_ACTION;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_SHAKE_ACTION;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_VIBRATE;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TIMER_VOLUME_BUTTONS_ACTION;
import static com.best.deskclock.settings.TimerSettingsActivity.KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import com.best.deskclock.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
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
                    // Values from InterfaceCustomizationActivity
                    booleans.put(KEY_AUTO_NIGHT_ACCENT_COLOR, prefs.getBoolean(KEY_AUTO_NIGHT_ACCENT_COLOR, true));
                    booleans.put(KEY_CARD_BACKGROUND, prefs.getBoolean(KEY_CARD_BACKGROUND, true));
                    booleans.put(KEY_CARD_BORDER, prefs.getBoolean(KEY_CARD_BORDER, false));
                    booleans.put(KEY_VIBRATIONS, prefs.getBoolean(KEY_VIBRATIONS, false));
                    booleans.put(KEY_TAB_INDICATOR, prefs.getBoolean(KEY_TAB_INDICATOR, true));
                    booleans.put(KEY_FADE_TRANSITIONS, prefs.getBoolean(KEY_FADE_TRANSITIONS, false));

                    // Values from ClockSettingsActivity
                    booleans.put(KEY_CLOCK_DISPLAY_SECONDS, prefs.getBoolean(KEY_CLOCK_DISPLAY_SECONDS, false));
                    booleans.put(KEY_AUTO_HOME_CLOCK, prefs.getBoolean(KEY_AUTO_HOME_CLOCK, true));

                    // Values from AlarmSettingsActivity
                    booleans.put(KEY_SWIPE_ACTION, prefs.getBoolean(KEY_SWIPE_ACTION, true));
                    booleans.put(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, prefs.getBoolean(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, false));
                    booleans.put(KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS, prefs.getBoolean(KEY_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS, false));
                    booleans.put(KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT, prefs.getBoolean(KEY_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT, false));

                    // Values from AlarmDisplayCustomizationActivity
                    booleans.put(KEY_DISPLAY_ALARM_SECONDS_HAND, prefs.getBoolean(KEY_DISPLAY_ALARM_SECONDS_HAND, true));
                    booleans.put(KEY_DISPLAY_RINGTONE_TITLE, prefs.getBoolean(KEY_DISPLAY_RINGTONE_TITLE, false));

                    // Values from TimerSettingsActivity
                    booleans.put(KEY_TIMER_VIBRATE, prefs.getBoolean(KEY_TIMER_VIBRATE, false));
                    booleans.put(KEY_TIMER_VOLUME_BUTTONS_ACTION, prefs.getBoolean(KEY_TIMER_VOLUME_BUTTONS_ACTION, false));
                    booleans.put(KEY_TIMER_POWER_BUTTON_ACTION, prefs.getBoolean(KEY_TIMER_POWER_BUTTON_ACTION, false));
                    booleans.put(KEY_TIMER_FLIP_ACTION, prefs.getBoolean(KEY_TIMER_FLIP_ACTION, false));
                    booleans.put(KEY_TIMER_SHAKE_ACTION, prefs.getBoolean(KEY_TIMER_SHAKE_ACTION, false));
                    booleans.put(KEY_KEEP_TIMER_SCREEN_ON, prefs.getBoolean(KEY_KEEP_TIMER_SCREEN_ON, true));
                    booleans.put(KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER, prefs.getBoolean(KEY_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER, false));
                    booleans.put(KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER, prefs.getBoolean(KEY_DISPLAY_WARNING_BEFORE_DELETING_TIMER, true));

                    // Values from ScreensaverSettingsActivity
                    booleans.put(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS, prefs.getBoolean(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS, false));
                    booleans.put(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS, prefs.getBoolean(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS, false));
                    booleans.put(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, prefs.getBoolean(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, false));
                    booleans.put(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC, prefs.getBoolean(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC, false));
                    booleans.put(KEY_SCREENSAVER_DATE_IN_BOLD, prefs.getBoolean(KEY_SCREENSAVER_DATE_IN_BOLD, true));
                    booleans.put(KEY_SCREENSAVER_DATE_IN_ITALIC, prefs.getBoolean(KEY_SCREENSAVER_DATE_IN_ITALIC, false));
                    booleans.put(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD, prefs.getBoolean(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD, true));
                    booleans.put(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC, prefs.getBoolean(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC, false));

                    // Values from DigitalWidgetCustomizationActivity
                    booleans.put(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, false));
                    booleans.put(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, prefs.getBoolean(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, true));
                    booleans.put(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, true));
                    booleans.put(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, true));
                    booleans.put(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, true));
                    booleans.put(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, true));
                    booleans.put(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, true));

                    // Values from VerticalDigitalWidgetCustomizationActivity
                    booleans.put(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND, prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND, false));
                    booleans.put(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, true));
                    booleans.put(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, true));
                    booleans.put(KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR, prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR, true));
                    booleans.put(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, true));

                    // Values from NextAlarmWidgetCustomizationActivity
                    booleans.put(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND, prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND, false));
                    booleans.put(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, true));
                    booleans.put(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, true));
                    booleans.put(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, true));

                    // Values from MaterialYouDigitalWidgetCustomizationActivity
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, true));
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, true));
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, true));
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, true));
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, true));
                    booleans.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, true));

                    // Values from MaterialYouVerticalDigitalWidgetCustomizationActivity
                    booleans.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, true));
                    booleans.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, true));
                    booleans.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, true));
                    booleans.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, true));

                    // Values from MaterialYouNextAlarmWidgetCustomizationActivity
                    booleans.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, true));
                    booleans.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, true));
                    booleans.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, true));

                } else if (entry.getValue() instanceof String) {
                    // Values from InterfaceCustomizationActivity
                    strings.put(KEY_THEME, prefs.getString(KEY_THEME, "0"));
                    strings.put(KEY_DARK_MODE, prefs.getString(KEY_DARK_MODE, "0"));
                    strings.put(KEY_ACCENT_COLOR, prefs.getString(KEY_ACCENT_COLOR, "0"));
                    strings.put(KEY_NIGHT_ACCENT_COLOR, prefs.getString(KEY_NIGHT_ACCENT_COLOR, "0"));

                    // Values from ClockSettingsActivity
                    strings.put(KEY_CLOCK_STYLE, prefs.getString(KEY_CLOCK_STYLE, "digital"));
                    strings.put(KEY_HOME_TIME_ZONE, prefs.getString(KEY_HOME_TIME_ZONE, null));

                    // Values from AlarmSettingsActivity
                    // Todo: possible to backup the alarm ringtone?
                    //strings.put(KEY_DEFAULT_ALARM_RINGTONE, prefs.getString(KEY_DEFAULT_ALARM_RINGTONE, null));
                    strings.put(KEY_AUTO_SILENCE, prefs.getString(KEY_AUTO_SILENCE, "10"));
                    strings.put(KEY_ALARM_SNOOZE, prefs.getString(KEY_ALARM_SNOOZE, "10"));
                    strings.put(KEY_ALARM_CRESCENDO, prefs.getString(KEY_ALARM_CRESCENDO, "0"));
                    strings.put(KEY_VOLUME_BUTTONS, prefs.getString(KEY_VOLUME_BUTTONS, "-1"));
                    strings.put(KEY_POWER_BUTTON, prefs.getString(KEY_POWER_BUTTON, "0"));
                    strings.put(KEY_FLIP_ACTION, prefs.getString(KEY_FLIP_ACTION, "0"));
                    strings.put(KEY_SHAKE_ACTION, prefs.getString(KEY_SHAKE_ACTION, "0"));
                    strings.put(KEY_WEEK_START, prefs.getString(KEY_WEEK_START, String.valueOf(Calendar.getInstance().getFirstDayOfWeek())));
                    strings.put(KEY_ALARM_NOTIFICATION_REMINDER_TIME, prefs.getString(KEY_ALARM_NOTIFICATION_REMINDER_TIME, "30"));
                    strings.put(KEY_MATERIAL_TIME_PICKER_STYLE, prefs.getString(KEY_MATERIAL_TIME_PICKER_STYLE, "analog"));

                    // Values from AlarmDisplayCustomizationActivity
                    strings.put(KEY_ALARM_CLOCK_STYLE, prefs.getString(KEY_ALARM_CLOCK_STYLE, "digital"));
                    strings.put(KEY_ALARM_CLOCK_FONT_SIZE, prefs.getString(KEY_ALARM_CLOCK_FONT_SIZE, "70"));
                    strings.put(KEY_ALARM_TITLE_FONT_SIZE, prefs.getString(KEY_ALARM_TITLE_FONT_SIZE, "26"));

                    // Values from TimerSettingsActivity
                    // Todo: possible to backup the timer ringtone?
                    //strings.put(KEY_DEFAULT_ALARM_RINGTONE, prefs.getString(KEY_DEFAULT_ALARM_RINGTONE, null));
                    strings.put(KEY_TIMER_AUTO_SILENCE, prefs.getString(KEY_TIMER_AUTO_SILENCE, "30"));
                    strings.put(KEY_TIMER_CRESCENDO, prefs.getString(KEY_TIMER_CRESCENDO, "0"));
                    strings.put(KEY_SORT_TIMER, prefs.getString(KEY_SORT_TIMER, "0"));
                    strings.put(KEY_DEFAULT_TIME_TO_ADD_TO_TIMER, prefs.getString(KEY_DEFAULT_TIME_TO_ADD_TO_TIMER, "1"));

                    // Values from StopwatchSettingsActivity
                    strings.put(KEY_SW_VOLUME_UP_ACTION, prefs.getString(KEY_SW_VOLUME_UP_ACTION, "0"));
                    strings.put(KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS, prefs.getString(KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS, "0"));
                    strings.put(KEY_SW_VOLUME_DOWN_ACTION, prefs.getString(KEY_SW_VOLUME_DOWN_ACTION, "0"));
                    strings.put(KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS, prefs.getString(KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS, "0"));

                    // Values from ScreensaverSettingsActivity
                    strings.put(KEY_SCREENSAVER_CLOCK_STYLE, prefs.getString(KEY_SCREENSAVER_CLOCK_STYLE, "digital"));

                    // Values from DigitalWidgetCustomizationActivity
                    strings.put(KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, prefs.getString(KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, "80"));

                    // Values from VerticalDigitalWidgetCustomizationActivity
                    strings.put(KEY_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, prefs.getString(KEY_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, "70"));

                    // Values from NextAlarmWidgetCustomizationActivity
                    strings.put(KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE, prefs.getString(KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE, "70"));

                    // Values from MaterialYouDigitalWidgetCustomizationActivity
                    strings.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, prefs.getString(KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, "80"));

                    // Values from MaterialYouVerticalDigitalWidgetCustomizationActivity
                    strings.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, prefs.getString(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, "70"));

                    // Values from MaterialYouNextAlarmWidgetCustomizationActivity
                    strings.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE, prefs.getString(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE, "70"));

                } else if (entry.getValue() instanceof Integer) {
                    // Values from AlarmDisplayCustomizationActivity
                    ints.put(KEY_ALARM_BACKGROUND_COLOR, prefs.getInt(KEY_ALARM_BACKGROUND_COLOR, Color.parseColor("#FF191C1E")));
                    ints.put(KEY_ALARM_BACKGROUND_AMOLED_COLOR, prefs.getInt(KEY_ALARM_BACKGROUND_AMOLED_COLOR, Color.parseColor("#FF000000")));
                    ints.put(KEY_ALARM_CLOCK_COLOR, prefs.getInt(KEY_ALARM_CLOCK_COLOR, Color.parseColor("#FF8A9297")));
                    ints.put(KEY_ALARM_SECONDS_HAND_COLOR, prefs.getInt(KEY_ALARM_SECONDS_HAND_COLOR, context.getColor(R.color.md_theme_primary)));
                    ints.put(KEY_ALARM_TITLE_COLOR, prefs.getInt(KEY_ALARM_TITLE_COLOR, Color.parseColor("#FF8A9297")));
                    ints.put(KEY_SNOOZE_BUTTON_COLOR, prefs.getInt(KEY_SNOOZE_BUTTON_COLOR, Color.parseColor("#FF8A9297")));
                    ints.put(KEY_DISMISS_BUTTON_COLOR, prefs.getInt(KEY_DISMISS_BUTTON_COLOR, Color.parseColor("#FF8A9297")));
                    ints.put(KEY_ALARM_BUTTON_COLOR, prefs.getInt(KEY_ALARM_BUTTON_COLOR, Color.parseColor("#FF8A9297")));
                    ints.put(KEY_PULSE_COLOR, prefs.getInt(KEY_PULSE_COLOR, Color.parseColor("#FFC0C7CD")));

                    // Values from ScreensaverSettingsActivity
                    ints.put(KEY_SCREENSAVER_CLOCK_COLOR_PICKER, prefs.getInt(KEY_SCREENSAVER_CLOCK_COLOR_PICKER, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_SCREENSAVER_DATE_COLOR_PICKER, prefs.getInt(KEY_SCREENSAVER_DATE_COLOR_PICKER, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER, prefs.getInt(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_SCREENSAVER_BRIGHTNESS, prefs.getInt(KEY_SCREENSAVER_BRIGHTNESS, 40));

                    // Values from DigitalWidgetCustomizationActivity
                    ints.put(KEY_DIGITAL_WIDGET_BACKGROUND_COLOR, prefs.getInt(KEY_DIGITAL_WIDGET_BACKGROUND_COLOR, Color.parseColor("#70000000")));
                    ints.put(KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, Color.parseColor("#FFFFFF")));

                    // Values from VerticalDigitalWidgetCustomizationActivity
                    ints.put(KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR, prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR, Color.parseColor("#70000000")));
                    ints.put(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, Color.parseColor("#FFFFFF")));

                    // Values from NextAlarmWidgetCustomizationActivity
                    ints.put(KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR, prefs.getInt(KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR, Color.parseColor("#70000000")));
                    ints.put(KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, Color.parseColor("#FFFFFF")));

                    // Values from MaterialYouDigitalWidgetCustomizationActivity
                    ints.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, Color.parseColor("#FFFFFF")));

                    // Values from MaterialYouVerticalDigitalWidgetCustomizationActivity
                    ints.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, Color.parseColor("#FFFFFF")));

                    // Values from MaterialYouNextAlarmWidgetCustomizationActivity
                    ints.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, Color.parseColor("#FFFFFF")));
                    ints.put(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, Color.parseColor("#FFFFFF")));
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
