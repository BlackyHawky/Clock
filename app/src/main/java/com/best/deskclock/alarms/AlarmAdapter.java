// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmItemViewHolder> {

    private static final String PAYLOAD_UPDATE_BACKGROUND = "PAYLOAD_UPDATE_BACKGROUND";

    private List<AlarmItemHolder> mItems = new ArrayList<>();

    public AlarmAdapter() {
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public AlarmItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.alarm_item, parent, false);
        return new AlarmItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmItemViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_UPDATE_BACKGROUND)) {
            holder.updateBackground();
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmItemViewHolder holder, int position) {
        AlarmItemHolder itemHolder = mItems.get(position);
        holder.bind(itemHolder);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).itemId;
    }

    public void setItems(List<AlarmItemHolder> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    public void removeItem(AlarmItemHolder itemHolder) {
        int position = mItems.indexOf(itemHolder);
        if (position != -1) {
            mItems.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void swapItems(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mItems, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mItems, i, i - 1);
            }
        }

        notifyItemMoved(fromPosition, toPosition);
        notifyItemRangeChanged(0, mItems.size(), PAYLOAD_UPDATE_BACKGROUND);
    }

    public List<AlarmItemHolder> getItems() {
        return mItems;
    }

}
