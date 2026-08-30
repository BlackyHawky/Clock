/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.HapticFeedbackConstantsCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uicomponents.toast.SnackbarManager;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * API for asynchronously mutating a single alarm.
 */
public final class AlarmUpdateHandler {

    private final Context mAppContext;
    private final SharedPreferences mPrefs;
    private final Typeface mFont;
    private final ScrollHandler mScrollHandler;
    private final View mSnackbarAnchor;
    private final boolean mIsVibrationsEnabled;

    private Alarm mDeletedAlarm;

    private String mSyncToastLabel = null;

    public AlarmUpdateHandler(@NonNull Context context, @NonNull SharedPreferences prefs, @NonNull Typeface font,
                              @Nullable ScrollHandler scrollHandler, @Nullable ViewGroup snackbarAnchor, boolean isVibrationsEnabled) {

        mAppContext = context.getApplicationContext();
        mPrefs = prefs;
        mFont = font;
        mScrollHandler = scrollHandler;
        mSnackbarAnchor = snackbarAnchor;
        mIsVibrationsEnabled = isVibrationsEnabled;
    }

    /**
     * Adds a new alarm on the background.
     *
     * @param alarm The alarm to be added.
     */
    public void asyncAddAlarm(@NonNull Alarm alarm) {
        asyncAddAlarm(alarm, true, null);
    }

    /**
     * Adds a new alarm on the background.
     *
     * @param alarm The alarm to be added.
     * @param listener A callback invoked on the main thread once the alarm has been successfully saved, providing the newly created alarm
     *                 with its generated database ID. Can be null.
     */
    public void asyncAddAlarm(@Nullable Alarm alarm, boolean showSnackbar, @Nullable OnAlarmSavedListener listener) {
        AppExecutors.getDiskIO().execute(() -> {
            AlarmInstance instance = null;
            Alarm newAlarm = null;

            if (alarm != null) {
                Events.sendAlarmEvent(R.string.action_create, R.string.label_deskclock);
                ContentResolver cr = mAppContext.getContentResolver();

                // Add alarm to db
                newAlarm = alarm.addAlarm(cr);

                // Be ready to scroll to this alarm on UI later.
                if (mScrollHandler != null) {
                    mScrollHandler.setSmoothScrollStableId(newAlarm.id);
                }

                // Create and add instance to db
                if (newAlarm.enabled) {
                    instance = setupAlarmInstance(newAlarm);
                }
            }

            final AlarmInstance finalInstance = instance;
            final Alarm finalNewAlarm = newAlarm;

            AppExecutors.getMainThread().post(() -> {
                if (showSnackbar && finalInstance != null && mSnackbarAnchor != null) {
                    LogUtils.v("Alarm created: " + finalInstance);
                    AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, mFont, finalInstance.getAlarmTime().getTimeInMillis());
                }

                if (listener != null && finalNewAlarm != null) {
                    listener.onAlarmSaved(finalNewAlarm);
                }
            });
        });
    }

    /**
     * Modifies an alarm on the background, and optionally show a toast when done.
     *
     * @param alarm       The alarm to be modified.
     * @param popToast    whether a toast should be displayed when done.
     * @param minorUpdate if true, don't affect any currently snoozed instances.
     */
    public void asyncUpdateAlarm(@NonNull Alarm alarm, boolean popToast, boolean minorUpdate) {
        AppExecutors.getDiskIO().execute(() -> {
            ContentResolver cr = mAppContext.getContentResolver();

            // Update alarm
            alarm.updateAlarm(cr);

            if (minorUpdate) {
                // Just update the instance in the database and update notifications.
                // Display a toast message for newly created alarms if the user took the opportunity to edit minor fields.
                final List<AlarmInstance> instanceList = AlarmInstance.getInstancesByAlarmId(cr, alarm.id);

                Long tempTime = null;

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
                    String languageCode = SettingsDAO.getLanguageCode(mPrefs);
                    int globalIntentId = SettingsDAO.getGlobalIntentId(mPrefs);

                    AlarmNotifications.updateNotification(mAppContext, newInstance, languageCode, globalIntentId);

                    if (popToast && tempTime == null) {
                        tempTime = newInstance.getAlarmTime().getTimeInMillis();
                    }
                }

                if (popToast && tempTime != null && mSnackbarAnchor != null) {
                    final Long timeToDisplay = tempTime;
                    AppExecutors.getMainThread().post(() ->
                        AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, mFont, timeToDisplay)
                    );
                }

                return;
            }

            // Otherwise, this is a major update and we're going to re-create the alarm.
            AlarmStateManager.deleteAllInstances(mAppContext, mPrefs, alarm.id);

            final AlarmInstance finalInstance = alarm.enabled ? setupAlarmInstance(alarm) : null;
            Long tempTime = null;

            if (popToast && finalInstance != null) {
                if (mSyncToastLabel != null) {
                    String labelToSearch = mSyncToastLabel;
                    mSyncToastLabel = null;
                    AlarmInstance next = AlarmInstance.getNextAlarmInstanceByLabel(cr, labelToSearch);
                    if (next != null) {
                        tempTime = next.getAlarmTime().getTimeInMillis();
                    }
                } else {
                    tempTime = finalInstance.getAlarmTime().getTimeInMillis();
                }
            }

            final Long timeToDisplay = tempTime;

            if (timeToDisplay != null && mSnackbarAnchor != null) {
                AppExecutors.getMainThread().post(() ->
                    AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, mFont, timeToDisplay)
                );
            }

        });
    }

    /**
     * Deletes an alarm on the background.
     *
     * @param alarm The alarm to be deleted.
     */
    public void asyncDeleteAlarm(@Nullable Alarm alarm) {
        AppExecutors.getDiskIO().execute(() -> {
            // Activity may be closed at this point , make sure data is still valid
            if (alarm == null) {
                // Nothing to do here, just return.
                return;
            }
            AlarmStateManager.deleteAllInstances(mAppContext, mPrefs, alarm.id);
            final boolean deleted = Alarm.deleteAlarm(mAppContext.getContentResolver(), alarm.id);

            AppExecutors.getMainThread().post(() -> {
                if (deleted) {
                    mDeletedAlarm = alarm;
                    showUndoBar();
                }
            });
        });
    }

    /**
     * Synchronizes the enabled state of all alarms sharing the same label and
     * synchronization setting as the given source alarm.
     *
     * @param sourceAlarm the alarm whose label and sync settings define the group
     * @param newState    the enabled state to apply to all matching alarms
     */
    public void asyncSyncAlarmsWithSameLabel(@NonNull Alarm sourceAlarm, boolean newState) {
        if (sourceAlarm.label == null || sourceAlarm.label.trim().isEmpty()) {
            // No label: nothing to synchronize
            return;
        }

        AppExecutors.getDiskIO().execute(() -> {
            ContentResolver cr = mAppContext.getContentResolver();
            List<Alarm> alarms = Alarm.getAlarms(cr, null);

            for (Alarm alarm : alarms) {
                if (alarm.id != sourceAlarm.id
                    && sourceAlarm.label.equals(alarm.label)
                    && sourceAlarm.syncByLabel == alarm.syncByLabel) {

                    if (alarm.enabled != newState) {
                        alarm.enabled = newState;

                        alarm.fixDateIfPast();

                        // We reuse the existing method to update the DB and reschedule timers
                        asyncUpdateAlarm(alarm, false, false);
                        LogUtils.d("Sync alarm " + alarm.id + " with label " + alarm.label);
                    }
                }
            }
        });
    }

    /**
     * Instructs the next alarm update operation to display a toast based on the earliest upcoming
     * alarm instance that shares the specified label, rather than the specific instance being updated.
     *
     * <p>This label is consumed once during the next call to {@code asyncUpdateAlarm()}
     * where {@code popToast} is {@code true}. It is used when enabling a group of synchronized
     * alarms to ensure that only one toast is shown, corresponding to the earliest upcoming alarm
     * within that specific synchronized group.</p>
     *
     * @param label the label of the synchronized alarm group to calculate the next upcoming time for
     */
    public void useSyncToastForLabel(@NonNull String label) {
        mSyncToastLabel = label;
    }

    /**
     * Hides any undo toast.
     */
    public void hideUndoBar() {
        mDeletedAlarm = null;
        SnackbarManager.dismiss();
    }

    private void showUndoBar() {
        if (mSnackbarAnchor == null) {
            return;
        }

        final Alarm alarmBeingDeleted = mDeletedAlarm;
        final AtomicBoolean isUndone = new AtomicBoolean(false);

        final Context localizedContext = Utils.getLocalizedContext(mAppContext, SettingsDAO.getLanguageCode(mPrefs));
        final Snackbar snackbar = Snackbar.make(mSnackbarAnchor, localizedContext.getString(R.string.alarm_deleted),
            Snackbar.LENGTH_LONG).setAction(R.string.alarm_undo, v -> {
            isUndone.set(true);

            if (mDeletedAlarm != null) {
                Utils.performHapticFeedback(v, mIsVibrationsEnabled, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                final Alarm alarmToRestore = mDeletedAlarm;

                mDeletedAlarm = null;

                asyncAddAlarm(alarmToRestore);
            }
        });

        // Remove the alarm background image if the alarm is deleted and not restored using the Undo button.
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                // Permanently delete the background image of the deleted alarm if the user does not click the Undo button.
                if (!isUndone.get()) {
                    if (alarmBeingDeleted != null && !TextUtils.isEmpty(alarmBeingDeleted.backgroundImage)) {
                        final String imagePath = alarmBeingDeleted.backgroundImage;

                        // Delete the file in the background to avoid blocking the interface.
                        AppExecutors.getDiskIO().execute(() -> {
                            FileUtils.clearFile(imagePath);
                            LogUtils.i("Background image file permanently deleted : " + imagePath);
                        });
                    }
                }
            }
        });

        SnackbarManager.show(snackbar, mFont);
    }

    @NonNull
    private AlarmInstance setupAlarmInstance(@NonNull Alarm alarm) {
        final ContentResolver cr = mAppContext.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        newInstance.addInstance(cr);
        // Register instance to state manager
        AlarmStateManager.registerInstance(mAppContext, mPrefs, newInstance, true);
        return newInstance;
    }

    /**
     * Callback interface used to listen for the completion of an alarm save operation.
     */
    public interface OnAlarmSavedListener {

        /**
         * Invoked when the alarm has been successfully saved to the database.
         *
         * @param savedAlarm The newly saved alarm, including its generated database ID.
         */
        void onAlarmSaved(@NonNull Alarm savedAlarm);
    }

}
