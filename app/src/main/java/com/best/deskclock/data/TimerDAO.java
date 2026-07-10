/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static com.best.deskclock.data.Timer.State.RESET;

import android.content.SharedPreferences;
import android.net.Uri;

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
    public static final String STATE = "timer_state_";

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
    public static final String BUTTON_TIME = "timer_button_time_";

    /**
     * Prefix for a key to a preference that stores the timer ringtone.
     */
    public static final String TIMER_RINGTONE = "timer_ringtone_";

    /**
     * Prefix for a key to a preference that stores the timer auto silence.
     */
    private static final String AUTO_SILENCE = "timer_auto_silence_";

    /**
     * Prefix for a key to a preference that stores the timer auto silence.
     */
    private static final String VOLUME_CRESCENDO = "timer_volume_crescendo_";

    /**
     * Prefix for a key to a preference that signals the timer should vibrate when it expires.
     */
    private static final String VIBRATE = "timer_vibrate_";

    /**
     * Prefix for a key to a preference that signals the flash should turn on when timers expire.
     */
    private static final String FLASH_ON = "timer_flash_on_";

    /**
     * Prefix for a key to a preference that signals the timer should be deleted on first reset.
     */
    public static final String DELETE_AFTER_USE = "timer_delete_after_use_";

    private TimerDAO() {
    }

    /**
     * @return the timers from permanent storage
     */
    static List<Timer> getTimers(SharedPreferences prefs, Uri defaultUri) {
        // Read the set of timer ids.
        final Set<String> timerIds = prefs.getStringSet(TIMER_IDS, Collections.emptySet());
        final List<Timer> timers = new ArrayList<>(timerIds.size());
        final boolean defaultVibrateFallback = SettingsDAO.isTimerVibrate(prefs);
        final boolean defaultFlashOnFallback = SettingsDAO.shouldTurnOnBackFlashForExpiredTimer(prefs);
        final String addTimeButtonValueFallback = String.valueOf(SettingsDAO.getDefaultTimeToAddToTimer(prefs));
        final Uri ringtoneFallback = SettingsDAO.getTimerRingtoneUri(prefs, defaultUri);
        final int autoSilenceDurationFallback = SettingsDAO.getTimerAutoSilenceDuration(prefs);
        final int volumeCrescendoDurationFallback = SettingsDAO.getTimerVolumeCrescendoDuration(prefs);

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
                final long lastWallClockTime = prefs.getLong(LAST_WALL_CLOCK_TIME + id, Timer.UNUSED);
                final long remainingTime = prefs.getLong(REMAINING_TIME + id, totalLength);
                final String label = prefs.getString(LABEL + id, null);
                final String buttonTime = prefs.getString(BUTTON_TIME + id, addTimeButtonValueFallback);

                String uriString = prefs.getString(TIMER_RINGTONE + id, null);

                if (uriString == null || uriString.isEmpty() || uriString.equals("null")) {
                    uriString = ringtoneFallback.toString();
                }

                final Uri ringtone = Uri.parse(uriString);

                final int autoSilenceDuration = prefs.getInt(AUTO_SILENCE + id, autoSilenceDurationFallback);
                final int volumeCrescendoDuration = prefs.getInt(VOLUME_CRESCENDO + id, volumeCrescendoDurationFallback);
                final boolean vibrate = prefs.getBoolean(VIBRATE + id, defaultVibrateFallback);
                final boolean flashOn = prefs.getBoolean(FLASH_ON + id, defaultFlashOnFallback);
                final boolean deleteAfterUse = prefs.getBoolean(DELETE_AFTER_USE + id, false);

                timers.add(new Timer(id, state, length, totalLength, lastStartTime, lastWallClockTime, remainingTime, label, buttonTime,
                    ringtone, autoSilenceDuration, volumeCrescendoDuration, vibrate, flashOn, deleteAfterUse)
                );
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

        String ringtoneString = (timer.getRingtoneUri() != null) ? timer.getRingtoneUri().toString() : "";
        editor.putString(TIMER_RINGTONE + id, ringtoneString);

        editor.putInt(AUTO_SILENCE + id, timer.getAutoSilence());
        editor.putInt(VOLUME_CRESCENDO + id, timer.getVolumeCrescendoDuration());
        editor.putBoolean(VIBRATE + id, timer.isVibrate());
        editor.putBoolean(FLASH_ON + id, timer.isFlashOn());
        editor.putBoolean(DELETE_AFTER_USE + id, timer.getDeleteAfterUse());

        editor.apply();

        // Return a new timer with the generated timer id present.
        return new Timer(id, timer.getState(), timer.getLength(), timer.getTotalLength(), timer.getLastStartTime(),
            timer.getLastWallClockTime(), timer.getRemainingTime(), timer.getLabel(), timer.getButtonTime(), timer.getRingtoneUri(),
            timer.getAutoSilence(), timer.getVolumeCrescendoDuration(), timer.isVibrate(), timer.isFlashOn(), timer.getDeleteAfterUse()
        );
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

        String ringtoneString = (timer.getRingtoneUri() != null) ? timer.getRingtoneUri().toString() : "";
        editor.putString(TIMER_RINGTONE + id, ringtoneString);

        editor.putInt(AUTO_SILENCE + id, timer.getAutoSilence());
        editor.putInt(VOLUME_CRESCENDO + id, timer.getVolumeCrescendoDuration());
        editor.putBoolean(VIBRATE + id, timer.isVibrate());
        editor.putBoolean(FLASH_ON + id, timer.isFlashOn());
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
        editor.remove(TIMER_RINGTONE + id);
        editor.remove(AUTO_SILENCE + id);
        editor.remove(VOLUME_CRESCENDO + id);
        editor.remove(VIBRATE + id);
        editor.remove((FLASH_ON + id));
        editor.remove(DELETE_AFTER_USE + id);

        editor.apply();
    }

    private static Set<String> getTimerIds(SharedPreferences prefs) {
        return prefs.getStringSet(TIMER_IDS, Collections.emptySet());
    }
}
