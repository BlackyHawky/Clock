/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.android.deskclock.LogUtils;

import java.util.Map;

import static com.android.deskclock.provider.ClockContract.AlarmsColumns;
import static com.android.deskclock.provider.ClockContract.InstancesColumns;
import static com.android.deskclock.provider.ClockDatabaseHelper.ALARMS_TABLE_NAME;
import static com.android.deskclock.provider.ClockDatabaseHelper.INSTANCES_TABLE_NAME;

public class ClockProvider extends ContentProvider {

    private ClockDatabaseHelper mOpenHelper;

    private static final int ALARMS = 1;
    private static final int ALARMS_ID = 2;
    private static final int INSTANCES = 3;
    private static final int INSTANCES_ID = 4;
    private static final int ALARMS_WITH_INSTANCES = 5;

    /**
     * Projection map used by query for snoozed alarms.
     */
    private static final Map<String, String> sAlarmsWithInstancesProjection = new ArrayMap<>();
    static {
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns._ID,
                ALARMS_TABLE_NAME + "." + AlarmsColumns._ID);
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
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.LABEL,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.LABEL);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.RINGTONE,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.RINGTONE);
        sAlarmsWithInstancesProjection.put(ALARMS_TABLE_NAME + "." + AlarmsColumns.DELETE_AFTER_USE,
                ALARMS_TABLE_NAME + "." + AlarmsColumns.DELETE_AFTER_USE);
        sAlarmsWithInstancesProjection.put(INSTANCES_TABLE_NAME + "."
                + InstancesColumns.ALARM_STATE,
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
    }

    private static final String ALARM_JOIN_INSTANCE_TABLE_STATEMENT =
            ALARMS_TABLE_NAME + " LEFT JOIN " + INSTANCES_TABLE_NAME + " ON (" +
            ALARMS_TABLE_NAME + "." + AlarmsColumns._ID + " = " + InstancesColumns.ALARM_ID + ")";

    private static final String ALARM_JOIN_INSTANCE_WHERE_STATEMENT =
            InstancesColumns.ALARM_STATE + " IS NULL OR " +
            InstancesColumns.ALARM_STATE + " = (SELECT MIN(" + InstancesColumns.ALARM_STATE +
                    ") FROM " + INSTANCES_TABLE_NAME + " WHERE " + InstancesColumns.ALARM_ID +
                    " = " + ALARMS_TABLE_NAME + "." + AlarmsColumns._ID + ")";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(ClockContract.AUTHORITY, "alarms", ALARMS);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "alarms/#", ALARMS_ID);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "instances", INSTANCES);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "instances/#", INSTANCES_ID);
        sURIMatcher.addURI(ClockContract.AUTHORITY, "alarms_with_instances", ALARMS_WITH_INSTANCES);
    }

    public ClockProvider() {
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new ClockDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs,
            String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        // Generate the body of the query
        int match = sURIMatcher.match(uri);
        switch (match) {
            case ALARMS:
                qb.setTables(ALARMS_TABLE_NAME);
                break;
            case ALARMS_ID:
                qb.setTables(ALARMS_TABLE_NAME);
                qb.appendWhere(AlarmsColumns._ID + "=");
                qb.appendWhere(uri.getLastPathSegment());
                break;
            case INSTANCES:
                qb.setTables(INSTANCES_TABLE_NAME);
                break;
            case INSTANCES_ID:
                qb.setTables(INSTANCES_TABLE_NAME);
                qb.appendWhere(InstancesColumns._ID + "=");
                qb.appendWhere(uri.getLastPathSegment());
                break;
            case ALARMS_WITH_INSTANCES:
                qb.setTables(ALARM_JOIN_INSTANCE_TABLE_STATEMENT);
                qb.appendWhere(ALARM_JOIN_INSTANCE_WHERE_STATEMENT);
                qb.setProjectionMap(sAlarmsWithInstancesProjection);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);

        if (ret == null) {
            LogUtils.e("Alarms.query: failed");
        } else {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return ret;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case ALARMS:
                return "vnd.android.cursor.dir/alarms";
            case ALARMS_ID:
                return "vnd.android.cursor.item/alarms";
            case INSTANCES:
                return "vnd.android.cursor.dir/instances";
            case INSTANCES_ID:
                return "vnd.android.cursor.item/instances";
            default:
                throw new IllegalArgumentException("Unknown URI");
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count;
        String alarmId;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case ALARMS_ID:
                alarmId = uri.getLastPathSegment();
                count = db.update(ALARMS_TABLE_NAME, values,
                        AlarmsColumns._ID + "=" + alarmId,
                        null);
                break;
            case INSTANCES_ID:
                alarmId = uri.getLastPathSegment();
                count = db.update(INSTANCES_TABLE_NAME, values,
                        InstancesColumns._ID + "=" + alarmId,
                        null);
                break;
            default: {
                throw new UnsupportedOperationException("Cannot update URI: " + uri);
            }
        }
        LogUtils.v("*** notifyChange() id: " + alarmId + " url " + uri);
        notifyChange(getContext().getContentResolver(), uri);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        long rowId;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case ALARMS:
                rowId = mOpenHelper.fixAlarmInsert(initialValues);
                break;
            case INSTANCES:
                rowId = db.insert(INSTANCES_TABLE_NAME, null, initialValues);
                break;
            default:
                throw new IllegalArgumentException("Cannot insert from URI: " + uri);
        }

        Uri uriResult = ContentUris.withAppendedId(AlarmsColumns.CONTENT_URI, rowId);
        notifyChange(getContext().getContentResolver(), uriResult);
        return uriResult;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        int count;
        String primaryKey;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case ALARMS:
                count = db.delete(ALARMS_TABLE_NAME, where, whereArgs);
                break;
            case ALARMS_ID:
                primaryKey = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where = AlarmsColumns._ID + "=" + primaryKey;
                } else {
                    where = AlarmsColumns._ID + "=" + primaryKey + " AND (" + where + ")";
                }
                count = db.delete(ALARMS_TABLE_NAME, where, whereArgs);
                break;
            case INSTANCES:
                count = db.delete(INSTANCES_TABLE_NAME, where, whereArgs);
                break;
            case INSTANCES_ID:
                primaryKey = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where = InstancesColumns._ID + "=" + primaryKey;
                } else {
                    where = InstancesColumns._ID + "=" + primaryKey + " AND (" + where + ")";
                }
                count = db.delete(INSTANCES_TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete from URI: " + uri);
        }

        notifyChange(getContext().getContentResolver(), uri);
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
