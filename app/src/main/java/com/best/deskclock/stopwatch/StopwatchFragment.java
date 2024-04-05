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

package com.best.deskclock.stopwatch;

import static android.R.attr.state_activated;
import static android.R.attr.state_pressed;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.best.deskclock.uidata.UiDataModel.Tab.STOPWATCH;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.best.deskclock.AnimatorUtils;
import com.best.deskclock.DeskClockFragment;
import com.best.deskclock.LogUtils;
import com.best.deskclock.R;
import com.best.deskclock.StopwatchTextController;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Lap;
import com.best.deskclock.data.Stopwatch;
import com.best.deskclock.data.StopwatchListener;
import com.best.deskclock.events.Events;
import com.best.deskclock.uidata.TabListener;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.uidata.UiDataModel.Tab;

import java.util.Objects;

/**
 * Fragment that shows the stopwatch and recorded laps.
 */
public final class StopwatchFragment extends DeskClockFragment {

    /**
     * Milliseconds between redraws while running.
     */
    private static final int REDRAW_PERIOD_RUNNING = 25;

    /**
     * Milliseconds between redraws while paused.
     */
    private static final int REDRAW_PERIOD_PAUSED = 500;

    /**
     * Keep the screen on when this tab is selected.
     */
    private final TabListener mTabWatcher = new TabWatcher();

    /**
     * Scheduled to update the stopwatch time and current lap time while stopwatch is running.
     */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /**
     * Updates the user interface in response to stopwatch changes.
     */
    private final StopwatchListener mStopwatchWatcher = new StopwatchWatcher();

    /**
     * The data source for {@link #mLapsList}.
     */
    private LapsAdapter mLapsAdapter;

    /**
     * The layout manager for the {@link #mLapsAdapter}.
     */
    private LinearLayoutManager mLapsLayoutManager;

    /**
     * Draws the reference lap while the stopwatch is running.
     */
    private StopwatchCircleView mTime;

    /**
     * The View containing both TextViews of the stopwatch.
     */
    private View mStopwatchWrapper;

    /**
     * Displays the recorded lap times.
     */
    private RecyclerView mLapsList;

    /**
     * Displays the current stopwatch time (seconds and above only).
     */
    private TextView mMainTimeText;

    /**
     * Displays the current stopwatch time (hundredths only).
     */
    private TextView mHundredthsTimeText;

    /**
     * Formats and displays the text in the stopwatch.
     */
    private StopwatchTextController mStopwatchTextController;

    /**
     * The public no-arg constructor required by all fragments.
     */
    public StopwatchFragment() {
        super(STOPWATCH);
    }

    private Activity mActivity;
    private Context mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        mLapsAdapter = new LapsAdapter(getContext());
        mLapsLayoutManager = new LinearLayoutManager(getContext());

        mContext = requireContext();

        final View v = inflater.inflate(R.layout.stopwatch_fragment, container, false);
        mTime = v.findViewById(R.id.stopwatch_circle);
        mLapsList = v.findViewById(R.id.laps_list);
        ((SimpleItemAnimator) Objects.requireNonNull(mLapsList.getItemAnimator())).setSupportsChangeAnimations(false);
        mLapsList.setLayoutManager(mLapsLayoutManager);

        // In landscape layouts, the laps list can reach the top of the screen and thus can cause
        // a drop shadow to appear. The same is not true for portrait landscapes.
        if (Utils.isLandscape(mContext)) {
            final ScrollPositionWatcher scrollPositionWatcher = new ScrollPositionWatcher();
            mLapsList.addOnLayoutChangeListener(scrollPositionWatcher);
            mLapsList.addOnScrollListener(scrollPositionWatcher);
        } else {
            setTabScrolledToTop(true);
        }
        mLapsList.setAdapter(mLapsAdapter);

        // Timer text serves as a virtual start/stop button.
        mMainTimeText = v.findViewById(R.id.stopwatch_time_text);
        mHundredthsTimeText = v.findViewById(R.id.stopwatch_hundredths_text);
        mStopwatchTextController = new StopwatchTextController(mMainTimeText, mHundredthsTimeText);
        mStopwatchWrapper = v.findViewById(R.id.stopwatch_time_wrapper);

        DataModel.getDataModel().addStopwatchListener(mStopwatchWatcher);

        mStopwatchWrapper.setOnClickListener(new TimeClickListener());
        if (mTime != null) {
            mStopwatchWrapper.setOnTouchListener(new CircleTouchListener());
        }

        final int colorAccent = mContext.getColor(R.color.md_theme_primary);
        final int textColorPrimary = mMainTimeText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
                new int[][]{{-state_activated, -state_pressed}, {}},
                new int[]{textColorPrimary, colorAccent});
        mMainTimeText.setTextColor(timeTextColor);
        mHundredthsTimeText.setTextColor(timeTextColor);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        mActivity = requireActivity();
        final Intent intent = mActivity.getIntent();
        if (intent != null) {
            final String action = intent.getAction();
            if (StopwatchService.ACTION_START_STOPWATCH.equals(action)) {
                DataModel.getDataModel().startStopwatch();
                // Consume the intent
                mActivity.setIntent(null);
            } else if (StopwatchService.ACTION_PAUSE_STOPWATCH.equals(action)) {
                DataModel.getDataModel().pauseStopwatch();
                // Consume the intent
                mActivity.setIntent(null);
            }
        }

        // Conservatively assume the data in the adapter has changed while the fragment was paused.
        mLapsAdapter.notifyDataSetChanged();

        // Synchronize the user interface with the data model.
        updateUI(FAB_AND_BUTTONS_IMMEDIATE);

        // Start watching for page changes away from this fragment.
        UiDataModel.getUiDataModel().addTabListener(mTabWatcher);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop all updates while the fragment is not visible.
        stopUpdatingTime();

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

    private void updateFab(@NonNull ImageView fab) {
        if (getStopwatch().isRunning()) {
            fab.setImageResource(R.drawable.ic_fab_pause);
            fab.setContentDescription(fab.getResources().getString(R.string.sw_pause_button));
        } else {
            fab.setImageResource(R.drawable.ic_fab_play);
            fab.setContentDescription(fab.getResources().getString(R.string.sw_start_button));
        }
        fab.setVisibility(VISIBLE);
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        updateFab(fab);
    }

    @Override
    public void onMorphFab(@NonNull ImageView fab) {
        // Update the fab's drawable to match the current timer state.
        updateFab(fab);
        // Animate the drawable.
        AnimatorUtils.startDrawableAnimation(fab);
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        left.setClickable(true);
        left.setImageDrawable(AppCompatResources.getDrawable(left.getContext(), R.drawable.ic_reset));
        left.setContentDescription(mContext.getString(R.string.sw_reset_button));
        left.setOnClickListener(v -> doReset());

        switch (getStopwatch().getState()) {
            case RESET -> {
                left.setVisibility(INVISIBLE);
                right.setClickable(true);
                right.setVisibility(INVISIBLE);
            }
            case RUNNING -> {
                left.setVisibility(VISIBLE);
                final boolean canRecordLaps = canRecordMoreLaps();
                right.setImageDrawable(AppCompatResources.getDrawable(right.getContext(), R.drawable.ic_tab_stopwatch_static));
                right.setContentDescription(mContext.getString(R.string.sw_lap_button));
                right.setClickable(canRecordLaps);
                right.setVisibility(canRecordLaps ? VISIBLE : INVISIBLE);
                right.setOnClickListener(v -> doAddLap());
            }
            case PAUSED -> {
                left.setVisibility(VISIBLE);
                right.setClickable(true);
                right.setVisibility(VISIBLE);
                right.setImageDrawable(AppCompatResources.getDrawable(right.getContext(), R.drawable.ic_share));
                right.setContentDescription(mContext.getString(R.string.sw_share_button));
                right.setOnClickListener(v -> doShare());
            }
        }
    }

    /**
     * Start the stopwatch.
     */
    private void doStart() {
        Events.sendStopwatchEvent(R.string.action_start, R.string.label_deskclock);
        DataModel.getDataModel().startStopwatch();
        Utils.vibrationTime(mContext, 50);
    }

    /**
     * Pause the stopwatch.
     */
    private void doPause() {
        Events.sendStopwatchEvent(R.string.action_pause, R.string.label_deskclock);
        DataModel.getDataModel().pauseStopwatch();
        Utils.vibrationTime(mContext, 50);
    }

    /**
     * Reset the stopwatch.
     */
    private void doReset() {
        final Stopwatch.State priorState = getStopwatch().getState();
        Events.sendStopwatchEvent(R.string.action_reset, R.string.label_deskclock);
        DataModel.getDataModel().resetStopwatch();
        mMainTimeText.setAlpha(1f);
        mHundredthsTimeText.setAlpha(1f);
        if (priorState == Stopwatch.State.RUNNING) {
            updateFab(FAB_MORPH);
        }
        Utils.vibrationTime(mContext, 10);
    }

    /**
     * Send stopwatch time and lap times to an external sharing application.
     */
    private void doShare() {
        // Disable the fab buttons to avoid double-taps on the share button.
        updateFab(BUTTONS_DISABLE);

        final String[] subjects = mContext.getResources().getStringArray(R.array.sw_share_strings);
        final String subject = subjects[(int) (Math.random() * subjects.length)];
        final String text = mLapsAdapter.getShareText();

        final Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, text)
                .setType("text/plain");

        final String title = mContext.getString(R.string.sw_share_button);
        final Intent shareChooserIntent = Intent.createChooser(shareIntent, title);
        try {
            mContext.startActivity(shareChooserIntent);
        } catch (ActivityNotFoundException anfe) {
            LogUtils.e("Cannot share lap data because no suitable receiving Activity exists");
            updateFab(BUTTONS_IMMEDIATE);
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
        updateFab(BUTTONS_IMMEDIATE);

        if (lap.getLapNumber() == 1) {
            // Child views from prior lap sets hang around and blit to the screen when adding the
            // first lap of the subsequent lap set. Remove those superfluous children here manually
            // to ensure they aren't seen as the first lap is drawn.
            mLapsList.removeAllViewsInLayout();

            if (mTime != null) {
                // Start animating the reference lap.
                mTime.update();
            }

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
        if (sceneRoot == null) {
            return;
        }

        TransitionManager.beginDelayedTransition(sceneRoot);

        if (clearLaps) {
            mLapsAdapter.clearLaps();
        }

        final boolean lapsVisible = mLapsAdapter.getItemCount() > 0;
        mLapsList.setVisibility(lapsVisible ? VISIBLE : GONE);

        if (Utils.isPortrait(mContext)) {
            // When the lap list is visible, it includes the bottom padding. When it is absent the
            // appropriate bottom padding must be applied to the container.
            final int bottom = lapsVisible ? 0 : Utils.toPixel(80, mContext);
            final int top = sceneRoot.getPaddingTop();
            final int left = sceneRoot.getPaddingLeft();
            final int right = sceneRoot.getPaddingRight();
            sceneRoot.setPadding(left, top, right, bottom);
        }
    }

    private void adjustWakeLock() {
        final boolean appInForeground = DataModel.getDataModel().isApplicationInForeground();
        if (getStopwatch().isRunning() && isTabSelected() && appInForeground) {
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            releaseWakeLock();
        }
    }

    private void releaseWakeLock() {
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        mMainTimeText.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    private void stopUpdatingTime() {
        mMainTimeText.removeCallbacks(mTimeUpdateRunnable);
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
        mStopwatchTextController.setTimeString(totalTime);

        // Update the current lap.
        final boolean currentLapIsVisible = mLapsLayoutManager.findFirstVisibleItemPosition() == 0;
        if (!stopwatch.isReset() && currentLapIsVisible) {
            mLapsAdapter.updateCurrentLap(mLapsList, totalTime);
        }
    }

    /**
     * Synchronize the UI state with the model data.
     */
    private void updateUI(@UpdateFabFlag int updateTypes) {
        adjustWakeLock();

        // Draw the latest stopwatch and current lap times.
        updateTime();

        if (mTime != null) {
            mTime.update();
        }

        final Stopwatch stopwatch = getStopwatch();
        if (!stopwatch.isReset()) {
            startUpdatingTime();
        }

        // Adjust the visibility of the list of laps.
        showOrHideLaps(stopwatch.isReset());

        // Update button states.
        updateFab(updateTypes);
    }

    /**
     * This runnable periodically updates times throughout the UI. It stops these updates when the
     * stopwatch is no longer running.
     */
    private final class TimeUpdateRunnable implements Runnable {
        @Override
        public void run() {
            final long startTime = Utils.now();

            updateTime();

            // Blink text iff the stopwatch is paused and not pressed.
            final View touchTarget = mTime != null ? mTime : mStopwatchWrapper;
            final Stopwatch stopwatch = getStopwatch();
            final boolean blink = stopwatch.isPaused()
                    && startTime % 1000 < 500
                    && !touchTarget.isPressed();

            if (blink) {
                mMainTimeText.setAlpha(0f);
                mHundredthsTimeText.setAlpha(0f);
            } else {
                mMainTimeText.setAlpha(1f);
                mHundredthsTimeText.setAlpha(1f);
            }

            if (!stopwatch.isReset()) {
                final long period = stopwatch.isPaused()
                        ? REDRAW_PERIOD_PAUSED
                        : REDRAW_PERIOD_RUNNING;
                final long endTime = Utils.now();
                final long delay = Math.max(0, startTime + period - endTime);
                mMainTimeText.postDelayed(this, delay);
            }
        }
    }

    /**
     * Acquire or release the wake lock based on the tab state.
     */
    private final class TabWatcher implements TabListener {
        @Override
        public void selectedTabChanged(Tab newSelectedTab) {
            adjustWakeLock();
        }
    }

    /**
     * Update the user interface in response to a stopwatch change.
     */
    private class StopwatchWatcher implements StopwatchListener {
        @Override
        public void stopwatchUpdated(Stopwatch after) {
            if (after.isReset()) {
                // Ensure the drop shadow is hidden when the stopwatch is reset.
                setTabScrolledToTop(true);
                if (DataModel.getDataModel().isApplicationInForeground()) {
                    updateUI(BUTTONS_IMMEDIATE);
                }
                return;
            }
            if (DataModel.getDataModel().isApplicationInForeground()) {
                updateUI(FAB_MORPH | BUTTONS_IMMEDIATE);
            }
        }

    }

    /**
     * Toggles stopwatch state when user taps stopwatch.
     */
    private final class TimeClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (getStopwatch().isRunning()) {
                DataModel.getDataModel().pauseStopwatch();
            } else {
                DataModel.getDataModel().startStopwatch();
            }
            Utils.vibrationTime(mContext, 50);
        }
    }

    /**
     * Checks if the user is pressing inside of the stopwatch circle.
     */
    private static final class CircleTouchListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            final int actionMasked = event.getActionMasked();
            if (actionMasked != MotionEvent.ACTION_DOWN) {
                return false;
            }
            final float rX = view.getWidth() / 2f;
            final float rY = (view.getHeight() - view.getPaddingBottom()) / 2f;
            final float r = Math.min(rX, rY);

            final float x = event.getX() - rX;
            final float y = event.getY() - rY;

            final boolean inCircle = Math.pow(x / r, 2.0) + Math.pow(y / r, 2.0) <= 1.0;

            // Consume the event if it is outside the circle
            return !inCircle;
        }
    }

    /**
     * Updates the vertical scroll state of this tab in the {@link UiDataModel} as the user scrolls
     * the recyclerview or when the size/position of elements within the recyclerview changes.
     */
    private final class ScrollPositionWatcher extends RecyclerView.OnScrollListener
            implements View.OnLayoutChangeListener {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            setTabScrolledToTop(Utils.isScrolledToTop(mLapsList));
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                   int oldLeft, int oldTop, int oldRight, int oldBottom) {
            setTabScrolledToTop(Utils.isScrolledToTop(mLapsList));
        }
    }
}
