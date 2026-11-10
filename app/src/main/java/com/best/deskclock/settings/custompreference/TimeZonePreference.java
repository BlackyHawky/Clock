// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.Utils;

import java.util.TimeZone;

/**
 * A custom {@link DialogPreference} that allows users to select a home time zone.
 * <p>This preference persists the selected home time zone as a {@link String}</p>.
 */
public class TimeZonePreference extends DialogPreference {

    public TimeZonePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    @Override
    public CharSequence getSummary() {
        String tzId = getTimeZone();

        boolean isFlagEnabled = SettingsDAO.isCityFlagEnabled(getDefaultSharedPreferences(getContext()));
        long currentTime = System.currentTimeMillis();

        SettingsDAO.TimeZoneDescriptor descriptor = SettingsDAO.TimeZoneDescriptor.create(
            getContext(), Utils.getLocaleFromContext(getContext()), tzId, currentTime, isFlagEnabled);

        return descriptor.timeZoneName();
    }

    /**
     * Retrieves the currently persisted home time zone key.
     *
     * @return The key corresponding to the selected home time zone, or a default value if none has been set.
     */
    public String getTimeZone() {
        return getPersistedString(TimeZone.getDefault().getID());
    }

    /**
     * Persists the given home time zone key.
     *
     * @param timeZoneId The key of the home time zone to store.
     */
    public void setTimeZone(@NonNull String timeZoneId) {
        persistString(timeZoneId);
        notifyChanged();
    }

    public void refreshSummary() {
        notifyChanged();
    }
}
