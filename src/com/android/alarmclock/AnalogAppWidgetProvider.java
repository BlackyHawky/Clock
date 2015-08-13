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

import com.android.deskclock.HandleDeskClockApiCalls;
import com.android.deskclock.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Simple widget to show an analog clock.
 */
public class AnalogAppWidgetProvider extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            return;
        }

        final String packageName = context.getPackageName();
        final RemoteViews views = new RemoteViews(packageName, R.layout.analog_appwidget);

        final Intent showClock = new Intent(HandleDeskClockApiCalls.ACTION_SHOW_CLOCK)
                .putExtra(HandleDeskClockApiCalls.EXTRA_FROM_WIDGET, true);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, showClock, 0);
        views.setOnClickPendingIntent(R.id.analog_appwidget, pendingIntent);

        final int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetIds, views);
    }
}