/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.content.Context;
import android.net.Uri;

import com.best.deskclock.utils.LogUtils;

import java.lang.ref.WeakReference;

public final class RingtonePreviewKlaxon {

    private static WeakReference<AsyncRingtonePlayer> sAsyncRingtonePlayerRef;

    private RingtonePreviewKlaxon() {
    }

    public static void stop(Context context) {
        LogUtils.i("RingtonePreviewKlaxon.stop()");
        getAsyncRingtonePlayer(context).stop();
    }

    public static void start(Context context, Uri uri) {
        stop(context);
        LogUtils.i("RingtonePreviewKlaxon.start()");
        getAsyncRingtonePlayer(context).play(uri, 0);
    }

    private static synchronized AsyncRingtonePlayer getAsyncRingtonePlayer(Context context) {
        if (sAsyncRingtonePlayerRef == null || sAsyncRingtonePlayerRef.get() == null) {
            sAsyncRingtonePlayerRef = new WeakReference<>(new AsyncRingtonePlayer(context.getApplicationContext()));
        }

        return sAsyncRingtonePlayerRef.get();
    }
}
