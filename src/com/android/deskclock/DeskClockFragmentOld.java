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

import static android.os.BatteryManager.BATTERY_STATUS_UNKNOWN;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlarmManager;
import android.app.Fragment;
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
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 * DeskClock clock view for desk docks.
 */
public class DeskClockFragmentOld extends Fragment  {

/*    private static final boolean DEBUG = false;

    private static final String LOG_TAG = "DeskClock";

    // Alarm action for midnight (so we can update the date display).
    private static final String ACTION_MIDNIGHT = "com.android.deskclock.MIDNIGHT";
    private static final String KEY_DIMMED = "dimmed";
    private static final String KEY_SCREEN_SAVER = "screen_saver";

    // This controls whether or not we will show a battery display when plugged
    // in.
    private static final boolean USE_BATTERY_DISPLAY = false;

    // Delay before engaging the burn-in protection mode (green-on-black).
    private final long SCREEN_SAVER_TIMEOUT = 5 * 60 * 1000; // 5 min

    // Repositioning delay in screen saver.
    public static final long SCREEN_SAVER_MOVE_DELAY = 60 * 1000; // 1 min

    // Delay before dimming the screen
    private static final long DIM_TIMEOUT = 10 * 1000; // 10 seconds

    // Color to use for text & graphics in screen saver mode.
    private int SCREEN_SAVER_COLOR = 0xFF006688;
    private int SCREEN_SAVER_COLOR_DIM = 0xFF001634;

    // Opacity of black layer between clock display and wallpaper.
    private final float DIM_BEHIND_AMOUNT_NORMAL = 0.4f;
    private final float DIM_BEHIND_AMOUNT_DIMMED = 0.8f; // higher contrast when display dimmed

    private final int SCREEN_SAVER_TIMEOUT_MSG   = 0x2000;
    private final int SCREEN_SAVER_MOVE_MSG      = 0x2001;
    private final int DIM_TIMEOUT_MSG            = 0x2002;

    // State variables follow.
    private DigitalClock mTime;
    private TextView mDate;

    private TextView mNextAlarm = null;
    private TextView mBatteryDisplay;

    private boolean mDimmed = false;
    private boolean mScreenSaverMode = false;

    private String mDateFormat;

    private int mBatteryLevel = -1;
    private boolean mPluggedIn = false;

    private boolean mLaunchedFromDock = false;

    private Random mRNG;

    private PendingIntent mMidnightIntent;

    private final Handler mHandy = new Handler() {
        @Override
        public void handleMessage(Message m) {
            if (m.what == SCREEN_SAVER_TIMEOUT_MSG) {
                saveScreen();
            } else if (m.what == SCREEN_SAVER_MOVE_MSG) {
                moveScreenSaver();
            } else if (m.what == DIM_TIMEOUT_MSG) {
                mDimmed = !mDimmed;
                doDim(true);
            }
        }
    };

    private View mAlarmButton;

    private void moveScreenSaver() {
        moveScreenSaverTo(-1,-1);
    }
    private void moveScreenSaverTo(int x, int y) {
        if (!mScreenSaverMode) return;

        final View saver_view = findViewById(R.id.time_date);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if (x < 0 || y < 0) {
            int myWidth = saver_view.getMeasuredWidth();
            int myHeight = saver_view.getMeasuredHeight();
            x = (int)(mRNG.nextFloat()*(metrics.widthPixels - myWidth));
            y = (int)(mRNG.nextFloat()*(metrics.heightPixels - myHeight));
        }

        if (DEBUG) Log.d(LOG_TAG, String.format("screen saver: %d: jumping to (%d,%d)",
                System.currentTimeMillis(), x, y));

        saver_view.setX(x);
        saver_view.setY(y);

        // Synchronize our jumping so that it happens exactly on the second.
        mHandy.sendEmptyMessageDelayed(SCREEN_SAVER_MOVE_MSG,
            SCREEN_SAVER_MOVE_DELAY +
            (1000 - (System.currentTimeMillis() % 1000)));
    }

    private void setWakeLock(boolean hold) {
        if (DEBUG) Log.d(LOG_TAG, (hold ? "hold" : " releas") + "ing wake lock");
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= (WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if (hold)
            winParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        else
            winParams.flags &= (~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        win.setAttributes(winParams);
    }

    private void scheduleScreenSaver() {
        if (!getResources().getBoolean(R.bool.config_requiresScreenSaver)) {
            return;
        }

        // reschedule screen saver
        mHandy.removeMessages(SCREEN_SAVER_TIMEOUT_MSG);
        mHandy.sendMessageDelayed(
            Message.obtain(mHandy, SCREEN_SAVER_TIMEOUT_MSG),
            SCREEN_SAVER_TIMEOUT);
    }

    private void scheduleDim() {
        mHandy.removeMessages(DIM_TIMEOUT_MSG);
        mHandy.sendMessageDelayed(
            Message.obtain(mHandy, DIM_TIMEOUT_MSG),
            DIM_TIMEOUT);
    }

    /**
     * Restores the screen by quitting the screensaver. This should be called only when
     * {@link #mScreenSaverMode} is true.
     *//*
    private void restoreScreen() {
        if (!mScreenSaverMode) return;
        if (DEBUG) Log.d(LOG_TAG, "restoreScreen");
        mScreenSaverMode = false;

        initViews();
        // When quitting the screen saver, un-dim the screen.
        mDimmed = false;
        doDim(true);

        scheduleScreenSaver();
        refreshAll();
    }

    /**
     * Start the screen-saver mode. This is useful for OLED displays that burn in quickly.
     * This should only be called when {@link #mScreenSaverMode} is false;
     *//*
    private void saveScreen() {
        if (mScreenSaverMode) return;
        if (DEBUG) Log.d(LOG_TAG, "saveScreen");

        // quickly stash away the x/y of the current date
        final View oldTimeDate = findViewById(R.id.time_date);
        int oldLoc[] = new int[2];
        oldLoc[0] = oldLoc[1] = -1;
        if (oldTimeDate != null) { // monkeys tell us this is not always around
            oldTimeDate.getLocationOnScreen(oldLoc);
        }

        mScreenSaverMode = true;
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        win.setAttributes(winParams);

        // give up any internal focus before we switch layouts
        final View focused = getCurrentFocus();
        if (focused != null) focused.clearFocus();

        setContentView(R.layout.desk_clock_saver);

        mTime = (DigitalClock) findViewById(R.id.time);
        mDate = (TextView) findViewById(R.id.date);

        final int color = mDimmed ? SCREEN_SAVER_COLOR_DIM : SCREEN_SAVER_COLOR;

        ((AndroidClockTextView)findViewById(R.id.timeDisplay)).setTextColor(color);
        ((AndroidClockTextView)findViewById(R.id.am_pm)).setTextColor(color);
        mDate.setTextColor(color);

        mTime.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);

        mBatteryDisplay = null;

        refreshDate();
        refreshAlarm();

        if (oldLoc[0] >= 0) {
            moveScreenSaverTo(oldLoc[0], oldLoc[1]);
        } else {
            moveScreenSaver();
        }
    }

    @Override
    public void onUserInteraction() {
        if (mScreenSaverMode) {
            restoreScreen();
        } else {
            if (mDimmed) {
                mDimmed = !mDimmed;
                doDim(true);
            }
            scheduleDim();
        }
    }

    // Adapted from KeyguardUpdateMonitor.java
    private void handleBatteryUpdate(int plugged, int status, int level) {
        final boolean pluggedIn = (plugged != 0);
        if (pluggedIn != mPluggedIn) {
            setWakeLock(pluggedIn);
        }
        if (pluggedIn != mPluggedIn || level != mBatteryLevel) {
            mBatteryLevel = level;
            mPluggedIn = pluggedIn;
            refreshBattery();
        }
    }

    private void refreshBattery() {
        // UX wants the battery level removed. This makes it not visible but
        // allows it to be easily turned back on if they change their mind.
        if (!USE_BATTERY_DISPLAY)
            return;
        if (mBatteryDisplay == null) return;

        if (mPluggedIn  || mBatteryLevel < LOW_BATTERY_THRESHOLD) {
            mBatteryDisplay.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, android.R.drawable.ic_lock_idle_charging, 0);
            mBatteryDisplay.setText(
                getString(R.string.battery_charging_level, mBatteryLevel));
            mBatteryDisplay.setVisibility(View.VISIBLE);
        } else {
            mBatteryDisplay.setVisibility(View.INVISIBLE);
        }
    }

    private void refreshDate() {
        final Date now = new Date();
        if (DEBUG) Log.d(LOG_TAG, "refreshing date..." + now);
        mDate.setText(DateFormat.format(mDateFormat, now));
    }

    private void refreshAlarm() {
        if (mNextAlarm == null) return;

        String nextAlarm = Settings.System.getString(getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED);
        if (!TextUtils.isEmpty(nextAlarm)) {
            mNextAlarm.setText(getString(R.string.control_set_alarm_with_existing, nextAlarm));
            mNextAlarm.setVisibility(View.VISIBLE);
        } else if (mAlarmButton != null) {
            mNextAlarm.setVisibility(View.INVISIBLE);
        } else {
            mNextAlarm.setText(R.string.control_set_alarm);
            mNextAlarm.setVisibility(View.VISIBLE);
        }
    }

    private void refreshAll() {
        refreshDate();
        refreshAlarm();
        refreshBattery();
    }

    private void doDim(boolean fade) {
        View tintView = findViewById(R.id.window_tint);
        if (tintView == null) return;

        mTime.setSystemUiVisibility(mDimmed ? View.SYSTEM_UI_FLAG_LOW_PROFILE
                : View.SYSTEM_UI_FLAG_VISIBLE);

        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();

        // dim the wallpaper somewhat (how much is determined below)
        winParams.flags |= (WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        if (mDimmed) {
            winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            winParams.dimAmount = DIM_BEHIND_AMOUNT_DIMMED;
            winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;

            // show the window tint
            tintView.startAnimation(AnimationUtils.loadAnimation(this,
                fade ? R.anim.dim
                     : R.anim.dim_instant));
            mActionBar.hide();
        } else {
            winParams.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            winParams.dimAmount = DIM_BEHIND_AMOUNT_NORMAL;
            winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

            // hide the window tint
            tintView.startAnimation(AnimationUtils.loadAnimation(this,
                fade ? R.anim.undim
                     : R.anim.undim_instant));
            mActionBar.show();
            scheduleDim();
        }
        win.setAttributes(winParams);
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        if (DEBUG) Log.d(LOG_TAG, "onNewIntent with intent: " + newIntent);

        // update our intent so that we can consult it to determine whether or
        // not the most recent launch was via a dock event
        setIntent(newIntent);
    }

    @Override
    public void onStart() {
        super.onStart();

        SCREEN_SAVER_COLOR = getResources().getColor(R.color.screen_saver_color);
        SCREEN_SAVER_COLOR_DIM = getResources().getColor(R.color.screen_saver_dim_color);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        filter.addAction(UiModeManager.ACTION_EXIT_DESK_MODE);
        filter.addAction(ACTION_MIDNIGHT);
        registerReceiver(mIntentReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();

        unregisterReceiver(mIntentReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.d(LOG_TAG, "onResume with intent: " + getIntent());

        // reload the date format in case the user has changed settings
        // recently
        mDateFormat = getString(R.string.full_wday_month_day_no_year);

        // Elaborate mechanism to find out when the day rolls over
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.add(Calendar.DATE, 1);
        long alarmTimeUTC = today.getTimeInMillis();

        mMidnightIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_MIDNIGHT), 0);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC, alarmTimeUTC, AlarmManager.INTERVAL_DAY, mMidnightIntent);
        if (DEBUG) Log.d(LOG_TAG, "set repeating midnight event at UTC: "
            + alarmTimeUTC + " ("
            + (alarmTimeUTC - System.currentTimeMillis())
            + " ms from now) repeating every "
            + AlarmManager.INTERVAL_DAY + " with intent: " + mMidnightIntent);

        // Adjust the display to reflect the currently chosen dim mode.
        doDim(false);
        if (!mScreenSaverMode) {
            restoreScreen(); // disable screen saver
        } else {
            // we have to set it to false because savescreen returns early if
            // it's true
            mScreenSaverMode = false;
            saveScreen();
        }
        refreshAll();
        setWakeLock(mPluggedIn);
        scheduleDim();
        scheduleScreenSaver();

        final boolean launchedFromDock
            = getIntent().hasCategory(Intent.CATEGORY_DESK_DOCK);
        mLaunchedFromDock = launchedFromDock;
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.d(LOG_TAG, "onPause");

        // Turn off the screen saver and cancel any pending timeouts.
        // (But don't un-dim.)
        mHandy.removeMessages(SCREEN_SAVER_TIMEOUT_MSG);
        mHandy.removeMessages(DIM_TIMEOUT_MSG);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(mMidnightIntent);

        super.onPause();
    }

    private void initViews() {
        // give up any internal focus before we switch layouts
        final View focused = getCurrentFocus();
        if (focused != null) focused.clearFocus();

        setContentView(R.layout.desk_clock);

        mTime = (DigitalClock) findViewById(R.id.time);
        mDate = (TextView) findViewById(R.id.date);
        mBatteryDisplay = (TextView) findViewById(R.id.battery);

        mTime.setSystemUiVisibility(View.STATUS_BAR_VISIBLE);
        mTime.getRootView().requestFocus();

        final View.OnClickListener alarmClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDimmed) {
                    mDimmed = false;
                    doDim(true);
                }
                startActivity(new Intent(DeskClockFragment.this, AlarmClock.class));
            }
        };

        mNextAlarm = (TextView) findViewById(R.id.nextAlarm);
        mNextAlarm.setOnClickListener(alarmClickListener);

        mAlarmButton = findViewById(R.id.alarm_button);
        View alarmControl = mAlarmButton != null ? mAlarmButton : findViewById(R.id.nextAlarm);
        alarmControl.setOnClickListener(alarmClickListener);

        View touchView = findViewById(R.id.window_touch);
        // TODO: have UI decide if we need the long press - until decision this is removed
        // since it is not in the wireframe
    /*    touchView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                saveScreen();
                return true;
            }
        });
    }*/
/*
    private void createTabs(int selectedIndex) {
        mActionBar = getActionBar();

        mActionBar.setDisplayOptions(0);
        if (mActionBar != null) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mTimerTab = mActionBar.newTab();
            mTimerTab.setText(getString(R.string.menu_timer));
            mTimerTab.setTabListener(this);
            mActionBar.addTab(mTimerTab);
            mClockTab = mActionBar.newTab();
            mClockTab.setText(getString(R.string.menu_clock));
            mClockTab.setTabListener(this);
            mActionBar.addTab(mClockTab);
            mStopwatchTab = mActionBar.newTab();
            mStopwatchTab.setText(getString(R.string.menu_stopwatch));
            mStopwatchTab.setTabListener(this);
            mActionBar.addTab(mStopwatchTab);
            mActionBar.setSelectedNavigationItem(selectedIndex);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mScreenSaverMode) {
            moveScreenSaver();
        } else {
            initViews();
            doDim(false);
            refreshAll();
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mRNG = new Random();
        if (icicle != null) {
            mDimmed = icicle.getBoolean(KEY_DIMMED, false);
            mScreenSaverMode = icicle.getBoolean(KEY_SCREEN_SAVER, false);
        }
        initViews();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_DIMMED, mDimmed);
        outState.putBoolean(KEY_SCREEN_SAVER, mScreenSaverMode);
    }*/
}
