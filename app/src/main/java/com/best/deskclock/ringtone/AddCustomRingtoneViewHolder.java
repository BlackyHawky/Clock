/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static android.view.View.GONE;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.best.deskclock.ItemAdapter.ItemViewHolder;
import com.best.deskclock.R;

final class AddCustomRingtoneViewHolder extends ItemViewHolder<AddCustomRingtoneHolder>
        implements View.OnClickListener {

    static final int VIEW_TYPE_ADD_NEW = Integer.MIN_VALUE;
    static final int CLICK_ADD_NEW = VIEW_TYPE_ADD_NEW;

    private AddCustomRingtoneViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        final View selectedView = itemView.findViewById(R.id.sound_image_selected);
        selectedView.setVisibility(GONE);

        final TextView nameView = itemView.findViewById(R.id.ringtone_name);
        nameView.setText(itemView.getContext().getString(R.string.add_new_sound));

        final ImageView imageView = itemView.findViewById(R.id.ringtone_image);
        imageView.setImageDrawable(AppCompatResources.getDrawable(itemView.getContext(), R.drawable.ic_add));
        imageView.getDrawable().setColorFilter(imageView.getContext().getColor(R.color.md_theme_background), PorterDuff.Mode.SRC_IN);
        imageView.setBackgroundResource(R.drawable.bg_circle);
        imageView.setBackgroundTintList(
                ColorStateList.valueOf(itemView.getContext().getColor(R.color.md_theme_primary)));
    }

    @Override
    public void onClick(View view) {
        notifyItemClicked(AddCustomRingtoneViewHolder.CLICK_ADD_NEW);
    }

    public static class Factory implements ItemViewHolder.Factory {

        private final LayoutInflater mInflater;

        Factory(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @Override
        public ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            final View itemView = mInflater.inflate(R.layout.ringtone_item_sound, parent, false);
            return new AddCustomRingtoneViewHolder(itemView);
        }
    }
}
