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

    // For all widgets
    public static final String ACTION_NEXT_ALARM_LABEL_CHANGED = "com.best.alarmclock.NEXT_ALARM_LABEL_CHANGED";
    public static final String ACTION_UPDATE_WIDGETS_AFTER_RESTORE = "com.best.alarmclock.UPDATE_WIDGETS_AFTER_RESTORE";

    // For digital and Material You digital widgets
    public static final String ACTION_WORLD_CITIES_CHANGED = "com.best.alarmclock.WORLD_CITIES_CHANGED";

    // For standard widgets
    public static final String ACTION_DIGITAL_WIDGET_CUSTOMIZED = "com.best.alarmclock.DIGITAL_WIDGET_CUSTOMIZED";
    public static final String ACTION_NEXT_ALARM_WIDGET_CUSTOMIZED = "com.best.alarmclock.NEXT_ALARM_WIDGET_CUSTOMIZED";
    public static final String ACTION_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED = "com.best.alarmclock.VERTICAL_DIGITAL_WIDGET_CUSTOMIZED";

    // For Material You widgets
    public static final String ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED = "com.best.alarmclock.MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED";
    public static final String ACTION_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZED = "com.best.alarmclock.MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZED";
    public static final String ACTION_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED = "com.best.alarmclock.MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED";


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

    public boolean isBackgroundDisplayedOnDigitalWidget() {
        return WidgetDAO.isBackgroundDisplayedOnDigitalWidget(mPrefs);
    }

    public int getDigitalWidgetBackgroundColor() {
        return WidgetDAO.getDigitalWidgetBackgroundColor(mPrefs);
    }

    public boolean areWorldCitiesDisplayedOnDigitalWidget() {
        return WidgetDAO.areWorldCitiesDisplayedOnDigitalWidget(mPrefs);
    }

    public String getDigitalWidgetMaxClockFontSize() {
        return WidgetDAO.getDigitalWidgetMaxClockFontSize(mPrefs);
    }

    boolean isDigitalWidgetDefaultClockColor() {
        return WidgetDAO.isDigitalWidgetDefaultClockColor(mPrefs);
    }

    int getDigitalWidgetCustomClockColor() {
        return WidgetDAO.getDigitalWidgetCustomClockColor(mPrefs);
    }

    boolean isDigitalWidgetDefaultDateColor() {
        return WidgetDAO.isDigitalWidgetDefaultDateColor(mPrefs);
    }

    int getDigitalWidgetCustomDateColor() {
        return WidgetDAO.getDigitalWidgetCustomDateColor(mPrefs);
    }

    boolean isDigitalWidgetDefaultNextAlarmColor() {
        return WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(mPrefs);
    }

    int getDigitalWidgetCustomNextAlarmColor() {
        return WidgetDAO.getDigitalWidgetCustomNextAlarmColor(mPrefs);
    }

    boolean isDigitalWidgetDefaultCityClockColor() {
        return WidgetDAO.isDigitalWidgetDefaultCityClockColor(mPrefs);
    }

    int getDigitalWidgetCustomCityClockColor() {
        return WidgetDAO.getDigitalWidgetCustomCityClockColor(mPrefs);
    }

    boolean isDigitalWidgetDefaultCityNameColor() {
        return WidgetDAO.isDigitalWidgetDefaultCityNameColor(mPrefs);
    }

    int getDigitalWidgetCustomCityNameColor() {
        return WidgetDAO.getDigitalWidgetCustomCityNameColor(mPrefs);
    }

    // *****************************
    // ** VERTICAL DIGITAL WIDGET **
    // *****************************

    public boolean isBackgroundDisplayedOnVerticalDigitalWidget() {
        return WidgetDAO.isBackgroundDisplayedOnVerticalDigitalWidget(mPrefs);
    }

    public int getVerticalDigitalWidgetBackgroundColor() {
        return WidgetDAO.getVerticalDigitalWidgetBackgroundColor(mPrefs);
    }

    public String getVerticalDigitalWidgetMaxClockFontSize() {
        return WidgetDAO.getVerticalDigitalWidgetMaxClockFontSize(mPrefs);
    }

    boolean isVerticalDigitalWidgetDefaultHoursColor() {
        return WidgetDAO.isVerticalDigitalWidgetDefaultHoursColor(mPrefs);
    }

    int getVerticalDigitalWidgetCustomHoursColor() {
        return WidgetDAO.getVerticalDigitalWidgetCustomHoursColor(mPrefs);
    }

    boolean isVerticalDigitalWidgetDefaultMinutesColor() {
        return WidgetDAO.isVerticalDigitalWidgetDefaultMinutesColor(mPrefs);
    }

    int getVerticalDigitalWidgetCustomMinutesColor() {
        return WidgetDAO.getVerticalDigitalWidgetCustomMinutesColor(mPrefs);
    }

    boolean isVerticalDigitalWidgetDefaultDateColor() {
        return WidgetDAO.isVerticalDigitalWidgetDefaultDateColor(mPrefs);
    }

    int getVerticalDigitalWidgetCustomDateColor() {
        return WidgetDAO.getVerticalDigitalWidgetCustomDateColor(mPrefs);
    }

    boolean isVerticalDigitalWidgetDefaultNextAlarmColor() {
        return WidgetDAO.isVerticalDigitalWidgetDefaultNextAlarmColor(mPrefs);
    }

    int getVerticalDigitalWidgetCustomNextAlarmColor() {
        return WidgetDAO.getVerticalDigitalWidgetCustomNextAlarmColor(mPrefs);
    }

    // ***********************
    // ** NEXT ALARM WIDGET **
    // ***********************

    public boolean isBackgroundDisplayedOnNextAlarmWidget() {
        return WidgetDAO.isBackgroundDisplayedOnNextAlarmWidget(mPrefs);
    }

    public int getNextAlarmWidgetBackgroundColor() {
        return WidgetDAO.getNextAlarmWidgetBackgroundColor(mPrefs);
    }

    public String getNextAlarmWidgetMaxFontSize() {
        return WidgetDAO.getNextAlarmWidgetMaxFontSize(mPrefs);
    }

    boolean isNextAlarmWidgetDefaultTitleColor() {
        return WidgetDAO.isNextAlarmWidgetDefaultTitleColor(mPrefs);
    }

    int getNextAlarmWidgetCustomTitleColor() {
        return WidgetDAO.getNextAlarmWidgetCustomTitleColor(mPrefs);
    }

    boolean isNextAlarmWidgetDefaultAlarmTitleColor() {
        return WidgetDAO.isNextAlarmWidgetDefaultAlarmTitleColor(mPrefs);
    }

    int getNextAlarmWidgetCustomAlarmTitleColor() {
        return WidgetDAO.getNextAlarmWidgetCustomAlarmTitleColor(mPrefs);
    }

    boolean isNextAlarmWidgetDefaultAlarmColor() {
        return WidgetDAO.isNextAlarmWidgetDefaultAlarmColor(mPrefs);
    }

    int getNextAlarmWidgetCustomAlarmColor() {
        return WidgetDAO.getNextAlarmWidgetCustomAlarmColor(mPrefs);
    }

    // *********************************
    // ** MATERIAL YOU DIGITAL WIDGET **
    // *********************************

    public boolean areWorldCitiesDisplayedOnMaterialYouDigitalWidget() {
        return WidgetDAO.areWorldCitiesDisplayedOnMaterialYouDigitalWidget(mPrefs);
    }

    public String getMaterialYouDigitalWidgetMaxClockFontSize() {
        return WidgetDAO.getMaterialYouDigitalWidgetMaxClockFontSize(mPrefs);
    }

    boolean isMaterialYouDigitalWidgetDefaultClockColor() {
        return WidgetDAO.isMaterialYouDigitalWidgetDefaultClockColor(mPrefs);
    }

    int getMaterialYouDigitalWidgetCustomClockColor() {
        return WidgetDAO.getMaterialYouDigitalWidgetCustomClockColor(mPrefs);
    }

    boolean isMaterialYouDigitalWidgetDefaultDateColor() {
        return WidgetDAO.isMaterialYouDigitalWidgetDefaultDateColor(mPrefs);
    }

    int getMaterialYouDigitalWidgetCustomDateColor() {
        return WidgetDAO.getMaterialYouDigitalWidgetCustomDateColor(mPrefs);
    }

    boolean isMaterialYouDigitalWidgetDefaultNextAlarmColor() {
        return WidgetDAO.isMaterialYouDigitalWidgetDefaultNextAlarmColor(mPrefs);
    }

    int getMaterialYouDigitalWidgetCustomNextAlarmColor() {
        return WidgetDAO.getMaterialYouDigitalWidgetCustomNextAlarmColor(mPrefs);
    }

    boolean isMaterialYouDigitalWidgetDefaultCityClockColor() {
        return WidgetDAO.isMaterialYouDigitalWidgetDefaultCityClockColor(mPrefs);
    }

    int getMaterialYouDigitalWidgetCustomCityClockColor() {
        return WidgetDAO.getMaterialYouDigitalWidgetCustomCityClockColor(mPrefs);
    }

    boolean isMaterialYouDigitalWidgetDefaultCityNameColor() {
        return WidgetDAO.isMaterialYouDigitalWidgetDefaultCityNameColor(mPrefs);
    }

    int getMaterialYouDigitalWidgetCustomCityNameColor() {
        return WidgetDAO.getMaterialYouDigitalWidgetCustomCityNameColor(mPrefs);
    }

    // ******************************************
    // ** MATERIAL YOU VERTICAL DIGITAL WIDGET **
    // ******************************************

    public String getMaterialYouVerticalDigitalWidgetMaxClockFontSize() {
        return WidgetDAO.getMaterialYouVerticalDigitalWidgetMaxClockFontSize(mPrefs);
    }

    boolean isMaterialYouVerticalDigitalWidgetDefaultHoursColor() {
        return WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultHoursColor(mPrefs);
    }

    int getMaterialYouVerticalDigitalWidgetCustomHoursColor() {
        return WidgetDAO.getMaterialYouVerticalDigitalWidgetCustomHoursColor(mPrefs);
    }

    boolean isMaterialYouVerticalDigitalWidgetDefaultMinutesColor() {
        return WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultMinutesColor(mPrefs);
    }

    int getMaterialYouVerticalDigitalWidgetCustomMinutesColor() {
        return WidgetDAO.getMaterialYouVerticalDigitalWidgetCustomMinutesColor(mPrefs);
    }

    boolean isMaterialYouVerticalDigitalWidgetDefaultDateColor() {
        return WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultDateColor(mPrefs);
    }

    int getMaterialYouVerticalDigitalWidgetCustomDateColor() {
        return WidgetDAO.getMaterialYouVerticalDigitalWidgetCustomDateColor(mPrefs);
    }

    boolean isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor() {
        return WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(mPrefs);
    }

    int getMaterialYouVerticalDigitalWidgetCustomNextAlarmColor() {
        return WidgetDAO.getMaterialYouVerticalDigitalWidgetCustomNextAlarmColor(mPrefs);
    }

    // ************************************
    // ** MATERIAL YOU NEXT ALARM WIDGET **
    // ************************************

    public String getMaterialYouNextAlarmWidgetMaxFontSize() {
        return WidgetDAO.getMaterialYouNextAlarmWidgetMaxFontSize(mPrefs);
    }

    boolean isMaterialYouNextAlarmWidgetDefaultTitleColor() {
        return WidgetDAO.isMaterialYouNextAlarmWidgetDefaultTitleColor(mPrefs);
    }

    int getMaterialYouNextAlarmWidgetCustomTitleColor() {
        return WidgetDAO.getMaterialYouNextAlarmWidgetCustomTitleColor(mPrefs);
    }

    boolean isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor() {
        return WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor(mPrefs);
    }

    int getMaterialYouNextAlarmWidgetCustomAlarmTitleColor() {
        return WidgetDAO.getMaterialYouNextAlarmWidgetCustomAlarmTitleColor(mPrefs);
    }

    boolean isMaterialYouNextAlarmWidgetDefaultAlarmColor() {
        return WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmColor(mPrefs);
    }

    int getMaterialYouNextAlarmWidgetCustomAlarmColor() {
        return WidgetDAO.getMaterialYouNextAlarmWidgetCustomAlarmColor(mPrefs);
    }

}
