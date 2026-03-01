// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import static android.view.View.GONE;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.best.deskclock.ItemAdapter.ItemViewHolder;
import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

final class AddButtonTipViewHolder extends ItemViewHolder<AddButtonTipHolder> {

    static final int VIEW_TYPE_BUTTON_TIP = Integer.MIN_VALUE;

    private AddButtonTipViewHolder(View itemView) {
        super(itemView);

        final Context context = itemView.getContext();
        final SharedPreferences prefs = getDefaultSharedPreferences(context);

        itemView.setPadding(0, 0, 0, (int) dpToPx(20, context.getResources().getDisplayMetrics()));

        final ImageView imageView = itemView.findViewById(R.id.ringtone_image);
        imageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_about));
        imageView.setPadding(0, 0, 0, 0);

        final View selectedView = itemView.findViewById(R.id.sound_image_selected);
        selectedView.setVisibility(GONE);

        final TextView nameView = itemView.findViewById(R.id.ringtone_name);
        final int textColor = MaterialColors.getColor(context, android.R.attr.textColorSecondary, Color.BLACK);
        Typeface baseTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));
        int style = Typeface.ITALIC;

        if (baseTypeface == null) {
            baseTypeface = Typeface.create("sans-serif", style);
        }

        Typeface styledTypeface = Typeface.create(baseTypeface, style);

        nameView.setTypeface(styledTypeface);
        nameView.setTextColor(textColor);
        nameView.setSingleLine(false);

        nameView.setText(context.getString(R.string.button_tip_title));
    }

    public record Factory(LayoutInflater mInflater) implements ItemViewHolder.Factory {

        @Override
        public ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            final View itemView = mInflater.inflate(R.layout.ringtone_item_sound, parent, false);
            return new AddButtonTipViewHolder(itemView);
        }
    }
}
