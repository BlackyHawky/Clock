/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.deskclock.alarms;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.Settings;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.DeskClock;
import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.util.Calendar;

public final class AlarmNotifications {
    // System intent action to notify that we change the alarm text.
    public static final String SYSTEM_ALARM_CHANGE_ACTION = "android.intent.action.ALARM_CHANGED";

    public static void broadcastNextAlarm(Context context, AlarmInstance instance)  {
        String timeString = "";
        boolean showStatusIcon = false;
        if (instance != null) {
            timeString = AlarmUtils.getFormattedTime(context, instance.getAlarmTime());
            showStatusIcon = true;
        }

        // Set and notify next alarm text to system
        Log.i("Displaying next alarm time: \'" + timeString + '\'');
        Settings.System.putString(context.getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED,
                timeString);
        Intent alarmChanged = new Intent(SYSTEM_ALARM_CHANGE_ACTION);
        alarmChanged.putExtra("alarmSet", showStatusIcon);
        context.sendBroadcast(alarmChanged);
    }

    public static void showLowPriorityNotification(Context context, AlarmInstance instance) {
        Log.v("Displaying low priority notification for alarm instance: " + instance.mId);
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Resources resources = context.getResources();
        Notification.Builder notification = new Notification.Builder(context)
                .setContentTitle(resources.getString(R.string.alarm_alert_predismiss_title))
                .setContentText(AlarmUtils.getAlarmText(context, instance))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setOngoing(false)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_DEFAULT);

        // Setup up hide notification
        Intent hideIntent = AlarmStateManager.createStateChangeIntent(context, "DELETE_TAG",
                instance, AlarmInstance.HIDE_NOTIFICATION_STATE);
        notification.setDeleteIntent(PendingIntent.getBroadcast(context, instance.hashCode(),
                hideIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup up dismiss action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG",
                instance, AlarmInstance.DISMISSED_STATE);
        notification.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                resources.getString(R.string.alarm_alert_dismiss_now_text),
                PendingIntent.getBroadcast(context, instance.hashCode(),
                        dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content action if instance is owned by alarm
        long alarmId = instance.mAlarmId == null ? Alarm.INVALID_ID : instance.mAlarmId;
        Intent viewAlarmIntent = Alarm.createIntent(context, DeskClock.class, alarmId);
        viewAlarmIntent.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
        viewAlarmIntent.putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, alarmId);
        viewAlarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(),
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showHighPriorityNotification(Context context, AlarmInstance instance) {
        Log.v("Displaying high priority notification for alarm instance: " + instance.mId);
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Resources resources = context.getResources();
        Notification.Builder notification = new Notification.Builder(context)
                .setContentTitle(resources.getString(R.string.alarm_alert_predismiss_title))
                .setContentText(AlarmUtils.getAlarmText(context, instance))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_HIGH);

        // Setup up dismiss action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG",
                instance, AlarmInstance.DISMISSED_STATE);
        notification.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                resources.getString(R.string.alarm_alert_dismiss_now_text),
                PendingIntent.getBroadcast(context, instance.hashCode(),
                        dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content action if instance is owned by alarm
        long alarmId = instance.mAlarmId == null ? Alarm.INVALID_ID : instance.mAlarmId;
        Intent viewAlarmIntent = Alarm.createIntent(context, DeskClock.class, alarmId);
        viewAlarmIntent.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
        viewAlarmIntent.putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, alarmId);
        viewAlarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(),
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showSnoozeNotification(Context context, AlarmInstance instance) {
        Log.v("Displaying snoozed notification for alarm instance: " + instance.mId);
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Resources resources = context.getResources();
        Notification.Builder notification = new Notification.Builder(context)
                .setContentTitle(instance.getLabelOrDefault(context))
                .setContentText(resources.getString(R.string.alarm_alert_snooze_until,
                        AlarmUtils.getFormattedTime(context, instance.getAlarmTime())))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MAX);

        // Setup up dismiss action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG",
                instance, AlarmInstance.DISMISSED_STATE);
        notification.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                resources.getString(R.string.alarm_alert_dismiss_text),
                PendingIntent.getBroadcast(context, instance.hashCode(),
                        dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content action if instance is owned by alarm
        long alarmId = instance.mAlarmId == null ? Alarm.INVALID_ID : instance.mAlarmId;
        Intent viewAlarmIntent = Alarm.createIntent(context, DeskClock.class, alarmId);
        viewAlarmIntent.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
        viewAlarmIntent.putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, alarmId);
        viewAlarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(),
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showMissedNotification(Context context, AlarmInstance instance) {
        Log.v("Displaying missed notification for alarm instance: " + instance.mId);
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        String label = instance.mLabel;
        String alarmTime = AlarmUtils.getFormattedTime(context, instance.getAlarmTime());
        String contextText = instance.mLabel.isEmpty() ? alarmTime :
                context.getString(R.string.alarm_missed_text, alarmTime, label);
        Notification.Builder notification = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.alarm_missed_title))
                .setContentText(contextText)
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setPriority(Notification.PRIORITY_HIGH);

        // Setup dismiss intent
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG",
                instance, AlarmInstance.DISMISSED_STATE);
        notification.setDeleteIntent(PendingIntent.getBroadcast(context, instance.hashCode(),
                dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content intent
        Intent showAndDismiss = AlarmInstance.createIntent(context, AlarmStateManager.class,
                instance.mId);
        showAndDismiss.setAction(AlarmStateManager.SHOW_AND_DISMISS_ALARM_ACTION);
        notification.setContentIntent(PendingIntent.getBroadcast(context, instance.hashCode(),
                showAndDismiss, PendingIntent.FLAG_UPDATE_CURRENT));

        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showAlarmNotification(Context context, AlarmInstance instance) {
        Log.v("Displaying alarm notification for alarm instance: " + instance.mId);
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Close dialogs and window shade, so this will display
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        Resources resources = context.getResources();
        Notification.Builder notification = new Notification.Builder(context)
                .setContentTitle(instance.getLabelOrDefault(context))
                .setContentText(AlarmUtils.getFormattedTime(context, instance.getAlarmTime()))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setOngoing(true)
                .setAutoCancel(false)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setWhen(0);

        // Setup Snooze Action
        Intent snoozeIntent = AlarmStateManager.createStateChangeIntent(context, "SNOOZE_TAG",
                instance, AlarmInstance.SNOOZE_STATE);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(R.drawable.stat_notify_alarm,
                resources.getString(R.string.alarm_alert_snooze_text), snoozePendingIntent);

        // Setup Dismiss Action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG",
                instance, AlarmInstance.DISMISSED_STATE);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context,
                instance.hashCode(), dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                resources.getString(R.string.alarm_alert_dismiss_text),
                dismissPendingIntent);

        // Setup Content Action
        Intent contentIntent = AlarmInstance.createIntent(context, AlarmActivity.class,
                instance.mId);
        notification.setContentIntent(PendingIntent.getActivity(context,
                instance.hashCode(), contentIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup fullscreen intent
        Intent fullScreenIntent = AlarmInstance.createIntent(context, AlarmActivity.class,
                instance.mId);
        // set action, so we can be different then content pending intent
        fullScreenIntent.setAction("fullscreen_activity");
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        notification.setFullScreenIntent(PendingIntent.getActivity(context,
                instance.hashCode(), fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT), true);
        notification.setPriority(Notification.PRIORITY_MAX);

        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void clearNotification(Context context, AlarmInstance instance) {
        Log.v("Clearing notifications for alarm instance: " + instance.mId);
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(instance.hashCode());
    }
}
