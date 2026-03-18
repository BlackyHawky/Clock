// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import static android.view.View.GONE;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

public class AddButtonTipViewHolder extends RecyclerView.ViewHolder {

    public AddButtonTipViewHolder(View itemView) {
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

        nameView.setTypeface(ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs)), Typeface.ITALIC);
        nameView.setTextColor(textColor);
        nameView.setSingleLine(false);
        nameView.setText(context.getString(R.string.button_tip_title));
    }

}
