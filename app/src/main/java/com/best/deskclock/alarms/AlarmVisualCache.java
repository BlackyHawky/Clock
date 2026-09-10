package com.best.deskclock.alarms;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.provider.AlarmInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A lightweight, thread-safe in-memory cache to temporarily store dismissed and snoozed alarm states.
 *
 * <p>This bridges the visual gap between a background action triggered from a notification
 * and the delayed database update via CursorLoader. It ensures the UI instantly reflects
 * the correct state when the app is reopened, rather than displaying an outdated status.</p>
 */
public class AlarmVisualCache {

    /**
     * Thread-safe map to store the alarm ID and the exact time it was dismissed
     */
    private static final Map<Long, Long> sDismissedAlarms = new ConcurrentHashMap<>();

    /**
     * Thread-safe maps for snoozed alarms to hold the instance.
     */
    private static final Map<Long, AlarmInstance> sSnoozedAlarms = new ConcurrentHashMap<>();

    /**
     * Thread-safe maps for snoozed alarms to hold the instance creation timestamp.
     */
    private static final Map<Long, Long> sSnoozedTimestamps = new ConcurrentHashMap<>();

    /**
     * The CursorLoader can take several seconds to sync when waking up from background restrictions.
     * 10 seconds provides a safe margin for the cache to live.
     */
    private static final long CACHE_EXPIRATION_MS = 10000;

    /**
     * Caches the ID of a recently dismissed alarm with the current timestamp.
     * Automatically invalidates any existing snooze cache for this alarm.
     *
     * @param alarmId the unique identifier of the dismissed alarm
     */
    public static void cacheDismissedAlarm(long alarmId) {
        sDismissedAlarms.put(alarmId, SystemClock.elapsedRealtime());

        sSnoozedAlarms.remove(alarmId);
        sSnoozedTimestamps.remove(alarmId);
    }

    /**
     * Caches the updated instance of a recently snoozed alarm with the current timestamp.
     *
     * @param alarmId         the unique identifier of the snoozed alarm
     * @param snoozedInstance the alarm instance containing the newly calculated snooze time
     */
    public static void cacheSnoozedAlarm(long alarmId, @NonNull AlarmInstance snoozedInstance) {
        sSnoozedAlarms.put(alarmId, snoozedInstance);
        sSnoozedTimestamps.put(alarmId, SystemClock.elapsedRealtime());
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

    /**
     * Retrieves the recently snoozed alarm instance if it exists and hasn't expired.
     * Automatically cleans up expired entries to prevent memory leaks.
     *
     * @param alarmId the unique identifier of the alarm to check
     * @return the snoozed {@link AlarmInstance} if cached and valid, {@code null} otherwise
     */
    @Nullable
    public static AlarmInstance getSnoozedAlarm(long alarmId) {
        Long timestamp = sSnoozedTimestamps.get(alarmId);

        if (timestamp != null) {
            if (SystemClock.elapsedRealtime() - timestamp < CACHE_EXPIRATION_MS) {
                return sSnoozedAlarms.get(alarmId);
            } else {
                // Expired: Clean up both maps
                sSnoozedAlarms.remove(alarmId);
                sSnoozedTimestamps.remove(alarmId);
            }
        }
        return null;
    }

    /**
     * Removes an alarm from the dismissal and snooze cache immediately.
     * Must be called whenever an alarm is toggled ON rescheduled or fired.
     *
     * @param alarmId the unique identifier of the alarm to invalidate
     */
    public static void invalidate(long alarmId) {
        sDismissedAlarms.remove(alarmId);
        sSnoozedAlarms.remove(alarmId);
        sSnoozedTimestamps.remove(alarmId);
    }

}
