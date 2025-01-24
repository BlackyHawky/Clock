/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.bedtime;

import static com.best.deskclock.utils.NotificationUtils.BEDTIME_NOTIFICATION_CHANNEL_ID;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.bedtime.beddata.DataSaver;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public final class BedtimeService extends Service {

    private static final String ACTION_PREFIX = "com.best.deskclock.action.";
    public static final String ACTION_SHOW_BEDTIME = ACTION_PREFIX + "SHOW_BEDTIME";
    public static final String ACTION_BED_REMIND_NOTIF = ACTION_PREFIX + "BED_REMIND_NOTIF";
    public static final String ACTION_LAUNCH_BEDTIME = ACTION_PREFIX + "LAUNCH_BEDTIME";
    private static final String ACTION_BEDTIME_CANCEL = ACTION_PREFIX + "BEDTIME_STOP";
    private static final String ACTION_BEDTIME_PAUSE = ACTION_PREFIX + "BEDTIME_PAUSE";
    private static final int notificationID = 1237449874; // we may need a proper id

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        DataSaver saver = DataSaver.getInstance(context);
        saver.restore();
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_BED_REMIND_NOTIF -> {
                    if (saver.enabled && wakeupAlarm(context).enabled) {
                        showRemindNotification(context);
                    }
                }
                case ACTION_LAUNCH_BEDTIME -> startBedtimeMode(context, saver);
                case ACTION_BEDTIME_CANCEL -> stopBedtimeMode(context);
                case ACTION_BEDTIME_PAUSE -> {
                    stopBedtimeMode(context);
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new Date());
                    calendar.add(Calendar.MINUTE, 30);
                    alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), getPendingIntent(context, ACTION_LAUNCH_BEDTIME));
                    String text = AlarmUtils.getFormattedTime(context, calendar.getTimeInMillis());
                    text = context.getString(R.string.bedtime_notification_resume, text);
                    showPausedNotification(context, text);
                }
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * PendingIntent constructor generally used in here just for scheduling bedtime mode.
     * @param context any Context
     * @param action for the intent either remind notification or launch bedtime
     * @return PendingIntent ready for scheduling
     */
    private static PendingIntent getPendingIntent(Context context, String action) {
        Intent intent = new Intent(context, BedtimeService.class);
        intent.setAction(action);
        return PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Gets the next time for scheduling based on the action.
     * @param action for remind notification or launch bedtime
     */
    private static long getNextBedtime(DataSaver saver, String action) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, saver.hour);
        calendar.set(Calendar.MINUTE, saver.minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // Check if the calculated time is in the past
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            // If it's in the past, add one day to the calendar to get the next occurrence
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // There currently are only two actions in which it makes sense to use this: remind notification and bed launch
        if (action.equals(ACTION_BED_REMIND_NOTIF)) {
            calendar.add(Calendar.MINUTE, -saver.notificationShowTime);
        }

        // Handle weekdays
        for (int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); !saver.daysOfWeek.isBitOn(dayOfWeek);) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        }
        return calendar.getTimeInMillis();
    }

    private static void showRemindNotification(Context context) {
        LogUtils.v("Displaying upcoming notification:" + "Bed");

        @StringRes final int eventLabel = R.string.label_notification;
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(ACTION_SHOW_BEDTIME)
                .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, BEDTIME_NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setContentTitle(context.getString(R.string.bedtime_reminder_notification_title, bedtimeString(context)))
                .setContentText(context.getString(R.string.bedtime_reminder_notification_text, wakeupTime(context), hoursOfSleep(context)))
                .setStyle(new NotificationCompat.BigTextStyle())
                .setContentIntent(pendingShowApp)
                .setColor(context.getColor(R.color.md_theme_primary))
                .setSmallIcon(R.drawable.ic_moon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(context, BEDTIME_NOTIFICATION_CHANNEL_ID);
        }
        final Notification notification = builder.build();
        notificationManagerCompat.cancel(notificationID);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Always false, because notification activation is always checked when the application is started.
            return;
        }

        notificationManagerCompat.notify(notificationID, notification);
    }

    private static void showLaunchNotification(Context context, String text) {
        LogUtils.v("Displaying upcomming notif:" + "Bed");

        @StringRes final int eventLabel = R.string.label_notification;
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(ACTION_SHOW_BEDTIME)
                .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, BEDTIME_NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setContentTitle(context.getString(R.string.bedtime_notification_title))
                .setContentText(text)
                .setContentIntent(pendingShowApp)
                .setColor(context.getColor(R.color.md_theme_primary))
                .setSmallIcon(R.drawable.ic_tab_bedtime_static)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        Intent pause = new Intent(context, BedtimeService.class);
        pause.setAction(ACTION_BEDTIME_PAUSE);
        builder.addAction(R.drawable.ic_fab_pause, context.getString(R.string.bedtime_notification_action_pause),
                PendingIntent.getService(context, 0, pause,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
        );

        Intent off = new Intent(context, BedtimeService.class);
        off.setAction(ACTION_BEDTIME_CANCEL);
        builder.addAction(R.drawable.ic_close, context.getString(R.string.bedtime_notification_action_turn_off),
                PendingIntent.getService(context, 0, off,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
        );

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(context, BEDTIME_NOTIFICATION_CHANNEL_ID);
        }
        final Notification notification = builder.build();
        notificationManagerCompat.cancel(notificationID);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Always false, because notification activation is always checked when the application is started.
            return;
        }

        notificationManagerCompat.notify(notificationID, notification);
    }

    private static void showPausedNotification(Context context, String text) {
        LogUtils.v("Displaying upcoming notification:" + "Bed");

        @StringRes final int eventLabel = R.string.label_notification;
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(ACTION_SHOW_BEDTIME)
                .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, BEDTIME_NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setContentTitle(context.getString(R.string.bedtime_paused_notification_title))
                .setContentText(text)
                .setContentIntent(pendingShowApp)
                .setColor(context.getColor(R.color.md_theme_primary))
                .setSmallIcon(R.drawable.ic_moon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        Intent intent = new Intent(context, BedtimeService.class);
        intent.setAction(ACTION_LAUNCH_BEDTIME);
        builder.addAction(R.drawable.ic_fab_play, context.getString(R.string.bedtime_notification_resume_action),
                PendingIntent.getService(context, notificationID, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
        );

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(context, BEDTIME_NOTIFICATION_CHANNEL_ID);
        }
        final Notification notification = builder.build();
        notificationManagerCompat.cancel(notificationID);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Always false, because notification activation is always checked when the application is started.
            return;
        }

        notificationManagerCompat.notify(notificationID, notification);
    }

    public static void cancelNotification(Context context) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.cancel(notificationID);
    }

    private void startBedtimeMode(Context context, DataSaver saver) {
        cancelBedtimeMode(context, ACTION_LAUNCH_BEDTIME);
        cancelBedtimeMode(context, ACTION_BED_REMIND_NOTIF);
        cancelNotification(context);
        if (saver.notificationShowTime != -1) {
            scheduleBedtimeMode(context, saver, ACTION_BED_REMIND_NOTIF);
        }
        // Hope activating needs at least a millisecond
        scheduleBedtimeMode(context, saver, ACTION_LAUNCH_BEDTIME);

        String text = context.getString(R.string.bedtime_notification_until, wakeupTime(context));
        showLaunchNotification(context, text);

        Intent off = new Intent(context, BedtimeService.class);
        off.setAction(ACTION_BEDTIME_CANCEL);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC,
                Objects.requireNonNull(AlarmInstance.getNextUpcomingInstanceByAlarmId(context.getContentResolver(),
                        wakeupAlarm(context).id)).getAlarmTime().getTimeInMillis(),
                PendingIntent.getService(context, 0, off,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
        );

    }

    /**
     * Schedules corresponding to the given action.
     * @param action remind notification or launch bedtime
     */
    public static void scheduleBedtimeMode(Context context, DataSaver saver, String action) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        cancelBedtimeMode(context, action);
        alarmManager.setExact(AlarmManager.RTC, getNextBedtime(saver, action), getPendingIntent(context, action));
    }

    /**
     * Cancels the schedule set by scheduleBedtimeMode.
     * @param action remind notification or launch bedtime
     */
    public static void cancelBedtimeMode(Context context, String action) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(context, action));
    }

    private void stopBedtimeMode(Context context) {
       cancelNotification(context);
    }

    private static String bedtimeString(Context context) {
        DataSaver saver = DataSaver.getInstance(context);
        saver.restore(); // Without this we can see the issue wrong bedtime length coming.
        String bedtimeHour = saver.hour < 10 ? "0" + saver.hour : Integer.toString(saver.hour);
        String bedtimeMinute = saver.minutes < 10 ? "0" + saver.minutes : Integer.toString(saver.minutes);
        String ending = "";

        if (!DataModel.getDataModel().is24HourFormat()) {
            bedtimeHour = saver.hour > 12 ? Integer.toString(saver.hour - 12) : Integer.toString(saver.hour);
            if (saver.hour > 11) {
                ending = " PM";
            } else {
                ending = " AM";
            }
        }

        return bedtimeHour + ":" + bedtimeMinute + ending;
    }

    private static Alarm wakeupAlarm(Context context) {
        return Alarm.getAlarmByLabel(context.getApplicationContext().getContentResolver(), BedtimeFragment.BEDTIME_LABEL);
    }

    private static String wakeupTime(Context context) {
        String wakeHour = wakeupAlarm(context).hour < 10
                ? "0" + wakeupAlarm(context).hour
                : Integer.toString(wakeupAlarm(context).hour);
        String wakeMinute = wakeupAlarm(context).minutes < 10
                ? "0" + wakeupAlarm(context).minutes
                : Integer.toString(wakeupAlarm(context).minutes);
        String ending = "";

        if (!DataModel.getDataModel().is24HourFormat()) {
            wakeHour = wakeupAlarm(context).hour > 12
                    ? Integer.toString(wakeupAlarm(context).hour - 12)
                    : Integer.toString(wakeupAlarm(context).hour);
            if (wakeupAlarm(context).hour > 11) {
                ending = " PM";
            } else {
                ending = " AM";
            }
        }

        return wakeHour + ":" + wakeMinute + ending;
    }

    private static String hoursOfSleep(Context context) {
        DataSaver saver = DataSaver.getInstance(context);

        int hDiff;
        if (saver.hour > wakeupAlarm(context).hour
                || saver.hour == wakeupAlarm(context).hour && saver.minutes > wakeupAlarm(context).minutes) {
            hDiff = wakeupAlarm(context).hour + 24 - saver.hour;
        } else if (saver.hour == wakeupAlarm(context).hour && saver.minutes == wakeupAlarm(context).minutes) {
            hDiff = 24;
        } else {
            hDiff = wakeupAlarm(context).hour - saver.hour;
        }

        int minDiff = wakeupAlarm(context).minutes - saver.minutes;
        if (minDiff < 0) {
            hDiff = hDiff - 1;
            minDiff = 60 + minDiff;
        }

        String diff;
        if (minDiff == 0) {
            diff = hDiff + "h";
        } else if (hDiff == 0) {
            diff = minDiff + "min";
        } else {
            diff = hDiff + "h " + minDiff + "min";
        }

        return diff;
    }
}