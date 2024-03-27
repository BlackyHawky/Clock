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

package com.best.deskclock.timer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Vibrator;

import com.best.deskclock.LogUtils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.ringtone.BaseKlaxon;

/**
 * Manages playing the timer ringtone and vibrating the device.
 */
public abstract class TimerKlaxon {

    private static final long[] VIBRATE_PATTERN = {500, 500};

    public static void stop(Context context) {
        BaseKlaxon.stop(context);
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
            BaseKlaxon.start(context, uri, crescendoDuration, AudioManager.STREAM_ALARM, -1, true);
        }

        if (DataModel.getDataModel().getTimerVibrate()) {
            final Vibrator vibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
            vibrator.vibrate(VIBRATE_PATTERN, 0, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }
    }
}
