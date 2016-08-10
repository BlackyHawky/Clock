/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.alarmclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;

/**
 * Simple widget to show an analog clock.
 */
public class AnalogAppWidgetProvider extends AppWidgetProvider {

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
            final RemoteViews widget = new RemoteViews(packageName, R.layout.analog_appwidget);

            // Tapping on the widget opens the app (if not on the lock screen).
            if (Utils.isWidgetClickable(wm, widgetId)) {
                final Intent openApp = new Intent(context, DeskClock.class);
                final PendingIntent pi = PendingIntent.getActivity(context, 0, openApp, 0);
                widget.setOnClickPendingIntent(R.id.analog_appwidget, pi);
            }

            wm.updateAppWidget(widgetId, widget);
        }
    }
}