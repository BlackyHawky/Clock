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

import android.os.SystemClock;

import static com.android.deskclock.data.Stopwatch.State.PAUSED;
import static com.android.deskclock.data.Stopwatch.State.RESET;
import static com.android.deskclock.data.Stopwatch.State.RUNNING;

/**
 * A read-only domain object representing a stopwatch.
 */
public final class Stopwatch {

    public enum State { RESET, RUNNING, PAUSED }

    /** The single, immutable instance of a reset stopwatch. */
    private static final Stopwatch RESET_STOPWATCH = new Stopwatch(RESET, Long.MIN_VALUE, 0);

    /** Current state of this stopwatch. */
    private final State mState;

    /** Elapsed time in ms the stopwatch was last started; {@link Long#MIN_VALUE} if not running. */
    private final long mLastStartTime;

    /** Elapsed time in ms this stopwatch has accumulated while running. */
    private final long mAccumulatedTime;

    Stopwatch(State state, long lastStartTime, long accumulatedTime) {
        mState = state;
        mLastStartTime = lastStartTime;
        mAccumulatedTime = accumulatedTime;
    }

    public State getState() { return mState; }
    public long getLastStartTime() { return mLastStartTime; }
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

        return mAccumulatedTime + (now() - mLastStartTime);
    }

    /**
     * @return the amount of time accumulated up to the last time the stopwatch was started
     */
    long getAccumulatedTime() {
        return mAccumulatedTime;
    }

    /**
     * @return a copy of this stopwatch that is running
     */
    Stopwatch start() {
        if (mState == RUNNING) {
            return this;
        }

        return new Stopwatch(RUNNING, now(), mAccumulatedTime);
    }

    /**
     * @return a copy of this stopwatch that is paused
     */
    Stopwatch pause() {
        if (mState != RUNNING) {
            return this;
        }

        final long accumulatedTime = mAccumulatedTime + (now() - mLastStartTime);
        return new Stopwatch(PAUSED, Long.MIN_VALUE, accumulatedTime);
    }

    /**
     * @return a copy of this stopwatch that is reset
     */
    Stopwatch reset() {
        return RESET_STOPWATCH;
    }

    private static long now() {
        return SystemClock.elapsedRealtime();
    }
}