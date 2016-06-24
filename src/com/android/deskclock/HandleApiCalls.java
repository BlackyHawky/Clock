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
import com.android.deskclock.uidata.UiDataModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.android.deskclock.uidata.UiDataModel.Tab.ALARMS;
import static com.android.deskclock.uidata.UiDataModel.Tab.TIMERS;

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
                    handleDismissAlarm(intent);
                    break;
                case AlarmClock.ACTION_SNOOZE_ALARM:
                    handleSnoozeAlarm();
            }
        } finally {
            finish();
        }
    }

    private void handleDismissAlarm(Intent intent) {
        // Change to the alarms tab.
        UiDataModel.getUiDataModel().setSelectedTab(ALARMS);

        // Open DeskClock which is now positioned on the alarms tab.
        startActivity(new Intent(mAppContext, DeskClock.class));

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
        // Validate the hour, if one was given.
        int hour = -1;
        if (intent.hasExtra(AlarmClock.EXTRA_HOUR)) {
            hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, hour);
            if (hour < 0 || hour > 23) {
                final int mins = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);
                Voice.notifyFailure(this, getString(R.string.invalid_time, hour, mins, " "));
                LogUtils.i("HandleApiCalls given illegal hour: " + hour);
                return;
            }
        }

        // Validate the minute, if one was given.
        final int minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);
        if (minutes < 0 || minutes > 59) {
            Voice.notifyFailure(this, getString(R.string.invalid_time, hour, minutes, " "));
            LogUtils.i("HandleApiCalls given illegal minute: " + minutes);
            return;
        }

        final boolean skipUi = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);

        final ContentResolver cr = getContentResolver();
        final Uri deeplink = intent.getData();
        if (deeplink != null) {
            // Attempt to locate the alarm via the deeplink.
            final Alarm alarm = Alarm.getAlarm(cr, deeplink);
            if (alarm == null) {
                Voice.notifyFailure(this, getString(R.string.cannot_locate_alarm));
                LogUtils.i(String.format("HandleApiCalls cannot locate alarm using: %s", deeplink));
                return;
            }

            // Remove all defunct instances.
            AlarmStateManager.deleteAllInstances(this, alarm.id);

            // Update the alarm with supplied data from the intent.
            updateAlarmFromIntent(alarm, intent);

            // Save the updated alarm.
            Alarm.updateAlarm(cr, alarm);

            // Create the next instance with the updated alarm data.
            final AlarmInstance alarmInstance = alarm.createInstanceAfter(Calendar.getInstance());
            setupInstance(alarmInstance, skipUi);

            final String time = DateFormat.getTimeFormat(this).format(
                    alarmInstance.getAlarmTime().getTime());
            Voice.notifySuccess(this, getString(R.string.alarm_is_set, time));
            Events.sendAlarmEvent(R.string.action_update, R.string.label_intent);
            LogUtils.i("HandleApiCalls updated existing alarm: %s", alarm);
            return;
        }

        // If time information was not provided an existing alarm cannot be located and a new one
        // cannot be created so show the UI for creating the alarm from scratch per spec.
        if (hour == -1) {
            // Change to the alarms tab.
            UiDataModel.getUiDataModel().setSelectedTab(ALARMS);

            // Intent has no time or an invalid time, open the alarm creation UI.
            final Intent createAlarm = Alarm.createIntent(this, DeskClock.class, Alarm.INVALID_ID)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(AlarmClockFragment.ALARM_CREATE_NEW_INTENT_EXTRA, true);

            // Open DeskClock which is now positioned on the alarms tab.
            startActivity(createAlarm);
            Voice.notifyFailure(this, getString(R.string.invalid_time, hour, minutes, " "));
            LogUtils.i("HandleApiCalls not given time information; opening UI");
            return;
        }

        final StringBuilder selection = new StringBuilder();
        final List<String> argsList = new ArrayList<>();
        setSelectionFromIntent(intent, hour, minutes, selection, argsList);

        // Try to locate an existing alarm using the intent data.
        final String[] args = argsList.toArray(new String[argsList.size()]);
        final List<Alarm> alarms = Alarm.getAlarms(cr, selection.toString(), args);
        if (!alarms.isEmpty()) {
            // Enable the first matching alarm.
            final Alarm alarm = alarms.get(0);
            alarm.enabled = true;
            Alarm.updateAlarm(cr, alarm);

            // Delete all old instances and create a new one with updated values.
            AlarmStateManager.deleteAllInstances(this, alarm.id);
            setupInstance(alarm.createInstanceAfter(Calendar.getInstance()), skipUi);
            Events.sendAlarmEvent(R.string.action_update, R.string.label_intent);
            LogUtils.i("HandleApiCalls deleted old, created new alarm: %s", alarm);
            return;
        }

        // No existing alarm could be located; create one using the intent data.
        Alarm alarm = new Alarm();
        updateAlarmFromIntent(alarm, intent);
        alarm.deleteAfterUse = !alarm.daysOfWeek.isRepeating() && skipUi;

        // Save the new alarm.
        alarm = Alarm.addAlarm(cr, alarm);

        // Create the next instance with the alarm data.
        final AlarmInstance alarmInstance = alarm.createInstanceAfter(Calendar.getInstance());
        setupInstance(alarmInstance, skipUi);

        Events.sendAlarmEvent(R.string.action_create, R.string.label_intent);
        final String time = DateFormat.getTimeFormat(this).format(
                alarmInstance.getAlarmTime().getTime());
        Voice.notifySuccess(this, getString(R.string.alarm_is_set, time));
        LogUtils.i("HandleApiCalls created alarm: %s", alarm);
    }

    private void handleShowAlarms() {
        // Change to the alarms tab.
        UiDataModel.getUiDataModel().setSelectedTab(ALARMS);

        // Open DeskClock which is now positioned on the alarms tab.
        startActivity(new Intent(this, DeskClock.class));

        Events.sendAlarmEvent(R.string.action_show, R.string.label_intent);
        LogUtils.i("HandleApiCalls show alarms");
    }

    private void handleSetTimer(Intent intent) {
        // If no length is supplied, show the timer setup view.
        if (!intent.hasExtra(AlarmClock.EXTRA_LENGTH)) {
            // Change to the timers tab.
            UiDataModel.getUiDataModel().setSelectedTab(TIMERS);

            // Open DeskClock which is now positioned on the timers tab and show the timer setup.
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

        final String label = getLabelFromIntent(intent, "");
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
            // Change to the timers tab.
            UiDataModel.getUiDataModel().setSelectedTab(TIMERS);

            // Open DeskClock which is now positioned on the timers tab.
            startActivity(new Intent(this, DeskClock.class)
                    .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId()));
        }
    }

    private void setupInstance(AlarmInstance instance, boolean skipUi) {
        instance = AlarmInstance.addInstance(this.getContentResolver(), instance);
        AlarmStateManager.registerInstance(this, instance, true);
        AlarmUtils.popAlarmSetToast(this, instance.getAlarmTime().getTimeInMillis());
        if (!skipUi) {
            // Change to the alarms tab.
            UiDataModel.getUiDataModel().setSelectedTab(ALARMS);

            // Open DeskClock which is now positioned on the alarms tab.
            final Intent showAlarm = Alarm.createIntent(this, DeskClock.class, instance.mAlarmId)
                    .putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, instance.mAlarmId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(showAlarm);
        }
    }

    /**
     * @param alarm the alarm to be updated
     * @param intent the intent containing new alarm field values to merge into the {@code alarm}
     */
    private static void updateAlarmFromIntent(Alarm alarm, Intent intent) {
        alarm.enabled = true;
        alarm.hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, alarm.hour);
        alarm.minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, alarm.minutes);
        alarm.vibrate = intent.getBooleanExtra(AlarmClock.EXTRA_VIBRATE, alarm.vibrate);
        alarm.alert = getAlertFromIntent(intent, alarm.alert);
        alarm.label = getLabelFromIntent(intent, alarm.label);
        alarm.daysOfWeek = getDaysFromIntent(intent, alarm.daysOfWeek.getBitSet());
    }

    private static String getLabelFromIntent(Intent intent, String defaultLabel) {
        final String message = intent.getExtras().getString(AlarmClock.EXTRA_MESSAGE, defaultLabel);
        return message == null ? "" : message;
    }

    private static DaysOfWeek getDaysFromIntent(Intent intent, int defaultDaysBitset) {
        if (!intent.hasExtra(AlarmClock.EXTRA_DAYS)) {
            return new DaysOfWeek(defaultDaysBitset);
        }

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

    private static Uri getAlertFromIntent(Intent intent, Uri defaultUri) {
        final String alert = intent.getStringExtra(AlarmClock.EXTRA_RINGTONE);
        if (alert != null) {
            if (AlarmClock.VALUE_RINGTONE_SILENT.equals(alert) || alert.isEmpty()) {
                return Alarm.NO_RINGTONE_URI;
            } else {
                return Uri.parse(alert);
            }
        } else {
            return defaultUri;
        }
    }

    /**
     * Assemble a database where clause to search for an alarm matching the given {@code hour} and
     * {@code minutes} as well as all of the optional information within the {@code intent}
     * including:
     *
     * <ul>
     *     <li>alarm message</li>
     *     <li>repeat days</li>
     *     <li>vibration setting</li>
     *     <li>ringtone uri</li>
     * </ul>
     *
     * @param intent contains details of the alarm to be located
     * @param hour the hour of the day of the alarm
     * @param minutes the minute of the hour of the alarm
     * @param selection an out parameter containing a SQL where clause
     * @param args an out parameter containing the values to substitute into the {@code selection}
     */
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
            args.add(getLabelFromIntent(intent, ""));
        }

        // Days is treated differently that other fields because if days is not specified, it
        // explicitly means "not recurring".
        selection.append(" AND ").append(Alarm.DAYS_OF_WEEK).append("=?");
        args.add(String.valueOf(getDaysFromIntent(intent, DaysOfWeek.NO_DAYS_SET).getBitSet()));

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
