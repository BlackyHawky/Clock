/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static com.best.deskclock.data.Timer.State.RESET;

import android.content.SharedPreferences;

import com.best.deskclock.data.Timer.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class encapsulates the transfer of data between {@link Timer} domain objects and their
 * permanent storage in {@link SharedPreferences}.
 */
public final class TimerDAO {

    /**
     * Key to a preference that stores the set of timer ids.
     */
    public static final String TIMER_IDS = "timers_list";

    /**
     * Key to a preference that stores the id to assign to the next timer.
     */
    private static final String NEXT_TIMER_ID = "next_timer_id";

    /**
     * Prefix for a key to a preference that stores the state of the timer.
     */
    private static final String STATE = "timer_state_";

    /**
     * Prefix for a key to a preference that stores the original timer length at creation.
     */
    private static final String LENGTH = "timer_setup_timet_";

    /**
     * Prefix for a key to a preference that stores the total timer length with additions.
     */
    private static final String TOTAL_LENGTH = "timer_original_timet_";

    /**
     * Prefix for a key to a preference that stores the last start time of the timer.
     */
    private static final String LAST_START_TIME = "timer_start_time_";

    /**
     * Prefix for a key to a preference that stores the epoch time when the timer last started.
     */
    private static final String LAST_WALL_CLOCK_TIME = "timer_wall_clock_time_";

    /**
     * Prefix for a key to a preference that stores the remaining time before expiry.
     */
    private static final String REMAINING_TIME = "timer_time_left_";

    /**
     * Prefix for a key to a preference that stores the label of the timer.
     */
    private static final String LABEL = "timer_label_";

    /**
     * Prefix for a key to a preference that stores the time of the timer button.
     */
    private static final String BUTTON_TIME = "timer_button_time";

    /**
     * Prefix for a key to a preference that signals the timer should be deleted on first reset.
     */
    private static final String DELETE_AFTER_USE = "delete_after_use_";

    private TimerDAO() {
    }

    /**
     * @return the timers from permanent storage
     */
    static List<Timer> getTimers(SharedPreferences prefs) {
        // Read the set of timer ids.
        final Set<String> timerIds = prefs.getStringSet(TIMER_IDS, Collections.emptySet());
        final List<Timer> timers = new ArrayList<>(timerIds.size());

        // Build a timer using the data associated with each timer id.
        for (String timerId : timerIds) {
            final int id = Integer.parseInt(timerId);
            final int stateValue = prefs.getInt(STATE + id, RESET.getValue());
            final State state = State.fromValue(stateValue);

            // Timer state may be null when migrating timers from prior releases which defined a
            // "deleted" state. Such a state is no longer required.
            if (state != null) {
                final long length = prefs.getLong(LENGTH + id, Long.MIN_VALUE);
                final long totalLength = prefs.getLong(TOTAL_LENGTH + id, Long.MIN_VALUE);
                final long lastStartTime = prefs.getLong(LAST_START_TIME + id, Timer.UNUSED);
                final long lastWallClockTime = prefs.getLong(LAST_WALL_CLOCK_TIME + id,
                        Timer.UNUSED);
                final long remainingTime = prefs.getLong(REMAINING_TIME + id, totalLength);
                final String label = prefs.getString(LABEL + id, null);
                final String buttonTime = prefs.getString(BUTTON_TIME + id, "1");
                final boolean deleteAfterUse = prefs.getBoolean(DELETE_AFTER_USE + id, false);
                timers.add(new Timer(id, state, length, totalLength, lastStartTime,
                        lastWallClockTime, remainingTime, label, buttonTime, deleteAfterUse));
            }
        }

        return timers;
    }

    /**
     * @param timer the timer to be added
     */
    static Timer addTimer(SharedPreferences prefs, Timer timer) {
        final SharedPreferences.Editor editor = prefs.edit();

        // Fetch the next timer id.
        final int id = prefs.getInt(NEXT_TIMER_ID, 0);
        editor.putInt(NEXT_TIMER_ID, id + 1);

        // Add the new timer id to the set of all timer ids.
        final Set<String> timerIds = new HashSet<>(getTimerIds(prefs));
        timerIds.add(String.valueOf(id));
        editor.putStringSet(TIMER_IDS, timerIds);

        // Record the fields of the timer.
        editor.putInt(STATE + id, timer.getState().getValue());
        editor.putLong(LENGTH + id, timer.getLength());
        editor.putLong(TOTAL_LENGTH + id, timer.getTotalLength());
        editor.putLong(LAST_START_TIME + id, timer.getLastStartTime());
        editor.putLong(LAST_WALL_CLOCK_TIME + id, timer.getLastWallClockTime());
        editor.putLong(REMAINING_TIME + id, timer.getRemainingTime());
        editor.putString(LABEL + id, timer.getLabel());
        editor.putString(BUTTON_TIME + id, timer.getButtonTime());
        editor.putBoolean(DELETE_AFTER_USE + id, timer.getDeleteAfterUse());

        editor.apply();

        // Return a new timer with the generated timer id present.
        return new Timer(id, timer.getState(), timer.getLength(), timer.getTotalLength(),
                timer.getLastStartTime(), timer.getLastWallClockTime(), timer.getRemainingTime(),
                timer.getLabel(), timer.getButtonTime(), timer.getDeleteAfterUse());
    }

    /**
     * @param timer the timer to be updated
     */
    static void updateTimer(SharedPreferences prefs, Timer timer) {
        final SharedPreferences.Editor editor = prefs.edit();

        // Record the fields of the timer.
        final int id = timer.getId();
        editor.putInt(STATE + id, timer.getState().getValue());
        editor.putLong(LENGTH + id, timer.getLength());
        editor.putLong(TOTAL_LENGTH + id, timer.getTotalLength());
        editor.putLong(LAST_START_TIME + id, timer.getLastStartTime());
        editor.putLong(LAST_WALL_CLOCK_TIME + id, timer.getLastWallClockTime());
        editor.putLong(REMAINING_TIME + id, timer.getRemainingTime());
        editor.putString(LABEL + id, timer.getLabel());
        editor.putString(BUTTON_TIME + id, timer.getButtonTime());
        editor.putBoolean(DELETE_AFTER_USE + id, timer.getDeleteAfterUse());

        editor.apply();
    }

    /**
     * @param timer the timer to be removed
     */
    static void removeTimer(SharedPreferences prefs, Timer timer) {
        final SharedPreferences.Editor editor = prefs.edit();

        final int id = timer.getId();

        // Remove the timer id from the set of all timer ids.
        final Set<String> timerIds = new HashSet<>(getTimerIds(prefs));
        timerIds.remove(String.valueOf(id));
        if (timerIds.isEmpty()) {
            editor.remove(TIMER_IDS);
            editor.remove(NEXT_TIMER_ID);
        } else {
            editor.putStringSet(TIMER_IDS, timerIds);
        }

        // Record the fields of the timer.
        editor.remove(STATE + id);
        editor.remove(LENGTH + id);
        editor.remove(TOTAL_LENGTH + id);
        editor.remove(LAST_START_TIME + id);
        editor.remove(LAST_WALL_CLOCK_TIME + id);
        editor.remove(REMAINING_TIME + id);
        editor.remove(LABEL + id);
        editor.remove(BUTTON_TIME + id);
        editor.remove(DELETE_AFTER_USE + id);

        editor.apply();
    }

    private static Set<String> getTimerIds(SharedPreferences prefs) {
        return prefs.getStringSet(TIMER_IDS, Collections.emptySet());
    }
}
