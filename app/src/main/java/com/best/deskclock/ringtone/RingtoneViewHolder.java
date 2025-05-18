/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.utils.RingtoneUtils.RANDOM_RINGTONE;
import static com.best.deskclock.utils.RingtoneUtils.RINGTONE_SILENT;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.color.MaterialColors;

final class RingtoneViewHolder extends ItemAdapter.ItemViewHolder<RingtoneHolder>
        implements OnClickListener {

    static final int VIEW_TYPE_SYSTEM_SOUND = R.layout.ringtone_item_sound;
    static final int VIEW_TYPE_CUSTOM_SOUND = -R.layout.ringtone_item_sound;
    static final int CLICK_NORMAL = 0;
    static final int CLICK_REMOVE = -1;

    private final View mSelectedView;
    private final TextView mNameView;
    private final ImageView mImageView;
    private final ImageButton mDeleteRingtone;

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
        // Allow text scrolling (all other attributes are indicated in the "ringtone_item_sound.xml" file)
        mNameView.setSelected(true);
        final Context context = itemView.getContext();
        final boolean opaque = itemHolder.isSelected();
        mNameView.setAlpha(opaque ? 1f : .63f);
        mImageView.setAlpha(opaque ? 1f : .63f);
        mImageView.clearColorFilter();

        final Drawable ringtone = AppCompatResources.getDrawable(context, R.drawable.ic_ringtone_active_animated);

        final int itemViewType = getItemViewType();
        if (itemViewType == VIEW_TYPE_CUSTOM_SOUND) {
            if (!RingtoneUtils.isRingtoneUriReadable(context, itemHolder.getUri())) {
                final Drawable error = AppCompatResources.getDrawable(context, R.drawable.ic_error);
                if (error != null) {
                    error.setTint(Color.parseColor("#FF4444"));
                }
                mImageView.setImageDrawable(error);
            } else {
                mImageView.setImageDrawable(ringtone);
            }

            mDeleteRingtone.setVisibility(VISIBLE);
            mDeleteRingtone.setOnClickListener(v -> notifyItemClicked(RingtoneViewHolder.CLICK_REMOVE));
        } else if (itemHolder.item == RINGTONE_SILENT) {
            final Drawable ringtoneSilent = AppCompatResources.getDrawable(context, R.drawable.ic_ringtone_silent);
            if (ringtoneSilent != null) {
                ringtoneSilent.setTint(MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK));
            }
            mImageView.setImageDrawable(ringtoneSilent);
        } else if (itemHolder.item == RANDOM_RINGTONE) {
            final Drawable randomRingtone = AppCompatResources.getDrawable(context, R.drawable.ic_random);
            mImageView.setImageDrawable(randomRingtone);
        } else {
            mImageView.setImageDrawable(ringtone);
        }

        final int backgroundColor;
        if (itemHolder.isSelected()) {
            mSelectedView.setVisibility(VISIBLE);

            backgroundColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.BLACK);

            if (itemHolder.isPlaying()) {
                AnimatorUtils.startDrawableAnimation(mImageView);
            }
        } else {
            mSelectedView.setVisibility(GONE);

            if (ThemeUtils.isNight(context.getResources())
                    && SettingsDAO.getDarkMode(getDefaultSharedPreferences(context)).equals(AMOLED_DARK_MODE)) {
                backgroundColor = Color.BLACK;
            } else {
                backgroundColor = MaterialColors.getColor(context, android.R.attr.colorBackground, Color.BLACK);
            }
        }

        itemView.setBackground(ThemeUtils.rippleDrawable(context, backgroundColor));
    }

    @Override
    public void onClick(View view) {
        notifyItemClicked(RingtoneViewHolder.CLICK_NORMAL);
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
