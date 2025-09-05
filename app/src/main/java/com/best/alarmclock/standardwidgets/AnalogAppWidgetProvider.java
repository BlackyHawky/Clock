/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.alarmclock.standardwidgets;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_WITHOUT_NUMBER;
import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_WITH_NUMBER;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.widget.RemoteViews;

import com.best.alarmclock.WidgetUtils;
import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.LogUtils;

/**
 * Simple widget to show an analog clock (with or without the second hand for Android12+).
 */
public class AnalogAppWidgetProvider extends AppWidgetProvider {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("AnlgWdgtProv");

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        final RemoteViews views = relayoutWidget(context, wm, widgetId);

        wm.updateAppWidget(widgetId, views);
    }

    private static RemoteViews relayoutWidget(Context context, AppWidgetManager wm, int widgetId) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        final String packageName = context.getPackageName();
        final RemoteViews widget = new RemoteViews(packageName, R.layout.standard_analog_appwidget);
        final boolean isSecondHandDisplayed = WidgetDAO.isSecondHandDisplayedOnAnalogWidget(prefs);

        // Handle dial
        widget.setIcon(R.id.analogAppwidget, "setDial",
                getAnalogClockDialIcon(context, WidgetDAO.getAnalogWidgetClockDial(prefs)));

        // Handle second hand
        if (isSecondHandDisplayed) {
            final Icon secondHandIcon = Icon.createWithResource(context, R.drawable.analog_clock_second);
            widget.setIcon(R.id.analogAppwidget, "setSecondHand", secondHandIcon);
        } else {
            widget.setIcon(R.id.analogAppwidget, "setSecondHand", null);
        }

        // Tapping on the widget opens the app (if not on the lock screen).
        if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
            final Intent openApp = new Intent(context, DeskClock.class);
            final PendingIntent pi = PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE);
            widget.setOnClickPendingIntent(R.id.analogAppwidget, pi);
        }

        return widget;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LOGGER.i("onReceive: " + intent);
        super.onReceive(context, intent);

        final AppWidgetManager wm = AppWidgetManager.getInstance(context);
        if (wm == null) {
            return;
        }

        final ComponentName provider = new ComponentName(context, getClass());
        final int[] widgetIds = wm.getAppWidgetIds(provider);
        final String action = intent.getAction();
        if (action != null) {
            if (action.equals(ACTION_APPWIDGET_UPDATE)) {
                for (int widgetId : widgetIds) {
                    updateAppWidget(context, wm, widgetId);
                }
            }
        }

        WidgetUtils.updateWidgetCount(context, getClass(), widgetIds.length, R.string.category_analog_widget);
    }

    /**
     * Called when widgets must provide remote views.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        super.onUpdate(context, wm, widgetIds);

        for (int widgetId : widgetIds) {
            updateAppWidget(context, wm, widgetId);
        }
    }

    private static Icon getAnalogClockDialIcon(Context context, String dial) {
        return switch (dial) {
            case ANALOG_WIDGET_CLOCK_DIAL_WITH_NUMBER ->
                    Icon.createWithResource(context, R.drawable.analog_clock_dial_with_number);
            case ANALOG_WIDGET_CLOCK_DIAL_WITHOUT_NUMBER ->
                    Icon.createWithResource(context, R.drawable.analog_clock_dial_without_number);
            default ->
                    Icon.createWithResource(context, R.drawable.standard_analog_appwidget_clock_dial);
        };
    }
}
