/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;

import com.best.deskclock.settings.PreferencesKeys;
import com.best.deskclock.utils.RingtoneUtils;

/**
 * All alarm data will eventually be accessed via this model.
 */
final class AlarmModel {

    private final SharedPreferences mPrefs;

    /**
     * The model from which ringtone data are fetched.
     */
    private final RingtoneModel mRingtoneModel;

    /**
     * Retain a hard reference to the shared preference observer to prevent it from being garbage
     * collected. See {@link SharedPreferences#registerOnSharedPreferenceChangeListener} for detail.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener = new PreferenceListener();

    /**
     * The uri of the default ringtone to use for alarms until the user explicitly chooses one.
     */
    private Uri mDefaultAlarmSettingsRingtoneUri;

    /**
     * The uri of the ringtone from settings to play for all alarms.
     */
    private Uri mAlarmRingtoneUriFromSettings;

    /**
     * The title of the ringtone to play for all alarms.
     */
    private String mAlarmRingtoneTitle;

    AlarmModel(SharedPreferences prefs, RingtoneModel ringtoneModel) {
        mPrefs = prefs;
        mRingtoneModel = ringtoneModel;

        // Clear caches affected by system settings when system settings change.
        prefs.registerOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    /**
     * @return the uri of the default ringtone to play for all alarms when no user selection exists
     */
    Uri getDefaultAlarmRingtoneUriFromSettings() {
        if (mDefaultAlarmSettingsRingtoneUri == null) {
            mDefaultAlarmSettingsRingtoneUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
        }
        return mDefaultAlarmSettingsRingtoneUri;
    }

    /**
     * @return the uri of the ringtone to play for all alarms
     */
    Uri getAlarmRingtoneUriFromSettings() {
        if (mAlarmRingtoneUriFromSettings == null) {
            mAlarmRingtoneUriFromSettings = SettingsDAO.getAlarmRingtoneUriFromSettings(mPrefs, getDefaultAlarmRingtoneUriFromSettings());
        }

        return mAlarmRingtoneUriFromSettings;
    }

    /**
     * @return the title of the ringtone that is played for all alarms
     */
    String getAlarmRingtoneTitle() {
        final Uri uri = getAlarmRingtoneUriFromSettings();
        mAlarmRingtoneTitle = mRingtoneModel.getRingtoneTitle(uri);

        return mAlarmRingtoneTitle;
    }

    /**
     * @param uri the uri of the ringtone from the settings to play for all alarms
     */
    void setAlarmRingtoneUriFromSettings(Uri uri) {
        SettingsDAO.setAlarmRingtoneUriFromSettings(mPrefs, uri);
    }

    /**
     * @param uri the uri of the ringtone of an existing alarm
     */
    void setSelectedAlarmRingtoneUri(Uri uri) {
        // Never set the silent ringtone as default; new alarms should always make sound by default.
        if (!RingtoneUtils.RINGTONE_SILENT.equals(uri)) {
            SettingsDAO.setSelectedAlarmRingtoneUri(mPrefs, uri);
        }
    }

    /**
     * This receiver is notified when shared preferences change. Cached information built on
     * preferences must be cleared.
     */
    private final class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (PreferencesKeys.KEY_DEFAULT_ALARM_RINGTONE.equals(key)) {
                mAlarmRingtoneUriFromSettings = null;
                mAlarmRingtoneTitle = null;
            }
        }
    }
}
