// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Global executors for the application to handle multi-threading.
 *
 * <p>This class provides a centralized way to execute background tasks and UI operations,
 * preventing memory leaks and excessive thread creation.</p>
 */
public class AppExecutors {

    /**
     * Single-thread asynchronous executor.
     * Ensures that all background tasks are executed sequentially (one after another)
     * in the order they are submitted.
     */
    private static final ExecutorService diskIO = Executors.newSingleThreadExecutor();

    /**
     * Handler attached to the application's main thread (UI Thread).
     * Safely sends tasks to the user interface from a background thread.
     */
    private static final Handler mainThread = new Handler(Looper.getMainLooper());

    /**
     * Returns the executor dedicated to heavy or blocking background operations.
     *
     * <p>Ideal for:
     * <ul>
     *   <li>Database queries or deletions (e.g., in AlarmClockFragment)</li>
     *   <li>Any long-running task that shouldn't block the UI Thread</li>
     * </ul>
     *
     * @return {@link ExecutorService} configured on a single background thread.
     */
    public static ExecutorService getDiskIO() {
        return diskIO;
    }

    /**
     * Returns the global Handler for the Main Thread.
     * Replaces the repetitive creation of {@code new Handler(Looper.getMainLooper())} in the codebase.
     *
     * <p>Ideal for:
     * <ul>
     *   <li>Updating views or restarting Loaders (e.g., {@code restartLoader})</li>
     *   <li>Displaying Toasts or Snackbars</li>
     *   <li>Executing UI-related delayed tasks (animations, activity recreation)</li>
     * </ul>
     *
     * @return {@link Handler} attached to the Main Looper.
     */
    public static Handler getMainThread() {
        return mainThread;
    }
}
