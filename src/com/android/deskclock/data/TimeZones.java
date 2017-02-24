/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.text.TextUtils;

/**
 * A read-only domain object representing the timezones from which to choose a "home" timezone.
 */
public final class TimeZones {

    private final CharSequence[] mTimeZoneIds;
    private final CharSequence[] mTimeZoneNames;

    TimeZones(CharSequence[] timeZoneIds, CharSequence[] timeZoneNames) {
        mTimeZoneIds = timeZoneIds;
        mTimeZoneNames = timeZoneNames;
    }

    public CharSequence[] getTimeZoneIds() {
        return mTimeZoneIds;
    }

    public CharSequence[] getTimeZoneNames() {
        return mTimeZoneNames;
    }

    /**
     * @param timeZoneId identifies the timezone to locate
     * @return the timezone name with the {@code timeZoneId}; {@code null} if it does not exist
     */
    CharSequence getTimeZoneName(CharSequence timeZoneId) {
        for (int i = 0; i < mTimeZoneIds.length; i++) {
            if (TextUtils.equals(timeZoneId, mTimeZoneIds[i])) {
                return mTimeZoneNames[i];
            }
        }

        return null;
    }

    /**
     * @param timeZoneId identifies the timezone to locate
     * @return {@code true} iff the timezone with the given id is present
     */
    boolean contains(String timeZoneId) {
        return getTimeZoneName(timeZoneId) != null;
    }
}