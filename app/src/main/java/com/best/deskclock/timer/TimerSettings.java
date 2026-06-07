// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import android.graphics.Typeface;

public class TimerSettings {
    // Timer time font
    public Typeface timerTimeTypeface;

    // Booleans
    public boolean isSingleTimerMode;
    public boolean areTimerButtonPositionsInverted;
    public boolean isIndicatorStateDisplay;

    // Colors
    public int colorPaused;
    public int colorRunning;
    public int colorExpired;
    public int colorMissed;

    // Sorting
    public String timerSorting;
}
