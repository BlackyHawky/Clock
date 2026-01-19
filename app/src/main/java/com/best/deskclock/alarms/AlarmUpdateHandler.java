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

import com.best.deskclock.R;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uicomponents.toast.SnackbarManager;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;
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

    private AlarmUpdateCallback mUpdateCallback;

    private boolean mUseGlobalNextAlarmToast = false;

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
                Alarm newAlarm = alarm.addAlarm(cr);

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
     * Modifies an alarm on the background, and optionally show a toast when done.
     *
     * @param alarm       The alarm to be modified.
     * @param popToast    whether or not a toast should be displayed when done.
     * @param minorUpdate if true, don't affect any currently snoozed instances.
     */
    public void asyncUpdateAlarm(final Alarm alarm, final boolean popToast, final boolean minorUpdate) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            ContentResolver cr = mAppContext.getContentResolver();

            if (mUpdateCallback != null) {
                LogUtils.v("AlarmUpdateHandler: notifying update started");
                handler.post(() -> mUpdateCallback.onAlarmUpdateStarted());
            }

            try {
                // Update alarm
                alarm.updateAlarm(cr);

                if (minorUpdate) {
                    // just update the instance in the database and update notifications.
                    final List<AlarmInstance> instanceList =
                            AlarmInstance.getInstancesByAlarmId(cr, alarm.id);
                    for (AlarmInstance instance : instanceList) {
                        // Make a copy of the existing instance
                        final AlarmInstance newInstance = new AlarmInstance(instance);
                        // Copy over minor change data to the instance; we don't know
                        // exactly which minor field changed, so just copy them all.
                        newInstance.mLabel = alarm.label;
                        newInstance.mSyncByLabel = alarm.syncByLabel;
                        newInstance.mVibrate = alarm.vibrate;
                        newInstance.mVibrationPattern = alarm.vibrationPattern;
                        newInstance.mFlash = alarm.flash;
                        newInstance.mRingtone = alarm.alert;
                        newInstance.mAutoSilenceDuration = alarm.autoSilenceDuration;
                        newInstance.mSnoozeDuration = alarm.snoozeDuration;
                        newInstance.mMissedAlarmRepeatLimit = alarm.missedAlarmRepeatLimit;
                        newInstance.mCrescendoDuration = alarm.crescendoDuration;
                        newInstance.mAlarmVolume = alarm.alarmVolume;

                        // If the alarm is in Missed state, mark it as Dismissed and clear its notification.
                        if (newInstance.mAlarmState == AlarmInstance.MISSED_STATE) {
                            LogUtils.i("Minor update: resetting missed alarm " + instance.mId);
                            newInstance.mAlarmState = AlarmInstance.DISMISSED_STATE;
                            AlarmNotifications.clearNotification(mAppContext, newInstance);
                        }
                        // Since we copied the mId of the old instance and the mId is used
                        // as the primary key in the AlarmInstance table, this will replace
                        // the existing instance.
                        newInstance.updateInstance(cr);
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
                        if (mUseGlobalNextAlarmToast) {
                            mUseGlobalNextAlarmToast = false;
                            AlarmInstance next = AlarmInstance.getNextAlarmInstance(cr);
                            if (next != null) {
                                AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, next.getAlarmTime().getTimeInMillis());
                            }
                        } else {
                            AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, finalInstance.getAlarmTime().getTimeInMillis());
                        }
                    }
                });
            } finally {
                if (mUpdateCallback != null) {
                    handler.post(() -> mUpdateCallback.onAlarmUpdateFinished());
                }
            }
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
     * Instructs the next alarm update operation to display a toast based on the next upcoming
     * alarm instance across all alarms, rather than the instance of the alarm being updated.
     *
     * <p>This flag is consumed once during the next call to {@code asyncUpdateAlarm()}
     * where {@code popToast} is {@code true}. It is used when enabling a group of synchronized
     * alarms to ensure that only one toast is shown, corresponding to the earliest upcoming alarm
     * in the group.
     */
    public void useGlobalNextAlarmToast() {
        mUseGlobalNextAlarmToast = true;
    }

    /**
     * Hides any undo toast.
     */
    public void hideUndoBar() {
        mDeletedAlarm = null;
        SnackbarManager.dismiss();
    }

    private void showUndoBar() {
        final Context localizedContext = Utils.getLocalizedContext(mAppContext);
        final Alarm deletedAlarm = mDeletedAlarm;
        final Snackbar snackbar = Snackbar.make(mSnackbarAnchor, localizedContext.getString(R.string.alarm_deleted),
                Snackbar.LENGTH_LONG).setAction(R.string.alarm_undo, v -> {
                    mDeletedAlarm = null;
                    asyncAddAlarm(deletedAlarm);
                });
        SnackbarManager.show(snackbar);
    }

    private AlarmInstance setupAlarmInstance(Alarm alarm) {
        final ContentResolver cr = mAppContext.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        newInstance.addInstance(cr);
        // Register instance to state manager
        AlarmStateManager.registerInstance(mAppContext, newInstance, true);
        return newInstance;
    }

    /**
     * Registers a callback to be notified when an alarm update starts or finishes.
     *
     * @param callback the callback to receive update lifecycle events
     */
    public void setAlarmUpdateCallback(AlarmUpdateCallback callback) {
        this.mUpdateCallback = callback;
    }

}
