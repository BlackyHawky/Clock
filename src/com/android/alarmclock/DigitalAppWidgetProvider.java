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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.android.deskclock.Alarms;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.worldclock.CitiesActivity;

import java.util.Calendar;

public class DigitalAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "DigitalAppWidgetProvider";

    public DigitalAppWidgetProvider() {
    }

    @Override
    public void onUpdate(Context ctxt, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            float ratio = WidgetUtils.getScaleRatio(ctxt, null, appWidgetId);
            updateClock(ctxt, appWidgetManager, appWidgetId, ratio);

        }
        super.onUpdate(ctxt, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        // scale the fonts of the clock to fit inside the new size
        float ratio = WidgetUtils.getScaleRatio(context, newOptions, appWidgetId);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        updateClock(context, widgetManager, appWidgetId, ratio);
    }

    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, DigitalAppWidgetProvider.class);
    }

    private void updateClock(
            Context c, AppWidgetManager appWidgetManager, int appWidgetId, float ratio) {
        RemoteViews widget = new RemoteViews(c.getPackageName(), R.layout.digital_appwidget);
        // launch clock when clicking on the time in the widget only if not a lock screen widget
        Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        if (newOptions != null &&
                newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
                != AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
            widget.setOnClickPendingIntent(R.id.digital_appwidget,
                    PendingIntent.getActivity(c, 0, new Intent(c, DeskClock.class), 0));
        }
        refreshAlarm(c, widget);
        WidgetUtils.setClockSize(c, widget, ratio);
        final Intent intent = new Intent(c, DigitalAppWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        widget.setRemoteAdapter(appWidgetId, R.id.digital_appwidget_listview, intent);
        widget.setPendingIntentTemplate(R.id.digital_appwidget_listview,
                PendingIntent.getActivity(c, 0, new Intent(c, CitiesActivity.class), 0));
        appWidgetManager.notifyAppWidgetViewDataChanged(
                appWidgetId, R.id.digital_appwidget_listview);
        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    private void refreshAlarm(Context c, RemoteViews clock) {
        String nextAlarm = Settings.System.getString(
                c.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        if (!TextUtils.isEmpty(nextAlarm)) {
            clock.setTextViewText(R.id.nextAlarm,
                    c.getString(R.string.control_set_alarm_with_existing, nextAlarm));
            clock.setViewVisibility(R.id.nextAlarm, View.VISIBLE);
        } else {
            clock.setViewVisibility(R.id.nextAlarm, View.GONE);
        }
    }
}
