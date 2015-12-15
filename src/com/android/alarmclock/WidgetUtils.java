/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.alarmclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;

public class WidgetUtils {
    static final String TAG = "WidgetUtils";

    public static void setClockSize(Context context, RemoteViews clock, float scale) {
        float fontSize = context.getResources().getDimension(R.dimen.widget_big_font_size);
        clock.setTextViewTextSize(
                R.id.the_clock, TypedValue.COMPLEX_UNIT_PX, fontSize * scale);
    }

    // Calculate the scale factor of the fonts in the widget
    public static float getScaleRatio(Context context, Bundle options, int id) {
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
            Resources res = context.getResources();
            float density = res.getDisplayMetrics().density;
            float ratio = (density * minWidth) / res.getDimension(R.dimen.min_digital_widget_width);
            ratio = Math.min(ratio, getHeightScaleRatio(context, options, id));
            ratio *= .83f;

            final SelectedCitiesRunnable selectedCitiesRunnable = new SelectedCitiesRunnable();
            DataModel.getDataModel().run(selectedCitiesRunnable);
            if (selectedCitiesRunnable.mAnyCitiesSelected) {
                return (ratio > 1f) ? 1f : ratio;
            }

            ratio = Math.min(ratio, 1.6f);
            if (res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                ratio = Math.max(ratio, .71f);
            }
            else {
                ratio = Math.max(ratio, .45f);
            }
            return ratio;
        }
        return 1f;
    }

    // Calculate the scale factor of the fonts in the list of  the widget using the widget height
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
            Resources res = context.getResources();
            float density = res.getDisplayMetrics().density;
            float ratio = density * minHeight / res.getDimension(R.dimen.min_digital_widget_height);
            if (res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                return ratio * 1.75f;
            }
            return ratio;
        }
        return 1;
    }


    // Decide if to show the list of world clock.
    // Check to see if the widget size is big enough, if it is return true.
    public static boolean showList(Context context, int id, float scale) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        if (widgetManager == null) {
            // no manager to make the calculation, show the list anyway
            return true;
        }
        Bundle options = widgetManager.getAppWidgetOptions(id);
        if (options == null) {
            // no data to make the calculation, show the list anyway
            return true;
        }
        Resources res = context.getResources();
        String whichHeight = res.getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT
                ? AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                : AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
        int height = options.getInt(whichHeight);
        if (height == 0) {
            // no data to make the calculation, show the list anyway
            return true;
        }
        float density = res.getDisplayMetrics().density;
        // Estimate height of date text box
        float lblBox = 1.35f * res.getDimension(R.dimen.label_font_size);
        float neededSize = res.getDimension(R.dimen.digital_widget_list_min_fixed_height) +
                2 * lblBox +
                scale * res.getDimension(R.dimen.digital_widget_list_min_scaled_height);
        return ((density * height) > neededSize);
    }

    /***
     * Set the format of the time on the clock according to the locale
     * @param context - Context used to get user's locale and time preferences
     * @param clock - view to format
     * @param showAmPm - show am/pm label if true
     * @param clockId - id of TextClock view as defined in the clock's layout.
     */
    public static void setTimeFormat(Context context, RemoteViews clock, boolean showAmPm,
            int clockId) {
        if (clock != null) {
            // Set the best format for 12 hours mode according to the locale
            clock.setCharSequence(clockId, "setFormat12Hour",
                    Utils.get12ModeFormat(context, showAmPm));
            // Set the best format for 24 hours mode according to the locale
            clock.setCharSequence(clockId, "setFormat24Hour", Utils.get24ModeFormat());
        }
    }

    private static class SelectedCitiesRunnable implements Runnable {

        private boolean mAnyCitiesSelected;

        @Override
        public void run() {
            mAnyCitiesSelected = !DataModel.getDataModel().getSelectedCities().isEmpty();
        }
    }
}

