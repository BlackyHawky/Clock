/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.databinding.RingtoneItemHeaderBinding;
import com.best.deskclock.uidata.UiConfig;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

    private final RingtoneItemHeaderBinding mBinding;

    public HeaderViewHolder(@NonNull View itemView, @NonNull UiConfig.Fonts fonts) {
        super(itemView);

        mBinding = RingtoneItemHeaderBinding.bind(itemView);

        mBinding.ringtoneItemHeader.setTypeface(fonts.general());
    }

    public void bind(@NonNull HeaderHolder itemHolder) {
        mBinding.ringtoneItemHeader.setText(itemHolder.getTextResId());
    }

}
