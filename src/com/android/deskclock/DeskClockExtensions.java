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

    public void clearNotification(Context context, long alarmId);

    public void sendNotification(Context context, NotificationType notificationType, long alarmId,
            String content, String title /* Only used for snoozes */);

    public void sendStateChange(Context context, StateChangeType stateChangeType, long alarmId,
            String label /* Only used for fire */);
}
