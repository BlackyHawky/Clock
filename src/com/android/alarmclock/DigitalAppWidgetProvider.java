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

import android.app.AlarmManager;
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
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.android.deskclock.HandleDeskClockApiCalls;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.worldclock.CitySelectionActivity;

import java.util.Locale;

import static android.app.AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED;
import static android.app.AlarmManager.RTC;
import static android.app.PendingIntent.FLAG_NO_CREATE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Intent.ACTION_DATE_CHANGED;
import static android.content.Intent.ACTION_LOCALE_CHANGED;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.content.Intent.ACTION_TIMEZONE_CHANGED;
import static android.content.Intent.ACTION_TIME_CHANGED;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static com.android.deskclock.alarms.AlarmStateManager.SYSTEM_ALARM_CHANGE_ACTION;
import static com.android.deskclock.data.DataModel.ACTION_DIGITAL_WIDGET_CHANGED;

public class DigitalAppWidgetProvider extends AppWidgetProvider {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("DigAppWidgetProvider");

    /** Intent action used for refreshing a world clock's date. See Nepal timezone: UTC+05:45. */
    private static final String ACTION_ON_QUARTER_HOUR = "com.android.deskclock.ON_QUARTER_HOUR";

    /** Intent used to deliver the {@link #ACTION_ON_QUARTER_HOUR} callback. */
    private static final Intent QUARTER_HOUR_INTENT = new Intent(ACTION_ON_QUARTER_HOUR);

    // Lazily creating this name to use with the AppWidgetManager
    private ComponentName mComponentName;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        // Schedule the quarter-hour callback if necessary.
        updateQuarterHourCallback(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        // Remove any scheduled quarter-hour callback.
        removeQuarterHourCallback(context);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        final String action = intent.getAction();
        if (DigitalAppWidgetService.LOGGING) {
            LOGGER.i("onReceive: " + action);
        }
        super.onReceive(context, intent);

        final AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        if (widgetManager == null) {
            return;
        }
        final int[] appWidgetIds = widgetManager.getAppWidgetIds(getComponentName(context));

        switch (action) {
            case ACTION_DATE_CHANGED:
            case ACTION_TIME_CHANGED:
            case ACTION_LOCALE_CHANGED:
            case ACTION_ON_QUARTER_HOUR:
            case ACTION_TIMEZONE_CHANGED:
                // Time has changed so reschedule the next quarter-hour callback.
                updateQuarterHourCallback(context);

                final String pName = context.getPackageName();
                for (int appWidgetId : appWidgetIds) {
                    widgetManager.notifyAppWidgetViewDataChanged(appWidgetId,
                            R.id.digital_appwidget_listview);
                    final RemoteViews widget = new RemoteViews(pName, R.layout.digital_appwidget);
                    final float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
                    refreshAlarm(context, widget, ratio);
                    WidgetUtils.setClockSize(context, widget, ratio);
                    WidgetUtils.setTimeFormat(context, widget, 0.4f /* amPmRatio */, R.id.clock);
                    widgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
                }

                break;

            case ACTION_DIGITAL_WIDGET_CHANGED:
                // Selected cities have changed so schedule/remove the next quarter-hour callback.
                updateQuarterHourCallback(context);
            case ACTION_SCREEN_ON:
            case SYSTEM_ALARM_CHANGE_ACTION:
            case ACTION_NEXT_ALARM_CLOCK_CHANGED:
                for (int appWidgetId : appWidgetIds) {
                    final float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
                    updateClock(context, widgetManager, appWidgetId, ratio);
                }
                break;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int appWidgetId : appWidgetIds) {
            final float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
            updateClock(context, appWidgetManager, appWidgetId, ratio);
        }

        updateQuarterHourCallback(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        // scale the fonts of the clock to fit inside the new size
        final float ratio = WidgetUtils.getScaleRatio(context, newOptions, appWidgetId);
        final AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        updateClock(context, widgetManager, appWidgetId, ratio);
    }

    private void updateClock(Context context, AppWidgetManager widgetManager, int appWidgetId,
            float ratio) {
        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.digital_appwidget);

        // Launch clock when clicking on the time in the widget only if not a lock screen widget
        final Bundle newOptions = widgetManager.getAppWidgetOptions(appWidgetId);
        if (newOptions != null &&
                newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
                != AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
            final Intent showClock = new Intent(HandleDeskClockApiCalls.ACTION_SHOW_CLOCK)
                    .putExtra(HandleDeskClockApiCalls.EXTRA_FROM_WIDGET, true);
            final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, showClock, 0);
            widget.setOnClickPendingIntent(R.id.digital_appwidget, pendingIntent);
        }

        // Setup formats and font sizes.
        refreshDate(context, widget, ratio);
        refreshAlarm(context, widget, ratio);
        WidgetUtils.setClockSize(context, widget, ratio);
        WidgetUtils.setTimeFormat(context, widget, 0.4f /* amPmRatio */, R.id.clock);

        // Set up R.id.digital_appwidget_listview to use a remote views adapter
        // That remote views adapter connects to a RemoteViewsService through intent.
        final Intent intent = new Intent(context, DigitalAppWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        widget.setRemoteAdapter(R.id.digital_appwidget_listview, intent);

        // Set up the click on any world clock to start the Cities Activity
        //TODO: Should this be in the options guard above?
        final Intent selectCitiesIntent = new Intent(context, CitySelectionActivity.class);
        widget.setPendingIntentTemplate(R.id.digital_appwidget_listview,
                PendingIntent.getActivity(context, 0, selectCitiesIntent, 0));

        // Refresh the widget
        widgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.digital_appwidget_listview);
        widgetManager.updateAppWidget(appWidgetId, widget);
    }

    private void refreshDate(Context context, RemoteViews widget, float ratio) {
        if (ratio < 1) {
            // The time text normally has a negative bottom margin to reduce the space between the
            // time and the date. When scaling down they overlap, so give the date a positive
            // top padding.
            final float padding = (1 - ratio) *
                    -context.getResources().getDimension(R.dimen.bottom_text_spacing_digital);
            widget.setViewPadding(R.id.date_and_alarm, 0, (int) padding, 0, 0);
        }

        // Set today's date format.
        final Locale locale = Locale.getDefault();
        final String skeleton = context.getString(R.string.abbrev_wday_month_day_no_year);
        final CharSequence timeFormat = DateFormat.getBestDateTimePattern(locale, skeleton);
        widget.setCharSequence(R.id.date, "setFormat12Hour", timeFormat);
        widget.setCharSequence(R.id.date, "setFormat24Hour", timeFormat);
        final float fontSize = context.getResources().getDimension(R.dimen.widget_label_font_size);
        widget.setTextViewTextSize(R.id.date, COMPLEX_UNIT_PX, fontSize * ratio);
    }

    private void refreshAlarm(Context context, RemoteViews widget, float ratio) {
        final String nextAlarm = Utils.getNextAlarm(context);
        if (TextUtils.isEmpty(nextAlarm)) {
            widget.setViewVisibility(R.id.nextAlarm, View.GONE);
            if (DigitalAppWidgetService.LOGGING) {
                LOGGER.v("DigitalWidget hides next alarm string");
            }
        } else  {
            final Resources resources = context.getResources();
            final float fontSize = resources.getDimension(R.dimen.widget_label_font_size);
            widget.setTextViewTextSize(R.id.nextAlarm, COMPLEX_UNIT_PX, fontSize * ratio);

            final int alarmIconResId;
            if (ratio < .72f) {
                alarmIconResId = R.drawable.ic_alarm_small_12dp;
            } else if (ratio < .95f) {
                alarmIconResId = R.drawable.ic_alarm_small_18dp;
            } else {
                alarmIconResId = R.drawable.ic_alarm_small_24dp;
            }
            widget.setTextViewCompoundDrawablesRelative(R.id.nextAlarm, alarmIconResId, 0, 0, 0);
            widget.setTextViewText(R.id.nextAlarm, nextAlarm);
            widget.setViewVisibility(R.id.nextAlarm, View.VISIBLE);
            if (DigitalAppWidgetService.LOGGING) {
                LOGGER.v("DigitalWidget sets next alarm string to " + nextAlarm);
            }
        }
    }

    /**
     * Remove the existing quarter-hour callback if it is not needed (no selected cities exist).
     * Add the quarter-hour callback if it is needed (selected cities exist).
     */
    private void updateQuarterHourCallback(Context context) {
        if (DataModel.getDataModel().getSelectedCities().isEmpty()) {
            // Remove the existing quarter-hour callback.
            removeQuarterHourCallback(context);
            return;
        }

        // Schedule the next quarter-hour callback; at least one city is displayed.
        final PendingIntent pi =
                PendingIntent.getBroadcast(context, 0, QUARTER_HOUR_INTENT, FLAG_UPDATE_CURRENT);
        final long onQuarterHour = Utils.getAlarmOnQuarterHour();
        getAlarmManager(context).setExact(RTC, onQuarterHour, pi);
    }

    /**
     * Remove the existing quarter-hour callback.
     */
    private void removeQuarterHourCallback(Context context) {
        final PendingIntent pi =
                PendingIntent.getBroadcast(context, 0, QUARTER_HOUR_INTENT, FLAG_NO_CREATE);
        if (pi != null) {
            getAlarmManager(context).cancel(pi);
            pi.cancel();
        }
    }

    /**
     * @return the ComponentName unique to DigitalAppWidgetProvider
     */
    private ComponentName getComponentName(Context context) {
        if (mComponentName == null) {
            mComponentName = new ComponentName(context, getClass());
        }
        return mComponentName;
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
}