/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_CITIES_BY_DESCENDING_TIME_ZONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_CITIES_BY_NAME;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_CITIES_MANUALLY;
import static com.best.deskclock.utils.Utils.ACTION_LANGUAGE_CODE_CHANGED;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.ArraySet;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel.CitySort;
import com.best.deskclock.settings.PreferencesKeys;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouDigitalAppWidgetProvider;
import com.best.deskclock.widgets.standardwidgets.DigitalAppWidgetProvider;

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

    /**
     * Retain a hard reference to the shared preference observer to prevent it from being garbage
     * collected. See {@link SharedPreferences#registerOnSharedPreferenceChangeListener} for detail.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final OnSharedPreferenceChangeListener mPreferenceListener = new PreferenceListener();

    /**
     * Clears data structures containing data that is locale-sensitive.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /**
     * List of listeners to invoke upon world city list change
     */
    private final List<CityListener> mCityListeners = new ArrayList<>();

    /**
     * Maps city ID to city instance.
     */
    private Map<String, City> mCityMap;

    /**
     * List of city instances in display order.
     */
    private List<City> mAllCities;

    /**
     * List of selected city instances in display order.
     */
    private List<City> mSelectedCities;

    /**
     * List of unselected city instances in display order.
     */
    private List<City> mUnselectedCities;

    /**
     * A city instance representing the home timezone of the user.
     */
    private City mHomeCity;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    CityModel(Context context, SharedPreferences prefs) {
        mContext = context;
        mPrefs = prefs;

        // Clear caches affected by locale when locale changes.
        final IntentFilter localeBroadcastFilter = new IntentFilter();
        localeBroadcastFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
        localeBroadcastFilter.addAction(ACTION_LANGUAGE_CODE_CHANGED);
        if (SdkUtils.isAtLeastAndroid13()) {
            mContext.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter, Context.RECEIVER_EXPORTED);
        } else {
            mContext.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter);
        }

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
            final TimeZone timeZone = SettingsDAO.getHomeTimeZone(mContext, mPrefs, TimeZone.getDefault());
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
            final Set<City> selectedSet = newArraySet(selected);

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
     * Creates a new instance of {@link ArraySet} containing all the elements of the provided collection.
     */
    public static <E> ArraySet<E> newArraySet(Collection<E> collection) {
        final ArraySet<E> arraySet = new ArraySet<>(collection.size());
        arraySet.addAll(collection);
        return arraySet;
    }

    /**
     * @return a list of cities selected for display
     */
    List<City> getSelectedCities() {
        if (mSelectedCities == null) {
            final List<City> selectedCities = CityDAO.getSelectedCities(mPrefs, getCityMap());

            final String citySorting = SettingsDAO.getCitySorting(mPrefs);

            Comparator<City> comparator = switch (citySorting) {
                case SORT_CITIES_BY_DESCENDING_TIME_ZONE -> Collections.reverseOrder(new City.UtcOffsetComparator());
                case SORT_CITIES_BY_NAME -> new City.NameComparator();
                case SORT_CITIES_MANUALLY -> null; // Don't sort
                default -> new City.UtcOffsetComparator();
            };

            if (comparator != null) {
                Collections.sort(selectedCities, comparator);
            }

            mSelectedCities = Collections.unmodifiableList(selectedCities);
        }

        return mSelectedCities;
    }

    /**
     * @param cities the new collection of cities selected for display by the user
     */
    void setSelectedCities(Collection<City> cities) {
        CityDAO.setSelectedCities(mPrefs, cities);

        // Clear caches affected by this update.
        mAllCities = null;
        mSelectedCities = null;
        mUnselectedCities = null;

        // Broadcast the change to the selected cities for the benefit of widgets.
        fireCitiesChanged();
    }

    /**
     * Updates the order of selected cities and persists it to SharedPreferences.
     * @param newOrder the new list of selected cities, in the desired order
     */
    void updateSelectedCitiesOrder(List<City> newOrder) {
        CityDAO.saveSelectedCitiesOrder(mPrefs, newOrder);

        // Clean cache to force a clean reload
        mSelectedCities = null;

        fireCitiesChanged();
    }

    /**
     * @return a comparator used to locate index positions
     */
    Comparator<City> getCityIndexComparator() {
        final CitySort citySort = SettingsDAO.getCitySort(mPrefs);
        if (citySort == CitySort.NAME) {
            return new City.NameIndexComparator();
        } else if (citySort == CitySort.UTC_OFFSET) {
            return new City.UtcOffsetIndexComparator();
        }
        throw new IllegalStateException("unexpected city sort: " + citySort);
    }

    /**
     * Adjust the order in which cities are sorted.
     */
    void toggleCitySort() {
        SettingsDAO.toggleCitySort(mPrefs);

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
        final CitySort citySort = SettingsDAO.getCitySort(mPrefs);
        if (citySort == CitySort.NAME) {
            return new City.NameComparator();
        } else if (citySort == CitySort.UTC_OFFSET) {
            return new City.UtcOffsetComparator();
        }
        throw new IllegalStateException("unexpected city sort: " + citySort);
    }

    /**
     * Notifies all registered {@link CityListener} instances that the list of
     * selected cities has changed.
     *
     * <p>Also updates the list of cities in digital widgets.</p>
     */
    private void fireCitiesChanged() {
        WidgetUtils.updateWidget(mContext, DigitalAppWidgetProvider.class);
        WidgetUtils.updateWidget(mContext, MaterialYouDigitalAppWidgetProvider.class);

        for (CityListener listener : mCityListeners) {
            listener.citiesChanged();
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
            if (key != null) {
                switch (key) {
                    case PreferencesKeys.KEY_HOME_TIME_ZONE:
                        mHomeCity = null;
                    case PreferencesKeys.KEY_AUTO_HOME_CLOCK:
                        fireCitiesChanged();
                        break;
                    case PreferencesKeys.KEY_SORT_CITIES:
                        mSelectedCities = null;
                        fireCitiesChanged();
                        break;
                }
            }
        }
    }
}
