// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import static java.util.Calendar.DAY_OF_WEEK;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.WidgetUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Abstract base class for RemoteViewsFactory implementations used in digital clock widgets
 * to render a list of world cities with their respective local times and day indicators.
 * <p>
 * This class provides a reusable framework for generating RemoteViews for each item in the
 * widget's city list. It encapsulates common logic for layout inflation, view binding,
 * and color configuration, while allowing subclasses to define style-specific resources
 * and behaviors.
 * </p>
 */
public abstract class BaseDigitalAppWidgetCityViewsFactory  implements RemoteViewsFactory {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("DgtlWdgtViewsFact");

    protected abstract int getLayoutId();
    protected abstract int getCityViewId();

    protected abstract int getLeftClockWithShadowId();
    protected abstract int getLeftClockWithoutShadowId();
    protected abstract int getLeftCityNameWithShadowId();
    protected abstract int getLeftCityNameWithoutShadowId();
    protected abstract int getLeftCityDayWithShadowId();
    protected abstract int getLeftCityDayWithoutShadowId();

    protected abstract int getRightClockWithShadowId();
    protected abstract int getRightClockWithoutShadowId();
    protected abstract int getRightCityNameWithShadowId();
    protected abstract int getRightCityNameWithoutShadowId();
    protected abstract int getRightCityDayWithShadowId();
    protected abstract int getRightCityDayWithoutShadowId();

    protected abstract int getCitySpacerId();

    protected abstract boolean isTextUppercaseDisplayed(SharedPreferences prefs);
    protected abstract boolean isTextShadowDisplayed(SharedPreferences prefs);

    protected abstract void configureColors(RemoteViews rv, Context context, SharedPreferences prefs,
                                            int clockId, int labelId, int dayId);

    private final Intent mFillInIntent = new Intent();

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final float m12HourFontSize;
    private final float m24HourFontSize;
    private final float mCityAndDayFontSize;
    private final int mWidgetId;
    private float mFontScale = 1;

    private City mHomeCity;
    private boolean mShowHomeClock;
    private List<City> mCities = Collections.emptyList();

    protected BaseDigitalAppWidgetCityViewsFactory(Context context, Intent intent) {
        mContext = context;
        mPrefs = getDefaultSharedPreferences(mContext);
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
        LOGGER.i("DigitalAppWidgetCityViewsFactory onCreate " + mWidgetId);
    }

    @Override
    public void onDestroy() {
        LOGGER.i("DigitalAppWidgetCityViewsFactory onDestroy " + mWidgetId);
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

        final RemoteViews rv = new RemoteViews(mContext.getPackageName(), getLayoutId());

        // Show the left clock if one exists.
        if (left != null) {
            update(rv, left, getLeftClockWithShadowId(), getLeftClockWithoutShadowId(),
                    getLeftCityNameWithShadowId(), getLeftCityNameWithoutShadowId(),
                    getLeftCityDayWithShadowId(), getLeftCityDayWithoutShadowId());
        } else {
            hide(rv, getLeftClockWithShadowId(), getLeftClockWithoutShadowId(),
                    getLeftCityNameWithShadowId(), getLeftCityNameWithoutShadowId(),
                    getLeftCityDayWithShadowId(), getLeftCityDayWithoutShadowId());
        }

        // Show the right clock if one exists.
        if (right != null) {
            update(rv, right, getRightClockWithShadowId(), getRightClockWithoutShadowId(),
                    getRightCityNameWithShadowId(), getRightCityNameWithoutShadowId(),
                    getRightCityDayWithShadowId(), getRightCityDayWithoutShadowId());
        } else {
            hide(rv, getRightClockWithShadowId(), getRightClockWithoutShadowId(),
                    getRightCityNameWithShadowId(), getRightCityNameWithoutShadowId(),
                    getRightCityDayWithShadowId(), getRightCityDayWithoutShadowId());
        }

        // Hide last spacer in last row; show for all others.
        final boolean lastRow = position == getCount() - 1;
        rv.setViewVisibility(getCitySpacerId(), lastRow ? GONE : VISIBLE);

        rv.setOnClickFillInIntent(getCityViewId(), mFillInIntent);
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

    private void update(RemoteViews rv, City city, int clockWithShadowId, int clockWithoutShadowId,
                        int nameWithShadowId, int nameWithoutShadowId,
                        int dayWithShadowId, int dayWithoutShadowId) {

        final boolean shadowEnabled = isTextShadowDisplayed(mPrefs);
        final boolean isTextUppercase = isTextUppercaseDisplayed(mPrefs);

        // Selection of active and inactive IDs
        int clockId = shadowEnabled ? clockWithShadowId : clockWithoutShadowId;
        int clockOffId = shadowEnabled ? clockWithoutShadowId : clockWithShadowId;

        int labelId = shadowEnabled ? nameWithShadowId : nameWithoutShadowId;
        int labelOffId = shadowEnabled ? nameWithoutShadowId : nameWithShadowId;

        int dayId = shadowEnabled ? dayWithShadowId : dayWithoutShadowId;
        int dayOffId = shadowEnabled ? dayWithoutShadowId : dayWithShadowId;

        // Hide inactive variants
        rv.setViewVisibility(clockOffId, GONE);
        rv.setViewVisibility(labelOffId, GONE);
        rv.setViewVisibility(dayOffId, GONE);

        // Make active variants visible
        rv.setViewVisibility(clockId, VISIBLE);
        rv.setViewVisibility(labelId, VISIBLE);

        if (DataModel.getDataModel().is24HourFormat()) {
            rv.setCharSequence(clockId, "setFormat24Hour",
                    ClockUtils.get24ModeFormat(mContext, false));
        } else {
            rv.setCharSequence(clockId, "setFormat12Hour",
                    ClockUtils.get12ModeFormat(mContext, 0.4f, false, false));
        }

        final boolean is24HourFormat = DateFormat.is24HourFormat(mContext);
        final float fontSize = is24HourFormat ? m24HourFontSize : m12HourFontSize;

        rv.setTextViewTextSize(clockId, TypedValue.COMPLEX_UNIT_PX, fontSize * mFontScale);
        rv.setString(clockId, "setTimeZone", city.getTimeZone().getID());

        rv.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_PX, mCityAndDayFontSize * mFontScale);
        if (isTextUppercase) {
            rv.setTextViewText(labelId, city.getName().toUpperCase());
        } else {
            rv.setTextViewText(labelId, city.getName());
        }

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
            if (isTextUppercase) {
                rv.setTextViewText(dayId, slashDay.toUpperCase());
            } else {
                rv.setTextViewText(dayId, slashDay);
            }
        }

        rv.setViewVisibility(dayId, displayDayOfWeek ? VISIBLE : GONE);

        configureColors(rv, mContext, mPrefs, clockId, labelId, dayId);
    }

    private void hide(RemoteViews clock, int clockWithShadowId, int clockWithoutShadowId,
                      int nameWithShadowId, int nameWithoutShadowId,
                      int dayWithShadowId, int dayWithoutShadowId) {

        // Hide all variants to avoid any visual residue
        clock.setViewVisibility(clockWithShadowId, GONE);
        clock.setViewVisibility(clockWithoutShadowId, GONE);
        clock.setViewVisibility(nameWithShadowId, GONE);
        clock.setViewVisibility(nameWithoutShadowId, GONE);
        clock.setViewVisibility(dayWithShadowId, GONE);
        clock.setViewVisibility(dayWithoutShadowId, GONE);
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
