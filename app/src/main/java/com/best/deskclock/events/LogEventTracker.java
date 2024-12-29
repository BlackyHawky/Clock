/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.events;

import android.content.Context;

import androidx.annotation.StringRes;

import com.best.deskclock.utils.LogUtils;

public final class LogEventTracker implements EventTracker {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("Events");

    private final Context mContext;

    public LogEventTracker(Context context) {
        mContext = context;
    }

    @Override
    public void sendEvent(@StringRes int category, @StringRes int action, @StringRes int label) {
        if (label == 0) {
            LOGGER.d("[%s] [%s]", safeGetString(category), safeGetString(action));
        } else {
            LOGGER.d("[%s] [%s] [%s]", safeGetString(category), safeGetString(action),
                    safeGetString(label));
        }
    }

    /**
     * @return Resource string represented by a given resource id, null if resId is invalid (0).
     */
    private String safeGetString(@StringRes int resId) {
        return resId == 0 ? null : mContext.getString(resId);
    }
}
