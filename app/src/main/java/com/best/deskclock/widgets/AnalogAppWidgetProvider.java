/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.widgets;

import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_FLOWER;
import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_SUN;
import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_WITHOUT_NUMBERS;
import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_WITH_NUMBERS;
import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_WITH_ROMAN_NUMBERS;
import static com.best.deskclock.settings.PreferencesDefaultValues.CLOCK_SECOND_HAND_LOLLIPOP;
import static com.best.deskclock.settings.PreferencesDefaultValues.CLOCK_SECOND_HAND_VINTAGE;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.SdkUtils;

/**
 * Simple widget to show an analog clock (with or without the second hand for Android12+).
 */
public class AnalogAppWidgetProvider extends BaseAnalogAppWidgetProvider {

    @Override
    protected int getLayoutId(SharedPreferences prefs) {
        if (SdkUtils.isAtLeastAndroid12()) {
            return R.layout.appwidget_analog_default;
        }

        return switch (WidgetDAO.getAnalogWidgetClockDial(prefs)) {
            case ANALOG_WIDGET_CLOCK_DIAL_WITH_NUMBERS -> R.layout.appwidget_analog_dial_with_number;
            case ANALOG_WIDGET_CLOCK_DIAL_WITHOUT_NUMBERS -> R.layout.appwidget_analog_dial_without_number;
            case ANALOG_WIDGET_CLOCK_DIAL_WITH_ROMAN_NUMBERS -> R.layout.appwidget_analog_dial_with_roman_numbers;
            case ANALOG_WIDGET_CLOCK_DIAL_SUN -> R.layout.appwidget_analog_dial_sun;
            case ANALOG_WIDGET_CLOCK_DIAL_FLOWER -> R.layout.appwidget_analog_dial_flower;
            default -> R.layout.appwidget_analog_default;
        };
    }

    @Override
    protected int getWidgetViewId() {
        return R.id.analogAppwidget;
    }

    @Override
    protected Icon getDialIcon(Context context, SharedPreferences prefs) {
        return switch (WidgetDAO.getAnalogWidgetClockDial(prefs)) {
            case ANALOG_WIDGET_CLOCK_DIAL_WITH_NUMBERS -> Icon.createWithResource(context, R.drawable.analog_clock_dial_with_numbers);
            case ANALOG_WIDGET_CLOCK_DIAL_WITHOUT_NUMBERS -> Icon.createWithResource(context, R.drawable.analog_clock_dial_without_numbers);
            case ANALOG_WIDGET_CLOCK_DIAL_WITH_ROMAN_NUMBERS -> Icon.createWithResource(context, R.drawable.analog_clock_dial_with_roman_numbers);
            case ANALOG_WIDGET_CLOCK_DIAL_SUN -> Icon.createWithResource(context, R.drawable.analog_clock_dial_sun);
            case ANALOG_WIDGET_CLOCK_DIAL_FLOWER -> Icon.createWithResource(context, R.drawable.analog_clock_dial_flower);
            default -> Icon.createWithResource(context, R.drawable.analog_clock_dial);
        };
    }

    @Override
    protected Icon getHourHandIcon(Context context, SharedPreferences prefs) {
        return switch (WidgetDAO.getAnalogWidgetClockDial(prefs)) {
            case ANALOG_WIDGET_CLOCK_DIAL_SUN, ANALOG_WIDGET_CLOCK_DIAL_FLOWER ->
                Icon.createWithResource(context, R.drawable.analog_clock_hour_rounded);
            case ANALOG_WIDGET_CLOCK_DIAL_WITH_ROMAN_NUMBERS -> Icon.createWithResource(context, R.drawable.analog_clock_roman_hour);
            default -> Icon.createWithResource(context, R.drawable.analog_clock_hour);
        };
    }

    @Override
    protected Icon getMinuteHandIcon(Context context, SharedPreferences prefs) {
        return switch (WidgetDAO.getAnalogWidgetClockDial(prefs)) {
            case ANALOG_WIDGET_CLOCK_DIAL_SUN, ANALOG_WIDGET_CLOCK_DIAL_FLOWER ->
                Icon.createWithResource(context, R.drawable.analog_clock_minute_rounded);
            case ANALOG_WIDGET_CLOCK_DIAL_WITH_ROMAN_NUMBERS -> Icon.createWithResource(context, R.drawable.analog_clock_roman_minute);
            default -> Icon.createWithResource(context, R.drawable.analog_clock_minute);
        };
    }

    @Override
    protected Icon getSecondHandIcon(Context context, SharedPreferences prefs) {
        String clockDial = WidgetDAO.getAnalogWidgetClockDial(prefs);

        if (clockDial.equals(ANALOG_WIDGET_CLOCK_DIAL_SUN) || clockDial.equals(ANALOG_WIDGET_CLOCK_DIAL_FLOWER)) {
            return Icon.createWithResource(context, R.drawable.analog_clock_second_circle);
        } else {
            return switch (WidgetDAO.getAnalogWidgetClockSecondHand(prefs)) {
                case CLOCK_SECOND_HAND_VINTAGE -> Icon.createWithResource(context, R.drawable.analog_clock_second_vintage);
                case CLOCK_SECOND_HAND_LOLLIPOP -> Icon.createWithResource(context, R.drawable.analog_clock_second_lollipop);
                default -> Icon.createWithResource(context, R.drawable.analog_clock_second);
            };
        }
    }

    @Override
    protected boolean isSecondHandDisplayed(SharedPreferences prefs) {
        return WidgetDAO.isSecondHandDisplayedOnAnalogWidget(prefs);
    }

    @Override
    protected void applyDialColor(Icon dialIcon, SharedPreferences prefs) {
        if (!WidgetDAO.isAnalogWidgetDefaultDialColor(prefs)) {
            dialIcon.setTint(WidgetDAO.getAnalogWidgetDialColor(prefs));
        }
    }

    @Override
    protected void applyHourHandColor(Icon hourHandIcon, SharedPreferences prefs) {
        if (!WidgetDAO.isAnalogWidgetDefaultHourHandColor(prefs)) {
            hourHandIcon.setTint(WidgetDAO.getAnalogWidgetHourHandColor(prefs));
        }
    }

    @Override
    protected void applyMinuteHandColor(Icon minuteHandIcon, SharedPreferences prefs) {
        if (!WidgetDAO.isAnalogWidgetDefaultMinuteHandColor(prefs)) {
            minuteHandIcon.setTint(WidgetDAO.getAnalogWidgetMinuteHandColor(prefs));
        }
    }

    @Override
    protected void applySecondHandColor(Icon secondHandIcon, SharedPreferences prefs) {
        if (!WidgetDAO.isAnalogWidgetDefaultSecondHandColor(prefs)) {
            secondHandIcon.setTint(WidgetDAO.getAnalogWidgetSecondHandColor(prefs));
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new AnalogAppWidgetProvider().updateAnalogWidget(context, wm, widgetId);
    }

}
