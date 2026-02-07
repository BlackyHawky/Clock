// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VOLUME_CRESCENDO_DURATION;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;

public class VolumeCrescendoDurationPreference extends DialogPreference {

    /**
     * Constructs a new VolumeCrescendoDurationPreference instance, used to manage user preferences
     * related to volume crescendo duration.
     *
     * @param context The application context in which this preference is used.
     * @param attrs   The attribute set from XML that may include custom parameters.
     */
    public VolumeCrescendoDurationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_preference_layout);
        setPersistent(true);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        PreferenceStyler.apply(holder);
        super.onBindViewHolder(holder);
    }

    @Override
    public CharSequence getSummary() {
        int seconds = getVolumeCrescendoDuration();

        if (seconds == DEFAULT_VOLUME_CRESCENDO_DURATION) {
            return getContext().getString(R.string.label_off);
        }

        int m = seconds / 60;
        int s = seconds % 60;

        if (m > 0 && s > 0) {
            String minutesString = getContext().getResources().getQuantityString(R.plurals.minutes, m, m);
            String secondsString = getContext().getResources().getQuantityString(R.plurals.seconds, s, s);
            return String.format("%s %s", minutesString, secondsString);
        } else if (m > 0) {
            return getContext().getResources().getQuantityString(R.plurals.minutes, m, m);
        } else {
            return getContext().getResources().getQuantityString(R.plurals.seconds, s, s);
        }
    }

    /**
     * Returns the currently persisted volume crescendo duration in seconds.
     *
     * @return The crescendo duration in seconds, or none if no value has been previously persisted.
     */
    public int getVolumeCrescendoDuration() {
        return getPersistedInt(DEFAULT_VOLUME_CRESCENDO_DURATION);
    }

    /**
     * Persists the volume crescendo duration in seconds.
     *
     * @param seconds The volume crescendo duration to be stored, in seconds.
     */
    public void setVolumeCrescendoDuration(int seconds) {
        persistInt(seconds);
    }

}
