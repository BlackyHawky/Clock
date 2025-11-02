// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.AnyRes;

import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.R;
import com.best.deskclock.data.CustomRingtone;
import com.best.deskclock.data.RingtoneModel;
import com.best.deskclock.data.SettingsDAO;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RingtoneUtils {

    /**
     * The ringtone preview duration in ms.
     */
    public static final long ALARM_PREVIEW_DURATION_MS = 5000;

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
     * @return {@code true} if the URI represents a random ringtone; {@code false} otherwise.
     */
    public static boolean isRandomRingtone(Uri uri) {
        return RANDOM_RINGTONE.equals(uri);
    }

    /**
     * @return {@code true} if the URI represents a random custom ringtone; {@code false} otherwise.
     */
    public static boolean isRandomCustomRingtone(Uri uri) {
        return RANDOM_CUSTOM_RINGTONE.equals(uri);
    }

    /**
     * @param resourceId identifies an application resource
     * @return the Uri by which the application resource is accessed
     */
    public static Uri getResourceUri(Context context, @AnyRes int resourceId) {
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
    public static boolean isRingtoneUriReadable(Context context, Uri uri) {
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
     * Creates and prepares a {@link MediaPlayer} instance to play a ringtone.
     *
     * @return A prepared {@link MediaPlayer} instance if successful,
     * or {@code null} if preparation fails.
     */
    public static MediaPlayer createPreparedMediaPlayer(Context context, Uri... ringtoneUris) {
        // Use a DirectBoot aware context if supported
        Context storageContext = Utils.getSafeStorageContext(context);

        MediaPlayer player = new MediaPlayer();

        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());

        for (Uri uri : ringtoneUris) {
            try {
                LogUtils.d("Trying to prepare MediaPlayer for URI: " + uri);
                player.reset();
                player.setDataSource(storageContext, uri);
                player.prepare();
                return player;
            } catch (IOException e) {
                LogUtils.e("Failed to prepare MediaPlayer for URI: " + uri, e);
            }
        }

        player.release();
        return null;
    }

    /**
     * @return the duration of the ringtone.
     */
    public static int getRingtoneDuration(Context context, Uri ringtoneUri) {
        MediaPlayer player = createPreparedMediaPlayer(
                context,
                ringtoneUri,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        );

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
     * <p>
     * If no valid ringtones are found, the system's default alarm ringtone is returned.
     */
    public static Uri getRandomRingtoneUri() {
        Context context = DeskClockApplication.getContext();

        RingtoneManager manager = new RingtoneManager(context);
        manager.setType(RingtoneManager.TYPE_ALARM);

        Cursor cursor = manager.getCursor();
        List<Uri> uris = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Uri uri = manager.getRingtoneUri(cursor.getPosition());
                if (uri != null) {
                    uris.add(uri);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (uris.isEmpty()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        return uris.get(new Random().nextInt(uris.size()));
    }

    /**
     * Returns a randomly selected custom ringtone URI.
     * <p>
     * If no valid ringtones are found, the system's default alarm ringtone is returned.
     */
    public static Uri getRandomCustomRingtoneUri() {
        Context context = DeskClockApplication.getContext();

        List<Uri> uris = new ArrayList<>();

        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(context);
        RingtoneModel ringtoneModel = new RingtoneModel(context, prefs);
        for (CustomRingtone custom : ringtoneModel.getCustomRingtones()) {
            if (custom.hasPermissions()) {
                uris.add(custom.getUri());
            }
        }
        ringtoneModel.releaseResources();

        if (uris.isEmpty()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        return uris.get(new Random().nextInt(uris.size()));
    }

    /**
     * @return {@code true} if a Bluetooth output device is connected. {@code false} otherwise.
     */
    public static boolean hasBluetoothDeviceConnected(Context context, SharedPreferences prefs) {
        if (!SettingsDAO.isAutoRoutingToBluetoothDeviceEnabled(prefs)) {
            return false;
        }

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            if (isBluetoothDevice(device)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return {@code true} if the Bluetooth device is of type A2DP or SCO Bluetooth.
     * {@code false} otherwise.
     */
    public static boolean isBluetoothDevice(AudioDeviceInfo device) {
        int type = device.getType();
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO;
    }

    /**
     * @return {@code true} if the device is currently in a telephone call. {@code false} otherwise.
     */
    public static boolean isInTelephoneCall(AudioManager audioManager) {
        final int audioMode = audioManager.getMode();
        if (SdkUtils.isAtLeastAndroid13()) {
            return audioMode == AudioManager.MODE_IN_COMMUNICATION ||
                    audioMode == AudioManager.MODE_COMMUNICATION_REDIRECT ||
                    audioMode == AudioManager.MODE_CALL_REDIRECT ||
                    audioMode == AudioManager.MODE_CALL_SCREENING ||
                    audioMode == AudioManager.MODE_IN_CALL;
        } else {
            return audioMode == AudioManager.MODE_IN_COMMUNICATION ||
                    audioMode == AudioManager.MODE_IN_CALL;
        }
    }

    /**
     * @return Uri of the ringtone to play when the user is in a telephone call
     */
    public static Uri getInCallRingtoneUri(Context context) {
        return RingtoneUtils.getResourceUri(context, R.raw.alarm_expire);
    }

    /**
     * @return Uri of the ringtone to play when the chosen ringtone fails to play
     */
    public static Uri getFallbackRingtoneUri(Context context) {
        return RingtoneUtils.getResourceUri(context, R.raw.alarm_expire);
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

        LogUtils.v("Ringtone crescendo %,.2f%% complete (scalar: %f, volume: %f dB)",
                fractionComplete * 100, volume, gain);

        return volume;
    }

    /**
     * Returns the minimum allowed volume level for the alarm stream.
     * <p>
     * On Android 9 (API 28) and above, this uses {@link AudioManager#getStreamMinVolume(int)}
     * to retrieve the actual minimum volume for {@link AudioManager#STREAM_ALARM}.
     * On earlier versions, where this API is not available, it defaults to {@code 0}.
     */
    public static int getAlarmMinVolume(AudioManager audioManager) {
        return SdkUtils.isAtLeastAndroid9() ? audioManager.getStreamMinVolume(AudioManager.STREAM_ALARM) : 0;
    }

}
