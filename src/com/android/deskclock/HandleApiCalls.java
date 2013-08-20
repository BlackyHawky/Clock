/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.deskclock;

import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static android.provider.AlarmClock.ACTION_SET_TIMER;
import static android.provider.AlarmClock.EXTRA_HOUR;
import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static android.provider.AlarmClock.EXTRA_MINUTES;
import static android.provider.AlarmClock.EXTRA_SKIP_UI;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.DaysOfWeek;

import java.util.Calendar;
import java.util.List;

public class HandleApiCalls extends Activity {
    @Override
    protected void onCreate(Bundle icicle) {
        try {
            super.onCreate(icicle);
            Intent intent = getIntent();
            if (intent != null) {
                if (ACTION_SET_ALARM.equals(intent.getAction())) {
                    handleSetAlarm(intent);
                } else if (ACTION_SET_TIMER.equals(intent.getAction())) {
                    handleSetTimer(intent);
                }
            }
        } finally {
            finish();
        }
    }

    /***
     * Processes the SET_ALARM intent
     * @param intent
     */
    private void handleSetAlarm(Intent intent) {

        // Intent has no time , open the alarm creation UI
        if (!intent.hasExtra(EXTRA_HOUR)) {
            Intent createAlarm = new Intent(this, DeskClock.class);
            createAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            createAlarm.putExtra(Alarms.ALARM_CREATE_NEW, true);
            createAlarm.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);

            startActivity(createAlarm);
            finish();
            return;
        }

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        final int hour = intent.getIntExtra(EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY));
        final int minutes = intent.getIntExtra(EXTRA_MINUTES, calendar.get(Calendar.MINUTE));
        final boolean skipUi = intent.getBooleanExtra(EXTRA_SKIP_UI, false);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        if (message == null) {
            message = "";
        }

        // Check if the alarm already exists and handle it
        ContentResolver cr = getContentResolver();
        List<Alarm> alarms = Alarm.getAlarms(cr,
                Alarm.HOUR + "=" + hour + " AND " +
                Alarm.MINUTES + "=" + minutes + " AND " +
                Alarm.DAYS_OF_WEEK + "=" + DaysOfWeek.NO_DAYS_SET + " AND " +
                Alarm.MESSAGE + "=?",
                new String[] { message });
        if (!alarms.isEmpty()) {
            enableAlarm(alarms.get(0), true, skipUi);
            finish();
            return;
        }

        // Otherwise insert it and handle it
        Alarm alarm = new Alarm(hour, minutes);
        alarm.enabled = true;
        alarm.label = message;

        Uri result = cr.insert(Alarm.CONTENT_URI, Alarm.createContentValues(alarm));
        enableAlarm(Alarm.getAlarm(cr, Alarm.getId(result)), false, skipUi);
        finish();
    }

    private void handleSetTimer(Intent intent) {

    }

    private void enableAlarm(Alarm alarm, boolean enable, boolean skipUi) {
        if (enable) {
            Alarms.enableAlarm(this, alarm.id, true);
            alarm.enabled = true;
        }
        AlarmUtils.popAlarmSetToast(this, alarm.calculateAlarmTime());
        if (skipUi) {
            Alarms.setAlarm(this, alarm);
        } else {
            Intent createAlarm = new Intent(this, DeskClock.class);
            createAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            createAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
            createAlarm.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
            createAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
            startActivity(createAlarm);
        }
    }
}
