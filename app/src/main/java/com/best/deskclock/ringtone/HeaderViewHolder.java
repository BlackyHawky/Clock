/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

    private final TextView mItemHeader;

    public HeaderViewHolder(View itemView, RingtoneAdapter adapter) {
        super(itemView);
        mItemHeader = itemView.findViewById(R.id.ringtone_item_header);

        mItemHeader.setTypeface(adapter.getGeneralTypeface());
    }

    public void bind(HeaderHolder itemHolder) {
        mItemHeader.setText(itemHolder.getTextResId());
    }

}
