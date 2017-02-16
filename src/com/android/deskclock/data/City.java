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

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A read-only domain object representing a city of the world and associated time information. It
 * also contains static comparators that can be instantiated to order cities in common sort orders.
 */
public final class City {

    /** A unique identifier for the city. */
    private final String mId;

    /** An optional numeric index used to order cities for display; -1 if no such index exists. */
    private final int mIndex;

    /** An index string used to order cities for display. */
    private final String mIndexString;

    /** The display name of the city. */
    private final String mName;

    /** The phonetic name of the city used to order cities for display. */
    private final String mPhoneticName;

    /** The TimeZone corresponding to the city. */
    private final TimeZone mTimeZone;

    /** A cached upper case form of the {@link #mName} used in case-insensitive name comparisons. */
    private String mNameUpperCase;

    /**
     * A cached upper case form of the {@link #mName} used in case-insensitive name comparisons
     * which ignore {@link #removeSpecialCharacters(String)} special characters.
     */
    private String mNameUpperCaseNoSpecialCharacters;

    City(String id, int index, String indexString, String name, String phoneticName, TimeZone tz) {
        mId = id;
        mIndex = index;
        mIndexString = indexString;
        mName = name;
        mPhoneticName = phoneticName;
        mTimeZone = tz;
    }

    public String getId() { return mId; }
    public int getIndex() { return mIndex; }
    public String getName() { return mName; }
    public TimeZone getTimeZone() { return mTimeZone; }
    public String getIndexString() { return mIndexString; }
    public String getPhoneticName() { return mPhoneticName; }

    /**
     * @return the city name converted to upper case
     */
    public String getNameUpperCase() {
        if (mNameUpperCase == null) {
            mNameUpperCase = mName.toUpperCase();
        }
        return mNameUpperCase;
    }

    /**
     * @return the city name converted to upper case with all special characters removed
     */
    private String getNameUpperCaseNoSpecialCharacters() {
        if (mNameUpperCaseNoSpecialCharacters == null) {
            mNameUpperCaseNoSpecialCharacters = removeSpecialCharacters(getNameUpperCase());
        }
        return mNameUpperCaseNoSpecialCharacters;
    }

    /**
     * @param upperCaseQueryNoSpecialCharacters search term with all special characters removed
     *      to match against the upper case city name
     * @return {@code true} iff the name of this city starts with the given query
     */
    public boolean matches(String upperCaseQueryNoSpecialCharacters) {
        // By removing all special characters, prefix matching becomes more liberal and it is easier
        // to locate the desired city. e.g. "St. Lucia" is matched by "StL", "St.L", "St L", "St. L"
        return getNameUpperCaseNoSpecialCharacters().startsWith(upperCaseQueryNoSpecialCharacters);
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "City {id=%s, index=%d, indexString=%s, name=%s, phonetic=%s, tz=%s}",
                mId, mIndex, mIndexString, mName, mPhoneticName, mTimeZone.getID());
    }

    /**
     * Strips out any characters considered optional for matching purposes. These include spaces,
     * dashes, periods and apostrophes.
     *
     * @param token a city name or search term
     * @return the given {@code token} without any characters considered optional when matching
     */
    public static String removeSpecialCharacters(String token) {
        return token.replaceAll("[ -.']", "");
    }

    /**
     * Orders by:
     *
     * <ol>
     *     <li>UTC offset of {@link #getTimeZone() timezone}</li>
     *     <li>{@link #getIndex() numeric index}</li>
     *     <li>{@link #getIndexString()} alphabetic index}</li>
     *     <li>{@link #getPhoneticName() phonetic name}</li>
     * </ol>
     */
    public static final class UtcOffsetComparator implements Comparator<City> {

        private final Comparator<City> mDelegate1 = new UtcOffsetIndexComparator();

        private final Comparator<City> mDelegate2 = new NameComparator();

        public int compare(City c1, City c2) {
            int result = mDelegate1.compare(c1, c2);

            if (result == 0) {
                result = mDelegate2.compare(c1, c2);
            }

            return result;
        }
    }

    /**
     * Orders by:
     *
     * <ol>
     *     <li>UTC offset of {@link #getTimeZone() timezone}</li>
     * </ol>
     */
    public static final class UtcOffsetIndexComparator implements Comparator<City> {

        // Snapshot the current time when the Comparator is created to obtain consistent offsets.
        private final long now = System.currentTimeMillis();

        public int compare(City c1, City c2) {
            final int utcOffset1 = c1.getTimeZone().getOffset(now);
            final int utcOffset2 = c2.getTimeZone().getOffset(now);
            return Integer.compare(utcOffset1, utcOffset2);
        }
    }

    /**
     * This comparator sorts using the city fields that influence natural name sort order:
     *
     * <ol>
     *     <li>{@link #getIndex() numeric index}</li>
     *     <li>{@link #getIndexString()} alphabetic index}</li>
     *     <li>{@link #getPhoneticName() phonetic name}</li>
     * </ol>
     */
    public static final class NameComparator implements Comparator<City> {

        private final Comparator<City> mDelegate = new NameIndexComparator();

        // Locale-sensitive comparator for phonetic names.
        private final Collator mNameCollator = Collator.getInstance();

        @Override
        public int compare(City c1, City c2) {
            int result = mDelegate.compare(c1, c2);

            if (result == 0) {
                result = mNameCollator.compare(c1.getPhoneticName(), c2.getPhoneticName());
            }

            return result;
        }
    }

    /**
     * Orders by:
     *
     * <ol>
     *     <li>{@link #getIndex() numeric index}</li>
     *     <li>{@link #getIndexString()} alphabetic index}</li>
     * </ol>
     */
    public static final class NameIndexComparator implements Comparator<City> {

        // Locale-sensitive comparator for index strings.
        private final Collator mNameCollator = Collator.getInstance();

        @Override
        public int compare(City c1, City c2) {
            int result = Integer.compare(c1.getIndex(), c2.getIndex());

            if (result == 0) {
                result = mNameCollator.compare(c1.getIndexString(), c2.getIndexString());
            }

            return result;
        }
    }
}