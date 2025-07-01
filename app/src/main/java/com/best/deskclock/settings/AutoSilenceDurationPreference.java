// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_TIMEOUT_NEVER;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_AUTO_SILENCE_DURATION;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.best.deskclock.R;

public class AutoSilenceDurationPreference extends DialogPreference {

    /**
     * Constructs a new AutoSilenceDurationPreference instance, used to manage user preferences
     * related to auto silence duration.
     *
     * @param context The application context in which this preference is used.
     * @param attrs   The attribute set from XML that may include custom parameters.
     */
    public AutoSilenceDurationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    /**
     * Returns the currently persisted auto silence duration in seconds.
     *
     * @return The auto silence duration in minutes, or none if no value has been previously persisted.
     */
    public int getAutoSilenceDuration() {
        return getPersistedInt(DEFAULT_AUTO_SILENCE_DURATION);
    }

    /**
     * Persists the auto silence duration in minutes.
     *
     * @param minutes The auto silence duration to be stored, in minutes.
     */
    public void setAutoSilenceDuration(int minutes) {
        persistInt(minutes);
    }

    @Override
    public CharSequence getSummary() {
        int minutes = getAutoSilenceDuration();

        if (minutes == ALARM_TIMEOUT_END_OF_RINGTONE) {
            return getContext().getString(R.string.auto_silence_end_of_ringtone);
        } else if (minutes == ALARM_TIMEOUT_NEVER) {
            return getContext().getString(R.string.auto_silence_never);
        } else {
            return getContext().getResources().getQuantityString(R.plurals.minutes, minutes, minutes);
        }
    }
}
