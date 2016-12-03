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

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationManagerCompat;

import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel.SilentSetting;

import java.util.ArrayList;
import java.util.List;

import static android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED;
import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;
import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.media.AudioManager.STREAM_ALARM;
import static android.media.RingtoneManager.TYPE_ALARM;
import static android.provider.Settings.System.CONTENT_URI;
import static android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;

/**
 * This model fetches and stores reasons that alarms may be suppressed or silenced by system
 * settings on the device. This information is displayed passively to notify the user of this
 * condition and set their expectations for future firing alarms.
 */
final class SilentSettingsModel {

    /** The Uri to the settings entry that stores alarm stream volume. */
    private static final Uri VOLUME_URI = Uri.withAppendedPath(CONTENT_URI, "volume_alarm_speaker");

    private final Context mContext;

    /** Used to query the alarm volume and display the system control to change the alarm volume. */
    private final AudioManager mAudioManager;

    /** Used to query the do-not-disturb setting value, also called "interruption filter". */
    private final NotificationManager mNotificationManager;

    /** Used to determine if the application is in the foreground. */
    private final NotificationModel mNotificationModel;

    /** List of listeners to invoke upon silence state change. */
    private final List<OnSilentSettingsListener> mListeners = new ArrayList<>(1);

    /**
     * The last setting known to be blocking alarms; {@code null} indicates no settings are
     * blocking the app or the app is not in the foreground.
     */
    private SilentSetting mSilentSetting;

    /** The background task that checks the device system settings that influence alarm firing. */
    private CheckSilenceSettingsTask mCheckSilenceSettingsTask;

    SilentSettingsModel(Context context, NotificationModel notificationModel) {
        mContext = context;
        mNotificationModel = notificationModel;

        mAudioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        // Watch for changes to the settings that may silence alarms.
        final ContentResolver cr = context.getContentResolver();
        final ContentObserver contentChangeWatcher = new ContentChangeWatcher();
        cr.registerContentObserver(VOLUME_URI, false, contentChangeWatcher);
        cr.registerContentObserver(DEFAULT_ALARM_ALERT_URI, false, contentChangeWatcher);
        if (Utils.isMOrLater()) {
            final IntentFilter filter = new IntentFilter(ACTION_INTERRUPTION_FILTER_CHANGED);
            context.registerReceiver(new DoNotDisturbChangeReceiver(), filter);
        }
    }

    void addSilentSettingsListener(OnSilentSettingsListener listener) {
        mListeners.add(listener);
    }

    void removeSilentSettingsListener(OnSilentSettingsListener listener) {
        mListeners.remove(listener);
    }

    /**
     * If the app is in the foreground, start a task to determine if any device setting will block
     * alarms from firing. If the app is in the background, clear any results from the last time
     * those settings were inspected.
     */
    void updateSilentState() {
        // Cancel any task in flight, the result is no longer relevant.
        if (mCheckSilenceSettingsTask != null) {
            mCheckSilenceSettingsTask.cancel(true);
            mCheckSilenceSettingsTask = null;
        }

        if (mNotificationModel.isApplicationInForeground()) {
            mCheckSilenceSettingsTask = new CheckSilenceSettingsTask();
            mCheckSilenceSettingsTask.execute();
        } else {
            setSilentState(null);
        }
    }

    /**
     * @param silentSetting the latest notion of which setting is suppressing alarms; {@code null}
     *      if no settings are suppressing alarms
     */
    private void setSilentState(SilentSetting silentSetting) {
        if (mSilentSetting != silentSetting) {
            final SilentSetting oldReason = mSilentSetting;
            mSilentSetting = silentSetting;

            for (OnSilentSettingsListener listener : mListeners) {
                listener.onSilentSettingsChange(oldReason, silentSetting);
            }
        }
    }

    /**
     * This task inspects a variety of system settings that can prevent alarms from firing or the
     * associated ringtone from playing. If any of them would prevent an alarm from firing or
     * making noise, a description of the setting is reported to this model on the main thread.
     */
    private final class CheckSilenceSettingsTask extends AsyncTask<Void, Void, SilentSetting> {
        @Override
        protected SilentSetting doInBackground(Void... parameters) {
            if (!isCancelled() && isDoNotDisturbBlockingAlarms()) {
                return SilentSetting.DO_NOT_DISTURB;
            } else if (!isCancelled() && isAlarmStreamMuted()) {
                return SilentSetting.MUTED_VOLUME;
            } else if (!isCancelled() && isSystemAlarmRingtoneSilent()) {
                return SilentSetting.SILENT_RINGTONE;
            } else if (!isCancelled() && isAppNotificationBlocked()) {
                return SilentSetting.BLOCKED_NOTIFICATIONS;
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (mCheckSilenceSettingsTask == this) {
                mCheckSilenceSettingsTask = null;
            }
        }

        @Override
        protected void onPostExecute(SilentSetting silentSetting) {
            if (mCheckSilenceSettingsTask == this) {
                mCheckSilenceSettingsTask = null;
                setSilentState(silentSetting);
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        private boolean isDoNotDisturbBlockingAlarms() {
            if (!Utils.isMOrLater()) {
                return false;
            }

            try {
                final int interruptionFilter = mNotificationManager.getCurrentInterruptionFilter();
                return interruptionFilter == INTERRUPTION_FILTER_NONE;
            } catch (Exception e) {
                // Since this is purely informational, avoid crashing the app.
                return false;
            }
        }

        private boolean isAlarmStreamMuted() {
            try {
                return mAudioManager.getStreamVolume(STREAM_ALARM) <= 0;
            } catch (Exception e) {
                // Since this is purely informational, avoid crashing the app.
                return false;
            }
        }

        private boolean isSystemAlarmRingtoneSilent() {
            try {
                return RingtoneManager.getActualDefaultRingtoneUri(mContext, TYPE_ALARM) == null;
            } catch (Exception e) {
                // Since this is purely informational, avoid crashing the app.
                return false;
            }
        }

        private boolean isAppNotificationBlocked() {
            try {
                return !NotificationManagerCompat.from(mContext).areNotificationsEnabled();
            } catch (Exception e) {
                // Since this is purely informational, avoid crashing the app.
                return false;
            }
        }
    }

    /**
     * Observe changes to specific URI for settings that can silence firing alarms.
     */
    private final class ContentChangeWatcher extends ContentObserver {
        private ContentChangeWatcher() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSilentState();
        }
    }

    /**
     * Observe changes to the do-not-disturb setting.
     */
    private final class DoNotDisturbChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSilentState();
        }
    }
}