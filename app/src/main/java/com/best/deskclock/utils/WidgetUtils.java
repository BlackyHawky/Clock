/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY;
import static android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.widget.TextClock;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.best.deskclock.R;
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
            final Resources res = context.getResources();
            float density = res.getDisplayMetrics().density;
            final int minDigitalWidgetWidth = ThemeUtils.convertDpToPixels(ThemeUtils.isTablet() ? 300 : 206, context);
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
            final Resources res = context.getResources();
            float density = res.getDisplayMetrics().density;
            final int minDigitalWidgetHeight = ThemeUtils.convertDpToPixels(ThemeUtils.isTablet() ? 170 : 129, context);
            float ratio = density * minHeight / minDigitalWidgetHeight;
            if (ThemeUtils.isPortrait()) {
                return ratio * 1.75f;
            }
            return ratio;
        }
        return 1;
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
     * @return "11:59" or "23:59" in the current locale
     */
    public static CharSequence getLongestTimeString(TextClock clock, boolean isMaterialYou) {
        final SharedPreferences prefs = getDefaultSharedPreferences(clock.getContext());
        boolean includeSeconds = isMaterialYou
                ? WidgetDAO.areSecondsDisplayedOnMaterialYouDigitalWidget(prefs)
                : WidgetDAO.areSecondsDisplayedOnDigitalWidget(prefs);
        final CharSequence format = clock.is24HourModeEnabled()
                ? ClockUtils.get24ModeFormat(clock.getContext(), includeSeconds)
                : ClockUtils.get12ModeFormat(clock.getContext(), getAmPmRatio(isMaterialYou, prefs), includeSeconds);
        final Calendar longestPMTime = Calendar.getInstance();
        longestPMTime.set(0, 0, 0, 23, 59);
        return DateFormat.format(format, longestPMTime);
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
                widgetProviderClass.getMethod("updateAppWidget", Context.class, AppWidgetManager.class, int.class)
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
