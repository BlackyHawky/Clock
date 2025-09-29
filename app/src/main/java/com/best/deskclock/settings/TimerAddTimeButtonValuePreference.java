// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_TIMER_ADD_TIME_BUTTON_VALUE;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.best.deskclock.R;

public class TimerAddTimeButtonValuePreference extends DialogPreference {

    public TimerAddTimeButtonValuePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    public int getAddTimeButtonValue() {
        return getPersistedInt(DEFAULT_TIMER_ADD_TIME_BUTTON_VALUE);
    }

    public void setAddTimeButtonValue(int minutes) {
        persistInt(minutes);
    }

    @Override
    public CharSequence getSummary() {
        int value = getAddTimeButtonValue();

        int m = value / 60;
        int s = value % 60;

        if (m > 0 && s > 0) {
            String hoursString = getContext().getResources().getQuantityString(R.plurals.minutes, m, m);
            String secondString = getContext().getResources().getQuantityString(R.plurals.seconds, s, s);
            return String.format("%s %s", hoursString, secondString);
        } else if (m == 60) {
            return getContext().getResources().getQuantityString(R.plurals.hours, 1, 1);
        } else if (m > 0) {
            return getContext().getResources().getQuantityString(R.plurals.minutes, m, m);
        } else {
            return getContext().getResources().getQuantityString(R.plurals.seconds, s, s);
        }
    }
}
