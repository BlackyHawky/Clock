package com.android.deskclock;

import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.android.deskclock.provider.AlarmInstance;

/** DeskClockExtensions. */
public interface DeskClockExtensions {

    public void sendNotification(NotificationManagerCompat nm,
            NotificationCompat.Builder notification,
            AlarmInstance instance);

    public void sendNotification(NotificationManagerCompat nm,
            NotificationCompat.Builder notification, AlarmInstance instance, Context context);

    public void clearNotification(NotificationManagerCompat nm, AlarmInstance instance);
}
