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

package com.android.deskclock.voice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.AlarmClock;
import android.text.format.DateUtils;

import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.stopwatch.Stopwatches;
import com.android.deskclock.timer.TimerFullScreenFragment;
import com.android.deskclock.timer.TimerObj;
import com.android.deskclock.timer.Timers;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CitiesActivity;
import com.android.deskclock.worldclock.CityObj;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandleVoiceApiCalls extends Activity {
    private Context mAppContext;

    private static final String ACTION_PREFIX = "com.android.deskclock.action.";

    // shows the tab with world clocks
    public static final String VOICE_ACTION_SHOW_CLOCK = ACTION_PREFIX + "VOICE_SHOW_CLOCK";
    // add a clock of a selected city, if no city is specified opens the city selection screen
    public static final String VOICE_ACTION_ADD_CLOCK = ACTION_PREFIX + "VOICE_ADD_CLOCK";
    // delete a clock of a selected city, if no city is specified shows CitiesActivity for the user
    // to choose a city
    public static final String VOICE_ACTION_DELETE_CLOCK = ACTION_PREFIX + "VOICE_DELETE_CLOCK";
    // extra for VOICE_ACTION_ADD_CLOCK and VOICE_ACTION_DELETE_CLOCK
    public static final String EXTRA_CITY = "com.android.deskclock.extra.clock.CITY";

    // shows the tab with the stopwatch
    public static final String VOICE_ACTION_SHOW_STOPWATCH = ACTION_PREFIX + "VOICE_SHOW_STOPWATCH";
    // starts the current stopwatch
    public static final String VOICE_ACTION_START_STOPWATCH = ACTION_PREFIX
            + "VOICE_START_STOPWATCH";
    // stops the current stopwatch
    public static final String VOICE_ACTION_STOP_STOPWATCH = ACTION_PREFIX + "VOICE_STOP_STOPWATCH";
    // laps the stopwatch that's currently running
    public static final String VOICE_ACTION_LAP_STOPWATCH = ACTION_PREFIX + "VOICE_LAP_STOPWATCH";
    // resets the stopwatch if it's stopped
    public static final String VOICE_ACTION_RESET_STOPWATCH = ACTION_PREFIX
            + "VOICE_RESET_STOPWATCH";

    // shows the tab with timers
    public static final String VOICE_ACTION_SHOW_TIMERS = ACTION_PREFIX + "VOICE_SHOW_TIMERS";
    // deletes the topmost timer
    public static final String VOICE_ACTION_DELETE_TIMER = ACTION_PREFIX + "VOICE_DELETE_TIMER";
    // stops the running timer
    public static final String VOICE_ACTION_STOP_TIMER = ACTION_PREFIX + "VOICE_STOP_TIMER";
    // starts the topmost timer
    public static final String VOICE_ACTION_START_TIMER = ACTION_PREFIX + "VOICE_START_TIMER";
    // resets the timer, works for both running and stopped
    public static final String VOICE_ACTION_RESET_TIMER = ACTION_PREFIX + "VOICE_RESET_TIMER";

    @Override
    protected void onCreate(Bundle icicle) {
        try {
            super.onCreate(icicle);
            mAppContext = getApplicationContext();

            final Intent intent = getIntent();
            if (intent == null) {
                return;
            }

            final String action = intent.getAction();
            switch (action) {
                case VOICE_ACTION_START_STOPWATCH:
                case VOICE_ACTION_STOP_STOPWATCH:
                case VOICE_ACTION_LAP_STOPWATCH:
                case VOICE_ACTION_SHOW_STOPWATCH:
                case VOICE_ACTION_RESET_STOPWATCH:
                    handleStopwatchIntent(action);
                    break;
                case AlarmClock.ACTION_DISMISS_ALARM:
                    handleAlarmIntent(action);
                    break;
                case VOICE_ACTION_SHOW_TIMERS:
                case VOICE_ACTION_DELETE_TIMER:
                case VOICE_ACTION_RESET_TIMER:
                case VOICE_ACTION_STOP_TIMER:
                case VOICE_ACTION_START_TIMER:
                    handleTimerIntent(action);
                    break;
                case VOICE_ACTION_SHOW_CLOCK:
                case VOICE_ACTION_ADD_CLOCK:
                case VOICE_ACTION_DELETE_CLOCK:
                    handleClockIntent(action);
                    break;
            }
        } finally {
            finish();
        }
    }

    private void handleAlarmIntent(final String action) {
        // Opens the UI for Alarms
        final Intent alarmIntent =
                Alarm.createIntent(mAppContext, DeskClock.class, Alarm.INVALID_ID)
                        .setAction(action)
                        .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
        startActivity(alarmIntent);

        final Intent intent = getIntent();

        new DismissAlarmAsync(mAppContext, intent).execute();
    }

    private void handleStopwatchIntent(String action) {
        // Opens the UI for stopwatch
        final Intent stopwatchIntent = new Intent(mAppContext, DeskClock.class)
                .setAction(action)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.STOPWATCH_TAB_INDEX);
        startActivity(stopwatchIntent);
        LogUtils.i("HandleVoiceApiCalls " + action);

        // checking if the stopwatch is already running
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        final boolean stopwatchAlreadyRunning =
                prefs.getBoolean(Stopwatches.NOTIF_CLOCK_RUNNING, false);

        if (stopwatchAlreadyRunning) {
            // don't fire START_STOPWATCH or RESET_STOPWATCH if a stopwatch is already running
            if (VOICE_ACTION_START_STOPWATCH.equals(action) ||
                    VOICE_ACTION_RESET_STOPWATCH.equals(action)) {
                LogUtils.i("Stopwatch is already running");
                return;
            }
        } else {
            // if a stopwatch isn't running, don't try to stop or lap it
            if (VOICE_ACTION_STOP_STOPWATCH.equals(action) ||
                    VOICE_ACTION_LAP_STOPWATCH.equals(action)) {
                LogUtils.i("Stopwatch isn't running");
                return;
            }
        }

        // Events setup
        switch (action) {
            case VOICE_ACTION_START_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_start, R.string.label_voice);
                LogUtils.i("Stopwatch was started.");
                break;
            case VOICE_ACTION_STOP_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_stop, R.string.label_voice);
                LogUtils.i("Stopwatch was stopped.");
                break;
            case VOICE_ACTION_LAP_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_lap, R.string.label_voice);
                LogUtils.i("Stopwatch was lapped.");
                break;
            case VOICE_ACTION_SHOW_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_show, R.string.label_voice);
                LogUtils.i("Stopwatch tab was shown.");
                break;
            case VOICE_ACTION_RESET_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_reset, R.string.label_voice);
                LogUtils.i("Stopwatch was reset.");
                break;
            default:
                return;
        }
        final Intent intent = new Intent(mAppContext, StopwatchService.class).setAction(action);
        startService(intent);
    }

    private void handleTimerIntent(final String action) {
        // Opens the UI for timers
        final Intent timerIntent = new Intent(mAppContext, DeskClock.class)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX)
                .putExtra(TimerFullScreenFragment.GOTO_SETUP_VIEW, false);
        switch (action) {
            case VOICE_ACTION_DELETE_TIMER:
                timerIntent.setAction(Timers.DELETE_TIMER);
                break;
            case VOICE_ACTION_START_TIMER:
                timerIntent.setAction(Timers.START_TIMER);
                break;
            case VOICE_ACTION_RESET_TIMER:
                timerIntent.setAction(Timers.RESET_TIMER);
                break;
            case VOICE_ACTION_STOP_TIMER:
                timerIntent.setAction(Timers.STOP_TIMER);
                break;
            case VOICE_ACTION_SHOW_TIMERS:
                // no action necessary
                break;
            default:
                return;
        }
        startActivity(timerIntent);
        LogUtils.i("HandleVoiceApiCalls " + action);

        if (VOICE_ACTION_SHOW_TIMERS.equals(action)) {
            Events.sendTimerEvent(R.string.action_show, R.string.label_voice);
            return;
        }
        new HandleTimersAsync(mAppContext, action).execute();
    }

    private void handleClockIntent(final String action) {
        // Opens the UI for clocks
        final Intent handleClock = new Intent(mAppContext, DeskClock.class)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.CLOCK_TAB_INDEX);
        startActivity(handleClock);

        new HandleClockAsync(mAppContext, getIntent()).execute();
    }

    static void dismissAlarm(Alarm alarm, Context context) {
        // only allow on background thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("dismissAlarm must be called on a " +
                    "background thread");
        }

        final AlarmInstance alarmInstance = AlarmInstance.getNextUpcomingInstanceByAlarmId(
                context.getContentResolver(), alarm.id);
        if (alarmInstance == null) {
            LogUtils.i("There are no alarm instances for this alarm id");
            return;
        }

        final Calendar nextAlarmTime = alarmInstance.getAlarmTime();
        final long nextAlarmTimeMillis = nextAlarmTime.getTimeInMillis();
        final long twoHours = -AlarmInstance.LOW_NOTIFICATION_HOUR_OFFSET *
                DateUtils.HOUR_IN_MILLIS;
        final boolean isWithinTwoHours = nextAlarmTimeMillis - System.currentTimeMillis()
                <= twoHours;

        if (isWithinTwoHours) {
            AlarmStateManager.setPreDismissState(context, alarmInstance);
            LogUtils.i("Dismiss %d:%d",alarm.hour, alarm.minutes);
            Events.sendAlarmEvent(R.string.action_dismiss,
                    R.string.label_voice);
        } else {
            LogUtils.i("%s wasn't dismissed, still more than two hours away.", alarm);
        }
    }

    private static class DismissAlarmAsync extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        private final Intent mIntent;

        public DismissAlarmAsync(Context context, Intent intent) {
            mContext = context;
            mIntent = intent;
        }

        @Override
        protected Void doInBackground(Void... parameters) {
            final List<Alarm> alarms = getRelevantAlarms();
            if (alarms.isEmpty()) {
                LogUtils.i("No alarms are scheduled.");
                return null;
            }

            final String searchMode = mIntent.getStringExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE);
            if (searchMode == null && alarms.size() > 1) {
                // TODO: add picker UI
                // shows the UI where user picks which alarm they want to DISMISS
//                final Intent pickSelectionIntent = new Intent(mContext, PickSelectionActivity.class)
//                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                        .setAction(mIntent.getAction())
//                        .putExtra(PickSelectionActivity.EXTRA_ALARMS,
//                                alarms.toArray(new Parcelable[alarms.size()]));
//                mContext.startActivity(pickSelectionIntent);
                return null;
            }

            // fetch the alarms that are specified by the intent
            final FetchMatchingAlarmsAction fmaa =
                    new FetchMatchingAlarmsAction(mContext, alarms, mIntent);
            fmaa.run();
            final List<Alarm> matchingAlarms = fmaa.getMatchingAlarms();

            // If there are multiple matching alarms and it wasn't expected
            // disambiguate what the user meant
            if (!AlarmClock.ALARM_SEARCH_MODE_ALL.equals(searchMode) && matchingAlarms.size() > 1) {
                // TODO: add picker UI
//              final Intent pickSelectionIntent = new Intent(mContext, PickSelectionActivity.class)
//                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                        .setAction(mIntent.getAction())
//                        .putExtra(PickSelectionActivity.EXTRA_ALARMS,
//                                matchingAlarms.toArray(new Parcelable[matchingAlarms.size()]));
//                mContext.startActivity(pickSelectionIntent);
                return null;
            }

            // Apply the action to the matching alarms
            for (Alarm alarm : matchingAlarms) {
                dismissAlarm(alarm, mContext);
                LogUtils.i("Alarm %s is dismissed", alarm);
            }
            return null;
        }

        private List<Alarm> getRelevantAlarms() {
            // since we want to dismiss we should only add enabled alarms
            final String selection = String.format("%s=?", Alarm.ENABLED);

            final String[] args = {"1"};
            return Alarm.getAlarms(mContext.getContentResolver(), selection, args);
        }
    }

    private static class HandleTimersAsync extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        private final String mAction;

        public HandleTimersAsync(Context context, String action) {
            mContext = context;
            mAction = action;
        }

        @Override
        protected Void doInBackground(Void... parameters) {
            final List<TimerObj> timers = new ArrayList<>();
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            TimerObj.getTimersFromSharedPrefs(prefs, timers);
            if (timers.isEmpty()) {
                LogUtils.i("There are no timers");
                return null;
            }
            // Delete the most recently created timer, the one that
            // shows up first on the Timers tab
            final TimerObj timer = timers.get(timers.size() - 1);

            switch (mAction) {
                case VOICE_ACTION_DELETE_TIMER:
                    timer.deleteFromSharedPref(prefs);
                    Events.sendTimerEvent(R.string.action_delete, R.string.label_voice);
                    LogUtils.i("Timer was successfully deleted");
                    break;
                case VOICE_ACTION_START_TIMER:
                    // adjust mStartTime to reflect that starting time changed,
                    // VOICE_ACTION_START_TIMER considers that a part of
                    // the timer's length might have elapsed
                    if (timer.mState == TimerObj.STATE_RUNNING) {
                        LogUtils.i("Timer is already running");
                        return null;
                    }
                    timer.setState(TimerObj.STATE_RUNNING);
                    timer.mStartTime = Utils.getTimeNow() -
                            (timer.mSetupLength - timer.mTimeLeft);
                    timer.writeToSharedPref(prefs);
                    LogUtils.i("Timer was successfully started");
                    Events.sendTimerEvent(R.string.action_start, R.string.label_voice);
                    break;
                case VOICE_ACTION_RESET_TIMER:
                    // timer can be reset only if it's stopped
                    if (timer.mState == TimerObj.STATE_STOPPED) {
                        timer.mTimeLeft = timer.mOriginalLength;
                        timer.writeToSharedPref(prefs);
                        LogUtils.i("Timer was successfully reset");
                        Events.sendTimerEvent(R.string.action_reset, R.string.label_voice);
                    } else {
                        LogUtils.i("Timer can't be reset because it isn't stopped");
                        return null;
                    }
                    break;
                case VOICE_ACTION_STOP_TIMER:
                    if (timer.mState == TimerObj.STATE_STOPPED) {
                        LogUtils.i("Timer is already stopped");
                        return null;
                    }
                    LogUtils.i("Timer was successfully stopped");
                    timer.setState(TimerObj.STATE_STOPPED);
                    timer.writeToSharedPref(prefs);
                    Events.sendTimerEvent(R.string.action_stop, R.string.label_voice);
                    break;
            }
            return null;
        }
    }

    private static class HandleClockAsync extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        private final Intent mIntent;

        public HandleClockAsync(Context context, Intent intent) {
            mContext = context;
            mIntent = intent;
        }

        @Override
        protected Void doInBackground(Void... parameters) {
            final String cityExtra = mIntent.getStringExtra(EXTRA_CITY);
            final SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(mContext);
            switch (mIntent.getAction()) {
                case VOICE_ACTION_ADD_CLOCK: {
                    // if a city isn't specified open CitiesActivity to choose a city
                    if (cityExtra == null) {
                        LogUtils.i("No city specified");
                        mContext.startActivity(new Intent(mContext, CitiesActivity.class));
                        Events.sendClockEvent(R.string.action_create, R.string.label_voice);
                        break;
                    }

                    // if a city is passed add that city to the list
                    final Map<String, CityObj> cities = Utils.loadCityMapFromXml(mContext);
                    final CityObj city = cities.get(cityExtra.toLowerCase());
                    // check if this city exists in the list of available cities
                    if (city == null) {
                        LogUtils.i("The city you specified is not available");
                        break;
                    }

                    final HashMap<String, CityObj> selectedCities =
                            Cities.readCitiesFromSharedPrefs(prefs);
                    // if this city is already added don't add it
                    if (selectedCities.put(city.mCityId, city) != null) {
                        LogUtils.i("The city you specified is already added");
                        break;
                    }

                    Cities.saveCitiesToSharedPrefs(prefs, selectedCities);
                    LogUtils.i("%s was successfully added", city.mCityName);
                    Events.sendClockEvent(R.string.action_start, R.string.label_voice);
                    break;
                }
                case VOICE_ACTION_DELETE_CLOCK: {
                    if (cityExtra == null) {
                        // if a city isn't specified open CitiesActivity to choose a city
                        LogUtils.i("No city specified");
                        mContext.startActivity(new Intent(mContext, CitiesActivity.class));
                        Events.sendClockEvent(R.string.action_create, R.string.label_voice);
                        break;
                    }

                    // if a city is specified check if it's selected and if so delete it
                    final Map<String, CityObj> cities = Utils.loadCityMapFromXml(mContext);
                    // check if this city exists in the list of available cities
                    final CityObj city = cities.get(cityExtra.toLowerCase());
                    if (city == null) {
                        LogUtils.i("The city you specified is not available");
                        break;
                    }

                    final HashMap<String, CityObj> selectedCities =
                            Cities.readCitiesFromSharedPrefs(prefs);
                    if (selectedCities.remove(city.mCityId) != null) {
                        Cities.saveCitiesToSharedPrefs(prefs, selectedCities);
                        LogUtils.i("%s was successfully deleted", city.mCityName);
                        Events.sendClockEvent(R.string.action_delete, R.string.label_voice);
                    }
                    break;
                }
                case VOICE_ACTION_SHOW_CLOCK:
                    Events.sendClockEvent(R.string.action_show, R.string.label_voice);
                    break;
            }
            return null;
        }
    }
}

