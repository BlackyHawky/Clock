// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_NOTIFICATION_REMINDER;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.best.deskclock.R;

/**
 * A custom {@link DialogPreference} that allows users to select the notification reminder time for alarms.
 *
 * <p>This preference stores the notification reminder time in minutes using Android's shared preferences system.
 * When shown in the preferences UI, it opens a custom dialog where the user can input hours and minutes.</p>
 */
public class AlarmNotificationReminderPreference extends DialogPreference {

    /**
     * Constructs a new AlarmNotificationReminderPreference instance, used to manage user preferences
     * related to notification reminder time.
     *
     * @param context The application context in which this preference is used.
     * @param attrs   The attribute set from XML that may include custom parameters.
     */
    public AlarmNotificationReminderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    @Override
    public CharSequence getSummary() {
        int minutes = getAlarmNotificationReminderTime();

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

    /**
     * Returns the currently persisted notification reminder time in minutes.
     *
     * @return The notification reminder time in minutes, or 30 if no value has been previously persisted.
     */
    public int getAlarmNotificationReminderTime() {
        return getPersistedInt(DEFAULT_ALARM_NOTIFICATION_REMINDER);
    }

    /**
     * Persists the notification reminder time in minutes.
     *
     * @param minutes The notification reminder time to be stored, in minutes.
     */
    public void setAlarmNotificationReminderTime(int minutes) {
        persistInt(minutes);
        notifyChanged();
    }

}
