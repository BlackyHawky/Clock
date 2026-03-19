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

import com.best.deskclock.DeskClockApplication;
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
public final class TimerKlaxon {

    private static final long[] VIBRATE_PATTERN = {500, 500};

    private static TimerKlaxon sInstance;

    private boolean mStarted = false;
    private AsyncRingtonePlayer mAsyncRingtonePlayer;
    private RingtonePlayer mRingtonePlayer;

    private TimerKlaxon() {
    }

    private static synchronized TimerKlaxon getInstance() {
        if (sInstance == null) {
            sInstance = new TimerKlaxon();
        }

        return sInstance;
    }

    public static void stop() {
        TimerKlaxon instance = getInstance();

        if (instance.mStarted) {
            LogUtils.i("TimerKlaxon.stop()");
            instance.mStarted = false;

            Context appContext = DeskClockApplication.getAppContext();
            SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(appContext);

            if (SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
                instance.getRingtonePlayer().stop();
            } else {
                instance.getAsyncRingtonePlayer().stop();
            }

            Vibrator vibrator = appContext.getSystemService(Vibrator.class);
            vibrator.cancel();
        }
    }

    public static void start() {
        // Make sure we are stopped before starting
        stop();
        LogUtils.i("TimerKlaxon.start()");

        Context appContext = DeskClockApplication.getAppContext();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(appContext);
        TimerKlaxon instance = getInstance();
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
                instance.getRingtonePlayer().play(uri, crescendoDuration);
            } else {
                instance.getAsyncRingtonePlayer().play(uri, crescendoDuration);
            }
        }

        if (SettingsDAO.isTimerVibrate(prefs)) {
            final Vibrator vibrator = appContext.getSystemService(Vibrator.class);
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

        instance.mStarted = true;
    }

    public static void deactivateRingtonePlayback() {
        Context context = DeskClockApplication.getAppContext();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(context);

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
