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

package com.best.deskclock.data;

import android.content.SharedPreferences;
import android.net.Uri;

import com.best.deskclock.data.DataModel.AlarmVolumeButtonBehavior;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.settings.SettingsActivity;

/**
 * All alarm data will eventually be accessed via this model.
 */
final class AlarmModel {

    /**
     * The model from which settings are fetched.
     */
    private final SettingsModel mSettingsModel;

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
     * The uri of the ringtone from settings to play for all alarms.
     */
    private Uri mAlarmRingtoneUriFromSettings;

    /**
     * The title of the ringtone to play for all alarms.
     */
    private String mAlarmRingtoneTitle;

    AlarmModel(SharedPreferences prefs, SettingsModel settingsModel, RingtoneModel ringtoneModel) {
        mSettingsModel = settingsModel;
        mRingtoneModel = ringtoneModel;

        // Clear caches affected by system settings when system settings change.
        prefs.registerOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    /**
     * @return the uri of the default ringtone to play for all alarms when no user selection exists
     */
    Uri getDefaultAlarmRingtoneUriFromSettings() {
        return mSettingsModel.getDefaultAlarmRingtoneUriFromSettings();
    }

    /**
     * @return the uri of the ringtone to play for all alarms
     */
    Uri getAlarmRingtoneUriFromSettings() {
        if (mAlarmRingtoneUriFromSettings == null) {
            mAlarmRingtoneUriFromSettings = mSettingsModel.getAlarmRingtoneUriFromSettings();
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
        mSettingsModel.setAlarmRingtoneUriFromSettings(uri);
    }

    /**
     * @param uri the uri of the ringtone of an existing alarm
     */
    void setSelectedAlarmRingtoneUri(Uri uri) {
        // Never set the silent ringtone as default; new alarms should always make sound by default.
        if (!Alarm.NO_RINGTONE_URI.equals(uri)) {
            mSettingsModel.setSelectedAlarmRingtoneUri(uri);
        }
    }

    long getAlarmCrescendoDuration() {
        return mSettingsModel.getAlarmCrescendoDuration();
    }

    AlarmVolumeButtonBehavior getAlarmVolumeButtonBehavior() {
        return mSettingsModel.getAlarmVolumeButtonBehavior();
    }

    AlarmVolumeButtonBehavior getAlarmPowerButtonBehavior() {
        return mSettingsModel.getAlarmPowerButtonBehavior();
    }

    int getAlarmTimeout() {
        return mSettingsModel.getAlarmTimeout();
    }

    int getSnoozeLength() {
        return mSettingsModel.getSnoozeLength();
    }

    int getFlipAction() {
        return mSettingsModel.getFlipAction();
    }

    int getShakeAction() {
        return mSettingsModel.getShakeAction();
    }

    /**
     * This receiver is notified when shared preferences change. Cached information built on
     * preferences must be cleared.
     */
    private final class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (SettingsActivity.KEY_DEFAULT_ALARM_RINGTONE.equals(key)) {
                mAlarmRingtoneUriFromSettings = null;
                mAlarmRingtoneTitle = null;
            }
        }
    }
}
