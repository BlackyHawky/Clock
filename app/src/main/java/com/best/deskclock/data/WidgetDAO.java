/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.SharedPreferences;

/**
 * This class encapsulates the transfer of data between widget objects and their permanent storage
 * in {@link SharedPreferences}.
 */
public final class WidgetDAO {

    /**
     * Suffix for a key to a preference that stores the instance count for a given widget type.
     */
    private static final String WIDGET_COUNT = "_widget_count";

    /**
     * @param widgetProviderClass indicates the type of widget being counted
     * @param count               the number of widgets of the given type
     * @return the delta between the new count and the old count
     */
    public static int updateWidgetCount(SharedPreferences prefs, Class<?> widgetProviderClass, int count) {
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
     * @return {@code true} if the seconds are displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean areSecondsDisplayedOnDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_SECONDS, DEFAULT_DIGITAL_WIDGET_DISPLAY_SECONDS);
    }

    /**
     * @return {@code true} if the AM/PM part is hidden on the digital widget; {@code false} otherwise.
     */
    public static boolean isAmPmHiddenOnDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_HIDE_AM_PM, DEFAULT_DIGITAL_WIDGET_HIDE_AM_PM);
    }

    /**
     * @return {@code true} if the background is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return a value indicating the background color in the digital widget .
     */
    public static int getDigitalWidgetBackgroundColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the cities are displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean areWorldCitiesDisplayedOnDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, DEFAULT_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
    }

    /**
     * @return the font size applied to the clock in the digital widget.
     */
    public static int getDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    /**
     * @return {@code true} if the default color is applied to the clock in the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultClockColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the clock in the digital widget.
     */
    public static int getDigitalWidgetCustomClockColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the date is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_DATE, DEFAULT_DIGITAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the default color is applied to the date in the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date in the digital widget.
     */
    public static int getDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM, DEFAULT_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm in the digital widget.
     */
    public static int getDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city clock in the digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultCityClockColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city clock in the digital widget.
     */
    public static int getDigitalWidgetCustomCityClockColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city name in the digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultCityNameColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city name in the digital widget.
     */
    public static int getDigitalWidgetCustomCityNameColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the digital widget.
     * {@code false} otherwise.
     */
    public static boolean isDigitalWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING,
                DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    // *****************************
    // ** VERTICAL DIGITAL WIDGET **
    // *****************************

    /**
     * @return {@code true} if the background is displayed on the vertical digital widget; {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnVerticalDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND, DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return a value indicating the background color in the vertical digital widget .
     */
    public static int getVerticalDigitalWidgetBackgroundColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return the font size applied to the hours in the vertical digital widget.
     */
    public static int getVerticalDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    /**
     * @return {@code true} if the default color is applied to the hours in the vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isVerticalDigitalWidgetDefaultHoursColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the hours in the vertical digital widget.
     */
    public static int getVerticalDigitalWidgetCustomHoursColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the minutes in the vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isVerticalDigitalWidgetDefaultMinutesColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the minutes in the vertical digital widget.
     */
    public static int getVerticalDigitalWidgetCustomMinutesColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the date is displayed on the vertical digital widget; {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnVerticalDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE, DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the default color is applied to the date in the vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isVerticalDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date in the vertical digital widget.
     */
    public static int getVerticalDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the vertical digital widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnVerticalDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM,
                DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isVerticalDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm in the vertical digital widget.
     */
    public static int getVerticalDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the digital widget.
     * {@code false} otherwise.
     */
    public static boolean isVerticalDigitalWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING,
                DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    // ***********************
    // ** NEXT ALARM WIDGET **
    // ***********************

    /**
     * @return {@code true} if the background is displayed on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnNextAlarmWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND, DEFAULT_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return a value indicating the background color in the Next alarm widget .
     */
    public static int getNextAlarmWidgetBackgroundColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return the font size applied to the Next alarm widget.
     */
    public static int getNextAlarmWidgetMaxFontSize(SharedPreferences prefs) {
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_MAXIMUM_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    /**
     * @return {@code true} if the default color is applied to the title in the Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultTitleColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the title in the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomTitleColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm title in the Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultAlarmTitleColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm title in the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomAlarmTitleColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm in the Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm in the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    // *********************************
    // ** MATERIAL YOU DIGITAL WIDGET **
    // *********************************

    /**
     * @return {@code true} if the seconds are displayed on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean areSecondsDisplayedOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_SECONDS_DISPLAYED,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_SECONDS);
    }

    /**
     * @return {@code true} if the AM/PM part is hidden on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isAmPmHiddenOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_HIDE_AM_PM,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_HIDE_AM_PM);
    }

    /**
     * @return {@code true} if the cities are displayed on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean areWorldCitiesDisplayedOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
    }

    /**
     * @return the font size applied to the clock in the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    /**
     * @return {@code true} if the default color is applied to the digital clock in the Material You widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultClockColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the clock in the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetCustomClockColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the date is displayed on the Material You digital widget; {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_DATE, DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the default color is applied to the date in the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date in the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the Material You digital widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm in the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city clock in the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultCityClockColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city clock in the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetCustomCityClockColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city name in the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultCityNameColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city name in the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetCustomCityNameColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the Material You digital widget.
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING,
                DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    // ******************************************
    // ** MATERIAL YOU VERTICAL DIGITAL WIDGET **
    // ******************************************

    /**
     * @return the font size applied to the hours in the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    /**
     * @return {@code true} if the default color is applied to the hours in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouVerticalDigitalWidgetDefaultHoursColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the hours in the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetCustomHoursColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the minutes in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouVerticalDigitalWidgetDefaultMinutesColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the minutes in the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetCustomMinutesColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the date is displayed on the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnMaterialYouVerticalDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE,
                DEFAULT_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the default color is applied to the date in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouVerticalDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date in the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnMaterialYouVerticalDigitalWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM,
                DEFAULT_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm in the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    // ************************************
    // ** MATERIAL YOU NEXT ALARM WIDGET **
    // ************************************

    /**
     * @return the font size applied to the Material You Next alarm widget.
     */
    public static int getMaterialYouNextAlarmWidgetMaxFontSize(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAXIMUM_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    /**
     * @return {@code true} if the default color is applied to the title in the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouNextAlarmWidgetDefaultTitleColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the title in the Material You Next alarm widget.
     */
    public static int getMaterialYouNextAlarmWidgetCustomTitleColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm title in the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm title in the Material You Next alarm widget.
     */
    public static int getMaterialYouNextAlarmWidgetCustomAlarmTitleColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm in the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouNextAlarmWidgetDefaultAlarmColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm in the Material You Next alarm widget.
     */
    public static int getMaterialYouNextAlarmWidgetCustomAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

}
