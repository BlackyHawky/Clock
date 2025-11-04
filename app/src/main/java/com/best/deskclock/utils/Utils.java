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
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_ESCALATING;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_HEARTBEAT;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_SOFT;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_STRONG;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_TICK_TOCK;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.OpenableColumns;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.util.Function;

import com.best.deskclock.BuildConfig;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Utils {

    /**
     * Action sent by a broadcast when the application language is changed.
     */
    public static final String ACTION_LANGUAGE_CODE_CHANGED = "com.best.deskclock.LANGUAGE_CODE_CHANGED";

    /**
     * @return {@code true} if the application is in development mode. {@code false} otherwise.
     * <br><p>
     * Note: no need to specify {@code "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE)}
     * because the app is not intended to be deployed in a custom ROM.</p></br>
     */
    public static boolean isDebugConfig() {
        return BuildConfig.DEBUG;
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
     * Returns a context that points to device-protected storage on Android 7+,
     * or the regular context on older versions.
     *
     * @param context the base context
     * @return a context suitable for accessing device-protected storage
     */
    public static Context getSafeStorageContext(Context context) {
        return SdkUtils.isAtLeastAndroid7()
                ? context.createDeviceProtectedStorageContext()
                : context;
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
     * Returns the vibration pattern associated with the given pattern key.
     *
     * @param patternKey the key identifying the vibration pattern
     * @return a long array representing the vibration pattern durations in milliseconds;
     *         if the pattern key is unknown, returns a default vibration pattern
     */
    public static long[] getVibrationPatternForKey(String patternKey) {
        return switch (patternKey) {
            case VIBRATION_PATTERN_SOFT -> new long[]{500, 200, 500};
            case VIBRATION_PATTERN_STRONG -> new long[]{500, 1000};
            case VIBRATION_PATTERN_HEARTBEAT -> new long[]{100, 100, 300, 100, 600};
            case VIBRATION_PATTERN_ESCALATING -> new long[]{500, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
            case VIBRATION_PATTERN_TICK_TOCK -> new long[]{300, 150, 300, 150};
            default -> new long[]{500, 500};
        };
    }

    /**
     * Initializes a cache map holding the current values of the given preferences.
     * <p>
     * This cache is used to compare old and new values during preference changes,
     * preventing unnecessary actions if the value has not actually changed.</p>
     *
     * @param keys List of preference keys to retrieve and cache.
     * @param getter Function that returns the value for a given key.
     * @return A map of preference keys to their current values.
     */
    public static Map<String, Object> initCachedValues(List<String> keys, Function<String, Object> getter) {
        Map<String, Object> cached = new HashMap<>();
        for (String key : keys) {
            cached.put(key, getter.apply(key));
        }
        return cached;
    }

    /**
     * Copies the given file to device-protected storage for Direct Boot compatibility.
     *
     * @param context   a context used to resolve storage location
     * @param sourceUri the URI of the source file to copy
     * @param title     a title used to generate a safe filename
     * @return a URI pointing to the copied file in device-protected storage, or null if the copy failed
     */
    public static Uri copyFileToDeviceProtectedStorage(Context context, Uri sourceUri, String title) {
        final Context storageContext = getSafeStorageContext(context);

        long sourceSize = getFileSize(storageContext, sourceUri);

        File[] existingFiles = storageContext.getFilesDir().listFiles();
        String safeTitle = toSafeFileName(title);

        if (existingFiles != null) {
            for (File file : existingFiles) {
                if (file.getName().startsWith(safeTitle)) {
                    if (file.length() == sourceSize) {
                        // Already copied
                        return Uri.fromFile(file);
                    }
                }
            }
        }

        // Copy if not found
        String filename = safeTitle + "_" + UUID.randomUUID().toString();
        File destFile = new File(storageContext.getFilesDir(), filename);
        try (InputStream inputStream = storageContext.getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(destFile)) {
            if (inputStream != null) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                } return Uri.fromFile(destFile);
            } else {
                LogUtils.e("InputStream null for URI: " + sourceUri);
            }
        } catch (IOException e) {
            LogUtils.e("Failed to copy ringtone", e);
        }

        return null;
    }

    /**
     * Gets the size of a file for mixed uri formats.
     *
     * <p>File pickers usually use {@code content://} but files stored in DeviceProtected storage
     * use {@code file://}.</p>
     */
    public static long getFileSize(Context context, Uri uri) {
        long size = -1;

        String scheme = uri.getScheme();
        if ("file".equalsIgnoreCase(scheme)) {
            File file = new File(Objects.requireNonNull(uri.getPath()));
            if (file.exists()) {
                size = file.length();
            }
        } else if ("content".equalsIgnoreCase(scheme)) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (!cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            }
        }

        // As a fallback: read the whole stream
        if (size < 0) {
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream != null) {
                    byte[] buffer = new byte[8192];
                    int read;
                    size = 0;
                    while ((read = inputStream.read(buffer)) != -1) {
                        size += read;
                    }
                }
            } catch (IOException e) {
                LogUtils.e("Failed to determine file size of ringtone", e);
            }
        }

        return size;
    }

    /**
     * Converts a given file title into a "safe" filename that can be stored
     * in the app's private storage without issues.
     *
     * <p>This method performs two main steps:
     * <ol>
     *   <li>Normalization of accented characters (e.g., é → e, à → a) to ensure ASCII compatibility.</li>
     *   <li>Replacement of any character not allowed in filenames (anything other than
     *       letters, digits, dot, or hyphen) with an underscore '_'.</li>
     * </ol>
     *
     * @param title The file title, possibly containing accents or special characters.
     * @return A sanitized string that can be safely used as a filename in app storage.
     */
    public static String toSafeFileName(String title) {
        // Normalize accented characters to their base form (é → e, ü → u, etc.)
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // Remove diacritical marks

        // Replace any remaining non-alphanumeric character (except dot or hyphen) with an underscore
        return normalized.replaceAll("[^a-zA-Z0-9.\\-]", "_");
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
