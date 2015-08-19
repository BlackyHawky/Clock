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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.android.deskclock.AlarmUtils;
import com.android.deskclock.R;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.ActionableToastBar;

import java.util.Calendar;

/**
 * API for asynchronously mutating a single alarm.
 */
public final class AlarmUpdateHandler implements View.OnTouchListener {

    private final Context mAppContext;
    private final ScrollHandler mScrollHandler;
    private final ViewGroup mUndoFrame;

    // For undo
    private Alarm mDeletedAlarm;
    private ActionableToastBar mUndoBar;

    public AlarmUpdateHandler(Context context, ScrollHandler scrollHandler, ViewGroup undoFrame) {
        mAppContext = context.getApplicationContext();
        mScrollHandler = scrollHandler;
        mUndoFrame = undoFrame;
        mUndoFrame.setOnTouchListener(this);
        mUndoBar = (ActionableToastBar) mUndoFrame.findViewById(R.id.undo_bar);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        hideUndoBar(true, event);
        return false;
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
                            AlarmUtils.popAlarmSetToast(
                                    mAppContext, instance.getAlarmTime().getTimeInMillis());
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

                        if (minorUpdate) {
                            // For minor updates, don't affect any currently snoozed instances.
                            AlarmStateManager.deleteNonSnoozeInstances(mAppContext, alarm.id);
                        } else {
                            AlarmStateManager.deleteAllInstances(mAppContext, alarm.id);
                        }

                        // Update alarm
                        Alarm.updateAlarm(cr, alarm);
                        if (alarm.enabled) {
                            return setupAlarmInstance(alarm);
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(AlarmInstance instance) {
                        if (popToast && instance != null) {
                            AlarmUtils.popAlarmSetToast(
                                    mAppContext, instance.getAlarmTime().getTimeInMillis());
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
     * Hides any undo toast.
     */
    public void hideUndoBar(boolean animate, MotionEvent event) {
        if (mUndoBar != null) {
            mUndoFrame.setVisibility(View.GONE);
            if (event != null && mUndoBar.isEventInToastBar(event)) {
                // Avoid touches inside the undo bar.
                return;
            }
            mUndoBar.hide(animate);
        }
        mDeletedAlarm = null;
    }

    private void showUndoBar() {
        final Alarm deletedAlarm = mDeletedAlarm;
        mUndoFrame.setVisibility(View.VISIBLE);
        mUndoBar.show(new ActionableToastBar.ActionClickedListener() {
                          @Override
                          public void onActionClicked() {
                              mDeletedAlarm = null;
                              asyncAddAlarm(deletedAlarm);
                          }
                      }, 0, mAppContext.getString(R.string.alarm_deleted),
                true, R.string.alarm_undo, true);
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
