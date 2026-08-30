// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.uidata;

import android.graphics.Typeface;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * A container class that groups UI-related configurations.
 * This helps avoid "Long Parameter List" code smells in Adapters and ViewHolders by bundling related variables together
 * (Parameter Object Pattern).
 */
public class UiConfig {

    /**
     * Groups typeface configurations used across the application.
     *
     * @param general        The standard regular typeface.
     * @param bold           The bold version of the general typeface.
     * @param alarmClockFont The specific digital font used for alarm clocks (can be null if not needed).
     * @param timerFont      The specific font used for timers (can be null if not needed).
     */
    public record Fonts(
        @NonNull Typeface general,
        @NonNull Typeface bold,
        @Nullable Typeface alarmClockFont,
        @Nullable Typeface timerFont,
        @Nullable Typeface clockFont,
        @Nullable Typeface clockBoldFont
    ) {}

    /**
     * Groups date localization and formatting patterns.
     *
     * @param locale          The current locale of the device.
     * @param pattern         The standard date format pattern (e.g., without year).
     * @param patternWithYear The date format pattern including the year.
     */
    public record DateFormat(
        @NonNull Locale locale,
        @NonNull String pattern,
        @NonNull String patternWithYear
    ) {}

    /**
     * Groups time formatting patterns, typically used for clocks and timers.
     *
     * @param locale        The current locale of the device.
     * @param pattern12     The time format pattern for 12-hour mode (AM/PM).
     * @param pattern24     The time format pattern for 24-hour mode.
     * @param is24HoursMode {@code true} if the time is in 24-hour format; 12-hour mode otherwise.
     */
    public record TimeFormat(
        @NonNull Locale locale,
        @NonNull String pattern12,
        @NonNull String pattern24,
        boolean is24HoursMode
    ) {}

    /**
     * Groups display metrics and layout state flags.
     *
     * @param metrics     The display metrics containing screen size and density.
     * @param isTablet    {@code true} if the device is currently using a tablet layout; {@code false} otherwise.
     * @param isPortrait  {@code true} if the device is in portrait orientation; {@code false} otherwise.
     * @param isLandscape {@code true} if the device is in landscape orientation; {@code false} otherwise.
     * @param isRtl       {@code true} if the layout direction is Right-To-Left (RTL); {@code false} otherwise.
     */
    public record Screen(
        @NonNull DisplayMetrics metrics,
        boolean isTablet,
        boolean isPortrait,
        boolean isLandscape,
        boolean isRtl
    ) {}

    /**
     * Groups theme and card styling configurations used across the application.
     *
     * @param isBackgroundDisplayed {@code true} if the card background color should be displayed.
     * @param isBorderDisplayed     {@code true} if a border stroke should be drawn around the cards.
     * @param isAmoledDarkMode      {@code true} if the pure black AMOLED dark mode is currently active.
     */
    public record CardStyle(
        boolean isBackgroundDisplayed,
        boolean isBorderDisplayed,
        boolean isAmoledDarkMode
    ) {}

    /**
     * Groups global user haptics settings.
     *
     * @param isVibrationsEnabled {@code true} if haptic feedback is allowed globally.
     */
    public record Haptics(
        boolean isVibrationsEnabled
    ) {}

}
