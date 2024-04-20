/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.best.deskclock.LogUtils;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerListener;

import java.util.List;

/**
 * This activity is designed to be shown over the lock screen. As such, it displays the expired
 * timers and a single button to reset them all. Each expired timer can also be reset to one minute
 * with a button in the user interface. All other timer operations are disabled in this activity.
 */
public class ExpiredTimersActivity extends AppCompatActivity {

    /**
     * Scheduled to update the timers while at least one is expired.
     */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /**
     * Updates the timers displayed in this activity as the backing data changes.
     */
    private final TimerListener mTimerChangeWatcher = new TimerChangeWatcher();

    /**
     * The scene root for transitions when expired timers are added/removed from this container.
     */
    private ViewGroup mExpiredTimersScrollView;

    /**
     * Displays the expired timers.
     */
    private ViewGroup mExpiredTimersView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final List<Timer> expiredTimers = getExpiredTimers();

        // If no expired timers, finish
        if (expiredTimers.size() == 0) {
            LogUtils.i("No expired timers, skipping display.");
            finish();
            return;
        }

        setContentView(R.layout.expired_timers_activity);

        mExpiredTimersView = findViewById(R.id.expired_timers_list);
        mExpiredTimersScrollView = findViewById(R.id.expired_timers_scroll);
        mExpiredTimersScrollView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);


        // Honor rotation on tablets; fix the orientation on phones.
        if (!Utils.isLandscape(getApplicationContext())) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        // Create views for each of the expired timers.
        for (Timer timer : expiredTimers) {
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
                case KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
                        KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_FOCUS -> {
                    DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_hardware_button);
                    return true;
                }
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

        final int timerId = timer.getId();
        final TimerItem timerItem = (TimerItem)
                getLayoutInflater().inflate(R.layout.timer_item, mExpiredTimersView, false);
        // Store the timer id as a tag on the view so it can be located on delete.
        timerItem.setId(timerId);
        timerItem.setBackground(Utils.cardBackground(timerItem.getContext()));
        mExpiredTimersView.addView(timerItem);

        // Hide the label hint for expired timers.
        final TextView labelView = timerItem.findViewById(R.id.timer_label);
        labelView.setHint(null);
        labelView.setVisibility(TextUtils.isEmpty(timer.getLabel()) ? View.GONE : View.VISIBLE);

        // Add logic to the "Add 1 Minute" button.
        final View addMinuteButton = timerItem.findViewById(R.id.add_one_min);
        addMinuteButton.setOnClickListener(v -> {
            final Timer timer12 = DataModel.getDataModel().getTimer(timerId);
            DataModel.getDataModel().addTimerMinute(timer12);
        });

        // Add logic to hide the 'X' and reset button
        final View closeButton = timerItem.findViewById(R.id.close);
        closeButton.setVisibility(View.GONE);
        final View resetButton = timerItem.findViewById(R.id.reset);
        resetButton.setVisibility(View.GONE);

        // Add logic to the "Stop" button
        final View stopButton = timerItem.findViewById(R.id.play_pause);
        stopButton.setOnClickListener(v -> {
            final Timer timer1 = DataModel.getDataModel().getTimer(timerId);
            DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_deskclock);
            removeTimer(timer1);
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

        final int timerId = timer.getId();
        final int count = mExpiredTimersView.getChildCount();
        for (int i = 0; i < count; ++i) {
            final View timerView = mExpiredTimersView.getChildAt(i);
            if (timerView.getId() == timerId) {
                mExpiredTimersView.removeView(timerView);
                break;
            }
        }

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

            final int count = mExpiredTimersView.getChildCount();
            for (int i = 0; i < count; ++i) {
                final TimerItem timerItem = (TimerItem) mExpiredTimersView.getChildAt(i);
                final Timer timer = DataModel.getDataModel().getTimer(timerItem.getId());
                if (timer != null) {
                    timerItem.update(timer);
                }
            }

            final long endTime = SystemClock.elapsedRealtime();

            // Try to maintain a consistent period of time between redraws.
            final long delay = Math.max(0L, startTime + 100L - endTime);
            mExpiredTimersView.postDelayed(this, delay);
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
