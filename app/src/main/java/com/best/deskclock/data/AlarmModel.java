/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;

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

        mAlarmRingtoneUriFromSettings = null;
        mAlarmRingtoneTitle = null;
    }

}
