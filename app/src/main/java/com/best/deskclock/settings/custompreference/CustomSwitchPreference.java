// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;

/**
 * A {@link SwitchPreferenceCompat} with a custom Material-style layout.
 *
 * <p>This class applies a custom card-based appearance to the preference row
 * and updates its typography and visual styling based on the user's settings
 * (font, card background, card border, etc.).</p>
 *
 * <p>The preference behavior remains identical to a standard {@link SwitchPreferenceCompat};
 * only its visual presentation is customized.</p>
 */
public class CustomSwitchPreference extends SwitchPreferenceCompat {

    public CustomSwitchPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_preference_layout);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        PreferenceStyler.apply(holder);
        super.onBindViewHolder(holder);

        if (holder.itemView.isInEditMode()) {
            ViewGroup widgetFrame = (ViewGroup) holder.findViewById(android.R.id.widget_frame);
            widgetFrame.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(getContext());
            View switchView = inflater.inflate(R.layout.settings_material_switch, widgetFrame, false);

            widgetFrame.addView(switchView);
        }
    }

}
