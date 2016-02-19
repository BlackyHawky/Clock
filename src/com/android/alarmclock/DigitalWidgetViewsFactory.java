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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.android.deskclock.R;
import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.android.deskclock.Utils.enforceMainLooper;
import static java.util.Calendar.DAY_OF_WEEK;

public class DigitalWidgetViewsFactory implements RemoteViewsFactory {

    private static final String TAG = "DigWidgetViewsFactory";

    private final Intent mFillInIntent = new Intent();

    private final Context mContext;
    private final Resources mResources;
    private final float mFontSize;
    private final float mFont24Size;
    private final int mWidgetId;
    private float mFontScale = 1;

    private City mHomeCity;
    private List<City> mCities;
    private boolean mShowHomeClock;

    public DigitalWidgetViewsFactory(Context context, Intent intent) {
        mContext = context;
        mResources = context.getResources();
        mFontSize = mResources.getDimension(R.dimen.widget_medium_font_size);
        mFont24Size = mResources.getDimension(R.dimen.widget_24_medium_font_size);
        mWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        if (DigitalAppWidgetService.LOGGING) {
            Log.i(TAG, "DigitalWidget onCreate " + mWidgetId);
        }
    }

    @Override
    public void onDestroy() {
        if (DigitalAppWidgetService.LOGGING) {
            Log.i(TAG, "DigitalWidget onDestroy " + mWidgetId);
        }
    }

    /**
     * <p>Synchronized to ensure single-threaded reading/writing of mCities, mHomeCity and
     * mShowHomeClock.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public synchronized int getCount() {
        if (!WidgetUtils.showList(mContext, mWidgetId, mFontScale)) {
            return 0;
        }

        final int homeClockCount = mShowHomeClock ? 1 : 0;
        final int worldClockCount = mCities.size();
        final double totalClockCount = homeClockCount + worldClockCount;

        // number of clocks / 2 clocks per row
        return (int) Math.ceil(totalClockCount / 2);
    }

    /**
     * <p>Synchronized to ensure single-threaded reading/writing of mCities, mHomeCity and
     * mShowHomeClock.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public synchronized RemoteViews getViewAt(int position) {
        final int homeClockOffset = mShowHomeClock ? -1 : 0;
        final int leftIndex = position * 2 + homeClockOffset;
        final int rightIndex = leftIndex + 1;

        final City left = leftIndex == -1 ? mHomeCity :
                (leftIndex < mCities.size() ? mCities.get(leftIndex) : null);
        final City right = rightIndex < mCities.size() ? mCities.get(rightIndex) : null;

        final RemoteViews clock =
                new RemoteViews(mContext.getPackageName(), R.layout.world_clock_remote_list_item);

        // Show the left clock if one exists.
        if (left != null) {
            update(clock, left, R.id.left_clock, R.id.city_name_left, R.id.city_day_left);
        } else {
            hide(clock, R.id.left_clock, R.id.city_name_left, R.id.city_day_left);
        }

        // Show the right clock if one exists.
        if (right != null) {
            update(clock, right, R.id.right_clock, R.id.city_name_right, R.id.city_day_right);
        } else {
            hide(clock, R.id.right_clock, R.id.city_name_right, R.id.city_day_right);
        }

        // Hide last spacer in last row; show for all others.
        final boolean lastRow = position == getCount() - 1;
        clock.setViewVisibility(R.id.city_spacer, lastRow ? View.GONE : View.VISIBLE);

        clock.setOnClickFillInIntent(R.id.widget_item, mFillInIntent);
        return clock;
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
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * <p>Synchronized to ensure single-threaded reading/writing of mCities, mHomeCity and
     * mShowHomeClock.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public synchronized void onDataSetChanged() {
        // Fetch the data on the main Looper.
        final RefreshRunnable refreshRunnable = new RefreshRunnable();
        DataModel.getDataModel().run(refreshRunnable);

        // Store the data in local variables.
        mFontScale = WidgetUtils.getScaleRatio(mContext, null, mWidgetId);
        mHomeCity = refreshRunnable.mHomeCity;
        mCities = refreshRunnable.mCities;
        mShowHomeClock = refreshRunnable.mShowHomeClock;
    }

    private void update(RemoteViews clock, City city, int clockId, int labelId, int dayId) {
        WidgetUtils.setTimeFormat(mContext, clock, true /* showAmPm */, clockId);

        final float fontSize = DateFormat.is24HourFormat(mContext) ? mFont24Size : mFontSize;
        clock.setTextViewTextSize(clockId, TypedValue.COMPLEX_UNIT_PX, fontSize * mFontScale);
        clock.setString(clockId, "setTimeZone", city.getTimeZoneId());
        clock.setTextViewText(labelId, city.getName());

        // Compute if the city week day matches the weekday of the current timezone.
        final Calendar localCal = Calendar.getInstance(TimeZone.getDefault());
        final Calendar cityCal = Calendar.getInstance(city.getTimeZone());
        final boolean displayDayOfWeek = localCal.get(DAY_OF_WEEK) != cityCal.get(DAY_OF_WEEK);

        // Bind the week day display.
        if (displayDayOfWeek) {
            final Locale locale = Locale.getDefault();
            final String weekday = cityCal.getDisplayName(DAY_OF_WEEK, Calendar.SHORT, locale);
            final String slashDay = mContext.getString(R.string.world_day_of_week_label, weekday);
            clock.setTextViewText(dayId, slashDay);
        }

        clock.setViewVisibility(dayId, displayDayOfWeek ? View.VISIBLE : View.GONE);
        clock.setViewVisibility(clockId, View.VISIBLE);
        clock.setViewVisibility(labelId, View.VISIBLE);
    }

    private void hide(RemoteViews clock, int clockId, int labelId, int dayId) {
        clock.setViewVisibility(dayId, View.INVISIBLE);
        clock.setViewVisibility(clockId, View.INVISIBLE);
        clock.setViewVisibility(labelId, View.INVISIBLE);
    }

    /**
     * This Runnable fetches data for this factory on the main thread to ensure all DataModel reads
     * occur on the main thread.
     */
    private static final class RefreshRunnable implements Runnable {

        private City mHomeCity;
        private List<City> mCities;
        private boolean mShowHomeClock;

        @Override
        public void run() {
            enforceMainLooper();

            mHomeCity = DataModel.getDataModel().getHomeCity();
            mCities = new ArrayList<>(DataModel.getDataModel().getSelectedCities());
            mShowHomeClock = DataModel.getDataModel().getShowHomeClock();
        }
    }
}