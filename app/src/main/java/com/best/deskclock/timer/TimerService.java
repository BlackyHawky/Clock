/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.events.Events;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;

import java.util.Arrays;
import java.util.List;

/**
 * <p>This service exists solely to allow {@link android.app.AlarmManager} and timer notifications
 * to alter the state of timers without disturbing the notification shade. If an activity were used
 * instead (even one that is not displayed) the notification manager implicitly closes the
 * notification shade which clashes with the use case of starting/pausing/resetting timers without
 * disturbing the notification shade.</p>
 *
 * <p>The service has a second benefit. It is used to start heads-up notifications for expired
 * timers in the foreground. This keeps the entire application in the foreground and thus prevents
 * the operating system from killing it while expired timers are firing.</p>
 */
public final class TimerService extends Service {

    /**
     * Extra for many actions specific to a given timer.
     */
    public static final String EXTRA_TIMER_ID = "com.best.deskclock.extra.TIMER_ID";
    private static final String ACTION_PREFIX = "com.best.deskclock.action.";

    /**
     * Shows the tab with timers; scrolls to a specific timer.
     */
    public static final String ACTION_SHOW_TIMER = ACTION_PREFIX + "SHOW_TIMER";

    /**
     * Pauses running timers; resets expired timers.
     */
    public static final String ACTION_PAUSE_TIMER = ACTION_PREFIX + "PAUSE_TIMER";

    /**
     * Starts the sole timer.
     */
    public static final String ACTION_START_TIMER = ACTION_PREFIX + "START_TIMER";

    /**
     * Resets the timer.
     */
    public static final String ACTION_RESET_TIMER = ACTION_PREFIX + "RESET_TIMER";

    /**
     * Adds minutes or hour to the timer.
     */
    public static final String ACTION_ADD_CUSTOM_TIME_TO_TIMER = ACTION_PREFIX + "ADD_CUSTOM_TIME_TO_TIMER";
    private static final String ACTION_TIMER_EXPIRED = ACTION_PREFIX + "TIMER_EXPIRED";
    private static final String ACTION_UPDATE_NOTIFICATION = ACTION_PREFIX + "UPDATE_NOTIFICATION";
    public static final String ACTION_RESET_EXPIRED_TIMERS = ACTION_PREFIX + "RESET_EXPIRED_TIMERS";
    public static final String ACTION_RESET_MISSED_TIMERS = ACTION_PREFIX + "RESET_MISSED_TIMERS";

    @NonNull
    public static Intent createTimerExpiredIntent(@NonNull Context context, @Nullable Timer timer) {
        final int timerId = timer == null ? -1 : timer.getId();
        return new Intent(context, TimerService.class)
            .setAction(ACTION_TIMER_EXPIRED)
            .putExtra(EXTRA_TIMER_ID, timerId);
    }

    @NonNull
    public static Intent createResetExpiredTimersIntent(@NonNull Context context) {
        return new Intent(context, TimerService.class).setAction(ACTION_RESET_EXPIRED_TIMERS);
    }


    @NonNull
    public static Intent createAddCustomTimeToTimerIntent(@NonNull Context context, int timerId) {
        return new Intent(context, TimerService.class)
            .setAction(ACTION_ADD_CUSTOM_TIME_TO_TIMER)
            .putExtra(EXTRA_TIMER_ID, timerId);
    }

    @NonNull
    public static Intent createUpdateNotificationIntent(@NonNull Context context) {
        return new Intent(context, TimerService.class).setAction(ACTION_UPDATE_NOTIFICATION);
    }

    private SharedPreferences mPrefs;

    private CameraManager mCameraManager;
    private String mCameraId;
    private boolean mFlashState = false;
    private boolean mIsFlashActive = false;
    private boolean mIsUserFlashlightOn = false;
    private CameraManager.TorchCallback mTorchCallback;
    private Handler mHandler;
    private Runnable mFlashRunnable;

    private SensorManager mSensorManager;
    private boolean mIsFlipActionEnabled;
    private boolean mIsShakeActionEnabled;

    private AudioManager mAudioManager;
    private AudioFocusRequest mAudioFocusRequest;

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mPrefs = getDefaultSharedPreferences(this);
        // Set up for flip and shake actions
        mSensorManager = getApplicationContext().getSystemService(SensorManager.class);
        mIsFlipActionEnabled = SettingsDAO.isFlipActionForTimersEnabled(mPrefs);
        mIsShakeActionEnabled = SettingsDAO.isShakeActionForTimersEnabled(mPrefs);

        mAudioManager = getApplicationContext().getSystemService(AudioManager.class);

        mCameraManager = getApplicationContext().getSystemService(CameraManager.class);
        mHandler = new Handler(Looper.getMainLooper());

        getBackCameraId();

        mTorchCallback = new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                super.onTorchModeChanged(cameraId, enabled);
                if (mCameraId != null && mCameraId.equals(cameraId)) {
                    // Update the initial state if it is not the alarm that is causing the flash to blink.
                    if (!mIsFlashActive) {
                        mIsUserFlashlightOn = enabled;
                    }
                }
            }
        };

        if (mCameraManager != null) {
            mCameraManager.registerTorchCallback(mTorchCallback, mHandler);
        }

        mFlashRunnable = new Runnable() {
            @Override
            public void run() {
                // Toggle flash state
                mFlashState = !mFlashState;
                toggleFlash(mFlashState);

                // Repeat action after 500ms
                mHandler.postDelayed(this, 500);
            }
        };
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        final DataModel dataModel = DataModel.getDataModel();

        try {
            if (intent == null) {
                return START_NOT_STICKY;
            }

            final String action = intent.getAction();
            final int label = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, R.string.label_intent);

            if (action != null) {
                switch (action) {
                    case ACTION_UPDATE_NOTIFICATION -> {
                        dataModel.updateTimerNotification();
                        return START_NOT_STICKY;
                    }
                    case ACTION_RESET_EXPIRED_TIMERS -> {
                        dataModel.resetOrDeleteExpiredTimers(label);
                        return START_NOT_STICKY;
                    }
                    case ACTION_RESET_MISSED_TIMERS -> {
                        dataModel.resetOrDeleteMissedTimers(label);
                        return START_NOT_STICKY;
                    }
                }
            }

            // Look up the timer in question.
            final int timerId = intent.getIntExtra(EXTRA_TIMER_ID, -1);
            final Timer timer = dataModel.getTimer(timerId);

            // If the timer cannot be located, ignore the action.
            if (timer == null) {
                return START_NOT_STICKY;
            }

            // Perform the action on the timer.
            if (action != null) {
                switch (action) {
                    case ACTION_START_TIMER -> {
                        Events.sendTimerEvent(R.string.action_start, label);
                        dataModel.startTimer(this, timer);
                    }
                    case ACTION_PAUSE_TIMER -> {
                        Events.sendTimerEvent(R.string.action_pause, label);
                        dataModel.pauseTimer(timer);
                    }
                    case ACTION_ADD_CUSTOM_TIME_TO_TIMER -> {
                        Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, label);
                        dataModel.addCustomTimeToTimer(timer);
                    }
                    case ACTION_RESET_TIMER -> {
                        dataModel.resetTimer(timer, label);
                        detachListeners();
                    }
                    case ACTION_TIMER_EXPIRED -> {
                        Events.sendTimerEvent(R.string.action_fire, label);
                        dataModel.expireTimer(this, timer);
                        turnOnFlash(timer);
                        stopMedia(timer);
                        attachListeners();
                    }
                }
            }
        } finally {
            // This service is foreground when expired timers exist and stopped when none exist.
            final List<Timer> expiredTimers = dataModel.getExpiredTimers();

            if (expiredTimers.isEmpty()) {
                stopSelf();
            } else {
                Timer currentActiveTimer = expiredTimers.get(expiredTimers.size() - 1);

                if (currentActiveTimer.isFlashOn()) {
                    if (!mIsUserFlashlightOn && !mIsFlashActive) {
                        mIsFlashActive = true;
                        mHandler.post(mFlashRunnable);
                    }
                } else {
                    stopFlash();
                }
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        LogUtils.v("TimerService.onDestroy() called");
        super.onDestroy();

        detachListeners();

        stopFlash();

        if (mCameraManager != null && mTorchCallback != null) {
            mCameraManager.unregisterTorchCallback(mTorchCallback);
        }

        if (mAudioManager != null) {
            try {
                if (SdkUtils.isAtLeastAndroid8() && mAudioFocusRequest != null) {
                    mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
                    mAudioFocusRequest = null;
                } else {
                    //noinspection deprecation
                    mAudioManager.abandonAudioFocus(null);
                }
            } catch (Exception e) {
                LogUtils.e("TimerService - Failed to abandon audio focus", e);
            }
        }
    }

    private void turnOnFlash(@NonNull Timer timer) {
        if (!timer.isFlashOn()) {
            // If the last timer added to the list of expired timers does not have the flash enabled
            // while another timer is triggered with the flash on, stop the flash.
            stopFlash();
            return;
        }

        if (mIsUserFlashlightOn) {
            LogUtils.v("Flashlight is already on by user. Bypassing timer flash.");
        } else if (!mIsFlashActive) {
            mIsFlashActive = true;
            mHandler.post(mFlashRunnable);
        }
    }

    private void stopFlash() {
        mHandler.removeCallbacks(mFlashRunnable);

        if (mIsFlashActive && DeviceUtils.hasBackFlash(this)) {
            toggleFlash(false);
        }

        mIsFlashActive = false;
    }

    private void getBackCameraId() {
        try {
            for (String id : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                // Check if it is the rear camera
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = id;
                    break;
                }
            }

            if (mCameraId == null) {
                LogUtils.e("mCameraId is null");
            }
        } catch (CameraAccessException e) {
            LogUtils.e("TimerService.onCreate - Failed to access the flash unit", e);
        }
    }

    private void toggleFlash(boolean state) {
        try {
            if (DeviceUtils.hasBackFlash(this) && mCameraId != null) {
                mCameraManager.setTorchMode(mCameraId, state);
            }
        } catch (CameraAccessException e) {
            LogUtils.e("TimerService.toggleFlash - Failed to access the flash unit", e);
        }
    }

    private void stopMedia(@NonNull Timer timer) {
        if (mAudioManager == null || !timer.getTurnOffMedia()) {
            return;
        }

        LogUtils.i("TimerService - Media paused for sleep timer");

        try {
            // Requesting audio focus ensures that media is paused.
            if (SdkUtils.isAtLeastAndroid8()) {
                mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                    .build();
                mAudioManager.requestAudioFocus(mAudioFocusRequest);
            } else {
                //noinspection deprecation
                mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        } catch (Exception e) {
            LogUtils.e("TimerService - Failed to dispatch media pause or request audio focus", e);
        }
    }

    private final ResettableSensorEventListener mFlipListener = new ResettableSensorEventListener() {

        // Accelerometers are not quite accurate.
        private static final float GRAVITY_UPPER_THRESHOLD = 1.3f * SensorManager.STANDARD_GRAVITY;
        private static final float GRAVITY_LOWER_THRESHOLD = 0.7f * SensorManager.STANDARD_GRAVITY;
        private static final int SENSOR_SAMPLES = 3;
        private final boolean[] mSamples = new boolean[SENSOR_SAMPLES];
        private boolean mStopped = false;
        private boolean mWasFaceUp;
        private int mSampleIndex;

        @Override
        public void onAccuracyChanged(@NonNull Sensor sensor, int acc) {
        }

        @Override
        public void reset() {
            mWasFaceUp = false;
            mStopped = false;
            Arrays.fill(mSamples, false);
        }

        private boolean filterSamples() {
            boolean allPass = true;
            for (boolean sample : mSamples) {
                allPass = allPass && sample;
            }
            return allPass;
        }

        @Override
        public void onSensorChanged(@NonNull SensorEvent event) {
            // Add a sample overwriting the oldest one. Several samples
            // are used to avoid the erroneous values the sensor sometimes
            // returns.
            float z = event.values[2];

            if (mStopped) {
                return;
            }

            if (!mWasFaceUp) {
                // Check if its face up enough.
                mSamples[mSampleIndex] = (z > GRAVITY_LOWER_THRESHOLD) && (z < GRAVITY_UPPER_THRESHOLD);

                // face up
                if (filterSamples()) {
                    mWasFaceUp = true;
                    Arrays.fill(mSamples, false);
                }
            } else {
                // Check if its face down enough.
                mSamples[mSampleIndex] = (z < -GRAVITY_LOWER_THRESHOLD) && (z > -GRAVITY_UPPER_THRESHOLD);

                // face down
                if (filterSamples()) {
                    mStopped = true;
                    handleAction(mIsFlipActionEnabled);
                }
            }

            mSampleIndex = ((mSampleIndex + 1) % SENSOR_SAMPLES);
        }
    };

    private final ResettableSensorEventListener mShakeListener = new ResettableSensorEventListener() {
        private static final int BUFFER = 5;
        private final float[] gravity = new float[3];
        private float average = 0;
        private int fill = 0;
        private boolean mStopped;
        private boolean mInitialized = false;

        @Override
        public void onAccuracyChanged(@NonNull Sensor sensor, int acc) {
        }

        @Override
        public void reset() {
            mStopped = false;
            mInitialized = false;
            average = 0;
            fill = 0;
            Arrays.fill(gravity, 0f);
        }

        public void onSensorChanged(@NonNull SensorEvent event) {
            if (mStopped) {
                return;
            }

            // On the first pass, capture the actual gravity and ignore the calculation.
            if (!mInitialized) {
                gravity[0] = event.values[0];
                gravity[1] = event.values[1];
                gravity[2] = event.values[2];
                mInitialized = true;
                return;
            }

            final float alpha = 0.8F;

            for (int i = 0; i < 3; i++) {
                gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i];
            }

            float x = event.values[0] - gravity[0];
            float y = event.values[1] - gravity[1];
            float z = event.values[2] - gravity[2];

            float sensitivity = SettingsDAO.getTimerShakeIntensity(mPrefs);

            if (fill <= BUFFER) {
                average += Math.abs(x) + Math.abs(y) + Math.abs(z);
                fill++;
            } else {
                if (average / BUFFER >= sensitivity) {
                    mStopped = true;
                    handleAction(mIsShakeActionEnabled);
                }
                average = 0;
                fill = 0;
            }
        }
    };

    private void attachListeners() {
        if (mIsFlipActionEnabled) {
            mFlipListener.reset();
            mSensorManager.registerListener(mFlipListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL, 0);
        }

        if (mIsShakeActionEnabled) {
            mShakeListener.reset();
            mSensorManager.registerListener(mShakeListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME, 50 * 1000); // Batch every 50 milliseconds
        }
    }

    private void detachListeners() {
        if (mIsFlipActionEnabled) {
            mSensorManager.unregisterListener(mFlipListener);
        }

        if (mIsShakeActionEnabled) {
            mSensorManager.unregisterListener(mShakeListener);
        }
    }

    private void handleAction(boolean actionIsEnabled) {
        if (actionIsEnabled) {
            startService(createResetExpiredTimersIntent(this));
        }
    }

    private interface ResettableSensorEventListener extends SensorEventListener {
        void reset();
    }
}
