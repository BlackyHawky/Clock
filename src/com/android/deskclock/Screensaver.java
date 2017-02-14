/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.TextClock;

import com.android.deskclock.data.DataModel;
import com.android.deskclock.uidata.UiDataModel;

public final class Screensaver extends DreamService {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("Screensaver");

    private final OnPreDrawListener mStartPositionUpdater = new StartPositionUpdater();
    private MoveScreensaverRunnable mPositionUpdater;

    private String mDateFormat;
    private String mDateFormatForAccessibility;

    private View mContentView;
    private View mMainClockView;
    private TextClock mDigitalClock;
    private AnalogClock mAnalogClock;

    /* Register ContentObserver to see alarm changes for pre-L */
    private final ContentObserver mSettingsContentObserver =
            Utils.isLOrLater() ? null : new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    Utils.refreshAlarm(Screensaver.this, mContentView);
                }
            };

    // Runs every midnight or when the time changes and refreshes the date.
    private final Runnable mMidnightUpdater = new Runnable() {
        @Override
        public void run() {
            Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        }
    };

    /**
     * Receiver to alarm clock changes.
     */
    private final BroadcastReceiver mAlarmChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Utils.refreshAlarm(Screensaver.this, mContentView);
        }
    };

    @Override
    public void onCreate() {
        LOGGER.v("Screensaver created");

        setTheme(R.style.Theme_DeskClock);
        super.onCreate();

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
    }

    @Override
    public void onAttachedToWindow() {
        LOGGER.v("Screensaver attached to window");
        super.onAttachedToWindow();

        setContentView(R.layout.desk_clock_saver);

        mContentView = findViewById(R.id.saver_container);
        mMainClockView = mContentView.findViewById(R.id.main_clock);
        mDigitalClock = (TextClock) mMainClockView.findViewById(R.id.digital_clock);
        mAnalogClock = (AnalogClock) mMainClockView.findViewById(R.id.analog_clock);

        setClockStyle();
        Utils.setClockIconTypeface(mContentView);
        Utils.setTimeFormat(mDigitalClock, false);
        mAnalogClock.enableSeconds(false);

        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        mPositionUpdater = new MoveScreensaverRunnable(mContentView, mMainClockView);

        // We want the screen saver to exit upon user interaction.
        setInteractive(false);
        setFullscreen(true);

        // Setup handlers for time reference changes and date updates.
        if (Utils.isLOrLater()) {
            registerReceiver(mAlarmChangedReceiver,
                    new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED));
        }

        if (mSettingsContentObserver != null) {
            @SuppressWarnings("deprecation")
            final Uri uri = Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED);
            getContentResolver().registerContentObserver(uri, false, mSettingsContentObserver);
        }

        Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        Utils.refreshAlarm(this, mContentView);

        startPositionUpdater();
        UiDataModel.getUiDataModel().addMidnightCallback(mMidnightUpdater, 100);
    }

    @Override
    public void onDetachedFromWindow() {
        LOGGER.v("Screensaver detached from window");
        super.onDetachedFromWindow();

        if (mSettingsContentObserver != null) {
            getContentResolver().unregisterContentObserver(mSettingsContentObserver);
        }

        UiDataModel.getUiDataModel().removePeriodicCallback(mMidnightUpdater);
        stopPositionUpdater();

        // Tear down handlers for time reference changes and date updates.
        if (Utils.isLOrLater()) {
            unregisterReceiver(mAlarmChangedReceiver);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LOGGER.v("Screensaver configuration changed");
        super.onConfigurationChanged(newConfig);

        startPositionUpdater();
    }

    private void setClockStyle() {
        Utils.setScreensaverClockStyle(mDigitalClock, mAnalogClock);
        final boolean dimNightMode = DataModel.getDataModel().getScreensaverNightModeOn();
        Utils.dimClockView(dimNightMode, mMainClockView);
        setScreenBright(!dimNightMode);
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
