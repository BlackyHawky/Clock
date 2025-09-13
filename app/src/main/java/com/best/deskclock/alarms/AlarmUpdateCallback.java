// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

/**
 * Callback interface for receiving lifecycle events during alarm updates.
 * Implementations can respond to the start and completion of alarm modifications.
 */
public interface AlarmUpdateCallback {

    /**
     * Called when an alarm update begins.
     * Useful for blocking sorting or preparing the UI.
     */
    void onAlarmUpdateStarted();

    /**
     * Called when an alarm update completes.
     * Useful for refreshing data or resuming sorting.
     */
    void onAlarmUpdateFinished();

}
