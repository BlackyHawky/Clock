/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SYSTEM_LANGUAGE_CODE;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import com.best.deskclock.BuildConfig;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;

import java.text.NumberFormat;
import java.util.Locale;

public class Utils {

    /**
     * Action sent by a broadcast when the application language is changed.
     */
    public static final String ACTION_LANGUAGE_CODE_CHANGED = "com.best.deskclock.LANGUAGE_CODE_CHANGED";

    /**
     * @return {@code true} if the application is in development mode (debug, eng or userdebug).
     * {@code false} otherwise.
     */
    public static boolean isDebugConfig() {
        return BuildConfig.DEBUG || "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
    }

    public static void enforceMainLooper() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalAccessError("May only call from main thread.");
        }
    }

    public static void enforceNotMainLooper() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalAccessError("May not call from main thread.");
        }
    }

    /**
     * @param view the scrollable view to test
     * @return {@code true} iff the {@code view} content is currently scrolled to the top
     */
    public static boolean isScrolledToTop(View view) {
        return !view.canScrollVertically(-1);
    }

    /**
     * Update and return the PendingIntent corresponding to the given {@code intent}.
     *
     * @param context the Context in which the PendingIntent should start the service
     * @param intent  an Intent describing the service to be started
     * @return a PendingIntent that will start a service
     */
    public static PendingIntent pendingServiceIntent(Context context, Intent intent) {
        return PendingIntent.getService(context, 0, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    /**
     * Creates and returns a {@link PendingIntent} to start a service, using a specific
     * {@code requestCode} to distinguish intents.
     *
     * @param context the Context in which the PendingIntent should start the service
     * @param intent  an Intent describing the service to be started
     * @param requestCode a unique identifier to differentiate between multiple PendingIntents
     */
    public static PendingIntent pendingServiceIntent(Context context, Intent intent, int requestCode) {
        return PendingIntent.getService(context, requestCode, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    /**
     * Update and return the PendingIntent corresponding to the given {@code intent}.
     *
     * @param context the Context in which the PendingIntent should start the activity
     * @param intent  an Intent describing the activity to be started
     * @return a PendingIntent that will start an activity
     */
    public static PendingIntent pendingActivityIntent(Context context, Intent intent) {
        // explicitly set the flag here, as getActivity() documentation states we must do so
        return PendingIntent.getActivity(context, 0, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public static String getNumberFormattedQuantityString(Context context, int id, int quantity) {
        final String localizedQuantity = NumberFormat.getInstance().format(quantity);
        return context.getResources().getQuantityString(id, quantity, localizedQuantity);
    }

    public static long now() {
        return DataModel.getDataModel().elapsedRealtime();
    }

    public static long wallClock() {
        return DataModel.getDataModel().currentTimeMillis();
    }

    /**
     * @param context The context from which to obtain strings
     * @param hours   Hours to display (if any)
     * @param minutes Minutes to display (if any)
     * @param seconds Seconds to display
     * @return Provided time formatted as a String
     */
    public static String getTimeString(Context context, int hours, int minutes, int seconds) {
        if (hours != 0) {
            return context.getString(R.string.hours_minutes_seconds, hours, minutes, seconds);
        }
        if (minutes != 0) {
            return context.getString(R.string.minutes_seconds, minutes, seconds);
        }
        return context.getString(R.string.seconds_only, seconds);
    }

    /**
     * Applies the specified locale to the given context by setting the locale
     * to the resources' configuration. If the custom language is set to the system language,
     * the system's locale is applied. Otherwise, a new locale is created using the custom language.
     * <p>
     * This method sets the default locale for the application and updates the configuration
     * of the context to reflect the locale change.
     *
     * @param context The context in which the locale should be applied.
     * @param customLanguageCode The custom language code (e.g., "en", "fr")
     *                           or a special keyword for the system language.
     */
    @SuppressLint("AppBundleLocaleChanges")
    public static void applySpecificLocale(Context context, String customLanguageCode) {
        Locale locale;

        if (DEFAULT_SYSTEM_LANGUAGE_CODE.equals(customLanguageCode)) {
            if (SdkUtils.isAtLeastAndroid7()) {
                locale = Resources.getSystem().getConfiguration().getLocales().get(0);
            } else {
                locale = Resources.getSystem().getConfiguration().locale;
            }
        } else {
            String[] parts = customLanguageCode.split("_");
            if (parts.length == 2) {
                locale = new Locale(parts[0], parts[1]);
            } else {
                locale = new Locale(customLanguageCode);
            }
        }

        Locale.setDefault(locale);

        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);
    }

    /**
     * Apply a custom locale and return a localized context.
     *
     * @param context the context in which the locale is to be applied.
     * @return a localized context based on the custom language.
     */
    public static Context getLocalizedContext(Context context) {
        String customLanguageCode = SettingsDAO.getCustomLanguageCode(getDefaultSharedPreferences(context));
        applySpecificLocale(context, customLanguageCode);
        return context.createConfigurationContext(context.getResources().getConfiguration());
    }

    /**
     * @return {@code true} if a vibrator is available on the device. {@code false} otherwise.
     */
    public static boolean hasVibrator(Context context) {
        Vibrator vibrator = context.getSystemService(Vibrator.class);
        return vibrator != null && vibrator.hasVibrator();
    }

    /**
     * Set the vibration duration if the device is equipped with a vibrator and if vibrations are enabled in the settings.
     *
     * @param context to define whether the device is equipped with a vibrator.
     * @param milliseconds Hours to display (if any)
     */
    public static void setVibrationTime(Context context, long milliseconds) {
        final boolean isVibrationsEnabled = SettingsDAO.isVibrationsEnabled(getDefaultSharedPreferences(context));
        final Vibrator vibrator = context.getSystemService(Vibrator.class);
        if (isVibrationsEnabled) {
            if (SdkUtils.isAtLeastAndroid8()) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(milliseconds);
            }
        }
    }

    /**
     * Checks if the user is pressing inside of the timer circle or the stopwatch circle.
     */
    public static final class CircleTouchListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            final int actionMasked = event.getActionMasked();
            if (actionMasked != MotionEvent.ACTION_DOWN) {
                return false;
            }
            final float rX = view.getWidth() / 2f;
            final float rY = (view.getHeight() - view.getPaddingBottom()) / 2f;
            final float r = Math.min(rX, rY);

            final float x = event.getX() - rX;
            final float y = event.getY() - rY;

            final boolean inCircle = Math.pow(x / r, 2.0) + Math.pow(y / r, 2.0) <= 1.0;

            // Consume the event if it is outside the circle
            return !inCircle;
        }
    }

}
