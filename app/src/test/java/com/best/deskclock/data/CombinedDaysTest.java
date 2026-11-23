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
    public void selectionRulesAreImmutableAndExclusive() {
        CombinedDays empty = new CombinedDays();
        CombinedDays selected = empty.addSelectedDate(2026, Calendar.AUGUST, 15);
        assertFalse(empty.isDateSelected(2026, Calendar.AUGUST, 15));
        assertTrue(selected.isDateSelected(2026, Calendar.AUGUST, 15));

        CombinedDays deselected = selected.addDeselectedDate(2026, Calendar.AUGUST, 15);
        assertTrue(deselected.isDateDeselected(2026, Calendar.AUGUST, 15));
        assertFalse(deselected.isDateSelected(2026, Calendar.AUGUST, 15));
    }
}
