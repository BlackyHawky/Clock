package com.android.deskclock;

import android.content.Context;

import com.android.deskclock.provider.Alarm;

/** DeskClockExtensions. */
public interface DeskClockExtensions {

    /**
     * Notify paired device that a new alarm has been created on the phone, so that the alarm can be
     * synced to the device.
     *
     * @param context  the application context.
     * @param newAlarm the alarm to add.
     */
    public void addAlarm(Context context, Alarm newAlarm);

    /**
     * Notify paired device that an alarm has been deleted from the phone so that it can also be
     * deleted from the device.
     *
     * @param context the application context.
     * @param alarmId the alarm id of the alarm to delete.
     */
    public void deleteAlarm(Context context, long alarmId);

}
