/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 * DeskClock clock view for desk docks.
 */
public class DeskClock extends Activity {
    private static final boolean DEBUG = false;

    private static final String LOG_TAG = "DeskClock";

    // Alarm action for midnight (so we can update the date display).
    private static final String KEY_SELECTED_TAB = "selected_tab";

    private ActionBar mActionBar;
    private Tab mTimerTab;
    private Tab mClockTab;
    private Tab mStopwatchTab;

    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;

    private static final int TIMER_TAB_INDEX = 0;
    private static final int CLOCK_TAB_INDEX = 1;
    private static final int STOPWATCH_TAB_INDEX = 2;

    private int mSselectedTab;

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        if (DEBUG) Log.d(LOG_TAG, "onNewIntent with intent: " + newIntent);

        // update our intent so that we can consult it to determine whether or
        // not the most recent launch was via a dock event
        setIntent(newIntent);
    }


    private void initViews() {
        // give up any internal focus before we switch layouts
        final View focused = getCurrentFocus();
        if (focused != null) focused.clearFocus();

        if (mTabsAdapter == null) {
            mViewPager = new ViewPager(this);
            mViewPager.setId(R.id.desk_clock_pager);
            mTabsAdapter = new TabsAdapter(this, mViewPager);
            createTabs(mSselectedTab);
        }
        setContentView(mViewPager);
    }

    private void createTabs(int selectedIndex) {
        mActionBar = getActionBar();

        mActionBar.setDisplayOptions(0);
        if (mActionBar != null) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mTimerTab = mActionBar.newTab();
            mTimerTab.setText(getString(R.string.menu_timer));
            mTabsAdapter.addTab(mTimerTab, TimerFragment.class,TIMER_TAB_INDEX);
            mClockTab = mActionBar.newTab();
            mClockTab.setText(getString(R.string.menu_clock));
            mTabsAdapter.addTab(mClockTab, ClockFragment.class,CLOCK_TAB_INDEX);
            mStopwatchTab = mActionBar.newTab();
            mStopwatchTab.setText(getString(R.string.menu_stopwatch));
            mTabsAdapter.addTab(mStopwatchTab, StopwatchFragment.class,STOPWATCH_TAB_INDEX);
            mActionBar.setSelectedNavigationItem(selectedIndex);
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mSselectedTab = CLOCK_TAB_INDEX;
        if (icicle != null) {
            mSselectedTab = icicle.getInt(KEY_SELECTED_TAB, CLOCK_TAB_INDEX);
        }
        initViews();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SELECTED_TAB, mActionBar.getSelectedNavigationIndex());
    }

    public void clockButtonsOnClick(View v) {
        if (v == null)
            return;
        switch (v.getId()) {
            case R.id.alarms_button:
                startActivity(new Intent(this, AlarmClock.class));
                break;
            case R.id.cities_button:
                Toast.makeText(this, "Not implemented yet", 2).show();
                break;
            case R.id.menu_button:
                showMenu(v);
                break;
            default:
                break;
        }
    }

    private void showMenu(View v) {
        PopupMenu menu = new PopupMenu(this, v);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener () {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_item_settings:
                        startActivity(new Intent(DeskClock.this, SettingsActivity.class));
                        return true;
                    case R.id.menu_item_help:
                        startActivity(new Intent(DeskClock.this, HelpActivity.class));
                        return true;
                    default:
                        break;
                }
                return true;
            }
        });
        menu.inflate(R.menu.desk_clock_menu);
        menu.show();
    }

    /***
     * Adapter for wrapping together the ActionBar's tab with the ViewPager
     */

    public static class TabsAdapter extends FragmentPagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

        private static final String KEY_TAB_POSITION = "tab_position";

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, int position) {
                clss = _class;
                args = new Bundle();
                args.putInt(KEY_TAB_POSITION, position);
            }

            public int getPosition() {
                return args.getInt(KEY_TAB_POSITION, 0);
            }
        }

        private final ArrayList<TabInfo> mTabs = new ArrayList <TabInfo>();
        ActionBar mActionBar;
        Context mContext;;
        ViewPager mPager;

        public TabsAdapter(Activity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            mContext = activity;
            mActionBar = activity.getActionBar();
            mPager = pager;
            mPager.setAdapter(this);
            mPager.setOnPageChangeListener(this);
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            Log.d("DeskClock","---------------------------- getting fragment for postiion " + position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, int position) {
            TabInfo info = new TabInfo(clss, position);
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Do nothing
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // Do nothing
        }

        @Override
        public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
            // Do nothing
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            TabInfo info = (TabInfo)tab.getTag();
            mPager.setCurrentItem(info.getPosition());
        }

        @Override
        public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
            // Do nothing

        }
    }
}
