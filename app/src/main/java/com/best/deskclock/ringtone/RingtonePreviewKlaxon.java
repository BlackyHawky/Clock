/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.content.Context;
import android.net.Uri;

import com.best.deskclock.utils.LogUtils;

public final class RingtonePreviewKlaxon {

    private static RingtonePlayer sRingtonePlayer;

    private RingtonePreviewKlaxon() {
    }

    public static void stop(Context context) {
        LogUtils.i("RingtonePreviewKlaxon.stop()");
        getRingtonePlayer(context).stop();
    }

    public static void start(Context context, Uri uri) {
        stop(context);
        LogUtils.i("RingtonePreviewKlaxon.start()");
        getRingtonePlayer(context).play(uri, 0);
    }

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
