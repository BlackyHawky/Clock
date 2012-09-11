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
import android.support.v13.app.FragmentStatePagerAdapter;
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
    private static final String KEY_CLOCK_STATE = "clock_state";

    private ActionBar mActionBar;
    private Tab mTimerTab;
    private Tab mClockTab;
    private Tab mStopwatchTab;

    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;

    private static final int TIMER_TAB_INDEX = 0;
    private static final int CLOCK_TAB_INDEX = 1;
    private static final int STOPWATCH_TAB_INDEX = 2;

    private int mSelectedTab;
    private final boolean mDimmed = false;

    private int mClockState = CLOCK_NORMAL;
    private static final int CLOCK_NORMAL = 0;
    private static final int CLOCK_LIGHTS_OUT = 1;
    private static final int CLOCK_DIMMED = 2;



    // Delay before hiding the action bar and buttons
    private static final long LIGHTSOUT_TIMEOUT = 10 * 1000; // 10 seconds
    // Delay before dimming the screen
    private static final long DIM_TIMEOUT = 10 * 1000; // 10 seconds

    // Opacity of black layer between clock display and wallpaper.
    private final float DIM_BEHIND_AMOUNT_NORMAL = 0.4f;
    private final float DIM_BEHIND_AMOUNT_DIMMED = 0.8f; // higher contrast when display dimmed

    private final int SCREEN_SAVER_TIMEOUT_MSG   = 0x2000;
    private final int SCREEN_SAVER_MOVE_MSG      = 0x2001;
    private final int DIM_TIMEOUT_MSG            = 0x2002;
    private final int LIGHTSOUT_TIMEOUT_MSG      = 0x2003;
    private final int BACK_TO_NORMAL_MSG         = 0x2004;



    private final Handler mHandy = new Handler() {
        @Override
        public void handleMessage(Message m) {
     /*       if (m.what == LIGHTSOUT_TIMEOUT_MSG) {
                doLightsOut(true);
                mClockState = CLOCK_LIGHTS_OUT;
                // Only dim if clock fragment is visible
                if (mViewPager.getCurrentItem() ==  CLOCK_TAB_INDEX) {
                    scheduleDim();
                }
            }  else if (m.what == DIM_TIMEOUT_MSG) {
                mClockState = CLOCK_DIMMED;
                doDim(true);
            } else if (m.what == BACK_TO_NORMAL_MSG){
                // ignore user interaction and do not go back to normal if a button was clicked
                DeskClockFragment f =
                        (DeskClockFragment) mTabsAdapter.getFragment(mViewPager.getCurrentItem());
                if (f != null && f.isButtonClicked()) {
                    return;
                }

                int oldState = mClockState;
                mClockState = CLOCK_NORMAL;

                switch (oldState) {
                    case CLOCK_LIGHTS_OUT:
                        doLightsOut(false);
                        break;
                    case CLOCK_DIMMED:
                        doLightsOut(false);
                        doDim(true);
                        break;
                }
            }*/
        }
    };

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        if (DEBUG) Log.d(LOG_TAG, "onNewIntent with intent: " + newIntent);

        // update our intent so that we can consult it to determine whether or
        // not the most recent launch was via a dock event
        setIntent(newIntent);
    }


    private void initViews() {

        if (mTabsAdapter == null) {
            mViewPager = new ViewPager(this);
            mViewPager.setId(R.id.desk_clock_pager);
            mTabsAdapter = new TabsAdapter(this, mViewPager);
            createTabs(mSelectedTab);
        }
        setContentView(mViewPager);
        mActionBar.setSelectedNavigationItem(mSelectedTab);
    }

    private void createTabs(int selectedIndex) {
        mActionBar = getActionBar();

        mActionBar.setDisplayOptions(0);
        if (mActionBar != null) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mTimerTab = mActionBar.newTab();
            mTimerTab.setIcon(R.drawable.timer_tab);
            mTabsAdapter.addTab(mTimerTab, TimerFragment.class,TIMER_TAB_INDEX);
            mClockTab = mActionBar.newTab();
            mClockTab.setIcon(R.drawable.clock_tab);
            mTabsAdapter.addTab(mClockTab, ClockFragment.class,CLOCK_TAB_INDEX);
            mStopwatchTab = mActionBar.newTab();
            mStopwatchTab.setIcon(R.drawable.stopwatch_tab);
            mTabsAdapter.addTab(mStopwatchTab, StopwatchFragment.class,STOPWATCH_TAB_INDEX);
            mActionBar.setSelectedNavigationItem(selectedIndex);
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mSelectedTab = CLOCK_TAB_INDEX;
        if (icicle != null) {
            mSelectedTab = icicle.getInt(KEY_SELECTED_TAB, CLOCK_TAB_INDEX);
            mClockState = icicle.getInt(KEY_CLOCK_STATE, CLOCK_NORMAL);
        }
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setClockState(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SELECTED_TAB, mActionBar.getSelectedNavigationIndex());
        outState.putInt(KEY_CLOCK_STATE, mClockState);
    }

    private void setClockState(boolean fade) {
        doDim(fade);
        switch(mClockState) {
            case CLOCK_NORMAL:
                doLightsOut(false);
                break;
            case CLOCK_LIGHTS_OUT:
            case CLOCK_DIMMED:
                doLightsOut(true);
                break;
            default:
                break;
        }
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

    private void scheduleLightsOut() {
        mHandy.removeMessages(LIGHTSOUT_TIMEOUT_MSG);
        mHandy.sendMessageDelayed(Message.obtain(mHandy, LIGHTSOUT_TIMEOUT_MSG), LIGHTSOUT_TIMEOUT);
    }

    public void doLightsOut(boolean state) {

        if (state) {
            mViewPager.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
            mActionBar.hide();
        } else {
            mViewPager.setSystemUiVisibility(View.STATUS_BAR_VISIBLE);
            mActionBar.show();
        }

        // in clock view show/hide the buttons at the bottom
        if (mViewPager.getCurrentItem() ==  CLOCK_TAB_INDEX) {
            Fragment f = mTabsAdapter.getFragment(CLOCK_TAB_INDEX);
            if (f != null) {
                ((ClockFragment)f).showButtons(!state);
            }
        }
        if (!state) {
            // Make sure dim will not start before lights out
            mHandy.removeMessages(DIM_TIMEOUT_MSG);
            scheduleLightsOut();
        }
    }

    private void doDim(boolean fade) {
        if (mClockState == CLOCK_DIMMED) {
            mViewPager.startAnimation(
                    AnimationUtils.loadAnimation(this, fade ? R.anim.dim : R.anim.dim_instant));
        } else {
            mViewPager.startAnimation(
                    AnimationUtils.loadAnimation(this, fade ? R.anim.undim : R.anim.undim_instant));
        }
    }

    private void scheduleDim() {
        mHandy.removeMessages(DIM_TIMEOUT_MSG);
        mHandy.sendMessageDelayed(Message.obtain(mHandy, DIM_TIMEOUT_MSG), DIM_TIMEOUT);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        mHandy.removeMessages(BACK_TO_NORMAL_MSG);
        mHandy.sendMessage(Message.obtain(mHandy, BACK_TO_NORMAL_MSG));
    }

    /***
     * Adapter for wrapping together the ActionBar's tab with the ViewPager
     */

    private class TabsAdapter extends FragmentStatePagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

        private static final String KEY_TAB_POSITION = "tab_position";

        final class TabInfo {
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
        private final Fragment [] mFragments = new Fragment[3];
        ActionBar mMainActionBar;
        Context mContext;
        ViewPager mPager;

        public TabsAdapter(Activity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            mContext = activity;
            mMainActionBar = activity.getActionBar();
            mPager = pager;
            mPager.setAdapter(this);
            mPager.setOnPageChangeListener(this);
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            DeskClockFragment f = (DeskClockFragment) Fragment.instantiate(
                    mContext, info.clss.getName(), info.args);
            f.setContext(mContext);
            mFragments [position] = f;
            return f;
        }

        public Fragment getFragment(int position) {
            return mFragments [position];
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
            mMainActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Do nothing
        }

        @Override
        public void onPageSelected(int position) {
            mMainActionBar.setSelectedNavigationItem(position);
            onUserInteraction();
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
            onUserInteraction();
        }

        @Override
        public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
            // Do nothing

        }
    }
}
