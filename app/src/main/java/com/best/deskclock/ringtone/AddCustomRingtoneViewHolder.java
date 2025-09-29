/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static android.view.View.GONE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.best.deskclock.ItemAdapter.ItemViewHolder;
import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

final class AddCustomRingtoneViewHolder extends ItemViewHolder<AddCustomRingtoneHolder>
        implements View.OnClickListener, View.OnLongClickListener {

    static final int VIEW_TYPE_ADD_NEW = Integer.MIN_VALUE;
    static final int CLICK_ADD_NEW = VIEW_TYPE_ADD_NEW;
    static final int CLICK_ADD_FOLDER = VIEW_TYPE_ADD_NEW + 1;

    private AddCustomRingtoneViewHolder(View itemView) {
        super(itemView);

        itemView.setOnClickListener(this);

        itemView.setOnLongClickListener(this);

        final Context context = itemView.getContext();

        final View selectedView = itemView.findViewById(R.id.sound_image_selected);
        selectedView.setVisibility(GONE);

        final TextView nameView = itemView.findViewById(R.id.ringtone_name);
        nameView.setSingleLine(false);

        //Add vertical spacing between lines
        nameView.setLineSpacing(ThemeUtils.convertDpToPixels(4, context), 1.0f);

        String title = context.getString(R.string.add_new_sound);
        String subtitle = context.getString(R.string.add_new_sound_subtitle);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(title).append("\n").append(subtitle);

        // Apply the small style to the "subtitle" part
        int start = builder.length() - subtitle.length();
        int end = builder.length();
        TextAppearanceSpan smallSpan = new TextAppearanceSpan(context, android.R.style.TextAppearance_Small);
        builder.setSpan(smallSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        nameView.setText(builder);

        final ImageView imageView = itemView.findViewById(R.id.ringtone_image);
        imageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_add));
        imageView.getDrawable().setTint(MaterialColors.getColor(context, android.R.attr.colorBackground, Color.BLACK));
        imageView.setBackgroundResource(R.drawable.bg_circle);
        imageView.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(
                context, androidx.appcompat.R.attr.colorPrimary, Color.BLACK))
        );

        final int backgroundColor;
        if (ThemeUtils.isNight(context.getResources())
                && SettingsDAO.getDarkMode(getDefaultSharedPreferences(context)).equals(AMOLED_DARK_MODE)) {
            backgroundColor = Color.BLACK;
        } else {
            backgroundColor = MaterialColors.getColor(context, android.R.attr.colorBackground, Color.BLACK);
        }

        itemView.setBackground(ThemeUtils.rippleDrawable(context, backgroundColor));
    }

    @Override
    public void onClick(View view) {
        notifyItemClicked(AddCustomRingtoneViewHolder.CLICK_ADD_NEW);
    }

    @Override
    public boolean onLongClick(View v) {
        notifyItemLongClicked(AddCustomRingtoneViewHolder.CLICK_ADD_FOLDER);
        return true;
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
