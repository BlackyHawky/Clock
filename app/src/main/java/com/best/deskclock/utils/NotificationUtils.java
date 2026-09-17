/*
 * Copyright (C) 2020 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW;

import android.app.NotificationChannel;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmNotifications;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.provider.AlarmInstance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class NotificationUtils {

    /**
     * Notification channel containing the foreground service.
     */
    public static final String FOREGROUND_SERVICE_CHANNEL_ID = "foregroundService";

    /**
     * Notification channel containing all missed alarm notifications.
     */
    public static final String ALARM_MISSED_NOTIFICATION_CHANNEL_ID = "alarmMissedNotification";

    /**
     * Notification channel containing all upcoming alarm notifications.
     */
    public static final String ALARM_UPCOMING_NOTIFICATION_CHANNEL_ID = "alarmUpcomingNotification_v2";

    /**
     * Notification channel containing all snooze notifications.
     */
    public static final String ALARM_SNOOZE_NOTIFICATION_CHANNEL_ID = "alarmSnoozingNotification_v2";

    /**
     * Notification channel containing all firing alarm and timer notifications.
     */
    public static final String FIRING_NOTIFICATION_CHANNEL_ID = "firingAlarmsAndTimersNotification";

    /**
     * Notification channel containing all TimerModel notifications.
     */
    public static final String TIMER_MODEL_NOTIFICATION_CHANNEL_ID = "timerNotification_v2";

    /**
     * Notification channel containing all missed timer notifications.
     */
    public static final String TIMER_MISSED_NOTIFICATION_CHANNEL_ID = "timerMissedNotification";

    /**
     * Notification channel containing all stopwatch notifications.
     */
    public static final String STOPWATCH_NOTIFICATION_CHANNEL_ID = "stopwatchNotification_v2";

    public static final String EXTRA_UPDATE_ALARM_NOTIFICATIONS = "EXTRA_UPDATE_ALARM_NOTIFICATIONS";

    private static final String TAG = NotificationUtils.class.getSimpleName();

    /**
     * Values used to bitmask certain channel defaults
     */
    private static final int PLAY_SOUND = 0x01;
    private static final int ENABLE_LIGHTS = 0x02;
    private static final int ENABLE_VIBRATION = 0x04;
    private static final int LOCKSCREEN_PUBLIC = 0x08;
    private static final int HIDE_BADGE = 0x10;

    private static final Map<String, int[]> CHANNEL_PROPS = new HashMap<>();

    static {
        CHANNEL_PROPS.put(FOREGROUND_SERVICE_CHANNEL_ID, new int[]{
            R.string.foreground_service_channel, IMPORTANCE_LOW, LOCKSCREEN_PUBLIC | HIDE_BADGE
        });

        CHANNEL_PROPS.put(ALARM_MISSED_NOTIFICATION_CHANNEL_ID, new int[]{
            R.string.alarm_missed_channel, IMPORTANCE_HIGH
        });

        CHANNEL_PROPS.put(ALARM_SNOOZE_NOTIFICATION_CHANNEL_ID, new int[]{
            R.string.alarm_snooze_channel, IMPORTANCE_DEFAULT, LOCKSCREEN_PUBLIC | HIDE_BADGE
        });

        CHANNEL_PROPS.put(ALARM_UPCOMING_NOTIFICATION_CHANNEL_ID, new int[]{
            R.string.alarm_upcoming_channel, IMPORTANCE_DEFAULT, LOCKSCREEN_PUBLIC | HIDE_BADGE
        });

        CHANNEL_PROPS.put(FIRING_NOTIFICATION_CHANNEL_ID, new int[]{
            R.string.firing_alarms_timers_channel, IMPORTANCE_HIGH, ENABLE_LIGHTS
        });

        CHANNEL_PROPS.put(STOPWATCH_NOTIFICATION_CHANNEL_ID, new int[]{
            R.string.stopwatch_channel, IMPORTANCE_DEFAULT, LOCKSCREEN_PUBLIC | HIDE_BADGE
        });

        CHANNEL_PROPS.put(TIMER_MODEL_NOTIFICATION_CHANNEL_ID, new int[]{
            R.string.timer_channel, IMPORTANCE_DEFAULT, LOCKSCREEN_PUBLIC | HIDE_BADGE
        });

        CHANNEL_PROPS.put(TIMER_MISSED_NOTIFICATION_CHANNEL_ID, new int[]{
            R.string.timer_missed_channel, IMPORTANCE_DEFAULT, LOCKSCREEN_PUBLIC
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createChannel(@NonNull Context context, @NonNull String id) {
        if (!CHANNEL_PROPS.containsKey(id)) {
            Log.e(TAG, "Invalid channel requested: " + id);
            return;
        }

        int[] properties = CHANNEL_PROPS.get(id);
        int nameId = Objects.requireNonNull(properties)[0];
        int importance = properties[1];
        NotificationChannel channel = new NotificationChannel(id, context.getString(nameId), importance);

        if (properties.length >= 3) {
            int bits = properties[2];

            channel.enableLights((bits & ENABLE_LIGHTS) != 0);
            channel.enableVibration((bits & ENABLE_VIBRATION) != 0);

            if ((bits & PLAY_SOUND) == 0) {
                channel.setSound(null, null);
            }

            if ((bits & LOCKSCREEN_PUBLIC) != 0) {
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            }

            if ((bits & HIDE_BADGE) != 0) {
                channel.setShowBadge(false);
            }
        }

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.createNotificationChannel(channel);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void deleteChannel(@NonNull NotificationManagerCompat nm, @NonNull String channelId) {
        NotificationChannel channel = nm.getNotificationChannel(channelId);
        if (channel != null) {
            nm.deleteNotificationChannel(channelId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void updateNotificationChannels(@NonNull Context context) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        // Whenever a channel's properties are updated, we must first delete the old channel ID,
        // which will then be recreated with new id.
        deleteChannel(nm, "alarmSnoozingNotification");
        deleteChannel(nm, "alarmUpcomingNotification");
        deleteChannel(nm, "stopwatchNotification");
        deleteChannel(nm, "timerNotification");

        // We recreate all existing channels so any language change or our name changes propagate to the actual channels
        for (String id : CHANNEL_PROPS.keySet()) {
            createChannel(context, id);
        }
    }

    /**
     * Clear all notifications. Useful after a restore or reset, for example.
     */
    public static void clearAllNotifications(@NonNull Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();
    }

    /**
     * Updates alarm notifications. Useful when changing languages, for example.
     */
    public static void updateAlarmNotifications(@NonNull Context appContext, @NonNull String languageCode, int globalIntentId) {
        final Context safeContext = appContext.getApplicationContext();
        final ContentResolver contentResolver = safeContext.getContentResolver();

        AppExecutors.getDiskIO().execute(() -> {
            final List<AlarmInstance> activeInstances = new ArrayList<>();

            activeInstances.addAll(AlarmInstance.getInstancesByState(contentResolver, AlarmInstance.NOTIFICATION_STATE));
            activeInstances.addAll(AlarmInstance.getInstancesByState(contentResolver, AlarmInstance.FIRED_STATE));
            activeInstances.addAll(AlarmInstance.getInstancesByState(contentResolver, AlarmInstance.SNOOZE_STATE));
            activeInstances.addAll(AlarmInstance.getInstancesByState(contentResolver, AlarmInstance.MISSED_STATE));

            AppExecutors.getMainThread().post(() -> {
                for (AlarmInstance instance : activeInstances) {
                    AlarmNotifications.updateNotification(safeContext, instance, languageCode, globalIntentId);
                }
            });
        });
    }

    @NonNull
    public static String getNotificationAlarmText(@NonNull Context context, @NonNull AlarmInstance instance, @NonNull String languageCode,
                                                  boolean isSnoozeNotification) {

        final Context localizedContext = Utils.getLocalizedContext(context, languageCode);
        final Locale locale = Utils.getLocaleFromContext(localizedContext);
        final boolean is24HourFormat = DateFormat.is24HourFormat(localizedContext);
        final int skeletonResId = getSkeletonResId(is24HourFormat, Calendar.getInstance(), instance.getAlarmTime());
        final String skeleton = localizedContext.getString(skeletonResId);
        final String pattern = DateFormat.getBestDateTimePattern(locale, skeleton);
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, locale);
        final String alarmTimeStr = simpleDateFormat.format(instance.getAlarmTime().getTime());
        final String formattedText = instance.mLabel.isEmpty() ? alarmTimeStr : alarmTimeStr + " - " + instance.mLabel;

        return isSnoozeNotification ? formattedText : FormattedTextUtils.capitalizeFirstLetter(formattedText, locale);
    }

    @StringRes
    private static int getSkeletonResId(boolean is24HourFormat, @NonNull Calendar now, @NonNull Calendar alarmTime) {
        final int currentYear = now.get(Calendar.YEAR);
        final int instanceYear = alarmTime.get(Calendar.YEAR);
        final int currentDayOfYear = now.get(Calendar.DAY_OF_YEAR);
        final int instanceDayOfYear = alarmTime.get(Calendar.DAY_OF_YEAR);

        final boolean isToday = (currentYear == instanceYear) && (currentDayOfYear == instanceDayOfYear);

        if (isToday) {
            // The alarm is set for today: display the time only
            return is24HourFormat ? R.string.time_24_hour : R.string.time_12_hour;
        } else if (currentYear != instanceYear) {
            // The alarm is set for another year: display the full date + time
            return is24HourFormat ? R.string.abbrev_wday_month_day_with_year_24_hour : R.string.abbrev_wday_month_day_with_year_12_hour;
        } else {
            // The alarm is for another day in the same year: display the date without the year + time.
            return is24HourFormat ? R.string.abbrev_wday_month_day_no_year_24_hour : R.string.abbrev_wday_month_day_no_year_12_hour;
        }
    }

}
