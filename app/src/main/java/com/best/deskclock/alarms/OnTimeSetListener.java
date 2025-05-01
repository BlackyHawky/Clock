// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

/**
 * Callback interface used to notify when a time has been selected.
 * Typically used in custom time pickers or dialogs.
 */
public interface OnTimeSetListener {

    /**
     * Called when the user sets a time.
     */
    void onTimeSet(int hour, int minute);
}
