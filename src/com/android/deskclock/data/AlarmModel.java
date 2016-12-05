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
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import com.android.deskclock.data.DataModel.AlarmVolumeButtonBehavior;
import com.android.deskclock.provider.Alarm;

/**
 * All alarm data will eventually be accessed via this model.
 */
final class AlarmModel {

    /** The model from which settings are fetched. */
    private final SettingsModel mSettingsModel;

    /** The uri of the default ringtone to play for new alarms; mirrors last selection. */
    private Uri mDefaultAlarmRingtoneUri;

    AlarmModel(Context context, SettingsModel settingsModel) {
        mSettingsModel = settingsModel;

        // Clear caches affected by system settings when system settings change.
        final ContentResolver cr = context.getContentResolver();
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
        // Never set the silent ringtone as default; new alarms should always make sound by default.
        if (!Alarm.NO_RINGTONE_URI.equals(uri)) {
            mSettingsModel.setDefaultAlarmRingtoneUri(uri);
            mDefaultAlarmRingtoneUri = uri;
        }
    }

    long getAlarmCrescendoDuration() {
        return mSettingsModel.getAlarmCrescendoDuration();
    }

    AlarmVolumeButtonBehavior getAlarmVolumeButtonBehavior() {
        return mSettingsModel.getAlarmVolumeButtonBehavior();
    }

    int getAlarmTimeout() {
        return mSettingsModel.getAlarmTimeout();
    }

    int getSnoozeLength() {
        return mSettingsModel.getSnoozeLength();
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
            mDefaultAlarmRingtoneUri = null;
        }
    }
}