/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.alarms.dataadapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.util.Calendar;

/**
 * A ViewHolder containing views for an alarm item in collapsed stated.
 */
public final class CollapsedAlarmViewHolder extends AlarmItemViewHolder {

    public static final int VIEW_TYPE = R.layout.alarm_time_collapsed;

    public final TextView alarmLabel;
    public final TextView daysOfWeek;
    public final TextView upcomingInstanceLabel;
    public final View hairLine;

    public CollapsedAlarmViewHolder(View itemView) {
        super(itemView);
        alarmLabel = (TextView) itemView.findViewById(R.id.label);
        daysOfWeek = (TextView) itemView.findViewById(R.id.days_of_week);
        upcomingInstanceLabel = (TextView) itemView.findViewById(R.id.upcoming_instance_label);
        hairLine = itemView.findViewById(R.id.hairline);

        // Expand handler
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getItemHolder().expand();
            }
        });
        alarmLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getItemHolder().expand();
            }
        });
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getItemHolder().expand();
            }
        });
        // Edit time handler
        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getItemHolder().getAlarmTimeClickHandler().onClockClicked(getItemHolder().item);
                getItemHolder().expand();
            }
        });
    }

    @Override
    protected void onBindItemView(AlarmItemHolder itemHolder) {
        super.onBindItemView(itemHolder);
        final Alarm alarm = itemHolder.item;
        final AlarmInstance alarmInstance = itemHolder.getAlarmInstance();
        final Context context = itemView.getContext();
        bindRepeatText(context, alarm);
        bindReadOnlyLabel(context, alarm);
        bindUpcomingInstance(context, alarm);
        boolean boundPreemptiveDismiss =
                bindPreemptiveDismissButton(context, alarm, alarmInstance);
        hairLine.setVisibility(boundPreemptiveDismiss ? View.GONE : View.VISIBLE);
    }

    private void bindReadOnlyLabel(Context context, Alarm alarm) {
        if (alarm.label != null && alarm.label.length() != 0) {
            alarmLabel.setText(alarm.label);
            alarmLabel.setVisibility(View.VISIBLE);
            alarmLabel.setContentDescription(context.getString(R.string.label_description)
                    + " " + alarm.label);
        } else {
            alarmLabel.setVisibility(View.GONE);
        }
    }

    private void bindRepeatText(Context context, Alarm alarm) {
        final String daysOfWeekText =
                alarm.daysOfWeek.toString(context, Utils.getFirstDayOfWeek(context));
        if (!TextUtils.isEmpty(daysOfWeekText)) {
            daysOfWeek.setText(daysOfWeekText);
            daysOfWeek.setContentDescription(alarm.daysOfWeek.toAccessibilityString(
                    context, Utils.getFirstDayOfWeek(context)));
            daysOfWeek.setVisibility(View.VISIBLE);
        } else {
            daysOfWeek.setVisibility(View.GONE);
        }
    }

    private void bindUpcomingInstance(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            upcomingInstanceLabel.setVisibility(View.GONE);
        } else {
            upcomingInstanceLabel.setVisibility(View.VISIBLE);
            final String labelText = Alarm.isTomorrow(alarm, Calendar.getInstance()) ?
                    context.getString(R.string.alarm_tomorrow) :
                    context.getString(R.string.alarm_today);
            upcomingInstanceLabel.setText(labelText);
        }
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {

        private final LayoutInflater mLayoutInflater;

        public Factory(LayoutInflater layoutInflater) {
            mLayoutInflater = layoutInflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            return new CollapsedAlarmViewHolder(mLayoutInflater.inflate(
                    viewType, parent, false /* attachToRoot */));
        }
    }
}