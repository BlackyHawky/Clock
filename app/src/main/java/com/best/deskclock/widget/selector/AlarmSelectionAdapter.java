/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.widget.selector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.TextTime;

import java.util.Calendar;
import java.util.List;

public class AlarmSelectionAdapter extends RecyclerView.Adapter<AlarmSelectionAdapter.ViewHolder> {

    private final Context context;
    private final List<AlarmSelection> alarms;
    private final OnAlarmClickListener listener;

    public interface OnAlarmClickListener {
        void onAlarmClick(Alarm alarm);
    }

    public AlarmSelectionAdapter(Context context, List<AlarmSelection> alarms, OnAlarmClickListener listener) {
        this.context = context;
        this.alarms = alarms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View row = inflater.inflate(R.layout.alarm_row, parent, false);

        int alarmRowMarginBottom = Utils.toPixel(Utils.isTablet(context) ? 64 : 8, context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, alarmRowMarginBottom);
        row.setLayoutParams(params);

        return new ViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlarmSelection selection = alarms.get(position);
        Alarm alarm = selection.getAlarm();

        holder.bind(alarm);

        holder.itemView.setOnClickListener(v -> listener.onAlarmClick(alarm));
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final TextTime alarmTime;
        final TextView alarmLabel;
        final TextView daysOfWeekView;

        public ViewHolder(View itemView) {
            super(itemView);

            alarmTime = itemView.findViewById(R.id.digital_clock);
            alarmLabel = itemView.findViewById(R.id.label);
            daysOfWeekView = itemView.findViewById(R.id.daysOfWeek);
        }

        public void bind(Alarm alarm) {
            Context context = itemView.getContext();

            alarmTime.setTime(alarm.hour, alarm.minutes);
            alarmLabel.setText(alarm.label);

            // find days when alarm is firing
            final String daysOfWeek;
            if (!alarm.daysOfWeek.isRepeating()) {
                daysOfWeek = Alarm.isTomorrow(alarm, Calendar.getInstance()) ?
                        context.getResources().getString(R.string.alarm_tomorrow) :
                        context.getResources().getString(R.string.alarm_today);
            } else {
                final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
                daysOfWeek = alarm.daysOfWeek.toString(context, weekdayOrder);
            }

            daysOfWeekView.setText(daysOfWeek);
        }
    }
}
