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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.android.deskclock.Utils.enforceMainLooper;

/**
 * All application-wide data is accessible through this singleton.
 */
public final class DataModel {

    /** Indicates the display style of clocks. */
    public enum ClockStyle {ANALOG, DIGITAL}

    /** Indicates the preferred sort order of cities. */
    public enum CitySort {NAME, UTC_OFFSET}

    public static final String ACTION_CITIES_CHANGED = "com.android.deskclock.CITIES_CHANGED";

    /** The single instance of this data model that exists for the life of the application. */
    private static final DataModel sDataModel = new DataModel();

    private Context mContext;

    /** The model from which settings are fetched. */
    private SettingsModel mSettingsModel;

    /** The model from which {@link City} objects are fetched. */
    private CityModel mCityModel;

    public static DataModel getDataModel() {
        return sDataModel;
    }

    private DataModel() {}

    /**
     * The context may be set precisely once during the application life.
     */
    public void setContext(Context context) {
        if (mContext != null) {
            throw new IllegalStateException("context has already been set");
        }
        mContext = context.getApplicationContext();
        mSettingsModel = new SettingsModel(mContext);
        mCityModel = new CityModel(mContext, mSettingsModel);
    }

    //
    // Cities
    //

    /**
     * @return a list of all cities in their display order
     */
    public List<City> getAllCities() {
        enforceMainLooper();
        return mCityModel.getAllCities();
    }

    /**
     * @param cityName the case-insensitive city name to search for
     * @return the city with the given {@code cityName}; {@code null} if no such city exists
     */
    public City getCity(String cityName) {
        enforceMainLooper();
        return mCityModel.getCity(cityName);
    }

    /**
     * @return a city representing the user's home timezone
     */
    public City getHomeCity() {
        enforceMainLooper();
        return mCityModel.getHomeCity();
    }

    /**
     * @return a list of cities not selected for display
     */
    public List<City> getUnselectedCities() {
        enforceMainLooper();
        return mCityModel.getUnselectedCities();
    }

    /**
     * @return a list of cities selected for display
     */
    public List<City> getSelectedCities() {
        enforceMainLooper();
        return mCityModel.getSelectedCities();
    }

    /**
     * @param cities the new collection of cities selected for display by the user
     */
    public void setSelectedCities(Collection<City> cities) {
        enforceMainLooper();
        mCityModel.setSelectedCities(cities);
    }

    /**
     * @return a comparator used to locate index positions
     */
    public Comparator<City> getCityIndexComparator() {
        enforceMainLooper();
        return mCityModel.getCityIndexComparator();
    }

    /**
     * @return the order in which cities are sorted
     */
    public CitySort getCitySort() {
        enforceMainLooper();
        return mCityModel.getCitySort();
    }

    /**
     * Adjust the order in which cities are sorted.
     */
    public void toggleCitySort() {
        enforceMainLooper();
        mCityModel.toggleCitySort();
    }

    //
    // Settings
    //

    /**
     * @return the style of clock to display in the clock application
     */
    public ClockStyle getClockStyle() {
        enforceMainLooper();
        return mSettingsModel.getClockStyle();
    }

    /**
     * @return the style of clock to display in the clock screensaver
     */
    public ClockStyle getScreensaverClockStyle() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverClockStyle();
    }

    /**
     * @return {@code true} if the users wants to automatically show a clock for their home timezone
     *      when they have travelled outside of that timezone
     */
    public boolean getShowHomeClock() {
        enforceMainLooper();
        return mSettingsModel.getShowHomeClock();
    }
}