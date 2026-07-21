/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.provider.Settings.ACTION_SOUND_SETTINGS;
import static com.best.deskclock.settings.PreferencesDefaultValues.DARK_THEME;
import static com.best.deskclock.settings.PreferencesDefaultValues.LIGHT_THEME;
import static com.best.deskclock.settings.PreferencesDefaultValues.SYSTEM_THEME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_THEME;
import static com.best.deskclock.utils.Utils.enforceMainLooper;
import static com.best.deskclock.utils.Utils.enforceNotMainLooper;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.DateFormat;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.R;
import com.best.deskclock.timer.TimerService;
import com.best.deskclock.uicomponents.toast.CustomToast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * All application-wide data is accessible through this singleton.
 */
public final class DataModel {

    /**
     * The single instance of this data model that exists for the life of the application.
     */
    private static final DataModel sDataModel = new DataModel();

    private Handler mHandler;

    /**
     * The model from which city data are fetched.
     */
    private CityModel mCityModel;

    /**
     * The model from which timer data are fetched.
     */
    private TimerModel mTimerModel;

    /**
     * The model from which alarm data are fetched.
     */
    private AlarmModel mAlarmModel;

    /**
     * The model from which data about settings that silence alarms are fetched.
     */
    private SilentSettingsModel mSilentSettingsModel;

    /**
     * The model from which stopwatch data are fetched.
     */
    private StopwatchModel mStopwatchModel;

    /**
     * The model from which notification data are fetched.
     */
    private NotificationModel mNotificationModel;

    /**
     * The model from which ringtone data are fetched.
     */
    private RingtoneModel mRingtoneModel;

    private DataModel() {
    }

    public static DataModel getDataModel() {
        return sDataModel;
    }

    /**
     * Initializes the data model with the context and shared preferences to be used.
     */
    public void init() {
        Context appContext = DeskClockApplication.getAppContext();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(appContext);

        final String themeValue = prefs.getString(KEY_THEME, SYSTEM_THEME);
        switch (themeValue) {
            case SYSTEM_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            case LIGHT_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            case DARK_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        mNotificationModel = new NotificationModel();
        mRingtoneModel = new RingtoneModel(appContext, prefs);
        mCityModel = new CityModel(appContext, prefs);
        mAlarmModel = new AlarmModel(prefs, mRingtoneModel);
        mSilentSettingsModel = new SilentSettingsModel(appContext, mNotificationModel);
        mStopwatchModel = new StopwatchModel(appContext, prefs, mNotificationModel);
        mTimerModel = new TimerModel(appContext, prefs, mRingtoneModel, mNotificationModel);
    }

    /**
     * Convenience for {@code run(runnable, 0)}, i.e. waits indefinitely.
     */
    public void run(Runnable runnable) {
        try {
            run(runnable, 0);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Updates all timers and the stopwatch after the device has shutdown and restarted.
     */
    public void updateAfterReboot() {
        enforceMainLooper();
        mTimerModel.updateTimersAfterReboot();
        mStopwatchModel.setStopwatch(getStopwatch().updateAfterReboot());
    }

    /**
     * Updates all timers and the stopwatch after the device's time has changed.
     */
    public void updateAfterTimeSet() {
        enforceMainLooper();
        mTimerModel.updateTimersAfterTimeSet();
        mStopwatchModel.setStopwatch(getStopwatch().updateAfterTimeSet());
    }

    /**
     * Posts a runnable to the main thread and blocks until the runnable executes. Used to access
     * the data model from the main thread.
     *
     * @noinspection SynchronizationOnLocalVariableOrMethodParameter
     */
    public void run(Runnable runnable, long waitMillis) throws InterruptedException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
            return;
        }

        final ExecutedRunnable er = new ExecutedRunnable(runnable);
        getHandler().post(er);

        // Wait for the data to arrive, if it has not.
        synchronized (er) {
            if (!er.isExecuted()) {
                er.wait(waitMillis);
            }
        }
    }

    /**
     * @return a handler associated with the main thread
     */
    private synchronized Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    /**
     * @return {@code true} when the application is open in the foreground; {@code false} otherwise
     */
    public boolean isApplicationInForeground() {
        enforceMainLooper();
        return mNotificationModel.isApplicationInForeground();
    }

    /**
     * @param inForeground {@code true} to indicate the application is open in the foreground
     */
    public void setApplicationInForeground(boolean inForeground) {
        enforceMainLooper();

        if (mNotificationModel.isApplicationInForeground() != inForeground) {
            mNotificationModel.setApplicationInForeground(inForeground);

            // Refresh all notifications in response to a change in app open state.
            mTimerModel.updateNotification();
            mTimerModel.updateMissedNotification();
            mTimerModel.updateHeadsUpNotification();
            mStopwatchModel.updateNotification();
            mSilentSettingsModel.updateSilentState();
        }
    }

    /**
     * Called when the notifications may be stale or absent from the notification manager and must
     * be rebuilt. e.g. after upgrading the application
     */
    public void updateAllNotifications() {
        enforceMainLooper();
        mTimerModel.updateNotification();
        mTimerModel.updateMissedNotification();
        mTimerModel.updateHeadsUpNotification();
        mStopwatchModel.updateNotification();
    }

    /**
     * @return a list of all cities in their display order
     */
    public List<City> getAllCities() {
        enforceMainLooper();
        return mCityModel.getAllCities();
    }

    /**
     * @return a city representing the user's home timezone
     */
    public City getHomeCity() {
        enforceMainLooper();
        return mCityModel.getHomeCity();
    }

    /**
     * @return a list of cities not selected for display
     */
    public List<City> getUnselectedCities() {
        enforceMainLooper();
        return mCityModel.getUnselectedCities();
    }

    /**
     * @return a list of cities selected for display
     */
    public List<City> getSelectedCities() {
        enforceMainLooper();
        return mCityModel.getSelectedCities();
    }

    /**
     * @param cities the new collection of cities selected for display by the user
     */
    public void setSelectedCities(Collection<City> cities) {
        enforceMainLooper();
        mCityModel.setSelectedCities(cities);
    }

    /**
     * Updates the order of selected cities and persists it to SharedPreferences.
     *
     * @param newOrder the new list of selected cities, in the desired order
     */
    public void updateSelectedCitiesOrder(List<City> newOrder) {
        enforceMainLooper();
        mCityModel.updateSelectedCitiesOrder(newOrder);
    }

    /**
     * @return a comparator used to locate index positions
     */
    public Comparator<City> getCityIndexComparator() {
        enforceMainLooper();
        return mCityModel.getCityIndexComparator();
    }

    /**
     * Adjust the order in which cities are sorted.
     */
    public void toggleCitySort() {
        enforceMainLooper();
        mCityModel.toggleCitySort();
    }

    /**
     * @param cityListener listener to be notified when the world city list changes
     */
    public void addCityListener(CityListener cityListener) {
        enforceMainLooper();
        mCityModel.addCityListener(cityListener);
    }

    /**
     * @param cityListener listener that no longer needs to be notified of world city list changes
     */
    public void removeCityListener(CityListener cityListener) {
        enforceMainLooper();
        mCityModel.removeCityListener(cityListener);
    }

    /**
     * @param timerListener to be notified when timers are added, updated and removed
     */
    public void addTimerListener(TimerListener timerListener) {
        enforceMainLooper();
        mTimerModel.addTimerListener(timerListener);
    }

    /**
     * @param timerListener to no longer be notified when timers are added, updated and removed
     */
    public void removeTimerListener(TimerListener timerListener) {
        enforceMainLooper();
        mTimerModel.removeTimerListener(timerListener);
    }

    /**
     * @return a list of timers for display
     */
    public List<Timer> getTimers() {
        enforceMainLooper();
        return mTimerModel.getTimers();
    }

    /**
     * @return {@code true} if at least one timer is running, paused, has expired or has been missed.
     * {@code false} otherwise.
     */
    public boolean hasActiveTimer() {
        for (Timer timer : getTimers()) {
            if (!timer.isReset()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if at least one timer is running or paused.
     * {@code false} otherwise.
     */
    public boolean hasRunningTimer() {
        for (Timer timer : getTimers()) {
            if (!timer.isReset() && !timer.isMissed() && !timer.isExpired()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Load timers from SharedPreferences after a restore or reset of settings
     */
    public void loadTimers() {
        mTimerModel.loadTimers();
    }

    /**
     * @return a list of expired timers for display
     */
    public List<Timer> getExpiredTimers() {
        enforceMainLooper();
        return mTimerModel.getExpiredTimers();
    }

    /**
     * @param timerId identifies the timer to return
     * @return the timer with the given {@code timerId}
     */
    public Timer getTimer(int timerId) {
        enforceMainLooper();
        return mTimerModel.getTimer(timerId);
    }

    /**
     * @param length            the length of the timer in milliseconds
     * @param label             describes the purpose of the timer
     * @param buttonTime        the time indicated in the timer time add button
     * @param ringtone          the timer ringtone
     * @param autoSilence       the auto silence duration
     * @param crescendoDuration the volume crescendo duration
     * @param isVibrate         {@code true} to enable vibration upon expiration, {@code false} otherwise
     * @param vibrationPattern  the vibration pattern
     * @param isFlashOn         {@code true} tu turn on the flash upon expiration, {@code false} otherwise
     * @param turnOffMedia      {@code true} to turn off media upon expiration, {@code false} otherwise.
     * @param deleteAfterUse    {@code true} indicates the timer should be deleted when it is reset
     * @return the newly added timer
     */
    public Timer addTimer(long length, String label, String buttonTime, Uri ringtone, int autoSilence, int crescendoDuration,
                          boolean isVibrate, String vibrationPattern, boolean isFlashOn, boolean turnOffMedia, boolean deleteAfterUse) {

        enforceMainLooper();

        return mTimerModel.addTimer(length, label, buttonTime, ringtone, autoSilence, crescendoDuration, isVibrate, vibrationPattern,
            isFlashOn, turnOffMedia, deleteAfterUse
        );
    }

    /**
     * Deletes a given timer.
     *
     * @param timer the timer to be removed
     */
    public void removeTimer(Timer timer, @StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.removeTimer(timer, eventLabelId);
    }

    /**
     * @param timer the timer to be started
     */
    public void startTimer(Timer timer) {
        startTimer(null, timer);
    }

    /**
     * @param service used to start foreground notifications for expired timers
     * @param timer   the timer to be started
     */
    public void startTimer(Service service, Timer timer) {
        enforceMainLooper();
        final Timer started = timer.start();
        mTimerModel.updateTimer(started);
        if (timer.getRemainingTime() <= 0) {
            if (service != null) {
                expireTimer(service, started);
            } else {
                Context context = DeskClockApplication.getAppContext();
                Intent intent = TimerService.createTimerExpiredIntent(context, started);

                ContextCompat.startForegroundService(context, intent);
            }
        }
    }

    /**
     * @param timer the timer to be paused
     */
    public void pauseTimer(Timer timer) {
        enforceMainLooper();
        mTimerModel.updateTimer(timer.pause());
    }

    /**
     * @param service used to start foreground notifications for expired timers
     * @param timer   the timer to be expired
     */
    public void expireTimer(Service service, Timer timer) {
        enforceMainLooper();
        mTimerModel.expireTimer(service, timer);
    }

    /**
     * Resets a given timer.
     *
     * @param timer        the timer to be reset
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetTimer(Timer timer, @StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetTimer(timer, eventLabelId);
    }

    /**
     * Resets or deletes all expired timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetOrDeleteExpiredTimers(@StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetOrDeleteExpiredTimers(eventLabelId);
    }

    /**
     * Resets or deletes all missed timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetOrDeleteMissedTimers(@StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetOrDeleteMissedTimers(eventLabelId);
    }

    /**
     * @param timer the timer to which minutes or hours should be added to the remaining time
     */
    public void addCustomTimeToTimer(Timer timer) {
        enforceMainLooper();
        mTimerModel.updateTimer(timer.addCustomTime());
    }

    /**
     * @param timer     the timer to which the new {@code newLength} belongs
     * @param newLength the new duration to store for the {@code timer}
     */
    public void setNewTimerDuration(Timer timer, long newLength) {
        enforceMainLooper();

        if (timer.getLength() == newLength) {
            return;
        }

        mTimerModel.updateTimer(timer.setNewDuration(newLength));
    }

    /**
     * Updates all customizable settings of a given timer at once.
     * If no changes are detected, the update is ignored to optimize performance.
     *
     * @param timer             the original timer to update
     * @param label             the new label for the timer
     * @param buttonTime        the new custom duration for the add button
     * @param ringtone          the new timer ringtone
     * @param autoSilence       the new custom auto silence duration
     * @param crescendoDuration the new custom volume crescendo duration
     * @param isVibrate         {@code true} to enable vibration upon expiration, {@code false} otherwise
     * @param vibrationPattern  the vibration pattern
     * @param isFlashOn         {@code true} tu turn on the flash upon expiration, {@code false} otherwise
     * @param turnOffMedia      {@code true} to turn off media upon expiration, {@code false} otherwise.
     * @param deleteAfterUse    {@code true} to automatically delete the timer after use, {@code false} otherwise
     */
    public void updateAllTimerSettings(Timer timer, String label, String buttonTime, Uri ringtone, int autoSilence, int crescendoDuration,
                                       boolean isVibrate, String vibrationPattern, boolean isFlashOn, boolean turnOffMedia,
                                       boolean deleteAfterUse) {

        enforceMainLooper();

        Timer updatedTimer = timer.setLabel(label)
            .setButtonTime(buttonTime)
            .setRingtone(ringtone)
            .setAutoSilence(autoSilence)
            .setCrescendoDuration(crescendoDuration)
            .setIsVibrate(isVibrate)
            .setVibrationPattern(vibrationPattern)
            .setFlashOn(isFlashOn)
            .setTurnOffMedia(turnOffMedia)
            .setDeleteAfterUse(deleteAfterUse);

        if (updatedTimer == timer) {
            return;
        }

        mTimerModel.updateTimer(updatedTimer);
    }

    /**
     * Updates the timer notifications to be current.
     */
    public void updateTimerNotification() {
        enforceMainLooper();
        mTimerModel.updateNotification();
    }

    /**
     * @return the uri of the default ringtone to play for all timers when no user selection exists
     */
    public Uri getDefaultTimerRingtoneUri() {
        enforceMainLooper();
        return mTimerModel.getDefaultTimerRingtoneUri();
    }

    /**
     * @return the uri of the ringtone to play for all timers
     */
    public Uri getTimerRingtoneUri() {
        enforceMainLooper();
        return mTimerModel.getTimerRingtoneUri();
    }

    /**
     * @param uri the uri of the ringtone to play for all timers
     */
    public void setTimerRingtoneUri(Uri uri) {
        enforceMainLooper();
        mTimerModel.setTimerRingtoneUri(uri);
    }

    /**
     * @return the title of the ringtone that is played for all timers
     */
    public String getTimerRingtoneTitle() {
        enforceMainLooper();
        return mTimerModel.getTimerRingtoneTitle();
    }

    /**
     * @return the uri of the default ringtone from the settings to play for all alarms when no user selection exists
     */
    public Uri getDefaultAlarmRingtoneUriFromSettings() {
        enforceMainLooper();
        return mAlarmModel.getDefaultAlarmRingtoneUriFromSettings();
    }

    /**
     * @return the uri of the ringtone from the settings to play for all alarms
     */
    public Uri getAlarmRingtoneUriFromSettings() {
        enforceMainLooper();
        return mAlarmModel.getAlarmRingtoneUriFromSettings();
    }

    /**
     * @return the title of the ringtone that is played for all alarms
     */
    public String getAlarmRingtoneTitle() {
        enforceMainLooper();
        return mAlarmModel.getAlarmRingtoneTitle();
    }

    /**
     * @param uri the uri of the ringtone from the settings to play for all alarms
     */
    public void setAlarmRingtoneUriFromSettings(Uri uri) {
        enforceMainLooper();
        mAlarmModel.setAlarmRingtoneUriFromSettings(uri);
    }

    /**
     * @param stopwatchListener to be notified when stopwatch changes or laps are added
     */
    public void addStopwatchListener(StopwatchListener stopwatchListener) {
        enforceMainLooper();
        mStopwatchModel.addStopwatchListener(stopwatchListener);
    }

    /**
     * @param stopwatchListener to no longer be notified when stopwatch changes or laps are added
     */
    public void removeStopwatchListener(StopwatchListener stopwatchListener) {
        enforceMainLooper();
        mStopwatchModel.removeStopwatchListener(stopwatchListener);
    }

    /**
     * @return the current state of the stopwatch
     */
    public Stopwatch getStopwatch() {
        enforceMainLooper();
        return mStopwatchModel.getStopwatch();
    }

    /**
     *
     */
    public void startStopwatch() {
        enforceMainLooper();
        mStopwatchModel.setStopwatch(getStopwatch().start());
    }

    /**
     *
     */
    public void pauseStopwatch() {
        enforceMainLooper();
        mStopwatchModel.setStopwatch(getStopwatch().pause());
    }

    /**
     *
     */
    public void resetStopwatch() {
        enforceMainLooper();
        mStopwatchModel.setStopwatch(getStopwatch().reset());
    }

    /**
     * @return the laps recorded for this stopwatch
     */
    public List<Lap> getLaps() {
        enforceMainLooper();
        return (mStopwatchModel != null) ? mStopwatchModel.getLaps() : new ArrayList<>();
    }

    /**
     * @return a newly recorded lap completed now; {@code null} if no more laps can be added
     */
    public Lap addLap() {
        enforceMainLooper();
        return mStopwatchModel.addLap();
    }

    /**
     * @return {@code true} iff more laps can be recorded
     */
    public boolean canAddMoreLaps() {
        enforceMainLooper();
        return mStopwatchModel.canAddMoreLaps();
    }

    /**
     * @return the longest lap time of all recorded laps and the current lap
     */
    public long getLongestLapTime() {
        enforceMainLooper();
        return mStopwatchModel.getLongestLapTime();
    }

    /**
     * @param time a point in time after the end of the last lap
     * @return the elapsed time between the given {@code time} and the end of the previous lap
     */
    public long getCurrentLapTime(long time) {
        enforceMainLooper();
        return mStopwatchModel.getCurrentLapTime(time);
    }

    /**
     * @return the current time in milliseconds
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * @return milliseconds since boot, including time spent in sleep
     */
    public long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * @return {@code true} if 24-hour time format is selected; {@code false} otherwise
     */
    public boolean is24HourFormat() {
        return DateFormat.is24HourFormat(DeskClockApplication.getAppContext());
    }

    /**
     * @return a new calendar object initialized to the {@link #currentTimeMillis()}
     */
    public Calendar getCalendar() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        return calendar;
    }

    /**
     * Ringtone titles are cached because loading them is expensive. This method
     * <strong>must</strong> be called on a background thread and is responsible for priming the
     * cache of ringtone titles to avoid later fetching titles on the main thread.
     */
    public void loadRingtoneTitles() {
        enforceNotMainLooper();
        mRingtoneModel.loadRingtoneTitles();
    }

    /**
     * Recheck the permission to read each custom ringtone.
     */
    public void loadRingtonePermissions() {
        enforceNotMainLooper();
        mRingtoneModel.loadRingtonePermissions();
    }

    /**
     * @param uri the uri of a ringtone
     * @return the title of the ringtone with the {@code uri}; {@code null} if it cannot be fetched
     */
    public String getRingtoneTitle(Uri uri) {
        enforceMainLooper();
        return mRingtoneModel.getRingtoneTitle(uri);
    }

    /**
     * @param uri   the uri of an audio file to use as a ringtone
     * @param title the title of the audio content at the given {@code uri}
     */
    public Uri customRingtoneToAdd(Uri uri, String title) {
        enforceMainLooper();
        return mRingtoneModel.customRingtoneToAdd(uri, title);
    }

    /**
     * @param uri identifies the ringtone to remove
     */
    public void removeCustomRingtone(Uri uri) {
        enforceMainLooper();
        mRingtoneModel.removeCustomRingtone(uri);
    }

    /**
     * @return {@code true} if a custom ringtone with a given name and size is already present
     * to avoid adding duplicates. {@code false} otherwise.
     */
    public boolean isCustomRingtoneAlreadyAdded(String name, long size) {
        return mRingtoneModel.customRingtoneAlreadyAdded(name, size) != null;
    }

    /**
     * @return all available custom ringtones
     */
    public List<CustomRingtone> getCustomRingtones() {
        enforceMainLooper();
        return mRingtoneModel.getCustomRingtones();
    }


    /**
     * @param silentSettingsListener to be notified when alarm-silencing settings change
     */
    public void addSilentSettingsListener(OnSilentSettingsListener silentSettingsListener) {
        enforceMainLooper();
        mSilentSettingsModel.addSilentSettingsListener(silentSettingsListener);
    }

    /**
     * @param silentSettingsListener to no longer be notified when alarm-silencing settings change
     */
    public void removeSilentSettingsListener(OnSilentSettingsListener silentSettingsListener) {
        enforceMainLooper();
        mSilentSettingsModel.removeSilentSettingsListener(silentSettingsListener);
    }

    /**
     * Indicates the display style of clocks.
     */
    public enum ClockStyle {ANALOG, ANALOG_MATERIAL, DIGITAL}

    /**
     * Indicates the preferred sort order of cities.
     */
    public enum CitySort {NAME, UTC_OFFSET}

    /**
     * Indicates the preferred behavior of power button when firing alarms.
     */
    public enum PowerButtonBehavior {NOTHING, SNOOZE, DISMISS}

    /**
     * Indicates the preferred behavior of volume button when firing alarms.
     */
    public enum VolumeButtonBehavior {CHANGE_VOLUME, SNOOZE, DISMISS, NOTHING}

    /**
     * Indicates the preferred behavior of external device buttons when firing alarms.
     */
    public enum HeadphonesButtonBehavior {NOTHING, SNOOZE, DISMISS}

    /**
     * Indicates the reason alarms may not fire or may fire silently.
     */
    public enum SilentSetting {
        DO_NOT_DISTURB(R.string.alarms_blocked_by_dnd, 0),
        SILENT_RINGTONE(R.string.silent_default_alarm_ringtone, R.string.change_setting_action);

        private final @StringRes
        int mLabelResId;
        private final @StringRes
        int mActionResId;

        SilentSetting(int labelResId, int actionResId) {
            mLabelResId = labelResId;
            mActionResId = actionResId;
        }

        public @StringRes
        int getLabelResId() {
            return mLabelResId;
        }

        public @StringRes
        int getActionResId() {
            return mActionResId;
        }

        public boolean isActionEnabled() {
            return mActionResId != 0;
        }

        public void executeAction(Context context) {
            if (this == SILENT_RINGTONE) {
                try {
                    context.startActivity(new Intent(ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (ActivityNotFoundException ex) {
                    CustomToast.show(context, "application_not_found");
                }
            }
        }
    }

    /**
     * Used to execute a delegate runnable and track its completion.
     */
    private static class ExecutedRunnable implements Runnable {

        private final Runnable mDelegate;
        private boolean mExecuted;

        private ExecutedRunnable(Runnable delegate) {
            this.mDelegate = delegate;
        }

        @Override
        public void run() {
            mDelegate.run();

            synchronized (this) {
                mExecuted = true;
                notifyAll();
            }
        }

        private boolean isExecuted() {
            return mExecuted;
        }
    }
}
