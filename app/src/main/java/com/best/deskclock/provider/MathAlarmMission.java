// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.provider;

import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_MISSION_MATH_HARDNESS_EASY;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_MISSION_MATH_HARDNESS_HARD;

import android.content.SharedPreferences;

import com.best.deskclock.data.SettingsDAO;

import java.util.Random;

public final class MathAlarmMission extends AlarmMission {

    public record MathChallenge(int left, int right) {
        public int expected() {
            return left + right;
        }

        public boolean matches(String answer) {
            final String normalizedAnswer = answer == null ? "" : answer.trim();

            try {
                return Integer.parseInt(normalizedAnswer) == expected();
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
    }

    public MathAlarmMission() {
        super(TYPE_MATH, "");
    }

    @Override
    public ChallengeType getChallengeType() {
        return ChallengeType.MATH;
    }

    public MathChallenge createMathChallenge(SharedPreferences prefs, Random random) {
        final String hardness = SettingsDAO.getAlarmMissionMathHardness(prefs);
        final int min;
        final int range;

        switch (hardness) {
            case ALARM_MISSION_MATH_HARDNESS_EASY -> {
                min = 1;
                range = 20;
            }
            case ALARM_MISSION_MATH_HARDNESS_HARD -> {
                min = 50;
                range = 450;
            }
            default -> {
                min = 10;
                range = 80;
            }
        }

        final int left = min + random.nextInt(range);
        final int right = min + random.nextInt(range);
        return new MathChallenge(left, right);
    }
}
