/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.provider;

import static com.best.deskclock.provider.ClockContract.AlarmsColumns;
import static com.best.deskclock.provider.ClockContract.InstancesColumns;
import static com.best.deskclock.provider.ClockDatabaseHelper.ALARMS_TABLE_NAME;
import static com.best.deskclock.provider.ClockDatabaseHelper.INSTANCES_TABLE_NAME;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.ArrayMap;

import androidx.annotation.NonNull;

import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;

import java.util.Map;
import java.util.Objects;

public class ClockProvider extends ContentProvider {

    private static final int ALARMS = 1;
    private static final int ALARMS_ID = 2;
    private static final int INSTANCES = 3;
    private static final int INSTANCES_ID = 4;
    private static final int ALARMS_WITH_INSTANCES = 5;
    /**
     * Projection map used by query for snoozed alarms.
     */
    private static final Map<String, String> sAlarmsWithInstancesProjection = new ArrayMap<>();
    private static final String ALARM_JOIN_INSTANCE_TABLE_STATEMENT =
            ALARMS_TABLE_NAME + " LEFT JOIN " + INSTANCES_TABLE_NAME + " ON (" +
                    ALARMS_TABLE_NAME + "." + AlarmsColumns._ID + " = " + InstancesColumns.ALARM_ID + ")";
    private static final String ALARM_JOIN_INSTANCE_WHERE_STATEMENT =
            INSTANCES_TABLE_NAME + "." + InstancesColumns._ID + " IS NULL OR " +
                    INSTANCES_TABLE_NAME + "." + InstancesColumns._ID + " = (" +
                    "SELECT " + InstancesColumns._ID +
                    " FROM " + INSTANCES_TABLE_NAME +
                    " WHERE " + InstancesColumns.ALARM_ID +
                    " = " + ALARMS_TABLE_NAME + "." + AlarmsColumns._ID +
                    " ORDER BY " + InstancesColumns.ALARM_STATE + ", " +
                    InstancesColumns.YEAR + ", " + InstancesColumns.MONTH + ", " +
                    InstancesColumns.DAY + " LIMIT 1)";
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns._ID,
                ALARMS_TABLE_NAME + "." + AlarmsColumns._ID);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.YEAR,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.YEAR);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.MONTH,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.MONTH);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.DAY,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.DAY);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.HOUR,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.HOUR);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.MINUTES,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.MINUTES);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.DAYS_OF_WEEK,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.DAYS_OF_WEEK);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.ENABLED,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.ENABLED);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.VIBRATE,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.VIBRATE);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.FLASH,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.FLASH);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.LABEL,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.LABEL);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.RINGTONE,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.RINGTONE);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.DELETE_AFTER_USE,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.DELETE_AFTER_USE);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.AUTO_SILENCE_DURATION,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.AUTO_SILENCE_DURATION);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.SNOOZE_DURATION,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.SNOOZE_DURATION);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.MISSED_ALARM_REPEAT_LIMIT,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.MISSED_ALARM_REPEAT_LIMIT);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.CRESCENDO_DURATION,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.CRESCENDO_DURATION);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.ALARM_VOLUME,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.ALARM_VOLUME);

        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.ALARM_STATE,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.ALARM_STATE);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns._ID,
                INSTANCES_TABLE_NAME + "." + InstancesColumns._ID);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.YEAR,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.YEAR);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.MONTH,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.MONTH);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.DAY,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.DAY);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.HOUR,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.HOUR);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.MINUTES,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.MINUTES);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.LABEL,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.LABEL);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.VIBRATE,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.VIBRATE);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.FLASH,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.FLASH);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.AUTO_SILENCE_DURATION,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.AUTO_SILENCE_DURATION);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.SNOOZE_DURATION,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.SNOOZE_DURATION);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.MISSED_ALARM_REPEAT_COUNT,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.MISSED_ALARM_REPEAT_COUNT);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.MISSED_ALARM_REPEAT_LIMIT,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.MISSED_ALARM_REPEAT_LIMIT);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.CRESCENDO_DURATION,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.CRESCENDO_DURATION);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "." + InstancesColumns.ALARM_VOLUME,
                INSTANCES_TABLE_NAME + "." + InstancesColumns.ALARM_VOLUME);
    }

    static {
        sURIMatcher.addURI(ClockContract.AUTHORITY, "alarms", ALARMS);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "alarms/#", ALARMS_ID);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "instances", INSTANCES);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "instances/#", INSTANCES_ID);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "alarms_with_instances", ALARMS_WITH_INSTANCES);
    }

    private ClockDatabaseHelper mOpenHelper;

    public ClockProvider() {
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        final Context storageContext;
        if (SdkUtils.isAtLeastAndroid7()) {
            // All N devices have split storage areas, but we may need to
            // migrate existing database into the new device encrypted
            // storage area, which is where our data lives from now on.
            if (context != null) {
                storageContext = context.createDeviceProtectedStorageContext();
                if (!storageContext.moveDatabaseFrom(context, ClockDatabaseHelper.DATABASE_NAME)) {
                    LogUtils.wtf("Failed to migrate database: %s", ClockDatabaseHelper.DATABASE_NAME);
                }
            } else {
                LogUtils.wtf("Context is null, cannot create device protected storage context.");
                return false;
            }
        } else {
            storageContext = context;
        }

        mOpenHelper = new ClockDatabaseHelper(storageContext);
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projectionIn, String selection,
                        String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        // Generate the body of the query
        int match = sURIMatcher.match(uri);
        switch (match) {
            case ALARMS -> qb.setTables(ALARMS_TABLE_NAME);
            case ALARMS_ID -> {
                qb.setTables(ALARMS_TABLE_NAME);
                qb.appendWhere(AlarmsColumns._ID + "=");
                qb.appendWhere(Objects.requireNonNull(uri.getLastPathSegment()));
            }
            case INSTANCES -> qb.setTables(INSTANCES_TABLE_NAME);
            case INSTANCES_ID -> {
                qb.setTables(INSTANCES_TABLE_NAME);
                qb.appendWhere(InstancesColumns._ID + "=");
                qb.appendWhere(Objects.requireNonNull(uri.getLastPathSegment()));
            }
            case ALARMS_WITH_INSTANCES -> {
                qb.setTables(ALARM_JOIN_INSTANCE_TABLE_STATEMENT);
                qb.appendWhere(ALARM_JOIN_INSTANCE_WHERE_STATEMENT);
                qb.setProjectionMap(sAlarmsWithInstancesProjection);
            }
            default -> throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);

        if (ret == null) {
            LogUtils.e("Alarms.query: failed");
        } else {
            ret.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        }

        return ret;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        int match = sURIMatcher.match(uri);
        return switch (match) {
            case ALARMS -> "vnd.android.cursor.dir/alarms";
            case ALARMS_ID -> "vnd.android.cursor.item/alarms";
            case INSTANCES -> "vnd.android.cursor.dir/instances";
            case INSTANCES_ID -> "vnd.android.cursor.item/instances";
            default -> throw new IllegalArgumentException("Unknown URI");
        };
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count;
        String alarmId;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case ALARMS_ID -> {
                alarmId = uri.getLastPathSegment();
                count = db.update(ALARMS_TABLE_NAME, values,
                        AlarmsColumns._ID + "=" + alarmId,
                        null);
            }
            case INSTANCES_ID -> {
                alarmId = uri.getLastPathSegment();
                count = db.update(INSTANCES_TABLE_NAME, values,
                        InstancesColumns._ID + "=" + alarmId,
                        null);
            }
            default -> throw new UnsupportedOperationException("Cannot update URI: " + uri);
        }

        LogUtils.v("*** notifyChange() id: " + alarmId + " url " + uri);
        notifyChange(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return count;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        long rowId;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        rowId = switch (sURIMatcher.match(uri)) {
            case ALARMS -> mOpenHelper.fixAlarmInsert(initialValues);
            case INSTANCES -> db.insert(INSTANCES_TABLE_NAME, null, initialValues);
            default -> throw new IllegalArgumentException("Cannot insert from URI: " + uri);
        };

        Uri uriResult = ContentUris.withAppendedId(uri, rowId);
        notifyChange(Objects.requireNonNull(getContext()).getContentResolver(), uriResult);
        return uriResult;
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        int count;
        String primaryKey;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case ALARMS -> count = db.delete(ALARMS_TABLE_NAME, where, whereArgs);
            case ALARMS_ID -> {
                primaryKey = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where = AlarmsColumns._ID + "=" + primaryKey;
                } else {
                    where = AlarmsColumns._ID + "=" + primaryKey + " AND (" + where + ")";
                }
                count = db.delete(ALARMS_TABLE_NAME, where, whereArgs);
            }
            case INSTANCES -> count = db.delete(INSTANCES_TABLE_NAME, where, whereArgs);
            case INSTANCES_ID -> {
                primaryKey = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where = InstancesColumns._ID + "=" + primaryKey;
                } else {
                    where = InstancesColumns._ID + "=" + primaryKey + " AND (" + where + ")";
                }
                count = db.delete(INSTANCES_TABLE_NAME, where, whereArgs);
            }
            default -> throw new IllegalArgumentException("Cannot delete from URI: " + uri);
        }

        notifyChange(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return count;
    }

    /**
     * Notify affected URIs of changes.
     */
    private void notifyChange(ContentResolver resolver, Uri uri) {
        resolver.notifyChange(uri, null);

        final int match = sURIMatcher.match(uri);
        // Also notify the joined table of changes to instances or alarms.
        if (match == ALARMS || match == INSTANCES || match == ALARMS_ID || match == INSTANCES_ID) {
            resolver.notifyChange(AlarmsColumns.ALARMS_WITH_INSTANCES_URI, null);
        }
    }
}
