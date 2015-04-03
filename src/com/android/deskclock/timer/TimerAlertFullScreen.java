/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.deskclock.timer;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.android.deskclock.BaseActivity;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.timer.TimerFullScreenFragment.OnEmptyListListener;

/**
 * Timer alarm alert: pops visible indicator. This activity is the version which
 * shows over the lock screen.
 * This activity re-uses TimerFullScreenFragment GUI
 */
public class TimerAlertFullScreen extends BaseActivity implements OnEmptyListListener {

    private static final String TAG = "TimerAlertFullScreen";
    private static final String FRAGMENT = "timer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.timer_alert_full_screen);
        final View view = findViewById(R.id.fragment_container);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        // Turn on the screen unless we are being launched from the AlarmAlert
        // subclass as a result of the screen turning off.
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        // Don't create overlapping fragments.
        if (getFragment() == null) {
            TimerFullScreenFragment timerFragment = new TimerFullScreenFragment();

            // Create fragment and give it an argument to only show
            // timers in STATE_TIMESUP state
            Bundle args = new Bundle();
            args.putBoolean(Timers.TIMESUP_MODE, true);

            timerFragment.setArguments(args);

            // Add the fragment to the 'fragment_container' FrameLayout
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, timerFragment, FRAGMENT).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Only show notifications for times-up when this activity closed.
        Utils.cancelTimesUpNotifications(this);
    }

    @Override
    public void onPause() {
        Utils.showTimesUpNotifications(this);

        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Handle key down and key up on a few of the system keys.
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        switch (event.getKeyCode()) {
        // Volume keys and camera keys stop all the timers
        case KeyEvent.KEYCODE_VOLUME_UP:
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_MUTE:
        case KeyEvent.KEYCODE_CAMERA:
        case KeyEvent.KEYCODE_FOCUS:
            if (up) {
                stopAllTimesUpTimers();
            }
            return true;

        default:
            break;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * this is called when a second timer is triggered while a previous alert
     * window is still active.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        TimerFullScreenFragment timerFragment = getFragment();
        if (timerFragment != null) {
            timerFragment.restartAdapter();
        }
        super.onNewIntent(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        ViewGroup viewContainer = (ViewGroup)findViewById(R.id.fragment_container);
        viewContainer.requestLayout();
        super.onConfigurationChanged(newConfig);
    }

    protected void stopAllTimesUpTimers() {
        TimerFullScreenFragment timerFragment = getFragment();
        if (timerFragment != null) {
            timerFragment.updateAllTimesUpTimers();
        }
    }

    @Override
    public void onEmptyList() {
        if (Timers.LOGGING) {
            Log.v(TAG, "onEmptyList");
        }
        onListChanged();
        finish();
    }

    @Override
    public void onListChanged() {
        Utils.showInUseNotifications(this);
    }

    private TimerFullScreenFragment getFragment() {
        return (TimerFullScreenFragment) getFragmentManager().findFragmentByTag(FRAGMENT);
    }
}
