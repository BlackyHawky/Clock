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
package com.android.deskclock.events;

import android.content.Context;
import android.support.annotation.StringRes;

import com.android.deskclock.LogUtils;

public final class LogEventTracker implements EventTracker {

    private static final String TAG = "Events";

    private final Context mContext;

    public LogEventTracker(Context context) {
        mContext = context;
    }

    @Override
    public void sendView(String screenName) {
        LogUtils.d(TAG, "viewing screen %s", screenName);
    }

    @Override
    public void sendEvent(@StringRes int category, @StringRes int action, @StringRes int label) {
        sendEvent(safeGetString(category), safeGetString(action), safeGetString(label));
    }

    @Override
    public void sendEvent(String category, String action, String label) {
        if (label == null) {
            LogUtils.d(TAG, "[%s] [%s]", category, action);
        } else {
            LogUtils.d(TAG, "[%s] [%s] [%s]", category, action, label);
        }
    }

    /**
     * @return Resource string represented by a given resource id, null if resId is invalid (0).
     */
    private String safeGetString(@StringRes int resId) {
        return resId == 0 ? null : mContext.getString(resId);
    }
}
