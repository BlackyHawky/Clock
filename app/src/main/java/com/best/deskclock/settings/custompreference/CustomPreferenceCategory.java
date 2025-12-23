package com.best.deskclock.settings.custompreference;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;

/**
 * A {@link PreferenceCategory} with a custom layout.
 *
 * <p>This class updates its typography and visual styling based on the user's settings
 * (font, accent color, etc.).</p>
 *
 * <p>The preference behavior remains identical to a standard {@link PreferenceCategory};
 * only its visual presentation is customized.</p>
 */
public class CustomPreferenceCategory extends PreferenceCategory {

    public CustomPreferenceCategory(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_preference_category_layout);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        if (holder.itemView.isInEditMode()) {
            // Skip logic during Android Studio preview
            return;
        }

        super.onBindViewHolder(holder);

        SharedPreferences prefs = getDefaultSharedPreferences(getContext());

        TextView prefTitle = (TextView) holder.findViewById(android.R.id.title);
        prefTitle.setTypeface(ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(prefs)));
    }
}
