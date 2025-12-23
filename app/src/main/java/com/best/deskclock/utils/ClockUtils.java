// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ANALOG_CLOCK_SIZE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
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
    public static void setAnalogClockSecondsEnabled(DataModel.ClockStyle clockStyle, AnalogClock analogClock,
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
    public static void setClockStyle(DataModel.ClockStyle clockStyle, View digitalClock, View analogClock) {
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
     * Adjusts the size of the analog clock view based on the current screen orientation
     * and user-defined preferences. When displayed in the Clock tab, the size is scaled
     * using a percentage value stored in SharedPreferences. This percentage is mapped
     * to a factor between 0.5× and 1.2× of the base size.
     *
     * @param analogClock the analog clock view whose size should be adjusted
     * @param prefs       the SharedPreferences containing user-defined size settings
     * @param isClockTab  {@code true} if the clock is displayed in the Clock tab; {@code false} otherwise
     */
    public static void adjustAnalogClockSize(View analogClock, SharedPreferences prefs, boolean isAlarm,
                                             boolean isClockTab, boolean isScreensaver) {

        float factor = 1.0f;

        if (isAlarm) {
            int sizePercent = SettingsDAO.getAlarmAnalogClockSize(prefs);

            factor = computeFactor(sizePercent);
        } else if (isClockTab) {
            int sizePercent = SettingsDAO.getAnalogClockSize(prefs);

            factor = computeFactor(sizePercent);
        } else if (isScreensaver) {
            int sizePercent = SettingsDAO.getScreensaverAnalogClockSize(prefs);

            factor = computeFactor(sizePercent);
        }

        int screenHeight = analogClock.getContext().getResources().getDisplayMetrics().heightPixels;
        int baseSize = ThemeUtils.isLandscape()
                ? (int) (screenHeight / 2.6)
                : (int) (screenHeight / 3.8);

        int finalSize = (int) (baseSize * factor);

        analogClock.getLayoutParams().height = finalSize;
        analogClock.getLayoutParams().width = finalSize;
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
    private static float computeFactor(int sizePercent) {
        if (sizePercent <= DEFAULT_ANALOG_CLOCK_SIZE) {
            // 1 → 70  => 0.5 → 1.0
            return 0.5f + ((sizePercent - 1) / 69f) * 0.5f;
        } else {
            // 70 → 100 => 1.0 → 1.2
            return 1.0f + ((sizePercent - 70) / 30f) * 0.2f;
        }
    }

    /**
     * Sets the typeface of a digital clock (TextClock) based on user preferences.
     * <p>
     * This method retrieves the font path stored in SharedPreferences and attempts
     * to load the corresponding Typeface from the file system. If the font file
     * does not exist, cannot be loaded, or no font is defined in preferences,
     * the clock will fall back to the default {@link Typeface#SANS_SERIF}.
     * </p>
     *
     * @param clock the TextClock instance whose font should be updated
     */
    public static void setDigitalClockFont(TextClock clock, String fontPath) {
        Typeface typeface = ThemeUtils.loadFont(fontPath);
        clock.setTypeface(typeface);
    }

    /**
     * Formats the time in the TextClock according to the Locale with a special
     * formatting treatment for the am/pm label.
     *
     * @param clock          TextClock to format
     * @param includeSeconds whether or not to include seconds in the clock's time
     */
    public static void setDigitalClockTimeFormat(TextClock clock, float amPmRatio, boolean includeSeconds,
                                                 boolean isAlarm, boolean isClockTab, boolean isScreensaver) {

        if (clock != null) {
            // Get the best format for 12 hours mode according to the locale
            clock.setFormat12Hour(get12ModeFormat(
                    clock.getContext(), amPmRatio, includeSeconds, isAlarm, isClockTab, isScreensaver));
            // Get the best format for 24 hours mode according to the locale
            clock.setFormat24Hour(get24ModeFormat(clock.getContext(), includeSeconds, isScreensaver));
        }
    }

    /**
     * @param amPmRatio      a value between 0 and 1 that is the ratio of the relative size of the
     *                       am/pm string to the time string
     * @param includeSeconds whether or not to include seconds in the time string
     * @return format string for 12 hours mode time, not including seconds
     */
    public static CharSequence get12ModeFormat(Context context, float amPmRatio, boolean includeSeconds,
                                               boolean isAlarm, boolean isClockTab, boolean isScreensaver) {

        SharedPreferences prefs = getDefaultSharedPreferences(context);

        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                includeSeconds ? "hmsa" : "hma");

        // Replace spaces with "Hair Space"
        pattern = pattern.replaceAll("\\s", "\u200A");

        if (amPmRatio <= 0) {
            pattern = pattern.replaceAll("\u200Aa", "").trim();
        } else {
            if (isScreensaver && SettingsDAO.isScreensaverDigitalClockInItalic(prefs)) {
                // For screensaver, add a "Thin Space" (\u2009) at the end of the AM/PM to prevent
                // its display from being cut off on some devices when in italic.
                // A "Thin Space" (\u2009) is also added at the beginning to correctly center the date,
                // alarm icon and next alarm.
                pattern = "\u2009" + pattern.replaceAll("a", "a" + "\u2009");
            }
        }

        // Build a spannable so that the am/pm will be formatted
        int amPmPos = pattern.indexOf('a');
        if (amPmPos == -1) {
            return pattern;
        }

        final Spannable sp = new SpannableString(pattern);
        sp.setSpan(new RelativeSizeSpan(amPmRatio), amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new StyleSpan(Typeface.NORMAL), amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        TypefaceSpan defaultSpan = new TypefaceSpan("sans-serif-bold");

        if (isAlarm) {
            applyTypefaceSpan(sp, amPmPos, ThemeUtils.loadFont(SettingsDAO.getAlarmFont(prefs)), defaultSpan);
        } else if (isClockTab) {
            applyTypefaceSpan(sp, amPmPos, ThemeUtils.loadFont(SettingsDAO.getDigitalClockFont(prefs)), defaultSpan);
        } else if (isScreensaver) {
            Typeface baseTypeface = ThemeUtils.loadFont(SettingsDAO.getScreensaverDigitalClockFont(prefs));
            boolean isItalic = SettingsDAO.isScreensaverDigitalClockInItalic(prefs);
            int style = isItalic ? Typeface.BOLD_ITALIC : Typeface.BOLD;

            if (baseTypeface == null) {
                baseTypeface = Typeface.create("sans-serif", style);
            }

            Typeface styledTypeface = Typeface.create(baseTypeface, style);

            if (SdkUtils.isAtLeastAndroid9()) {
                sp.setSpan(new TypefaceSpan(styledTypeface), amPmPos, amPmPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                sp.setSpan(new CustomTypefaceSpan(styledTypeface), amPmPos, amPmPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            sp.setSpan(defaultSpan, amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return sp;
    }

    /**
     * Applies the appropriate typeface span to the AM/PM marker.<br>
     * If the provided typeface is null, a default span is used. Otherwise, a bold version
     * of the typeface is applied, with compatibility handling for older Android versions.
     */
    private static void applyTypefaceSpan(Spannable sp, int amPmPos, Typeface userTypeface, TypefaceSpan defaultSpan) {
        if (userTypeface == null) {
            sp.setSpan(defaultSpan, amPmPos, amPmPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            Typeface boldTypeface = Typeface.create(userTypeface, Typeface.BOLD);
            if (SdkUtils.isAtLeastAndroid9()) {
                sp.setSpan(new TypefaceSpan(boldTypeface), amPmPos, amPmPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                sp.setSpan(new CustomTypefaceSpan(boldTypeface), amPmPos, amPmPos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static CharSequence get24ModeFormat(Context context, boolean includeSeconds, boolean isScreensaver) {
        if (isScreensaver && SettingsDAO.isScreensaverDigitalClockInItalic(getDefaultSharedPreferences(context))) {
            // For screensaver, add a "Thin Space" (\u2009) at the end of the time to prevent
            // its display from being cut off on some devices when in italic.
            // A "Thin Space" (\u2009) is also added at the beginning to correctly center the date,
            // alarm icon and next alarm.
            return "\u2009"
                    + DateFormat.getBestDateTimePattern(Locale.getDefault(), includeSeconds ? "Hms" : "Hm")
                    + "\u2009";
        } else {
            return DateFormat.getBestDateTimePattern(Locale.getDefault(), includeSeconds ? "Hms" : "Hm");
        }
    }

    /**
     * Clock views can call this to refresh their date.
     **/
    public static void updateDate(String dateSkeleton, String descriptionSkeleton, View clock) {
        final TextView dateDisplay = clock.findViewById(R.id.date);

        if (dateDisplay == null) {
            return;
        }

        final Locale locale = Locale.getDefault();
        String datePattern = DateFormat.getBestDateTimePattern(locale, dateSkeleton);
        final String descriptionPattern = DateFormat.getBestDateTimePattern(locale, descriptionSkeleton);

        final Date now = new Date();
        dateDisplay.setText(new SimpleDateFormat(datePattern, locale).format(now));
        dateDisplay.setVisibility(View.VISIBLE);
        dateDisplay.setContentDescription(new SimpleDateFormat(descriptionPattern, locale).format(now));
    }

    /**
     * Applies a bold font to the date.
     */
    public static void applyBoldDateTypeface(View clock) {
        SharedPreferences prefs = getDefaultSharedPreferences(clock.getContext());
        final TextView date = clock.findViewById(R.id.date);

        if (date == null) {
            return;
        }

        date.setTypeface(ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(prefs)));
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
    public static Date getNextDay(Date time, Collection<TimeZone> zones) {
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
    public static void setClockIconTypeface(View clock) {
        final TextView nextAlarmIconView = clock.findViewById(R.id.nextAlarmIcon);
        nextAlarmIconView.setTypeface(getAlarmIconTypeface(clock.getContext()));
    }

    /**
     * To display the alarm clock in this font, use the character {@link R.string#clock_emoji}.
     *
     * @return a special font containing a glyph that draws an alarm clock
     */
    public static Typeface getAlarmIconTypeface(Context context) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/clock.ttf");
    }
}
