// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.uicomponents.toast.CustomToast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.Objects;
import java.util.UUID;

public class FileUtils {

    /**
     * Copies the given file to device-protected storage for Direct Boot compatibility.
     *
     * @param context   a context used to resolve storage location
     * @param sourceUri the URI of the source file to copy
     * @param title     a title used to generate a safe filename
     * @return a URI pointing to the copied file in device-protected storage, or null if the copy failed
     */
    @Nullable
    public static Uri copyFileToDeviceProtectedStorage(@NonNull Context context, @NonNull Uri sourceUri, @NonNull String title) {
        final Context storageContext = Utils.getSafeStorageContext(context);

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
    public static long getFileSize(@NonNull Context context, @NonNull Uri uri) {
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
    @NonNull
    public static String toSafeFileName(@NonNull String title) {
        // Normalize accented characters to their base form (é → e, ü → u, etc.) and remove diacritical marks
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        // Replace any remaining non-alphanumeric character (except dot or hyphen) with an underscore
        return normalized.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    /**
     * Opens a file picker allowing the user to select either a font file or an image file.
     *
     * @param launcher   The ActivityResultLauncher used to start the document picker.
     * @param isFontFile True to filter for font files, false to filter for image files.
     */
    public static void selectFile(@NonNull ActivityResultLauncher<Intent> launcher, boolean isFontFile) {
        final String type = isFontFile ? "*/*" : "image/*";
        final String[] mimeTypes = isFontFile
            ? new String[]{"application/x-font-ttf", "application/x-font-otf", "font/ttf", "font/otf"}
            : new String[]{"image/jpeg", "image/png"};

        launcher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(type)
            .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        );
    }

    /**
     * Deletes a file from storage and removes its associated preference entry.
     *
     * @param context     Application context.
     * @param accentStyle The resolved accent color (taking auto night mode into account).
     * @param font        The font used for the Toast message.
     * @param path        The absolute path of the file to delete.
     * @param isFontFile  True if the deleted file is a font, false if it is an image.
     */
    public static void deleteCustomFile(@NonNull Context context, int accentStyle, @Nullable Typeface font, @NonNull String path,
                                        boolean isFontFile) {

        AppExecutors.getDiskIO().execute(() -> {
            clearFile(path);

            AppExecutors.getMainThread().post(() -> CustomToast.show(context, accentStyle, font, isFontFile
                ? R.string.custom_font_toast_message_deleted
                : R.string.background_image_toast_message_deleted)
            );
        });
    }

    /**
     * Deletes the file at the given path if it exists and is a regular file.
     *
     * @param path The absolute path of the file to delete.
     */
    public static void clearFile(@Nullable String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    LogUtils.w("Unable to delete file: " + path);
                }
            }
        }
    }

    /**
     * Scans both standard and device-protected storage to physically delete all custom media files (fonts and background images)
     * used by the application.
     *
     * <p>This prevents file leaks and ensures a completely clean state during a reset or a restore.</p>
     *
     * @param context The context used to access the application's storage directories.
     */
    public static void wipeAllCustomFiles(@NonNull Context context) {
        File[] directoriesToScan = new File[] {
            context.getFilesDir(),
            Utils.getSafeStorageContext(context).getFilesDir()
        };

        for (File dir : directoriesToScan) {
            if (dir != null) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        if (getCustomFilePrefKey(fileName) != null || fileName.startsWith(FILE_SPECIFIC_ALARM_BACKGROUND)) {
                            clearFile(file.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the corresponding preference key for a given custom file (font or background image).
     *
     * @param fileName The name of the file to check.
     * @return The associated preference key, or null if the file is not a recognized custom media file.
     */
    @Nullable
    public static String getCustomFilePrefKey(@NonNull String fileName) {
        if (fileName.startsWith(FILE_GENERAL_FONT)) {
            return KEY_GENERAL_FONT;
        } else if (fileName.startsWith(FILE_ALARM_FONT)) {
            return KEY_ALARM_FONT;
        } else if (fileName.startsWith(FILE_ALARM_BACKGROUND)) {
            return KEY_ALARM_BACKGROUND_IMAGE;
        } else if (fileName.startsWith(FILE_TIMER_FONT)) {
            return KEY_TIMER_DURATION_FONT;
        } else if (fileName.startsWith(FILE_TIMER_BACKGROUND)) {
            return KEY_TIMER_BACKGROUND_IMAGE;
        } else if (fileName.startsWith(FILE_STOPWATCH_FONT)) {
            return KEY_SW_FONT;
        } else if (fileName.startsWith(FILE_SCREENSAVER_DIGITAL_CLOCK_FONT)) {
            return KEY_SCREENSAVER_DIGITAL_CLOCK_FONT;
        } else if (fileName.startsWith(FILE_SCREENSAVER_BACKGROUND)) {
            return KEY_SCREENSAVER_BACKGROUND_IMAGE;
        } else if (fileName.startsWith(FILE_DIGITAL_CLOCK_FONT)) {
            return KEY_DIGITAL_CLOCK_FONT;
        }

        return null;
    }
}
