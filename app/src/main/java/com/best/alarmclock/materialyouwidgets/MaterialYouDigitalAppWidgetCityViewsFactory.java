/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.alarmclock.materialyouwidgets;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static java.util.Calendar.DAY_OF_WEEK;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.best.alarmclock.WidgetUtils;
import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This factory produces entries in the world cities list view displayed at the bottom of the
 * material you digital widget. Each row is comprised of two world cities located side-by-side.
 */
public class MaterialYouDigitalAppWidgetCityViewsFactory implements RemoteViewsFactory {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("MYDgtlWdgtViewsFact");

    private final Intent mFillInIntent = new Intent();

    private final Context mContext;
    private final float m12HourFontSize;
    private final float m24HourFontSize;
    private final float mCityAndDayFontSize;
    private final int mWidgetId;
    private float mFontScale = 1;

    private City mHomeCity;
    private boolean mShowHomeClock;
    private List<City> mCities = Collections.emptyList();

    public MaterialYouDigitalAppWidgetCityViewsFactory(Context context, Intent intent) {
        mContext = context;
        mWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
        final boolean isTablet = ThemeUtils.isTablet();

        m12HourFontSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                isTablet ? 52 : 32, context.getResources().getDisplayMetrics());
        m24HourFontSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                isTablet ? 65 : 40, context.getResources().getDisplayMetrics());
        mCityAndDayFontSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                isTablet ? 20 : 12, context.getResources().getDisplayMetrics());
    }

    @Override
    public void onCreate() {
        LOGGER.i("DigitalAppWidgetMaterialYouCityViewsFactory onCreate " + mWidgetId);
    }

    @Override
    public void onDestroy() {
        LOGGER.i("DigitalAppWidgetMaterialYouCityViewsFactory onDestroy " + mWidgetId);
    }

    /**
     * <p>Synchronized to ensure single-threaded reading/writing of mCities, mHomeCity and
     * mShowHomeClock.</p>
     * <p>
     * {@inheritDoc}
     */
    @Override
    public synchronized int getCount() {
        final int homeClockCount = mShowHomeClock ? 1 : 0;
        final int worldClockCount = mCities.size();
        final double totalClockCount = homeClockCount + worldClockCount;

        // number of clocks / 2 clocks per row
        return (int) Math.ceil(totalClockCount / 2);
    }

    /**
     * <p>Synchronized to ensure single-threaded reading/writing of mCities, mHomeCity and
     * mShowHomeClock.</p>
     * <p>
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

        final RemoteViews rv =
                new RemoteViews(mContext.getPackageName(), R.layout.world_clock_material_you_remote_list_item);

        // Show the left clock if one exists.
        if (left != null) {
            update(rv, left, R.id.leftClock, R.id.cityNameLeft, R.id.cityDayLeft);
        } else {
            hide(rv, R.id.leftClock, R.id.cityNameLeft, R.id.cityDayLeft);
        }

        // Show the right clock if one exists.
        if (right != null) {
            update(rv, right, R.id.rightClock, R.id.cityNameRight, R.id.cityDayRight);
        } else {
            hide(rv, R.id.rightClock, R.id.cityNameRight, R.id.cityDayRight);
        }

        // Hide last spacer in last row; show for all others.
        final boolean lastRow = position == getCount() - 1;
        rv.setViewVisibility(R.id.citySpacer, lastRow ? View.GONE : View.VISIBLE);

        rv.setOnClickFillInIntent(R.id.widgetItem, mFillInIntent);
        return rv;
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
        return false;
    }

    /**
     * <p>Synchronized to ensure single-threaded reading/writing of mCities, mHomeCity and
     * mShowHomeClock.</p>
     * <p>
     * {@inheritDoc}
     */
    @Override
    public synchronized void onDataSetChanged() {
        // Fetch the data on the main Looper.
        final RefreshRunnable refreshRunnable = new RefreshRunnable(mContext);
        DataModel.getDataModel().run(refreshRunnable);

        // Store the data in local variables.
        mHomeCity = refreshRunnable.mHomeCity;
        mCities = refreshRunnable.mCities;
        mShowHomeClock = refreshRunnable.mShowHomeClock;
        mFontScale = WidgetUtils.getScaleRatio(mContext, null, mWidgetId, mCities.size());
    }

    private void update(RemoteViews rv, City city, int clockId, int labelId, int dayId) {
        final SharedPreferences prefs = getDefaultSharedPreferences(mContext);

        rv.setCharSequence(clockId, "setFormat12Hour",
                ClockUtils.get12ModeFormat(mContext, 0.4f, false)
        );
        rv.setCharSequence(clockId, "setFormat24Hour",
                ClockUtils.get24ModeFormat(mContext, false)
        );

        final boolean is24HourFormat = DateFormat.is24HourFormat(mContext);
        final float fontSize = is24HourFormat ? m24HourFontSize : m12HourFontSize;

        rv.setTextViewTextSize(clockId, TypedValue.COMPLEX_UNIT_PX, fontSize * mFontScale);
        rv.setString(clockId, "setTimeZone", city.getTimeZone().getID());

        rv.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_PX, mCityAndDayFontSize * mFontScale);
        rv.setTextViewText(labelId, city.getName());

        // Compute if the city week day matches the weekday of the current timezone.
        final Calendar localCal = Calendar.getInstance(TimeZone.getDefault());
        final Calendar cityCal = Calendar.getInstance(city.getTimeZone());
        final boolean displayDayOfWeek = localCal.get(DAY_OF_WEEK) != cityCal.get(DAY_OF_WEEK);

        // Bind the week day display.
        if (displayDayOfWeek) {
            final Locale locale = Locale.getDefault();
            final String weekday = cityCal.getDisplayName(DAY_OF_WEEK, Calendar.SHORT, locale);
            final String slashDay = mContext.getString(R.string.world_day_of_week_label, weekday);
            rv.setTextViewTextSize(dayId, TypedValue.COMPLEX_UNIT_PX, mCityAndDayFontSize * mFontScale);
            rv.setTextViewText(dayId, slashDay);
        }

        rv.setViewVisibility(dayId, displayDayOfWeek ? View.VISIBLE : View.GONE);
        rv.setViewVisibility(clockId, View.VISIBLE);
        rv.setViewVisibility(labelId, View.VISIBLE);

        final int customCityClockColor = WidgetDAO.getMaterialYouDigitalWidgetCustomCityClockColor(prefs);
        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultCityClockColor(prefs)) {
            rv.setTextColor(clockId, mContext.getColor(R.color.digital_widget_time_color));
        } else {
            rv.setTextColor(clockId, customCityClockColor);
        }

        final int customCityNameColor = WidgetDAO.getMaterialYouDigitalWidgetCustomCityNameColor(prefs);
        if (WidgetDAO.isMaterialYouDigitalWidgetDefaultCityNameColor(prefs)) {
            rv.setTextColor(labelId, mContext.getColor(R.color.widget_text_color));
            rv.setTextColor(dayId, mContext.getColor(R.color.widget_text_color));
        } else {
            rv.setTextColor(labelId, customCityNameColor);
            rv.setTextColor(dayId, customCityNameColor);
        }
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

        private final Context mContext;
        private City mHomeCity;
        private List<City> mCities;
        private boolean mShowHomeClock;

        public RefreshRunnable(Context context) {
            this.mContext = context;
        }

        @Override
        public void run() {
            mHomeCity = DataModel.getDataModel().getHomeCity();
            mCities = new ArrayList<>(DataModel.getDataModel().getSelectedCities());
            mShowHomeClock = SettingsDAO.getShowHomeClock(mContext, getDefaultSharedPreferences(mContext));
        }
    }
}
