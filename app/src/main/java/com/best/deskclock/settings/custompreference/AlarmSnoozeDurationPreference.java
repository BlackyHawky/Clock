// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_SNOOZE_DURATION;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.best.deskclock.R;
import com.best.deskclock.utils.AlarmUtils;

/**
 * A custom {@link DialogPreference} that allows users to select the snooze duration for alarms.
 * <p>
 * This preference stores the snooze duration in minutes using Android's SharedPreferences system.
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
    public AlarmSnoozeDurationPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    @Override
    public CharSequence getSummary() {
        if (getSnoozeDuration() == ALARM_SNOOZE_DURATION_DISABLED) {
            return getContext().getString(R.string.snooze_duration_none);
        } else {
            return AlarmUtils.getSnoozeText(getContext(), getSnoozeDuration(), false);
        }
    }

    /**
     * Returns the currently persisted snooze delay duration in minutes.
     *
     * @return The snooze delay in minutes, or 10 if no value has been previously persisted.
     */
    public int getSnoozeDuration() {
        return getPersistedInt(DEFAULT_ALARM_SNOOZE_DURATION);
    }

    /**
     * Persists the snooze delay duration in minutes.
     *
     * @param minutes The snooze duration to be stored, in minutes.
     */
    public void setSnoozeDuration(int minutes) {
        persistInt(minutes);
        notifyChanged();
    }

}
