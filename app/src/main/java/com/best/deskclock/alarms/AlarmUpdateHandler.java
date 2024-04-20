/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import com.best.deskclock.AlarmUtils;
import com.best.deskclock.R;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.widget.toast.SnackbarManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * API for asynchronously mutating a single alarm.
 */
public final class AlarmUpdateHandler {

    private final Context mAppContext;
    private final ScrollHandler mScrollHandler;
    private final View mSnackbarAnchor;

    // For undo
    private Alarm mDeletedAlarm;

    public AlarmUpdateHandler(Context context, ScrollHandler scrollHandler, ViewGroup snackbarAnchor) {
        mAppContext = context.getApplicationContext();
        mScrollHandler = scrollHandler;
        mSnackbarAnchor = snackbarAnchor;
    }

    /**
     * Adds a new alarm on the background.
     *
     * @param alarm The alarm to be added.
     */
    public void asyncAddAlarm(final Alarm alarm) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            AlarmInstance instance = null;
            if (alarm != null) {
                Events.sendAlarmEvent(R.string.action_create, R.string.label_deskclock);
                ContentResolver cr = mAppContext.getContentResolver();

                // Add alarm to db
                Alarm newAlarm = Alarm.addAlarm(cr, alarm);

                // Be ready to scroll to this alarm on UI later.
                mScrollHandler.setSmoothScrollStableId(newAlarm.id);

                // Create and add instance to db
                if (newAlarm.enabled) {
                    instance = setupAlarmInstance(newAlarm);
                }
            }

            final AlarmInstance finalInstance = instance;
            handler.post(() -> {
                if (finalInstance != null) {
                    AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, finalInstance.getAlarmTime().getTimeInMillis());
                }
            });
        });
    }

    /**
     * Adds a new alarm on the background for the bedtime.
     *
     * @param alarm The bedtime alarm to be added.
     */
    public void asyncAddAlarmForBedtime(final Alarm alarm) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            AlarmInstance instance = null;
            if (alarm != null) {
                Events.sendAlarmEvent(R.string.action_create, R.string.label_deskclock);
                ContentResolver cr = mAppContext.getContentResolver();

                // Add alarm to db
                Alarm newAlarm = Alarm.addAlarm(cr, alarm);

                // Create and add instance to db
                if (newAlarm.enabled) {
                    instance = setupAlarmInstance(newAlarm);
                }
            }

            final AlarmInstance finalInstance = instance;
            handler.post(() -> {
                if (finalInstance != null) {
                    AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, finalInstance.getAlarmTime().getTimeInMillis());
                }
            });
        });
    }

    /**
     * Modifies an alarm on the background, and optionally show a toast when done.
     *
     * @param alarm       The alarm to be modified.
     * @param popToast    whether or not a toast should be displayed when done.
     * @param minorUpdate if true, don't affect any currently snoozed instances.
     */
    public void asyncUpdateAlarm(final Alarm alarm, final boolean popToast,
                                 final boolean minorUpdate) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            ContentResolver cr = mAppContext.getContentResolver();

            // Update alarm
            Alarm.updateAlarm(cr, alarm);

            if (minorUpdate) {
                // just update the instance in the database and update notifications.
                final List<AlarmInstance> instanceList =
                        AlarmInstance.getInstancesByAlarmId(cr, alarm.id);
                for (AlarmInstance instance : instanceList) {
                    // Make a copy of the existing instance
                    final AlarmInstance newInstance = new AlarmInstance(instance);
                    // Copy over minor change data to the instance; we don't know
                    // exactly which minor field changed, so just copy them all.
                    newInstance.mVibrate = alarm.vibrate;
                    newInstance.mRingtone = alarm.alert;
                    newInstance.mLabel = alarm.label;
                    // Since we copied the mId of the old instance and the mId is used
                    // as the primary key in the AlarmInstance table, this will replace
                    // the existing instance.
                    AlarmInstance.updateInstance(cr, newInstance);
                    // Update the notification for this instance.
                    AlarmNotifications.updateNotification(mAppContext, newInstance);
                }
                return;
            }
            // Otherwise, this is a major update and we're going to re-create the alarm
            AlarmStateManager.deleteAllInstances(mAppContext, alarm.id);

            final AlarmInstance finalInstance = alarm.enabled ? setupAlarmInstance(alarm) : null;

            handler.post(() -> {
                if (popToast && finalInstance != null) {
                    AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, finalInstance.getAlarmTime().getTimeInMillis());
                }
            });
        });
    }

    /**
     * Deletes an alarm on the background.
     *
     * @param alarm The alarm to be deleted.
     */
    public void asyncDeleteAlarm(final Alarm alarm) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // Activity may be closed at this point , make sure data is still valid
            if (alarm == null) {
                // Nothing to do here, just return.
                return;
            }
            AlarmStateManager.deleteAllInstances(mAppContext, alarm.id);
            final boolean deleted = Alarm.deleteAlarm(mAppContext.getContentResolver(), alarm.id);

            handler.post(() -> {
                if (deleted) {
                    mDeletedAlarm = alarm;
                    showUndoBar();
                }
            });
        });
    }

    /**
     * Hides any undo toast.
     */
    public void hideUndoBar() {
        mDeletedAlarm = null;
        SnackbarManager.dismiss();
    }

    private void showUndoBar() {
        final Alarm deletedAlarm = mDeletedAlarm;
        final Snackbar snackbar = Snackbar.make(mSnackbarAnchor,
                        mAppContext.getString(R.string.alarm_deleted), Snackbar.LENGTH_LONG)
                .setAction(R.string.alarm_undo, v -> {
                    mDeletedAlarm = null;
                    asyncAddAlarm(deletedAlarm);
                });
        SnackbarManager.show(snackbar);
    }

    private AlarmInstance setupAlarmInstance(Alarm alarm) {
        final ContentResolver cr = mAppContext.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        AlarmInstance.addInstance(cr, newInstance);
        // Register instance to state manager
        AlarmStateManager.registerInstance(mAppContext, newInstance, true);
        return newInstance;
    }
}
