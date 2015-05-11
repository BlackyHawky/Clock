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

public interface EventTracker {
    /**
     * Send screen view tracking to Log system.
     *
     * @param screenName Screen name to be logged
     */
    void sendView(String screenName);

    /**
     * Send category, action and label describing an event to Log system.
     *
     * @param category string resource id indicating Alarm, Clock, Timer or Stopwatch or 0 for no
     *                 category
     * @param action string resource id indicating how the entity was altered;
     *               e.g. create, delete, fire, etc or 0 for no action
     * @param label string resource id indicating where the action originated;
     *              e.g. DeskClock (UI), Intent, Notification, etc. or 0 for no label
     */
    void sendEvent(@StringRes int category, @StringRes int action, @StringRes int label);

    /**
     * Send category, action and label describing an event to Log system.
     *
     * @param category Alarm, Clock, Timer or Stopwatch
     * @param action how the entity was altered; e.g. create, delete, fire, etc
     * @param label where the action originated; e.g. DeskClock (UI), Intent, Notification, etc.
     */
    void sendEvent(String category, String action, String label);
}
