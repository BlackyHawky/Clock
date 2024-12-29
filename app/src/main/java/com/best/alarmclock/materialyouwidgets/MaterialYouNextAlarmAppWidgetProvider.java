// SPDX-License-Identifier: GPL-3.0-only

package com.best.alarmclock.materialyouwidgets;

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

import static com.best.deskclock.data.WidgetModel.ACTION_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZED;
import static com.best.deskclock.data.WidgetModel.ACTION_NEXT_ALARM_LABEL_CHANGED;
import static com.best.deskclock.data.WidgetModel.ACTION_UPDATE_WIDGETS_AFTER_RESTORE;

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
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;
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
 *        Next alarm
 *        Alarm title
 *      ⏰ THU 9:30 AM
 * </pre>
 * <p>
 * If no alarm is scheduled to ring in the future:
 * <pre>
 *      No upcoming alarm
 * </pre>
 * <p>
 * This widget is scaling the font sizes to fit within the widget bounds chosen by the user without
 * any clipping. To do so it measures layouts offscreen using a range of font sizes in order to
 * choose optimal values.
 */
public class MaterialYouNextAlarmAppWidgetProvider extends AppWidgetProvider {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("MYNextAlarmWdgtProv");

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

        // Create a remote view for the next alarm.
        final String packageName = context.getPackageName();
        final RemoteViews rv = new RemoteViews(packageName, R.layout.material_you_next_alarm_widget);

        // Tapping on the widget opens the app (if not on the lock screen).
        if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
            final Intent openApp = new Intent(context, DeskClock.class);
            final PendingIntent pi = PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.material_you_next_alarm_widget, pi);
        }

        // Apply color to the next alarm and the next alarm title.
        // The default color is defined in the xml files to match the device's day/night theme.
        final String nextAlarmTime = AlarmUtils.getNextAlarm(context);
        final String nextAlarmTitle = AlarmUtils.getNextAlarmTitle(context);
        final String nextAlarmText = context.getString(R.string.next_alarm_widget_text);
        final String noAlarmTitle = context.getString(R.string.next_alarm_widget_title_no_alarm);
        final boolean isDefaultTitleColor = DataModel.getDataModel().isMaterialYouNextAlarmWidgetDefaultTitleColor();
        final boolean isDefaultAlarmTitleColor = DataModel.getDataModel().isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor();
        final boolean isDefaultAlarmColor = DataModel.getDataModel().isMaterialYouNextAlarmWidgetDefaultAlarmColor();
        final int customTitleColor = DataModel.getDataModel().getMaterialYouNextAlarmWidgetCustomTitleColor();
        final int customAlarmTitleColor = DataModel.getDataModel().getMaterialYouNextAlarmWidgetCustomAlarmTitleColor();
        final int customAlarmColor = DataModel.getDataModel().getMaterialYouNextAlarmWidgetCustomAlarmColor();

        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            rv.setViewVisibility(R.id.nextAlarmTitle, GONE);
            rv.setViewVisibility(R.id.nextAlarmTitleForCustomColor, GONE);
        } else {
            if (isDefaultAlarmTitleColor) {
                rv.setViewVisibility(R.id.nextAlarmTitle, VISIBLE);
                rv.setViewVisibility(R.id.nextAlarmTitleForCustomColor, GONE);
                rv.setTextViewText(R.id.nextAlarmTitle, nextAlarmTitle);
            } else {
                rv.setViewVisibility(R.id.nextAlarmTitle, GONE);
                rv.setViewVisibility(R.id.nextAlarmTitleForCustomColor, VISIBLE);
                rv.setTextViewText(R.id.nextAlarmTitleForCustomColor, nextAlarmTitle);
                rv.setTextColor(R.id.nextAlarmTitleForCustomColor, customAlarmTitleColor);
            }
        }

        if (TextUtils.isEmpty(nextAlarmTime)) {
            rv.setViewVisibility(R.id.nextAlarm, GONE);
            rv.setViewVisibility(R.id.nextAlarmIcon, GONE);
            rv.setViewVisibility(R.id.nextAlarmForCustomColor, GONE);
            rv.setViewVisibility(R.id.nextAlarmIconForCustomColor, GONE);
            if (isDefaultTitleColor) {
                rv.setViewVisibility(R.id.nextAlarmText, VISIBLE);
                rv.setViewVisibility(R.id.nextAlarmTextForCustomColor, GONE);
                rv.setTextViewText(R.id.nextAlarmText, noAlarmTitle);
            } else {
                rv.setViewVisibility(R.id.nextAlarmText, GONE);
                rv.setViewVisibility(R.id.nextAlarmTextForCustomColor, VISIBLE);
                rv.setTextViewText(R.id.nextAlarmTextForCustomColor, noAlarmTitle);
                rv.setTextColor(R.id.nextAlarmTextForCustomColor, customTitleColor);
            }
        } else {
            if (isDefaultTitleColor) {
                rv.setViewVisibility(R.id.nextAlarmText, VISIBLE);
                rv.setViewVisibility(R.id.nextAlarmTextForCustomColor, GONE);
                rv.setTextViewText(R.id.nextAlarmText, nextAlarmText);
            } else {
                rv.setViewVisibility(R.id.nextAlarmText, GONE);
                rv.setViewVisibility(R.id.nextAlarmTextForCustomColor, VISIBLE);
                rv.setTextViewText(R.id.nextAlarmTextForCustomColor, nextAlarmText);
                rv.setTextColor(R.id.nextAlarmTextForCustomColor, customTitleColor);
            }

            if (isDefaultAlarmColor) {
                rv.setViewVisibility(R.id.nextAlarm, VISIBLE);
                rv.setViewVisibility(R.id.nextAlarmIcon, VISIBLE);
                rv.setViewVisibility(R.id.nextAlarmForCustomColor, GONE);
                rv.setViewVisibility(R.id.nextAlarmIconForCustomColor, GONE);
                rv.setTextViewText(R.id.nextAlarm, nextAlarmTime);
            } else {
                rv.setViewVisibility(R.id.nextAlarm, GONE);
                rv.setViewVisibility(R.id.nextAlarmIcon, GONE);
                rv.setViewVisibility(R.id.nextAlarmForCustomColor, VISIBLE);
                rv.setViewVisibility(R.id.nextAlarmIconForCustomColor, VISIBLE);
                rv.setTextViewText(R.id.nextAlarmForCustomColor, nextAlarmTime);
                rv.setTextColor(R.id.nextAlarmForCustomColor, customAlarmColor);
            }
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
        final String widgetMaxFontSize =
                DataModel.getDataModel().getMaterialYouNextAlarmWidgetMaxFontSize();
        final int largestFontSizePx = Utils.toPixel(Integer.parseInt(widgetMaxFontSize), context);

        // Create a size template that describes the widget bounds.
        final Sizes template = new Sizes(targetWidthPx, targetHeightPx, largestFontSizePx);

        // Compute optimal font sizes and icon sizes to fit within the widget bounds.
        final Sizes sizes = optimizeSizes(context, template, nextAlarmTime);
        if (LOGGER.isVerboseLoggable()) {
            LOGGER.v(sizes.toString());
        }

        // Apply the computed sizes to the remote views.
        rv.setTextViewTextSize(R.id.nextAlarmText, COMPLEX_UNIT_PX, sizes.mFontSizePx);
        rv.setTextViewTextSize(R.id.nextAlarmTitle, COMPLEX_UNIT_PX, sizes.mFontSizePx);
        rv.setImageViewBitmap(R.id.nextAlarmIcon, sizes.mIconBitmap);
        rv.setTextViewTextSize(R.id.nextAlarm, COMPLEX_UNIT_PX, sizes.mFontSizePx);

        rv.setTextViewTextSize(R.id.nextAlarmTextForCustomColor, COMPLEX_UNIT_PX, sizes.mFontSizePx);
        rv.setTextViewTextSize(R.id.nextAlarmTitleForCustomColor, COMPLEX_UNIT_PX, sizes.mFontSizePx);
        rv.setImageViewBitmap(R.id.nextAlarmIconForCustomColor, sizes.mIconBitmap);
        rv.setTextViewTextSize(R.id.nextAlarmForCustomColor, COMPLEX_UNIT_PX, sizes.mFontSizePx);

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
                inflater.inflate(R.layout.material_you_next_alarm_widget_sizer, null);

        // Configure the next alarm views to display the next alarm time or be gone.
        final String nextAlarmTitle = AlarmUtils.getNextAlarmTitle(context);
        final TextView nextAlarmTitleView = sizer.findViewById(R.id.nextAlarmTitle);
        final TextView nextAlarmText = sizer.findViewById(R.id.nextAlarmText);
        final TextView nextAlarmIcon = sizer.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarm = sizer.findViewById(R.id.nextAlarm);
        final TextView nextAlarmTitleViewForCustomColor = sizer.findViewById(R.id.nextAlarmTitleForCustomColor);
        final TextView nextAlarmTextForCustomColor = sizer.findViewById(R.id.nextAlarmTextForCustomColor);
        final TextView nextAlarmIconForCustomColor = sizer.findViewById(R.id.nextAlarmIconForCustomColor);
        final TextView nextAlarmForCustomColor = sizer.findViewById(R.id.nextAlarmForCustomColor);
        final boolean isDefaultTitleColor = DataModel.getDataModel().isMaterialYouNextAlarmWidgetDefaultTitleColor();
        final boolean isDefaultAlarmTitleColor = DataModel.getDataModel().isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor();
        final boolean isDefaultAlarmColor = DataModel.getDataModel().isMaterialYouNextAlarmWidgetDefaultAlarmColor();
        final int customTitleColor = DataModel.getDataModel().getMaterialYouNextAlarmWidgetCustomTitleColor();
        final int customAlarmTitleColor = DataModel.getDataModel().getMaterialYouNextAlarmWidgetCustomAlarmTitleColor();
        final int customAlarmColor = DataModel.getDataModel().getMaterialYouNextAlarmWidgetCustomAlarmColor();

        if (TextUtils.isEmpty(nextAlarmTime) || TextUtils.isEmpty(nextAlarmTitle)) {
            nextAlarmTitleView.setVisibility(GONE);
            nextAlarmTitleViewForCustomColor.setVisibility(GONE);
        } else {
            if (isDefaultAlarmTitleColor) {
                nextAlarmTitleView.setVisibility(VISIBLE);
                nextAlarmTitleViewForCustomColor.setVisibility(GONE);
                nextAlarmTitleView.setText(nextAlarmTitle);
            } else {
                nextAlarmTitleView.setVisibility(GONE);
                nextAlarmTitleViewForCustomColor.setVisibility(VISIBLE);
                nextAlarmTitleView.setText(nextAlarmTitle);
                nextAlarmTitleViewForCustomColor.setTextColor(customAlarmTitleColor);
            }
        }

        if (TextUtils.isEmpty(nextAlarmTime)) {
            nextAlarm.setVisibility(GONE);
            nextAlarmIcon.setVisibility(GONE);
            nextAlarmForCustomColor.setVisibility(GONE);
            nextAlarmIconForCustomColor.setVisibility(GONE);
            if (isDefaultTitleColor) {
                nextAlarmText.setVisibility(VISIBLE);
                nextAlarmTextForCustomColor.setVisibility(GONE);
                nextAlarmText.setText(context.getString(R.string.next_alarm_widget_title_no_alarm));
            } else {
                nextAlarmText.setVisibility(GONE);
                nextAlarmTextForCustomColor.setVisibility(VISIBLE);
                nextAlarmTextForCustomColor.setText(context.getString(R.string.next_alarm_widget_title_no_alarm));
                nextAlarmTextForCustomColor.setTextColor(customTitleColor);
            }
        } else {
            if (isDefaultTitleColor) {
                nextAlarmText.setVisibility(VISIBLE);
                nextAlarmTextForCustomColor.setVisibility(GONE);
                nextAlarmText.setText(context.getString(R.string.next_alarm_widget_text));
            } else {
                nextAlarmText.setVisibility(GONE);
                nextAlarmTextForCustomColor.setVisibility(VISIBLE);
                nextAlarmTextForCustomColor.setText(context.getString(R.string.next_alarm_widget_text));
                nextAlarmTextForCustomColor.setTextColor(customTitleColor);
            }

            if (isDefaultAlarmColor) {
                nextAlarm.setVisibility(VISIBLE);
                nextAlarmIcon.setVisibility(VISIBLE);
                nextAlarmForCustomColor.setVisibility(GONE);
                nextAlarmIconForCustomColor.setVisibility(GONE);
                nextAlarm.setText(nextAlarmTime);
                nextAlarmIcon.setTypeface(UiDataModel.getUiDataModel().getAlarmIconTypeface());
            } else {
                nextAlarm.setVisibility(GONE);
                nextAlarmIcon.setVisibility(GONE);
                nextAlarmForCustomColor.setVisibility(VISIBLE);
                nextAlarmIconForCustomColor.setVisibility(VISIBLE);
                nextAlarmForCustomColor.setText(nextAlarmTime);
                nextAlarmForCustomColor.setTextColor(customAlarmColor);
                nextAlarmIconForCustomColor.setTypeface(UiDataModel.getUiDataModel().getAlarmIconTypeface());
                nextAlarmIconForCustomColor.setTextColor(customAlarmColor);
            }
        }

        // Measure the widget at the largest possible size.
        Sizes high = measure(template, template.getLargestNextAlarmFontSizePx(), sizer);
        if (!high.hasViolations()) {
            return high;
        }

        // Measure the widget at the smallest possible size.
        Sizes low = measure(template, template.getSmallestNextAlarmFontSizePx(), sizer);
        if (low.hasViolations()) {
            return low;
        }

        // Binary search between the smallest and largest sizes until an optimum size is found.
        while (low.getNextAlarmFontSizePx() != high.getNextAlarmFontSizePx()) {
            final int midFontSize = (low.getNextAlarmFontSizePx() + high.getNextAlarmFontSizePx()) / 2;
            if (midFontSize == low.getNextAlarmFontSizePx()) {
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
     * Compute all font and icon sizes based on the given {@code nextAlarmFontSize} and apply them to
     * the offscreen {@code sizer} view. Measure the {@code sizer} view and return the resulting
     * size measurements.
     */
    private static Sizes measure(Sizes template, int nextAlarmFontSize, View sizer) {
        // Create a copy of the given template sizes.
        final Sizes measuredSizes = template.newSize();

        // Configure the next alarm to display the widest time string.
        final TextView nextAlarmText = sizer.findViewById(R.id.nextAlarmText);
        final TextView nextAlarmTitle = sizer.findViewById(R.id.nextAlarmTitle);
        final TextView nextAlarm = sizer.findViewById(R.id.nextAlarm);
        final TextView nextAlarmIcon = sizer.findViewById(R.id.nextAlarmIcon);

        final TextView nextAlarmTextForCustomColor = sizer.findViewById(R.id.nextAlarmTextForCustomColor);
        final TextView nextAlarmTitleForCustomColor = sizer.findViewById(R.id.nextAlarmTitleForCustomColor);
        final TextView nextAlarmForCustomColor = sizer.findViewById(R.id.nextAlarmForCustomColor);
        final TextView nextAlarmIconForCustomColor = sizer.findViewById(R.id.nextAlarmIconForCustomColor);

        // Adjust the font sizes.
        measuredSizes.setNextAlarmFontSizePx(nextAlarmFontSize);

        nextAlarmText.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarmTitle.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarm.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarm.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);
        nextAlarmIcon.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
        nextAlarmIcon.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);

        nextAlarmTextForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarmTitleForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarmForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx);
        nextAlarmForCustomColor.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);
        nextAlarmIconForCustomColor.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mIconFontSizePx);
        nextAlarmIconForCustomColor.setPadding(0, 0, measuredSizes.mIconPaddingPx, 0);

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

        // If an alarm icon is required, generate one from the TextView with the special font.
        if (nextAlarmIcon.getVisibility() == VISIBLE) {
            measuredSizes.mIconBitmap = Utils.createBitmap(nextAlarmIcon);
        }

        if (nextAlarmIconForCustomColor.getVisibility() == VISIBLE) {
            measuredSizes.mIconBitmap = Utils.createBitmap(nextAlarmIconForCustomColor);
        }

        return measuredSizes;
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
                case ACTION_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZED:
                case ACTION_NEXT_ALARM_LABEL_CHANGED:
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
        intentFilter.addAction(ACTION_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZED);
        intentFilter.addAction(ACTION_NEXT_ALARM_LABEL_CHANGED);
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

        // scale the fonts of the next alarm to fit inside the new size
        relayoutWidget(context, AppWidgetManager.getInstance(context), widgetId, options);
    }

    /**
     * This class stores the target size of the widget as well as the measured size using a given
     * font size. Icons are scaled proportional to the next alarm font.
     */
    private static final class Sizes {

        private final int mTargetWidthPx;
        private final int mTargetHeightPx;
        private final int mLargestNextAlarmFontSizePx;
        private final int mSmallestNextAlarmFontSizePx;
        private Bitmap mIconBitmap;

        private int mMeasuredWidthPx;
        private int mMeasuredHeightPx;

        /**
         * The size of the font to use on next alarm time fields.
         */
        private int mFontSizePx;

        /**
         * The size of the font set for the next alarm field.
         */
        private int mNextAlarmFontSizePx;

        private int mIconFontSizePx;
        private int mIconPaddingPx;

        private Sizes(int targetWidthPx, int targetHeightPx, int largestNextAlarmFontSizePx) {
            mTargetWidthPx = targetWidthPx;
            mTargetHeightPx = targetHeightPx;
            mLargestNextAlarmFontSizePx = largestNextAlarmFontSizePx;
            mSmallestNextAlarmFontSizePx = 1;
        }

        private static void append(StringBuilder builder, String format, Object... args) {
            builder.append(String.format(Locale.ENGLISH, format, args));
        }

        private int getLargestNextAlarmFontSizePx() {
            return mLargestNextAlarmFontSizePx;
        }

        private int getSmallestNextAlarmFontSizePx() {
            return mSmallestNextAlarmFontSizePx;
        }

        private int getNextAlarmFontSizePx() {
            return mNextAlarmFontSizePx;
        }

        private void setNextAlarmFontSizePx(int nextAlarmFontSizePx) {
            mNextAlarmFontSizePx = nextAlarmFontSizePx;
            mFontSizePx = max(1, round(nextAlarmFontSizePx / 3f));
            mIconFontSizePx = (int) (mFontSizePx * 1.4f);
            mIconPaddingPx = mFontSizePx / 5;
        }

        private boolean hasViolations() {
            return mMeasuredWidthPx > mTargetWidthPx || mMeasuredHeightPx > mTargetHeightPx;
        }

        private Sizes newSize() {
            return new Sizes(mTargetWidthPx, mTargetHeightPx, mLargestNextAlarmFontSizePx);
        }

        @NonNull
        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder(1000);
            builder.append("\n");
            append(builder, "Target dimensions: %dpx x %dpx\n",
                    mTargetWidthPx, mTargetHeightPx);
            append(builder, "Last valid widget container measurement: %dpx x %dpx\n",
                    mMeasuredWidthPx, mMeasuredHeightPx);

            if (mMeasuredWidthPx > mTargetWidthPx) {
                append(builder, "Measured width %dpx exceeded widget width %dpx\n",
                        mMeasuredWidthPx, mTargetWidthPx);
            }
            if (mMeasuredHeightPx > mTargetHeightPx) {
                append(builder, "Measured height %dpx exceeded widget height %dpx\n",
                        mMeasuredHeightPx, mTargetHeightPx);
            }
            append(builder, "Next alarm font: %dpx\n", mNextAlarmFontSizePx);
            return builder.toString();
        }
    }
}
