// SPDX-License-Identifier: GPL-3.0-only

package com.best.alarmclock.standardwidgets;

import static android.app.AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED;
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

import static com.best.deskclock.data.WidgetModel.ACTION_UPDATE_WIDGETS_AFTER_RESTORE;
import static com.best.deskclock.data.WidgetModel.ACTION_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED;

import static java.lang.Math.max;
import static java.lang.Math.round;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.best.alarmclock.WidgetUtils;
import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;

import java.util.Locale;

/**
 * <p>This provider produces a widget resembling one of the formats below.</p>
 * <p>
 * If an alarm is scheduled to ring in the future:
 * <pre>
 *        WED, FEB 3
 *           12
 *           59
 *      ⏰ THU 9:30 AM
 * </pre>
 * <p>
 * If no alarm is scheduled to ring in the future:
 * <pre>
 *        WED, FEB 3
 *           12
 *           59
 * </pre>
 * <p>
 * This widget is scaling the font sizes to fit within the widget bounds chosen by the user without
 * any clipping. To do so it measures layouts offscreen using a range of font sizes in order to
 * choose optimal values.
 */
public class VerticalDigitalAppWidgetProvider extends AppWidgetProvider {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("VertDgtlWdgtProv");

    private static boolean sReceiversRegistered;

    /**
     * Compute optimal font and icon sizes offscreen for both portrait and landscape orientations
     * using the last known widget size and apply them to the widget.
     */
    private static void relayoutWidget(Context context, AppWidgetManager wm, int widgetId, Bundle options) {
        final RemoteViews portrait = relayoutWidget(context, wm, widgetId, options, true);
        final RemoteViews landscape = relayoutWidget(context, wm, widgetId, options, false);
        final RemoteViews widget = new RemoteViews(landscape, portrait);
        wm.updateAppWidget(widgetId, widget);
    }

    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
    }

    /**
     * Compute optimal font and icon sizes offscreen for the given orientation.
     */
    private static RemoteViews relayoutWidget(Context context, AppWidgetManager wm, int widgetId,
                                              Bundle options, boolean portrait) {

        // Create a remote view for the digital clock.
        final String packageName = context.getPackageName();
        final boolean isBackgroundDisplayedOnWidget = DataModel.getDataModel().isBackgroundDisplayedOnVerticalDigitalWidget();
        final RemoteViews rv = new RemoteViews(packageName, isBackgroundDisplayedOnWidget
                ? R.layout.vertical_digital_widget_with_background
                : R.layout.vertical_digital_widget
        );

        // Tapping on the widget opens the app (if not on the lock screen).
        if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
            final Intent openApp = new Intent(context, DeskClock.class);
            final PendingIntent pi = PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.verticalDigitalWidget, pi);
        }

        // Configure child views of the remote view.
        final CharSequence dateFormat = getDateFormat(context);
        rv.setCharSequence(R.id.date, "setFormat12Hour", dateFormat);
        rv.setCharSequence(R.id.date, "setFormat24Hour", dateFormat);

        final String nextAlarmTime = AlarmUtils.getNextAlarm(context);
        if (TextUtils.isEmpty(nextAlarmTime)) {
            rv.setViewVisibility(R.id.nextAlarm, GONE);
            rv.setViewVisibility(R.id.nextAlarmIcon, GONE);
        } else {
            rv.setTextViewText(R.id.nextAlarm, nextAlarmTime);
            rv.setViewVisibility(R.id.nextAlarm, VISIBLE);
            rv.setViewVisibility(R.id.nextAlarmIcon, VISIBLE);
        }

        if (options == null) {
            options = wm.getAppWidgetOptions(widgetId);
        }

        // Fetch the widget size selected by the user.
        final Resources resources = context.getResources();
        final float density = resources.getDisplayMetrics().density;
        final int minWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_WIDTH));
        final int minHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_HEIGHT));
        final int maxWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_WIDTH));
        final int maxHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_HEIGHT));
        final int targetWidthPx = portrait ? minWidthPx : maxWidthPx;
        final int targetHeightPx = portrait ? maxHeightPx : minHeightPx;
        final String maxClockFontSize = DataModel.getDataModel().getVerticalDigitalWidgetMaxClockFontSize();
        final int largestClockFontSizePx = Utils.toPixel(Integer.parseInt(maxClockFontSize), context);

        // Create a size template that describes the widget bounds.
        final Sizes template = new Sizes(targetWidthPx, targetHeightPx, largestClockFontSizePx);

        // Compute optimal font sizes and icon sizes to fit within the widget bounds.
        final Sizes sizes = optimizeSizes(context, template, nextAlarmTime);
        if (LOGGER.isVerboseLoggable()) {
            LOGGER.v(sizes.toString());
        }

        // Apply the computed sizes to the remote views.
        rv.setImageViewBitmap(R.id.nextAlarmIcon, sizes.mIconBitmap);
        rv.setTextViewTextSize(R.id.date, COMPLEX_UNIT_PX, sizes.mFontSizePx);
        rv.setTextViewTextSize(R.id.nextAlarm, COMPLEX_UNIT_PX, sizes.mFontSizePx);
        rv.setTextViewTextSize(R.id.clockHours, COMPLEX_UNIT_PX, sizes.mClockFontSizePx);
        rv.setTextViewTextSize(R.id.clockMinutes, COMPLEX_UNIT_PX, sizes.mClockFontSizePx);

        // Apply the color to the hours.
        final boolean isDefaultHoursColor = DataModel.getDataModel().isVerticalDigitalWidgetDefaultHoursColor();
        final int customHoursColor = DataModel.getDataModel().getVerticalDigitalWidgetCustomHoursColor();

        if (isDefaultHoursColor) {
            rv.setTextColor(R.id.clockHours, Color.WHITE);
        } else {
            rv.setTextColor(R.id.clockHours, customHoursColor);
        }

        // Apply the color to the minutes.
        final boolean isDefaultMinutesColor = DataModel.getDataModel().isVerticalDigitalWidgetDefaultMinutesColor();
        final int customMinutesColor = DataModel.getDataModel().getVerticalDigitalWidgetCustomMinutesColor();

        if (isDefaultMinutesColor) {
            rv.setTextColor(R.id.clockMinutes, Color.WHITE);
        } else {
            rv.setTextColor(R.id.clockMinutes, customMinutesColor);
        }

        // Apply the color to the date.
        final boolean isDefaultDateColor = DataModel.getDataModel().isVerticalDigitalWidgetDefaultDateColor();
        final int customDateColor = DataModel.getDataModel().getVerticalDigitalWidgetCustomDateColor();

        if (isDefaultDateColor) {
            rv.setTextColor(R.id.date, Color.WHITE);
        } else {
            rv.setTextColor(R.id.date, customDateColor);
        }

        // Apply the color to the next alarm.
        final boolean isDefaultNextAlarmColor = DataModel.getDataModel().isVerticalDigitalWidgetDefaultNextAlarmColor();
        final int customNextAlarmColor = DataModel.getDataModel().getVerticalDigitalWidgetCustomNextAlarmColor();

        if (isDefaultNextAlarmColor) {
            rv.setTextColor(R.id.nextAlarm, Color.WHITE);
        } else {
            rv.setTextColor(R.id.nextAlarm, customNextAlarmColor);
        }

        // Apply the color to the digital widget background.
        int backgroundColor = DataModel.getDataModel().getVerticalDigitalWidgetBackgroundColor();
        rv.setInt(R.id.digitalWidgetBackground, "setBackgroundColor", backgroundColor);

        return rv;
    }

    /**
     * Inflate an offscreen copy of the widget views. Binary search through the range of sizes until
     * the optimal sizes that fit within the widget bounds are located.
     */
    private static Sizes optimizeSizes(Context context, Sizes template, String nextAlarmTime) {
        // Inflate a test layout to compute sizes at different font sizes.
        final LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") final View sizer =
                inflater.inflate(R.layout.vertical_digital_widget_sizer, null);

        // Configure the date to display the current date string.
        final CharSequence dateFormat = getDateFormat(context);
        final TextClock date = sizer.findViewById(R.id.date);
        date.setFormat12Hour(dateFormat);
        date.setFormat24Hour(dateFormat);

        // Configure the next alarm views to display the next alarm time or be gone.
        final TextView nextAlarmIcon = sizer.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarm = sizer.findViewById(R.id.nextAlarm);
        if (TextUtils.isEmpty(nextAlarmTime)) {
            nextAlarm.setVisibility(GONE);
            nextAlarmIcon.setVisibility(GONE);
        } else {
            nextAlarm.setText(nextAlarmTime);
            nextAlarm.setVisibility(VISIBLE);
            nextAlarmIcon.setVisibility(VISIBLE);
            nextAlarmIcon.setTypeface(UiDataModel.getUiDataModel().getAlarmIconTypeface());
            // Apply the color to the next alarm icon.
            final boolean isDefaultNextAlarmColor = DataModel.getDataModel().isVerticalDigitalWidgetDefaultNextAlarmColor();
            final int customNextAlarmColor = DataModel.getDataModel().getVerticalDigitalWidgetCustomNextAlarmColor();

            if (isDefaultNextAlarmColor) {
                nextAlarmIcon.setTextColor(Color.WHITE);
            } else {
                nextAlarmIcon.setTextColor(customNextAlarmColor);
            }
        }

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
     * Compute all font and icon sizes based on the given {@code clockFontSize} and apply them to
     * the offscreen {@code sizer} view. Measure the {@code sizer} view and return the resulting
     * size measurements.
     */
    private static Sizes measure(Sizes template, int clockFontSize, View sizer) {
        // Create a copy of the given template sizes.
        final Sizes measuredSizes = template.newSize();

        // Configure the clock to display the widest time string.
        final TextClock date = sizer.findViewById(R.id.date);
        final TextClock hours = sizer.findViewById(R.id.clockHours);
        final TextClock minutes = sizer.findViewById(R.id.clockMinutes);
        final TextView nextAlarm = sizer.findViewById(R.id.nextAlarm);
        final TextView nextAlarmIcon = sizer.findViewById(R.id.nextAlarmIcon);
        // On some devices, the text shadow is cut off, so we have to add it to the end of the next alarm text.
        // The result is that next alarm text and the icon are perfectly centered.
        final int textShadowPadding = Utils.toPixel(3, sizer.getContext());

        // Adjust the font sizes.
        measuredSizes.setClockFontSizePx(clockFontSize);

        hours.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mClockFontSizePx);
        minutes.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mClockFontSizePx);
        date.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarm.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarm.setPadding(0, 0, measuredSizes.mIconFontSizePx + textShadowPadding, 0);
        nextAlarmIcon.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
        nextAlarmIcon.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);

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

        measuredSizes.mMeasuredTextClockWidthPx = hours.getMeasuredWidth();
        measuredSizes.mMeasuredTextClockHeightPx = hours.getMeasuredHeight();
        measuredSizes.mMeasuredTextClockMinutesWidthPx = hours.getMeasuredWidth();
        measuredSizes.mMeasuredTextClockMinutesHeightPx = hours.getMeasuredHeight();

        // If an alarm icon is required, generate one from the TextView with the special font.
        if (nextAlarmIcon.getVisibility() == VISIBLE) {
            measuredSizes.mIconBitmap = Utils.createBitmap(nextAlarmIcon);
        }

        return measuredSizes;
    }

    /**
     * @return the locale-specific date pattern
     */
    private static String getDateFormat(Context context) {
        final Locale locale = Locale.getDefault();
        final String skeleton = context.getString(R.string.abbrev_wday_month_day_no_year);
        return DateFormat.getBestDateTimePattern(locale, skeleton);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        LOGGER.i("onReceive: " + intent);
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
                case ACTION_NEXT_ALARM_CLOCK_CHANGED:
                case ACTION_LOCALE_CHANGED:
                case ACTION_TIME_CHANGED:
                case ACTION_TIMEZONE_CHANGED:
                case ACTION_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED:
                case ACTION_UPDATE_WIDGETS_AFTER_RESTORE:
                    for (int widgetId : widgetIds) {
                        relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId));
                    }
            }
        }

        final DataModel dm = DataModel.getDataModel();
        dm.updateWidgetCount(getClass(), widgetIds.length, R.string.category_digital_widget);
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private static void registerReceivers(Context context, BroadcastReceiver receiver) {
        if (sReceiversRegistered) return;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CONFIGURATION_CHANGED);
        intentFilter.addAction(ACTION_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED);
        intentFilter.addAction(ACTION_UPDATE_WIDGETS_AFTER_RESTORE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getApplicationContext().registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            context.getApplicationContext().registerReceiver(receiver, intentFilter);
        }
        sReceiversRegistered = true;
    }

    /**
     * Called when the app widget changes sizes.
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager wm, int widgetId, Bundle options) {
        super.onAppWidgetOptionsChanged(context, wm, widgetId, options);

        // scale the fonts of the clock to fit inside the new size
        relayoutWidget(context, AppWidgetManager.getInstance(context), widgetId, options);
    }

    /**
     * This class stores the target size of the widget as well as the measured size using a given
     * clock font size. All other fonts and icons are scaled proportional to the clock font.
     */
    private static final class Sizes {

        private final int mTargetWidthPx;
        private final int mTargetHeightPx;
        private final int mLargestClockFontSizePx;
        private final int mSmallestClockFontSizePx;
        private Bitmap mIconBitmap;

        private int mMeasuredWidthPx;
        private int mMeasuredHeightPx;
        private int mMeasuredTextClockWidthPx;
        private int mMeasuredTextClockHeightPx;
        private int mMeasuredTextClockMinutesWidthPx;
        private int mMeasuredTextClockMinutesHeightPx;

        /**
         * The size of the font to use on the date / next alarm time fields.
         */
        private int mFontSizePx;

        /**
         * The size of the font to use on the clock field.
         */
        private int mClockFontSizePx;

        private int mIconFontSizePx;
        private int mIconPaddingPx;

        private Sizes(int targetWidthPx, int targetHeightPx, int largestClockFontSizePx) {
            mTargetWidthPx = targetWidthPx;
            mTargetHeightPx = targetHeightPx;
            mLargestClockFontSizePx = largestClockFontSizePx;
            mSmallestClockFontSizePx = 1;
        }

        private static void append(StringBuilder builder, String format, Object... args) {
            builder.append(String.format(Locale.ENGLISH, format, args));
        }

        private int getLargestClockFontSizePx() {
            return mLargestClockFontSizePx;
        }

        private int getSmallestClockFontSizePx() {
            return mSmallestClockFontSizePx;
        }

        private int getClockFontSizePx() {
            return mClockFontSizePx;
        }

        private void setClockFontSizePx(int clockFontSizePx) {
            mClockFontSizePx = clockFontSizePx;
            mFontSizePx = max(1, round(clockFontSizePx / 5f));
            mIconFontSizePx = (int) (mFontSizePx * 1.4f);
            mIconPaddingPx = mFontSizePx / 3;
        }

        private boolean hasViolations() {
            return mMeasuredWidthPx > mTargetWidthPx || mMeasuredHeightPx > mTargetHeightPx;
        }

        private Sizes newSize() {
            return new Sizes(mTargetWidthPx, mTargetHeightPx, mLargestClockFontSizePx);
        }

        @NonNull
        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder(1000);
            builder.append("\n");
            append(builder, "Target dimensions: %dpx x %dpx\n", mTargetWidthPx, mTargetHeightPx);
            append(builder, "Last valid widget container measurement: %dpx x %dpx\n", mMeasuredWidthPx, mMeasuredHeightPx);
            append(builder, "Last text clock measurement: %dpx x %dpx\n", mMeasuredTextClockWidthPx, mMeasuredTextClockHeightPx);
            append(builder, "Last text clock minutes measurement: %dpx x %dpx\n", mMeasuredTextClockMinutesWidthPx, mMeasuredTextClockMinutesHeightPx);
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
    }
}
