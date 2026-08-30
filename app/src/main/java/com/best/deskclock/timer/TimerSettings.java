// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

/**
 * A container class that holds all user preferences and UI configurations specific to the Timer feature.
 */
public class TimerSettings {

    /** The format pattern used to display the timer's expected end time. */
    public CharSequence timerEndTimeFormatPattern;

    // --- Booleans ---

    /** True if the app is restricted to displaying/running only one timer at a time. */
    public boolean isSingleTimerMode;

    /** True if timers are displayed in compact mode. */
    public boolean isCompactTimersDisplayed;

    /** True if the expected end time of the timer (e.g., "Ends at 14:30") should be displayed. */
    public boolean isTimerEndTimeDisplayed;

    /** True if the end time should be formatted in 24-hour format, false for AM/PM. */
    public boolean is24HourFormat;

    /** True if the start/stop/reset button positions should be inverted (e.g., for left-handed use). */
    public boolean areTimerButtonPositionsInverted;

    /** True if the visual state indicator (showing whether the timer is running/paused) is visible. */
    public boolean isIndicatorStateDisplay;

    // --- Indicator colors ---

    /** The color representing a paused timer. */
    public int colorPaused;

    /** The color representing a currently running timer. */
    public int colorRunning;

    /** The color representing a timer that has reached zero and is actively expiring. */
    public int colorExpired;

    /** The color representing a timer that has expired and was missed/ignored by the user. */
    public int colorMissed;

    // --- Sorting ---

    /** The sorting criteria used to order the list of active/saved timers. */
    public String timerSorting;

    /**
     * The custom manual order of timers, represented as a comma-separated string of timer IDs.
     * This is used exclusively when {@code timerSorting} is set to manual sorting.
     */
    public String savedTimerOrder;
}
