/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.media.AudioManager.STREAM_ALARM;
import static android.media.RingtoneManager.TITLE_COLUMN_INDEX;

import static com.best.deskclock.utils.RingtoneUtils.RANDOM_CUSTOM_RINGTONE;
import static com.best.deskclock.utils.RingtoneUtils.RANDOM_RINGTONE;

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
import android.os.Looper;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.best.deskclock.R;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * All ringtone data is accessed via this model.
 */
public final class RingtoneModel {

    private final Context mContext;

    private final SharedPreferences mPrefs;

    /**
     * Maps ringtone uri to ringtone title; looking up a title from scratch is expensive.
     */
    private final Map<Uri, String> mRingtoneTitles = new ArrayMap<>(16);

    /**
     * Clears data structures containing data that is locale-sensitive.
     */
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /**
     * Observer for changes to system settings affecting the default alarm ringtone.
     * <p>
     * This observer listens for modifications to system settings, such as changes to the default
     * alarm ringtone.
     * When a change is detected, it triggers the clearing of the ringtone title cache to ensure
     * that the displayed information remains up-to-date. This prevents outdated titles
     * (like those of default ringtones) from being used after a change in system settings.
     * <p>
     * The observer is registered to listen for changes to the URI
     * `Settings.System.DEFAULT_ALARM_ALERT_URI`, which corresponds to the default alarm ringtone
     *  on the device.
     */
    private final ContentObserver mSystemObserver = new SystemAlarmAlertChangeObserver();

    /**
     * A mutable copy of the custom ringtones.
     */
    private List<CustomRingtone> mCustomRingtones;

    public RingtoneModel(Context context, SharedPreferences prefs) {
        mContext = Utils.getSafeStorageContext(context);

        mPrefs = prefs;

        // Clear caches affected by system settings when system settings change.
        final ContentResolver cr = mContext.getContentResolver();
        cr.registerContentObserver(Settings.System.DEFAULT_ALARM_ALERT_URI, false, mSystemObserver);

        // Clear caches affected by locale when locale changes.
        final IntentFilter localeBroadcastFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        if (SdkUtils.isAtLeastAndroid13()) {
            mContext.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            mContext.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter);
        }
    }

    Uri customRingtoneToAdd(Uri uri, String title) {
        // If the new ringtone is already present in an existing ringtone, do nothing.
        long size = Utils.getFileSize(mContext, uri);

        Uri existingRingtone = customRingtoneAlreadyAdded(title, size);
        if (existingRingtone != null) {
            return existingRingtone;
        }

        // Make ringtone available during DirectBoot
        String safeTitle = title.replaceAll("[^a-zA-Z0-9.\\-]", "_");
        String uniqueSuffix = "_" + UUID.randomUUID().toString();
        String filename = safeTitle + uniqueSuffix;

        File destFile = new File(mContext.getFilesDir(), filename);
        try (InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(destFile)) {
            if (inputStream != null) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                uri = Uri.fromFile(destFile);
            } else {
                LogUtils.e("Failed to open input stream for URI: " + uri);
            }
        } catch (IOException e) {
            LogUtils.e("Failed to copy ringtone to device protected storage, continue using user storage", e);
        }

        final CustomRingtone ringtone = CustomRingtoneDAO.addCustomRingtone(mPrefs, uri, title);
        getMutableCustomRingtones().add(ringtone);
        Collections.sort(getMutableCustomRingtones());

        return uri;
    }

    void removeCustomRingtone(Uri uri) {
        final List<CustomRingtone> ringtones = getMutableCustomRingtones();
        for (CustomRingtone ringtone : ringtones) {
            if (ringtone.getUri().equals(uri)) {
                // Remove the file from device protected storage
                File file = new File(Objects.requireNonNull(uri.getPath()));
                if (file.exists()) {
                    File directBootDir = mContext.getFilesDir().getParentFile();

                    if (directBootDir != null && file.getAbsolutePath().startsWith(directBootDir.getAbsolutePath())) {
                        boolean deleted = file.delete();
                        if (!deleted) {
                            LogUtils.e("Failed to delete local log file");
                        }
                    }
                }

                CustomRingtoneDAO.removeCustomRingtone(mPrefs, ringtone.getId());
                ringtones.remove(ringtone);
                break;
            }
        }
    }

    Uri customRingtoneAlreadyAdded(String name, long size) {
        for (CustomRingtone ringtone : getMutableCustomRingtones()) {
            String ringtoneName = ringtone.getTitle();
            Uri ringtoneUri = ringtone.getUri();

            // Compare the name
            if (ringtoneName != null && ringtoneName.equalsIgnoreCase(name)) {
                // If the name is the same, try to compare the size
                if (Utils.getFileSize(mContext, ringtoneUri) == size) {
                    return ringtoneUri;
                }
            }
        }

        return null;
    }

    private CustomRingtone getCustomRingtone(Uri uri) {
        for (CustomRingtone ringtone : getMutableCustomRingtones()) {
            if (ringtone.getUri().equals(uri)) {
                return ringtone;
            }
        }

        return null;
    }

    public List<CustomRingtone> getCustomRingtones() {
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

        for (ListIterator<CustomRingtone> i = ringtones.listIterator(); i.hasNext(); ) {
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
        } catch (Throwable throwable) {
            // best attempt only
            LogUtils.e("Error loading ringtone title cache", throwable);
        }
    }

    String getRingtoneTitle(Uri uri) {
        final Context localizedContext = Utils.getLocalizedContext(mContext);

        // Special case: no ringtone has a title of "random" or "random_custom.
        if (RANDOM_RINGTONE.equals(uri) || RANDOM_CUSTOM_RINGTONE.equals(uri)) {
            return localizedContext.getString(R.string.random_ringtone_title);
        }

        // Special case: no ringtone has a title of "Silent".
        if (RingtoneUtils.RINGTONE_SILENT.equals(uri)) {
            return localizedContext.getString(R.string.silent_ringtone_title);
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
                return localizedContext.getString(R.string.unknown_ringtone_title);
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
     * Releases the resources used by the ringtone model, including observers and receivers.
     * <p>
     * Calling this method is crucial to avoid memory leaks, especially when the `RingtoneModel`
     * instance is no longer in use, and to ensure that the application does not retain unnecessary
     * references to system resources.
     */
    public void releaseResources() {
        try {
            mContext.getContentResolver().unregisterContentObserver(mSystemObserver);
        } catch (Exception e) {
            LogUtils.e("Failed to unregister ContentObserver", e);
        }
        try {
            mContext.unregisterReceiver(mLocaleChangedReceiver);
        } catch (Exception e) {
            LogUtils.e("Failed to unregister BroadcastReceiver", e);
        }
    }

    /**
     * This receiver is notified when system settings change. Cached information built on
     * those system settings must be cleared.
     */
    private final class SystemAlarmAlertChangeObserver extends ContentObserver {

        private SystemAlarmAlertChangeObserver() {
            super(new Handler(Looper.getMainLooper()));
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
