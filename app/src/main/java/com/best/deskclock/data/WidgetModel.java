/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import android.content.SharedPreferences;

import androidx.annotation.StringRes;

import com.best.deskclock.R;
import com.best.deskclock.events.Events;

/**
 * All widget data is accessed via this model.
 */
public final class WidgetModel {

    public static final String ACTION_WORLD_CITIES_CHANGED =
            "com.best.alarmclock.WORLD_CITIES_CHANGED";
    public static final String ACTION_WORLD_CITIES_DISPLAYED =
            "com.best.alarmclock.WORLD_CITIES_DISPLAYED";
    public static final String ACTION_DIGITAL_WIDGET_CLOCK_COLOR_CHANGED =
            "com.best.alarmclock.DIGITAL_WIDGET_CLOCK_COLOR_CHANGED";
    public static final String ACTION_DIGITAL_WIDGET_DATE_COLOR_CHANGED =
            "com.best.alarmclock.DIGITAL_WIDGET_DATE_COLOR_CHANGED";
    public static final String ACTION_DIGITAL_WIDGET_NEXT_ALARM_COLOR_CHANGED =
            "com.best.alarmclock.DIGITAL_WIDGET_NEXT_ALARM_COLOR_CHANGED";
    public static final String ACTION_DIGITAL_WIDGET_CITY_CLOCK_COLOR_CHANGED =
            "com.best.alarmclock.DIGITAL_WIDGET_CITY_CLOCK_COLOR_CHANGED";
    public static final String ACTION_DIGITAL_WIDGET_CITY_NAME_COLOR_CHANGED =
            "com.best.alarmclock.DIGITAL_WIDGET_CITY_NAME_COLOR_CHANGED";
    public static final String ACTION_DIGITAL_WIDGET_CLOCK_FONT_SIZE_CHANGED =
            "com.best.alarmclock.DIGITAL_WIDGET_CLOCK_FONT_SIZE_CHANGED";

    private final SharedPreferences mPrefs;

    WidgetModel(SharedPreferences prefs) {
        mPrefs = prefs;
    }

    /**
     * @param widgetClass     indicates the type of widget being counted
     * @param count           the number of widgets of the given type
     * @param eventCategoryId identifies the category of event to send
     */
    void updateWidgetCount(Class<?> widgetClass, int count, @StringRes int eventCategoryId) {
        int delta = WidgetDAO.updateWidgetCount(mPrefs, widgetClass, count);
        for (; delta > 0; delta--) {
            Events.sendEvent(eventCategoryId, R.string.action_create, 0);
        }
        for (; delta < 0; delta++) {
            Events.sendEvent(eventCategoryId, R.string.action_delete, 0);
        }
    }

    // ********************
    // ** DIGITAL WIDGET **
    // ********************

    public boolean areWorldCitiesDisplayedOnWidget() {
        return WidgetDAO.areWorldCitiesDisplayedOnWidget(mPrefs);
    }

    public String getDigitalWidgetMaxClockFontSize() {
        return WidgetDAO.getDigitalWidgetMaxClockFontSize(mPrefs);
    }

    boolean isDigitalWidgetClockDefaultColor() {
        return WidgetDAO.isDigitalWidgetClockDefaultColor(mPrefs);
    }

    int getDigitalWidgetClockCustomColor() {
        return WidgetDAO.getDigitalWidgetClockCustomColor(mPrefs);
    }

    boolean isDigitalWidgetDateDefaultColor() {
        return WidgetDAO.isDigitalWidgetDateDefaultColor(mPrefs);
    }

    int getDigitalWidgetDateCustomColor() {
        return WidgetDAO.getDigitalWidgetDateCustomColor(mPrefs);
    }

    boolean isDigitalWidgetNextAlarmDefaultColor() {
        return WidgetDAO.isDigitalWidgetNextAlarmDefaultColor(mPrefs);
    }

    int getDigitalWidgetNextAlarmCustomColor() {
        return WidgetDAO.getDigitalWidgetNextAlarmCustomColor(mPrefs);
    }

    boolean isDigitalWidgetCityClockDefaultColor() {
        return WidgetDAO.isDigitalWidgetCityClockDefaultColor(mPrefs);
    }

    int getDigitalWidgetCityClockCustomColor() {
        return WidgetDAO.getDigitalWidgetCityClockCustomColor(mPrefs);
    }

    boolean isDigitalWidgetCityNameDefaultColor() {
        return WidgetDAO.isDigitalWidgetCityNameDefaultColor(mPrefs);
    }

    int getDigitalWidgetCityNameCustomColor() {
        return WidgetDAO.getDigitalWidgetCityNameCustomColor(mPrefs);
    }

    // *********************************
    // ** MATERIAL YOU DIGITAL WIDGET **
    // *********************************

    public boolean areWorldCitiesDisplayedOnMaterialYouWidget() {
        return WidgetDAO.areWorldCitiesDisplayedOnMaterialYouWidget(mPrefs);
    }

    public String getMaterialYouDigitalWidgetMaxClockFontSize() {
        return WidgetDAO.getMaterialYouDigitalWidgetMaxClockFontSize(mPrefs);
    }

    boolean isMaterialYouDigitalWidgetClockDefaultColor() {
        return WidgetDAO.isMaterialYouDigitalWidgetClockDefaultColor(mPrefs);
    }

    int getMaterialYouDigitalWidgetClockCustomColor() {
        return WidgetDAO.getMaterialYouDigitalWidgetClockCustomColor(mPrefs);
    }

    boolean isMaterialYouDigitalWidgetDateDefaultColor() {
        return WidgetDAO.isMaterialYouDigitalWidgetDateDefaultColor(mPrefs);
    }

    int getMaterialYouDigitalWidgetDateCustomColor() {
        return WidgetDAO.getMaterialYouDigitalWidgetDateCustomColor(mPrefs);
    }

    boolean isMaterialYouDigitalWidgetNextAlarmDefaultColor() {
        return WidgetDAO.isMaterialYouDigitalWidgetNextAlarmDefaultColor(mPrefs);
    }

    int getMaterialYouDigitalWidgetNextAlarmCustomColor() {
        return WidgetDAO.getMaterialYouDigitalWidgetNextAlarmCustomColor(mPrefs);
    }

    boolean isMaterialYouDigitalWidgetCityClockDefaultColor() {
        return WidgetDAO.isMaterialYouDigitalWidgetCityClockDefaultColor(mPrefs);
    }

    int getMaterialYouDigitalWidgetCityClockCustomColor() {
        return WidgetDAO.getMaterialYouDigitalWidgetCityClockCustomColor(mPrefs);
    }

    boolean isMaterialYouDigitalWidgetCityNameDefaultColor() {
        return WidgetDAO.isMaterialYouDigitalWidgetCityNameDefaultColor(mPrefs);
    }

    int getMaterialYouDigitalWidgetCityNameCustomColor() {
        return WidgetDAO.getMaterialYouDigitalWidgetCityNameCustomColor(mPrefs);
    }
}
