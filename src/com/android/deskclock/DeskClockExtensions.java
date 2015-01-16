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

    public void sendNotification(Context context, NotificationType notificationType,
            long alarmId);
}
