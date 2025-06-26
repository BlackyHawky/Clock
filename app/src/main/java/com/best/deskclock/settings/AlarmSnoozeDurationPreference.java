// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_SNOOZE_DURATION;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.best.deskclock.R;

/**
 * A custom {@link DialogPreference} that allows users to select the snooze duration for alarms.
 * <p>
 * This preference stores the snooze duration in minutes using Android's shared preferences system.
 * When shown in the preferences UI, it opens a custom dialog where the user can input hours and minutes.
 * </p>
 */
public class AlarmSnoozeDurationPreference extends DialogPreference {

    /**
     * Constructs a new AlarmSnoozeDurationPreference instance, used to manage user preferences
     * related to alarm snooze duration.
     *
     * @param context The application context in which this preference is used.
     * @param attrs   The attribute set from XML that may include custom parameters.
     */
    public AlarmSnoozeDurationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    /**
     * Returns the currently persisted snooze delay duration in minutes.
     *
     * @return The snooze delay in minutes, or 10 if no value has been previously persisted.
     */
    public int getRepeatDelayMinutes() {
        return getPersistedInt(DEFAULT_ALARM_SNOOZE_DURATION);
    }

    /**
     * Persists the snooze delay duration in minutes.
     *
     * @param minutes The snooze duration to be stored, in minutes.
     */
    public void setRepeatDelayMinutes(int minutes) {
        persistInt(minutes);
    }

    @Override
    public CharSequence getSummary() {
        int minutes = getRepeatDelayMinutes();

        if (minutes == -1) {
            return getContext().getString(R.string.snooze_duration_none);
        }

        int h = minutes / 60;
        int m = minutes % 60;

        if (h > 0 && m > 0) {
            String hoursString = getContext().getResources().getQuantityString(R.plurals.hours, h, h);
            String minutesString = getContext().getResources().getQuantityString(R.plurals.minutes, m, m);
            return String.format("%s %s", hoursString, minutesString);
        } else if (h > 0) {
            return getContext().getResources().getQuantityString(R.plurals.hours, h, h);
        } else {
            return getContext().getResources().getQuantityString(R.plurals.minutes, m, m);
        }
    }
}
