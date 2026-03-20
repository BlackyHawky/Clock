// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;

import java.util.ArrayList;
import java.util.List;

public class RingtoneAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_SYSTEM_SOUND = R.layout.ringtone_item_sound;
    public static final int VIEW_TYPE_CUSTOM_SOUND = -R.layout.ringtone_item_sound;
    public static final int VIEW_TYPE_HEADER = R.layout.ringtone_item_header;
    public static final int VIEW_TYPE_BUTTON_TIP = Integer.MIN_VALUE;

    private List<RingtoneItem> mItems = new ArrayList<>();
    private final OnRingtoneClickListener mListener;

    public RingtoneAdapter(OnRingtoneClickListener listener) {
        mListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(viewType, parent, false));
        } else if (viewType == VIEW_TYPE_BUTTON_TIP) {
            return new AddButtonTipViewHolder(inflater.inflate(R.layout.ringtone_item_sound, parent, false));
        } else {
            return new RingtoneViewHolder(inflater.inflate(Math.abs(viewType), parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RingtoneItem item = mItems.get(position);

        if (holder instanceof RingtoneViewHolder ringtoneViewHolder) {
            ringtoneViewHolder.bind((RingtoneHolder) item, mListener);
        } else if (holder instanceof HeaderViewHolder headerViewHolder) {
            headerViewHolder.bind((HeaderHolder) item);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setItems(List<RingtoneItem> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    public List<RingtoneItem> getItems() {
        return mItems;
    }

    public interface RingtoneItem {
        int getViewType();
    }

    public interface OnRingtoneClickListener {
        void onRingtoneClick(RingtoneHolder holder);

        void onRemoveRingtoneClick(RingtoneHolder holder);
    }

}
