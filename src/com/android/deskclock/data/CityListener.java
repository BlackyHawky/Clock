package com.android.deskclock.data;

import java.util.List;

/**
 * The interface through which interested parties are notified of changes to the world cities list.
 */
public interface CityListener {
    void citiesChanged(List<City> oldCities, List<City> newCities);
}
