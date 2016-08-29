/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.data;

import static com.android.deskclock.Utils.now;
import static com.android.deskclock.Utils.wallClock;
import static com.android.deskclock.data.Stopwatch.State.PAUSED;
import static com.android.deskclock.data.Stopwatch.State.RESET;
import static com.android.deskclock.data.Stopwatch.State.RUNNING;
import static com.android.deskclock.provider.ClockContract.AUTHORITY;

/**
 * A read-only domain object representing a stopwatch.
 */
public final class Stopwatch {

    public enum State { RESET, RUNNING, PAUSED }

    static final long UNUSED = Long.MIN_VALUE;

    /** The single, immutable instance of a reset stopwatch. */
    private static final Stopwatch RESET_STOPWATCH = new Stopwatch(RESET, UNUSED, UNUSED, 0);

    /** Current state of this stopwatch. */
    private final State mState;

    /** Elapsed time in ms the stopwatch was last started; {@link #UNUSED} if not running. */
    private final long mLastStartTime;

    /** The time since epoch at which the stopwatch was last started. */
    private final long mLastStartWallClockTime;

    /** Elapsed time in ms this stopwatch has accumulated while running. */
    private final long mAccumulatedTime;

    Stopwatch(State state, long lastStartTime, long lastWallClockTime, long accumulatedTime) {
        mState = state;
        mLastStartTime = lastStartTime;
        mLastStartWallClockTime = lastWallClockTime;
        mAccumulatedTime = accumulatedTime;
    }

    public State getState() { return mState; }
    public long getLastStartTime() { return mLastStartTime; }
    public long getLastWallClockTime() { return mLastStartWallClockTime; }
    public boolean isReset() { return mState == RESET; }
    public boolean isPaused() { return mState == PAUSED; }
    public boolean isRunning() { return mState == RUNNING; }

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

    Stopwatch updateAfterReboot() {
        if (mState != RUNNING) {
            return this;
        }
        final long timeSinceBoot = now();
        final long wallClockTime = wallClock();
        final long delta = wallClockTime - mLastStartWallClockTime;
        return new Stopwatch(mState, timeSinceBoot, wallClockTime, mAccumulatedTime + delta);
    }

    Stopwatch updateAfterTimeSet() {
        if (mState != RUNNING) {
            return this;
        }
        final long timeSinceBoot = now();
        final long wallClockTime = wallClock();
        final long delta = timeSinceBoot - mLastStartTime;
        if (delta < 0) {
            return this;
        }
        return new Stopwatch(mState, timeSinceBoot, wallClockTime, mAccumulatedTime + delta);
    }
}
