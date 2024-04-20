// SPDX-License-Identifier: GPL-3.0-only
package com.best.alarmclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;

/**
 * Simple widget to show the Material You analog clock.
 */
public class AnalogAppWidgetMaterialYouProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        final AppWidgetManager wm = AppWidgetManager.getInstance(context);
        if (wm == null) {
            return;
        }

        // Send events for newly created/deleted widgets.
        final ComponentName provider = new ComponentName(context, getClass());
        final int widgetCount = wm.getAppWidgetIds(provider).length;

        final DataModel dm = DataModel.getDataModel();
        dm.updateWidgetCount(getClass(), widgetCount, R.string.category_analog_widget);
    }

    /**
     * Called when widgets must provide remote views.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        super.onUpdate(context, wm, widgetIds);

        for (int widgetId : widgetIds) {
            final String packageName = context.getPackageName();
            final RemoteViews widget = new RemoteViews(packageName, R.layout.analog_appwidget_material_you);

            // Tapping on the widget opens the app (if not on the lock screen).
            if (Utils.isWidgetClickable(wm, widgetId)) {
                final Intent openApp = new Intent(context, DeskClock.class);
                final PendingIntent pi = PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE);
                widget.setOnClickPendingIntent(R.id.analog_appwidget_material_you, pi);
            }

            wm.updateAppWidget(widgetId, widget);
        }
    }
}
