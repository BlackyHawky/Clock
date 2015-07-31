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
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CityObj {
    // Regex to match numeric values.
    private static final Pattern NATURAL_INDEX_REGEX = Pattern.compile("\\d+");
    // Data key to store in
    private static final String CITY_NAME = "city_name_";
    private static final String CITY_PHONETIC_NAME = "city_phonetic_name_";
    private static final String CITY_TIME_ZONE = "city_tz_";
    private static final String CITY_ID = "city_id_";
    private static final String CITY_INDEX = "city_index_";
    private static final String CITY_NUMERIC_INDEX = "city_numeric_index_";

    public String mCityName;
    public String mCityPhoneticName;
    public String mTimeZone;
    public String mCityId;
    public String mCityIndex;
    public int mCityNumericIndex;
    public boolean isHeader;

    public CityObj(String name, String phoneticName, String timezone, String id, String index) {
        mCityName = name;
        mCityPhoneticName = TextUtils.isEmpty(phoneticName) ? name : phoneticName;
        mTimeZone = timezone;
        mCityId = id;
        mCityIndex = index;

        // Build a natural index by extracting numeric values from {@link #mCityIndex} and store as
        // {@link #mCityNumericIndex}, which is used as primary strength to sort a list of
        // {@link CityObj}.
        // This is necessary so we can sort index list like ["1 stroke, 2 strokes, 10 strokes"]
        // in a human friendly way.
        if (mCityIndex != null) {
            final Matcher matcher = NATURAL_INDEX_REGEX.matcher(mCityIndex);
            mCityNumericIndex = matcher.find() ? Integer.parseInt(matcher.group()) : 0;
        }
    }

    @Override
    public String toString() {
        return "CityObj{" +
                "name=" + mCityName +
                ", phonetic=" + mCityPhoneticName +
                ", timezone=" + mTimeZone +
                ", id=" + mCityId +
                ", index=" + mCityIndex +
                ", numericIndex=" + mCityNumericIndex +
                '}';
    }

    public CityObj(SharedPreferences prefs, int index) {
        mCityName = prefs.getString(CITY_NAME + index, null);
        mCityPhoneticName = prefs.getString(CITY_PHONETIC_NAME + index, mCityName);
        mTimeZone = prefs.getString(CITY_TIME_ZONE + index, null);
        mCityId = prefs.getString(CITY_ID + index, null);
        mCityIndex = prefs.getString(CITY_INDEX + index, null);
        mCityNumericIndex = prefs.getInt(CITY_NUMERIC_INDEX + index, 0);
    }

    public void saveCityToSharedPrefs(SharedPreferences.Editor editor, int index) {
        editor.putString(CITY_NAME + index, mCityName);
        editor.putString(CITY_PHONETIC_NAME + index, mCityPhoneticName);
        editor.putString(CITY_TIME_ZONE + index, mTimeZone);
        editor.putString(CITY_ID + index, mCityId);
        editor.putString(CITY_INDEX + index, mCityIndex);
        editor.putInt(CITY_NUMERIC_INDEX + index, mCityNumericIndex);
    }
}
