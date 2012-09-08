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

package com.android.deskclock;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.deskclock.CircleTimerView;
import com.android.deskclock.Log;
import com.android.deskclock.R;


public class TimerFragment extends DeskClockFragment {

    private static final String STATE_KEY = "state";
    private static final String TIME_LEFT_KEY = "time_left";
    private static final String TIMER_TIME_KEY = "timer_time";

    // timer states
    private static final int TIMER_RUNNING = 1;
    private static final int TIMER_STOPPED = 2;
    private int mState = TIMER_RUNNING;
    private long mTimerTime = 1 * 10 * 1000;  // 1 minute in millis
    private long mTimeLeft = mTimerTime;
    private long mCurrentTime;
    private boolean mStartTimerAnimation = false;

    private CircleTimerView mTime;
    private View mTimesUp, mAddOneButton, mCancelButton;

    Runnable mTimeUpdateThread = new Runnable() {
        @Override
        public void run() {
            if (mTime != null) {
                long newCurrentTime = System.currentTimeMillis();
                long delta = newCurrentTime - mCurrentTime;
                mTimeLeft -= delta;
                mTime.setTimeString(getTimeText(mTimeLeft));
                mCurrentTime = newCurrentTime;
                if (mTimeLeft <= 0) {
                    timesUp();
                } else {
                    mTime.postDelayed(mTimeUpdateThread, 500);
                }
            }
        }
    };


    public TimerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.timer_fragment, container, false);

        mTime = (CircleTimerView)v.findViewById(R.id.timer_time);
        mTimesUp = v.findViewById(R.id.times_up);
        mAddOneButton = v.findViewById(R.id.timer_add_button);
        mAddOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimerTime += (60 * 1000);
                mTimeLeft += (60 * 1000);
                mTime.pauseIntervalAnimation();
                mTime.setIntervalTime(mTimerTime);
                mTime.setTimeString(getTimeText(mTimeLeft));
                mTime.startIntervalAnimation();
            }
        });
        mCancelButton = v.findViewById(R.id.timer_dismiss_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mState = TIMER_STOPPED;
                stopUpdateThread();
                mTime.stopIntervalAnimation();
            }
        });

        if (savedInstanceState != null) {
            mState = savedInstanceState.getInt(STATE_KEY, TIMER_RUNNING);
            mTimeLeft = savedInstanceState.getLong(TIME_LEFT_KEY, 0);
            mTimerTime = savedInstanceState.getLong(TIMER_TIME_KEY, 60000);
        } else {
            // No previous state, so assume this is the start of the timer and start animation.
            // Only do it one time since the view preserve the animation status in all other cases
            mStartTimerAnimation = true;
        }
        return v;
    }

    @Override
    public void onResume() {
        mTime.setTimerMode(true);
        mTime.setTimeString(getTimeText(mTimeLeft));
        mTime.setIntervalTime(mTimerTime);
        mCurrentTime = System.currentTimeMillis();
        if (mState == TIMER_RUNNING) {
            startUpdateThread();
            if (mStartTimerAnimation) {
                mTime.startIntervalAnimation();
            }
        }
        setViewsVisibility();
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mState == TIMER_RUNNING) {
            stopUpdateThread();
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        outState.putInt(STATE_KEY, mState);
        outState.putLong(TIME_LEFT_KEY, mTimeLeft);
        outState.putLong(TIMER_TIME_KEY, mTimerTime);
        super.onSaveInstanceState(outState);
    }

    private void setViewsVisibility() {
        switch (mState) {
            case TIMER_STOPPED:
                mTime.setVisibility(View.INVISIBLE);
                mTimesUp.setVisibility(View.VISIBLE);
                mAddOneButton.setVisibility(View.INVISIBLE);
                mCancelButton.setVisibility(View.INVISIBLE);
                break;
            case TIMER_RUNNING:
                mTime.setVisibility(View.VISIBLE);
                mTimesUp.setVisibility(View.INVISIBLE);
                mAddOneButton.setVisibility(View.VISIBLE);
                mCancelButton.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }
    private void startUpdateThread() {
        mTime.post(mTimeUpdateThread);
    }


    private void stopUpdateThread() {
        mTime.removeCallbacks(mTimeUpdateThread);
    }

    /***
     * Sets the string of the time remaining on the timer up to second accuracy
     * @param time - in millis left on the timer
     */
    private String getTimeText(long time) {
        if (time < 0) {
            time = 0;
        }
        long seconds, minutes, hours;
        seconds = time / 1000;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 99) {
            hours = 0;
        }
        // TODO: must build to account for localization
        String timeStr =
                String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        return timeStr;
    }

    private void timesUp() {
        // Stop everything
        mState = TIMER_STOPPED;
        setViewsVisibility();
        mTime.stopIntervalAnimation();
        // Play the timer alarm
/*        Intent playAlarm = new Intent(Alarms.ALARM_ALERT_ACTION);
        Alarm alarm = new Alarm();
        alarm.silent = false;
        alarm.vibrate = false;
        final SharedPreferences timerRingtone = getSharedPreferences("timer_default_ringtone", 0);
        Log.v("-------------------------- ringtone " + timerRingtone);
//        alarm.alert =

  //      playAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
    //    startService(playAlarm);
*/
    }

}
