/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static android.view.View.ALPHA;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.TRANSLATION_Y;
import static android.view.View.VISIBLE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMER_CREATION_VIEW_SPINNER_STYLE;
import static com.best.deskclock.uidata.UiDataModel.Tab.TIMERS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.DeskClock;
import com.best.deskclock.DeskClockFragment;
import com.best.deskclock.R;
import com.best.deskclock.RunnableFragment;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerListener;
import com.best.deskclock.events.Events;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import java.io.Serializable;
import java.util.Objects;

/**
 * Displays a vertical list of timers in all states.
 */
public final class TimerFragment extends DeskClockFragment implements RunnableFragment {

    private static final String EXTRA_TIMER_SETUP = "com.best.deskclock.action.TIMER_SETUP";

    private static final String KEY_TIMER_SETUP_STATE = "timer_setup_input";

    private SharedPreferences mPrefs;
    private Context mContext;
    private RecyclerView mRecyclerView;
    private Serializable mTimerSetupState;
    private TimerSetupView mCreateTimerView;
    private CustomTimerSpinnerSetupView mCreateTimerSpinnerView;
    private TimerAdapter mAdapter;
    private View mTimersView;
    private View mCurrentView;
    private ItemTouchHelper mItemTouchHelper;
    private boolean mIsTablet;
    private boolean mIsLandscape;

    /**
     * Updates the FABs in response to timers being added or removed.
     */
    private final TimerListener mTimerWatcher = new TimerWatcher();

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
        mPrefs = getDefaultSharedPreferences(mContext);
        mAdapter = new TimerAdapter(mContext, mPrefs, new TimerClickHandler(this));
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mTimersView = view.findViewById(R.id.timer_view);
        mCreateTimerView = view.findViewById(R.id.timer_setup);
        mCreateTimerSpinnerView = view.findViewById(R.id.timer_spinner_setup);
        mIsTablet = ThemeUtils.isTablet();
        mIsLandscape = ThemeUtils.isLandscape();

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(getLayoutManager(mContext));
        // Due to the ViewPager and the location of FAB, set a bottom padding and/or a right padding
        // to prevent the reset button from being hidden by the FAB (e.g. when scrolling down).
        final int bottomPadding = ThemeUtils.convertDpToPixels(mIsTablet ? 110 : mIsLandscape ? 4 : 95, mContext);
        final int rightPadding = ThemeUtils.convertDpToPixels(!mIsTablet && mIsLandscape ? 85 : 0, mContext);
        mRecyclerView.setPadding(0, 0, rightPadding, bottomPadding);
        mRecyclerView.setClipToPadding(false);

        mCreateTimerView.setFabContainer(this);
        mCreateTimerSpinnerView.setOnChangeListener(() -> updateFab(FAB_SHRINK_AND_EXPAND));

        DataModel.getDataModel().addTimerListener(mAdapter);
        DataModel.getDataModel().addTimerListener(mTimerWatcher);

        mAdapter.loadTimerList();

        mItemTouchHelper = new ItemTouchHelper(new TimerAdapter.TimerItemTouchHelper(mAdapter, mRecyclerView));
        handleItemTouchHelper();

        // If timer setup state is present, retrieve it to be later honored.
        if (savedInstanceState != null) {
            mTimerSetupState = SdkUtils.isAtLeastAndroid13()
                    ? savedInstanceState.getSerializable(KEY_TIMER_SETUP_STATE, int[].class)
                    : savedInstanceState.getSerializable(KEY_TIMER_SETUP_STATE);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

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
                updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                mTimerSetupState = null;
            }
        } else {
            // Otherwise, default to showing the list of timers.
            showTimersView(FAB_AND_BUTTONS_IMMEDIATE);
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isTabSelected() && mCurrentView != mTimersView && hasTimers()) {
                            animateToView(mTimersView, false);
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();

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
        if (mCurrentView != mTimersView) {
            mTimerSetupState = mCreateTimerView.getState();
            outState.putSerializable(KEY_TIMER_SETUP_STATE, mTimerSetupState);
        }
    }

    @Override
    public void startRunnable() {
        startUpdatingTime();
    }

    @Override
    public void stopRunnable() {
        stopUpdatingTime();
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

        } else if (mCurrentView == getTimerCreationView()) {
            right.setVisibility(INVISIBLE);

            left.setClickable(true);
            left.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_cancel));
            left.setContentDescription(mContext.getString(android.R.string.cancel));
            // If no timers yet exist, the user is forced to create the first one.
            left.setVisibility(hasTimers() ? VISIBLE : INVISIBLE);
            left.setOnClickListener(v -> {
                mCreateTimerView.reset();
                mCreateTimerSpinnerView.reset();
                animateToView(mTimersView, false);
                left.announceForAccessibility(mContext.getString(R.string.timer_canceled));
                Utils.setVibrationTime(mContext, 10);
            });
        }
    }

    @Override
    public void onFabClick() {
        if (mCurrentView == mTimersView) {
            animateToView(getTimerCreationView(), true);
        } else if (mCurrentView == getTimerCreationView()) {
            mCreatingTimer = true;
            try {
                // Create the new timer.
                final long timerLength = getTimeInMillis();
                String defaultTimeToAddToTimer = String.valueOf(SettingsDAO.getDefaultTimeToAddToTimer(mPrefs));
                final Timer timer = DataModel.getDataModel().addTimer(timerLength, "",
                        defaultTimeToAddToTimer, false);
                Events.sendTimerEvent(R.string.action_create, R.string.label_deskclock);

                // Start the new timer.
                DataModel.getDataModel().startTimer(timer);
                Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
                Utils.setVibrationTime(mContext, 50);
            } finally {
                mCreatingTimer = false;
            }

            // Return to the list of timers.
            animateToView(mTimersView, false);
        }
    }

    @Override
    public void onFabLongClick(@NonNull ImageView fab) {
        fab.setHapticFeedbackEnabled(false);
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
        // Stop periodic updates for active timers.
        stopUpdatingTime();

        // Show the creation view; hide the timer view.
        mTimersView.setVisibility(GONE);

        // Reset all possible time picker views to be hidden in order to only show one of them later
        mCreateTimerView.setVisibility(GONE);
        mCreateTimerSpinnerView.setVisibility(GONE);

        // Record the fact that the create view is visible.
        mCurrentView = getTimerCreationView();
        mCurrentView.setVisibility(VISIBLE);

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
        mCreateTimerSpinnerView.setVisibility(GONE);

        // Record the fact that the create view is visible.
        mCurrentView = mTimersView;

        // Start periodic updates for active timers.
        startUpdatingTime();

        // Update the fab and buttons.
        updateFab(updateTypes);
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
            getTimerCreationView().setVisibility(VISIBLE);
        }
        // Avoid double-taps by enabling/disabling the set of buttons active on the new view.
        updateFab(BUTTONS_DISABLE);

        final long animationDuration = 600;

        final ViewTreeObserver viewTreeObserver = toView.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (viewTreeObserver.isAlive()) {
                    viewTreeObserver.removeOnPreDrawListener(this);
                }

                final float distanceY = requireView().getHeight() + requireView().getY();
                final float translationDistance = animateDown ? -distanceY : distanceY;

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
                            mCreateTimerSpinnerView.reset();
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
                        mCreateTimerSpinnerView.setTranslationY(0f);
                        mTimersView.setAlpha(1f);
                        mCreateTimerView.setAlpha(1f);
                        mCreateTimerSpinnerView.setAlpha(1f);
                    }
                });

                animatorSet.start();

                return true;
            }
        });
    }

    private void updateFab(@NonNull ImageView fab) {
        if (mContext != null) {
            if (mCurrentView == mTimersView) {
                fab.setImageResource(R.drawable.ic_add);
                fab.setContentDescription(mContext.getString(R.string.timer_add_timer));
                fab.setVisibility(VISIBLE);
            } else if (mCurrentView == getTimerCreationView()) {
                if (hasValidInput()) {
                    fab.setImageResource(R.drawable.ic_fab_play);
                    fab.setContentDescription(mContext.getString(R.string.timer_start));
                    fab.setVisibility(VISIBLE);
                } else {
                    fab.setContentDescription(null);
                    fab.setVisibility(INVISIBLE);
                }
            }
        }
    }

    private boolean isSpinnerCreationView() {
        return SettingsDAO.getTimerCreationViewStyle(mPrefs).equals(TIMER_CREATION_VIEW_SPINNER_STYLE);
    }

    private boolean hasValidInput() {
        if (isSpinnerCreationView()) {
            return mCreateTimerSpinnerView.getValue().toMillis() != 0;
        } else {
            return mCreateTimerView.hasValidInput();
        }
    }

    private long getTimeInMillis() {
        if (isSpinnerCreationView()) {
            return mCreateTimerSpinnerView.getValue().toMillis();
        } else {
            return mCreateTimerView.getTimeInMillis();
        }
    }

    private View getTimerCreationView() {
        if (isSpinnerCreationView()) {
            return mCreateTimerSpinnerView;
        } else {
            return mCreateTimerView;
        }
    }

    public void startUpdatingTime() {
        if (!isTabSelected() || !DataModel.getDataModel().hasActiveTimer()) {
            return;
        }

        mAdapter.updateTime();
    }

    public void stopUpdatingTime() {
        mAdapter.stopAllUpdating();
    }

    private boolean hasTimers() {
        return mAdapter.getItemCount() > 0;
    }

    private RecyclerView.LayoutManager getLayoutManager(Context context) {
        if (mIsTablet && mAdapter.getItemCount() > 1) {
            return new GridLayoutManager(context, mIsLandscape ? 3 : 2);
        }

        return new LinearLayoutManager(context, mIsLandscape
                ? LinearLayoutManager.HORIZONTAL
                : LinearLayoutManager.VERTICAL, false);
    }

    private void handleItemTouchHelper() {
        if (mAdapter.getItemCount() > 1) {
            mItemTouchHelper.attachToRecyclerView(mRecyclerView);
        } else {
            mItemTouchHelper.attachToRecyclerView(null);
        }
    }

    private void adjustWakeLock() {
        if (DataModel.getDataModel().hasActiveTimer() || SettingsDAO.shouldScreenRemainOn(mPrefs)) {
            ThemeUtils.keepScreenOn(requireActivity());
        } else {
            ThemeUtils.releaseKeepScreenOn(requireActivity());
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

            // Required to adjust the layout for tablets that use either a GridLayoutManager or a LinearLayoutManager.
            if (mIsTablet) {
                mRecyclerView.setLayoutManager(getLayoutManager(mContext));
            }

            // Required to attach the ItemTouchHelper when there is more than one timer.
            handleItemTouchHelper();
        }

        @Override
        public void timerUpdated(Timer before, Timer after) {
            // If the timer started, animate the timers and scroll to its position.
            if (before.isReset() && !after.isReset()) {
                Objects.requireNonNull(mRecyclerView.getLayoutManager()).scrollToPosition(mAdapter.getTimers().indexOf(before));
            }

            adjustWakeLock();
        }

        @Override
        public void timerRemoved(Timer timer) {
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);

            if (mCurrentView == mTimersView && mAdapter.getItemCount() == 0) {
                animateToView(getTimerCreationView(), true);
            }

            // Required to adjust the layout for tablets that use either a GridLayoutManager or a LinearLayoutManager.
            if (mIsTablet) {
                mRecyclerView.setLayoutManager(getLayoutManager(mContext));
            }

            // Required to detach the ItemTouchHelper when there is only one timer left.
            handleItemTouchHelper();

            adjustWakeLock();
        }
    }
}
