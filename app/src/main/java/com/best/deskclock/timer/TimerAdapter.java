/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.Context;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerListener;
import com.best.deskclock.R;

import java.util.List;
import java.util.Map;

/**
 * This adapter produces a {@link TimerViewHolder} for each timer.
 */
class TimerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements TimerListener {

    /** Maps each timer id to the corresponding {@link TimerViewHolder} that draws it. */
    private final Map<Integer, TimerViewHolder> mHolders = new ArrayMap<>();
    private final TimerClickHandler mTimerClickHandler;

    public TimerAdapter(TimerClickHandler timerClickHandler) {
        mTimerClickHandler = timerClickHandler;
    }

    @Override
    public int getItemCount() {
        return getTimers().size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.timer_item, parent, false);
        return new TimerViewHolder(view, mTimerClickHandler);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder itemViewHolder, int position) {
        TimerViewHolder holder = (TimerViewHolder) itemViewHolder;
        mHolders.put(getTimer(position).getId(), holder);
        holder.onBind(getTimer(position).getId());
    }

    @Override
    public void timerAdded(Timer timer) {
        notifyDataSetChanged();
    }

    @Override
    public void timerRemoved(Timer timer) {
        mHolders.remove(timer.getId());
        notifyDataSetChanged();
    }

    @Override
    public void timerUpdated(Timer before, Timer after) {
        notifyDataSetChanged();
    }

    /**
     * @return {@code true} if at least one timer is in a state requiring continuous updates
     */
    boolean updateTime() {
        boolean continuousUpdates = false;
        for (TimerViewHolder holder : mHolders.values()) {
            continuousUpdates |= holder.updateTime();
        }
        return continuousUpdates;
    }

    Timer getTimer(int index) {
        return getTimers().get(index);
    }

    private List<Timer> getTimers() {
        return DataModel.getDataModel().getTimers();
    }
}
