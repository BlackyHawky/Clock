/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.best.deskclock.AlarmAlertWakeLock;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.LogUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * This service is in charge of starting/stopping the alarm. It will bring up and manage the
 * {@link AlarmActivity} as well as {@link AlarmKlaxon}.
 * <p>
 * Registers a broadcast receiver to listen for snooze/dismiss intents. The broadcast receiver
 * exits early if AlarmActivity is bound to prevent double-processing of the snooze/dismiss intents.
 */
public class AlarmService extends Service {

    /**
     * AlarmActivity and AlarmService (when unbound) listen for this broadcast intent
     * so that other applications can snooze the alarm (after ALARM_ALERT_ACTION and before
     * ALARM_DONE_ACTION).
     */
    public static final String ALARM_SNOOZE_ACTION = "com.best.deskclock.ALARM_SNOOZE";

    /**
     * AlarmActivity and AlarmService listen for this broadcast intent so that other
     * applications can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     */
    public static final String ALARM_DISMISS_ACTION = "com.best.deskclock.ALARM_DISMISS";

    /**
     * A public action sent by AlarmService when the alarm has started.
     */
    public static final String ALARM_ALERT_ACTION = "com.best.deskclock.ALARM_ALERT";

    /**
     * A public action sent by AlarmService when the alarm has stopped for any reason.
     */
    public static final String ALARM_DONE_ACTION = "com.best.deskclock.ALARM_DONE";

    /**
     * Private action used to stop an alarm with this service.
     */
    public static final String STOP_ALARM_ACTION = "STOP_ALARM";

    /**
     * Private action used to stop an alarm with a double vibrations when the alarm is snoozed with this service.
     */
    public static final String STOP_ALARM_WITH_DOUBLE_VIBRATION_ACTION = "STOP_ALARM_WITH_DOUBLE_VIBRATION";

    /**
     * Private action used to stop an alarm with a single vibration when the alarm is dismissed with this service.
     */
    public static final String STOP_ALARM_WITH_SINGLE_VIBRATION_ACTION = "STOP_ALARM_WITH_SINGLE_VIBRATION";

    /**
     * Constant for No action
     */
    private static final int ALARM_NO_ACTION = 0;

    /**
     * Constant for Snooze
     */
    private static final int ALARM_SNOOZE = 1;

    /**
     * Constant for Dismiss
     */
    private static final int ALARM_DISMISS = 2;

    /**
     * Binder given to AlarmActivity.
     */
    private final IBinder mBinder = new Binder();
    
    /**
     * Whether the service is currently bound to AlarmActivity
     */
    private boolean mIsBound = false;

    /**
     * Whether the receiver is currently registered
     */
    private boolean mIsRegistered = false;

    private Vibrator mVibrator;

    private AlarmInstance mCurrentAlarm = null;

    private final BroadcastReceiver mActionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LogUtils.i("AlarmService received intent %s", action);
            if (mCurrentAlarm == null || mCurrentAlarm.mAlarmState != AlarmInstance.FIRED_STATE) {
                LogUtils.i("No valid firing alarm");
                return;
            }

            if (mIsBound) {
                LogUtils.i("AlarmActivity bound; AlarmService no-op");
                return;
            }

            if (action != null) {
                switch (action) {
                    case ALARM_SNOOZE_ACTION -> {
                        // Set the alarm state to snoozed.
                        // If this broadcast receiver is handling the snooze intent then AlarmActivity
                        // must not be showing, so always show snooze toast.
                        AlarmStateManager.setSnoozeState(context, mCurrentAlarm, true);
                        Events.sendAlarmEvent(R.string.action_snooze, R.string.label_intent);
                    }
                    case ALARM_DISMISS_ACTION -> {
                        // Set the alarm state to dismissed.
                        AlarmStateManager.deleteInstanceAndUpdateParent(context, mCurrentAlarm);
                        Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_intent);
                    }
                }
            }
        }
    };

    private SensorManager mSensorManager;
    private int mFlipAction;
    private final ResettableSensorEventListener mFlipListener = new ResettableSensorEventListener() {
                // Accelerometers are not quite accurate.
                private static final float GRAVITY_UPPER_THRESHOLD = 1.3f * SensorManager.STANDARD_GRAVITY;
                private static final float GRAVITY_LOWER_THRESHOLD = 0.7f * SensorManager.STANDARD_GRAVITY;
                private static final int SENSOR_SAMPLES = 3;
                private final boolean[] mSamples = new boolean[SENSOR_SAMPLES];
                private boolean mStopped;
                private boolean mWasFaceUp;
                private int mSampleIndex;

                @Override
                public void onAccuracyChanged(Sensor sensor, int acc) {
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
                public void onSensorChanged(SensorEvent event) {
                    // Add a sample overwriting the oldest one. Several samples are used to avoid
                    // the erroneous values the sensor sometimes returns.
                    float z = event.values[2];

                    if (mStopped) {
                        return;
                    }

                    if (!mWasFaceUp) {
                        // Check if its face up enough.
                        mSamples[mSampleIndex] = (z > GRAVITY_LOWER_THRESHOLD) &&
                                (z < GRAVITY_UPPER_THRESHOLD);

                        // Face up
                        if (filterSamples()) {
                            mWasFaceUp = true;
                            Arrays.fill(mSamples, false);
                        }
                    } else {
                        // Check if its face down enough.
                        mSamples[mSampleIndex] = (z < -GRAVITY_LOWER_THRESHOLD) &&
                                (z > -GRAVITY_UPPER_THRESHOLD);

                        // Face down
                        if (filterSamples()) {
                            mStopped = true;
                            handleAction(mFlipAction);
                        }
                    }

                    mSampleIndex = ((mSampleIndex + 1) % SENSOR_SAMPLES);
                }
            };

    private int mShakeAction;
    private final SensorEventListener mShakeListener = new SensorEventListener() {
        private static final float SENSITIVITY = 16;
        private static final int BUFFER = 5;
        private final float[] gravity = new float[3];
        private float average = 0;
        private int fill = 0;

        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        public void onSensorChanged(SensorEvent event) {
            final float alpha = 0.8F;

            for (int i = 0; i < 3; i++) {
                gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i];
            }

            float x = event.values[0] - gravity[0];
            float y = event.values[1] - gravity[1];
            float z = event.values[2] - gravity[2];

            if (fill <= BUFFER) {
                average += Math.abs(x) + Math.abs(y) + Math.abs(z);
                fill++;
            } else {
                if (average / BUFFER >= SENSITIVITY) {
                    handleAction(mShakeAction);
                }
                average = 0;
                fill = 0;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        mIsBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mIsBound = false;
        return super.onUnbind(intent);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();

        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Register the broadcast receiver
        final IntentFilter filter = new IntentFilter(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mActionsReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(mActionsReceiver, filter);
        }
        mIsRegistered = true;

        // Setup for flip and shake actions
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mFlipAction = DataModel.getDataModel().getFlipAction();
        mShakeAction = DataModel.getDataModel().getShakeAction();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.v("AlarmService.onStartCommand() with %s", intent);
        if (intent == null) {
            return Service.START_NOT_STICKY;
        }

        final long instanceId = AlarmInstance.getId(intent.getData());
        switch (Objects.requireNonNull(intent.getAction())) {
            case AlarmStateManager.CHANGE_STATE_ACTION -> {
                AlarmStateManager.handleIntent(this, intent);

                // If state is changed to firing, actually fire the alarm!
                final int alarmState = intent.getIntExtra(AlarmStateManager.ALARM_STATE_EXTRA, -1);
                if (alarmState == AlarmInstance.FIRED_STATE) {
                    final ContentResolver cr = this.getContentResolver();
                    final AlarmInstance instance = AlarmInstance.getInstance(cr, instanceId);
                    if (instance == null) {
                        LogUtils.e("No instance found to start alarm: %d", instanceId);
                        if (mCurrentAlarm != null) {
                            // Only release lock if we are not firing alarm
                            AlarmAlertWakeLock.releaseCpuLock();
                        }
                        break;
                    }

                    if (mCurrentAlarm != null && mCurrentAlarm.mId == instanceId) {
                        LogUtils.e("Alarm already started for instance: %d", instanceId);
                        break;
                    }
                    startAlarm(instance);
                }
            }
            case STOP_ALARM_ACTION -> {
                if (mCurrentAlarm != null && mCurrentAlarm.mId != instanceId) {
                    LogUtils.e("Can't stop alarm for instance: %d because current alarm is: %d",
                            instanceId, mCurrentAlarm.mId);
                    break;
                }
                stopCurrentAlarm();
                stopSelf();
            }
            case STOP_ALARM_WITH_DOUBLE_VIBRATION_ACTION -> {
                if (mCurrentAlarm != null && mCurrentAlarm.mId != instanceId) {
                    LogUtils.e("Can't perform double vibration and stop alarm for instance: %d " +
                                    "because current alarm is: %d", instanceId, mCurrentAlarm.mId);
                    break;
                }
                performDoubleVibration();
                stopSelf();
            }
            case STOP_ALARM_WITH_SINGLE_VIBRATION_ACTION -> {
                if (mCurrentAlarm != null && mCurrentAlarm.mId != instanceId) {
                    LogUtils.e("Can't perform single vibration and stop alarm for instance: %d " +
                                    "because current alarm is: %d", instanceId, mCurrentAlarm.mId);
                    break;
                }
                performSingleVibration();
                stopSelf();
            }
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        LogUtils.v("AlarmService.onDestroy() called");
        super.onDestroy();
        if (mCurrentAlarm != null) {
            stopCurrentAlarm();
        }

        if (mIsRegistered) {
            unregisterReceiver(mActionsReceiver);
            mIsRegistered = false;
        }
    }

    private void startAlarm(AlarmInstance instance) {
        LogUtils.v("AlarmService.start with instance: " + instance.mId);
        if (mCurrentAlarm != null) {
            AlarmStateManager.setMissedState(this, mCurrentAlarm);
            stopCurrentAlarm();
        }

        AlarmAlertWakeLock.acquireCpuWakeLock(this);

        mCurrentAlarm = instance;
        AlarmNotifications.showAlarmNotification(this, mCurrentAlarm);
        AlarmKlaxon.start(this, mCurrentAlarm);
        sendBroadcast(new Intent(ALARM_ALERT_ACTION));
        attachListeners();
    }

    private void stopCurrentAlarm() {
        if (mCurrentAlarm == null) {
            LogUtils.v("There is no current alarm to stop");
            return;
        }

        final long instanceId = mCurrentAlarm.mId;
        LogUtils.v("AlarmService.stop with instance: %s", instanceId);

        AlarmKlaxon.stop(this);
        sendBroadcast(new Intent(ALARM_DONE_ACTION));

        stopForeground(true);

        mCurrentAlarm = null;
        detachListeners();
        AlarmAlertWakeLock.releaseCpuLock();
    }

    private void performSingleVibration() {
        if (mCurrentAlarm == null) {
            LogUtils.v("There is no current alarm to stop so it's impossible to perform a single vibration");
            return;
        }

        final long instanceId = mCurrentAlarm.mId;
        LogUtils.v("AlarmService.stop with single vibration with instance: %s", instanceId);

        AlarmKlaxon.stop(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator.vibrate(VibrationEffect.createWaveform(new long[]{700, 500}, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            mVibrator.vibrate(new long[]{700, 500}, -1);
        }

        sendBroadcast(new Intent(ALARM_DONE_ACTION));

        stopForeground(true);

        mCurrentAlarm = null;
        detachListeners();
        AlarmAlertWakeLock.releaseCpuLock();
    }

    private void performDoubleVibration() {
        if (mCurrentAlarm == null) {
            LogUtils.v("There is no current alarm to stop so it's impossible to perform a double vibration");
            return;
        }

        final long instanceId = mCurrentAlarm.mId;
        LogUtils.v("AlarmService.stop with double vibration with instance: %s", instanceId);

        AlarmKlaxon.stop(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator.vibrate(VibrationEffect.createWaveform(new long[]{700, 200, 100, 500}, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            mVibrator.vibrate(new long[]{700, 200, 100, 500}, -1);
        }

        sendBroadcast(new Intent(ALARM_DONE_ACTION));

        stopForeground(true);

        mCurrentAlarm = null;
        detachListeners();
        AlarmAlertWakeLock.releaseCpuLock();
    }

    /**
     * Utility method to help stop an alarm properly. Nothing will happen, if alarm is not firing
     * or using a different instance.
     *
     * @param context  application context
     * @param instance you are trying to stop
     */
    public static void stopAlarm(Context context, AlarmInstance instance) {
        final Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId)
                .setAction(STOP_ALARM_ACTION);

        // We don't need a wake lock here, since we are trying to kill an alarm
        context.startService(intent);
    }

    /**
     * Utility method to help stop an alarm properly and perform a double vibration when the alarm is snoozed.
     * Nothing will happen, if alarm is not firing or using a different instance.
     *
     * @param context  application context
     * @param instance you are trying to stop
     */
    public static void stopAlarmWithDoubleVibration(Context context, AlarmInstance instance) {
        final Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction(STOP_ALARM_WITH_DOUBLE_VIBRATION_ACTION);

        // We don't need a wake lock here, since we are trying to kill an alarm
        context.startService(intent);
    }

    /**
     * Utility method to help stop an alarm properly and perform a single vibration when the alarm is dismissed.
     * Nothing will happen, if alarm is not firing or using a different instance.
     *
     * @param context  application context
     * @param instance you are trying to stop
     */
    public static void stopAlarmWithSingleVibration(Context context, AlarmInstance instance) {
        final Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction(STOP_ALARM_WITH_SINGLE_VIBRATION_ACTION);

        // We don't need a wake lock here, since we are trying to kill an alarm
        context.startService(intent);
    }

    private void attachListeners() {
        if (mFlipAction != ALARM_NO_ACTION) {
            mFlipListener.reset();
            mSensorManager.registerListener(mFlipListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL, 300 * 1000); // Batch every 300 milliseconds
        }

        if (mShakeAction != ALARM_NO_ACTION) {
            mSensorManager.registerListener(mShakeListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_GAME, 50 * 1000); // Batch every 50 milliseconds
        }
    }

    private void detachListeners() {
        if (mFlipAction != ALARM_NO_ACTION) {
            mSensorManager.unregisterListener(mFlipListener);
        }
        if (mShakeAction != ALARM_NO_ACTION) {
            mSensorManager.unregisterListener(mShakeListener);
        }
    }

    private void handleAction(int action) {
        if (action == ALARM_SNOOZE) { // Setup Snooze Action
            startService(AlarmStateManager.createStateChangeIntent(this,
                    AlarmStateManager.ALARM_SNOOZE_TAG, mCurrentAlarm, AlarmInstance.SNOOZE_STATE));
        } else if (action == ALARM_DISMISS) { // Setup Dismiss Action
            startService(AlarmStateManager.createStateChangeIntent(this,
                    AlarmStateManager.ALARM_DISMISS_TAG, mCurrentAlarm, AlarmInstance.DISMISSED_STATE));
        }
    }

    private interface ResettableSensorEventListener extends SensorEventListener {
        void reset();
    }
}
