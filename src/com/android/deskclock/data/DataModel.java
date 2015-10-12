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

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.android.deskclock.Utils.enforceMainLooper;

/**
 * All application-wide data is accessible through this singleton.
 */
public final class DataModel {

    /** Indicates the display style of clocks. */
    public enum ClockStyle {ANALOG, DIGITAL}

    /** Indicates the preferred sort order of cities. */
    public enum CitySort {NAME, UTC_OFFSET}

    public static final String ACTION_CITIES_CHANGED = "com.android.deskclock.CITIES_CHANGED";

    /** The single instance of this data model that exists for the life of the application. */
    private static final DataModel sDataModel = new DataModel();

    private Handler mHandler;

    private Context mContext;

    /** The model from which settings are fetched. */
    private SettingsModel mSettingsModel;

    /** The model from which city data are fetched. */
    private CityModel mCityModel;

    /** The model from which timer data are fetched. */
    private TimerModel mTimerModel;

    /** The model from which alarm data are fetched. */
    private AlarmModel mAlarmModel;

    /** The model from which stopwatch data are fetched. */
    private StopwatchModel mStopwatchModel;

    /** The model from which notification data are fetched. */
    private NotificationModel mNotificationModel;

    public static DataModel getDataModel() {
        return sDataModel;
    }

    private DataModel() {}

    /**
     * The context may be set precisely once during the application life.
     */
    public void setContext(Context context) {
        if (mContext != null) {
            throw new IllegalStateException("context has already been set");
        }
        mContext = context.getApplicationContext();

        mSettingsModel = new SettingsModel(mContext);
        mNotificationModel = new NotificationModel();
        mCityModel = new CityModel(mContext, mSettingsModel);
        mTimerModel = new TimerModel(mContext, mSettingsModel);
        mAlarmModel = new AlarmModel(mContext, mSettingsModel);
        mStopwatchModel = new StopwatchModel(mContext, mNotificationModel);
    }

    /**
     * Posts a runnable to the main thread and blocks until the runnable executes. Used to access
     * the data model from the main thread.
     */
    public void run(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
            return;
        }

        final ExecutedRunnable er = new ExecutedRunnable(runnable);
        getHandler().post(er);

        // Wait for the data to arrive, if it has not.
        synchronized (er) {
            if (!er.isExecuted()) {
                try {
                    er.wait();
                } catch (InterruptedException ignored) {
                    // ignore
                }
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

    //
    // Application
    //

    /**
     * @param inForeground {@code true} to indicate the application is open in the foreground
     */
    public void setApplicationInForeground(boolean inForeground) {
        enforceMainLooper();

        if (mNotificationModel.isApplicationInForeground() != inForeground) {
            mNotificationModel.setApplicationInForeground(inForeground);

            // Refresh all notifications in response to a change in app open state.
            mStopwatchModel.updateNotification();
        }
    }

    /**
     * @return {@code true} when the application is open in the foreground; {@code false} otherwise
     */
    public boolean isApplicationInForeground() {
        return mNotificationModel.isApplicationInForeground();
    }

    //
    // Cities
    //

    /**
     * @return a list of all cities in their display order
     */
    public List<City> getAllCities() {
        enforceMainLooper();
        return mCityModel.getAllCities();
    }

    /**
     * @param cityName the case-insensitive city name to search for
     * @return the city with the given {@code cityName}; {@code null} if no such city exists
     */
    public City getCity(String cityName) {
        enforceMainLooper();
        return mCityModel.getCity(cityName);
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

    //
    // Timers
    //

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
     * @return the title of the ringtone that is played for all timers
     */
    public String getTimerRingtoneTitle() {
        enforceMainLooper();
        return mTimerModel.getTimerRingtoneTitle();
    }

    //
    // Alarms
    //

    /**
     * @return the uri of the ringtone to which all new alarms default
     */
    public Uri getDefaultAlarmRingtoneUri() {
        enforceMainLooper();
        return mAlarmModel.getDefaultAlarmRingtoneUri();
    }

    /**
     * @param uri the uri of the ringtone to which future new alarms will default
     */
    public void setDefaultAlarmRingtoneUri(Uri uri) {
        enforceMainLooper();
        mAlarmModel.setDefaultAlarmRingtoneUri(uri);
    }

    /**
     * @param uri the uri of a ringtone
     * @return the title of the ringtone with the {@code uri}; {@code null} if it cannot be fetched
     */
    public String getAlarmRingtoneTitle(Uri uri) {
        enforceMainLooper();
        return mAlarmModel.getAlarmRingtoneTitle(uri);
    }

    //
    // Stopwatch
    //

    /**
     * @return the current state of the stopwatch
     */
    public Stopwatch getStopwatch() {
        enforceMainLooper();
        return mStopwatchModel.getStopwatch();
    }

    /**
     * @return the stopwatch after being started
     */
    public Stopwatch startStopwatch() {
        enforceMainLooper();
        return mStopwatchModel.setStopwatch(getStopwatch().start());
    }

    /**
     * @return the stopwatch after being paused
     */
    public Stopwatch pauseStopwatch() {
        enforceMainLooper();
        return mStopwatchModel.setStopwatch(getStopwatch().pause());
    }

    /**
     * @return the stopwatch after being reset
     */
    public Stopwatch resetStopwatch() {
        enforceMainLooper();
        return mStopwatchModel.setStopwatch(getStopwatch().reset());
    }

    /**
     * @return the laps recorded for this stopwatch
     */
    public List<Lap> getLaps() {
        enforceMainLooper();
        return mStopwatchModel.getLaps();
    }

    /**
     * @return a newly recorded lap completed now; {@code null} if no more laps can be added
     */
    public Lap addLap() {
        enforceMainLooper();
        return mStopwatchModel.addLap();
    }

    /**
     * Clears the laps recorded for this stopwatch.
     */
    public void clearLaps() {
        enforceMainLooper();
        mStopwatchModel.clearLaps();
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

    //
    // Settings
    //

    /**
     * @return the style of clock to display in the clock application
     */
    public ClockStyle getClockStyle() {
        enforceMainLooper();
        return mSettingsModel.getClockStyle();
    }

    /**
     * @return the style of clock to display in the clock screensaver
     */
    public ClockStyle getScreensaverClockStyle() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverClockStyle();
    }

    /**
     * @return {@code true} if the users wants to automatically show a clock for their home timezone
     *      when they have travelled outside of that timezone
     */
    public boolean getShowHomeClock() {
        enforceMainLooper();
        return mSettingsModel.getShowHomeClock();
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