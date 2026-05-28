/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.databinding.RingtoneItemHeaderBinding;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

    private final RingtoneItemHeaderBinding mBinding;

    public HeaderViewHolder(View itemView, RingtoneAdapter adapter) {
        super(itemView);

        mBinding = RingtoneItemHeaderBinding.bind(itemView);

        mBinding.ringtoneItemHeader.setTypeface(adapter.getGeneralTypeface());
    }

    public void bind(HeaderHolder itemHolder) {
        mBinding.ringtoneItemHeader.setText(itemHolder.getTextResId());
    }

}
