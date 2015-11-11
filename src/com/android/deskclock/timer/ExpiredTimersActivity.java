/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.deskclock.BaseActivity;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.data.TimerListener;

import java.util.List;

/**
 * This activity is designed to be shown over the lock screen. As such, it displays the expired
 * timers and a single button to reset them all. Each expired timer can also be reset to one minute
 * with a button in the user interface. All other timer operations are disabled in this activity.
 */
public class ExpiredTimersActivity extends BaseActivity {

    /** Scheduled to update the timers while at least one is expired. */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /** Updates the timers displayed in this activity as the backing data changes. */
    private final TimerListener mTimerChangeWatcher = new TimerChangeWatcher();

    /** The scene root for transitions when expired timers are added/removed from this container. */
    private ViewGroup mExpiredTimersScrollView;

    /** Displays the expired timers. */
    private ViewGroup mExpiredTimersView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.expired_timers_activity);

        mExpiredTimersView = (ViewGroup) findViewById(R.id.expired_timers_list);
        mExpiredTimersScrollView = (ViewGroup) findViewById(R.id.expired_timers_scroll);

        findViewById(R.id.fab).setOnClickListener(new FabClickListener());

        final View view = findViewById(R.id.expired_timers_activity);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        // Create views for each of the expired timers.
        for (Timer timer : getExpiredTimers()) {
            addTimer(timer);
        }

        // Update views in response to timer data changes.
        DataModel.getDataModel().addTimerListener(mTimerChangeWatcher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUpdatingTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdatingTime();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DataModel.getDataModel().removeTimerListener(mTimerChangeWatcher);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                case KeyEvent.KEYCODE_VOLUME_MUTE:
                case KeyEvent.KEYCODE_CAMERA:
                case KeyEvent.KEYCODE_FOCUS:
                    DataModel.getDataModel().resetExpiredTimers(R.string.label_hardware_button);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * Post the first runnable to update times within the UI. It will reschedule itself as needed.
     */
    private void startUpdatingTime() {
        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
        mExpiredTimersView.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    private void stopUpdatingTime() {
        mExpiredTimersView.removeCallbacks(mTimeUpdateRunnable);
    }

    /**
     * Create and add a new view that corresponds with the given {@code timer}.
     */
    private void addTimer(Timer timer) {
        TransitionManager.beginDelayedTransition(mExpiredTimersScrollView, new AutoTransition());

        final TimerItem timerItem = (TimerItem)
                getLayoutInflater().inflate(R.layout.timer_item, mExpiredTimersView, false);
        // Store the timer id as a tag on the view so it can be located on delete.
        timerItem.setTag(timer.getId());
        mExpiredTimersView.addView(timerItem);

        // Hide the label hint for expired timers.
        final TextView labelView = (TextView) timerItem.findViewById(R.id.timer_label);
        labelView.setHint(null);

        // Add logic to the "Add 1 Minute" button.
        final View addMinuteButton = timerItem.findViewById(R.id.reset_add);
        addMinuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int index = mExpiredTimersView.indexOfChild(timerItem);
                final Timer timer = getExpiredTimers().get(index);
                DataModel.getDataModel().addTimerMinute(timer);
            }
        });

        // If the first timer was just added, center it.
        final List<Timer> expiredTimers = getExpiredTimers();
        if (expiredTimers.size() == 1) {
            centerFirstTimer();
        } else if (expiredTimers.size() == 2) {
            uncenterFirstTimer();
        }
    }

    /**
     * Remove an existing view that corresponds with the given {@code timer}.
     */
    private void removeTimer(Timer timer) {
        TransitionManager.beginDelayedTransition(mExpiredTimersScrollView, new AutoTransition());

        final View timerView = mExpiredTimersView.findViewWithTag(timer.getId());
        mExpiredTimersView.removeView(timerView);

        // If the second last timer was just removed, center the last timer.
        final List<Timer> expiredTimers = getExpiredTimers();
        if (expiredTimers.isEmpty()) {
            finish();
        } else if (expiredTimers.size() == 1) {
            centerFirstTimer();
        }
    }

    /**
     * Center the single timer.
     */
    private void centerFirstTimer() {
        final FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) mExpiredTimersView.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        mExpiredTimersView.requestLayout();
    }

    /**
     * Display the multiple timers as a scrollable list.
     */
    private void uncenterFirstTimer() {
        final FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) mExpiredTimersView.getLayoutParams();
        lp.gravity = Gravity.NO_GRAVITY;
        mExpiredTimersView.requestLayout();
    }

    private List<Timer> getExpiredTimers() {
        return DataModel.getDataModel().getExpiredTimers();
    }

    /**
     * Periodically refreshes the state of each timer.
     */
    private class TimeUpdateRunnable implements Runnable {
        @Override
        public void run() {
            final long startTime = SystemClock.elapsedRealtime();

            for (int i = 0; i < mExpiredTimersView.getChildCount(); i++) {
                final TimerItem timerItem = (TimerItem) mExpiredTimersView.getChildAt(i);
                final Timer timer = getExpiredTimers().get(i);
                timerItem.update(timer);
            }

            final long endTime = SystemClock.elapsedRealtime();

            // Try to maintain a consistent period of time between redraws.
            final long delay = Math.max(0, startTime + 20 - endTime);
            mExpiredTimersView.postDelayed(this, delay);
        }
    }

    /**
     * Clicking the fab resets all expired timers.
     */
    private class FabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            stopUpdatingTime();
            DataModel.getDataModel().removeTimerListener(mTimerChangeWatcher);
            DataModel.getDataModel().resetExpiredTimers(R.string.label_deskclock);
            finish();
        }
    }

    /**
     * Adds and removes expired timers from this activity based on their state changes.
     */
    private class TimerChangeWatcher implements TimerListener {
        @Override
        public void timerAdded(Timer timer) {
            if (timer.isExpired()) {
                addTimer(timer);
            }
        }

        @Override
        public void timerUpdated(Timer before, Timer after) {
            if (!before.isExpired() && after.isExpired()) {
                addTimer(after);
            } else if (before.isExpired() && !after.isExpired()) {
                removeTimer(before);
            }
        }

        @Override
        public void timerRemoved(Timer timer) {
            if (timer.isExpired()) {
                removeTimer(timer);
            }
        }
    }
}