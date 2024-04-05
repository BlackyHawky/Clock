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

package com.best.deskclock.timer;

import static android.view.View.ALPHA;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.TRANSLATION_Y;
import static android.view.View.VISIBLE;
import static com.best.deskclock.uidata.UiDataModel.Tab.TIMERS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.AnimatorUtils;
import com.best.deskclock.DeskClock;
import com.best.deskclock.DeskClockFragment;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerListener;
import com.best.deskclock.events.Events;
import com.best.deskclock.uidata.UiDataModel;

import java.io.Serializable;

/**
 * Displays a vertical list of timers in all states.
 */
public final class TimerFragment extends DeskClockFragment {

    private static final String EXTRA_TIMER_SETUP = "com.best.deskclock.action.TIMER_SETUP";

    private static final String KEY_TIMER_SETUP_STATE = "timer_setup_input";


    /**
     * Scheduled to update the timers while at least one is running.
     */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /**
     * Updates the FABs in response to timers being added or removed.
     */
    private final TimerListener mTimerWatcher = new TimerWatcher();

    private TimerSetupView mCreateTimerView;
    private TimerAdapter mAdapter;

    private View mTimersView;
    private View mCurrentView;
    private RecyclerView mRecyclerView;

    private Serializable mTimerSetupState;

    private Context mContext;

    /**
     * {@code true} while this fragment is creating a new timer; {@code false} otherwise.
     */
    private boolean mCreatingTimer;

    /**
     * @return an Intent that selects the timers tab with the setup screen for a new timer in place.
     */
    public static Intent createTimerSetupIntent(Context context) {
        return new Intent(context, DeskClock.class).putExtra(EXTRA_TIMER_SETUP, true);
    }

    /**
     * The public no-arg constructor required by all fragments.
     */
    public TimerFragment() {
        super(TIMERS);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.timer_fragment, container, false);

        mContext = requireContext();

        TimerClickHandler timerClickHandler = new TimerClickHandler(this);
        mAdapter = new TimerAdapter(timerClickHandler);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(getLayoutManager(view.getContext()));

        mTimersView = view.findViewById(R.id.timer_view);
        mCreateTimerView = view.findViewById(R.id.timer_setup);
        mCreateTimerView.setFabContainer(this);

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

        boolean createTimer = false;
        int showTimerId = -1;

        // Examine the intent of the parent activity to determine which view to display.
        final Intent intent = requireActivity().getIntent();
        if (intent != null) {
            // These extras are single-use; remove them after honoring them.
            createTimer = intent.getBooleanExtra(EXTRA_TIMER_SETUP, false);
            intent.removeExtra(EXTRA_TIMER_SETUP);

            showTimerId = intent.getIntExtra(TimerService.EXTRA_TIMER_ID, -1);
            intent.removeExtra(TimerService.EXTRA_TIMER_ID);
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the timer creation view is visible, store the input for later restoration.
        if (mCurrentView == mCreateTimerView) {
            mTimerSetupState = mCreateTimerView.getState();
            outState.putSerializable(KEY_TIMER_SETUP_STATE, mTimerSetupState);
        }
    }

    private void updateFab(@NonNull ImageView fab) {
        if (mCurrentView == mTimersView) {
            fab.setImageResource(R.drawable.ic_add);
            fab.setContentDescription(mContext.getString(R.string.timer_add_timer));
            fab.setVisibility(VISIBLE);
        } else if (mCurrentView == mCreateTimerView) {
            if (mCreateTimerView.hasValidInput()) {
                fab.setImageResource(R.drawable.ic_fab_play);
                fab.setContentDescription(mContext.getString(R.string.timer_start));
                fab.setVisibility(VISIBLE);
            } else {
                fab.setContentDescription(null);
                fab.setVisibility(INVISIBLE);
            }
        }
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
        if (mCurrentView == mTimersView) {
            left.setVisibility(INVISIBLE);
            right.setVisibility(INVISIBLE);

        } else if (mCurrentView == mCreateTimerView) {
            right.setVisibility(INVISIBLE);

            left.setClickable(true);
            left.setImageDrawable(AppCompatResources.getDrawable(left.getContext(), R.drawable.ic_cancel));
            left.setContentDescription(mContext.getString(R.string.timer_cancel));
            // If no timers yet exist, the user is forced to create the first one.
            left.setVisibility(hasTimers() ? VISIBLE : INVISIBLE);
            left.setOnClickListener(v -> {
                mCreateTimerView.reset();
                animateToView(mTimersView, false);
                left.announceForAccessibility(mContext.getString(R.string.timer_canceled));
            });
        }
    }

    @Override
    public void onFabClick(@NonNull ImageView fab) {
        if (mCurrentView == mTimersView) {
            animateToView(mCreateTimerView, true);
        } else if (mCurrentView == mCreateTimerView) {
            mCreatingTimer = true;
            try {
                // Create the new timer.
                final long timerLength = mCreateTimerView.getTimeInMillis();
                final Timer timer = DataModel.getDataModel().addTimer(timerLength, "", false);
                Events.sendTimerEvent(R.string.action_create, R.string.label_deskclock);

                // Start the new timer.
                DataModel.getDataModel().startTimer(timer);
                Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);


            } finally {
                mCreatingTimer = false;
            }

            // Return to the list of timers.
            animateToView(mTimersView, true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mCurrentView == mCreateTimerView) {
            return mCreateTimerView.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }



    /**
     * Display the view that creates a new timer.
     */
    private void showCreateTimerView(int updateTypes) {
        // Stop animating the timers.
        stopUpdatingTime();

        // Show the creation view; hide the timer view.
        mTimersView.setVisibility(GONE);
        mCreateTimerView.setVisibility(VISIBLE);

        // Record the fact that the create view is visible.
        mCurrentView = mCreateTimerView;

        // Update the fab and buttons.
        updateFab(updateTypes);
    }

    /**
     * Display the view that lists all existing timers.
     */
    private void showTimersView(int updateTypes) {
        // Clear any defunct timer creation state; the next timer creation starts fresh.
        mTimerSetupState = null;

        // Show the timer view; hide the creation view.
        mTimersView.setVisibility(VISIBLE);
        mCreateTimerView.setVisibility(GONE);

        // Record the fact that the create view is visible.
        mCurrentView = mTimersView;

        // Update the fab and buttons.
        updateFab(updateTypes);

        // Start animating the timers.
        startUpdatingTime();
    }



    /**
     * @param toView      one of {@link #mTimersView} or {@link #mCreateTimerView}
     * @param animateDown {@code true} if the views should animate upwards, otherwise downwards
     */
    private void animateToView(final View toView, final boolean animateDown) {
        if (mCurrentView == toView) {
            return;
        }

        final boolean toTimers = toView == mTimersView;
        if (toTimers) {
            mTimersView.setVisibility(VISIBLE);
        } else {
            mCreateTimerView.setVisibility(VISIBLE);
        }
        // Avoid double-taps by enabling/disabling the set of buttons active on the new view.
        updateFab(BUTTONS_DISABLE);

        final long animationDuration = UiDataModel.getUiDataModel().getMediumAnimationDuration();

        final ViewTreeObserver viewTreeObserver = toView.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (viewTreeObserver.isAlive()) {
                    viewTreeObserver.removeOnPreDrawListener(this);
                }

                final View view = mTimersView.findViewById(R.id.timer_time);
                final float distanceY = view != null ? view.getHeight() + view.getY() : 0;
                final float translationDistance = animateDown ? distanceY : -distanceY;

                toView.setTranslationY(-translationDistance);
                mCurrentView.setTranslationY(0f);
                toView.setAlpha(0f);
                mCurrentView.setAlpha(1f);

                final Animator translateCurrent = ObjectAnimator.ofFloat(mCurrentView, TRANSLATION_Y, translationDistance);
                final Animator translateNew = ObjectAnimator.ofFloat(toView, TRANSLATION_Y, 0f);
                final AnimatorSet translationAnimatorSet = new AnimatorSet();
                translationAnimatorSet.playTogether(translateCurrent, translateNew);
                translationAnimatorSet.setDuration(animationDuration);
                translationAnimatorSet.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

                final Animator fadeOutAnimator = ObjectAnimator.ofFloat(mCurrentView, ALPHA, 0f);
                fadeOutAnimator.setDuration(animationDuration / 2);
                fadeOutAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);

                        // The fade-out animation and fab-shrinking animation should run together.
                        updateFab(FAB_AND_BUTTONS_SHRINK);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (toTimers) {
                            showTimersView(FAB_AND_BUTTONS_EXPAND);

                            // Reset the state of the create view.
                            mCreateTimerView.reset();
                        } else {
                            showCreateTimerView(FAB_AND_BUTTONS_EXPAND);
                        }

                        // Update the fab and button states now that the correct view is visible and
                        // before the animation to expand the fab and buttons starts.
                        updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                    }
                });

                final Animator fadeInAnimator = ObjectAnimator.ofFloat(toView, ALPHA, 1f);
                fadeInAnimator.setDuration(animationDuration / 2);
                fadeInAnimator.setStartDelay(animationDuration / 2);

                final AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(fadeOutAnimator, fadeInAnimator, translationAnimatorSet);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mTimersView.setTranslationY(0f);
                        mCreateTimerView.setTranslationY(0f);
                        mTimersView.setAlpha(1f);
                        mCreateTimerView.setAlpha(1f);
                    }
                });

                animatorSet.start();

                return true;
            }
        });
    }

    private boolean hasTimers() {
        return mAdapter.getItemCount() > 0;
    }

    private void startUpdatingTime() {
        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
        mRecyclerView.post(mTimeUpdateRunnable);
    }

    private void stopUpdatingTime() {
        mRecyclerView.removeCallbacks(mTimeUpdateRunnable);
    }

    private RecyclerView.LayoutManager getLayoutManager(Context context) {
        if (Utils.isTablet(context)) {
            int columnCount = Utils.isLandscape(context) ? 3 : 2;
            return new GridLayoutManager(context, columnCount);
        }

        return new LinearLayoutManager(context, Utils.isLandscape(context)
                ? LinearLayoutManager.HORIZONTAL
                : LinearLayoutManager.VERTICAL, false);
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


    /** Update the fab in response to the visible timer changing.
     */
    private class TimerWatcher implements TimerListener {
        @Override
        public void timerAdded(Timer timer) {

            // If the timer is being created via this fragment avoid adjusting the fab.
            // Timer setup view is about to be animated away in response to this timer creation.
            // Changes to the fab immediately preceding that animation are jarring.
            if (!mCreatingTimer) {
                updateFab(FAB_AND_BUTTONS_IMMEDIATE);
            }
        }

        @Override
        public void timerUpdated(Timer before, Timer after) {
            // If the timer started, animate the timers.
            if (before.isReset() && !after.isReset()) {
                startUpdatingTime();
            }


        }

        @Override
        public void timerRemoved(Timer timer) {

            updateFab(FAB_AND_BUTTONS_IMMEDIATE);

            if (mCurrentView == mTimersView && mAdapter.getItemCount() == 0) {
                animateToView(mCreateTimerView, false);
            }
        }
    }
}
