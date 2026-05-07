// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import static android.view.View.GONE;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;

import com.google.android.material.color.MaterialColors;

public class AddButtonTipViewHolder extends RecyclerView.ViewHolder {

    public AddButtonTipViewHolder(View itemView, RingtoneAdapter adapter) {
        super(itemView);

        final Context context = itemView.getContext();

        itemView.setPadding(0, 0, 0, adapter.getDisplayMetricsPadding());

        final ImageView imageView = itemView.findViewById(R.id.ringtone_image);
        imageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_about));
        imageView.setPadding(0, 0, 0, 0);

        final View selectedView = itemView.findViewById(R.id.sound_image_selected);
        selectedView.setVisibility(GONE);

        final TextView nameView = itemView.findViewById(R.id.ringtone_name);
        final int textColor = MaterialColors.getColor(context, android.R.attr.textColorSecondary, Color.BLACK);

        nameView.setTypeface(adapter.getGeneralTypeface(), Typeface.ITALIC);
        nameView.setTextColor(textColor);
        nameView.setSingleLine(false);
        nameView.setText(context.getString(R.string.button_tip_title));
    }

}
