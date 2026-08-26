/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

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
    public static String getAnalogWidgetClockDial(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getString(KEY_ANALOG_WIDGET_CLOCK_DIAL, DEFAULT_ANALOG_WIDGET_CLOCK_DIAL);
    }

    /**
     * @return {@code true} if the second hand is displayed on the analog widget; {@code false} otherwise.
     */
    public static boolean isSecondHandDisplayedOnAnalogWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_WITH_SECOND_HAND, DEFAULT_ANALOG_WIDGET_WITH_SECOND_HAND);
    }

    /**
     * @return the clock second hand displayed on the analog widget.
     */
    public static String getAnalogWidgetClockSecondHand(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getString(KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND, DEFAULT_CLOCK_SECOND_HAND);
    }

    /**
     * @return {@code true} if the default color is applied to the dial on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultDialColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_DIAL_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the dial color on the analog widget.
     */
    public static int getAnalogWidgetDialColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_DIAL_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the hour hand on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultHourHandColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_HOUR_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the hour hand color on the analog widget.
     */
    public static int getAnalogWidgetHourHandColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_HOUR_HAND_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the minute hand on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultMinuteHandColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_MINUTE_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the minute hand color on the analog widget.
     */
    public static int getAnalogWidgetMinuteHandColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_MINUTE_HAND_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the second hand on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultSecondHandColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_SECOND_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the second hand color on the analog widget.
     */
    public static int getAnalogWidgetSecondHandColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_SECOND_HAND_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    // ********************
    // ** DIGITAL WIDGET **
    // ********************

    /**
     * @return {@code true} if the text is displayed in uppercase on the digital widget; {@code false} otherwise.
     */
    public static boolean isTextUppercaseDisplayedOnDigitalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_TEXT_UPPERCASE, DEFAULT_WIDGET_TEXT_UPPERCASE_DISPLAYED);
    }

    /**
     * @return {@code true} if the text shadow is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isTextShadowDisplayedOnDigitalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW, DEFAULT_WIDGET_TEXT_SHADOW_DISPLAYED);
    }

    /**
     * @return {@code true} if the seconds are displayed on the digital widget;
     * {@code false} otherwise.
     */
    public static boolean areSecondsDisplayedOnDigitalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_SECONDS_DISPLAYED, DEFAULT_DIGITAL_WIDGET_DISPLAY_SECONDS);
    }

    /**
     * @return {@code true} if the AM/PM part is hidden on the digital widget; {@code false} otherwise.
     */
    public static boolean isAmPmHiddenOnDigitalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_HIDE_AM_PM, DEFAULT_DIGITAL_WIDGET_HIDE_AM_PM);
    }

    /**
     * @return {@code true} if the background is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnDigitalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if the background corner radius is customizable for the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetBackgroundCornerRadiusCustomizable(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGETS_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return a value indicating the digital widget background corner radius.
     */
    public static int getDigitalWidgetBackgroundCornerRadius(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the date is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnDigitalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_DATE, DEFAULT_DIGITAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the date is displayed above the time on the digital widget; {@code false} otherwise.
     */
    public static boolean isTopDateDisplayedOnDigitalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_TOP_DATE, DEFAULT_DIGITAL_WIDGET_DISPLAY_TOP_DATE);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnDigitalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM, DEFAULT_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if the title of the next alarm is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmTitleDisplayedOnDigitalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM_TITLE, DEFAULT_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM_TITLE);
    }

    /**
     * @return {@code true} if the cities are displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean areWorldCitiesDisplayedOnDigitalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, DEFAULT_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the digital widget. {@code false} otherwise.
     */
    public static boolean isDigitalWidgetHorizontalPaddingApplied(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING, DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return {@code true} if the default color is applied to the background on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultBackgroundColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the background color on the digital widget.
     */
    public static int getDigitalWidgetBackgroundColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the digital clock on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultClockColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the clock on the digital widget.
     */
    public static int getDigitalWidgetCustomClockColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the date on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultDateColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date on the digital widget.
     */
    public static int getDigitalWidgetCustomDateColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultNextAlarmColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm on the digital widget.
     */
    public static int getDigitalWidgetCustomNextAlarmColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm title on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultNextAlarmTitleColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm title on the digital widget.
     */
    public static int getDigitalWidgetCustomNextAlarmTitleColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city clock on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultCityClockColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city clock on the digital widget.
     */
    public static int getDigitalWidgetCustomCityClockColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city name on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultCityNameColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city name on the digital widget.
     */
    public static int getDigitalWidgetCustomCityNameColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city note on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultCityNoteColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NOTE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city note on the digital widget.
     */
    public static int getDigitalWidgetCustomCityNoteColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NOTE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the clock on the digital widget.
     */
    public static int getDigitalWidgetMaxClockFontSize(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    // *********************
    // ** VERTICAL WIDGET **
    // *********************

    /**
     * @return {@code true} if the text is displayed in uppercase on the vertical widget; {@code false} otherwise.
     */
    public static boolean isTextUppercaseDisplayedOnVerticalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DISPLAY_TEXT_UPPERCASE, DEFAULT_WIDGET_TEXT_UPPERCASE_DISPLAYED);
    }

    /**
     * @return {@code true} if the text shadow is displayed on the vertical widget; {@code false} otherwise.
     */
    public static boolean isTextShadowDisplayedOnVerticalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DISPLAY_TEXT_SHADOW, DEFAULT_WIDGET_TEXT_SHADOW_DISPLAYED);
    }

    /**
     * @return {@code true} if the background is displayed on the vertical widget; {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnVerticalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DISPLAY_BACKGROUND,
            DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if the background corner radius is customizable for the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetBackgroundCornerRadiusCustomizable(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGETS_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return a value indicating the vertical widget background corner radius.
     */
    public static int getVerticalWidgetBackgroundCornerRadius(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the date is displayed on the vertical widget; {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnVerticalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DISPLAY_DATE, DEFAULT_VERTICAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the vertical widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnVerticalWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DISPLAY_NEXT_ALARM, DEFAULT_VERTICAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the vertical widget. {@code false} otherwise.
     */
    public static boolean isVerticalWidgetHorizontalPaddingApplied(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_APPLY_HORIZONTAL_PADDING, DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return {@code true} if the default color is applied to the background on the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetDefaultBackgroundColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the background color on the vertical widget.
     */
    public static int getVerticalWidgetBackgroundColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the hours on the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetDefaultHoursColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DEFAULT_HOURS_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the hours on the vertical widget.
     */
    public static int getVerticalWidgetCustomHoursColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_CUSTOM_HOURS_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the minutes on the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetDefaultMinutesColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DEFAULT_MINUTES_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the minutes on the vertical widget.
     */
    public static int getVerticalWidgetCustomMinutesColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_CUSTOM_MINUTES_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the date on the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetDefaultDateColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date on the vertical widget.
     */
    public static int getVerticalWidgetCustomDateColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm on the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetDefaultNextAlarmColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm on the vertical widget.
     */
    public static int getVerticalWidgetCustomNextAlarmColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the hours on the vertical widget.
     */
    public static int getVerticalWidgetMaxClockFontSize(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    // ***********************
    // ** NEXT ALARM WIDGET **
    // ***********************

    /**
     * @return {@code true} if the text is displayed in uppercase on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isTextUppercaseDisplayedOnNextAlarmWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_TEXT_UPPERCASE, DEFAULT_WIDGET_TEXT_UPPERCASE_DISPLAYED);
    }

    /**
     * @return {@code true} if the text shadow is displayed on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isTextShadowDisplayedOnNextAlarmWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_TEXT_SHADOW, DEFAULT_WIDGET_TEXT_SHADOW_DISPLAYED);
    }

    /**
     * @return {@code true} if the background is displayed on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnNextAlarmWidget(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND, DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if the background corner radius is customizable for the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetBackgroundCornerRadiusCustomizable(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS,
            DEFAULT_WIDGETS_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return a value indicating the Next alarm widget background corner radius.
     */
    public static int getNextAlarmWidgetBackgroundCornerRadius(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the Next alarm widget. {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetHorizontalPaddingApplied(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING, DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return {@code true} if the default color is applied to the background on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultBackgroundColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the background color on the Next alarm widget.
     */
    public static int getNextAlarmWidgetBackgroundColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the title on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultTitleColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the title on the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomTitleColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm title on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultAlarmTitleColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm title on the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomAlarmTitleColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultAlarmColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm on the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomAlarmColor(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the Next alarm widget.
     */
    public static int getNextAlarmWidgetMaxFontSize(@NonNull SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_MAXIMUM_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

}
