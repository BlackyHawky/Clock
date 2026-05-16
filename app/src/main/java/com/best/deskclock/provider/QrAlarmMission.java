// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.provider;

import android.content.SharedPreferences;

import com.best.deskclock.data.SettingsDAO;

public final class QrAlarmMission extends AlarmMission {

    public QrAlarmMission(String expectedQrContent) {
        super(TYPE_QR, expectedQrContent);
    }

    @Override
    public ChallengeType getChallengeType() {
        return ChallengeType.QR;
    }

    @Override
    public boolean requiresPayload() {
        return true;
    }

    @Override
    public boolean canStart(SharedPreferences prefs) {
        return isConfigured() || SettingsDAO.isAlarmMissionQrGloballyConfigured(prefs);
    }

    @Override
    public boolean matches(String input) {
        final String normalizedInput = input == null ? "" : input.trim();
        return getPayload().equals(normalizedInput);
    }

    @Override
    public boolean verifyInput(String input, SharedPreferences prefs) {
        if (isConfigured()) {
            return matches(input);
        }

        return SettingsDAO.matchesAlarmMissionQrFromSettings(prefs, input);
    }
}
