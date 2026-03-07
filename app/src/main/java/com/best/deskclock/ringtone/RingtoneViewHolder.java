/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.color.MaterialColors;

public class RingtoneViewHolder extends RecyclerView.ViewHolder {

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final View mSelectedView;
    private final TextView mNameView;
    private final ImageView mImageView;
    private final ImageButton mDeleteRingtone;
    private final Drawable mRingtoneIcon;
    private final Drawable mErrorIcon;
    private final Drawable mSilentIcon;
    private final Drawable mRandomIcon;

    public RingtoneViewHolder(View itemView) {
        super(itemView);

        mContext = itemView.getContext();
        mPrefs = getDefaultSharedPreferences(mContext);

        mSelectedView = itemView.findViewById(R.id.sound_image_selected);
        mNameView = itemView.findViewById(R.id.ringtone_name);
        mImageView = itemView.findViewById(R.id.ringtone_image);
        mDeleteRingtone = itemView.findViewById(R.id.delete_ringtone);
        mRingtoneIcon = AppCompatResources.getDrawable(mContext, R.drawable.ic_ringtone_active_animated);
        mRandomIcon = AppCompatResources.getDrawable(mContext, R.drawable.ic_random);

        Drawable error = AppCompatResources.getDrawable(mContext, R.drawable.ic_error);
        if (error != null) {
            mErrorIcon = error.mutate();
            mErrorIcon.setTint(ContextCompat.getColor(mContext, android.R.color.holo_red_light));
        } else {
            mErrorIcon = null;
        }

        Drawable silent = AppCompatResources.getDrawable(mContext, R.drawable.ic_ringtone_silent);
        if (silent != null) {
            mSilentIcon = silent.mutate();
            mSilentIcon.setTint(MaterialColors.getColor(mContext,
                    com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK));
        } else {
            mSilentIcon = null;
        }

        mNameView.setTypeface(ThemeUtils.loadFont(SettingsDAO.getGeneralFont(mPrefs)));
        // Allow text scrolling (all other attributes are indicated in the "ringtone_item_sound.xml" file)
        mNameView.setSelected(true);
    }

    public void bind(RingtoneHolder itemHolder, RingtoneAdapter.OnRingtoneClickListener listener) {
        mNameView.setText(itemHolder.getName());

        final boolean isSelected = itemHolder.isSelected();
        float alpha = isSelected ? 1f : 0.6f;
        mNameView.setAlpha(alpha);
        mImageView.setAlpha(alpha);
        mImageView.clearColorFilter();

        setupIcon(mContext, itemHolder);

        if (getItemViewType() == RingtoneAdapter.VIEW_TYPE_CUSTOM_SOUND) {
            mDeleteRingtone.setVisibility(View.VISIBLE);
            mDeleteRingtone.setOnClickListener(v -> listener.onRemoveRingtoneClick(itemHolder));
        } else {
            mDeleteRingtone.setVisibility(View.GONE);
        }

        int backgroundColor = getBackgroundColor(mContext, mPrefs, isSelected);
        mSelectedView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        itemView.setBackground(ThemeUtils.rippleDrawable(mContext, backgroundColor));

        // Animate the icon if playing
        if (mImageView.getDrawable() instanceof Animatable animatable) {
            if (isSelected && itemHolder.isPlaying()) {
                animatable.start();
            } else {
                animatable.stop();
            }
        }

        itemView.setOnClickListener(v -> listener.onRingtoneClick(itemHolder));
    }

    private void setupIcon(Context context, RingtoneHolder itemHolder) {
        if (getItemViewType() == RingtoneAdapter.VIEW_TYPE_CUSTOM_SOUND) {
            if (!RingtoneUtils.isRingtoneUriReadable(context, itemHolder.getUri())) {
                mImageView.setImageDrawable(mErrorIcon);
            } else {
                mImageView.setImageDrawable(mRingtoneIcon);
            }
        } else if (itemHolder.isSilent()) {
            mImageView.setImageDrawable(mSilentIcon);
        } else if (itemHolder.isRandom()) {
            mImageView.setImageDrawable(mRandomIcon);
        } else {
            mImageView.setImageDrawable(mRingtoneIcon);
        }
    }

    private int getBackgroundColor(Context context, SharedPreferences prefs, boolean isSelected) {
        if (isSelected) {
            return MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.BLACK);
        } else {
            if (ThemeUtils.isNight(context.getResources()) && AMOLED_DARK_MODE.equals(SettingsDAO.getDarkMode(prefs))) {
                return Color.BLACK;
            }

            return MaterialColors.getColor(context, android.R.attr.colorBackground, Color.BLACK);
        }
    }

}
