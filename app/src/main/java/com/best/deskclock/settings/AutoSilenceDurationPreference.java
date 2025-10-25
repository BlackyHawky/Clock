// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_AUTO_SILENCE_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_TIMER_AUTO_SILENCE_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_NEVER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_AUTO_SILENCE_DURATION;

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
     * Returns the currently persisted auto silence duration
     * For alarms : in minutes.
     * For timers : in seconds.
     *
     * @return The auto silence duration, or none if no value has been previously persisted.
     */
    public int getAutoSilenceDuration() {
        return getPersistedInt(isForTimer()
                ? DEFAULT_TIMER_AUTO_SILENCE_DURATION
                : DEFAULT_AUTO_SILENCE_DURATION);
    }

    /**
     * @return {@code true} if this preference is for timers; {@code false} if it is for alarms.
     */
    public boolean isForTimer() {
        return KEY_TIMER_AUTO_SILENCE_DURATION.equals(getKey());
    }

    /**
     * Persists the auto silence duration.
     *
     * @param duration Duration in seconds (for timers) or minutes (for alarms).
     */
    public void setAutoSilenceDuration(int duration) {
        persistInt(duration);
    }

    @Override
    public CharSequence getSummary() {
        int duration = getAutoSilenceDuration();

        if (duration == TIMEOUT_END_OF_RINGTONE) {
            return getContext().getString(R.string.auto_silence_end_of_ringtone);
        } else if (duration == TIMEOUT_NEVER) {
            return getContext().getString(R.string.label_never);
        }

        if (isForTimer()) {
            int m = duration / 60;
            int s = duration % 60;

            if (m > 0 && s > 0) {
                String minutesString = getContext().getResources().getQuantityString(R.plurals.minutes, m, m);
                String secondsString = getContext().getResources().getQuantityString(R.plurals.seconds, s, s);
                return String.format("%s %s", minutesString, secondsString);
            } else if (m > 0) {
                return getContext().getResources().getQuantityString(R.plurals.minutes, m, m);
            } else {
                return getContext().getResources().getQuantityString(R.plurals.seconds, s, s);
            }
        } else {
            return getContext().getResources().getQuantityString(R.plurals.minutes, duration, duration);
        }
    }
}
