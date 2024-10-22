/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

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
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DEFAULT_FONT_SIZE;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_FONT_SIZE;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.MaterialYouDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_FONT_SIZE;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR;
import static com.best.deskclock.settings.MaterialYouNextAlarmWidgetCustomizationActivity.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR;
import static com.best.deskclock.settings.MaterialYouVerticalDigitalWidgetCustomizationActivity.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_FONT_SIZE;
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
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_DEFAULT_FONT_SIZE;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.NextAlarmWidgetCustomizationActivity.KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_FONT_SIZE;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.VerticalDigitalWidgetCustomizationActivity.KEY_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE;

import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * This class encapsulates the transfer of data between widget objects and their permanent storage
 * in {@link SharedPreferences}.
 */
final class WidgetDAO {

    /**
     * Suffix for a key to a preference that stores the instance count for a given widget type.
     */
    private static final String WIDGET_COUNT = "_widget_count";

    private WidgetDAO() {
    }

    /**
     * @param widgetProviderClass indicates the type of widget being counted
     * @param count               the number of widgets of the given type
     * @return the delta between the new count and the old count
     */
    static int updateWidgetCount(SharedPreferences prefs, Class<?> widgetProviderClass, int count) {
        final String key = widgetProviderClass.getSimpleName() + WIDGET_COUNT;
        final int oldCount = prefs.getInt(key, 0);
        if (count == 0) {
            prefs.edit().remove(key).apply();
        } else {
            prefs.edit().putInt(key, count).apply();
        }
        return count - oldCount;
    }

    // ********************
    // ** DIGITAL WIDGET **
    // ********************

    /**
     * @return {@code true} if the background is displayed on the digital widget; {@code false} otherwise.
     */
    static boolean isBackgroundDisplayedOnDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, false);
    }

    /**
     * @return a value indicating the background color in the digital widget .
     */
    static int getDigitalWidgetBackgroundColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_BACKGROUND_COLOR, Color.parseColor("#70000000"));
    }

    /**
     * @return {@code true} if the cities are displayed on the digital widget; {@code false} otherwise.
     */
    static boolean areWorldCitiesDisplayedOnDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, true);
    }

    /**
     * @return the font size applied to the clock in the digital widget.
     */
    static String getDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        return prefs.getString(KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, KEY_DIGITAL_WIDGET_DEFAULT_FONT_SIZE);
    }

    /**
     * @return {@code true} if the default color is applied to the clock in the digital widget; {@code false} otherwise.
     */
    static boolean isDigitalWidgetDefaultClockColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, true);
    }

    /**
     * @return a value indicating the color of the clock in the digital widget.
     */
    static int getDigitalWidgetCustomClockColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the date in the digital widget; {@code false} otherwise.
     */
    static boolean isDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, true);
    }

    /**
     * @return a value indicating the color of the date in the digital widget.
     */
    static int getDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the digital widget;
     * {@code false} otherwise.
     */
    static boolean isDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, true);
    }

    /**
     * @return a value indicating the color of the next alarm in the digital widget.
     */
    static int getDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the city clock in the digital widget;
     * {@code false} otherwise.
     */
    static boolean isDigitalWidgetDefaultCityClockColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, true);
    }

    /**
     * @return a value indicating the color of the city clock in the digital widget.
     */
    static int getDigitalWidgetCustomCityClockColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the city name in the digital widget;
     * {@code false} otherwise.
     */
    static boolean isDigitalWidgetDefaultCityNameColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, true);
    }

    /**
     * @return a value indicating the color of the city name in the digital widget.
     */
    static int getDigitalWidgetCustomCityNameColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, Color.parseColor("#FFFFFF"));
    }

    // *****************************
    // ** VERTICAL DIGITAL WIDGET **
    // *****************************

    /**
     * @return {@code true} if the background is displayed on the vertical digital widget; {@code false} otherwise.
     */
    static boolean isBackgroundDisplayedOnVerticalDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND, false);
    }

    /**
     * @return a value indicating the background color in the vertical digital widget .
     */
    static int getVerticalDigitalWidgetBackgroundColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR, Color.parseColor("#70000000"));
    }

    /**
     * @return the font size applied to the hours in the vertical digital widget.
     */
    static String getVerticalDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        return prefs.getString(KEY_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE,
                KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_FONT_SIZE
        );
    }

    /**
     * @return {@code true} if the default color is applied to the hours in the vertical digital widget;
     * {@code false} otherwise.
     */
    static boolean isVerticalDigitalWidgetDefaultHoursColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, true);
    }

    /**
     * @return a value indicating the color of the hours in the vertical digital widget.
     */
    static int getVerticalDigitalWidgetCustomHoursColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the minutes in the vertical digital widget;
     * {@code false} otherwise.
     */
    static boolean isVerticalDigitalWidgetDefaultMinutesColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, true);
    }

    /**
     * @return a value indicating the color of the minutes in the vertical digital widget.
     */
    static int getVerticalDigitalWidgetCustomMinutesColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the date in the vertical digital widget;
     * {@code false} otherwise.
     */
    static boolean isVerticalDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the date in the vertical digital widget.
     */
    static int getVerticalDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the vertical digital widget;
     * {@code false} otherwise.
     */
    static boolean isVerticalDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, true);
    }

    /**
     * @return a value indicating the color of the next alarm in the vertical digital widget.
     */
    static int getVerticalDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, Color.parseColor("#FFFFFF"));
    }

    // ***********************
    // ** NEXT ALARM WIDGET **
    // ***********************

    /**
     * @return {@code true} if the background is displayed on the Next alarm widget; {@code false} otherwise.
     */
    static boolean isBackgroundDisplayedOnNextAlarmWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND, false);
    }

    /**
     * @return a value indicating the background color in the Next alarm widget .
     */
    static int getNextAlarmWidgetBackgroundColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR, Color.parseColor("#70000000"));
    }

    /**
     * @return the font size applied to the Next alarm widget.
     */
    static String getNextAlarmWidgetMaxFontSize(SharedPreferences prefs) {
        return prefs.getString(KEY_NEXT_ALARM_WIDGET_MAX_FONT_SIZE, KEY_NEXT_ALARM_WIDGET_DEFAULT_FONT_SIZE
        );
    }

    /**
     * @return {@code true} if the default color is applied to the title in the Next alarm widget;
     * {@code false} otherwise.
     */
    static boolean isNextAlarmWidgetDefaultTitleColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, true);
    }

    /**
     * @return a value indicating the color of the title in the Next alarm widget.
     */
    static int getNextAlarmWidgetCustomTitleColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the alarm title in the Next alarm widget;
     * {@code false} otherwise.
     */
    static boolean isNextAlarmWidgetDefaultAlarmTitleColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, true);
    }

    /**
     * @return a value indicating the color of the alarm title in the Next alarm widget.
     */
    static int getNextAlarmWidgetCustomAlarmTitleColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the alarm in the Next alarm widget;
     * {@code false} otherwise.
     */
    static boolean isNextAlarmWidgetDefaultAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, true);
    }

    /**
     * @return a value indicating the color of the alarm in the Next alarm widget.
     */
    static int getNextAlarmWidgetCustomAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, Color.parseColor("#FFFFFF"));
    }

    // *********************************
    // ** MATERIAL YOU DIGITAL WIDGET **
    // *********************************

    /**
     * @return {@code true} if the cities are displayed on the Material You digital widget;
     * {@code false} otherwise.
     */
    static boolean areWorldCitiesDisplayedOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, true);
    }

    /**
     * @return the font size applied to the clock in the Material You digital widget.
     */
    static String getMaterialYouDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        return prefs.getString(KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE,
                KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_FONT_SIZE
        );
    }

    /**
     * @return {@code true} if the default color is applied to the digital clock in the Material You widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouDigitalWidgetDefaultClockColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, true);
    }

    /**
     * @return a value indicating the color of the clock in the Material You digital widget.
     */
    static int getMaterialYouDigitalWidgetCustomClockColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the date in the Material You digital widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, true);
    }

    /**
     * @return a value indicating the color of the date in the Material You digital widget.
     */
    static int getMaterialYouDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the Material You digital widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, true);
    }

    /**
     * @return a value indicating the color of the next alarm in the Material You digital widget.
     */
    static int getMaterialYouDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the city clock in the Material You digital widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouDigitalWidgetDefaultCityClockColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, true);
    }

    /**
     * @return a value indicating the color of the city clock in the Material You digital widget.
     */
    static int getMaterialYouDigitalWidgetCustomCityClockColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the city name in the Material You digital widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouDigitalWidgetDefaultCityNameColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, true);
    }

    /**
     * @return a value indicating the color of the city name in the Material You digital widget.
     */
    static int getMaterialYouDigitalWidgetCustomCityNameColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    // ******************************************
    // ** MATERIAL YOU VERTICAL DIGITAL WIDGET **
    // ******************************************

    /**
     * @return the font size applied to the hours in the Material You vertical digital widget.
     */
    static String getMaterialYouVerticalDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        return prefs.getString(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE,
                KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_FONT_SIZE
        );
    }

    /**
     * @return {@code true} if the default color is applied to the hours in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouVerticalDigitalWidgetDefaultHoursColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, true);
    }

    /**
     * @return a value indicating the color of the hours in the Material You vertical digital widget.
     */
    static int getMaterialYouVerticalDigitalWidgetCustomHoursColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the minutes in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouVerticalDigitalWidgetDefaultMinutesColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, true);
    }

    /**
     * @return a value indicating the color of the minutes in the Material You vertical digital widget.
     */
    static int getMaterialYouVerticalDigitalWidgetCustomMinutesColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the date in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouVerticalDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, true);
    }

    /**
     * @return a value indicating the color of the date in the Material You vertical digital widget.
     */
    static int getMaterialYouVerticalDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, true);
    }

    /**
     * @return a value indicating the color of the next alarm in the Material You vertical digital widget.
     */
    static int getMaterialYouVerticalDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    // ************************************
    // ** MATERIAL YOU NEXT ALARM WIDGET **
    // ************************************

    /**
     * @return the font size applied to the Material You Next alarm widget.
     */
    static String getMaterialYouNextAlarmWidgetMaxFontSize(SharedPreferences prefs) {
        return prefs.getString(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAX_FONT_SIZE,
                KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_FONT_SIZE
        );
    }

    /**
     * @return {@code true} if the default color is applied to the title in the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouNextAlarmWidgetDefaultTitleColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, true);
    }

    /**
     * @return a value indicating the color of the title in the Material You Next alarm widget.
     */
    static int getMaterialYouNextAlarmWidgetCustomTitleColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the alarm title in the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, true);
    }

    /**
     * @return a value indicating the color of the alarm title in the Material You Next alarm widget.
     */
    static int getMaterialYouNextAlarmWidgetCustomAlarmTitleColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the alarm in the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouNextAlarmWidgetDefaultAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, true);
    }

    /**
     * @return a value indicating the color of the alarm in the Material You Next alarm widget.
     */
    static int getMaterialYouNextAlarmWidgetCustomAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

}
