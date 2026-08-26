/*
 * Copyright (C) 2008 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.NonNull;

/**
 * Utility class to hold wake lock in app.
 */
public class AlarmAlertWakeLock {

    private static final String TAG = "com.best.deskclock:AlarmAlertWakeLock";

    private static PowerManager.WakeLock sCpuWakeLock;

    public static PowerManager.WakeLock createPartialWakeLock(@NonNull Context context) {
        PowerManager pm = context.getApplicationContext().getSystemService(PowerManager.class);
        return pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    @SuppressLint("WakelockTimeout")
    public static void acquireCpuWakeLock(@NonNull Context context) {
        if (sCpuWakeLock != null) {
            return;
        }

        sCpuWakeLock = createPartialWakeLock(context);
        sCpuWakeLock.acquire();
    }

    public static void releaseCpuLock() {
        if (sCpuWakeLock != null && sCpuWakeLock.isHeld()) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }
}
