// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets.materialyouwidgets;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.widgets.BaseDigitalAppWidgetCityViewsFactory;

/**
 * This factory produces entries in the world cities list view displayed at the bottom of the
 * material you digital widget. Each row is comprised of two world cities located side-by-side.
 */
public class MaterialYouDigitalAppWidgetCityViewsFactory extends BaseDigitalAppWidgetCityViewsFactory {

    public MaterialYouDigitalAppWidgetCityViewsFactory(Context context, Intent intent) {
        super(context, intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.world_clock_material_you_remote_list_item;
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
    protected void configureColors(RemoteViews rv, Context context, SharedPreferences prefs, int clockId, int labelId, int dayId) {
        int cityClockColor = WidgetDAO.isMaterialYouDigitalWidgetDefaultCityClockColor(prefs)
                ? ContextCompat.getColor(context, R.color.digital_widget_time_color)
                : WidgetDAO.getMaterialYouDigitalWidgetCustomCityClockColor(prefs);

        rv.setTextColor(clockId, cityClockColor);

        int cityNameColor = WidgetDAO.isMaterialYouDigitalWidgetDefaultCityNameColor(prefs)
                ? ContextCompat.getColor(context, R.color.widget_text_color)
                : WidgetDAO.getMaterialYouDigitalWidgetCustomCityNameColor(prefs);

        rv.setTextColor(labelId, cityNameColor);
        rv.setTextColor(dayId, cityNameColor);
    }

}
