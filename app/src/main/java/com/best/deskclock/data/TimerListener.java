/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import androidx.annotation.NonNull;

/**
 * The interface through which interested parties are notified of changes to one of the timers.
 */
public interface TimerListener {

    /**
     * @param timer the timer that was added
     */
    void timerAdded(@NonNull Timer timer);

    /**
     * @param before the timer state before the update
     * @param after  the timer state after the update
     */
    void timerUpdated(@NonNull Timer before, @NonNull Timer after);

    /**
     * @param timer the timer that was removed
     */
    void timerRemoved(@NonNull Timer timer);
}
