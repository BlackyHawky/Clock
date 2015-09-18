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

import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Stopwatch;
import com.android.deskclock.events.Events;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.timer.TimerFullScreenFragment;
import com.android.deskclock.timer.TimerObj;
import com.android.deskclock.timer.Timers;
import com.android.deskclock.worldclock.CitySelectionActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HandleDeskClockApiCalls extends Activity {
    private Context mAppContext;

    private static final String ACTION_PREFIX = "com.android.deskclock.action.";

    // shows the tab with world clocks
    public static final String ACTION_SHOW_CLOCK = ACTION_PREFIX + "SHOW_CLOCK";
    // extra for ACTION_SHOW_CLOCK indicating the clock is being displayed from tapping the widget
    public static final String EXTRA_FROM_WIDGET = "com.android.deskclock.extra.clock.FROM_WIDGET";
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
    // pauses the current stopwatch that's currently running
    public static final String ACTION_PAUSE_STOPWATCH = ACTION_PREFIX + "PAUSE_STOPWATCH";
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

    // extra for actions originating from the notifications
    public static final String EXTRA_FROM_NOTIFICATION =
            "com.android.deskclock.extra.FROM_NOTIFICATION";

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
            LogUtils.i("HandleDeskClockApiCalls " + action);

            switch (action) {
                case ACTION_START_STOPWATCH:
                case ACTION_PAUSE_STOPWATCH:
                case ACTION_LAP_STOPWATCH:
                case ACTION_SHOW_STOPWATCH:
                case ACTION_RESET_STOPWATCH:
                    handleStopwatchIntent(intent);
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
                    handleClockIntent(intent);
                    break;
            }
        } finally {
            finish();
        }
    }

    private void handleStopwatchIntent(Intent intent) {
        final String action = intent.getAction();

        // Determine where this intent originated.
        final boolean fromNotif =
                intent.getBooleanExtra(HandleDeskClockApiCalls.EXTRA_FROM_NOTIFICATION, false);
        final int label = fromNotif ? R.string.label_notification : R.string.label_intent;

        if (ACTION_SHOW_STOPWATCH.equals(action)) {
            Events.sendStopwatchEvent(R.string.action_show, label);
        } else {
            final Stopwatch stopwatch = DataModel.getDataModel().getStopwatch();

            final String reason;
            boolean fail = false;
            switch (action) {
                case ACTION_START_STOPWATCH: {
                    if (stopwatch.isRunning()) {
                        fail = true;
                        reason = getString(R.string.stopwatch_already_running);
                    } else {
                        Events.sendStopwatchEvent(R.string.action_start, label);
                        reason = getString(R.string.stopwatch_started);
                    }
                    break;
                }
                case ACTION_PAUSE_STOPWATCH: {
                    if (!stopwatch.isRunning()) {
                        fail = true;
                        reason = getString(R.string.stopwatch_isnt_running);
                    } else {
                        Events.sendStopwatchEvent(R.string.action_pause, label);
                        reason = getString(R.string.stopwatch_paused);
                    }
                    break;
                }
                case ACTION_LAP_STOPWATCH: {
                    if (!stopwatch.isRunning()) {
                        fail = true;
                        reason = getString(R.string.stopwatch_isnt_running);
                    } else {
                        Events.sendStopwatchEvent(R.string.action_lap, label);
                        reason = getString(R.string.stopwatch_lapped);
                    }
                    break;
                }
                case ACTION_RESET_STOPWATCH: {
                    if (stopwatch.isRunning()) {
                        fail = true;
                        reason = getString(R.string.stopwatch_cant_be_reset_because_is_running);
                    } else {
                        Events.sendStopwatchEvent(R.string.action_reset, label);
                        reason = getString(R.string.stopwatch_reset);
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown stopwatch action: " + action);
            }

            if (fail) {
                Voice.notifyFailure(this, reason);
            } else {
                // Perform the action on the stopwatch.
                final Intent performActionIntent = new Intent(mAppContext, StopwatchService.class)
                        .setAction(action);
                startService(performActionIntent);
                Voice.notifySuccess(this, reason);
            }
            LogUtils.i(reason);
        }

        // Open the UI to the stopwatch.
        final Intent stopwatchIntent = new Intent(mAppContext, DeskClock.class)
                .setAction(action)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.STOPWATCH_TAB_INDEX);
        startActivity(stopwatchIntent);
    }

    private void handleTimerIntent(final String action) {
        // Opens the UI for timers
        final Intent timerIntent = new Intent(mAppContext, DeskClock.class)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX)
                .putExtra(TimerFullScreenFragment.GOTO_SETUP_VIEW, false);
        startActivity(timerIntent);

        if (ACTION_SHOW_TIMERS.equals(action)) {
            Events.sendTimerEvent(R.string.action_show, R.string.label_intent);
            return;
        }
        new HandleTimersAsync(mAppContext, action, this).execute();
    }

    private void handleClockIntent(Intent intent) {
        final String action = intent.getAction();

        if (ACTION_SHOW_CLOCK.equals(action)) {
            final boolean fromWidget = intent.getBooleanExtra(EXTRA_FROM_WIDGET, false);
            final int label = fromWidget ? R.string.label_widget : R.string.label_intent;
            Events.sendClockEvent(R.string.action_show, label);
        } else {
            final String cityName = intent.getStringExtra(EXTRA_CITY);

            final String reason;
            boolean fail = false;

            // If no city was given, start the city chooser.
            if (cityName == null) {
                reason = getString(R.string.no_city_selected);
                LogUtils.i(reason);
                Voice.notifySuccess(this, reason);
                startActivity(new Intent(this, CitySelectionActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                switch (action) {
                    case ACTION_ADD_CLOCK:
                        Events.sendClockEvent(R.string.action_add, R.string.label_intent);
                        break;
                    case ACTION_DELETE_CLOCK:
                        Events.sendClockEvent(R.string.action_delete, R.string.label_intent);
                        break;
                }
                return;
            }

            // If a city was given, ensure it can be located.
            final City city = DataModel.getDataModel().getCity(cityName);
            if (city == null) {
                reason = getString(R.string.the_city_you_specified_is_not_available);
                LogUtils.i(reason);
                Voice.notifyFailure(this, reason);
                switch (action) {
                    case ACTION_ADD_CLOCK:
                        Events.sendClockEvent(R.string.action_add, R.string.label_intent);
                        break;
                    case ACTION_DELETE_CLOCK:
                        Events.sendClockEvent(R.string.action_delete, R.string.label_intent);
                        break;
                }
                return;
            }

            final Set<City> selectedCities =
                    Utils.newArraySet(DataModel.getDataModel().getSelectedCities());

            switch (action) {
                case ACTION_ADD_CLOCK: {
                    // Fail if the city is already present.
                    if (!selectedCities.add(city)) {
                        fail = true;
                        reason = getString(R.string.the_city_already_added);
                        break;
                    }

                    // Otherwise report the success.
                    DataModel.getDataModel().setSelectedCities(selectedCities);
                    reason = getString(R.string.city_added, city.getName());
                    Events.sendClockEvent(R.string.action_add, R.string.label_intent);
                    break;
                }
                case ACTION_DELETE_CLOCK: {
                    // Fail if the city is not present.
                    if (!selectedCities.remove(city)) {
                        fail = true;
                        reason = getString(R.string.the_city_you_specified_is_not_available);
                        break;
                    }

                    // Otherwise report the success.
                    DataModel.getDataModel().setSelectedCities(selectedCities);
                    reason = getString(R.string.city_deleted, city.getName());
                    Events.sendClockEvent(R.string.action_delete, R.string.label_intent);
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown clock action: " + action);
            }

            if (fail) {
                Voice.notifyFailure(this, reason);
            } else {
                Voice.notifySuccess(this, reason);
            }
            LogUtils.i(reason);
        }

        // Opens the UI for clocks
        final Intent clockIntent = new Intent(mAppContext, DeskClock.class)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.CLOCK_TAB_INDEX);
        startActivity(clockIntent);
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
}
