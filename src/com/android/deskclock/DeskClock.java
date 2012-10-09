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
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.android.deskclock.stopwatch.StopwatchFragment;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.stopwatch.Stopwatches;
import com.android.deskclock.timer.TimerFragment;
import com.android.deskclock.timer.Timers;
import com.android.deskclock.worldclock.CitiesActivity;

import java.util.ArrayList;
import java.util.TimeZone;

/**
 * DeskClock clock view for desk docks.
 */
public class DeskClock extends Activity {
    private static final boolean DEBUG = false;

    private static final String LOG_TAG = "DeskClock";

    // Alarm action for midnight (so we can update the date display).
    private static final String KEY_SELECTED_TAB = "selected_tab";
    private static final String KEY_CLOCK_STATE = "clock_state";

    public static final String SELECT_TAB_INTENT_EXTRA = "deskclock.select.tab";

    private ActionBar mActionBar;
    private Tab mTimerTab;
    private Tab mClockTab;
    private Tab mStopwatchTab;

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

    public static final int TIMER_TAB_INDEX = 0;
    public static final int CLOCK_TAB_INDEX = 1;
    public static final int STOPWATCH_TAB_INDEX = 2;

    private int mSelectedTab;
    private final boolean mDimmed = false;
    private boolean mAddingTimer;
    private int[] mClockButtonIds;
    private int[] mTimerButtonId;
    private int mFooterHeight;
    private int mDimAnimationDuration;
    private View mClockBackground;
    private View mClockForeground;
    private boolean mIsBlackBackground;

    private int mClockState = CLOCK_NORMAL;
    private static final int CLOCK_NORMAL = 0;
    private static final int CLOCK_LIGHTS_OUT = 1;
    private static final int CLOCK_DIMMED = 2;



    // Delay before hiding the action bar and buttons
    private static final long CLOCK_LIGHTSOUT_TIMEOUT = 10 * 1000; // 10 seconds
    private static final long TIMER_SW_LIGHTSOUT_TIMEOUT = 3 * 1000; // 10 seconds
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
            switch(m.what) {
                case LIGHTSOUT_TIMEOUT_MSG:
                    if (mViewPager.getCurrentItem() == TIMER_TAB_INDEX && mAddingTimer) {
                        break;
                    }
                    mClockState = CLOCK_LIGHTS_OUT;
                    setClockState(true);
                    break;
                case DIM_TIMEOUT_MSG:
                    mClockState = CLOCK_DIMMED;
                    doDim(true);
                    break;
            }
        }
    };

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        if (DEBUG) Log.d(LOG_TAG, "onNewIntent with intent: " + newIntent);

        // update our intent so that we can consult it to determine whether or
        // not the most recent launch was via a dock event
        setIntent(newIntent);

        // Timer receiver may ask to go to the timers fragment if a timer expired.
        int tab = newIntent.getIntExtra(SELECT_TAB_INTENT_EXTRA, -1);
        if (tab != -1) {
            if (mActionBar != null) {
                mActionBar.setSelectedNavigationItem(tab);
            }
        }
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
            mTimerTab.setContentDescription(R.string.menu_timer);
            mTabsAdapter.addTab(mTimerTab, TimerFragment.class,TIMER_TAB_INDEX);

            mClockTab = mActionBar.newTab();
            mClockTab.setIcon(R.drawable.clock_tab);
            mClockTab.setContentDescription(R.string.menu_clock);
            mTabsAdapter.addTab(mClockTab, ClockFragment.class,CLOCK_TAB_INDEX);
            mStopwatchTab = mActionBar.newTab();
            mStopwatchTab.setIcon(R.drawable.stopwatch_tab);
            mStopwatchTab.setContentDescription(R.string.menu_stopwatch);
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

        // Timer receiver may ask the app to go to the timer fragment if a timer expired
        Intent i = getIntent();
        if (i != null) {
            int tab = i.getIntExtra(SELECT_TAB_INTENT_EXTRA, -1);
            if (tab != -1) {
                mSelectedTab = tab;
            }
        }
        initViews();
        setHomeTimeZone();

        int[] buttonIds = {R.id.alarms_button, R.id.cities_button, R.id.menu_button};
        mClockButtonIds = buttonIds;
        int[] button = {R.id.timer_add_timer};
        mTimerButtonId = button;
        mFooterHeight = (int) getResources().getDimension(R.dimen.button_footer_height);
        mDimAnimationDuration = getResources().getInteger(R.integer.dim_animation_duration);
        mClockBackground = findViewById(R.id.clock_background);
        mClockForeground = findViewById(R.id.clock_foreground);
        mIsBlackBackground = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAddingTimer = true;
        setClockState(false);

        Intent stopwatchIntent = new Intent(getApplicationContext(), StopwatchService.class);
        stopwatchIntent.setAction(Stopwatches.KILL_NOTIF);
        startService(stopwatchIntent);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Timers.NOTIF_APP_OPEN, true);
        editor.apply();
        Intent timerIntent = new Intent();
        timerIntent.setAction(Timers.NOTIF_IN_USE_CANCEL);
        sendBroadcast(timerIntent);
    }

    @Override
    public void onPause() {
        removeLightsMessages();

        Intent intent = new Intent(getApplicationContext(), StopwatchService.class);
        intent.setAction(Stopwatches.SHOW_NOTIF);
        startService(intent);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Timers.NOTIF_APP_OPEN, false);
        editor.apply();
        Intent timerIntent = new Intent();
        timerIntent.setAction(Timers.NOTIF_IN_USE_SHOW);
        sendBroadcast(timerIntent);

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, mActionBar.getSelectedNavigationIndex());
        outState.putInt(KEY_CLOCK_STATE, mClockState);
    }

    public void clockButtonsOnClick(View v) {
        if (!isClockStateNormal()) {
            bringLightsUp(true);
            return;
        }
        if (v == null)
            return;
        switch (v.getId()) {
            case R.id.alarms_button:
                startActivity(new Intent(this, AlarmClock.class));
                break;
            case R.id.cities_button:
                startActivity(new Intent(this, CitiesActivity.class));
                break;
            case R.id.menu_button:
                showMenu(v);
                break;
            default:
                break;
        }
    }

    private void showMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener () {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_item_settings:
                        startActivity(new Intent(DeskClock.this, SettingsActivity.class));
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
                    default:
                        break;
                }
                return true;
            }
        });
        popupMenu.inflate(R.menu.desk_clock_menu);

        Menu menu = popupMenu.getMenu();
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }
        popupMenu.show();
    }

    /***
     * Insert the local time zone as the Home Time Zone if one is not set
     */
    private void setHomeTimeZone() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String homeTimeZone = prefs.getString(SettingsActivity.KEY_HOME_TZ, "");
        if (!homeTimeZone.isEmpty()) {
        return;
        }
        homeTimeZone = TimeZone.getDefault().getID();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SettingsActivity.KEY_HOME_TZ, homeTimeZone);
        editor.apply();
        Log.v(LOG_TAG, "Setting home time zone to " + homeTimeZone);
    }

    public boolean isClockStateNormal() {
        return mClockState == CLOCK_NORMAL;
    }

    private boolean isClockStateDimmed() {
        return mClockState == CLOCK_DIMMED;
    }

    public void clockOnViewClick(View view) {
        // Toggle lights
        switch(mClockState) {
            case CLOCK_NORMAL:
                mClockState = CLOCK_LIGHTS_OUT;
                break;
            case CLOCK_LIGHTS_OUT:
            case CLOCK_DIMMED:
                mClockState = CLOCK_NORMAL;
                break;
            default:
                Log.v(LOG_TAG, "in a bad state. setting to normal.");
                mClockState = CLOCK_NORMAL;
                break;
        }
        setClockState(true);
    }

    private void setClockState(boolean fade) {
        doDim(fade);
        switch(mClockState) {
            case CLOCK_NORMAL:
                doLightsOut(false, fade);
                break;
            case CLOCK_LIGHTS_OUT:
                doLightsOut(true, fade);
                if (mViewPager.getCurrentItem() == CLOCK_TAB_INDEX) {
                    scheduleDim();
                }
                break;
            case CLOCK_DIMMED:
                doLightsOut(true, fade);
                break;
            default:
                break;
        }
    }

    private void doDim(boolean fade) {
        if (mClockBackground == null) {
            mClockBackground = findViewById(R.id.clock_background);
            mClockForeground = findViewById(R.id.clock_foreground);
            if (mClockBackground == null || mClockForeground == null) {
                return;
            }
        }
        if (mClockState == CLOCK_DIMMED) {
            mClockForeground.startAnimation(
                    AnimationUtils.loadAnimation(this, fade ? R.anim.dim : R.anim.dim_instant));
            TransitionDrawable backgroundFade =
                    (TransitionDrawable) mClockBackground.getBackground();
            TransitionDrawable foregroundFade =
                    (TransitionDrawable) mClockForeground.getBackground();
            backgroundFade.startTransition(fade ? mDimAnimationDuration : 0);
            foregroundFade.startTransition(fade ? mDimAnimationDuration : 0);
            mIsBlackBackground = true;
        } else {
            if (mIsBlackBackground) {
                mClockForeground.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.undim));
                TransitionDrawable backgroundFade =
                        (TransitionDrawable) mClockBackground.getBackground();
                TransitionDrawable foregroundFade =
                        (TransitionDrawable) mClockForeground.getBackground();
                backgroundFade.reverseTransition(0);
                foregroundFade.reverseTransition(0);
                mIsBlackBackground = false;
            }
        }
    }

    public void doLightsOut(boolean lightsOut, boolean fade) {
        if (lightsOut) {
            mActionBar.hide();
            mViewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            hideAnimated(fade, R.id.clock_footer, mFooterHeight, mClockButtonIds);
            if (mViewPager.getCurrentItem() != STOPWATCH_TAB_INDEX) {
                hideAnimated(fade, R.id.timer_footer, mFooterHeight, mTimerButtonId);
            }
        } else {
            mActionBar.show();
            mViewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            unhideAnimated(fade, R.id.clock_footer, mFooterHeight, mClockButtonIds);
            if (mViewPager.getCurrentItem() != STOPWATCH_TAB_INDEX) {
                unhideAnimated(fade, R.id.timer_footer, mFooterHeight, mTimerButtonId);
            }
            // Make sure dim will not start before lights out
            removeLightsMessages();
            scheduleLightsOut();
        }
    }

    private void hideAnimated(boolean fade, int viewId, int toYDelta, final int[] buttonIds) {
        View view = findViewById(viewId);
        if (view != null) {
            Animation hideAnimation = new TranslateAnimation(0, 0, 0, toYDelta);
            hideAnimation.setDuration(fade ? 350 : 0);
            hideAnimation.setFillAfter(true);
            hideAnimation.setInterpolator(AnimationUtils.loadInterpolator(this,
                    android.R.interpolator.decelerate_cubic));
            hideAnimation.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (buttonIds != null) {
                        for (int buttonId : buttonIds) {
                            View button = findViewById(buttonId);
                            if (button != null) {
                                button.setVisibility(View.GONE);
                            }
                        }
                    }
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationStart(Animation animation) {
                }
            });
            view.startAnimation(hideAnimation);
        }
    }

    private void unhideAnimated(boolean fade, int viewId, int fromYDelta, final int[] buttonIds) {
        View view = findViewById(viewId);
        if (view != null) {
            Animation unhideAnimation = new TranslateAnimation(0, 0, fromYDelta, 0);
            unhideAnimation.setDuration(fade ? 350 : 0);
            unhideAnimation.setFillAfter(true);
            unhideAnimation.setInterpolator(AnimationUtils.loadInterpolator(this,
                    android.R.interpolator.decelerate_cubic));
            if (buttonIds != null) {
                for (int buttonId : buttonIds) {
                    View button = findViewById(buttonId);
                    if (button != null) {
                        button.setVisibility(View.VISIBLE);
                    }
                }
            }
            view.startAnimation(unhideAnimation);
        }
    }

    public void removeLightsMessages() {
        mHandy.removeMessages(BACK_TO_NORMAL_MSG);
        mHandy.removeMessages(LIGHTSOUT_TIMEOUT_MSG);
        mHandy.removeMessages(DIM_TIMEOUT_MSG);
    }

    public void scheduleLightsOut() {
        removeLightsMessages();
        long timeout;
        if (mViewPager.getCurrentItem() == CLOCK_TAB_INDEX) {
            timeout = CLOCK_LIGHTSOUT_TIMEOUT;
        } else {
            timeout = TIMER_SW_LIGHTSOUT_TIMEOUT;
        }
        mHandy.sendMessageDelayed(Message.obtain(mHandy, LIGHTSOUT_TIMEOUT_MSG), timeout);
    }

    public void bringLightsUp(boolean fade) {
        removeLightsMessages();
        if (mClockState != CLOCK_NORMAL) {
            mClockState = CLOCK_NORMAL;
            setClockState(fade);
        } else {
            scheduleLightsOut();
        }
    }

    private void scheduleDim() {
        removeLightsMessages();
        mHandy.sendMessageDelayed(Message.obtain(mHandy, DIM_TIMEOUT_MSG), DIM_TIMEOUT);
    }

    public void setTimerAddingTimerState(boolean addingTimer) {
        mAddingTimer = addingTimer;
        if (addingTimer && mViewPager.getCurrentItem() == TIMER_TAB_INDEX) {
            removeLightsMessages();
        }
    }

    /***
     * Adapter for wrapping together the ActionBar's tab with the ViewPager
     */

    private class TabsAdapter extends FragmentPagerAdapter
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
            return f;
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
            boolean fade = !isClockStateNormal();
            bringLightsUp(fade);
        }

        @Override
        public void onPageSelected(int position) {
            mMainActionBar.setSelectedNavigationItem(position);
            boolean fade = !isClockStateNormal();
            bringLightsUp(fade);
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
            boolean fade = !isClockStateNormal();
            bringLightsUp(fade);
        }

        @Override
        public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
            // Do nothing

        }
    }

    public static class OnTapListener implements OnTouchListener {
        private DeskClock mActivity;
        private float mLastTouchX;
        private float mLastTouchY;
        private long mLastTouchTime;
        private float MAX_MOVEMENT_ALLOWED = 20;
        private long MAX_TIME_ALLOWED = 500;

        public OnTapListener(Activity activity) {
            mActivity = (DeskClock) activity;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case (MotionEvent.ACTION_DOWN):
                    if (mActivity.isClockStateDimmed()) {
                        mActivity.clockOnViewClick(v);
                        resetValues();
                        break;
                    }
                    mLastTouchX = e.getX();
                    mLastTouchY = e.getY();
                    mLastTouchTime = Utils.getTimeNow();
                    mActivity.removeLightsMessages();
                    break;
                case (MotionEvent.ACTION_UP):
                    float xDiff = Math.abs(e.getX()-mLastTouchX);
                    float yDiff = Math.abs(e.getY()-mLastTouchY);
                    long timeDiff = (Utils.getTimeNow() - mLastTouchTime);
                    if (xDiff < MAX_MOVEMENT_ALLOWED && yDiff < MAX_MOVEMENT_ALLOWED
                            && timeDiff < MAX_TIME_ALLOWED) {
                        mActivity.clockOnViewClick(v);
                        return true;
                    } else {
                        if (mActivity.isClockStateNormal()){
                            mActivity.scheduleLightsOut();
                        } else if (!mActivity.isClockStateDimmed()) {
                            mActivity.scheduleDim();
                        }
                    }
                    break;
                case (MotionEvent.ACTION_MOVE):
                    break;
                default:
                    resetValues();
            }
            return false;
        }

        private void resetValues() {
            mLastTouchX = -1*MAX_MOVEMENT_ALLOWED + 1;
            mLastTouchY = -1*MAX_MOVEMENT_ALLOWED + 1;
            mLastTouchTime = -1*MAX_TIME_ALLOWED + 1;
        }
    }

}
