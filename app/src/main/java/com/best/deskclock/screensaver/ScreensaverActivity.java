/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.screensaver;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.os.BatteryManager.EXTRA_PLUGGED;

import static com.best.deskclock.utils.AlarmUtils.ACTION_NEXT_ALARM_CHANGED_BY_CLOCK;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.best.deskclock.BaseActivity;
import com.best.deskclock.R;
import com.best.deskclock.events.Events;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.ScreensaverUtils;
import com.best.deskclock.utils.SdkUtils;

import java.util.Objects;

public class ScreensaverActivity extends BaseActivity {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("ScreensaverActivity");

    private final OnPreDrawListener mStartPositionUpdater = new StartPositionUpdater();
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private View mContentView;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LOGGER.v("ScreensaverActivity onReceive, action: " + intent.getAction());

            switch (Objects.requireNonNull(intent.getAction())) {
                case Intent.ACTION_POWER_CONNECTED -> updateWakeLock(true);
                case Intent.ACTION_POWER_DISCONNECTED -> updateWakeLock(false);
                case Intent.ACTION_USER_PRESENT -> finish();
                case ACTION_NEXT_ALARM_CHANGED_BY_CLOCK ->
                        AlarmUtils.refreshAlarm(ScreensaverActivity.this, mContentView);
            }
        }
    };

    // Runs every midnight or when the time changes and refreshes the date.
    private final Runnable mMidnightUpdater = new Runnable() {
        @Override
        public void run() {
            ClockUtils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        }
    };

    private View mMainClockView;

    private MoveScreensaverRunnable mPositionUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        setContentView(R.layout.desk_clock_saver);
        mContentView = findViewById(R.id.saver_container);

        mMainClockView = findViewById(R.id.main_clock);

        ScreensaverUtils.setScreensaverMarginsAndClockStyle(this, mMainClockView);

        mPositionUpdater = new MoveScreensaverRunnable(mContentView, mMainClockView);

        final Intent intent = getIntent();
        if (intent != null) {
            final int eventLabel = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, 0);
            Events.sendScreensaverEvent(R.string.action_show, eventLabel);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onStart() {
        super.onStart();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(ACTION_NEXT_ALARM_CHANGED_BY_CLOCK);

        if (SdkUtils.isAtLeastAndroid13()) {
            registerReceiver(mIntentReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mIntentReceiver, filter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ScreensaverUtils.hideScreensaverSystemBars(getWindow(), mContentView);

        ClockUtils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        AlarmUtils.refreshAlarm(ScreensaverActivity.this, mContentView);

        startPositionUpdater();
        UiDataModel.getUiDataModel().addMidnightCallback(mMidnightUpdater, 100);

        final Intent intent = SdkUtils.isAtLeastAndroid13()
                ? registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
                : registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        final boolean pluggedIn = intent != null && intent.getIntExtra(EXTRA_PLUGGED, 0) != 0;
        updateWakeLock(pluggedIn);
    }

    @Override
    public void onPause() {
        super.onPause();
        UiDataModel.getUiDataModel().removePeriodicCallback(mMidnightUpdater);
        stopPositionUpdater();
    }

    @Override
    public void onStop() {
        unregisterReceiver(mIntentReceiver);
        super.onStop();
    }

    @Override
    public void onUserInteraction() {
        // We want the screen saver to exit upon user interaction.
        finish();
    }

    /**
     * @param pluggedIn {@code true} if the device is currently plugged in to a charger
     */
    private void updateWakeLock(boolean pluggedIn) {
        final Window win = getWindow();
        final WindowManager.LayoutParams winParams = win.getAttributes();

        if (SdkUtils.isAtLeastAndroid11()) {
            WindowInsetsController insetsController = win.getInsetsController();
            if (insetsController != null) {
                insetsController.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
                insetsController.hide(WindowInsets.Type.systemBars());
            }
        } else {
            winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }

        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;

        int flags = getWindowFlags();

        if (pluggedIn) {
            winParams.flags |= flags;
        } else {
            winParams.flags &= ~flags;
        }

        win.setAttributes(winParams);

        if (SdkUtils.isAtLeastAndroid81()) {
            setShowWhenLocked(pluggedIn);
            setTurnScreenOn(pluggedIn);
        }
    }

    /**
     * Returns the flags to apply for modern versions of Android (API 27 and above).
     * <p>
     * Use official methods like {@link #setShowWhenLocked(boolean)} and {@link #setTurnScreenOn(boolean)}
     * to manage lock screen display and screen wake-up.
     *
     * @return the flags to apply to the window to keep the screen active.
     */
    private static int getWindowFlags() {
        if (SdkUtils.isAtLeastAndroid81()) {
            return WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
        } else {
            return getLegacyWindowFlags();
        }
    }

    /**
     * @return the flags to apply to the window to keep the screen active (before API 27).
     */
    private static int getLegacyWindowFlags() {
        return WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
    }

    /**
     * The {@link #mContentView} will be drawn shortly. When that draw occurs, the position updater
     * callback will also be executed to choose a random position for the time display as well as
     * schedule future callbacks to move the time display each minute.
     */
    private void startPositionUpdater() {
        mContentView.getViewTreeObserver().addOnPreDrawListener(mStartPositionUpdater);
    }

    /**
     * This activity is no longer in the foreground; position callbacks should be removed.
     */
    private void stopPositionUpdater() {
        mContentView.getViewTreeObserver().removeOnPreDrawListener(mStartPositionUpdater);
        mPositionUpdater.stop();
    }

    private final class StartPositionUpdater implements OnPreDrawListener {
        /**
         * This callback occurs after initial layout has completed. It is an appropriate place to
         * select a random position for {@link #mMainClockView} and schedule future callbacks to update
         * its position.
         *
         * @return {@code true} to continue with the drawing pass
         */
        @Override
        public boolean onPreDraw() {
            if (mContentView.getViewTreeObserver().isAlive()) {
                // Start the periodic position updater.
                mPositionUpdater.start();

                // This listener must now be removed to avoid starting the position updater again.
                mContentView.getViewTreeObserver().removeOnPreDrawListener(mStartPositionUpdater);
            }
            return true;
        }
    }

}
