/*
 * Copyright (C) 2020 The LineageOS Project
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

package com.android.deskclock;

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW;

import android.app.NotificationChannel;
import android.content.Context;
import android.util.ArraySet;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;

import com.android.deskclock.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NotificationUtils {

    private static final String TAG = NotificationUtils.class.getSimpleName();

    /**
     * Notification channel containing all missed alarm notifications.
     */
    public static final String ALARM_MISSED_NOTIFICATION_CHANNEL_ID = "alarmMissedNotification";

    /**
     * Notification channel containing all upcoming alarm notifications.
     */
    public static final String ALARM_UPCOMING_NOTIFICATION_CHANNEL_ID = "alarmUpcomingNotification";

    /**
     * Notification channel containing all snooze notifications.
     */
    public static final String ALARM_SNOOZE_NOTIFICATION_CHANNEL_ID = "alarmSnoozingNotification";

    /**
     * Notification channel containing all firing alarm and timer notifications.
     */
    public static final String FIRING_NOTIFICATION_CHANNEL_ID = "firingAlarmsAndTimersNotification";

    /**
     * Notification channel containing all TimerModel notifications.
     */
    public static final String TIMER_MODEL_NOTIFICATION_CHANNEL_ID = "timerNotification";

    /**
     * Notification channel containing all stopwatch notifications.
     */
    public static final String STOPWATCH_NOTIFICATION_CHANNEL_ID = "stopwatchNotification";

    /**
     * Values used to bitmask certain channel defaults
     */
    private static final int PLAY_SOUND = 0x01;
    private static final int ENABLE_LIGHTS = 0x02;
    private static final int ENABLE_VIBRATION = 0x04;

    private static Map<String, int[]> CHANNEL_PROPS = new HashMap<String, int[]>();
    static {
        CHANNEL_PROPS.put(ALARM_MISSED_NOTIFICATION_CHANNEL_ID, new int[]{
                R.string.alarm_missed_channel,
                IMPORTANCE_HIGH
        });
        CHANNEL_PROPS.put(ALARM_SNOOZE_NOTIFICATION_CHANNEL_ID, new int[]{
                R.string.alarm_snooze_channel,
                IMPORTANCE_LOW
        });
        CHANNEL_PROPS.put(ALARM_UPCOMING_NOTIFICATION_CHANNEL_ID, new int[]{
                R.string.alarm_upcoming_channel,
                IMPORTANCE_LOW
        });
        CHANNEL_PROPS.put(FIRING_NOTIFICATION_CHANNEL_ID, new int[]{
                R.string.firing_alarms_timers_channel,
                IMPORTANCE_HIGH,
                ENABLE_LIGHTS
        });
        CHANNEL_PROPS.put(STOPWATCH_NOTIFICATION_CHANNEL_ID, new int[]{
                R.string.stopwatch_channel,
                IMPORTANCE_LOW
        });
        CHANNEL_PROPS.put(TIMER_MODEL_NOTIFICATION_CHANNEL_ID, new int[]{
                R.string.timer_channel,
                IMPORTANCE_LOW
        });
    }

    public static void createChannel(Context context, String id) {
        if (!Utils.isOOrLater()) {
            return;
        }

        if (!CHANNEL_PROPS.containsKey(id)) {
            Log.e(TAG, "Invalid channel requested: " + id);
            return;
        }

        int[] properties = (int[]) CHANNEL_PROPS.get(id);
        int nameId = properties[0];
        int importance = properties[1];
        NotificationChannel channel = new NotificationChannel(
                id, context.getString(nameId), importance);
        if (properties.length >= 3) {
            int bits = properties[2];
            channel.enableLights((bits & ENABLE_LIGHTS) != 0);
            channel.enableVibration((bits & ENABLE_VIBRATION) != 0);
            if ((bits & PLAY_SOUND) == 0) {
                channel.setSound(null, null);
            }
        }
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.createNotificationChannel(channel);
    }

    private static void deleteChannel(NotificationManagerCompat nm, String channelId) {
        NotificationChannel channel = nm.getNotificationChannel(channelId);
        if (channel != null) {
            nm.deleteNotificationChannel(channelId);
        }
    }

    private static Set<String> getAllExistingChannelIds(NotificationManagerCompat nm) {
        Set<String> result = new ArraySet<>();
        for (NotificationChannel channel : nm.getNotificationChannels()) {
            result.add(channel.getId());
        }
        return result;
    }

    public static void updateNotificationChannels(Context context) {
        if (!Utils.isOOrLater()) {
            return;
        }

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        // These channels got a new behavior so we need to recreate them with new ids
        deleteChannel(nm, "alarmLowPriorityNotification");
        deleteChannel(nm, "alarmHighPriorityNotification");
        deleteChannel(nm, "StopwatchNotification");
        deleteChannel(nm, "alarmNotification");
        deleteChannel(nm, "TimerModelNotification");
        deleteChannel(nm, "firingAlarmsTimersNotification");
        deleteChannel(nm, "alarmSnoozeNotification");

        // We recreate all existing channels so any language change or our name changes propagate
        // to the actual channels
        Set<String> existingChannelIds = getAllExistingChannelIds(nm);
        for (String id : existingChannelIds) {
            createChannel(context, id);
        }
    }
}
