// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import static android.app.PendingIntent.FLAG_NO_CREATE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH;
import static android.content.Intent.ACTION_APPLICATION_LOCALE_CHANGED;
import static android.content.Intent.ACTION_CONFIGURATION_CHANGED;
import static android.content.Intent.ACTION_LOCALE_CHANGED;
import static android.content.Intent.ACTION_TIMEZONE_CHANGED;
import static android.content.Intent.ACTION_TIME_CHANGED;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.View.GONE;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CITY_NOTE;
import static com.best.deskclock.utils.WidgetUtils.EXTRA_CITY_INDEX;
import static com.best.deskclock.utils.WidgetUtils.EXTRA_WIDGET_ID;
import static com.best.deskclock.utils.WidgetUtils.METHOD_SET_TIME_ZONE;

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
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.worldclock.CitySelectionActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    protected abstract int getLayoutWithShadowId();

    protected abstract int getLayoutWithoutShadowId();

    protected abstract int getSizerLayoutWithShadowId();

    protected abstract int getSizerLayoutWithoutShadowId();

    protected abstract int getWidgetViewId();

    protected abstract int getClockViewId();

    protected abstract int getClockHoursViewId();

    protected abstract int getClockMinutesViewId();

    protected abstract int getTopDateViewId();

    protected abstract int getDateViewId();

    protected abstract int getNextAlarmIconId();

    protected abstract int getNextAlarmViewId();

    protected abstract int getNoAlarmTitleViewId();

    protected abstract int getLabelIconViewId();

    protected abstract int getNextAlarmTitleViewId();

    protected abstract int getWorldCityListViewId();

    protected abstract int getClockCustomViewId();

    protected abstract int getClockHoursCustomViewId();

    protected abstract int getClockMinutesCustomViewId();

    protected abstract int getTopDateCustomViewId();

    protected abstract int getDateCustomViewId();

    protected abstract int getNextAlarmIconCustomId();

    protected abstract int getNextAlarmCustomViewId();

    protected abstract int getNoAlarmTitleCustomViewId();

    protected abstract int getLabelIconCustomViewId();

    protected abstract int getNextAlarmTitleCustomViewId();

    protected abstract boolean isTextUppercase(@NonNull SharedPreferences prefs);

    protected abstract boolean isTextShadowDisplayed(@NonNull SharedPreferences prefs);

    protected abstract boolean areWorldCitiesDisplayed(@NonNull SharedPreferences prefs);

    protected abstract boolean isHorizontalPaddingApplied(@NonNull SharedPreferences prefs);

    protected abstract int getMaxWidgetFontSize(@NonNull SharedPreferences prefs);

    protected abstract float getFontScaleFactor();

    protected abstract String getNextAlarmTime(@NonNull Context context);

    protected abstract Class<?> getCityServiceClass();

    protected abstract int getCityLayoutId();

    protected abstract boolean isDefaultCityClockColor(@NonNull SharedPreferences prefs);

    protected abstract int getCityClockColor(@NonNull SharedPreferences prefs);

    protected abstract boolean isDefaultCityNameColor(@NonNull SharedPreferences prefs);

    protected abstract int getCityNameColor(@NonNull SharedPreferences prefs);

    protected abstract boolean isDefaultCityNoteColor(@NonNull SharedPreferences prefs);

    protected abstract int getCityNoteColor(@NonNull SharedPreferences prefs);

    protected abstract void bindDateClickAction(@NonNull RemoteViews rv, @NonNull SharedPreferences prefs,
                                                @NonNull PendingIntent calendarPendingIntent);

    protected abstract void configureClock(@NonNull RemoteViews rv, @NonNull SharedPreferences prefs);

    protected abstract void configureDate(@NonNull RemoteViews rv, @NonNull Context context, @NonNull SharedPreferences prefs);

    protected abstract void configureNextAlarm(@NonNull RemoteViews rv, @NonNull Context context, @NonNull SharedPreferences prefs,
                                               @NonNull String nextAlarmTime);

    protected abstract void configureNextAlarmTitle(@NonNull RemoteViews rv, @NonNull SharedPreferences prefs,
                                                    @NonNull String nextAlarmTime, @Nullable String nextAlarmTitle);

    protected abstract void configureBackground(@NonNull RemoteViews rv, @NonNull Context context, @NonNull SharedPreferences prefs,
                                                @NonNull DisplayMetrics displayMetrics, int widthPx, int heightPx);

    protected abstract void configureSizerClock(@NonNull View sizer, @NonNull SharedPreferences prefs);

    protected abstract void configureSizerDate(@NonNull View sizer, @NonNull Context context, @NonNull SharedPreferences prefs);

    protected abstract void configureSizerNextAlarm(@NonNull View sizer, @NonNull Context context, @NonNull SharedPreferences prefs,
                                                    @NonNull String nextAlarmTime);

    protected abstract void configureSizerNextAlarmTitle(@NonNull View sizer, @NonNull Context context, @NonNull SharedPreferences prefs,
                                                         @NonNull String nextAlarmTime);

    protected abstract void configureClockForMeasurement(@NonNull View sizer, @NonNull DigitalWidgetSizes measuredSizes,
                                                         @NonNull SharedPreferences prefs);

    protected abstract void configureDateForMeasurement(@NonNull View sizer, @NonNull DigitalWidgetSizes measuredSizes,
                                                        @NonNull SharedPreferences prefs);

    protected abstract void configureNextAlarmForMeasurement(@NonNull View sizer, @NonNull DigitalWidgetSizes measuredSizes,
                                                             @NonNull SharedPreferences prefs);

    protected abstract void finalizeMeasurement(@NonNull View sizer, @NonNull DigitalWidgetSizes measuredSizes,
                                                @NonNull SharedPreferences prefs);

    /**
     * Rebuild and update the widget for the given instance.
     *
     * <ul>
     *   <li>Take a stable snapshot of the selected cities and optionally prefix the home city.</li>
     *   <li>Build RemoteViews for both portrait and landscape orientations.</li>
     *   <li>On Android 12 and later, construct {@link RemoteViews.RemoteCollectionItems} from the
     *       snapshot and assign the collection adapter to both orientation RemoteViews before
     *       combining them.</li>
     *   <li>Combine the two orientation RemoteViews into a single RemoteViews and call
     *       {@link AppWidgetManager#updateAppWidget(int, RemoteViews)} to push the update.</li>
     *   <li>On pre-Android 12 devices, call {@code AppWidgetManager.notifyAppWidgetViewDataChanged(int, int)}
     *       after updating the RemoteViews.</li>
     * </ul>
     *
     * @param context  application context; used to read preferences and resources
     * @param wm       AppWidgetManager instance used to query options and push updates
     * @param widgetId id of the widget instance to update
     * @param options  widget options bundle (can be {@code null}; the method will query the manager)
     */
    protected void relayoutWidget(@NonNull Context context, @NonNull AppWidgetManager wm, int widgetId, @NonNull Bundle options) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        DataModel dataModel = DataModel.getDataModel();
        final List<City> cities = new ArrayList<>(dataModel.getSelectedCities());
        final City home = dataModel.getHomeCity();
        final boolean showHomeClock = SettingsDAO.getShowHomeClock(context, prefs);

        if (showHomeClock) {
            cities.add(0, home);
        }

        final RemoteViews portrait = buildRemoteViewsForOrientation(context, prefs, displayMetrics, wm, widgetId, options, true, cities);
        final RemoteViews landscape = buildRemoteViewsForOrientation(context, prefs, displayMetrics, wm, widgetId, options, false, cities);

        if (SdkUtils.isAtLeastAndroid12()) {
            if (cities.isEmpty()) {
                final RemoteViews widget = new RemoteViews(landscape, portrait);
                wm.updateAppWidget(widgetId, widget);
                return;
            }

            RemoteViews.RemoteCollectionItems items = buildRemoteCollectionItemsForCities(context, prefs, displayMetrics, widgetId, cities);
            portrait.setRemoteAdapter(getWorldCityListViewId(), items);
            landscape.setRemoteAdapter(getWorldCityListViewId(), items);
        }

        final RemoteViews widget = new RemoteViews(landscape, portrait);
        wm.updateAppWidget(widgetId, widget);
        updateDayChangeCallback(context);

        if (SdkUtils.isBeforeAndroid12()) {
            //noinspection deprecation
            wm.notifyAppWidgetViewDataChanged(widgetId, getWorldCityListViewId());
        }
    }

    /**
     * Compute optimal font and icon sizes offscreen for the given orientation.
     */
    protected RemoteViews buildRemoteViewsForOrientation(@NonNull Context context, @NonNull SharedPreferences prefs,
                                                         @NonNull DisplayMetrics displayMetrics, @NonNull AppWidgetManager wm,
                                                         int widgetId, @Nullable Bundle options, boolean portrait,
                                                         @NonNull List<City> cities) {

        // Create a remote view for the digital clock.
        RemoteViews rv = new RemoteViews(context.getPackageName(), isTextShadowDisplayed(prefs)
            ? getLayoutWithShadowId()
            : getLayoutWithoutShadowId());

        // Tapping on the widget opens the app or the calendar (if not on the lock screen).
        if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
            Intent clockIntent = new Intent(context, DeskClock.class);
            PendingIntent clockPendingIntent = PendingIntent.getActivity(context, 0, clockIntent, PendingIntent.FLAG_IMMUTABLE);

            rv.setOnClickPendingIntent(getWidgetViewId(), clockPendingIntent);

            Uri calendarUri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build();
            Intent calendarIntent = new Intent(Intent.ACTION_VIEW);
            calendarIntent.setData(calendarUri);
            calendarIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent calendarPendingIntent = PendingIntent.getActivity(context, 1, calendarIntent, PendingIntent.FLAG_IMMUTABLE);

            bindDateClickAction(rv, prefs, calendarPendingIntent);
        }

        // Configure child views of the remote view.
        if (options == null) {
            options = wm.getAppWidgetOptions(widgetId);
        }

        // Compute optimal font sizes and icon sizes to fit within the widget bounds.
        final String nextAlarmTime = getNextAlarmTime(Utils.getLocalizedContext(context, SettingsDAO.getLanguageCode(prefs)));
        final String nextAlarmTitle = AlarmUtils.getNextAlarmTitle(context);

        configureClock(rv, prefs);
        configureDate(rv, context, prefs);
        configureNextAlarm(rv, context, prefs, nextAlarmTime);
        configureNextAlarmTitle(rv, prefs, nextAlarmTime, nextAlarmTitle);

        // Fetch the widget size selected by the user.
        final float density = displayMetrics.density;
        final int minWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_WIDTH));
        final int minHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_HEIGHT));
        final int maxWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_WIDTH));
        final int maxHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_HEIGHT));
        final int targetWidthPx = portrait ? minWidthPx : maxWidthPx;
        final int targetHeightPx = portrait ? maxHeightPx : minHeightPx;
        final int largestClockFontSizePx = (int) dpToPx(areWorldCitiesDisplayed(prefs) && !cities.isEmpty()
            ? 80
            : getMaxWidgetFontSize(prefs), displayMetrics);

        // Create a size template that describes the widget bounds.
        final DigitalWidgetSizes template = new DigitalWidgetSizes(targetWidthPx, targetHeightPx, largestClockFontSizePx);
        final DigitalWidgetSizes sizes = optimizeSizes(context, prefs, displayMetrics, template, nextAlarmTime);
        if (LOGGER.isVerboseLoggable()) {
            LOGGER.v(sizes.toString());
        }

        // Apply the computed sizes to the remote views.
        configureBackground(rv, context, prefs, displayMetrics, targetWidthPx, targetHeightPx);
        configureSizes(rv, sizes);
        configureBitmaps(rv, sizes);
        configureWorldCityList(rv, context, prefs, displayMetrics, wm, widgetId, sizes, cities);

        return rv;
    }

    protected void configureSizes(@NonNull RemoteViews rv, @NonNull DigitalWidgetSizes sizes) {
        safeSetTextSize(rv, getClockViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getClockHoursViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getClockMinutesViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getTopDateViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getDateViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNextAlarmViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNoAlarmTitleViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNextAlarmTitleViewId(), sizes.mFontSizePx);

        safeSetTextSize(rv, getClockCustomViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getClockHoursCustomViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getClockMinutesCustomViewId(), sizes.mWidgetFontSizePx);
        safeSetTextSize(rv, getTopDateCustomViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getDateCustomViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNextAlarmCustomViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNoAlarmTitleCustomViewId(), sizes.mFontSizePx);
        safeSetTextSize(rv, getNextAlarmTitleCustomViewId(), sizes.mFontSizePx);
    }

    protected void configureBitmaps(@NonNull RemoteViews rv, @NonNull DigitalWidgetSizes sizes) {
        safeSetImageBitmap(rv, getNextAlarmIconId(), sizes.mIconBitmap);
        safeSetImageBitmap(rv, getNextAlarmIconCustomId(), sizes.mIconBitmap);
        safeSetImageBitmap(rv, getLabelIconViewId(), sizes.mLabelBitmap);
        safeSetImageBitmap(rv, getLabelIconCustomViewId(), sizes.mLabelBitmap);
    }

    protected void safeSetTextSize(@NonNull RemoteViews rv, int viewId, float sizePx) {
        if (viewId != 0) {
            rv.setTextViewTextSize(viewId, COMPLEX_UNIT_PX, sizePx);
        }
    }

    protected void safeSetImageBitmap(@NonNull RemoteViews rv, int viewId, Bitmap bitmap) {
        if (viewId != 0 && bitmap != null) {
            rv.setImageViewBitmap(viewId, bitmap);
        }
    }

    protected void configureWorldCityList(@NonNull RemoteViews rv, @NonNull Context context, @NonNull SharedPreferences prefs,
                                          @NonNull DisplayMetrics displayMetrics, @NonNull AppWidgetManager wm, int widgetId,
                                          @NonNull DigitalWidgetSizes sizes, @NonNull List<City> cities) {

        if (getCityServiceClass() == null) {
            return;
        }

        final int smallestWorldCityListSizePx = (int) dpToPx(80, displayMetrics);
        if (!SettingsDAO.isClockTabVisible(prefs)
            || !areWorldCitiesDisplayed(prefs)
            || cities.isEmpty()
            || sizes.getListHeight() <= smallestWorldCityListSizePx) {
            rv.setViewVisibility(getWorldCityListViewId(), GONE);
            return;
        }

        rv.setViewVisibility(getWorldCityListViewId(), VISIBLE);

        if (SdkUtils.isAtLeastAndroid12()) {
            RemoteViews.RemoteCollectionItems items = buildRemoteCollectionItemsForCities(context, prefs, displayMetrics, widgetId, cities);
            rv.setRemoteAdapter(getWorldCityListViewId(), items);
        } else {
            Intent intent = new Intent(context, getCityServiceClass());
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            //noinspection deprecation
            rv.setRemoteAdapter(getWorldCityListViewId(), intent);
        }

        if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
            Intent selectCity = new Intent(context, CitySelectionActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, widgetId, selectCity, PendingIntent.FLAG_IMMUTABLE);
            rv.setPendingIntentTemplate(getWorldCityListViewId(), pi);
        }
    }

    /**
     * Inflate an offscreen copy of the widget views. Binary search through the range of sizes until
     * the optimal sizes that fit within the widget bounds are located.
     */
    protected DigitalWidgetSizes optimizeSizes(@NonNull Context context, @NonNull SharedPreferences prefs,
                                               @NonNull DisplayMetrics displayMetrics, @NonNull DigitalWidgetSizes template,
                                               @NonNull String nextAlarmTime) {

        // Inflate a test layout to compute sizes at different font sizes.
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View sizer = inflater.inflate(isTextShadowDisplayed(prefs)
            ? getSizerLayoutWithShadowId()
            : getSizerLayoutWithoutShadowId(), null);

        int horizontalPadding = (int) dpToPx(isHorizontalPaddingApplied(prefs) ? 20 : 0, displayMetrics);
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
    protected DigitalWidgetSizes measure(@NonNull DigitalWidgetSizes template, int widgetFontSize, @NonNull View sizer,
                                         @NonNull SharedPreferences prefs) {

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

    /**
     * Build the RemoteCollectionItems representing the rows of the widget city list.
     *
     * <p>For each pair of cities (left/right) this method creates a RemoteViews for the row,
     * configures texts, formats and fill-in intents, and collects them into a RemoteCollectionItems
     * object returned to the system.
     *
     * @param context  context used to access resources and services
     * @param prefs    the {@link SharedPreferences} containing user preferences
     * @param widgetId id of the widget for which the collection is built
     * @param cities   list of cities to display (order and possible presence of the home city)
     * @return RemoteViews.RemoteCollectionItems ready to be passed to RemoteViews.setRemoteAdapter(...)
     */
    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.S)
    private RemoteViews.RemoteCollectionItems buildRemoteCollectionItemsForCities(@NonNull Context context,
                                                                                  @NonNull SharedPreferences prefs,
                                                                                  @NonNull DisplayMetrics displayMetrics,
                                                                                  int widgetId, @Nullable List<City> cities) {

        RemoteViews.RemoteCollectionItems.Builder builder = new RemoteViews.RemoteCollectionItems.Builder();
        if (cities == null || cities.isEmpty()) {
            return builder.build();
        }

        final boolean isTablet = ThemeUtils.isTablet();
        final boolean shadowEnabled = isTextShadowDisplayed(prefs);
        final boolean isTextUppercase = isTextUppercase(prefs);
        final boolean is24HourFormat = DateFormat.is24HourFormat(context);

        final float hour12FontSize = dpToPx(isTablet ? 52 : 32, displayMetrics);
        final float hour24FontSize = dpToPx(isTablet ? 65 : 40, displayMetrics);
        final float cityAndDayFontSize = dpToPx(isTablet ? 20 : 14, displayMetrics);

        final int totalRows = (int) Math.ceil((double) cities.size() / 2);
        int rowIndex = 0;

        final Calendar localCal = Calendar.getInstance(TimeZone.getDefault());

        for (int i = 0; i < cities.size(); i += 2, rowIndex++) {
            final City left = cities.get(i);
            final City right = (i + 1 < cities.size()) ? cities.get(i + 1) : null;

            RemoteViews rowRv = new RemoteViews(context.getPackageName(), getCityLayoutId());
            final float fontScale = WidgetUtils.getScaleRatio(context, displayMetrics, null, widgetId, cities.size());

            if (right != null) {
                rowRv.setViewVisibility(R.id.twoColumnContainer, View.VISIBLE);
                rowRv.setViewVisibility(R.id.singleColumnContainer, View.GONE);

                fillColumn(context, prefs, rowRv, left, i, widgetId, true, false, shadowEnabled, isTextUppercase,
                    is24HourFormat, hour12FontSize, hour24FontSize, cityAndDayFontSize, fontScale, localCal,
                    isDefaultCityClockColor(prefs), getCityClockColor(prefs), isDefaultCityNameColor(prefs), getCityNameColor(prefs),
                    isDefaultCityNoteColor(prefs), getCityNoteColor(prefs));

                fillColumn(context, prefs, rowRv, right, i + 1, widgetId, false, false, shadowEnabled,
                    isTextUppercase, is24HourFormat, hour12FontSize, hour24FontSize, cityAndDayFontSize, fontScale, localCal,
                    isDefaultCityClockColor(prefs), getCityClockColor(prefs), isDefaultCityNameColor(prefs), getCityNameColor(prefs),
                    isDefaultCityNoteColor(prefs), getCityNoteColor(prefs));
            } else {
                rowRv.setViewVisibility(R.id.twoColumnContainer, View.GONE);
                rowRv.setViewVisibility(R.id.singleColumnContainer, View.VISIBLE);

                fillColumn(context, prefs, rowRv, left, i, widgetId, true, true, shadowEnabled, isTextUppercase,
                    is24HourFormat, hour12FontSize, hour24FontSize, cityAndDayFontSize, fontScale, localCal,
                    isDefaultCityClockColor(prefs), getCityClockColor(prefs), isDefaultCityNameColor(prefs), getCityNameColor(prefs),
                    isDefaultCityNoteColor(prefs), getCityNoteColor(prefs));
            }

            boolean lastRow = (rowIndex == totalRows - 1);
            rowRv.setViewVisibility(R.id.citySpacer, lastRow ? View.GONE : View.VISIBLE);

            long leftId = left != null ? WidgetUtils.getStableIdForCity(left) : 0L;
            long rightId = right != null ? WidgetUtils.getStableIdForCity(right) : 0L;
            long rowStableId = WidgetUtils.combineStableIds(leftId, rightId);
            builder.addItem(rowStableId, rowRv);
        }

        return builder.build();
    }

    /**
     * Fill a column (left, right or single) of a city list row.
     *
     * <p>Configures the clock (12/24h format, timezone), the city name and optional day label,
     * applies sizes and colors, manages the container visibility and prepares the click fill‑in intent.
     *
     * @param rowRv                   RemoteViews representing the row to populate
     * @param city                    City object to display in the column (can be null to hide the column)
     * @param cityIndex               index of the city in the list (used for the fill‑in intent)
     * @param widgetId                id of the widget (used for the fill‑in intent)
     * @param isLeft                  true if the column is the left column
     * @param isSingle                true if the column is the single-column layout
     * @param shadowEnabled           true to use the shadowed variant
     * @param isTextUppercase         true to display text in uppercase
     * @param is24HourFormat          true for 24-hour format, false for 12-hour
     * @param hour12FontSize          font size for 12-hour format (px)
     * @param hour24FontSize          font size for 24-hour format (px)
     * @param cityAndDayFontSize      font size for city name and day label (px)
     * @param fontScale               scale factor applied to font sizes
     * @param useDefaultClockColor    true if the default clock color is used
     * @param customClockColor        custom color for the clock text
     * @param useDefaultCityNameColor true if the default city name color is used
     * @param customCityNameColor     custom color for the city name and day text
     * @param useDefaultCityNoteColor true if the default city note color is used
     * @param customCityNoteColor     custom color for the city note
     * @param context                 context used to access resources
     * @param localCal                local Calendar used to determine whether to show the day label
     */
    private void fillColumn(@NonNull Context context, @NonNull SharedPreferences prefs, @NonNull RemoteViews rowRv, @Nullable City city,
                            int cityIndex, int widgetId, boolean isLeft, boolean isSingle, boolean shadowEnabled, boolean isTextUppercase,
                            boolean is24HourFormat, float hour12FontSize, float hour24FontSize, float cityAndDayFontSize, float fontScale,
                            @NonNull Calendar localCal, boolean useDefaultClockColor, int customClockColor, boolean useDefaultCityNameColor,
                            int customCityNameColor, boolean useDefaultCityNoteColor, int customCityNoteColor) {

        if (city == null) {
            // Hide the corresponding container
            if (isSingle) {
                rowRv.setViewVisibility(R.id.singleContainer, View.GONE);
            } else if (isLeft) {
                rowRv.setViewVisibility(R.id.leftContainer, View.GONE);
            } else {
                rowRv.setViewVisibility(R.id.rightContainer, View.GONE);
            }
            return;
        }

        // Choice of IDs according to left/right/single, shadow and default color
        final int clockId, nameId, dayId, noteId, containerId;
        final int[] clockIdsGroup, nameIdsGroup, dayIdsGroup, noteIdsGroup;

        if (isSingle) {
            clockIdsGroup = new int[]{R.id.singleClock, R.id.singleClockNoShadow,
                R.id.singleClockForCustomColor, R.id.singleClockNoShadowForCustomColor};
            clockId = useDefaultClockColor
                ? (shadowEnabled ? R.id.singleClock : R.id.singleClockNoShadow)
                : (shadowEnabled ? R.id.singleClockForCustomColor : R.id.singleClockNoShadowForCustomColor);

            nameIdsGroup = new int[]{R.id.singleCityName, R.id.singleCityNameNoShadow,
                R.id.singleCityNameForCustomColor, R.id.singleCityNameNoShadowForCustomColor};
            nameId = useDefaultCityNameColor
                ? (shadowEnabled ? R.id.singleCityName : R.id.singleCityNameNoShadow)
                : (shadowEnabled ? R.id.singleCityNameForCustomColor : R.id.singleCityNameNoShadowForCustomColor);

            dayIdsGroup = new int[]{R.id.singleCityDay, R.id.singleCityDayNoShadow,
                R.id.singleCityDayForCustomColor, R.id.singleCityDayNoShadowForCustomColor};
            dayId = useDefaultCityNameColor
                ? (shadowEnabled ? R.id.singleCityDay : R.id.singleCityDayNoShadow)
                : (shadowEnabled ? R.id.singleCityDayForCustomColor : R.id.singleCityDayNoShadowForCustomColor);

            noteIdsGroup = new int[]{R.id.singleCityNote, R.id.singleCityNoteNoShadow,
                R.id.singleCityNoteForCustomColor, R.id.singleCityNoteNoShadowForCustomColor};
            noteId = useDefaultCityNoteColor
                ? (shadowEnabled ? R.id.singleCityNote : R.id.singleCityNoteNoShadow)
                : (shadowEnabled ? R.id.singleCityNoteForCustomColor : R.id.singleCityNoteNoShadowForCustomColor);

            containerId = R.id.singleContainer;
        } else if (isLeft) {
            clockIdsGroup = new int[]{R.id.leftClock, R.id.leftClockNoShadow,
                R.id.leftClockForCustomColor, R.id.leftClockNoShadowForCustomColor};
            clockId = useDefaultClockColor
                ? (shadowEnabled ? R.id.leftClock : R.id.leftClockNoShadow)
                : (shadowEnabled ? R.id.leftClockForCustomColor : R.id.leftClockNoShadowForCustomColor);

            nameIdsGroup = new int[]{R.id.cityNameLeft, R.id.cityNameLeftNoShadow,
                R.id.cityNameLeftForCustomColor, R.id.cityNameLeftNoShadowForCustomColor};

            nameId = useDefaultCityNameColor
                ? (shadowEnabled ? R.id.cityNameLeft : R.id.cityNameLeftNoShadow)
                : (shadowEnabled ? R.id.cityNameLeftForCustomColor : R.id.cityNameLeftNoShadowForCustomColor);

            dayIdsGroup = new int[]{R.id.cityDayLeft, R.id.cityDayLeftNoShadow,
                R.id.cityDayLeftForCustomColor, R.id.cityDayLeftNoShadowForCustomColor};

            dayId = useDefaultCityNameColor
                ? (shadowEnabled ? R.id.cityDayLeft : R.id.cityDayLeftNoShadow)
                : (shadowEnabled ? R.id.cityDayLeftForCustomColor : R.id.cityDayLeftNoShadowForCustomColor);

            noteIdsGroup = new int[]{R.id.cityNoteLeft, R.id.cityNoteLeftNoShadow,
                R.id.cityNoteLeftForCustomColor, R.id.cityNoteLeftNoShadowForCustomColor};
            noteId = useDefaultCityNoteColor
                ? (shadowEnabled ? R.id.cityNoteLeft : R.id.cityNoteLeftNoShadow)
                : (shadowEnabled ? R.id.cityNoteLeftForCustomColor : R.id.cityNoteLeftNoShadowForCustomColor);

            containerId = R.id.leftContainer;
        } else {
            clockIdsGroup = new int[]{R.id.rightClock, R.id.rightClockNoShadow,
                R.id.rightClockForCustomColor, R.id.rightClockNoShadowForCustomColor};
            clockId = useDefaultClockColor
                ? (shadowEnabled ? R.id.rightClock : R.id.rightClockNoShadow)
                : (shadowEnabled ? R.id.rightClockForCustomColor : R.id.rightClockNoShadowForCustomColor);

            nameIdsGroup = new int[]{R.id.cityNameRight, R.id.cityNameRightNoShadow,
                R.id.cityNameRightForCustomColor, R.id.cityNameRightNoShadowForCustomColor};
            nameId = useDefaultCityNameColor
                ? (shadowEnabled ? R.id.cityNameRight : R.id.cityNameRightNoShadow)
                : (shadowEnabled ? R.id.cityNameRightForCustomColor : R.id.cityNameRightNoShadowForCustomColor);

            dayIdsGroup = new int[]{R.id.cityDayRight, R.id.cityDayRightNoShadow,
                R.id.cityDayRightForCustomColor, R.id.cityDayRightNoShadowForCustomColor};
            dayId = useDefaultCityNameColor
                ? (shadowEnabled ? R.id.cityDayRight : R.id.cityDayRightNoShadow)
                : (shadowEnabled ? R.id.cityDayRightForCustomColor : R.id.cityDayRightNoShadowForCustomColor);

            noteIdsGroup = new int[]{R.id.cityNoteRight, R.id.cityNoteRightNoShadow,
                R.id.cityNoteRightForCustomColor, R.id.cityNoteRightNoShadowForCustomColor};
            noteId = useDefaultCityNoteColor
                ? (shadowEnabled ? R.id.cityNoteRight : R.id.cityNoteRightNoShadow)
                : (shadowEnabled ? R.id.cityNoteRightForCustomColor : R.id.cityNoteRightNoShadowForCustomColor);

            containerId = R.id.rightContainer;
        }

        rowRv.setViewVisibility(containerId, View.VISIBLE);

        // Hide inactive variants, show active ones
        showActiveVariant(rowRv, clockId, clockIdsGroup);
        showActiveVariant(rowRv, nameId, nameIdsGroup);
        showActiveVariant(rowRv, dayId, dayIdsGroup);
        showActiveVariant(rowRv, noteId, noteIdsGroup);

        // Time format
        WidgetUtils.applyClockFormat(rowRv, clockId, 0.4f, false);

        rowRv.setString(clockId, METHOD_SET_TIME_ZONE, city.getTimeZone().getID());
        rowRv.setTextViewTextSize(clockId, TypedValue.COMPLEX_UNIT_PX, (is24HourFormat ? hour24FontSize : hour12FontSize) * fontScale);
        if (!useDefaultClockColor) {
            rowRv.setTextColor(clockId, customClockColor);
        }

        // City name
        rowRv.setTextViewTextSize(nameId, TypedValue.COMPLEX_UNIT_PX, cityAndDayFontSize * fontScale);
        rowRv.setTextViewText(nameId, isTextUppercase ? city.getName().toUpperCase() : city.getName());
        if (!useDefaultCityNameColor) {
            rowRv.setTextColor(nameId, customCityNameColor);
        }

        // Day if different
        Calendar cityCal = Calendar.getInstance(city.getTimeZone());
        boolean displayDay = localCal.get(Calendar.DAY_OF_WEEK) != cityCal.get(Calendar.DAY_OF_WEEK);
        if (displayDay) {
            String weekday = cityCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault());
            String slashDay = context.getString(R.string.world_day_of_week_label, weekday);
            rowRv.setTextViewText(dayId, isTextUppercase ? slashDay.toUpperCase() : slashDay);
            rowRv.setTextViewTextSize(dayId, TypedValue.COMPLEX_UNIT_PX, cityAndDayFontSize * fontScale);
            if (!useDefaultCityNameColor) {
                rowRv.setTextColor(dayId, customCityNameColor);
            }
        }
        rowRv.setViewVisibility(dayId, displayDay ? View.VISIBLE : View.GONE);

        // City note
        String cityNote = prefs.getString(KEY_CITY_NOTE + city.getId(), null);
        boolean displayCityNote = cityNote != null && SettingsDAO.isCityNoteEnabled(prefs);
        if (displayCityNote) {
            rowRv.setTextViewTextSize(noteId, TypedValue.COMPLEX_UNIT_PX, cityAndDayFontSize * fontScale);
            rowRv.setTextViewText(noteId, isTextUppercase ? cityNote.toUpperCase() : cityNote);
            if (!useDefaultCityNoteColor) {
                rowRv.setTextColor(noteId, customCityNoteColor);
            }
        }
        rowRv.setViewVisibility(noteId, displayCityNote ? View.VISIBLE : View.GONE);

        // Fill-in intent
        Intent fill = new Intent();
        fill.putExtra(EXTRA_CITY_INDEX, cityIndex);
        fill.putExtra(EXTRA_WIDGET_ID, widgetId);
        rowRv.setOnClickFillInIntent(containerId, fill);

        if (isSingle) {
            rowRv.setOnClickFillInIntent(R.id.rightSpacer, fill);
        }
    }

    /**
     * Show the active variant of a view and hide the inactive variants.
     *
     * <p>Hides all views specified by <code>inactiveIds</code> (visibility = GONE)
     * then makes the view specified by <code>activeId</code> visible (visibility = VISIBLE).
     *
     * @param rv            RemoteViews containing the views to modify
     * @param activeId      id of the view to activate (VISIBLE)
     * @param allVariantIds ids of all views
     */
    private void showActiveVariant(@NonNull RemoteViews rv, int activeId, @NonNull int... allVariantIds) {
        for (int id : allVariantIds) {
            if (id != activeId) {
                rv.setViewVisibility(id, View.GONE);
            }
        }

        rv.setViewVisibility(activeId, View.VISIBLE);
    }


    @Override
    public void onEnabled(@NonNull Context context) {
        super.onEnabled(context);

        // Schedule the day-change callback if necessary.
        updateDayChangeCallback(context);

        // Schedule alarm for daily widget update at midnight
        WidgetUtils.scheduleDailyWidgetUpdate(context, DailyWidgetUpdateReceiver.class);
    }

    @Override
    public void onDisabled(@NonNull Context context) {
        super.onDisabled(context);

        // Remove any scheduled day-change callback.
        removeDayChangeCallback(context);
    }

    @Override
    public void onDeleted(@NonNull Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        WidgetUtils.cancelDailyWidgetUpdate(context, DailyWidgetUpdateReceiver.class);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
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
                case ACTION_APPLICATION_LOCALE_CHANGED:
                case ACTION_TIME_CHANGED:
                case ACTION_TIMEZONE_CHANGED:
                case ACTION_ON_DAY_CHANGE:
                    for (int widgetId : widgetIds) {
                        relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
                    }
            }
        }

        int widgetCount = widgetIds.length;
        int delta = WidgetDAO.updateWidgetCount(getDefaultSharedPreferences(context), getClass(), widgetCount);

        WidgetUtils.updateWidgetCount(delta, R.string.category_digital_widget);

        if (widgetIds.length > 0) {
            WidgetUtils.scheduleDailyWidgetUpdate(context, DailyWidgetUpdateReceiver.class);
        }
    }

    /**
     * Called when widgets must provide remote views.
     */
    @Override
    public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager wm, int[] widgetIds) {
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
    public void onAppWidgetOptionsChanged(@NonNull Context context, @NonNull AppWidgetManager wm, int widgetId, @NonNull Bundle options) {
        super.onAppWidgetOptionsChanged(context, wm, widgetId, options);

        // Scale the fonts of the clock to fit inside the new size
        relayoutWidget(context, AppWidgetManager.getInstance(context), widgetId, options);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private static void registerReceivers(@NonNull Context context, @NonNull BroadcastReceiver receiver) {
        if (sReceiversRegistered) return;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CONFIGURATION_CHANGED);
        intentFilter.addAction(ACTION_ON_DAY_CHANGE);

        if (SdkUtils.isAtLeastAndroid13()) {
            context.getApplicationContext().registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.getApplicationContext().registerReceiver(receiver, intentFilter);
        }
        sReceiversRegistered = true;
    }

    /**
     * Remove the existing day-change callback if it is not needed (no selected cities exist).
     * Add the day-change callback if it is needed (selected cities exist).
     */
    private void updateDayChangeCallback(@NonNull Context context) {
        if (getCityServiceClass() == null) {
            return;
        }

        final DataModel dataModel = DataModel.getDataModel();
        final List<City> selectedCities = dataModel.getSelectedCities();
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
            zones.add(dataModel.getHomeCity().getTimeZone());
        }
        for (City city : selectedCities) {
            zones.add(city.getTimeZone());
        }
        final Date nextDay = ClockUtils.getNextDay(new Date(), zones);

        // Schedule the next day-change callback; at least one city is displayed.
        final PendingIntent pi = PendingIntent.getBroadcast(
            context, 0, DAY_CHANGE_INTENT, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        WidgetUtils.getAlarmManager(context).setExact(AlarmManager.RTC, Objects.requireNonNull(nextDay).getTime(), pi);
    }

    /**
     * Remove the existing day-change callback.
     */
    private void removeDayChangeCallback(@NonNull Context context) {
        final PendingIntent pi = PendingIntent.getBroadcast(
            context, 0, DAY_CHANGE_INTENT, FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            WidgetUtils.getAlarmManager(context).cancel(pi);
            pi.cancel();
        }
    }

}
