package com.best.deskclock.alarms;

import android.os.SystemClock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A lightweight, thread-safe in-memory cache to temporarily store dismissed alarm IDs.
 *
 * <p>This bridges the visual gap between a background dismiss action triggered from a notification
 * and the delayed database update via CursorLoader. It ensures the UI instantly reflects
 * the dismissed state when the app is reopened, rather than displaying an outdated status.</p>
 */
public class AlarmVisualCache {

    // Thread-safe map to store the alarm ID and the exact time it was dismissed
    private static final Map<Long, Long> sDismissedAlarms = new ConcurrentHashMap<>();

    // The CursorLoader can take several seconds to sync when waking up from background restrictions.
    // 10 seconds provides a safe margin for the cache to live.
    private static final long CACHE_EXPIRATION_MS = 10000;

    /**
     * Caches the ID of a recently dismissed alarm with the current timestamp.
     *
     * @param alarmId the unique identifier of the dismissed alarm
     */
    public static void cacheDismissedAlarm(long alarmId) {
        sDismissedAlarms.put(alarmId, SystemClock.elapsedRealtime());
    }

    /**
     * Checks if the given alarm was recently dismissed and is still within the cache expiration time.
     * Automatically cleans up expired entries to prevent memory leaks.
     *
     * @param alarmId the unique identifier of the alarm to check
     * @return {@code true} if the alarm was dismissed recently, {@code false} otherwise
     */
    public static boolean isDismissed(long alarmId) {
        Long timestamp = sDismissedAlarms.get(alarmId);

        if (timestamp != null) {
            if (SystemClock.elapsedRealtime() - timestamp < CACHE_EXPIRATION_MS) {
                return true;
            } else {
                sDismissedAlarms.remove(alarmId);
            }
        }

        return false;
    }

}
