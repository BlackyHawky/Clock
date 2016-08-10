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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public final class AlarmNotifications {
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    /**
     * Formats times such that chronological order and lexicographical order agree.
     */
    private static final DateFormat SORT_KEY_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    /**
     * This value is coordinated with group ids from
     * {@link com.android.deskclock.data.NotificationModel}
     */
    private static final String UPCOMING_GROUP_KEY = "1";

    /**
     * This value is coordinated with group ids from
     * {@link com.android.deskclock.data.NotificationModel}
     */
    private static final String MISSED_GROUP_KEY = "4";

    /**
     * This value is coordinated with notification ids from
     * {@link com.android.deskclock.data.NotificationModel}
     */
    private static final int ALARM_GROUP_NOTIFICATION_ID = Integer.MAX_VALUE - 4;

    /**
     * This value is coordinated with notification ids from
     * {@link com.android.deskclock.data.NotificationModel}
     */
    private static final int ALARM_GROUP_MISSED_NOTIFICATION_ID = Integer.MAX_VALUE - 5;

    public static void showLowPriorityNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying low priority notification for alarm instance: " + instance.mId);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setShowWhen(false)
                .setContentTitle(context.getString(
                        R.string.alarm_alert_predismiss_title))
                .setContentText(AlarmUtils.getAlarmText(context, instance, true /* includeLabel */))
                .setColor(ContextCompat.getColor(context, R.color.default_background))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setAutoCancel(false)
                .setSortKey(createSortKey(instance))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        if (Utils.isNOrLater()) {
            notification.setGroup(UPCOMING_GROUP_KEY);
        }

        // Setup up hide notification
        Intent hideIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DELETE_TAG, instance,
                AlarmInstance.HIDE_NOTIFICATION_STATE);
        notification.setDeleteIntent(PendingIntent.getService(context, instance.hashCode(),
                hideIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup up dismiss action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.PREDISMISSED_STATE);
        notification.addAction(R.drawable.ic_alarm_off_24dp,
                context.getString(R.string.alarm_alert_dismiss_now_text),
                PendingIntent.getService(context, instance.hashCode(),
                        dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content action if instance is owned by alarm
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(),
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(instance.hashCode(), notification.build());
        updateAlarmGroupNotification(context);
    }

    public static void showHighPriorityNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying high priority notification for alarm instance: " + instance.mId);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setShowWhen(false)
                .setContentTitle(context.getString(R.string.alarm_alert_predismiss_title))
                .setContentText(AlarmUtils.getAlarmText(context, instance, true /* includeLabel */))
                .setColor(ContextCompat.getColor(context, R.color.default_background))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setAutoCancel(false)
                .setSortKey(createSortKey(instance))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        if (Utils.isNOrLater()) {
            notification.setGroup(UPCOMING_GROUP_KEY);
        }

        // Setup up dismiss action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.PREDISMISSED_STATE);
        notification.addAction(R.drawable.ic_alarm_off_24dp,
                context.getString(R.string.alarm_alert_dismiss_now_text),
                PendingIntent.getService(context, instance.hashCode(),
                        dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content action if instance is owned by alarm
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(),
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(instance.hashCode(), notification.build());
        updateAlarmGroupNotification(context);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static int getActiveNotificationsCount(Context context, String group) {
        NotificationManager nm = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications = nm.getActiveNotifications();
        int count = 0;
        for (StatusBarNotification statusBarNotification : notifications) {
            final Notification n = statusBarNotification.getNotification();
            if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != Notification.FLAG_GROUP_SUMMARY
                    && group.equals(n.getGroup())) {
                count++;
            }
        }
        return count;
    }

    public static void updateAlarmGroupNotification(Context context) {
        if (!Utils.isNOrLater()) {
            return;
        }

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        if (getActiveNotificationsCount(context, UPCOMING_GROUP_KEY) == 0) {
            nm.cancel(ALARM_GROUP_NOTIFICATION_ID);
            return;
        }

        NotificationCompat.Builder summaryNotification = new NotificationCompat.Builder(context)
                .setShowWhen(false)
                .setColor(ContextCompat.getColor(context, R.color.default_background))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setGroup(UPCOMING_GROUP_KEY)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);


        nm.notify(ALARM_GROUP_NOTIFICATION_ID, summaryNotification.build());
    }

    public static void updateAlarmGroupMissedNotification(Context context) {
        if (!Utils.isNOrLater()) {
            return;
        }

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        if (getActiveNotificationsCount(context, MISSED_GROUP_KEY) == 0) {
            nm.cancel(ALARM_GROUP_MISSED_NOTIFICATION_ID);
            return;
        }

        NotificationCompat.Builder summaryNotification = new NotificationCompat.Builder(context)
                .setShowWhen(false)
                .setColor(ContextCompat.getColor(context, R.color.default_background))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setGroup(MISSED_GROUP_KEY)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        nm.notify(ALARM_GROUP_MISSED_NOTIFICATION_ID, summaryNotification.build());
    }

    public static void showSnoozeNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying snoozed notification for alarm instance: " + instance.mId);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setShowWhen(false)
                .setContentTitle(instance.getLabelOrDefault(context))
                .setContentText(context.getString(R.string.alarm_alert_snooze_until,
                        AlarmUtils.getFormattedTime(context, instance.getAlarmTime())))
                .setColor(ContextCompat.getColor(context, R.color.default_background))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setAutoCancel(false)
                .setSortKey(createSortKey(instance))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        if (Utils.isNOrLater()) {
            notification.setGroup(UPCOMING_GROUP_KEY);
        }

        // Setup up dismiss action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.DISMISSED_STATE);
        notification.addAction(R.drawable.ic_alarm_off_24dp,
                context.getString(R.string.alarm_alert_dismiss_text),
                PendingIntent.getService(context, instance.hashCode(),
                        dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content action if instance is owned by alarm
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(),
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(instance.hashCode(), notification.build());
        updateAlarmGroupNotification(context);
    }

    public static void showMissedNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying missed notification for alarm instance: " + instance.mId);

        String label = instance.mLabel;
        String alarmTime = AlarmUtils.getFormattedTime(context, instance.getAlarmTime());
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setShowWhen(false)
                .setContentTitle(context.getString(R.string.alarm_missed_title))
                .setContentText(instance.mLabel.isEmpty() ? alarmTime :
                        context.getString(R.string.alarm_missed_text, alarmTime, label))
                .setColor(ContextCompat.getColor(context, R.color.default_background))
                .setSortKey(createSortKey(instance))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        if (Utils.isNOrLater()) {
            notification.setGroup(MISSED_GROUP_KEY);
        }

        final int hashCode = instance.hashCode();

        // Setup dismiss intent
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.DISMISSED_STATE);
        notification.setDeleteIntent(PendingIntent.getService(context, hashCode,
                dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content intent
        Intent showAndDismiss = AlarmInstance.createIntent(context, AlarmStateManager.class,
                instance.mId);
        showAndDismiss.putExtra(EXTRA_NOTIFICATION_ID, hashCode);
        showAndDismiss.setAction(AlarmStateManager.SHOW_AND_DISMISS_ALARM_ACTION);
        notification.setContentIntent(PendingIntent.getBroadcast(context, hashCode,
                showAndDismiss, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(hashCode, notification.build());
        updateAlarmGroupMissedNotification(context);
    }

    public static void showAlarmNotification(Service service, AlarmInstance instance) {
        LogUtils.v("Displaying alarm notification for alarm instance: " + instance.mId);

        Resources resources = service.getResources();
        NotificationCompat.Builder notification = new NotificationCompat.Builder(service)
                .setContentTitle(instance.getLabelOrDefault(service))
                .setContentText(AlarmUtils.getFormattedTime(service, instance.getAlarmTime()))
                .setColor(ContextCompat.getColor(service, R.color.default_background))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setOngoing(true)
                .setAutoCancel(false)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setWhen(0)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        // Setup Snooze Action
        Intent snoozeIntent = AlarmStateManager.createStateChangeIntent(service,
                AlarmStateManager.ALARM_SNOOZE_TAG, instance, AlarmInstance.SNOOZE_STATE);
        snoozeIntent.putExtra(AlarmStateManager.FROM_NOTIFICATION_EXTRA, true);
        PendingIntent snoozePendingIntent = PendingIntent.getService(service, instance.hashCode(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(R.drawable.ic_snooze_24dp,
                resources.getString(R.string.alarm_alert_snooze_text), snoozePendingIntent);

        // Setup Dismiss Action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(service,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.DISMISSED_STATE);
        dismissIntent.putExtra(AlarmStateManager.FROM_NOTIFICATION_EXTRA, true);
        PendingIntent dismissPendingIntent = PendingIntent.getService(service,
                instance.hashCode(), dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(R.drawable.ic_alarm_off_24dp,
                resources.getString(R.string.alarm_alert_dismiss_text),
                dismissPendingIntent);

        // Setup Content Action
        Intent contentIntent = AlarmInstance.createIntent(service, AlarmActivity.class,
                instance.mId);
        notification.setContentIntent(PendingIntent.getActivity(service,
                instance.hashCode(), contentIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup fullscreen intent
        Intent fullScreenIntent = AlarmInstance.createIntent(service, AlarmActivity.class,
                instance.mId);
        // set action, so we can be different then content pending intent
        fullScreenIntent.setAction("fullscreen_activity");
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        notification.setFullScreenIntent(PendingIntent.getActivity(service,
                instance.hashCode(), fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT), true);
        notification.setPriority(NotificationCompat.PRIORITY_MAX);

        clearNotification(service, instance);
        service.startForeground(instance.hashCode(), notification.build());
    }

    public static void clearNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Clearing notifications for alarm instance: " + instance.mId);
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.cancel(instance.hashCode());
        updateAlarmGroupNotification(context);
        updateAlarmGroupMissedNotification(context);
    }

    /**
     * Updates the notification for an existing alarm. Use if the label has changed.
     */
    public static void updateNotification(Context context, AlarmInstance instance) {
        switch (instance.mAlarmState) {
            case AlarmInstance.LOW_NOTIFICATION_STATE:
                showLowPriorityNotification(context, instance);
                break;
            case AlarmInstance.HIGH_NOTIFICATION_STATE:
                showHighPriorityNotification(context, instance);
                break;
            case AlarmInstance.SNOOZE_STATE:
                showSnoozeNotification(context, instance);
                break;
            case AlarmInstance.MISSED_STATE:
                showMissedNotification(context, instance);
                break;
            default:
                LogUtils.d("No notification to update");
        }
    }

    public static Intent createViewAlarmIntent(Context context, AlarmInstance instance) {
        final long alarmId = instance.mAlarmId == null ? Alarm.INVALID_ID : instance.mAlarmId;
        return Alarm.createIntent(context, DeskClock.class, alarmId)
                .putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, alarmId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * Alarm notifications are sorted chronologically. Missed alarms are sorted chronologically
     * <strong>after</strong> all upcoming/snoozed alarms by including the "MISSED" prefix on the
     * sort key.
     *
     * @param instance the alarm instance for which the notification is generated
     * @return the sort key that specifies the order of this alarm notification
     */
    private static String createSortKey(AlarmInstance instance) {
        final String timeKey = SORT_KEY_FORMAT.format(instance.getAlarmTime().getTime());
        final boolean missedAlarm = instance.mAlarmState == AlarmInstance.MISSED_STATE;
        return missedAlarm ? ("MISSED " + timeKey) : timeKey;
    }
}