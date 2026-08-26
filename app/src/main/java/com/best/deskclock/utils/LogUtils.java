/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.BuildConfig;
import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.SettingsDAO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class LogUtils {

    /**
     * Default logger used for generic logging, i.eTAG. when a specific log tag isn't specified.
     */
    private final static Logger DEFAULT_LOGGER = new Logger("Clock");

    public static void v(@NonNull String message, @NonNull Object... args) {
        DEFAULT_LOGGER.v(message, args);
    }

    public static void d(@NonNull String message, @NonNull Object... args) {
        DEFAULT_LOGGER.d(message, args);
    }

    public static void i(@NonNull String message, @NonNull Object... args) {
        DEFAULT_LOGGER.i(message, args);
    }

    public static void w(@NonNull String message, @NonNull Object... args) {
        DEFAULT_LOGGER.w(message, args);
    }

    public static void e(@NonNull String message, @NonNull Object... args) {
        DEFAULT_LOGGER.e(message, args);
    }

    public static void e(@NonNull String message, @NonNull Throwable e) {
        DEFAULT_LOGGER.e(message, e);
    }

    public static void wtf(@NonNull String message, @NonNull Object... args) {
        DEFAULT_LOGGER.wtf(message, args);
    }

    public static void wtf(@NonNull Throwable e) {
        DEFAULT_LOGGER.wtf(e);
    }

    /**
     * Retrieve locally saved custom logs via LogUtils.
     * These logs are usually stored in an internal file of the application.
     */
    @NonNull
    public static String getSavedLocalLogs(@NonNull Context context) {
        File localLogFile = getLocalLogFile(context);
        if (!localLogFile.exists()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(localLogFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e("LogUtils", "Error reading local log file", e);
        }

        return builder.toString();
    }

    /**
     * Delete the local file where custom logs are saved.
     */
    public static void clearSavedLocalLogs(@NonNull Context context) {
        File localLogFile = getLocalLogFile(context);
        if (localLogFile.exists()) {
            boolean deleted = localLogFile.delete();
            if (!deleted) {
                Log.e("LogUtils", "Failed to delete local log file");
            }
        }
    }

    /**
     * Return the local file where custom logs are saved.
     * The file is located in the application's private directory (accessible via getFilesDir()).
     */
    @NonNull
    private static File getLocalLogFile(@NonNull Context context) {
        Context storageContext = Utils.getSafeStorageContext(context);
        return new File(storageContext.getFilesDir(), "log_utils_logs.txt");
    }

    /**
     * Add a line of text to the end of the custom log file.
     */
    private static void appendToFile(@NonNull Context context, @NonNull String logLine) {
        try (FileWriter writer = new FileWriter(getLocalLogFile(context), true)) {
            writer.write(logLine + "\n");
        } catch (IOException e) {
            Log.e("LogUtils", "Error writing to local log file", e);
        }
    }

    /**
     * Generates a header containing basic device and application information.
     * <p>
     * The header includes:
     * <ul>
     *   <li>Device manufacturer</li>
     *   <li>Device model</li>
     *   <li>Device code name</li>
     *   <li>Android version and SDK level</li>
     *   <li>Application version name and code</li>
     * </ul>
     * An empty line is added at the end to visually separate the header from the logs.
     *
     * @return A formatted string containing the log file header.
     */
    @NonNull
    public static String generateLocalLogFileHeader() {
        return "Device Manufacturer: " + Build.MANUFACTURER + "\n" +
            "Device Model: " + Build.MODEL + "\n" +
            "Device Code Name: " + Build.DEVICE + "\n" +
            "Android Version: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")" + "\n" +
            "App Version: " + BuildConfig.VERSION_NAME + " (Code " + BuildConfig.VERSION_CODE + ")" + "\n" +
            // Empty line to separate header and logs
            "\n";
    }

    public record Logger(@NonNull String logTag) {

        private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<>() {
            @NonNull
            @Override
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            }
        };

        private boolean isLoggingEnabled() {
            Context context = DeskClockApplication.getAppContext();
            if (context == null) return false;

            SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(context);
            return BuildConfig.DEBUG || SettingsDAO.isDebugSettingsDisplayed(prefs);
        }

        @NonNull
        private String format(@NonNull String level, @NonNull String message, @Nullable Object... args) {
            String formatted = (args == null || args.length == 0) ? message : String.format(message, args);
            String timestamp = Objects.requireNonNull(DATE_FORMAT.get()).format(new Date());
            return timestamp + " [" + level + "] " + logTag + ": " + formatted;
        }

        public void v(@NonNull String message, @NonNull Object... args) {
            if (isVerboseLoggable()) {
                String log = format("VERBOSE", message, args);
                Log.v(logTag, log);
                appendToFileCompat(log);
            }
        }

        public void d(@NonNull String message, @NonNull Object... args) {
            if (isDebugLoggable()) {
                String log = format("DEBUG", message, args);
                Log.d(logTag, log);
                appendToFileCompat(log);
            }
        }

        public void i(@NonNull String message, @NonNull Object... args) {
            if (isInfoLoggable()) {
                String log = format("INFO", message, args);
                Log.i(logTag, log);
                appendToFileCompat(log);
            }
        }

        public void w(@NonNull String message, @NonNull Object... args) {
            if (isWarnLoggable()) {
                String log = format("WARN", message, args);
                Log.w(logTag, log);
                appendToFileCompat(log);
            }
        }

        public void e(@NonNull String message, @NonNull Object... args) {
            if (isErrorLoggable()) {
                String log = format("ERROR", message, args);
                Log.e(logTag, log);
                appendToFileCompat(log);
            }
        }

        public void e(@NonNull String message, @NonNull Throwable e) {
            if (isErrorLoggable()) {
                String log = format("ERROR", message) + " — " + Log.getStackTraceString(e);
                Log.e(logTag, log);
                appendToFileCompat(log);
            }
        }

        public void wtf(@NonNull String message, @NonNull Object... args) {
            if (isWtfLoggable()) {
                String log = format("WTF", message, args);
                Log.wtf(logTag, log);
                appendToFileCompat(log);
            }
        }

        public void wtf(@NonNull Throwable e) {
            if (isWtfLoggable()) {
                String log = format("WTF", "Exception") + " — " + Log.getStackTraceString(e);
                Log.wtf(logTag, log);
                appendToFileCompat(log);
            }
        }

        private void appendToFileCompat(@NonNull String log) {
            AppExecutors.getDiskIO().execute(() -> {
                Context context = DeskClockApplication.getAppContext();
                if (context != null) {
                    appendToFile(context, log);
                }
            });
        }

        public boolean isVerboseLoggable() {
            return isLoggingEnabled() || Log.isLoggable(logTag, Log.VERBOSE);
        }

        public boolean isDebugLoggable() {
            return isLoggingEnabled() || Log.isLoggable(logTag, Log.DEBUG);
        }

        public boolean isInfoLoggable() {
            return isLoggingEnabled() || Log.isLoggable(logTag, Log.INFO);
        }

        public boolean isWarnLoggable() {
            return isLoggingEnabled() || Log.isLoggable(logTag, Log.WARN);
        }

        public boolean isErrorLoggable() {
            return isLoggingEnabled() || Log.isLoggable(logTag, Log.ERROR);
        }

        public boolean isWtfLoggable() {
            return isLoggingEnabled() || Log.isLoggable(logTag, Log.ASSERT);
        }
    }
}
