/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms.alarmselection;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.databinding.AlarmRowBinding;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.utils.ThemeUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AlarmSelectionAdapter extends RecyclerView.Adapter<AlarmSelectionAdapter.ViewHolder> {

    private final List<AlarmSelection> alarms;
    private final OnAlarmClickListener listener;

    private final Typeface mRegularTypeface;
    private final Typeface mAlarmBoldTypeface;
    private final Weekdays.Order mWeekdayOrder;
    private final SimpleDateFormat mDateFormat;

    public AlarmSelectionAdapter(Context context, List<AlarmSelection> alarms, OnAlarmClickListener listener) {
        this.alarms = alarms;
        this.listener = listener;

        SharedPreferences prefs = getDefaultSharedPreferences(context);

        mRegularTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));
        mAlarmBoldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getAlarmFont(prefs));
        mWeekdayOrder = SettingsDAO.getWeekdayOrder(prefs);

        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMMMd");
        mDateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AlarmRowBinding binding = AlarmRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlarmSelection selection = alarms.get(position);
        Alarm alarm = selection.getAlarm();

        holder.bind(alarm, mRegularTypeface, mAlarmBoldTypeface, mWeekdayOrder, mDateFormat);

        holder.itemView.setOnClickListener(v -> listener.onAlarmClick(alarm));
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final AlarmRowBinding binding;

        public ViewHolder(AlarmRowBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        public void bind(Alarm alarm, Typeface regularTypeface, Typeface alarmBoldTypeface, Weekdays.Order weekdayOrder,
                         SimpleDateFormat dateFormat) {

            Context context = itemView.getContext();

            binding.digitalClock.setTime(alarm.hour, alarm.minutes);
            binding.digitalClock.setTypeface(alarmBoldTypeface);
            binding.alarmLabel.setText(alarm.label);
            binding.alarmLabel.setTypeface(regularTypeface);

            // Find days when alarm is firing
            if (alarm.daysOfWeek.isRepeating()) {
                final String daysOfWeekText = alarm.daysOfWeek.toString(context, weekdayOrder);
                binding.daysOfWeek.setText(daysOfWeekText);

                final String string = alarm.daysOfWeek.toAccessibilityString(context, weekdayOrder);
                binding.daysOfWeek.setContentDescription(string);
            } else {
                Calendar calendar = Calendar.getInstance();

                if (alarm.isTomorrow(calendar) && !alarm.isSpecifiedDate()) {
                    binding.daysOfWeek.setText(context.getResources().getString(R.string.alarm_tomorrow));
                } else if (alarm.isSpecifiedDate()) {
                    if (Alarm.isSpecifiedDateTomorrow(alarm.year, alarm.month, alarm.day)) {
                        binding.daysOfWeek.setText(context.getResources().getString(R.string.alarm_tomorrow));
                    } else if (alarm.isDateInThePast()) {
                        // If the date has passed, the new alarm will be scheduled either the same day
                        // or the next day depending on the time; the text is therefore updated accordingly.
                        if (alarm.hour < calendar.get(Calendar.HOUR_OF_DAY)
                            || (alarm.hour == calendar.get(Calendar.HOUR_OF_DAY) && alarm.minutes < calendar.get(Calendar.MINUTE))
                            || (alarm.hour == calendar.get(Calendar.HOUR_OF_DAY) && alarm.minutes == calendar.get(Calendar.MINUTE))) {
                            binding.daysOfWeek.setText(context.getString(R.string.alarm_tomorrow));
                        } else {
                            binding.daysOfWeek.setText(context.getString(R.string.alarm_today));
                        }
                    } else {
                        int year = alarm.year;
                        int month = alarm.month;
                        int dayOfMonth = alarm.day;

                        calendar.set(year, month, dayOfMonth);

                        String formattedDate = dateFormat.format(calendar.getTime());

                        binding.daysOfWeek.setText(context.getResources().getString(R.string.alarm_scheduled_for, formattedDate));
                    }
                } else {
                    binding.daysOfWeek.setText(context.getResources().getString(R.string.alarm_today));
                }
            }

            binding.daysOfWeek.setTypeface(regularTypeface);
        }
    }

    public interface OnAlarmClickListener {
        void onAlarmClick(Alarm alarm);
    }

}
