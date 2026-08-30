/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.utils.LogUtils;

public final class RingtonePreviewKlaxon {

    public record Config(
        boolean isAdvancedAudioPlaybackEnabled,
        @NonNull RingtonePlayer.Config ringtonePlayerConfig
    ) {}

    private static RingtonePreviewKlaxon sInstance;

    private AsyncRingtonePlayer mAsyncRingtonePlayer;

    private RingtonePlayer mRingtonePlayer;

    private RingtonePreviewKlaxon() {
    }

    private static synchronized RingtonePreviewKlaxon getInstance() {
        if (sInstance == null) {
            sInstance = new RingtonePreviewKlaxon();
        }
        return sInstance;
    }

    public static void stop(boolean isAdvancedAudioPlaybackEnabled) {
        LogUtils.i("RingtonePreviewKlaxon.stop()");

        if (isAdvancedAudioPlaybackEnabled) {
            if (getInstance().mRingtonePlayer != null) {
                getInstance().mRingtonePlayer.stop();
            }
        } else {
            if (getInstance().mAsyncRingtonePlayer != null) {
                getInstance().mAsyncRingtonePlayer.stop();
            }
        }
    }

    public static void stopPreviewFromSpeakers() {
        LogUtils.i("RingtonePreviewKlaxon.stop()");
        if (getInstance().mAsyncRingtonePlayer != null) {
            getInstance().mAsyncRingtonePlayer.stop();
        }
    }

    public static void start(@NonNull Uri uri, @NonNull Config config) {
        stop(config.isAdvancedAudioPlaybackEnabled());
        LogUtils.i("RingtonePreviewKlaxon.start()");

        if (config.isAdvancedAudioPlaybackEnabled()) {
            getInstance().getRingtonePlayer(config.ringtonePlayerConfig()).play(uri, 0);
        } else {
            getInstance().getAsyncRingtonePlayer().play(uri, 0);
        }
    }

    public static void startPreviewOnlyFromSpeakers(@NonNull Uri uri) {
        stopPreviewFromSpeakers();
        LogUtils.i("RingtonePreviewKlaxon.start()");
        getInstance().getAsyncRingtonePlayer().play(uri, 0);
    }

    public static synchronized void releaseResources() {
        if (sInstance != null) {
            if (sInstance.mAsyncRingtonePlayer != null) {
                sInstance.mAsyncRingtonePlayer.shutdown();
                sInstance.mAsyncRingtonePlayer = null;
            }

            if (sInstance.mRingtonePlayer != null) {
                sInstance.mRingtonePlayer.stop();
                sInstance.mRingtonePlayer = null;
            }
        }
    }

    // MediaPlayer
    private AsyncRingtonePlayer getAsyncRingtonePlayer() {
        if (mAsyncRingtonePlayer == null) {
            mAsyncRingtonePlayer = new AsyncRingtonePlayer(DeskClockApplication.getAppContext());
        }

        return mAsyncRingtonePlayer;
    }

    // ExoPlayer
    private RingtonePlayer getRingtonePlayer(@NonNull RingtonePlayer.Config config) {
        if (mRingtonePlayer == null) {
            mRingtonePlayer = new RingtonePlayer(DeskClockApplication.getAppContext(), config);
        }

        return mRingtonePlayer;
    }

}
