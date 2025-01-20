/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.media.AudioManager.FLAG_SHOW_UI;
import static android.media.AudioManager.STREAM_ALARM;
import static android.provider.Settings.ACTION_SOUND_SETTINGS;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.DARK_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.LIGHT_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.SYSTEM_THEME;
import static com.best.deskclock.utils.Utils.enforceMainLooper;
import static com.best.deskclock.utils.Utils.enforceNotMainLooper;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import com.best.deskclock.R;
import com.best.deskclock.settings.InterfaceCustomizationActivity;
import com.best.deskclock.timer.TimerService;

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

    private Context mContext;

    /**
     * The model from which settings are fetched.
     */
    private SettingsModel mSettingsModel;

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
     * The model from which widget data are fetched.
     */
    private WidgetModel mWidgetModel;

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
     * The model from which time data are fetched.
     */
    private TimeModel mTimeModel;

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
    public void init(Context context, SharedPreferences prefs) {
        if (mContext != context) {
            mContext = context.getApplicationContext();

            final String themeValue = prefs.getString(InterfaceCustomizationActivity.KEY_THEME, SYSTEM_THEME);
            switch (themeValue) {
                case SYSTEM_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                case LIGHT_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                case DARK_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }

            mTimeModel = new TimeModel(mContext);
            mWidgetModel = new WidgetModel(prefs);
            mNotificationModel = new NotificationModel();
            mRingtoneModel = new RingtoneModel(mContext, prefs);
            mSettingsModel = new SettingsModel(mContext, prefs, mTimeModel);
            mCityModel = new CityModel(mContext, prefs, mSettingsModel);
            mAlarmModel = new AlarmModel(prefs, mSettingsModel, mRingtoneModel);
            mSilentSettingsModel = new SilentSettingsModel(mContext, mNotificationModel);
            mStopwatchModel = new StopwatchModel(mContext, prefs, mNotificationModel);
            mTimerModel = new TimerModel(mContext, prefs, mSettingsModel, mRingtoneModel, mNotificationModel);
        }
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
     * @return a comparator used to locate index positions
     */
    public Comparator<City> getCityIndexComparator() {
        enforceMainLooper();
        return mCityModel.getCityIndexComparator();
    }

    /**
     * @return the order in which cities are sorted
     */
    public CitySort getCitySort() {
        enforceMainLooper();
        return mCityModel.getCitySort();
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
     * @param length         the length of the timer in milliseconds
     * @param label          describes the purpose of the timer
     * @param buttonTime     the time indicated in the timer time add button
     * @param deleteAfterUse {@code true} indicates the timer should be deleted when it is reset
     * @return the newly added timer
     */
    public Timer addTimer(long length, String label, String buttonTime, boolean deleteAfterUse) {
        enforceMainLooper();
        return mTimerModel.addTimer(length, label, buttonTime, deleteAfterUse);
    }

    /**
     * @param timer the timer to be removed
     */
    public void removeTimer(Timer timer) {
        enforceMainLooper();
        mTimerModel.removeTimer(timer);
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
                mContext.startService(TimerService.createTimerExpiredIntent(mContext, started));
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
     * If the given {@code timer} is expired and marked for deletion after use then this method
     * removes the timer. The timer is otherwise transitioned to the reset state and continues
     * to exist.
     *
     * @param timer        the timer to be reset
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetOrDeleteTimer(Timer timer, @StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetTimer(timer, true, eventLabelId);
    }

    /**
     * Resets all expired timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetOrDeleteExpiredTimers(@StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetOrDeleteExpiredTimers(eventLabelId);
    }

    /**
     * Resets all unexpired timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetUnexpiredTimers(@StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetUnexpiredTimers(eventLabelId);
    }

    /**
     * Resets all missed timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetMissedTimers(@StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetMissedTimers(eventLabelId);
    }

    /**
     * @param timer the timer to which minutes or hours should be added to the remaining time
     */
    public void addCustomTimeToTimer(Timer timer) {
        enforceMainLooper();
        mTimerModel.updateTimer(timer.addCustomTime());
    }

    /**
     * @param timer the timer to which the new {@code label} belongs
     * @param label the new label to store for the {@code timer}
     */
    public void setTimerLabel(Timer timer, String label) {
        enforceMainLooper();
        mTimerModel.updateTimer(timer.setLabel(label));
    }

    /**
     * @param timer the timer to which the new {@code buttonTime} belongs
     * @param buttonTime the new add button text to store for the {@code timer}
     */
    public void setTimerButtonTime(Timer timer, String buttonTime) {
        enforceMainLooper();
        mTimerModel.updateTimer(timer.setButtonTime(buttonTime));
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
     * @return {@code true} iff the ringtone to play for all timers is the silent ringtone
     */
    public boolean isTimerRingtoneSilent() {
        enforceMainLooper();
        return mTimerModel.isTimerRingtoneSilent();
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
     * @return the duration, in milliseconds, of the crescendo to apply to timer ringtone playback;
     * {@code 0} implies no crescendo should be applied
     */
    public long getTimerCrescendoDuration() {
        enforceMainLooper();
        return mTimerModel.getTimerCrescendoDuration();
    }

    /**
     * @return whether vibrate is enabled for all timers.
     */
    public boolean getTimerVibrate() {
        enforceMainLooper();
        return mTimerModel.getTimerVibrate();
    }

    /**
     * @param enabled whether vibrate is enabled for all timers.
     */
    public void setTimerVibrate(boolean enabled) {
        enforceMainLooper();
        mTimerModel.setTimerVibrate(enabled);
    }

    /**
     * @return whether the expired timer is reset with the volume buttons. {@code false} otherwise.
     */
    public boolean isExpiredTimerResetWithVolumeButtons() {
        return mTimerModel.isExpiredTimerResetWithVolumeButtons();
    }

    /**
     * @return whether the expired timer is reset with the power button. {@code false} otherwise.
     */
    public boolean isExpiredTimerResetWithPowerButton() {
        return mTimerModel.isExpiredTimerResetWithPowerButton();
    }

    /**
     * @return whether flip action for timers is enabled. {@code false} otherwise.
     */
    public boolean isFlipActionForTimersEnabled() {
        return mTimerModel.isFlipActionForTimersEnabled();
    }

    /**
     * @return whether shake action for timers is enabled. {@code false} otherwise.
     */
    public boolean isShakeActionForTimersEnabled() {
        return mTimerModel.isShakeActionForTimersEnabled();
    }

    /**
     * @return the timer sorting manually, in ascending order of duration, in descending order of duration or by name
     */
    public String getTimerSortingPreference() {
        enforceMainLooper();
        return mTimerModel.getTimerSortingPreference();
    }

    /**
     * @return the default minutes or hour to add to timer when the "Add Minute Or Hour" button is clicked.
     */
    public int getDefaultTimeToAddToTimer() {
        enforceMainLooper();
        return mTimerModel.getDefaultTimeToAddToTimer();
    }

    /**
     * @return {@code true} if the timer display must remain on. {@code false} otherwise.
     */
    public boolean shouldTimerDisplayRemainOn() {
        enforceMainLooper();
        return mTimerModel.shouldTimerDisplayRemainOn();
    }

    /**
     * @return {@code true} if the timer background must be transparent. {@code false} otherwise.
     */
    public boolean isTimerBackgroundTransparent() {
        enforceMainLooper();
        return mTimerModel.isTimerBackgroundTransparent();
    }

    /**
     * @return {@code true} if a warning is displayed before deleting a timer. {@code false} otherwise.
     */
    public boolean isWarningDisplayedBeforeDeletingTimer() {
        enforceMainLooper();
        return mTimerModel.isWarningDisplayedBeforeDeletingTimer();
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
     * @param uri the uri of the ringtone of an existing alarm
     */
    public void setSelectedAlarmRingtoneUri(Uri uri) {
        enforceMainLooper();
        mAlarmModel.setSelectedAlarmRingtoneUri(uri);
    }

    /**
     * @return the duration, in milliseconds, of the crescendo to apply to alarm ringtone playback;
     * {@code 0} implies no crescendo should be applied
     */
    public long getAlarmCrescendoDuration() {
        enforceMainLooper();
        return mAlarmModel.getAlarmCrescendoDuration();
    }

    /**
     * @return the behavior to execute when volume buttons are pressed while firing an alarm
     */
    public VolumeButtonBehavior getAlarmVolumeButtonBehavior() {
        enforceMainLooper();
        return mAlarmModel.getAlarmVolumeButtonBehavior();
    }

    /**
     * @return the behavior to execute when power buttons are pressed while firing an alarm
     */
    public PowerButtonBehavior getAlarmPowerButtonBehavior() {
        enforceMainLooper();
        return mAlarmModel.getAlarmPowerButtonBehavior();
    }

    /**
     * @return the number of minutes an alarm may ring before it has timed out and becomes missed
     */
    public int getAlarmTimeout() {
        return mAlarmModel.getAlarmTimeout();
    }

    /**
     * @return the number of minutes an alarm will remain snoozed before it rings again
     */
    public int getSnoozeLength() {
        return mAlarmModel.getSnoozeLength();
    }

    public int getFlipAction() {
        return mAlarmModel.getFlipAction();
    }

    public int getShakeAction() {
        return mAlarmModel.getShakeAction();
    }

    /**
     * @return the number of minutes before the upcoming alarm notification appears
     */
    public int getAlarmNotificationReminderTime() {
        return mAlarmModel.getAlarmNotificationReminderTime();
    }

    /**
     * @return {@code true} if alarm vibrations are enabled when creating alarms. {@code false} otherwise.
     */
    public boolean areAlarmVibrationsEnabledByDefault() {
        return mAlarmModel.areAlarmVibrationsEnabledByDefault();
    }

    /**
     * @return {@code true} if vibrations are enabled to indicate whether the alarm is snoozed or dismissed.
     * {@code false} otherwise.
     */
    public boolean areSnoozedOrDismissedAlarmVibrationsEnabled() {
        return mAlarmModel.areSnoozedOrDismissedAlarmVibrationsEnabled();
    }

    /**
     * @return {@code true} if occasional alarm should be deleted by default. {@code false} otherwise.
     */
    public boolean isOccasionalAlarmDeletedByDefault() {
        return mAlarmModel.isOccasionalAlarmDeletedByDefault();
    }

    /**
     * @return the time picker style.
     */
    public String getMaterialTimePickerStyle() {
        return mAlarmModel.getMaterialTimePickerStyle();
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed on the alarm.
     */
    public ClockStyle getAlarmClockStyle() {
        return mAlarmModel.getAlarmClockStyle();
    }

    /**
     * @return a value indicating whether analog clock seconds hand is displayed on the alarm.
     */
    public boolean isAlarmSecondsHandDisplayed() {
        return mAlarmModel.isAlarmSecondsHandDisplayed();
    }

    /**
     * @return a value indicating alarm background color.
     */
    public int getAlarmBackgroundColor() {
        return mAlarmModel.getAlarmBackgroundColor();
    }

    /**
     * @return a value indicating alarm background amoled color.
     */
    public int getAlarmBackgroundAmoledColor() {
        return mAlarmModel.getAlarmBackgroundAmoledColor();
    }

    /**
     * @return a value indicating the alarm clock color.
     */
    public int getAlarmClockColor() {
        return mAlarmModel.getAlarmClockColor();
    }

    /**
     * @return a value indicating the alarm seconds hand color.
     */
    public int getAlarmSecondsHandColor() {
        return mAlarmModel.getAlarmSecondsHandColor();
    }

    /**
     * @return a value indicating the alarm title color.
     */
    public int getAlarmTitleColor() {
        return mAlarmModel.getAlarmTitleColor();
    }

    /**
     * @return a value indicating the snooze button color.
     */
    public int getSnoozeButtonColor() {
        return mAlarmModel.getSnoozeButtonColor();
    }

    /**
     * @return a value indicating the dismiss button color.
     */
    public int getDismissButtonColor() {
        return mAlarmModel.getDismissButtonColor();
    }

    /**
     * @return a value indicating the alarm button color.
     */
    public int getAlarmButtonColor() {
        return mAlarmModel.getAlarmButtonColor();
    }

    /**
     * @return a value indicating the pulse color.
     */
    public int getPulseColor() {
        return mAlarmModel.getPulseColor();
    }

    /**
     * @return the font size applied to the alarm clock.
     */
    public String getAlarmClockFontSize() {
        return mAlarmModel.getAlarmClockFontSize();
    }

    /**
     * @return the font size applied to the alarm title.
     */
    public String getAlarmTitleFontSize() {
        return mAlarmModel.getAlarmTitleFontSize();
    }

    /**
     * @return {@code true} if the ringtone title should be displayed on the lock screen when the alarm is triggered.
     * {@code false} otherwise.
     */
    public boolean isRingtoneTitleDisplayed() {
        return mAlarmModel.isRingtoneTitleDisplayed();
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
        return mTimeModel.currentTimeMillis();
    }

    /**
     * @return milliseconds since boot, including time spent in sleep
     */
    public long elapsedRealtime() {
        return mTimeModel.elapsedRealtime();
    }

    /**
     * @return {@code true} if 24 hour time format is selected; {@code false} otherwise
     */
    public boolean is24HourFormat() {
        return mTimeModel.is24HourFormat();
    }

    /**
     * @return a new calendar object initialized to the {@link #currentTimeMillis()}
     */
    public Calendar getCalendar() {
        return mTimeModel.getCalendar();
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
    public void addCustomRingtone(Uri uri, String title) {
        enforceMainLooper();
        mRingtoneModel.addCustomRingtone(uri, title);
    }

    /**
     * @param uri identifies the ringtone to remove
     */
    public void removeCustomRingtone(Uri uri) {
        enforceMainLooper();
        mRingtoneModel.removeCustomRingtone(uri);
    }

    /**
     * @return all available custom ringtones
     */
    public List<CustomRingtone> getCustomRingtones() {
        enforceMainLooper();
        return mRingtoneModel.getCustomRingtones();
    }

    /**
     * @param widgetClass     indicates the type of widget being counted
     * @param count           the number of widgets of the given type
     * @param eventCategoryId identifies the category of event to send
     */
    public void updateWidgetCount(Class<?> widgetClass, int count, @StringRes int eventCategoryId) {
        enforceMainLooper();
        mWidgetModel.updateWidgetCount(widgetClass, count, eventCategoryId);
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
     * @return the id used to discriminate relevant AlarmManager callbacks from defunct ones
     */
    public int getGlobalIntentId() {
        return mSettingsModel.getGlobalIntentId();
    }

    /**
     * Update the id used to discriminate relevant AlarmManager callbacks from defunct ones
     */
    public void updateGlobalIntentId() {
        enforceMainLooper();
        mSettingsModel.updateGlobalIntentId();
    }

    /**
     * @return the theme applied.
     */
    public String getTheme() {
        enforceMainLooper();
        return mSettingsModel.getTheme();
    }

    /**
     * @return the accent color applied.
     */
    public String getAccentColor() {
        enforceMainLooper();
        return mSettingsModel.getAccentColor();
    }

    /**
     * @return {@code true} if auto night accent color is enabled. {@code false} otherwise.
     */
    public boolean isAutoNightAccentColorEnabled() {
        return mSettingsModel.isAutoNightAccentColorEnabled();
    }

    /**
     * @return the night accent color applied.
     */
    public String getNightAccentColor() {
        enforceMainLooper();
        return mSettingsModel.getNightAccentColor();
    }

    /**
     * @return the dark mode of the applied theme.
     */
    public String getDarkMode() {
        enforceMainLooper();
        return mSettingsModel.getDarkMode();
    }

    /**
     * @return whether or not the background should be displayed in a view.
     */
    public boolean isCardBackgroundDisplayed() {
        enforceMainLooper();
        return mSettingsModel.isCardBackgroundDisplayed();
    }

    /**
     * @return whether or not the border should be displayed in a view.
     */
    public boolean isCardBorderDisplayed() {
        enforceMainLooper();
        return mSettingsModel.isCardBorderDisplayed();
    }

    /**
     * @return whether or not the vibrations are enabled for the buttons.
     */
    public boolean isVibrationsEnabled() {
        enforceMainLooper();
        return mSettingsModel.isVibrationsEnabled();
    }

    /**
     * @return whether or not the tab indicator is displayed in the bottom navigation menu.
     */
    public boolean isTabIndicatorDisplayed() {
        return mSettingsModel.isTabIndicatorDisplayed();
    }

    /**
     * @return whether or not the fade transitions are enabled.
     */
    public boolean isFadeTransitionsEnabled() {
        enforceMainLooper();
        return mSettingsModel.isFadeTransitionsEnabled();
    }

    /**
     * @return the style of the clock to display in the clock application
     */
    public ClockStyle getClockStyle() {
        enforceMainLooper();
        return mSettingsModel.getClockStyle();
    }

    /**
     * @return the style of the clock to display in the clock screensaver
     */
    public ClockStyle getScreensaverClockStyle() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverClockStyle();
    }

    /**
     * @return the dynamic colors of the clock to display in the screensaver
     */
    public boolean areScreensaverClockDynamicColors() {
        enforceMainLooper();
        return mSettingsModel.areScreensaverClockDynamicColors();
    }

    /**
     * @return the color of the clock to display in the screensaver
     */
    public int getScreensaverClockColorPicker() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverClockColorPicker();
    }

    /**
     * @return the color of the date to display in the screensaver
     */
    public int getScreensaverDateColorPicker() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverDateColorPicker();
    }

    /**
     * @return the color of the next alarm to display in the screensaver
     */
    public int getScreensaverNextAlarmColorPicker() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverNextAlarmColorPicker();
    }

    /**
     * @return the night mode brightness of clock to display in the clock application
     */
    public int getScreensaverBrightness() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverBrightness();
    }

    /**
     * @return the style of clock to display in the clock application
     */
    public boolean areScreensaverClockSecondsDisplayed() {
        enforceMainLooper();
        return mSettingsModel.areScreensaverClockSecondsDisplayed();
    }

    /**
     * @return {@code true} if the screensaver should show the time in bold
     */
    public boolean isScreensaverDigitalClockInBold() {
        enforceMainLooper();
        return mSettingsModel.isScreensaverDigitalClockInBold();
    }

    /**
     * @return {@code true} if the screensaver should show the time in italic
     */
    public boolean isScreensaverDigitalClockInItalic() {
        enforceMainLooper();
        return mSettingsModel.isScreensaverDigitalClockInItalic();
    }

    /**
     * @return {@code true} if the screensaver should show the date in bold
     */
    public boolean isScreensaverDateInBold() {
        enforceMainLooper();
        return mSettingsModel.isScreensaverDateInBold();
    }

    /**
     * @return {@code true} if the screensaver should show the date in italic
     */
    public boolean isScreensaverDateInItalic() {
        enforceMainLooper();
        return mSettingsModel.isScreensaverDateInItalic();
    }

    /**
     * @return {@code true} if the screensaver should show the next alarm in bold
     */
    public boolean isScreensaverNextAlarmInBold() {
        enforceMainLooper();
        return mSettingsModel.isScreensaverNextAlarmInBold();
    }

    /**
     * @return {@code true} if the screensaver should show the next alarm in italic
     */
    public boolean isScreensaverNextAlarmInItalic() {
        enforceMainLooper();
        return mSettingsModel.isScreensaverNextAlarmInItalic();
    }

    /**
     * @return the style of clock to display in the clock application
     */
    public boolean getDisplayClockSeconds() {
        enforceMainLooper();
        return mSettingsModel.getDisplayClockSeconds();
    }

    /**
     * @param displaySeconds whether or not to display seconds for main clock
     */
    public void setDisplayClockSeconds(boolean displaySeconds) {
        enforceMainLooper();
        mSettingsModel.setDisplayClockSeconds(displaySeconds);
    }

    /**
     * @return {@code true} if the users wants to automatically show a clock for their home timezone
     * when they have travelled outside of that timezone
     */
    public boolean getShowHomeClock() {
        enforceMainLooper();
        return mSettingsModel.getShowHomeClock();
    }

    /**
     * @return {@code true} if swipe action is enabled to dismiss or snooze alarms. {@code false} otherwise.
     */
    public boolean isSwipeActionEnabled() {
        enforceMainLooper();
        return mSettingsModel.isSwipeActionEnabled();
    }

    /**
     * @return the display order of the weekdays, which can start with {@link Calendar#SATURDAY},
     * {@link Calendar#SUNDAY} or {@link Calendar#MONDAY}
     */
    public Weekdays.Order getWeekdayOrder() {
        enforceMainLooper();
        return mSettingsModel.getWeekdayOrder();
    }

    /**
     * @return {@code true} if the restore process (of backup and restore) has completed
     */
    public boolean isRestoreBackupFinished() {
        return mSettingsModel.isRestoreBackupFinished();
    }

    /**
     * @param finished {@code true} means the restore process (of backup and restore) has completed
     */
    public void setRestoreBackupFinished(boolean finished) {
        mSettingsModel.setRestoreBackupFinished(finished);
    }

    /**
     * @return the action to execute when volume up button is pressed for the stopwatch
     */
    public String getVolumeUpActionForStopwatch() {
        enforceMainLooper();
        return mSettingsModel.getVolumeUpActionForStopwatch();
    }

    /**
     * @return the action to execute when volume up button is long pressed for the stopwatch
     */
    public String getVolumeUpActionAfterLongPressForStopwatch() {
        enforceMainLooper();
        return mSettingsModel.getVolumeUpActionAfterLongPressForStopwatch();
    }

    /**
     * @return the action to execute when volume down button is pressed for the stopwatch
     */
    public String getVolumeDownActionForStopwatch() {
        enforceMainLooper();
        return mSettingsModel.getVolumeDownActionForStopwatch();
    }

    /**
     * @return the action to execute when volume down button is long pressed for the stopwatch
     */
    public String getVolumeDownActionAfterLongPressForStopwatch() {
        enforceMainLooper();
        return mSettingsModel.getVolumeDownActionAfterLongPressForStopwatch();
    }

    /**
     * @return a description of the time zones available for selection
     */
    public TimeZones getTimeZones() {
        enforceMainLooper();
        return mSettingsModel.getTimeZones();
    }

    // ********************
    // ** DIGITAL WIDGET **
    // ********************

    /**
     * @return {@code true} if the background is displayed on the digital widget; {@code false} otherwise.
     */
    public boolean isBackgroundDisplayedOnDigitalWidget() {
        return mWidgetModel.isBackgroundDisplayedOnDigitalWidget();
    }

    /**
     * @return a value indicating the background color in the digital widget .
     */
    public int getDigitalWidgetBackgroundColor() {
        return mWidgetModel.getDigitalWidgetBackgroundColor();
    }

    /**
     * @return {@code true} if the cities are displayed on the digital widget; {@code false} otherwise.
     */
    public boolean areWorldCitiesDisplayedOnDigitalWidget() {
        return mWidgetModel.areWorldCitiesDisplayedOnDigitalWidget();
    }

    /**
     * @return the font size applied to the clock in the digital widget.
     */
    public String getDigitalWidgetMaxClockFontSize() {
        return mWidgetModel.getDigitalWidgetMaxClockFontSize();
    }

    /**
     * @return {@code true} if the default color is applied to the clock in the digital widget; {@code false} otherwise.
     */
    public boolean isDigitalWidgetDefaultClockColor() {
        return mWidgetModel.isDigitalWidgetDefaultClockColor();
    }

    /**
     * @return a value indicating the color of the clock in the digital widget.
     */
    public int getDigitalWidgetCustomClockColor() {
        return mWidgetModel.getDigitalWidgetCustomClockColor();
    }

    /**
     * @return {@code true} if the default color is applied to the date in the digital widget; {@code false} otherwise.
     */
    public boolean isDigitalWidgetDefaultDateColor() {
        return mWidgetModel.isDigitalWidgetDefaultDateColor();
    }

    /**
     * @return a value indicating the color of the date in the digital widget.
     */
    public int getDigitalWidgetCustomDateColor() {
        return mWidgetModel.getDigitalWidgetCustomDateColor();
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the digital widget;
     * {@code false} otherwise.
     */
    public boolean isDigitalWidgetDefaultNextAlarmColor() {
        return mWidgetModel.isDigitalWidgetDefaultNextAlarmColor();
    }

    /**
     * @return a value indicating the color of the next alarm in the digital widget.
     */
    public int getDigitalWidgetCustomNextAlarmColor() {
        return mWidgetModel.getDigitalWidgetCustomNextAlarmColor();
    }

    /**
     * @return {@code true} if the default color is applied to the city clock in the digital widget;
     * {@code false} otherwise.
     */
    public boolean isDigitalWidgetDefaultCityClockColor() {
        return mWidgetModel.isDigitalWidgetDefaultCityClockColor();
    }

    /**
     * @return a value indicating the color of the city clock in the digital widget.
     */
    public int getDigitalWidgetCustomCityClockColor() {
        return mWidgetModel.getDigitalWidgetCustomCityClockColor();
    }

    /**
     * @return {@code true} if the default color is applied to the city name in the digital widget;
     * {@code false} otherwise.
     */
    public boolean isDigitalWidgetDefaultCityNameColor() {
        return mWidgetModel.isDigitalWidgetDefaultCityNameColor();
    }

    /**
     * @return a value indicating the color of the city name in the digital widget.
     */
    public int getDigitalWidgetCustomCityNameColor() {
        return mWidgetModel.getDigitalWidgetCustomCityNameColor();
    }

    // *****************************
    // ** VERTICAL DIGITAL WIDGET **
    // *****************************

    /**
     * @return {@code true} if the background is displayed on the vertical digital widget; {@code false} otherwise.
     */
    public boolean isBackgroundDisplayedOnVerticalDigitalWidget() {
        return mWidgetModel.isBackgroundDisplayedOnVerticalDigitalWidget();
    }

    /**
     * @return a value indicating the background color in the vertical digital widget .
     */
    public int getVerticalDigitalWidgetBackgroundColor() {
        return mWidgetModel.getVerticalDigitalWidgetBackgroundColor();
    }

    /**
     * @return the font size applied to the hours in the vertical digital widget.
     */
    public String getVerticalDigitalWidgetMaxClockFontSize() {
        return mWidgetModel.getVerticalDigitalWidgetMaxClockFontSize();
    }

    /**
     * @return {@code true} if the default color is applied to the hours in the vertical digital widget;
     * {@code false} otherwise.
     */
    public boolean isVerticalDigitalWidgetDefaultHoursColor() {
        return mWidgetModel.isVerticalDigitalWidgetDefaultHoursColor();
    }

    /**
     * @return a value indicating the color of the hours in the vertical digital widget.
     */
    public int getVerticalDigitalWidgetCustomHoursColor() {
        return mWidgetModel.getVerticalDigitalWidgetCustomHoursColor();
    }

    /**
     * @return {@code true} if the default color is applied to the minutes in the vertical digital widget;
     * {@code false} otherwise.
     */
    public boolean isVerticalDigitalWidgetDefaultMinutesColor() {
        return mWidgetModel.isVerticalDigitalWidgetDefaultMinutesColor();
    }

    /**
     * @return a value indicating the color of the minutes in the vertical digital widget.
     */
    public int getVerticalDigitalWidgetCustomMinutesColor() {
        return mWidgetModel.getVerticalDigitalWidgetCustomMinutesColor();
    }

    /**
     * @return {@code true} if the default color is applied to the date in the vertical digital widget;
     * {@code false} otherwise.
     */
    public boolean isVerticalDigitalWidgetDefaultDateColor() {
        return mWidgetModel.isVerticalDigitalWidgetDefaultDateColor();
    }

    /**
     * @return a value indicating the color of the date in the vertical digital widget.
     */
    public int getVerticalDigitalWidgetCustomDateColor() {
        return mWidgetModel.getVerticalDigitalWidgetCustomDateColor();
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the vertical digital widget;
     * {@code false} otherwise.
     */
    public boolean isVerticalDigitalWidgetDefaultNextAlarmColor() {
        return mWidgetModel.isVerticalDigitalWidgetDefaultNextAlarmColor();
    }

    /**
     * @return a value indicating the color of the next alarm in the vertical digital widget.
     */
    public int getVerticalDigitalWidgetCustomNextAlarmColor() {
        return mWidgetModel.getVerticalDigitalWidgetCustomNextAlarmColor();
    }

    // ***********************
    // ** NEXT ALARM WIDGET **
    // ***********************

    /**
     * @return {@code true} if the background is displayed on the Next alarm widget; {@code false} otherwise.
     */
    public boolean isBackgroundDisplayedOnNextAlarmWidget() {
        return mWidgetModel.isBackgroundDisplayedOnNextAlarmWidget();
    }

    /**
     * @return a value indicating the background color in the Next alarm widget .
     */
    public int getNextAlarmWidgetBackgroundColor() {
        return mWidgetModel.getNextAlarmWidgetBackgroundColor();
    }

    /**
     * @return the font size applied to the Next alarm widget.
     */
    public String getNextAlarmWidgetMaxFontSize() {
        return mWidgetModel.getNextAlarmWidgetMaxFontSize();
    }

    /**
     * @return {@code true} if the default color is applied to the title in the Next alarm widget;
     * {@code false} otherwise.
     */
    public boolean isNextAlarmWidgetDefaultTitleColor() {
        return mWidgetModel.isNextAlarmWidgetDefaultTitleColor();
    }

    /**
     * @return a value indicating the color of the title in the Next alarm widget.
     */
    public int getNextAlarmWidgetCustomTitleColor() {
        return mWidgetModel.getNextAlarmWidgetCustomTitleColor();
    }

    /**
     * @return {@code true} if the default color is applied to the alarm title in the Next alarm widget;
     * {@code false} otherwise.
     */
    public boolean isNextAlarmWidgetDefaultAlarmTitleColor() {
        return mWidgetModel.isNextAlarmWidgetDefaultAlarmTitleColor();
    }

    /**
     * @return a value indicating the color of the alarm title in the Next alarm widget.
     */
    public int getNextAlarmWidgetCustomAlarmTitleColor() {
        return mWidgetModel.getNextAlarmWidgetCustomAlarmTitleColor();
    }

    /**
     * @return {@code true} if the default color is applied to the alarm in the Next alarm widget;
     * {@code false} otherwise.
     */
    public boolean isNextAlarmWidgetDefaultAlarmColor() {
        return mWidgetModel.isNextAlarmWidgetDefaultAlarmColor();
    }

    /**
     * @return a value indicating the color of the alarm in the Next alarm widget.
     */
    public int getNextAlarmWidgetCustomAlarmColor() {
        return mWidgetModel.getNextAlarmWidgetCustomAlarmColor();
    }

    // *********************************
    // ** MATERIAL YOU DIGITAL WIDGET **
    // *********************************

    /**
     * @return {@code true} if the cities are displayed on the Material You digital widget;
     * {@code false} otherwise.
     */
    public boolean areWorldCitiesDisplayedOnMaterialYouDigitalWidget() {
        enforceMainLooper();
        return mWidgetModel.areWorldCitiesDisplayedOnMaterialYouDigitalWidget();
    }

    /**
     * @return the font size applied to the clock in the Material You digital widget.
     */
    public String getMaterialYouDigitalWidgetMaxClockFontSize() {
        enforceMainLooper();
        return mWidgetModel.getMaterialYouDigitalWidgetMaxClockFontSize();
    }

    /**
     * @return {@code true} if the default color is applied to the digital clock in the Material You widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouDigitalWidgetDefaultClockColor() {
        return mWidgetModel.isMaterialYouDigitalWidgetDefaultClockColor();
    }

    /**
     * @return a value indicating the color of the clock in the Material You digital widget.
     */
    public int getMaterialYouDigitalWidgetCustomClockColor() {
        return mWidgetModel.getMaterialYouDigitalWidgetCustomClockColor();
    }

    /**
     * @return {@code true} if the default color is applied to the date in the Material You digital widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouDigitalWidgetDefaultDateColor() {
        return mWidgetModel.isMaterialYouDigitalWidgetDefaultDateColor();
    }

    /**
     * @return a value indicating the color of the date in the Material You digital widget.
     */
    public int getMaterialYouDigitalWidgetCustomDateColor() {
        return mWidgetModel.getMaterialYouDigitalWidgetCustomDateColor();
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the Material You digital widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouDigitalWidgetDefaultNextAlarmColor() {
        return mWidgetModel.isMaterialYouDigitalWidgetDefaultNextAlarmColor();
    }

    /**
     * @return a value indicating the color of the next alarm in the Material You digital widget.
     */
    public int getMaterialYouDigitalWidgetCustomNextAlarmColor() {
        return mWidgetModel.getMaterialYouDigitalWidgetCustomNextAlarmColor();
    }

    /**
     * @return {@code true} if the default color is applied to the city clock in the Material You digital widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouDigitalWidgetDefaultCityClockColor() {
        return mWidgetModel.isMaterialYouDigitalWidgetDefaultCityClockColor();
    }

    /**
     * @return a value indicating the color of the city clock in the Material You digital widget.
     */
    public int getMaterialYouDigitalWidgetCustomCityClockColor() {
        return mWidgetModel.getMaterialYouDigitalWidgetCustomCityClockColor();
    }

    /**
     * @return {@code true} if the default color is applied to the city name in the Material You digital widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouDigitalWidgetDefaultCityNameColor() {
        return mWidgetModel.isMaterialYouDigitalWidgetDefaultCityNameColor();
    }

    /**
     * @return a value indicating the color of the city name in the Material You digital widget.
     */
    public int getMaterialYouDigitalWidgetCustomCityNameColor() {
        return mWidgetModel.getMaterialYouDigitalWidgetCustomCityNameColor();
    }

    // ******************************************
    // ** MATERIAL YOU VERTICAL DIGITAL WIDGET **
    // ******************************************

    /**
     * @return the font size applied to the hours in the Material You vertical digital widget.
     */
    public String getMaterialYouVerticalDigitalWidgetMaxClockFontSize() {
        return mWidgetModel.getMaterialYouVerticalDigitalWidgetMaxClockFontSize();
    }

    /**
     * @return {@code true} if the default color is applied to the hours in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouVerticalDigitalWidgetDefaultHoursColor() {
        return mWidgetModel.isMaterialYouVerticalDigitalWidgetDefaultHoursColor();
    }

    /**
     * @return a value indicating the color of the hours in the Material You vertical digital widget.
     */
    public int getMaterialYouVerticalDigitalWidgetCustomHoursColor() {
        return mWidgetModel.getMaterialYouVerticalDigitalWidgetCustomHoursColor();
    }

    /**
     * @return {@code true} if the default color is applied to the minutes in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouVerticalDigitalWidgetDefaultMinutesColor() {
        return mWidgetModel.isMaterialYouVerticalDigitalWidgetDefaultMinutesColor();
    }

    /**
     * @return a value indicating the color of the minutes in the Material You vertical digital widget.
     */
    public int getMaterialYouVerticalDigitalWidgetCustomMinutesColor() {
        return mWidgetModel.getMaterialYouVerticalDigitalWidgetCustomMinutesColor();
    }

    /**
     * @return {@code true} if the default color is applied to the date in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouVerticalDigitalWidgetDefaultDateColor() {
        return mWidgetModel.isMaterialYouVerticalDigitalWidgetDefaultDateColor();
    }

    /**
     * @return a value indicating the color of the date in the Material You vertical digital widget.
     */
    public int getMaterialYouVerticalDigitalWidgetCustomDateColor() {
        return mWidgetModel.getMaterialYouVerticalDigitalWidgetCustomDateColor();
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm in the Material You vertical digital widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor() {
        return mWidgetModel.isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor();
    }

    /**
     * @return a value indicating the color of the next alarm in the Material You vertical digital widget.
     */
    public int getMaterialYouVerticalDigitalWidgetCustomNextAlarmColor() {
        return mWidgetModel.getMaterialYouVerticalDigitalWidgetCustomNextAlarmColor();
    }

    // ************************************
    // ** MATERIAL YOU NEXT ALARM WIDGET **
    // ************************************

    /**
     * @return the font size applied to the Material You Next alarm widget.
     */
    public String getMaterialYouNextAlarmWidgetMaxFontSize() {
        return mWidgetModel.getMaterialYouNextAlarmWidgetMaxFontSize();
    }

    /**
     * @return {@code true} if the default color is applied to the title in the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouNextAlarmWidgetDefaultTitleColor() {
        return mWidgetModel.isMaterialYouNextAlarmWidgetDefaultTitleColor();
    }

    /**
     * @return a value indicating the color of the title in the Material You Next alarm widget.
     */
    public int getMaterialYouNextAlarmWidgetCustomTitleColor() {
        return mWidgetModel.getMaterialYouNextAlarmWidgetCustomTitleColor();
    }

    /**
     * @return {@code true} if the default color is applied to the alarm title in the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor() {
        return mWidgetModel.isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor();
    }

    /**
     * @return a value indicating the color of the alarm title in the Material You Next alarm widget.
     */
    public int getMaterialYouNextAlarmWidgetCustomAlarmTitleColor() {
        return mWidgetModel.getMaterialYouNextAlarmWidgetCustomAlarmTitleColor();
    }

    /**
     * @return {@code true} if the default color is applied to the alarm in the Material You Next alarm widget;
     * {@code false} otherwise.
     */
    public boolean isMaterialYouNextAlarmWidgetDefaultAlarmColor() {
        return mWidgetModel.isMaterialYouNextAlarmWidgetDefaultAlarmColor();
    }

    /**
     * @return a value indicating the color of the alarm in the Material You Next alarm widget.
     */
    public int getMaterialYouNextAlarmWidgetCustomAlarmColor() {
        return mWidgetModel.getMaterialYouNextAlarmWidgetCustomAlarmColor();
    }

    /**
     * Indicates the display style of clocks.
     */
    public enum ClockStyle {ANALOG, DIGITAL}

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
    public enum VolumeButtonBehavior {CHANGE_VOLUME, SNOOZE_ALARM, DISMISS_ALARM, DO_NOTHING}

    /**
     * Indicates the reason alarms may not fire or may fire silently.
     */
    public enum SilentSetting {
        DO_NOT_DISTURB(R.string.alarms_blocked_by_dnd, 0, Predicate.FALSE, null),
        MUTED_VOLUME(R.string.alarm_volume_muted,
                R.string.unmute_alarm_volume,
                Predicate.TRUE,
                new UnmuteAlarmVolumeListener()),
        SILENT_RINGTONE(R.string.silent_default_alarm_ringtone,
                R.string.change_setting_action,
                new ChangeSoundActionPredicate(),
                new ChangeSoundSettingsListener());

        private final @StringRes
        int mLabelResId;
        private final @StringRes
        int mActionResId;
        private final Predicate<Context> mActionEnabled;
        private final View.OnClickListener mActionListener;

        SilentSetting(int labelResId, int actionResId, Predicate<Context> actionEnabled,
                      View.OnClickListener actionListener) {
            mLabelResId = labelResId;
            mActionResId = actionResId;
            mActionEnabled = actionEnabled;
            mActionListener = actionListener;
        }

        public @StringRes
        int getLabelResId() {
            return mLabelResId;
        }

        public @StringRes
        int getActionResId() {
            return mActionResId;
        }

        public View.OnClickListener getActionListener() {
            return mActionListener;
        }

        public boolean isActionEnabled(Context context) {
            return mLabelResId != 0 && mActionEnabled.apply(context);
        }

        private static class UnmuteAlarmVolumeListener implements View.OnClickListener {
            @Override
            public void onClick(View v) {
                // Set the alarm volume to 11/16th of max and show the slider UI.
                // 11/16th of max is the initial volume of the alarm stream on a fresh install.
                final Context context = v.getContext();
                final AudioManager am = (AudioManager) context.getSystemService(AUDIO_SERVICE);
                final int index = Math.round(am.getStreamMaxVolume(STREAM_ALARM) * 11f / 16f);
                am.setStreamVolume(STREAM_ALARM, index, FLAG_SHOW_UI);
            }
        }

        private static class ChangeSoundSettingsListener implements View.OnClickListener {
            @Override
            public void onClick(View v) {
                final Context context = v.getContext();
                context.startActivity(new Intent(ACTION_SOUND_SETTINGS)
                        .addFlags(FLAG_ACTIVITY_NEW_TASK));
            }
        }

        private static class ChangeSoundActionPredicate implements Predicate<Context> {
            @Override
            public boolean apply(Context context) {
                final Intent intent = new Intent(ACTION_SOUND_SETTINGS);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(context, "application_not_found", Toast.LENGTH_SHORT).show();
                }
                return true;
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
