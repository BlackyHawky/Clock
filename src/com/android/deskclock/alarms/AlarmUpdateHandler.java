/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.alarms;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;

import com.android.deskclock.AlarmUtils;
import com.android.deskclock.R;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.toast.SnackbarManager;

import java.util.Calendar;
import java.util.List;

/**
 * API for asynchronously mutating a single alarm.
 */
public final class AlarmUpdateHandler {

    private final Context mAppContext;
    private final ScrollHandler mScrollHandler;
    private final View mSnackbarAnchor;

    // For undo
    private Alarm mDeletedAlarm;

    public AlarmUpdateHandler(Context context, ScrollHandler scrollHandler,
            ViewGroup snackbarAnchor) {
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
        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {
                    @Override
                    protected AlarmInstance doInBackground(Void... parameters) {
                        if (alarm != null) {
                            Events.sendAlarmEvent(R.string.action_create, R.string.label_deskclock);
                            ContentResolver cr = mAppContext.getContentResolver();

                            // Add alarm to db
                            Alarm newAlarm = Alarm.addAlarm(cr, alarm);

                            // Be ready to scroll to this alarm on UI later.
                            mScrollHandler.setSmoothScrollStableId(newAlarm.id);

                            // Create and add instance to db
                            if (newAlarm.enabled) {
                                return setupAlarmInstance(newAlarm);
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(AlarmInstance instance) {
                        if (instance != null) {
                            AlarmUtils.popAlarmSetSnackbar(
                                    mSnackbarAnchor, instance.getAlarmTime().getTimeInMillis());
                        }
                    }
                };
        updateTask.execute();
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
        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {
                    @Override
                    protected AlarmInstance doInBackground(Void... parameters) {
                        Events.sendAlarmEvent(R.string.action_update, R.string.label_deskclock);
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
                            return null;
                        }
                        // Otherwise, this is a major update and we're going to re-create the alarm
                        AlarmStateManager.deleteAllInstances(mAppContext, alarm.id);

                        return alarm.enabled ? setupAlarmInstance(alarm) : null;
                    }

                    @Override
                    protected void onPostExecute(AlarmInstance instance) {
                        if (popToast && instance != null) {
                            AlarmUtils.popAlarmSetSnackbar(
                                    mSnackbarAnchor, instance.getAlarmTime().getTimeInMillis());
                        }
                    }
                };
        updateTask.execute();
    }

    /**
     * Deletes an alarm on the background.
     *
     * @param alarm The alarm to be deleted.
     */
    public void asyncDeleteAlarm(final Alarm alarm) {
        final AsyncTask<Void, Void, Boolean> deleteTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... parameters) {
                // Activity may be closed at this point , make sure data is still valid
                if (alarm == null) {
                    // Nothing to do here, just return.
                    return false;
                }
                Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
                AlarmStateManager.deleteAllInstances(mAppContext, alarm.id);
                return Alarm.deleteAlarm(mAppContext.getContentResolver(), alarm.id);
            }

            @Override
            protected void onPostExecute(Boolean deleted) {
                if (deleted) {
                    mDeletedAlarm = alarm;
                    showUndoBar();
                }
            }
        };
        deleteTask.execute();
    }

    /**
     * Show a toast when an alarm is predismissed.
     *
     * @param instance Instance being predismissed.
     */
    public void showPredismissToast(AlarmInstance instance) {
        final String time = DateFormat.getTimeFormat(mAppContext).format(
                instance.getAlarmTime().getTime());
        final String text = mAppContext.getString(R.string.alarm_is_dismissed, time);
        SnackbarManager.show(Snackbar.make(mSnackbarAnchor, text, Snackbar.LENGTH_SHORT));
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
                .setAction(R.string.alarm_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDeletedAlarm = null;
                        asyncAddAlarm(deletedAlarm);
                    }
                });
        SnackbarManager.show(snackbar);
    }

    private AlarmInstance setupAlarmInstance(Alarm alarm) {
        final ContentResolver cr = mAppContext.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        newInstance = AlarmInstance.addInstance(cr, newInstance);
        // Register instance to state manager
        AlarmStateManager.registerInstance(mAppContext, newInstance, true);
        return newInstance;
    }
}
