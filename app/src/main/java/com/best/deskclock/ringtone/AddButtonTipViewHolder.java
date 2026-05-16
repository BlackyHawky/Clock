// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import static android.view.View.GONE;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;

import com.best.deskclock.databinding.RingtoneItemSoundBinding;
import com.google.android.material.color.MaterialColors;

public class AddButtonTipViewHolder extends RecyclerView.ViewHolder {

    public AddButtonTipViewHolder(View itemView, RingtoneAdapter adapter) {
        super(itemView);

        final Context context = itemView.getContext();

        RingtoneItemSoundBinding binding = RingtoneItemSoundBinding.bind(itemView);

        binding.getRoot().setPadding(0, 0, 0, adapter.getDisplayMetricsPadding());

        binding.ringtoneImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_about));
        binding.ringtoneImage.setPadding(0, 0, 0, 0);

        binding.soundImageSelected.setVisibility(GONE);

        final int textColor = MaterialColors.getColor(context, android.R.attr.textColorSecondary, Color.BLACK);

        binding.ringtoneName.setTypeface(adapter.getGeneralTypeface(), Typeface.ITALIC);
        binding.ringtoneName.setTextColor(textColor);
        binding.ringtoneName.setSingleLine(false);
        binding.ringtoneName.setText(context.getString(R.string.button_tip_title));
    }

}
