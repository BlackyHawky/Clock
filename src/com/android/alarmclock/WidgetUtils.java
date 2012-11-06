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
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.android.deskclock.R;

public class WidgetUtils {
    static final String TAG = "WidgetUtils";

    public static void setClockSize(Context context, RemoteViews clock, float scale) {
        float fontSize = context.getResources().getDimension(R.dimen.widget_big_font_size);
        clock.setTextViewTextSize(
                R.id.the_clock, TypedValue.COMPLEX_UNIT_PX, fontSize * scale);
    }

    // Calculate the scale factor of the fonts in the widget
    public static float getScaleRatio(Context context, Bundle options, int id) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        if (options == null) {
            options = widgetManager.getAppWidgetOptions(id);
        }
        if (options != null) {
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            if (minWidth == 0 || minHeight == 0) {
                return 1f;
            }
            Log.v(TAG,"------------------------- " + minWidth + " , " + minHeight);
            Resources res = context.getResources();
            float ratio= minWidth / res.getDimension(R.dimen.min_digital_widget_width);
            Log.v(TAG,"------------------------- ratio " + ratio);

            return (ratio > 1) ? 1 : ratio;
        }
        return 1;
    }

    // Decide if to show the list of world clock.
    // Check to see if the widget size is big enough, if it is return true.
    public static boolean showList(Context context, int clock) {
        // Calculate
        return true;
    }


}

