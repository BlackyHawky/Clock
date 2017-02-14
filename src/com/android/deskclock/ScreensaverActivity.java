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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextClock;

import com.android.deskclock.events.Events;
import com.android.deskclock.uidata.UiDataModel;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.os.BatteryManager.EXTRA_PLUGGED;

public class ScreensaverActivity extends BaseActivity {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("ScreensaverActivity");

    /** These flags keep the screen on if the device is plugged in. */
    private static final int WINDOW_FLAGS = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

    private final OnPreDrawListener mStartPositionUpdater = new StartPositionUpdater();

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LOGGER.v("ScreensaverActivity onReceive, action: " + intent.getAction());

            switch (intent.getAction()) {
                case Intent.ACTION_POWER_CONNECTED:
                    updateWakeLock(true);
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    updateWakeLock(false);
                    break;
                case Intent.ACTION_USER_PRESENT:
                    finish();
                    break;
                case AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED:
                    Utils.refreshAlarm(ScreensaverActivity.this, mContentView);
                    break;
            }
        }
    };

    /* Register ContentObserver to see alarm changes for pre-L */
    private final ContentObserver mSettingsContentObserver = Utils.isPreL()
        ? new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                Utils.refreshAlarm(ScreensaverActivity.this, mContentView);
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

    private String mDateFormat;
    private String mDateFormatForAccessibility;

    private View mContentView;
    private View mMainClockView;

    private MoveScreensaverRunnable mPositionUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        setContentView(R.layout.desk_clock_saver);
        mContentView = findViewById(R.id.saver_container);
        mMainClockView = mContentView.findViewById(R.id.main_clock);

        final View digitalClock = mMainClockView.findViewById(R.id.digital_clock);
        final AnalogClock analogClock =
                (AnalogClock) mMainClockView.findViewById(R.id.analog_clock);

        Utils.setClockIconTypeface(mMainClockView);
        Utils.setTimeFormat((TextClock) digitalClock, false);
        Utils.setClockStyle(digitalClock, analogClock);
        Utils.dimClockView(true, mMainClockView);
        analogClock.enableSeconds(false);

        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        mContentView.setOnSystemUiVisibilityChangeListener(new InteractionListener());

        mPositionUpdater = new MoveScreensaverRunnable(mContentView, mMainClockView);

        final Intent intent = getIntent();
        if (intent != null) {
            final int eventLabel = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, 0);
            Events.sendScreensaverEvent(R.string.action_show, eventLabel);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        if (Utils.isLOrLater()) {
            filter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
        }
        registerReceiver(mIntentReceiver, filter);

        if (mSettingsContentObserver != null) {
            @SuppressWarnings("deprecation")
            final Uri uri = Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED);
            getContentResolver().registerContentObserver(uri, false, mSettingsContentObserver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        Utils.refreshAlarm(ScreensaverActivity.this, mContentView);

        startPositionUpdater();
        UiDataModel.getUiDataModel().addMidnightCallback(mMidnightUpdater, 100);

        final Intent intent = registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
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
        if (mSettingsContentObserver != null) {
            getContentResolver().unregisterContentObserver(mSettingsContentObserver);
        }
        unregisterReceiver(mIntentReceiver);
        super.onStop();
    }

    @Override
    public void onUserInteraction() {
        // We want the screen saver to exit upon user interaction.
        finish();
    }

    /**
     * @param pluggedIn {@code true} iff the device is currently plugged in to a charger
     */
    private void updateWakeLock(boolean pluggedIn) {
        final Window win = getWindow();
        final WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (pluggedIn) {
            winParams.flags |= WINDOW_FLAGS;
        } else {
            winParams.flags &= (~WINDOW_FLAGS);
        }
        win.setAttributes(winParams);
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

    private final class InteractionListener implements View.OnSystemUiVisibilityChangeListener {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            // When the user interacts with the screen, the navigation bar reappears
            if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                // We want the screen saver to exit upon user interaction.
                finish();
            }
        }
    }
}
