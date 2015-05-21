/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.deskclock.timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import com.android.deskclock.R;
import com.android.deskclock.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TimerObj implements Parcelable {

    public static final String KEY_NEXT_TIMER_ID = "next_timer_id";

    private static final String TAG = "TimerObj";
    // Max timer length is 9 hours + 99 minutes + 99 seconds
    public static final long MAX_TIMER_LENGTH = (9 * 3600 + 99 * 60  + 99) * 1000;
    public static final long MINUTE_IN_MILLIS = 60 * 1000;

    public int mTimerId;             // Unique id
    public long mStartTime;          // With mTimeLeft , used to calculate the correct time
    public long mTimeLeft;           // in the timer.
    public long mOriginalLength;     // length set at start of timer and by +1 min after times up
    public long mSetupLength;        // length set at start of timer
    public TimerListItem mView;
    public int mState;
    public int mPriorState;
    public String mLabel;
    public boolean mDeleteAfterUse;

    public static final int STATE_RUNNING = 1;
    public static final int STATE_STOPPED = 2;
    public static final int STATE_TIMESUP = 3;
    public static final int STATE_RESTART = 4;
    public static final int STATE_DELETED = 5;

    private static final String PREF_TIMER_ID = "timer_id_";
    private static final String PREF_START_TIME  = "timer_start_time_";
    private static final String PREF_TIME_LEFT = "timer_time_left_";
    private static final String PREF_ORIGINAL_TIME = "timer_original_timet_";
    private static final String PREF_SETUP_TIME = "timer_setup_timet_";
    private static final String PREF_STATE = "timer_state_";
    private static final String PREF_PRIOR_STATE = "timer_prior_state_";
    private static final String PREF_LABEL = "timer_label_";
    private static final String PREF_DELETE_AFTER_USE = "delete_after_use_";

    private static final String PREF_TIMERS_LIST = "timers_list";

    public static final Parcelable.Creator<TimerObj> CREATOR = new Parcelable.Creator<TimerObj>() {
        @Override
        public TimerObj createFromParcel(Parcel p) {
            return new TimerObj(p);
        }

        @Override
        public TimerObj[] newArray(int size) {
            return new TimerObj[size];
        }
    };

    public void writeToSharedPref(SharedPreferences prefs) {
        final String id = Integer.toString(mTimerId);

        final Set<String> timerIds = getTimerIds(prefs);
        timerIds.add(id);

        final SharedPreferences.Editor editor = prefs.edit()
                .putInt(PREF_TIMER_ID + id, mTimerId)
                .putLong(PREF_START_TIME + id, mStartTime)
                .putLong(PREF_TIME_LEFT + id, mTimeLeft)
                .putLong(PREF_ORIGINAL_TIME + id, mOriginalLength)
                .putLong(PREF_SETUP_TIME + id, mSetupLength)
                .putInt(PREF_STATE + id, mState)
                .putInt(PREF_PRIOR_STATE + id, mPriorState)
                .putStringSet(PREF_TIMERS_LIST, timerIds)
                .putString(PREF_LABEL + id, mLabel)
                .putBoolean(PREF_DELETE_AFTER_USE + id, mDeleteAfterUse);

        editor.apply();
    }

    public void readFromSharedPref(SharedPreferences prefs) {
        final String id = Integer.toString(mTimerId);

        mStartTime = prefs.getLong(PREF_START_TIME + id, 0);
        mTimeLeft = prefs.getLong(PREF_TIME_LEFT + id, 0);
        mOriginalLength = prefs.getLong(PREF_ORIGINAL_TIME + id, 0);
        mSetupLength = prefs.getLong(PREF_SETUP_TIME + id, 0);
        mState = prefs.getInt(PREF_STATE + id, 0);
        mPriorState = prefs.getInt(PREF_PRIOR_STATE + id, 0);
        mLabel = prefs.getString(PREF_LABEL + id, "");
        mDeleteAfterUse = prefs.getBoolean(PREF_DELETE_AFTER_USE + id, false);
    }

    public boolean deleteFromSharedPref(SharedPreferences prefs) {
        final String id = Integer.toString(mTimerId);

        final Set<String> timerIds = getTimerIds(prefs);
        timerIds.remove(id);

        final SharedPreferences.Editor editor = prefs.edit()
                .remove(PREF_TIMER_ID + id)
                .remove(PREF_START_TIME + id)
                .remove(PREF_TIME_LEFT + id)
                .remove(PREF_ORIGINAL_TIME + id)
                .remove(PREF_SETUP_TIME + id)
                .remove(PREF_STATE + id)
                .remove(PREF_PRIOR_STATE + id)
                .putStringSet(PREF_TIMERS_LIST, timerIds)
                .remove(PREF_LABEL + id)
                .remove(PREF_DELETE_AFTER_USE + id);

        if (timerIds.isEmpty()) {
            editor.remove(KEY_NEXT_TIMER_ID);
        }

        return editor.commit();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTimerId);
        dest.writeLong(mStartTime);
        dest.writeLong(mTimeLeft);
        dest.writeLong(mOriginalLength);
        dest.writeLong(mSetupLength);
        dest.writeInt(mState);
        dest.writeInt(mPriorState);
        dest.writeString(mLabel);
    }

    public TimerObj(Parcel p) {
        mTimerId = p.readInt();
        mStartTime = p.readLong();
        mTimeLeft = p.readLong();
        mOriginalLength = p.readLong();
        mSetupLength = p.readLong();
        mState = p.readInt();
        mPriorState = p.readInt();
        mLabel = p.readString();
    }

    private TimerObj() {
        this(0 /* timerLength */, 0 /* timerId */);
    }

    public TimerObj(long timerLength, int timerId) {
      init(timerLength, timerId);
    }

    public TimerObj(long timerLength, Context context) {
        init(timerLength, getNextTimerId(context));
    }

    public TimerObj(long length, String label, Context context) {
        this(length, context);
        mLabel = label != null ? label : "";
    }

    private void init (long length, int timerId) {
        /* TODO: mTimerId must avoid StopwatchService.NOTIFICATION_ID,
         * TimerReceiver.IN_USE_NOTIFICATION_ID, and alarm ID's (which seem to be 1, 2, ..)
         */
        mTimerId = timerId;
        mStartTime = Utils.getTimeNow();
        mTimeLeft = mOriginalLength = mSetupLength = length;
        mLabel = "";
    }

    private int getNextTimerId(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int nextTimerId;
        synchronized (TimerObj.class) {
            nextTimerId = prefs.getInt(KEY_NEXT_TIMER_ID, 0);
            prefs.edit().putInt(KEY_NEXT_TIMER_ID, nextTimerId + 1).apply();
        }
        return nextTimerId;
    }

    public static boolean isTimerStateSharedPrefKey(String prefKey) {
        return prefKey.startsWith(PREF_STATE);
    }

    public static int getTimerIdFromTimerStateKey(String timerStatePrefKey) {
        final String timerId = timerStatePrefKey.substring(PREF_STATE.length());
        return Integer.parseInt(timerId);
    }

    public long updateTimeLeft(boolean forceUpdate) {
        if (isTicking() || forceUpdate) {
            long millis = Utils.getTimeNow();
            mTimeLeft = mOriginalLength - (millis - mStartTime);
        }
        return mTimeLeft;
    }

    public void setState(int state) {
        mPriorState = mState;
        mState = state;
    }

    public String getLabelOrDefault(Context context) {
        return (mLabel == null || mLabel.length() == 0) ? context.getString(
                R.string.timer_notification_label)
                : mLabel;
    }

    public boolean isTicking() {
        return mState == STATE_RUNNING || mState == STATE_TIMESUP;
    }

    public boolean isInUse() {
        return mState == STATE_RUNNING || mState == STATE_STOPPED;
    }

    public void addTime(long time) {
        mTimeLeft = mOriginalLength - (Utils.getTimeNow() - mStartTime);
        if (mTimeLeft < MAX_TIMER_LENGTH - time) {
            mOriginalLength += time;
        }
    }

    public boolean getDeleteAfterUse() {
        return mDeleteAfterUse;
    }

    public long getTimesupTime() {
        return mStartTime + mOriginalLength;
    }

    public static TimerObj getTimerFromSharedPrefs(SharedPreferences prefs, int timerId) {
        final TimerObj timer = new TimerObj();
        timer.mTimerId = timerId;
        timer.readFromSharedPref(prefs);
        return timer;
    }

    public static void getTimersFromSharedPrefs(SharedPreferences prefs, List<TimerObj> timers) {
        for (String timerIdString : getTimerIds(prefs)) {
            final int timerId = Integer.parseInt(timerIdString);
            timers.add(getTimerFromSharedPrefs(prefs, timerId));
        }

        Collections.sort(timers, new Comparator<TimerObj>() {
            @Override
            public int compare(TimerObj timer1, TimerObj timer2) {
               return timer1.mTimerId - timer2.mTimerId;
            }
        });
    }

    public static void getTimersFromSharedPrefs(SharedPreferences prefs, List<TimerObj> timers,
            int state) {
        for (String timerIdString : getTimerIds(prefs)) {
            final int timerId = Integer.parseInt(timerIdString);
            final TimerObj timer = getTimerFromSharedPrefs(prefs, timerId);
            if (timer.mState == state) {
                timers.add(timer);
            }
        }
    }

    public static void putTimersInSharedPrefs(SharedPreferences prefs, List<TimerObj> timers) {
        for (TimerObj timer : timers) {
            timer.writeToSharedPref(prefs);
        }
    }

    public static void resetTimersInSharedPrefs(SharedPreferences prefs) {
        final List<TimerObj> timers = new  ArrayList<>();
        getTimersFromSharedPrefs(prefs, timers);
        for (TimerObj timer : timers) {
            timer.setState(TimerObj.STATE_RESTART);
            timer.mTimeLeft = timer.mOriginalLength = timer.mSetupLength;
            timer.writeToSharedPref(prefs);
        }
    }

    private static Set<String> getTimerIds(SharedPreferences prefs) {
        // return a defensive copy that is safe to mutate; see doc for getStringSet() for details
        return new HashSet<>(prefs.getStringSet(PREF_TIMERS_LIST, Collections.<String>emptySet()));
    }
}
