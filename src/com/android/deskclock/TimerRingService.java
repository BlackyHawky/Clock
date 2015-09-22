/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.deskclock.timer.TimerKlaxon;

/**
 * Play the timer's ringtone. Will continue playing the same alarm until the service is stopped.
 */
public class TimerRingService extends Service {

    private boolean mPlaying = false;
    private TelephonyManager mTelephonyManager;
    private int mInitialCallState;

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            // The user might already be in a call when the timer fires. When
            // we register onCallStateChanged, we get the initial in-call state
            // which kills the timer. Check against the initial call state so
            // we don't kill the timer during a call.
            if (state != TelephonyManager.CALL_STATE_IDLE && state != mInitialCallState) {
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        // Listen for incoming calls to kill the timer.
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        AlarmAlertWakeLock.acquireScreenCpuWakeLock(this);
    }

    @Override
    public void onDestroy() {
        stop();
        // Stop listening for incoming calls.
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        AlarmAlertWakeLock.releaseCpuLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // No intent, tell the system not to restart us.
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        play();

        // Record the initial call state here so that the new timer has the newest state.
        mInitialCallState = mTelephonyManager.getCallState();

        return START_STICKY;
    }

    private void play() {
        if (mPlaying) {
            return;
        }
        LogUtils.v("TimerRingService.play()");
        TimerKlaxon.start(this);

        mPlaying = true;
    }

    /**
     * Stops timer audio
     */
    public void stop() {
        LogUtils.v("TimerRingService.stop()");

        if (mPlaying) {
            mPlaying = false;
        }

        TimerKlaxon.stop(this);
    }

}
