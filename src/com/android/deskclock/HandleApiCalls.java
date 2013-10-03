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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.timer.TimerFragment;
import com.android.deskclock.timer.TimerObj;
import com.android.deskclock.timer.Timers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static android.provider.AlarmClock.ACTION_SET_TIMER;
import static android.provider.AlarmClock.ACTION_SHOW_ALARMS;
import static android.provider.AlarmClock.EXTRA_DAYS;
import static android.provider.AlarmClock.EXTRA_HOUR;
import static android.provider.AlarmClock.EXTRA_LENGTH;
import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static android.provider.AlarmClock.EXTRA_MINUTES;
import static android.provider.AlarmClock.EXTRA_RINGTONE;
import static android.provider.AlarmClock.EXTRA_SKIP_UI;
import static android.provider.AlarmClock.EXTRA_VIBRATE;
import static android.provider.AlarmClock.VALUE_RINGTONE_SILENT;

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
                } else if (ACTION_SHOW_ALARMS.equals(intent.getAction())) {
                    handleShowAlarms();
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
        // If not provided or invalid, show UI
        final int hour = intent.getIntExtra(EXTRA_HOUR, -1);

        // If not provided, use zero. If it is provided, make sure it's valid, otherwise, show UI
        final int minutes;
        if (intent.hasExtra(EXTRA_MINUTES)) {
            minutes = intent.getIntExtra(EXTRA_MINUTES, -1);
        } else {
            minutes = 0;
        }
        if (hour < 0 || hour > 23 || minutes < 0 || minutes > 59) {
            // Intent has no time or an invalid time, open the alarm creation UI
            Intent createAlarm = Alarm.createIntent(this, DeskClock.class, Alarm.INVALID_ID);
            createAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            createAlarm.putExtra(AlarmClockFragment.ALARM_CREATE_NEW_INTENT_EXTRA, true);
            createAlarm.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
            startActivity(createAlarm);
            finish();
            return;
        }

        final boolean skipUi = intent.getBooleanExtra(EXTRA_SKIP_UI, false);

        final StringBuilder selection = new StringBuilder();
        final List<String> args = new ArrayList<String>();
        setSelectionFromIntent(intent, hour, minutes, selection, args);

        // Check if the alarm already exists and handle it
        final ContentResolver cr = getContentResolver();
        final List<Alarm> alarms = Alarm.getAlarms(cr,
                selection.toString(),
                args.toArray(new String[args.size()]));
        if (!alarms.isEmpty()) {
            Alarm alarm = alarms.get(0);
            alarm.enabled = true;
            Alarm.updateAlarm(cr, alarm);

            // Delete all old instances and create a new one with updated values
            AlarmStateManager.deleteAllInstances(this, alarm.id);
            setupInstance(alarm.createInstanceAfter(Calendar.getInstance()), skipUi);
            finish();
            return;
        }

        // Otherwise insert it and handle it
        final String message = getMessageFromIntent(intent);
        final DaysOfWeek daysOfWeek = getDaysFromIntent(intent);
        final boolean vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, false);
        final String alert = intent.getStringExtra(EXTRA_RINGTONE);

        Alarm alarm = new Alarm(hour, minutes);
        alarm.enabled = true;
        alarm.label = message;
        alarm.daysOfWeek = daysOfWeek;
        alarm.vibrate = vibrate;

        if (alert == null) {
            alarm.alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else if (VALUE_RINGTONE_SILENT.equals(alert) || alert.isEmpty()) {
            alarm.alert = Alarm.NO_RINGTONE_URI;
        } else {
            alarm.alert = Uri.parse(alert);
        }
        alarm.deleteAfterUse = !daysOfWeek.isRepeating() && skipUi;

        alarm = Alarm.addAlarm(cr, alarm);
        setupInstance(alarm.createInstanceAfter(Calendar.getInstance()), skipUi);
        finish();
    }

    private void handleShowAlarms() {
        startActivity(new Intent(this, DeskClock.class)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX));
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
        String label = getMessageFromIntent(intent);

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

    private void setupInstance(AlarmInstance instance, boolean skipUi) {
        instance = AlarmInstance.addInstance(this.getContentResolver(), instance);
        AlarmStateManager.registerInstance(this, instance, true);
        AlarmUtils.popAlarmSetToast(this, instance.getAlarmTime().getTimeInMillis());
        if (!skipUi) {
            Intent showAlarm = Alarm.createIntent(this, DeskClock.class, instance.mAlarmId);
            showAlarm.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
            showAlarm.putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, instance.mAlarmId);
            showAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(showAlarm);
        }
    }

    private String getMessageFromIntent(Intent intent) {
        final String message = intent.getStringExtra(EXTRA_MESSAGE);
        return message == null ? "" : message;
    }

    private DaysOfWeek getDaysFromIntent(Intent intent) {
        final DaysOfWeek daysOfWeek = new DaysOfWeek(0);
        final ArrayList<Integer> days = intent.getIntegerArrayListExtra(EXTRA_DAYS);
        if (days != null) {
            final int[] daysArray = new int[days.size()];
            for (int i = 0; i < days.size(); i++) {
                daysArray[i] = days.get(i);
            }
            daysOfWeek.setDaysOfWeek(true, daysArray);
        } else {
            // API says to use an ArrayList<Integer> but we allow the user to use a int[] too.
            final int[] daysArray = intent.getIntArrayExtra(EXTRA_DAYS);
            if (daysArray != null) {
                daysOfWeek.setDaysOfWeek(true, daysArray);
            }
        }
        return daysOfWeek;
    }

    private void setSelectionFromIntent(
            Intent intent,
            int hour,
            int minutes,
            StringBuilder selection,
            List<String> args) {
        selection.append(Alarm.HOUR).append("=?");
        args.add(String.valueOf(hour));
        selection.append(" AND ").append(Alarm.MINUTES).append("=?");
        args.add(String.valueOf(minutes));

        if (intent.hasExtra(EXTRA_MESSAGE)) {
            selection.append(" AND ").append(Alarm.LABEL).append("=?");
            args.add(getMessageFromIntent(intent));
        }

        // Days is treated differently that other fields because if days is not specified, it
        // explicitly means "not recurring".
        selection.append(" AND ").append(Alarm.DAYS_OF_WEEK).append("=?");
        args.add(String.valueOf(intent.hasExtra(EXTRA_DAYS)
                ? getDaysFromIntent(intent).getBitSet() : DaysOfWeek.NO_DAYS_SET));

        if (intent.hasExtra(EXTRA_VIBRATE)) {
            selection.append(" AND ").append(Alarm.VIBRATE).append("=?");
            args.add(intent.getBooleanExtra(EXTRA_VIBRATE, false) ? "1" : "0");
        }

        if (intent.hasExtra(EXTRA_RINGTONE)) {
            selection.append(" AND ").append(Alarm.RINGTONE).append("=?");

            String ringTone = intent.getStringExtra(EXTRA_RINGTONE);
            if (ringTone == null) {
                // If the intent explicitly specified a NULL ringtone, treat it as the default
                // ringtone.
                ringTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
            } else if (VALUE_RINGTONE_SILENT.equals(ringTone) || ringTone.isEmpty()) {
                    ringTone = Alarm.NO_RINGTONE;
            }
            args.add(ringTone);
        }
    }
}
