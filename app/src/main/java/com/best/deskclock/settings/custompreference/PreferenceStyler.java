// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

/**
 * Utility class that applies custom styling to a preference row.
 *
 * <p>This includes:</p>
 * <ul>
 *     <li>Applying a custom background or transparent background to the preference card</li>
 *     <li>Adding or removing a card border depending on user settings</li>
 *     <li>Applying the user‑selected typeface to the preference title and summary</li>
 * </ul>
 *
 * <p>The method is intended to be called from a {@link PreferenceViewHolder}
 * inside a custom Preference implementation.</p>
 */
public class PreferenceStyler {

    /**
     * Applies custom visual styling to the given {@link PreferenceViewHolder}.
     *
     * <p>This method:</p>
     * <ul>
     *     <li>Loads user settings from SharedPreferences</li>
     *     <li>Styles the preference card (background and border)</li>
     *     <li>Applies the selected typeface to the title and summary text</li>
     * </ul>
     *
     * @param holder the view holder representing the preference row
     */
    public static void apply(@NonNull PreferenceViewHolder holder) {
        if (holder.itemView.isInEditMode()) {
            return;
        }

        Context context = holder.itemView.getContext();
        SharedPreferences prefs = getDefaultSharedPreferences(context);

        MaterialCardView prefCardView = (MaterialCardView) holder.findViewById(R.id.pref_card_view);
        boolean isCardBackgroundDisplayed = SettingsDAO.isCardBackgroundDisplayed(prefs);
        boolean isCardBorderDisplayed = SettingsDAO.isCardBorderDisplayed(prefs);

        float strokeWidth = dpToPx(2, context.getResources().getDisplayMetrics());

        if (isCardBackgroundDisplayed) {
            prefCardView.setCardBackgroundColor(
                    MaterialColors.getColor(prefCardView,
                            com.google.android.material.R.attr.colorSurface)
            );
        } else {
            prefCardView.setCardBackgroundColor(Color.TRANSPARENT);
        }

        prefCardView.setStrokeWidth(isCardBorderDisplayed ? (int) strokeWidth : 0);

        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        TextView title = (TextView) holder.findViewById(android.R.id.title);
        if (title != null) {
            title.setTypeface(typeface);
        }

        TextView summary = (TextView) holder.findViewById(android.R.id.summary);
        if (summary != null) {
            summary.setTypeface(typeface);
        }
    }

}
