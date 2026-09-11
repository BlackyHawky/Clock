// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import static java.util.Calendar.MONDAY;
import static java.util.Calendar.TUESDAY;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class CombinedDaysTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static Calendar utcDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(year, month, day);
        return c;
    }

    @Test
    public void dateKey_roundTrips() {
        int[] parts = CombinedDays.parseDateKey(CombinedDays.dateKey(2026, 8, 15));
        assertEquals(2026, parts[0]);
        assertEquals(8, parts[1]);
        assertEquals(15, parts[2]);
        assertEquals("2026-09-15", CombinedDays.dateKey(2026, 8, 15));
    }

    @Test
    public void addSelectedDate_addsAndIsIdempotent() {
        CombinedDays days = new CombinedDays();
        CombinedDays once = days.addSelectedDate(2026, 8, 15);
        assertEquals(1, once.getSelectedDateCount());
        assertTrue(once.isDateSelected(2026, 8, 15));
        CombinedDays twice = once.addSelectedDate(2026, 8, 15);
        assertEquals(1, twice.getSelectedDateCount());
        assertFalse(days.isDateSelected(2026, 8, 15));
    }

    @Test
    public void addSelectedDate_prunesDeselected() {
        CombinedDays days = new CombinedDays()
            .addDeselectedDate(2026, 8, 15)
            .addSelectedDate(2026, 8, 15);
        assertTrue(days.isDateSelected(2026, 8, 15));
        assertFalse(days.isDateDeselected(2026, 8, 15));
        assertEquals(0, days.getDeselectedDateCount());
    }

    @Test
    public void addDeselectedDate_prunesSelected() {
        CombinedDays days = new CombinedDays()
            .addSelectedDate(2026, 8, 15)
            .addDeselectedDate(2026, 8, 15);
        assertTrue(days.isDateDeselected(2026, 8, 15));
        assertFalse(days.isDateSelected(2026, 8, 15));
        assertEquals(0, days.getSelectedDateCount());
    }

    @Test
    public void removeSelectedDate_clearsBothLists() {
        CombinedDays days = new CombinedDays()
            .addSelectedDate(2026, 8, 15)
            .addDeselectedDate(2026, 8, 15)
            .removeSelectedDate(2026, 8, 15);
        assertFalse(days.isDateSelected(2026, 8, 15));
        assertFalse(days.isDateDeselected(2026, 8, 15));
    }

    @Test
    public void removeDeselectedDate_clearsBothLists() {
        CombinedDays days = new CombinedDays()
            .addSelectedDate(2026, 8, 15)
            .addDeselectedDate(2026, 8, 15)
            .removeDeselectedDate(2026, 8, 15);
        assertFalse(days.isDateSelected(2026, 8, 15));
        assertFalse(days.isDateDeselected(2026, 8, 15));
    }

    @Test
    public void toggleSelectedDate_togglesAndNormalizes() {
        CombinedDays days = new CombinedDays().addDeselectedDate(2026, 8, 15);
        CombinedDays added = days.toggleSelectedDate(2026, 8, 15);
        assertTrue(added.isDateSelected(2026, 8, 15));
        assertFalse(added.isDateDeselected(2026, 8, 15));

        CombinedDays removed = added.toggleSelectedDate(2026, 8, 15);
        assertFalse(removed.isDateSelected(2026, 8, 15));
        assertFalse(removed.isDateDeselected(2026, 8, 15));
    }

    @Test
    public void toggleDeselectedDate_togglesAndNormalizes() {
        CombinedDays days = new CombinedDays().addSelectedDate(2026, 8, 15);
        CombinedDays added = days.toggleDeselectedDate(2026, 8, 15);
        assertTrue(added.isDateDeselected(2026, 8, 15));
        assertFalse(added.isDateSelected(2026, 8, 15));

        CombinedDays removed = added.toggleDeselectedDate(2026, 8, 15);
        assertFalse(removed.isDateDeselected(2026, 8, 15));
        assertFalse(removed.isDateSelected(2026, 8, 15));
    }

    @Test
    public void json_roundTripsAllLists() {
        CombinedDays original = new CombinedDays()
            .addSelectedDate(2026, 8, 15)
            .addDeselectedDate(2026, 8, 20)
            .addDismissedDate(2026, 8, 22);
        CombinedDays restored = CombinedDays.fromJson(original.toJson());
        assertEquals(original, restored);
        assertTrue(restored.hasDismissedDates());
    }

    @Test
    public void json_includesVersionField() throws Exception {
        String json = new CombinedDays().addSelectedDate(2026, 8, 15).toJson();
        JSONObject obj = new JSONObject(json);
        assertEquals(1, obj.getInt("version"));
    }

    @Test
    public void json_parsesLegacyBlobWithoutVersion() {
        String legacy = "{\"selectedDates\":[\"2026-09-15\"],\"deselectedDates\":[\"2026-09-20\"],\"dismissedDates\":[]}";
        CombinedDays days = CombinedDays.fromJson(legacy);
        assertTrue(days.isDateSelected(2026, 8, 15));
        assertTrue(days.isDateDeselected(2026, 8, 20));
    }

    @Test
    public void json_malformed_returnsEmpty() {
        CombinedDays days = CombinedDays.fromJson("{\"selectedDates\" not json");
        assertTrue(days.isEmpty());
        assertEquals("", CombinedDays.EMPTY_JSON);
    }

    @Test
    public void clear_returnsEmpty() {
        CombinedDays days = new CombinedDays().addSelectedDate(2026, 8, 15);
        assertTrue(days.clear().isEmpty());
        assertFalse(days.isEmpty());
    }

    @Test
    public void isDateActive_weekdayOn() {
        Weekdays weekdays = Weekdays.fromCalendarDays(MONDAY);
        CombinedDays none = new CombinedDays();
        assertTrue(none.isDateActive(2026, 8, 14, weekdays)); // Monday

        CombinedDays skipped = none.addDeselectedDate(2026, 8, 14);
        assertFalse(skipped.isDateActive(2026, 8, 14, weekdays));
    }

    @Test
    public void isDateActive_weekdayOff() {
        Weekdays weekdays = Weekdays.fromCalendarDays(MONDAY);
        CombinedDays none = new CombinedDays();
        assertFalse(none.isDateActive(2026, 8, 16, weekdays)); // Tuesday

        CombinedDays added = none.addSelectedDate(2026, 8, 16);
        assertTrue(added.isDateActive(2026, 8, 16, weekdays));
    }

    @Test
    public void isDateActive_dismissedDoesNotChangeState() {
        Weekdays weekdays = Weekdays.fromCalendarDays(MONDAY);
        CombinedDays days = new CombinedDays().addDismissedDate(2026, 8, 14);
        assertTrue(days.isDateActive(2026, 8, 14, weekdays));
    }

    @Test
    public void cleanup_removesOnlyRedundantOverrides() {
        Weekdays weekdays = Weekdays.fromCalendarDays(MONDAY, TUESDAY);
        CombinedDays days = new CombinedDays()
            // Redundant: weekday selected
            .addSelectedDate(2026, 8, 14) // Monday
            // Redundant: weekday off
            .addDeselectedDate(2026, 8, 16) // Wednesday
            // Keep: weekday off, explicitly added
            .addSelectedDate(2026, 8, 18) // Friday
            // Keep: weekday selected, explicitly skipped
            .addDeselectedDate(2026, 8, 15); // Tuesday

        CombinedDays cleaned = days.cleanup(weekdays);
        assertFalse(cleaned.isDateSelected(2026, 8, 14));
        assertFalse(cleaned.isDateDeselected(2026, 8, 16));
        assertTrue(cleaned.isDateSelected(2026, 8, 18));
        assertTrue(cleaned.isDateDeselected(2026, 8, 15));
    }

    @Test
    public void hasRedundantOverrides_detectsRedundancy() {
        Weekdays weekdays = Weekdays.fromCalendarDays(MONDAY);
        assertFalse(new CombinedDays().hasRedundantOverrides(weekdays));
        assertTrue(new CombinedDays().addSelectedDate(2026, 8, 14).hasRedundantOverrides(weekdays));
        assertFalse(new CombinedDays().addSelectedDate(2026, 8, 16).hasRedundantOverrides(weekdays));
    }

    @Test
    public void getNextSelectedDate_onOrAfter_skipsDismissed() {
        CombinedDays days = new CombinedDays()
            .addSelectedDate(2026, 8, 15)
            .addSelectedDate(2026, 8, 20)
            .addDismissedDate(2026, 8, 15);

        Calendar next = days.getNextSelectedDate(utcDate(2026, 8, 14));
        assertNotNull(next);
        assertEquals(20, next.get(Calendar.DAY_OF_MONTH));

        next = days.getNextSelectedDate(utcDate(2026, 8, 20));
        assertNotNull(next);
        assertEquals(20, next.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void getNextSelectedDate_nullBeyondOneYear() {
        CombinedDays days = new CombinedDays().addSelectedDate(2028, 0, 1);
        assertNull(days.getNextSelectedDate(utcDate(2026, 8, 14)));
    }

    @Test
    public void getPreviousSelectedDate_strictlyBefore_skipsDismissed() {
        CombinedDays days = new CombinedDays()
            .addSelectedDate(2026, 8, 13)
            .addSelectedDate(2026, 8, 14)
            .addSelectedDate(2026, 8, 20)
            .addDismissedDate(2026, 8, 14);

        Calendar previous = days.getPreviousSelectedDate(utcDate(2026, 8, 21));
        assertNotNull(previous);
        assertEquals(20, previous.get(Calendar.DAY_OF_MONTH));

        // Sep 14 is selected but dismissed, so it is skipped in favor of Sep 13
        previous = days.getPreviousSelectedDate(utcDate(2026, 8, 20));
        assertNotNull(previous);
        assertEquals(13, previous.get(Calendar.DAY_OF_MONTH));

        previous = days.getPreviousSelectedDate(utcDate(2026, 8, 14));
        assertNotNull(previous);
        assertEquals(13, previous.get(Calendar.DAY_OF_MONTH));

        assertNull(days.getPreviousSelectedDate(utcDate(2026, 8, 13)));
    }

    @Test
    public void removePastDates_keepsFutureAndStripsPast() {
        Calendar today = Calendar.getInstance();
        int year = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH);
        int day = today.get(Calendar.DAY_OF_MONTH);

        Calendar inPast = (Calendar) today.clone();
        inPast.add(Calendar.DAY_OF_MONTH, -3);
        Calendar inFuture = (Calendar) today.clone();
        inFuture.add(Calendar.DAY_OF_MONTH, 5);

        CombinedDays days = new CombinedDays()
            .addSelectedDate(inPast.get(Calendar.YEAR), inPast.get(Calendar.MONTH), inPast.get(Calendar.DAY_OF_MONTH))
            .addSelectedDate(year, month, day)
            .addSelectedDate(inFuture.get(Calendar.YEAR), inFuture.get(Calendar.MONTH), inFuture.get(Calendar.DAY_OF_MONTH));

        CombinedDays kept = days.removePastDates(-1, -1);
        assertFalse(kept.isDateSelected(inPast.get(Calendar.YEAR), inPast.get(Calendar.MONTH), inPast.get(Calendar.DAY_OF_MONTH)));
        assertTrue(kept.isDateSelected(year, month, day)); // time check off -> today kept
        assertTrue(kept.isDateSelected(inFuture.get(Calendar.YEAR), inFuture.get(Calendar.MONTH), inFuture.get(Calendar.DAY_OF_MONTH)));
    }

    @Test
    public void removePastDates_removesTodayWhenAlarmTimePassed() {
        Calendar today = Calendar.getInstance();

        // Alarm time guaranteed already passed today: one minute ago.
        int alarmHour = today.get(Calendar.HOUR_OF_DAY);
        int alarmMinute = today.get(Calendar.MINUTE) - 1;
        if (alarmMinute < 0) {
            alarmMinute = 59;
            alarmHour = (alarmHour == 0) ? 23 : alarmHour - 1;
        }

        CombinedDays days = new CombinedDays().addSelectedDate(
            today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        CombinedDays kept = days.removePastDates(alarmHour, alarmMinute);
        assertFalse(kept.isDateSelected(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)));
    }

    @Test
    public void getters_areUnmodifiable() {
        CombinedDays days = new CombinedDays().addSelectedDate(2026, 8, 15);
        assertThrows(UnsupportedOperationException.class, () -> days.getSelectedDates().add("2026-08-16"));
        assertThrows(UnsupportedOperationException.class, () -> days.getDeselectedDates().add("2026-08-16"));
        assertThrows(UnsupportedOperationException.class, () -> days.getDismissedDates().add("2026-08-16"));
    }

    @Test
    public void equalsAndHashCode() {
        CombinedDays a = new CombinedDays().addSelectedDate(2026, 8, 15);
        CombinedDays b = new CombinedDays().addSelectedDate(2026, 8, 15);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(new CombinedDays()));
    }
}