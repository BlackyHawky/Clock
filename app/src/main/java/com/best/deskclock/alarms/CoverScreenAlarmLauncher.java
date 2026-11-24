// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DeviceProductInfo;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;

/**
 * Experimental. On some flip phones (e.g. Motorola Razr) SystemUI does not launch the full-screen intent
 * when the phone is folded, it only shows a heads-up on the cover screen.
 * <p>
 * If the user allowed "Display over other apps" and the phone looks folded, we start AlarmActivity
 * ourselves on the cover display.
 * <p>
 * The cover display is the only non-default display that looks like a built-in panel: not private,
 * secure, supports protected buffers and reports a built-in connection (API 31+). The built-in
 * connection check is what rules out HDMI and USB-C screens, their public flags look the same as the
 * cover screen. The flag checks rule out virtual displays (cast, screen recording, Android Auto).
 * <p>
 * Folded is decided like this, always with the default display OFF:
 * <ul>
 *     <li>the hinge angle sensor says closed: launch, whatever the cover display state is
 *     (AlarmActivity turns the screen on);</li>
 *     <li>no hinge sensor or no answer in time: wait up to {@link #POLL_TIMEOUT_MS} for the cover
 *     display to be ON or dozing, since the system turns it on for the alarm notification.</li>
 * </ul>
 */
public final class CoverScreenAlarmLauncher {

    private static final float HINGE_CLOSED_MAX_DEGREES = 30f;
    private static final long HINGE_TIMEOUT_MS = 500;
    private static final long POLL_INTERVAL_MS = 100;
    private static final long POLL_TIMEOUT_MS = 1500;

    private CoverScreenAlarmLauncher() {
    }

    /**
     * A launch still waiting for the hinge angle or the display state. Main thread only.
     */
    public static final class PendingLaunch {
        private final long mInstanceId;
        // Token for all posted callbacks, so cancel() removes only ours.
        private final Object mToken = new Object();
        private final Handler mHandler = new Handler(Looper.getMainLooper());
        @Nullable
        private SensorManager mSensorManager;
        @Nullable
        private SensorEventListener mListener;
        // Set once launched, skipped or canceled.
        private boolean mDone;

        private PendingLaunch(long instanceId) {
            mInstanceId = instanceId;
        }

        /**
         * Stops waiting: unregisters the hinge listener and removes posted callbacks. Safe to call
         * more than once, and after the launch already happened.
         */
        void cancel() {
            mHandler.removeCallbacksAndMessages(mToken);
            if (mSensorManager != null && mListener != null) {
                mSensorManager.unregisterListener(mListener);
                mSensorManager = null;
                mListener = null;
            }

            if (!mDone) {
                mDone = true;
                LogUtils.i("CoverScreen: cancelled pending launch for instance %d", mInstanceId);
            }
        }

        private void postDelayed(@NonNull Runnable runnable, long delayMs) {
            mHandler.postAtTime(runnable, mToken, SystemClock.uptimeMillis() + delayMs);
        }
    }

    /**
     * Starts AlarmActivity on the cover display if the phone looks folded and the user allowed it.
     * Works asynchronously on the main thread, never blocks the caller.
     *
     * @return the pending launch, so the caller can cancel it when the alarm stops; {@code null} if
     * nothing was started.
     */
    @Nullable
    static PendingLaunch launchIfFolded(@NonNull AlarmService service, @NonNull AlarmInstance instance) {
        try {
            if (!SdkUtils.isAtLeastAndroid12()) {
                LogUtils.i("CoverScreen: skip, needs Android 12 to check for a built-in display");
                return null;
            }

            if (!Settings.canDrawOverlays(service)) {
                LogUtils.i("CoverScreen: skip, 'Display over other apps' is not allowed");
                return null;
            }

            if (!hasBuiltInSecondDisplay(service)) {
                LogUtils.i("CoverScreen: skip, no built-in second display found");
                return null;
            }

            final PendingLaunch pending = new PendingLaunch(instance.mId);
            final SensorManager sensorManager = service.getSystemService(SensorManager.class);
            Sensor hinge = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE) : null;
            if (hinge == null && sensorManager != null) {
                hinge = sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE, true);
            }

            if (hinge == null) {
                LogUtils.i("CoverScreen: no hinge angle sensor, checking display states");
                pollDisplayStates(service, instance, pending, SystemClock.elapsedRealtime());
            } else {
                readHingeAngle(service, instance, pending, sensorManager, hinge);
            }
            return pending;
        } catch (Exception e) {
            LogUtils.e("CoverScreen: direct launch failed", e);
            return null;
        }
    }

    /**
     * Reads the first hinge angle event (on-change sensors report their current value when
     * registered), then decides. Falls back to the display states on timeout.
     */
    @RequiresApi(31)
    private static void readHingeAngle(@NonNull AlarmService service, @NonNull AlarmInstance instance,
                                       @NonNull PendingLaunch pending, @NonNull SensorManager sensorManager,
                                       @NonNull Sensor hinge) {

        // Set by the first event or the timeout, so only one of them decides.
        final boolean[] done = {false};

        final SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (done[0] || pending.mDone) {
                    return;
                }

                done[0] = true;
                sensorManager.unregisterListener(this);
                pending.mHandler.removeCallbacksAndMessages(pending.mToken);

                final float angle = event.values[0];
                if (angle > HINGE_CLOSED_MAX_DEGREES) {
                    pending.mDone = true;
                    LogUtils.i("CoverScreen: skip, hinge angle %.1f means open", angle);
                    return;
                }

                LogUtils.i("CoverScreen: hinge angle %.1f means closed", angle);
                runSafely(() -> {
                    final Display cover = findCoverDisplay(service);
                    if (cover != null) {
                        launch(service, instance, pending, cover);
                    } else {
                        pending.mDone = true;
                    }
                });
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        pending.postDelayed(() -> {
            if (done[0] || pending.mDone) {
                return;
            }

            done[0] = true;
            sensorManager.unregisterListener(listener);
            LogUtils.i("CoverScreen: no hinge angle after %d ms, checking display states", HINGE_TIMEOUT_MS);
            runSafely(() -> pollDisplayStates(service, instance, pending, SystemClock.elapsedRealtime()));
        }, HINGE_TIMEOUT_MS);

        pending.mSensorManager = sensorManager;
        pending.mListener = listener;
        if (!sensorManager.registerListener(listener, hinge, SensorManager.SENSOR_DELAY_NORMAL, pending.mHandler)) {
            pending.mHandler.removeCallbacksAndMessages(pending.mToken);
            LogUtils.i("CoverScreen: hinge angle sensor registration failed, checking display states");
            pollDisplayStates(service, instance, pending, SystemClock.elapsedRealtime());
        }
    }

    /**
     * Fallback without hinge angle: launches once the cover display is ON or dozing,
     * checking again every {@link #POLL_INTERVAL_MS} while it is OFF.
     */
    @RequiresApi(31)
    private static void pollDisplayStates(@NonNull AlarmService service, @NonNull AlarmInstance instance,
                                          @NonNull PendingLaunch pending, long startTime) {

        if (pending.mDone) {
            return;
        }

        final Display cover = findCoverDisplay(service);
        if (cover == null) {
            pending.mDone = true;
            return;
        }

        final int state = cover.getState();
        if (state == Display.STATE_ON || state == Display.STATE_DOZE || state == Display.STATE_DOZE_SUSPEND) {
            launch(service, instance, pending, cover);
            return;
        }

        final long elapsed = SystemClock.elapsedRealtime() - startTime;
        if (elapsed >= POLL_TIMEOUT_MS) {
            pending.mDone = true;
            LogUtils.i("CoverScreen: skip, cover displayId=%d still %s after %d ms",
                cover.getDisplayId(), stateToString(state), elapsed);
            return;
        }

        if (service.isNotFiring(instance.mId)) {
            pending.mDone = true;
            LogUtils.i("CoverScreen: skip, instance %d is no longer firing", instance.mId);
            return;
        }

        pending.postDelayed(() -> runSafely(() -> pollDisplayStates(service, instance, pending, startTime)),
            POLL_INTERVAL_MS);
    }

    @RequiresApi(31)
    private static void launch(@NonNull AlarmService service, @NonNull AlarmInstance instance,
                               @NonNull PendingLaunch pending, @NonNull Display cover) {

        if (pending.mDone) {
            return;
        }

        pending.mDone = true;

        if (service.isNotFiring(instance.mId)) {
            LogUtils.i("CoverScreen: skip, instance %d is no longer firing", instance.mId);
            return;
        }

        final Intent intent = AlarmNotifications.createFullScreenIntent(service, instance);
        final ActivityOptions options = ActivityOptions.makeBasic();
        // Without a display id the system uses the default display, which is off when folded.
        options.setLaunchDisplayId(cover.getDisplayId());
        LogUtils.i("CoverScreen: starting AlarmActivity for instance %d on displayId=%d (%s)",
            instance.mId, cover.getDisplayId(), stateToString(cover.getState()));
        service.startActivity(intent, options.toBundle());
    }

    private static void runSafely(@NonNull Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            LogUtils.e("CoverScreen: direct launch failed", e);
        }
    }

    /**
     * Used by the settings screen to decide whether to show the cover screen permission.
     *
     * @return {@code true} if a non-default display looks like a built-in panel, whatever its state.
     */
    public static boolean hasBuiltInSecondDisplay(@NonNull Context context) {
        if (!SdkUtils.isAtLeastAndroid12()) {
            return false;
        }

        final DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) {
            return false;
        }

        for (Display display : dm.getDisplays()) {
            if (display.getDisplayId() != Display.DEFAULT_DISPLAY && getBuiltInRejectReason(display) == null) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return the only non-default built-in display, whatever its state, if the default display is OFF.
     * Otherwise {@code null}. Logs why it skips.
     */
    @Nullable
    @RequiresApi(31)
    private static Display findCoverDisplay(@NonNull Context context) {
        final DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) {
            LogUtils.i("CoverScreen: skip, no DisplayManager");
            return null;
        }

        Display defaultDisplay = null;
        Display coverDisplay = null;
        int candidates = 0;

        for (Display display : dm.getDisplays()) {
            final boolean isDefault = display.getDisplayId() == Display.DEFAULT_DISPLAY;
            final String rejectReason = isDefault ? null : getBuiltInRejectReason(display);

            if (isDefault) {
                defaultDisplay = display;
            } else if (rejectReason == null) {
                candidates++;
                coverDisplay = display;
            }
        }

        if (defaultDisplay == null) {
            LogUtils.i("CoverScreen: skip, default display not found");
            return null;
        }

        if (defaultDisplay.getState() != Display.STATE_OFF) {
            LogUtils.i("CoverScreen: skip, default display is %s, not OFF", stateToString(defaultDisplay.getState()));
            return null;
        }

        if (candidates != 1) {
            LogUtils.i("CoverScreen: skip, found %d cover display candidates, need exactly 1", candidates);
            return null;
        }

        return coverDisplay;
    }

    /**
     * @return why the given display doesn't look like a built-in panel, or {@code null} if it does.
     */
    @Nullable
    @RequiresApi(31)
    private static String getBuiltInRejectReason(@NonNull Display display) {
        final int flags = display.getFlags();
        if ((flags & Display.FLAG_PRIVATE) != 0) {
            return "private";
        }

        if ((flags & Display.FLAG_SECURE) == 0) {
            return "not secure";
        }

        if ((flags & Display.FLAG_SUPPORTS_PROTECTED_BUFFERS) == 0) {
            return "no protected buffers";
        }

        final DeviceProductInfo productInfo = display.getDeviceProductInfo();
        if (productInfo == null) {
            return "no product info";
        }

        if (productInfo.getConnectionToSinkType() != DeviceProductInfo.CONNECTION_TO_SINK_BUILT_IN) {
            return "not built-in";
        }

        return null;
    }

    @NonNull
    private static String stateToString(int state) {
        return switch (state) {
            case Display.STATE_OFF -> "OFF";
            case Display.STATE_ON -> "ON";
            case Display.STATE_DOZE -> "DOZE";
            case Display.STATE_DOZE_SUSPEND -> "DOZE_SUSPEND";
            case Display.STATE_VR -> "VR";
            case Display.STATE_ON_SUSPEND -> "ON_SUSPEND";
            default -> "UNKNOWN(" + state + ")";
        };
    }
}
