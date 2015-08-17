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

import com.android.deskclock.data.DataModel.CitySort;
import com.android.deskclock.data.DataModel.ClockStyle;

import java.util.TimeZone;

/**
 * All settings data is accessed via this model.
 */
final class SettingsModel {

    private final Context mContext;

    SettingsModel(Context context) {
        mContext = context;
    }

    CitySort getCitySort() {
        return SettingsDAO.getCitySort(mContext);
    }

    void toggleCitySort() {
        SettingsDAO.toggleCitySort(mContext);
    }

    TimeZone getHomeTimeZone() {
        return SettingsDAO.getHomeTimeZone(mContext);
    }

    ClockStyle getClockStyle() {
        return SettingsDAO.getClockStyle(mContext);
    }

    ClockStyle getScreensaverClockStyle() {
        return SettingsDAO.getScreensaverClockStyle(mContext);
    }

    boolean getShowHomeClock() {
        if (!SettingsDAO.getAutoShowHomeClock(mContext)) {
            return false;
        }

        // Show the home clock if the current time and home time differ.
        // (By using UTC offset for this comparison the various DST rules are considered)
        final TimeZone homeTimeZone = SettingsDAO.getHomeTimeZone(mContext);
        final long now = System.currentTimeMillis();
        return homeTimeZone.getOffset(now) != TimeZone.getDefault().getOffset(now);
    }
}