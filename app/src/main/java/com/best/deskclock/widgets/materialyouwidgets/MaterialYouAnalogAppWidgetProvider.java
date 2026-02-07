// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets.materialyouwidgets;

import static com.best.deskclock.settings.PreferencesDefaultValues.MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL_FLOWER;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.widgets.BaseAnalogAppWidgetProvider;

/**
 * Simple widget to show the Material You analog clock (with or without the second hand for Android12+).
 */
public class MaterialYouAnalogAppWidgetProvider extends BaseAnalogAppWidgetProvider {

    @Override
    protected int getLayoutId(SharedPreferences prefs) {
        if (SdkUtils.isAtLeastAndroid12()) {
            return R.layout.material_you_analog_appwidget;
        }

        if (WidgetDAO.getMaterialYouAnalogWidgetClockDial(prefs).equals(MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL_FLOWER)) {
            return R.layout.material_you_analog_appwidget_dial_flower;
        } else {
            return R.layout.material_you_analog_appwidget;
        }
    }

    @Override
    protected int getWidgetViewId() {
        return R.id.materialYouAnalogAppwidget;
    }

    @Override
    protected Icon getDialIcon(Context context, SharedPreferences prefs) {
        return Icon.createWithResource(context,
                WidgetDAO.getMaterialYouAnalogWidgetClockDial(prefs).equals(MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL_FLOWER)
                        ? R.drawable.material_you_analog_clock_dial_flower
                        : R.drawable.material_you_analog_clock_dial_sun);
    }

    @Override
    protected Icon getHourHandIcon(Context context) {
        return Icon.createWithResource(context, R.drawable.material_you_analog_clock_hour);
    }

    @Override
    protected Icon getMinuteHandIcon(Context context) {
        return Icon.createWithResource(context, R.drawable.material_you_analog_clock_minute);
    }

    @Override
    protected Icon getSecondHandIcon(Context context) {
        return Icon.createWithResource(context, R.drawable.material_you_analog_clock_second);
    }

    @Override
    protected boolean isSecondHandDisplayed(SharedPreferences prefs) {
        return WidgetDAO.isSecondHandDisplayedOnMaterialYouAnalogWidget(prefs);
    }

    @Override
    protected void applyDialColor(Icon dialIcon, SharedPreferences prefs) {
        if (!WidgetDAO.isMaterialYouAnalogWidgetDefaultDialColor(prefs)) {
            dialIcon.setTint(WidgetDAO.getMaterialYouAnalogWidgetDialColor(prefs));
        }
    }

    @Override
    protected void applyHourHandColor(Icon hourHandIcon, SharedPreferences prefs) {
        if (!WidgetDAO.isMaterialYouAnalogWidgetDefaultHourHandColor(prefs)) {
            hourHandIcon.setTint(WidgetDAO.getMaterialYouAnalogWidgetHourHandColor(prefs));
        }
    }

    @Override
    protected void applyMinuteHandColor(Icon minuteHandIcon, SharedPreferences prefs) {
        if (!WidgetDAO.isMaterialYouAnalogWidgetDefaultMinuteHandColor(prefs)) {
            minuteHandIcon.setTint(WidgetDAO.getMaterialYouAnalogWidgetMinuteHandColor(prefs));
        }
    }

    @Override
    protected void applySecondHandColor(Icon secondHandIcon, SharedPreferences prefs) {
        if (!WidgetDAO.isMaterialYouAnalogWidgetDefaultSecondHandColor(prefs)) {
            secondHandIcon.setTint(WidgetDAO.getMaterialYouAnalogWidgetSecondHandColor(prefs));
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new MaterialYouAnalogAppWidgetProvider().updateAnalogWidget(context, wm, widgetId);
    }

}
