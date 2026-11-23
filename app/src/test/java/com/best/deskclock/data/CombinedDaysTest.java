/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class CombinedDaysTest {

    @Test
    public void dateKeyRoundTripsAndRejectsInvalidDates() {
        assertEquals("2024-02-29", CombinedDays.dateKey(2024, Calendar.FEBRUARY, 29));
        assertEquals(2024, CombinedDays.parseDateKey("2024-02-29")[0]);
        assertEquals(Calendar.FEBRUARY, CombinedDays.parseDateKey("2024-02-29")[1]);
        assertEquals(29, CombinedDays.parseDateKey("2024-02-29")[2]);

        assertInvalidDate("2023-02-29");
        assertInvalidDate("2024-13-01");
        assertInvalidDate("2024-1-01");
        assertInvalidDate("0000-01-01");
    }

    @Test
    public void mutationsAreImmutableAndKeepDateSetsExclusive() {
        CombinedDays original = new CombinedDays().addDeselectedDate(2026, Calendar.AUGUST, 15);
        CombinedDays selected = original.addSelectedDate(2026, Calendar.AUGUST, 15);

        assertTrue(original.isDateDeselected(2026, Calendar.AUGUST, 15));
        assertFalse(original.isDateSelected(2026, Calendar.AUGUST, 15));
        assertTrue(selected.isDateSelected(2026, Calendar.AUGUST, 15));
        assertFalse(selected.isDateDeselected(2026, Calendar.AUGUST, 15));

        CombinedDays dismissed = selected.addDismissedDate(2026, Calendar.AUGUST, 15);
        assertTrue(dismissed.isDateDismissed(2026, Calendar.AUGUST, 15));
        assertFalse(dismissed.isDateActive(2026, Calendar.AUGUST, 15, Weekdays.NONE));
        assertFalse(selected.isDateDismissed(2026, Calendar.AUGUST, 15));
    }

    @Test
    public void changingDateRuleClearsDismissalForThatDate() {
        CombinedDays dismissed = new CombinedDays()
            .addSelectedDate(2026, Calendar.AUGUST, 15)
            .addDismissedDate(2026, Calendar.AUGUST, 15);

        CombinedDays reselected = dismissed.addSelectedDate(2026, Calendar.AUGUST, 15);
        assertFalse(reselected.isDateDismissed(2026, Calendar.AUGUST, 15));

        CombinedDays removed = dismissed.removeSelectedDate(2026, Calendar.AUGUST, 15);
        assertFalse(removed.isDateDismissed(2026, Calendar.AUGUST, 15));
    }

    @Test
    public void dateNavigationSupportsFarFutureDatesAndSkipsDismissed() {
        CombinedDays days = new CombinedDays()
            .addSelectedDate(2028, Calendar.JANUARY, 1)
            .addSelectedDate(2027, Calendar.DECEMBER, 31)
            .addDismissedDate(2027, Calendar.DECEMBER, 31);
        Calendar from = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        from.clear();
        from.set(2027, Calendar.MARCH, 1);

        Calendar next = days.getNextSelectedDate(from);
        assertEquals(2028, next.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, next.get(Calendar.MONTH));
        assertEquals(1, next.get(Calendar.DAY_OF_MONTH));

        Calendar before = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        before.clear();
        before.set(2028, Calendar.JANUARY, 2);
        Calendar previous = days.getPreviousSelectedDate(before);
        assertEquals(2028, previous.get(Calendar.YEAR));
    }

    @Test
    public void cleanupOnlyKeepsOverridesThatChangeTheWeekdaySchedule() {
        Weekdays weekdays = Weekdays.fromCalendarDays(Calendar.MONDAY, Calendar.TUESDAY,
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY);
        CombinedDays overrides = new CombinedDays()
            .addDeselectedDate(2026, Calendar.JUNE, 8) // Monday, meaningful
            .addDeselectedDate(2026, Calendar.JUNE, 13) // Saturday, redundant
            .addSelectedDate(2026, Calendar.JUNE, 20) // Saturday, meaningful
            .addSelectedDate(2026, Calendar.JUNE, 15); // Monday, redundant

        CombinedDays cleaned = overrides.cleanup(weekdays);
        assertEquals(1, cleaned.getDeselectedDateCount());
        assertEquals(1, cleaned.getSelectedDateCount());
        assertTrue(cleaned.isDateDeselected(2026, Calendar.JUNE, 8));
        assertTrue(cleaned.isDateSelected(2026, Calendar.JUNE, 20));
        assertFalse(cleaned.hasRedundantOverrides(weekdays));
    }

    @Test
    public void malformedAndFutureFormatsDoNotCreateInvalidDateEntries() {
        CombinedDays malformed = CombinedDays.fromJson(
            "{\"version\":1,\"selectedDates\":[\"2027-02-29\",\"bad\"],"
                + "\"deselectedDates\":[17],\"dismissedDates\":[]}");
        assertTrue(malformed.isEmpty());

        CombinedDays unsupported = CombinedDays.fromJson(
            "{\"version\":2,\"selectedDates\":[\"2027-03-04\"]}");
        assertTrue(unsupported.isEmpty());
    }
}
