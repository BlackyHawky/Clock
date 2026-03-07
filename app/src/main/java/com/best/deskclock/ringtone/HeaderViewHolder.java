/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

    private final TextView mItemHeader;

    public HeaderViewHolder(View itemView) {
        super(itemView);
        mItemHeader = itemView.findViewById(R.id.ringtone_item_header);

        mItemHeader.setTypeface(ThemeUtils.loadFont(
                SettingsDAO.getGeneralFont(getDefaultSharedPreferences(mItemHeader.getContext()))));
    }

    public void bind(HeaderHolder itemHolder) {
        mItemHeader.setText(itemHolder.getTextResId());
    }

}
