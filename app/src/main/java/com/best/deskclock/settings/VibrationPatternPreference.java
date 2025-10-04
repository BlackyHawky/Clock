// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VIBRATION_PATTERN;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.best.deskclock.R;

/**
 * A custom {@link DialogPreference} that allows users to select a vibration pattern.
 * <p>
 * This preference persists the selected pattern as a {@link String}, which can
 * later be retrieved and used to generate a {@link android.os.VibrationEffect}.
 */
public class VibrationPatternPreference extends DialogPreference {

    /**
     * Creates a new {@link VibrationPatternPreference} instance.
     *
     * @param context The application context.
     * @param attrs   The attribute set containing custom XML attributes.
     */
    public VibrationPatternPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    /**
     * Retrieves the currently persisted vibration pattern key.
     *
     * @return The key corresponding to the selected vibration pattern,
     * or a default value if none has been set.
     */
    public String getPattern() {
        return getPersistedString(DEFAULT_VIBRATION_PATTERN);
    }

    /**
     * Persists the given vibration pattern key.
     *
     * @param patternKey The key of the vibration pattern to store.
     */
    public void setPattern(String patternKey) {
        persistString(patternKey);
    }

    @Override
    public CharSequence getSummary() {
        String patternKey = getPattern();

        String[] entries = getContext().getResources().getStringArray(R.array.vibration_pattern_entries);
        String[] values = getContext().getResources().getStringArray(R.array.vibration_pattern_values);

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(patternKey)) {
                return entries[i];
            }
        }

        return super.getSummary();
    }

}

