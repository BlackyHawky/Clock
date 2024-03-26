/*
 *  Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.best.deskclock.ringtone.ui;


import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;

import com.best.deskclock.ringtone.RingtoneItem;
import com.squareup.picasso.Picasso;

import com.best.deskclock.R;
import com.best.deskclock.ringtone.RingtonePreviewKlaxon;

import java.util.ArrayList;

/**
 * A fragment which is generally used for the ringtone picker.
 * It is responsible for everything ui related in the ringtonePicker.
 * Fragments extending this one are only for data-fetching
 */
public class BasePickerFragment extends Fragment {
    ArrayList<RingtoneItem> mList;

    ArrayAdapter<RingtoneItem> mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View v = inflater.inflate(R.layout.ringtone_picker_fragment, container, false);
        ListView list = v.findViewById(R.id.ringtone_content);
        list.setDivider(null);
        mList = getList(getContext());
        mAdapter = adapter(mList);
        list.setAdapter(mAdapter);
        int c = getActivity().getColor(R.color.md_theme_primary);
        c = Color.argb(88, Color.red(c), Color.green(c), Color.blue(c));
        list.setBackgroundColor(ColorUtils.compositeColors(c, getActivity().getColor(R.color.md_theme_surface)));
        return v;
    }

    public ArrayAdapter<RingtoneItem> adapter(ArrayList<RingtoneItem> list) {
        return new ArrayAdapter<RingtoneItem>(getActivity(), R.layout.ringtone_item_sound, list) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                RingtonePickerActivity activity = (RingtonePickerActivity) getActivity();
                view = view == null ? getLayoutInflater().inflate(R.layout.ringtone_item_sound, null) : view;
                RingtoneItem item = mList.get(position);
                TextView title = view.findViewById(R.id.ringtone_name);
                title.setText(item.title);
                TextView desc = view.findViewById(R.id.ringtone_desc);
                if (item.desc == null) {
                    desc.setVisibility(View.GONE);
                } else {
                    desc.setText(item.desc);
                    desc.setVisibility(View.VISIBLE);
                }
                if (activity.mCurrentItem.uri.equals(item.uri)) {
                    activity.mCurrentItem = item;
                }
                view.findViewById(R.id.sound_image_selected).setVisibility(activity.mCurrentItem.equals(item) ? View.VISIBLE : View.GONE);

                Drawable d = activity.getDrawable(item.iconId);
                d.setTint(activity.getColor(R.color.md_theme_surface));
                ImageView image = view.findViewById(R.id.ringtone_image);
                if (item.imageUri != null && Uri.parse(item.imageUri) != null) {
                    Picasso cropper = Picasso.get();
                    cropper.load(Uri.parse(item.imageUri))
                            .placeholder(d)
                            .resizeDimen(R.dimen.ringtone_image_size, R.dimen.ringtone_image_size)
                            .centerCrop()
                            .into(image);
                } else {
                    image.setImageDrawable(d);
                }
                view.setOnClickListener(view1 -> {
                    RingtonePreviewKlaxon.stop(getActivity());
                    if (!activity.mCurrentItem.equals(item)) {
                        activity.mCurrentItem = item;
                        RingtonePreviewKlaxon.start(getActivity(), Uri.parse(item.uri), AudioManager.STREAM_ALARM);
                        mAdapter.notifyDataSetChanged();
                        for (int i = 0; i<=4; i++) {
                            (activity.mAdapter.getItem(i)).mAdapter.notifyDataSetChanged();
                        }
                    }
                });

                return view;
            }
        };
    }

    ArrayList<RingtoneItem> getList(Context context) {
        return new ArrayList<>();
    }
}
