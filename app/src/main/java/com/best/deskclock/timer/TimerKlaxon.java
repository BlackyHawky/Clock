/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Vibrator;

import com.best.deskclock.AsyncRingtonePlayer;
import com.best.deskclock.LogUtils;
import com.best.deskclock.data.DataModel;

/**
 * Manages playing the timer ringtone and vibrating the device.
 */
public abstract class TimerKlaxon {

    private static final long[] VIBRATE_PATTERN = {500, 500};

    private static boolean sStarted = false;
    private static AsyncRingtonePlayer sAsyncRingtonePlayer;

    private TimerKlaxon() {
    }

    public static void stop(Context context) {
        if (sStarted) {
            LogUtils.i("TimerKlaxon.stop()");
            sStarted = false;
            getAsyncRingtonePlayer(context).stop();
            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
        }
    }

    public static void start(Context context) {
        // Make sure we are stopped before starting
        stop(context);
        LogUtils.i("TimerKlaxon.start()");

        // Look up user-selected timer ringtone.
        if (DataModel.getDataModel().isTimerRingtoneSilent()) {
            // Special case: Silent ringtone
            LogUtils.i("Playing silent ringtone for timer");
        } else {
            final Uri uri = DataModel.getDataModel().getTimerRingtoneUri();
            final long crescendoDuration = DataModel.getDataModel().getTimerCrescendoDuration();
            getAsyncRingtonePlayer(context).play(uri, crescendoDuration);
        }

        if (DataModel.getDataModel().getTimerVibrate()) {
            final Vibrator vibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
            vibrator.vibrate(VIBRATE_PATTERN, 0, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
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
