/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.best.deskclock.AnimatorUtils;
import com.best.deskclock.ItemAdapter;
import com.best.deskclock.R;
import com.best.deskclock.Utils;

final class RingtoneViewHolder extends ItemAdapter.ItemViewHolder<RingtoneHolder>
        implements OnClickListener {

    static final int VIEW_TYPE_SYSTEM_SOUND = R.layout.ringtone_item_sound;
    static final int VIEW_TYPE_CUSTOM_SOUND = -R.layout.ringtone_item_sound;
    static final int CLICK_NORMAL = 0;
    static final int CLICK_REMOVE = -1;
    static final int CLICK_NO_PERMISSIONS = -2;

    private final View mSelectedView;
    private final TextView mNameView;
    private final ImageView mImageView;
    private final ImageView mDeleteRingtone;

    private RingtoneViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        mSelectedView = itemView.findViewById(R.id.sound_image_selected);
        mNameView = itemView.findViewById(R.id.ringtone_name);
        mImageView = itemView.findViewById(R.id.ringtone_image);
        mDeleteRingtone = itemView.findViewById(R.id.delete_ringtone);
    }

    @Override
    protected void onBindItemView(RingtoneHolder itemHolder) {
        mNameView.setText(itemHolder.getName());

        final boolean opaque = itemHolder.isSelected() || !itemHolder.hasPermissions();
        mNameView.setAlpha(opaque ? 1f : .63f);
        mImageView.setAlpha(opaque ? 1f : .63f);
        mImageView.clearColorFilter();

        final Drawable ringtone = AppCompatResources.getDrawable(itemView.getContext(), (R.drawable.ic_ringtone));

        final int itemViewType = getItemViewType();
        if (itemViewType == VIEW_TYPE_CUSTOM_SOUND) {
            if (!itemHolder.hasPermissions()) {
                final Drawable error = AppCompatResources.getDrawable(itemView.getContext(), (R.drawable.ic_error));
                if (error != null) {
                    error.setTint(Color.parseColor("#FF4444"));
                }
                mImageView.setImageDrawable(error);
            } else {
                mImageView.setImageDrawable(ringtone);
            }
        } else if (itemHolder.item == Utils.RINGTONE_SILENT) {
            final Drawable ringtoneSilent = AppCompatResources.getDrawable(itemView.getContext(), (R.drawable.ic_ringtone_silent));
            if (ringtoneSilent != null) {
                ringtoneSilent.setTint(itemView.getContext().getColor(R.color.md_theme_onSurfaceVariant));
            }
            mImageView.setImageDrawable(ringtoneSilent);
        } else {
            mImageView.setImageDrawable(ringtone);
        }
        AnimatorUtils.startDrawableAnimation(mImageView);

        mSelectedView.setVisibility(itemHolder.isSelected() ? VISIBLE : GONE);

        final int bgColorId = itemHolder.isSelected() ? R.color.md_theme_surfaceVariant : android.R.color.transparent;
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), bgColorId));

        if (itemViewType == VIEW_TYPE_CUSTOM_SOUND) {
            mDeleteRingtone.setVisibility(VISIBLE);
            mDeleteRingtone.getDrawable().setTint(mDeleteRingtone.getContext().getColor(R.color.md_theme_onSurfaceVariant));
            mDeleteRingtone.setOnClickListener(v -> notifyItemClicked(RingtoneViewHolder.CLICK_REMOVE));
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
