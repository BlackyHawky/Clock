/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.alarmclock;

import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY;
import static android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.StringRes;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.utils.ThemeUtils;

public class WidgetUtils {

    // For all widgets
    public static final String ACTION_NEXT_ALARM_LABEL_CHANGED = "com.best.alarmclock.NEXT_ALARM_LABEL_CHANGED";
    public static final String ACTION_LANGUAGE_CODE_CHANGED = "com.best.alarmclock.LANGUAGE_CODE_CHANGED";
    public static final String ACTION_UPDATE_WIDGETS_AFTER_RESTORE = "com.best.alarmclock.UPDATE_WIDGETS_AFTER_RESTORE";

    // For digital and Material You digital widgets
    public static final String ACTION_WORLD_CITIES_CHANGED = "com.best.alarmclock.WORLD_CITIES_CHANGED";

    // For standard widgets
    public static final String ACTION_DIGITAL_WIDGET_CUSTOMIZED = "com.best.alarmclock.DIGITAL_WIDGET_CUSTOMIZED";
    public static final String ACTION_NEXT_ALARM_WIDGET_CUSTOMIZED = "com.best.alarmclock.NEXT_ALARM_WIDGET_CUSTOMIZED";
    public static final String ACTION_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED = "com.best.alarmclock.VERTICAL_DIGITAL_WIDGET_CUSTOMIZED";

    // For Material You widgets
    public static final String ACTION_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED = "com.best.alarmclock.MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZED";
    public static final String ACTION_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZED = "com.best.alarmclock.MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZED";
    public static final String ACTION_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED = "com.best.alarmclock.MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZED";

    /**
     * Static variable to know if the fragment displayed comes from the widget or from the settings.
     * <p>
     * When the user presses the back button, it lets you know whether the activity should stop
     * or whether to return to the settings page.
     */
    public static boolean isLaunchedFromWidget = false;

    /**
     * Method to reset the flag if necessary.
     * <p>
     * Must be called in the onStop() method in every file that handles widgets fragments.
     */
    public static void resetLaunchFlag() {
        isLaunchedFromWidget = false;
    }

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
     * @param widgetClass     indicates the type of widget being counted
     * @param count           the number of widgets of the given type
     * @param eventCategoryId identifies the category of event to send
     */
    public static void updateWidgetCount(Context context, Class<?> widgetClass, int count, @StringRes int eventCategoryId) {
        int delta = WidgetDAO.updateWidgetCount(getDefaultSharedPreferences(context), widgetClass, count);
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

}
