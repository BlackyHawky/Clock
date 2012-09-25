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
import android.util.Log;

import com.android.deskclock.Alarm;
import com.android.deskclock.AlarmKlaxon;
import com.android.deskclock.Alarms;
import com.android.deskclock.DeskClock;
import com.android.deskclock.TimerRingService;

import java.util.ArrayList;
import java.util.Iterator;

public class TimerReceiver extends BroadcastReceiver {
    private static final String TAG = "TimerReceiver";

    ArrayList<TimerObj> mTimers;

    @Override
    public void onReceive(final Context context, final Intent intent) {

         int timer;
         Log.d(TAG, " got intent with action " + intent.getAction());

         if (intent.hasExtra(Timers.TIMER_INTENT_EXTRA)) {
             // Get the alarm out of the Intent
             timer = intent.getIntExtra(Timers.TIMER_INTENT_EXTRA, -1);
             if (timer == -1) {
                 Log.d(TAG, " got intent without Timer data");
             }
         } else {
             // No data to work with, do nothing
             Log.d(TAG, " got intent without Timer data");
             return;
         }


//        NotificationManager nm =
  //              (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

         // Get the updated timers data.
         if (mTimers == null) {
             mTimers = new ArrayList<TimerObj> ();
         }
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
         TimerObj.getTimersFromSharedPrefs(prefs, mTimers);

         // TODO: Update notifications

        if (Timers.TIMES_UP.equals(intent.getAction())) {
            // Find the timer (if it doesn't exists, it was probably deleted).
            TimerObj t = Timers.findTimer(mTimers, timer);
            if (t == null) {
                Log.d(TAG, " timer not found in list - do nothing");
                return;
            }

            t.mState = TimerObj.STATE_TIMESUP;
            t.writeToSharedPref(prefs);
            // Play ringtone by using AlarmKlaxon service with a default alarm.
            Log.d(TAG, "playing ringtone");
            Intent si = new Intent();
            si.setClass(context, TimerRingService.class);
            context.startService(si);

            // Start the DeskClock Activity
            Intent i = new Intent();
            i.setClass(context, DeskClock.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX);
            context.startActivity(i);

        } else if (Timers.TIMER_RESET.equals(intent.getAction())
                || Timers.DELETE_TIMER.equals(intent.getAction())
                || Timers.TIMER_DONE.equals(intent.getAction())) {
            // Stop Ringtone if all tiemrs are not in timesup status
            if (Timers.findExpiredTimer(mTimers) == null) {
                // Stop ringtone
                Log.d(TAG, "stopping ringtone");
                Intent si = new Intent();
                si.setClass(context, TimerRingService.class);
                context.stopService(si);
            }
        }
        // Update the next "Times up" alarm
        updateNextTimesup(context) ;
    }
     private static PendingIntent createTimerActivityIntent(Context context, TimerObj t, String action) {
         Intent i = new Intent();
         i.setClass(context, TimerReceiver.class);
         i.setAction(action);
         i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         i.putExtra(Timers.TIMER_INTENT_EXTRA, t);
         return PendingIntent.getActivity(context, 0, i,
                     PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
     }

    private static Notification buildTimerNotification(
            Context context, TimerObj t, Boolean onGoing, PendingIntent contentIntent) {
         Notification.Builder builder = new Notification.Builder(context);
         builder.setContentIntent(contentIntent);
         builder.setContentTitle("Timer");
         builder.setContentText("Now or Never");
//         builder.setDeleteIntent(null);  // what to do when the notification is cleared
         builder.setOngoing(onGoing);
//         builder.setSound(null);
         builder.setWhen(System.currentTimeMillis());
         builder.setTicker("Timer is here");
    //    builder.setSmallIcon(R.drawable.ic_clock_alarm_on);
  //       builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_clock_alarm_on));

         return builder.build();

     }

    // Scan all timers and find the one that will expire next.
    // Tell AlarmManager to send a "Time's up" message to this receiver when this timer expires.
    // If no timer exists, clear "time's up" message.
    private void updateNextTimesup(Context context) {
        long nextTimesup = Long.MAX_VALUE;
        boolean nextTimerfound = false;
        Iterator<TimerObj> i = mTimers.iterator();
        TimerObj t = null;
        while(i.hasNext()) {
            t = i.next();
            if (t.mState == TimerObj.STATE_RUNNING) {
                long timesupTime = t.getTimesupTime();
                if (timesupTime < nextTimesup) {
                    nextTimesup = timesupTime;
                    nextTimerfound = true;
                }
            }
        }

        Intent intent = new Intent();
        intent.setAction(Timers.TIMES_UP);
        intent.setClass(context, TimerReceiver.class);
        if (t != null) {
            intent.putExtra(Timers.TIMER_INTENT_EXTRA, t.mTimerId);
        }
        AlarmManager mngr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent p = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        if (nextTimerfound) {
            mngr.set(AlarmManager.RTC_WAKEUP, nextTimesup, p);
            Log.d(TAG,"Setting times up to " + nextTimesup);
        } else {
            Log.d(TAG,"canceling times up");
            mngr.cancel(p);
        }
    }

}