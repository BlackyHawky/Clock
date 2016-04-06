/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * This class encapsulates the transfer of data between widget objects and their permanent storage
 * in {@link SharedPreferences}.
 */
final class WidgetDAO {

    /** Prefix for a key to a preference that stores the id of a city displayed in a widget. */
    private static final String WIDGET_CITY_ID_PREFIX = "widget_city_id_";

    /** Suffix for a key to a preference that stores the instance count for a given widget type. */
    private static final String WIDGET_COUNT_SUFFIX = "_widget_count";

    /** Lazily instantiated and cached for the life of the application. */
    private static SharedPreferences sPrefs;

    private WidgetDAO() {}

    /**
     * @param widgetId identifies a city widget in the launcher
     * @return the id of the City to display in the widget
     */
    public static String getWidgetCityId(Context context, int widgetId) {
        final SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getString(WIDGET_CITY_ID_PREFIX + widgetId, null);
    }

    /**
     * @param widgetId identifies a city widget in the launcher
     * @param cityId identifies the City to display in the widget; {@code null} implies remove City
     */
    public static void setWidgetCityId(Context context, int widgetId, String cityId) {
        final SharedPreferences prefs = getSharedPreferences(context);
        if (cityId == null) {
            prefs.edit().remove(WIDGET_CITY_ID_PREFIX + widgetId).apply();
        } else {
            prefs.edit().putString(WIDGET_CITY_ID_PREFIX + widgetId, cityId).apply();
        }
    }

    /**
     * @param widgetProviderClass indicates the type of widget being counted
     * @param count the number of widgets of the given type
     * @return the delta between the new count and the old count
     */
    public static int updateWidgetCount(Context context, Class widgetProviderClass, int count) {
        final SharedPreferences prefs = getSharedPreferences(context);
        final String key = widgetProviderClass.getSimpleName() + WIDGET_COUNT_SUFFIX;
        final int oldCount = prefs.getInt(key, 0);
        if (count == 0) {
            prefs.edit().remove(key).apply();
        } else {
            prefs.edit().putInt(key, count).apply();
        }
        return count - oldCount;
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        }

        return sPrefs;
    }
}