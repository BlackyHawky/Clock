/*
 * Copyright (C) 2011 The Android Open Source Project
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

    private View mContentView, mSaverView;
    private View mAnalogClock, mDigitalClock;
    private String mDateFormat;
    private String mDateFormatForAccessibility;

    /* Register ContentObserver to see alarm changes for pre-L */
    private final ContentObserver mSettingsContentObserver = Utils.isPreL()
        ? new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                Utils.refreshAlarm(Screensaver.this, mContentView);
            }
        }
        : null;

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
        super.onCreate();

        setTheme(R.style.ScreensaverActivityTheme);

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
    }

    @Override
    public void onAttachedToWindow() {
        LOGGER.v("Screensaver attached to window");
        super.onAttachedToWindow();

        setContentView(R.layout.desk_clock_saver);
        mDigitalClock = findViewById(R.id.digital_clock);
        mAnalogClock = findViewById(R.id.analog_clock);
        mSaverView = findViewById(R.id.main_clock);
        mContentView = (View) mSaverView.getParent();

        setClockStyle();
        Utils.setTimeFormat((TextClock) mDigitalClock);

        mPositionUpdater = new MoveScreensaverRunnable(mContentView, mSaverView);

        // We want the screen saver to exit upon user interaction.
        setInteractive(false);

        setFullscreen(true);

        // Setup handlers for time reference changes and date updates.
        final IntentFilter filter = new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
        registerReceiver(mAlarmChangedReceiver, filter);

        if (mSettingsContentObserver != null) {
            @SuppressWarnings("deprecation")
            final Uri uri = Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED);
            getContentResolver().registerContentObserver(uri, false, mSettingsContentObserver);
        }

        Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        Utils.refreshAlarm(Screensaver.this, mContentView);

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
        unregisterReceiver(mAlarmChangedReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LOGGER.v("Screensaver configuration changed");
        super.onConfigurationChanged(newConfig);

        // Ignore the configuration change if no window exists.
        if (getWindow() != null) {
            // Restart the position updater via a PreDrawListener after layout is complete.
            startPositionUpdater();
        }
    }

    private void setClockStyle() {
        Utils.setScreensaverClockStyle(mDigitalClock, mAnalogClock);
        final boolean dimNightMode = DataModel.getDataModel().getScreensaverNightModeOn();
        Utils.dimClockView(dimNightMode, mSaverView);
        setScreenBright(!dimNightMode);
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
         * select a random position for {@link #mSaverView} and schedule future callbacks to update
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
