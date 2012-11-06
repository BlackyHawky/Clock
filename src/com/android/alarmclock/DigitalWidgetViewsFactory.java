/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.android.deskclock.Alarms;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.worldclock.CityObj;
import com.android.deskclock.worldclock.WorldClockAdapter;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class DigitalWidgetViewsFactory extends BroadcastReceiver implements RemoteViewsFactory {
    private static final String TAG = "DigitalWidgetViewsFactory";

    private Context mContext;
    private int mId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private RemoteWorldClockAdapter mAdapter;
    private String mDateFormat;
    private Calendar mCalendar;
    private boolean mReloadCitiesList = true;
    private float mFontScale = 1;

    // An adapter to provide the view for the list of cities in the world clock.
    private class RemoteWorldClockAdapter extends WorldClockAdapter {
        private final float mFontSize;

        public RemoteWorldClockAdapter(Context context) {
            super(context);
            mFontSize = context.getResources().getDimension(R.dimen.widget_medium_font_size);
        }

        public RemoteViews getViewAt(int position) {
            // There are 2 cities per item
            int index = position * 2;
            if (index < 0 || index >= mCitiesList.length) {
                return null;
            }

            RemoteViews views = new RemoteViews(
                    mContext.getPackageName(), R.layout.world_clock_remote_list_item);

            // Always how the left clock
            updateView(views, (CityObj) mCitiesList[index], R.id.timeDisplayHoursLeft,
                    R.id.timeDisplayMinutesLeft, R.id.city_name_left, R.id.city_day_left);
            // Show the right clock if any, make it invisible if there is no clock on the right
            // to keep the left view on the left.
            if (index + 1 < mCitiesList.length) {
                updateView(views, (CityObj) mCitiesList[index + 1], R.id.timeDisplayHoursRight,
                        R.id.timeDisplayMinutesRight, R.id.city_name_right, R.id.city_day_right);
            } else {
                hideView(views, R.id.timeDisplayHoursRight, R.id.timeDisplayMinutesRight,
                        R.id.city_name_right, R.id.city_day_right);
            }
            return views;
        }

        private void updateView(RemoteViews clock, CityObj cityObj, int hoursId, int minutesId,
                int labelId, int dayId) {
            final Calendar now = Calendar.getInstance();
            now.setTimeInMillis(System.currentTimeMillis());
            int myDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
            now.setTimeZone(TimeZone.getTimeZone(cityObj.mTimeZone));
            int cityDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
            if (Alarms.get24HourMode(mContext)) {
                clock.setTextViewText(hoursId, DateFormat.format(Utils.HOURS_24, now));
            } else {
                clock.setTextViewText(hoursId, DateFormat.format(Utils.HOURS, now));
            }
            clock.setTextViewText(minutesId, DateFormat.format(Utils.MINUTES, now));
            clock.setTextViewText(labelId, cityObj.mCityName);
            if (myDayOfWeek != cityDayOfWeek) {
                clock.setTextViewText(dayId, mContext.getString(
                        R.string.world_day_of_week_label, now.getDisplayName(
                                Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())));
                clock.setViewVisibility(dayId, View.VISIBLE);
            } else {
                clock.setViewVisibility(dayId, View.GONE);
            }

            clock.setTextViewTextSize(hoursId, TypedValue.COMPLEX_UNIT_PX, mFontSize * mFontScale);
            clock.setTextViewTextSize(
                    minutesId, TypedValue.COMPLEX_UNIT_PX, mFontSize * mFontScale);

            clock.setViewVisibility(hoursId, View.VISIBLE);
            clock.setViewVisibility(minutesId, View.VISIBLE);
            clock.setViewVisibility(labelId, View.VISIBLE);
        }

        private void hideView(
                RemoteViews clock, int hoursId, int minutesId, int labelId, int dayId) {
            clock.setViewVisibility(hoursId, View.INVISIBLE);
            clock.setViewVisibility(minutesId, View.INVISIBLE);
            clock.setViewVisibility(labelId, View.INVISIBLE);
            clock.setViewVisibility(dayId, View.INVISIBLE);
        }
    }

    public DigitalWidgetViewsFactory(Context c, Intent intent) {
        mContext = c;
        mId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        mAdapter = new RemoteWorldClockAdapter(c);
    }

    public DigitalWidgetViewsFactory() {
    }

    @Override
    public int getCount() {
        return mAdapter.getCount();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews v = mAdapter.getViewAt(position);
        if (v != null) {
            Intent fillInIntent = new Intent();
            v.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
        }
        return v;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onCreate() {
        // Do intent listening registration here since doing it in the manifest creates a new
        // new factory
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction("com.android.deskclock.NEXT_ALARM_TIME_SET");
        filter.addAction("com.android.deskclock.worldclock.update");
        mContext.registerReceiver(this, filter);
    }

    @Override
    public void onDataSetChanged() {
        if (mReloadCitiesList) {
            mAdapter.loadData(mContext);
            mReloadCitiesList = false;
        }
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(mContext);
        mFontScale = DigitalAppWidgetProvider.getScaleRatio(mContext, null, mId);
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(this);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (mId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }

        mContext = context;
        String action = intent.getAction();
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        if (action.equals("com.android.deskclock.NEXT_ALARM_TIME_SET")) {
            // Update the next alarm text view
            RemoteViews widget =
                    new RemoteViews(context.getPackageName(), R.layout.digital_appwidget);
            refreshAlarm(context, widget);
            widgetManager.partiallyUpdateAppWidget(mId, widget);
        } else if (action.equals("com.android.deskclock.worldclock.update")) {
            // Reload the list of cities
            mReloadCitiesList = true;
            widgetManager.notifyAppWidgetViewDataChanged(mId, R.id.digital_appwidget_listview);

        } else {
            // For any time change or locale change, refresh all
            widgetManager.notifyAppWidgetViewDataChanged(mId, R.id.digital_appwidget_listview);
            RemoteViews widget =
                    new RemoteViews(context.getPackageName(), R.layout.digital_appwidget);
            if (mDateFormat == null) {
                mDateFormat =
                        context.getResources().getString(R.string.abbrev_wday_month_day_no_year);
            }
            setTime(context, widget);
            refreshAlarm(context, widget);
            updateDateRemoteView(mDateFormat, widget);
            widgetManager.partiallyUpdateAppWidget(mId, widget);
        }
    }

    private void refreshAlarm(Context c, RemoteViews widget) {
        String nextAlarm = Settings.System.getString(c.getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED);
        if (!TextUtils.isEmpty(nextAlarm)) {
            widget.setTextViewText(R.id.nextAlarm,
                    c.getString(R.string.control_set_alarm_with_existing, nextAlarm));
            widget.setViewVisibility(R.id.nextAlarm, View.VISIBLE);
        } else  {
            widget.setViewVisibility(R.id.nextAlarm, View.GONE);
        }
    }

    private void updateDateRemoteView(
            String dateFormat, RemoteViews clock) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());

        CharSequence newDate = DateFormat.format(dateFormat, cal);
        clock.setTextViewText(R.id.date, newDate);
    }
    private void setTime(Context c, RemoteViews clock) {
        if (mCalendar == null) {
            mCalendar = Calendar.getInstance();
        }
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        if (Alarms.get24HourMode(c)) {
            clock.setTextViewText(
                    R.id.timeDisplayHours, DateFormat.format(Utils.HOURS_24, mCalendar));
        } else {
            clock.setTextViewText(R.id.timeDisplayHours, DateFormat.format(Utils.HOURS, mCalendar));
        }
        clock.setTextViewText(R.id.timeDisplayMinutes, DateFormat.format(Utils.MINUTES, mCalendar));
    }
}

