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

package com.best.deskclock.alarms.dataadapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.ItemAnimator;
import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmTimeClickHandler;
import com.best.deskclock.bedtime.BedtimeFragment;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.widget.TextTime;

import java.util.Calendar;

/**
 * Abstract ViewHolder for alarm time items.
 */
public abstract class AlarmItemViewHolder extends ItemAdapter.ItemViewHolder<AlarmItemHolder>
        implements ItemAnimator.OnAnimateChangeListener {

    public static final float CLOCK_ENABLED_ALPHA = 1f;
    public static final float CLOCK_DISABLED_ALPHA = 0.63f;

    private final TextView editLabel;
    public final TextTime clock;
    public final CompoundButton onOff;
    public final TextView daysOfWeek;
    private final TextView upcomingInstanceLabel;
    public final ImageView arrow;

    public AlarmItemViewHolder(View itemView) {
        super(itemView);

        editLabel = itemView.findViewById(R.id.edit_label);
        clock = itemView.findViewById(R.id.digital_clock);
        onOff = itemView.findViewById(R.id.onoff);
        daysOfWeek = itemView.findViewById(R.id.days_of_week);
        upcomingInstanceLabel = itemView.findViewById(R.id.upcoming_instance_label);
        arrow = itemView.findViewById(R.id.arrow);

        editLabel.setOnClickListener(view -> {
            if (!getItemHolder().item.equals(Alarm.getAlarmByLabel(itemView.getContext().getContentResolver(), BedtimeFragment.BEDLABEL))) {
                getAlarmTimeClickHandler().onEditLabelClicked(getItemHolder().item);
            }
        });

        onOff.setOnCheckedChangeListener((compoundButton, checked) ->
                getItemHolder().getAlarmTimeClickHandler().setAlarmEnabled(getItemHolder().item, checked));
    }

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        final Alarm alarm = itemHolder.item;
        final Context context = itemView.getContext();
        bindEditLabel(context, alarm);
        bindOnOffSwitch(alarm);
        bindClock(alarm);
        bindRepeatText(context, alarm);
        bindUpcomingInstance(context, alarm);
        itemView.setContentDescription(clock.getText() + " " + alarm.getLabelOrDefault(context));
    }

    private void bindEditLabel(Context context, Alarm alarm) {
        if (alarm.label.length() == 0) {
            editLabel.setText(context.getString(R.string.add_label));
            editLabel.setTypeface(Typeface.DEFAULT);
            editLabel.setAlpha(CLOCK_DISABLED_ALPHA);
        } else {
            editLabel.setText(alarm.equals(Alarm.getAlarmByLabel(context.getContentResolver(), BedtimeFragment.BEDLABEL))
                    ? context.getString(R.string.wakeup_alarm_label_visible)
                    : alarm.label);
            editLabel.setContentDescription(alarm.label != null && alarm.label.length() > 0
                    ? context.getString(R.string.label_description) + " " + alarm.label
                    : context.getString(R.string.no_label_specified));
            editLabel.setTypeface(Typeface.DEFAULT_BOLD);
            editLabel.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        }
    }

    protected void bindOnOffSwitch(Alarm alarm) {
        if (onOff.isChecked() != alarm.enabled) {
            onOff.setChecked(alarm.enabled);
        }
    }

    protected void bindClock(Alarm alarm) {
        clock.setTime(alarm.hour, alarm.minutes);
        clock.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        clock.setTypeface(alarm.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }

    private void bindRepeatText(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            daysOfWeek.setVisibility(View.VISIBLE);

            final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
            final String daysOfWeekText = alarm.enabled
                    ? alarm.daysOfWeek.toString(context, weekdayOrder)
                    : context.getString(R.string.alarm_inactive);
            daysOfWeek.setText(daysOfWeekText);
            daysOfWeek.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);

            final String string = alarm.daysOfWeek.toAccessibilityString(context, weekdayOrder);
            daysOfWeek.setContentDescription(string);
        } else {
            daysOfWeek.setVisibility(View.GONE);
        }
    }

    private void bindUpcomingInstance(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            upcomingInstanceLabel.setVisibility(View.GONE);
        } else {
            upcomingInstanceLabel.setVisibility(View.VISIBLE);
            final String labelText;
            if (alarm.enabled) {
                labelText = Alarm.isTomorrow(alarm, Calendar.getInstance())
                        ? context.getString(R.string.alarm_tomorrow)
                        : context.getString(R.string.alarm_today);
            } else {
                labelText = context.getString(R.string.alarm_inactive);
            }
            upcomingInstanceLabel.setText(labelText);
            upcomingInstanceLabel.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        }
    }

    private AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return getItemHolder().getAlarmTimeClickHandler();
    }

}
