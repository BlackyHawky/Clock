/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.events;

import androidx.annotation.StringRes;

public interface EventTracker {
    /**
     * Record the event in some form or fashion.
     *
     * @param category indicates what entity raised the event: Alarm, Clock, Timer or Stopwatch
     * @param action   indicates how the entity was altered; e.g. create, delete, fire, etc.
     * @param label    indicates where the action originated; e.g. DeskClock (UI), Intent,
     *                 Notification, etc.; 0 indicates no label could be established
     */
    void sendEvent(@StringRes int category, @StringRes int action, @StringRes int label);
}
