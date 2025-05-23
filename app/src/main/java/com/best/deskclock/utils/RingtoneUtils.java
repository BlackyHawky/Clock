// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.AnyRes;

import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.data.CustomRingtone;
import com.best.deskclock.data.RingtoneModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RingtoneUtils {

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
        if (Settings.System.DEFAULT_ALARM_ALERT_URI.equals(uri)) {
            return true;
        }

        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            return stream != null;
        } catch (Exception e) {
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
        MediaPlayer player = new MediaPlayer();

        for (Uri uri : ringtoneUris) {
            try {
                player.reset();
                player.setDataSource(context, uri);
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
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

}
