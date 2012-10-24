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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.android.deskclock.Alarms;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.Utils;

import java.util.Calendar;

public class DigitalAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "DigitalAppWidgetProvider";
    private String mDateFormat = null;
    private final Calendar mCalendar;

    public DigitalAppWidgetProvider() {
        mCalendar = Calendar.getInstance();
    }

    @Override
    public void onUpdate(Context ctxt, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            float ratio = getScaleRatio(ctxt, null, appWidgetId);
            updateClock(ctxt, appWidgetManager, appWidgetId, ratio);
        }
        super.onUpdate(ctxt, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        // scale the fonts of the clock to fit inside the new size
        float ratio = getScaleRatio(context, newOptions, appWidgetId);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        updateClock(context, widgetManager, appWidgetId, ratio);
    }

    // Calculate the scale factor of the fonts in the widget
    public static float getScaleRatio(Context context, Bundle options, int id) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        if (options == null) {
            options = widgetManager.getAppWidgetOptions(id);
        }
        if (options != null) {
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

            Resources res = context.getResources();
            float widthRatio = minWidth / res.getDimension(R.dimen.min_digital_widget_width);
            float heightRatio = minHeight / res.getDimension(R.dimen.min_digital_widget_height);

            float ratio = Math.min(widthRatio, heightRatio);
            return (ratio > 1) ? 1 : ratio;
        }
        return 1;
    }

    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, DigitalAppWidgetProvider.class);
    }

    private void updateClock(
            Context c, AppWidgetManager appWidgetManager, int appWidgetId, float ratio) {
        if (mDateFormat == null) {
            mDateFormat = c.getResources().getString(R.string.abbrev_wday_month_day_no_year);
        }
        RemoteViews widget = new RemoteViews(c.getPackageName(), R.layout.digital_appwidget);
        widget.setOnClickPendingIntent(R.id.digital_appwidget,
                PendingIntent.getActivity(c, 0, new Intent(c, DeskClock.class), 0));
        setTime(c, widget, ratio);
        updateDateRemoteView(mDateFormat, widget);
        refreshAlarm(c, widget);
        final Intent intent = new Intent(c, DigitalAppWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        widget.setRemoteAdapter(appWidgetId, R.id.digital_appwidget_listview, intent);
        widget.setPendingIntentTemplate(R.id.digital_appwidget_listview,
                PendingIntent.getActivity(c, 0, new Intent(c, DeskClock.class), 0));
        appWidgetManager.notifyAppWidgetViewDataChanged(
                appWidgetId, R.id.digital_appwidget_listview);
        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    private void updateDateRemoteView(String dateFormat, RemoteViews clock) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());

        CharSequence newDate = DateFormat.format(dateFormat, cal);
        clock.setTextViewText(R.id.date, newDate);
    }

    private void refreshAlarm(Context c, RemoteViews clock) {
        String nextAlarm = Settings.System.getString(
                c.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        if (!nextAlarm.isEmpty()) {
            clock.setTextViewText(R.id.nextAlarm,
                    c.getString(R.string.control_set_alarm_with_existing, nextAlarm));
            clock.setViewVisibility(R.id.nextAlarm, View.VISIBLE);
        } else {
            clock.setViewVisibility(R.id.nextAlarm, View.GONE);
        }
    }

    private void setTime(Context c, RemoteViews clock, float scale) {
        float fontSize = c.getResources().getDimension(R.dimen.big_font_size);
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        if (Alarms.get24HourMode(c)) {
            clock.setTextViewText(
                    R.id.timeDisplayHours, DateFormat.format(Utils.HOURS_24, mCalendar));

        } else {
            clock.setTextViewText(R.id.timeDisplayHours, DateFormat.format(Utils.HOURS, mCalendar));
        }
        clock.setTextViewText(R.id.timeDisplayMinutes, DateFormat.format(Utils.MINUTES, mCalendar));
        clock.setTextViewTextSize(
                R.id.timeDisplayHours, TypedValue.COMPLEX_UNIT_PX, fontSize * scale);
        clock.setTextViewTextSize(
                R.id.timeDisplayMinutes, TypedValue.COMPLEX_UNIT_PX, fontSize * scale);
    }
}
