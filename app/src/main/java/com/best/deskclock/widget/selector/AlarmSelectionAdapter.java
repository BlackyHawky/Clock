/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.widget.selector;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.widget.TextTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AlarmSelectionAdapter extends ArrayAdapter<AlarmSelection> {

    public AlarmSelectionAdapter(Context context, int id, List<AlarmSelection> alarms) {
        super(context, id, alarms);
    }

    @Override
    public @NonNull
    View getView(int position, @Nullable View convertView,
                 @NonNull ViewGroup parent) {
        final Context context = getContext();
        View row = convertView;
        if (row == null) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            row = inflater.inflate(R.layout.alarm_row, parent, false);

            final int alarmRowMarginBottom = Utils.toPixel(Utils.isTablet(context) ? 64 : 8, context);
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, alarmRowMarginBottom);
            row.setLayoutParams(params);
        }

        final AlarmSelection selection = getItem(position);
        assert selection != null;
        final Alarm alarm = selection.getAlarm();

        final TextTime alarmTime = row.findViewById(R.id.digital_clock);
        alarmTime.setTime(alarm.hour, alarm.minutes);

        final TextView alarmLabel = row.findViewById(R.id.label);
        alarmLabel.setText(alarm.label);

        // find days when alarm is firing
        final TextView daysOfWeekView = row.findViewById(R.id.daysOfWeek);

        if (alarm.daysOfWeek.isRepeating()) {
            final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
            daysOfWeekView.setText(alarm.daysOfWeek.toString(context, weekdayOrder));
        } else {
            if (Alarm.isTomorrow(alarm, Calendar.getInstance()) && !alarm.isDateSpecified()) {
                daysOfWeekView.setText(context.getString(R.string.alarm_tomorrow));
            } else if (alarm.isDateSpecified()) {
                if (Alarm.isDateSpecifiedTomorrow(alarm.year, alarm.month, alarm.day)) {
                    daysOfWeekView.setText(context.getString(R.string.alarm_tomorrow));
                } else {
                    int year = alarm.year;
                    int month = alarm.month;
                    int dayOfMonth = alarm.day;
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year, month, dayOfMonth);
                    String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMMMd");
                    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                    String formattedDate = dateFormat.format(calendar.getTime());
                    daysOfWeekView.setText(context.getString(R.string.alarm_scheduled_for, formattedDate));
                }
            } else {
                daysOfWeekView.setText(context.getString(R.string.alarm_today));
            }
        }

        return row;
    }
}
