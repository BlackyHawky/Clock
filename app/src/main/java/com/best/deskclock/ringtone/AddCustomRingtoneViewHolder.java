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

package com.best.deskclock.ringtone;

import static android.view.View.GONE;

import android.graphics.drawable.Drawable;
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
        nameView.setTextColor(itemView.getContext().getColor(R.color.md_theme_onSurfaceVariant));

        final ImageView imageView = itemView.findViewById(R.id.ringtone_image);
        final Drawable iconAdd = AppCompatResources.getDrawable(itemView.getContext(), R.drawable.ic_add);
        if (iconAdd == null) {
            return;
        }
        iconAdd.setTint(imageView.getContext().getColor(R.color.md_theme_onSurfaceVariant));
        imageView.setImageDrawable(iconAdd);
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
