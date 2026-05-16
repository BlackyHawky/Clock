// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;
import com.best.deskclock.databinding.SettingsPreferenceColorThumbnailBinding;
import com.best.deskclock.utils.ThemeUtils;
import com.rarepebble.colorpicker.ColorPreference;

/**
 * This class extends {@link ColorPreference} and overrides the view binding to show a circular thumbnail
 * with the currently selected color.
 */
public class ColorPickerPreference extends ColorPreference {

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        if (holder.itemView.isInEditMode()) {
            // Skip logic during Android Studio preview
            return;
        }

        super.onBindViewHolder(holder);

        LinearLayout widgetFrameView = (LinearLayout) holder.findViewById(android.R.id.widget_frame);

        if (widgetFrameView != null) {
            widgetFrameView.setVisibility(View.VISIBLE);
            widgetFrameView.removeAllViews();

            SettingsPreferenceColorThumbnailBinding binding = SettingsPreferenceColorThumbnailBinding.inflate(
                LayoutInflater.from(getContext()), widgetFrameView, true);

            int color = getColor();
            GradientDrawable circle = (GradientDrawable) ThemeUtils.circleDrawable();
            circle.setColor(color);
            binding.colorPreview.setBackground(circle);

            GradientDrawable borderCircle = new GradientDrawable();
            borderCircle.setShape(GradientDrawable.OVAL);
            borderCircle.setColor(Color.TRANSPARENT);
            borderCircle.setStroke(
                (int) dpToPx(2, getContext().getResources().getDisplayMetrics()),
                ContextCompat.getColor(getContext(), R.color.md_theme_outline)
            );
            binding.border.setBackground(borderCircle);
        }
    }

}
