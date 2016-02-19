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
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.AlarmClock;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.timer.TimerFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * This activity is never visible. It processes all public intents defined by {@link AlarmClock}
 * that apply to alarms and timers. Its definition in AndroidManifest.xml requires callers to hold
 * the com.android.alarm.permission.SET_ALARM permission to complete the requested action.
 */
public class HandleApiCalls extends Activity {

    private Context mAppContext;

    @Override
    protected void onCreate(Bundle icicle) {
        try {
            super.onCreate(icicle);
            mAppContext = getApplicationContext();
            final Intent intent = getIntent();
            final String action = intent == null ? null : intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case AlarmClock.ACTION_SET_ALARM:
                    handleSetAlarm(intent);
                    break;
                case AlarmClock.ACTION_SHOW_ALARMS:
                    handleShowAlarms();
                    break;
                case AlarmClock.ACTION_SET_TIMER:
                    handleSetTimer(intent);
                    break;
                case AlarmClock.ACTION_DISMISS_ALARM:
                    handleDismissAlarm(intent.getAction());
                    break;
                case AlarmClock.ACTION_SNOOZE_ALARM:
                    handleSnoozeAlarm();
            }
        } finally {
            finish();
        }
    }

    private void handleDismissAlarm(final String action) {
        // Opens the UI for Alarms
        final Intent alarmIntent =
                Alarm.createIntent(mAppContext, DeskClock.class, Alarm.INVALID_ID)
                        .setAction(action)
                        .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
        startActivity(alarmIntent);

        final Intent intent = getIntent();

        new DismissAlarmAsync(mAppContext, intent, this).execute();
    }

    public static void dismissAlarm(Alarm alarm, Context context, Activity activity) {
        // only allow on background thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("dismissAlarm must be called on a " +
                    "background thread");
        }

        final AlarmInstance alarmInstance = AlarmInstance.getNextUpcomingInstanceByAlarmId(
                context.getContentResolver(), alarm.id);
        if (alarmInstance == null) {
            final String reason = context.getString(R.string.no_alarm_scheduled_for_this_time);
            Voice.notifyFailure(activity, reason);
            LogUtils.i(reason);
            return;
        }

        final String time = DateFormat.getTimeFormat(context).format(
                alarmInstance.getAlarmTime().getTime());
        if (Utils.isAlarmWithin24Hours(alarmInstance)) {
            AlarmStateManager.setPreDismissState(context, alarmInstance);
            final String reason = context.getString(R.string.alarm_is_dismissed, time);
            LogUtils.i(reason);
            Voice.notifySuccess(activity, reason);
            Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_intent);
        } else {
            final String reason = context.getString(
                    R.string.alarm_cant_be_dismissed_still_more_than_24_hours_away, time);
            Voice.notifyFailure(activity, reason);
            LogUtils.i(reason);
        }
    }

    private static class DismissAlarmAsync extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        private final Intent mIntent;
        private final Activity mActivity;

        public DismissAlarmAsync(Context context, Intent intent, Activity activity) {
            mContext = context;
            mIntent = intent;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... parameters) {
            final List<Alarm> alarms = getEnabledAlarms(mContext);
            if (alarms.isEmpty()) {
                final String reason = mContext.getString(R.string.no_scheduled_alarms);
                LogUtils.i(reason);
                Voice.notifyFailure(mActivity, reason);
                return null;
            }

            // remove Alarms in MISSED, DISMISSED, and PREDISMISSED states
            for (Iterator<Alarm> i = alarms.iterator(); i.hasNext();) {
                final AlarmInstance alarmInstance = AlarmInstance.getNextUpcomingInstanceByAlarmId(
                        mContext.getContentResolver(), i.next().id);
                if (alarmInstance == null ||
                        alarmInstance.mAlarmState > AlarmInstance.FIRED_STATE) {
                    i.remove();
                }
            }

            final String searchMode = mIntent.getStringExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE);
            if (searchMode == null && alarms.size() > 1) {
                // shows the UI where user picks which alarm they want to DISMISS
                final Intent pickSelectionIntent = new Intent(mContext,
                        AlarmSelectionActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(AlarmSelectionActivity.EXTRA_ALARMS,
                                alarms.toArray(new Parcelable[alarms.size()]));
                mContext.startActivity(pickSelectionIntent);
                Voice.notifySuccess(mActivity, mContext.getString(R.string.pick_alarm_to_dismiss));
                return null;
            }

            // fetch the alarms that are specified by the intent
            final FetchMatchingAlarmsAction fmaa =
                    new FetchMatchingAlarmsAction(mContext, alarms, mIntent, mActivity);
            fmaa.run();
            final List<Alarm> matchingAlarms = fmaa.getMatchingAlarms();

            // If there are multiple matching alarms and it wasn't expected
            // disambiguate what the user meant
            if (!AlarmClock.ALARM_SEARCH_MODE_ALL.equals(searchMode) && matchingAlarms.size() > 1) {
              final Intent pickSelectionIntent = new Intent(mContext, AlarmSelectionActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(AlarmSelectionActivity.EXTRA_ALARMS,
                                matchingAlarms.toArray(new Parcelable[matchingAlarms.size()]));
                mContext.startActivity(pickSelectionIntent);
                Voice.notifySuccess(mActivity, mContext.getString(R.string.pick_alarm_to_dismiss));
                return null;
            }

            // Apply the action to the matching alarms
            for (Alarm alarm : matchingAlarms) {
                dismissAlarm(alarm, mContext, mActivity);
                LogUtils.i("Alarm %s is dismissed", alarm);
            }
            return null;
        }

        private static List<Alarm> getEnabledAlarms(Context context) {
            final String selection = String.format("%s=?", Alarm.ENABLED);
            final String[] args = { "1" };
            return Alarm.getAlarms(context.getContentResolver(), selection, args);
        }
    }

    private void handleSnoozeAlarm() {
        new SnoozeAlarmAsync(mAppContext, this).execute();
    }

    private static class SnoozeAlarmAsync extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        private final Activity mActivity;

        public SnoozeAlarmAsync(Context context, Activity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... parameters) {
            final List<AlarmInstance> alarmInstances = AlarmInstance.getInstancesByState(
                    mContext.getContentResolver(), AlarmInstance.FIRED_STATE);
            if (alarmInstances.isEmpty()) {
                final String reason = mContext.getString(R.string.no_firing_alarms);
                LogUtils.i(reason);
                Voice.notifyFailure(mActivity, reason);
                return null;
            }

            for (AlarmInstance firingAlarmInstance : alarmInstances) {
                snoozeAlarm(firingAlarmInstance, mContext, mActivity);
            }
            return null;
        }
    }

    static void snoozeAlarm(AlarmInstance alarmInstance, Context context, Activity activity) {
        // only allow on background thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("snoozeAlarm must be called on a " +
                    "background thread");
        }
        final String time = DateFormat.getTimeFormat(context).format(
                alarmInstance.getAlarmTime().getTime());
        final String reason = context.getString(R.string.alarm_is_snoozed, time);
        LogUtils.i(reason);
        Voice.notifySuccess(activity, reason);
        AlarmStateManager.setSnoozeState(context, alarmInstance, true);
        LogUtils.i("Snooze %d:%d", alarmInstance.mHour, alarmInstance.mMinute);
        Events.sendAlarmEvent(R.string.action_snooze, R.string.label_intent);
    }

    /***
     * Processes the SET_ALARM intent
     * @param intent Intent passed to the app
     */
    private void handleSetAlarm(Intent intent) {
        // If not provided or invalid, show UI
        final int hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1);

        // If not provided, use zero. If it is provided, make sure it's valid, otherwise, show UI
        final int minutes;
        if (intent.hasExtra(AlarmClock.EXTRA_MINUTES)) {
            minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1);
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
            Voice.notifyFailure(this, getString(R.string.invalid_time, hour, minutes, " "));
            LogUtils.i("HandleApiCalls no/invalid time; opening UI");
            return;
        }

        Events.sendAlarmEvent(R.string.action_create, R.string.label_intent);
        final boolean skipUi = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);

        final StringBuilder selection = new StringBuilder();
        final List<String> args = new ArrayList<>();
        setSelectionFromIntent(intent, hour, minutes, selection, args);

        final String message = getMessageFromIntent(intent);
        final DaysOfWeek daysOfWeek = getDaysFromIntent(intent);
        final boolean vibrate = intent.getBooleanExtra(AlarmClock.EXTRA_VIBRATE, false);
        final String alert = intent.getStringExtra(AlarmClock.EXTRA_RINGTONE);

        Alarm alarm = new Alarm(hour, minutes);
        alarm.enabled = true;
        alarm.label = message;
        alarm.daysOfWeek = daysOfWeek;
        alarm.vibrate = vibrate;

        if (alert != null) {
            if (AlarmClock.VALUE_RINGTONE_SILENT.equals(alert) || alert.isEmpty()) {
                alarm.alert = Alarm.NO_RINGTONE_URI;
            } else {
                alarm.alert = Uri.parse(alert);
            }
        }
        alarm.deleteAfterUse = !daysOfWeek.isRepeating() && skipUi;

        final ContentResolver cr = getContentResolver();
        alarm = Alarm.addAlarm(cr, alarm);
        final AlarmInstance alarmInstance = alarm.createInstanceAfter(Calendar.getInstance());
        setupInstance(alarmInstance, skipUi);
        final String time = DateFormat.getTimeFormat(mAppContext).format(
                alarmInstance.getAlarmTime().getTime());
        Voice.notifySuccess(this, getString(R.string.alarm_is_set, time));
        LogUtils.i("HandleApiCalls set up alarm: %s", alarm);
    }

    private void handleShowAlarms() {
        startActivity(new Intent(this, DeskClock.class)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX));
        Events.sendAlarmEvent(R.string.action_show, R.string.label_intent);
        LogUtils.i("HandleApiCalls show alarms");
    }

    private void handleSetTimer(Intent intent) {
        // If no length is supplied, show the timer setup view.
        if (!intent.hasExtra(AlarmClock.EXTRA_LENGTH)) {
            startActivity(TimerFragment.createTimerSetupIntent(this));
            LogUtils.i("HandleApiCalls showing timer setup");
            return;
        }

        // Verify that the timer length is between one second and one day.
        final long lengthMillis = SECOND_IN_MILLIS * intent.getIntExtra(AlarmClock.EXTRA_LENGTH, 0);
        if (lengthMillis < Timer.MIN_LENGTH || lengthMillis > Timer.MAX_LENGTH) {
            Voice.notifyFailure(this, getString(R.string.invalid_timer_length));
            LogUtils.i("Invalid timer length requested: " + lengthMillis);
            return;
        }

        final String label = getMessageFromIntent(intent);
        final boolean skipUi = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);

        // Attempt to reuse an existing timer that is Reset with the same length and label.
        Timer timer = null;
        for (Timer t : DataModel.getDataModel().getTimers()) {
            if (!t.isReset()) { continue; }
            if (t.getLength() != lengthMillis) { continue; }
            if (!TextUtils.equals(label, t.getLabel())) { continue; }

            timer = t;
            break;
        }

        // Create a new timer if one could not be reused.
        if (timer == null) {
            timer = DataModel.getDataModel().addTimer(lengthMillis, label, skipUi);
            Events.sendTimerEvent(R.string.action_create, R.string.label_intent);
        }

        // Start the selected timer.
        DataModel.getDataModel().startTimer(timer);
        Events.sendTimerEvent(R.string.action_start, R.string.label_intent);
        Voice.notifySuccess(this, getString(R.string.timer_created));

        // If not instructed to skip the UI, display the running timer.
        if (!skipUi) {
            startActivity(new Intent(this, DeskClock.class)
                    .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX)
                    .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId()));
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

    private static String getMessageFromIntent(Intent intent) {
        final String message = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
        return message == null ? "" : message;
    }

    private static DaysOfWeek getDaysFromIntent(Intent intent) {
        final DaysOfWeek daysOfWeek = new DaysOfWeek(0);
        final ArrayList<Integer> days = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);
        if (days != null) {
            final int[] daysArray = new int[days.size()];
            for (int i = 0; i < days.size(); i++) {
                daysArray[i] = days.get(i);
            }
            daysOfWeek.setDaysOfWeek(true, daysArray);
        } else {
            // API says to use an ArrayList<Integer> but we allow the user to use a int[] too.
            final int[] daysArray = intent.getIntArrayExtra(AlarmClock.EXTRA_DAYS);
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

        if (intent.hasExtra(AlarmClock.EXTRA_MESSAGE)) {
            selection.append(" AND ").append(Alarm.LABEL).append("=?");
            args.add(getMessageFromIntent(intent));
        }

        // Days is treated differently that other fields because if days is not specified, it
        // explicitly means "not recurring".
        selection.append(" AND ").append(Alarm.DAYS_OF_WEEK).append("=?");
        args.add(String.valueOf(intent.hasExtra(AlarmClock.EXTRA_DAYS)
                ? getDaysFromIntent(intent).getBitSet() : DaysOfWeek.NO_DAYS_SET));

        if (intent.hasExtra(AlarmClock.EXTRA_VIBRATE)) {
            selection.append(" AND ").append(Alarm.VIBRATE).append("=?");
            args.add(intent.getBooleanExtra(AlarmClock.EXTRA_VIBRATE, false) ? "1" : "0");
        }

        if (intent.hasExtra(AlarmClock.EXTRA_RINGTONE)) {
            selection.append(" AND ").append(Alarm.RINGTONE).append("=?");

            String ringTone = intent.getStringExtra(AlarmClock.EXTRA_RINGTONE);
            if (ringTone == null) {
                // If the intent explicitly specified a NULL ringtone, treat it as the default
                // ringtone.
                ringTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
            } else if (AlarmClock.VALUE_RINGTONE_SILENT.equals(ringTone) || ringTone.isEmpty()) {
                    ringTone = Alarm.NO_RINGTONE;
            }
            args.add(ringTone);
        }
    }
}
