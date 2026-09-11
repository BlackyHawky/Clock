// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static java.util.Calendar.FRIDAY;
import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;
import static java.util.Calendar.THURSDAY;
import static java.util.Calendar.TUESDAY;
import static java.util.Calendar.WEDNESDAY;

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
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final Weekdays WEEKDAYS_MON_FRI =
        Weekdays.fromCalendarDays(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);
    private static final Weekdays WEEKDAYS_ALL =
        Weekdays.fromCalendarDays(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY);

    private static Calendar at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(BERLIN);
        c.clear();
        c.set(year, month, day, hour, minute);
        return c;
    }

    private static long utcMidnightMillis(int year, int month, int day) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(year, month, day);
        return c.getTimeInMillis();
    }

    private static Alarm buildAlarm(int hour, int minute, int year, int month, int day,
                                    Weekdays weekdays, String combinedJson) {
        return buildAlarm(hour, minute, year, month, day, weekdays, combinedJson, 0L, 0L);
    }

    private static Alarm buildAlarm(int hour, int minute, int year, int month, int day,
                                    Weekdays weekdays, String combinedJson,
                                    long pauseStart, long pauseEnd) {
        return new Alarm(1L, true, year, month, day, hour, minute, true,
            "vibrationPattern", true, weekdays, "test", false, "content://ringtone",
            false, 10, 5, 1, 0, 5, 0, pauseStart, pauseEnd, "", 0, "easy", combinedJson);
    }

    @Test
    public void repeating_SkipsDeselectedDate() {
        CombinedDays combined = new CombinedDays().addDeselectedDate(2026, 8, 15); // Tue
        Alarm alarm = buildAlarm(7, 0, 2026, 8, 14, WEEKDAYS_MON_FRI, combined.toJson());
        Calendar now = at(2026, 8, 14, 7, 0);

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        assertEquals(2026, next.get(Calendar.YEAR));
        assertEquals(8, next.get(Calendar.MONTH));
        assertEquals(16, next.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, next.get(Calendar.MINUTE));
    }

    @Test
    public void repeating_SkipsDismissedDate() {
        CombinedDays combined = new CombinedDays().addDismissedDate(2026, 8, 15);
        Alarm alarm = buildAlarm(7, 0, 2026, 8, 14, WEEKDAYS_MON_FRI, combined.toJson());
        Calendar now = at(2026, 8, 14, 7, 0);

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        assertEquals(16, next.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void repeating_PrefersEarlierAddedDate() {
        CombinedDays combined = new CombinedDays().addSelectedDate(2026, 7, 15); // Sat Aug 15
        Alarm alarm = buildAlarm(7, 0, 2026, 7, 10, WEEKDAYS_MON_FRI, combined.toJson());
        Calendar now = at(2026, 7, 14, 20, 0); // Fri Aug 14

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        assertEquals(15, next.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void repeating_KeepsWeekdayWhenAddedDateIsLater() {
        CombinedDays combined = new CombinedDays().addSelectedDate(2026, 7, 26); // Wed Aug 26
        Alarm alarm = buildAlarm(7, 0, 2026, 7, 10, WEEKDAYS_MON_FRI, combined.toJson());
        Calendar now = at(2026, 7, 14, 20, 0); // Fri Aug 14

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        assertEquals(17, next.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void datesOnly_FiresOnSelectedDate() {
        CombinedDays combined = new CombinedDays().addSelectedDate(2026, 8, 15);
        Alarm alarm = buildAlarm(7, 0, 2026, 8, 14, Weekdays.NONE, combined.toJson());
        Calendar now = at(2026, 8, 14, 20, 0);

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        assertEquals(15, next.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void datesOnly_SkipsPassedDateToNextSelected() {
        CombinedDays combined = new CombinedDays()
            .addSelectedDate(2026, 8, 15)
            .addSelectedDate(2026, 8, 17);
        Alarm alarm = buildAlarm(7, 0, 2026, 8, 14, Weekdays.NONE, combined.toJson());
        Calendar now = at(2026, 8, 15, 8, 0);

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        assertEquals(17, next.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void datesOnly_SentinelWhenOnlyDateAlreadyPassed() {
        CombinedDays combined = new CombinedDays().addSelectedDate(2026, 7, 15);
        Alarm alarm = buildAlarm(7, 0, 2026, 7, 14, Weekdays.NONE, combined.toJson());
        Calendar now = at(2026, 7, 15, 8, 0);

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        assertEquals(2027, next.get(Calendar.YEAR));
        assertEquals(7, next.get(Calendar.MONTH));
        assertEquals(16, next.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void datesOnly_SentinelWhenOnlyDateIsFarOut() {
        CombinedDays combined = new CombinedDays().addSelectedDate(2028, 0, 1);
        Alarm alarm = buildAlarm(7, 0, 2026, 7, 14, Weekdays.NONE, combined.toJson());
        Calendar now = at(2026, 7, 14, 20, 0);

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        assertEquals(2027, next.get(Calendar.YEAR));
        assertEquals(7, next.get(Calendar.MONTH));
        assertEquals(15, next.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void legacyOneShot_KeepsUpstreamDayShiftBehavior() {
        Alarm alarm = buildAlarm(7, 0, 2026, 0, 5, Weekdays.NONE, "");
        Calendar now = at(2026, 8, 14, 20, 0);

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        assertEquals(2026, next.get(Calendar.YEAR));
        assertEquals(0, next.get(Calendar.MONTH));
        assertEquals(6, next.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void repeating_DefersPastPauseWindow() {
        CombinedDays combined = new CombinedDays();
        long pauseStart = utcMidnightMillis(2026, 5, 2); // Jun 2
        long pauseEnd = utcMidnightMillis(2026, 5, 5);   // Jun 5
        Alarm alarm = buildAlarm(7, 0, 2026, 0, 1, WEEKDAYS_ALL, combined.toJson(), pauseStart, pauseEnd);
        Calendar now = at(2026, 5, 1, 8, 0);

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        assertEquals(2026, next.get(Calendar.YEAR));
        assertEquals(5, next.get(Calendar.MONTH));
        assertEquals(6, next.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void repeating_DstSpringForwardKeepsWallClockTime() {
        Alarm alarm = buildAlarm(7, 0, 2026, 2, 1, WEEKDAYS_ALL, "");
        Calendar now = at(2026, 2, 28, 20, 0);

        Calendar next = alarm.getNextAlarmTime(now);
        assertNotNull(next);
        // 2026-03-29 is the Berlin spring-forward day: 02:00 -> 03:00.
        assertEquals(2026, next.get(Calendar.YEAR));
        assertEquals(2, next.get(Calendar.MONTH));
        assertEquals(29, next.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, next.get(Calendar.MINUTE));
        assertEquals(2 * 3600_000L, next.get(Calendar.ZONE_OFFSET) + next.get(Calendar.DST_OFFSET));

        // Round-tripping the instant must reproduce the same wall-clock time.
        Calendar rebound = Calendar.getInstance(BERLIN);
        rebound.setTimeInMillis(next.getTimeInMillis());
        assertEquals(7, rebound.get(Calendar.HOUR_OF_DAY));
        assertEquals(29, rebound.get(Calendar.DAY_OF_MONTH));
    }

    // ---- getPreviousAlarmTime ----

    @Test
    public void previous_Repeating_GivesPreviousWeekday() {
        Alarm alarm = buildAlarm(7, 0, 2026, 0, 1, WEEKDAYS_ALL, "");
        Calendar now = at(2026, 5, 10, 7, 30); // Wed

        Calendar previous = alarm.getPreviousAlarmTime(now);
        assertNotNull(previous);
        assertEquals(9, previous.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, previous.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void previous_Repeating_BeforeAlarmTimeStepsBack() {
        Alarm alarm = buildAlarm(7, 0, 2026, 0, 1, WEEKDAYS_ALL, "");
        Calendar now = at(2026, 5, 10, 6, 0); // Wed before alarm time

        Calendar previous = alarm.getPreviousAlarmTime(now);
        assertNotNull(previous);
        assertEquals(8, previous.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void previous_Repeating_SkipsDeselectedDate() {
        CombinedDays combined = new CombinedDays().addDeselectedDate(2026, 5, 9); // Tue
        Alarm alarm = buildAlarm(7, 0, 2026, 0, 1, WEEKDAYS_MON_FRI, combined.toJson());
        Calendar now = at(2026, 5, 10, 7, 30);

        Calendar previous = alarm.getPreviousAlarmTime(now);
        assertNotNull(previous);
        assertEquals(8, previous.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, previous.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void previous_Repeating_PrefersLaterAddedDate() {
        CombinedDays combined = new CombinedDays().addSelectedDate(2026, 5, 10); // Wed
        Alarm alarm = buildAlarm(7, 0, 2026, 0, 1, WEEKDAYS_MON_FRI, combined.toJson());
        Calendar now = at(2026, 5, 11, 7, 30);

        Calendar previous = alarm.getPreviousAlarmTime(now);
        assertNotNull(previous);
        assertEquals(10, previous.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, previous.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void previous_Repeating_IgnoresAddedDateInFuture() {
        CombinedDays combined = new CombinedDays().addSelectedDate(2026, 5, 12); // Fri (future)
        Alarm alarm = buildAlarm(7, 0, 2026, 0, 1, WEEKDAYS_MON_FRI, combined.toJson());
        Calendar now = at(2026, 5, 11, 7, 30);

        Calendar previous = alarm.getPreviousAlarmTime(now);
        assertNotNull(previous);
        assertEquals(10, previous.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void previous_DatesOnly_GivesLatestSelectedBeforeNow() {
        CombinedDays combined = new CombinedDays().addSelectedDate(2026, 5, 8);
        Alarm alarm = buildAlarm(7, 0, 2026, 0, 1, Weekdays.NONE, combined.toJson());
        Calendar now = at(2026, 5, 10, 8, 0);

        Calendar previous = alarm.getPreviousAlarmTime(now);
        assertNotNull(previous);
        assertEquals(8, previous.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, previous.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void previous_DatesOnly_ReturnsNullWhenNothingBefore() {
        CombinedDays combined = new CombinedDays().addSelectedDate(2026, 5, 15);
        Alarm alarm = buildAlarm(7, 0, 2026, 0, 1, Weekdays.NONE, combined.toJson());
        Calendar now = at(2026, 5, 10, 8, 0);

        assertNull(alarm.getPreviousAlarmTime(now));
    }

    @Test
    public void previous_LegacyOneShot_ReturnsNull() {
        Alarm alarm = buildAlarm(7, 0, 2026, 4, 5, Weekdays.NONE, "");
        Calendar now = at(2026, 5, 10, 8, 0);

        assertNull(alarm.getPreviousAlarmTime(now));
    }

    @Test
    public void previous_DismissedDateIsSkipped() {
        CombinedDays combined = new CombinedDays()
            .addSelectedDate(2026, 5, 8)
            .addSelectedDate(2026, 5, 9)
            .addDismissedDate(2026, 5, 9);
        Alarm alarm = buildAlarm(7, 0, 2026, 0, 1, Weekdays.NONE, combined.toJson());
        Calendar now = at(2026, 5, 11, 8, 0);

        Calendar previous = alarm.getPreviousAlarmTime(now);
        assertNotNull(previous);
        assertEquals(8, previous.get(Calendar.DAY_OF_MONTH));
        assertTrue(previous.getTimeInMillis() < now.getTimeInMillis());
    }
}