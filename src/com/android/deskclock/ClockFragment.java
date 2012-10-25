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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CityObj;

import java.text.Collator;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Fragment that shows  the clock (analog or digital), the next alarm info and the world clock.
 */

public class ClockFragment extends DeskClockFragment implements OnSharedPreferenceChangeListener {

    private static final String BUTTONS_HIDDEN_KEY = "buttons_hidden";
    private final static String TAG = "ClockFragment";

    private View mButtons;
    private boolean mButtonsHidden = false;
    private View mDigitalClock, mAnalogClock, mClockFrame;
    private WorldClockAdapter mAdapter;
    private ListView mList;
    private String mClockStyle;
    private SharedPreferences mPrefs;
    private final Collator mCollator = Collator.getInstance();
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private String mDefaultClockStyle;

    private PendingIntent mQuarterlyIntent;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
            @Override
        public void onReceive(Context context, Intent intent) {
            boolean changed = intent.getAction().equals(Intent.ACTION_TIME_CHANGED)
                    || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED);
            if (changed || intent.getAction().equals(Utils.ACTION_ON_QUARTER_HOUR)) {
                Utils.updateDate(mDateFormat, mDateFormatForAccessibility,mClockFrame);
                if (mAdapter != null) {
                    // *CHANGED may modify the need for showing the Home City
                    if (changed && (mAdapter.hasHomeCity() != mAdapter.needHomeCity())) {
                        mAdapter.loadData(context);
                    } else {
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
            if (changed || intent.getAction().equals(Alarms.ALARM_DONE_ACTION)
                    || intent.getAction().equals(Alarms.ALARM_SNOOZE_CANCELLED)) {
                Utils.refreshAlarm(getActivity(), mClockFrame);
            }
        }
    };

    private final Handler mHandler = new Handler();

    public ClockFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle icicle) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.clock_fragment, container, false);
        mButtons = v.findViewById(R.id.clock_buttons);
        if (icicle != null) {
            mButtonsHidden = icicle.getBoolean(BUTTONS_HIDDEN_KEY, false);
        }
        mList = (ListView)v.findViewById(R.id.cities);
        mList.setDivider(null);
        View headerView = inflater.inflate(R.layout.blank_header_view, mList, false);
        mList.addHeaderView(headerView);
        mClockFrame = inflater.inflate(R.layout.main_clock_frame, mList, false);
        mDigitalClock = mClockFrame.findViewById(R.id.digital_clock);
        mAnalogClock = mClockFrame.findViewById(R.id.analog_clock);
        mList.addHeaderView(mClockFrame, null, false);
        View footerView = inflater.inflate(R.layout.blank_footer_view, mList, false);
        footerView.setBackgroundResource(R.color.blackish);
        mList.addFooterView(footerView);
        mAdapter = new WorldClockAdapter(getActivity());
        mList.setAdapter(mAdapter);
        mList.setOnTouchListener(new OnTouchListener() {
            private final float MAX_MOVEMENT_ALLOWED = 20;
            private float mLastTouchX, mLastTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case (MotionEvent.ACTION_DOWN):
                        long time = Utils.getTimeNow();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(getActivity(), ScreensaverActivity.class));
                            }
                        }, ViewConfiguration.getLongPressTimeout());
                        mLastTouchX = event.getX();
                        mLastTouchY = event.getY();
                        break;
                    case (MotionEvent.ACTION_MOVE):
                        float xDiff = Math.abs(event.getX()-mLastTouchX);
                        float yDiff = Math.abs(event.getY()-mLastTouchY);
                        if (xDiff >= MAX_MOVEMENT_ALLOWED || yDiff >= MAX_MOVEMENT_ALLOWED) {
                            mHandler.removeCallbacksAndMessages(null);
                        }
                        break;
                    default:
                        mHandler.removeCallbacksAndMessages(null);
                }
                return false;
            }
        });
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mDefaultClockStyle = getActivity().getResources().getString(R.string.default_clock_style);
        return v;
    }

    @Override
    public void onResume () {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        long alarmOnQuarterHour = Utils.getAlarmOnQuarterHour();
        mQuarterlyIntent = PendingIntent.getBroadcast(
                getActivity(), 0, new Intent(Utils.ACTION_ON_QUARTER_HOUR), 0);
        ((AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE)).setRepeating(
                AlarmManager.RTC, alarmOnQuarterHour, AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                mQuarterlyIntent);
        // Besides monitoring when quarter-hour changes, monitor other actions that
        // effect clock time
        IntentFilter filter = new IntentFilter(Utils.ACTION_ON_QUARTER_HOUR);
        filter.addAction(Alarms.ALARM_DONE_ACTION);
        filter.addAction(Alarms.ALARM_SNOOZE_CANCELLED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getActivity().registerReceiver(mIntentReceiver, filter);

        mButtons.setAlpha(mButtonsHidden ? 0 : 1);

        // Resume can invoked after changing the cities list.
        if (mAdapter != null) {
            mAdapter.reloadData(getActivity());
        }
        // Resume can invoked after changing the clock style.
        View clockView = Utils.setClockStyle(getActivity(), mDigitalClock, mAnalogClock,
                SettingsActivity.KEY_CLOCK_STYLE);
        mClockStyle = (clockView == mDigitalClock ?
                Utils.CLOCK_TYPE_DIGITAL : Utils.CLOCK_TYPE_ANALOG);
        mAdapter.notifyDataSetChanged();

        Utils.updateDate(mDateFormat, mDateFormatForAccessibility,mClockFrame);
        Utils.refreshAlarm(getActivity(), mClockFrame);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        ((AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE)).cancel(mQuarterlyIntent);
        getActivity().unregisterReceiver(mIntentReceiver);
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        outState.putBoolean(BUTTONS_HIDDEN_KEY, mButtonsHidden);
        super.onSaveInstanceState(outState);
    }

    public void showButtons(boolean show) {
        if (mButtons == null) {
            return;
        }
        if (show && mButtonsHidden) {
            mButtons.startAnimation(
                    AnimationUtils.loadAnimation(getActivity(), R.anim.unhide));
            mButtonsHidden = false;
        } else if (!show && !mButtonsHidden) {
            mButtons.startAnimation(
                    AnimationUtils.loadAnimation(getActivity(), R.anim.hide));
            mButtonsHidden = true;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key == SettingsActivity.KEY_CLOCK_STYLE) {
            mClockStyle = prefs.getString(SettingsActivity.KEY_CLOCK_STYLE, mDefaultClockStyle);
        }
    }

    private class WorldClockAdapter extends BaseAdapter {
        Object [] mCitiesList;
        LayoutInflater mInflater;
        @SuppressWarnings("hiding")
        Context mContext;
        HashMap<String, CityObj> mCitiesDb = new HashMap<String, CityObj>();

        public WorldClockAdapter(Context context) {
            super();
            mContext = context;
            loadData(context);
            mInflater = LayoutInflater.from(context);
            // Read the cities DB so that the names and timezones will be taken from the DB
            // and not from the selected list so that change of locale or changes in the DB will
            // be reflected.
            CityObj [] cities = Utils.loadCitiesDataBase(context);
            if (cities != null) {
                for (int i = 0; i < cities.length; i ++) {
                    mCitiesDb.put(cities[i].mCityId, cities [i]);
                }
            }
        }

        public void reloadData(Context context) {
            loadData(context);
            notifyDataSetChanged();
        }

        private void loadData(Context context) {
            mCitiesList = Cities.readCitiesFromSharedPrefs(
                    PreferenceManager.getDefaultSharedPreferences(context)).values().toArray();
            sortList();
            mCitiesList = addHomeCity();
        }

        /***
         * Adds the home city as the first item of the adapter if the feature is on and the device time
         * zone is different from the home time zone that was set by the user.
         * return the list of cities.
         */
        private Object[] addHomeCity() {
            if (needHomeCity()) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
                String homeTZ = sharedPref.getString(SettingsActivity.KEY_HOME_TZ, "");
                CityObj c = new CityObj(
                        mContext.getResources().getString(R.string.home_label), homeTZ, null);
                Object[] temp = new Object[mCitiesList.length + 1];
                temp[0] = c;
                for (int i = 0; i < mCitiesList.length; i++) {
                    temp[i + 1] = mCitiesList[i];
                }
                return temp;
            } else {
                return mCitiesList;
            }
        }

        public boolean needHomeCity() {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (sharedPref.getBoolean(SettingsActivity.KEY_AUTO_HOME_CLOCK, false)) {
                String homeTZ = sharedPref.getString(
                        SettingsActivity.KEY_HOME_TZ, TimeZone.getDefault().getID());
                final Date now = new Date();
                return TimeZone.getTimeZone(homeTZ).getOffset(now.getTime())
                        != TimeZone.getDefault().getOffset(now.getTime());
            } else {
                return false;
            }
        }

        public boolean hasHomeCity() {
            return (mCitiesList != null) && mCitiesList.length > 0 && ((CityObj)mCitiesList[0]).mCityId == null;
        }

        private void sortList() {
            final Date now = new Date();

            // Sort by the Offset from GMT taking DST into account
            // and if the same sort by City Name
            Arrays.sort(mCitiesList, new Comparator<Object>() {
                private int safeCityNameCompare(CityObj city1, CityObj city2) {
                    if (city1.mCityName == null && city2.mCityName == null) {
                        return 0;
                    } else if (city1.mCityName == null) {
                        return -1;
                    } else if (city2.mCityName == null) {
                        return 1;
                    } else {
                        return mCollator.compare(city1.mCityName, city2.mCityName);
                    }
                }

                @Override
                public int compare(Object object1, Object object2) {
                    CityObj city1 = (CityObj) object1;
                    CityObj city2 = (CityObj) object2;
                    if (city1.mTimeZone == null && city2.mTimeZone == null) {
                        return safeCityNameCompare(city1, city2);
                    } else if (city1.mTimeZone == null) {
                        return -1;
                    } else if (city2.mTimeZone == null) {
                        return 1;
                    }

                    int gmOffset1 = TimeZone.getTimeZone(city1.mTimeZone).getOffset(now.getTime());
                    int gmOffset2 = TimeZone.getTimeZone(city2.mTimeZone).getOffset(now.getTime());
                    if (gmOffset1 == gmOffset2) {
                        return safeCityNameCompare(city1, city2);
                    } else {
                        return gmOffset1 - gmOffset2;
                    }
                }
            });
        }

        @Override
        public int getCount() {
            // Each item in the list holds 1 or 2 clocks
            return (mCitiesList.length  + 1)/2;
        }

        @Override
        public Object getItem(int p) {
            return null;
        }

        @Override
        public long getItemId(int p) {
            return p;
        }

        @Override
        public boolean isEnabled(int p) {
            return false;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            // Index in cities list
            int index = position * 2;
            if (index < 0 || index >= mCitiesList.length) {
                return null;
            }

            if (view == null) {
                view = mInflater.inflate(R.layout.world_clock_list_item, parent, false);
            }
            // The world clock list item can hold two world clocks
            View rightClock = view.findViewById(R.id.city_right);
            updateView(view.findViewById(R.id.city_left), (CityObj)mCitiesList[index]);
            if (index + 1 < mCitiesList.length) {
                rightClock.setVisibility(View.VISIBLE);
                updateView(rightClock, (CityObj)mCitiesList[index + 1]);
            } else {
                // To make sure the spacing is right , make sure that the right clock style is selected
                // even if the clock is invisible.
                DigitalClock dclock = (DigitalClock)(rightClock.findViewById(R.id.digital_clock));
                AnalogClock aclock = (AnalogClock)(rightClock.findViewById(R.id.analog_clock));
                if (mClockStyle.equals("analog")) {
                    dclock.setVisibility(View.GONE);
                    aclock.setVisibility(View.INVISIBLE);
                } else {
                    dclock.setVisibility(View.INVISIBLE);
                    aclock.setVisibility(View.GONE);
                }
                rightClock.setVisibility(View.INVISIBLE);
            }

            return view;
        }

        private void updateView(View clock, CityObj cityObj) {
            View nameLayout= clock.findViewById(R.id.city_name_layout);
            TextView name = (TextView)(nameLayout.findViewById(R.id.city_name));
            TextView dayOfWeek = (TextView)(nameLayout.findViewById(R.id.city_day));
            DigitalClock dclock = (DigitalClock)(clock.findViewById(R.id.digital_clock));
            AnalogClock aclock = (AnalogClock)(clock.findViewById(R.id.analog_clock));

            if (mClockStyle.equals("analog")) {
                dclock.setVisibility(View.GONE);
                aclock.setVisibility(View.VISIBLE);
                aclock.setTimeZone(cityObj.mTimeZone);
                aclock.enableSeconds(false);
            } else {
                dclock.setVisibility(View.VISIBLE);
                aclock.setVisibility(View.GONE);
                dclock.setTimeZone(cityObj.mTimeZone);
            }
            CityObj cityInDb = mCitiesDb.get(cityObj.mCityId);

            // Home city or city not in DB , use data from the save selected cities list
            if (cityObj.mCityId == null || cityInDb == null) {
                name.setText(cityObj.mCityName);
            } else {
                name.setText(cityInDb.mCityName);
            }
            final Calendar now = Calendar.getInstance();
            now.setTimeZone(TimeZone.getDefault());
            int myDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
            // Get timezone from cities DB if available
            String cityTZ = (cityInDb != null) ? cityInDb.mTimeZone:cityObj.mTimeZone;
            now.setTimeZone(TimeZone.getTimeZone(cityTZ));
            int cityDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
            if (myDayOfWeek != cityDayOfWeek) {
                dayOfWeek.setText(getString(R.string.world_day_of_week_label, now.getDisplayName(
                        Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())));
                dayOfWeek.setVisibility(View.VISIBLE);
            } else {
                dayOfWeek.setVisibility(View.GONE);
            }
        }
    }
}
