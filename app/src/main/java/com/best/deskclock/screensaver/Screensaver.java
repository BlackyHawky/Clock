/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.screensaver;

import static com.best.deskclock.utils.AlarmUtils.ACTION_NEXT_ALARM_CHANGED_BY_CLOCK;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;

import com.best.deskclock.R;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.ScreensaverUtils;
import com.best.deskclock.utils.SdkUtils;

public final class Screensaver extends DreamService {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("Screensaver");

    private final OnPreDrawListener mStartPositionUpdater = new StartPositionUpdater();
    private MoveScreensaverRunnable mPositionUpdater;

    private String mDateFormat;
    private String mDateFormatForAccessibility;

    private View mContentView;

    // Runs every midnight or when the time changes and refreshes the date.
    private final Runnable mMidnightUpdater = new Runnable() {
        @Override
        public void run() {
            ClockUtils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        }
    };

    /**
     * Receiver to alarm clock changes.
     */
    private final BroadcastReceiver mAlarmChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AlarmUtils.refreshAlarm(Screensaver.this, mContentView);
        }
    };

    private View mMainClockView;

    @Override
    public void onCreate() {
        LOGGER.v("Screensaver created");
        super.onCreate();

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onAttachedToWindow() {
        LOGGER.v("Screensaver attached to window");
        super.onAttachedToWindow();

        setContentView(R.layout.desk_clock_saver);

        mContentView = findViewById(R.id.saver_container);
        mMainClockView = mContentView.findViewById(R.id.main_clock);

        ScreensaverUtils.setScreensaverMarginsAndClockStyle(this, mMainClockView);

        ScreensaverUtils.hideScreensaverSystemBars(getWindow(), mContentView);

        mPositionUpdater = new MoveScreensaverRunnable(mContentView, mMainClockView);

        // We want the screen saver to exit upon user interaction.
        setInteractive(false);
        setFullscreen(true);

        // Setup handlers for time reference changes and date updates.
        final IntentFilter filter = new IntentFilter(ACTION_NEXT_ALARM_CHANGED_BY_CLOCK);
        if (SdkUtils.isAtLeastAndroid13()) {
            registerReceiver(mAlarmChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mAlarmChangedReceiver, filter);
        }

        ClockUtils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        AlarmUtils.refreshAlarm(this, mContentView);

        startPositionUpdater();
        UiDataModel.getUiDataModel().addMidnightCallback(mMidnightUpdater, 100);
    }

    @Override
    public void onDetachedFromWindow() {
        LOGGER.v("Screensaver detached from window");
        super.onDetachedFromWindow();

        UiDataModel.getUiDataModel().removePeriodicCallback(mMidnightUpdater);
        stopPositionUpdater();

        // Tear down handlers for time reference changes and date updates.
        unregisterReceiver(mAlarmChangedReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LOGGER.v("Screensaver configuration changed");
        super.onConfigurationChanged(newConfig);

        startPositionUpdater();
    }

    /**
     * The {@link #mContentView} will be drawn shortly. When that draw occurs, the position updater
     * callback will also be executed to choose a random position for the time display as well as
     * schedule future callbacks to move the time display each minute.
     */
    private void startPositionUpdater() {
        if (mContentView != null) {
            mContentView.getViewTreeObserver().addOnPreDrawListener(mStartPositionUpdater);
        }
    }

    /**
     * This activity is no longer in the foreground; position callbacks should be removed.
     */
    private void stopPositionUpdater() {
        if (mContentView != null) {
            mContentView.getViewTreeObserver().removeOnPreDrawListener(mStartPositionUpdater);
        }
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
                // (Re)start the periodic position updater.
                mPositionUpdater.start();

                // This listener must now be removed to avoid starting the position updater again.
                mContentView.getViewTreeObserver().removeOnPreDrawListener(mStartPositionUpdater);
            }
            return true;
        }
    }
}
