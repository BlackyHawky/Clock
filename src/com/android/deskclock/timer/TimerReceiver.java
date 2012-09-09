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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.Log;

public class TimerReceiver extends BroadcastReceiver {
    private static final String TAG = "TimerReceiver";

     @Override
    public void onReceive(final Context context, final Intent intent) {

         Timer timer;
         Log.d(TAG, " got intent");

         if (intent.hasExtra(Timer.TIMER_INTENT_EXTRA)) {
             // Get the alarm out of the Intent
             timer = intent.getParcelableExtra(Timer.TIMER_INTENT_EXTRA);
         } else {
             // No data to work with, do nothing
             Log.d(TAG, " got intent without Timer data");
             return;
         }

        NotificationManager nm =
                (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

         if (Timer.START_TIMER.equals(intent.getAction())) {
             // Start intent is send by the timer activity, show timer notification and set an alarm
             Log.d(TAG," got start intent");
             PendingIntent i = createTimerActivityIntent(context, timer);
             nm.notify(timer.mTimerId, buildTimerNotification(context, timer, false, i));

         } else if (Timer.CANCEL_TIMER.equals(intent.getAction())) {
             // Cancel intent can be sent by either the app or from the notification
             // Remove notification, cancel alarm and tell timer app the timer was canceled,
             //
             Log.d(TAG ," got cancel intent");
             nm.cancel(timer.mTimerId);

         } else if (Timer.TIMES_UP.equals(intent.getAction())) {
             // Times up comes as an alarm notification, update the notification, timer activity and
             // play the alarm ring tone.
             Log.d(TAG," got timesup intent");


         } else if (Timer.TIMER_RESET.equals(intent.getAction())) {
             // Reset can come with the times up notification is swiped or from the activity
             // Remove the notification, stop playing the alarm, tell timer activity to reset
         }
    }


     private static PendingIntent createTimerActivityIntent(Context context, Timer t) {
         Intent clickIntent = new Intent();
     //    clickIntent.setClass(context, TimerActivity.class);
         clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         clickIntent.putExtra(Timer.TIMER_INTENT_EXTRA, t);
         return PendingIntent.getActivity(context, 0, clickIntent,
                     PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
     }

    private static Notification buildTimerNotification(
            Context context, Timer t, Boolean onGoing, PendingIntent contentIntent) {
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
}