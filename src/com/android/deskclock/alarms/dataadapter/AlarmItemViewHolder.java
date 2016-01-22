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
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.deskclock.AlarmUtils;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.TextTime;

import java.util.Objects;

/**
 * Abstract ViewHolder for alarm time items.
 */
public abstract class AlarmItemViewHolder extends ItemAdapter.ItemViewHolder<AlarmItemHolder> {

    private static final float CLOCK_ENABLED_ALPHA = 1f;
    private static final float CLOCK_DISABLED_ALPHA = 0.69f;

    public final TextTime clock;
    public final CompoundButton onoff;
    public final View arrow;
    public final View preemptiveDismissContainer;
    public final TextView preemptiveDismissButton;

    private final TextView mAlarmMutedButton;
    private final ContentObserver mVolumeObserver;

    public AlarmItemViewHolder(final View itemView, Handler handler) {
        super(itemView);

        clock = (TextTime) itemView.findViewById(R.id.digital_clock);
        onoff = (CompoundButton) itemView.findViewById(R.id.onoff);
        arrow = itemView.findViewById(R.id.arrow);
        preemptiveDismissContainer = itemView.findViewById(R.id.preemptive_dismiss_container);
        preemptiveDismissButton =
                (TextView) itemView.findViewById(R.id.preemptive_dismiss_button);
        mAlarmMutedButton = (Button) itemView.findViewById(R.id.alarm_muted_button);

        preemptiveDismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlarmInstance alarmInstance = getItemHolder().getAlarmInstance();
                getItemHolder().getAlarmTimeClickHandler().dismissAlarmInstance(alarmInstance);
            }
        });
        onoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                getItemHolder().getAlarmTimeClickHandler().setAlarmEnabled(
                        getItemHolder().item, checked);
            }
        });
        mAlarmMutedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context context = itemView.getContext();
                final Alarm alarm = getItemHolder().item;
                if (hasSilentRingtone(alarm)) {
                    getItemHolder().getAlarmTimeClickHandler().onRingtoneClicked(alarm);
                } else {
                    // Alarm volume muted.
                    final AudioManager audioManager =
                            (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM,
                            AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
                }
            }
        });
        mVolumeObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                if (getItemHolder() != null) {
                    getItemHolder().notifyItemChanged();
                }
            }
        };
    }

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        final Context context = itemView.getContext();
        final Alarm alarm = itemHolder.item;
        bindOnOffSwitch(alarm);
        bindAlarmMutedButton(context, alarm);
        bindClock(context, alarm);
        context.getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mVolumeObserver);
    }

    @Override
    protected void onRecycleItemView() {
        itemView.getContext().getContentResolver().unregisterContentObserver(mVolumeObserver);
    }

    protected void bindOnOffSwitch(Alarm alarm) {
        onoff.setChecked(alarm.enabled);
    }

    protected void bindClock(Context context, Alarm alarm) {
        clock.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        clock.setFormat(context);
        clock.setTime(alarm.hour, alarm.minutes);
    }

    protected boolean bindPreemptiveDismissButton(Context context, Alarm alarm,
            AlarmInstance alarmInstance) {

        final AudioManager audioManager =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final boolean muted = audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0
                || hasSilentRingtone(alarm);
        final boolean canBind = alarm.canPreemptivelyDismiss() && alarmInstance != null && !muted;

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

    protected void bindAlarmMutedButton(Context context, Alarm alarm) {
        final AudioManager audioManager =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (hasSilentRingtone(alarm)) {
            mAlarmMutedButton.setVisibility(View.VISIBLE);
            mAlarmMutedButton.setText(R.string.silent_ringtone);
        } else if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
            mAlarmMutedButton.setVisibility(View.VISIBLE);
            mAlarmMutedButton.setText(R.string.alarm_volume_muted);
        } else {
            mAlarmMutedButton.setVisibility(View.GONE);
        }
    }

    private boolean hasSilentRingtone(Alarm alarm) {
        return Objects.equals(alarm.alert, DataModel.getDataModel().getSilentRingtoneUri());
    }
}
