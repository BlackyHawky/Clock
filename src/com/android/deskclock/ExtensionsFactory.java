package com.android.deskclock;

import android.content.Context;

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
        public void clearNotification(Context context, long alarmId) {
        }

        @Override
        public void sendNotification(Context context, NotificationType notificationType,
                long alarmId, String content, String title) {
        }

        @Override
        public void sendStateChange(Context context, StateChangeType stateChangeType,
                long alarmId) {
        }
    }
}
