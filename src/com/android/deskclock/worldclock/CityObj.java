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

package com.android.deskclock.worldclock;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CityObj {

    private static final String TAG = "CityObj";
    private static final String CITY_NAME = "city_name_";
    private static final String CITY_TIME_ZONE = "city_tz_";

    public String mCityName;
    public String mTimeZone;

    public CityObj(String name, String timezone) {
        mCityName = name;
        mTimeZone = timezone;
    }


    public CityObj(SharedPreferences prefs, int id) {
        mCityName = prefs.getString(CITY_NAME + id, null);
        mTimeZone = prefs.getString(CITY_TIME_ZONE + id, null);
    }

    public void saveCityToSharedPrefs(SharedPreferences.Editor editor, int id) {
        editor.putString (CITY_NAME + id, mCityName);
        editor.putString (CITY_TIME_ZONE + id, mTimeZone);
    }

}
