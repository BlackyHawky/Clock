/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.ringtone.AsyncRingtonePlayer;
import com.best.deskclock.utils.LogUtils;

/**
 * Manages playing alarm ringtones and vibrating the device.
 */
final class AlarmKlaxon {

    private static final long[] VIBRATE_PATTERN = {500, 500};

    private static boolean sStarted = false;
    private static AsyncRingtonePlayer sAsyncRingtonePlayer;

    private AlarmKlaxon() {
    }

    public static void stop(Context context) {
        if (sStarted) {
            LogUtils.v("AlarmKlaxon.stop()");
            sStarted = false;
            getAsyncRingtonePlayer(context).stop();
            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
        }
    }

    public static void start(Context context, AlarmInstance instance) {
        // Make sure we are stopped before starting
        stop(context);
        LogUtils.v("AlarmKlaxon.start()");

        if (!AlarmInstance.NO_RINGTONE_URI.equals(instance.mRingtone)) {
            final long crescendoDuration = DataModel.getDataModel().getAlarmCrescendoDuration();
            getAsyncRingtonePlayer(context).play(instance.mRingtone, crescendoDuration);
        }

        if (instance.mVibrate) {
            final Vibrator vibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33 and above
                VibrationAttributes vibrationAttributes = new VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build();
                VibrationEffect vibrationEffect = VibrationEffect.createWaveform(VIBRATE_PATTERN, 0);
                vibrator.vibrate(vibrationEffect, vibrationAttributes);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26 to 32
                VibrationEffect vibrationEffect = VibrationEffect.createWaveform(VIBRATE_PATTERN, 0);
                vibrator.vibrate(vibrationEffect, audioAttributes);
            } else { // Before API 26
                vibrator.vibrate(VIBRATE_PATTERN, 0, audioAttributes);
            }
        }

        sStarted = true;
    }

    private static synchronized AsyncRingtonePlayer getAsyncRingtonePlayer(Context context) {
        if (sAsyncRingtonePlayer == null) {
            sAsyncRingtonePlayer = new AsyncRingtonePlayer(context.getApplicationContext());
        }

        return sAsyncRingtonePlayer;
    }
}
