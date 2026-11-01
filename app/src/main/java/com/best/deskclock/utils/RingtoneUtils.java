// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.FILE_SYSTEM_RINGTONE;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Looper;
import android.view.KeyEvent;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.IntentCompat;

import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.R;
import com.best.deskclock.data.CustomRingtone;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.RingtoneModel;
import com.best.deskclock.provider.Alarm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class RingtoneUtils {

    /**
     * The ringtone preview duration in ms.
     */
    public static final long ALARM_PREVIEW_DURATION_MS = 5000;

    /**
     * Broadcast intent action when the volume for a particular stream type changes.
     *
     * <p>Note: We define this constant manually because {@code AudioManager.VOLUME_CHANGED_ACTION}
     * is marked as {@code @hide} and {@code @UnsupportedAppUsage} in the Android SDK,
     * preventing direct access.</p>
     */
    public static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";

    /**
     * The volume applied to the player during a call.
     */
    public static final float IN_CALL_VOLUME = 0.14f;

    /**
     * {@link Uri} signifying the "silent" ringtone.
     */
    public static final Uri RINGTONE_SILENT = Uri.EMPTY;

    /**
     * {@link Uri} signifying the "random" ringtone.
     */
    public static final Uri RANDOM_RINGTONE = Uri.parse("random");

    /**
     * {@link Uri} signifying the "random custom" ringtone.
     */
    public static final Uri RANDOM_CUSTOM_RINGTONE = Uri.parse("random_custom");

    /**
     * @return {@code true} if the URI starts with one of the possible system directories for ringtones.
     * {@code false} otherwise.
     * <p>This excludes custom ringtones that cause problems during restoration.</p>
     */
    public static boolean isSystemRingtone(@NonNull Uri uri) {
        String uriString = uri.toString().toLowerCase();
        return (uriString.startsWith("content://media/external/audio/") ||
            uriString.startsWith("content://media/internal/audio/") ||
            uriString.startsWith("content://media/") ||
            uriString.startsWith("content://settings/") ||
            uriString.startsWith("file:///system/media/audio/") ||
            uriString.startsWith("file:///system/media/"));
    }

    /**
     * @return {@code true} if the URI represents a random ringtone; {@code false} otherwise.
     */
    public static boolean isRandomRingtone(@NonNull Uri uri) {
        return RANDOM_RINGTONE.equals(uri);
    }

    /**
     * @return {@code true} if the URI represents a random custom ringtone; {@code false} otherwise.
     */
    public static boolean isRandomCustomRingtone(@NonNull Uri uri) {
        return RANDOM_CUSTOM_RINGTONE.equals(uri);
    }

    /**
     * @param resourceId identifies an application resource
     * @return the Uri by which the application resource is accessed
     */
    public static Uri getResourceUri(@NonNull Context context, @AnyRes int resourceId) {
        return new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.getPackageName())
            .path(String.valueOf(resourceId))
            .build();
    }

    /**
     * @return {@code true} if the given URI of a ringtone is readable by the application.
     * {@code false} otherwise.
     */
    public static boolean isRingtoneUriReadable(@NonNull Context context, @NonNull Uri uri) {
        if (RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).equals(uri)) {
            uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
        }

        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            return stream != null;
        } catch (Exception e) {
            LogUtils.e("Ringtone URI is not readable: " + uri, e);
            return false;
        }
    }

    /**
     * Creates and activates a MediaSession to intercept headphones buttons.
     *
     * @param context    The context for obtaining the MediaSession
     * @param sessionTag The session name
     * @param onAction   Callback executed when a Play/Pause/Stop button is pressed
     * @return the active MediaSession
     */
    @NonNull
    public static MediaSession createMediaSession(@NonNull Context context, @NonNull String sessionTag, @NonNull Runnable onAction) {
        MediaSession mediaSession = new MediaSession(context, sessionTag);

        PlaybackState state = new PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP | PlaybackState.ACTION_PAUSE)
            .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build();

        mediaSession.setPlaybackState(state);

        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonEvent) {
                KeyEvent keyEvent = IntentCompat.getParcelableExtra(mediaButtonEvent, Intent.EXTRA_KEY_EVENT, KeyEvent.class);

                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = keyEvent.getKeyCode();

                    if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_STOP ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {

                        onAction.run();

                        return true;
                    }
                }

                return super.onMediaButtonEvent(mediaButtonEvent);
            }
        });

        mediaSession.setActive(true);

        return mediaSession;
    }

    /**
     * Creates and prepares a {@link MediaPlayer} instance to play a ringtone.
     *
     * @return A prepared {@link MediaPlayer} instance if successful,
     * or {@code null} if preparation fails.
     */
    @Nullable
    public static MediaPlayer createPreparedMediaPlayer(@NonNull Context context, @NonNull Uri... ringtoneUris) {
        // Use a DirectBoot aware context if supported
        Context storageContext = Utils.getSafeStorageContext(context.getApplicationContext());

        MediaPlayer player = new MediaPlayer();

        player.setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build());

        for (Uri uri : ringtoneUris) {
            if (uri == null) {
                continue;
            }

            try {
                LogUtils.d("Trying to prepare MediaPlayer for URI: " + uri);
                player.reset();

                // If it is the default URI, resolve its real URI
                if (Objects.equals(uri, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))) {
                    Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(storageContext, RingtoneManager.TYPE_ALARM);
                    if (actualUri != null) {
                        uri = actualUri;
                    }
                }

                Uri stableUri = uri.buildUpon().clearQuery().build();
                String safeUriId = stableUri.toString().replaceAll("[^a-zA-Z0-9.\\-]", "_");
                File expectedLocalCopy = new File(storageContext.getFilesDir(), FILE_SYSTEM_RINGTONE + safeUriId);

                if (expectedLocalCopy.exists() && expectedLocalCopy.length() > 0) {
                    LogUtils.d("Direct Boot: Using local copied ringtone: " + expectedLocalCopy.getName());
                    player.setDataSource(storageContext, Uri.fromFile(expectedLocalCopy));
                } else {
                    player.setDataSource(storageContext, uri);
                }

                player.prepare();
                return player;
            } catch (Exception e) {
                if (!DeviceUtils.isUserUnlocked(storageContext) && uri.toString().startsWith("content://media")) {
                    LogUtils.w("MediaStore URI not accessible before unlock: " + uri);
                } else {
                    LogUtils.e("Failed to prepare MediaPlayer for URI: " + uri, e);
                }
            }
        }

        player.release();
        return null;
    }

    /**
     * @return the duration of the ringtone.
     */
    public static int getRingtoneDuration(@NonNull Context context, @NonNull Uri ringtoneUri) {
        MediaPlayer player = createPreparedMediaPlayer(context, ringtoneUri, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

        if (player == null) {
            return 0;
        }

        int duration = player.getDuration();
        LogUtils.d("Ringtone duration for URI " + ringtoneUri + " = " + duration + " ms");
        player.release();
        return duration;
    }

    /**
     * Returns a randomly selected system alarm ringtone URI.
     *
     * <p>If no valid ringtones are found, the system's default alarm ringtone is returned.</p>
     */
    public static Uri getRandomRingtoneUri() {
        Context context = DeskClockApplication.getAppContext();
        Context safeContext = Utils.getSafeStorageContext(context);

        File filesDir = safeContext.getFilesDir();
        if (filesDir != null) {
            File[] copiedFiles = filesDir.listFiles((dir, name) -> name.startsWith(FILE_SYSTEM_RINGTONE));
            if (copiedFiles != null && copiedFiles.length > 0) {
                File randomFile = copiedFiles[new Random().nextInt(copiedFiles.length)];

                LogUtils.d("Random: Prioritizing local copy:" + randomFile.getName());
                return Uri.fromFile(randomFile);
            }
        }

        LogUtils.w("Random: No local copies found, querying MediaStore directly.");

        List<Uri> uris = getSystemRingtoneUris(safeContext);
        if (!uris.isEmpty()) {
            return uris.get(new Random().nextInt(uris.size()));
        }

        LogUtils.w("Random: No system ringtones and no local copies found. Falling back to default URI");
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    }

    @NonNull
    private static List<Uri> getSystemRingtoneUris(@NonNull Context context) {
        RingtoneManager manager = new RingtoneManager(context.getApplicationContext());
        manager.setType(RingtoneManager.TYPE_ALARM);

        List<Uri> uris = new ArrayList<>();

        try (Cursor cursor = manager.getCursor()) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Uri uri = manager.getRingtoneUri(cursor.getPosition());
                    if (uri != null) {
                        uris.add(uri);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            LogUtils.e("Failed to retrieve system ringtone URIs", e);
        }

        return uris;
    }

    /**
     * Returns a randomly selected custom ringtone URI.
     * <p>
     * If no valid ringtones are found, the system's default alarm ringtone is returned.
     */
    public static Uri getRandomCustomRingtoneUri() {
        Context context = DeskClockApplication.getAppContext();
        Context safeContext = Utils.getSafeStorageContext(context);

        List<Uri> uris = new ArrayList<>();

        if (Looper.myLooper() == Looper.getMainLooper()) {
            for (CustomRingtone custom : DataModel.getDataModel().getCustomRingtones()) {
                if (isRingtoneUriReadable(safeContext, custom.getUri())) {
                    uris.add(custom.getUri());
                }
            }
        } else {
            SharedPreferences prefs = getDefaultSharedPreferences(safeContext);
            RingtoneModel ringtoneModel = new RingtoneModel(safeContext, prefs);

            for (CustomRingtone custom : ringtoneModel.getCustomRingtones()) {
                if (isRingtoneUriReadable(safeContext, custom.getUri())) {
                    uris.add(custom.getUri());
                }
            }

            ringtoneModel.releaseResources();
        }

        if (uris.isEmpty()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        return uris.get(new Random().nextInt(uris.size()));
    }

    /**
     * Checks if the URI is already pointing to a protected local copy of the ringtone.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isCopiedSystemRingtone(@NonNull Context context, @NonNull Uri uri) {
        Context safeContext = Utils.getSafeStorageContext(context);
        File filesDir = safeContext.getFilesDir();
        return uri.toString().startsWith(Uri.fromFile(filesDir).toString());
    }

    /**
     * Physically copies the ringtone to the Device Protected Storage.
     */
    public static void copySystemRingtoneToSafeStorage(@NonNull Context context, @NonNull Uri uri) {
        Context safeContext = Utils.getSafeStorageContext(context);

        // If it is the default URI, resolve its real URI before copying
        if (Objects.equals(uri, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))) {
            Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
            if (actualUri != null) {
                uri = actualUri;
            }
        }

        // File name
        Uri stableUri = uri.buildUpon().clearQuery().build();
        String safeTitle = stableUri.toString().replaceAll("[^a-zA-Z0-9.\\-]", "_");
        String filename = FILE_SYSTEM_RINGTONE + safeTitle;
        File destFile = new File(safeContext.getFilesDir(), filename);

        // Stop if the copy already exists
        if (destFile.exists() && destFile.length() > 0) {
            LogUtils.d("System ringtone already copied: " + destFile.getAbsolutePath());
            return;
        }

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(destFile)) {
            if (inputStream != null) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                LogUtils.i("Successfully copied system ringtone to: " + destFile.getAbsolutePath());
            } else {
                LogUtils.e("Failed to open input stream for system URI: " + uri);
            }
        } catch (Exception e) {
            LogUtils.e("Failed to copy system ringtone to device protected storage", e);
        }

        // If the copy failed and left a 0-byte shell, check if the file is deleted.
        if (destFile.exists() && destFile.length() == 0) {
            boolean deleted = destFile.delete();
            if (deleted) {
                LogUtils.w("WARNING: Copied file is 0 bytes! Successfully deleted corrupted file: " + destFile.getName());
            } else {
                LogUtils.e("WARNING: Copied file is 0 bytes, but failed to delete corrupted file: " + destFile.getName());
            }
        }
    }

    /**
     * Copies all system alarm ringtones to Device Protected Storage.
     * Used for random system ringtone during Direct Boot.
     */
    public static void copyAllSystemRingtonesToSafeStorage(@NonNull Context context) {
        RingtoneManager manager = new RingtoneManager(context);
        manager.setType(RingtoneManager.TYPE_ALARM);

        try (Cursor cursor = manager.getCursor()) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Uri uri = manager.getRingtoneUri(cursor.getPosition());
                    if (uri != null) {
                        copySystemRingtoneToSafeStorage(context, uri);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            LogUtils.e("Failed to copy all system ringtones", e);
        }
    }

    /**
     * Migrates all system ringtones currently used by alarms to the device's protected storage.
     *
     * <p>This ensures that system ringtones remain accessible and can be played during Direct Boot
     * (before the user unlocks the device). It processes the default alarm ringtone as well as
     * any specific ringtones assigned to existing alarms, skipping those that are silent,
     * random, or already copied.</p>
     *
     * @param context The context used to access the application's alarms and storage directories.
     */
    public static void migrateSystemRingtonesToLocal(@NonNull Context context) {
        Set<Uri> urisToProcess = new HashSet<>();
        boolean needsAllSystemRingtones = false;

        urisToProcess.add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

        List<Alarm> alarms = Alarm.getAlarms(context.getContentResolver(), null);
        for (Alarm alarm : alarms) {
            if (alarm.alert != null) {
                if (isRandomRingtone(alarm.alert)) {
                    needsAllSystemRingtones = true;
                } else {
                    urisToProcess.add(alarm.alert);
                }
            }
        }

        if (needsAllSystemRingtones) {
            copyAllSystemRingtonesToSafeStorage(context);
        }

        for (Uri uri : urisToProcess) {
            if (!RINGTONE_SILENT.equals(uri)
                && !isRandomRingtone(uri)
                && !isRandomCustomRingtone(uri)
                && !isCopiedSystemRingtone(context, uri)) {

                copySystemRingtoneToSafeStorage(context, uri);
            }
        }
    }

    /**
     * @return {@code true} if an external audio device is connected. {@code false} otherwise.
     */
    public static boolean hasExternalAudioDeviceConnected(@NonNull Context context, boolean isAutoRoutingToExternalAudioDevice) {
        if (!isAutoRoutingToExternalAudioDevice) {
            return false;
        }

        AudioManager audioManager = context.getApplicationContext().getSystemService(AudioManager.class);

        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            if (isExternalAudioDevice(device)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return {@code true} if the device is an external audio output device
     * (Bluetooth A2DP/SCO or wired headphones/headset). {@code false} otherwise.
     */
    public static boolean isExternalAudioDevice(@NonNull AudioDeviceInfo device) {
        int type = device.getType();

        if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            || type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
            return true;
        }

        if (SdkUtils.isAtLeastAndroid8()) {
            return type == AudioDeviceInfo.TYPE_USB_HEADSET;
        }

        return false;
    }

    /**
     * @return {@code true} if the device is currently in a telephone call. {@code false} otherwise.
     */
    public static boolean isInTelephoneCall(@NonNull AudioManager audioManager) {
        final int audioMode = audioManager.getMode();
        if (SdkUtils.isAtLeastAndroid13()) {
            return audioMode == AudioManager.MODE_IN_COMMUNICATION
                || audioMode == AudioManager.MODE_COMMUNICATION_REDIRECT
                || audioMode == AudioManager.MODE_CALL_REDIRECT
                || audioMode == AudioManager.MODE_CALL_SCREENING
                || audioMode == AudioManager.MODE_IN_CALL;
        } else {
            return audioMode == AudioManager.MODE_IN_COMMUNICATION || audioMode == AudioManager.MODE_IN_CALL;
        }
    }

    /**
     * @return Uri of the ringtone to play when the user is in a telephone call
     */
    public static Uri getInCallRingtoneUri(@NonNull Context context) {
        return getResourceUri(context, R.raw.alarm_expire);
    }

    /**
     * @return Uri of the ringtone to play when the chosen ringtone fails to play
     */
    public static Uri getFallbackRingtoneUri(@NonNull Context context) {
        return getResourceUri(context, R.raw.alarm_expire);
    }

    /**
     * @param currentTime current time of the device
     * @param stopTime    time at which the crescendo finishes
     * @param duration    length of time over which the crescendo occurs
     * @return the scalar volume value that produces a linear increase in volume (in decibels)
     */
    public static float computeVolume(long currentTime, long stopTime, long duration) {
        // Compute the percentage of the crescendo that has completed.
        float fractionComplete = 1 - Math.max(0f, Math.min(1f, (stopTime - currentTime) / (float) duration));

        // Use the fraction to compute a target decibel between -40dB (near silent) and 0dB (max).
        final float gain = (fractionComplete * 40) - 40;

        // Convert the target gain (in decibels) into the corresponding volume scalar.
        final float volume = (float) Math.pow(10f, gain / 20f);

        LogUtils.v("Ringtone crescendo %,.2f%% complete (scalar: %f, volume: %f dB)", fractionComplete * 100, volume, gain);

        return volume;
    }

    /**
     * Returns the minimum allowed volume level for the alarm stream.
     * <p>
     * On Android 9 (API 28) and above, this uses {@link AudioManager#getStreamMinVolume(int)}
     * to retrieve the actual minimum volume for {@link AudioManager#STREAM_ALARM}.
     * On earlier versions, where this API is not available, it defaults to {@code 0}.
     */
    public static int getAlarmMinVolume(@NonNull AudioManager audioManager) {
        return SdkUtils.isAtLeastAndroid9() ? audioManager.getStreamMinVolume(AudioManager.STREAM_ALARM) : 0;
    }

    /**
     * Checks if the alarm stream volume is currently set to its minimum level or lower.
     *
     * @return {@code true} if the alarm volume is too low, {@code false} otherwise or if an error occurs.
     */
    public static boolean isAlarmStreamLow(@NonNull Context context) {
        AudioManager audioManager = context.getApplicationContext().getSystemService(AudioManager.class);

        try {
            final int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            int minVolume = RingtoneUtils.getAlarmMinVolume(audioManager);

            return currentVolume <= minVolume;
        } catch (Exception e) {
            // Since this is purely informational, avoid crashing the app.
            return false;
        }
    }

    /**
     * Increases the alarm stream volume to a safe and audible level (two-thirds of the usable range).
     * It also triggers the system volume UI to provide immediate visual feedback to the user.
     *
     * @param context The context used to retrieve the audio service.
     */
    public static void fixAlarmStreamLow(@NonNull Context context) {
        AudioManager audioManager = context.getApplicationContext().getSystemService(AudioManager.class);

        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        final int minVolume = RingtoneUtils.getAlarmMinVolume(audioManager);
        final int usableRange = maxVolume - minVolume;
        final int index = minVolume + (2 * usableRange / 3);

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, index, AudioManager.FLAG_SHOW_UI);
    }

}
