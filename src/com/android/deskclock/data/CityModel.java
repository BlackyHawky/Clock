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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel.CitySort;
import com.android.deskclock.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * All {@link City} data is accessed via this model.
 */
final class CityModel {

    private final Context mContext;

    private final SharedPreferences mPrefs;

    /** The model from which settings are fetched. */
    private final SettingsModel mSettingsModel;

    /**
     * Retain a hard reference to the shared preference observer to prevent it from being garbage
     * collected. See {@link SharedPreferences#registerOnSharedPreferenceChangeListener} for detail.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final OnSharedPreferenceChangeListener mPreferenceListener = new PreferenceListener();

    /** Clears data structures containing data that is locale-sensitive. */
    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /** List of listeners to invoke upon world city list change */
    private final List<CityListener> mCityListeners = new ArrayList<>();

    /** Maps city ID to city instance. */
    private Map<String, City> mCityMap;

    /** List of city instances in display order. */
    private List<City> mAllCities;

    /** List of selected city instances in display order. */
    private List<City> mSelectedCities;

    /** List of unselected city instances in display order. */
    private List<City> mUnselectedCities;

    /** A city instance representing the home timezone of the user. */
    private City mHomeCity;

    CityModel(Context context, SharedPreferences prefs, SettingsModel settingsModel) {
        mContext = context;
        mPrefs = prefs;
        mSettingsModel = settingsModel;

        // Clear caches affected by locale when locale changes.
        final IntentFilter localeBroadcastFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter);

        // Clear caches affected by preferences when preferences change.
        prefs.registerOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    void addCityListener(CityListener cityListener) {
        mCityListeners.add(cityListener);
    }

    void removeCityListener(CityListener cityListener) {
        mCityListeners.remove(cityListener);
    }

    /**
     * @return a list of all cities in their display order
     */
    List<City> getAllCities() {
        if (mAllCities == null) {
            // Create a set of selections to identify the unselected cities.
            final List<City> selected = new ArrayList<>(getSelectedCities());

            // Sort the selected cities alphabetically by name.
            Collections.sort(selected, new City.NameComparator());

            // Combine selected and unselected cities into a single list.
            final List<City> allCities = new ArrayList<>(getCityMap().size());
            allCities.addAll(selected);
            allCities.addAll(getUnselectedCities());
            mAllCities = Collections.unmodifiableList(allCities);
        }

        return mAllCities;
    }

    /**
     * @return a city representing the user's home timezone
     */
    City getHomeCity() {
        if (mHomeCity == null) {
            final String name = mContext.getString(R.string.home_label);
            final TimeZone timeZone = mSettingsModel.getHomeTimeZone();
            mHomeCity = new City(null, -1, null, name, name, timeZone);
        }

        return mHomeCity;
    }

    /**
     * @return a list of cities not selected for display
     */
    List<City> getUnselectedCities() {
        if (mUnselectedCities == null) {
            // Create a set of selections to identify the unselected cities.
            final List<City> selected = new ArrayList<>(getSelectedCities());
            final Set<City> selectedSet = Utils.newArraySet(selected);

            final Collection<City> all = getCityMap().values();
            final List<City> unselected = new ArrayList<>(all.size() - selectedSet.size());
            for (City city : all) {
                if (!selectedSet.contains(city)) {
                    unselected.add(city);
                }
            }

            // Sort the unselected cities according by the user's preferred sort.
            Collections.sort(unselected, getCitySortComparator());
            mUnselectedCities = Collections.unmodifiableList(unselected);
        }

        return mUnselectedCities;
    }

    /**
     * @return a list of cities selected for display
     */
    List<City> getSelectedCities() {
        if (mSelectedCities == null) {
            final List<City> selectedCities = CityDAO.getSelectedCities(mPrefs, getCityMap());
            Collections.sort(selectedCities, new City.UtcOffsetComparator());
            mSelectedCities = Collections.unmodifiableList(selectedCities);
        }

        return mSelectedCities;
    }

    /**
     * @param cities the new collection of cities selected for display by the user
     */
    void setSelectedCities(Collection<City> cities) {
        final List<City> oldCities = getAllCities();
        CityDAO.setSelectedCities(mPrefs, cities);

        // Clear caches affected by this update.
        mAllCities = null;
        mSelectedCities = null;
        mUnselectedCities = null;

        // Broadcast the change to the selected cities for the benefit of widgets.
        fireCitiesChanged(oldCities, getAllCities());
    }

    /**
     * @return a comparator used to locate index positions
     */
    Comparator<City> getCityIndexComparator() {
        final CitySort citySort = mSettingsModel.getCitySort();
        switch (citySort) {
            case NAME: return new City.NameIndexComparator();
            case UTC_OFFSET: return new City.UtcOffsetIndexComparator();
        }
        throw new IllegalStateException("unexpected city sort: " + citySort);
    }

    /**
     * @return the order in which cities are sorted
     */
    CitySort getCitySort() {
        return mSettingsModel.getCitySort();
    }

    /**
     * Adjust the order in which cities are sorted.
     */
    void toggleCitySort() {
        mSettingsModel.toggleCitySort();

        // Clear caches affected by this update.
        mAllCities = null;
        mUnselectedCities = null;
    }

    private Map<String, City> getCityMap() {
        if (mCityMap == null) {
            mCityMap = CityDAO.getCities(mContext);
        }

        return mCityMap;
    }

    private Comparator<City> getCitySortComparator() {
        final CitySort citySort = mSettingsModel.getCitySort();
        switch (citySort) {
            case NAME: return new City.NameComparator();
            case UTC_OFFSET: return new City.UtcOffsetComparator();
        }
        throw new IllegalStateException("unexpected city sort: " + citySort);
    }

    private void fireCitiesChanged(List<City> oldCities, List<City> newCities) {
        mContext.sendBroadcast(new Intent(DataModel.ACTION_WORLD_CITIES_CHANGED));
        for (CityListener cityListener : mCityListeners) {
            cityListener.citiesChanged(oldCities, newCities);
        }
    }

    /**
     * Cached information that is locale-sensitive must be cleared in response to locale changes.
     */
    private final class LocaleChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mCityMap = null;
            mHomeCity = null;
            mAllCities = null;
            mSelectedCities = null;
            mUnselectedCities = null;
        }
    }

    /**
     * This receiver is notified when shared preferences change. Cached information built on
     * preferences must be cleared.
     */
    private final class PreferenceListener implements OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case SettingsActivity.KEY_HOME_TZ:
                    mHomeCity = null;
                case SettingsActivity.KEY_AUTO_HOME_CLOCK:
                    final List<City> cities = getAllCities();
                    fireCitiesChanged(cities, cities);
                    break;
            }
        }
    }
}
