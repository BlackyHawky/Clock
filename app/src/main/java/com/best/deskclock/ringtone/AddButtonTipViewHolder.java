// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import static android.view.View.GONE;

import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.databinding.RingtoneItemSoundBinding;
import com.best.deskclock.uidata.UiConfig;

public class AddButtonTipViewHolder extends RecyclerView.ViewHolder {

    public AddButtonTipViewHolder(@NonNull View itemView, @NonNull RingtoneAdapter adapter, @NonNull UiConfig.Fonts fonts) {
        super(itemView);

        RingtoneItemSoundBinding binding = RingtoneItemSoundBinding.bind(itemView);

        binding.getRoot().setPadding(0, 0, 0, adapter.getDisplayMetricsPadding());

        binding.ringtoneImage.setImageDrawable(adapter.getAboutIcon());
        binding.ringtoneImage.setPadding(0, 0, 0, 0);

        binding.soundImageSelected.setVisibility(GONE);

        binding.ringtoneName.setTypeface(fonts.general(), Typeface.ITALIC);
        binding.ringtoneName.setTextColor(adapter.getTipTextColor());
        binding.ringtoneName.setSingleLine(false);
        binding.ringtoneName.setText(adapter.getTipText());
    }

}
