// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;

/**
 * A {@link Preference} with a custom layout applied to the title in About.
 */
public class CustomAboutTitlePreference extends Preference {

    public CustomAboutTitlePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_about_title);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        if (holder.itemView.isInEditMode()) {
            return;
        }

        Context context = holder.itemView.getContext();
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String fontPath = SettingsDAO.getGeneralFont(prefs);

        TextView slogan = (TextView) holder.findViewById(R.id.slogan);
        if (slogan != null) {
            slogan.setTypeface(ThemeUtils.loadFont(fontPath));
        }

        super.onBindViewHolder(holder);

        TextView title = (TextView) holder.findViewById(android.R.id.title);
        title.setTypeface(ThemeUtils.boldTypeface(fontPath));
    }

}
