// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import android.os.Build;

/**
 * Utility class centralizing Android SDK version checks.
 */
public class SdkUtils {

    /**
     * Generic method.
     *
     * @return {@code true} if the current version is at least equal to {@code versionCode}.
     * {@code false}otherwise.
     */
    public static boolean isAtLeastVersion(int versionCode) {
        return Build.VERSION.SDK_INT >= versionCode;
    }

    /**
     * @return {@code true} if the API version is greater than or equal to 24 (Nougat).
     * {@code false} otherwise.
     */
    public static boolean isAtLeastAndroid7() {
        return isAtLeastVersion(Build.VERSION_CODES.N);
    }

    /**
     * @return {@code true} if the API version is greater than or equal to 25 (Nougat - Android 7.1).
     * {@code false} otherwise.
     */
    public static boolean isAtLeastAndroid71() {
        return isAtLeastVersion(Build.VERSION_CODES.N_MR1);
    }

    /**
     * @return {@code true} if the API version is before 26 (Oreo).
     * {@code false} otherwise.
     */
    public static boolean isBeforeAndroid8() {
        return !isAtLeastAndroid8();
    }

    /**
     * @return {@code true} if the API version is greater than or equal to 26 (Oreo).
     * {@code false} otherwise.
     */
    public static boolean isAtLeastAndroid8() {
        return isAtLeastVersion(Build.VERSION_CODES.O);
    }

    /**
     * @return {@code true} if the API version is greater than or equal to 27 (Oreo - Android 8.1).
     * {@code false} otherwise.
     */
    public static boolean isAtLeastAndroid81() {
        return isAtLeastVersion(Build.VERSION_CODES.O_MR1);
    }

    /**
     * @return {@code true} if the API version is greater than or equal to 28 (Pie).
     * {@code false} otherwise.
     */
    public static boolean isAtLeastAndroid9() {
        return isAtLeastVersion(Build.VERSION_CODES.P);
    }

    /**
     * @return {@code true} if the API version is greater than or equal to 29 (Quince Tart).
     * {@code false} otherwise.
     */
    public static boolean isAtLeastAndroid10() {
        return isAtLeastVersion(Build.VERSION_CODES.Q);
    }

    /**
     * @return {@code true} if the API version is greater than or equal to 30 (Red Velvet Cake).
     * {@code false} otherwise.
     */
    public static boolean isAtLeastAndroid11() {
        return isAtLeastVersion(Build.VERSION_CODES.R);
    }

    /**
     * @return {@code true} if the API version is greater than or equal to 31 (Snow Cone).
     * {@code false} otherwise.
     */
    public static boolean isAtLeastAndroid12() {
        return isAtLeastVersion(Build.VERSION_CODES.S);
    }

    /**
     * @return {@code true} if the API version is greater than or equal to 33 (Tiramisu).
     * {@code false} otherwise.
     */
    public static boolean isAtLeastAndroid13() {
        return isAtLeastVersion(Build.VERSION_CODES.TIRAMISU);
    }

    /**
     * @return {@code true} if the API version is greater than or equal to 34 (Upside Down Cake).
     * {@code false} otherwise.
     */
    public static boolean isAtLeastAndroid14() {
        return isAtLeastVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
    }
}
