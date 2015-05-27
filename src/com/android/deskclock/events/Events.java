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

package com.android.deskclock.events;

import android.support.annotation.StringRes;

import com.android.deskclock.R;

import java.util.ArrayList;
import java.util.Collection;

public final class Events {

    private static final Collection<EventTracker> sEventTrackers = new ArrayList<>();

    public static void addEventTracker(EventTracker eventTracker) {
        sEventTrackers.add(eventTracker);
    }

    public static void removeEventTracker(EventTracker eventTracker) {
        sEventTrackers.remove(eventTracker);
    }

    /**
     * Tracks an alarm event.
     *
     * @param action resource id of event action
     * @param label resource id of event label
     */
    public static void sendAlarmEvent(@StringRes int action, @StringRes int label) {
        sendEvent(R.string.category_alarm, action, label);
    }

    /**
     * Tracks a clock event.
     *
     * @param action resource id of event action
     * @param label resource id of event label
     */
    public static void sendClockEvent(@StringRes int action, @StringRes int label) {
        sendEvent(R.string.category_clock, action, label);
    }

    /**
     * Tracks an timer event.
     *
     * @param action resource id of event action
     * @param label resource id of event label
     */
    public static void sendTimerEvent(@StringRes int action, @StringRes int label) {
        sendEvent(R.string.category_timer, action, label);
    }

    /**
     * Tracks an stopwatch event.
     *
     * @param action resource id of event action
     * @param label resource id of event label
     */
    public static void sendStopwatchEvent(@StringRes int action, @StringRes int label) {
        sendEvent(R.string.category_stopwatch, action, label);
    }

    /**
     * Tracks an event. Events have a category, action, label and value. This
     * method can be used to track events such as button presses or other user
     * interactions with your application (value is not used in this app).
     *
     * @param category resource id of event category
     * @param action resource id of event action
     * @param label resource id of event label
     */
    public static void sendEvent(@StringRes int category, @StringRes int action,
            @StringRes int label) {
        for (EventTracker eventTracker : sEventTrackers) {
            eventTracker.sendEvent(category, action, label);
        }
    }

    /**
     * Tracks an event. Events have a category, action, label and value. This
     * method can be used to track events such as button presses or other user
     * interactions with your application (value is not used in this app).
     *
     * @param category the event category
     * @param action the event action
     * @param label the event label
     */
    public static void sendEvent(String category, String action, String label) {
        if (category != null && action != null) {
            for (EventTracker eventTracker : sEventTrackers) {
                eventTracker.sendEvent(category, action, label);
            }
        }
    }

    /**
     * Tracks entering a view with a new app screen name.
     *
     * @param screenName the new app screen name
     */
    public static void sendView(String screenName) {
        for (EventTracker eventTracker : sEventTrackers) {
            eventTracker.sendView(screenName);
        }
    }
}