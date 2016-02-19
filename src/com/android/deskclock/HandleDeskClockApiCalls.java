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
import android.os.Bundle;

import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.events.Events;
import com.android.deskclock.worldclock.CitySelectionActivity;

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

    // shows the tab with timers; optionally scrolls to a specific timer
    public static final String ACTION_SHOW_TIMERS = ACTION_PREFIX + "SHOW_TIMERS";
    // pauses running timers; resets expired timers
    public static final String ACTION_PAUSE_TIMER = ACTION_PREFIX + "PAUSE_TIMER";
    // starts the sole timer
    public static final String ACTION_START_TIMER = ACTION_PREFIX + "START_TIMER";
    // resets the timer
    public static final String ACTION_RESET_TIMER = ACTION_PREFIX + "RESET_TIMER";
    // adds an extra minute to the timer
    public static final String ACTION_ADD_MINUTE_TIMER = ACTION_PREFIX + "ADD_MINUTE_TIMER";

    // extra for many actions specific to a given timer
    public static final String EXTRA_TIMER_ID =
            "com.android.deskclock.extra.TIMER_ID";

    // Describes the entity responsible for the action being performed.
    public static final String EXTRA_EVENT_LABEL = "com.android.deskclock.extra.EVENT_LABEL";

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
                case ACTION_RESET_TIMER:
                case ACTION_PAUSE_TIMER:
                case ACTION_START_TIMER:
                    handleTimerIntent(intent);
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
        final int eventLabel = intent.getIntExtra(EXTRA_EVENT_LABEL, R.string.label_intent);

        if (ACTION_SHOW_STOPWATCH.equals(action)) {
            Events.sendStopwatchEvent(R.string.action_show, eventLabel);
        } else {
            final String reason;
            boolean fail = false;
            switch (action) {
                case ACTION_START_STOPWATCH: {
                    DataModel.getDataModel().startStopwatch();
                    Events.sendStopwatchEvent(R.string.action_start, eventLabel);
                    reason = getString(R.string.stopwatch_started);
                    break;
                }
                case ACTION_PAUSE_STOPWATCH: {
                    DataModel.getDataModel().pauseStopwatch();
                    Events.sendStopwatchEvent(R.string.action_pause, eventLabel);
                    reason = getString(R.string.stopwatch_paused);
                    break;
                }
                case ACTION_RESET_STOPWATCH: {
                    DataModel.getDataModel().clearLaps();
                    DataModel.getDataModel().resetStopwatch();
                    Events.sendStopwatchEvent(R.string.action_reset, eventLabel);
                    reason = getString(R.string.stopwatch_reset);
                    break;
                }
                case ACTION_LAP_STOPWATCH: {
                    if (!DataModel.getDataModel().getStopwatch().isRunning()) {
                        fail = true;
                        reason = getString(R.string.stopwatch_isnt_running);
                    } else {
                        DataModel.getDataModel().addLap();
                        Events.sendStopwatchEvent(R.string.action_lap, eventLabel);
                        reason = getString(R.string.stopwatch_lapped);
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown stopwatch action: " + action);
            }

            if (fail) {
                Voice.notifyFailure(this, reason);
            } else {
                Voice.notifySuccess(this, reason);
            }
            LogUtils.i(reason);
        }

        // Open the UI to the stopwatch.
        final Intent stopwatchIntent = new Intent(mAppContext, DeskClock.class)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.STOPWATCH_TAB_INDEX);
        startActivity(stopwatchIntent);
    }

    private void handleTimerIntent(Intent intent) {
        final String action = intent.getAction();

        // Determine where this intent originated.
        final int eventLabel = intent.getIntExtra(EXTRA_EVENT_LABEL, R.string.label_intent);
        int timerId = intent.getIntExtra(EXTRA_TIMER_ID, -1);
        Timer timer = null;

        if (ACTION_SHOW_TIMERS.equals(action)) {
            Events.sendTimerEvent(R.string.action_show, eventLabel);
        } else {
            String reason = null;
            if (timerId == -1) {
                // No timer id was given explicitly, so check if only one timer exists.
                final List<Timer> timers =  DataModel.getDataModel().getTimers();
                if (timers.isEmpty()) {
                    // No timers exist to control.
                    reason = getString(R.string.no_timers_exist);
                } else if (timers.size() > 1) {
                    // Many timers exist so the control command is ambiguous.
                    reason = getString(R.string.too_many_timers_exist);
                } else {
                    timer = timers.get(0);
                }
            } else {
                // Verify that the given timer does exist.
                timer = DataModel.getDataModel().getTimer(timerId);
                if (timer == null) {
                    reason = getString(R.string.timer_does_not_exist);
                }
            }

            if (timer == null) {
                Voice.notifyFailure(this, reason);
            } else {
                timerId = timer.getId();

                // Otherwise the control command can be honored.
                switch (action) {
                    case ACTION_RESET_TIMER: {
                        DataModel.getDataModel().resetOrDeleteTimer(timer, eventLabel);
                        if (timer.isExpired() && timer.getDeleteAfterUse()) {
                            timerId = -1;
                            reason = getString(R.string.timer_deleted);
                        } else {
                            reason = getString(R.string.timer_was_reset);
                        }
                        break;
                    }
                    case ACTION_START_TIMER: {
                        DataModel.getDataModel().startTimer(timer);
                        Events.sendTimerEvent(R.string.action_start, eventLabel);
                        reason = getString(R.string.timer_started);
                        break;
                    }
                    case ACTION_PAUSE_TIMER: {
                        DataModel.getDataModel().pauseTimer(timer);
                        Events.sendTimerEvent(R.string.action_pause, eventLabel);
                        reason = getString(R.string.timer_paused);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("unknown timer action: " + action);
                }

                Voice.notifySuccess(this, reason);
            }

            LogUtils.i(reason);
        }

        // Open the UI to the timers.
        final Intent showTimers = new Intent(mAppContext, DeskClock.class)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX);
        if (timerId != -1) {
            showTimers.putExtra(EXTRA_TIMER_ID, timerId);
        }
        startActivity(showTimers);
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
}