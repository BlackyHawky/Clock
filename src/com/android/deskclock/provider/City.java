/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.deskclock.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public final class City implements ClockContract.CitiesColumns {
    private static final String[] QUERY_COLUMNS = {
            CITY_ID,
            CITY_NAME,
            TIMEZONE_NAME,
            TIMEZONE_OFFSET
    };

    /**
     * These save calls to cursor.getColumnIndexOrThrow()
     * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
     */
    private static final int CITY_ID_INDEX = 0;
    private static final int CITY_NAME_INDEX = 1;
    private static final int TIMEZONE_NAME_INDEX = 2;
    private static final int TIMEZONE_OFFSET_INDEX = 3;

    private static final int COLUMN_COUNT = TIMEZONE_OFFSET_INDEX + 1;

    public static ContentValues createContentValues(City city) {
        ContentValues values = new ContentValues(COLUMN_COUNT);
        values.put(CITY_ID, city.mCityId);
        values.put(CITY_NAME, city.mCityName);
        values.put(TIMEZONE_NAME, city.mTimezoneName);
        values.put(TIMEZONE_OFFSET, city.mTimezoneOffset);
        return values;
    }

    public static String getCityId(Uri contentUri) {
        return contentUri.getLastPathSegment();
    }

    /**
     * Return content uri for specific city id.
     *
     * @param cityId to append to content uri
     *
     * @return a new city content uri with the given ID appended to the end of the path
     */
    public static Uri getContentUriForId(String cityId) {
        return CONTENT_URI.buildUpon().appendEncodedPath(cityId).build();
    }


    /**
     * Get city from cityId.
     *
     * @param contentResolver to perform the query on.
     * @param cityId for the desired city.
     * @return city if found, null otherwise
     */
    public static City getCity(ContentResolver contentResolver, String cityId) {
        Cursor cursor = contentResolver.query(getContentUriForId(cityId),
                QUERY_COLUMNS, null, null, null);
        City result = null;
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                result = new City(cursor);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    /**
     * Get a list of cities given selection.
     *
     * @param contentResolver to perform the query on.
     * @param selection A filter declaring which rows to return, formatted as an
     *         SQL WHERE clause (excluding the WHERE itself). Passing null will
     *         return all rows for the given URI.
     * @param selectionArgs You may include ?s in selection, which will be
     *         replaced by the values from selectionArgs, in the order that they
     *         appear in the selection. The values will be bound as Strings.
     * @return list of alarms matching where clause or empty list if none found.
     */
    public static List<City> getCities(ContentResolver contentResolver,
            String selection, String... selectionArgs) {
        Cursor cursor  = contentResolver.query(CONTENT_URI, QUERY_COLUMNS,
                selection, selectionArgs, null);
        List<City> result = new LinkedList<City>();
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    result.add(new City(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public static City addCity(ContentResolver contentResolver, City city) {
        ContentValues values = createContentValues(city);
        Uri uri = contentResolver.insert(CONTENT_URI, values);
        city.mCityId = getCityId(uri);
        return city;
    }

    public static boolean updateCity(ContentResolver contentResolver, City city) {
        ContentValues values = createContentValues(city);
        Uri updateUri = getContentUriForId(city.mCityId);
        long rowsUpdated = contentResolver.update(updateUri, values, null, null);
        return rowsUpdated == 1;
    }

    public static boolean deleteCity(ContentResolver contentResolver, String cityId) {
        Uri uri = getContentUriForId(cityId);
        int deletedRows = contentResolver.delete(uri, "", null);
        return deletedRows == 1;
    }

    // Public fields
    public String mCityId;
    public String mCityName;
    public String mTimezoneName;
    public int mTimezoneOffset;

    public City(String cityId, String cityName, String timezoneName, int timezoneOffset) {
        mCityId = cityId;
        mCityName = cityName;
        mTimezoneName = timezoneName;
        mTimezoneOffset = timezoneOffset;
    }

    public City(Cursor c) {
        mCityId = c.getString(CITY_ID_INDEX);
        mCityName = c.getString(CITY_NAME_INDEX);
        mTimezoneName = c.getString(TIMEZONE_NAME_INDEX);
        mTimezoneOffset = c.getInt(TIMEZONE_OFFSET_INDEX);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof City)) return false;
        final City other = (City) o;
        return mCityId.equals(other.mCityId);
    }

    @Override
    public int hashCode() {
        return mCityId.hashCode();
    }

    @Override
    public String toString() {
        return "Instance{" +
                "mCityId=" + mCityId +
                ", mCityName=" + mCityName +
                ", mTimezoneName=" + mTimezoneName +
                ", mTimezoneOffset=" + mTimezoneOffset +
                '}';
    }
}
