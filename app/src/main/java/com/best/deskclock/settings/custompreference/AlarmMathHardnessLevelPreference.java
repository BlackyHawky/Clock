// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_MATH_HARDNESS_LEVEL;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.best.deskclock.R;

/**
 * A custom {@link DialogPreference} that allows users to select a math hardness level.
 * <p>This preference persists the selected math hardness as a {@link String}</p>.
 */
public class AlarmMathHardnessLevelPreference extends DialogPreference {

    public AlarmMathHardnessLevelPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    @Override
    public CharSequence getSummary() {
        String mathHardnessKey = getMathHardnessLevel();

        String[] entries = getContext().getResources().getStringArray(R.array.math_hardness_level_entries);
        String[] values = getContext().getResources().getStringArray(R.array.math_hardness_level_values);

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(mathHardnessKey)) {
                return entries[i];
            }
        }

        return super.getSummary();
    }

    /**
     * Retrieves the currently persisted math hardness key.
     *
     * @return The key corresponding to the selected math hardness, or a default value if none has been set.
     */
    public String getMathHardnessLevel() {
        return getPersistedString(DEFAULT_MATH_HARDNESS_LEVEL);
    }

    /**
     * Persists the given math hardness key.
     *
     * @param mathHardnessLevelKey The key of the math hardness to store.
     */
    public void setMathHardnessLevel(@NonNull String mathHardnessLevelKey) {
        persistString(mathHardnessLevelKey);
        notifyChanged();
    }

}
