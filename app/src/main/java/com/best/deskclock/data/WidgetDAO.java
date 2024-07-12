/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.DEFAULT_DIGITAL_WIDGET_FONT_SIZE;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_BACKGROUND_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CITY_CLOCK_CUSTOM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CITY_CLOCK_DEFAULT_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CITY_NAME_CUSTOM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CITY_NAME_DEFAULT_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CLOCK_CUSTOM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_CLOCK_DEFAULT_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DATE_CUSTOM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DATE_DEFAULT_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_NEXT_ALARM_CUSTOM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_NEXT_ALARM_DEFAULT_COLOR;
import static com.best.deskclock.settings.DigitalWidgetCustomizationActivity.KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.DEFAULT_DIGITAL_WIDGET_MATERIAL_YOU_FONT_SIZE;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_CITY_CLOCK_CUSTOM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_CITY_CLOCK_DEFAULT_COLOR;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_CITY_NAME_CUSTOM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_CITY_NAME_DEFAULT_COLOR;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_CLOCK_CUSTOM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_CLOCK_DEFAULT_COLOR;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_DATE_CUSTOM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_DATE_DEFAULT_COLOR;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_MAX_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_NEXT_ALARM_CUSTOM_COLOR;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_NEXT_ALARM_DEFAULT_COLOR;
import static com.best.deskclock.settings.DigitalWidgetMaterialYouCustomizationActivity.KEY_DIGITAL_WIDGET_MATERIAL_YOU_WORLD_CITIES_DISPLAYED;

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
     * @return a value indicating the color of the the digital widget background.
     */
    static int getDigitalWidgetBackgroundColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_BACKGROUND_COLOR, Color.parseColor("#70000000"));
    }

    /**
     * @return {@code true} if the cities are displayed on the widget; {@code false} otherwise.
     */
    static boolean areWorldCitiesDisplayedOnWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, true);
    }

    /**
     * @return the font size applied to the digital clock widget.
     */
    static String getDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        return prefs.getString(KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE, DEFAULT_DIGITAL_WIDGET_FONT_SIZE);
    }

    /**
     * @return {@code true} if the default color is applied to the digital widget clock; {@code false} otherwise.
     */
    static boolean isDigitalWidgetClockDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_CLOCK_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the the digital widget clock
     */
    static int getDigitalWidgetClockCustomColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CLOCK_CUSTOM_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the digital widget date; {@code false} otherwise.
     */
    static boolean isDigitalWidgetDateDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DATE_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the the digital widget date.
     */
    static int getDigitalWidgetDateCustomColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_DATE_CUSTOM_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the digital widget next alarm;
     * {@code false} otherwise.
     */
    static boolean isDigitalWidgetNextAlarmDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_NEXT_ALARM_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the the digital widget next alarm.
     */
    static int getDigitalWidgetNextAlarmCustomColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_NEXT_ALARM_CUSTOM_COLOR, Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the digital widget city clock;
     * {@code false} otherwise.
     */
    static boolean isDigitalWidgetCityClockDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_CITY_CLOCK_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the the digital widget city clock.
     */
    static int getDigitalWidgetCityClockCustomColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CITY_CLOCK_CUSTOM_COLOR, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code true} if the default color is applied to the digital widget city name;
     * {@code false} otherwise.
     */
    static boolean isDigitalWidgetCityNameDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_CITY_NAME_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the the digital widget city name.
     */
    static int getDigitalWidgetCityNameCustomColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_CITY_NAME_CUSTOM_COLOR, Color.parseColor("#FFFFFF"));
    }

    // *********************************
    // ** MATERIAL YOU DIGITAL WIDGET **
    // *********************************

    /**
     * @return {@code true} if the cities are displayed on the Material You digital widget;
     * {@code false} otherwise.
     */
    static boolean areWorldCitiesDisplayedOnMaterialYouWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_MATERIAL_YOU_WORLD_CITIES_DISPLAYED, true);
    }

    /**
     * @return the font size applied to the Material You digital clock widget.
     */
    static String getMaterialYouDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        return prefs.getString(KEY_DIGITAL_WIDGET_MATERIAL_YOU_MAX_CLOCK_FONT_SIZE,
                DEFAULT_DIGITAL_WIDGET_MATERIAL_YOU_FONT_SIZE
        );
    }

    /**
     * @return {@code true} if the default color is applied to the Material You digital widget clock;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouDigitalWidgetClockDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_MATERIAL_YOU_CLOCK_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the the Material You digital widget clock
     */
    static int getMaterialYouDigitalWidgetClockCustomColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_MATERIAL_YOU_CLOCK_CUSTOM_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the Material You digital widget date;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouDigitalWidgetDateDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_MATERIAL_YOU_DATE_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the the Material You digital widget date.
     */
    static int getMaterialYouDigitalWidgetDateCustomColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_MATERIAL_YOU_DATE_CUSTOM_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the Material You digital widget next alarm;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouDigitalWidgetNextAlarmDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_MATERIAL_YOU_NEXT_ALARM_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the the Material You digital widget next alarm.
     */
    static int getMaterialYouDigitalWidgetNextAlarmCustomColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_MATERIAL_YOU_NEXT_ALARM_CUSTOM_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the Material You digital widget city clock;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouDigitalWidgetCityClockDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_MATERIAL_YOU_CITY_CLOCK_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the the Material You digital widget city clock.
     */
    static int getMaterialYouDigitalWidgetCityClockCustomColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_MATERIAL_YOU_CITY_CLOCK_CUSTOM_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }

    /**
     * @return {@code true} if the default color is applied to the Material You digital widget city name;
     * {@code false} otherwise.
     */
    static boolean isMaterialYouDigitalWidgetCityNameDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_MATERIAL_YOU_CITY_NAME_DEFAULT_COLOR, true);
    }

    /**
     * @return a value indicating the color of the Material You digital widget city name.
     */
    static int getMaterialYouDigitalWidgetCityNameCustomColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_DIGITAL_WIDGET_MATERIAL_YOU_CITY_NAME_CUSTOM_COLOR,
                Color.parseColor("#FFFFFF")
        );
    }
}
