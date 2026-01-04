/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY;
import static android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.widget.RemoteViews;
import android.widget.TextClock;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouAnalogAppWidgetProvider;
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouDigitalAppWidgetProvider;
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouNextAlarmAppWidgetProvider;
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouVerticalDigitalAppWidgetProvider;
import com.best.deskclock.widgets.standardwidgets.AnalogAppWidgetProvider;
import com.best.deskclock.widgets.standardwidgets.DigitalAppWidgetProvider;
import com.best.deskclock.widgets.standardwidgets.NextAlarmAppWidgetProvider;
import com.best.deskclock.widgets.standardwidgets.VerticalDigitalAppWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WidgetUtils {

    public static final String KEY_LAUNCHED_FROM_WIDGET = "launched_from_widget";

    private static final String METHOD_UPDATE_APP_WIDGET = "updateAppWidget";
    private static final String METHOD_SET_FORMAT_24 = "setFormat24Hour";
    private static final String METHOD_SET_FORMAT_12 = "setFormat12Hour";
    public static final String METHOD_SET_TIME_ZONE = "setTimeZone";
    public static final String METHOD_SET_IMAGE_ICON = "setImageIcon";

    public static final String EXTRA_CITY_INDEX = "city_index";
    public static final String EXTRA_WIDGET_ID = "widget_id";

    /**
     * Helper method to know if the fragment displayed comes from the widget or from the settings.
     */
    public static boolean isLaunchedFromWidget(@Nullable Bundle args) {
        return args != null && args.getBoolean(KEY_LAUNCHED_FROM_WIDGET, false);
    }

    /**
     * Adds a back-press callback that finishes the activity if the fragment was launched from a widget.
     *
     * @param fragment The fragment in which to add the back-press behavior.
     */
    public static void addFinishOnBackPressedIfLaunchedFromWidget(@NonNull Fragment fragment) {
        if (isLaunchedFromWidget(fragment.getArguments())) {
            fragment.requireActivity()
                    .getOnBackPressedDispatcher()
                    .addCallback(fragment, new OnBackPressedCallback(true) {
                        @Override
                        public void handleOnBackPressed() {
                            fragment.requireActivity().finish();
                        }
                    });
        }
    }

    /**
     * Suffix for a key to a preference that stores the instance count for a given widget type.
     */
    private static final String WIDGET_COUNT = "_widget_count";

    /**
     * Calculate the scale factor of the fonts in the widget
     */
    public static float getScaleRatio(Context context, Bundle options, int id, int cityCount) {
        if (options == null) {
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            if (widgetManager == null) {
                // no manager , do no scaling
                return 1f;
            }
            options = widgetManager.getAppWidgetOptions(id);
        }
        if (options != null) {
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            if (minWidth == 0) {
                // No data , do no scaling
                return 1f;
            }

            final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            float density = displayMetrics.density;
            final int minDigitalWidgetWidth = (int) dpToPx(ThemeUtils.isTablet() ? 300 : 206, displayMetrics);
            float ratio = (density * minWidth) / minDigitalWidgetWidth;
            ratio = Math.min(ratio, getHeightScaleRatio(context, options, id));
            ratio *= .83f;

            if (cityCount > 0) {
                return Math.min(ratio, 1f);
            }

            ratio = Math.min(ratio, 1.6f);
            if (ThemeUtils.isPortrait()) {
                ratio = Math.max(ratio, .71f);
            } else {
                ratio = Math.max(ratio, .45f);
            }
            return ratio;
        }
        return 1f;
    }

    /**
     * Calculate the scale factor of the fonts in the list of  the widget using the widget height
     */
    private static float getHeightScaleRatio(Context context, Bundle options, int id) {
        if (options == null) {
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            if (widgetManager == null) {
                // no manager , do no scaling
                return 1f;
            }
            options = widgetManager.getAppWidgetOptions(id);
        }
        if (options != null) {
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            if (minHeight == 0) {
                // No data , do no scaling
                return 1f;
            }

            final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            float density = displayMetrics.density;
            final int minDigitalWidgetHeight = (int) dpToPx(ThemeUtils.isTablet() ? 170 : 129, displayMetrics);
            float ratio = density * minHeight / minDigitalWidgetHeight;
            if (ThemeUtils.isPortrait()) {
                return ratio * 1.75f;
            }
            return ratio;
        }
        return 1;
    }

    /**
     * Combine two 32-bit stable ids into a single 64-bit stable id.
     *
     * <p>The inputs are treated as unsigned 32-bit values: `a` occupies the high 32 bits
     * (bits 63..32) and `b` the low 32 bits (bits 31..0). If the combined result equals 0L
     * (reserved/fallback), the method returns 1L to avoid using 0 as a valid id.
     *
     * @param a stable 32-bit id (placed in the high part of the long)
     * @param b stable 32-bit id (placed in the low part of the long)
     * @return a non-zero 64-bit combined id (returns 1L if the combined value would be 0L)
     */
    public static long combineStableIds(long a, long b) {
        long combined = ((a & 0xffffffffL) << 32) | (b & 0xffffffffL);
        return combined == 0L ? 1L : combined;
    }

    /**
     * Returns a stable identifier for a city based on its internal id.
     *
     * <p>Assumes city IDs follow the "C<number>" pattern (e.g. "C1", "C2").
     * Extracts the numeric part of the id and converts it to a long. If the city
     * object is null or the id is missing, the method returns 1L.
     *
     * @param city the City object to derive the stable id from (may be null)
     * @return a long representing the stable id extracted from the city id (or 1L as fallback)
     */
    public static long getStableIdForCity(City city) {
        if (city == null) {
            return 1L;
        }

        String id = city.getId();
        // City IDs are in the format C1, C2, C3, etc., so remove everything that is not a number
        String digits = id.replaceAll("\\D+", "");
        return Long.parseLong(digits);
    }

    /**
     * @param widgetProviderClass indicates the type of widget being counted
     * @param count               the number of widgets of the given type
     * @return the delta between the new count and the old count
     */
    public static int updateWidgetCount(SharedPreferences prefs, Class<?> widgetProviderClass, int count) {
        final String key = widgetProviderClass.getSimpleName() + WIDGET_COUNT;
        final int oldCount = prefs.getInt(key, 0);
        if (count == 0) {
            prefs.edit().remove(key).apply();
        } else {
            prefs.edit().putInt(key, count).apply();
        }
        return count - oldCount;
    }

    /**
     * @param widgetClass     indicates the type of widget being counted
     * @param count           the number of widgets of the given type
     * @param eventCategoryId identifies the category of event to send
     */
    public static void updateWidgetCount(Context context, Class<?> widgetClass, int count, @StringRes int eventCategoryId) {
        int delta = updateWidgetCount(getDefaultSharedPreferences(context), widgetClass, count);
        for (; delta > 0; delta--) {
            Events.sendEvent(eventCategoryId, R.string.action_create, 0);
        }
        for (; delta < 0; delta++) {
            Events.sendEvent(eventCategoryId, R.string.action_delete, 0);
        }
    }

    /**
     * @return {@code true} if the widget is being hosted in a container where tapping is allowed
     */
    public static boolean isWidgetClickable(AppWidgetManager widgetManager, int widgetId) {
        final Bundle wo = widgetManager.getAppWidgetOptions(widgetId);
        return wo != null && wo.getInt(OPTION_APPWIDGET_HOST_CATEGORY, -1) != WIDGET_CATEGORY_KEYGUARD;
    }

    /**
     * Creates a rounded icon with the specified dimensions, color, and corner radius.
     * <p>
     * This method generates a {@link Bitmap} with rounded corners using the given width,
     * height, fill color, and corner radius, then wraps it into an {@link Icon} object.
     * It is useful for dynamically creating background visuals for widgets.
     *
     * @param width  the width of the bitmap in pixels
     * @param height the height of the bitmap in pixels
     * @param color  the fill color of the rounded rectangle
     * @param radius the corner radius in pixels to apply to all four corners
     * @return an {@link Icon} containing the rounded bitmap
     */
    public static Icon createRoundedIcon(int width, int height, int color, int radius) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);

        RectF rect = new RectF(0, 0, width, height);
        canvas.drawRoundRect(rect, radius, radius, paint);

        return Icon.createWithBitmap(bitmap);
    }

    /**
     * @return the default Material You background color for day mode.
     */
    public static int getMaterialBackgroundColorDay(Context context) {
        return SdkUtils.isAtLeastAndroid12()
                ? ContextCompat.getColor(context, android.R.color.system_accent2_50)
                : Color.TRANSPARENT;
    }

    /**
     * @return the default Material You background color for night mode.
     */
    public static int getMaterialBackgroundColorNight(Context context) {
        return SdkUtils.isAtLeastAndroid12()
                ? ContextCompat.getColor(context, android.R.color.system_accent2_800)
                : Color.TRANSPARENT;
    }

    /**
     * @return "11:59" or "23:59" in the current locale
     */
    public static CharSequence getLongestTimeString(TextClock clock, boolean isMaterialYou) {
        final SharedPreferences prefs = getDefaultSharedPreferences(clock.getContext());
        boolean includeSeconds = isMaterialYou
                ? WidgetDAO.areSecondsDisplayedOnMaterialYouDigitalWidget(prefs)
                : WidgetDAO.areSecondsDisplayedOnDigitalWidget(prefs);
        final CharSequence format = clock.is24HourModeEnabled()
                ? ClockUtils.get24ModeFormat(includeSeconds, false)
                : ClockUtils.get12ModeFormat(clock.getContext(), getAmPmRatio(isMaterialYou, prefs),
                includeSeconds, false, false, false);
        final Calendar longestPMTime = Calendar.getInstance();
        longestPMTime.set(0, 0, 0, 23, 59);
        return DateFormat.format(format, longestPMTime);
    }

    /**
     * Configure the TextClock format on a RemoteViews instance.
     *
     * @param rv            RemoteViews to update
     * @param context       context for resources
     * @param clockViewId   the TextClock view id
     * @param amPmRatio     am/pm ratio for 12h format
     * @param showSeconds   whether seconds should be shown
     */
    public static void applyClockFormat(RemoteViews rv, Context context, int clockViewId, float amPmRatio,
                                        boolean showSeconds) {

        if (rv == null || clockViewId == 0) {
            return;
        }

        if (DataModel.getDataModel().is24HourFormat()) {
            rv.setCharSequence(clockViewId, METHOD_SET_FORMAT_24, ClockUtils.get24ModeFormat(
                    showSeconds, false));
        } else {
            rv.setCharSequence(clockViewId, METHOD_SET_FORMAT_12, ClockUtils.get12ModeFormat(
                    context, amPmRatio, showSeconds, false, false, false));
        }
    }

    /**
     * @return the ratio to use for the AM/PM part on the digital widgets.
     */
    public static float getAmPmRatio(boolean isMaterialYou, SharedPreferences prefs) {
        boolean areAmPmHidden = isMaterialYou
                ? WidgetDAO.isAmPmHiddenOnMaterialYouDigitalWidget(prefs)
                : WidgetDAO.isAmPmHiddenOnDigitalWidget(prefs);
        return areAmPmHidden ? 0 : 0.4f;
    }

    /**
     * @return The locale-specific date pattern.
     */
    public static String getDateFormat(Context context) {
        Locale locale = Locale.getDefault();
        final String skeleton = context.getString(R.string.abbrev_wday_month_day_no_year);
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale);

        return simpleDateFormat.format(new Date());
    }

    /**
     * Schedule alarm for daily widget update at midnight.
     */
    public static void scheduleDailyWidgetUpdate(Context context, Class<? extends BroadcastReceiver> receiverClass) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, receiverClass);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        getAlarmManager(context).setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        LogUtils.i("Alarm scheduled for widget receiver: " + receiverClass.getSimpleName());
    }

    /**
     * Helper method to cancel daily widget update.
     */
    public static void cancelDailyWidgetUpdate(Context context, Class<? extends BroadcastReceiver> receiverClass) {
        Intent intent = new Intent(context, receiverClass);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (getAlarmManager(context) != null && pendingIntent != null) {
            getAlarmManager(context).cancel(pendingIntent);
            pendingIntent.cancel();
        }

        LogUtils.i("Alarm cancelled for widget receiver: " + receiverClass.getSimpleName());
    }

    /**
     * Retrieves the system AlarmManager service, which allows scheduling and managing alarms.
     *
     * @param context The Context used to access the system service.
     * @return The AlarmManager system service instance, which can be used to set, cancel, or query alarms.
     */
    public static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Helper method to update a widget.
     * <p>Note: The widget provider class must declare a public static method named
     * {@code updateAppWidget(Context, AppWidgetManager, int)}.</p>
     */
    public static void updateWidget(Context context, Class<?> widgetProviderClass) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, widgetProviderClass);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(widget);

        for (int widgetId : widgetIds) {
            try {
                // Use the static "updateAppWidget()" method on the appropriate provider
                widgetProviderClass.getMethod(METHOD_UPDATE_APP_WIDGET, Context.class, AppWidgetManager.class, int.class)
                        .invoke(null, context, appWidgetManager, widgetId);
            } catch (Exception e) {
                LogUtils.e("Error updating widget " + widgetProviderClass.getSimpleName(), e);
            }
        }
    }

    /**
     * Helper method to update a specific widget with a 300ms delay.
     */
    public static void scheduleWidgetUpdate(Context context, Class<?> widgetProviderClass) {
        new Handler(context.getMainLooper()).postDelayed(() ->
                updateWidget(context, widgetProviderClass), 300);
    }

    /**
     * Helper method to update all widgets.
     */
    public static void updateAllWidgets(Context context) {
        Class<?>[] widgetProviders = {
                AnalogAppWidgetProvider.class,
                DigitalAppWidgetProvider.class,
                NextAlarmAppWidgetProvider.class,
                VerticalDigitalAppWidgetProvider.class,
                MaterialYouAnalogAppWidgetProvider.class,
                MaterialYouDigitalAppWidgetProvider.class,
                MaterialYouNextAlarmAppWidgetProvider.class,
                MaterialYouVerticalDigitalAppWidgetProvider.class
        };

        for (Class<?> provider : widgetProviders) {
            updateWidget(context, provider);
        }
    }

    /**
     * Helper method to update all digital widgets.
     */
    public static void updateAllDigitalWidgets(Context context) {
        Class<?>[] widgetProviders = {
                DigitalAppWidgetProvider.class,
                NextAlarmAppWidgetProvider.class,
                VerticalDigitalAppWidgetProvider.class,
                MaterialYouDigitalAppWidgetProvider.class,
                MaterialYouNextAlarmAppWidgetProvider.class,
                MaterialYouVerticalDigitalAppWidgetProvider.class
        };

        for (Class<?> provider : widgetProviders) {
            updateWidget(context, provider);
        }
    }
}
