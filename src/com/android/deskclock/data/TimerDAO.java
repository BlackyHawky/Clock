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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.deskclock.data.Timer.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.android.deskclock.data.Timer.State.RESET;

/**
 * This class encapsulates the transfer of data between {@link Timer} domain objects and their
 * permanent storage in {@link SharedPreferences}.
 */
final class TimerDAO {

    // Key to a preference that stores the set of timer ids.
    private static final String TIMER_IDS = "timers_list";

    // Key to a preference that stores the id to assign to the next timer.
    private static final String NEXT_TIMER_ID = "next_timer_id";

    // Prefix for a key to a preference that stores the state of the timer.
    private static final String STATE = "timer_state_";

    // Prefix for a key to a preference that stores the length of the timer when it was created.
    private static final String LENGTH = "timer_setup_timet_";

    // Prefix for a key to a preference that stores the total length of the timer with additions.
    private static final String TOTAL_LENGTH = "timer_original_timet_";

    // Prefix for a key to a preference that stores the last start time of the timer.
    private static final String LAST_START_TIME = "timer_start_time_";

    // Prefix for a key to a preference that stores the remaining time before expiry.
    private static final String REMAINING_TIME = "timer_time_left_";

    // Prefix for a key to a preference that stores the label of the timer.
    private static final String LABEL = "timer_label_";

    // Prefix for a key to a preference that signals the timer should be deleted on first reset.
    private static final String DELETE_AFTER_USE = "delete_after_use_";

    // Lazily instantiated and cached for the life of the application.
    private static SharedPreferences sPrefs;

    private TimerDAO() {}

    /**
     * @return the timers from permanent storage
     */
    public static List<Timer> getTimers(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);

        // Read the set of timer ids.
        final Set<String> timerIds = prefs.getStringSet(TIMER_IDS, Collections.<String>emptySet());
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
                final long lastStartTime = prefs.getLong(LAST_START_TIME + id, Long.MIN_VALUE);
                final long remainingTime = prefs.getLong(REMAINING_TIME + id, totalLength);
                final String label = prefs.getString(LABEL + id, null);
                final boolean deleteAfterUse = prefs.getBoolean(DELETE_AFTER_USE + id, false);
                timers.add(new Timer(id, state, length, totalLength, lastStartTime, remainingTime,
                        label, deleteAfterUse));
            }
        }

        return timers;
    }

    /**
     * @param timer the timer to be added
     */
    public static Timer addTimer(Context context, Timer timer) {
        final SharedPreferences prefs = getSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();

        // Fetch the next timer id.
        final int id = prefs.getInt(NEXT_TIMER_ID, 0);
        editor.putInt(NEXT_TIMER_ID, id + 1);

        // Add the new timer id to the set of all timer ids.
        final Set<String> timerIds = new HashSet<>(getTimerIds(context));
        timerIds.add(String.valueOf(id));
        editor.putStringSet(TIMER_IDS, timerIds);

        // Record the fields of the timer.
        editor.putInt(STATE + id, timer.getState().getValue());
        editor.putLong(LENGTH + id, timer.getLength());
        editor.putLong(TOTAL_LENGTH + id, timer.getTotalLength());
        editor.putLong(LAST_START_TIME + id, timer.getLastStartTime());
        editor.putLong(REMAINING_TIME + id, timer.getRemainingTime());
        editor.putString(LABEL + id, timer.getLabel());
        editor.putBoolean(DELETE_AFTER_USE + id, timer.getDeleteAfterUse());

        editor.apply();

        // Return a new timer with the generated timer id present.
        return new Timer(id, timer.getState(), timer.getLength(), timer.getTotalLength(),
                timer.getLastStartTime(), timer.getRemainingTime(), timer.getLabel(),
                timer.getDeleteAfterUse());
    }

    /**
     * @param timer the timer to be updated
     */
    public static void updateTimer(Context context, Timer timer) {
        final SharedPreferences prefs = getSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();

        // Record the fields of the timer.
        final int id = timer.getId();
        editor.putInt(STATE + id, timer.getState().getValue());
        editor.putLong(LENGTH + id, timer.getLength());
        editor.putLong(TOTAL_LENGTH + id, timer.getTotalLength());
        editor.putLong(LAST_START_TIME + id, timer.getLastStartTime());
        editor.putLong(REMAINING_TIME + id, timer.getRemainingTime());
        editor.putString(LABEL + id, timer.getLabel());
        editor.putBoolean(DELETE_AFTER_USE + id, timer.getDeleteAfterUse());

        editor.apply();
    }

    /**
     * @param timer the timer to be removed
     */
    public static void removeTimer(Context context, Timer timer) {
        final SharedPreferences prefs = getSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();

        final int id = timer.getId();

        // Remove the timer id from the set of all timer ids.
        final Set<String> timerIds = new HashSet<>(getTimerIds(context));
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
        editor.remove(REMAINING_TIME + id);
        editor.remove(LABEL + id);
        editor.remove(DELETE_AFTER_USE + id);

        editor.apply();
    }

    private static Set<String> getTimerIds(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getStringSet(TIMER_IDS, Collections.<String>emptySet());
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        }

        return sPrefs;
    }
}