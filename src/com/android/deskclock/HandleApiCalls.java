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
import static android.provider.AlarmClock.EXTRA_LENGTH;
import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static android.provider.AlarmClock.EXTRA_MINUTES;
import static android.provider.AlarmClock.EXTRA_SKIP_UI;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.timer.TimerFragment;
import com.android.deskclock.timer.TimerObj;
import com.android.deskclock.timer.Timers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HandleApiCalls extends Activity {

    public static final long TIMER_MIN_LENGTH = 1000;
    public static final long TIMER_MAX_LENGTH = 24 * 60 * 60 * 1000;

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
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // If no length is supplied , show the timer setup view
        if (!intent.hasExtra(EXTRA_LENGTH)) {
            startActivity(new Intent(this, DeskClock.class)
                  .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX)
                  .putExtra(TimerFragment.GOTO_SETUP_VIEW, true));
            return;
        }

        final long length = 1000l * intent.getIntExtra(EXTRA_LENGTH, 0);
        if (length < TIMER_MIN_LENGTH || length > TIMER_MAX_LENGTH) {
            Log.i("Invalid timer length requested: " + length);
            return;
        }
        String label = intent.getStringExtra(EXTRA_MESSAGE);
        if (label == null) {
            label = "";
        }

        TimerObj timer = null;
        // Find an existing matching time
        final ArrayList<TimerObj> timers = new ArrayList<TimerObj>();
        TimerObj.getTimersFromSharedPrefs(prefs, timers);
        for (TimerObj t : timers) {
            if (t.mSetupLength == length && (TextUtils.equals(label, t.mLabel))
                    && t.mState == TimerObj.STATE_RESTART) {
                timer = t;
                break;
            }
        }

        boolean skipUi = intent.getBooleanExtra(EXTRA_SKIP_UI, false);
        if (timer == null) {
            // Use a new timer
            timer = new TimerObj(length, label);
            // Timers set without presenting UI to the user will be deleted after use
            timer.mDeleteAfterUse = skipUi;
        }

        timer.mState = TimerObj.STATE_RUNNING;
        timer.mStartTime = Utils.getTimeNow();
        timer.writeToSharedPref(prefs);

        // Tell TimerReceiver that the timer was started
        sendBroadcast(new Intent().setAction(Timers.START_TIMER)
                .putExtra(Timers.TIMER_INTENT_EXTRA, timer.mTimerId));

        if (skipUi) {
            Utils.showInUseNotifications(this);
        } else {
            startActivity(new Intent(this, DeskClock.class)
                    .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX));
        }
    }

    private void enableAlarm(Alarm alarm, boolean enable, boolean skipUi) {
        if (enable) {
            Alarms.enableAlarm(this, (int)alarm.id, true);
            alarm.enabled = true;
        }
        Alarms.setAlarm(this, alarm);
        AlarmUtils.popAlarmSetToast(this, alarm.calculateAlarmTime());
        if (!skipUi) {
            Intent createdAlarm = new Intent(this, DeskClock.class);
            createdAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            createdAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
            createdAlarm.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
            createdAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
            startActivity(createdAlarm);
        }
    }
}
