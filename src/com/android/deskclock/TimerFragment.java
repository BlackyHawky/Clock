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
import android.widget.ListView;
import android.widget.TextView;

import com.android.deskclock.CircleTimerView;
import com.android.deskclock.Log;
import com.android.deskclock.R;


public class TimerFragment extends DeskClockFragment {

	private int mTimersNum;
	ListView mTimersList;
	View mNewTimerPage;
	View mTimersListPage;

	public TimerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.timer_fragment, container, false);
        mTimersList = (ListView)v.findViewById(R.id.timers_list);
        mNewTimerPage = v.findViewById(R.id.new_timer_page);
        mTimersListPage = v.findViewById(R.id.timers_list_page);
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
            mTimersListPage.setVisibility(View.GONE);
        	mNewTimerPage.setVisibility(View.VISIBLE);
        } else {
        	mTimersList.setVisibility(View.VISIBLE);
        	mTimersListPage.setVisibility(View.GONE);
        }
    }
}
