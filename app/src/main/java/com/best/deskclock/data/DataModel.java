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

package com.best.deskclock.data;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.media.AudioManager.FLAG_SHOW_UI;
import static android.media.AudioManager.STREAM_ALARM;
import static android.provider.Settings.ACTION_SOUND_SETTINGS;
import static com.best.deskclock.Utils.enforceMainLooper;
import static com.best.deskclock.Utils.enforceNotMainLooper;
import static com.best.deskclock.settings.SettingsActivity.DARK_THEME;
import static com.best.deskclock.settings.SettingsActivity.LIGHT_THEME;
import static com.best.deskclock.settings.SettingsActivity.SYSTEM_THEME;

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

import com.best.deskclock.Predicate;
import com.best.deskclock.R;
import com.best.deskclock.settings.SettingsActivity;
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

    public static final String ACTION_WORLD_CITIES_CHANGED = "com.best.deskclock.WORLD_CITIES_CHANGED";

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

            final String themeValue = prefs.getString(SettingsActivity.KEY_THEME, SYSTEM_THEME);
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
     * @param deleteAfterUse {@code true} indicates the timer should be deleted when it is reset
     * @return the newly added timer
     */
    public Timer addTimer(long length, String label, boolean deleteAfterUse) {
        enforceMainLooper();
        return mTimerModel.addTimer(length, label, deleteAfterUse);
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
     * removes the the timer. The timer is otherwise transitioned to the reset state and continues
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
     * @param timer the timer to which a minute should be added to the remaining time
     */
    public void addTimerMinute(Timer timer) {
        enforceMainLooper();
        mTimerModel.updateTimer(timer.addMinute());
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
    public AlarmVolumeButtonBehavior getAlarmVolumeButtonBehavior() {
        enforceMainLooper();
        return mAlarmModel.getAlarmVolumeButtonBehavior();
    }

    /**
     * @return the behavior to execute when power buttons are pressed while firing an alarm
     */
    public AlarmVolumeButtonBehavior getAlarmPowerButtonBehavior() {
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

    //
    // Widgets
    //

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
     * @return the dark mode of the applied theme.
     */
    public String getDarkMode() {
        enforceMainLooper();
        return mSettingsModel.getDarkMode();
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
    public boolean getScreensaverClockDynamicColors() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverClockDynamicColors();
    }

    /**
     * @return the color of the clock to display in the screensaver
     */
    public String getScreensaverClockPresetColors() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverClockPresetColors();
    }

    /**
     * @return the color of the date to display in the screensaver
     */
    public String getScreensaverDatePresetColors() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverDatePresetColors();
    }

    /**
     * @return the color of the next alarm to display in the screensaver
     */
    public String getScreensaverNextAlarmPresetColors() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverNextAlarmPresetColors();
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
    public boolean getDisplayScreensaverClockSeconds() {
        enforceMainLooper();
        return mSettingsModel.getDisplayScreensaverClockSeconds();
    }

    /**
     * @return {@code true} if the screensaver should show the time in bold
     */
    public boolean getScreensaverBoldDigitalClock() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverBoldDigitalClock();
    }

    /**
     * @return {@code true} if the screensaver should show the time in italic
     */
    public boolean getScreensaverItalicDigitalClock() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverItalicDigitalClock();
    }

    /**
     * @return {@code true} if the screensaver should show the date in bold
     */
    public boolean getScreensaverBoldDate() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverBoldDate();
    }

    /**
     * @return {@code true} if the screensaver should show the date in italic
     */
    public boolean getScreensaverItalicDate() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverItalicDate();
    }

    /**
     * @return {@code true} if the screensaver should show the next alarm in bold
     */
    public boolean getScreensaverBoldNextAlarm() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverBoldNextAlarm();
    }

    /**
     * @return {@code true} if the screensaver should show the next alarm in italic
     */
    public boolean getScreensaverItalicNextAlarm() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverItalicNextAlarm();
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
     * @return a description of the time zones available for selection
     */
    public TimeZones getTimeZones() {
        enforceMainLooper();
        return mSettingsModel.getTimeZones();
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
     * Indicates the preferred behavior of hardware volume buttons when firing alarms.
     */
    public enum AlarmVolumeButtonBehavior {NOTHING, SNOOZE, DISMISS}

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
