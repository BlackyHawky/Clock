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
import android.util.Log;

import com.android.deskclock.DeskClock;
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

    ArrayList<TimerObj> mTimers;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (Timers.LOGGING) {
            Log.v(TAG, "Received intent " + intent.toString());
        }
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
        if (Timers.NOTIF_IN_USE_SHOW.equals(actionType)) {
            showInUseNotification(context);
            return;
        } else if (Timers.NOTIF_TIMES_UP_SHOW.equals(actionType)) {
            showTimesUpNotification(context);
            return;
        } else if (Timers.NOTIF_TIMES_UP_CANCEL.equals(actionType)) {
            cancelTimesUpNotification(context);
            return;
        }

        // Remaining actions provide a timer Id
        if (!intent.hasExtra(Timers.TIMER_INTENT_EXTRA)) {
            // No data to work with, do nothing
            Log.e(TAG, "got intent without Timer data");
            return;
        }

        // Get the timer out of the Intent
        int timerId = intent.getIntExtra(Timers.TIMER_INTENT_EXTRA, -1);
        if (timerId == -1) {
            Log.d(TAG, "OnReceive:intent without Timer data for " + actionType);
        }

        TimerObj t = Timers.findTimer(mTimers, timerId);

        if (Timers.TIMES_UP.equals(actionType)) {
            // Find the timer (if it doesn't exists, it was probably deleted).
            if (t == null) {
                Log.d(TAG, " timer not found in list - do nothing");
                return;
            }

            t.setState(TimerObj.STATE_TIMESUP);
            t.writeToSharedPref(prefs);
            Events.sendEvent(R.string.category_timer, R.string.action_fire, 0);

            // Play ringtone by using TimerRingService service with a default alarm.
            Log.d(TAG, "playing ringtone");
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

            // Start the TimerAlertFullScreen activity.
            Intent timersAlert = new Intent(context, TimerAlertFullScreen.class);
            timersAlert.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            context.startActivity(timersAlert);
        } else if (Timers.RESET_TIMER.equals(actionType)
                || Timers.DELETE_TIMER.equals(actionType)
                || Timers.TIMER_DONE.equals(actionType)) {
            // Stop Ringtone if all timers are not in times-up status
            stopRingtoneIfNoTimesup(context);

            if (t != null) {
                cancelTimesUpNotification(context, t);
            }
        } else if (Timers.NOTIF_TIMES_UP_STOP.equals(actionType)) {
            // Find the timer (if it doesn't exists, it was probably deleted).
            if (t == null) {
                Log.d(TAG, "timer to stop not found in list - do nothing");
                return;
            } else if (t.mState != TimerObj.STATE_TIMESUP) {
                Log.d(TAG, "action to stop but timer not in times-up state - do nothing");
                return;
            }

            // Update timer state
            t.setState(t.getDeleteAfterUse() ? TimerObj.STATE_DELETED : TimerObj.STATE_RESTART);
            t.mTimeLeft = t.mOriginalLength = t.mSetupLength;
            t.writeToSharedPref(prefs);

            // Flag to tell DeskClock to re-sync with the database
            prefs.edit().putBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, true).apply();

            cancelTimesUpNotification(context, t);

            // Done with timer - delete from data base
            if (t.getDeleteAfterUse()) {
                t.deleteFromSharedPref(prefs);
            }

            // Stop Ringtone if no timers are in times-up status
            stopRingtoneIfNoTimesup(context);
        } else if (Timers.NOTIF_TIMES_UP_PLUS_ONE.equals(actionType)) {
            // Find the timer (if it doesn't exists, it was probably deleted).
            if (t == null) {
                Log.d(TAG, "timer to +1m not found in list - do nothing");
                return;
            } else if (t.mState != TimerObj.STATE_TIMESUP) {
                Log.d(TAG, "action to +1m but timer not in times up state - do nothing");
                return;
            }

            // Restarting the timer with 1 minute left.
            t.setState(TimerObj.STATE_RUNNING);
            t.mStartTime = Utils.getTimeNow();
            t.mTimeLeft = t. mOriginalLength = TimerObj.MINUTE_IN_MILLIS;
            t.writeToSharedPref(prefs);

            // Flag to tell DeskClock to re-sync with the database
            prefs.edit().putBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, true).apply();

            cancelTimesUpNotification(context, t);

            // If the app is not open, refresh the in-use notification
            if (!prefs.getBoolean(Timers.NOTIF_APP_OPEN, false)) {
                showInUseNotification(context);
            }

            // Stop Ringtone if no timers are in times-up status
            stopRingtoneIfNoTimesup(context);
        } else if (Timers.TIMER_UPDATE.equals(actionType)) {
            // Find the timer (if it doesn't exists, it was probably deleted).
            if (t == null) {
                Log.d(TAG, " timer to update not found in list - do nothing");
                return;
            }

            // Refresh buzzing notification
            if (t.mState == TimerObj.STATE_TIMESUP) {
                // Must cancel the previous notification to get all updates displayed correctly
                cancelTimesUpNotification(context, t);
                showTimesUpNotification(context, t);
            }
        }
        if (intent.getBooleanExtra(Timers.UPDATE_NEXT_TIMESUP, true)) {
            // Update the next "Times up" alarm unless explicitly told not to.
            updateNextTimesup(context);
        }
    }

    private void stopRingtoneIfNoTimesup(final Context context) {
        if (Timers.findExpiredTimer(mTimers) == null) {
            // Stop ringtone
            Log.d(TAG, "stopping ringtone");
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
        AlarmManager mngr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent p = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        if (t != null) {
            if (Utils.isKitKatOrLater()) {
                mngr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextTimesup, p);
            } else {
                mngr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextTimesup, p);
            }
            if (Timers.LOGGING) {
                Log.d(TAG, "Setting times up to " + nextTimesup);
            }
        } else {
            // if no timer is found Pending Intents should be canceled
            // to keep the internal state consistent with the UI
            mngr.cancel(p);
            p.cancel();
            if (Timers.LOGGING) {
                Log.v(TAG, "no next times up");
            }
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
        if (timersInUse.size() == 1) {
            TimerObj timer = timersInUse.get(0);
            boolean timerIsTicking = timer.isTicking();
            String label = timer.getLabelOrDefault(context);
            title = timerIsTicking ? label : context.getString(R.string.timer_stopped);
            long timeLeft = timerIsTicking ? timer.getTimesupTime() - now : timer.mTimeLeft;
            contentText = buildTimeRemaining(context, timeLeft);
            if (timerIsTicking && timeLeft > TimerObj.MINUTE_IN_MILLIS) {
                nextBroadcastTime = getBroadcastTime(now, timeLeft);
            }
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
        showCollapsedNotificationWithNext(context, title, contentText, nextBroadcastTime);
    }

    private long getBroadcastTime(long now, long timeUntilBroadcast) {
        long seconds = timeUntilBroadcast / 1000;
        seconds = seconds - ( (seconds / 60) * 60 );
        return now + (seconds * 1000);
    }

    private void showCollapsedNotificationWithNext(
            final Context context, String title, String text, Long nextBroadcastTime) {
        Intent activityIntent = new Intent(context, DeskClock.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX);
        PendingIntent pendingActivityIntent = PendingIntent.getActivity(context, 0, activityIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        showCollapsedNotification(context, title, text, NotificationCompat.PRIORITY_HIGH,
                pendingActivityIntent, IN_USE_NOTIFICATION_ID, false);

        if (nextBroadcastTime == null) {
            return;
        }
        Intent nextBroadcast = new Intent();
        nextBroadcast.setAction(Timers.NOTIF_IN_USE_SHOW);
        PendingIntent pendingNextBroadcast =
                PendingIntent.getBroadcast(context, 0, nextBroadcast, 0);
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Utils.isKitKatOrLater()) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME, nextBroadcastTime, pendingNextBroadcast);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, nextBroadcastTime, pendingNextBroadcast);
        }
    }

    private static void showCollapsedNotification(final Context context, String title, String text,
            int priority, PendingIntent pendingIntent, int notificationId, boolean showTicker) {
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
        if (showTicker) {
            builder.setTicker(text);
        }

        final Notification notification = builder.build();
        notification.contentIntent = pendingIntent;
        NotificationManagerCompat.from(context).notify(notificationId, notification);
    }

    private String buildTimeRemaining(Context context, long timeLeft) {
        if (timeLeft < 0) {
            // We should never be here...
            Log.v(TAG, "Will not show notification for timer already expired.");
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

    private void showTimesUpNotification(final Context context) {
        for (TimerObj timerObj : Timers.timersInTimesUp(mTimers) ) {
            showTimesUpNotification(context, timerObj);
        }
    }

    private void showTimesUpNotification(final Context context, TimerObj timerObj) {
        // Content Intent. When clicked will show the timer full screen
        PendingIntent contentIntent = PendingIntent.getActivity(context, timerObj.mTimerId,
                new Intent(context, TimerAlertFullScreen.class).putExtra(
                        Timers.TIMER_INTENT_EXTRA, timerObj.mTimerId),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Add one minute action button
        PendingIntent addOneMinuteAction = PendingIntent.getBroadcast(context, timerObj.mTimerId,
                new Intent(Timers.NOTIF_TIMES_UP_PLUS_ONE)
                        .putExtra(Timers.TIMER_INTENT_EXTRA, timerObj.mTimerId),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Add stop/done action button
        PendingIntent stopIntent = PendingIntent.getBroadcast(context, timerObj.mTimerId,
                new Intent(Timers.NOTIF_TIMES_UP_STOP)
                        .putExtra(Timers.TIMER_INTENT_EXTRA, timerObj.mTimerId),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Notification creation
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_add_24dp,
                        context.getResources().getString(R.string.timer_plus_1_min),
                        addOneMinuteAction)
                .addAction(
                        timerObj.getDeleteAfterUse()
                                ? android.R.drawable.ic_menu_close_clear_cancel
                                : R.drawable.ic_stop_24dp,
                        timerObj.getDeleteAfterUse()
                                ? context.getResources().getString(R.string.timer_done)
                                : context.getResources().getString(R.string.timer_stop),
                        stopIntent)
                .setContentTitle(timerObj.getLabelOrDefault(context))
                .setContentText(context.getResources().getString(R.string.timer_times_up))
                .setSmallIcon(R.drawable.stat_notify_timer)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setWhen(0);

        // Send the notification using the timer's id to identify the
        // correct notification
        NotificationManagerCompat.from(context).notify(timerObj.mTimerId, builder.build());
        if (Timers.LOGGING) {
            Log.v(TAG, "Setting times-up notification for "
                    + timerObj.getLabelOrDefault(context) + " #" + timerObj.mTimerId);
        }
    }

    private void cancelTimesUpNotification(final Context context) {
        for (TimerObj timerObj : Timers.timersInTimesUp(mTimers) ) {
            cancelTimesUpNotification(context, timerObj);
        }
    }

    private void cancelTimesUpNotification(final Context context, TimerObj timerObj) {
        NotificationManagerCompat.from(context).cancel(timerObj.mTimerId);
        if (Timers.LOGGING) {
            Log.v(TAG, "Canceling times-up notification for "
                    + timerObj.getLabelOrDefault(context) + " #" + timerObj.mTimerId);
        }
    }
}
