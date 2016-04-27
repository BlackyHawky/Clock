/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import static android.app.PendingIntent.FLAG_NO_CREATE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.app.PendingIntent.getBroadcast;
import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Intent.ACTION_DATE_CHANGED;
import static android.content.Intent.ACTION_LOCALE_CHANGED;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.content.Intent.ACTION_TIMEZONE_CHANGED;
import static android.content.Intent.ACTION_TIME_CHANGED;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * <p>This provider produces a widget resembling:</p>
 * <pre>
 *    12:59 AM
 * ADELAIDE / THU
 * </pre>
 *
 * This widget is scaling the font sizes to fit within the widget bounds chosen by the user without
 * any clipping. To do so it measures layouts offscreen using a range of font sizes in order to
 * choose optimal values.
 */
public class CityAppWidgetProvider extends AppWidgetProvider {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("CityWidgetProvider");

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        LOGGER.i("City Widget processing action %s", intent.getAction());
        super.onReceive(context, intent);

        final AppWidgetManager wm = AppWidgetManager.getInstance(context);
        if (wm == null) {
            return;
        }

        final ComponentName provider = new ComponentName(context, getClass());
        final int[] widgetIds = wm.getAppWidgetIds(provider);

        switch (intent.getAction()) {
            case ACTION_SCREEN_ON:
            case ACTION_TIME_CHANGED:
            case ACTION_DATE_CHANGED:
            case ACTION_LOCALE_CHANGED:
            case ACTION_TIMEZONE_CHANGED:
                for (int widgetId : widgetIds) {
                    relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
                }
        }

        updateNextDayBroadcast(context, widgetIds);

        final DataModel dm = DataModel.getDataModel();
        dm.updateWidgetCount(getClass(), widgetIds.length, R.string.category_city_widget);
    }

    /**
     * Called when widgets must provide remote views.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        super.onUpdate(context, wm, widgetIds);

        for (int widgetId : widgetIds) {
            relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
        }
    }

    /**
     * Called when a widget changes sizes.
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager wm, int widgetId,
            Bundle options) {
        super.onAppWidgetOptionsChanged(context, wm, widgetId, options);

        relayoutWidget(context, AppWidgetManager.getInstance(context), widgetId, options);
    }

    /**
     * Called when widgets have been removed.
     */
    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        super.onDeleted(context, widgetIds);

        for (int widgetId : widgetIds) {
            DataModel.getDataModel().setWidgetCity(widgetId, null);
        }
    }

    /**
     * Called when widgets have been restored from backup. Remaps cities associated with old widget
     * ids to new replacement widget ids.
     */
    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);

        for (int i = 0; i < oldWidgetIds.length; i++) {
            final int oldWidgetId = oldWidgetIds[i];
            final int newWidgetId = newWidgetIds[i];

            // Get the city mapped to the old widget id.
            final City city = DataModel.getDataModel().getWidgetCity(oldWidgetId);

            // Remove the old widget id mapping.
            DataModel.getDataModel().setWidgetCity(oldWidgetId, null);

            // Create the new widget id mapping.
            DataModel.getDataModel().setWidgetCity(newWidgetId, city);
        }
    }

    /**
     * Compute optimal font sizes offscreen for both portrait and landscape orientations using the
     * last known widget size and apply them to the widget.
     */
    private static void relayoutWidget(Context context, AppWidgetManager wm, int widgetId,
            Bundle options) {
        // Fetch the city to display in this widget.
        final City city = DataModel.getDataModel().getWidgetCity(widgetId);

        // Return early if there is no city data; occurs while configuration is not yet complete.
        if (city == null) {
            return;
        }

        final RemoteViews portrait = relayoutWidget(context, wm, widgetId, options, city, true);
        final RemoteViews landscape = relayoutWidget(context, wm, widgetId, options, city, false);
        final RemoteViews widget = new RemoteViews(landscape, portrait);
        wm.updateAppWidget(widgetId, widget);
    }

    /**
     * Compute optimal font sizes offscreen for the given orientation.
     */
    private static RemoteViews relayoutWidget(Context context, AppWidgetManager wm, int widgetId,
            Bundle options, City city, boolean portrait) {
        if (options == null) {
            options = wm.getAppWidgetOptions(widgetId);
        }

        // Create a size template that describes the widget bounds.
        final Resources resources = context.getResources();
        final float density = resources.getDisplayMetrics().density;
        final int minWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_WIDTH));
        final int minHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_HEIGHT));
        final int maxWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_WIDTH));
        final int maxHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_HEIGHT));
        final int targetWidthPx = portrait ? minWidthPx : maxWidthPx;
        final int targetHeightPx = portrait ? maxHeightPx : minHeightPx;
        final int fontSizePx = resources.getDimensionPixelSize(R.dimen.city_widget_name_font_size);
        final int largestClockFontSizePx =
                resources.getDimensionPixelSize(R.dimen.widget_max_clock_font_size);
        final Sizes template = new Sizes(city, targetWidthPx, targetHeightPx, fontSizePx,
                largestClockFontSizePx);

        // Create a remote view for the city widget.
        final String packageName = context.getPackageName();
        final RemoteViews rv = new RemoteViews(packageName, R.layout.city_widget);

        // Tapping on the widget opens the app (if not on the lock screen).
        if (Utils.isWidgetClickable(wm, widgetId)) {
            final Intent openApp = new Intent(context, DeskClock.class);
            final PendingIntent pi = PendingIntent.getActivity(context, 0, openApp, 0);
            rv.setOnClickPendingIntent(R.id.city_widget, pi);
        }

        // Configure child views of the remote view.
        rv.setCharSequence(R.id.clock, "setFormat12Hour", get12HourFormat());
        rv.setCharSequence(R.id.clock, "setFormat24Hour", Utils.get24ModeFormat());

        rv.setTextViewText(R.id.city_name, template.getCityName());
        rv.setString(R.id.clock, "setTimeZone", template.getTimeZoneId());
        rv.setString(R.id.city_day, "setTimeZone", template.getTimeZoneId());

        // Compute optimal font sizes to fit within the widget bounds.
        final Sizes sizes = optimizeSizes(context, template);
        if (LOGGER.isVerboseLoggable()) {
            LOGGER.v(sizes.toString());
        }

        // Apply the computed sizes to the remote views.
        rv.setTextViewTextSize(R.id.clock, COMPLEX_UNIT_PX, sizes.mClockFontSizePx);
        rv.setTextViewTextSize(R.id.city_day, COMPLEX_UNIT_PX, sizes.mFontSizePx);
        rv.setTextViewTextSize(R.id.city_name, COMPLEX_UNIT_PX, sizes.mFontSizePx);
        rv.setInt(R.id.city_name, "setMaxWidth", sizes.mCityNameMaxWidthPx);
        return rv;
    }

    /**
     * Inflate an offscreen copy of the widget views. Binary search through the range of font sizes
     * until the optimal font sizes that fit within the widget bounds are located.
     */
    private static Sizes optimizeSizes(Context context, Sizes template) {
        // Inflate a test layout to compute sizes at different font sizes.
        final LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        final View sizer = inflater.inflate(R.layout.city_widget, null /* root */);

        // Configure the clock to display the preferred time formats.
        final TextClock clock = (TextClock) sizer.findViewById(R.id.clock);
        clock.setFormat12Hour(get12HourFormat());
        clock.setFormat24Hour(Utils.get24ModeFormat());

        // Measure the widget at the largest possible size.
        Sizes high = measure(template, template.getLargestClockFontSizePx(), sizer);
        if (!high.hasViolations()) {
            return high;
        }

        // Measure the widget at the smallest possible size.
        Sizes low = measure(template, template.getSmallestClockFontSizePx(), sizer);
        if (low.hasViolations()) {
            return low;
        }

        // Binary search between the smallest and largest sizes until an optimum size is found.
        while (low.getClockFontSizePx() != high.getClockFontSizePx()) {
            final int midFontSize = (low.getClockFontSizePx() + high.getClockFontSizePx()) / 2;
            if (midFontSize == low.getClockFontSizePx()) {
                return low;
            }

            final Sizes midSize = measure(template, midFontSize, sizer);
            if (midSize.hasViolations()) {
                high = midSize;
            } else {
                low = midSize;
            }
        }

        return low;
    }

    /**
     * Compute all font sizes based on the given {@code clockFontSizePx} and apply them to the
     * offscreen {@code sizer} view. Measure the {@code sizer} view and return the resulting size
     * measurements. Since the localized strings for "AM" and "PM" may be different, layouts for
     * morning and afternoon times are measured and the largest dimensions are reported as the
     * required size.
     */
    private static Sizes measure(Sizes template, int clockFontSizePx, View sizer) {
        final TextClock clock = (TextClock) sizer.findViewById(R.id.clock);
        clock.setTimeZone(template.getTimeZoneId());

        final CharSequence amTime = getLongestAMTimeString(clock);
        final CharSequence pmTime = getLongestPMTimeString(clock);

        final TextClock cityDay = (TextClock) sizer.findViewById(R.id.city_day);
        cityDay.setTimeZone(template.getTimeZoneId());
        // The city name will be elided to fit in its bounds. Don't set a city name which could
        // trigger a false layout violation.

        // Measure the size of the widget at 11:59AM and 11:59PM.
        final Sizes amSizes = measure(template, clockFontSizePx, amTime, sizer);
        final Sizes pmSizes = measure(template, clockFontSizePx, pmTime, sizer);

        // Report the largest dimensions between the two different times.
        final Sizes merged = amSizes.newSize();
        merged.setClockFontSizePx(clockFontSizePx);
        merged.mMeasuredWidthPx = max(amSizes.mMeasuredWidthPx, pmSizes.mMeasuredWidthPx);
        merged.mMeasuredHeightPx = max(amSizes.mMeasuredHeightPx, pmSizes.mMeasuredHeightPx);
        merged.mMeasuredTextClockWidthPx =
                max(amSizes.mMeasuredTextClockWidthPx, pmSizes.mMeasuredTextClockWidthPx);
        merged.mMeasuredTextClockHeightPx =
                max(amSizes.mMeasuredTextClockHeightPx, pmSizes.mMeasuredTextClockHeightPx);
        merged.mCityNameMaxWidthPx = min(amSizes.mCityNameMaxWidthPx, pmSizes.mCityNameMaxWidthPx);
        return merged;
    }

    /**
     * Compute all font and icon sizes based on the given {@code clockFontSizePx} at the given
     * {@code time} and apply them to the offscreen {@code sizer} view. Measure the {@code sizer}
     * view and return the resulting size measurements.
     */
    private static Sizes measure(Sizes template, int clockFontSizePx, CharSequence time, View sizer) {
        // Create a copy of the given template sizes.
        final Sizes measuredSizes = template.newSize();

        // Configure the clock to display the time string.
        final TextClock clock = (TextClock) sizer.findViewById(R.id.clock);
        final TextClock cityDay = (TextClock) sizer.findViewById(R.id.city_day);
        final TextView cityName = (TextView) sizer.findViewById(R.id.city_name);

        // Adjust the font sizes.
        measuredSizes.setClockFontSizePx(clockFontSizePx);
        clock.setText(time);
        clock.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mClockFontSizePx);
        cityDay.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        cityName.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);

        // Measure and layout the sizer.
        final int widthSize = View.MeasureSpec.getSize(measuredSizes.mTargetWidthPx);
        final int heightSize = View.MeasureSpec.getSize(measuredSizes.mTargetHeightPx);
        final int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(widthSize, UNSPECIFIED);
        final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(heightSize, UNSPECIFIED);
        sizer.measure(widthMeasureSpec, heightMeasureSpec);
        sizer.layout(0, 0, sizer.getMeasuredWidth(), sizer.getMeasuredHeight());

        // Copy the measurements into the result object.
        measuredSizes.mMeasuredWidthPx = sizer.getMeasuredWidth();
        measuredSizes.mMeasuredHeightPx = sizer.getMeasuredHeight();
        measuredSizes.mMeasuredTextClockWidthPx = clock.getMeasuredWidth();
        measuredSizes.mMeasuredTextClockHeightPx = clock.getMeasuredHeight();
        measuredSizes.mCityNameMaxWidthPx = template.mTargetWidthPx - cityDay.getMeasuredWidth();

        return measuredSizes;
    }

    /**
     * @return "11:59AM" or "11:59" in the current locale
     */
    private static CharSequence getLongestAMTimeString(TextClock clock) {
        final CharSequence format = clock.is24HourModeEnabled()
                ? clock.getFormat24Hour()
                : clock.getFormat12Hour();
        final Calendar longestAMTime = Calendar.getInstance();
        longestAMTime.set(0, 0, 0, 11, 59);
        return DateFormat.format(format, longestAMTime);
    }

    /**
     * @return "11:59PM" or "23:59" in the current locale
     */
    private static CharSequence getLongestPMTimeString(TextClock clock) {
        final CharSequence format = clock.is24HourModeEnabled()
                ? clock.getFormat24Hour()
                : clock.getFormat12Hour();
        final Calendar longestPMTime = Calendar.getInstance();
        longestPMTime.set(0, 0, 0, 23, 59);
        return DateFormat.format(format, longestPMTime);
    }

    /**
     * @return the locale-specific 12-hour time format with the AM/PM string scaled to 40% of the
     *      normal font height
     */
    private static CharSequence get12HourFormat() {
        return Utils.get12ModeFormat(0.4f /* amPmRatio */);
    }

    /**
     * Schedule or cancel the next-day broadcast as necessary. This broadcast is necessary because
     * the week day displayed in the city widget can vary in width for some locales and thus a
     * layout refresh must be computed.
     */
    private static void updateNextDayBroadcast(Context context, int[] widgetIds) {
        // Fetch all time zones represented by all city widgets.
        final Set<TimeZone> zones = new ArraySet<>(widgetIds.length);
        for (int widgetId : widgetIds) {
            final City city = DataModel.getDataModel().getWidgetCity(widgetId);
            if (city != null) {
                zones.add(city.getTimeZone());
            }
        }

        // Build an intent that will update all city widgets.
        final Intent update =
                new Intent(ACTION_APPWIDGET_UPDATE, null, context, CityAppWidgetProvider.class)
                        .putExtra(EXTRA_APPWIDGET_IDS, widgetIds);
        final AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        if (zones.isEmpty()) {
            // No city widgets exist so cancel the next-day broadcast.
            final PendingIntent pi = getBroadcast(context, 0, update, FLAG_NO_CREATE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        } else {
            // Compute the next time at which the day changes in any of the time zones.
            final Date nextMidnight = Utils.getNextDay(new Date(), zones);

            // Schedule an intent to update all city widgets on next day change.
            final PendingIntent pi = getBroadcast(context, 0, update, FLAG_UPDATE_CURRENT);
            am.setExact(AlarmManager.RTC, nextMidnight.getTime(), pi);
        }
    }

    /**
     * This class stores the target size of the widget as well as the measured size using a given
     * clock font size. All other fonts and icons are scaled proportional to the clock font.
     */
    private static final class Sizes {

        private final City mCity;
        private final int mTargetWidthPx;
        private final int mTargetHeightPx;
        private final int mLargestClockFontSizePx;
        private final int mSmallestClockFontSizePx;

        private int mMeasuredWidthPx;
        private int mMeasuredHeightPx;
        private int mMeasuredTextClockWidthPx;
        private int mMeasuredTextClockHeightPx;

        /** The size of the font to use on the city name / day of week fields. */
        private final int mFontSizePx;

        /** The size of the font to use on the clock field. */
        private int mClockFontSizePx;

        /** If the city name requires more width that this threshold the text is elided. */
        private int mCityNameMaxWidthPx;

        private Sizes(City city, int targetWidthPx, int targetHeightPx, int fontSizePx,
                int largestClockFontSizePx) {
            mCity = city;
            mTargetWidthPx = targetWidthPx;
            mTargetHeightPx = targetHeightPx;
            mFontSizePx = fontSizePx;
            mLargestClockFontSizePx = largestClockFontSizePx;
            mSmallestClockFontSizePx = 0;
        }

        private String getCityName() { return mCity.getName(); }
        private String getTimeZoneId() { return mCity.getTimeZone().getID(); }
        private int getClockFontSizePx() { return mClockFontSizePx; }
        private void setClockFontSizePx(int clockFontSizePx) { mClockFontSizePx = clockFontSizePx; }
        private int getLargestClockFontSizePx() { return mLargestClockFontSizePx; }
        private int getSmallestClockFontSizePx() { return mSmallestClockFontSizePx; }

        private boolean hasViolations() {
            return mMeasuredWidthPx > mTargetWidthPx || mMeasuredHeightPx > mTargetHeightPx;
        }

        private Sizes newSize() {
            return new Sizes(mCity, mTargetWidthPx, mTargetHeightPx, mFontSizePx,
                    mLargestClockFontSizePx);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder(1000);
            builder.append("\n");
            append(builder, "Target dimensions: %dpx x %dpx\n", mTargetWidthPx, mTargetHeightPx);
            append(builder, "Last valid widget container measurement: %dpx x %dpx\n",
                    mMeasuredWidthPx, mMeasuredHeightPx);
            append(builder, "Last text clock measurement: %dpx x %dpx\n",
                    mMeasuredTextClockWidthPx, mMeasuredTextClockHeightPx);
            if (mMeasuredWidthPx > mTargetWidthPx) {
                append(builder, "Measured width %dpx exceeded widget width %dpx\n",
                        mMeasuredWidthPx, mTargetWidthPx);
            }
            if (mMeasuredHeightPx > mTargetHeightPx) {
                append(builder, "Measured height %dpx exceeded widget height %dpx\n",
                        mMeasuredHeightPx, mTargetHeightPx);
            }
            append(builder, "Clock font: %dpx\n", mClockFontSizePx);
            return builder.toString();
        }

        private static void append(StringBuilder builder, String format, Object... args) {
            builder.append(String.format(Locale.ENGLISH, format, args));
        }
    }
}