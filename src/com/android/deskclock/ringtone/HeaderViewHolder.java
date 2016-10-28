/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.ringtone;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;

final class HeaderViewHolder extends ItemAdapter.ItemViewHolder<HeaderHolder> {

    static final int VIEW_TYPE_ITEM_HEADER = R.layout.ringtone_item_header;

    private final TextView mItemHeader;

    private HeaderViewHolder(View itemView) {
        super(itemView);
        mItemHeader = (TextView) itemView.findViewById(R.id.ringtone_item_header);
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