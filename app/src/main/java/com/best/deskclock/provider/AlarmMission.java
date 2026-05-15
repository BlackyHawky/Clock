// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.provider;

import android.text.TextUtils;

public abstract class AlarmMission {
    public static final int TYPE_NONE = 0;
    public static final int TYPE_MATH = 1;
    public static final int TYPE_QR = 2;

    private final int mType;
    private final String mPayload;

    protected AlarmMission(int type, String payload) {
        mType = type;
        mPayload = payload == null ? "" : payload.trim();
    }

    public final int getType() {
        return mType;
    }

    public final boolean isEnabled() {
        return mType != TYPE_NONE;
    }

    public String getPayload() {
        return mPayload;
    }

    public boolean requiresPayload() {
        return false;
    }

    public boolean isConfigured() {
        return !requiresPayload() || !TextUtils.isEmpty(mPayload);
    }

    public boolean matches(String input) {
        return false;
    }
}