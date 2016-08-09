/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.deskclock.timer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.HandleDeskClockApiCalls;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.data.TimerListener;
import com.android.deskclock.events.Events;
import com.android.deskclock.uidata.UiDataModel;

import java.io.Serializable;
import java.util.Arrays;

import static android.view.View.ALPHA;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.SCALE_X;
import static android.view.View.VISIBLE;
import static com.android.deskclock.FabContainer.UpdateType.DISABLE_BUTTONS;
import static com.android.deskclock.FabContainer.UpdateType.FAB_AND_BUTTONS_SHRINK_AND_EXPAND;
import static com.android.deskclock.FabContainer.UpdateType.FAB_AND_BUTTONS_IMMEDIATE;
import static com.android.deskclock.uidata.UiDataModel.Tab.TIMERS;

/**
 * Displays a vertical list of timers in all states.
 */
public final class TimerFragment extends DeskClockFragment {

    private static final String EXTRA_TIMER_SETUP = "com.android.deskclock.action.TIMER_SETUP";

    private static final String KEY_TIMER_SETUP_STATE = "timer_setup_input";

    /** Notified when the user swipes vertically to change the visible timer. */
    private final TimerPageChangeListener mTimerPageChangeListener = new TimerPageChangeListener();

    /** Scheduled to update the timers while at least one is running. */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /** Updates the {@link #mPageIndicators} in response to timers being added or removed. */
    private final TimerListener mTimerWatcher = new TimerWatcher();

    private TimerSetupView mCreateTimerView;
    private ViewPager mViewPager;
    private TimerPagerAdapter mAdapter;
    private View mTimersView;
    private View mCurrentView;
    private ImageView[] mPageIndicators;

    private Serializable mTimerSetupState;

    /**
     * @return an Intent that selects the timers tab with the setup screen for a new timer in place.
     */
    public static Intent createTimerSetupIntent(Context context) {
        return new Intent(context, DeskClock.class).putExtra(EXTRA_TIMER_SETUP, true);
    }

    /** The public no-arg constructor required by all fragments. */
    public TimerFragment() {
        super(TIMERS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.timer_fragment, container, false);

        mAdapter = new TimerPagerAdapter(getChildFragmentManager());
        mViewPager = (ViewPager) view.findViewById(R.id.vertical_view_pager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(mTimerPageChangeListener);

        mTimersView = view.findViewById(R.id.timer_view);
        mCreateTimerView = (TimerSetupView) view.findViewById(R.id.timer_setup);
        mCreateTimerView.setFabContainer(this);
        mPageIndicators = new ImageView[] {
                (ImageView) view.findViewById(R.id.page_indicator0),
                (ImageView) view.findViewById(R.id.page_indicator1),
                (ImageView) view.findViewById(R.id.page_indicator2),
                (ImageView) view.findViewById(R.id.page_indicator3)
        };

        DataModel.getDataModel().addTimerListener(mAdapter);
        DataModel.getDataModel().addTimerListener(mTimerWatcher);

        // If timer setup state is present, retrieve it to be later honored.
        if (savedInstanceState != null) {
            mTimerSetupState = savedInstanceState.getSerializable(KEY_TIMER_SETUP_STATE);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Initialize the page indicators.
        updatePageIndicators();

        boolean createTimer = false;
        int showTimerId = -1;

        // Examine the intent of the parent activity to determine which view to display.
        final Intent intent = getActivity().getIntent();
        if (intent != null) {
            // These extras are single-use; remove them after honoring them.
            createTimer = intent.getBooleanExtra(EXTRA_TIMER_SETUP, false);
            intent.removeExtra(EXTRA_TIMER_SETUP);

            showTimerId = intent.getIntExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, -1);
            intent.removeExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID);
        }

        // Choose the view to display in this fragment.
        if (showTimerId != -1) {
            // A specific timer must be shown; show the list of timers.
            showTimersView(FAB_AND_BUTTONS_IMMEDIATE);
        } else if (!hasTimers() || createTimer || mTimerSetupState != null) {
            // No timers exist, a timer is being created, or the last view was timer setup;
            // show the timer setup view.
            showCreateTimerView(FAB_AND_BUTTONS_IMMEDIATE);

            if (mTimerSetupState != null) {
                mCreateTimerView.setState(mTimerSetupState);
                mTimerSetupState = null;
            }
        } else {
            // Otherwise, default to showing the list of timers.
            showTimersView(FAB_AND_BUTTONS_IMMEDIATE);
        }

        // If the intent did not specify a timer to show, show the last timer that expired.
        if (showTimerId == -1) {
            final Timer timer = DataModel.getDataModel().getMostRecentExpiredTimer();
            showTimerId = timer == null ? -1 : timer.getId();
        }

        // If a specific timer should be displayed, display the corresponding timer tab.
        if (showTimerId != -1) {
            final Timer timer = DataModel.getDataModel().getTimer(showTimerId);
            if (timer != null) {
                final int index = DataModel.getDataModel().getTimers().indexOf(timer);
                mViewPager.setCurrentItem(index);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop updating the timers when this fragment is no longer visible.
        stopUpdatingTime();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        DataModel.getDataModel().removeTimerListener(mAdapter);
        DataModel.getDataModel().removeTimerListener(mTimerWatcher);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the timer creation view is visible, store the input for later restoration.
        if (mCurrentView == mCreateTimerView) {
            mTimerSetupState = mCreateTimerView.getState();
            outState.putSerializable(KEY_TIMER_SETUP_STATE, mTimerSetupState);
        }
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        if (mCurrentView == mTimersView) {
            final Timer timer = getTimer();
            if (timer == null) {
                fab.setVisibility(INVISIBLE);
                return;
            }

            fab.setVisibility(VISIBLE);
            switch (timer.getState()) {
                case RUNNING:
                    fab.setImageResource(R.drawable.ic_pause_white_24dp);
                    fab.setContentDescription(fab.getResources().getString(R.string.timer_stop));
                    break;
                case RESET:
                case PAUSED:
                    fab.setImageResource(R.drawable.ic_start_white_24dp);
                    fab.setContentDescription(fab.getResources().getString(R.string.timer_start));
                    break;
                case EXPIRED:
                    fab.setImageResource(R.drawable.ic_stop_white_24dp);
                    fab.setContentDescription(fab.getResources().getString(R.string.timer_stop));
                    break;
            }

        } else if (mCurrentView == mCreateTimerView) {
            if (mCreateTimerView.hasValidInput()) {
                fab.setImageResource(R.drawable.ic_start_white_24dp);
                fab.setContentDescription(fab.getResources().getString(R.string.timer_start));
                fab.setVisibility(VISIBLE);
            } else {
                fab.setVisibility(INVISIBLE);
            }
        }
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageButton left, @NonNull ImageButton right) {
        if (mCurrentView == mTimersView) {
            left.setEnabled(true);
            left.setImageResource(R.drawable.ic_delete);
            left.setContentDescription(left.getResources().getString(R.string.timer_delete));
            left.setVisibility(mCurrentView != mTimersView ? GONE : VISIBLE);

            right.setEnabled(true);
            right.setImageResource(R.drawable.ic_add_timer);
            right.setContentDescription(right.getResources().getString(R.string.timer_add_timer));
            right.setVisibility(mCurrentView != mTimersView ? GONE : VISIBLE);

        } else if (mCurrentView == mCreateTimerView) {
            left.setEnabled(true);
            left.setImageResource(R.drawable.ic_close);
            left.setContentDescription(left.getResources().getString(R.string.timer_cancel));
            // If no timers yet exist, the user is forced to create the first one.
            left.setVisibility(hasTimers() ? VISIBLE : INVISIBLE);

            right.setVisibility(GONE);
        }
    }

    @Override
    public void onFabClick(@NonNull ImageView fab) {
        if (mCurrentView == mTimersView) {
            final Timer timer = getTimer();

            // If no timer is currently showing a fab action is meaningless.
            if (timer == null) {
                return;
            }

            switch (timer.getState()) {
                case RUNNING:
                    DataModel.getDataModel().pauseTimer(timer);
                    Events.sendTimerEvent(R.string.action_stop, R.string.label_deskclock);
                    break;
                case PAUSED:
                case RESET:
                    DataModel.getDataModel().startTimer(timer);
                    Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
                    break;
                case EXPIRED:
                    DataModel.getDataModel().resetOrDeleteTimer(timer, R.string.label_deskclock);
                    break;
            }

        } else if (mCurrentView == mCreateTimerView) {
            // Create the new timer.
            final long length = mCreateTimerView.getTimeInMillis();
            final Timer timer = DataModel.getDataModel().addTimer(length, "", false);
            Events.sendTimerEvent(R.string.action_create, R.string.label_deskclock);

            // Start the new timer.
            DataModel.getDataModel().startTimer(timer);
            Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);

            // Reset the state of the create view.
            mCreateTimerView.reset();

            // Display the freshly created timer view.
            mViewPager.setCurrentItem(0);

            // Return to the list of timers.
            animateToView(mTimersView, null);
        }
    }

    @Override
    public void onLeftButtonClick(@NonNull ImageButton left) {
        if (mCurrentView == mTimersView) {
            final Timer timer = getTimer();
            if (timer == null) {
                return;
            }

            if (mAdapter.getCount() > 1) {
                animateTimerRemove(timer);
            } else {
                animateToView(mCreateTimerView, timer);
            }

            left.announceForAccessibility(getActivity().getString(R.string.timer_deleted));

        } else if (mCurrentView == mCreateTimerView) {
            // Clicking the X icon on the timer creation page returns to the timers list.
            mCreateTimerView.reset();

            animateToView(mTimersView, null);

            left.announceForAccessibility(getActivity().getString(R.string.timer_canceled));
        }
    }

    @Override
    public void onRightButtonClick(@NonNull ImageButton right) {
        animateToView(mCreateTimerView, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mCurrentView == mCreateTimerView) {
            return mCreateTimerView.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Updates the state of the page indicators so they reflect the selected page in the context of
     * all pages.
     */
    private void updatePageIndicators() {
        final int page = mViewPager.getCurrentItem();
        final int pageIndicatorCount = mPageIndicators.length;
        final int pageCount = mAdapter.getCount();

        final int[] states = computePageIndicatorStates(page, pageIndicatorCount, pageCount);
        for (int i = 0; i < states.length; i++) {
            final int state = states[i];
            final ImageView pageIndicator = mPageIndicators[i];
            if (state == 0) {
                pageIndicator.setVisibility(GONE);
            } else {
                pageIndicator.setVisibility(VISIBLE);
                pageIndicator.setImageResource(state);
            }
        }
    }

    /**
     * @param page the selected page; value between 0 and {@code pageCount}
     * @param pageIndicatorCount the number of indicators displaying the {@code page} location
     * @param pageCount the number of pages that exist
     * @return an array of length {@code pageIndicatorCount} specifying which image to display for
     *      each page indicator or 0 if the page indicator should be hidden
     */
    @VisibleForTesting
    static int[] computePageIndicatorStates(int page, int pageIndicatorCount, int pageCount) {
        // Compute the number of page indicators that will be visible.
        final int rangeSize = Math.min(pageIndicatorCount, pageCount);

        // Compute the inclusive range of pages to indicate centered around the selected page.
        int rangeStart = page - (rangeSize / 2);
        int rangeEnd = rangeStart + rangeSize - 1;

        // Clamp the range of pages if they extend beyond the last page.
        if (rangeEnd >= pageCount) {
            rangeEnd = pageCount - 1;
            rangeStart = rangeEnd - rangeSize + 1;
        }

        // Clamp the range of pages if they extend beyond the first page.
        if (rangeStart < 0) {
            rangeStart = 0;
            rangeEnd = rangeSize - 1;
        }

        // Build the result with all page indicators initially hidden.
        final int[] states = new int[pageIndicatorCount];
        Arrays.fill(states, 0);

        // If 0 or 1 total pages exist, all page indicators must remain hidden.
        if (rangeSize < 2) {
            return states;
        }

        // Initialize the visible page indicators to be dark.
        Arrays.fill(states, 0, rangeSize, R.drawable.ic_swipe_circle_dark);

        // If more pages exist before the first page indicator, make it a fade-in gradient.
        if (rangeStart > 0) {
            states[0] = R.drawable.ic_swipe_circle_top;
        }

        // If more pages exist after the last page indicator, make it a fade-out gradient.
        if (rangeEnd < pageCount - 1) {
            states[rangeSize - 1] = R.drawable.ic_swipe_circle_bottom;
        }

        // Set the indicator of the selected page to be light.
        states[page - rangeStart] = R.drawable.ic_swipe_circle_light;

        return states;
    }

    /**
     * Display the view that creates a new timer.
     */
    private void showCreateTimerView(UpdateType updateType) {
        // Stop animating the timers.
        stopUpdatingTime();

        // Show the creation view; hide the timer view.
        mTimersView.setVisibility(GONE);
        mCreateTimerView.setVisibility(VISIBLE);

        // Record the fact that the create view is visible.
        mCurrentView = mCreateTimerView;

        // Update the fab and buttons.
        updateFab(updateType);
    }

    /**
     * Display the view that lists all existing timers.
     */
    private void showTimersView(UpdateType updateType) {
        // Clear any defunct timer creation state; the next timer creation starts fresh.
        mTimerSetupState = null;

        // Show the timer view; hide the creation view.
        mTimersView.setVisibility(VISIBLE);
        mCreateTimerView.setVisibility(GONE);

        // Record the fact that the create view is visible.
        mCurrentView = mTimersView;

        // Update the fab and buttons.
        updateFab(updateType);

        // Start animating the timers.
        startUpdatingTime();
    }

    /**
     * @param timerToRemove the timer to be removed during the animation
     */
    private void animateTimerRemove(final Timer timerToRemove) {
        final long duration = UiDataModel.getUiDataModel().getShortAnimationDuration();

        final Animator fadeOut = ObjectAnimator.ofFloat(mViewPager, ALPHA, 1, 0);
        fadeOut.setDuration(duration);
        fadeOut.setInterpolator(new DecelerateInterpolator());
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                DataModel.getDataModel().removeTimer(timerToRemove);
                Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
            }
        });

        final Animator fadeIn = ObjectAnimator.ofFloat(mViewPager, ALPHA, 0, 1);
        fadeIn.setDuration(duration);
        fadeIn.setInterpolator(new AccelerateInterpolator());

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(fadeOut).before(fadeIn);
        animatorSet.start();
    }

    /**
     * @param toView one of {@link #mTimersView} or {@link #mCreateTimerView}
     * @param timerToRemove the timer to be removed during the animation; {@code null} if no timer
     *      should be removed
     */
    private void animateToView(View toView, final Timer timerToRemove) {
        if (mCurrentView == toView) {
            throw new IllegalStateException("toView is already the current view");
        }

        final boolean toTimers = toView == mTimersView;

        // Avoid double-taps by enabling/disabling the set of buttons active on the new view.
        updateFab(DISABLE_BUTTONS);

        final long duration = UiDataModel.getUiDataModel().getShortAnimationDuration();
        final Animator rotateFrom = ObjectAnimator.ofFloat(mCurrentView, SCALE_X, 1, 0);
        rotateFrom.setDuration(duration);
        rotateFrom.setInterpolator(new DecelerateInterpolator());
        rotateFrom.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (timerToRemove != null) {
                    DataModel.getDataModel().removeTimer(timerToRemove);
                    Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
                }

                mCurrentView.setScaleX(1);
                if (toTimers) {
                    showTimersView(FAB_AND_BUTTONS_SHRINK_AND_EXPAND);
                } else {
                    showCreateTimerView(FAB_AND_BUTTONS_SHRINK_AND_EXPAND);
                }
            }
        });

        final Animator rotateTo = ObjectAnimator.ofFloat(toView, SCALE_X, 0, 1);
        rotateTo.setDuration(duration);
        rotateTo.setInterpolator(new AccelerateInterpolator());

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(rotateFrom).before(rotateTo);
        animatorSet.start();
    }

    private boolean hasTimers() {
        return mAdapter.getCount() > 0;
    }

    private Timer getTimer() {
        if (mViewPager == null) {
            return null;
        }

        return mAdapter.getCount() == 0 ? null : mAdapter.getTimer(mViewPager.getCurrentItem());
    }

    private void startUpdatingTime() {
        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
        mViewPager.post(mTimeUpdateRunnable);
    }

    private void stopUpdatingTime() {
        mViewPager.removeCallbacks(mTimeUpdateRunnable);
    }

    /**
     * Periodically refreshes the state of each timer.
     */
    private class TimeUpdateRunnable implements Runnable {
        @Override
        public void run() {
            final long startTime = SystemClock.elapsedRealtime();
            // If no timers require continuous updates, avoid scheduling the next update.
            if (!mAdapter.updateTime()) {
                return;
            }
            final long endTime = SystemClock.elapsedRealtime();

            // Try to maintain a consistent period of time between redraws.
            final long delay = Math.max(0, startTime + 20 - endTime);
            mTimersView.postDelayed(this, delay);
        }
    }

    /**
     * Update the page indicators and fab in response to a new timer becoming visible.
     */
    private class TimerPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            updatePageIndicators();
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);

            // Showing a new timer page may introduce a timer requiring continuous updates.
            startUpdatingTime();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // Teasing a neighboring timer may introduce a timer requiring continuous updates.
            if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                startUpdatingTime();
            }
        }
    }

    /**
     * Update the page indicators in response to timers being added or removed.
     * Update the fab in response to the visible timer changing.
     */
    private class TimerWatcher implements TimerListener {
        @Override
        public void timerAdded(Timer timer) {
            // The user interface should not be updated unless the fragment is resumed. It will be
            // refreshed during onResume later if it is not currently resumed.
            if (!isResumed()) {
                return;
            }

            updatePageIndicators();
        }

        @Override
        public void timerUpdated(Timer before, Timer after) {
            // The user interface should not be updated unless the fragment is resumed. It will be
            // refreshed during onResume later if it is not currently resumed.
            if (!isResumed()) {
                return;
            }

            // If the timer started, animate the timers.
            if (before.isReset() && !after.isReset()) {
                startUpdatingTime();
            }

            // Fetch the index of the change.
            final int index = DataModel.getDataModel().getTimers().indexOf(after);

            // If the timer just expired but is not displayed, display it now.
            if (!before.isExpired() && after.isExpired() && index != mViewPager.getCurrentItem()) {
                mViewPager.setCurrentItem(index, true);

            } else if (mCurrentView == mTimersView && index == mViewPager.getCurrentItem()) {
                // If the visible timer changed, update the fab to match its new state.
                updateFab(FAB_AND_BUTTONS_IMMEDIATE);
            }
        }

        @Override
        public void timerRemoved(Timer timer) {
            // The user interface should not be updated unless the fragment is resumed. It will be
            // refreshed during onResume later if it is not currently resumed.
            if (!isResumed()) {
                return;
            }

            updatePageIndicators();
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);
        }
    }
}