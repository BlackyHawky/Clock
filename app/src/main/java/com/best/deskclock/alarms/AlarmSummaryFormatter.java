// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.R;
import com.best.deskclock.data.CombinedDays;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.text.DateFormatSymbols;
import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

/**
 * Builds the single-line schedule summary shown below an alarm's time.
 */
final class AlarmSummaryFormatter {

    private static final String SEGMENT_SEPARATOR = " | ";
    private static final int[] WEEKDAY_NAME_LENGTHS = {0, 3, 2};

    private AlarmSummaryFormatter() {}

    @NonNull
    static CharSequence formatSummary(
        @NonNull Context context,
        @NonNull Weekdays weekdays,
        @NonNull Weekdays.Order order,
        @NonNull CombinedDays combinedDays,
        @Nullable Calendar nextAlarm,
        @NonNull Calendar now,
        @NonNull Locale locale,
        int nextAlarmDay,
        boolean highlightNextWeekday,
        int availableWidth,
        @NonNull TextPaint paint) {
        return formatSummary(context, weekdays, null, order, combinedDays, nextAlarm, now, locale,
            nextAlarmDay, highlightNextWeekday, availableWidth, paint);
    }

    @NonNull
    static CharSequence formatSummary(
        @NonNull Context context,
        @NonNull Weekdays weekdays,
        @Nullable CharSequence scheduleLabel,
        @NonNull Weekdays.Order order,
        @NonNull CombinedDays combinedDays,
        @Nullable Calendar nextAlarm,
        @NonNull Calendar now,
        @NonNull Locale locale,
        int nextAlarmDay,
        boolean highlightNextWeekday,
        int availableWidth,
        @NonNull TextPaint paint) {

        CharSequence fallback = "";
        for (int weekdayNameLength : WEEKDAY_NAME_LENGTHS) {
            for (DateStyle dateStyle : getDateStyles(nextAlarm, now)) {
                CharSequence candidate = buildSummary(context, weekdays, scheduleLabel, order,
                    combinedDays, nextAlarm, now, locale, weekdayNameLength, dateStyle,
                    nextAlarmDay, highlightNextWeekday);
                fallback = candidate;
                if (availableWidth > 0
                    && Layout.getDesiredWidth(candidate, paint) <= availableWidth) {
                    return candidate;
                }
            }
        }
        return fallback;
    }

    @NonNull
    static String formatWeekdays(
        @NonNull Context context,
        @NonNull Weekdays weekdays,
        @NonNull Weekdays.Order order,
        @NonNull Locale locale,
        int maximumNameLength) {
        final String[] names = maximumNameLength == 0
            ? new DateFormatSymbols(locale).getWeekdays()
            : new DateFormatSymbols(locale).getShortWeekdays();
        final String separator = context.getString(R.string.day_concat);
        final StringBuilder result = new StringBuilder();

        for (int calendarDay : order.getCalendarDays()) {
            if (!weekdays.isBitOn(calendarDay)) {
                continue;
            }

            if (result.length() > 0) {
                result.append(separator);
            }
            String name = FormattedTextUtils.capitalizeFirstLetter(names[calendarDay], locale);
            if (maximumNameLength > 0) {
                name = shorten(name, maximumNameLength, locale);
            }
            result.append(name);
        }

        return result.toString();
    }

    @NonNull
    static String formatOverrideCounts(@NonNull Context context, int selected, int excluded) {
        if (selected > 0 && excluded > 0) {
            return context.getString(R.string.dates_summary_added_excluded, selected, excluded);
        }
        if (selected > 0) {
            return context.getString(R.string.dates_summary_added, selected);
        }
        if (excluded > 0) {
            return context.getString(R.string.dates_summary_excluded, excluded);
        }
        return "";
    }

    @NonNull
    static String formatNextDate(
        @NonNull Context context,
        @NonNull Calendar nextAlarm,
        @NonNull Calendar now,
        @NonNull Locale locale,
        int weekdayNameLength,
        boolean abbreviateRelative,
        boolean includeYear) {
        final Calendar today = atSameZone(now, nextAlarm);
        if (sameDate(nextAlarm, today)) {
            return abbreviateRelative
                ? abbreviatedRelativeLabel(context, nextAlarm, R.string.alarm_today)
                : context.getString(R.string.alarm_today);
        }

        final Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        if (sameDate(nextAlarm, tomorrow)) {
            return abbreviateRelative
                ? abbreviatedRelativeLabel(context, nextAlarm, R.string.alarm_tomorrow)
                : context.getString(R.string.alarm_tomorrow);
        }

        final String skeleton = context.getString(includeYear
            ? R.string.abbrev_wday_month_day_with_year
            : R.string.abbrev_wday_month_day_no_year);
        final String pattern = DateFormat.getBestDateTimePattern(locale, skeleton);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, locale);
        dateFormat.setTimeZone(nextAlarm.getTimeZone());
        String date = dateFormat.format(nextAlarm.getTime());
        date = FormattedTextUtils.capitalizeFirstLetter(date, locale);

        if (weekdayNameLength == 2) {
            final String weekdayPattern = DateFormat.getBestDateTimePattern(locale, "EEE");
            final SimpleDateFormat weekdayFormat = new SimpleDateFormat(weekdayPattern, locale);
            weekdayFormat.setTimeZone(nextAlarm.getTimeZone());
            final String weekday = weekdayFormat.format(nextAlarm.getTime());
            final int weekdayStart = date.indexOf(weekday);
            if (weekdayStart >= 0) {
                date = date.substring(0, weekdayStart)
                    + shorten(weekday, 2, locale)
                    + date.substring(weekdayStart + weekday.length());
            }
        }
        return date;
    }

    private static CharSequence buildSummary(
        Context context,
        Weekdays weekdays,
        @Nullable CharSequence scheduleLabel,
        Weekdays.Order order,
        CombinedDays combinedDays,
        @Nullable Calendar nextAlarm,
        Calendar now,
        Locale locale,
        int weekdayNameLength,
        DateStyle dateStyle,
        int nextAlarmDay,
        boolean highlightNextWeekday) {

        final SpannableStringBuilder result = new SpannableStringBuilder();
        final CharSequence weekdayText = scheduleLabel == null
            ? formatWeekdays(context, weekdays, order, locale, weekdayNameLength)
            : scheduleLabel;
        if (weekdayText.length() > 0) {
            appendSegment(result, weekdayText);
            if (scheduleLabel == null && highlightNextWeekday && nextAlarmDay > 0) {
                final String nextWeekday = weekdayName(context, nextAlarmDay, locale, weekdayNameLength);
                final int start = TextUtils.indexOf(result, nextWeekday);
                if (start >= 0) {
                    final int color = MaterialColors.getColor(
                        context, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
                    result.setSpan(new ForegroundColorSpan(color), start, start + nextWeekday.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    result.setSpan(new StyleSpan(Typeface.BOLD), start, start + nextWeekday.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        final String overrideCounts = formatOverrideCounts(context,
            combinedDays.getSelectedDateCount(), combinedDays.getDeselectedDateCount());
        if (!overrideCounts.isEmpty()) {
            final int start = appendSegment(result, overrideCounts);
            result.setSpan(new StyleSpan(Typeface.BOLD), start, result.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (nextAlarm != null) {
            final int start = appendSegment(result, formatNextDate(context, nextAlarm, now, locale,
                dateStyle.weekdayNameLength, dateStyle.abbreviateRelative, dateStyle.includeYear));
            final int accentColor = MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorTertiary, Color.BLACK);
            result.setSpan(new ForegroundColorSpan(accentColor), start, result.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            result.setSpan(new StyleSpan(Typeface.BOLD), start, result.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return result;
    }

    private static int appendSegment(SpannableStringBuilder result, CharSequence segment) {
        if (result.length() > 0) {
            result.append(SEGMENT_SEPARATOR);
        }
        int start = result.length();
        result.append(segment);
        return start;
    }

    private static List<DateStyle> getDateStyles(@Nullable Calendar nextAlarm, Calendar now) {
        final List<DateStyle> styles = new ArrayList<>();
        if (nextAlarm == null) {
            styles.add(DateStyle.NONE);
            return styles;
        }

        final Calendar today = atSameZone(now, nextAlarm);
        final Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        if (sameDate(nextAlarm, today) || sameDate(nextAlarm, tomorrow)) {
            styles.add(new DateStyle(3, false, false));
            styles.add(new DateStyle(3, true, false));
            return styles;
        }

        final boolean differentYear = nextAlarm.get(Calendar.YEAR) != today.get(Calendar.YEAR);
        if (differentYear) {
            styles.add(new DateStyle(3, false, true));
            styles.add(new DateStyle(2, false, true));
            styles.add(new DateStyle(3, false, false));
            styles.add(new DateStyle(2, false, false));
        } else {
            styles.add(new DateStyle(3, false, false));
            styles.add(new DateStyle(2, false, false));
        }
        return styles;
    }

    @NonNull
    private static String weekdayName(
        Context context, int calendarDay, Locale locale, int maximumNameLength) {
        final String[] names = maximumNameLength == 0
            ? new DateFormatSymbols(locale).getWeekdays()
            : new DateFormatSymbols(locale).getShortWeekdays();
        String name = FormattedTextUtils.capitalizeFirstLetter(names[calendarDay], locale);
        return maximumNameLength > 0 ? shorten(name, maximumNameLength, locale) : name;
    }

    @NonNull
    private static String abbreviatedRelativeLabel(Context context, Calendar date, int fullLabelResId) {
        final String fullLabel = context.getString(fullLabelResId);
        final Formatter formatter = new Formatter(new StringBuilder(), Utils.getLocaleFromContext(context));
        DateUtils.formatDateRange(context, formatter, date.getTimeInMillis(), date.getTimeInMillis(),
            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_ABBREV_RELATIVE,
            date.getTimeZone().getID());
        final String abbreviated = formatter.toString();
        if (abbreviated != null
            && abbreviated.codePointCount(0, abbreviated.length())
                < fullLabel.codePointCount(0, fullLabel.length())) {
            return abbreviated;
        }

        if (fullLabelResId == R.string.alarm_today
            && "Today".equalsIgnoreCase(fullLabel)) {
            return "Tdy";
        }
        if (fullLabelResId == R.string.alarm_tomorrow
            && "Tomorrow".equalsIgnoreCase(fullLabel)) {
            return "Tmr";
        }
        return shorten(fullLabel, 3, Utils.getLocaleFromContext(context));
    }

    private static Calendar atSameZone(Calendar calendar, Calendar target) {
        final Calendar copy = (Calendar) calendar.clone();
        copy.setTimeZone(target.getTimeZone());
        return copy;
    }

    private static boolean sameDate(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
            && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    @NonNull
    private static String shorten(@NonNull String value, int maximumLength, @NonNull Locale locale) {
        if (maximumLength <= 0) {
            return value;
        }
        final BreakIterator iterator = BreakIterator.getCharacterInstance(locale);
        iterator.setText(value);
        int end = 0;
        for (int i = 0; i < maximumLength; i++) {
            end = iterator.next();
            if (end == BreakIterator.DONE) {
                return value;
            }
        }
        if (iterator.next() == BreakIterator.DONE) {
            return value;
        }
        return value.substring(0, end);
    }

    private record DateStyle(int weekdayNameLength, boolean abbreviateRelative, boolean includeYear) {
        private static final DateStyle NONE = new DateStyle(3, false, false);
    }
}
