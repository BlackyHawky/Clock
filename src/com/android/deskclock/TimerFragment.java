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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.deskclock.timer.TimerObj;

import java.util.ArrayList;


public class TimerFragment extends DeskClockFragment {


	private int mTimersNum;
	ListView mTimersList;
	View mNewTimerPage;
	View mTimersListPage;
	Button mClear, mStart, mAddTimer;
	TimerSetupView mTimerSetup;
	TimersListAdapter mAdapter;

	public TimerFragment() {
    }

	class TimersListAdapter extends BaseAdapter {

	    ArrayList<TimerObj> mTimers = new ArrayList<TimerObj> ();

	    @Override
        public int getCount() {
            return mTimers.size();
        }

        @Override
        public Object getItem(int p) {
            return null;
        }

        @Override
        public long getItemId(int p) {
            if (p >= 0 && p < mTimers.size()) {
                return mTimers.get(p).mTimerId;
            }
            return 0;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v;

            if (convertView != null) {
                v = (TextView) convertView;
            } else {
                v = new TextView(getActivity());
            }
            v.setText("Nothing here yet");
            v.setTextSize(40);
            return v;
        }

        public void addTimer(TimerObj t) {
            mTimers.add(0, t);
            notifyDataSetChanged();
        }

	}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.timer_fragment, container, false);
        mTimersList = (ListView)v.findViewById(R.id.timers_list);
        mAdapter = new TimersListAdapter();
        mTimersList.setAdapter(mAdapter);
        mNewTimerPage = v.findViewById(R.id.new_timer_page);
        mTimersListPage = v.findViewById(R.id.timers_list_page);
        mTimerSetup = (TimerSetupView)v.findViewById(R.id.timer_setup);
        mClear = (Button)v.findViewById(R.id.timer_clear);
        mClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimerSetup.reset();
            }

        });
        mStart = (Button)v.findViewById(R.id.timer_start);
        mStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int timerLength = mTimerSetup.getTime();
                if (timerLength == 0) {
                    return;
                }
                TimerObj t = new TimerObj();
                t.mStartTime = System.currentTimeMillis();
                t.mOriginalLength = mTimerSetup.getTime() * 1000;
                t.mTimerId = (int) System.currentTimeMillis();
                mAdapter.addTimer(t);
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
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void setPage() {
        if (mTimersNum != 0) {
            gotoTimersView();
        } else {
            gotoSetupView();
        }
    }
    private void gotoSetupView() {
        mNewTimerPage.setVisibility(View.VISIBLE);
        mTimersListPage.setVisibility(View.GONE);
    }
    private void gotoTimersView() {
        mNewTimerPage.setVisibility(View.GONE);
        mTimersListPage.setVisibility(View.VISIBLE);
    }
}
