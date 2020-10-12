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

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationManagerCompat;
import android.util.ArraySet;

import com.android.deskclock.AlarmAlertWakeLock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;
import com.android.deskclock.settings.SettingsActivity;
import com.android.deskclock.timer.TimerKlaxon;
import com.android.deskclock.timer.TimerService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.android.deskclock.data.Timer.State.EXPIRED;
import static com.android.deskclock.data.Timer.State.RESET;

/**
 * All {@link Timer} data is accessed via this model.
 */
final class TimerModel {

    /**
     * Running timers less than this threshold are left running/expired; greater than this
     * threshold are considered missed.
     */
    private static final long MISSED_THRESHOLD = -MINUTE_IN_MILLIS;

    private final Context mContext;

    private final SharedPreferences mPrefs;

    /** The alarm manager system service that calls back when timers expire. */
    private final AlarmManager mAlarmManager;

    /** The model from which settings are fetched. */
    private final SettingsModel mSettingsModel;

    /** The model from which notification data are fetched. */
    private final NotificationModel mNotificationModel;

    /** The model from which ringtone data are fetched. */
    private final RingtoneModel mRingtoneModel;

    /** Used to create and destroy system notifications related to timers. */
    private final NotificationManagerCompat mNotificationManager;

    /** Update timer notification when locale changes. */
    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /**
     * Retain a hard reference to the shared preference observer to prevent it from being garbage
     * collected. See {@link SharedPreferences#registerOnSharedPreferenceChangeListener} for detail.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final OnSharedPreferenceChangeListener mPreferenceListener = new PreferenceListener();

    /** The listeners to notify when a timer is added, updated or removed. */
    private final List<TimerListener> mTimerListeners = new ArrayList<>();

    /** Delegate that builds platform-specific timer notifications. */
    private final TimerNotificationBuilder mNotificationBuilder = new TimerNotificationBuilder();

    /**
     * The ids of expired timers for which the ringer is ringing. Not all expired timers have their
     * ids in this collection. If a timer was already expired when the app was started its id will
     * be absent from this collection.
     */
    @SuppressLint("NewApi")
    private final Set<Integer> mRingingIds = new ArraySet<>();

    /** The uri of the ringtone to play for timers. */
    private Uri mTimerRingtoneUri;

    /** The title of the ringtone to play for timers. */
    private String mTimerRingtoneTitle;

    /** A mutable copy of the timers. */
    private List<Timer> mTimers;

    /** A mutable copy of the expired timers. */
    private List<Timer> mExpiredTimers;

    /** A mutable copy of the missed timers. */
    private List<Timer> mMissedTimers;

    /**
     * The service that keeps this application in the foreground while a heads-up timer
     * notification is displayed. Marking the service as foreground prevents the operating system
     * from killing this application while expired timers are actively firing.
     */
    private Service mService;

    TimerModel(Context context, SharedPreferences prefs, SettingsModel settingsModel,
            RingtoneModel ringtoneModel, NotificationModel notificationModel) {
        mContext = context;
        mPrefs = prefs;
        mSettingsModel = settingsModel;
        mRingtoneModel = ringtoneModel;
        mNotificationModel = notificationModel;
        mNotificationManager = NotificationManagerCompat.from(context);

        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        // Clear caches affected by preferences when preferences change.
        prefs.registerOnSharedPreferenceChangeListener(mPreferenceListener);

        // Update timer notification when locale changes.
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
     * @return all missed timers in their expiration order
     */
    private List<Timer> getMissedTimers() {
        return Collections.unmodifiableList(getMutableMissedTimers());
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
        Timer timer = new Timer(-1, RESET, length, length, Timer.UNUSED, Timer.UNUSED, length,
                label, deleteAfterUse);

        // Add the timer to permanent storage.
        timer = TimerDAO.addTimer(mPrefs, timer);

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
        if (timer.isExpired()) {
            updateHeadsUpNotification();
        } else {
            updateNotification();
        }
    }

    /**
     * If the given {@code timer} is expired and marked for deletion after use then this method
     * removes the the timer. The timer is otherwise transitioned to the reset state and continues
     * to exist.
     *
     * @param timer        the timer to be reset
     * @param allowDelete  {@code true} if the timer is allowed to be deleted instead of reset
     *                     (e.g. one use timers)
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     * @return the reset {@code timer} or {@code null} if the timer was deleted
     */
    Timer resetTimer(Timer timer, boolean allowDelete, @StringRes int eventLabelId) {
        final Timer result = doResetOrDeleteTimer(timer, allowDelete, eventLabelId);

        // Update the notification after updating the timer data.
        if (timer.isMissed()) {
            updateMissedNotification();
        } else if (timer.isExpired()) {
            updateHeadsUpNotification();
        } else {
            updateNotification();
        }

        return result;
    }

    /**
     * Update timers after system reboot.
     */
    void updateTimersAfterReboot() {
        final List<Timer> timers = new ArrayList<>(getTimers());
        for (Timer timer : timers) {
            doUpdateAfterRebootTimer(timer);
        }

        // Update the notifications once after all timers are updated.
        updateNotification();
        updateMissedNotification();
        updateHeadsUpNotification();
    }

    /**
     * Update timers after time set.
     */
    void updateTimersAfterTimeSet() {
        final List<Timer> timers = new ArrayList<>(getTimers());
        for (Timer timer : timers) {
            doUpdateAfterTimeSetTimer(timer);
        }

        // Update the notifications once after all timers are updated.
        updateNotification();
        updateMissedNotification();
        updateHeadsUpNotification();
    }

    /**
     * Reset all expired timers. Exactly one parameter should be filled, with preference given to
     * eventLabelId.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    void resetOrDeleteExpiredTimers(@StringRes int eventLabelId) {
        final List<Timer> timers = new ArrayList<>(getTimers());
        for (Timer timer : timers) {
            if (timer.isExpired()) {
                doResetOrDeleteTimer(timer, true /* allowDelete */, eventLabelId);
            }
        }

        // Update the notifications once after all timers are updated.
        updateHeadsUpNotification();
    }

    /**
     * Reset all missed timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    void resetMissedTimers(@StringRes int eventLabelId) {
        final List<Timer> timers = new ArrayList<>(getTimers());
        for (Timer timer : timers) {
            if (timer.isMissed()) {
                doResetOrDeleteTimer(timer, true /* allowDelete */, eventLabelId);
            }
        }

        // Update the notifications once after all timers are updated.
        updateMissedNotification();
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
                doResetOrDeleteTimer(timer, true /* allowDelete */, eventLabelId);
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
     * @param uri the uri of the ringtone to play for all timers
     */
    void setTimerRingtoneUri(Uri uri) {
        mSettingsModel.setTimerRingtoneUri(uri);
    }

    /**
     * @return the title of the ringtone that is played for all timers
     */
    String getTimerRingtoneTitle() {
        if (mTimerRingtoneTitle == null) {
            if (isTimerRingtoneSilent()) {
                // Special case: no ringtone has a title of "Silent".
                mTimerRingtoneTitle = mContext.getString(R.string.silent_ringtone_title);
            } else {
                final Uri defaultUri = getDefaultTimerRingtoneUri();
                final Uri uri = getTimerRingtoneUri();

                if (defaultUri.equals(uri)) {
                    // Special case: default ringtone has a title of "Timer Expired".
                    mTimerRingtoneTitle = mContext.getString(R.string.default_timer_ringtone_title);
                } else {
                    mTimerRingtoneTitle = mRingtoneModel.getRingtoneTitle(uri);
                }
            }
        }

        return mTimerRingtoneTitle;
    }

    /**
     * @return the duration, in milliseconds, of the crescendo to apply to timer ringtone playback;
     *      {@code 0} implies no crescendo should be applied
     */
    long getTimerCrescendoDuration() {
        return mSettingsModel.getTimerCrescendoDuration();
    }

    /**
     * @return {@code true} if the device vibrates when timers expire
     */
    boolean getTimerVibrate() {
        return mSettingsModel.getTimerVibrate();
    }

    /**
     * @param enabled {@code true} if the device should vibrate when timers expire
     */
    void setTimerVibrate(boolean enabled) {
        mSettingsModel.setTimerVibrate(enabled);
    }

    private List<Timer> getMutableTimers() {
        if (mTimers == null) {
            mTimers = TimerDAO.getTimers(mPrefs);
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

    private List<Timer> getMutableMissedTimers() {
        if (mMissedTimers == null) {
            mMissedTimers = new ArrayList<>();

            for (Timer timer : getMutableTimers()) {
                if (timer.isMissed()) {
                    mMissedTimers.add(timer);
                }
            }
            Collections.sort(mMissedTimers, Timer.EXPIRY_COMPARATOR);
        }

        return mMissedTimers;
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
        TimerDAO.updateTimer(mPrefs, timer);

        // Update the timer in the cache.
        final Timer oldTimer = timers.set(index, timer);

        // Clear the cache of expired timers if the timer changed to/from expired.
        if (before.isExpired() || timer.isExpired()) {
            mExpiredTimers = null;
        }
        // Clear the cache of missed timers if the timer changed to/from missed.
        if (before.isMissed() || timer.isMissed()) {
            mMissedTimers = null;
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
    private void doRemoveTimer(Timer timer) {
        // Remove the timer from permanent storage.
        TimerDAO.removeTimer(mPrefs, timer);

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

        // Clear the cache of missed timers if a new missed timer was added.
        if (timer.isMissed()) {
            mMissedTimers = null;
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
     * @param allowDelete  {@code true} if the timer is allowed to be deleted instead of reset
     *                     (e.g. one use timers)
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     * @return the reset {@code timer} or {@code null} if the timer was deleted
     */
    private Timer doResetOrDeleteTimer(Timer timer, boolean allowDelete,
            @StringRes int eventLabelId) {
        if (allowDelete
                && (timer.isExpired() || timer.isMissed())
                && timer.getDeleteAfterUse()) {
            doRemoveTimer(timer);
            if (eventLabelId != 0) {
                Events.sendTimerEvent(R.string.action_delete, eventLabelId);
            }
            return null;
        } else if (!timer.isReset()) {
            final Timer reset = timer.reset();
            doUpdateTimer(reset);
            if (eventLabelId != 0) {
                Events.sendTimerEvent(R.string.action_reset, eventLabelId);
            }
            return reset;
        }

        return timer;
    }

    /**
     * This method updates/removes timer data after a reboot without updating notifications.
     *
     * @param timer the timer to be updated
     */
    private void doUpdateAfterRebootTimer(Timer timer) {
        Timer updated = timer.updateAfterReboot();
        if (updated.getRemainingTime() < MISSED_THRESHOLD && updated.isRunning()) {
            updated = updated.miss();
        }
        doUpdateTimer(updated);
    }

    private void doUpdateAfterTimeSetTimer(Timer timer) {
        final Timer updated = timer.updateAfterTimeSet();
        doUpdateTimer(updated);
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
            schedulePendingIntent(mAlarmManager, nextExpiringTimer.getExpirationTime(), pi);
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

        // Otherwise build and post a notification reflecting the latest unexpired timers.
        final Notification notification =
                mNotificationBuilder.build(mContext, mNotificationModel, unexpired);
        final int notificationId = mNotificationModel.getUnexpiredTimerNotificationId();
        mNotificationManager.notify(notificationId, notification);
    }

    /**
     * Updates the notification controlling missed timers. This notification is only displayed when
     * the application is not open.
     */
    void updateMissedNotification() {
        // Notifications should be hidden if the app is open.
        if (mNotificationModel.isApplicationInForeground()) {
            mNotificationManager.cancel(mNotificationModel.getMissedTimerNotificationId());
            return;
        }

        final List<Timer> missed = getMissedTimers();

        if (missed.isEmpty()) {
            mNotificationManager.cancel(mNotificationModel.getMissedTimerNotificationId());
            return;
        }

        final Notification notification = mNotificationBuilder.buildMissed(mContext,
                mNotificationModel, missed);
        final int notificationId = mNotificationModel.getMissedTimerNotificationId();
        mNotificationManager.notify(notificationId, notification);
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

        // Otherwise build and post a foreground notification reflecting the latest expired timers.
        final Notification notification = mNotificationBuilder.buildHeadsUp(mContext, expired);
        final int notificationId = mNotificationModel.getExpiredTimerNotificationId();
        mService.startForeground(notificationId, notification);
    }

    /**
     * Update the timer notification in response to a locale change.
     */
    private final class LocaleChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTimerRingtoneTitle = null;
            updateNotification();
            updateMissedNotification();
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

    static void schedulePendingIntent(AlarmManager am, long triggerTime, PendingIntent pi) {
        if (Utils.isMOrLater()) {
            // Ensure the timer fires even if the device is dozing.
            am.setExactAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP, triggerTime, pi);
        } else {
            am.setExact(ELAPSED_REALTIME_WAKEUP, triggerTime, pi);
        }
    }
}
