// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

/**
 * Interface for fragments that manage periodic background tasks via Runnables.
 * It allows starting and stopping updates related to the user interface or recurring tasks.
 */
public interface RunnableFragment {

    /**
     * Starts the Runnable associated with the fragment.
     * This method is called when the fragment becomes active or visible.
     */
    void startRunnable();

    /**
     * Stops the Runnable associated with the fragment.
     * This method is called when the fragment becomes inactive or invisible.
     */
    void stopRunnable();
}
