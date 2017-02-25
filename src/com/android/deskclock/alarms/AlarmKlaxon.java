/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.deskclock.alarms;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Vibrator;

import com.android.deskclock.AsyncRingtonePlayer;
import com.android.deskclock.LogUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.AlarmInstance;

/**
 * Manages playing alarm ringtones and vibrating the device.
 */
final class AlarmKlaxon {

    private static final long[] VIBRATE_PATTERN = {500, 500};

    private static boolean sStarted = false;
    private static AsyncRingtonePlayer sAsyncRingtonePlayer;

    private AlarmKlaxon() {}

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
            final Vibrator vibrator = getVibrator(context);
            if (Utils.isLOrLater()) {
                vibrateLOrLater(vibrator);
            } else {
                vibrator.vibrate(VIBRATE_PATTERN, 0);
            }
        }

        sStarted = true;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void vibrateLOrLater(Vibrator vibrator) {
        vibrator.vibrate(VIBRATE_PATTERN, 0, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
    }

    private static Vibrator getVibrator(Context context) {
        return ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
    }

    private static synchronized AsyncRingtonePlayer getAsyncRingtonePlayer(Context context) {
        if (sAsyncRingtonePlayer == null) {
            sAsyncRingtonePlayer = new AsyncRingtonePlayer(context.getApplicationContext());
        }

        return sAsyncRingtonePlayer;
    }
}