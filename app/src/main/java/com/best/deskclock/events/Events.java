/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.events;

import androidx.annotation.StringRes;

import com.best.deskclock.R;
import com.best.deskclock.controller.Controller;

/**
 * This thin layer over {@link Controller#sendEvent} eases the API usage.
 */
public final class Events {

    /**
     * Extra describing the entity responsible for the action being performed.
     */
    public static final String EXTRA_EVENT_LABEL = "com.best.deskclock.extra.EVENT_LABEL";

    /**
     * Tracks an alarm event.
     *
     * @param action resource id of event action
     * @param label  resource id of event label
     */
    public static void sendAlarmEvent(@StringRes int action, @StringRes int label) {
        sendEvent(R.string.category_alarm, action, label);
    }

    /**
     * Tracks a clock event.
     *
     * @param action resource id of event action
     * @param label  resource id of event label
     */
    public static void sendClockEvent(@StringRes int action, @StringRes int label) {
        sendEvent(R.string.category_clock, action, label);
    }

    /**
     * Tracks a timer event.
     *
     * @param action resource id of event action
     * @param label  resource id of event label
     */
    public static void sendTimerEvent(@StringRes int action, @StringRes int label) {
        sendEvent(R.string.category_timer, action, label);
    }

    /**
     * Tracks a stopwatch event.
     *
     * @param action resource id of event action
     * @param label  resource id of event label
     */
    public static void sendStopwatchEvent(@StringRes int action, @StringRes int label) {
        sendEvent(R.string.category_stopwatch, action, label);
    }

    /**
     * Tracks a bedtime event.
     *
     * @param action resource id of event action
     * @param label resource id of event label
     */
    public static void sendBedtimeEvent(@StringRes int action, @StringRes int label) {
        sendEvent(R.string.category_bedtime, action, label);
    }


    /**
     * Tracks a screensaver event.
     *
     * @param action resource id of event action
     * @param label  resource id of event label
     */
    public static void sendScreensaverEvent(@StringRes int action, @StringRes int label) {
        sendEvent(R.string.category_screensaver, action, label);
    }

    /**
     * Tracks an event. Events have a category, action, label and value. This
     * method can be used to track events such as button presses or other user
     * interactions with your application (value is not used in this app).
     *
     * @param category resource id of event category
     * @param action   resource id of event action
     * @param label    resource id of event label
     */
    public static void sendEvent(@StringRes int category, @StringRes int action,
                                 @StringRes int label) {
        Controller.getController().sendEvent(category, action, label);
    }
}
