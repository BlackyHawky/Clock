/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.deskclock.timer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.TimerRingService;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;

import java.util.ArrayList;
import java.util.Iterator;

public class TimerReceiver extends BroadcastReceiver {
    private static final String TAG = "TimerReceiver";

    // Make this a large number to avoid the alarm ID's which seem to be 1, 2, ...
    // Must also be different than StopwatchService.NOTIFICATION_ID
    private static final int IN_USE_NOTIFICATION_ID = Integer.MAX_VALUE - 2;

    // There will only be one times up timer notification at a time.
    private static final int TIMER_TIMESUP_NOTIFICATION_ID = Integer.MAX_VALUE - 3;

    private static final int NO_SINGLE_RUNNING_TIMER = Integer.MAX_VALUE - 4;

    ArrayList<TimerObj> mTimers;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        LogUtils.v(TAG, "Received intent " + intent.toString());

        String actionType = intent.getAction();
        // This action does not need the timers data
        if (Timers.NOTIF_IN_USE_CANCEL.equals(actionType)) {
            cancelInUseNotification(context);
            return;
        }

        // Get the updated timers data.
        if (mTimers == null) {
            mTimers = new ArrayList<>();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        TimerObj.getTimersFromSharedPrefs(prefs, mTimers);

        // These actions do not provide a timer ID, but do use the timers data
        switch (actionType) {
            case Timers.NOTIF_IN_USE_SHOW:
                showInUseNotification(context);
                return;
            case Timers.NOTIF_UPDATE:
                updateTimesUpNotification(context);
                return;
            case Timers.NOTIF_RESET_ALL_TIMERS:
                resetAllTimers(context, prefs);
                return;
        }

        // Remaining actions provide a timer Id
        if (!intent.hasExtra(Timers.TIMER_INTENT_EXTRA)) {
            // No data to work with, do nothing
            LogUtils.e(TAG, "Got intent without Timer data");
            return;
        }

        // Get the timer out of the Intent
        int timerId = intent.getIntExtra(Timers.TIMER_INTENT_EXTRA, -1);
        if (timerId == -1) {
            LogUtils.d(TAG, "OnReceive:intent without Timer data for " + actionType);
        }

        TimerObj t = Timers.findTimer(mTimers, timerId);

        // Actions from in-use notifications that control a single active but not firing timer.
        // If these are being used, we must not be in the app
        switch (actionType) {
            case Timers.NOTIF_PAUSE_TIMER:
                pauseTimer(context, prefs, t);
                return;
            case Timers.NOTIF_PLUS_ONE_TIMER:
                plusOneTimer(context, prefs, t);
                return;
            case Timers.NOTIF_RESUME_TIMER:
                resumeTimer(context, prefs, t);
                return;
            case Timers.NOTIF_RESET_TIMER:
                resetSingleTimer(context, prefs, t);
                return;
        }

        if (Timers.TIMES_UP.equals(actionType)) {
            // Find the timer (if it doesn't exist, it was probably deleted).
            if (t == null) {
                LogUtils.d(TAG, "Timer not found in list - do nothing");
                return;
            }

            // Perform an extra check just to verify that time is actually up.
            if (t.getTimesupTime() > Utils.getTimeNow()) {
                LogUtils.i(TAG, "Times up time is still in the future; not firing.");
                return;
            }

            t.setState(TimerObj.STATE_TIMESUP);
            t.writeToSharedPref(prefs);
            Events.sendEvent(R.string.category_timer, R.string.action_fire, 0);

            // Play ringtone by using TimerRingService service with a default alarm.
            LogUtils.d(TAG, "Playing timer ringtone");
            Intent si = new Intent();
            si.setClass(context, TimerRingService.class);
            context.startService(si);

            // Update the in-use notification
            if (getNextRunningTimer(mTimers, false, Utils.getTimeNow()) == null) {
                // Found no running timers.
                cancelInUseNotification(context);
            } else {
                showInUseNotification(context);
            }

            // Flag to tell DeskClock to re-sync with the database. Important if the timer
            // expires while we are still on the timerfragment page; we need to update the fab.
            prefs.edit().putBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, true).apply();
            updateTimesUpNotification(context);
        } else if (Timers.RESET_TIMER.equals(actionType)
                || Timers.DELETE_TIMER.equals(actionType)
                || Timers.TIMER_DONE.equals(actionType)) {
            // Stop Ringtone if all timers are not in times-up status
            stopRingtoneIfNoTimesup(context);
            updateTimesUpNotification(context);
        } else if (Timers.NOTIF_TIMES_UP_STOP.equals(actionType)) {
            if (intent.getBooleanExtra(Timers.NOTIF_STOP_ALL_TIMERS, false)) {
                LogUtils.d(TAG, "Stopping all timers");
                for (TimerObj timesUpTimer : Timers.timersInTimesUp(mTimers)) {
                    stopTimer(prefs, timesUpTimer);
                }
            } else {
                stopTimer(prefs, t);
            }
            stopRingtoneIfNoTimesup(context);
            // Flag to tell DeskClock to re-sync with the database. Important if the HUN stop is
            // clicked while TimerFragment is open.
            prefs.edit().putBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, true).apply();
            updateTimesUpNotification(context);
        } else if (Timers.NOTIF_TIMES_UP_PLUS_ONE.equals(actionType)) {
            // Find the timer (if it doesn't exists, it was probably deleted).
            if (t == null) {
                LogUtils.d(TAG, "Timer to +1m not found in list - do nothing");
                return;
            } else if (t.mState != TimerObj.STATE_TIMESUP) {
                LogUtils.d(TAG, "Action to +1m but timer not in times up state - do nothing");
                return;
            }

            // Restarting the timer with 1 minute left.
            t.setState(TimerObj.STATE_RUNNING);
            t.mStartTime = Utils.getTimeNow();
            t.mTimeLeft = t. mOriginalLength = TimerObj.MINUTE_IN_MILLIS;
            t.writeToSharedPref(prefs);

            // Flag to tell DeskClock to re-sync with the database
            prefs.edit().putBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, true).apply();

            // If the app is not open, refresh the in-use notification
            if (!prefs.getBoolean(Timers.NOTIF_APP_OPEN, false)) {
                showInUseNotification(context);
            }

            // Stop Ringtone if no timers are in times-up status
            stopRingtoneIfNoTimesup(context);
            updateTimesUpNotification(context);
        } else if (Timers.TIMER_UPDATE.equals(actionType)) {
            // Find the timer (if it doesn't exists, it was probably deleted).
            if (t == null) {
                LogUtils.d(TAG, "Timer to update not found in list - do nothing");
                return;
            }
            updateTimesUpNotification(context);
        } else {
            // In this case, we do not want to update the times up notification because any other
            // actions (such as updating timer state) do not change the set of Times Up timers,
            // so that would result in a distracting and unnnecessary refresh of the notification.
        }
        if (intent.getBooleanExtra(Timers.UPDATE_NEXT_TIMESUP, true)) {
            // Update the next "Times up" alarm unless explicitly told not to.
            updateNextTimesup(context);
        }
    }

    private void pauseTimer(Context context, SharedPreferences prefs, TimerObj t) {
        t.setState(TimerObj.STATE_STOPPED);
        t.writeToSharedPref(prefs);
        t.updateTimeLeft(true);

        updateNextTimesup(context);

        // Update the in use notification.
        showInUseNotification(context);

        Events.sendTimerEvent(R.string.action_stop, R.string.label_notification);
    }

    private void plusOneTimer(Context context, SharedPreferences prefs, TimerObj t) {
        t.setState(TimerObj.STATE_RUNNING);
        t.addTime(TimerObj.MINUTE_IN_MILLIS);
        t.updateTimeLeft(true);
        t.writeToSharedPref(prefs);

        updateNextTimesup(context);

        // Update the in use notification.
        showInUseNotification(context);

        Events.sendTimerEvent(R.string.action_add_minute, R.string.label_notification);
    }

    private void resumeTimer(Context context, SharedPreferences prefs, TimerObj t) {
        t.setState(TimerObj.STATE_RUNNING);
        t.updateTimeLeft(true);
        t.writeToSharedPref(prefs);

        updateNextTimesup(context);

        // Update the in use notification.
        showInUseNotification(context);

        Events.sendTimerEvent(R.string.action_resume, R.string.label_notification);
    }

    private void resetTimer(SharedPreferences prefs, TimerObj t) {
        t.setState(TimerObj.STATE_RESTART);
        t.mTimeLeft = t.mOriginalLength = t.mSetupLength;
        t.writeToSharedPref(prefs);
    }

    private void resetSingleTimer(Context context, SharedPreferences prefs, TimerObj t) {
        resetTimer(prefs, t);

        updateNextTimesup(context);

        // Remove the in use notification.
        cancelInUseNotification(context);

        Events.sendTimerEvent(R.string.action_reset, R.string.label_notification);
    }

    private void resetAllTimers(Context context, SharedPreferences prefs) {
        for (TimerObj t: Timers.timersInUse(mTimers)) {
            resetTimer(prefs, t);
        }
        updateNextTimesup(context);

        // Remove the in use notification.
        cancelInUseNotification(context);

        Events.sendTimerEvent(R.string.action_reset, R.string.label_notification);
    }


    private void stopTimer(SharedPreferences prefs, TimerObj t) {
        LogUtils.i(TAG, "Stopping timer id: " + t.mTimerId);
        if (t == null) {
            LogUtils.i(TAG, "Timer to stop not found in list - do nothing");
            return;
        } else if (t.mState != TimerObj.STATE_TIMESUP) {
            LogUtils.i(TAG, "Action to stop but timer not in times-up state - do nothing");
            return;
        }

        if (t.mDeleteAfterUse) {
            t.deleteFromSharedPref(prefs);
            Events.sendTimerEvent(R.string.action_delete, R.string.label_notification);
        } else {
            t.setState(TimerObj.STATE_RESTART);
            t.mOriginalLength = t.mSetupLength;
            t.mTimeLeft = t.mSetupLength;
            Events.sendTimerEvent(R.string.action_reset, R.string.label_notification);
            t.writeToSharedPref(prefs);
        }
    }

    private void stopRingtoneIfNoTimesup(final Context context) {
        if (Timers.findExpiredTimer(mTimers) == null) {
            // Stop ringtone
            LogUtils.d(TAG, "Stopping timer ringtone");
            Intent si = new Intent();
            si.setClass(context, TimerRingService.class);
            context.stopService(si);
        }
    }

    // Scan all timers and find the one that will expire next.
    // Tell AlarmManager to send a "Time's up" message to this receiver when this timer expires.
    // If no timer exists, clear "time's up" message.
    private void updateNextTimesup(Context context) {
        TimerObj t = getNextRunningTimer(mTimers, false, Utils.getTimeNow());
        long nextTimesup = (t == null) ? -1 : t.getTimesupTime();
        int timerId = (t == null) ? -1 : t.mTimerId;

        Intent intent = new Intent();
        intent.setAction(Timers.TIMES_UP);
        intent.setClass(context, TimerReceiver.class);
        // Time-critical, should be foreground
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        if (!mTimers.isEmpty()) {
            intent.putExtra(Timers.TIMER_INTENT_EXTRA, timerId);
        }
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final PendingIntent p = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        if (t != null) {
            if (Utils.isMOrLater()) {
                // Make sure we fire the timer even when device is in doze mode.
                // The timer is not guaranteed to fire at the exact time. It can have up to 15
                // minutes delay.
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextTimesup, p);
                LogUtils.d(TAG, "Setting times up to (approximately) " + nextTimesup);
            } else {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextTimesup, p);
                LogUtils.d(TAG, "Setting times up to (exactly) " + nextTimesup);
            }
        } else {
            // if no timer is found Pending Intents should be canceled
            // to keep the internal state consistent with the UI
            am.cancel(p);
            p.cancel();
            LogUtils.v(TAG, "No next times up");
        }
    }

    private void showInUseNotification(final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean appOpen = prefs.getBoolean(Timers.NOTIF_APP_OPEN, false);
        ArrayList<TimerObj> timersInUse = Timers.timersInUse(mTimers);
        int numTimersInUse = timersInUse.size();

        if (appOpen || numTimersInUse == 0) {
            return;
        }

        String title, contentText;
        Long nextBroadcastTime = null;
        long now = Utils.getTimeNow();
        // This is the TimerObj corresponding to the single in use timer when there is only one.
        // It is null when there are multiple timers in use.
        TimerObj inUseTimer = null;
        if (timersInUse.size() == 1) {
            final TimerObj timer = timersInUse.get(0);
            boolean timerIsTicking = timer.isTicking();
            String label = timer.getLabelOrDefault(context);
            title = timerIsTicking ? label : context.getString(R.string.timer_paused);
            long timeLeft = timerIsTicking ? timer.getTimesupTime() - now : timer.mTimeLeft;
            contentText = buildTimeRemaining(context, timeLeft);
            if (timerIsTicking && timeLeft > TimerObj.MINUTE_IN_MILLIS) {
                nextBroadcastTime = getBroadcastTime(now, timeLeft);
            }
            inUseTimer = timer;
        } else {
            TimerObj timer = getNextRunningTimer(timersInUse, false, now);
            if (timer == null) {
                // No running timers.
                title = String.format(
                        context.getString(R.string.timers_stopped), numTimersInUse);
                contentText = context.getString(R.string.all_timers_stopped_notif);
            } else {
                // We have at least one timer running and other timers stopped.
                title = String.format(
                        context.getString(R.string.timers_in_use), numTimersInUse);
                long completionTime = timer.getTimesupTime();
                long timeLeft = completionTime - now;
                contentText = String.format(context.getString(R.string.next_timer_notif),
                        buildTimeRemaining(context, timeLeft));
                if (timeLeft <= TimerObj.MINUTE_IN_MILLIS) {
                    TimerObj timerWithUpdate = getNextRunningTimer(timersInUse, true, now);
                    if (timerWithUpdate != null) {
                        completionTime = timerWithUpdate.getTimesupTime();
                        timeLeft = completionTime - now;
                        nextBroadcastTime = getBroadcastTime(now, timeLeft);
                    }
                } else {
                    nextBroadcastTime = getBroadcastTime(now, timeLeft);
                }
            }
        }
        showCollapsedNotificationWithNext(context, title, contentText, nextBroadcastTime,
                inUseTimer);
    }

    private long getBroadcastTime(long now, long timeUntilBroadcast) {
        long seconds = timeUntilBroadcast / 1000;
        seconds = seconds - ( (seconds / 60) * 60 );
        return now + (seconds * 1000);
    }

    /**
     * Show collapsed timer notification and set up AlarmManager to update the notification when
     * the next timer update will occur.
     * @param inUseTimer is the single running timer TimerObj when there is only one and null when
     *                   there are multiple paused/running timers
     */
    private void showCollapsedNotificationWithNext(
            Context context, String title, String text, Long nextBroadcastTime,
            TimerObj inUseTimer) {
        final Intent activityIntent = new Intent(context, DeskClock.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX)
                .putExtra(Timers.FIRST_LAUNCH_FROM_API_CALL, true);

        // Scroll to an active timer.
        if (inUseTimer != null) {
            activityIntent.putExtra(Timers.SCROLL_TO_TIMER_ID, inUseTimer.mTimerId);
        } else {
            // Scroll to the first non-paused timer if there is one; otherwise, just pick the first
            // in use timer in the list.
            final ArrayList<TimerObj> inUseTimers = Timers.timersInUse(mTimers);
            int scrollId = inUseTimers.get(0).mTimerId;
            for (TimerObj t : inUseTimers) {
                if (t.mState == TimerObj.STATE_RUNNING) {
                    scrollId = t.mTimerId;
                    break;
                }
            }
            activityIntent.putExtra(Timers.SCROLL_TO_TIMER_ID, scrollId);
        }

        final PendingIntent pendingActivityIntent = PendingIntent.getActivity(context, 0,
                activityIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        showCollapsedNotification(context, title, text, NotificationCompat.PRIORITY_HIGH,
                pendingActivityIntent, IN_USE_NOTIFICATION_ID, inUseTimer);

        if (nextBroadcastTime == null) {
            return;
        }

        final Intent nextBroadcast = new Intent(Timers.NOTIF_IN_USE_SHOW);
        final PendingIntent pendingNextBroadcast =
                PendingIntent.getBroadcast(context, 0, nextBroadcast, 0);
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setExact(AlarmManager.ELAPSED_REALTIME, nextBroadcastTime, pendingNextBroadcast);
    }

    /**
     * Add appropriate actions to builder depending on timer state.
     */
    private static NotificationCompat.Builder getSingleTimerNotificationActions(Context context,
            TimerObj inUseTimer, NotificationCompat.Builder builder, int notificationId) {
        final boolean isPaused = !inUseTimer.isTicking();
        final int timerId = inUseTimer.mTimerId;

        if (isPaused) {
            // Paused single timer - show resume and reset.
            final Intent resumeIntent = new Intent(Timers.NOTIF_RESUME_TIMER)
                    .putExtra(Timers.TIMER_INTENT_EXTRA, timerId);
            final PendingIntent pendingResumeIntent = PendingIntent.getBroadcast(context,
                    notificationId, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            final Intent resetIntent = new Intent(Timers.NOTIF_RESET_TIMER)
                    .putExtra(Timers.TIMER_INTENT_EXTRA, timerId);
            final PendingIntent pendingResetIntent = PendingIntent.getBroadcast(context,
                    notificationId, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(R.drawable.ic_start_24dp,
                    context.getString(R.string.sw_resume_button), pendingResumeIntent);
            builder.addAction(R.drawable.ic_reset_24dp,
                    context.getString(R.string.sw_reset_button), pendingResetIntent);
        } else {
            // Running single timer - show pause and add minute.
            final Intent pauseIntent = new Intent(Timers.NOTIF_PAUSE_TIMER)
                    .putExtra(Timers.TIMER_INTENT_EXTRA, timerId);
            final PendingIntent pendingPauseIntent = PendingIntent.getBroadcast(context,
                    notificationId, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            final Intent addMinuteIntent = new Intent(Timers.NOTIF_PLUS_ONE_TIMER)
                    .putExtra(Timers.TIMER_INTENT_EXTRA, timerId);
            final PendingIntent pendingAddIntent = PendingIntent.getBroadcast(context,
                    notificationId, addMinuteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(R.drawable.ic_pause_24dp, context.getString(R.string.timer_pause),
                    pendingPauseIntent);
            builder.addAction(R.drawable.ic_add_24dp, context.getString(R.string.timer_plus_1_min),
                    pendingAddIntent);
        }
        return builder;
    }

    /**
     * If single running timer, show a notification with pause and +1 actions and clicking
     * the notification should take you to the specific timer in the list.
     * If single paused timer, show resume and reset options.
     * Otherwise (multiple running and/or paused timers) show  a "reset all" option.
     */
    private static void showCollapsedNotification(final Context context, String title, String text,
            int priority, PendingIntent pendingIntent, int notificationId, TimerObj inUseTimer) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setAutoCancel(false)
                .setContentTitle(title)
                .setContentText(text)
                .setDeleteIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(priority)
                .setShowWhen(false)
                .setSmallIcon(R.drawable.stat_notify_timer)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        if (inUseTimer != null) {
            getSingleTimerNotificationActions(context, inUseTimer, builder, notificationId);
        } else {
            final Intent resetAllIntent = new Intent(Timers.NOTIF_RESET_ALL_TIMERS);
            final PendingIntent pendingResetAllIntent = PendingIntent.getBroadcast(context,
                    notificationId, resetAllIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_reset_24dp,
                    context.getString(R.string.timer_reset_all), pendingResetAllIntent);
        }
        final Notification notification = builder.build();
        notification.contentIntent = pendingIntent;
        NotificationManagerCompat.from(context).notify(notificationId, notification);
    }

    private String buildTimeRemaining(Context context, long timeLeft) {
        if (timeLeft < 0) {
            // We should never be here...
            LogUtils.v(TAG, "Will not show notification for timer already expired.");
            return null;
        }

        long seconds, minutes, hours;
        seconds = timeLeft / 1000;
        minutes = seconds / 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 99) {
            hours = 0;
        }

        String minSeq = Utils.getNumberFormattedQuantityString(context, R.plurals.minutes,
                (int) minutes);

        String hourSeq = Utils.getNumberFormattedQuantityString(context, R.plurals.hours,
                (int) hours);

        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;
        int index = (dispHour ? 1 : 0) | (dispMinute ? 2 : 0);
        String[] formats = context.getResources().getStringArray(R.array.timer_notifications);
        return String.format(formats[index], hourSeq, minSeq);
    }

    private TimerObj getNextRunningTimer(
            ArrayList<TimerObj> timers, boolean requireNextUpdate, long now) {
        long nextTimesup = Long.MAX_VALUE;
        boolean nextTimerFound = false;
        Iterator<TimerObj> i = timers.iterator();
        TimerObj t = null;
        while(i.hasNext()) {
            TimerObj tmp = i.next();
            if (tmp.mState == TimerObj.STATE_RUNNING) {
                long timesupTime = tmp.getTimesupTime();
                long timeLeft = timesupTime - now;
                if (timesupTime < nextTimesup && (!requireNextUpdate || timeLeft > 60) ) {
                    nextTimesup = timesupTime;
                    nextTimerFound = true;
                    t = tmp;
                }
            }
        }
        if (nextTimerFound) {
            return t;
        } else {
            return null;
        }
    }

    public static void cancelInUseNotification(final Context context) {
        NotificationManagerCompat.from(context).cancel(IN_USE_NOTIFICATION_ID);
    }

    private void updateTimesUpNotification(final Context context) {
        final ArrayList<TimerObj> timesUpTimers =  Timers.timersInTimesUp(mTimers);

        if (timesUpTimers.isEmpty()) {
            LogUtils.i("No more timers are firing.");
            cancelTimesUpNotification(context);
        } else {
            updateTimesUpNotification(context, timesUpTimers);
        }
    }

    /**
     * Update Times up notification; can be for a single timer or multiple. Either way, the
     * notification id used will be TIMER_TIMESUP_NOTIFICATION_ID because there will only ever be
     * one times up notification at a time.
     *
     * @param context
     * @param timesUpTimers list of Timers in TIMES_UP state
     */
    private void updateTimesUpNotification(final Context context,
            ArrayList<TimerObj> timesUpTimers) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        final TimerObj timerObj = timesUpTimers.get(0);
        final int timerId = timerObj.mTimerId;
        final int timesUpTimersCount = timesUpTimers.size();
        // Whether this should be a combined notification for multiple timers.
        final boolean showCombined = timesUpTimersCount > 1;

        if (showCombined) {
            LogUtils.i("Showing combined times up notification for %d timers", timesUpTimersCount);
        } else {
            LogUtils.i("Showing times up notification for timer %d", timerId);
        }
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        // Content Intent. When clicked will show the timer full screen
        final PendingIntent contentIntent = PendingIntent.getActivity(context, timerId,
                new Intent(context, TimerAlertFullScreen.class).putExtra(
                        Timers.TIMER_INTENT_EXTRA, timerId),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Add stop/done action button
        final Intent newStopIntent = new Intent(Timers.NOTIF_TIMES_UP_STOP)
                        .putExtra(Timers.TIMER_INTENT_EXTRA, timerId);

        if (showCombined) {
            // Allow the stop button to stop all currently firing timers
            newStopIntent.putExtra(Timers.NOTIF_STOP_ALL_TIMERS, true);
        }

        LogUtils.v(TAG, "Setting heads-up notification for " + timesUpTimersCount + " timers");

        PendingIntent stopIntent = PendingIntent.getBroadcast(context, timerId, newStopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Notification creation
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentIntent(contentIntent)
                .setContentTitle(timerObj.getLabelOrDefault(context))
                .setContentText(context.getString(R.string.timer_times_up))
                .setSmallIcon(R.drawable.stat_notify_timer)
                .setLocalOnly(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setWhen(0);

        if (showCombined) {
            builder.addAction(R.drawable.ic_stop_24dp,
                    context.getString(R.string.timer_stop_all),
                    stopIntent)
                    .setContentText(context.getString(R.string.timer_multi_times_up,
                            timesUpTimersCount))
                    .setContentTitle(context.getString(R.string.timer_notification_label));
        } else {
            // Add one minute action button
            final PendingIntent addOneMinuteAction = PendingIntent.getBroadcast(context, timerId,
                    new Intent(Timers.NOTIF_TIMES_UP_PLUS_ONE)
                            .putExtra(Timers.TIMER_INTENT_EXTRA, timerId),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // If only one timer is firing, add the +1 button
            builder.addAction(timerObj.getDeleteAfterUse()
                                    ? android.R.drawable.ic_menu_close_clear_cancel
                                    : R.drawable.ic_stop_24dp,
                            timerObj.getDeleteAfterUse()
                                    ? context.getString(R.string.timer_done)
                                    : context.getString(R.string.timer_stop),
                            stopIntent)
                    .addAction(R.drawable.ic_add_24dp,
                    context.getString(R.string.timer_plus_1_min),
                    addOneMinuteAction)
                    .setContentTitle(timerObj.getLabelOrDefault(context))
                    .setContentText(context.getString(R.string.timer_times_up));
        }

        // set up full screen intent
        final Intent fullScreenIntent = new Intent(context, TimerAlertFullScreen.class);

        // set action, so we can be different than content pending intent
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        builder.setFullScreenIntent(PendingIntent.getActivity(context,
                timerObj.hashCode(), fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT), true);

        final Notification notification = builder.build();

        // Remove previous times up notifications so there is only ever one timesup notification
        cancelTimesUpNotification(context);

        nm.notify(TIMER_TIMESUP_NOTIFICATION_ID, notification);
    }

    private void cancelTimesUpNotification(final Context context) {
        NotificationManagerCompat.from(context).cancel(TIMER_TIMESUP_NOTIFICATION_ID);
    }
}
