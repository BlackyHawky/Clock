/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.stopwatch;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Lap;
import com.android.deskclock.data.Stopwatch;
import com.android.deskclock.events.Events;
import com.android.deskclock.timer.CountingTimerView;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.ON_AFTER_RELEASE;
import static android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * Fragment that shows the stopwatch and recorded laps.
 */
public final class StopwatchFragment extends DeskClockFragment {

    private static final String TAG = "StopwatchFragment";

    /** Scheduled to update the stopwatch time and current lap time while stopwatch is running. */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /** Used to determine when talk back is on in order to lower the time update rate. */
    private AccessibilityManager mAccessibilityManager;

    /** {@code true} while the {@link #mLapsList} is transitioning between shown and hidden. */
    private boolean mLapsListIsTransitioning;

    /** The data source for {@link #mLapsList}. */
    private LapsAdapter mLapsAdapter;

    /** The layout manager for the {@link #mLapsAdapter}. */
    private LinearLayoutManager mLapsLayoutManager;

    /** Draws the reference lap while the stopwatch is running. */
    private StopwatchCircleView mTime;

    /** Displays the recorded lap times. */
    private RecyclerView mLapsList;

    /** Displays the current stopwatch time. */
    private CountingTimerView mTimeText;

    /** Held while the stopwatch is running and this fragment is forward to keep the screen on. */
    private PowerManager.WakeLock mWakeLock;

    /** The public no-arg constructor required by all fragments. */
    public StopwatchFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        mLapsAdapter = new LapsAdapter(getActivity());
        mLapsLayoutManager = new LinearLayoutManager(getActivity());

        final View v = inflater.inflate(R.layout.stopwatch_fragment, container, false);
        mTime = (StopwatchCircleView) v.findViewById(R.id.stopwatch_time);
        mLapsList = (RecyclerView) v.findViewById(R.id.laps_list);
        ((SimpleItemAnimator) mLapsList.getItemAnimator()).setSupportsChangeAnimations(false);
        mLapsList.setLayoutManager(mLapsLayoutManager);
        mLapsList.setAdapter(mLapsAdapter);

        // Timer text serves as a virtual start/stop button.
        mTimeText = (CountingTimerView) v.findViewById(R.id.stopwatch_time_text);
        mTimeText.setVirtualButtonEnabled(true);
        mTimeText.registerVirtualButtonAction(new ToggleStopwatchRunnable());

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAccessibilityManager =
                (AccessibilityManager) getActivity().getSystemService(ACCESSIBILITY_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Conservatively assume the data in the adapter has changed while the fragment was paused.
        mLapsAdapter.notifyDataSetChanged();

        // Update the state of the buttons.
        setFabAppearance();
        setLeftRightButtonAppearance();

        // Draw the current stopwatch and lap times.
        updateTime();

        // Start updates if the stopwatch is running; blink text if it is paused.
        switch (getStopwatch().getState()) {
            case RUNNING:
                acquireWakeLock();
                mTime.update();
                startUpdatingTime();
                break;
            case PAUSED:
                mTimeText.blinkTimeStr(true);
                break;
        }

        // Adjust the visibility of the list of laps.
        showOrHideLaps(false);

        // Start watching for page changes away from this fragment.
        getDeskClock().registerPageChangedListener(this);

        // View is hidden in onPause, make sure it is visible now.
        final View view = getView();
        if (view != null) {
            view.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        final View view = getView();
        if (view != null) {
            // Make the view invisible because when the lock screen is activated, the window stays
            // active under it. Later, when unlocking the screen, we see the old stopwatch time for
            // a fraction of a second.
            getView().setVisibility(INVISIBLE);
        }

        // Stop all updates while the fragment is not visible.
        stopUpdatingTime();
        mTimeText.blinkTimeStr(false);

        // Stop watching for page changes away from this fragment.
        getDeskClock().unregisterPageChangedListener(this);

        // Release the wake lock if it is currently held.
        releaseWakeLock();
    }

    @Override
    public void onPageChanged(int page) {
        if (page == DeskClock.STOPWATCH_TAB_INDEX && getStopwatch().isRunning()) {
            acquireWakeLock();
        } else {
            releaseWakeLock();
        }
    }

    @Override
    public void onFabClick(View view) {
        toggleStopwatchState();
    }

    @Override
    public void onLeftButtonClick(View view) {
        switch (getStopwatch().getState()) {
            case RUNNING:
                doAddLap();
                break;
            case PAUSED:
                doReset();
                break;
        }
    }

    @Override
    public void onRightButtonClick(View view) {
        doShare();
    }

    @Override
    public void setFabAppearance() {
        if (mFab == null || getSelectedTab() != DeskClock.STOPWATCH_TAB_INDEX) {
            return;
        }

        if (getStopwatch().isRunning()) {
            mFab.setImageResource(R.drawable.ic_pause_white_24dp);
            mFab.setContentDescription(getString(R.string.sw_pause_button));
        } else {
            mFab.setImageResource(R.drawable.ic_start_white_24dp);
            mFab.setContentDescription(getString(R.string.sw_start_button));
        }
        mFab.setVisibility(VISIBLE);
    }

    @Override
    public void setLeftRightButtonAppearance() {
        if (mLeftButton == null || mRightButton == null ||
                getSelectedTab() != DeskClock.STOPWATCH_TAB_INDEX) {
            return;
        }

        mRightButton.setImageResource(R.drawable.ic_share);
        mRightButton.setContentDescription(getString(R.string.sw_share_button));

        switch (getStopwatch().getState()) {
            case RESET:
                mLeftButton.setEnabled(false);
                mLeftButton.setVisibility(INVISIBLE);
                mRightButton.setVisibility(INVISIBLE);
                break;
            case RUNNING:
                mLeftButton.setImageResource(R.drawable.ic_lap);
                mLeftButton.setContentDescription(getString(R.string.sw_lap_button));
                mLeftButton.setEnabled(canRecordMoreLaps());
                mLeftButton.setVisibility(canRecordMoreLaps() ? VISIBLE : INVISIBLE);
                mRightButton.setVisibility(INVISIBLE);
                break;
            case PAUSED:
                mLeftButton.setEnabled(true);
                mLeftButton.setImageResource(R.drawable.ic_reset);
                mLeftButton.setContentDescription(getString(R.string.sw_reset_button));
                mLeftButton.setVisibility(VISIBLE);
                mRightButton.setVisibility(VISIBLE);
                break;
        }
    }

    /**
     * Start the stopwatch.
     */
    private void doStart() {
        Events.sendStopwatchEvent(R.string.action_start, R.string.label_deskclock);

        // Update the stopwatch state.
        DataModel.getDataModel().startStopwatch();

        // Start UI updates.
        startUpdatingTime();
        mTime.update();
        mTimeText.blinkTimeStr(false);

        // Update button states.
        setFabAppearance();
        setLeftRightButtonAppearance();

        // Acquire the wake lock.
        acquireWakeLock();
    }

    /**
     * Pause the stopwatch.
     */
    private void doPause() {
        Events.sendStopwatchEvent(R.string.action_pause, R.string.label_deskclock);

        // Update the stopwatch state
        DataModel.getDataModel().pauseStopwatch();

        // Redraw the paused stopwatch time.
        updateTime();

        // Stop UI updates.
        stopUpdatingTime();
        mTimeText.blinkTimeStr(true);

        // Update button states.
        setFabAppearance();
        setLeftRightButtonAppearance();

        // Release the wake lock.
        releaseWakeLock();
    }

    /**
     * Reset the stopwatch.
     */
    private void doReset() {
        Events.sendStopwatchEvent(R.string.action_reset, R.string.label_deskclock);

        // Update the stopwatch state.
        DataModel.getDataModel().resetStopwatch();

        // Clear the laps.
        showOrHideLaps(true);

        // Clear the times.
        mTime.postInvalidateOnAnimation();
        mTimeText.setTime(0, true, true);
        mTimeText.blinkTimeStr(false);

        // Update button states.
        setFabAppearance();
        setLeftRightButtonAppearance();

        // Release the wake lock.
        releaseWakeLock();
    }

    /**
     * Send stopwatch time and lap times to an external sharing application.
     */
    private void doShare() {
        final String[] subjects = getResources().getStringArray(R.array.sw_share_strings);
        final String subject = subjects[(int)(Math.random() * subjects.length)];
        final String text = mLapsAdapter.getShareText();

        final Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, text)
                .setType("text/plain");

        final Context context = getActivity();
        final String title = context.getString(R.string.sw_share_button);
        final Intent shareChooserIntent = Intent.createChooser(shareIntent, title);
        try {
            context.startActivity(shareChooserIntent);
        } catch (ActivityNotFoundException anfe) {
            LogUtils.e("No compatible receiver is found");
        }
    }

    /**
     * Record and add a new lap ending now.
     */
    private void doAddLap() {
        Events.sendStopwatchEvent(R.string.action_lap, R.string.label_deskclock);

        // Record a new lap.
        final Lap lap = mLapsAdapter.addLap();
        if (lap == null) {
            return;
        }

        // Update button states.
        setLeftRightButtonAppearance();

        if (lap.getLapNumber() == 1) {
            // Child views from prior lap sets hang around and blit to the screen when adding the
            // first lap of the subsequent lap set. Remove those superfluous children here manually
            // to ensure they aren't seen as the first lap is drawn.
            mLapsList.removeAllViewsInLayout();

            // Start animating the reference lap.
            mTime.update();

            // Recording the first lap transitions the UI to display the laps list.
            showOrHideLaps(false);
        }

        // Ensure the newly added lap is visible on screen.
        mLapsList.scrollToPosition(0);
    }

    /**
     * Show or hide the list of laps.
     */
    private void showOrHideLaps(boolean clearLaps) {
        final Transition transition = new AutoTransition()
                .addListener(new Transition.TransitionListener() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        mLapsListIsTransitioning = true;
                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        mLapsListIsTransitioning = false;
                    }

                    @Override
                    public void onTransitionCancel(Transition transition) {
                    }

                    @Override
                    public void onTransitionPause(Transition transition) {
                    }

                    @Override
                    public void onTransitionResume(Transition transition) {
                    }
                });

        final ViewGroup sceneRoot = (ViewGroup) getView();
        TransitionManager.beginDelayedTransition(sceneRoot, transition);

        if (clearLaps) {
            mLapsAdapter.clearLaps();
        }

        final boolean lapsVisible = mLapsAdapter.getItemCount() > 0;
        mLapsList.setVisibility(lapsVisible ? VISIBLE : GONE);
    }

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            final PowerManager pm = (PowerManager) getActivity().getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(SCREEN_BRIGHT_WAKE_LOCK | ON_AFTER_RELEASE, TAG);
            mWakeLock.setReferenceCounted(false);
        }
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    /**
     * Either pause or start the stopwatch based on its current state.
     */
    private void toggleStopwatchState() {
        if (getStopwatch().isRunning()) {
            doPause();
        } else {
            doStart();
        }
    }

    private Stopwatch getStopwatch() {
        return DataModel.getDataModel().getStopwatch();
    }

    private boolean canRecordMoreLaps() {
        return DataModel.getDataModel().canAddMoreLaps();
    }

    /**
     * Post the first runnable to update times within the UI. It will reschedule itself as needed.
     */
    private void startUpdatingTime() {
        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
        mTime.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    private void stopUpdatingTime() {
        mTime.removeCallbacks(mTimeUpdateRunnable);
    }

    /**
     * Update all time displays based on a single snapshot of the stopwatch progress. This includes
     * the stopwatch time drawn in the circle, the current lap time and the total elapsed time in
     * the list of laps.
     */
    private void updateTime() {
        // Compute the total time of the stopwatch.
        final long totalTime = getStopwatch().getTotalTime();

        // Update the total time display.
        mTimeText.setTime(totalTime, true, true);

        // Update the current lap.
        final boolean currentLapIsVisible = mLapsLayoutManager.findFirstVisibleItemPosition() == 0;
        if (!mLapsListIsTransitioning && currentLapIsVisible) {
            mLapsAdapter.updateCurrentLap(mLapsList, totalTime);
        }
    }

    /**
     * This runnable periodically updates times throughout the UI. It stops these updates when the
     * stopwatch is no longer running.
     */
    private final class TimeUpdateRunnable implements Runnable {
        @Override
        public void run() {
            final long startTime = SystemClock.elapsedRealtime();

            updateTime();

            if (getStopwatch().isRunning()) {
                // The stopwatch is still running so execute this runnable again after a delay.
                final boolean talkBackOn = mAccessibilityManager.isTouchExplorationEnabled();

                // Grant longer time between redraws when talk-back is on to let it catch up.
                final int period = talkBackOn ? 500 : 25;

                // Try to maintain a consistent period of time between redraws.
                final long endTime = SystemClock.elapsedRealtime();
                final long delay = Math.max(0, startTime + period - endTime);

                mTime.postDelayed(this, delay);
            }
        }
    }

    /**
     * Tapping the stopwatch text also toggles the stopwatch state, just like the fab.
     */
    private final class ToggleStopwatchRunnable implements Runnable {
        @Override
        public void run() {
            toggleStopwatchState();
        }
    }
}
