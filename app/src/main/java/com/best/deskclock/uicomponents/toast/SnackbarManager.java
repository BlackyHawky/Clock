/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents.toast;

import com.best.deskclock.R;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

/**
 * Manages visibility of Snackbar and allow preemptive dismiss of current displayed Snackbar.
 */
public final class SnackbarManager {

    private static WeakReference<Snackbar> sSnackbar = null;

    public static void show(Snackbar snackbar) {
        sSnackbar = new WeakReference<>(snackbar);
        if (ThemeUtils.isTablet() || (!ThemeUtils.isTablet() && ThemeUtils.isPortrait())) {
            snackbar.setAnchorView(R.id.button_layout);
        }
        snackbar.show();
    }

    public static void dismiss() {
        final Snackbar snackbar = sSnackbar == null ? null : sSnackbar.get();
        if (snackbar != null) {
            snackbar.dismiss();
            sSnackbar = null;
        }
    }
}
