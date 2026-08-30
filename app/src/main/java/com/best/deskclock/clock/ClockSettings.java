// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.clock;

import com.best.deskclock.data.DataModel;

/**
 * A container class that holds all user preferences and UI configurations specific to the main Clock and World Clock features.
 */
public class ClockSettings {

    // --- Main Clock ---

    /** The overall style of the clock (e.g., Digital or Analog). */
    public DataModel.ClockStyle clockStyle;

    /** True if the clock uses a 24-hour format, false for 12-hour (AM/PM) format. */
    public boolean is24HourFormat;

    /** True if the clock should display the current seconds. */
    public boolean showSeconds;

    /** True if the upcoming scheduled alarm should be displayed under the clock. */
    public boolean isNextAlarmDisplayed;

    /** True if text elements (like AM/PM or date) should be forced to uppercase. */
    public boolean isTextUppercase;

    // --- Digital Clock ---

    /** The font size used for the digital clock display. */
    public int digitalClockFontSize;

    // --- Analog Clock ---

    /** The classic analog clock dial style. */
    public String clockDial;

    /** The Material analog clock dial style. */
    public String clockDialMaterial;

    /** The analog clock second hand style. */
    public String clockSecondHand;

    /** The resolved accent color (taking auto night mode into account) used to tint the analog clock components. */
    public String activeAccentColor;

    /** The size of the analog clock face, expressed as a percentage of the screen space. */
    public int analogClockSizePercent;

    // --- City List (World Clock) ---

    /** True if a secondary clock showing the home time zone should be displayed when traveling. */
    public boolean showHomeClock;

    /** True if custom notes or descriptions for saved cities are enabled and displayed. */
    public boolean isCityNoteEnabled;

    /** The sorting criteria used to order the selected cities in the World Clock list. */
    public String citySorting;
}
