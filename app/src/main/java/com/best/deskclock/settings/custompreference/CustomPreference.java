// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;

/**
 * A {@link Preference} with a custom Material-style layout.
 *
 * <p>This class applies a custom card-based appearance to the preference row
 * and updates its typography and visual styling based on the user's settings
 * (font, card background, card border, etc.).</p>
 *
 * <p>The preference behavior remains identical to a standard {@link Preference};
 * only its visual presentation is customized.</p>
 */
public class CustomPreference extends Preference {

    public CustomPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_preference_layout);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        PreferenceStyler.apply(holder);
        super.onBindViewHolder(holder);
    }

}
