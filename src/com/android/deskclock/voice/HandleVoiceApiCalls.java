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
import android.app.VoiceInteractor;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class HandleVoiceApiCalls extends Activity {
    private static final String ACTION_PREFIX = "com.android.deskclock.action.";

    // shows the tab with world clocks
    public static final String VOICE_ACTION_SHOW_CLOCK = ACTION_PREFIX + "VOICE_SHOW_CLOCK";
    // add a clock of a selected city, if no city is specified opens the city selection screen
    public static final String VOICE_ACTION_ADD_CLOCK = ACTION_PREFIX + "VOICE_ADD_CLOCK";
    // TODO: change default behavior from deleting a random city. Only delete if EXTRA_CITY
    // is specified.
    // delete a clock of a selected city, if no city is specified deletes one of the selected cities
    public static final String VOICE_ACTION_DELETE_CLOCK = ACTION_PREFIX + "VOICE_DELETE_CLOCK";
    // extra for VOICE_ACTION_ADD_CLOCK and VOICE_ACTION_DELETE_CLOCK
    public static final String EXTRA_CITY = "com.android.deskclock.extra.clock.CITY";

    // dismiss upcoming alarm (if it's last added)
    public static final String VOICE_ACTION_DISMISS_UPCOMING_ALARM = ACTION_PREFIX
            + "VOICE_DISMISS_UPCOMING_ALARM";

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
                case AlarmClock.ACTION_VOICE_CANCEL_ALARM:
                case AlarmClock.ACTION_VOICE_DELETE_ALARM:
                case VOICE_ACTION_DISMISS_UPCOMING_ALARM:
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
        final Intent alarmIntent = Alarm.createIntent(this, DeskClock.class, Alarm.INVALID_ID)
                .setAction(action)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
        startActivity(alarmIntent);

        // choose the most recently created  alarm if there is no other way to find out
        // which alarm the user means
        final Context context = this;
        final Intent intent = getIntent();

        switch (action) {
            case AlarmClock.ACTION_VOICE_CANCEL_ALARM:
            case AlarmClock.ACTION_VOICE_DELETE_ALARM:
                new AsyncTask<Void, Void, List<Alarm>>() {
                    @Override
                    protected List<Alarm> doInBackground(Void... parameters) {
                        // Check if the alarm already exists and handle it
                        final List<Alarm> alarms = Alarm.getAlarms(getContentResolver(), null);
                        if (alarms.isEmpty()) {
                            LogUtils.i("No alarms to cancel or delete.");
                            return null;
                        }
                        final String searchMode =
                                intent.getStringExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE);
                        return processAlarmSearchMode(alarms, searchMode, action);
                    }

                    @Override
                    protected void onPostExecute(List<Alarm> alarms) {
                        if (alarms != null) {
                            // TODO: if SEARCH_MODE is empty launch UI to pick an alarm
                            // alarms are used by pickAlarm
                        }
                    }
                }.execute();
                break;
            case VOICE_ACTION_DISMISS_UPCOMING_ALARM:
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... parameters) {
                        // checks if the next firing alarm is coming up in less than 2 hours
                        final AlarmInstance nextAlarm =
                                AlarmStateManager.getNextFiringAlarm(context);
                        if (nextAlarm != null) {
                            final Calendar nextAlarmTime = nextAlarm.getAlarmTime();
                            if (isWithinTwoHours(nextAlarmTime)) {
                                AlarmStateManager.setDismissState(context, nextAlarm);
                                LogUtils.i("Dismiss " + nextAlarm.getAlarmTime());
                                Events.sendAlarmEvent(R.string.action_dismiss,
                                        R.string.label_voice);
                            } else {
                                LogUtils.i("Alarm " + nextAlarm.getAlarmTime().toString()
                                        + " wasn't dismissed, still more than two hours away.");
                            }
                        } else {
                            LogUtils.i("No upcoming alarms to dismiss");
                        }
                        return null;
                    }
                 }.execute();
                break;
        }
    }

    private List<Alarm> processAlarmSearchMode(List<Alarm> alarms, String searchMode,
                String action) {
        // returns alarms to be processed by pickAlarms that shows the UI to pick an alarm
        if (searchMode == null) {
            return alarms;
        }

        final Collection<Alarm> matchingAlarms = new ArrayList<>();
        switch (searchMode) {
            case AlarmClock.ALARM_SEARCH_MODE_TIME:
                // at least one of these has to be specified in this search mode.
                final int hour = getIntent().getIntExtra(AlarmClock.EXTRA_HOUR, -1);
                // if minutes weren't specified default to 0
                final int minutes = getIntent().getIntExtra(AlarmClock.EXTRA_MINUTES, 0);
                final boolean isPm = getIntent().getBooleanExtra(AlarmClock.EXTRA_IS_PM, false);
                final int hour24 = (isPm && hour < 12) ? (hour + 12) : hour;

                if (hour24 < 0 || hour24 > 23 || minutes < 0 || minutes > 59) {
                    final String pmam = isPm? "pm" : "am";
                    notifyFailure("Invalid time specified: " + hour24 + ":" + minutes + " " + pmam);
                    LogUtils.e("Invalid time specified: " + hour24 + ":" + minutes + " " + pmam);
                    return null;
                }
                final List<Alarm> selectedAlarms = getAlarmsByHourMinutes(hour24, minutes);
                if (selectedAlarms.isEmpty()) {
                    LogUtils.i("No alarm at %d:%d.", hour24, minutes);
                    return null;
                } else if (selectedAlarms.size() == 1){
                    matchingAlarms.add(selectedAlarms.get(0));
                } else {
                    // TODO: if there are multiple matching ones show the UI to pick alarms
                }
                break;
            case AlarmClock.ALARM_SEARCH_MODE_NEXT:
                // TODO: handle the case when there are multiple alarms firing at the same time
                final AlarmInstance nextAlarm = AlarmStateManager.getNextFiringAlarm(this);
                matchingAlarms.add(Alarm.getAlarm(getContentResolver(), nextAlarm.mAlarmId));
                break;
            case AlarmClock.ALARM_SEARCH_MODE_ALL:
                matchingAlarms.addAll(alarms);
                break;
            default:
                // TODO: if SEARCH_MODE is empty launch UI to pick an alarm
                return null;
        }

        for (Alarm alarm : matchingAlarms) {
            switch (action) {
                case AlarmClock.ACTION_VOICE_CANCEL_ALARM:
                    disableAlarm(alarm);
                    break;
                case AlarmClock.ACTION_VOICE_DELETE_ALARM:
                    deleteAlarm(alarm);
                    break;
            }
        }

        return null;
    }

    private List<Alarm> getAlarmsByHourMinutes(int hour24, int minutes) {
        final List<String> args = new ArrayList<>();
        final String selection = String.format("%s=? AND %s=?", Alarm.HOUR, Alarm.MINUTES);
        args.add(String.valueOf(hour24));
        args.add(String.valueOf(minutes));

        return Alarm.getAlarms(getContentResolver(), selection,
                args.toArray(new String[args.size()]));
    }

    private boolean isWithinTwoHours(Calendar nextAlarmTime) {
        final long nextAlarmTimeMillis =
                nextAlarmTime.getTimeInMillis();
        final long twoHours = AlarmInstance.LOW_NOTIFICATION_HOUR_OFFSET * -1 *
                DateUtils.HOUR_IN_MILLIS;
        return nextAlarmTimeMillis - System.currentTimeMillis() <= twoHours;
    }

    protected void notifySuccess(CharSequence prompt) {
        if (getVoiceInteractor() != null) {
            getVoiceInteractor().submitRequest(
                    new VoiceInteractor.CompleteVoiceRequest(prompt, null));
        }
    }

    /**
     * Indicates when the setting could not be changed.
     */
    protected void notifyFailure(CharSequence reason) {
        getVoiceInteractor().submitRequest(
                new VoiceInteractor.AbortVoiceRequest(reason, null));
    }

    private void disableAlarm(Alarm alarm) {
        final ContentResolver cr = getContentResolver();
        alarm.enabled = false;
        Alarm.updateAlarm(cr, alarm);
        LogUtils.i("Cancel alarm %s", alarm);
        notifySuccess("Alarm " + alarm + " is canceled.");
        Events.sendAlarmEvent(R.string.action_disable, R.string.label_voice);
    }

    private void deleteAlarm(Alarm alarm) {
        final ContentResolver cr = getContentResolver();
        AlarmStateManager.deleteAllInstances(this, alarm.id);
        Alarm.deleteAlarm(cr, alarm.id);
        LogUtils.i("Delete alarm %s", alarm);
        notifySuccess("Alarm " + alarm + " is deleted.");
        Events.sendAlarmEvent(R.string.action_delete, R.string.label_voice);
    }

    private void handleStopwatchIntent(String action) {
        // Opens the UI for stopwatch
        final Intent stopwatchIntent = new Intent(this, DeskClock.class)
                .setAction(action)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.STOPWATCH_TAB_INDEX);
        startActivity(stopwatchIntent);
        LogUtils.i("HandleVoiceApiCalls " + action);

        // checking if the stopwatch is already running
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean stopwatchAlreadyRunning =
                prefs.getBoolean(Stopwatches.NOTIF_CLOCK_RUNNING, false);

        if (stopwatchAlreadyRunning) {
            // don't fire START_STOPWATCH or RESET_STOPWATCH if a stopwatch is already running
            if (VOICE_ACTION_START_STOPWATCH.equals(action) ||
                    VOICE_ACTION_RESET_STOPWATCH.equals(action)) {
                LogUtils.i("Can't fire " + action + " if stopwatch already running");
                return;
            }
        } else {
            // if a stopwatch isn't running, don't try to stop or lap it
            if (VOICE_ACTION_STOP_STOPWATCH.equals(action) ||
                VOICE_ACTION_LAP_STOPWATCH.equals(action)) {
                notifyFailure("Can't fire " + action + " if stopwatch isn't running");
                LogUtils.i("Can't fire " + action + " if stopwatch isn't running");
                return;
            }
         }

        // Events setup
        switch (action) {
            case VOICE_ACTION_START_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_start, R.string.label_voice);
                break;
            case VOICE_ACTION_STOP_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_stop, R.string.label_voice);
                break;
            case VOICE_ACTION_LAP_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_lap, R.string.label_voice);
                break;
            case VOICE_ACTION_SHOW_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_show, R.string.label_voice);
                break;
            case VOICE_ACTION_RESET_STOPWATCH:
                Events.sendStopwatchEvent(R.string.action_reset, R.string.label_voice);
                break;
            default:
                return;
        }

        final Intent intent = new Intent(this, StopwatchService.class).setAction(action);
        startService(intent);
        notifySuccess("Stopwatch mode was changed.");
    }

    private void handleTimerIntent(final String action) {
        // Opens the UI for timers
        final Intent timerIntent = new Intent(this, DeskClock.class)
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

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {

                final List<TimerObj> timers = new ArrayList<>();
                final SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(HandleVoiceApiCalls.this);
                TimerObj.getTimersFromSharedPrefs(prefs, timers);
                if (timers.isEmpty()) {
                    return null;
                }

                // Delete the most recently created timer, the one that
                // shows up first on the Timers tab
                final TimerObj timer = timers.get(timers.size() - 1);

                switch (action) {
                    case VOICE_ACTION_DELETE_TIMER:
                        timer.deleteFromSharedPref(prefs);
                        Events.sendTimerEvent(R.string.action_delete, R.string.label_voice);
                        break;
                    case VOICE_ACTION_START_TIMER:
                        // adjust mStartTime to reflect that starting time changed,
                        // VOICE_ACTION_START_TIMER considers that a part of
                        // the timer's length might have elapsed
                        if (timer.mState == TimerObj.STATE_RUNNING) {
                            return null;
                        }
                        timer.setState(TimerObj.STATE_RUNNING);
                        timer.mStartTime = Utils.getTimeNow() -
                                (timer.mSetupLength - timer.mTimeLeft);
                        timer.writeToSharedPref(prefs);
                        Events.sendTimerEvent(R.string.action_start, R.string.label_voice);
                        break;
                    case VOICE_ACTION_RESET_TIMER:
                        // timer can be reset only if it's stopped
                        if (timer.mState == TimerObj.STATE_STOPPED) {
                            timer.mTimeLeft = timer.mOriginalLength;
                            timer.writeToSharedPref(prefs);
                            Events.sendTimerEvent(R.string.action_reset, R.string.label_voice);
                        } else {
                            LogUtils.i("Timer can't be reset because it isn't stopped");
                            return null;
                        }
                        break;
                    case VOICE_ACTION_STOP_TIMER:
                        if (timer.mState == TimerObj.STATE_STOPPED) {
                            return null;
                        }
                        timer.setState(TimerObj.STATE_STOPPED);
                        timer.writeToSharedPref(prefs);
                        Events.sendTimerEvent(R.string.action_stop, R.string.label_voice);
                        break;
                }
                return null;
            }
        }.execute();
        LogUtils.i("HandleTimerIntent", action);
    }


    private void handleClockIntent(final String action) {
        // Opens the UI for clocks
        final Intent handleClock = new Intent(this, DeskClock.class)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.CLOCK_TAB_INDEX);
        startActivity(handleClock);

        final Context context = this;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                final String cityExtra = getIntent().getStringExtra(EXTRA_CITY);
                final SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(context);
                switch (action) {
                    case VOICE_ACTION_ADD_CLOCK:
                        // if a city isn't specified open CitiesActivity to choose a city
                        if (cityExtra == null) {
                            startActivity( new Intent(context, CitiesActivity.class));
                            Events.sendClockEvent(R.string.action_create, R.string.label_voice);
                        } else {
                            final String cityExtraFormatted = formatToTitleCase(cityExtra);
                            // if a city is passed add that city to the list
                            final HashMap<String, CityObj> cities =
                                    Utils.loadCityMapFromXml(HandleVoiceApiCalls.this);
                            // check if this city exists in the list of available cities
                            if (!cities.containsKey(cityExtraFormatted)) {
                                return null;
                            }
                            final HashMap<String, CityObj> selectedCities =
                                    Cities.readCitiesFromSharedPrefs(prefs);
                            final CityObj city = cities.get(cityExtraFormatted);
                            // if this city is already added don't add it
                            if (selectedCities.containsKey(city.mCityId)) {
                                return null;
                            }
                            final CityObj selectedCity = cities.get(cityExtraFormatted);
                            selectedCities.put(selectedCity.mCityId, selectedCity);
                            Cities.saveCitiesToSharedPrefs(prefs, selectedCities);
                            Events.sendClockEvent(R.string.action_start,
                                    R.string.label_voice);
                            return null;
                        }

                        break;
                    case VOICE_ACTION_DELETE_CLOCK:
                        final HashMap<String, CityObj> selectedCities =
                                Cities.readCitiesFromSharedPrefs(prefs);
                        final Collection<CityObj> col = selectedCities.values();
                        // if there's no extra delete a city from the list
                        if (cityExtra == null) {
                            // TODO: change default behavior to delete a city only if it's specified
                            // if it's not specified show a UI to pick the city
                            final Iterator<CityObj> i = col.iterator();
                            // gets one of the cities. Cities are not sorted by any logical order.
                            if (i.hasNext()) {
                                final CityObj city = i.next();
                                selectedCities.remove(city.mCityId);
                                Cities.saveCitiesToSharedPrefs(prefs, selectedCities);
                                Events.sendClockEvent(R.string.action_delete, R.string.label_voice);
                            }
                        } else {
                            final String cityExtraFormatted = formatToTitleCase(cityExtra);
                            // if a city is specified check if it's selected and if so delete it
                            final HashMap<String, CityObj> cities =
                                    Utils.loadCityMapFromXml(HandleVoiceApiCalls.this);
                            // check if this city exists in the list of available cities
                            final CityObj city = cities.get(cityExtraFormatted);
                            if (city == null) {
                                return null;
                            }
                            if (selectedCities.containsKey(city.mCityId)) {
                                selectedCities.remove(city.mCityId);
                                Cities.saveCitiesToSharedPrefs(prefs, selectedCities);
                                Events.sendClockEvent(R.string.action_delete,
                                        R.string.label_voice);
                            }
                        }
                        break;
                    case VOICE_ACTION_SHOW_CLOCK:
                        Events.sendClockEvent(R.string.action_show, R.string.label_voice);
                        break;
                }
                return null;
            }
        }.execute();
    }

    // TODO: replace with toLowerCase
    private String formatToTitleCase(String city) {
        final StringBuilder stringBuilder = new StringBuilder(city);
        stringBuilder.replace(0, stringBuilder.length(), stringBuilder.toString().toLowerCase())
                .setCharAt(0, Character.toTitleCase(stringBuilder.charAt(0)));
        return stringBuilder.toString();

    }
}
