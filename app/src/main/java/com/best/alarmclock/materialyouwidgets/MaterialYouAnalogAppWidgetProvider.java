// SPDX-License-Identifier: GPL-3.0-only

package com.best.alarmclock.materialyouwidgets;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL_FLOWER;

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
 * Simple widget to show the Material You analog clock (with or without the second hand for Android12+).
 */
public class MaterialYouAnalogAppWidgetProvider extends AppWidgetProvider {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("MYAnlgWdgtProv");

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        final RemoteViews views = relayoutWidget(context, wm, widgetId);

        wm.updateAppWidget(widgetId, views);
    }

    private static RemoteViews relayoutWidget(Context context, AppWidgetManager wm, int widgetId) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.material_you_analog_appwidget);

        // Tapping on the widget opens the app (if not on the lock screen).
        if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
            final Intent openApp = new Intent(context, DeskClock.class);
            final PendingIntent pi = PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.materialYouAnalogAppwidget, pi);
        }

        // Configure child views of the remote view.
        final Icon dialIcon = getMaterialYouAnalogClockDialIcon(context, WidgetDAO.getMaterialYouAnalogWidgetClockDial(prefs));
        final Icon hourHandIcon = Icon.createWithResource(context, R.drawable.material_you_analog_clock_hour);
        final Icon minuteHandIcon = Icon.createWithResource(context, R.drawable.material_you_analog_clock_minute);
        final Icon secondHandIcon = Icon.createWithResource(context, R.drawable.material_you_analog_clock_second);

        rv.setIcon(R.id.materialYouAnalogAppwidget, "setDial", dialIcon);
        rv.setIcon(R.id.materialYouAnalogAppwidget, "setHourHand", hourHandIcon);
        rv.setIcon(R.id.materialYouAnalogAppwidget, "setMinuteHand", minuteHandIcon);

        // Apply the color to the dial.
        if (!WidgetDAO.isMaterialYouAnalogWidgetDefaultDialColor(prefs)) {
            dialIcon.setTint(WidgetDAO.getMaterialYouAnalogWidgetDialColor(prefs));
        }

        // Apply the color to the hour hand.
        if (!WidgetDAO.isMaterialYouAnalogWidgetDefaultHourHandColor(prefs)) {
            hourHandIcon.setTint(WidgetDAO.getMaterialYouAnalogWidgetHourHandColor(prefs));
        }

        // Apply the color to the minute hand.
        if (!WidgetDAO.isMaterialYouAnalogWidgetDefaultMinuteHandColor(prefs)) {
            minuteHandIcon.setTint(WidgetDAO.getMaterialYouAnalogWidgetMinuteHandColor(prefs));
        }

        // Apply the color to the second hand if it's displayed.
        if (WidgetDAO.isSecondHandDisplayedOnMaterialYouAnalogWidget(prefs)) {
            rv.setIcon(R.id.materialYouAnalogAppwidget, "setSecondHand", secondHandIcon);

            if (!WidgetDAO.isMaterialYouAnalogWidgetDefaultSecondHandColor(prefs)) {
                secondHandIcon.setTint(WidgetDAO.getMaterialYouAnalogWidgetSecondHandColor(prefs));
            }
        } else {
            rv.setIcon(R.id.materialYouAnalogAppwidget, "setSecondHand", null);
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

    private static Icon getMaterialYouAnalogClockDialIcon(Context context, String dial) {
        if (dial.equals(MATERIAL_YOU_ANALOG_WIDGET_CLOCK_DIAL_FLOWER)) {
            return Icon.createWithResource(context, R.drawable.material_you_analog_clock_dial_flower);
        } else {
            return Icon.createWithResource(context, R.drawable.material_you_analog_clock_dial_sun);
        }
    }

}
