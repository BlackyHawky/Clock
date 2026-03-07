// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;

import java.util.ArrayList;
import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmItemViewHolder> {

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

}
