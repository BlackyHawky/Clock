// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import static com.best.deskclock.settings.PreferencesDefaultValues.MATH_HARDNESS_LEVEL_EASY;
import static com.best.deskclock.settings.PreferencesDefaultValues.MATH_HARDNESS_LEVEL_HARD;
import static com.best.deskclock.settings.PreferencesDefaultValues.MATH_HARDNESS_LEVEL_NORMAL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

public record AlarmMathChallenge(int left, int right) {

    public int expected() {
        return left + right;
    }

    public boolean matches(@Nullable String answer) {
        final String normalizedAnswer = answer == null ? "" : answer.trim();

        try {
            return Integer.parseInt(normalizedAnswer) == expected();
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @NonNull
    public static AlarmMathChallenge create(@NonNull String hardness, @NonNull Random random) {
        final int min;
        final int range;

        switch (hardness) {
            case MATH_HARDNESS_LEVEL_EASY -> {
                min = 1;
                range = 20;
            }
            case MATH_HARDNESS_LEVEL_NORMAL -> {
                min = 10;
                range = 80;
            }
            case MATH_HARDNESS_LEVEL_HARD -> {
                min = 50;
                range = 450;
            }
            default -> {
                min = 0;
                range = 0;
            }
        }

        final int left = min + random.nextInt(range);
        final int right = min + random.nextInt(range);
        return new AlarmMathChallenge(left, right);
    }
}
