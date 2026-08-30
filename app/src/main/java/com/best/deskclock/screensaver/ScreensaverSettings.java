// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.screensaver;

import android.graphics.Typeface;

import com.best.deskclock.data.DataModel;

/**
 * A container class that holds all user preferences and UI configurations specific to the screensaver.
 */
public class ScreensaverSettings {

    // --- Miscellaneous ---

    /** The absolute file path to the custom background image, or null if none is set. */
    public String backgroundImagePath;

    /** The resolved custom typeface to be applied to the screensaver text elements. */
    public Typeface screensaverTypeface;

    /** The blur intensity/radius applied to the screensaver background image. */
    public float blurIntensity;

    /** The selected style for the screensaver clock (e.g., Digital, Analog, Material). */
    public DataModel.ClockStyle clockStyle;

    /** Whether the clock should display seconds (second hand for analog, digits for digital). */
    public boolean areClockSecondsEnabled;

    /** The brightness level percentage (0-100) used to dim the screensaver elements. */
    public int brightnessPercentage;

    /** Whether the text elements (such as date and next alarm) should be displayed in uppercase. */
    public boolean isUppercase;


    // --- Colors ---

    /** The resolved color applied to the clock elements. */
    public int clockColor;

    /** The resolved color applied to the date text. */
    public int dateColor;

    /** The resolved color applied to the next alarm text and icon. */
    public int nextAlarmColor;

    /** The resolved color applied to the battery text and icon. */
    public int batteryColor;

    /** The resolved accent color key used to tint specific analog clock components. */
    public String activeAccentColor;


    // --- Analog Clock ---

    /** The preference string representing the standard analog clock dial style. */
    public String clockDial;

    /** The preference string representing the Material analog clock dial style. */
    public String clockDialMaterial;

    /** The preference string representing the analog clock second hand style. */
    public String clockSecondHand;

    /** The size ratio/percentage of the analog clock relative to the screen size. */
    public float analogClockSize;


    // --- Digital Clock ---

    /** Whether the digital clock text should be rendered in bold. */
    public boolean isDigitalBold;

    /** Whether the digital clock text should be rendered in italic. */
    public boolean isDigitalItalic;

    /** The user-preferred font size for the digital clock. */
    public float digitalFontSize;


    // --- Other UI Elements ---

    /** Whether the date text should be rendered in bold. */
    public boolean isDateBold;

    /** Whether the date text should be rendered in italic. */
    public boolean isDateItalic;

    /** Whether the next upcoming alarm should be displayed on the screensaver. */
    public boolean isNextAlarmDisplayed;

    /** Whether the next alarm text should be rendered in bold. */
    public boolean isNextAlarmBold;

    /** Whether the next alarm text should be rendered in italic. */
    public boolean isNextAlarmItalic;

    /** Whether the current device battery level should be displayed on the screensaver. */
    public boolean isBatteryDisplayed;

    /** Whether the battery text should be rendered in bold. */
    public boolean isBatteryBold;

    /** Whether the battery text should be rendered in italic. */
    public boolean isBatteryItalic;

}
