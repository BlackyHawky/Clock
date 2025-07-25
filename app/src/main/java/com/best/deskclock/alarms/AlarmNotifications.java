/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.best.deskclock.utils.NotificationUtils.ALARM_MISSED_NOTIFICATION_CHANNEL_ID;
import static com.best.deskclock.utils.NotificationUtils.ALARM_SNOOZE_NOTIFICATION_CHANNEL_ID;
import static com.best.deskclock.utils.NotificationUtils.ALARM_UPCOMING_NOTIFICATION_CHANNEL_ID;
import static com.best.deskclock.utils.NotificationUtils.FIRING_NOTIFICATION_CHANNEL_ID;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.service.notification.StatusBarNotification;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.best.deskclock.AlarmClockFragment;
import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.SdkUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public final class AlarmNotifications {
    static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    /**
     * Formats times such that chronological order and lexicographical order agree.
     */
    private static final DateFormat SORT_KEY_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    /**
     * This value is coordinated with group ids from
     * {link com.best.deskclock.data.NotificationModel}
     */
    private static final String UPCOMING_GROUP_KEY = "1";

    /**
     * This value is coordinated with group ids from
     * {link com.best.deskclock.data.NotificationModel}
     */
    private static final String MISSED_GROUP_KEY = "4";

    /**
     * This value is coordinated with notification ids from
     * {link com.best.deskclock.data.NotificationModel}
     */
    private static final int ALARM_GROUP_NOTIFICATION_ID = Integer.MAX_VALUE - 4;

    /**
     * This value is coordinated with notification ids from
     * {link com.best.deskclock.data.NotificationModel}
     */
    private static final int ALARM_GROUP_MISSED_NOTIFICATION_ID = Integer.MAX_VALUE - 5;

    /**
     * This value is coordinated with notification ids from
     * {link com.best.deskclock.data.NotificationModel}
     */
    private static final int ALARM_FIRING_NOTIFICATION_ID = Integer.MAX_VALUE - 7;

    static synchronized void showUpcomingNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying upcoming alarm notification for alarm instance: " + instance.mId);

        final Alarm alarm = Alarm.getAlarm(context.getContentResolver(), instance.mAlarmId);
        final String contentTitle;

        if (alarm == null) {
            LogUtils.wtf("Failed to retrieve alarm with ID: %d", instance.mAlarmId);
            return;
        }

        if (!alarm.daysOfWeek.isRepeating()) {
            if (alarm.deleteAfterUse) {
                contentTitle = context.getString(R.string.occasional_alarm_alert_predismiss_title);
            } else {
                contentTitle = context.getString(R.string.alarm_alert_predismiss_title);
            }
        } else {
            contentTitle = context.getString(R.string.alarm_alert_predismiss_title);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, ALARM_UPCOMING_NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setContentTitle(contentTitle)
                .setContentText(AlarmUtils.getAlarmText(context, instance, true))
                .setColor(context.getColor(R.color.md_theme_primary))
                .setSmallIcon(R.drawable.ic_tab_alarm_static)
                .setAutoCancel(false)
                .setSortKey(createSortKey(instance))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true)
                .setOngoing(true)
                .setGroup(UPCOMING_GROUP_KEY);

        // Setup up dismiss action
        final int id = instance.hashCode();
        final String dismissActionTitle;
        if (!alarm.daysOfWeek.isRepeating() && alarm.deleteAfterUse) {
            dismissActionTitle = context.getString(R.string.alarm_alert_dismiss_and_delete_text);
        } else {
            dismissActionTitle = context.getString(R.string.alarm_alert_dismiss_text);
        }

        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.PREDISMISSED_STATE);
        builder.addAction(R.drawable.ic_alarm_off, dismissActionTitle,
                PendingIntent.getService(context, id, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE));

        // Setup content action if instance is owned by alarm
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        builder.setContentIntent(PendingIntent.getActivity(context, id,
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.createChannel(context, ALARM_UPCOMING_NOTIFICATION_CHANNEL_ID);
        }
        final Notification notification = builder.build();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Always false, because notification activation is always checked when the application is started.
            return;
        }

        nm.notify(id, notification);
        updateUpcomingAlarmGroupNotification(context, -1, notification);
    }

    private static boolean isGroupSummary(Notification n) {
        return (n.flags & Notification.FLAG_GROUP_SUMMARY) == Notification.FLAG_GROUP_SUMMARY;
    }

    /**
     * Method which returns the first active notification for a given group. If a notification was
     * just posted, provide it to make sure it is included as a potential result. If a notification
     * was just canceled, provide the id so that it is not included as a potential result. These
     * extra parameters are needed due to a race condition which exists in
     * {@link NotificationManager#getActiveNotifications()}.
     *
     * @param context                Context from which to grab the NotificationManager
     * @param group                  The group key to query for notifications
     * @param canceledNotificationId The id of the just-canceled notification (-1 if none)
     * @param postedNotification     The notification that was just posted
     * @return The first active notification for the group
     */

    private static Notification getFirstActiveNotification(Context context, String group,
                                                           int canceledNotificationId, Notification postedNotification) {

        final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final StatusBarNotification[] notifications = nm.getActiveNotifications();
        Notification firstActiveNotification = postedNotification;
        for (StatusBarNotification statusBarNotification : notifications) {
            final Notification n = statusBarNotification.getNotification();
            if (!isGroupSummary(n) && group.equals(n.getGroup()) && statusBarNotification.getId() != canceledNotificationId) {
                if (firstActiveNotification == null || n.getSortKey().compareTo(firstActiveNotification.getSortKey()) < 0) {
                    firstActiveNotification = n;
                }
            }
        }
        return firstActiveNotification;
    }

    private static Notification getActiveGroupSummaryNotification(Context context, String group) {
        final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final StatusBarNotification[] notifications = nm.getActiveNotifications();
        for (StatusBarNotification statusBarNotification : notifications) {
            final Notification n = statusBarNotification.getNotification();
            if (isGroupSummary(n) && group.equals(n.getGroup())) {
                return n;
            }
        }
        return null;
    }

    private static void updateUpcomingAlarmGroupNotification(Context context, int canceledNotificationId,
                                                             Notification postedNotification) {

        final NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        final Notification firstUpcoming = getFirstActiveNotification(context, UPCOMING_GROUP_KEY,
                canceledNotificationId, postedNotification);
        if (firstUpcoming == null) {
            nm.cancel(ALARM_GROUP_NOTIFICATION_ID);
            return;
        }

        Notification summary = getActiveGroupSummaryNotification(context, UPCOMING_GROUP_KEY);
        if (summary == null || !Objects.equals(summary.contentIntent, firstUpcoming.contentIntent)) {
            if (SdkUtils.isAtLeastAndroid8()) {
                NotificationUtils.createChannel(context, ALARM_UPCOMING_NOTIFICATION_CHANNEL_ID);
            }
            summary = new NotificationCompat.Builder(context, ALARM_UPCOMING_NOTIFICATION_CHANNEL_ID)
                    .setShowWhen(false)
                    .setContentIntent(firstUpcoming.contentIntent)
                    .setColor(context.getColor(R.color.md_theme_primary))
                    .setSmallIcon(R.drawable.ic_tab_alarm_static)
                    .setGroup(UPCOMING_GROUP_KEY)
                    .setGroupSummary(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setLocalOnly(true)
                    .build();

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Always false, because notification activation is always checked when the application is started.
                return;
            }

            nm.notify(ALARM_GROUP_NOTIFICATION_ID, summary);
        }
    }

    private static void updateMissedAlarmGroupNotification(Context context, int canceledNotificationId, Notification postedNotification) {
        final NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        final Notification firstMissed = getFirstActiveNotification(context, MISSED_GROUP_KEY,
                canceledNotificationId, postedNotification);
        if (firstMissed == null) {
            nm.cancel(ALARM_GROUP_MISSED_NOTIFICATION_ID);
            return;
        }

        Notification summary = getActiveGroupSummaryNotification(context, MISSED_GROUP_KEY);
        if (summary == null || !Objects.equals(summary.contentIntent, firstMissed.contentIntent)) {
            if (SdkUtils.isAtLeastAndroid8()) {
                NotificationUtils.createChannel(context, ALARM_MISSED_NOTIFICATION_CHANNEL_ID);
            }
            summary = new NotificationCompat.Builder(context, ALARM_MISSED_NOTIFICATION_CHANNEL_ID)
                    .setShowWhen(false)
                    .setContentIntent(firstMissed.contentIntent)
                    .setColor(context.getColor(R.color.md_theme_primary))
                    .setSmallIcon(R.drawable.ic_tab_alarm_static)
                    .setGroup(MISSED_GROUP_KEY)
                    .setGroupSummary(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setLocalOnly(true)
                    .build();

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Always false, because notification activation is always checked when the application is started.
                return;
            }

            nm.notify(ALARM_GROUP_MISSED_NOTIFICATION_ID, summary);
        }
    }

    static synchronized void showSnoozeNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying snoozed notification for alarm instance: " + instance.mId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, ALARM_SNOOZE_NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setContentTitle(instance.getLabelOrDefault(context))
                .setContentText(context.getString(R.string.alarm_alert_snooze_until,
                        AlarmUtils.getFormattedTime(context, instance.getAlarmTime())))
                .setColor(context.getColor(R.color.md_theme_primary))
                .setSmallIcon(R.drawable.ic_tab_alarm_static)
                .setAutoCancel(false)
                .setSortKey(createSortKey(instance))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true)
                .setGroup(UPCOMING_GROUP_KEY);

        // Setup up dismiss action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.DISMISSED_STATE);

        final int id = instance.hashCode();

        builder.addAction(R.drawable.ic_alarm_off, context.getString(R.string.alarm_alert_dismiss_text), PendingIntent.getService(context, id,
                dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        // Setup content action if instance is owned by alarm
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        builder.setContentIntent(PendingIntent.getActivity(context, id,
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.createChannel(context, ALARM_SNOOZE_NOTIFICATION_CHANNEL_ID);
        }
        final Notification notification = builder.build();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Always false, because notification activation is always checked when the application is started.
            return;
        }

        nm.notify(id, notification);
        updateUpcomingAlarmGroupNotification(context, -1, notification);
    }

    @SuppressLint("LaunchActivityFromNotification")
    static synchronized void showMissedNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying missed notification for alarm instance: " + instance.mId);

        String label = instance.mLabel;
        String alarmTime = AlarmUtils.getFormattedTime(context, instance.getAlarmTime());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, ALARM_MISSED_NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setContentTitle(context.getString(R.string.alarm_missed_title))
                .setContentText(instance.mLabel.isEmpty() ? alarmTime :
                        context.getString(R.string.alarm_missed_text, alarmTime, label))
                .setColor(context.getColor(R.color.md_theme_primary))
                .setSortKey(createSortKey(instance))
                .setSmallIcon(R.drawable.ic_tab_alarm_static)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true)
                .setGroup(MISSED_GROUP_KEY);

        final int id = instance.hashCode();

        // Setup dismiss intent
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.DISMISSED_STATE);
        builder.setDeleteIntent(PendingIntent.getService(context, id,
                dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        // Setup content intent
        Intent showAndDismiss = AlarmInstance.createIntent(context, AlarmStateManager.class, instance.mId);
        showAndDismiss.putExtra(EXTRA_NOTIFICATION_ID, id);
        showAndDismiss.setAction(AlarmStateManager.SHOW_AND_DISMISS_ALARM_ACTION);
        builder.setContentIntent(PendingIntent.getBroadcast(context, id,
                showAndDismiss, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.createChannel(context, ALARM_MISSED_NOTIFICATION_CHANNEL_ID);
        }
        final Notification notification = builder.build();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Always false, because notification activation is always checked when the application is started.
            return;
        }

        nm.notify(id, notification);
        updateMissedAlarmGroupNotification(context, -1, notification);
    }

    static synchronized void showAlarmNotification(Service service, AlarmInstance instance) {
        LogUtils.v("Displaying alarm notification for alarm instance: " + instance.mId);

        Resources resources = service.getResources();
        NotificationCompat.Builder notification = new NotificationCompat.Builder(
                service, FIRING_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(instance.getLabelOrDefault(service))
                .setContentText(AlarmUtils.getFormattedTime(service, instance.getAlarmTime()))
                .setColor(service.getColor(R.color.md_theme_primary))
                .setSmallIcon(R.drawable.ic_tab_alarm_static)
                .setOngoing(true)
                .setAutoCancel(false)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setWhen(0)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        // Setup Snooze Action only if snooze duration has NOT been set to "None" in the settings
        // or if "Enable alarm snooze actions" is enabled in the expanded alarm view
        if (instance.mSnoozeDuration != ALARM_SNOOZE_DURATION_DISABLED) {
            Intent snoozeIntent = AlarmStateManager.createStateChangeIntent(service,
                    AlarmStateManager.ALARM_SNOOZE_TAG, instance, AlarmInstance.SNOOZE_STATE);
            snoozeIntent.putExtra(AlarmStateManager.FROM_NOTIFICATION_EXTRA, true);
            PendingIntent snoozePendingIntent = PendingIntent.getService(service,
                    ALARM_FIRING_NOTIFICATION_ID, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            notification.addAction(R.drawable.ic_snooze, resources.getString(R.string.alarm_alert_snooze_text), snoozePendingIntent);
        }

        // Setup Dismiss Action
        final String dismissActionTitle;
        final Alarm alarm = Alarm.getAlarm(service.getContentResolver(), instance.mAlarmId);

        if (alarm == null) {
            LogUtils.wtf("Failed to retrieve alarm with ID: %d", instance.mAlarmId);
            return;
        }

        // Setup up dismiss action
        if (!alarm.daysOfWeek.isRepeating()) {
            if (alarm.deleteAfterUse) {
                dismissActionTitle = resources.getString(R.string.alarm_alert_dismiss_and_delete_text);
            } else {
                dismissActionTitle = resources.getString(R.string.alarm_alert_dismiss_text);
            }
        } else {
            dismissActionTitle = resources.getString(R.string.alarm_alert_dismiss_text);
        }

        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(service,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.DISMISSED_STATE);
        dismissIntent.putExtra(AlarmStateManager.FROM_NOTIFICATION_EXTRA, true);
        PendingIntent dismissPendingIntent = PendingIntent.getService(service,
                ALARM_FIRING_NOTIFICATION_ID, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notification.addAction(R.drawable.ic_alarm_off, dismissActionTitle, dismissPendingIntent);
        // Stop alarm if user clears notification.
        notification.setDeleteIntent(dismissPendingIntent);

        // Setup Content Action
        Intent contentIntent = AlarmInstance.createIntent(service, AlarmActivity.class, instance.mId);
        notification.setContentIntent(PendingIntent.getActivity(service,
                ALARM_FIRING_NOTIFICATION_ID, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        // Setup fullscreen intent
        Intent fullScreenIntent = AlarmInstance.createIntent(service, AlarmActivity.class, instance.mId);
        // set action, so we can be different then content pending intent
        fullScreenIntent.setAction("fullscreen_activity");
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        notification.setFullScreenIntent(PendingIntent.getActivity(service, ALARM_FIRING_NOTIFICATION_ID, fullScreenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE), true);
        notification.setPriority(NotificationCompat.PRIORITY_HIGH);

        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.createChannel(service, FIRING_NOTIFICATION_CHANNEL_ID);
        }
        clearNotification(service, instance);
        service.startForeground(ALARM_FIRING_NOTIFICATION_ID, notification.build());
    }

    public static synchronized void clearNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Clearing notifications for alarm instance: " + instance.mId);
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        final int id = instance.hashCode();
        nm.cancel(id);
        updateUpcomingAlarmGroupNotification(context, id, null);
        updateMissedAlarmGroupNotification(context, id, null);
    }

    /**
     * Updates the notification for an existing alarm.
     */
    static void updateNotification(Context context, AlarmInstance instance) {
        switch (instance.mAlarmState) {
            case AlarmInstance.NOTIFICATION_STATE -> showUpcomingNotification(context, instance);
            case AlarmInstance.SNOOZE_STATE -> showSnoozeNotification(context, instance);
            case AlarmInstance.MISSED_STATE -> showMissedNotification(context, instance);
            default -> LogUtils.d("No notification to update");
        }
    }

    static Intent createViewAlarmIntent(Context context, AlarmInstance instance) {
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
