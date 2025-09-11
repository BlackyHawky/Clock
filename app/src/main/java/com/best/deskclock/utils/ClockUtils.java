// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmActivity;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.screensaver.Screensaver;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.settings.AlarmDisplayPreviewActivity;
import com.best.deskclock.uicomponents.AnalogClock;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ClockUtils {

    /**
     * Configure the clock that is visible to display seconds. The clock that is not visible never
     * displays seconds to avoid it scheduling unnecessary ticking runnable.
     */
    public static void setClockSecondsEnabled(DataModel.ClockStyle clockStyle, TextClock digitalClock,
                                              AnalogClock analogClock, boolean displaySeconds) {

        switch (clockStyle) {
            case ANALOG, ANALOG_MATERIAL -> {
                setTimeFormat(digitalClock, false);
                analogClock.enableSeconds(displaySeconds);
                return;
            }
            case DIGITAL -> {
                analogClock.enableSeconds(false);
                setTimeFormat(digitalClock, displaySeconds);
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
                final Context context = analogClock.getContext();
                int screenHeight = context.getResources().getDisplayMetrics().heightPixels;

                // Optimally adjust the height and the width of the analog clock when displayed
                // on a tablet or phone in portrait or landscape mode
                if (context instanceof AlarmActivity || context instanceof AlarmDisplayPreviewActivity) {
                    if (ThemeUtils.isTablet()) {
                        analogClock.getLayoutParams().height = ThemeUtils.isLandscape()
                                ? screenHeight / 2 : screenHeight / 4;
                        analogClock.getLayoutParams().width = ThemeUtils.isLandscape()
                                ? screenHeight / 2 : screenHeight / 4;
                    } else {
                        analogClock.getLayoutParams().height = ThemeUtils.isLandscape()
                                ? (int) (screenHeight / 1.6) : (int) (screenHeight / 3.2);
                        analogClock.getLayoutParams().width = ThemeUtils.isLandscape()
                                ? (int) (screenHeight / 1.6) : (int) (screenHeight / 3.2);
                    }
                } else {
                    analogClock.getLayoutParams().height = ThemeUtils.isLandscape()
                            ? (int) (screenHeight / 2.6) : (int) (screenHeight / 3.8);
                    analogClock.getLayoutParams().width = ThemeUtils.isLandscape()
                            ? (int) (screenHeight / 2.6) : (int) (screenHeight / 3.8);
                }

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
     * Formats the time in the TextClock according to the Locale with a special
     * formatting treatment for the am/pm label.
     *
     * @param clock          TextClock to format
     * @param includeSeconds whether or not to include seconds in the clock's time
     */
    public static void setTimeFormat(TextClock clock, boolean includeSeconds) {
        if (clock != null) {
            // Get the best format for 12 hours mode according to the locale
            clock.setFormat12Hour(get12ModeFormat(clock.getContext(), 0.4f, includeSeconds));
            // Get the best format for 24 hours mode according to the locale
            clock.setFormat24Hour(get24ModeFormat(clock.getContext(), includeSeconds));
        }
    }

    /**
     * @param amPmRatio      a value between 0 and 1 that is the ratio of the relative size of the
     *                       am/pm string to the time string
     * @param includeSeconds whether or not to include seconds in the time string
     * @return format string for 12 hours mode time, not including seconds
     */
    public static CharSequence get12ModeFormat(Context context, float amPmRatio, boolean includeSeconds) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                includeSeconds ? "hmsa" : "hma");

        // Replace spaces with "Hair Space"
        pattern = pattern.replaceAll("\\s", "\u200A");

        if (amPmRatio <= 0) {
            pattern = pattern.replaceAll("\u200Aa", "").trim();
        } else {
            if (context instanceof ScreensaverActivity || context instanceof Screensaver) {
                if (SettingsDAO.isScreensaverDigitalClockInItalic(getDefaultSharedPreferences(context))) {
                    // For screensaver, add a "Hair Space" (\u200A) at the end of the AM/PM to prevent
                    // its display from being cut off on some devices when in italic.
                    pattern = pattern.replaceAll("a", "a" + "\u200A");
                }
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
        sp.setSpan(new TypefaceSpan("sans-serif-bold"), amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return sp;
    }

    public static CharSequence get24ModeFormat(Context context, boolean includeSeconds) {
        if (context instanceof ScreensaverActivity || context instanceof Screensaver) {
            if (SettingsDAO.isScreensaverDigitalClockInItalic(getDefaultSharedPreferences(context))) {
                // For screensaver, add a "Thin Space" (\u2009) at the end of the time to prevent
                // its display from being cut off on some devices when in italic.
                return DateFormat.getBestDateTimePattern(Locale.getDefault(), includeSeconds ? "Hms" : "Hm") + "\u2009";
            } else {
                return DateFormat.getBestDateTimePattern(Locale.getDefault(), includeSeconds ? "Hms" : "Hm");
            }
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

        final Locale l = Locale.getDefault();
        String datePattern = DateFormat.getBestDateTimePattern(l, dateSkeleton);
        if (dateDisplay.getContext() instanceof ScreensaverActivity || dateDisplay.getContext() instanceof Screensaver) {
            final SharedPreferences prefs = getDefaultSharedPreferences(clock.getContext());
            // Add a "Thin Space" (\u2009) at the end of the date to prevent its display from being cut off on some devices.
            // (The display of the date is only cut off at the end if it is defined in italics in the screensaver settings).
            if (SettingsDAO.isScreensaverDateInItalic(prefs)) {
                datePattern = "\u2009" + DateFormat.getBestDateTimePattern(l, dateSkeleton) + "\u2009";
            } else if (SettingsDAO.isScreensaverNextAlarmInItalic(prefs)) {
                datePattern = "\u2009" + DateFormat.getBestDateTimePattern(l, dateSkeleton);
            }
        }

        final String descriptionPattern = DateFormat.getBestDateTimePattern(l, descriptionSkeleton);
        final Date now = new Date();
        dateDisplay.setText(new SimpleDateFormat(datePattern, l).format(now));
        dateDisplay.setVisibility(View.VISIBLE);
        dateDisplay.setContentDescription(new SimpleDateFormat(descriptionPattern, l).format(now));
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
