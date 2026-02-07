/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.ringtone.AsyncRingtonePlayer;
import com.best.deskclock.ringtone.RingtonePlayer;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;

/**
 * Manages playing the timer ringtone and vibrating the device.
 */
public abstract class TimerKlaxon {

    private static final long[] VIBRATE_PATTERN = {500, 500};

    private static boolean sStarted = false;

    private static AsyncRingtonePlayer sAsyncRingtonePlayer;

    private static RingtonePlayer sRingtonePlayer;

    private TimerKlaxon() {
    }

    public static void stop(Context context, SharedPreferences prefs) {
        if (sStarted) {
            LogUtils.i("TimerKlaxon.stop()");
            sStarted = false;
            if (SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
                getRingtonePlayer(context).stop();
            } else {
                getAsyncRingtonePlayer(context).stop();
            }
            Vibrator vibrator = context.getSystemService(Vibrator.class);
            vibrator.cancel();
        }
    }

    public static void start(Context context, SharedPreferences prefs) {
        // Make sure we are stopped before starting
        stop(context, prefs);
        LogUtils.i("TimerKlaxon.start()");

        Uri uri = DataModel.getDataModel().getTimerRingtoneUri();

        // Look up user-selected timer ringtone.
        if (RingtoneUtils.RINGTONE_SILENT.equals(uri)) {
            // Special case: Silent ringtone
            LogUtils.i("Playing silent ringtone for timer");
        } else {
            if (RingtoneUtils.isRandomRingtone(uri)) {
                uri = RingtoneUtils.getRandomRingtoneUri();
            } else if (RingtoneUtils.isRandomCustomRingtone(uri)) {
                uri = RingtoneUtils.getRandomCustomRingtoneUri();
            }

            // Crescendo duration always in milliseconds
            final int crescendoDuration = SettingsDAO.getTimerVolumeCrescendoDuration(prefs) * 1000;

            if (SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
                getRingtonePlayer(context).play(uri, crescendoDuration);
            } else {
                getAsyncRingtonePlayer(context).play(uri, crescendoDuration);
            }
        }

        if (SettingsDAO.isTimerVibrate(prefs)) {
            final Vibrator vibrator = context.getSystemService(Vibrator.class);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            if (SdkUtils.isAtLeastAndroid13()) {
                VibrationAttributes vibrationAttributes = new VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build();
                VibrationEffect vibrationEffect = VibrationEffect.createWaveform(VIBRATE_PATTERN, 0);
                vibrator.vibrate(vibrationEffect, vibrationAttributes);
            } else if (SdkUtils.isAtLeastAndroid8()) {
                VibrationEffect vibrationEffect = VibrationEffect.createWaveform(VIBRATE_PATTERN, 0);
                vibrator.vibrate(vibrationEffect, audioAttributes);
            } else {
                vibrator.vibrate(VIBRATE_PATTERN, 0, audioAttributes);
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
