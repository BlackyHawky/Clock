// SPDX-License-Identifier: GPL-3.0-only

package com.best.alarmclock;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.widget.RemoteViews;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;

/**
 * Abstract base class for analog app widget providers.
 * <p>
 * This class encapsulates the shared logic for rendering analog clock widgets,
 * including layout inflation and view configuration.
 * </p>
 */
public abstract class BaseAnalogAppWidgetProvider extends AppWidgetProvider {

    protected abstract int getLayoutId();
    protected abstract int getWidgetViewId();
    protected abstract Icon getDialIcon(Context context, SharedPreferences prefs);
    protected abstract Icon getHourHandIcon(Context context);
    protected abstract Icon getMinuteHandIcon(Context context);
    protected abstract Icon getSecondHandIcon(Context context);
    protected abstract boolean isSecondHandDisplayed(SharedPreferences prefs);
    protected abstract void applyDialColor(Icon dialIcon, SharedPreferences prefs);
    protected abstract void applyHourHandColor(Icon hourHandIcon, SharedPreferences prefs);
    protected abstract void applyMinuteHandColor(Icon minuteHandIcon, SharedPreferences prefs);
    protected abstract void applySecondHandColor(Icon secondHandIcon, SharedPreferences prefs);

    public void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        RemoteViews views = relayoutWidget(context, wm, widgetId);
        wm.updateAppWidget(widgetId, views);
    }

    protected RemoteViews relayoutWidget(Context context, AppWidgetManager wm, int widgetId) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());

        // Tapping on the widget opens the app (if not on the lock screen).
        if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
            Intent openApp = new Intent(context, DeskClock.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(getWidgetViewId(), pi);
        }

        // Configure child views of the remote view.
        Icon dialIcon = getDialIcon(context, prefs);
        Icon hourHandIcon = getHourHandIcon(context);
        Icon minuteHandIcon = getMinuteHandIcon(context);
        Icon secondHandIcon = getSecondHandIcon(context);

        rv.setIcon(getWidgetViewId(), "setDial", dialIcon);
        rv.setIcon(getWidgetViewId(), "setHourHand", hourHandIcon);
        rv.setIcon(getWidgetViewId(), "setMinuteHand", minuteHandIcon);

        applyDialColor(dialIcon, prefs);
        applyHourHandColor(hourHandIcon, prefs);
        applyMinuteHandColor(minuteHandIcon, prefs);

        if (isSecondHandDisplayed(prefs)) {
            rv.setIcon(getWidgetViewId(), "setSecondHand", secondHandIcon);
            applySecondHandColor(secondHandIcon, prefs);
        } else {
            rv.setIcon(getWidgetViewId(), "setSecondHand", null);
        }

        return rv;
    }

    /**
     * Called when widgets must provide remote views.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        for (int widgetId : widgetIds) {
            updateAppWidget(context, wm, widgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        if (wm == null) return;

        ComponentName provider = new ComponentName(context, getClass());
        int[] widgetIds = wm.getAppWidgetIds(provider);
        if (intent.getAction() != null && intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            for (int widgetId : widgetIds) {
                updateAppWidget(context, wm, widgetId);
            }
        }

        WidgetUtils.updateWidgetCount(context, getClass(), widgetIds.length, R.string.category_analog_widget);
    }
}

