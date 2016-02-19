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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.alarms.utils.DayOrderUtils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;

import java.util.HashSet;

/**
 * A ViewHolder containing views for an alarm item in expanded stated.
 */
public final class ExpandedAlarmViewHolder extends AlarmTimeViewHolder {

    public final CheckBox repeat;
    public final TextView editLabel;
    public final LinearLayout repeatDays;
    public final CompoundButton[] dayButtons = new CompoundButton[7];
    public final CheckBox vibrate;
    public final TextView ringtone;
    public final Button delete;
    public final View preemptiveDismissContainer;
    public final TextView preemptiveDismissButton;

    private final boolean mHasVibrator;
    private final int[] mDayOrder;

    public ExpandedAlarmViewHolder(View itemView,
            final boolean hasVibrator,
            final AlarmTimeClickHandler alarmTimeClickHandler,
            final AlarmTimeAdapter alarmTimeAdapter) {
        super(itemView, alarmTimeClickHandler);
        final Context context = itemView.getContext();
        mHasVibrator = hasVibrator;
        mDayOrder = DayOrderUtils.getDayOrder(context);
        final Resources.Theme theme = context.getTheme();
        int[] attrs = new int[] { android.R.attr.selectableItemBackground };

        final TypedArray typedArray = theme.obtainStyledAttributes(attrs);
        final LayerDrawable background = new LayerDrawable(new Drawable[] {
                context.getResources().getDrawable(R.drawable.alarm_background_expanded),
                typedArray.getDrawable(0) });
        itemView.setBackground(background);
        typedArray.recycle();

        final int firstDay = Utils.getZeroIndexedFirstDayOfWeek(context);

        delete = (Button) itemView.findViewById(R.id.delete);

        repeat = (CheckBox) itemView.findViewById(R.id.repeat_onoff);
        vibrate = (CheckBox) itemView.findViewById(R.id.vibrate_onoff);
        ringtone = (TextView) itemView.findViewById(R.id.choose_ringtone);
        editLabel = (TextView) itemView.findViewById(R.id.edit_label);
        repeatDays = (LinearLayout) itemView.findViewById(R.id.repeat_days);

        // Build button for each day.
        LayoutInflater mInflater = LayoutInflater.from(context);
        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; i++) {
            final CompoundButton dayButton = (CompoundButton) mInflater.inflate(
                    R.layout.day_button, repeatDays, false /* attachToRoot */);
            dayButton.setText(Utils.getShortWeekday(i, firstDay));
            dayButton.setContentDescription(Utils.getLongWeekday(i, firstDay));
            repeatDays.addView(dayButton);
            dayButtons[i] = dayButton;
        }

        preemptiveDismissContainer = itemView.findViewById(R.id.preemptive_dismiss_container);
        preemptiveDismissButton =
                (TextView) itemView.findViewById(R.id.preemptive_dismiss_button);

        // Collapse handler
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmTimeAdapter.collapse(getAdapterPosition());
            }
        });
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmTimeAdapter.collapse(getAdapterPosition());
            }
        });
        // Edit time handler
        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmTimeClickHandler.onClockClicked(mAlarm);
            }
        });
        // Edit label handler
        editLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alarmTimeClickHandler.onEditLabelClicked(mAlarm);
            }
        });
        // Vibrator checkbox handler
        vibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmTimeClickHandler.setAlarmVibrationEnabled(mAlarm, ((CheckBox) v).isChecked());
            }
        });
        // Ringtone editor handler
        ringtone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alarmTimeClickHandler.onRingtoneClicked(mAlarm);
            }
        });
        // Delete alarm handler
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmTimeClickHandler.onDeleteClicked(mAlarm);
                v.announceForAccessibility(context.getString(R.string.alarm_deleted));
            }
        });
        // Repeat checkbox handler
        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean checked = ((CheckBox) view).isChecked();
                alarmTimeClickHandler.setAlarmRepeatEnabled(mAlarm, checked);
            }
        });
        // Day buttons handler
        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; i++) {
            final int buttonIndex = i;
            dayButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean isChecked = ((CompoundButton) view).isChecked();
                    alarmTimeClickHandler.setDayOfWeekEnabled(mAlarm, isChecked, buttonIndex);
                }
            });
        }
    }

    @Override
    public void bindAlarm(Context context, Alarm alarm, AlarmInstance alarmInstance) {
        setData(alarm, alarmInstance);
        bindOnOffSwitch(context, alarm);
        bindClock(context, alarm);
        bindEditLabel(alarm);
        bindDaysOfWeekButtons(alarm);
        bindVibrator(alarm);
        bindRingtoneTitle(context, alarm);
        bindPreemptiveDismissButton(context, alarm, alarmInstance);
    }

    private void bindRingtoneTitle(Context context, Alarm alarm) {
        final String title = DataModel.getDataModel().getAlarmRingtoneTitle(alarm.alert);
        final String description = context.getString(R.string.ringtone_description);

        ringtone.setText(title);
        ringtone.setContentDescription(description + " " + title);
    }

    private void bindDaysOfWeekButtons(Alarm alarm) {
        HashSet<Integer> setDays = alarm.daysOfWeek.getSetDays();
        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; i++) {
            final CompoundButton dayButton = dayButtons[i];
            if (setDays.contains(mDayOrder[i])) {
                dayButton.setChecked(true);
                dayButton.setTextColor(Utils.getCurrentHourColor());
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(itemView.getContext().getResources().getColor(R.color
                        .white));
            }
        }
        if (alarm.daysOfWeek.isRepeating()) {
            repeat.setChecked(true);
            repeatDays.setVisibility(View.VISIBLE);
        } else {
            repeat.setChecked(false);
            repeatDays.setVisibility(View.GONE);
        }
    }

    private void bindEditLabel(Alarm alarm) {
        if (alarm.label != null && alarm.label.length() > 0) {
            editLabel.setText(alarm.label);
        } else {
            editLabel.setText(R.string.label);
        }
    }

    private void bindVibrator(Alarm alarm) {
        if (!mHasVibrator) {
            vibrate.setVisibility(View.INVISIBLE);
        } else {
            vibrate.setVisibility(View.VISIBLE);
            vibrate.setChecked(alarm.vibrate);
        }
    }
}
