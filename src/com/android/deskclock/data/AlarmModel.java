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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;

import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.provider.Alarm;

import java.util.Map;

/**
 * All alarm data will eventually be accessed via this model.
 */
final class AlarmModel {

    private final Context mContext;

    /** The model from which settings are fetched. */
    private final SettingsModel mSettingsModel;

    /** The uri of the default ringtone to play for new alarms; mirrors last selection. */
    private Uri mDefaultAlarmRingtoneUri;

    /** Maps ringtone uri to ringtone title; looking up a title from scratch is expensive. */
    private final Map<Uri, String> mRingtoneTitles = new ArrayMap<>(8);

    AlarmModel(Context context, SettingsModel settingsModel) {
        mContext = context;
        mSettingsModel = settingsModel;

        // Clear caches affected by system settings when system settings change.
        final ContentResolver cr = mContext.getContentResolver();
        final ContentObserver observer = new SystemAlarmAlertChangeObserver();
        cr.registerContentObserver(Settings.System.DEFAULT_ALARM_ALERT_URI, false, observer);
    }

    Uri getDefaultAlarmRingtoneUri() {
        if (mDefaultAlarmRingtoneUri == null) {
            mDefaultAlarmRingtoneUri = mSettingsModel.getDefaultAlarmRingtoneUri();
        }

        return mDefaultAlarmRingtoneUri;
    }

    void setDefaultAlarmRingtoneUri(Uri uri) {
        mSettingsModel.setDefaultAlarmRingtoneUri(uri);
        mDefaultAlarmRingtoneUri = uri;
    }

    String getAlarmRingtoneTitle(Uri uri) {
        // Special case: no ringtone has a title of "Silent".
        if (Alarm.NO_RINGTONE_URI.equals(uri)) {
            return mContext.getString(R.string.silent_ringtone_title);
        }

        // Check the cache.
        String title = mRingtoneTitles.get(uri);

        if (title == null) {
            // This is slow because a media player is created during Ringtone object creation.
            final Ringtone ringtone = RingtoneManager.getRingtone(mContext, uri);
            if (ringtone == null) {
                LogUtils.e("No ringtone for uri: %s", uri);
                return mContext.getString(R.string.unknown_ringtone_title);
            }

            // Cache the title for later use.
            title = ringtone.getTitle(mContext);
            mRingtoneTitles.put(uri, title);
        }
        return title;
    }

    /**
     * This receiver is notified when system settings change. Cached information built on
     * those system settings must be cleared.
     */
    private final class SystemAlarmAlertChangeObserver extends ContentObserver {

        private SystemAlarmAlertChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            LogUtils.i("Detected change to system default alarm ringtone; clearing caches");

            mDefaultAlarmRingtoneUri = null;

            // Titles such as "Default ringtone (Oxygen)" are wrong after default ringtone changes.
            mRingtoneTitles.clear();
        }
    }
}