package com.android.deskclock;

import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.android.deskclock.provider.AlarmInstance;

public class ExtensionsFactory {
    private static DeskClockExtensions sDeskClockExtensions = null;

    private ExtensionsFactory() { }

    public static void init(DeskClockExtensions extensions) {
        sDeskClockExtensions = extensions;
    }

    public static DeskClockExtensions getDeskClockExtensions() {
        if (sDeskClockExtensions == null) {
            sDeskClockExtensions = new NullDeskClockExtensions();
        }

        return sDeskClockExtensions;
    }

    private static class NullDeskClockExtensions implements DeskClockExtensions {
        @Override
        public void sendNotification(NotificationManagerCompat nm,
                NotificationCompat.Builder notification, AlarmInstance instance) { }

        @Override
        public void sendNotification(NotificationManagerCompat nm,
                NotificationCompat.Builder notification, AlarmInstance instance, Context context) {
        }

        @Override
        public void clearNotification(NotificationManagerCompat nm, AlarmInstance instance) {
        }
    }
}
