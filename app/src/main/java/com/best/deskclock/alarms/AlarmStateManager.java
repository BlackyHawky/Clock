/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.content.Context.ALARM_SERVICE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_TIMEOUT_NEVER;
import static com.best.deskclock.utils.AlarmUtils.ACTION_NEXT_ALARM_CHANGED_BY_CLOCK;

import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import com.best.deskclock.AlarmAlertWakeLock;
import com.best.deskclock.AlarmClockFragment;
import com.best.deskclock.AsyncHandler;
import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This class handles all the state changes for alarm instances. You need to
 * register all alarm instances with the state manager if you want them to
 * be activated. If a major time change has occurred (ie. TIMEZONE_CHANGE, TIMESET_CHANGE),
 * then you must also re-register instances to fix their states.
 * <p>
 * Please see {@link #registerInstance) for special transitions when major time changes occur.
 * <p>
 * Following states:
 * <p>
 * SILENT_STATE:
 * This state is used when the alarm is activated, but doesn't need to display anything. It
 * is in charge of changing the alarm instance state to a NOTIFICATION_STATE.
 * <p>
 * NOTIFICATION_STATE:
 * This state is used to notify the user that the alarm will go off
 * It is in charge of triggering a FIRED_STATE or DISMISS_STATE
 * <p>
 * SNOOZED_STATE:
 * The SNOOZED_STATE behaves like a NOTIFICATION_STATE, but with a different message. It
 * also increments the alarm time in the instance to reflect the new snooze time.
 * <p>
 * FIRED_STATE:
 * The FIRED_STATE is used when the alarm is firing. It will start the AlarmService, and wait
 * until the user interacts with the alarm via SNOOZED_STATE or DISMISS_STATE change. If the user
 * doesn't then it might be change to MISSED_STATE if auto-silenced was enabled.
 * <p>
 * MISSED_STATE:
 * The MISSED_STATE is used when the alarm already fired, but the user could not interact with
 * it. At this point the alarm instance is dead and we check the parent alarm to see if we need
 * to disable or schedule a new alarm_instance. There is also a notification shown to the user
 * that he/she missed the alarm and that stays for
 * {@link AlarmInstance#MISSED_TIME_TO_LIVE_HOUR_OFFSET} or until the user acknownledges it.
 * <p>
 * DISMISS_STATE:
 * This is really a transient state that will properly delete the alarm instance. Use this state,
 * whenever you want to get rid of the alarm instance. This state will also check the alarm
 * parent to see if it should disable or schedule a new alarm instance.
 */
public final class AlarmStateManager extends BroadcastReceiver {

    // Intent action to trigger an instance state change.
    public static final String CHANGE_STATE_ACTION = "change_state";

    // Intent action to show the alarm and dismiss the instance
    public static final String SHOW_AND_DISMISS_ALARM_ACTION = "show_and_dismiss_alarm";

    // Extra key to set the desired state change.
    public static final String ALARM_STATE_EXTRA = "intent.extra.alarm.state";

    // Extra key to indicate the state change was launched from a notification.
    public static final String FROM_NOTIFICATION_EXTRA = "intent.extra.from.notification";

    // Intent category tags used to dismiss or snooze an alarm
    public static final String ALARM_DISMISS_TAG = "DISMISS_TAG";
    public static final String ALARM_SNOOZE_TAG = "SNOOZE_TAG";

    // Buffer time in seconds to fire alarm instead of marking it missed.
    public static final int ALARM_FIRE_BUFFER = 15;

    // Intent action for an AlarmManager alarm serving only to set the next alarm indicators
    private static final String INDICATOR_ACTION = "indicator";

    // Extra key to set the global broadcast id.
    private static final String ALARM_GLOBAL_ID_EXTRA = "intent.extra.alarm.global.id";

    // Intent category tag used when schedule state change intents in alarm manager.
    private static final String ALARM_MANAGER_TAG = "ALARM_MANAGER";
    private static final String ACTION_SET_POWEROFF_ALARM = "org.codeaurora.poweroffalarm.action.SET_ALARM";
    private static final String ACTION_CANCEL_POWEROFF_ALARM = "org.codeaurora.poweroffalarm.action.CANCEL_ALARM";
    private static final String POWER_OFF_ALARM_PACKAGE = "com.qualcomm.qti.poweroffalarm";
    private static final String TIME = "time";

    // Schedules alarm state transitions; can be mocked for testing purposes.
    private static final StateChangeScheduler sStateChangeScheduler = new AlarmManagerStateChangeScheduler();

    private static Calendar getCurrentTime() {
        return DataModel.getDataModel().getCalendar();
    }

    /**
     * Update the next alarm stored in framework. This value is also displayed in digital widgets,
     * the clock tab and the screensaver in this app.
     */
    private static void updateNextAlarm(Context context) {
        final AlarmInstance nextAlarm = getNextFiringAlarm(context);

        if (nextAlarm != null) {
            setPowerOffAlarm(context, nextAlarm);
        }

        updateNextAlarmInAlarmManager(context, nextAlarm);

        // Adding a Handler ensures better fluidity when activating/deactivating the alarm
        new Handler(context.getMainLooper()).postDelayed(() -> {
            Intent nextAlarmChangedIntent = new Intent(ACTION_NEXT_ALARM_CHANGED_BY_CLOCK);
            nextAlarmChangedIntent.setPackage(context.getPackageName());
            context.sendBroadcast(nextAlarmChangedIntent);

            Intent appwidgetUpdateIntent = new Intent(ACTION_APPWIDGET_UPDATE);
            appwidgetUpdateIntent.setPackage(context.getPackageName());
            context.sendBroadcast(appwidgetUpdateIntent);
        }, 300);
    }


    /**
     * Returns an alarm instance of an alarm that's going to fire next.
     *
     * @param context application context
     * @return an alarm instance that will fire earliest relative to current time.
     */
    public static AlarmInstance getNextFiringAlarm(Context context) {
        final ContentResolver cr = context.getContentResolver();
        final String activeAlarmQuery = AlarmInstance.ALARM_STATE + "<" + AlarmInstance.FIRED_STATE;
        final List<AlarmInstance> alarmInstances = AlarmInstance.getInstances(cr, activeAlarmQuery);

        AlarmInstance nextAlarm = null;
        for (AlarmInstance instance : alarmInstances) {
            if (nextAlarm == null || instance.getAlarmTime().before(nextAlarm.getAlarmTime())) {
                nextAlarm = instance;
            }
        }
        return nextAlarm;
    }

    /**
     * Used in L and later devices where "next alarm" is stored in the Alarm Manager.
     */
    private static void updateNextAlarmInAlarmManager(Context context, AlarmInstance nextAlarm) {
        // Sets a surrogate alarm with alarm manager that provides the AlarmClockInfo for the
        // alarm that is going to fire next. The operation is constructed such that it is ignored
        // by AlarmStateManager.

        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        final int flags = (nextAlarm == null ? PendingIntent.FLAG_NO_CREATE : 0) | PendingIntent.FLAG_IMMUTABLE;
        final PendingIntent operation = PendingIntent.getBroadcast(context, 0,
                AlarmStateManager.createIndicatorIntent(context), flags);

        if (nextAlarm != null) {
            LogUtils.i("Setting upcoming AlarmClockInfo for alarm: " + nextAlarm.mId);
            long alarmTime = nextAlarm.getAlarmTime().getTimeInMillis();

            // Create an intent that can be used to show or edit details of the next alarm.
            PendingIntent viewIntent = PendingIntent.getActivity(context, nextAlarm.hashCode(),
                    AlarmNotifications.createViewAlarmIntent(context, nextAlarm),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            final AlarmClockInfo info = new AlarmClockInfo(alarmTime, viewIntent);
            updateNextAlarm(alarmManager, info, operation);
        } else if (operation != null) {
            LogUtils.i("Canceling upcoming AlarmClockInfo");
            alarmManager.cancel(operation);
        }
    }

    private static void updateNextAlarm(AlarmManager am, AlarmClockInfo info, PendingIntent op) {
        am.setAlarmClock(info, op);
    }

    /**
     * Used by dismissed and missed states, to update parent alarm. This will either
     * disable, delete or reschedule parent alarm.
     *
     * @param context  application context
     * @param instance to update parent for
     */
    private static void updateParentAlarm(Context context, AlarmInstance instance) {
        ContentResolver cr = context.getContentResolver();
        Alarm alarm = Alarm.getAlarm(cr, instance.mAlarmId);
        if (alarm == null) {
            LogUtils.e("Parent has been deleted with instance: " + instance);
            return;
        }

        if (!alarm.daysOfWeek.isRepeating()) {
            if (alarm.deleteAfterUse) {
                LogUtils.i("Deleting parent alarm: " + alarm.id);
                Alarm.deleteAlarm(cr, alarm.id);
                if (!DataModel.getDataModel().isApplicationInForeground()) {
                    final String time = DateFormat.getTimeFormat(context).format(instance.getAlarmTime().getTime());
                    Toast.makeText(context, context.getString(R.string.occasional_alarm_deleted, time),
                            Toast.LENGTH_LONG).show();
                }
            } else {
                LogUtils.i("Disabling parent alarm: " + alarm.id);
                alarm.enabled = false;
                Alarm.updateAlarm(cr, alarm);
            }
        } else {
            // Schedule the next repeating instance which may be before the current instance if a
            // time jump has occurred. Otherwise, if the current instance is the next instance
            // and has already been fired, schedule the subsequent instance.
            AlarmInstance nextRepeatedInstance = alarm.createInstanceAfter(getCurrentTime());
            if (instance.mAlarmState > AlarmInstance.FIRED_STATE
                    && nextRepeatedInstance.getAlarmTime().equals(instance.getAlarmTime())) {
                nextRepeatedInstance = alarm.createInstanceAfter(instance.getAlarmTime());
            }

            LogUtils.i("Creating new instance for repeating alarm " + alarm.id + " at " +
                    AlarmUtils.getFormattedTime(context, nextRepeatedInstance.getAlarmTime()));
            AlarmInstance.addInstance(cr, nextRepeatedInstance);
            registerInstance(context, nextRepeatedInstance, true);
        }
    }

    /**
     * Utility method to create a proper change state intent.
     *
     * @param context  application context
     * @param tag      used to make intent differ from other state change intents.
     * @param instance to change state to
     * @param state    to change to.
     * @return intent that can be used to change an alarm instance state
     */
    public static Intent createStateChangeIntent(Context context, String tag,
                                                 AlarmInstance instance, Integer state) {
        // This intent is directed to AlarmService, though the actual handling of it occurs here
        // in AlarmStateManager. The reason is that evidence exists showing the jump between the
        // broadcast receiver (AlarmStateManager) and service (AlarmService) can be thwarted by the
        // Out Of Memory killer. If clock is killed during that jump, firing an alarm can fail to
        // occur. To be safer, the call begins in AlarmService, which has the power to display the
        // firing alarm if needed, so no jump is needed.
        Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction(CHANGE_STATE_ACTION);
        intent.addCategory(tag);
        intent.putExtra(ALARM_GLOBAL_ID_EXTRA, SettingsDAO.getGlobalIntentId(getDefaultSharedPreferences(context)));
        if (state != null) {
            intent.putExtra(ALARM_STATE_EXTRA, state.intValue());
        }
        return intent;
    }

    /**
     * Schedule alarm instance state changes with {@link AlarmManager}.
     *
     * @param ctx      application context
     * @param time     to trigger state change
     * @param instance to change state to
     * @param newState to change to
     */
    private static void scheduleInstanceStateChange(Context ctx, Calendar time, AlarmInstance instance, int newState) {
        sStateChangeScheduler.scheduleInstanceStateChange(ctx, time, instance, newState);
    }

    /**
     * Cancel all {@link AlarmManager} timers for instance.
     *
     * @param ctx      application context
     * @param instance to disable all {@link AlarmManager} timers
     */
    private static void cancelScheduledInstanceStateChange(Context ctx, AlarmInstance instance) {
        sStateChangeScheduler.cancelScheduledInstanceStateChange(ctx, instance);
    }

    /**
     * This will set the alarm instance to the SILENT_STATE and update
     * the application notifications and schedule any state changes that need
     * to occur in the future.
     *
     * @param context  application context
     * @param instance to set state to
     */
    public static void setSilentState(Context context, AlarmInstance instance) {
        LogUtils.i("Setting silent state to instance " + instance.mId);

        // Update alarm in db
        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.SILENT_STATE;
        AlarmInstance.updateInstance(contentResolver, instance);

        // Setup instance notification and scheduling timers
        AlarmNotifications.clearNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getNotificationTime(context),
                instance, AlarmInstance.NOTIFICATION_STATE);
    }

    /**
     * This will set the alarm instance to the NOTIFICATION_STATE and update
     * the application notifications and schedule any state changes that need
     * to occur in the future.
     *
     * @param context  application context
     * @param instance to set state to
     */
    public static void setNotificationState(Context context, AlarmInstance instance) {
        LogUtils.i("Setting notification state to instance " + instance.mId);

        // Update alarm state in db
        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.NOTIFICATION_STATE;
        AlarmInstance.updateInstance(contentResolver, instance);

        // Setup instance notification and scheduling timers
        AlarmNotifications.showUpcomingNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getAlarmTime(), instance, AlarmInstance.FIRED_STATE);
    }

    /**
     * This will set the alarm instance to the FIRED_STATE and update
     * the application notifications and schedule any state changes that need
     * to occur in the future.
     *
     * @param context  application context
     * @param instance to set state to
     */
    public static void setFiredState(Context context, AlarmInstance instance) {
        LogUtils.i("Setting fire state to instance " + instance.mId);

        // Update alarm state in db
        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.FIRED_STATE;

        // If the alarm ringtone is set to "Random", assign a random ringtone uri to the alarm.
        Alarm alarm = Alarm.getAlarm(context.getContentResolver(), instance.mAlarmId);
        if (alarm != null) {
            if (RingtoneUtils.isRandomRingtone(alarm.alert)) {
                instance.mRingtone = RingtoneUtils.getRandomRingtoneUri();
            } else if (RingtoneUtils.isRandomCustomRingtone(alarm.alert)) {
                instance.mRingtone = RingtoneUtils.getRandomCustomRingtoneUri();
            }
        }

        AlarmInstance.updateInstance(contentResolver, instance);

        if (instance.mAlarmId != null) {
            // if the time changed *backward* and pushed an instance from missed back to fired,
            // remove any other scheduled instances that may exist
            AlarmInstance.deleteOtherInstances(context, contentResolver, instance.mAlarmId, instance.mId);
        }

        Events.sendAlarmEvent(R.string.action_fire, 0);

        Calendar timeout = instance.getTimeout(context);
        if (timeout != null) {
            scheduleInstanceStateChange(context, timeout, instance, AlarmInstance.MISSED_STATE);
        }

        // Instance not valid anymore, so find next alarm that will fire and notify system
        updateNextAlarm(context);
    }

    /**
     * This will set the alarm instance to the SNOOZE_STATE and update
     * the application notifications and schedule any state changes that need
     * to occur in the future.
     *
     * @param context  application context
     * @param instance to set state to
     */
    public static void setSnoozeState(final Context context, AlarmInstance instance, boolean showToast) {
        final SharedPreferences prefs = getDefaultSharedPreferences(context);
        final int snoozeMinutes = instance.mSnoozeDuration;
        Calendar newAlarmTime = Calendar.getInstance();
        // If the "Snooze duration" setting has been set to "None" or if "Enable alarm snooze actions"
        // is not enabled in the expanded alarm view, simply dismiss the alarm.
        if (snoozeMinutes == ALARM_SNOOZE_DURATION_DISABLED) {
            deleteInstanceAndUpdateParent(context, instance);
            return;
        }

        // Stop alarm if this instance is firing it; a double vibration will be performed if enabled in settings
        // to indicate that the alarm is correctly snoozed.
        if (SettingsDAO.areSnoozedOrDismissedAlarmVibrationsEnabled(prefs)) {
            AlarmService.stopAlarmWithDoubleVibration(context, instance);
        } else {
            AlarmService.stopAlarm(context, instance);
        }

        // Calculate the new snooze alarm time
        newAlarmTime.add(Calendar.MINUTE, snoozeMinutes);

        // Update alarm state and new alarm time in db.
        LogUtils.i("Setting snoozed state to instance " + instance.mId + " for "
                + AlarmUtils.getFormattedTime(context, newAlarmTime));
        instance.setAlarmTime(newAlarmTime);
        instance.mAlarmState = AlarmInstance.SNOOZE_STATE;
        AlarmInstance.updateInstance(context.getContentResolver(), instance);

        // Setup instance notification and scheduling timers
        AlarmNotifications.showSnoozeNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getAlarmTime(), instance, AlarmInstance.FIRED_STATE);

        // Display the snooze minutes in a toast.
        if (showToast) {
            final Handler mainHandler = new Handler(context.getMainLooper());
            final Runnable myRunnable = () -> {
                String displayTime = String.format(context.getResources()
                                .getQuantityText(R.plurals.alarm_alert_snooze_set, snoozeMinutes).toString(), snoozeMinutes);
                Toast.makeText(context, displayTime, Toast.LENGTH_LONG).show();
            };
            mainHandler.post(myRunnable);
        }

        // Instance time changed, so find next alarm that will fire and notify system
        updateNextAlarm(context);
    }

    /**
     * This will set the alarm instance to the MISSED_STATE and update
     * the application notifications and schedule any state changes that need
     * to occur in the future.
     *
     * @param context  application context
     * @param instance to set state to
     */
    public static void setMissedState(Context context, AlarmInstance instance) {
        LogUtils.i("Setting missed state to instance " + instance.mId);

        // If the "Alarm silence" setting has not been set to "Never", we don't want alarms
        // to be seen as missed but snoozed.
        // This avoids having to create multiple alarms for the same reason.
        if (instance.mAutoSilenceDuration != ALARM_TIMEOUT_NEVER) {
            setSnoozeState(context, instance, true);
            return;
        }

        // Stop alarm if this instance is firing it
        AlarmService.stopAlarm(context, instance);

        // Check parent if it needs to reschedule, disable or delete itself
        if (instance.mAlarmId != null) {
            updateParentAlarm(context, instance);
        }

        // Update alarm state
        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.MISSED_STATE;
        AlarmInstance.updateInstance(contentResolver, instance);

        // Setup instance notification and scheduling timers
        AlarmNotifications.showMissedNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getMissedTimeToLive(), instance, AlarmInstance.DISMISSED_STATE);

        cancelPowerOffAlarm(context, instance);
        // Instance is not valid anymore, so find next alarm that will fire and notify system
        updateNextAlarm(context);
    }

    /**
     * This will set the alarm instance to the PREDISMISSED_STATE and schedule an instance state
     * change to DISMISSED_STATE at the regularly scheduled firing time.
     *
     * @param context  application context
     * @param instance to set state to
     */
    public static void setPreDismissState(Context context, AlarmInstance instance) {
        LogUtils.i("Setting predismissed state to instance " + instance.mId);

        // Stop alarm if this instance is firing it; a single vibration will be performed if enabled in settings
        // to indicate that the alarm is correctly dismissed.
        if (SettingsDAO.areSnoozedOrDismissedAlarmVibrationsEnabled(getDefaultSharedPreferences(context))) {
            AlarmService.stopAlarmWithSingleVibration(context, instance);
        }

        // Update alarm in database
        final ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.PREDISMISSED_STATE;
        AlarmInstance.updateInstance(contentResolver, instance);

        // Setup instance notification and scheduling timers
        AlarmNotifications.clearNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getAlarmTime(), instance, AlarmInstance.DISMISSED_STATE);

        // Check parent if it needs to reschedule, disable or delete itself
        if (instance.mAlarmId != null) {
            updateParentAlarm(context, instance);
        }

        cancelPowerOffAlarm(context, instance);
        updateNextAlarm(context);
    }

    /**
     * This just sets the alarm instance to DISMISSED_STATE.
     */
    public static void setDismissState(Context context, AlarmInstance instance) {
        LogUtils.i("Setting dismissed state to instance " + instance.mId);
        instance.mAlarmState = AlarmInstance.DISMISSED_STATE;
        final ContentResolver contentResolver = context.getContentResolver();
        AlarmInstance.updateInstance(contentResolver, instance);

        cancelPowerOffAlarm(context, instance);
    }

    /**
     * This will delete the alarm instance, update the application notifications, and schedule
     * any state changes that need to occur in the future.
     *
     * @param context  application context
     * @param instance to set state to
     */
    public static void deleteInstanceAndUpdateParent(Context context, AlarmInstance instance) {
        LogUtils.i("Deleting instance " + instance.mId + " and updating parent alarm.");

        // Stop alarm if this instance is firing it; a single vibration will be performed if enabled in settings
        // to indicate that the alarm is correctly dismissed.
        if (SettingsDAO.areSnoozedOrDismissedAlarmVibrationsEnabled(getDefaultSharedPreferences(context))) {
            AlarmService.stopAlarmWithSingleVibration(context, instance);
        } else {
            // Stop alarm if this instance is firing it
            AlarmService.stopAlarm(context, instance);
        }

        // Remove all other timers and notifications associated to it
        unregisterInstance(context, instance);

        // Check parent if it needs to reschedule, disable or delete itself
        if (instance.mAlarmId != null) {
            updateParentAlarm(context, instance);
        }

        // Delete instance as it is not needed anymore
        AlarmInstance.deleteInstance(context.getContentResolver(), instance.mId);

        // Instance is not valid anymore, so find next alarm that will fire and notify system
        updateNextAlarm(context);
    }

    /**
     * This will set the instance state to DISMISSED_STATE and remove its notifications and
     * alarm timers.
     *
     * @param context  application context
     * @param instance to unregister
     */
    public static void unregisterInstance(Context context, AlarmInstance instance) {
        LogUtils.i("Unregistering instance " + instance.mId);
        AlarmNotifications.clearNotification(context, instance);
        cancelScheduledInstanceStateChange(context, instance);
        setDismissState(context, instance);
    }

    /**
     * This registers the AlarmInstance to the state manager. This will look at the instance
     * and choose the most appropriate state to put it in. This is primarily used by new
     * alarms, but it can also be called when the system time changes.
     * <p>
     * Most state changes are handled by the states themselves, but during major time changes we
     * have to correct the alarm instance state. This means we have to handle special cases as
     * describe below:
     *
     * <ul>
     *     <li>Make sure all dismissed alarms are never re-activated</li>
     *     <li>Make sure pre-dismissed alarms stay predismissed</li>
     *     <li>Make sure firing alarms stayed fired unless they should be auto-silenced</li>
     *     <li>Missed instance that have parents should be re-enabled if we went back in time</li>
     *     <li>If alarm was SNOOZED, then show the notification but don't update time</li>
     *     <li>If low priority notification was hidden, then make sure it stays hidden</li>
     * </ul>
     * <p>
     * If none of these special case are found, then we just check the time and see what is the
     * proper state for the instance.
     *
     * @param context  application context
     * @param instance to register
     */
    public static void registerInstance(Context context, AlarmInstance instance, boolean updateNextAlarm) {
        LogUtils.i("Registering instance: " + instance.mId);
        final ContentResolver cr = context.getContentResolver();
        final Alarm alarm = Alarm.getAlarm(cr, instance.mAlarmId);
        final Calendar currentTime = getCurrentTime();
        final Calendar alarmTime = instance.getAlarmTime();
        final Calendar timeoutTime = instance.getTimeout(context);
        final Calendar notificationTime = instance.getNotificationTime(context);
        final Calendar missedTTL = instance.getMissedTimeToLive();

        // Handle special use cases here
        if (instance.mAlarmState == AlarmInstance.DISMISSED_STATE) {
            // This should never happen, but add a quick check here
            LogUtils.e("Alarm Instance is dismissed, but never deleted");
            deleteInstanceAndUpdateParent(context, instance);
            return;
        } else if (instance.mAlarmState == AlarmInstance.FIRED_STATE) {
            // Keep alarm firing, unless it should be timed out
            boolean hasTimeout = timeoutTime != null && currentTime.after(timeoutTime);
            if (!hasTimeout) {
                setFiredState(context, instance);
                return;
            }
        } else if (instance.mAlarmState == AlarmInstance.MISSED_STATE) {
            if (currentTime.before(alarmTime)) {
                if (instance.mAlarmId == null) {
                    LogUtils.i("Cannot restore missed instance for one-time alarm");
                    // This instance parent got deleted (ie. deleteAfterUse), so we should not re-activate it.
                    deleteInstanceAndUpdateParent(context, instance);
                    return;
                }

                // TODO: This will re-activate missed snoozed alarms, but will
                //  use our normal notifications. This is not ideal, but very rare use-case.
                //  We should look into fixing this in the future.

                // Make sure we re-enable the parent alarm of the instance
                // because it will get activated by by the below code
                Objects.requireNonNull(alarm).enabled = true;
                Alarm.updateAlarm(cr, alarm);
            }
        } else if (instance.mAlarmState == AlarmInstance.PREDISMISSED_STATE) {
            if (currentTime.before(alarmTime)) {
                setPreDismissState(context, instance);
            } else {
                deleteInstanceAndUpdateParent(context, instance);
            }
            return;
        }

        // Fix states that are time sensitive
        if (currentTime.after(missedTTL)) {
            // Alarm is so old, just dismiss it
            deleteInstanceAndUpdateParent(context, instance);
        } else if (currentTime.after(alarmTime)) {
            // There is a chance that the TIME_SET occurred right when the alarm should go off, so
            // we need to add a check to see if we should fire the alarm instead of marking it missed.
            Calendar alarmBuffer = Calendar.getInstance();
            alarmBuffer.setTime(alarmTime.getTime());
            alarmBuffer.add(Calendar.SECOND, ALARM_FIRE_BUFFER);
            if (currentTime.before(alarmBuffer)) {
                setFiredState(context, instance);
            } else {
                setMissedState(context, instance);
            }
        } else if (instance.mAlarmState == AlarmInstance.SNOOZE_STATE) {
            // We only want to display snooze notification and not update the time,
            // so handle showing the notification directly
            AlarmNotifications.showSnoozeNotification(context, instance);
            scheduleInstanceStateChange(context, instance.getAlarmTime(), instance, AlarmInstance.FIRED_STATE);
        } else if (currentTime.after(notificationTime)) {
            setNotificationState(context, instance);
        } else {
            // Alarm is still active, so initialize as a silent alarm
            setSilentState(context, instance);
        }

        // The caller prefers to handle updateNextAlarm for optimization
        if (updateNextAlarm) {
            updateNextAlarm(context);
        }
    }

    /**
     * This will delete and unregister all instances associated with alarmId, without affect
     * the alarm itself. This should be used whenever modifying or deleting an alarm.
     *
     * @param context application context
     * @param alarmId to find instances to delete.
     */
    public static void deleteAllInstances(Context context, long alarmId) {
        LogUtils.i("Deleting all instances of alarm: " + alarmId);
        ContentResolver cr = context.getContentResolver();
        List<AlarmInstance> instances = AlarmInstance.getInstancesByAlarmId(cr, alarmId);
        for (AlarmInstance instance : instances) {
            unregisterInstance(context, instance);
            AlarmInstance.deleteInstance(context.getContentResolver(), instance.mId);
        }
        updateNextAlarm(context);
    }

    /**
     * Fix and update all alarm instance when a time change event occurs.
     *
     * @param context application context
     */
    public static void fixAlarmInstances(Context context) {
        LogUtils.i("Fixing alarm instances");
        // Register all instances after major time changes or when phone restarts
        final ContentResolver contentResolver = context.getContentResolver();
        final Calendar currentTime = getCurrentTime();

        // Sort the instances in reverse chronological order so that later instances are fixed or deleted
        // before re-scheduling prior instances (which may re-create or update the later instances).
        final List<AlarmInstance> instances = AlarmInstance.getInstances(contentResolver, null);
        Collections.sort(instances, (lhs, rhs) -> rhs.getAlarmTime().compareTo(lhs.getAlarmTime()));

        for (AlarmInstance instance : instances) {
            final Alarm alarm = Alarm.getAlarm(contentResolver, instance.mAlarmId);
            if (alarm == null) {
                unregisterInstance(context, instance);
                AlarmInstance.deleteInstance(contentResolver, instance.mId);
                LogUtils.e("Found instance without matching alarm; deleting instance %s", instance);
                continue;
            }
            final Calendar priorAlarmTime = alarm.getPreviousAlarmTime(instance.getAlarmTime());
            final Calendar missedTTLTime = instance.getMissedTimeToLive();
            if (currentTime.before(priorAlarmTime) || currentTime.after(missedTTLTime)) {
                final Calendar oldAlarmTime = instance.getAlarmTime();
                final Calendar newAlarmTime = alarm.getNextAlarmTime(currentTime);
                final CharSequence oldTime = DateFormat.format("MM/dd/yyyy hh:mm a", oldAlarmTime);
                final CharSequence newTime = DateFormat.format("MM/dd/yyyy hh:mm a", newAlarmTime);
                LogUtils.i("A time change has caused an existing alarm scheduled to fire at %s to" +
                        " be replaced by a new alarm scheduled to fire at %s", oldTime, newTime);

                // The time change is so dramatic the AlarmInstance doesn't make any sense;
                // remove it and schedule the new appropriate instance.
                AlarmStateManager.deleteInstanceAndUpdateParent(context, instance);
            } else {
                registerInstance(context, instance, false);
            }
        }

        updateNextAlarm(context);
    }

    /**
     * Utility method to set alarm instance state via constants.
     *
     * @param context  application context
     * @param instance to change state on
     * @param state    to change to
     */
    private static void setAlarmState(Context context, AlarmInstance instance, int state) {
        if (instance == null) {
            LogUtils.e("Null alarm instance while setting state to %d", state);
            return;
        }
        switch (state) {
            case AlarmInstance.SILENT_STATE -> setSilentState(context, instance);
            case AlarmInstance.NOTIFICATION_STATE -> setNotificationState(context, instance);
            case AlarmInstance.FIRED_STATE -> setFiredState(context, instance);
            case AlarmInstance.SNOOZE_STATE -> setSnoozeState(context, instance, true);
            case AlarmInstance.MISSED_STATE -> setMissedState(context, instance);
            case AlarmInstance.PREDISMISSED_STATE -> setPreDismissState(context, instance);
            case AlarmInstance.DISMISSED_STATE -> deleteInstanceAndUpdateParent(context, instance);
            default -> LogUtils.e("Trying to change to unknown alarm state: " + state);
        }
    }

    public static void handleIntent(Context context, Intent intent) {
        final String action = intent.getAction();
        LogUtils.v("AlarmStateManager received intent " + intent);
        if (CHANGE_STATE_ACTION.equals(action)) {
            Uri uri = intent.getData();
            AlarmInstance instance = AlarmInstance.getInstance(context.getContentResolver(), AlarmInstance.getId(uri));
            if (instance == null) {
                LogUtils.e("Can not change state for unknown instance: " + uri);
                return;
            }

            int globalId = SettingsDAO.getGlobalIntentId(getDefaultSharedPreferences(context));
            int intentId = intent.getIntExtra(ALARM_GLOBAL_ID_EXTRA, -1);
            int alarmState = intent.getIntExtra(ALARM_STATE_EXTRA, -1);
            if (intentId != globalId) {
                LogUtils.i("IntentId: " + intentId + " GlobalId: " + globalId + " AlarmState: " + alarmState);
                // Allows dismiss/snooze requests to go through
                if (!intent.hasCategory(ALARM_DISMISS_TAG) && !intent.hasCategory(ALARM_SNOOZE_TAG)) {
                    LogUtils.i("Ignoring old Intent");
                    return;
                }
            }

            if (intent.getBooleanExtra(FROM_NOTIFICATION_EXTRA, false)) {
                if (intent.hasCategory(ALARM_DISMISS_TAG)) {
                    Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_notification);
                } else if (intent.hasCategory(ALARM_SNOOZE_TAG)) {
                    Events.sendAlarmEvent(R.string.action_snooze, R.string.label_notification);
                }
            }

            if (alarmState >= 0) {
                setAlarmState(context, instance, alarmState);
            } else {
                registerInstance(context, instance, true);
            }
        } else if (SHOW_AND_DISMISS_ALARM_ACTION.equals(action)) {
            Uri uri = intent.getData();
            AlarmInstance instance = AlarmInstance.getInstance(context.getContentResolver(), AlarmInstance.getId(uri));

            if (instance == null) {
                LogUtils.e("Null AlarmInstance for SHOW_AND_DISMISS");
                // Dismiss the notification
                final int id = intent.getIntExtra(AlarmNotifications.EXTRA_NOTIFICATION_ID, -1);
                if (id != -1) {
                    NotificationManagerCompat.from(context).cancel(id);
                }
                return;
            }

            long alarmId = instance.mAlarmId == null ? Alarm.INVALID_ID : instance.mAlarmId;
            final Intent viewAlarmIntent = Alarm.createIntent(context, DeskClock.class, alarmId)
                    .putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, alarmId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Open DeskClock which is now positioned on the alarms tab.
            context.startActivity(viewAlarmIntent);

            deleteInstanceAndUpdateParent(context, instance);
        }
    }

    /**
     * Creates an intent that can be used to set an AlarmManager alarm to set the next alarm
     * indicators.
     */
    public static Intent createIndicatorIntent(Context context) {
        return new Intent(context, AlarmStateManager.class).setAction(INDICATOR_ACTION);
    }

    private static void setPowerOffAlarm(Context context, AlarmInstance instance) {
        LogUtils.i("Set next power off alarm : instance id " + instance.mId);
        Intent intent = new Intent(ACTION_SET_POWEROFF_ALARM);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setPackage(POWER_OFF_ALARM_PACKAGE);
        intent.putExtra(TIME, instance.getAlarmTime().getTimeInMillis());
        context.sendBroadcast(intent);
    }

    private static void cancelPowerOffAlarm(Context context, AlarmInstance instance) {
        Intent intent = new Intent(ACTION_CANCEL_POWEROFF_ALARM);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(TIME, instance.getAlarmTime().getTimeInMillis());
        intent.setPackage(POWER_OFF_ALARM_PACKAGE);
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (INDICATOR_ACTION.equals(intent.getAction())) {
            return;
        }

        final PendingResult result = goAsync();
        final PowerManager.WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire(10000L);
        AsyncHandler.post(() -> {
            handleIntent(context, intent);
            result.finish();
            wl.release();
        });
    }

    /**
     * Abstracts away how state changes are scheduled. The {@link AlarmManagerStateChangeScheduler}
     * implementation schedules callbacks within the system AlarmManager. Alternate
     * implementations, such as test case mocks can subvert this behavior.
     */
    interface StateChangeScheduler {
        void scheduleInstanceStateChange(Context context, Calendar time, AlarmInstance instance, int newState);

        void cancelScheduledInstanceStateChange(Context context, AlarmInstance instance);
    }

    /**
     * Schedules state change callbacks within the AlarmManager.
     */
    private static class AlarmManagerStateChangeScheduler implements StateChangeScheduler {
        @Override
        public void scheduleInstanceStateChange(Context context, Calendar time, AlarmInstance instance, int newState) {
            final long timeInMillis = time.getTimeInMillis();
            LogUtils.i("Scheduling state change %d to instance %d at %s (%d)", newState,
                    instance.mId, AlarmUtils.getFormattedTime(context, time), timeInMillis);
            final Intent stateChangeIntent = createStateChangeIntent(context, ALARM_MANAGER_TAG, instance, newState);
            // Treat alarm state change as high priority, use foreground broadcasts
            stateChangeIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            PendingIntent pendingIntent = PendingIntent.getService(context, instance.hashCode(),
                    stateChangeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            final AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            // Ensure the alarm fires even if the device is dozing.
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }

        @Override
        public void cancelScheduledInstanceStateChange(Context context, AlarmInstance instance) {
            LogUtils.v("Canceling instance " + instance.mId + " timers");

            // Create a PendingIntent that will match any one set for this instance
            PendingIntent pendingIntent = PendingIntent.getService(context, instance.hashCode(),
                    createStateChangeIntent(context, ALARM_MANAGER_TAG, instance, null),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

            if (pendingIntent != null) {
                AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
                am.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }
}
