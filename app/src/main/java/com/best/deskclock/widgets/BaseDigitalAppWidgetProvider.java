// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import static android.app.PendingIntent.FLAG_NO_CREATE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH;
import static android.content.Intent.ACTION_CONFIGURATION_CHANGED;
import static android.content.Intent.ACTION_LOCALE_CHANGED;
import static android.content.Intent.ACTION_TIMEZONE_CHANGED;
import static android.content.Intent.ACTION_TIME_CHANGED;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.View.GONE;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.VISIBLE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.worldclock.CitySelectionActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

/**
 * Abstract base class for digital app widget providers.
 * <p>
 * This class encapsulates the shared logic for rendering digital clock widgets,
 * including layout inflation, size optimization, and view configuration.
 * It supports both portrait and landscape orientations and handles system broadcasts
 * such as time changes, locale updates, and daily refreshes.
 * </p>
 */
public abstract class BaseDigitalAppWidgetProvider extends AppWidgetProvider {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("BaseDgtlWdgtProv");

    private static boolean sReceiversRegistered;

    /**
     * Intent action used for refreshing a world city display when any of them changes days or when
     * the default TimeZone changes days. This affects the widget display because the day-of-week is
     * only visible when the world city day-of-week differs from the default TimeZone's day-of-week.
     */
    private static final String ACTION_ON_DAY_CHANGE = "com.best.deskclock.ON_DAY_CHANGE";

    /**
     * Intent used to deliver the {@link #ACTION_ON_DAY_CHANGE} callback.
     */
    private static final Intent DAY_CHANGE_INTENT = new Intent(ACTION_ON_DAY_CHANGE);

    protected abstract int getLayoutId();
    protected abstract int getSizerLayoutId();
    protected abstract int getWidgetViewId();
    protected abstract int getClockViewId();
    protected abstract int getClockHoursViewId();
    protected abstract int getClockMinutesViewId();
    protected abstract int getDateViewId();
    protected abstract int getNextAlarmIconId();
    protected abstract int getNextAlarmViewId();
    protected abstract int getNextAlarmTextViewId();
    protected abstract int getNextAlarmTitleViewId();
    protected abstract int getWorldCityListViewId();

    protected abstract int getClockCustomViewId();
    protected abstract int getClockHoursCustomViewId();
    protected abstract int getClockMinutesCustomViewId();
    protected abstract int getDateCustomViewId();
    protected abstract int getNextAlarmIconCustomId();
    protected abstract int getNextAlarmCustomViewId();
    protected abstract int getNextAlarmTextCustomViewId();
    protected abstract int getNextAlarmTitleCustomViewId();

    protected abstract boolean areWorldCitiesDisplayed(SharedPreferences prefs);
    protected abstract boolean isHorizontalPaddingApplied(SharedPreferences prefs);
    protected abstract int getMaxWidgetFontSize(SharedPreferences prefs);
    protected abstract float getFontScaleFactor();

    protected abstract Class<?> getCityServiceClass();

    protected abstract void bindDateClickAction(RemoteViews rv, SharedPreferences prefs, PendingIntent calendarPendingIntent);

    protected abstract void configureClock(RemoteViews rv, Context context, SharedPreferences prefs);
    protected abstract void configureDate(RemoteViews rv, Context context, SharedPreferences prefs);
    protected abstract void configureNextAlarm(RemoteViews rv, Context context, SharedPreferences prefs, String nextAlarmTime);
    protected abstract void configureNextAlarmTitle(RemoteViews rv, SharedPreferences prefs, String nextAlarmTime, String nextAlarmTitle);
    protected abstract void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs);

    protected abstract void configureSizerClock(View sizer, SharedPreferences prefs);
    protected abstract void configureSizerDate(View sizer, Context context, SharedPreferences prefs);
    protected abstract void configureSizerNextAlarm(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime);
    protected abstract void configureSizerNextAlarmTitle(View sizer, Context context, SharedPreferences prefs, String nextAlarmTime);

    protected abstract void configureClockForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs);
    protected abstract void configureDateForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs);
    protected abstract void configureNextAlarmForMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs);
    protected abstract void finalizeMeasurement(View sizer, DigitalWidgetSizes measuredSizes, SharedPreferences prefs);

    /**
     * Compute optimal font and icon sizes offscreen for both portrait and landscape orientations
     * using the last known widget size and apply them to the widget.
     */
    protected void relayoutWidget(Context context, AppWidgetManager wm, int widgetId, Bundle options) {
        final RemoteViews portrait = relayoutWidget(context, wm, widgetId, options, true);
        final RemoteViews landscape = relayoutWidget(context, wm, widgetId, options, false);
        final RemoteViews widget = new RemoteViews(landscape, portrait);
        wm.updateAppWidget(widgetId, widget);
        wm.notifyAppWidgetViewDataChanged(widgetId, getWorldCityListViewId());
    }

    /**
     * Compute optimal font and icon sizes offscreen for the given orientation.
     */
    protected RemoteViews relayoutWidget(Context context, AppWidgetManager wm, int widgetId,
                                         Bundle options, boolean portrait) {

        // Create a remote view for the digital clock.
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());

        // Tapping on the widget opens the app or the calendar (if not on the lock screen).
        if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
            Intent clockIntent = new Intent(context, DeskClock.class);
            PendingIntent clockPendingIntent = PendingIntent.getActivity(context, 0, clockIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            rv.setOnClickPendingIntent(getWidgetViewId(), clockPendingIntent);

            Uri calendarUri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build();
            Intent calendarIntent = new Intent(Intent.ACTION_VIEW);
            calendarIntent.setData(calendarUri);
            calendarIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent calendarPendingIntent = PendingIntent.getActivity(context, 1,
                    calendarIntent, PendingIntent.FLAG_IMMUTABLE);

            bindDateClickAction(rv, prefs, calendarPendingIntent);
        }

        // Configure child views of the remote view.
        if (options == null) {
            options = wm.getAppWidgetOptions(widgetId);
        }

        // Compute optimal font sizes and icon sizes to fit within the widget bounds.
        final String nextAlarmTime = AlarmUtils.getNextAlarm(context);
        final String nextAlarmTitle = AlarmUtils.getNextAlarmTitle(context);

        configureClock(rv, context, prefs);
        configureDate(rv, context, prefs);
        configureNextAlarm(rv, context, prefs, nextAlarmTime);
        configureNextAlarmTitle(rv, prefs, nextAlarmTime, nextAlarmTitle);
        configureBackground(rv, context, prefs);

        // Fetch the widget size selected by the user.
        final float density = context.getResources().getDisplayMetrics().density;
        final int minWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_WIDTH));
        final int minHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_HEIGHT));
        final int maxWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_WIDTH));
        final int maxHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_HEIGHT));
        final int targetWidthPx = portrait ? minWidthPx : maxWidthPx;
        final int targetHeightPx = portrait ? maxHeightPx : minHeightPx;
        final List<City> selectedCities = new ArrayList<>(DataModel.getDataModel().getSelectedCities());
        final boolean showHomeClock = SettingsDAO.getShowHomeClock(context, prefs);
        final boolean hasCitiesToDisplay = !selectedCities.isEmpty() || showHomeClock;
        final int largestClockFontSizePx = ThemeUtils.convertDpToPixels(
                areWorldCitiesDisplayed(prefs) && hasCitiesToDisplay ? 80 : getMaxWidgetFontSize(prefs),
                context);

        // Create a size template that describes the widget bounds.
        final DigitalWidgetSizes template = new DigitalWidgetSizes(targetWidthPx, targetHeightPx, largestClockFontSizePx);
        final DigitalWidgetSizes sizes = optimizeSizes(context, template, nextAlarmTime);
        if (LOGGER.isVerboseLoggable()) {
            LOGGER.v(sizes.toString());
        }

        // Apply the computed sizes to the remote views.
        configureSizes(rv, sizes);
        configureBitmaps(rv, sizes);
        configureWorldCityList(rv, context, prefs, wm, widgetId, sizes);

        return rv;
    }

    protected void configureSizes(RemoteViews rv, DigitalWidgetSizes sizes) {
        safeSetTextSize(rv, getClockViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getClockHoursViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getClockMinutesViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getDateViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNextAlarmViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNextAlarmTextViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNextAlarmTitleViewId(), sizes.mFontSizePx);

        safeSetTextSize(rv, getClockCustomViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getClockHoursCustomViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getClockMinutesCustomViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getDateCustomViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNextAlarmCustomViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNextAlarmTextCustomViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNextAlarmTitleCustomViewId(), sizes.mFontSizePx);
    }

    protected void configureBitmaps(RemoteViews rv, DigitalWidgetSizes sizes) {
        safeSetImageBitmap(rv, getNextAlarmIconId(), sizes.mIconBitmap);
        safeSetImageBitmap(rv, getNextAlarmIconCustomId(), sizes.mIconBitmap);
    }

    protected void safeSetTextSize(RemoteViews rv, int viewId, float sizePx) {
        if (viewId != 0) {
            rv.setTextViewTextSize(viewId, COMPLEX_UNIT_PX, sizePx);
        }
    }

    protected void safeSetImageBitmap(RemoteViews rv, int viewId, Bitmap bitmap) {
        if (viewId != 0 && bitmap != null) {
            rv.setImageViewBitmap(viewId, bitmap);
        }
    }

    protected void configureWorldCityList(RemoteViews rv, Context context, SharedPreferences prefs,
                                          AppWidgetManager wm, int widgetId, DigitalWidgetSizes sizes) {

        if (getCityServiceClass() == null) {
            return;
        }

        final int smallestWorldCityListSizePx = ThemeUtils.convertDpToPixels(80, context);
        if (sizes.getListHeight() <= smallestWorldCityListSizePx || !areWorldCitiesDisplayed(prefs)) {
            rv.setViewVisibility(getWorldCityListViewId(), GONE);
        } else {
            Intent intent = new Intent(context, getCityServiceClass());
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            rv.setRemoteAdapter(getWorldCityListViewId(), intent);
            rv.setViewVisibility(getWorldCityListViewId(), VISIBLE);
            if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
                Intent selectCity = new Intent(context, CitySelectionActivity.class);
                PendingIntent pi = PendingIntent.getActivity(context, 0, selectCity, PendingIntent.FLAG_IMMUTABLE);
                rv.setPendingIntentTemplate(getWorldCityListViewId(), pi);
            }
        }
    }

    /**
     * Inflate an offscreen copy of the widget views. Binary search through the range of sizes until
     * the optimal sizes that fit within the widget bounds are located.
     */
    protected DigitalWidgetSizes optimizeSizes(Context context, DigitalWidgetSizes template, String nextAlarmTime) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        // Inflate a test layout to compute sizes at different font sizes.
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View sizer = inflater.inflate(getSizerLayoutId(), null);

        int horizontalPadding = ThemeUtils.convertDpToPixels(
                isHorizontalPaddingApplied(prefs) ? 20 : 0, context);
        sizer.setPadding(horizontalPadding, 0, horizontalPadding, 0);

        configureSizerClock(sizer, prefs);
        configureSizerDate(sizer, context, prefs);
        configureSizerNextAlarm(sizer, context, prefs, nextAlarmTime);
        configureSizerNextAlarmTitle(sizer, context, prefs, nextAlarmTime);

        // Measure the widget at the largest possible size.
        DigitalWidgetSizes high = measure(template, template.getLargestFontSizePx(), sizer, prefs);
        if (!high.hasViolations()) {
            return high;
        }

        // Measure the widget at the smallest possible size.
        DigitalWidgetSizes low = measure(template, template.getSmallestFontSizePx(), sizer, prefs);
        if (low.hasViolations()) {
            return low;
        }

        // Binary search between the smallest and largest sizes until an optimum size is found.
        while (low.getWidgetFontSizePx() != high.getWidgetFontSizePx()) {
            int midFontSize = (low.getWidgetFontSizePx() + high.getWidgetFontSizePx()) / 2;
            if (midFontSize == low.getWidgetFontSizePx()) return low;

            DigitalWidgetSizes midSize = measure(template, midFontSize, sizer, prefs);
            if (midSize.hasViolations()) {
                high = midSize;
            } else {
                low = midSize;
            }
        }

        return low;
    }

    /**
     * Compute all font and icon sizes based on the given {@code clockFontSize} and apply them to
     * the offscreen {@code sizer} view. Measure the {@code sizer} view and return the resulting
     * size measurements.
     */
    protected DigitalWidgetSizes measure(DigitalWidgetSizes template, int widgetFontSize, View sizer, SharedPreferences prefs) {
        // Create a copy of the given template sizes.
        DigitalWidgetSizes measuredSizes = template.newSize();

        // Adjust the font sizes.
        measuredSizes.setWidgetFontSizePx(widgetFontSize, getFontScaleFactor());

        // Configure items to display the widest strings.
        configureClockForMeasurement(sizer, measuredSizes, prefs);
        configureDateForMeasurement(sizer, measuredSizes, prefs);
        configureNextAlarmForMeasurement(sizer, measuredSizes, prefs);

        // Measure and layout the sizer.
        int widthSize = View.MeasureSpec.getSize(measuredSizes.mTargetWidthPx);
        int heightSize = View.MeasureSpec.getSize(measuredSizes.mTargetHeightPx);
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(widthSize, UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(heightSize, UNSPECIFIED);

        sizer.measure(widthMeasureSpec, heightMeasureSpec);
        sizer.layout(0, 0, sizer.getMeasuredWidth(), sizer.getMeasuredHeight());

        // Copy the measurements into the result object.
        measuredSizes.mMeasuredWidthPx = sizer.getMeasuredWidth();
        measuredSizes.mMeasuredHeightPx = sizer.getMeasuredHeight();

        finalizeMeasurement(sizer, measuredSizes, prefs);

        return measuredSizes;
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        // Schedule the day-change callback if necessary.
        updateDayChangeCallback(context);

        // Schedule alarm for daily widget update at midnight
        WidgetUtils.scheduleDailyWidgetUpdate(context, DailyWidgetUpdateReceiver.class);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        // Remove any scheduled day-change callback.
        removeDayChangeCallback(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        WidgetUtils.cancelDailyWidgetUpdate(context, DailyWidgetUpdateReceiver.class);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.i(getClass().getSimpleName() + " - onReceive: " + intent);

        super.onReceive(context, intent);

        final AppWidgetManager wm = AppWidgetManager.getInstance(context);
        if (wm == null) {
            return;
        }

        final ComponentName provider = new ComponentName(context, getClass());
        final int[] widgetIds = wm.getAppWidgetIds(provider);

        final String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_CONFIGURATION_CHANGED:
                case ACTION_LOCALE_CHANGED:
                case ACTION_TIME_CHANGED:
                case ACTION_TIMEZONE_CHANGED:
                case ACTION_ON_DAY_CHANGE:
                    for (int widgetId : widgetIds) {
                        relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
                    }
            }
        }

        WidgetUtils.updateWidgetCount(context, getClass(), widgetIds.length, R.string.category_digital_widget);

        if (widgetIds.length > 0) {
            updateDayChangeCallback(context);
            WidgetUtils.scheduleDailyWidgetUpdate(context, DailyWidgetUpdateReceiver.class);
        }
    }

    /**
     * Called when widgets must provide remote views.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        super.onUpdate(context, wm, widgetIds);

        registerReceivers(context, this);

        for (int widgetId : widgetIds) {
            relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
        }
    }

    /**
     * Called when the app widget changes sizes.
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager wm, int widgetId, Bundle options) {
        super.onAppWidgetOptionsChanged(context, wm, widgetId, options);

        // Scale the fonts of the clock to fit inside the new size
        relayoutWidget(context, AppWidgetManager.getInstance(context), widgetId, options);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private static void registerReceivers(Context context, BroadcastReceiver receiver) {
        if (sReceiversRegistered) return;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CONFIGURATION_CHANGED);
        intentFilter.addAction(ACTION_ON_DAY_CHANGE);

        if (SdkUtils.isAtLeastAndroid13()) {
            context.getApplicationContext().registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            context.getApplicationContext().registerReceiver(receiver, intentFilter);
        }
        sReceiversRegistered = true;
    }

    /**
     * Remove the existing day-change callback if it is not needed (no selected cities exist).
     * Add the day-change callback if it is needed (selected cities exist).
     */
    private void updateDayChangeCallback(Context context) {
        final DataModel dm = DataModel.getDataModel();
        final List<City> selectedCities = dm.getSelectedCities();
        final boolean showHomeClock = SettingsDAO.getShowHomeClock(context, getDefaultSharedPreferences(context));
        if (selectedCities.isEmpty() && !showHomeClock) {
            // Remove the existing day-change callback.
            removeDayChangeCallback(context);
            return;
        }

        // Look up the time at which the next day change occurs across all timezones.
        final Set<TimeZone> zones = new ArraySet<>(selectedCities.size() + 2);
        zones.add(TimeZone.getDefault());
        if (showHomeClock) {
            zones.add(dm.getHomeCity().getTimeZone());
        }
        for (City city : selectedCities) {
            zones.add(city.getTimeZone());
        }
        final Date nextDay = ClockUtils.getNextDay(new Date(), zones);

        // Schedule the next day-change callback; at least one city is displayed.
        final PendingIntent pi =
                PendingIntent.getBroadcast(context, 0, DAY_CHANGE_INTENT, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        WidgetUtils.getAlarmManager(context).setExact(AlarmManager.RTC, Objects.requireNonNull(nextDay).getTime(), pi);
    }

    /**
     * Remove the existing day-change callback.
     */
    private void removeDayChangeCallback(Context context) {
        final PendingIntent pi =
                PendingIntent.getBroadcast(context, 0, DAY_CHANGE_INTENT, FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            WidgetUtils.getAlarmManager(context).cancel(pi);
            pi.cancel();
        }
    }

}
