// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.databinding.RingtoneItemAutoplayBinding;
import com.best.deskclock.uidata.UiConfig;

public class AutoplaySwitchViewHolder extends RecyclerView.ViewHolder {
    private final RingtoneItemAutoplayBinding mBinding;

    public AutoplaySwitchViewHolder(@NonNull View itemView, @NonNull RingtoneAdapter adapter, @NonNull UiConfig.Fonts fonts) {
        super(itemView);

        mBinding = RingtoneItemAutoplayBinding.bind(itemView);

        itemView.setBackground(adapter.getBgSelectedState().newDrawable());

        mBinding.autoplayTitle.setTypeface(fonts.general());
    }

    public void bind(boolean isAutoPlayEnabled, @NonNull RingtoneAdapter.OnRingtoneClickListener listener) {
        mBinding.autoplaySwitch.setOnCheckedChangeListener(null);
        mBinding.autoplaySwitch.setChecked(isAutoPlayEnabled);

        mBinding.autoplaySwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            listener.onAutoPlayToggled(isChecked));

        itemView.setOnClickListener(v -> mBinding.autoplaySwitch.toggle());
    }

}
