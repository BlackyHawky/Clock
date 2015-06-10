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

package com.android.deskclock.voice;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.provider.AlarmClock;

import com.android.deskclock.LogUtils;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Returns a list of alarms that are specified by the intent
 * processed by HandleVoiceApiCalls
 * if there are more than 1 matching alarms and the SEARCH_MODE is not ALL
 * we show a picker UI dialog
 */
class FetchMatchingAlarmsAction implements Runnable {

    private final Context mContext;
    private final List<Alarm> mAlarms;
    private final Intent mIntent;
    private final List<Alarm> mMatchingAlarms = new ArrayList<>();

    public FetchMatchingAlarmsAction(Context context, List<Alarm> alarms, Intent intent) {
        mContext = context;
        // only enabled alarms are passed
        mAlarms = alarms;
        mIntent = intent;
    }

    @Override
    public void run() {
        // only allow on background thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Must be called on a background thread");
        }

        final String searchMode = mIntent.getStringExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE);
        // if search mode isn't specified show all alarms in the UI picker
        if (searchMode == null) {
            mMatchingAlarms.addAll(mAlarms);
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        switch (searchMode) {
            case AlarmClock.ALARM_SEARCH_MODE_TIME:
                // at least one of these has to be specified in this search mode.
                final int hour = mIntent.getIntExtra(AlarmClock.EXTRA_HOUR, -1);
                // if minutes weren't specified default to 0
                final int minutes = mIntent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);
                final Boolean isPm = (Boolean) mIntent.getExtras().get(AlarmClock.EXTRA_IS_PM);

                if (hour < 0 || hour > 23 || minutes < 0 || minutes > 59 || (hour > 12 && isPm)) {
                    final String amPm = isPm ? "pm" : "am";
                    LogUtils.e("Invalid time specified: %d:%d %s", hour, minutes, amPm);
                    return;
                }

                final int hour24 =
                        (isPm != null && isPm && hour >= 0 && hour < 12) ? (hour + 12) : hour;

                final List<Alarm> selectedAlarms = new ArrayList<>();
                for (Alarm alarm : mAlarms) {
                    if (alarm.hour == hour24 && alarm.minutes == minutes) {
                        selectedAlarms.add(alarm);
                    }
                }
                if (selectedAlarms.isEmpty()) {
                    LogUtils.i("No alarm at %d:%d", hour24, minutes);
                    return;
                }
                // there might me multiple alarms at the same time
                mMatchingAlarms.addAll(selectedAlarms);
                break;
            case AlarmClock.ALARM_SEARCH_MODE_NEXT:
                final AlarmInstance nextAlarm = AlarmStateManager.getNextFiringAlarm(mContext);
                if (nextAlarm == null) {
                    LogUtils.i("No alarms are scheduled.");
                    return;
                }

                // get time from nextAlarm and see if there are any other alarms matching this time
                final Calendar nextTime = nextAlarm.getAlarmTime();
                final List<Alarm> alarmsFiringAtSameTime = getAlarmsByHourMinutes(
                        nextTime.get(Calendar.HOUR_OF_DAY), nextTime.get(Calendar.MINUTE), cr);
                // there might me multiple alarms firing next
                mMatchingAlarms.addAll(alarmsFiringAtSameTime);
                break;
            case AlarmClock.ALARM_SEARCH_MODE_ALL:
                mMatchingAlarms.addAll(mAlarms);
                break;
        }
    }

    private List<Alarm> getAlarmsByHourMinutes(int hour24, int minutes, ContentResolver cr) {
        // if we want to dismiss we should only add enabled alarms
        final String selection = String.format("%s=? AND %s=? AND %s=?",
                Alarm.HOUR, Alarm.MINUTES, Alarm.ENABLED);
        final String[] args = { String.valueOf(hour24), String.valueOf(minutes), "1" };
        return Alarm.getAlarms(cr, selection, args);
    }

    public List<Alarm> getMatchingAlarms() {
        return mMatchingAlarms;
    }
}