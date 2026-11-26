/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.best.deskclock.data.CombinedDays;
import com.best.deskclock.data.Weekdays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class AlarmSchedulerTest {

    private static final TimeZone BERLIN = TimeZone.getTimeZone("Europe/Berlin");
    private static final Weekdays WEEKDAYS =
        Weekdays.fromCalendarDays(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY);
    private static final Weekdays ALL_DAYS =
        Weekdays.fromCalendarDays(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY);

    @Test
    public void repeatingSkipsDeselectedAndDismissedDates() {
        CombinedDays overrides = new CombinedDays()
            .addDeselectedDate(2026, Calendar.SEPTEMBER, 21)
            .addDismissedDate(2026, Calendar.SEPTEMBER, 22);
        Alarm alarm = alarm(WEEKDAYS, overrides);

        Calendar next = alarm.getNextAlarmTime(at(2026, Calendar.SEPTEMBER, 18, 7, 0));

        assertDateTime(next, 2026, Calendar.SEPTEMBER, 23, 7, 0);
    }

    @Test
    public void repeatingChoosesEarlierOfWeekdayAndAddedDate() {
        CombinedDays overrides = new CombinedDays()
            .addSelectedDate(2026, Calendar.SEPTEMBER, 19);
        Alarm alarm = alarm(WEEKDAYS, overrides);

        Calendar next = alarm.getNextAlarmTime(at(2026, Calendar.SEPTEMBER, 18, 20, 0));

        assertDateTime(next, 2026, Calendar.SEPTEMBER, 19, 7, 0);
    }

    @Test
    public void repeatingKeepsWeekdayWhenAddedDateIsLater() {
        CombinedDays overrides = new CombinedDays()
            .addSelectedDate(2026, Calendar.SEPTEMBER, 23);
        Alarm alarm = alarm(WEEKDAYS, overrides);

        Calendar next = alarm.getNextAlarmTime(at(2026, Calendar.SEPTEMBER, 18, 20, 0));

        assertDateTime(next, 2026, Calendar.SEPTEMBER, 21, 7, 0);
    }

    @Test
    public void dateOnlyAlarmSelectsTheNextUnexpiredDate() {
        CombinedDays dates = new CombinedDays()
            .addSelectedDate(2026, Calendar.SEPTEMBER, 19)
            .addSelectedDate(2026, Calendar.SEPTEMBER, 21)
            .addSelectedDate(2030, Calendar.JANUARY, 1);
        Alarm alarm = alarm(Weekdays.NONE, dates);

        Calendar next = alarm.getNextAlarmTime(at(2026, Calendar.SEPTEMBER, 19, 8, 0));

        assertDateTime(next, 2026, Calendar.SEPTEMBER, 21, 7, 0);
        assertTrueFutureSelectedDate(alarm, at(2026, Calendar.SEPTEMBER, 21, 8, 0));
    }

    @Test
    public void dateOnlyAlarmSupportsDatesBeyondOneYear() {
        CombinedDays dates = new CombinedDays().addSelectedDate(2030, Calendar.JANUARY, 1);
        Alarm alarm = alarm(Weekdays.NONE, dates);

        Calendar next = alarm.getNextAlarmTime(at(2026, Calendar.SEPTEMBER, 18, 20, 0));

        assertDateTime(next, 2030, Calendar.JANUARY, 1, 7, 0);
    }

    @Test
    public void legacyOneTimeAlarmRollsToTomorrowAfterItsAlarmTimePasses() {
        Alarm alarm = alarm(Weekdays.NONE, new CombinedDays());
        alarm.year = 2026;
        alarm.month = Calendar.SEPTEMBER;
        alarm.day = 18;

        Calendar next = alarm.getNextAlarmTime(at(2026, Calendar.SEPTEMBER, 18, 7, 30));

        assertDateTime(next, 2026, Calendar.SEPTEMBER, 19, 7, 0);
    }

    @Test
    public void repeatingAlarmPreservesWallClockTimeAcrossDst() {
        Alarm alarm = alarm(ALL_DAYS, new CombinedDays());

        Calendar next = alarm.getNextAlarmTime(at(2026, Calendar.MARCH, 28, 20, 0));

        assertDateTime(next, 2026, Calendar.MARCH, 29, 7, 0);
        assertEquals(BERLIN.getOffset(next.getTimeInMillis()), next.get(Calendar.ZONE_OFFSET)
            + next.get(Calendar.DST_OFFSET));
    }

    @Test
    public void repeatingAlarmDefersOccurrencesInsidePauseWindow() {
        Alarm alarm = alarm(ALL_DAYS, new CombinedDays());
        alarm.pauseStartDate = utcMidnight(2026, Calendar.JUNE, 2);
        alarm.pauseEndDate = utcMidnight(2026, Calendar.JUNE, 5);

        Calendar next = alarm.getNextAlarmTime(at(2026, Calendar.JUNE, 1, 8, 0));

        assertDateTime(next, 2026, Calendar.JUNE, 6, 7, 0);
    }

    @Test
    public void previousOccurrenceIncludesTodayIfItsAlarmTimeHasPassed() {
        Alarm alarm = alarm(ALL_DAYS, new CombinedDays());

        Calendar previous = alarm.getPreviousAlarmTime(at(2026, Calendar.JUNE, 10, 7, 30));

        assertNotNull(previous);
        assertDateTime(previous, 2026, Calendar.JUNE, 10, 7, 0);
    }

    @Test
    public void previousOccurrenceStepsBackWhenTodayAlarmTimeIsStillAhead() {
        Alarm alarm = alarm(ALL_DAYS, new CombinedDays());

        Calendar previous = alarm.getPreviousAlarmTime(at(2026, Calendar.JUNE, 10, 6, 0));

        assertNotNull(previous);
        assertDateTime(previous, 2026, Calendar.JUNE, 9, 7, 0);
    }

    @Test
    public void previousOccurrenceComparesWeekdayAndAddedDates() {
        CombinedDays dates = new CombinedDays().addSelectedDate(2026, Calendar.JUNE, 10);
        Alarm alarm = alarm(WEEKDAYS, dates);

        Calendar previous = alarm.getPreviousAlarmTime(at(2026, Calendar.JUNE, 11, 6, 30));

        assertNotNull(previous);
        assertDateTime(previous, 2026, Calendar.JUNE, 10, 7, 0);
    }

    @Test
    public void dateOnlyAlarmHasNoPreviousOccurrenceBeforeItsFirstDate() {
        CombinedDays dates = new CombinedDays().addSelectedDate(2026, Calendar.JUNE, 15);
        Alarm alarm = alarm(Weekdays.NONE, dates);

        assertNull(alarm.getPreviousAlarmTime(at(2026, Calendar.JUNE, 10, 8, 0)));
    }

    private static Alarm alarm(Weekdays weekdays, CombinedDays combinedDays) {
        return alarm(weekdays, combinedDays, 7, 0);
    }

    private static Alarm alarm(Weekdays weekdays, CombinedDays combinedDays, int hour, int minute) {
        Alarm alarm = new Alarm(1L, true, 2026, Calendar.JANUARY, 1, hour, minute, true,
            "default", true, weekdays, "test", false, "content://ringtone", false, 10,
            5, 1, 0, 5, 0, 0, 0, "", 0, "off", false, "Europe/Berlin",
            new long[]{Alarm.DEFAULT_CATEGORY_ID}, "", "", false, combinedDays);
        return alarm;
    }

    private static Calendar at(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(BERLIN);
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar;
    }

    private static long utcMidnight(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month, day);
        return calendar.getTimeInMillis();
    }

    private static void assertDateTime(Calendar actual, int year, int month, int day, int hour, int minute) {
        assertNotNull(actual);
        assertEquals(year, actual.get(Calendar.YEAR));
        assertEquals(month, actual.get(Calendar.MONTH));
        assertEquals(day, actual.get(Calendar.DAY_OF_MONTH));
        assertEquals(hour, actual.get(Calendar.HOUR_OF_DAY));
        assertEquals(minute, actual.get(Calendar.MINUTE));
    }

    private static void assertTrueFutureSelectedDate(Alarm alarm, Calendar after) {
        org.junit.Assert.assertTrue(alarm.hasFutureSelectedDate(after));
    }
}
