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
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

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
final class AlarmKlaxon {

    private static boolean sStarted = false;

    private static AsyncRingtonePlayer sAsyncRingtonePlayer;

    private static RingtonePlayer sRingtonePlayer;

    private static Handler sHandler;
    private static Runnable sVibrationRunnable;

    private static int sPreviousAlarmVolume = -1;

    private AlarmKlaxon() {
    }

    public static void stop(Context context, SharedPreferences prefs) {
        if (sHandler != null && sVibrationRunnable != null) {
            sHandler.removeCallbacks(sVibrationRunnable);
            sVibrationRunnable = null;
            sHandler = null;
        }

        if (sStarted) {
            sStarted = false;
            if (DeviceUtils.isUserUnlocked(context) && SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
                LogUtils.v("AlarmKlaxon.stop() ExoPlayer");
                getRingtonePlayer(context).stop();
            } else {
                LogUtils.v("AlarmKlaxon.stop() MediaPlayer");
                getAsyncRingtonePlayer(context).stop();

                if (SettingsDAO.isPerAlarmVolumeEnabled(prefs) && sPreviousAlarmVolume != -1) {
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    // Restore the original alarm volume only if it was changed
                    if (currentVolume != sPreviousAlarmVolume) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, sPreviousAlarmVolume, 0);
                    }

                    sPreviousAlarmVolume = -1;
                }
            }

            final Vibrator vibrator = context.getSystemService(Vibrator.class);
            vibrator.cancel();
        }
    }

    public static void start(Context context, SharedPreferences prefs, AlarmInstance instance) {
        // Make sure we are stopped before starting
        stop(context, prefs);

        if (!RingtoneUtils.RINGTONE_SILENT.equals(instance.mRingtone)) {
            // Crescendo duration always in milliseconds
            final int crescendoDuration = instance.mCrescendoDuration * 1000;
            if (DeviceUtils.isUserUnlocked(context) && SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
                LogUtils.v("AlarmKlaxon.start() with ExoPlayer");
                getRingtonePlayer(context).play(instance.mRingtone, crescendoDuration);
            } else {
                LogUtils.v("AlarmKlaxon.start() with MediaPlayer");
                if (SettingsDAO.isPerAlarmVolumeEnabled(prefs)) {
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

                    if (sPreviousAlarmVolume == -1) {
                        sPreviousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    }

                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    if (currentVolume != instance.mAlarmVolume) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, instance.mAlarmVolume, 0);
                    }
                }

                getAsyncRingtonePlayer(context).play(instance.mRingtone, crescendoDuration);
            }
        }

        if (instance.mVibrate) {
            long delayInMillis = SettingsDAO.getVibrationStartDelay(prefs) * 1000L;
            // Add a small safety margin in case the vibration pattern starts with 0 ms,
            // to prevent any vibration if the alarm stops right at the delay limit.
            final long SAFETY_MARGIN_MS = 300;
            delayInMillis += SAFETY_MARGIN_MS;

            sHandler = new Handler(Looper.getMainLooper());

            sVibrationRunnable = () -> {
                final Vibrator vibrator = context.getSystemService(Vibrator.class);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();

                String patternKey = SettingsDAO.getVibrationPattern(prefs);
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
                sHandler.postDelayed(sVibrationRunnable, delayInMillis);
            } else {
                LogUtils.v("AlarmKlaxon: vibration started immediately");
                sVibrationRunnable.run();
            }
        }

        sStarted = true;
    }

    public static void deactivateRingtonePlayback(SharedPreferences prefs) {
        if (SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
            stopListeningToPreferences();
        } else {
            releaseResources();
        }
    }

    // MediaPlayer
    private static synchronized AsyncRingtonePlayer getAsyncRingtonePlayer(Context context) {
        if (sAsyncRingtonePlayer == null) {
            sAsyncRingtonePlayer = new AsyncRingtonePlayer(context.getApplicationContext());
        }

        return sAsyncRingtonePlayer;
    }

    public static synchronized void releaseResources() {
        if (sAsyncRingtonePlayer != null) {
            sAsyncRingtonePlayer.shutdown();
            sAsyncRingtonePlayer = null;
        }
    }

    // ExoPlayer
    private static synchronized RingtonePlayer getRingtonePlayer(Context context) {
        if (sRingtonePlayer == null) {
            sRingtonePlayer = new RingtonePlayer(context.getApplicationContext());
        }

        return sRingtonePlayer;
    }

    public static synchronized void stopListeningToPreferences() {
        if (sRingtonePlayer != null) {
            sRingtonePlayer.stopListeningToPreferences();
            sRingtonePlayer = null;
        }
    }
}
