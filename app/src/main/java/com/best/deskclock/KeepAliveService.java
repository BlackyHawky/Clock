// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import static com.best.deskclock.utils.NotificationUtils.FOREGROUND_SERVICE_CHANNEL_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;

/**
 * A Foreground Service designed to keep the application process alive in the background.
 *
 * <p>By displaying a persistent notification, this service elevates the process priority,
 * significantly reducing the chance of the Android system killing the app to save memory
 * or battery.</p>
 */
public class KeepAliveService extends Service {

    /**
     * This value is coordinated with notification ids from
     * {link com.best.deskclock.data.NotificationModel}
     */
    public static final int FOREGROUND_SERVICE_NOTIFICATION_ID = Integer.MAX_VALUE - 6;

    @Override
    public void onCreate() {
        super.onCreate();

        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.createChannel(this, FOREGROUND_SERVICE_CHANNEL_ID);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.v("KeepAliveService.onStartCommand() with %s", intent);

        Intent notificationIntent = new Intent(this, DeskClock.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, FOREGROUND_SERVICE_CHANNEL_ID)
            .setContentTitle(getString(Utils.getStringResByBuildType(
                R.string.app_label, R.string.app_label_debug, R.string.app_label_nightly)))
            .setContentText(getString(R.string.foreground_service_message))
            .setColor(getColor(R.color.md_theme_primary))
            .setSmallIcon(R.drawable.ic_tab_alarm_static)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .build();

        int foregroundServiceType = 0;

        if (SdkUtils.isAtLeastAndroid14()) {
            foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
        }

        ServiceCompat.startForeground(this, FOREGROUND_SERVICE_NOTIFICATION_ID, notification, foregroundServiceType);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LogUtils.v("KeepAliveService.onDestroy() called");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
