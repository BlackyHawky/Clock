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
 * Data that must be coordinated across all notifications is accessed via this model.
 */
final class NotificationModel {

    private boolean mApplicationInForeground;

    /**
     * @param inForeground {@code true} to indicate the application is open in the foreground
     */
    void setApplicationInForeground(boolean inForeground) {
        mApplicationInForeground = inForeground;
    }

    /**
     * @return {@code true} while the application is open in the foreground
     */
    boolean isApplicationInForeground() {
        return mApplicationInForeground;
    }

    /**
     * @return a value that identifies the stopwatch notification
     */
    int getStopwatchNotificationId() {
        return Integer.MAX_VALUE - 1;
    }

    /**
     * @return a value that identifies the notification for running/paused timers
     */
    int getUnexpiredTimerNotificationId() {
        return Integer.MAX_VALUE - 2;
    }

    /**
     * @return a value that identifies the notification for expired timers
     */
    int getExpiredTimerNotificationId() {
        return Integer.MAX_VALUE - 3;
    }
}