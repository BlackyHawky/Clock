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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.View;
import android.widget.TextClock;

import com.android.deskclock.Utils.ScreensaverMoveSaverRunnable;

public class Screensaver extends DreamService {
    static final boolean DEBUG = false;
    static final String TAG = "DeskClock/Screensaver";

    private View mContentView, mSaverView;
    private View mAnalogClock, mDigitalClock;
    private String mDateFormat;
    private String mDateFormatForAccessibility;

    private final Handler mHandler = new Handler();

    private final ScreensaverMoveSaverRunnable mMoveSaverRunnable;

    private final ContentObserver mSettingsContentObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            Utils.refreshAlarm(Screensaver.this, mContentView);
        }
    };

    // Thread that runs every midnight and refreshes the date.
    private final Runnable mMidnightUpdater = new Runnable() {
        @Override
        public void run() {
            Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
            Utils.setMidnightUpdater(mHandler, mMidnightUpdater);
        }
    };

    /**
     * Receiver to handle time reference changes.
     */
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action != null && (action.equals(Intent.ACTION_TIME_CHANGED)
                    || action.equals(Intent.ACTION_TIMEZONE_CHANGED))) {
                Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
                Utils.refreshAlarm(Screensaver.this, mContentView);
                Utils.setMidnightUpdater(mHandler, mMidnightUpdater);
            }
        }
    };

    public Screensaver() {
        if (DEBUG) Log.d(TAG, "Screensaver allocated");
        mMoveSaverRunnable = new ScreensaverMoveSaverRunnable(mHandler);
    }

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Screensaver created");
        super.onCreate();

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (DEBUG) Log.d(TAG, "Screensaver configuration changed");
        super.onConfigurationChanged(newConfig);
        mHandler.removeCallbacks(mMoveSaverRunnable);
        layoutClockSaver();
        mHandler.post(mMoveSaverRunnable);
    }

    @Override
    public void onAttachedToWindow() {
        if (DEBUG) Log.d(TAG, "Screensaver attached to window");
        super.onAttachedToWindow();

        // We want the screen saver to exit upon user interaction.
        setInteractive(false);

        setFullscreen(true);

        layoutClockSaver();

        // Setup handlers for time reference changes and date updates.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mIntentReceiver, filter);
        Utils.setMidnightUpdater(mHandler, mMidnightUpdater);

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED),
                false,
                mSettingsContentObserver);
        mHandler.post(mMoveSaverRunnable);
    }

    @Override
    public void onDetachedFromWindow() {
        if (DEBUG) Log.d(TAG, "Screensaver detached from window");
        super.onDetachedFromWindow();

        mHandler.removeCallbacks(mMoveSaverRunnable);
        getContentResolver().unregisterContentObserver(mSettingsContentObserver);

        // Tear down handlers for time reference changes and date updates.
        Utils.cancelMidnightUpdater(mHandler, mMidnightUpdater);
        unregisterReceiver(mIntentReceiver);
    }

    private void setClockStyle() {
        Utils.setClockStyle(this, mDigitalClock, mAnalogClock,
                ScreensaverSettingsActivity.KEY_CLOCK_STYLE);
        mSaverView = findViewById(R.id.main_clock);
        boolean dimNightMode = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(ScreensaverSettingsActivity.KEY_NIGHT_MODE, false);
        Utils.dimClockView(dimNightMode, mSaverView);
        setScreenBright(!dimNightMode);
    }

    private void layoutClockSaver() {
        setContentView(R.layout.desk_clock_saver);
        mDigitalClock = findViewById(R.id.digital_clock);
        mAnalogClock =findViewById(R.id.analog_clock);
        setClockStyle();
        Utils.setTimeFormat((TextClock)mDigitalClock,
            (int)getResources().getDimension(R.dimen.bottom_text_size));

        mContentView = (View) mSaverView.getParent();
        mSaverView.setAlpha(0);

        mMoveSaverRunnable.registerViews(mContentView, mSaverView);

        Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        Utils.refreshAlarm(Screensaver.this, mContentView);
    }
}
