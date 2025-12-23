/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;

final class HeaderViewHolder extends ItemAdapter.ItemViewHolder<HeaderHolder> {

    static final int VIEW_TYPE_ITEM_HEADER = R.layout.ringtone_item_header;

    private final TextView mItemHeader;

    private HeaderViewHolder(View itemView) {
        super(itemView);
        mItemHeader = itemView.findViewById(R.id.ringtone_item_header);
    }

    @Override
    protected void onBindItemView(HeaderHolder itemHolder) {
        mItemHeader.setText(itemHolder.getTextResId());
        mItemHeader.setTypeface(ThemeUtils.loadFont(
                SettingsDAO.getGeneralFont(getDefaultSharedPreferences(mItemHeader.getContext()))));
    }

    public record Factory(LayoutInflater mInflater) implements ItemAdapter.ItemViewHolder.Factory {

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            return new HeaderViewHolder(mInflater.inflate(viewType, parent, false));
        }
    }
}
