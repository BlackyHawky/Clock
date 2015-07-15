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

package com.android.deskclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.android.deskclock.events.Events;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.stopwatch.Stopwatches;
import com.android.deskclock.timer.TimerFullScreenFragment;
import com.android.deskclock.timer.TimerObj;
import com.android.deskclock.timer.Timers;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CitiesActivity;
import com.android.deskclock.worldclock.CityObj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HandleDeskClockApiCalls extends Activity {
    private Context mAppContext;

    private static final String ACTION_PREFIX = "com.android.deskclock.action.";

    // shows the tab with world clocks
    public static final String ACTION_SHOW_CLOCK = ACTION_PREFIX + "SHOW_CLOCK";
    // add a clock of a selected city, if no city is specified opens the city selection screen
    public static final String ACTION_ADD_CLOCK = ACTION_PREFIX + "ADD_CLOCK";
    // delete a clock of a selected city, if no city is specified shows CitiesActivity for the user
    // to choose a city
    public static final String ACTION_DELETE_CLOCK = ACTION_PREFIX + "DELETE_CLOCK";
    // extra for ACTION_ADD_CLOCK and ACTION_DELETE_CLOCK
    public static final String EXTRA_CITY = "com.android.deskclock.extra.clock.CITY";

    // shows the tab with the stopwatch
    public static final String ACTION_SHOW_STOPWATCH = ACTION_PREFIX + "SHOW_STOPWATCH";
    // starts the current stopwatch
    public static final String ACTION_START_STOPWATCH = ACTION_PREFIX + "START_STOPWATCH";
    // stops the current stopwatch
    public static final String ACTION_STOP_STOPWATCH = ACTION_PREFIX + "STOP_STOPWATCH";
    // laps the stopwatch that's currently running
    public static final String ACTION_LAP_STOPWATCH = ACTION_PREFIX + "LAP_STOPWATCH";
    // resets the stopwatch if it's stopped
    public static final String ACTION_RESET_STOPWATCH = ACTION_PREFIX + "RESET_STOPWATCH";

    // shows the tab with timers
    public static final String ACTION_SHOW_TIMERS = ACTION_PREFIX + "SHOW_TIMERS";
    // deletes the topmost timer
    public static final String ACTION_DELETE_TIMER = ACTION_PREFIX + "DELETE_TIMER";
    // stops the running timer
    public static final String ACTION_STOP_TIMER = ACTION_PREFIX + "STOP_TIMER";
    // starts the topmost timer
    public static final String ACTION_START_TIMER = ACTION_PREFIX + "START_TIMER";
    // resets the timer, works for both running and stopped
    public static final String ACTION_RESET_TIMER = ACTION_PREFIX + "RESET_TIMER";

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
                case ACTION_START_STOPWATCH:
                case ACTION_STOP_STOPWATCH:
                case ACTION_LAP_STOPWATCH:
                case ACTION_SHOW_STOPWATCH:
                case ACTION_RESET_STOPWATCH:
                    handleStopwatchIntent(action);
                    break;
                case ACTION_SHOW_TIMERS:
                case ACTION_DELETE_TIMER:
                case ACTION_RESET_TIMER:
                case ACTION_STOP_TIMER:
                case ACTION_START_TIMER:
                    handleTimerIntent(action);
                    break;
                case ACTION_SHOW_CLOCK:
                case ACTION_ADD_CLOCK:
                case ACTION_DELETE_CLOCK:
                    handleClockIntent(action);
                    break;
            }
        } finally {
            finish();
        }
    }

    private void handleStopwatchIntent(String action) {
        // Opens the UI for stopwatch
        final Intent stopwatchIntent = new Intent(mAppContext, DeskClock.class)
                .setAction(action)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.STOPWATCH_TAB_INDEX);
        startActivity(stopwatchIntent);
        LogUtils.i("HandleDeskClockApiCalls " + action);

        if (action.equals(ACTION_SHOW_STOPWATCH)) {
            Events.sendStopwatchEvent(R.string.action_show, R.string.label_intent);
            return;
        }

        // checking if the stopwatch is already running
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        final boolean stopwatchAlreadyRunning =
                prefs.getBoolean(Stopwatches.NOTIF_CLOCK_RUNNING, false);

        if (stopwatchAlreadyRunning) {
            // don't fire START_STOPWATCH or RESET_STOPWATCH if a stopwatch is already running
            if (ACTION_START_STOPWATCH.equals(action)) {
                final String reason = getString(R.string.stopwatch_already_running);
                Voice.notifyFailure(this, reason);
                LogUtils.i(reason);
                return;
            } else if (ACTION_RESET_STOPWATCH.equals(action)) { // RESET_STOPWATCH
                final String reason = getString(R.string.stopwatch_cant_be_reset_because_is_running);
                Voice.notifyFailure(this, reason);
                LogUtils.i(reason);
                return;
            }
        } else {
            // if a stopwatch isn't running, don't try to stop or lap it
            if (ACTION_STOP_STOPWATCH.equals(action) ||
                    ACTION_LAP_STOPWATCH.equals(action)) {
                final String reason = getString(R.string.stopwatch_isnt_running);
                Voice.notifyFailure(this, reason);
                LogUtils.i(reason);
                return;
            }
        }

        final String reason;
        // Events and voice interactor setup
        switch (action) {
            case ACTION_START_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_start, R.string.label_intent);
                reason = getString(R.string.stopwatch_started);
                break;
            case ACTION_STOP_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_stop, R.string.label_intent);
                reason = getString(R.string.stopwatch_stopped);
                break;
            case ACTION_LAP_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_lap, R.string.label_intent);
                reason = getString(R.string.stopwatch_lapped);
                break;
            case ACTION_RESET_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_reset, R.string.label_intent);
                reason = getString(R.string.stopwatch_reset);
                break;
            default:
                return;
        }
        final Intent intent = new Intent(mAppContext, StopwatchService.class).setAction(action);
        startService(intent);
        Voice.notifySuccess(this, reason);
        LogUtils.i(reason);
    }

    private void handleTimerIntent(final String action) {
        // Opens the UI for timers
        final Intent timerIntent = new Intent(mAppContext, DeskClock.class)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX)
                .putExtra(TimerFullScreenFragment.GOTO_SETUP_VIEW, false);
        startActivity(timerIntent);
        LogUtils.i("HandleDeskClockApiCalls " + action);

        if (ACTION_SHOW_TIMERS.equals(action)) {
            Events.sendTimerEvent(R.string.action_show, R.string.label_intent);
            return;
        }
        new HandleTimersAsync(mAppContext, action, this).execute();
    }

    private void handleClockIntent(final String action) {
        // Opens the UI for clocks
        final Intent handleClock = new Intent(mAppContext, DeskClock.class)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.CLOCK_TAB_INDEX);
        startActivity(handleClock);

        new HandleClockAsync(mAppContext, getIntent(), this).execute();
    }

    private static class HandleTimersAsync extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        private final String mAction;
        private final Activity mActivity;

        public HandleTimersAsync(Context context, String action, Activity activity) {
            mContext = context;
            mAction = action;
            mActivity = activity;
        }
        // STOP_TIMER and START_TIMER should only be triggered if there is one timer that is
        // not stopped or not started respectively. This method checks all timers to find only
        // one that corresponds to that.
        // Only change the mode of the timer if no disambiguation is necessary

        @Override
        protected Void doInBackground(Void... parameters) {
            final List<TimerObj> timers = new ArrayList<>();
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            TimerObj.getTimersFromSharedPrefs(prefs, timers);
            if (timers.isEmpty()) {
                final String reason = mContext.getString(R.string.no_timer_set);
                LogUtils.i(reason);
                Voice.notifyFailure(mActivity, reason);
                return null;
            }
            final TimerObj timer;
            final String timerAction;
            switch (mAction) {
                case ACTION_DELETE_TIMER: {
                    timerAction = Timers.DELETE_TIMER;
                    // Delete a timer only if there's one available
                    if (timers.size() > 1) {
                        final String reason = mContext.getString(R.string.multiple_timers_available);
                        LogUtils.i(reason);
                        Voice.notifyFailure(mActivity, reason);
                        return null;
                    }

                    timer = timers.get(0);
                    timer.deleteFromSharedPref(prefs);
                    Events.sendTimerEvent(R.string.action_delete, R.string.label_intent);
                    final String reason = mContext.getString(R.string.timer_deleted);
                    Voice.notifySuccess(mActivity, reason);
                    LogUtils.i(reason);
                    break;
                }
                case ACTION_START_TIMER: {
                    timerAction = Timers.START_TIMER;
                    timer = getTimerWithStateToIgnore(timers, TimerObj.STATE_RUNNING);
                    // Only start a timer if there's one non-running timer available
                    if (timer == null) {
                        // notifyFailure was already triggered
                        return null;
                    }
                    timer.setState(TimerObj.STATE_RUNNING);
                    timer.mStartTime = Utils.getTimeNow() - (timer.mSetupLength - timer.mTimeLeft);
                    timer.writeToSharedPref(prefs);
                    final String reason = mContext.getString(R.string.timer_started);
                    Voice.notifySuccess(mActivity, reason);
                    LogUtils.i(reason);
                    Events.sendTimerEvent(R.string.action_start, R.string.label_intent);
                    break;
                }
                case ACTION_RESET_TIMER: {
                    timerAction = Timers.RESET_TIMER;
                    // Since timer can be reset only if it's stopped
                    // it's only triggered when there's only one stopped timer
                    final Set<Integer> statesToInclude = new HashSet<>();
                    statesToInclude.add(TimerObj.STATE_STOPPED);
                    timer = getTimerWithStatesToInclude(timers, statesToInclude, mAction);
                    if (timer == null) {
                        return null;
                    }
                    final String reason = mContext.getString(R.string.timer_was_reset);
                    Voice.notifySuccess(mActivity, reason);
                    LogUtils.i(reason);
                    timer.setState(TimerObj.STATE_RESTART);
                    timer.mTimeLeft = timer.mSetupLength;
                    timer.writeToSharedPref(prefs);
                    Events.sendTimerEvent(R.string.action_reset, R.string.label_intent);
                    break;
                }
                case ACTION_STOP_TIMER: {
                    timerAction = Timers.STOP_TIMER;
                    final Set<Integer> statesToInclude = new HashSet<>();
                    statesToInclude.add(TimerObj.STATE_TIMESUP);
                    statesToInclude.add(TimerObj.STATE_RUNNING);
                    // Timer is stopped if there's only one running timer
                    timer = getTimerWithStatesToInclude(timers, statesToInclude, mAction);
                    if (timer == null) {
                        return null;
                    }
                    final String reason = mContext.getString(R.string.timer_stopped);
                    LogUtils.i(reason);
                    Voice.notifySuccess(mActivity, reason);
                    if (timer.mState == TimerObj.STATE_RUNNING) {
                        timer.setState(TimerObj.STATE_STOPPED);
                    } else {
                        // if the time is up on the timer
                        // restart it and reset the length
                        timer.setState(TimerObj.STATE_RESTART);
                        timer.mTimeLeft = timer.mSetupLength;
                    }
                    timer.writeToSharedPref(prefs);
                    Events.sendTimerEvent(R.string.action_stop, R.string.label_intent);
                    break;
                }
                default:
                    return null;
            }
            // updating the time for next firing timer
            final Intent i = new Intent()
                    .setAction(timerAction)
                    .putExtra(Timers.TIMER_INTENT_EXTRA, timer.mTimerId)
                    .putExtra(Timers.UPDATE_NEXT_TIMESUP, true)
                    // Make sure the receiver is getting the intent ASAP.
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            mContext.sendBroadcast(i);
            return null;
        }

        /**
         * @param timers available to the user
         * @param stateToIgnore the opposite of the state that the timer should be in
         * @return a timer only if there's one timer available that is of a state
         * other than the state that's passed
         * in all other cases returns null
         */
        private TimerObj getTimerWithStateToIgnore(List<TimerObj> timers, int stateToIgnore) {
            TimerObj soleTimer = null;
            for (TimerObj timer : timers) {
                if (timer.mState != stateToIgnore) {
                    if (soleTimer == null) {
                        soleTimer = timer;
                    } else {
                        // soleTimer has already been set
                        final String reason = mContext.getString(R.string.multiple_timers_available);
                        LogUtils.i(reason);
                        Voice.notifyFailure(mActivity, reason);
                        return null;
                    }
                }
            }
            return soleTimer;
        }

        /**
         * @param timers available to the user
         * @param statesToInclude acceptable states of the timer
         * @return a timer only if there's one timer available that is of the state
         * that is passed in
         * in all other cases returns null
         */
        private TimerObj getTimerWithStatesToInclude(
                List<TimerObj> timers, Set<Integer> statesToInclude, String action) {
            TimerObj soleTimer = null;
            for (TimerObj timer : timers) {
                if (statesToInclude.contains(timer.mState)) {
                    if (soleTimer == null) {
                        soleTimer = timer;
                    } else {
                        // soleTimer has already been set
                        final String reason = mContext.getString(
                                R.string.multiple_timers_available);
                        LogUtils.i(reason);
                        Voice.notifyFailure(mActivity, reason);
                        return null;
                    }
                }
            }
            // if there are no timers of desired property
            // announce it to the user
            if (soleTimer == null) {
                if (action.equals(ACTION_RESET_TIMER)) {
                    // all timers are running
                    final String reason = mContext.getString(
                            R.string.timer_cant_be_reset_because_its_running);
                    LogUtils.i(reason);
                    Voice.notifyFailure(mActivity, reason);
                } else if (action.equals(ACTION_STOP_TIMER)) {
                    // no running timers
                    final String reason = mContext.getString(R.string.timer_already_stopped);
                    LogUtils.i(reason);
                    Voice.notifyFailure(mActivity, reason);
                }
            }
            return soleTimer;
        }
    }

    private static class HandleClockAsync extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        private final Intent mIntent;
        private final Activity mActivity;

        public HandleClockAsync(Context context, Intent intent, Activity activity) {
            mContext = context;
            mIntent = intent;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... parameters) {
            final String cityExtra = mIntent.getStringExtra(EXTRA_CITY);
            final SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(mContext);
            switch (mIntent.getAction()) {
                case ACTION_ADD_CLOCK: {
                    // if a city isn't specified open CitiesActivity to choose a city
                    if (cityExtra == null) {
                        final String reason = mContext.getString(R.string.no_city_selected);
                        Voice.notifyFailure(mActivity, reason);
                        LogUtils.i(reason);
                        startCitiesActivity();
                        Events.sendClockEvent(R.string.action_create, R.string.label_intent);
                        break;
                    }

                    // if a city is passed add that city to the list
                    final Map<String, CityObj> cities = Utils.loadCityMapFromXml(mContext);
                    final CityObj city = cities.get(cityExtra.toLowerCase());
                    // check if this city exists in the list of available cities
                    if (city == null) {
                        final String reason = mContext.getString(
                                R.string.the_city_you_specified_is_not_available);
                        Voice.notifyFailure(mActivity, reason);
                        LogUtils.i(reason);
                        break;
                    }

                    final HashMap<String, CityObj> selectedCities =
                            Cities.readCitiesFromSharedPrefs(prefs);
                    // if this city is already added don't add it
                    if (selectedCities.put(city.mCityId, city) != null) {
                        final String reason = mContext.getString(R.string.the_city_already_added);
                        Voice.notifyFailure(mActivity, reason);
                        LogUtils.i(reason);
                        break;
                    }

                    Cities.saveCitiesToSharedPrefs(prefs, selectedCities);
                    final String reason = mContext.getString(R.string.city_added, city.mCityName);
                    Voice.notifySuccess(mActivity, reason);
                    LogUtils.i(reason);
                    Events.sendClockEvent(R.string.action_start, R.string.label_intent);
                    break;
                }
                case ACTION_DELETE_CLOCK: {
                    if (cityExtra == null) {
                        // if a city isn't specified open CitiesActivity to choose a city
                        final String reason = mContext.getString(R.string.no_city_selected);
                        Voice.notifyFailure(mActivity, reason);
                        LogUtils.i(reason);
                        startCitiesActivity();
                        Events.sendClockEvent(R.string.action_create, R.string.label_intent);
                        break;
                    }

                    // if a city is specified check if it's selected and if so delete it
                    final Map<String, CityObj> cities = Utils.loadCityMapFromXml(mContext);
                    // check if this city exists in the list of available cities
                    final CityObj city = cities.get(cityExtra.toLowerCase());
                    if (city == null) {
                        final String reason = mContext.getString(
                                R.string.the_city_you_specified_is_not_available);
                        Voice.notifyFailure(mActivity, reason);
                        LogUtils.i(reason);
                        break;
                    }

                    final HashMap<String, CityObj> selectedCities =
                            Cities.readCitiesFromSharedPrefs(prefs);
                    if (selectedCities.remove(city.mCityId) != null) {
                        final String reason = mContext.getString(R.string.city_deleted,
                                city.mCityName);
                        Voice.notifySuccess(mActivity, reason);
                        LogUtils.i(reason);
                        Cities.saveCitiesToSharedPrefs(prefs, selectedCities);
                        Events.sendClockEvent(R.string.action_delete, R.string.label_intent);
                    } else {
                        // the specified city hasn't been added to the user's list yet
                        Voice.notifyFailure(mActivity, mContext.getString(
                                R.string.the_city_you_specified_is_not_available));
                    }
                    break;
                }
                case ACTION_SHOW_CLOCK:
                    Events.sendClockEvent(R.string.action_show, R.string.label_intent);
                    break;
            }
            return null;
        }

        private void startCitiesActivity() {
            mContext.startActivity(new Intent(mContext, CitiesActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}

