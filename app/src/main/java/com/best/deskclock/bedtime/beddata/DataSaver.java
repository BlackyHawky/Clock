// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.bedtime.beddata;

import android.content.Context;
import android.content.SharedPreferences;

import com.best.deskclock.data.Weekdays;

public class DataSaver {

    private static final String PREF_BASE = "BEDTIME.";
    private static final String PREF_NAME = PREF_BASE + "DataSaverPrefs";
    private static final String KEY_ENABLED = PREF_BASE + "enabled";
    private static final String KEY_HOUR = PREF_BASE + "hour";
    private static final String KEY_MINUTES = PREF_BASE + "minutes";
    private static final String KEY_NOTIF_SHOW_TIME = PREF_BASE + "notifShowTime";
    private static final String KEY_DAYS_OF_WEEK = PREF_BASE + "daysOfWeek";
    private static final String KEY_DO_NOT_DISTURB = PREF_BASE + "doNotDisturb";
    private static final String KEY_TURN_OFF_ALARM = PREF_BASE + "turnoffAlarm";
    /*private static final String KEY_NIGHT_LIGHT = PREF_BASE + "nightLight";FIXME: disabled features only work with root(and even then aren't implemented)
    private static final String KEY_ALWAYS_ON_DISPLAY = "alwaysOnDisplay";*/
    private static final String KEY_DIM_WALL = PREF_BASE + "dimWall";
    //private static final String KEY_ORIG_WALL = PREF_BASE + "origWall";
    //private static final String KEY_DARK_THEME = PREF_BASE + "darkTheme"; FIXME: if any of these feature should work properly we need to sync everything with google's wellbeing which at least i can't do

    private static DataSaver instance;
    private final Context context;

    public boolean enabled = false;
    public int hour;
    public int minutes;
    public int notifShowTime;
    public Weekdays daysOfWeek;

    // what bedtime mode actually is for
    public boolean doNotDisturb = false;
    public boolean turnoffAlarm = false;
    // screen options at bedtime
    /*public boolean nightLight = false;
    public boolean alwaysOnDisplay = false;*/
    public boolean dimWall = false;
    //public boolean darkTheme = false;

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
        editor.putInt(KEY_NOTIF_SHOW_TIME, notifShowTime);
        editor.putInt(KEY_DAYS_OF_WEEK, daysOfWeek.getBits());
        editor.putBoolean(KEY_DO_NOT_DISTURB, doNotDisturb);
        editor.putBoolean(KEY_TURN_OFF_ALARM, turnoffAlarm);
        /*editor.putBoolean(KEY_NIGHT_LIGHT, nightLight);
        editor.putBoolean(KEY_ALWAYS_ON_DISPLAY, alwaysOnDisplay);*/
        editor.putBoolean(KEY_DIM_WALL, dimWall);
        //editor.putBoolean(KEY_DARK_THEME, darkTheme);
        editor.apply();
    }

    // Restores the values (sets them from their saving location)
    public void restore() {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        enabled = preferences.getBoolean(KEY_ENABLED, false);
        hour = preferences.getInt(KEY_HOUR, 23);
        minutes = preferences.getInt(KEY_MINUTES, 0);
        notifShowTime = preferences.getInt(KEY_NOTIF_SHOW_TIME, 15);
        daysOfWeek = Weekdays.fromBits(preferences.getInt(KEY_DAYS_OF_WEEK, 31));
        doNotDisturb = preferences.getBoolean(KEY_DO_NOT_DISTURB, false);
        turnoffAlarm = preferences.getBoolean(KEY_TURN_OFF_ALARM, false);
        /*nightLight = preferences.getBoolean(KEY_NIGHT_LIGHT, false);
        alwaysOnDisplay = preferences.getBoolean(KEY_ALWAYS_ON_DISPLAY, false);*/
        dimWall = preferences.getBoolean(KEY_DIM_WALL, false);
        //darkTheme = preferences.getBoolean(KEY_DARK_THEME, false);
    }
}

