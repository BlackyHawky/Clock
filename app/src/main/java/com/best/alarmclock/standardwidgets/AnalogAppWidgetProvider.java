/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.alarmclock.standardwidgets;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ANALOG_WIDGET_WITH_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_WITH_SECOND_HAND;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.best.alarmclock.WidgetUtils;
import com.best.deskclock.DeskClock;
import com.best.deskclock.R;

/**
 * Simple widget to show an analog clock (with or without the second hand for Android12+).
 */
public class AnalogAppWidgetProvider extends AppWidgetProvider {

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        final boolean isSecondHandDisplayed =
                prefs.getBoolean(KEY_ANALOG_WIDGET_WITH_SECOND_HAND, DEFAULT_ANALOG_WIDGET_WITH_SECOND_HAND);
        final RemoteViews views = isSecondHandDisplayed
                ? relayoutWidget(context, wm, widgetId, true)
                : relayoutWidget(context, wm, widgetId, false);

        wm.updateAppWidget(widgetId, views);
    }

    private static RemoteViews relayoutWidget(Context context, AppWidgetManager wm, int widgetId, boolean isSecondHandDisplayed) {
        final String packageName = context.getPackageName();
        final RemoteViews widget = new RemoteViews(packageName, isSecondHandDisplayed
                ? R.layout.standard_analog_appwidget_with_second_hand
                : R.layout.standard_analog_appwidget_without_second_hand);

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

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        prefs.edit().remove(KEY_ANALOG_WIDGET_WITH_SECOND_HAND).apply();
    }

}
