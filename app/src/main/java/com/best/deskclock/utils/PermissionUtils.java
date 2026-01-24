// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.POWER_SERVICE;

import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.PowerManager;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public class PermissionUtils {

    private static final String PERMISSION_POWER_OFF_ALARM = "org.codeaurora.permission.POWER_OFF_ALARM";

    /**
     * @return {@code true} when Ignore Battery Optimizations permission is granted;
     * {@code false} otherwise.
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        final PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    /**
     * @return {@code true} when Notifications permission is granted; {@code false} otherwise.
     */
    public static boolean areNotificationsEnabled(Context context) {
        if (SdkUtils.isAtLeastAndroid13()) {
            return ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }

    /**
     * @return {@code true} when Full Screen Notifications permission is granted for Android 14+;
     * {@code false} otherwise.
     */
    public static boolean areFullScreenNotificationsEnabled(Context context) {
        if (SdkUtils.isAtLeastAndroid14()) {
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            return notificationManager.canUseFullScreenIntent();
        }

        return false;
    }

    /**
     * @return {@code true} when essential permissions are not granted; {@code false} otherwise.
     */
    public static boolean areEssentialPermissionsNotGranted(Context context) {
        return !isIgnoringBatteryOptimizations(context)
                || !areNotificationsEnabled(context)
                || SdkUtils.isAtLeastAndroid14() && !areFullScreenNotificationsEnabled(context);
    }

    /**
     * Grant or revoke Power Off Alarm permission (available only on specific devices).
     */
     public static void grantPowerOffPermissionForSupportedDevices(FragmentActivity activity) {
         if (DeviceUtils.isPowerOffAlarmUnSupported()) {
             return;
         }

         int codeForPowerOffAlarm = 0;
         if (activity.checkSelfPermission(PERMISSION_POWER_OFF_ALARM) != PackageManager.PERMISSION_GRANTED) {
             activity.requestPermissions(new String[]{PERMISSION_POWER_OFF_ALARM}, codeForPowerOffAlarm);
         }
     }

}
