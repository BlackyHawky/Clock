// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

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

import androidx.annotation.NonNull;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.WidgetUtils;

/**
 * Abstract base class for analog app widget providers.
 * <p>
 * This class encapsulates the shared logic for rendering analog clock widgets,
 * including layout inflation and view configuration.
 * </p>
 */
public abstract class BaseAnalogAppWidgetProvider extends AppWidgetProvider {

    protected abstract int getLayoutId(@NonNull SharedPreferences prefs);

    protected abstract int getWidgetViewId();

    protected abstract Icon getDialIcon(@NonNull Context context, @NonNull SharedPreferences prefs);

    protected abstract Icon getHourHandIcon(@NonNull Context context, @NonNull SharedPreferences prefs);

    protected abstract Icon getMinuteHandIcon(@NonNull Context context, @NonNull SharedPreferences prefs);

    protected abstract Icon getSecondHandIcon(@NonNull Context context, @NonNull SharedPreferences prefs);

    protected abstract boolean isSecondHandDisplayed(@NonNull SharedPreferences prefs);

    protected abstract void applyDialColor(@NonNull Icon dialIcon, @NonNull SharedPreferences prefs);

    protected abstract void applyHourHandColor(@NonNull Icon hourHandIcon, @NonNull SharedPreferences prefs);

    protected abstract void applyMinuteHandColor(@NonNull Icon minuteHandIcon, @NonNull SharedPreferences prefs);

    protected abstract void applySecondHandColor(@NonNull Icon secondHandIcon, @NonNull SharedPreferences prefs);

    protected void updateAnalogWidget(@NonNull Context context, @NonNull AppWidgetManager wm, int widgetId) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        RemoteViews views = relayoutWidget(context, prefs, wm, widgetId);
        wm.updateAppWidget(widgetId, views);
    }

    protected RemoteViews relayoutWidget(@NonNull Context context, @NonNull SharedPreferences prefs, @NonNull AppWidgetManager wm,
                                         int widgetId) {

        RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId(prefs));

        // Tapping on the widget opens the app (if not on the lock screen).
        if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
            Intent openApp = new Intent(context, DeskClock.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(getWidgetViewId(), pi);
        }

        // Configure child views of the remote view for Android 12+.
        if (SdkUtils.isAtLeastAndroid12()) {
            Icon dialIcon = getDialIcon(context, prefs);
            Icon hourHandIcon = getHourHandIcon(context, prefs);
            Icon minuteHandIcon = getMinuteHandIcon(context, prefs);
            Icon secondHandIcon = getSecondHandIcon(context, prefs);

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
        }

        return rv;
    }

    /**
     * Called when widgets must provide remote views.
     */
    @Override
    public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager wm, @NonNull int[] widgetIds) {
        for (int widgetId : widgetIds) {
            updateAnalogWidget(context, wm, widgetId);
        }
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        LogUtils.i(getClass().getSimpleName() + " - onReceive: " + intent);

        super.onReceive(context, intent);

        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        if (wm == null) {
            return;
        }

        ComponentName provider = new ComponentName(context, getClass());
        int widgetCount = wm.getAppWidgetIds(provider).length;
        int delta = WidgetDAO.updateWidgetCount(getDefaultSharedPreferences(context), getClass(), widgetCount);

        WidgetUtils.updateWidgetCount(delta, R.string.category_analog_widget);
    }
}

