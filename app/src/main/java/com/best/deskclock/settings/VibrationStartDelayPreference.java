// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VIBRATION_START_DELAY;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.best.deskclock.R;

/**
 * A custom {@link DialogPreference} that allows users to select a vibration start delay.
 */
public class VibrationStartDelayPreference extends DialogPreference {

    /**
     * Constructs a new {@link VibrationStartDelayPreference} instance, used to manage user preferences
     * related to vibration start delay.
     *
     * @param context The application context in which this preference is used.
     * @param attrs   The attribute set from XML that may include custom parameters.
     */
    public VibrationStartDelayPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    /**
     * Returns the currently persisted vibration start delay in seconds.
     *
     * @return The vibration start delay in seconds, or none if no value has been previously persisted.
     */
    public int getVibrationStartDelay() {
        return getPersistedInt(DEFAULT_VIBRATION_START_DELAY);
    }

    /**
     * Persists the vibration start delay in seconds.
     *
     * @param seconds The vibration start delay to be stored, in seconds.
     */
    public void setVibrationStartDelay(int seconds) {
        persistInt(seconds);
    }

    @Override
    public CharSequence getSummary() {
        int seconds = getVibrationStartDelay();

        if (seconds == DEFAULT_VIBRATION_START_DELAY) {
            return getContext().getString(R.string.vibration_start_delay_none);
        } else {
            int minutes = seconds / 60;
            return getContext().getResources().getQuantityString(R.plurals.minutes, minutes, minutes);
        }
    }

}
