// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import android.content.Context;
import android.graphics.Color;

import com.google.android.material.color.MaterialColors;

import java.util.Calendar;

public class PreferencesDefaultValues {

    // **************
    // ** SETTINGS **
    // **************

    // About
    public static final boolean DEFAULT_DISPLAY_DEBUG_SETTINGS = false;

    // Interface
    public static final String SYSTEM_THEME = "0";
    public static final String LIGHT_THEME = "1";
    public static final String DARK_THEME = "2";
    public static final String DEFAULT_DARK_MODE = "0";
    public static final String AMOLED_DARK_MODE = "1";
    public static final String DEFAULT_ACCENT_COLOR = "0";
    public static final String BLUE_GRAY_ACCENT_COLOR = "1";
    public static final String BROWN_ACCENT_COLOR = "2";
    public static final String GREEN_ACCENT_COLOR = "3";
    public static final String INDIGO_ACCENT_COLOR = "4";
    public static final String ORANGE_ACCENT_COLOR = "5";
    public static final String PINK_ACCENT_COLOR = "6";
    public static final String RED_ACCENT_COLOR = "7";
    public static final String BLACK_ACCENT_COLOR = "8";
    public static final String PURPLE_ACCENT_COLOR = "9";
    public static final String YELLOW_ACCENT_COLOR = "10";
    public static final String BLUE_ACCENT_COLOR = "11";
    public static final boolean DEFAULT_AUTO_NIGHT_ACCENT_COLOR = true;
    public static final String DEFAULT_NIGHT_ACCENT_COLOR = "0";
    public static final boolean DEFAULT_CARD_BACKGROUND = true;
    public static final boolean DEFAULT_CARD_BORDER = false;
    public static final String DEFAULT_SYSTEM_LANGUAGE_CODE = "system_language_code";
    public static final String DEFAULT_TAB_TO_DISPLAY = "-1";
    public static final boolean DEFAULT_VIBRATIONS = false;
    public static final boolean DEFAULT_TOOLBAR_TITLE = true;
    public static final String DEFAULT_TAB_TITLE_VISIBILITY = "0";
    public static final String TAB_TITLE_VISIBILITY_NEVER = "1";
    public static final boolean DEFAULT_TAB_INDICATOR = true;
    public static final boolean DEFAULT_FADE_TRANSITIONS = false;
    public static final boolean DEFAULT_KEEP_SCREEN_ON = false;

    // Clock
    public static final String DEFAULT_CLOCK_STYLE = "digital";
    public static final boolean DEFAULT_DISPLAY_CLOCK_SECONDS = false;
    public static final String DEFAULT_SORT_CITIES_BY_ASCENDING_TIME_ZONE = "0";
    public static final String SORT_CITIES_BY_DESCENDING_TIME_ZONE = "1";
    public static final String SORT_CITIES_BY_NAME = "2";
    public static final String SORT_CITIES_MANUALLY = "3";
    public static final boolean DEFAULT_ENABLE_CITY_NOTE = false;
    public static final boolean DEFAULT_AUTO_HOME_CLOCK = true;
    public static final String DEFAULT_HOME_TIME_ZONE = null;

    // Alarm
    public static final boolean DEFAULT_ENABLE_PER_ALARM_AUTO_SILENCE = true;
    public static final int DEFAULT_AUTO_SILENCE_DURATION = 10;
    public static final boolean DEFAULT_ENABLE_PER_ALARM_SNOOZE_DURATION = true;
    public static final int DEFAULT_ALARM_SNOOZE_DURATION = 10;
    public static final int ALARM_SNOOZE_DURATION_DISABLED = -1;
    public static final boolean DEFAULT_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT = true;
    public static final String DEFAULT_MISSED_ALARM_REPEAT_LIMIT = "-1";
    public static final boolean DEFAULT_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION = true;
    public static final int DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION = 0;
    public static final boolean DEFAULT_ENABLE_PER_ALARM_VOLUME = false;
    public static final int DEFAULT_ALARM_VOLUME = 5;
    public static final boolean DEFAULT_ADVANCED_AUDIO_PLAYBACK = false;
    public static final boolean DEFAULT_AUTO_ROUTING_TO_BLUETOOTH_DEVICE = false;
    public static final boolean DEFAULT_SYSTEM_MEDIA_VOLUME = true;
    public static final int DEFAULT_BLUETOOTH_VOLUME = 70;
    public static final boolean DEFAULT_SWIPE_ACTION = true;
    public static final String DEFAULT_VOLUME_BEHAVIOR = "-1";
    public static final String VOLUME_BEHAVIOR_CHANGE_VOLUME = "0";
    public static final String VOLUME_BEHAVIOR_SNOOZE = "1";
    public static final String VOLUME_BEHAVIOR_DISMISS = "2";
    public static final String DEFAULT_POWER_BEHAVIOR = "0";
    public static final String POWER_BEHAVIOR_SNOOZE = "1";
    public static final String POWER_BEHAVIOR_DISMISS = "2";
    public static final String DEFAULT_FLIP_ACTION = "0";
    public static final String DEFAULT_SHAKE_ACTION = "0";
    public static final int DEFAULT_SHAKE_INTENSITY = 16;
    public static final String DEFAULT_SORT_BY_ALARM_TIME = "0";
    public static final String SORT_ALARM_BY_NEXT_ALARM_TIME = "1";
    public static final String SORT_ALARM_BY_NAME = "2";
    public static final String SORT_ALARM_BY_DESCENDING_CREATION_ORDER = "3";
    public static final String SORT_ALARM_BY_ASCENDING_CREATION_ORDER = "4";
    public static final boolean DEFAULT_DISPLAY_ENABLED_ALARMS_FIRST = false;
    public static final boolean DEFAULT_ENABLE_ALARM_FAB_LONG_PRESS = false;
    public static final String DEFAULT_WEEK_START = String.valueOf(Calendar.getInstance().getFirstDayOfWeek());
    public static final boolean DEFAULT_DISPLAY_DISMISS_BUTTON = false;
    public static final String DEFAULT_ALARM_NOTIFICATION_REMINDER_TIME = "30";
    public static final String DEFAULT_VIBRATION_PATTERN = "default";
    public static final String VIBRATION_PATTERN_SOFT = "soft";
    public static final String VIBRATION_PATTERN_STRONG = "strong";
    public static final String VIBRATION_PATTERN_HEARTBEAT = "heartbeat";
    public static final String VIBRATION_PATTERN_ESCALATING = "escalating";
    public static final String VIBRATION_PATTERN_TICK_TOCK = "tick_tock";
    public static final int DEFAULT_VIBRATION_START_DELAY = 0;
    public static final boolean DEFAULT_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT = false;
    public static final boolean DEFAULT_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS = false;
    public static final boolean DEFAULT_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM = false;
    public static final boolean DEFAULT_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT = false;
    public static final String DEFAULT_TIME_PICKER_STYLE = "analog";
    public static final String SPINNER_TIME_PICKER_STYLE = "spinner";
    public static final String DEFAULT_DATE_PICKER_STYLE = "calendar";
    public static final String SPINNER_DATE_PICKER_STYLE = "spinner";

    // Alarm Display Customization
    public static final boolean DEFAULT_DISPLAY_ALARM_SECOND_HAND = true;
    public static final int DEFAULT_ALARM_BACKGROUND_COLOR = Color.parseColor("#FF191C1E");
    public static final int DEFAULT_ALARM_BACKGROUND_AMOLED_COLOR = Color.BLACK;
    public static final int DEFAULT_SLIDE_ZONE_COLOR = Color.parseColor("#FF2E3337");
    public static final int DEFAULT_ALARM_CLOCK_COLOR = Color.WHITE;
    public static final int DEFAULT_ALARM_TITLE_COLOR = Color.WHITE;
    public static final int DEFAULT_SNOOZE_TITLE_COLOR = Color.WHITE;
    public static final int DEFAULT_DISMISS_TITLE_COLOR = Color.WHITE;
    public static final int DEFAULT_ALARM_DIGITAL_CLOCK_FONT_SIZE = 70;
    public static final int DEFAULT_ALARM_TITLE_FONT_SIZE_PREF = 30;
    public static final boolean DEFAULT_ALARM_DISPLAY_TEXT_SHADOW = false;
    public static final int DEFAULT_ALARM_SHADOW_COLOR = Color.parseColor("#80FFFFFF");
    public static final int DEFAULT_ALARM_SHADOW_OFFSET = 10;
    public static final boolean DEFAULT_DISPLAY_RINGTONE_TITLE = false;
    public static final int DEFAULT_RINGTONE_TITLE_COLOR = Color.WHITE;
    public static final boolean DEFAULT_ENABLE_BACKGROUND_IMAGE = false;
    public static final boolean DEFAULT_ENABLE_BLUR_EFFECT = false;
    public static final int DEFAULT_BLUR_INTENSITY = 20;
    public static int getDefaultAlarmInversePrimaryColor(Context context) {
        return MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK);
    }

    // Timer
    public static final String DEFAULT_TIMER_CREATION_VIEW_STYLE = "keypad";
    public static final String TIMER_CREATION_VIEW_SPINNER_STYLE = "spinner";
    public static final int DEFAULT_TIMER_AUTO_SILENCE_DURATION = 30; // 30 seconds
    public static final int DEFAULT_TIMER_VOLUME_CRESCENDO_DURATION = 0;
    public static final boolean DEFAULT_TIMER_VIBRATE = false;
    public static final boolean DEFAULT_TIMER_VOLUME_BUTTONS_ACTION = false;
    public static final boolean DEFAULT_TIMER_POWER_BUTTON_ACTION = false;
    public static final boolean DEFAULT_TIMER_FLIP_ACTION = false;
    public static final boolean DEFAULT_TIMER_SHAKE_ACTION = false;
    public static final int DEFAULT_TIMER_SHAKE_INTENSITY = 16;
    public static final String DEFAULT_SORT_TIMER_MANUALLY = "0";
    public static final String SORT_TIMER_BY_ASCENDING_DURATION = "1";
    public static final String SORT_TIMER_BY_DESCENDING_DURATION = "2";
    public static final String SORT_TIMER_BY_NAME = "3";
    public static final int DEFAULT_TIMER_ADD_TIME_BUTTON_VALUE = 60;
    public static final boolean DEFAULT_TRANSPARENT_BACKGROUND_FOR_EXPIRED_TIMER = false;
    public static final boolean DEFAULT_DISPLAY_WARNING_BEFORE_DELETING_TIMER = false;

    // Stopwatch
    public static final String DEFAULT_SW_ACTION = "0";
    public static final String SW_ACTION_START_PAUSE = "1";
    public static final String SW_ACTION_RESET = "2";
    public static final String SW_ACTION_LAP = "3";
    public static final String SW_ACTION_SHARE = "4";

    // Screensaver
    public static final boolean DEFAULT_DISPLAY_SCREENSAVER_CLOCK_SECONDS = false;
    public static final boolean DEFAULT_SCREENSAVER_CLOCK_DYNAMIC_COLORS = false;
    public static final int DEFAULT_SCREENSAVER_CUSTOM_COLOR = Color.WHITE;
    public static final int DEFAULT_SCREENSAVER_BRIGHTNESS = 40;
    public static final boolean DEFAULT_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD = false;
    public static final boolean DEFAULT_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC = false;
    public static final boolean DEFAULT_SCREENSAVER_DATE_IN_BOLD = true;
    public static final boolean DEFAULT_SCREENSAVER_DATE_IN_ITALIC = false;
    public static final boolean DEFAULT_SCREENSAVER_NEXT_ALARM_IN_BOLD = true;
    public static final boolean DEFAULT_SCREENSAVER_NEXT_ALARM_IN_ITALIC = false;

    // Common settings values
    public static final String DEFAULT_CLOCK_DIAL = "dial_with_numbers";
    public static final String DEFAULT_CLOCK_DIAL_MATERIAL = "dial_sun";
    public static final String DEFAULT_CLOCK_SECOND_HAND = "default";
    public static final int TIMEOUT_NEVER = -1;
    public static final int TIMEOUT_END_OF_RINGTONE = -2;

    // **************
    // ** WIDGETS **
    // **************

    // Analog Widget
    public static final String DEFAULT_ANALOG_WIDGET_CLOCK_DIAL = "default";
    public static final String ANALOG_WIDGET_CLOCK_DIAL_WITH_NUMBERS = "dial_with_numbers";
    public static final String ANALOG_WIDGET_CLOCK_DIAL_WITHOUT_NUMBERS = "dial_without_numbers";

    // DigitalWidgetSettingsFragment
    public static final boolean DEFAULT_DIGITAL_WIDGET_DISPLAY_SECONDS = false;
    public static final boolean DEFAULT_DIGITAL_WIDGET_HIDE_AM_PM = false;
    public static final boolean DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND = false;
    public static final boolean DEFAULT_DIGITAL_WIDGET_DISPLAY_DATE = true;
    public static final boolean DEFAULT_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM = true;
    public static final boolean DEFAULT_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED = true;

    // NextAlarmWidgetSettingsFragment
    public static final boolean DEFAULT_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND = false;

    // Vertical Digital Widget
    public static final boolean DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND = false;
    public static final boolean DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE = true;
    public static final boolean DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM = true;

    // Material You Analog Widget
    public static final String DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL = "dial_sun";
    public static final String MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL_FLOWER = "dial_flower";
    public static final int DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_DIAL_COLOR = Color.parseColor("#EEF0FF");
    public static final int DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_HOUR_HAND_COLOR = Color.parseColor("#575E71");
    public static final int DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_MINUTE_HAND_COLOR = Color.parseColor("#475D92");
    public static final int DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_SECOND_HAND_COLOR = Color.parseColor("#725572");

    // Material You Digital Widget
    public static final boolean DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_SECONDS = false;
    public static final boolean DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_HIDE_AM_PM = false;
    public static final boolean DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_BACKGROUND = true;
    public static final boolean DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_DATE = true;
    public static final boolean DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM = true;
    public static final boolean DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED = true;

    // Material You Vertical Digital Widget
    public static final boolean DEFAULT_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE = true;
    public static final boolean DEFAULT_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM = true;

    // Common widget values
    public static final boolean DEFAULT_ANALOG_WIDGET_WITH_SECOND_HAND = false;
    public static final boolean DEFAULT_WIDGETS_DEFAULT_COLOR = true;
    public static final int DEFAULT_WIDGETS_BACKGROUND_COLOR = Color.parseColor("#70000000");
    public static final int DEFAULT_WIDGETS_CUSTOM_COLOR = Color.WHITE;
    public static final int DEFAULT_WIDGETS_FONT_SIZE = 70;
    public static final boolean DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING = true;
}
