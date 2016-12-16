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
     * Record the event in some form or fashion.
     *
     * @param category indicates what entity raised the event: Alarm, Clock, Timer or Stopwatch
     * @param action indicates how the entity was altered; e.g. create, delete, fire, etc.
     * @param label indicates where the action originated; e.g. DeskClock (UI), Intent,
     *      Notification, etc.; 0 indicates no label could be established
     */
    void sendEvent(@StringRes int category, @StringRes int action, @StringRes int label);
}