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

/**
 * The interface through which interested parties are notified of changes to one of the timers.
 */
public interface TimerListener {

    /**
     * @param timer the timer that was added
     */
    void timerAdded(Timer timer);

    /**
     * @param before the timer state before the update
     * @param after the timer state after the update
     */
    void timerUpdated(Timer before, Timer after);

    /**
     * @param timer the timer that was removed
     */
    void timerRemoved(Timer timer);
}