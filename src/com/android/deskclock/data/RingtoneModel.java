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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.provider.Alarm;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static android.media.AudioManager.STREAM_ALARM;
import static android.media.RingtoneManager.TITLE_COLUMN_INDEX;

/**
 * All ringtone data is accessed via this model.
 */
final class RingtoneModel {

    private final Context mContext;

    private final SharedPreferences mPrefs;

    /** Maps ringtone uri to ringtone title; looking up a title from scratch is expensive. */
    private final Map<Uri, String> mRingtoneTitles = new ArrayMap<>(16);

    /** Clears data structures containing data that is locale-sensitive. */
    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /** A mutable copy of the custom ringtones. */
    private List<CustomRingtone> mCustomRingtones;

    RingtoneModel(Context context, SharedPreferences prefs) {
        mContext = context;
        mPrefs = prefs;

        // Clear caches affected by system settings when system settings change.
        final ContentResolver cr = mContext.getContentResolver();
        final ContentObserver observer = new SystemAlarmAlertChangeObserver();
        cr.registerContentObserver(Settings.System.DEFAULT_ALARM_ALERT_URI, false, observer);

        // Clear caches affected by locale when locale changes.
        final IntentFilter localeBroadcastFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter);
    }

    CustomRingtone addCustomRingtone(Uri uri, String title) {
        // If the uri is already present in an existing ringtone, do nothing.
        final CustomRingtone existing = getCustomRingtone(uri);
        if (existing != null) {
            return existing;
        }

        final CustomRingtone ringtone = CustomRingtoneDAO.addCustomRingtone(mPrefs, uri, title);
        getMutableCustomRingtones().add(ringtone);
        Collections.sort(getMutableCustomRingtones());
        return ringtone;
    }

    void removeCustomRingtone(Uri uri) {
        final List<CustomRingtone> ringtones = getMutableCustomRingtones();
        for (CustomRingtone ringtone : ringtones) {
            if (ringtone.getUri().equals(uri)) {
                CustomRingtoneDAO.removeCustomRingtone(mPrefs, ringtone.getId());
                ringtones.remove(ringtone);
                break;
            }
        }
    }

    private CustomRingtone getCustomRingtone(Uri uri) {
        for (CustomRingtone ringtone : getMutableCustomRingtones()) {
            if (ringtone.getUri().equals(uri)) {
                return ringtone;
            }
        }

        return null;
    }

    List<CustomRingtone> getCustomRingtones() {
        return Collections.unmodifiableList(getMutableCustomRingtones());
    }

    @SuppressLint("NewApi")
    void loadRingtonePermissions() {
        final List<CustomRingtone> ringtones = getMutableCustomRingtones();
        if (ringtones.isEmpty()) {
            return;
        }

        final List<UriPermission> uriPermissions =
                mContext.getContentResolver().getPersistedUriPermissions();
        final Set<Uri> permissions = new ArraySet<>(uriPermissions.size());
        for (UriPermission uriPermission : uriPermissions) {
            permissions.add(uriPermission.getUri());
        }

        for (ListIterator<CustomRingtone> i = ringtones.listIterator(); i.hasNext();) {
            final CustomRingtone ringtone = i.next();
            i.set(ringtone.setHasPermissions(permissions.contains(ringtone.getUri())));
        }
    }

    void loadRingtoneTitles() {
        // Early return if the cache is already primed.
        if (!mRingtoneTitles.isEmpty()) {
            return;
        }

        final RingtoneManager ringtoneManager = new RingtoneManager(mContext);
        ringtoneManager.setType(STREAM_ALARM);

        // Cache a title for each system ringtone.
        try (Cursor cursor = ringtoneManager.getCursor()) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                final String ringtoneTitle = cursor.getString(TITLE_COLUMN_INDEX);
                final Uri ringtoneUri = ringtoneManager.getRingtoneUri(cursor.getPosition());
                mRingtoneTitles.put(ringtoneUri, ringtoneTitle);
            }
        } catch (Throwable ignored) {
            // best attempt only
            LogUtils.e("Error loading ringtone title cache", ignored);
        }
    }

    String getRingtoneTitle(Uri uri) {
        // Special case: no ringtone has a title of "Silent".
        if (Alarm.NO_RINGTONE_URI.equals(uri)) {
            return mContext.getString(R.string.silent_ringtone_title);
        }

        // If the ringtone is custom, it has its own title.
        final CustomRingtone customRingtone = getCustomRingtone(uri);
        if (customRingtone != null) {
            return customRingtone.getTitle();
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

    private List<CustomRingtone> getMutableCustomRingtones() {
        if (mCustomRingtones == null) {
            mCustomRingtones = CustomRingtoneDAO.getCustomRingtones(mPrefs);
            Collections.sort(mCustomRingtones);
        }

        return mCustomRingtones;
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

            // Titles such as "Default ringtone (Oxygen)" are wrong after default ringtone changes.
            mRingtoneTitles.clear();
        }
    }

    /**
     * Cached information that is locale-sensitive must be cleared in response to locale changes.
     */
    private final class LocaleChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Titles such as "Default ringtone (Oxygen)" are wrong after locale changes.
            mRingtoneTitles.clear();
        }
    }
}