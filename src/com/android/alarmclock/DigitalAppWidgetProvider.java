/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.alarmclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.worldclock.CitiesActivity;

public class DigitalAppWidgetProvider extends AppWidgetProvider {
    public DigitalAppWidgetProvider() {
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
            updateClock(context, appWidgetManager, appWidgetId, ratio);

        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        // scale the fonts of the clock to fit inside the new size
        float ratio = WidgetUtils.getScaleRatio(context, newOptions, appWidgetId);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        updateClock(context, widgetManager, appWidgetId, ratio);
    }

    private void updateClock(
            Context context, AppWidgetManager appWidgetManager, int appWidgetId, float ratio) {
        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.digital_appwidget);

        // Launch clock when clicking on the time in the widget only if not a lock screen widget
        Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        if (newOptions != null &&
                newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
                != AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
            widget.setOnClickPendingIntent(R.id.digital_appwidget,
                    PendingIntent.getActivity(context, 0, new Intent(context, DeskClock.class), 0));
        }

        // Setup alarm text and font sizes
        DigitalWidgetViewsFactory.refreshAlarm(context, widget);
        WidgetUtils.setClockSize(context, widget, ratio);

        // Set up R.id.digital_appwidget_listview to use a remote views adapter
        // That remote views adapter connects to a RemoteViewsService through intent.
        final Intent intent = new Intent(context, DigitalAppWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        widget.setRemoteAdapter(R.id.digital_appwidget_listview, intent);

        // Set up the click on any world clock to start the Cities Activity
        //TODO: Should this be in the options guard above?
        widget.setPendingIntentTemplate(R.id.digital_appwidget_listview,
                PendingIntent.
                        getActivity(context, 0, new Intent(context, CitiesActivity.class), 0));

        // Refresh the widget
        appWidgetManager.notifyAppWidgetViewDataChanged(
                appWidgetId, R.id.digital_appwidget_listview);
        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }
}
