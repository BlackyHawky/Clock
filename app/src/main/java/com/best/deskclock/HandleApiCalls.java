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

package com.best.deskclock;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.best.deskclock.AlarmSelectionActivity.ACTION_DISMISS;
import static com.best.deskclock.AlarmSelectionActivity.EXTRA_ACTION;
import static com.best.deskclock.AlarmSelectionActivity.EXTRA_ALARMS;
import static com.best.deskclock.provider.AlarmInstance.FIRED_STATE;
import static com.best.deskclock.provider.AlarmInstance.SNOOZE_STATE;
import static com.best.deskclock.uidata.UiDataModel.Tab.ALARMS;
import static com.best.deskclock.uidata.UiDataModel.Tab.TIMERS;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.AlarmClock;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.best.deskclock.alarms.AlarmStateManager;
import com.best.deskclock.controller.Controller;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.timer.TimerFragment;
import com.best.deskclock.timer.TimerService;
import com.best.deskclock.uidata.UiDataModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This activity is never visible. It processes all public intents defined by {@link AlarmClock}
 * that apply to alarms and timers. Its definition in AndroidManifest.xml requires callers to hold
 * the com.android.alarm.permission.SET_ALARM permission to complete the requested action.
 */
public class HandleApiCalls extends Activity {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("HandleApiCalls");

    private Context mAppContext;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mAppContext = getApplicationContext();

        try {
            final Intent intent = getIntent();
            final String action = intent == null ? null : intent.getAction();
            if (action == null) {
                return;
            }
            LOGGER.i("onCreate: " + intent);

            switch (action) {
                case AlarmClock.ACTION_SET_ALARM -> handleSetAlarm(intent);
                case AlarmClock.ACTION_SHOW_ALARMS -> handleShowAlarms();
                case AlarmClock.ACTION_SET_TIMER -> handleSetTimer(intent);
                case AlarmClock.ACTION_SHOW_TIMERS -> handleShowTimers();
                case AlarmClock.ACTION_DISMISS_ALARM -> handleDismissAlarm(intent);
                case AlarmClock.ACTION_SNOOZE_ALARM -> handleSnoozeAlarm();
                case AlarmClock.ACTION_DISMISS_TIMER -> handleDismissTimer(intent);
            }
        } catch (Exception e) {
            LOGGER.wtf(e);
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

    public static void dismissAlarm(Alarm alarm, Activity activity) {
        final Context context = activity.getApplicationContext();
        final AlarmInstance instance = AlarmInstance.getNextUpcomingInstanceByAlarmId(
                context.getContentResolver(), alarm.id);
        if (instance == null) {
            final String reason = context.getString(R.string.no_alarm_scheduled_for_this_time);
            Controller.getController().notifyVoiceFailure(activity, reason);
            LOGGER.i("No alarm instance to dismiss");
            return;
        }

        dismissAlarmInstance(instance, activity);
    }

    public static void dismissAlarmInstance(AlarmInstance instance, Activity activity) {
        Utils.enforceNotMainLooper();

        final Context context = activity.getApplicationContext();
        final Date alarmTime = instance.getAlarmTime().getTime();
        final String time = DateFormat.getTimeFormat(context).format(alarmTime);

        if (instance.mAlarmState == FIRED_STATE || instance.mAlarmState == SNOOZE_STATE) {
            // Always dismiss alarms that are fired or snoozed.
            AlarmStateManager.deleteInstanceAndUpdateParent(context, instance);
        } else if (Utils.isAlarmWithin24Hours(instance)) {
            // Upcoming alarms are always predismissed.
            AlarmStateManager.setPreDismissState(context, instance);
        } else {
            // Otherwise the alarm cannot be dismissed at this time.
            final String reason = context.getString(
                    R.string.alarm_cant_be_dismissed_still_more_than_24_hours_away, time);
            Controller.getController().notifyVoiceFailure(activity, reason);
            LOGGER.i("Can't dismiss alarm more than 24 hours in advance");
        }

        // Log the successful dismissal.
        final String reason = context.getString(R.string.alarm_is_dismissed, time);
        Controller.getController().notifyVoiceSuccess(activity, reason);
        LOGGER.i("Alarm dismissed: " + instance);
        Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_intent);
    }

    private static class DismissAlarmAsync {

        private final Context mContext;
        private final Intent mIntent;
        private final Activity mActivity;

        public DismissAlarmAsync(Context context, Intent intent, Activity activity) {
            mContext = context;
            mIntent = intent;
            mActivity = activity;
        }

        protected void execute() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                final ContentResolver cr = mContext.getContentResolver();
                final List<Alarm> alarms = getEnabledAlarms(mContext);
                if (alarms.isEmpty()) {
                    final String reason = mContext.getString(R.string.no_scheduled_alarms);
                    Controller.getController().notifyVoiceFailure(mActivity, reason);
                    LOGGER.i("No scheduled alarms");
                    return;
                }

                // remove Alarms in MISSED, DISMISSED, and PREDISMISSED states
                for (Iterator<Alarm> i = alarms.iterator(); i.hasNext();) {
                    final AlarmInstance instance = AlarmInstance.getNextUpcomingInstanceByAlarmId(
                            cr, i.next().id);
                    if (instance == null || instance.mAlarmState > FIRED_STATE) {
                        i.remove();
                    }
                }

                final String searchMode = mIntent.getStringExtra(
                        AlarmClock.EXTRA_ALARM_SEARCH_MODE);
                if (searchMode == null && alarms.size() > 1) {
                    // shows the UI where user picks which alarm they want to DISMISS
                    final Intent pickSelectionIntent = new Intent(mContext,
                            AlarmSelectionActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(EXTRA_ACTION, ACTION_DISMISS)
                            .putExtra(EXTRA_ALARMS, alarms.toArray(new Parcelable[0]));
                    mContext.startActivity(pickSelectionIntent);
                    final String voiceMessage = mContext.getString(R.string.pick_alarm_to_dismiss);
                    Controller.getController().notifyVoiceSuccess(mActivity, voiceMessage);
                    return;
                }

                // fetch the alarms that are specified by the intent
                final FetchMatchingAlarmsAction fmaa =
                        new FetchMatchingAlarmsAction(mContext, alarms, mIntent, mActivity);
                fmaa.run();
                final List<Alarm> matchingAlarms = fmaa.getMatchingAlarms();

                // If there are multiple matching alarms and it wasn't expected
                // disambiguate what the user meant
                if (!AlarmClock.ALARM_SEARCH_MODE_ALL.equals(searchMode) &&
                        matchingAlarms.size() > 1) {
                    final Intent pickSelectionIntent = new Intent(mContext,
                            AlarmSelectionActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(EXTRA_ACTION, ACTION_DISMISS)
                            .putExtra(EXTRA_ALARMS, matchingAlarms.toArray(new Parcelable[0]));
                    mContext.startActivity(pickSelectionIntent);
                    final String voiceMessage = mContext.getString(R.string.pick_alarm_to_dismiss);
                    Controller.getController().notifyVoiceSuccess(mActivity, voiceMessage);
                    return;
                }

                // Apply the action to the matching alarms
                for (Alarm alarm : matchingAlarms) {
                    dismissAlarm(alarm, mActivity);
                    LOGGER.i("Alarm dismissed: " + alarm);
                }
            });
        }

        private static List<Alarm> getEnabledAlarms(Context context) {
            final String selection = String.format("%s=?", Alarm.ENABLED);
            final String[] args = { "1" };
            return Alarm.getAlarms(context.getContentResolver(), selection, args);
        }
    }

    private void handleSnoozeAlarm() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            final Context context = getApplicationContext();
            final ContentResolver cr = context.getContentResolver();
            final List<AlarmInstance> alarmInstances = AlarmInstance.getInstancesByState(
                    cr, FIRED_STATE);
            if (alarmInstances.isEmpty()) {
                final String reason = context.getString(R.string.no_firing_alarms);
                Controller.getController().notifyVoiceFailure(this, reason);
                LOGGER.i("No firing alarms");
                return;
            }

            for (AlarmInstance firingAlarmInstance : alarmInstances) {
                snoozeAlarm(firingAlarmInstance, context, this);
            }
        });
    }

    static void snoozeAlarm(AlarmInstance alarmInstance, Context context, Activity activity) {
        Utils.enforceNotMainLooper();

        final String time = DateFormat.getTimeFormat(context).format(
                alarmInstance.getAlarmTime().getTime());
        final String reason = context.getString(R.string.alarm_is_snoozed, time);
        AlarmStateManager.setSnoozeState(context, alarmInstance, true);

        Controller.getController().notifyVoiceSuccess(activity, reason);
        LOGGER.i("Alarm snoozed: " + alarmInstance);
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
                final String voiceMessage = getString(R.string.invalid_time, hour, mins, " ");
                Controller.getController().notifyVoiceFailure(this, voiceMessage);
                LOGGER.i("Illegal hour: " + hour);
                return;
            }
        }

        // Validate the minute, if one was given.
        final int minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);
        if (minutes < 0 || minutes > 59) {
            final String voiceMessage = getString(R.string.invalid_time, hour, minutes, " ");
            Controller.getController().notifyVoiceFailure(this, voiceMessage);
            LOGGER.i("Illegal minute: " + minutes);
            return;
        }

        final boolean skipUi = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);
        final ContentResolver cr = getContentResolver();

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
            final String voiceMessage = getString(R.string.invalid_time, hour, minutes, " ");
            Controller.getController().notifyVoiceFailure(this, voiceMessage);
            LOGGER.i("Missing alarm time; opening UI");
            return;
        }

        final StringBuilder selection = new StringBuilder();
        final List<String> argsList = new ArrayList<>();
        setSelectionFromIntent(intent, hour, minutes, selection, argsList);

        // Try to locate an existing alarm using the intent data.
        final String[] args = argsList.toArray(new String[0]);
        final List<Alarm> alarms = Alarm.getAlarms(cr, selection.toString(), args);

        final Alarm alarm;
        if (!alarms.isEmpty()) {
            // Enable the first matching alarm.
            alarm = alarms.get(0);
            alarm.enabled = true;
            Alarm.updateAlarm(cr, alarm);

            // Delete all old instances.
            AlarmStateManager.deleteAllInstances(this, alarm.id);

            Events.sendAlarmEvent(R.string.action_update, R.string.label_intent);
            LOGGER.i("Updated alarm: " + alarm);
        } else {
            // No existing alarm could be located; create one using the intent data.
            alarm = new Alarm();
            updateAlarmFromIntent(alarm, intent);
            alarm.deleteAfterUse = !alarm.daysOfWeek.isRepeating() && skipUi;

            // Save the new alarm.
            Alarm.addAlarm(cr, alarm);

            Events.sendAlarmEvent(R.string.action_create, R.string.label_intent);
            LOGGER.i("Created new alarm: " + alarm);
        }

        // Schedule the next instance.
        final Calendar now = DataModel.getDataModel().getCalendar();
        final AlarmInstance alarmInstance = alarm.createInstanceAfter(now);
        setupInstance(alarmInstance, skipUi);

        final String time = DateFormat.getTimeFormat(this)
                .format(alarmInstance.getAlarmTime().getTime());
        Controller.getController().notifyVoiceSuccess(this, getString(R.string.alarm_is_set, time));
    }

    private void handleDismissTimer(Intent intent) {
        final Uri dataUri = intent.getData();
        if (dataUri != null) {
            final Timer selectedTimer = getSelectedTimer(dataUri);
            if (selectedTimer != null) {
                DataModel.getDataModel().resetOrDeleteTimer(selectedTimer, R.string.label_intent);
                Controller.getController().notifyVoiceSuccess(this,
                        getResources().getQuantityString(R.plurals.expired_timers_dismissed, 1));
                LOGGER.i("Timer dismissed: " + selectedTimer);
            } else {
                Controller.getController().notifyVoiceFailure(this,
                        getString(R.string.invalid_timer));
                LOGGER.e("Could not dismiss timer: invalid URI");
            }
        } else {
            final List<Timer> expiredTimers = DataModel.getDataModel().getExpiredTimers();
            if (!expiredTimers.isEmpty()) {
                for (Timer timer : expiredTimers) {
                    DataModel.getDataModel().resetOrDeleteTimer(timer, R.string.label_intent);
                }
                final int numberOfTimers = expiredTimers.size();
                final String timersDismissedMessage = getResources().getQuantityString(
                        R.plurals.expired_timers_dismissed, numberOfTimers, numberOfTimers);
                Controller.getController().notifyVoiceSuccess(this, timersDismissedMessage);
                LOGGER.i(timersDismissedMessage);
            } else {
                Controller.getController().notifyVoiceFailure(this,
                        getString(R.string.no_expired_timers));
                LOGGER.e("Could not dismiss timer: no expired timers");
            }
        }
    }

    private Timer getSelectedTimer(Uri dataUri) {
        try {
            final int timerId = (int) ContentUris.parseId(dataUri);
            return DataModel.getDataModel().getTimer(timerId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void handleShowAlarms() {
        Events.sendAlarmEvent(R.string.action_show, R.string.label_intent);

        // Open DeskClock positioned on the alarms tab.
        UiDataModel.getUiDataModel().setSelectedTab(ALARMS);
        startActivity(new Intent(this, DeskClock.class));
    }

    private void handleShowTimers() {
        Events.sendTimerEvent(R.string.action_show, R.string.label_intent);

        final Intent showTimersIntent = new Intent(this, DeskClock.class);

        final List<Timer> timers = DataModel.getDataModel().getTimers();
        if (!timers.isEmpty()) {
            final Timer newestTimer = timers.get(timers.size() - 1);
            showTimersIntent.putExtra(TimerService.EXTRA_TIMER_ID, newestTimer.getId());
        }

        // Open DeskClock positioned on the timers tab.
        UiDataModel.getUiDataModel().setSelectedTab(TIMERS);
        startActivity(showTimersIntent);
    }

    private void handleSetTimer(Intent intent) {
        // If no length is supplied, show the timer setup view.
        if (!intent.hasExtra(AlarmClock.EXTRA_LENGTH)) {
            // Change to the timers tab.
            UiDataModel.getUiDataModel().setSelectedTab(TIMERS);

            // Open DeskClock which is now positioned on the timers tab and show the timer setup.
            startActivity(TimerFragment.createTimerSetupIntent(this));
            LOGGER.i("Showing timer setup");
            return;
        }

        // Verify that the timer length is between one second and one day.
        final long lengthMillis = SECOND_IN_MILLIS * intent.getIntExtra(AlarmClock.EXTRA_LENGTH, 0);
        if (lengthMillis < Timer.MIN_LENGTH) {
            final String voiceMessage = getString(R.string.invalid_timer_length);
            Controller.getController().notifyVoiceFailure(this, voiceMessage);
            LOGGER.i("Invalid timer length requested: " + lengthMillis);
            return;
        }

        final String label = getLabelFromIntent(intent, "");
        final boolean skipUi = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);

        // Attempt to reuse an existing timer that is Reset with the same length and label.
        Timer timer = null;
        for (Timer t : DataModel.getDataModel().getTimers()) {
            if (!t.isReset()) {
                continue;
            }
            if (t.getLength() != lengthMillis) {
                continue;
            }
            if (!TextUtils.equals(label, t.getLabel())) {
                continue;
            }

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
        Controller.getController().notifyVoiceSuccess(this, getString(R.string.timer_created));

        // If not instructed to skip the UI, display the running timer.
        if (!skipUi) {
            // Change to the timers tab.
            UiDataModel.getUiDataModel().setSelectedTab(TIMERS);

            // Open DeskClock which is now positioned on the timers tab.
            startActivity(new Intent(this, DeskClock.class)
                    .putExtra(TimerService.EXTRA_TIMER_ID, timer.getId()));
        }
    }

    private void setupInstance(AlarmInstance instance, boolean skipUi) {
        AlarmInstance.addInstance(this.getContentResolver(), instance);
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
     * @param alarm  the alarm to be updated
     * @param intent the intent containing new alarm field values to merge into the {@code alarm}
     */
    private static void updateAlarmFromIntent(Alarm alarm, Intent intent) {
        alarm.enabled = true;
        alarm.hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, alarm.hour);
        alarm.minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, alarm.minutes);
        alarm.vibrate = intent.getBooleanExtra(AlarmClock.EXTRA_VIBRATE, alarm.vibrate);
        alarm.alert = getAlertFromIntent(intent, alarm.alert);
        alarm.label = getLabelFromIntent(intent, alarm.label);
        alarm.daysOfWeek = getDaysFromIntent(intent, alarm.daysOfWeek);
    }

    private static String getLabelFromIntent(Intent intent, String defaultLabel) {
        final String message = intent.getExtras().getString(AlarmClock.EXTRA_MESSAGE, defaultLabel);
        return message == null ? "" : message;
    }

    private static Weekdays getDaysFromIntent(Intent intent, Weekdays defaultWeekdays) {
        if (!intent.hasExtra(AlarmClock.EXTRA_DAYS)) {
            return defaultWeekdays;
        }

        final List<Integer> days = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);
        if (days != null) {
            final int[] daysArray = new int[days.size()];
            for (int i = 0; i < days.size(); i++) {
                daysArray[i] = days.get(i);
            }
            return Weekdays.fromCalendarDays(daysArray);
        } else {
            // API says to use an ArrayList<Integer> but we allow the user to use a int[] too.
            final int[] daysArray = intent.getIntArrayExtra(AlarmClock.EXTRA_DAYS);
            if (daysArray != null) {
                return Weekdays.fromCalendarDays(daysArray);
            }
        }
        return defaultWeekdays;
    }

    private static Uri getAlertFromIntent(Intent intent, Uri defaultUri) {
        final String alert = intent.getStringExtra(AlarmClock.EXTRA_RINGTONE);
        if (alert == null) {
            return defaultUri;
        } else if (AlarmClock.VALUE_RINGTONE_SILENT.equals(alert) || alert.isEmpty()) {
            return Alarm.NO_RINGTONE_URI;
        }

        return Uri.parse(alert);
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
     * @param intent    contains details of the alarm to be located
     * @param hour      the hour of the day of the alarm
     * @param minutes   the minute of the hour of the alarm
     * @param selection an out parameter containing a SQL where clause
     * @param args      an out parameter containing the values to substitute into the {@code selection}
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

        // Days is treated differently than other fields because if days is not specified, it
        // explicitly means "not recurring".
        selection.append(" AND ").append(Alarm.DAYS_OF_WEEK).append("=?");
        args.add(String.valueOf(getDaysFromIntent(intent, Weekdays.NONE).getBits()));

        if (intent.hasExtra(AlarmClock.EXTRA_VIBRATE)) {
            selection.append(" AND ").append(Alarm.VIBRATE).append("=?");
            args.add(intent.getBooleanExtra(AlarmClock.EXTRA_VIBRATE, false) ? "1" : "0");
        }

        if (intent.hasExtra(AlarmClock.EXTRA_RINGTONE)) {
            selection.append(" AND ").append(Alarm.RINGTONE).append("=?");

            // If the intent explicitly specified a NULL ringtone, treat it as the default ringtone.
            final Uri defaultRingtone = DataModel.getDataModel().getDefaultAlarmRingtoneUriFromSettings();
            final Uri ringtone = getAlertFromIntent(intent, defaultRingtone);
            args.add(ringtone.toString());
        }
    }
}
