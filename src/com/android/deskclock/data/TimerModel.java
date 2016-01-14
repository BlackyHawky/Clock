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

package com.android.deskclock.data;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.ArraySet;

import com.android.deskclock.AlarmAlertWakeLock;
import com.android.deskclock.HandleDeskClockApiCalls;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;
import com.android.deskclock.settings.SettingsActivity;
import com.android.deskclock.timer.ExpiredTimersActivity;
import com.android.deskclock.timer.TimerKlaxon;
import com.android.deskclock.timer.TimerService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP;
import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.android.deskclock.data.Timer.State.EXPIRED;
import static com.android.deskclock.data.Timer.State.RESET;

/**
 * All {@link Timer} data is accessed via this model.
 */
final class TimerModel {

    private final Context mContext;

    /** The alarm manager system service that calls back when timers expire. */
    private final AlarmManager mAlarmManager;

    /** The model from which settings are fetched. */
    private final SettingsModel mSettingsModel;

    /** The model from which notification data are fetched. */
    private final NotificationModel mNotificationModel;

    /** Used to create and destroy system notifications related to timers. */
    private final NotificationManagerCompat mNotificationManager;

    /** Update timer notification when locale changes. */
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /**
     * Retain a hard reference to the shared preference observer to prevent it from being garbage
     * collected. See {@link SharedPreferences#registerOnSharedPreferenceChangeListener} for detail.
     */
    private final OnSharedPreferenceChangeListener mPreferenceListener = new PreferenceListener();

    /** The listeners to notify when a timer is added, updated or removed. */
    private final List<TimerListener> mTimerListeners = new ArrayList<>();

    /**
     * The ids of expired timers for which the ringer is ringing. Not all expired timers have their
     * ids in this collection. If a timer was already expired when the app was started its id will
     * be absent from this collection.
     */
    private final Set<Integer> mRingingIds = new ArraySet<>();

    /** The uri of the ringtone to play for timers. */
    private Uri mTimerRingtoneUri;

    /** The title of the ringtone to play for timers. */
    private String mTimerRingtoneTitle;

    /** A mutable copy of the timers. */
    private List<Timer> mTimers;

    /** A mutable copy of the expired timers. */
    private List<Timer> mExpiredTimers;

    /**
     * The service that keeps this application in the foreground while a heads-up timer
     * notification is displayed. Marking the service as foreground prevents the operating system
     * from killing this application while expired timers are actively firing.
     */
    private Service mService;

    TimerModel(Context context, SettingsModel settingsModel, NotificationModel notificationModel) {
        mContext = context;
        mSettingsModel = settingsModel;
        mNotificationModel = notificationModel;
        mNotificationManager = NotificationManagerCompat.from(context);

        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        // Clear caches affected by preferences when preferences change.
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.registerOnSharedPreferenceChangeListener(mPreferenceListener);

        // Update stopwatch notification when locale changes.
        final IntentFilter localeBroadcastFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter);
    }

    /**
     * @param timerListener to be notified when timers are added, updated and removed
     */
    void addTimerListener(TimerListener timerListener) {
        mTimerListeners.add(timerListener);
    }

    /**
     * @param timerListener to no longer be notified when timers are added, updated and removed
     */
    void removeTimerListener(TimerListener timerListener) {
        mTimerListeners.remove(timerListener);
    }

    /**
     * @return all defined timers in their creation order
     */
    List<Timer> getTimers() {
        return Collections.unmodifiableList(getMutableTimers());
    }

    /**
     * @return all expired timers in their expiration order
     */
    List<Timer> getExpiredTimers() {
        return Collections.unmodifiableList(getMutableExpiredTimers());
    }

    /**
     * @param timerId identifies the timer to return
     * @return the timer with the given {@code timerId}
     */
    Timer getTimer(int timerId) {
        for (Timer timer : getMutableTimers()) {
            if (timer.getId() == timerId) {
                return timer;
            }
        }

        return null;
    }

    /**
     * @return the timer that last expired and is still expired now; {@code null} if no timers are
     *      expired
     */
    Timer getMostRecentExpiredTimer() {
        final List<Timer> timers = getMutableExpiredTimers();
        return timers.isEmpty() ? null : timers.get(timers.size() - 1);
    }

    /**
     * @param length the length of the timer in milliseconds
     * @param label describes the purpose of the timer
     * @param deleteAfterUse {@code true} indicates the timer should be deleted when it is reset
     * @return the newly added timer
     */
    Timer addTimer(long length, String label, boolean deleteAfterUse) {
        // Create the timer instance.
        Timer timer = new Timer(-1, RESET, length, length, Long.MIN_VALUE, length, label,
                deleteAfterUse);

        // Add the timer to permanent storage.
        timer = TimerDAO.addTimer(mContext, timer);

        // Add the timer to the cache.
        getMutableTimers().add(0, timer);

        // Update the timer notification.
        updateNotification();
        // Heads-Up notification is unaffected by this change

        // Notify listeners of the change.
        for (TimerListener timerListener : mTimerListeners) {
            timerListener.timerAdded(timer);
        }

        return timer;
    }

    /**
     * @param service used to start foreground notifications related to expired timers
     * @param timer the timer to be expired
     */
    void expireTimer(Service service, Timer timer) {
        if (mService == null) {
            // If this is the first expired timer, retain the service that will be used to start
            // the heads-up notification in the foreground.
            mService = service;
        } else if (mService != service) {
            // If this is not the first expired timer, the service should match the one given when
            // the first timer expired.
            LogUtils.wtf("Expected TimerServices to be identical");
        }

        updateTimer(timer.expire());
    }

    /**
     * @param timer an updated timer to store
     */
    void updateTimer(Timer timer) {
        final Timer before = doUpdateTimer(timer);

        // Update the notification after updating the timer data.
        updateNotification();

        // If the timer started or stopped being expired, update the heads-up notification.
        if (before.getState() != timer.getState()) {
            if (before.isExpired() || timer.isExpired()) {
                updateHeadsUpNotification();
            }
        }
    }

    /**
     * @param timer an existing timer to be removed
     */
    void removeTimer(Timer timer) {
        doRemoveTimer(timer);

        // Update the timer notifications after removing the timer data.
        updateNotification();
        if (timer.isExpired()) {
            updateHeadsUpNotification();
        }
    }

    /**
     * If the given {@code timer} is expired and marked for deletion after use then this method
     * removes the the timer. The timer is otherwise transitioned to the reset state and continues
     * to exist.
     *
     * @param timer the timer to be reset
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    void resetOrDeleteTimer(Timer timer, @StringRes int eventLabelId) {
        doResetOrDeleteTimer(timer, eventLabelId);

        // Update the notification after updating the timer data.
        updateNotification();

        // If the timer stopped being expired, update the heads-up notification.
        if (timer.isExpired()) {
            updateHeadsUpNotification();
        }
    }

    /**
     * Reset all timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    void resetTimers(@StringRes int eventLabelId) {
        final List<Timer> timers = new ArrayList<>(getTimers());
        for (Timer timer : timers) {
            doResetOrDeleteTimer(timer, eventLabelId);
        }

        // Update the notifications once after all timers are reset.
        updateNotification();
        updateHeadsUpNotification();
    }

    /**
     * Reset all expired timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    void resetExpiredTimers(@StringRes int eventLabelId) {
        final List<Timer> timers = new ArrayList<>(getTimers());
        for (Timer timer : timers) {
            if (timer.isExpired()) {
                doResetOrDeleteTimer(timer, eventLabelId);
            }
        }

        // Update the notifications once after all timers are updated.
        updateNotification();
        updateHeadsUpNotification();
    }

    /**
     * Reset all unexpired timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    void resetUnexpiredTimers(@StringRes int eventLabelId) {
        final List<Timer> timers = new ArrayList<>(getTimers());
        for (Timer timer : timers) {
            if (timer.isRunning() || timer.isPaused()) {
                doResetOrDeleteTimer(timer, eventLabelId);
            }
        }

        // Update the notification once after all timers are updated.
        updateNotification();
        // Heads-Up notification is unaffected by this change
    }

    /**
     * @return the uri of the default ringtone to play for all timers when no user selection exists
     */
    Uri getDefaultTimerRingtoneUri() {
        return mSettingsModel.getDefaultTimerRingtoneUri();
    }

    /**
     * @return {@code true} iff the ringtone to play for all timers is the silent ringtone
     */
    boolean isTimerRingtoneSilent() {
        return Uri.EMPTY.equals(getTimerRingtoneUri());
    }

    /**
     * @return the uri of the ringtone to play for all timers
     */
    Uri getTimerRingtoneUri() {
        if (mTimerRingtoneUri == null) {
            mTimerRingtoneUri = mSettingsModel.getTimerRingtoneUri();
        }

        return mTimerRingtoneUri;
    }

    /**
     * @return the title of the ringtone that is played for all timers
     */
    String getTimerRingtoneTitle() {
        if (mTimerRingtoneTitle == null) {
            if (isTimerRingtoneSilent()) {
                // Special case: no ringtone has a title of "Silent".
                mTimerRingtoneTitle = mContext.getString(R.string.silent_timer_ringtone_title);
            } else {
                final Uri defaultUri = getDefaultTimerRingtoneUri();
                final Uri uri = getTimerRingtoneUri();

                if (defaultUri.equals(uri)) {
                    // Special case: default ringtone has a title of "Timer Expired".
                    mTimerRingtoneTitle = mContext.getString(R.string.default_timer_ringtone_title);
                } else {
                    final Ringtone ringtone = RingtoneManager.getRingtone(mContext, uri);
                    mTimerRingtoneTitle = ringtone.getTitle(mContext);
                }
            }
        }

        return mTimerRingtoneTitle;
    }

    private List<Timer> getMutableTimers() {
        if (mTimers == null) {
            mTimers = TimerDAO.getTimers(mContext);
            Collections.sort(mTimers, Timer.ID_COMPARATOR);
        }

        return mTimers;
    }

    private List<Timer> getMutableExpiredTimers() {
        if (mExpiredTimers == null) {
            mExpiredTimers = new ArrayList<>();

            for (Timer timer : getMutableTimers()) {
                if (timer.isExpired()) {
                    mExpiredTimers.add(timer);
                }
            }
            Collections.sort(mExpiredTimers, Timer.EXPIRY_COMPARATOR);
        }

        return mExpiredTimers;
    }

    /**
     * This method updates timer data without updating notifications. This is useful in bulk-update
     * scenarios so the notifications are only rebuilt once.
     *
     * @param timer an updated timer to store
     * @return the state of the timer prior to the update
     */
    private Timer doUpdateTimer(Timer timer) {
        // Retrieve the cached form of the timer.
        final List<Timer> timers = getMutableTimers();
        final int index = timers.indexOf(timer);
        final Timer before = timers.get(index);

        // If no change occurred, ignore this update.
        if (timer == before) {
            return timer;
        }

        // Update the timer in permanent storage.
        TimerDAO.updateTimer(mContext, timer);

        // Update the timer in the cache.
        final Timer oldTimer = timers.set(index, timer);

        // Clear the cache of expired timers if the timer changed to/from expired.
        if (before.isExpired() || timer.isExpired()) {
            mExpiredTimers = null;
        }

        // Update the timer expiration callback.
        updateAlarmManager();

        // Update the timer ringer.
        updateRinger(before, timer);

        // Notify listeners of the change.
        for (TimerListener timerListener : mTimerListeners) {
            timerListener.timerUpdated(before, timer);
        }

        return oldTimer;
    }

    /**
     * This method removes timer data without updating notifications. This is useful in bulk-remove
     * scenarios so the notifications are only rebuilt once.
     *
     * @param timer an existing timer to be removed
     */
    void doRemoveTimer(Timer timer) {
        // Remove the timer from permanent storage.
        TimerDAO.removeTimer(mContext, timer);

        // Remove the timer from the cache.
        final List<Timer> timers = getMutableTimers();
        final int index = timers.indexOf(timer);

        // If the timer cannot be located there is nothing to remove.
        if (index == -1) {
            return;
        }

        timer = timers.remove(index);

        // Clear the cache of expired timers if a new expired timer was added.
        if (timer.isExpired()) {
            mExpiredTimers = null;
        }

        // Update the timer expiration callback.
        updateAlarmManager();

        // Update the timer ringer.
        updateRinger(timer, null);

        // Notify listeners of the change.
        for (TimerListener timerListener : mTimerListeners) {
            timerListener.timerRemoved(timer);
        }
    }

    /**
     * This method updates/removes timer data without updating notifications. This is useful in
     * bulk-update scenarios so the notifications are only rebuilt once.
     *
     * If the given {@code timer} is expired and marked for deletion after use then this method
     * removes the the timer. The timer is otherwise transitioned to the reset state and continues
     * to exist.
     *
     * @param timer the timer to be reset
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    private void doResetOrDeleteTimer(Timer timer, @StringRes int eventLabelId) {
        if (timer.isExpired() && timer.getDeleteAfterUse()) {
            doRemoveTimer(timer);
            if (eventLabelId != 0) {
                Events.sendTimerEvent(R.string.action_delete, eventLabelId);
            }
        } else if (!timer.isReset()) {
            doUpdateTimer(timer.reset());
            if (eventLabelId != 0) {
                Events.sendTimerEvent(R.string.action_reset, eventLabelId);
            }
        }
    }

    /**
     * Updates the callback given to this application from the {@link AlarmManager} that signals the
     * expiration of the next timer. If no timers are currently set to expire (i.e. no running
     * timers exist) then this method clears the expiration callback from AlarmManager.
     */
    private void updateAlarmManager() {
        // Locate the next firing timer if one exists.
        Timer nextExpiringTimer = null;
        for (Timer timer : getMutableTimers()) {
            if (timer.isRunning()) {
                if (nextExpiringTimer == null) {
                    nextExpiringTimer = timer;
                } else if (timer.getExpirationTime() < nextExpiringTimer.getExpirationTime()) {
                    nextExpiringTimer = timer;
                }
            }
        }

        // Build the intent that signals the timer expiration.
        final Intent intent = TimerService.createTimerExpiredIntent(mContext, nextExpiringTimer);

        if (nextExpiringTimer == null) {
            // Cancel the existing timer expiration callback.
            final PendingIntent pi = PendingIntent.getService(mContext,
                    0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_NO_CREATE);
            if (pi != null) {
                mAlarmManager.cancel(pi);
                pi.cancel();
            }
        } else {
            // Update the existing timer expiration callback.
            final PendingIntent pi = PendingIntent.getService(mContext,
                    0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
            schedulePendingIntent(nextExpiringTimer.getExpirationTime(), pi);
        }
    }

    /**
     * Starts and stops the ringer for timers if the change to the timer demands it.
     *
     * @param before the state of the timer before the change; {@code null} indicates added
     * @param after the state of the timer after the change; {@code null} indicates delete
     */
    private void updateRinger(Timer before, Timer after) {
        // Retrieve the states before and after the change.
        final Timer.State beforeState = before == null ? null : before.getState();
        final Timer.State afterState = after == null ? null : after.getState();

        // If the timer state did not change, the ringer state is unchanged.
        if (beforeState == afterState) {
            return;
        }

        // If the timer is the first to expire, start ringing.
        if (afterState == EXPIRED && mRingingIds.add(after.getId()) && mRingingIds.size() == 1) {
            AlarmAlertWakeLock.acquireScreenCpuWakeLock(mContext);
            TimerKlaxon.start(mContext);
        }

        // If the expired timer was the last to reset, stop ringing.
        if (beforeState == EXPIRED && mRingingIds.remove(before.getId()) && mRingingIds.isEmpty()) {
            TimerKlaxon.stop(mContext);
            AlarmAlertWakeLock.releaseCpuLock();
        }
    }

    /**
     * Updates the notification controlling unexpired timers. This notification is only displayed
     * when the application is not open.
     */
    void updateNotification() {
        // Notifications should be hidden if the app is open.
        if (mNotificationModel.isApplicationInForeground()) {
            mNotificationManager.cancel(mNotificationModel.getUnexpiredTimerNotificationId());
            return;
        }

        // Filter the timers to just include unexpired ones.
        final List<Timer> unexpired = new ArrayList<>();
        for (Timer timer : getMutableTimers()) {
            if (timer.isRunning() || timer.isPaused()) {
                unexpired.add(timer);
            }
        }

        // If no unexpired timers exist, cancel the notification.
        if (unexpired.isEmpty()) {
            mNotificationManager.cancel(mNotificationModel.getUnexpiredTimerNotificationId());
            return;
        }

        // Sort the unexpired timers to locate the next one scheduled to expire.
        Collections.sort(unexpired, Timer.EXPIRY_COMPARATOR);
        final Timer timer = unexpired.get(0);
        final long remainingTime = timer.getRemainingTime();

        // Generate some descriptive text, a title, and some actions based on timer states.
        final String contentText;
        final String contentTitle;
        @DrawableRes int firstActionIconId, secondActionIconId = 0;
        @StringRes int firstActionTitleId, secondActionTitleId = 0;
        Intent firstActionIntent, secondActionIntent = null;

        if (unexpired.size() == 1) {
            contentText = formatElapsedTimeUntilExpiry(remainingTime);

            if (timer.isRunning()) {
                // Single timer is running.
                if (TextUtils.isEmpty(timer.getLabel())) {
                    contentTitle = mContext.getString(R.string.timer_notification_label);
                } else {
                    contentTitle = timer.getLabel();
                }

                firstActionIconId = R.drawable.ic_pause_24dp;
                firstActionTitleId = R.string.timer_pause;
                firstActionIntent = new Intent(mContext, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_PAUSE_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());

                secondActionIconId = R.drawable.ic_add_24dp;
                secondActionTitleId = R.string.timer_plus_1_min;
                secondActionIntent = new Intent(mContext, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_ADD_MINUTE_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());
            } else {
                // Single timer is paused.
                contentTitle = mContext.getString(R.string.timer_paused);

                firstActionIconId = R.drawable.ic_start_24dp;
                firstActionTitleId = R.string.sw_resume_button;
                firstActionIntent = new Intent(mContext, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_START_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());

                secondActionIconId = R.drawable.ic_reset_24dp;
                secondActionTitleId = R.string.sw_reset_button;
                secondActionIntent = new Intent(mContext, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_RESET_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());
            }
        } else {
            if (timer.isRunning()) {
                // At least one timer is running.
                final String timeRemaining = formatElapsedTimeUntilExpiry(remainingTime);
                contentText = mContext.getString(R.string.next_timer_notif, timeRemaining);
                contentTitle = mContext.getString(R.string.timers_in_use, unexpired.size());
            } else {
                // All timers are paused.
                contentText = mContext.getString(R.string.all_timers_stopped_notif);
                contentTitle = mContext.getString(R.string.timers_stopped, unexpired.size());
            }

            firstActionIconId = R.drawable.ic_reset_24dp;
            firstActionTitleId = R.string.timer_reset_all;
            firstActionIntent = TimerService.createResetUnexpiredTimersIntent(mContext);
        }

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(mContext, HandleDeskClockApiCalls.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(HandleDeskClockApiCalls.ACTION_SHOW_TIMERS)
                .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId())
                .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = PendingIntent.getActivity(mContext, 0, showApp,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setOngoing(true)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setContentText(contentText)
                .setContentTitle(contentTitle)
                .setContentIntent(pendingShowApp)
                .setSmallIcon(R.drawable.stat_notify_timer)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        final PendingIntent firstAction = PendingIntent.getService(mContext, 0,
                firstActionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        final String firstActionTitle = mContext.getString(firstActionTitleId);
        builder.addAction(firstActionIconId, firstActionTitle, firstAction);

        if (secondActionIntent != null) {
            final PendingIntent secondAction = PendingIntent.getService(mContext, 0,
                    secondActionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            final String secondActionTitle = mContext.getString(secondActionTitleId);
            builder.addAction(secondActionIconId, secondActionTitle, secondAction);
        }

        // Update the notification.
        final Notification notification = builder.build();
        final int notificationId = mNotificationModel.getUnexpiredTimerNotificationId();
        mNotificationManager.notify(notificationId, notification);

        final Intent updateNotification = TimerService.createUpdateNotificationIntent(mContext);
        if (timer.isRunning() && remainingTime > MINUTE_IN_MILLIS) {
            // Schedule a callback to update the time-sensitive information of the running timer.
            final PendingIntent pi = PendingIntent.getService(mContext, 0, updateNotification,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

            final long nextMinuteChange = remainingTime % MINUTE_IN_MILLIS;
            final long triggerTime = SystemClock.elapsedRealtime() + nextMinuteChange;

            schedulePendingIntent(triggerTime, pi);
        } else {
            // Cancel the update notification callback.
            final PendingIntent pi = PendingIntent.getService(mContext, 0, updateNotification,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_NO_CREATE);
            if (pi != null) {
                mAlarmManager.cancel(pi);
                pi.cancel();
            }
        }
    }

    /**
     * Updates the heads-up notification controlling expired timers. This heads-up notification is
     * displayed whether the application is open or not.
     */
    private void updateHeadsUpNotification() {
        // Nothing can be done with the heads-up notification without a valid service reference.
        if (mService == null) {
            return;
        }

        final List<Timer> expired = getExpiredTimers();

        // If no expired timers exist, stop the service (which cancels the foreground notification).
        if (expired.isEmpty()) {
            mService.stopSelf();
            mService = null;
            return;
        }

        // Generate some descriptive text, a title, and an action name based on the timer count.
        final int timerId;
        final String contentText;
        final String contentTitle;
        final String resetActionTitle;
        if (expired.size() > 1) {
            timerId = -1;
            contentText = mContext.getString(R.string.timer_multi_times_up, expired.size());
            contentTitle = mContext.getString(R.string.timer_notification_label);
            resetActionTitle = mContext.getString(R.string.timer_stop_all);
        } else {
            final Timer timer = expired.get(0);
            timerId = timer.getId();
            resetActionTitle = mContext.getString(R.string.timer_stop);
            contentText = mContext.getString(R.string.timer_times_up);

            final String label = timer.getLabel();
            if (TextUtils.isEmpty(label)) {
                contentTitle = mContext.getString(R.string.timer_notification_label);
            } else {
                contentTitle = label;
            }
        }

        // Content intent shows the timer full screen when clicked.
        final Intent content = new Intent(mContext, ExpiredTimersActivity.class);
        final PendingIntent pendingContent = PendingIntent.getActivity(mContext, 0, content,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Full screen intent has flags so it is different than the content intent.
        final Intent fullScreen = new Intent(mContext, ExpiredTimersActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        final PendingIntent pendingFullScreen = PendingIntent.getActivity(mContext, 0, fullScreen,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // First action intent is either reset single timer or reset all timers.
        final Intent reset = TimerService.createResetExpiredTimersIntent(mContext);
        final PendingIntent pendingReset = PendingIntent.getService(mContext, 0, reset,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setWhen(0)
                .setOngoing(true)
                .setLocalOnly(true)
                .setAutoCancel(false)
                .setContentText(contentText)
                .setContentTitle(contentTitle)
                .setContentIntent(pendingContent)
                .setSmallIcon(R.drawable.stat_notify_timer)
                .setFullScreenIntent(pendingFullScreen, true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .addAction(R.drawable.ic_stop_24dp, resetActionTitle, pendingReset);

        // Add a second action if only a single timer is expired.
        if (expired.size() == 1) {
            // Second action intent adds a minute to a single timer.
            final Intent addMinute = TimerService.createAddMinuteTimerIntent(mContext, timerId);
            final PendingIntent pendingAddMinute = PendingIntent.getService(mContext, 0, addMinute,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            final String addMinuteTitle = mContext.getString(R.string.timer_plus_1_min);
            builder.addAction(R.drawable.ic_add_24dp, addMinuteTitle, pendingAddMinute);
        }

        // Update the notification.
        final Notification notification = builder.build();
        final int notificationId = mNotificationModel.getExpiredTimerNotificationId();
        mService.startForeground(notificationId, notification);
    }

    /**
     * Format "7 hours 52 minutes remaining"
     */
    @VisibleForTesting
    String formatElapsedTimeUntilExpiry(long remainingTime) {
        final int hours = (int) remainingTime / (int) HOUR_IN_MILLIS;
        final int minutes = (int) remainingTime / ((int) MINUTE_IN_MILLIS) % 60;

        String minSeq = Utils.getNumberFormattedQuantityString(mContext, R.plurals.minutes, minutes);
        String hourSeq = Utils.getNumberFormattedQuantityString(mContext, R.plurals.hours, hours);

        // The verb "remaining" may have to change tense for singular subjects in some languages.
        final String verb = mContext.getString((minutes > 1 || hours > 1)
                ? R.string.timer_remaining_multiple
                : R.string.timer_remaining_single);

        final boolean showHours = hours > 0;
        final boolean showMinutes = minutes > 0;

        int formatStringId;
        if (showHours) {
            if (showMinutes) {
                formatStringId = R.string.timer_notifications_hours_minutes;
            } else {
                formatStringId = R.string.timer_notifications_hours;
            }
        } else if (showMinutes) {
            formatStringId = R.string.timer_notifications_minutes;
        } else {
            formatStringId = R.string.timer_notifications_less_min;
        }
        return String.format(mContext.getString(formatStringId), hourSeq, minSeq, verb);
    }

    private void schedulePendingIntent(long triggerTime, PendingIntent pi) {
        if (Utils.isMOrLater()) {
            // Make sure the timer fires when the device is in doze mode. The timer is not
            // guaranteed to fire at the requested time. It may be delayed up to 15 minutes.
            mAlarmManager.setExactAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP, triggerTime, pi);
        } else {
            mAlarmManager.setExact(ELAPSED_REALTIME_WAKEUP, triggerTime, pi);
        }
    }

    /**
     * Update the stopwatch notification in response to a locale change.
     */
    private final class LocaleChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNotification();
            updateHeadsUpNotification();
        }
    }

    /**
     * This receiver is notified when shared preferences change. Cached information built on
     * preferences must be cleared.
     */
    private final class PreferenceListener implements OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case SettingsActivity.KEY_TIMER_RINGTONE:
                    mTimerRingtoneUri = null;
                    mTimerRingtoneTitle = null;
                    break;
            }
        }
    }
}