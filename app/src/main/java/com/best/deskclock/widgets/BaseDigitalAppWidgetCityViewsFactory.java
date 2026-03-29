// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CITY_NOTE;
import static com.best.deskclock.utils.WidgetUtils.METHOD_SET_TIME_ZONE;
import static java.util.Calendar.DAY_OF_WEEK;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
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
public abstract class BaseDigitalAppWidgetCityViewsFactory implements RemoteViewsFactory {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("DgtlWdgtViewsFact");

    protected abstract int getLayoutId();

    protected abstract int getCityViewId();

    protected abstract int getLeftClockWithShadowId();
    protected abstract int getLeftClockForCustomColorId();
    protected abstract int getLeftClockNoShadowId();
    protected abstract int getLeftClockNoShadowForCustomColorId();

    protected abstract int getLeftCityNameWithShadowId();
    protected abstract int getLeftCityNameForCustomColorId();
    protected abstract int getLeftCityNameNoShadowId();
    protected abstract int getLeftCityNameNoShadowForCustomColorId();

    protected abstract int getLeftCityDayWithShadowId();
    protected abstract int getLeftCityDayForCustomColorId();
    protected abstract int getLeftCityDayNoShadowId();
    protected abstract int getLeftCityDayNoShadowForCustomColorId();

    protected abstract int getLeftCityNoteWithShadowId();
    protected abstract int getLeftCityNoteForCustomColorId();
    protected abstract int getLeftCityNoteNoShadowId();
    protected abstract int getLeftCityNoteNoShadowForCustomColorId();

    protected abstract int getRightClockWithShadowId();
    protected abstract int getRightClockForCustomColorId();
    protected abstract int getRightClockNoShadowId();
    protected abstract int getRightClockNoShadowForCustomColorId();

    protected abstract int getRightCityNameWithShadowId();
    protected abstract int getRightCityNameForCustomColorId();
    protected abstract int getRightCityNameNoShadowId();
    protected abstract int getRightCityNameNoShadowForCustomColorId();

    protected abstract int getRightCityDayWithShadowId();
    protected abstract int getRightCityDayForCustomColorId();
    protected abstract int getRightCityDayNoShadowId();
    protected abstract int getRightCityDayNoShadowForCustomColorId();

    protected abstract int getRightCityNoteWithShadowId();
    protected abstract int getRightCityNoteForCustomColorId();
    protected abstract int getRightCityNoteNoShadowId();
    protected abstract int getRightCityNoteNoShadowForCustomColorId();

    protected abstract int getCitySpacerId();

    protected abstract boolean isTextUppercaseDisplayed(SharedPreferences prefs);
    protected abstract boolean isTextShadowDisplayed(SharedPreferences prefs);

    protected abstract boolean isDefaultCityClockColor(SharedPreferences prefs);
    protected abstract int getCityClockColor(SharedPreferences prefs);

    protected abstract boolean isDefaultCityNameColor(SharedPreferences prefs);
    protected abstract int getCityNameColor(SharedPreferences prefs);

    protected abstract boolean isDefaultCityNoteColor(SharedPreferences prefs);
    protected abstract int getCityNoteColor(SharedPreferences prefs);

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
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        m12HourFontSize = dpToPx(isTablet ? 52 : 32, displayMetrics);
        m24HourFontSize = dpToPx(isTablet ? 65 : 40, displayMetrics);
        mCityAndDayFontSize = dpToPx(isTablet ? 20 : 14, displayMetrics);
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
            update(rv, left, getLeftClockWithShadowId(), getLeftClockNoShadowId(),
                getLeftClockForCustomColorId(), getLeftClockNoShadowForCustomColorId(),
                getLeftCityNameWithShadowId(), getLeftCityNameNoShadowId(),
                getLeftCityNameForCustomColorId(), getLeftCityNameNoShadowForCustomColorId(),
                getLeftCityDayWithShadowId(), getLeftCityDayNoShadowId(),
                getLeftCityDayForCustomColorId(), getLeftCityDayNoShadowForCustomColorId(),
                getLeftCityNoteWithShadowId(), getLeftCityNoteNoShadowId(),
                getLeftCityNoteForCustomColorId(), getLeftCityNoteNoShadowForCustomColorId(),
                isDefaultCityClockColor(mPrefs), getCityClockColor(mPrefs),
                isDefaultCityNameColor(mPrefs), getCityNameColor(mPrefs),
                isDefaultCityNoteColor(mPrefs), getCityNoteColor(mPrefs));
        } else {
            hide(rv, getLeftClockWithShadowId(), getLeftClockNoShadowId(),
                getLeftClockForCustomColorId(), getLeftClockNoShadowForCustomColorId(),
                getLeftCityNameWithShadowId(), getLeftCityNameNoShadowId(),
                getLeftCityNameForCustomColorId(), getLeftCityNameNoShadowForCustomColorId(),
                getLeftCityDayWithShadowId(), getLeftCityDayNoShadowId(),
                getLeftCityDayForCustomColorId(), getLeftCityDayNoShadowForCustomColorId(),
                getLeftCityNoteWithShadowId(), getLeftCityNoteNoShadowId(),
                getLeftCityNoteForCustomColorId(), getLeftCityNoteNoShadowForCustomColorId());
        }

        // Show the right clock if one exists.
        if (right != null) {
            update(rv, right, getRightClockWithShadowId(), getRightClockNoShadowId(),
                getRightClockForCustomColorId(), getRightClockNoShadowForCustomColorId(),
                getRightCityNameWithShadowId(), getRightCityNameNoShadowId(),
                getRightCityNameForCustomColorId(), getRightCityNameNoShadowForCustomColorId(),
                getRightCityDayWithShadowId(), getRightCityDayNoShadowId(),
                getRightCityDayForCustomColorId(), getRightCityDayNoShadowForCustomColorId(),
                getRightCityNoteWithShadowId(), getRightCityNoteNoShadowId(),
                getRightCityNoteForCustomColorId(), getRightCityNoteNoShadowForCustomColorId(),
                isDefaultCityClockColor(mPrefs), getCityClockColor(mPrefs),
                isDefaultCityNameColor(mPrefs), getCityNameColor(mPrefs),
                isDefaultCityNoteColor(mPrefs), getCityNoteColor(mPrefs));
        } else {
            hide(rv, getRightClockWithShadowId(), getRightClockNoShadowId(),
                getRightClockForCustomColorId(), getRightClockNoShadowForCustomColorId(),
                getRightCityNameWithShadowId(), getRightCityNameNoShadowId(),
                getRightCityNameForCustomColorId(), getRightCityNameNoShadowForCustomColorId(),
                getRightCityDayWithShadowId(), getRightCityDayNoShadowId(),
                getRightCityDayForCustomColorId(), getRightCityDayNoShadowForCustomColorId(),
                getRightCityNoteWithShadowId(), getRightCityNoteNoShadowId(),
                getRightCityNoteForCustomColorId(), getRightCityNoteNoShadowForCustomColorId());
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

    private void update(RemoteViews rv, City city, int clockWithShadowId, int clockNoShadowId,
                        int clockForCustomColorId, int clockNoShadowForCustomColorId,
                        int nameWithShadowId, int nameNoShadowId,
                        int nameForCustomColorId, int nameNoShadowForCustomColorId,
                        int dayWithShadowId, int dayNoShadowId,
                        int dayForCustomColorId, int dayNoShadowForCustomColorId,
                        int noteWithShadowId, int noteNoShadowId,
                        int noteForCustomColorId, int noteNoShadowForCustomColorId,
                        boolean useDefaultClockColor, int customClockColor,
                        boolean useDefaultCityNameColor, int customCityNameColor,
                        boolean useDefaultCityNoteColor, int customCityNoteColor) {

        final boolean shadowEnabled = isTextShadowDisplayed(mPrefs);
        final boolean isTextUppercase = isTextUppercaseDisplayed(mPrefs);
        final boolean is24HourFormat = DataModel.getDataModel().is24HourFormat();
        final float fontSize = is24HourFormat ? m24HourFontSize : m12HourFontSize;

        // Selection of active and inactive IDs
        int clockId = useDefaultClockColor
            ? (shadowEnabled ? clockWithShadowId : clockNoShadowId)
            : (shadowEnabled ? clockForCustomColorId : clockNoShadowForCustomColorId);
        int[] allClockIds = {clockWithShadowId, clockNoShadowId, clockForCustomColorId, clockNoShadowForCustomColorId};
        for (int id : allClockIds) {
            rv.setViewVisibility(id, id == clockId ? VISIBLE : GONE);
        }

        int labelId = useDefaultCityNameColor
            ? (shadowEnabled ? nameWithShadowId : nameNoShadowId)
            : (shadowEnabled ? nameForCustomColorId : nameNoShadowForCustomColorId);
        int[] allLabelIds = {nameWithShadowId, nameNoShadowId, nameForCustomColorId, nameNoShadowForCustomColorId};
        for (int id : allLabelIds) {
            rv.setViewVisibility(id, id == labelId ? VISIBLE : GONE);
        }

        int dayId = useDefaultCityNameColor
            ? (shadowEnabled ? dayWithShadowId : dayNoShadowId)
            : (shadowEnabled ? dayForCustomColorId : dayNoShadowForCustomColorId);
        int[] allDayIds = {dayWithShadowId, dayNoShadowId, dayForCustomColorId, dayNoShadowForCustomColorId};
        for (int id : allDayIds) {
            rv.setViewVisibility(id, id == dayId ? VISIBLE : GONE);
        }

        int noteId = useDefaultCityNoteColor
            ? (shadowEnabled ? noteWithShadowId : noteNoShadowId)
            : (shadowEnabled ? noteForCustomColorId : noteNoShadowForCustomColorId);
        int[] allNoteIds = {noteWithShadowId, noteNoShadowId, noteForCustomColorId, noteNoShadowForCustomColorId};
        for (int id : allNoteIds) {
            rv.setViewVisibility(id, id == noteId ? VISIBLE : GONE);
        }

        // Time format
        WidgetUtils.applyClockFormat(rv, mContext, clockId, 0.4f, false);

        rv.setTextViewTextSize(clockId, TypedValue.COMPLEX_UNIT_PX, fontSize * mFontScale);
        rv.setString(clockId, METHOD_SET_TIME_ZONE, city.getTimeZone().getID());
        if (!useDefaultClockColor) {
            rv.setTextColor(clockId, customClockColor);
        }

        // City name
        rv.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_PX, mCityAndDayFontSize * mFontScale);
        rv.setTextViewText(labelId, isTextUppercase ? city.getName().toUpperCase() : city.getName());
        if (!useDefaultCityNameColor) {
            rv.setTextColor(labelId, customCityNameColor);
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
            rv.setTextViewText(dayId, isTextUppercase ? slashDay.toUpperCase() : slashDay);
            if (!useDefaultCityNameColor) {
                rv.setTextColor(dayId, customCityNameColor);
            }
        }

        rv.setViewVisibility(dayId, displayDayOfWeek ? VISIBLE : GONE);

        // Bind the city note
        String cityNote = mPrefs.getString(KEY_CITY_NOTE + city.getId(), null);
        boolean displayCityNote = cityNote != null && SettingsDAO.isCityNoteEnabled(mPrefs);

        if (displayCityNote) {
            rv.setTextViewTextSize(noteId, TypedValue.COMPLEX_UNIT_PX, mCityAndDayFontSize * mFontScale);
            rv.setTextViewText(noteId, isTextUppercase ? cityNote.toUpperCase() : cityNote);
            if (!useDefaultCityNoteColor) {
                rv.setTextColor(noteId, customCityNoteColor);
            }
        }

        rv.setViewVisibility(noteId, displayCityNote ? VISIBLE : GONE);
    }

    private void hide(RemoteViews clock,
                      int clockWithShadowId, int clockNoShadowId,
                      int clockForCustomColorId, int clockNoShadowForCustomColorId,
                      int nameWithShadowId, int nameNoShadowId,
                      int nameForCustomColorId, int nameNoShadowForCustomColorId,
                      int dayWithShadowId, int dayNoShadowId,
                      int dayForCustomColorId, int dayNoShadowForCustomColorId,
                      int noteWithShadowId, int noteNoShadowId,
                      int noteForCustomColorId, int noteNoShadowForCustomColorId) {

        // On regroupe tous les IDs dans un tableau pour les cacher d'un seul coup
        int[] allIdsToHide = {
            clockWithShadowId, clockNoShadowId, clockForCustomColorId, clockNoShadowForCustomColorId,
            nameWithShadowId, nameNoShadowId, nameForCustomColorId, nameNoShadowForCustomColorId,
            dayWithShadowId, dayNoShadowId, dayForCustomColorId, dayNoShadowForCustomColorId,
            noteWithShadowId, noteNoShadowId, noteForCustomColorId, noteNoShadowForCustomColorId
        };

        for (int id : allIdsToHide) {
            clock.setViewVisibility(id, GONE);
        }
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
