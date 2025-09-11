/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.alarmclock.standardwidgets;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_WIDGETS_CUSTOM_COLOR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.best.alarmclock.BaseDigitalAppWidgetCityViewsFactory;
import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;

/**
 * This factory produces entries in the world cities list view displayed at the bottom of the
 * digital widget. Each row is comprised of two world cities located side-by-side.
 */
public class DigitalAppWidgetCityViewsFactory extends BaseDigitalAppWidgetCityViewsFactory {

    public DigitalAppWidgetCityViewsFactory(Context context, Intent intent) {
        super(context, intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.world_clock_remote_list_item;
    }

    @Override
    protected int getCityViewId() {
        return R.id.widgetItem;
    }

    @Override
    protected int getLeftClockId() {
        return R.id.leftClock;
    }

    @Override
    protected int getLeftCityNameId() {
        return R.id.cityNameLeft;
    }

    @Override
    protected int getLeftCityDayId() {
        return R.id.cityDayLeft;
    }

    @Override
    protected int getRightClockId() {
        return R.id.rightClock;
    }

    @Override
    protected int getRightCityNameId() {
        return R.id.cityNameRight;
    }

    @Override
    protected int getRightCityDayId() {
        return R.id.cityDayRight;
    }

    @Override
    protected int getCitySpacerId() {
        return R.id.citySpacer;
    }

    @Override
    protected void configureColors(RemoteViews rv, Context context, SharedPreferences prefs,
                                   int clockId, int labelId, int dayId) {

        int cityClockColor = WidgetDAO.isDigitalWidgetDefaultCityClockColor(prefs)
                ? DEFAULT_WIDGETS_CUSTOM_COLOR
                : WidgetDAO.getDigitalWidgetCustomCityClockColor(prefs);

        rv.setTextColor(clockId, cityClockColor);

        int cityNameColor = WidgetDAO.isDigitalWidgetDefaultCityNameColor(prefs)
                ? DEFAULT_WIDGETS_CUSTOM_COLOR
                : WidgetDAO.getDigitalWidgetCustomCityNameColor(prefs);

        rv.setTextColor(labelId, cityNameColor);
        rv.setTextColor(dayId, cityNameColor);
    }

}
