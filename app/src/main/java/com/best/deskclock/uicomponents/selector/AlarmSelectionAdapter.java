/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents.selector;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uicomponents.TextTime;
import com.best.deskclock.utils.ThemeUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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

        int alarmRowMarginBottom = ThemeUtils.convertDpToPixels(ThemeUtils.isTablet() ? 64 : 8, context);
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

            // Find days when alarm is firing
            if (alarm.daysOfWeek.isRepeating()) {
                final Weekdays.Order weekdayOrder = SettingsDAO.getWeekdayOrder(getDefaultSharedPreferences(context));
                final String daysOfWeekText = alarm.daysOfWeek.toString(context, weekdayOrder);
                daysOfWeekView.setText(daysOfWeekText);

                final String string = alarm.daysOfWeek.toAccessibilityString(context, weekdayOrder);
                daysOfWeekView.setContentDescription(string);
            } else {
                Calendar calendar = Calendar.getInstance();

                if (Alarm.isTomorrow(alarm, calendar) && !alarm.isSpecifiedDate()) {
                    daysOfWeekView.setText(context.getResources().getString(R.string.alarm_tomorrow));
                } else if (alarm.isSpecifiedDate()) {
                    if (Alarm.isSpecifiedDateTomorrow(alarm.year, alarm.month, alarm.day)) {
                        daysOfWeekView.setText(context.getResources().getString(R.string.alarm_tomorrow));
                    } else if (alarm.isDateInThePast()) {
                        // If the date has passed, the new alarm will be scheduled either the same day
                        // or the next day depending on the time; the text is therefore updated accordingly.
                        if (alarm.hour < calendar.get(Calendar.HOUR_OF_DAY)
                                || (alarm.hour == calendar.get(Calendar.HOUR_OF_DAY) && alarm.minutes < calendar.get(Calendar.MINUTE))
                                || (alarm.hour == calendar.get(Calendar.HOUR_OF_DAY) && alarm.minutes == calendar.get(Calendar.MINUTE))) {
                            daysOfWeekView.setText(context.getString(R.string.alarm_tomorrow));
                        } else {
                            daysOfWeekView.setText(context.getString(R.string.alarm_today));
                        }
                    } else {
                        int year = alarm.year;
                        int month = alarm.month;
                        int dayOfMonth = alarm.day;

                        calendar.set(year, month, dayOfMonth);

                        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMMMd");
                        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                        String formattedDate = dateFormat.format(calendar.getTime());

                        daysOfWeekView.setText(context.getResources().getString(R.string.alarm_scheduled_for, formattedDate));
                    }
                } else {
                    daysOfWeekView.setText(context.getResources().getString(R.string.alarm_today));
                }
            }
        }
    }
}
