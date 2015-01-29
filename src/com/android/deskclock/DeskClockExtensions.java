package com.android.deskclock;

import android.content.Context;

/** DeskClockExtensions. */
public interface DeskClockExtensions {
    // enum NotificationType
    public enum NotificationType {
        HIGH_PRIO,
        LOW_PRIO,
        MISSED,
        SNOOZE,
        CLEAR,
    }

    // enum StateChangeType
    public enum StateChangeType {
        FIRE,
        SNOOZE,
        DISMISS
    }

    public void sendNotification(Context context, NotificationType notificationType,
            long alarmId);

    public void sendNotificationWithExtra(Context context, NotificationType notificationType,
            long alarmId, String extra);

    public void sendStateChange(Context context, StateChangeType stateChangeType,
            long alarmId);
}
