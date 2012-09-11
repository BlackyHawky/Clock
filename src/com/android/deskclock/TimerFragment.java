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

import android.content.Context;
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


public class TimerFragment extends DeskClockFragment implements OnClickListener {


	private int mTimersNum;
	ListView mTimersList;
	View mNewTimerPage;
	View mTimersListPage;
	Button mClear, mStart, mAddTimer;
	TimerSetupView mTimerSetup;
	TimersListAdapter mAdapter;

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
                    mTimers.remove(i);
                    notifyDataSetChanged();
                    return;
                }
            }
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;

/*            if (convertView != null) {
                v = convertView;
            } else {*/
                v = mInflater.inflate(R.layout.timer_list_item, parent, false);
            //}
            TimerView tv = (TimerView) v.findViewById(R.id.timer_time_text);
            TimerObj o = (TimerObj)getItem(position);
            o.mView = v;
            if (tv != null) {
                tv.setTime(o.mTimeLeft / 10);
                tv.setTag(o);
            }
            Button delete = (Button)v.findViewById(R.id.timer_delete);
            delete.setOnClickListener(TimerFragment.this);
            delete.setTag(new ClickAction(ClickAction.ACTION_DELETE, o));
            Button plusOne = (Button)v. findViewById(R.id.timer_plus_one);
            plusOne.setOnClickListener(TimerFragment.this);
            plusOne.setTag(new ClickAction(ClickAction.ACTION_PLUS_ONE, o));
            Button stop = (Button)v. findViewById(R.id.timer_stop);
            stop.setOnClickListener(TimerFragment.this);
            stop.setTag(new ClickAction(ClickAction.ACTION_STOP, o));
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
        mAdapter = new TimersListAdapter(mContext);
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
                TimerObj t = new TimerObj(mTimerSetup.getTime() * 1000);
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

    @Override
    public void onClick(View v) {
        ClickAction tag = (ClickAction)v.getTag();
        switch (tag.mAction) {
            case ClickAction.ACTION_DELETE:
                mAdapter.deleteTimer(tag.mTimer.mTimerId);
                if (mAdapter.getCount() == 0) {
                    gotoSetupView();
                }
                break;
            case ClickAction.ACTION_PLUS_ONE:
                tag.mTimer.mTimeLeft += 60000; //60 seconds in millis
                tag.mTimer.mView.invalidate();
                break;
            case ClickAction.ACTION_STOP:
                break;
            default:
                break;
        }
    }
}
