/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED;
import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;
import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.media.AudioManager.STREAM_ALARM;
import static android.media.RingtoneManager.TYPE_ALARM;
import static android.provider.Settings.System.CONTENT_URI;
import static android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;

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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.best.deskclock.data.DataModel.SilentSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This model fetches and stores reasons that alarms may be suppressed or silenced by system
 * settings on the device. This information is displayed passively to notify the user of this
 * condition and set their expectations for future firing alarms.
 */
final class SilentSettingsModel {

    /**
     * The Uri to the settings entry that stores alarm stream volume.
     */
    private static final Uri VOLUME_URI = Uri.withAppendedPath(CONTENT_URI, "volume_alarm_speaker");

    private final Context mContext;

    /**
     * Used to query the alarm volume and display the system control to change the alarm volume.
     */
    private final AudioManager mAudioManager;

    /**
     * Used to query the do-not-disturb setting value, also called "interruption filter".
     */
    private final NotificationManager mNotificationManager;

    /**
     * Used to determine if the application is in the foreground.
     */
    private final NotificationModel mNotificationModel;

    /**
     * List of listeners to invoke upon silence state change.
     */
    private final List<OnSilentSettingsListener> mListeners = new ArrayList<>(1);

    /**
     * The background task that checks the device system settings that influence alarm firing.
     */
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
        final IntentFilter filter = new IntentFilter(ACTION_INTERRUPTION_FILTER_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(new DoNotDisturbChangeReceiver(), filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
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
            mCheckSilenceSettingsTask.cancel();
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
     *                      if no settings are suppressing alarms
     */
    private void setSilentState(SilentSetting silentSetting) {
        for (OnSilentSettingsListener listener : mListeners) {
            listener.onSilentSettingsChange(silentSetting);
        }
    }

    /**
     * This task inspects a variety of system settings that can prevent alarms from firing or the
     * associated ringtone from playing. If any of them would prevent an alarm from firing or
     * making noise, a description of the setting is reported to this model on the main thread.
     */
    private final class CheckSilenceSettingsTask {
        final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
        final Handler mHandler = new Handler(Looper.getMainLooper());

        private void execute() {
            mExecutor.execute(() -> {
                final SilentSetting silentSetting;
                if (isDoNotDisturbBlockingAlarms()) {
                    silentSetting = SilentSetting.DO_NOT_DISTURB;
                } else if (isAlarmStreamMuted()) {
                    silentSetting = SilentSetting.MUTED_VOLUME;
                } else if (isSystemAlarmRingtoneSilent()) {
                    silentSetting = SilentSetting.SILENT_RINGTONE;
                } else {
                    silentSetting = null;
                }

                mHandler.post(() -> {
                    if (mCheckSilenceSettingsTask == this) {
                        mCheckSilenceSettingsTask = null;
                        setSilentState(silentSetting);
                    }
                });
            });
        }

        private void cancel() {
            mExecutor.shutdownNow();
        }

        private boolean isDoNotDisturbBlockingAlarms() {
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
