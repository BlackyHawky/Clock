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
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.deskclock.AlarmUtils;
import com.android.deskclock.R;
import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.TextTime;

/**
 * Abstract ViewHolder for alarm time items.
 */
public abstract class AlarmTimeViewHolder extends RecyclerView.ViewHolder {

    private static final float CLOCK_ENABLED_ALPHA = 1f;
    private static final float CLOCK_DISABLED_ALPHA = 0.69f;

    public final TextTime clock;
    public final CompoundButton onoff;
    public final View arrow;
    public final View preemptiveDismissContainer;
    public final TextView preemptiveDismissButton;

    protected Alarm mAlarm;
    protected AlarmInstance mAlarmInstance;

    private final AlarmTimeClickHandler mAlarmTimeClickHandler;

    public AlarmTimeViewHolder(View itemView, AlarmTimeClickHandler alarmTimeClickHandler) {
        super(itemView);
        mAlarmTimeClickHandler = alarmTimeClickHandler;
        clock = (TextTime) itemView.findViewById(R.id.digital_clock);
        onoff = (CompoundButton) itemView.findViewById(R.id.onoff);
        arrow = itemView.findViewById(R.id.arrow);
        preemptiveDismissContainer = itemView.findViewById(R.id.preemptive_dismiss_container);
        preemptiveDismissButton =
                (TextView) itemView.findViewById(R.id.preemptive_dismiss_button);
        preemptiveDismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlarmTimeClickHandler.dismissAlarmInstance(mAlarmInstance);
            }
        });
        onoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                mAlarmTimeClickHandler.setAlarmEnabled(mAlarm, checked);
            }
        });
    }

    public void setData(Alarm alarm, AlarmInstance alarmInstance) {
        mAlarmInstance = alarmInstance;
        mAlarm = alarm;
    }

    public void clearData() {
        mAlarmInstance = null;
        mAlarm = null;
    }

    /**
     * Binds the view with {@link Alarm} data.
     */
    public abstract void bindAlarm(Context context, Alarm alarm, AlarmInstance alarmInstance);

    protected void bindOnOffSwitch(Context context, Alarm alarm) {
        onoff.setChecked(alarm.enabled);
        ((SwitchCompat) onoff).setTextOn(context.getString(R.string.on_switch));
        ((SwitchCompat) onoff).setTextOff(context.getString(R.string.off_switch));
    }

    protected void bindClock(Context context, Alarm alarm) {
        clock.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        clock.setFormat(context);
        clock.setTime(alarm.hour, alarm.minutes);
    }

    protected boolean bindPreemptiveDismissButton(Context context, Alarm alarm,
            AlarmInstance alarmInstance) {
        boolean canBind = alarm.canPreemptivelyDismiss() && alarmInstance != null;
        if (canBind) {
            preemptiveDismissContainer.setVisibility(View.VISIBLE);
            final String dismissText = alarm.instanceState == AlarmInstance.SNOOZE_STATE
                    ? context.getString(R.string.alarm_alert_snooze_until,
                    AlarmUtils.getAlarmText(context, alarmInstance, false))
                    : context.getString(R.string.alarm_alert_dismiss_now_text);
            preemptiveDismissButton.setText(dismissText);
        } else {
            preemptiveDismissContainer.setVisibility(View.GONE);
        }
        return canBind;
    }
}
