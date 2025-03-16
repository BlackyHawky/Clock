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
    public static final boolean DEFAULT_AUTO_NIGHT_ACCENT_COLOR = true;
    public static final String DEFAULT_NIGHT_ACCENT_COLOR = "0";
    public static final boolean DEFAULT_CARD_BACKGROUND = true;
    public static final boolean DEFAULT_CARD_BORDER = false;
    public static final String DEFAULT_SYSTEM_LANGUAGE_CODE = "system_language_code";
    public static final String DEFAULT_TAB_TO_DISPLAY = "-1";
    public static final boolean DEFAULT_VIBRATIONS = false;
    public static final boolean DEFAULT_TAB_INDICATOR = true;
    public static final boolean DEFAULT_FADE_TRANSITIONS = false;

    // Clock
    public static final String DEFAULT_CLOCK_STYLE = "digital";
    public static final boolean DEFAULT_CLOCK_DISPLAY_SECONDS = false;
    public static final boolean DEFAULT_AUTO_HOME_CLOCK = true;
    public static final String DEFAULT_HOME_TIME_ZONE = null;

    // Alarm
    public static final String DEFAULT_AUTO_SILENCE = "10";
    public static final String DEFAULT_ALARM_SNOOZE_DURATION = "10";
    public static final String DEFAULT_ALARM_CRESCENDO_DURATION = "0";
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
    public static final String DEFAULT_WEEK_START = String.valueOf(Calendar.getInstance().getFirstDayOfWeek());
    public static final String DEFAULT_ALARM_NOTIFICATION_REMINDER_TIME = "30";
    public static final boolean DEFAULT_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT = false;
    public static final boolean DEFAULT_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS = false;
    public static final boolean DEFAULT_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM = false;
    public static final boolean DEFAULT_ENABLE_DELETE_OCCASIONAL_ALARM_BY_DEFAULT = false;
    public static final String DEFAULT_TIME_PICKER_STYLE = "analog";
    public static final String SPINNER_TIME_PICKER_STYLE = "spinner";

    // Alarm Display Customization
    public static final boolean DEFAULT_DISPLAY_ALARM_SECONDS_HAND = true;
    public static final int DEFAULT_ALARM_BACKGROUND_COLOR = Color.parseColor("#FF191C1E");
    public static final int DEFAULT_ALARM_BACKGROUND_AMOLED_COLOR = Color.parseColor("#FF000000");
    public static final int DEFAULT_ALARM_CLOCK_COLOR = Color.parseColor("#FF8A9297");
    public static final int DEFAULT_ALARM_TITLE_COLOR = Color.parseColor("#FF8A9297");
    public static final int DEFAULT_SNOOZE_BUTTON_COLOR = Color.parseColor("#FF8A9297");
    public static final int DEFAULT_DISMISS_BUTTON_COLOR = Color.parseColor("#FF8A9297");
    public static final int DEFAULT_ALARM_BUTTON_COLOR = Color.parseColor("#FF8A9297");
    public static final int DEFAULT_PULSE_COLOR = Color.parseColor("#FFC0C7CD");
    public static final String DEFAULT_ALARM_CLOCK_FONT_SIZE = "70";
    public static final String DEFAULT_ALARM_TITLE_FONT_SIZE = "26";
    public static final boolean DEFAULT_DISPLAY_RINGTONE_TITLE = false;
    public static int getDefaultAlarmSecondsHandColor(Context context) {
        return MaterialColors.getColor(context, android.R.attr.colorPrimary, Color.BLACK);
    }

    // Timer
    public static final String DEFAULT_TIMER_AUTO_SILENCE = "30";
    public static final String DEFAULT_TIMER_CRESCENDO_DURATION = "0";
    public static final boolean DEFAULT_TIMER_VIBRATE = false;
    public static final boolean DEFAULT_TIMER_VOLUME_BUTTONS_ACTION = false;
    public static final boolean DEFAULT_TIMER_POWER_BUTTON_ACTION = false;
    public static final boolean DEFAULT_TIMER_FLIP_ACTION = false;
    public static final boolean DEFAULT_TIMER_SHAKE_ACTION = false;
    public static final String DEFAULT_SORT_TIMER_MANUALLY = "0";
    public static final String SORT_TIMER_BY_ASCENDING_DURATION = "1";
    public static final String SORT_TIMER_BY_DESCENDING_DURATION = "2";
    public static final String SORT_TIMER_BY_NAME = "3";
    public static final String DEFAULT_TIME_TO_ADD_TO_TIMER = "1";
    public static final boolean DEFAULT_KEEP_TIMER_SCREEN_ON = true;
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
    public static final int DEFAULT_SCREENSAVER_CUSTOM_COLOR = Color.parseColor("#FFFFFF");
    public static final int DEFAULT_SCREENSAVER_BRIGHTNESS = 40;
    public static final boolean DEFAULT_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD = false;
    public static final boolean DEFAULT_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC = false;
    public static final boolean DEFAULT_SCREENSAVER_DATE_IN_BOLD = true;
    public static final boolean DEFAULT_SCREENSAVER_DATE_IN_ITALIC = false;
    public static final boolean DEFAULT_SCREENSAVER_NEXT_ALARM_IN_BOLD = true;
    public static final boolean DEFAULT_SCREENSAVER_NEXT_ALARM_IN_ITALIC = false;

    // **************
    // ** WIDGETS **
    // **************

    // Analog Widget
    public static boolean DEFAULT_ANALOG_WIDGET_WITH_SECOND_HAND = false;

    // DigitalWidgetSettingsFragment
    public static final boolean DEFAULT_DIGITAL_WIDGET_DISPLAY_SECONDS = false;
    public static final boolean DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND = false;
    public static final boolean DEFAULT_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED = true;
    public static final String DEFAULT_DIGITAL_WIDGET_FONT_SIZE = "80";

    // NextAlarmWidgetSettingsFragment
    public static final boolean DEFAULT_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND = false;

    // Vertical Digital Widget
    public static final boolean DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND = false;

    // Material You Analog Widget
    public static boolean DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_WITH_SECOND_HAND = false;

    // Material You Digital Widget
    public static final boolean DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_SECONDS = false;
    public static final boolean DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED = true;
    public static final String DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_FONT_SIZE = "80";

    // Common widget values
    public static final boolean DEFAULT_WIDGETS_DEFAULT_COLOR = true;
    public static final int DEFAULT_WIDGETS_BACKGROUND_COLOR = Color.parseColor("#70000000");
    public static final int DEFAULT_WIDGETS_CUSTOM_COLOR = Color.parseColor("#FFFFFF");
    public static final String DEFAULT_WIDGETS_FONT_SIZE = "70";

}
