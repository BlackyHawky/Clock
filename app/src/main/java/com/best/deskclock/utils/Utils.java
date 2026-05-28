/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEBUG_LANGUAGE_CODE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SYSTEM_LANGUAGE_CODE;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_ESCALATING;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_HEARTBEAT;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_SOFT;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_STRONG;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_TICK_TOCK;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_KEEP_ANDROID_OPEN_DIALOG;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.OpenableColumns;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.os.LocaleListCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Function;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.BuildConfig;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.CustomDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Utils {

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
     * @param context     the Context in which the PendingIntent should start the service
     * @param intent      an Intent describing the service to be started
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

    /**
     * Convenience method to start a service.
     *
     * @param context The context required to start the service.
     */
    public static void startService(Context context, Class<?> cls) {
        Intent serviceIntent = new Intent(context, cls);

        if (SdkUtils.isAtLeastAndroid8()) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    /**
     * Convenience method to stop a service.
     *
     * @param context The context required to stop the service.
     */
    public static void stopService(Context context, Class<?> cls) {
        Intent serviceIntent = new Intent(context, cls);
        context.stopService(serviceIntent);
    }

    /**
     * Displays a {@link DialogFragment} only if it is not already shown.
     *
     * <p>This method checks whether a fragment with the given tag is already
     * present in the {@link FragmentManager}. If so, the call is ignored to prevent
     * opening the same dialog multiple times (e.g., due to fast repeated clicks).</p>
     *
     * @param manager  the FragmentManager used to display the dialog
     * @param fragment the DialogFragment instance to show
     * @param tag      the unique tag identifying this dialog in the FragmentManager
     */
    public static void showDialogFragment(FragmentManager manager, DialogFragment fragment, String tag) {
        if (manager == null || manager.isDestroyed()) {
            return;
        }

        // Finish any outstanding fragment work.
        manager.executePendingTransactions();

        final Fragment existing = manager.findFragmentByTag(tag);
        // Prevents the same dialog from being opened twice.
        if (existing != null) {
            return;
        }

        final FragmentTransaction tx = manager.beginTransaction();

        tx.addToBackStack(null);
        fragment.show(tx, tag);
    }

    public static long now() {
        return DataModel.getDataModel().elapsedRealtime();
    }

    public static long wallClock() {
        return DataModel.getDataModel().currentTimeMillis();
    }

    /**
     * @return The specific string resource ID corresponding to the active build type.
     */
    @StringRes
    public static int getStringResByBuildType(@StringRes int resId, @StringRes int resDebugId, @StringRes int resNightlyId) {
        if (BuildConfig.IS_DEBUG_BUILD) {
            return resDebugId;
        } else if (BuildConfig.IS_NIGHTLY_BUILD) {
            return resNightlyId;
        } else {
            return resId;
        }
    }

    /**
     * Creates and returns a {@link Context} configured with the user's preferred Locale.
     *
     * <p>If the user selected the system default language, the system Locale is used.
     * Otherwise, a Locale is built from the stored custom language code.</p>
     *
     * @param context The base context used to read preferences and resources.
     * @return A new Context whose configuration applies the selected Locale.
     */
    @SuppressLint("AppBundleLocaleChanges")
    public static Context getLocalizedContext(Context context) {
        String customLanguageCode = SettingsDAO.getLanguageCode(getDefaultSharedPreferences(context));

        if (DEFAULT_SYSTEM_LANGUAGE_CODE.equals(customLanguageCode)) {
            return context;
        }

        Locale locale = Locale.forLanguageTag(customLanguageCode);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    /**
     * Applies the application's language settings during an app reset or backup restore.
     *
     * <p>If the app is being reset, it applies the default language (or a specific language for debug/nightly builds).
     * If the app is being restored, it reads the saved language from shared preferences and applies it.</p>
     *
     * @param context        the context used to access shared preferences
     * @param isResettingApp true if the app is resetting to default settings, false if restoring from a backup
     */
    public static void applyAppLanguage(Context context, boolean isResettingApp) {
        if (isResettingApp) {
            if (BuildConfig.IS_DEBUG_BUILD || BuildConfig.IS_NIGHTLY_BUILD) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(DEBUG_LANGUAGE_CODE));
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            }
        } else {
            SharedPreferences prefs = getDefaultSharedPreferences(context);
            String customLanguageCode = SettingsDAO.getLanguageCode(prefs);

            if (customLanguageCode.equals(DEFAULT_SYSTEM_LANGUAGE_CODE)) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(customLanguageCode));
            }
        }
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
     * Set the vibration duration if the device is equipped with a vibrator and if vibrations are enabled in the settings.
     *
     * @param context      to define whether the device is equipped with a vibrator.
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
     * if the pattern key is unknown, returns a default vibration pattern
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
     * @param keys   List of preference keys to retrieve and cache.
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
                }
                return Uri.fromFile(destFile);
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
        // Normalize accented characters to their base form (é → e, ü → u, etc.) and remove diacritical marks
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        // Replace any remaining non-alphanumeric character (except dot or hyphen) with an underscore
        return normalized.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    /**
     * @return a dialog related to Google's announcement about app development.
     *
     * <p>Note: Clicking the "OK" button will no longer display this dialog box.</p>
     */
    public static AlertDialog displayKeepAndroidOpenDialog(Context context, SharedPreferences prefs, boolean isCancelable) {
        Spanned message = HtmlCompat.fromHtml(context.getString(R.string.keep_android_open_message_italic)
                + context.getString(R.string.keep_android_open_message), HtmlCompat.FROM_HTML_MODE_LEGACY);

        AlertDialog dialog = CustomDialog.create(
            context,
            null,
            AppCompatResources.getDrawable(context, R.drawable.ic_about_article),
            context.getString(R.string.keep_android_open_title),
            message,
            null,
            context.getString(android.R.string.ok),
            (d, w) -> {
                if (prefs.getBoolean(KEY_DISPLAY_KEEP_ANDROID_OPEN_DIALOG, true)) {
                    prefs.edit().putBoolean(KEY_DISPLAY_KEEP_ANDROID_OPEN_DIALOG, false).apply();
                }

                d.dismiss();
            },
            context.getString(R.string.keep_android_open_more_info_button),
            null,
            context.getString(R.string.keep_android_open_solution_button),
            null,
            alertDialog -> {
                Button moreInfoButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (moreInfoButton != null) {
                    moreInfoButton.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://keepandroidopen.org")))
                    );
                }

                Button solutionButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                if (solutionButton != null) {
                    solutionButton.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/woheller69/FreeDroidWarn?tab=readme-ov-file#solutions")))
                    );
                }
            },
            CustomDialog.SoftInputMode.NONE);

        dialog.setCancelable(isCancelable);

        return dialog;
    }

    /**
     * Checks if the user is pressing inside the timer circle or the stopwatch circle.
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
