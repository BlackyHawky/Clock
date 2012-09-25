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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.R;
import com.android.deskclock.TimerSetupView;

import java.util.ArrayList;


public class TimerFragment extends DeskClockFragment implements OnClickListener {

    private static final String TAG = "TimerFragment";
    private ListView mTimersList;
    private View mNewTimerPage;
    private View mTimersListPage;
    private Button mCancel, mStart, mAddTimer;
    private TimerSetupView mTimerSetup;
    private TimersListAdapter mAdapter;
    private boolean mTicking = false;

    public TimerFragment() {
    }

    class ClickAction {
        public static final int ACTION_STOP = 1;
        public static final int ACTION_PLUS_ONE = 2;
        public static final int ACTION_DELETE = 3;

        public int mAction;
        public TimerObj mTimer;

        public ClickAction(int action, TimerObj t) {
            mAction = action;
            mTimer = t;
        }
    }

    class TimersListAdapter extends BaseAdapter {

        ArrayList<TimerObj> mTimers = new ArrayList<TimerObj> ();
        private final LayoutInflater mInflater;
        Context mContext;

        public TimersListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mTimers.size();
        }

        @Override
        public Object getItem(int p) {
            return mTimers.get(p);
        }

        @Override
        public long getItemId(int p) {
            if (p >= 0 && p < mTimers.size()) {
                return mTimers.get(p).mTimerId;
            }
            return 0;
        }

        public void deleteTimer(int id) {
            for (int i = 0; i < mTimers.size(); i++) {
                TimerObj t = mTimers.get(i);
                if (t.mTimerId == id) {
                    ((TimerListItem)t.mView).stop();
                    t.deleteFromSharedPref(PreferenceManager.getDefaultSharedPreferences(mContext));
                    mTimers.remove(i);
                    notifyDataSetChanged();
                    return;
                }
            }
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TimerListItem v;

   //         if (convertView != null) {
     //           v = (TimerListItem) convertView;
       //     } else {
                v = new TimerListItem (mContext);
         //   }

            TimerObj o = (TimerObj)getItem(position);
            o.mView = v;
            long timeLeft =  o.updateTimeLeft();
            v.set(o.mOriginalLength, timeLeft);
            v.setTime(timeLeft / 10);
            v.start();
            v.setTime(timeLeft / 10);
            Button delete = (Button)v.findViewById(R.id.timer_delete);
            delete.setOnClickListener(TimerFragment.this);
            delete.setTag(new ClickAction(ClickAction.ACTION_DELETE, o));
            Button plusOne = (Button)v. findViewById(R.id.timer_plus_one);
            plusOne.setOnClickListener(TimerFragment.this);
            plusOne.setTag(new ClickAction(ClickAction.ACTION_PLUS_ONE, o));
            Button stop = (Button)v. findViewById(R.id.timer_stop);
            stop.setOnClickListener(TimerFragment.this);
            stop.setTag(new ClickAction(ClickAction.ACTION_STOP, o));
            TimerFragment.this.setTimerButtons(o);
            return v;
        }

        public void addTimer(TimerObj t) {
            mTimers.add(0, t);
            notifyDataSetChanged();
        }

        public void onSaveInstanceState(Bundle outState) {
            TimerObj.putTimersInSharedPrefs(
                    PreferenceManager.getDefaultSharedPreferences(mContext), mTimers);
        }

        public void onRestoreInstanceState(Bundle outState) {
            TimerObj.getTimersFromSharedPrefs(
                    PreferenceManager.getDefaultSharedPreferences(mContext), mTimers);
            notifyDataSetChanged();
        }

        public void saveGlobalState() {
            TimerObj.putTimersInSharedPrefs(
                    PreferenceManager.getDefaultSharedPreferences(mContext), mTimers);
        }
    }

    private final Runnable mClockTick = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < mAdapter.getCount(); i ++) {
                TimerObj t = (TimerObj) mAdapter.getItem(i);
                if (t.mState == TimerObj.STATE_RUNNING || t.mState == TimerObj.STATE_TIMESUP) {
                    long timeLeft = t.updateTimeLeft();
                    if ((TimerListItem)(t.mView) != null) {
                        ((TimerListItem)(t.mView)).setTime(timeLeft/10);
                    } else {
                        Log.v("timer fragment"," timer view is null");
                    }
                }
                if (t.mTimeLeft <= 0 && t.mState != TimerObj.STATE_DONE
                        && t.mState != TimerObj.STATE_RESTART) {
                    t.mState = TimerObj.STATE_TIMESUP;
                    TimerFragment.this.setTimerButtons(t);
                }

            }
            mTimersList.postDelayed(mClockTick, 1000 - (System.currentTimeMillis() % 1000));
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.timer_fragment, container, false);
        mTimersList = (ListView)v.findViewById(R.id.timers_list);
        mAdapter = new TimersListAdapter(getActivity());
        mAdapter.onRestoreInstanceState(savedInstanceState);
        mTimersList.setAdapter(mAdapter);
        mNewTimerPage = v.findViewById(R.id.new_timer_page);
        mTimersListPage = v.findViewById(R.id.timers_list_page);
        mTimerSetup = (TimerSetupView)v.findViewById(R.id.timer_setup);
        mCancel = (Button)v.findViewById(R.id.timer_cancel);
        mCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapter.getCount() != 0) {
                    gotoTimersView();
                }
            }
        });
        if (mAdapter.getCount() == 0) {
            mCancel.setVisibility(View.INVISIBLE);
        }
        mStart = (Button)v.findViewById(R.id.timer_start);
        mStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // New timer create if timer length is not zero
                // Create a new timer object to track the timer and
                // switch to the timers view.
                int timerLength = mTimerSetup.getTime();
                if (timerLength == 0) {
                    return;
                }
                TimerObj t = new TimerObj(timerLength * 1000);
                t.mState = TimerObj.STATE_RUNNING;
                mAdapter.addTimer(t);
                updateTimersState(t, Timers.START_TIMER);
                gotoTimersView();
            }

        });
        mAddTimer = (Button)v.findViewById(R.id.timer_add_timer);
        mAddTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimerSetup.reset();
                gotoSetupView();
            }

        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        setPage();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopClockTicks();
        saveGlobalState();
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            mAdapter.onSaveInstanceState (outState);
        }
    }

    @Override
    public void saveGlobalState () {
        super.saveGlobalState();
        if (mAdapter != null) {
            mAdapter.saveGlobalState ();
        }
    }

    public void setPage() {
        if (mAdapter.getCount() != 0) {
            gotoTimersView();
        } else {
            gotoSetupView();
        }
    }
    private void gotoSetupView() {
        mNewTimerPage.setVisibility(View.VISIBLE);
        mTimersListPage.setVisibility(View.GONE);
        stopClockTicks();
        if (mAdapter.getCount() == 0) {
            mCancel.setVisibility(View.INVISIBLE);
        } else {
            mCancel.setVisibility(View.VISIBLE);
        }
    }
    private void gotoTimersView() {
        mNewTimerPage.setVisibility(View.GONE);
        mTimersListPage.setVisibility(View.VISIBLE);
        startClockTicks();
    }

    @Override
    public void onClick(View v) {
        ClickAction tag = (ClickAction)v.getTag();
        switch (tag.mAction) {
            case ClickAction.ACTION_DELETE:
                TimerObj t = tag.mTimer;
                mAdapter.deleteTimer(t.mTimerId);
                if (mAdapter.getCount() == 0) {
                    mTimerSetup.reset();
                    gotoSetupView();
                }
                // Tell receiver the timer was deleted.
                // It will stop all activity related to the timer
                updateTimersState(t, Timers.DELETE_TIMER);
                break;
            case ClickAction.ACTION_PLUS_ONE:
                onPlusOneButtonPressed(tag.mTimer);
                setTimerButtons(tag.mTimer);
                break;
            case ClickAction.ACTION_STOP:
                onStopButtonPressed(tag.mTimer);
                setTimerButtons(tag.mTimer);
                break;
            default:
                break;
        }
    }

    private void onPlusOneButtonPressed(TimerObj t) {
        switch(t.mState) {
            case TimerObj.STATE_RUNNING:
                 t.addTime(60000); //60 seconds in millis
                 long timeLeft = t.updateTimeLeft();
                 ((TimerListItem)(t.mView)).setTime(timeLeft/10);
                 ((TimerListItem)(t.mView)).setLength(timeLeft);
                 updateTimersState(t, Timers.TIMER_UPDATE);
                break;
            case TimerObj.STATE_TIMESUP:
                // +1 min when the time is up will restart the timer with 1 minute left.
                t.mState = TimerObj.STATE_RUNNING;
                t.mStartTime = System.currentTimeMillis();
                t.mTimeLeft = t. mOriginalLength = 60000;
                ((TimerListItem)t.mView).setTime(t.mTimeLeft / 10);
                ((TimerListItem)t.mView).set(t.mOriginalLength, t.mTimeLeft);
                ((TimerListItem) t.mView).start();
                updateTimersState(t, Timers.TIMER_RESET);
                updateTimersState(t, Timers.START_TIMER);
                break;
            case TimerObj.STATE_STOPPED:
            case TimerObj.STATE_DONE:
                t.mState = TimerObj.STATE_RESTART;
                t.mTimeLeft = t.mSetupLength;
                ((TimerListItem)t.mView).stop();
                ((TimerListItem)t.mView).setTime(t.mTimeLeft / 10);
                ((TimerListItem)t.mView).set(t.mOriginalLength, t.mTimeLeft);
                updateTimersState(t, Timers.TIMER_RESET);
                break;
            default:
                break;
        }
    }




    private void onStopButtonPressed(TimerObj t) {
        switch(t.mState) {
            case TimerObj.STATE_RUNNING:
                // Stop timer and save the remaining time of the timer
                t.mState = TimerObj.STATE_STOPPED;
                ((TimerListItem) t.mView).pause();
                t.updateTimeLeft();
                updateTimersState(t, Timers.TIMER_STOP);
                break;
            case TimerObj.STATE_STOPPED:
                // Reset the remaining time and continue timer
                t.mState = TimerObj.STATE_RUNNING;
                t.mStartTime = System.currentTimeMillis() - (t.mOriginalLength - t.mTimeLeft);
                ((TimerListItem) t.mView).start();
                updateTimersState(t, Timers.START_TIMER);
                break;
            case TimerObj.STATE_TIMESUP:
                t.mState = TimerObj.STATE_DONE;
                ((TimerListItem) t.mView).stop();
                updateTimersState(t, Timers.TIMER_DONE);
                break;
            case TimerObj.STATE_DONE:
                break;
            case TimerObj.STATE_RESTART:
                t.mState = TimerObj.STATE_RUNNING;
                t.mStartTime = System.currentTimeMillis() - (t.mOriginalLength - t.mTimeLeft);
                ((TimerListItem) t.mView).start();
                updateTimersState(t, Timers.START_TIMER);
                break;
            default:
                break;
        }
    }

    private void setTimerButtons(TimerObj t) {
        Context a = getActivity();
        if (a == null || t == null || t.mView == null) {
            return;
        }
        Button plusOne = (Button) t.mView.findViewById(R.id.timer_plus_one);
        Button stop = (Button) t.mView.findViewById(R.id.timer_stop);
        Resources r = a.getResources();
        switch (t.mState) {
            case TimerObj.STATE_RUNNING:
                plusOne.setVisibility(View.VISIBLE);
                plusOne.setText(r.getString(R.string.timer_plus_one));
                stop.setText(r.getString(R.string.timer_stop));
                stop.setEnabled(true);
                break;
            case TimerObj.STATE_STOPPED:
                plusOne.setVisibility(View.VISIBLE);
                plusOne.setText(r.getString(R.string.timer_reset));
                stop.setText(r.getString(R.string.timer_start));
                stop.setEnabled(true);
                break;
            case TimerObj.STATE_TIMESUP:
                plusOne.setVisibility(View.VISIBLE);
                stop.setText(r.getString(R.string.timer_stop));
                stop.setEnabled(true);
                break;
            case TimerObj.STATE_DONE:
                plusOne.setVisibility(View.VISIBLE);
                plusOne.setText(r.getString(R.string.timer_reset));
                stop.setText(r.getString(R.string.timer_start));
                stop.setEnabled(false);
                break;
            case TimerObj.STATE_RESTART:
                plusOne.setVisibility(View.INVISIBLE);
                stop.setText(r.getString(R.string.timer_start));
                stop.setEnabled(true);
                break;
            default:
                break;
        }
    }

    private void startClockTicks() {
        mTimersList.postDelayed(mClockTick, 1000 - (System.currentTimeMillis() % 1000));
        mTicking = true;
    }
    private void stopClockTicks() {
        if (mTicking) {
            mTimersList.removeCallbacks(mClockTick);
            mTicking = false;
        }
    }

    private void updateTimersState(TimerObj t, String action) {
        if (!Timers.DELETE_TIMER.equals(action)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            t.writeToSharedPref(prefs);
        }
        Intent i = new Intent();
        i.setAction(action);
        i.putExtra(Timers.TIMER_INTENT_EXTRA, t.mTimerId);
        getActivity().sendBroadcast(i);
    }
}
