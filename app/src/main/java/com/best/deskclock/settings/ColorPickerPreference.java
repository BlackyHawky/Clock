// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;
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
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View thumbnail = addThumbnail(holder.itemView);
        if (thumbnail != null) {
            thumbnail.setBackground(ThemeUtils.circleDrawable());
            thumbnail.setClipToOutline(true);
            thumbnail.setOutlineProvider(ViewOutlineProvider.BACKGROUND);

            View colorPreview = thumbnail.findViewById(R.id.colorPreview);
            if (colorPreview != null) {
                int color = getColor();
                GradientDrawable circle = (GradientDrawable) ThemeUtils.circleDrawable();
                circle.setColor(color);
                colorPreview.setBackground(circle);
            }

            View border = thumbnail.findViewById(R.id.border);
            if (border != null) {
                GradientDrawable borderCircle = new GradientDrawable();
                borderCircle.setShape(GradientDrawable.OVAL);
                borderCircle.setColor(Color.TRANSPARENT);
                borderCircle.setStroke(
                        (int) dpToPx(1, getContext().getResources().getDisplayMetrics()),
                        ContextCompat.getColor(getContext(), com.rarepebble.colorpicker.R.color.gray600)
                );
                border.setBackground(borderCircle);
            }
        }
    }

    private View addThumbnail(View view) {
        LinearLayout widgetFrameView = view.findViewById(android.R.id.widget_frame);
        if (widgetFrameView == null) {
            return null;
        }

        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.removeAllViews();

        LayoutInflater.from(getContext()).inflate(R.layout.settings_preference_color_thumbnail,
                widgetFrameView);

        return widgetFrameView.findViewById(R.id.thumbnail);
    }
}
