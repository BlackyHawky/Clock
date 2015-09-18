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
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.android.deskclock.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates the transfer of data between {@link City} domain objects and their
 * permanent storage in {@link Resources} and {@link SharedPreferences}.
 */
final class CityDAO {

    // Regex to match natural index values when parsing city names.
    private static final Pattern INDEX_REGEX = Pattern.compile("\\d+");

    // Key to a preference that stores the number of selected cities.
    private static final String NUMBER_OF_CITIES = "number_of_cities";

    // Prefix for a key to a preference that stores the id of a selected city.
    private static final String CITY_ID = "city_id_";

    // Lazily instantiated and cached for the life of the application.
    private static SharedPreferences sPrefs;

    private CityDAO() {}

    /**
     * @param cityMap maps city ids to city instances
     * @return the list of city ids selected for display by the user
     */
    public static List<City> getSelectedCities(Context context, Map<String, City> cityMap) {
        final SharedPreferences prefs = getSharedPreferences(context);
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
    public static void setSelectedCities(Context context, Collection<City> cities) {
        final SharedPreferences prefs = getSharedPreferences(context);
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
    public static Map<String, City> getCities(Context context) {
        final Resources resources = context.getResources();
        final String[] ids = resources.getStringArray(R.array.cities_id);
        final String[] names = resources.getStringArray(R.array.cities_names);
        final String[] timezones = resources.getStringArray(R.array.cities_tz);

        if (ids.length != names.length) {
            final String locale = Locale.getDefault().toString();
            final String format = "id count (%d) != name count (%d) for locale %s";
            final String message = String.format(format, ids.length, names.length, locale);
            throw new IllegalStateException(message);
        }

        if (ids.length != timezones.length) {
            final String locale = Locale.getDefault().toString();
            final String format = "id count (%d) != timezone count (%d) for locale %s";
            final String message = String.format(format, ids.length, timezones.length, locale);
            throw new IllegalStateException(message);
        }

        final Map<String, City> cities = new ArrayMap<>(ids.length);
        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            if ("C0".equals(id)) {
                continue;
            }
            cities.put(id, createCity(id, names[i], timezones[i]));
        }
        return Collections.unmodifiableMap(cities);
    }

    /**
     * @param id unique identifier for city
     * @param formattedName "[index string]=[name]" or "[index string]=[name]:[phonetic name]",
     *                      If [index string] is empty, we use the first character of name as index,
     *                      If phonetic name is empty, we use the name itself as phonetic.
     * @param timeZoneId identifies the timezone in which the city is located
     */
    @VisibleForTesting
    static City createCity(String id, String formattedName, String timeZoneId) {
        final String[] parts = formattedName.split("[=:]");
        final String name = parts[1];
        // Extract index string from input, use the first character of city name as index string
        // if it's not explicitly provided.
        final String indexString = TextUtils.isEmpty(parts[0])
                ? String.valueOf(name.charAt(0)) : parts[0];
        final String phoneticName = parts.length == 3 ? parts[2] : name;

        final Matcher matcher = INDEX_REGEX.matcher(indexString);
        final int index = matcher.find() ? Integer.parseInt(matcher.group()) : -1;

        return new City(id, index, indexString, name, phoneticName, timeZoneId);
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        }

        return sPrefs;
    }
}