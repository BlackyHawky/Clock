/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.best.deskclock.AppExecutors;
import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.ringtone.AsyncRingtonePlayer;
import com.best.deskclock.ringtone.RingtonePlayer;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

/**
 * Manages playing alarm ringtones and vibrating the device.
 */
public final class AlarmKlaxon {

    private static AlarmKlaxon sInstance;

    private boolean mStarted = false;
    private AsyncRingtonePlayer mAsyncRingtonePlayer;
    private RingtonePlayer mRingtonePlayer;
    private Runnable mVibrationRunnable;
    private int mPreviousAlarmVolume = -1;

    private AlarmKlaxon() {
    }

    private static synchronized AlarmKlaxon getInstance() {
        if (sInstance == null) {
            sInstance = new AlarmKlaxon();
        }

        return sInstance;
    }

    public static void stop() {
        AlarmKlaxon instance = getInstance();
        Context appContext = DeskClockApplication.getAppContext();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(appContext);

        if (instance.mVibrationRunnable != null) {
            AppExecutors.getMainThread().removeCallbacks(instance.mVibrationRunnable);
            instance.mVibrationRunnable = null;
        }

        if (instance.mStarted) {
            instance.mStarted = false;
            if (DeviceUtils.isUserUnlocked(appContext) && SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
                LogUtils.v("AlarmKlaxon.stop() ExoPlayer");
                instance.getRingtonePlayer().stop();
            } else {
                LogUtils.v("AlarmKlaxon.stop() MediaPlayer");
                instance.getAsyncRingtonePlayer().stop();

                if (SettingsDAO.isPerAlarmVolumeEnabled(prefs) && instance.mPreviousAlarmVolume != -1) {
                    AudioManager audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    // Restore the original alarm volume only if it was changed
                    if (currentVolume != instance.mPreviousAlarmVolume) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, instance.mPreviousAlarmVolume, 0);
                    }


                    instance.mPreviousAlarmVolume = -1;
                }
            }

            final Vibrator vibrator = appContext.getSystemService(Vibrator.class);
            vibrator.cancel();
        }
    }

    public static void start(AlarmInstance alarmInstance) {
        // Make sure we are stopped before starting
        stop();

        Context appContext = DeskClockApplication.getAppContext();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(appContext);
        AlarmKlaxon instance = getInstance();
        boolean isRingtoneSilent = RingtoneUtils.RINGTONE_SILENT.equals(alarmInstance.mRingtone);

        if (!isRingtoneSilent) {
            // Crescendo duration always in milliseconds
            final int crescendoDuration = alarmInstance.mCrescendoDuration * 1000;
            if (DeviceUtils.isUserUnlocked(appContext) && SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
                LogUtils.v("AlarmKlaxon.start() with ExoPlayer");

                instance.getRingtonePlayer().play(alarmInstance.mRingtone, crescendoDuration);
            } else {
                LogUtils.v("AlarmKlaxon.start() with MediaPlayer");

                if (SettingsDAO.isPerAlarmVolumeEnabled(prefs)) {
                    AudioManager audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);

                    if (instance.mPreviousAlarmVolume == -1) {
                        instance.mPreviousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    }

                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    if (currentVolume != alarmInstance.mAlarmVolume) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarmInstance.mAlarmVolume, 0);
                    }
                }

                instance.getAsyncRingtonePlayer().play(alarmInstance.mRingtone, crescendoDuration);
            }
        }

        if (alarmInstance.mVibrate) {
            long delayInMillis;

            if (isRingtoneSilent) {
                delayInMillis = 0;
                LogUtils.v("AlarmKlaxon: Ringtone is silent, bypassing vibration delay");
            } else {
                delayInMillis = SettingsDAO.getVibrationStartDelay(prefs) * 1000L;
                // Add a small safety margin in case the vibration pattern starts with 0 ms,
                // to prevent any vibration if the alarm stops right at the delay limit.
                final long SAFETY_MARGIN_MS = 300;
                delayInMillis += SAFETY_MARGIN_MS;
            }

            instance.mVibrationRunnable = () -> {
                final Vibrator vibrator = appContext.getSystemService(Vibrator.class);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

                String patternKey = SettingsDAO.isPerAlarmVibrationPatternEnabled(prefs)
                    ? alarmInstance.mVibrationPattern
                    : SettingsDAO.getVibrationPattern(prefs);
                long[] pattern = Utils.getVibrationPatternForKey(patternKey);

                if (SdkUtils.isAtLeastAndroid13()) {
                    VibrationAttributes vibrationAttributes = new VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build();
                    VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, 0);
                    vibrator.vibrate(vibrationEffect, vibrationAttributes);
                } else if (SdkUtils.isAtLeastAndroid8()) {
                    VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, 0);
                    vibrator.vibrate(vibrationEffect, audioAttributes);
                } else {
                    vibrator.vibrate(pattern, 0, audioAttributes);
                }
            };

            if (delayInMillis > 0) {
                LogUtils.v("AlarmKlaxon: vibration scheduled in " + delayInMillis + "ms");
                AppExecutors.getMainThread().postDelayed(instance.mVibrationRunnable, delayInMillis);
            } else {
                LogUtils.v("AlarmKlaxon: vibration started immediately");
                instance.mVibrationRunnable.run();
            }
        }

        instance.mStarted = true;
    }

    public static void deactivateRingtonePlayback() {
        Context appContext = DeskClockApplication.getAppContext();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(appContext);

        if (SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
            stopListeningToPreferences();
        } else {
            releaseResources();
        }
    }

    // MediaPlayer
    private AsyncRingtonePlayer getAsyncRingtonePlayer() {
        if (mAsyncRingtonePlayer == null) {
            mAsyncRingtonePlayer = new AsyncRingtonePlayer(DeskClockApplication.getAppContext());
        }

        return mAsyncRingtonePlayer;
    }

    public static synchronized void releaseResources() {
        if (sInstance != null && sInstance.mAsyncRingtonePlayer != null) {
            sInstance.mAsyncRingtonePlayer.shutdown();
            sInstance.mAsyncRingtonePlayer = null;
        }
    }

    // ExoPlayer
    private RingtonePlayer getRingtonePlayer() {
        if (mRingtonePlayer == null) {
            mRingtonePlayer = new RingtonePlayer(DeskClockApplication.getAppContext());
        }

        return mRingtonePlayer;
    }

    public static synchronized void stopListeningToPreferences() {
        if (sInstance != null && sInstance.mRingtonePlayer != null) {
            sInstance.mRingtonePlayer.stopListeningToPreferences();
            sInstance.mRingtonePlayer = null;
        }
    }
}
