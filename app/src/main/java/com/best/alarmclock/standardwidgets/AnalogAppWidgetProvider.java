/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.alarmclock.standardwidgets;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_WITHOUT_NUMBERS;
import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_WITH_NUMBERS;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_CLOCK_SECOND_HAND;

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
        final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.standard_analog_appwidget);

        // Tapping on the widget opens the app (if not on the lock screen).
        if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
            final Intent openApp = new Intent(context, DeskClock.class);
            final PendingIntent pi = PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.analogAppwidget, pi);
        }

        // Configure child views of the remote view.
        final Icon dialIcon = getAnalogClockDialIcon(context, WidgetDAO.getAnalogWidgetClockDial(prefs));
        final Icon hourHandIcon = Icon.createWithResource(context, R.drawable.analog_clock_hour);
        final Icon minuteHandIcon = Icon.createWithResource(context, R.drawable.analog_clock_minute);
        final Icon secondHandIcon = Icon.createWithResource(context,
                WidgetDAO.getAnalogWidgetClockSecondHand(prefs).equals(DEFAULT_CLOCK_SECOND_HAND)
                        ? R.drawable.analog_clock_second
                        : R.drawable.analog_clock_second_vintage);

        rv.setIcon(R.id.analogAppwidget, "setDial", dialIcon);
        rv.setIcon(R.id.analogAppwidget, "setHourHand", hourHandIcon);
        rv.setIcon(R.id.analogAppwidget, "setMinuteHand", minuteHandIcon);

        // Apply the color to the dial.
        if (!WidgetDAO.isAnalogWidgetDefaultDialColor(prefs)) {
            dialIcon.setTint(WidgetDAO.getAnalogWidgetDialColor(prefs));
        }

        // Apply the color to the hour hand.
        if (!WidgetDAO.isAnalogWidgetDefaultHourHandColor(prefs)) {
            hourHandIcon.setTint(WidgetDAO.getAnalogWidgetHourHandColor(prefs));
        }

        // Apply the color to the minute hand.
        if (!WidgetDAO.isAnalogWidgetDefaultMinuteHandColor(prefs)) {
            minuteHandIcon.setTint(WidgetDAO.getAnalogWidgetMinuteHandColor(prefs));
        }

        // Apply the color to the second hand if it's displayed.
        if (WidgetDAO.isSecondHandDisplayedOnAnalogWidget(prefs)) {
            rv.setIcon(R.id.analogAppwidget, "setSecondHand", secondHandIcon);

            if (!WidgetDAO.isAnalogWidgetDefaultSecondHandColor(prefs)) {
                secondHandIcon.setTint(WidgetDAO.getAnalogWidgetSecondHandColor(prefs));
            }
        } else {
            rv.setIcon(R.id.analogAppwidget, "setSecondHand", null);
        }

        return rv;
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
            case ANALOG_WIDGET_CLOCK_DIAL_WITH_NUMBERS ->
                    Icon.createWithResource(context, R.drawable.analog_clock_dial_with_numbers);
            case ANALOG_WIDGET_CLOCK_DIAL_WITHOUT_NUMBERS ->
                    Icon.createWithResource(context, R.drawable.analog_clock_dial_without_numbers);
            default ->
                    Icon.createWithResource(context, R.drawable.standard_analog_appwidget_clock_dial);
        };
    }
}
