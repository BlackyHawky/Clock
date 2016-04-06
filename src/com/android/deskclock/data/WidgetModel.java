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
import android.support.annotation.StringRes;
import android.util.ArrayMap;

import com.android.deskclock.R;
import com.android.deskclock.events.Events;

import java.util.Map;

/**
 * All widget data is accessed via this model.
 */
final class WidgetModel {

    private final Context mContext;

    /** The model from which city data are fetched. */
    private final CityModel mCityModel;

    /** Maps widget ID to city ID; items are loaded individually as widgets request data. */
    private final Map<Integer, String> mWidgetCityMap = new ArrayMap<>();

    WidgetModel(Context context, CityModel cityModel) {
        mContext = context;
        mCityModel = cityModel;
    }

    /**
     * @param widgetId identifies a city widget in the launcher
     * @return the City data to display in the widget
     */
    City getWidgetCity(int widgetId) {
        String cityId = mWidgetCityMap.get(widgetId);
        if (cityId == null) {
            cityId = WidgetDAO.getWidgetCityId(mContext, widgetId);
            mWidgetCityMap.put(widgetId, cityId);
        }

        return mCityModel.getCityById(cityId);
    }

    /**
     * @param widgetId identifies a city widget in the launcher
     * @param city the City to display in the widget; {@code null} implies remove City
     */
    void setWidgetCity(int widgetId, City city) {
        final String cityId = city == null ? null : city.getId();
        WidgetDAO.setWidgetCityId(mContext, widgetId, cityId);
        if (cityId == null) {
            mWidgetCityMap.remove(widgetId);
        } else {
            mWidgetCityMap.put(widgetId, cityId);
        }
    }

    /**
     * @param widgetClass indicates the type of widget being counted
     * @param count the number of widgets of the given type
     * @param eventCategoryId identifies the category of event to send
     */
    void updateWidgetCount(Class widgetClass, int count, @StringRes int eventCategoryId) {
        int delta = WidgetDAO.updateWidgetCount(mContext, widgetClass, count);
        for (; delta > 0; delta--) {
            Events.sendEvent(eventCategoryId, R.string.action_create, 0);
        }
        for (; delta < 0; delta++) {
            Events.sendEvent(eventCategoryId, R.string.action_delete, 0);
        }
    }
}