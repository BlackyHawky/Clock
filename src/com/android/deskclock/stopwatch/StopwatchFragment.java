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

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Lap;
import com.android.deskclock.data.Stopwatch;
import com.android.deskclock.data.StopwatchListener;
import com.android.deskclock.events.Events;
import com.android.deskclock.timer.CountingTimerView;
import com.android.deskclock.uidata.TabListener;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.uidata.UiDataModel.Tab;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.android.deskclock.FabContainer.UpdateType.FAB_AND_BUTTONS_IMMEDIATE;
import static com.android.deskclock.uidata.UiDataModel.Tab.STOPWATCH;

/**
 * Fragment that shows the stopwatch and recorded laps.
 */
public final class StopwatchFragment extends DeskClockFragment {

    /** Keep the screen on when this tab is selected. */
    private final TabListener mTabWatcher = new TabWatcher();

    /** Scheduled to update the stopwatch time and current lap time while stopwatch is running. */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /** Updates the user interface in response to stopwatch changes. */
    private final StopwatchListener mStopwatchWatcher = new StopwatchWatcher();

    /** Used to determine when talk back is on in order to lower the time update rate. */
    private AccessibilityManager mAccessibilityManager;

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

    /** The public no-arg constructor required by all fragments. */
    public StopwatchFragment() {
        super(STOPWATCH);
    }

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

        DataModel.getDataModel().addStopwatchListener(mStopwatchWatcher);

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

        // Synchronize the user interface with the data model.
        updateUI();

        // Start watching for page changes away from this fragment.
        UiDataModel.getUiDataModel().addTabListener(mTabWatcher);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop all updates while the fragment is not visible.
        stopUpdatingTime();
        mTimeText.blinkTimeStr(false);

        // Stop watching for page changes away from this fragment.
        UiDataModel.getUiDataModel().removeTabListener(mTabWatcher);

        // Release the wake lock if it is currently held.
        releaseWakeLock();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        DataModel.getDataModel().removeStopwatchListener(mStopwatchWatcher);
    }

    @Override
    public void onFabClick(@NonNull ImageView fab) {
        toggleStopwatchState();
    }

    @Override
    public void onLeftButtonClick(@NonNull ImageButton left) {
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
    public void onRightButtonClick(@NonNull ImageButton right) {
        doShare();
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        if (getStopwatch().isRunning()) {
            fab.setImageResource(R.drawable.ic_pause_white_24dp);
            fab.setContentDescription(fab.getResources().getString(R.string.sw_pause_button));
        } else {
            fab.setImageResource(R.drawable.ic_start_white_24dp);
            fab.setContentDescription(fab.getResources().getString(R.string.sw_start_button));
        }
        fab.setVisibility(VISIBLE);
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageButton left, @NonNull ImageButton right) {
        right.setImageResource(R.drawable.ic_share);
        right.setContentDescription(right.getResources().getString(R.string.sw_share_button));

        switch (getStopwatch().getState()) {
            case RESET:
                left.setEnabled(false);
                left.setVisibility(INVISIBLE);
                right.setVisibility(INVISIBLE);
                break;
            case RUNNING:
                left.setImageResource(R.drawable.ic_lap);
                left.setContentDescription(left.getResources().getString(R.string.sw_lap_button));
                left.setEnabled(canRecordMoreLaps());
                left.setVisibility(canRecordMoreLaps() ? VISIBLE : INVISIBLE);
                right.setVisibility(INVISIBLE);
                break;
            case PAUSED:
                left.setEnabled(true);
                left.setImageResource(R.drawable.ic_reset);
                left.setContentDescription(left.getResources().getString(R.string.sw_reset_button));
                left.setVisibility(VISIBLE);
                right.setVisibility(VISIBLE);
                break;
        }
    }

    /**
     * Start the stopwatch.
     */
    private void doStart() {
        Events.sendStopwatchEvent(R.string.action_start, R.string.label_deskclock);
        DataModel.getDataModel().startStopwatch();
    }

    /**
     * Pause the stopwatch.
     */
    private void doPause() {
        Events.sendStopwatchEvent(R.string.action_pause, R.string.label_deskclock);
        DataModel.getDataModel().pauseStopwatch();
    }

    /**
     * Reset the stopwatch.
     */
    private void doReset() {
        Events.sendStopwatchEvent(R.string.action_reset, R.string.label_deskclock);
        DataModel.getDataModel().resetStopwatch();
    }

    /**
     * Send stopwatch time and lap times to an external sharing application.
     */
    private void doShare() {
        final String[] subjects = getResources().getStringArray(R.array.sw_share_strings);
        final String subject = subjects[(int) (Math.random() * subjects.length)];
        final String text = mLapsAdapter.getShareText();

        @SuppressLint("InlinedApi")
        @SuppressWarnings("deprecation")
        final Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .addFlags(Utils.isLOrLater() ? Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
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
        updateFab(FAB_AND_BUTTONS_IMMEDIATE);

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
        final ViewGroup sceneRoot = (ViewGroup) getView();
        TransitionManager.beginDelayedTransition(sceneRoot);

        if (clearLaps) {
            mLapsAdapter.clearLaps();
        }

        final boolean lapsVisible = mLapsAdapter.getItemCount() > 0;
        mLapsList.setVisibility(lapsVisible ? VISIBLE : GONE);
    }

    private void adjustWakeLock() {
        final boolean appInForeground = DataModel.getDataModel().isApplicationInForeground();
        if (getStopwatch().isRunning() && isTabSelected() && appInForeground) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            releaseWakeLock();
        }
    }

    private void releaseWakeLock() {
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        final Stopwatch stopwatch = getStopwatch();
        final long totalTime = stopwatch.getTotalTime();

        // Update the total time display.
        mTimeText.setTime(totalTime, true, true);

        // Update the current lap.
        final boolean currentLapIsVisible = mLapsLayoutManager.findFirstVisibleItemPosition() == 0;
        if (!stopwatch.isReset() && currentLapIsVisible) {
            mLapsAdapter.updateCurrentLap(mLapsList, totalTime);
        }
    }

    /**
     * Synchronize the UI state with the model data.
     */
    private void updateUI() {
        adjustWakeLock();

        // Draw the latest stopwatch and current lap times.
        updateTime();

        mTime.update();

        // Start updates if the stopwatch is running.
        final Stopwatch stopwatch = getStopwatch();
        if (stopwatch.isRunning()) {
            startUpdatingTime();
        }

        // Blink text iff the stopwatch is paused.
        mTimeText.blinkTimeStr(stopwatch.isPaused());

        // Adjust the visibility of the list of laps.
        showOrHideLaps(stopwatch.isReset());

        // Update button states.
        updateFab(FAB_AND_BUTTONS_IMMEDIATE);
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

    /**
     * Acquire or release the wake lock based on the tab state.
     */
    private final class TabWatcher implements TabListener {
        @Override
        public void selectedTabChanged(Tab oldSelectedTab, Tab newSelectedTab) {
            adjustWakeLock();
        }
    }

    /**
     * Update the user interface in response to a stopwatch change.
     */
    private class StopwatchWatcher implements StopwatchListener {
        @Override
        public void stopwatchUpdated(Stopwatch before, Stopwatch after) {
            if (DataModel.getDataModel().isApplicationInForeground()) {
                updateUI();
            }
        }

        @Override
        public void lapAdded(Lap lap) {}
    }
}
