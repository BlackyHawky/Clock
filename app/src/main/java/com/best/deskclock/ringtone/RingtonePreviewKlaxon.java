/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.LogUtils;

public final class RingtonePreviewKlaxon {

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

    public static void stop() {
        LogUtils.i("RingtonePreviewKlaxon.stop()");

        Context appContext = DeskClockApplication.getAppContext();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(appContext);

        if (SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
            getInstance().getRingtonePlayer().stop();
        } else {
            getInstance().getAsyncRingtonePlayer().stop();
        }
    }

    public static void stopPreviewFromSpeakers() {
        LogUtils.i("RingtonePreviewKlaxon.stop()");
        getInstance().getAsyncRingtonePlayer().stop();
    }

    public static void start(Uri uri) {
        stop();
        LogUtils.i("RingtonePreviewKlaxon.start()");

        Context appContext = DeskClockApplication.getAppContext();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(appContext);

        if (SettingsDAO.isAdvancedAudioPlaybackEnabled(prefs)) {
            getInstance().getRingtonePlayer().play(uri, 0);
        } else {
            getInstance().getAsyncRingtonePlayer().play(uri, 0);
        }
    }

    public static void startPreviewOnlyFromSpeakers(Uri uri) {
        stopPreviewFromSpeakers();
        LogUtils.i("RingtonePreviewKlaxon.start()");
        getInstance().getAsyncRingtonePlayer().play(uri, 0);
    }

    public static synchronized void releaseResources() {
        if (sInstance != null && sInstance.mAsyncRingtonePlayer != null) {
            sInstance.mAsyncRingtonePlayer.shutdown();
            sInstance.mAsyncRingtonePlayer = null;
        }
    }

    public static synchronized void stopListeningToPreferences() {
        if (sInstance != null && sInstance.mRingtonePlayer != null) {
            sInstance.mRingtonePlayer.stopListeningToPreferences();
            sInstance.mRingtonePlayer = null;
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
    private RingtonePlayer getRingtonePlayer() {
        if (mRingtonePlayer == null) {
            mRingtonePlayer = new RingtonePlayer(DeskClockApplication.getAppContext());
        }

        return mRingtonePlayer;
    }

}
