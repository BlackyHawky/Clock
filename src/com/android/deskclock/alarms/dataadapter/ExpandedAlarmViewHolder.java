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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.alarms.utils.DayOrderUtils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.uidata.UiDataModel;

import java.util.HashSet;

import static android.content.Context.VIBRATOR_SERVICE;

/**
 * A ViewHolder containing views for an alarm item in expanded stated.
 */
public final class ExpandedAlarmViewHolder extends AlarmItemViewHolder {

    public static final int VIEW_TYPE = R.layout.alarm_time_expanded;

    public final CheckBox repeat;
    public final TextView editLabel;
    public final LinearLayout repeatDays;
    public final CompoundButton[] dayButtons = new CompoundButton[7];
    public final CheckBox vibrate;
    public final TextView ringtone;
    public final ImageButton delete;

    private final boolean mHasVibrator;
    private final int[] mDayOrder;

    public ExpandedAlarmViewHolder(View itemView, boolean hasVibrator) {
        super(itemView);

        final Context context = itemView.getContext();
        mHasVibrator = hasVibrator;
        mDayOrder = DayOrderUtils.getDayOrder(context);
        final Resources.Theme theme = context.getTheme();
        int[] attrs = new int[] { android.R.attr.selectableItemBackground };

        final TypedArray typedArray = theme.obtainStyledAttributes(attrs);
        final LayerDrawable background = new LayerDrawable(new Drawable[] {
                ContextCompat.getDrawable(context, R.drawable.alarm_background_expanded),
                typedArray.getDrawable(0) });
        itemView.setBackground(background);
        typedArray.recycle();

        final int firstDay = Utils.getZeroIndexedFirstDayOfWeek(context);

        delete = (ImageButton) itemView.findViewById(R.id.delete);

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

        // Collapse handler
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getItemHolder().collapse();
            }
        });
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getItemHolder().collapse();
            }
        });
        // Edit time handler
        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAlarmTimeClickHandler().onClockClicked(getItemHolder().item);
            }
        });
        // Edit label handler
        editLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAlarmTimeClickHandler().onEditLabelClicked(getItemHolder().item);
            }
        });
        // Vibrator checkbox handler
        vibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAlarmTimeClickHandler().setAlarmVibrationEnabled(getItemHolder().item,
                        ((CheckBox) v).isChecked());
            }
        });
        // Ringtone editor handler
        ringtone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAlarmTimeClickHandler().onRingtoneClicked(getItemHolder().item);
            }
        });
        // Delete alarm handler
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAlarmTimeClickHandler().onDeleteClicked(getItemHolder().item);
                v.announceForAccessibility(context.getString(R.string.alarm_deleted));
            }
        });
        // Repeat checkbox handler
        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean checked = ((CheckBox) view).isChecked();
                getAlarmTimeClickHandler().setAlarmRepeatEnabled(getItemHolder().item, checked);
            }
        });
        // Day buttons handler
        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; i++) {
            final int buttonIndex = i;
            dayButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean isChecked = ((CompoundButton) view).isChecked();
                    getAlarmTimeClickHandler().setDayOfWeekEnabled(getItemHolder().item,
                            isChecked, buttonIndex);
                }
            });
        }
    }

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        super.onBindItemView(itemHolder);
        final Alarm alarm = itemHolder.item;
        final AlarmInstance alarmInstance = itemHolder.getAlarmInstance();
        final Context context = itemView.getContext();
        bindEditLabel(alarm);
        bindDaysOfWeekButtons(alarm);
        bindVibrator(alarm);
        bindRingtone(context, alarm);
        bindPreemptiveDismissButton(context, alarm, alarmInstance);
    }

    private void bindRingtone(Context context, Alarm alarm) {
        final String title = DataModel.getDataModel().getAlarmRingtoneTitle(alarm.alert);
        ringtone.setText(title);

        final String description = context.getString(R.string.ringtone_description);
        ringtone.setContentDescription(description + " " + title);

        final boolean silent = Utils.RINGTONE_SILENT.equals(alarm.alert);
        final int startResId = silent ? R.drawable.ic_ringtone_silent : R.drawable.ic_ringtone;
        final Drawable startDrawable = Utils.getVectorDrawable(context, startResId);
        ringtone.setCompoundDrawablesWithIntrinsicBounds(startDrawable, null, null, null);
    }

    private void bindDaysOfWeekButtons(Alarm alarm) {
        HashSet<Integer> setDays = alarm.daysOfWeek.getSetDays();
        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; i++) {
            final CompoundButton dayButton = dayButtons[i];
            if (setDays.contains(mDayOrder[i])) {
                dayButton.setChecked(true);
                dayButton.setTextColor(UiDataModel.getUiDataModel().getWindowBackgroundColor());
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(Color.WHITE);
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

    private AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return getItemHolder().getAlarmTimeClickHandler();
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {

        private final LayoutInflater mLayoutInflator;
        private final boolean mHasVibrator;

        public Factory(Context context, LayoutInflater layoutInflater) {
            mLayoutInflator = layoutInflater;
            mHasVibrator = ((Vibrator) context.getSystemService(VIBRATOR_SERVICE)).hasVibrator();
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            return new ExpandedAlarmViewHolder(mLayoutInflator.inflate(
                    viewType, parent, false /* attachToRoot */), mHasVibrator);
        }
    }
}