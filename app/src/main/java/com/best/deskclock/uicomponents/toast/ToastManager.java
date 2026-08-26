/*
 * Copyright (C) 2008 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents.toast;

import android.widget.Toast;

import androidx.annotation.NonNull;

public final class ToastManager {

    private static Toast sToast = null;

    private ToastManager() {
    }

    public static void setToast(@NonNull Toast toast) {
        if (sToast != null)
            sToast.cancel();
        sToast = toast;
    }

    public static void cancelToast() {
        if (sToast != null)
            sToast.cancel();
        sToast = null;
    }

}
