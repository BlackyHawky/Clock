// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import android.os.Vibrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DeviceUtils {

    private static final Set<String> POWER_OFF_ALARM_UNSUPPORTED_MANUFACTURERS = Set.of("zte", "huawei");
    private static final Set<String> POWER_OFF_ALARM_UNSUPPORTED_MODELS = Set.of("a103zt", "noh-nx9");

    /**
     * Checks whether the current user is unlocked on the device.
     *
     * <p>This method returns {@code true} if the device is running Android 7.0 (API level 24)
     * or higher and the user has completed the unlock process (e.g., after device boot).</p>
     *
     * <p>Accessing certain data or services before the user is unlocked may cause
     * security exceptions or return null results, so this check is useful when working with
     * file-based encryption or user-specific resources.</p>
     *
     * @param context The context used to retrieve the {@link UserManager} system service.
     * @return {@code true} if the user is unlocked and the SDK version is at least Android 7.0;
     * {@code false} otherwise.
     */
    public static boolean isUserUnlocked(Context context) {
        // Direct Boot doesn't exist before Android 7
        if (!SdkUtils.isAtLeastAndroid7()) {
            return true;
        }

        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        // Can't determine, assume unlocked
        if (userManager == null) {
            return true;
        }

        return userManager.isUserUnlocked();
    }

    /**
     * @return {@code true} if the device is known to not support power-off alarms;
     * {@code false} otherwise.
     */
    public static boolean isPowerOffAlarmUnSupported() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();

        boolean isUnsupportedManufacturer = POWER_OFF_ALARM_UNSUPPORTED_MANUFACTURERS.contains(manufacturer);
        boolean isUnsupportedModel = POWER_OFF_ALARM_UNSUPPORTED_MODELS.contains(model);

        return isUnsupportedManufacturer && isUnsupportedModel;
    }

    /**
     * Check if the device is running MIUI.
     * <p>
     * By default, HyperOS is excluded from verification.
     * If you want to include HyperOS in the verification, pass excludeHyperOS as false.
     *
     * @param excludeHyperOS Indicate whether to exclude HyperOS.
     * @return {@code true} if the device is running MIUI ; {@code false} otherwise.
     */
    public static boolean isMiui(boolean excludeHyperOS) {
        // Check if the device is from Xiaomi, Redmi or POCO.
        String brand = Build.BRAND.toLowerCase();
        Set<String> xiaomiBrands = new HashSet<>(Arrays.asList("xiaomi", "redmi", "poco"));
        if (!xiaomiBrands.contains(brand)) {
            return false;
        }

        // This feature is present in both MIUI and HyperOS.
        String miuiVersion = getProperty("ro.miui.ui.version.name");
        boolean isMiui = miuiVersion != null && !miuiVersion.trim().isEmpty();
        // This feature is exclusive to HyperOS and is not present in MIUI.
        String hyperOSVersion = getProperty("ro.mi.os.version.name");
        boolean isHyperOS = hyperOSVersion != null && !hyperOSVersion.trim().isEmpty();

        return isMiui && (!excludeHyperOS || !isHyperOS);
    }

    /**
     * Private method to get the value of a system property.
     */
    private static String getProperty(String property) {
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec("getprop " + property);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
            return reader.readLine();
        } catch (IOException ignored) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Overload of isMiui method with excludeHyperOS set to true by default.
     */
    public static boolean isMiui() {
        return isMiui(true);
    }

    /**
     * @return {@code true} if a vibrator is available on the device. {@code false} otherwise.
     */
    public static boolean hasVibrator(Context context) {
        Vibrator vibrator = context.getSystemService(Vibrator.class);
        return vibrator != null && vibrator.hasVibrator();
    }

}
