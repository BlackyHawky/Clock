/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

/**
 * A read-only domain object representing a stopwatch lap.
 *
 * @param mLapNumber       The 1-based position of the lap.
 * @param mLapTime         Elapsed time in ms since the lap was last started.
 * @param mAccumulatedTime Elapsed time in ms accumulated for all laps up to and including this one.
 */
public record Lap(int mLapNumber, long mLapTime, long mAccumulatedTime) {

    public int getLapNumber() {
        return mLapNumber;
    }

    public long getLapTime() {
        return mLapTime;
    }

    public long getAccumulatedTime() {
        return mAccumulatedTime;
    }
}
