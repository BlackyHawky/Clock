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
import android.net.Uri;

import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel.CitySort;
import com.android.deskclock.data.DataModel.ClockStyle;

import java.util.TimeZone;

/**
 * All settings data is accessed via this model.
 */
final class SettingsModel {

    private final Context mContext;

    private final SharedPreferences mPrefs;

    /** The uri of the default ringtone to use for timers until the user explicitly chooses one. */
    private Uri mDefaultTimerRingtoneUri;

    SettingsModel(Context context, SharedPreferences prefs) {
        mContext = context;
        mPrefs = prefs;

        // Set the user's default home timezone if one has not yet been chosen.
        SettingsDAO.setDefaultHomeTimeZone(prefs, TimeZone.getDefault());
        // Set the user's default display seconds preference if one has not yet been chosen.
        SettingsDAO.setDefaultDisplayClockSeconds(mContext, prefs);
    }

    CitySort getCitySort() {
        return SettingsDAO.getCitySort(mPrefs);
    }

    void toggleCitySort() {
        SettingsDAO.toggleCitySort(mPrefs);
    }

    TimeZone getHomeTimeZone() {
        return SettingsDAO.getHomeTimeZone(mPrefs);
    }

    ClockStyle getClockStyle() {
        return SettingsDAO.getClockStyle(mContext, mPrefs);
    }

    boolean getDisplayClockSeconds() {
        return SettingsDAO.getDisplayClockSeconds(mPrefs);
    }

    void setDisplayClockSeconds(boolean shouldDisplaySeconds) {
        SettingsDAO.setDisplayClockSeconds(mPrefs, shouldDisplaySeconds);
    }

    ClockStyle getScreensaverClockStyle() {
        return SettingsDAO.getScreensaverClockStyle(mContext, mPrefs);
    }

    boolean getScreensaverNightModeOn() {
        return SettingsDAO.getScreensaverNightModeOn(mPrefs);
    }

    boolean getShowHomeClock() {
        if (!SettingsDAO.getAutoShowHomeClock(mPrefs)) {
            return false;
        }

        // Show the home clock if the current time and home time differ.
        // (By using UTC offset for this comparison the various DST rules are considered)
        final TimeZone homeTimeZone = SettingsDAO.getHomeTimeZone(mPrefs);
        final long now = System.currentTimeMillis();
        return homeTimeZone.getOffset(now) != TimeZone.getDefault().getOffset(now);
    }

    Uri getDefaultTimerRingtoneUri() {
        if (mDefaultTimerRingtoneUri == null) {
            mDefaultTimerRingtoneUri = Utils.getResourceUri(mContext, R.raw.timer_expire);
        }

        return mDefaultTimerRingtoneUri;
    }

    void setTimerRingtoneUri(Uri uri) {
        SettingsDAO.setTimerRingtoneUri(mPrefs, uri);
    }

    Uri getTimerRingtoneUri() {
        return SettingsDAO.getTimerRingtoneUri(mPrefs, getDefaultTimerRingtoneUri());
    }

    Uri getDefaultAlarmRingtoneUri() {
        return SettingsDAO.getDefaultAlarmRingtoneUri(mPrefs);
    }

    void setDefaultAlarmRingtoneUri(Uri uri) {
        SettingsDAO.setDefaultAlarmRingtoneUri(mPrefs, uri);
    }

    Weekdays.Order getWeekdayOrder() {
        return SettingsDAO.getWeekdayOrder(mPrefs);
    }

    boolean getTimerVibrate() {
        return SettingsDAO.getTimerVibrate(mPrefs);
    }

    void setTimerVibrate(boolean enabled) {
        SettingsDAO.setTimerVibrate(mPrefs, enabled);
    }
}