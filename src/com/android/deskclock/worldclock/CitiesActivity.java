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

package com.android.deskclock.worldclock;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

import com.android.deskclock.Alarms;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * Cities chooser for the world clock
 */
public class CitiesActivity extends Activity implements OnCheckedChangeListener {

    /** This must be false for production.  If true, turns on logging,
        test code, etc. */
    static final boolean DEBUG = false;

    private LayoutInflater mFactory;
    private ListView mCitiesList;
    private CityAdapter mAdapter;
    private HashMap<String, CityObj> mUserSelectedCities;
    private Calendar mCalendar;


    private class CityAdapter extends BaseAdapter {
        ArrayList<CityObj> mAllTheCitiesList;
        HashMap<String, CityObj> mSelectedCitiesList;
        LayoutInflater mInflater;
        private boolean mIs24HoursMode;

        public CityAdapter(
                Context context,  HashMap<String, CityObj> selectedList, LayoutInflater factory) {
            super();
            mAllTheCitiesList = new ArrayList<CityObj> ();
            mAllTheCitiesList.add(new CityObj("San Farncisco", "America/Los_Angeles"));
            mAllTheCitiesList.add(new CityObj("New York", "America/New_York"));
            mAllTheCitiesList.add(new CityObj("London", "Europe/London"));
            mAllTheCitiesList.add(new CityObj("Tel Aviv", "Asia/Jerusalem"));
            mSelectedCitiesList = selectedList;
            mInflater = factory;
            mCalendar = Calendar.getInstance();
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            set24HoursMode(context);
        }

        @Override
        public int getCount() {
            return mAllTheCitiesList.size();
        }

        @Override
        public Object getItem(int p) {
            if (p >=0 && p < mAllTheCitiesList.size()) {
                return mAllTheCitiesList.get(p);
            }
            return null;
        }

        @Override
        public long getItemId(int p) {
            return p;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (position < 0 || position >=  mAllTheCitiesList.size()) {
                return null;
            }
            if (view == null) {
                view = mInflater.inflate(R.layout.city_list_item, parent, false);
            }
            CityObj c = mAllTheCitiesList.get(position);
            TextView name = (TextView)view.findViewById(R.id.city_name);
            TextView tz = (TextView)view.findViewById(R.id.city_time);
            CheckBox cb = (CheckBox)view.findViewById(R.id.city_onoff);
            cb.setTag(c);
            cb.setChecked(mSelectedCitiesList.containsKey(c.mCityName));
            cb.setOnCheckedChangeListener(CitiesActivity.this);
            name.setText(c.mCityName);
            mCalendar.setTimeZone(TimeZone.getTimeZone(c.mTimeZone));
            tz.setText(DateFormat.format(mIs24HoursMode ? "k:mm" : "h:mmaa", mCalendar));
            return view;
        }

        public void set24HoursMode(Context c) {
            mIs24HoursMode = Alarms.get24HourMode(c);
            notifyDataSetChanged();
        }
    }


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mFactory = LayoutInflater.from(this);

        updateLayout();
    }

    private void updateLayout() {
        setContentView(R.layout.cities_activity);
        mCitiesList = (ListView) findViewById(R.id.cities_list);
        mUserSelectedCities = Cities.readCitiesFromSharedPrefs(
                PreferenceManager.getDefaultSharedPreferences(this));
        mAdapter = new CityAdapter(this, mUserSelectedCities, mFactory);
        mCitiesList.setAdapter(mAdapter);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.set24HoursMode(this);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        Cities.saveCitiesToSharedPrefs(PreferenceManager.getDefaultSharedPreferences(this),
                mUserSelectedCities);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_item_help:
                Intent i = item.getIntent();
                if (i != null) {
                    try {
                        startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        // No activity found to match the intent - ignore
                    }
                }
                return true;
            case android.R.id.home:
                Intent intent = new Intent(this, DeskClock.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cities_menu, menu);
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCheckedChanged(CompoundButton b, boolean checked) {
        CityObj c = (CityObj)b.getTag();
        if (checked) {
            mUserSelectedCities.put(c.mCityName, c);
        } else {
            mUserSelectedCities.remove(c.mCityName);
        }
        mAdapter.notifyDataSetChanged();
    }
}
