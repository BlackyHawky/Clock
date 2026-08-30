// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ANALOG_CLOCK_SIZE;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uicomponents.CustomTypefaceSpan;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ClockUtils {

    /**
     * Configure the analog clock that is visible to display seconds.
     * If the analog clock is not visible, it never displays seconds to avoid it scheduling unnecessary
     * ticking runnable.
     */
    public static void setAnalogClockSecondsEnabled(@NonNull DataModel.ClockStyle clockStyle, @NonNull AnalogClock analogClock,
                                                    boolean displaySeconds) {

        switch (clockStyle) {
            case ANALOG, ANALOG_MATERIAL -> {
                analogClock.enableSeconds(displaySeconds);
                return;
            }
            case DIGITAL -> {
                analogClock.enableSeconds(false);
                return;
            }
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * Set whether the digital or analog clock should be displayed in the application.
     * Returns the view to be displayed.
     *
     * @param digitalClock if the view concerned is the digital clock
     * @param analogClock  if the view concerned is the analog clock
     */
    public static void setClockStyle(@NonNull DataModel.ClockStyle clockStyle, @NonNull View digitalClock, @NonNull View analogClock) {
        switch (clockStyle) {
            case ANALOG, ANALOG_MATERIAL -> {
                analogClock.setVisibility(View.VISIBLE);
                digitalClock.setVisibility(View.GONE);
                return;
            }
            case DIGITAL -> {
                digitalClock.setVisibility(View.VISIBLE);
                analogClock.setVisibility(View.GONE);
                return;
            }
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * Adjusts the size of the analog clock view based on the current screen orientation.
     *
     * @param analogClock    the analog clock view whose size should be adjusted
     * @param displayMetrics the display metrics containing screen size and density
     * @param sizePercent    the analog clock size defined in the settings
     */
    public static void adjustAnalogClockSize(@NonNull View analogClock, @NonNull DisplayMetrics displayMetrics, float sizePercent,
                                             boolean isLandscape) {

        float factor = computeFactor(sizePercent);

        int screenHeight = displayMetrics.heightPixels;
        int baseSize = isLandscape
            ? (int) (screenHeight / 2.6)
            : (int) (screenHeight / 3.8);

        int finalSize = (int) (baseSize * factor);

        analogClock.getLayoutParams().height = finalSize;
        analogClock.getLayoutParams().width = finalSize;

        analogClock.requestLayout();
    }

    /**
     * Computes a scaling factor for the analog clock size based on a user-defined
     * percentage value. The percentage ranges from 1 to 100 and is mapped to a
     * size multiplier between 0.5× and 1.2×. Values from 1 to 70 scale linearly
     * from 0.5× to 1.0×, while values from 70 to 100 scale from 1.0× to 1.2×.
     *
     * @param sizePercent the user-selected size percentage (1–100)
     * @return the computed scaling factor to apply to the base clock size
     */
    private static float computeFactor(float sizePercent) {
        if (sizePercent <= DEFAULT_ANALOG_CLOCK_SIZE) {
            // 1 → 70  => 0.5 → 1.0
            return 0.5f + ((sizePercent - 1) / 69f) * 0.5f;
        } else {
            // 70 → 100 => 1.0 → 1.2
            return 1.0f + ((sizePercent - 70) / 30f) * 0.2f;
        }
    }

    /**
     * Formats the time in the TextClock according to the Locale with a special
     * formatting treatment for the am/pm label.
     *
     * @param clock                 TextClock to format
     * @param includeSeconds        whether to include seconds in the clock's time
     * @param amPmRatio             a value between 0 and 1 that is the ratio of the relative size of the am/pm string to the time string
     * @param amPmTypeface          the {@link Typeface} applied to the am/pm text
     * @param fallbackFontFamily    the fallback font family used if amPmTypeface is null
     * @param textStyle             the font style (e.g., 'Typeface.BOLD') to apply to the am/pm text
     * @param addScreensaverPadding {@code true} if an EM space should be added around the am/pm text
     *                              to prevent it from being cut off on some screensavers
     */
    public static void setDigitalClockTimeFormat(@Nullable TextClock clock, boolean includeSeconds, float amPmRatio,
                                                 @Nullable Typeface amPmTypeface, @NonNull String fallbackFontFamily, int textStyle,
                                                 boolean addScreensaverPadding) {
        if (clock != null) {
            // Get the best format for 12 hours mode according to the locale
            clock.setFormat12Hour(
                get12ModeFormat(includeSeconds, amPmRatio, amPmTypeface, fallbackFontFamily, textStyle, addScreensaverPadding));
            // Get the best format for 24 hours mode according to the locale
            clock.setFormat24Hour(get24ModeFormat(includeSeconds, addScreensaverPadding));
        }
    }

    /**
     * @param includeSeconds        whether to include seconds in the time string
     * @param amPmRatio             a value between 0 and 1 that is the ratio of the relative size of the am/pm string to the time string
     * @param amPmTypeface          the {@link Typeface} applied to the am/pm text
     * @param fallbackFontFamily    the fallback font family used if amPmTypeface is null
     * @param textStyle             the font style (e.g., 'Typeface.BOLD') to apply to the am/pm text
     * @param addScreensaverPadding {@code true} if an EM space should be added around the am/pm text
     *                              to prevent it from being cut off on some screensavers
     * @return a formatted CharSequence for 12-hour mode time, with styled AM/PM text
     */
    @NonNull
    public static CharSequence get12ModeFormat(boolean includeSeconds, float amPmRatio, @Nullable Typeface amPmTypeface,
                                               @NonNull String fallbackFontFamily, int textStyle, boolean addScreensaverPadding) {

        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), includeSeconds ? "hmsa" : "hma");

        // Replace spaces with "Hair Space"
        pattern = pattern.replaceAll("\\s", "\u200A");

        if (amPmRatio <= 0) {
            pattern = pattern.replace("\u200Aa", "").trim();
        } else if (addScreensaverPadding) {
            // For screensaver, add an "EM Space" (\u2003) at the end of the AM/PM to prevent
            // its display from being cut off on some devices.
            pattern = "\u2003" + pattern.replace("a", "a" + "\u2003");
        }

        // Build a spannable so that the am/pm will be formatted
        int amPmPos = pattern.indexOf('a');
        if (amPmPos == -1) {
            return pattern;
        }

        final Spannable sp = new SpannableString(pattern);
        sp.setSpan(new RelativeSizeSpan(amPmRatio), amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (amPmTypeface != null) {
            if (SdkUtils.isAtLeastAndroid9()) {
                sp.setSpan(new TypefaceSpan(amPmTypeface), amPmPos, amPmPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                sp.setSpan(new CustomTypefaceSpan(amPmTypeface), amPmPos, amPmPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            sp.setSpan(new TypefaceSpan(fallbackFontFamily), amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        sp.setSpan(new StyleSpan(textStyle), amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return sp;
    }

    public static CharSequence get24ModeFormat(boolean includeSeconds, boolean isScreensaver) {
        if (isScreensaver) {
            // For screensaver, add an "EM Space" (\u2003) at the end of the time to prevent
            // its display from being cut off on some devices.
            return "\u2003"
                + DateFormat.getBestDateTimePattern(Locale.getDefault(), includeSeconds ? "Hms" : "Hm")
                + "\u2003";
        } else {
            return DateFormat.getBestDateTimePattern(Locale.getDefault(), includeSeconds ? "Hms" : "Hm");
        }
    }

    /**
     * Clock views can call this to refresh their date.
     **/
    public static void updateDate(@NonNull String dateSkeleton, @NonNull String descriptionSkeleton, @NonNull View clock,
                                  boolean isUppercase) {

        final TextView dateDisplay = clock.findViewById(R.id.date);

        if (dateDisplay == null) {
            return;
        }

        final Locale locale = Locale.getDefault();
        String datePattern = DateFormat.getBestDateTimePattern(locale, dateSkeleton);
        final String descriptionPattern = DateFormat.getBestDateTimePattern(locale, descriptionSkeleton);

        final Date now = new Date();
        String formattedDate = new SimpleDateFormat(datePattern, locale).format(now);

        dateDisplay.setAllCaps(isUppercase);
        dateDisplay.setText(FormattedTextUtils.capitalizeFirstLetter(formattedDate, locale));
        dateDisplay.setVisibility(View.VISIBLE);
        dateDisplay.setContentDescription(new SimpleDateFormat(descriptionPattern, locale).format(now));
    }

    /**
     * Applies a bold font to the date.
     */
    public static void applyBoldDateTypeface(@NonNull View clock, @NonNull Typeface boldTypeface) {
        final TextView date = clock.findViewById(R.id.date);

        if (date == null) {
            return;
        }

        date.setTypeface(boldTypeface);
    }

    /**
     * Given a point in time, return the subsequent moment any of the time zones changes days.
     * e.g. Given 8:00pm on 1/1/2016 and time zones in LA and NY this method would return a Date for
     * midnight on 1/2/2016 in the NY timezone since it changes days first.
     *
     * @param time  a point in time from which to compute midnight on the subsequent day
     * @param zones a collection of time zones
     * @return the nearest point in the future at which any of the time zones changes days
     */
    @Nullable
    public static Date getNextDay(@NonNull Date time, @NonNull Collection<TimeZone> zones) {
        Calendar next = null;
        for (TimeZone tz : zones) {
            final Calendar c = Calendar.getInstance(tz);
            c.setTime(time);

            // Advance to the next day.
            c.add(Calendar.DAY_OF_YEAR, 1);

            // Reset the time to midnight.
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            if (next == null || c.compareTo(next) < 0) {
                next = c;
            }
        }

        return next == null ? null : next.getTime();
    }

    /**
     * Apply the clock icon font to the next alarm view.
     */
    public static void setClockIconTypeface(@NonNull View clock) {
        final TextView nextAlarmIconView = clock.findViewById(R.id.nextAlarmIcon);
        nextAlarmIconView.setTypeface(getAlarmIconTypeface(clock.getContext()));
    }

    /**
     * <ul>
     *     <li>To display the alarm clock in this font, use the character {@link R.string#clock_emoji}.</li>
     *     <li>To display the label in this font, use the character {@link R.string#label_emoji}.</li>
     * </ul>
     *
     * @return a special font containing a glyph that draws an alarm clock or a label.
     */
    public static Typeface getAlarmIconTypeface(@NonNull Context context) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/clock.ttf");
    }
}
