/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static com.best.deskclock.data.Stopwatch.State.PAUSED;
import static com.best.deskclock.data.Stopwatch.State.RESET;
import static com.best.deskclock.data.Stopwatch.State.RUNNING;
import static com.best.deskclock.utils.Utils.now;
import static com.best.deskclock.utils.Utils.wallClock;

/**
 * A read-only domain object representing a stopwatch.
 */
public final class Stopwatch {

    static final long UNUSED = Long.MIN_VALUE;
    /**
     * The single, immutable instance of a reset stopwatch.
     */
    private static final Stopwatch RESET_STOPWATCH = new Stopwatch(RESET, UNUSED, UNUSED, 0);
    /**
     * Current state of this stopwatch.
     */
    private final State mState;
    /**
     * Elapsed time in ms the stopwatch was last started; {@link #UNUSED} if not running.
     */
    private final long mLastStartTime;
    /**
     * The time since epoch at which the stopwatch was last started.
     */
    private final long mLastStartWallClockTime;
    /**
     * Elapsed time in ms this stopwatch has accumulated while running.
     */
    private final long mAccumulatedTime;

    Stopwatch(State state, long lastStartTime, long lastWallClockTime, long accumulatedTime) {
        mState = state;
        mLastStartTime = lastStartTime;
        mLastStartWallClockTime = lastWallClockTime;
        mAccumulatedTime = accumulatedTime;
    }

    public State getState() {
        return mState;
    }

    public long getLastStartTime() {
        return mLastStartTime;
    }

    public long getLastWallClockTime() {
        return mLastStartWallClockTime;
    }

    public boolean isReset() {
        return mState == RESET;
    }

    public boolean isPaused() {
        return mState == PAUSED;
    }

    public boolean isRunning() {
        return mState == RUNNING;
    }

    /**
     * @return the total amount of time accumulated up to this moment
     */
    public long getTotalTime() {
        if (mState != RUNNING) {
            return mAccumulatedTime;
        }

        // In practice, "now" can be any value due to device reboots. When the real-time clock
        // is reset, there is no more guarantee that "now" falls after the last start time. To
        // ensure the stopwatch is monotonically increasing, normalize negative time segments to 0,
        final long timeSinceStart = now() - mLastStartTime;
        return mAccumulatedTime + Math.max(0, timeSinceStart);
    }

    /**
     * @return the amount of time accumulated up to the last time the stopwatch was started
     */
    public long getAccumulatedTime() {
        return mAccumulatedTime;
    }

    /**
     * @return a copy of this stopwatch that is running
     */
    Stopwatch start() {
        if (mState == RUNNING) {
            return this;
        }

        return new Stopwatch(RUNNING, now(), wallClock(), getTotalTime());
    }

    /**
     * @return a copy of this stopwatch that is paused
     */
    Stopwatch pause() {
        if (mState != RUNNING) {
            return this;
        }

        return new Stopwatch(PAUSED, UNUSED, UNUSED, getTotalTime());
    }

    /**
     * @return a copy of this stopwatch that is reset
     */
    Stopwatch reset() {
        return RESET_STOPWATCH;
    }

    /**
     * @return this Stopwatch if it is not running or an updated version based on wallclock time.
     * The internals of the stopwatch are updated using the wallclock time which is durable
     * across reboots.
     */
    Stopwatch updateAfterReboot() {
        if (mState != RUNNING) {
            return this;
        }
        final long timeSinceBoot = now();
        final long wallClockTime = wallClock();
        // Avoid negative time deltas. They can happen in practice, but they can't be used. Simply
        // update the recorded times and proceed with no change in accumulated time.
        final long delta = Math.max(0, wallClockTime - mLastStartWallClockTime);
        return new Stopwatch(mState, timeSinceBoot, wallClockTime, mAccumulatedTime + delta);
    }

    /**
     * @return this Stopwatch if it is not running or an updated version based on the realtime.
     * The internals of the stopwatch are updated using the realtime clock which is accurate
     * across wallclock time adjustments.
     */
    Stopwatch updateAfterTimeSet() {
        if (mState != RUNNING) {
            return this;
        }
        final long timeSinceBoot = now();
        final long wallClockTime = wallClock();
        final long delta = timeSinceBoot - mLastStartTime;
        if (delta < 0) {
            // Avoid negative time deltas. They typically happen following reboots when TIME_SET is
            // broadcast before BOOT_COMPLETED. Simply ignore the time update and hope
            // updateAfterReboot() can successfully correct the data at a later time.
            return this;
        }
        return new Stopwatch(mState, timeSinceBoot, wallClockTime, mAccumulatedTime + delta);
    }

    public enum State {RESET, RUNNING, PAUSED}
}
