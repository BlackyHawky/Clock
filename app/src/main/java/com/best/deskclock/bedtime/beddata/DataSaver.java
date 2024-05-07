// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.bedtime.beddata;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;

public class DataSaver {

    private static final String PREF_BASE = "BEDTIME.";
    private static final String PREF_NAME = PREF_BASE + "DataSaverPrefs";
    private static final String KEY_ENABLED = PREF_BASE + "enabled";
    private static final String KEY_HOUR = PREF_BASE + "hour";
    private static final String KEY_MINUTES = PREF_BASE + "minutes";
    private static final String KEY_NOTIFICATION_SHOW_TIME = PREF_BASE + "notificationShowTime";
    private static final String KEY_DAYS_OF_WEEK = PREF_BASE + "daysOfWeek";
    private static final String KEY_TURN_OFF_ALARM = PREF_BASE + "turnoffAlarm";
    private static final String SLEEP_BASE = PREF_BASE + "SLEEP.";
    private static final String KEY_SLEEP_URI = SLEEP_BASE + "sleepUri";
    private static final String KEY_SLEEP_LENGTH = SLEEP_BASE + "sleepLength";

    private static DataSaver instance;
    private final Context context;

    // bedtime mode timing
    public boolean enabled = false;
    public int hour;
    public int minutes;
    public int notificationShowTime;
    public Weekdays daysOfWeek;
    public boolean turnoffAlarm = false;

    // for lullaby
    public Uri sleepUri = DataModel.getDataModel().getAlarmRingtoneUriFromSettings();
    public int sleepLength = 15;

    private DataSaver(Context context) {
        this.context = context;
    }

    public static synchronized DataSaver getInstance(Context context) {
        if (instance == null) {
            instance = new DataSaver(context.getApplicationContext());
        }
        return instance;
    }

    // Saves these values to SharedPreferences
    public void save() {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_ENABLED, enabled);
        editor.putInt(KEY_HOUR, hour);
        editor.putInt(KEY_MINUTES, minutes);
        editor.putInt(KEY_NOTIFICATION_SHOW_TIME, notificationShowTime);
        editor.putInt(KEY_DAYS_OF_WEEK, daysOfWeek.getBits());
        editor.putBoolean(KEY_TURN_OFF_ALARM, turnoffAlarm);
        editor.putString(KEY_SLEEP_URI, sleepUri.toString());
        editor.putInt(KEY_SLEEP_LENGTH, sleepLength);
        editor.apply();
    }

    // Restores the values (sets them from their saving location)
    public void restore() {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        enabled = preferences.getBoolean(KEY_ENABLED, false);
        hour = preferences.getInt(KEY_HOUR, 23);
        minutes = preferences.getInt(KEY_MINUTES, 0);
        notificationShowTime = preferences.getInt(KEY_NOTIFICATION_SHOW_TIME, 15);
        daysOfWeek = Weekdays.fromBits(preferences.getInt(KEY_DAYS_OF_WEEK, 31));
        turnoffAlarm = preferences.getBoolean(KEY_TURN_OFF_ALARM, false);
        sleepUri = Uri.parse(preferences.getString(KEY_SLEEP_URI, DataModel.getDataModel().getAlarmRingtoneUriFromSettings().toString()));// TODO: the default alarm sound isn't a nice Uri to begin sleeping
        sleepLength = preferences.getInt(KEY_SLEEP_LENGTH, sleepLength);
    }
}

