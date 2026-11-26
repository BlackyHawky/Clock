/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ClockDatabaseHelperTest {

    @Test
    public void freshDatabaseIncludesCombinedDaysAndCategories() {
        File file = new File(RuntimeEnvironment.getApplication().getCacheDir(), "fresh-alarms.db");
        try (SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            ClockDatabaseHelper helper = new ClockDatabaseHelper(RuntimeEnvironment.getApplication());
            helper.onCreate(database);

            Set<String> columns = columns(database, ClockDatabaseHelper.ALARMS_TABLE_NAME);
            assertTrue(columns.contains(ClockContract.AlarmsColumns.COMBINED_DAYS));
            assertTrue(columns.contains(ClockContract.AlarmsColumns.CATEGORY_IDS));
            try (Cursor cursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{ClockDatabaseHelper.CATEGORIES_TABLE_NAME})) {
                assertTrue(cursor.moveToFirst());
            }
            helper.close();
        } finally {
            // The test database lives only under Robolectric's cache directory.
            file.delete();
        }
    }

    @Test
    public void upgradingVersion32AddsMissingColumnsWithoutDroppingAlarms() {
        File file = new File(RuntimeEnvironment.getApplication().getCacheDir(), "upgrade-32.db");
        try (SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            database.execSQL("CREATE TABLE alarm_templates (_id INTEGER PRIMARY KEY, hour INTEGER NOT NULL)");
            ContentValues row = new ContentValues();
            row.put("_id", 42L);
            row.put("hour", 7);
            database.insertOrThrow("alarm_templates", null, row);

            ClockDatabaseHelper helper = new ClockDatabaseHelper(RuntimeEnvironment.getApplication());
            helper.onUpgrade(database, 32, 34);

            assertTrue(columns(database, "alarm_templates").contains(ClockContract.AlarmsColumns.COMBINED_DAYS));
            assertTrue(columns(database, "alarm_templates").contains(ClockContract.AlarmsColumns.LOCK));
            try (Cursor cursor = database.rawQuery(
                "SELECT _id, hour, combinedDays, lock FROM alarm_templates WHERE _id=42", null)) {
                assertTrue(cursor.moveToFirst());
                assertEquals(7, cursor.getInt(1));
                assertEquals("", cursor.getString(2));
                assertEquals(0, cursor.getInt(3));
            }
            helper.close();
        } finally {
            file.delete();
        }
    }

    @Test
    public void upgradingUpstreamVersion30RepairsPreviewColumns() {
        File file = new File(RuntimeEnvironment.getApplication().getCacheDir(), "upgrade-30.db");
        try (SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            database.execSQL("CREATE TABLE alarm_templates (_id INTEGER PRIMARY KEY, hour INTEGER NOT NULL)");
            ContentValues row = new ContentValues();
            row.put("_id", 9L);
            row.put("hour", 6);
            database.insertOrThrow("alarm_templates", null, row);

            ClockDatabaseHelper helper = new ClockDatabaseHelper(RuntimeEnvironment.getApplication());
            helper.onUpgrade(database, 30, 34);
            helper.onUpgrade(database, 34, 34);

            Set<String> columns = columns(database, "alarm_templates");
            assertTrue(columns.contains(ClockContract.AlarmsColumns.COMBINED_DAYS));
            assertTrue(columns.contains(ClockContract.AlarmsColumns.CATEGORY_IDS));
            assertTrue(columns.contains(ClockContract.AlarmsColumns.LOCK));
            try (Cursor cursor = database.rawQuery(
                "SELECT hour, combinedDays FROM alarm_templates WHERE _id=9", null)) {
                assertTrue(cursor.moveToFirst());
                assertEquals(6, cursor.getInt(0));
                assertEquals("", cursor.getString(1));
            }
            try (Cursor cursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{ClockDatabaseHelper.CATEGORIES_TABLE_NAME})) {
                assertTrue(cursor.moveToFirst());
            }
            helper.close();
        } finally {
            file.delete();
        }
    }

    @Test
    public void upgradingPreviewVersion33KeepsExistingCombinedDays() {
        File file = new File(RuntimeEnvironment.getApplication().getCacheDir(), "upgrade-33.db");
        try (SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            database.execSQL("CREATE TABLE alarm_templates "
                + "(_id INTEGER PRIMARY KEY, combinedDays TEXT NOT NULL DEFAULT '')");
            ContentValues row = new ContentValues();
            row.put("_id", 7L);
            row.put("combinedDays", "{\"version\":1,\"selectedDates\":[\"2027-03-04\"]}");
            database.insertOrThrow("alarm_templates", null, row);

            ClockDatabaseHelper helper = new ClockDatabaseHelper(RuntimeEnvironment.getApplication());
            helper.onUpgrade(database, 33, 34);

            try (Cursor cursor = database.rawQuery(
                "SELECT combinedDays FROM alarm_templates WHERE _id=7", null)) {
                assertTrue(cursor.moveToFirst());
                assertEquals("{\"version\":1,\"selectedDates\":[\"2027-03-04\"]}", cursor.getString(0));
            }
            helper.close();
        } finally {
            file.delete();
        }
    }

    private static Set<String> columns(SQLiteDatabase database, String table) {
        Set<String> result = new HashSet<>();
        try (Cursor cursor = database.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                result.add(cursor.getString(nameIndex));
            }
        }
        return result;
    }
}
