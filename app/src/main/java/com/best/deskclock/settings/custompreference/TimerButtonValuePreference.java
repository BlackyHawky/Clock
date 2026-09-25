// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_TIMER_TIME_BUTTON_VALUE;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.best.deskclock.R;

public class TimerButtonValuePreference extends DialogPreference {

    public TimerButtonValuePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    @Override
    public CharSequence getSummary() {
        int value = getButtonValue();

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

    public int getButtonValue() {
        return getPersistedInt(DEFAULT_TIMER_TIME_BUTTON_VALUE);
    }

    public void setButtonValue(int minutes) {
        persistInt(minutes);
        notifyChanged();
    }

}
