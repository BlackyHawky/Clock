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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.android.deskclock.R;
import com.android.deskclock.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates the transfer of data between {@link City} domain objects and their
 * permanent storage in {@link Resources} and {@link SharedPreferences}.
 */
final class CityDAO {

    /** Regex to match numeric index values when parsing city names. */
    private static final Pattern NUMERIC_INDEX_REGEX = Pattern.compile("\\d+");

    /** Key to a preference that stores the number of selected cities. */
    private static final String NUMBER_OF_CITIES = "number_of_cities";

    /** Prefix for a key to a preference that stores the id of a selected city. */
    private static final String CITY_ID = "city_id_";

    private CityDAO() {}

    /**
     * @param cityMap maps city ids to city instances
     * @return the list of city ids selected for display by the user
     */
    static List<City> getSelectedCities(Context context, Map<String, City> cityMap) {
        final SharedPreferences prefs = Utils.getDefaultSharedPreferences(context);
        final int size = prefs.getInt(NUMBER_OF_CITIES, 0);
        final List<City> selectedCities = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            final String id = prefs.getString(CITY_ID + i, null);
            final City city = cityMap.get(id);
            if (city != null) {
                selectedCities.add(city);
            }
        }

        return selectedCities;
    }

    /**
     * @param cities the collection of cities selected for display by the user
     */
    static void setSelectedCities(Context context, Collection<City> cities) {
        final SharedPreferences prefs = Utils.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(NUMBER_OF_CITIES, cities.size());

        int count = 0;
        for (City city : cities) {
            editor.putString(CITY_ID + count, city.getId());
            count++;
        }

        editor.apply();
    }

    /**
     * @return the domain of cities from which the user may choose a world clock
     */
    static Map<String, City> getCities(Context context) {
        final Resources resources = context.getResources();
        final TypedArray cityStrings = resources.obtainTypedArray(R.array.city_ids);
        final int citiesCount = cityStrings.length();

        final Map<String, City> cities = new ArrayMap<>(citiesCount);
        try {
            for (int i = 0; i < citiesCount; ++i) {
                // Attempt to locate the resource id defining the city as a string.
                final int cityResourceId = cityStrings.getResourceId(i, 0);
                if (cityResourceId == 0) {
                    final String message = String.format(Locale.ENGLISH,
                            "Unable to locate city resource id for index %d", i);
                    throw new IllegalStateException(message);
                }

                final String id = resources.getResourceEntryName(cityResourceId);
                final String cityString = cityStrings.getString(i);
                if (cityString == null) {
                    final String message = String.format("Unable to locate city with id %s", id);
                    throw new IllegalStateException(message);
                }

                // Attempt to parse the time zone from the city entry.
                final String[] cityParts = cityString.split("[|]");
                if (cityParts.length != 2) {
                    final String message = String.format(
                            "Error parsing malformed city %s", cityString);
                    throw new IllegalStateException(message);
                }

                // Attempt to resolve the time zone id to a valid time zone.
                final String tzId = cityParts[1];
                final TimeZone tz = TimeZone.getTimeZone(tzId);
                // If the time zone lookup fails, GMT is returned. No cities actually map to GMT.
                if ("GMT".equals(tz.getID())) {
                    final String message = String.format(
                            "Unable to locate timezone with id %s", tzId);
                    throw new IllegalStateException(message);
                }

                cities.put(id, createCity(id, cityParts[0], tz));
            }
        } finally {
            cityStrings.recycle();
        }

        return Collections.unmodifiableMap(cities);
    }

    /**
     * @param id unique identifier for city
     * @param formattedName "[index string]=[name]" or "[index string]=[name]:[phonetic name]",
     *                      If [index string] is empty, use the first character of name as index,
     *                      If phonetic name is empty, use the name itself as phonetic name.
     * @param tz the timezone in which the city is located
     */
    @VisibleForTesting
    static City createCity(String id, String formattedName, TimeZone tz) {
        final String[] parts = formattedName.split("[=:]");
        final String name = parts[1];
        // Extract index string from input, use the first character of city name as the index string
        // if one is not explicitly provided.
        final String indexString = TextUtils.isEmpty(parts[0])
                ? name.substring(0, 1) : parts[0];
        final String phoneticName = parts.length == 3 ? parts[2] : name;

        final Matcher matcher = NUMERIC_INDEX_REGEX.matcher(indexString);
        final int index = matcher.find() ? Integer.parseInt(matcher.group()) : -1;

        return new City(id, index, indexString, name, phoneticName, tz);
    }
}