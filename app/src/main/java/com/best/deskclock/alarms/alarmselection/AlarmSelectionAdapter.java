/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms.alarmselection;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.databinding.AlarmRowBinding;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.ClockUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class AlarmSelectionAdapter extends RecyclerView.Adapter<AlarmSelectionAdapter.ViewHolder> {

    private final List<AlarmSelection> mAlarms;
    private final OnAlarmClickListener mListener;
    private final UiConfig.Fonts mFonts;
    private final Weekdays.Order mWeekdayOrder;
    private final SimpleDateFormat mDateFormat;
    private final boolean mIs24HourMode;
    private final CharSequence mFormat12;
    private final CharSequence mFormat24;

    public AlarmSelectionAdapter(@NonNull List<AlarmSelection> alarms, @NonNull UiConfig.Fonts fonts,
                                 @NonNull UiConfig.DateFormat dateFormatConfig, @NonNull Weekdays.Order weekdayOrder,
                                 boolean is24HourMode, @NonNull OnAlarmClickListener listener) {

        mAlarms = alarms;
        mFonts = fonts;
        mWeekdayOrder = weekdayOrder;
        mListener = listener;
        mIs24HourMode = is24HourMode;
        mDateFormat = new SimpleDateFormat(dateFormatConfig.patternWithYear(), dateFormatConfig.locale());

        Typeface alarmFont = fonts.alarmClockFont() != null ? fonts.alarmClockFont() : fonts.bold();
        mFormat12 = ClockUtils.get12ModeFormat(false, 0.5f, alarmFont, "sans-serif", Typeface.BOLD, false);
        mFormat24 = ClockUtils.get24ModeFormat(false, false);
    }

    public UiConfig.Fonts getFonts() { return mFonts; }
    public Weekdays.Order getWeekdayOrder() { return mWeekdayOrder; }
    public SimpleDateFormat getDateFormat() { return mDateFormat; }
    public boolean is24HourFormat() { return mIs24HourMode; }
    public CharSequence getFormat12() { return mFormat12; }
    public CharSequence getFormat24() { return mFormat24; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AlarmRowBinding binding = AlarmRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlarmSelection selection = mAlarms.get(position);
        Alarm alarm = selection.getAlarm();

        holder.bind(alarm);

        holder.itemView.setOnClickListener(v -> mListener.onAlarmClick(alarm));
    }

    @Override
    public int getItemCount() {
        return mAlarms.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final AlarmRowBinding mBinding;
        final AlarmSelectionAdapter mAdapter;

        public ViewHolder(@NonNull AlarmRowBinding binding, @NonNull AlarmSelectionAdapter adapter) {

            super(binding.getRoot());

            mBinding = binding;
            mAdapter = adapter;

            UiConfig.Fonts fonts = mAdapter.getFonts();
            mBinding.alarmLabel.setTypeface(fonts.general());
            mBinding.daysOfWeek.setTypeface(fonts.general());
        }

        public void bind(@NonNull Alarm alarm) {
            Context context = itemView.getContext();
            boolean is24HourMode = mAdapter.is24HourFormat();
            CharSequence format12 = mAdapter.getFormat12();
            CharSequence format24 = mAdapter.getFormat24();
            Typeface alarmFont = mAdapter.getFonts().alarmClockFont() != null ? mAdapter.getFonts().alarmClockFont() : mAdapter.getFonts().bold();

            mBinding.digitalClock.configure(is24HourMode, format12, format24);
            mBinding.digitalClock.setTypeface(alarmFont);
            mBinding.digitalClock.setTime(alarm.hour, alarm.minutes);

            mBinding.alarmLabel.setText(alarm.label);

            // Find days when alarm is firing
            if (alarm.daysOfWeek.isRepeating()) {
                final String daysOfWeekText = alarm.daysOfWeek.toString(context, mAdapter.getWeekdayOrder());
                mBinding.daysOfWeek.setText(daysOfWeekText);

                final String string = alarm.daysOfWeek.toAccessibilityString(context, mAdapter.getWeekdayOrder());
                mBinding.daysOfWeek.setContentDescription(string);
            } else {
                Calendar calendar = Calendar.getInstance();

                if (alarm.isTomorrow(calendar) && !alarm.isSpecifiedDate()) {
                    mBinding.daysOfWeek.setText(context.getResources().getString(R.string.alarm_tomorrow));
                } else if (alarm.isSpecifiedDate()) {
                    if (Alarm.isSpecifiedDateTomorrow(alarm.year, alarm.month, alarm.day)) {
                        mBinding.daysOfWeek.setText(context.getResources().getString(R.string.alarm_tomorrow));
                    } else if (alarm.isDateInThePast()) {
                        // If the date has passed, the new alarm will be scheduled either the same day
                        // or the next day depending on the time; the text is therefore updated accordingly.
                        if (alarm.hour < calendar.get(Calendar.HOUR_OF_DAY)
                            || (alarm.hour == calendar.get(Calendar.HOUR_OF_DAY) && alarm.minutes < calendar.get(Calendar.MINUTE))
                            || (alarm.hour == calendar.get(Calendar.HOUR_OF_DAY) && alarm.minutes == calendar.get(Calendar.MINUTE))) {
                            mBinding.daysOfWeek.setText(context.getString(R.string.alarm_tomorrow));
                        } else {
                            mBinding.daysOfWeek.setText(context.getString(R.string.alarm_today));
                        }
                    } else {
                        int year = alarm.year;
                        int month = alarm.month;
                        int dayOfMonth = alarm.day;

                        calendar.set(year, month, dayOfMonth);

                        String formattedDate = mAdapter.getDateFormat().format(calendar.getTime());

                        mBinding.daysOfWeek.setText(context.getResources().getString(R.string.alarm_scheduled_for, formattedDate));
                    }
                } else {
                    mBinding.daysOfWeek.setText(context.getResources().getString(R.string.alarm_today));
                }
            }
        }
    }

    public interface OnAlarmClickListener {
        void onAlarmClick(@NonNull Alarm alarm);
    }

}
