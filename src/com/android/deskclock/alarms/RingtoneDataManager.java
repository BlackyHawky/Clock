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

package com.android.deskclock.alarms;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.Alarm;

/**
 * Data manager for alarm ringtone.
 */
public final class RingtoneDataManager {

    private static final String KEY_RINGTONE_TITLE_CACHE = "ringtoneTitleCache";
    private static final String PREF_KEY_DEFAULT_ALARM_RINGTONE_URI = "default_alarm_ringtone_uri";

    private final Bundle mCache; // Key: ringtone uri, value: ringtone title
    private final Fragment mFragment;
    private final Context mAppContext;
    private final AlarmUpdateHandler mAlarmUpdateHandler;

    public RingtoneDataManager(Fragment fragment, Bundle savedState,
            AlarmUpdateHandler alarmUpdateHandler) {
        mFragment = fragment;
        mAppContext = fragment.getActivity().getApplicationContext();
        mAlarmUpdateHandler = alarmUpdateHandler;
        Bundle cache = null;
        if (savedState != null) {
            cache = savedState.getBundle(KEY_RINGTONE_TITLE_CACHE);
        }
        mCache = cache == null ? new Bundle() : cache;
    }

    public void saveInstance(Bundle outState) {
        outState.putBundle(KEY_RINGTONE_TITLE_CACHE, mCache);
    }

    /**
     * Get ringtone title from cache.
     *
     * @param uri The uri of the ringtone.
     * @return The ringtone title. {@literal null} if no matching ringtone found.
     */
    public String getRingtoneTitle(Uri uri) {
        // Try the cache first
        String title = mCache.getString(uri.toString());
        if (title == null) {
            // If the user cannot read the ringtone file, insert our own name rather than the
            // ugly one returned by Ringtone.getTitle().
            if (!Utils.hasPermissionToDisplayRingtoneTitle(mAppContext, uri)) {
                title = mAppContext.getString(R.string.custom_ringtone);
            } else {
                // This is slow because a media player is created during Ringtone object creation.
                final Ringtone ringTone = RingtoneManager.getRingtone(mAppContext, uri);
                if (ringTone == null) {
                    LogUtils.i("No ringtone for uri %s", uri.toString());
                    return null;
                }
                title = ringTone.getTitle(mAppContext);
            }

            if (title != null) {
                mCache.putString(uri.toString(), title);
            }
        }
        return title;
    }

    /**
     * Clears the cached ringtone titles.
     */
    public void clearTitleCache() {
        mCache.clear();
    }

    /**
     * Gets the default alarm ringtone uri.
     */
    public Uri getDefaultRingtoneUri() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        final String ringtoneUriString = sp.getString(PREF_KEY_DEFAULT_ALARM_RINGTONE_URI, null);

        final Uri ringtoneUri;
        if (ringtoneUriString != null) {
            ringtoneUri = Uri.parse(ringtoneUriString);
        } else {
            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(mAppContext,
                    RingtoneManager.TYPE_ALARM);
        }

        return ringtoneUri;
    }

    /**
     * Saves ringtone for an alarm.
     */
    public void saveRingtoneUri(Alarm alarm, Intent intent) {
        Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri == null) {
            uri = Alarm.NO_RINGTONE_URI;
        }
        alarm.alert = uri;

        // Save the last selected ringtone as the default for new alarms
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        if (uri == null) {
            sp.edit().remove(PREF_KEY_DEFAULT_ALARM_RINGTONE_URI).apply();
        } else {
            sp.edit().putString(PREF_KEY_DEFAULT_ALARM_RINGTONE_URI, uri.toString()).apply();
        }

        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);

        // If the user chose an external ringtone and has not yet granted the permission to read
        // external storage, ask them for that permission now.
        if (!Utils.hasPermissionToDisplayRingtoneTitle(mAppContext, uri)) {
            final String[] perms = { Manifest.permission.READ_EXTERNAL_STORAGE };
            mFragment.requestPermissions(perms, R.id.request_permission_code_external_storage);
        }
    }
}
