// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.provider;

import android.content.SharedPreferences;
import android.text.TextUtils;

public abstract class AlarmMission {
    public static final int TYPE_NONE = 0;
    public static final int TYPE_MATH = 1;
    public static final int TYPE_QR = 2;

    public enum ChallengeType {
        NONE,
        MATH,
        QR
    }

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

    public boolean canStart(SharedPreferences prefs) {
        return isConfigured();
    }

    public boolean verifyInput(String input, SharedPreferences prefs) {
        return matches(input);
    }

    public ChallengeType getChallengeType() {
        return ChallengeType.NONE;
    }

    public static AlarmMission from(int missionType) {
        return from(missionType, "");
    }

    public static AlarmMission from(int missionType, String payload) {
        return switch (missionType) {
            case TYPE_MATH -> new MathAlarmMission();
            case TYPE_QR -> new QrAlarmMission(payload);
            default -> new NoAlarmMission();
        };
    }
}