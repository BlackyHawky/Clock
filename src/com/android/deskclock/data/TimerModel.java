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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.android.deskclock.R;
import com.android.deskclock.settings.SettingsActivity;

/**
 * All timer data will eventually be accessed via this model.
 */
final class TimerModel {

    private final Context mContext;

    /** The model from which settings are fetched. */
    private final SettingsModel mSettingsModel;

    /**
     * Retain a hard reference to the shared preference observer to prevent it from being garbage
     * collected. See {@link SharedPreferences#registerOnSharedPreferenceChangeListener} for detail.
     */
    private final OnSharedPreferenceChangeListener mPreferenceListener = new PreferenceListener();

    /** The uri of the ringtone to play for timers. */
    private Uri mTimerRingtoneUri;

    /** The title of the ringtone to play for timers. */
    private String mTimerRingtoneTitle;

    TimerModel(Context context, SettingsModel settingsModel) {
        mContext = context;
        mSettingsModel = settingsModel;

        // Clear caches affected by preferences when preferences change.
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.registerOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    Uri getDefaultTimerRingtoneUri() {
        return mSettingsModel.getDefaultTimerRingtoneUri();
    }

    boolean isTimerRingtoneSilent() {
        return Uri.EMPTY.equals(getTimerRingtoneUri());
    }

    Uri getTimerRingtoneUri() {
        if (mTimerRingtoneUri == null) {
            mTimerRingtoneUri = mSettingsModel.getTimerRingtoneUri();
        }

        return mTimerRingtoneUri;
    }

    String getTimerRingtoneTitle() {
        if (mTimerRingtoneTitle == null) {
            if (isTimerRingtoneSilent()) {
                // Special case: no ringtone has a title of "Silent".
                mTimerRingtoneTitle = mContext.getString(R.string.silent_timer_ringtone_title);
            } else {
                final Uri defaultUri = getDefaultTimerRingtoneUri();
                final Uri uri = getTimerRingtoneUri();

                if (defaultUri.equals(uri)) {
                    // Special case: default ringtone has a title of "Timer Expired".
                    mTimerRingtoneTitle = mContext.getString(R.string.default_timer_ringtone_title);
                } else {
                    final Ringtone ringtone = RingtoneManager.getRingtone(mContext, uri);
                    mTimerRingtoneTitle = ringtone.getTitle(mContext);
                }
            }
        }

        return mTimerRingtoneTitle;
    }

    /**
     * This receiver is notified when shared preferences change. Cached information built on
     * preferences must be cleared.
     */
    private final class PreferenceListener implements OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case SettingsActivity.KEY_TIMER_RINGTONE:
                    mTimerRingtoneUri = null;
                    mTimerRingtoneTitle = null;
                    break;
            }
        }
    }
}