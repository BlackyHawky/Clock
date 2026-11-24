// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import static java.util.Calendar.FRIDAY;
import static java.util.Calendar.MONDAY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.best.deskclock.data.CombinedDays;
import com.best.deskclock.data.Weekdays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class AlarmSummaryFormatterTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
    }

    @Test
    public void weekdaysUseFullThreeAndTwoLetterNames() {
        Weekdays weekdays = Weekdays.fromCalendarDays(MONDAY, FRIDAY);

        assertEquals("Monday, Friday", AlarmSummaryFormatter.formatWeekdays(
            mContext, weekdays, Weekdays.Order.MON_TO_SUN, Locale.US, 0));
        assertEquals("Mon, Fri", AlarmSummaryFormatter.formatWeekdays(
            mContext, weekdays, Weekdays.Order.MON_TO_SUN, Locale.US, 3));
        assertEquals("Mo, Fr", AlarmSummaryFormatter.formatWeekdays(
            mContext, weekdays, Weekdays.Order.MON_TO_SUN, Locale.US, 2));
    }

    @Test
    public void overrideSummaryUsesCompactAddedAndExcludedCounts() {
        assertEquals("+2 -3", AlarmSummaryFormatter.formatOverrideCounts(mContext, 2, 3));
        assertEquals("+2", AlarmSummaryFormatter.formatOverrideCounts(mContext, 2, 0));
        assertEquals("-3", AlarmSummaryFormatter.formatOverrideCounts(mContext, 0, 3));
        assertEquals("", AlarmSummaryFormatter.formatOverrideCounts(mContext, 0, 0));
    }

    @Test
    public void summaryAlwaysIncludesTheNextDateForRepeatingAlarms() {
        Calendar now = utcDate(2026, Calendar.NOVEMBER, 24);
        Calendar nextAlarm = utcDate(2026, Calendar.NOVEMBER, 27);
        TextPaint paint = new TextPaint();
        paint.setTextSize(14);

        CharSequence summary = AlarmSummaryFormatter.formatSummary(
            mContext,
            Weekdays.fromCalendarDays(MONDAY),
            Weekdays.Order.MON_TO_SUN,
            new CombinedDays(),
            nextAlarm,
            now,
            Locale.US,
            FRIDAY,
            false,
            Integer.MAX_VALUE,
            paint);

        assertTrue(summary.toString().startsWith("Monday | "));
        assertTrue(summary.toString().contains("27"));
        assertTrue(summary.toString().contains("Nov"));
        assertFalse(summary.toString().contains("\n"));
    }

    @Test
    public void summaryPlacesCompactOverridesBetweenWeekdaysAndNextDate() {
        Calendar now = utcDate(2026, Calendar.NOVEMBER, 24);
        Calendar nextAlarm = utcDate(2026, Calendar.NOVEMBER, 27);
        CombinedDays overrides = new CombinedDays()
            .addSelectedDate(2026, Calendar.NOVEMBER, 28)
            .addSelectedDate(2026, Calendar.NOVEMBER, 29)
            .addDeselectedDate(2026, Calendar.NOVEMBER, 30);
        TextPaint paint = new TextPaint();
        paint.setTextSize(14);

        CharSequence summary = AlarmSummaryFormatter.formatSummary(
            mContext,
            Weekdays.fromCalendarDays(MONDAY),
            Weekdays.Order.MON_TO_SUN,
            overrides,
            nextAlarm,
            now,
            Locale.US,
            FRIDAY,
            false,
            Integer.MAX_VALUE,
            paint);

        assertEquals("Monday | +2 -1 | Fri, Nov 27", summary.toString());

        Spanned styledSummary = (Spanned) summary;
        assertTrue(styledSummary.getSpans(0, summary.length(), StyleSpan.class).length >= 2);
        assertTrue(styledSummary.getSpans(0, summary.length(), ForegroundColorSpan.class).length >= 1);
    }

    @Test
    public void summaryUsesCompactWeekdaysAndRelativeDateWhenSpaceIsTight() {
        Calendar now = utcDate(2026, Calendar.NOVEMBER, 24);
        Calendar tomorrow = (Calendar) now.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        CombinedDays overrides = new CombinedDays()
            .addSelectedDate(2026, Calendar.NOVEMBER, 28)
            .addDeselectedDate(2026, Calendar.NOVEMBER, 29);
        TextPaint paint = new TextPaint();
        paint.setTextSize(14);

        CharSequence summary = AlarmSummaryFormatter.formatSummary(
            mContext,
            Weekdays.fromCalendarDays(MONDAY),
            Weekdays.Order.MON_TO_SUN,
            overrides,
            tomorrow,
            now,
            Locale.US,
            Calendar.WEDNESDAY,
            false,
            1,
            paint);

        assertEquals("Mo | +1 -1 | Tmr", summary.toString());
    }

    @Test
    public void todayAndTomorrowLabelsAreUsedOnlyForMatchingDates() {
        Calendar now = utcDate(2026, Calendar.NOVEMBER, 24);

        assertEquals("Today", AlarmSummaryFormatter.formatNextDate(
            mContext, now, now, Locale.US, 3, false, true));

        Calendar tomorrow = (Calendar) now.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        assertEquals("Tomorrow", AlarmSummaryFormatter.formatNextDate(
            mContext, tomorrow, now, Locale.US, 3, false, true));

        Calendar later = (Calendar) now.clone();
        later.add(Calendar.DAY_OF_MONTH, 3);
        String laterLabel = AlarmSummaryFormatter.formatNextDate(
            mContext, later, now, Locale.US, 3, false, true);
        assertFalse(laterLabel.equals("Today"));
        assertFalse(laterLabel.equals("Tomorrow"));
        assertTrue(laterLabel.contains("27"));
        assertTrue(laterLabel.contains("Nov"));
    }

    @Test
    public void relativeLabelsUseTheAlarmTimeZone() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
        now.clear();
        now.set(2026, Calendar.NOVEMBER, 24, 23, 30, 0);
        Calendar nextAlarm = Calendar.getInstance(UTC, Locale.US);
        nextAlarm.setTimeInMillis(now.getTimeInMillis());

        assertEquals("Today", AlarmSummaryFormatter.formatNextDate(
            mContext, nextAlarm, now, Locale.US, 3, false, true));
    }

    @Test
    public void datesInAnotherYearIncludeTheYearWhenThereIsRoom() {
        Calendar now = utcDate(2026, Calendar.DECEMBER, 31);
        Calendar nextYear = utcDate(2027, Calendar.JANUARY, 2);

        String formatted = AlarmSummaryFormatter.formatNextDate(
            mContext, nextYear, now, Locale.US, 3, false, true);

        assertTrue(formatted.contains("2027"));
    }

    @Test
    public void formattedDateUsesTheAlarmTimeZone() {
        Calendar now = utcDate(2026, Calendar.NOVEMBER, 25);
        Calendar nextAlarm = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"), Locale.US);
        nextAlarm.clear();
        nextAlarm.set(2026, Calendar.NOVEMBER, 27, 0, 30, 0);

        String formatted = AlarmSummaryFormatter.formatNextDate(
            mContext, nextAlarm, now, Locale.US, 3, false, false);

        assertTrue(formatted.contains("Fri"));
        assertTrue(formatted.contains("27"));
    }

    private static Calendar utcDate(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance(UTC, Locale.US);
        calendar.clear();
        calendar.set(year, month, day, 8, 0, 0);
        return calendar;
    }
}
