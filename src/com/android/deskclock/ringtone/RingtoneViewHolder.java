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

import android.graphics.PorterDuff;
import androidx.core.content.ContextCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.Utils;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.OnCreateContextMenuListener;
import static android.view.View.VISIBLE;

final class RingtoneViewHolder extends ItemAdapter.ItemViewHolder<RingtoneHolder>
        implements OnClickListener, OnCreateContextMenuListener {

    static final int VIEW_TYPE_SYSTEM_SOUND = R.layout.ringtone_item_sound;
    static final int VIEW_TYPE_CUSTOM_SOUND = -R.layout.ringtone_item_sound;
    static final int CLICK_NORMAL = 0;
    static final int CLICK_LONG_PRESS = -1;
    static final int CLICK_NO_PERMISSIONS = -2;

    private final View mSelectedView;
    private final TextView mNameView;
    private final ImageView mImageView;

    private RingtoneViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        mSelectedView = itemView.findViewById(R.id.sound_image_selected);
        mNameView = (TextView) itemView.findViewById(R.id.ringtone_name);
        mImageView = (ImageView) itemView.findViewById(R.id.ringtone_image);
    }

    @Override
    protected void onBindItemView(RingtoneHolder itemHolder) {
        mNameView.setText(itemHolder.getName());
        final boolean opaque = itemHolder.isSelected() || !itemHolder.hasPermissions();
        mNameView.setAlpha(opaque ? 1f : .63f);
        mImageView.setAlpha(opaque ? 1f : .63f);
        mImageView.clearColorFilter();

        final int itemViewType = getItemViewType();
        if (itemViewType == VIEW_TYPE_CUSTOM_SOUND) {
            if (!itemHolder.hasPermissions()) {
                mImageView.setImageResource(R.drawable.ic_ringtone_not_found);
                final int colorAccent = ThemeUtils.resolveColor(itemView.getContext(),
                        R.attr.colorAccent);
                mImageView.setColorFilter(colorAccent, PorterDuff.Mode.SRC_ATOP);
            } else {
                mImageView.setImageResource(R.drawable.placeholder_album_artwork);
            }
        } else if (itemHolder.item == Utils.RINGTONE_SILENT) {
            mImageView.setImageResource(R.drawable.ic_ringtone_silent);
        } else {
            mImageView.setImageResource(R.drawable.ic_ringtone);
        }
        AnimatorUtils.startDrawableAnimation(mImageView);

        mSelectedView.setVisibility(itemHolder.isSelected() ? VISIBLE : GONE);

        final int bgColorId = itemHolder.isSelected() ? R.color.white_08p : R.color.transparent;
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), bgColorId));

        if (itemViewType == VIEW_TYPE_CUSTOM_SOUND) {
            itemView.setOnCreateContextMenuListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        if (getItemHolder().hasPermissions()) {
            notifyItemClicked(RingtoneViewHolder.CLICK_NORMAL);
        } else {
            notifyItemClicked(RingtoneViewHolder.CLICK_NO_PERMISSIONS);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view,
            ContextMenu.ContextMenuInfo contextMenuInfo) {
        notifyItemClicked(RingtoneViewHolder.CLICK_LONG_PRESS);
        contextMenu.add(Menu.NONE, 0, Menu.NONE, R.string.remove_sound);
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {

        private final LayoutInflater mInflater;

        Factory(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            final View itemView = mInflater.inflate(R.layout.ringtone_item_sound, parent, false);
            return new RingtoneViewHolder(itemView);
        }
    }
}
