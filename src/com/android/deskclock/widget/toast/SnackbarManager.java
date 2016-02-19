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

package com.android.deskclock.widget.toast;

import android.support.design.widget.Snackbar;

import java.lang.ref.WeakReference;

/**
 * Manages visibility of Snackbar and allow preemptively dismiss current Snackbar on screen.
 */
public final class SnackbarManager {

    private static WeakReference<Snackbar> sSnackbar = null;

    private SnackbarManager() {}

    public static void show(Snackbar snackbar) {
        sSnackbar = new WeakReference<>(snackbar);
        snackbar.show();
    }

    public static void dismiss() {
        if (sSnackbar != null && sSnackbar.get() != null) {
            sSnackbar.get().dismiss();
            sSnackbar = null;
        }
    }
}
