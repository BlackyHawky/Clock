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

    // *******************
    // ** ANALOG WIDGET **
    // *******************

    /**
     * @return the dial applied to the clock on the analog widget.
     */
    public static String getAnalogWidgetClockDial(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getString(KEY_ANALOG_WIDGET_CLOCK_DIAL, DEFAULT_ANALOG_WIDGET_CLOCK_DIAL);
    }

    /**
     * @return {@code true} if the second hand is displayed on the analog widget; {@code false} otherwise.
     */
    public static boolean isSecondHandDisplayedOnAnalogWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_WITH_SECOND_HAND, DEFAULT_ANALOG_WIDGET_WITH_SECOND_HAND);
    }

    /**
     * @return the clock second hand displayed on the analog widget.
     */
    public static String getAnalogWidgetClockSecondHand(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getString(KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND, DEFAULT_CLOCK_SECOND_HAND);
    }

    /**
     * @return {@code true} if the default color is applied to the dial on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultDialColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_DIAL_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the dial color on the analog widget.
     */
    public static int getAnalogWidgetDialColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_DIAL_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the hour hand on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultHourHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_HOUR_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the hour hand color on the analog widget.
     */
    public static int getAnalogWidgetHourHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_HOUR_HAND_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the minute hand on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultMinuteHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_MINUTE_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the minute hand color on the analog widget.
     */
    public static int getAnalogWidgetMinuteHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_MINUTE_HAND_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the second hand on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultSecondHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_SECOND_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the second hand color on the analog widget.
     */
    public static int getAnalogWidgetSecondHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_SECOND_HAND_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    // ********************
    // ** DIGITAL WIDGET **
    // ********************

    /**
     * @return {@code true} if the seconds are displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean areSecondsDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_SECONDS, DEFAULT_DIGITAL_WIDGET_DISPLAY_SECONDS);
    }

    /**
     * @return {@code true} if the AM/PM part is hidden on the digital widget; {@code false} otherwise.
     */
    public static boolean isAmPmHiddenOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_HIDE_AM_PM, DEFAULT_DIGITAL_WIDGET_HIDE_AM_PM);
    }

    /**
     * @return {@code true} if the background is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if the date is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_DATE, DEFAULT_DIGITAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM, DEFAULT_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if the cities are displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean areWorldCitiesDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, DEFAULT_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDigitalWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING,
                DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return a value indicating the background color on the digital widget .
     */
    public static int getDigitalWidgetBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the clock on the digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the clock on the digital widget.
     */
    public static int getDigitalWidgetCustomClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the date on the digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date on the digital widget.
     */
    public static int getDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm on the digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm on the digital widget.
     */
    public static int getDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city clock on the digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultCityClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city clock on the digital widget.
     */
    public static int getDigitalWidgetCustomCityClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city name on the digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultCityNameColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city name on the digital widget.
     */
    public static int getDigitalWidgetCustomCityNameColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the clock in the digital widget.
     */
    public static int getDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    // *****************************
    // ** VERTICAL DIGITAL WIDGET **
    // *****************************

    /**
     * @return {@code true} if the background is displayed on the vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnVerticalDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND, DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if the date is displayed on the vertical digital widget; {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnVerticalDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE, DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnVerticalDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM,
                DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the vertical digital widget.
     * {@code false} otherwise.
     */
    public static boolean isVerticalDigitalWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING,
                DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return a value indicating the background color on the vertical digital widget .
     */
    public static int getVerticalDigitalWidgetBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the hours on the vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isVerticalDigitalWidgetDefaultHoursColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the hours on the vertical digital widget.
     */
    public static int getVerticalDigitalWidgetCustomHoursColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the minutes on the vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isVerticalDigitalWidgetDefaultMinutesColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the minutes on the vertical digital widget.
     */
    public static int getVerticalDigitalWidgetCustomMinutesColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the date on the vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isVerticalDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date on the vertical digital widget.
     */
    public static int getVerticalDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm on the vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isVerticalDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm on the vertical digital widget.
     */
    public static int getVerticalDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the hours on the vertical digital widget.
     */
    public static int getVerticalDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_digital_widget.xml
        return prefs.getInt(KEY_VERTICAL_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    // ***********************
    // ** NEXT ALARM WIDGET **
    // ***********************

    /**
     * @return {@code true} if the background is displayed on the Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnNextAlarmWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND, DEFAULT_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the Next alarm widget.
     * {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING,
                DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return a value indicating the background color on the Next alarm widget .
     */
    public static int getNextAlarmWidgetBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the title àn the Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the title on the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm title on the Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultAlarmTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm title on the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomAlarmTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm on the Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm on the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the Next alarm widget.
     */
    public static int getNextAlarmWidgetMaxFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_MAXIMUM_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    // ********************************
    // ** MATERIAL YOU ANALOG WIDGET **
    // ********************************

    /**
     * @return the dial applied to the clock on the Material You analog widget.
     */
    public static String getMaterialYouAnalogWidgetClockDial(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_analog_widget.xml
        return prefs.getString(KEY_MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL, DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL);
    }

    /**
     * @return {@code true} if the second hand is displayed on the Material You analog widget;
     * {@code false} otherwise.
     */
    public static boolean isSecondHandDisplayedOnMaterialYouAnalogWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_analog_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_ANALOG_WIDGET_WITH_SECOND_HAND, DEFAULT_ANALOG_WIDGET_WITH_SECOND_HAND);
    }

    /**
     * @return {@code true} if the default color is applied to the dial on the Material You analog widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouAnalogWidgetDefaultDialColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_analog_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_ANALOG_WIDGET_DEFAULT_DIAL_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the dial color on the Material You analog widget.
     */
    public static int getMaterialYouAnalogWidgetDialColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_analog_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_DIAL_COLOR,
                DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_DIAL_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the hour hand on the Material You analog widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouAnalogWidgetDefaultHourHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_analog_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_ANALOG_WIDGET_DEFAULT_HOUR_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the hour hand color on the Material You analog widget.
     */
    public static int getMaterialYouAnalogWidgetHourHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_analog_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_HOUR_HAND_COLOR,
                DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_HOUR_HAND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the minute hand on the Material You analog widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouAnalogWidgetDefaultMinuteHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_analog_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_ANALOG_WIDGET_DEFAULT_MINUTE_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the minute hand color on the Material You analog widget.
     */
    public static int getMaterialYouAnalogWidgetMinuteHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_analog_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_MINUTE_HAND_COLOR,
                DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_MINUTE_HAND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the second hand on the Material You analog widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouAnalogWidgetDefaultSecondHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_analog_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_ANALOG_WIDGET_DEFAULT_SECOND_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the second hand color on the Material You analog widget.
     */
    public static int getMaterialYouAnalogWidgetSecondHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_analog_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_SECOND_HAND_COLOR,
                DEFAULT_MATERIAL_YOU_ANALOG_WIDGET_CUSTOM_SECOND_HAND_COLOR);
    }

    // *********************************
    // ** MATERIAL YOU DIGITAL WIDGET **
    // *********************************

    /**
     * @return {@code true} if the seconds are displayed on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean areSecondsDisplayedOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_SECONDS_DISPLAYED,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_SECONDS);
    }

    /**
     * @return {@code true} if the AM/PM part is hidden on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isAmPmHiddenOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_HIDE_AM_PM,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_HIDE_AM_PM);
    }

    /**
     * @return {@code true} if the background is displayed on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_BACKGROUND,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if the date is displayed on the Material You digital widget; {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_DATE, DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the Material You digital widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if the cities are displayed on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean areWorldCitiesDisplayedOnMaterialYouDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the Material You digital widget.
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING,
                DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return {@code true} if the default color is applied to the background on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the background color on the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the digital clock on the Material You widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the clock on the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetCustomClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the date on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date on the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm on the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city clock on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultCityClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city clock on the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetCustomCityClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city name on the Material You digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouDigitalWidgetDefaultCityNameColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city name on the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetCustomCityNameColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the clock on the Material You digital widget.
     */
    public static int getMaterialYouDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    // ******************************************
    // ** MATERIAL YOU VERTICAL DIGITAL WIDGET **
    // ******************************************

    /**
     * @return {@code true} if the background is displayed on the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnMaterialYouVerticalDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if the date is displayed on the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnMaterialYouVerticalDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE,
                DEFAULT_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnMaterialYouVerticalDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM,
                DEFAULT_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the Material You vertical digital widget.
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouVerticalDigitalWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING,
                DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return {@code true} if the default color is applied to the background on the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouVerticalDigitalWidgetDefaultBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the background color on the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the hours on the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouVerticalDigitalWidgetDefaultHoursColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the hours on the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetCustomHoursColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the minutes on the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouVerticalDigitalWidgetDefaultMinutesColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the minutes on the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetCustomMinutesColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the date on the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouVerticalDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date on the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm on the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm on the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the hours on the Material You vertical digital widget.
     */
    public static int getMaterialYouVerticalDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_vertical_digital_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    // ************************************
    // ** MATERIAL YOU NEXT ALARM WIDGET **
    // ************************************

    /**
     * @return {@code true} if the background is displayed on the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnMaterialYouNextAlarmWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND,
                DEFAULT_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the Material You Next alarm widget.
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouNextAlarmWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING,
                DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return {@code true} if the default color is applied to the background on the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouNextAlarmWidgetDefaultBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the background color on the Material You Next alarm widget.
     */
    public static int getMaterialYouNextAlarmWidgetBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the title on the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouNextAlarmWidgetDefaultTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the title on the Material You Next alarm widget.
     */
    public static int getMaterialYouNextAlarmWidgetCustomTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm title on the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm title on the Material You Next alarm widget.
     */
    public static int getMaterialYouNextAlarmWidgetCustomAlarmTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm on the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public static boolean isMaterialYouNextAlarmWidgetDefaultAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getBoolean(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm on the Material You Next alarm widget.
     */
    public static int getMaterialYouNextAlarmWidgetCustomAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the Material You Next alarm widget.
     */
    public static int getMaterialYouNextAlarmWidgetMaxFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_material_you_next_alarm_widget.xml
        return prefs.getInt(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAXIMUM_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

}
