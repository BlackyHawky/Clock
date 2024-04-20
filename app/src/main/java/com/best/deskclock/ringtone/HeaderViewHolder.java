/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.R;

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
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {

        private final LayoutInflater mInflater;

        Factory(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            return new HeaderViewHolder(mInflater.inflate(viewType, parent, false));
        }
    }
}
