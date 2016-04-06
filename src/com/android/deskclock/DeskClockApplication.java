/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import com.android.alarmclock.CityAppWidgetProvider;
import com.android.alarmclock.DigitalAppWidgetProvider;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.events.LogEventTracker;
import com.android.deskclock.uidata.UiDataModel;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS;

public class DeskClockApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final Context applicationContext = getApplicationContext();
        DataModel.getDataModel().setContext(applicationContext);
        UiDataModel.getUiDataModel().setContext(applicationContext);
        Events.addEventTracker(new LogEventTracker(applicationContext));
    }

    /**
     * Some widget layouts are sensitive to orientation changes but it is very difficult to detect
     * orientation changes from within the widget providers themselves. Consequently, the
     * orientation change is detected here and the widget manager is tickled to update the widgets.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateWidgets(CityAppWidgetProvider.class);
        updateWidgets(DigitalAppWidgetProvider.class);
    }

    /**
     * Broadcasts an Intent that updates all widgets backed by the same widget provider.
     */
    private void updateWidgets(Class<?> widgetProviderClass) {
        // Retrieve all widget ids of the given type.
        final AppWidgetManager am = AppWidgetManager.getInstance(this);
        final ComponentName cn = new ComponentName(this, widgetProviderClass);
        final int[] appWidgetIds = am.getAppWidgetIds(cn);

        // No widget ids means nothing to update.
        if (appWidgetIds.length == 0) {
            return;
        }

        // Broadcast an intent to update all instances of the widget.
        final Intent intent = new Intent(ACTION_APPWIDGET_UPDATE, null, this, widgetProviderClass);
        intent.putExtra(EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(intent);
    }
}