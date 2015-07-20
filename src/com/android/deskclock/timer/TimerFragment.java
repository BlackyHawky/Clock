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
import android.animation.TimeInterpolator;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.R;
import com.android.deskclock.TimerSetupView;
import com.android.deskclock.Utils;
import com.android.deskclock.VerticalViewPager;
import com.android.deskclock.events.Events;

public class TimerFragment extends DeskClockFragment implements OnSharedPreferenceChangeListener {
    public static final long ANIMATION_TIME_MILLIS = DateUtils.SECOND_IN_MILLIS / 3;

    private static final String KEY_SETUP_SELECTED = "_setup_selected";
    private static final String KEY_ENTRY_STATE = "entry_state";
    private static final int PAGINATION_DOTS_COUNT = 4;
    private static final String CURR_PAGE = "_currPage";
    private static final TimeInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final TimeInterpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final long ROTATE_ANIM_DURATION_MILIS = 150;

    // Transitions are available only in API 19+
    private static final boolean USE_TRANSITION_FRAMEWORK =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    private boolean mTicking = false;
    private TimerSetupView mSetupView;
    private VerticalViewPager mViewPager;
    private TimerFragmentAdapter mAdapter;
    private ImageButton mCancel;
    private ViewGroup mContentView;
    private View mTimerView;
    private View mLastView;
    private ImageView[] mPageIndicators = new ImageView[PAGINATION_DOTS_COUNT];
    private Transition mDeleteTransition;
    private SharedPreferences mPrefs;
    private Bundle mViewState = null;
    private NotificationManager mNotificationManager;

    private final ViewPager.OnPageChangeListener mOnPageChangeListener =
            new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    highlightPageIndicator(position);
                    TimerFragment.this.setTimerViewFabIcon(getCurrentTimer());
                }
            };

    private final Runnable mClockTick = new Runnable() {
        boolean mVisible = true;
        final static int TIME_PERIOD_MS = 1000;
        final static int TIME_DELAY_MS = 20;
        final static int SPLIT = TIME_PERIOD_MS / 2;

        @Override
        public void run() {
            // Setup for blinking
            final boolean visible = Utils.getTimeNow() % TIME_PERIOD_MS < SPLIT;
            final boolean toggle = mVisible != visible;
            mVisible = visible;
            for (int i = 0; i < mAdapter.getCount(); i++) {
                final TimerObj t = mAdapter.getTimerAt(i);
                if (t.mState == TimerObj.STATE_RUNNING || t.mState == TimerObj.STATE_TIMESUP) {
                    final long timeLeft = t.updateTimeLeft(false);
                    if (t.mView != null) {
                        t.mView.setTime(timeLeft, false);
                        // Update button every 1/2 second
                        if (toggle) {
                            final ImageButton addMinuteButton = (ImageButton)
                                    t.mView.findViewById(R.id.reset_add);
                            final boolean canAddMinute = TimerObj.MAX_TIMER_LENGTH - t.mTimeLeft
                                    > TimerObj.MINUTE_IN_MILLIS;
                            addMinuteButton.setEnabled(canAddMinute);
                        }
                    }
                }
                if (t.mTimeLeft <= 0 && t.mState != TimerObj.STATE_RESTART) {
                    t.setState(TimerObj.STATE_TIMESUP);
                    if (t.mView != null) {
                        t.mView.timesUp();
                    }
                }
                // The blinking
                if (toggle && t.mView != null) {
                    if (t.mState == TimerObj.STATE_TIMESUP) {
                        t.mView.setCircleBlink(mVisible);
                    }
                    if (t.mState == TimerObj.STATE_STOPPED) {
                        t.mView.setTextBlink(mVisible);
                    }
                }
            }
            mTimerView.postDelayed(mClockTick, TIME_DELAY_MS);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewState = savedInstanceState;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.timer_fragment, container, false);
        mContentView = (ViewGroup) view;
        mTimerView = view.findViewById(R.id.timer_view);
        mSetupView = (TimerSetupView) view.findViewById(R.id.timer_setup);
        mViewPager = (VerticalViewPager) view.findViewById(R.id.vertical_view_pager);
        mPageIndicators[0] = (ImageView) view.findViewById(R.id.page_indicator0);
        mPageIndicators[1] = (ImageView) view.findViewById(R.id.page_indicator1);
        mPageIndicators[2] = (ImageView) view.findViewById(R.id.page_indicator2);
        mPageIndicators[3] = (ImageView) view.findViewById(R.id.page_indicator3);
        mCancel = (ImageButton) view.findViewById(R.id.timer_cancel);
        mCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapter.getCount() != 0) {
                    final AnimatorListenerAdapter adapter = new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mSetupView.reset(); // Make sure the setup is cleared for next time
                            mSetupView.setScaleX(1.0f); // Reset the scale for setup view
                            goToPagerView();
                        }
                    };
                    createRotateAnimator(adapter, false).start();
                }
            }
        });
        if (USE_TRANSITION_FRAMEWORK) {
            mDeleteTransition = new AutoTransition();
            mDeleteTransition.setDuration(ANIMATION_TIME_MILLIS / 2);
            mDeleteTransition.setInterpolator(new AccelerateDecelerateInterpolator());
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Context context = getActivity();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mNotificationManager = (NotificationManager) context.getSystemService(Context
                .NOTIFICATION_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof DeskClock) {
            DeskClock activity = (DeskClock) getActivity();
            activity.registerPageChangedListener(this);
        }

        if (mAdapter == null) {
            mAdapter = new TimerFragmentAdapter(getChildFragmentManager(), mPrefs);
        }
        mAdapter.populateTimersFromPref();
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(mOnPageChangeListener);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        if (mPrefs.getBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, false)) {
            // Clear the flag indicating the adapter is out of sync with the database.
            mPrefs.edit().putBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, false).apply();
        }

        mCancel.setVisibility(mAdapter.getCount() == 0 ? View.INVISIBLE : View.VISIBLE);

        boolean goToSetUpView;
        // Process extras that were sent to the app and were intended for the timer fragment
        final Intent newIntent = getActivity().getIntent();
        if (newIntent != null
                && newIntent.getBooleanExtra(TimerFullScreenFragment.GOTO_SETUP_VIEW, false)) {
            goToSetUpView = true;
        } else if (newIntent != null
                && newIntent.getBooleanExtra(Timers.FIRST_LAUNCH_FROM_API_CALL, false)) {
            // We use this extra to identify if a. this activity is launched from api call,
            // and b. this fragment is resumed for the first time. If both are true,
            // we should show the timer view instead of setup view.
            goToSetUpView = false;
            highlightPageIndicator(0);
            // Find the id of the timer to scroll to. Timers are loaded from SharedPrefs using a
            // HashSet as an intermediary, so we need to find the position in this specific
            // Adapter instance instead of just passing the position through the intent.
            final int timerPosition = ((TimerFragmentAdapter) mViewPager.getAdapter())
                    .getTimerPosition(newIntent.getIntExtra(Timers.SCROLL_TO_TIMER_ID, 0));
            mViewPager.setCurrentItem(timerPosition);

            // Reset the extra to false to ensure when next time the fragment resume,
            // we no longer care if it's from api call or not.
            newIntent.putExtra(Timers.FIRST_LAUNCH_FROM_API_CALL, false);
        } else {
            if (mViewState != null) {
                final int currPage = mViewState.getInt(CURR_PAGE);
                mViewPager.setCurrentItem(currPage);
                highlightPageIndicator(currPage);
                final boolean hasPreviousInput = mViewState.getBoolean(KEY_SETUP_SELECTED, false);
                goToSetUpView = hasPreviousInput || mAdapter.getCount() == 0;
                mSetupView.restoreEntryState(mViewState, KEY_ENTRY_STATE);
            } else {
                highlightPageIndicator(0);
                // If user was not previously using the setup, determine which view to go by count
                goToSetUpView = mAdapter.getCount() == 0;
            }
        }
        if (goToSetUpView) {
            goToSetUpView();
        } else {
            goToPagerView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof DeskClock) {
            ((DeskClock) getActivity()).unregisterPageChangedListener(this);
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        if (mAdapter != null) {
            mAdapter.saveTimersToSharedPrefs();
        }
        stopClockTicks();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSetupView != null) {
            outState.putBoolean(KEY_SETUP_SELECTED, mSetupView.getVisibility() == View.VISIBLE);
            mSetupView.saveEntryState(outState, KEY_ENTRY_STATE);
        }
        outState.putInt(CURR_PAGE, mViewPager.getCurrentItem());
        mViewState = outState;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewState = null;
    }

    @Override
    public void onPageChanged(int page) {
        if (page == DeskClock.TIMER_TAB_INDEX && mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    // Starts the ticks that animate the timers.
    private void startClockTicks() {
        mTimerView.postDelayed(mClockTick, 20);
        mTicking = true;
    }

    // Stops the ticks that animate the timers.
    private void stopClockTicks() {
        if (mTicking) {
            mViewPager.removeCallbacks(mClockTick);
            mTicking = false;
        }
    }

    private void goToPagerView() {
        mTimerView.setVisibility(View.VISIBLE);
        mSetupView.setVisibility(View.GONE);
        mLastView = mTimerView;
        setLeftRightButtonAppearance();
        setFabAppearance();
        startClockTicks();
    }

    private void goToSetUpView() {
        if (mAdapter.getCount() == 0) {
            mCancel.setVisibility(View.INVISIBLE);
        } else {
            mCancel.setVisibility(View.VISIBLE);
        }
        mTimerView.setVisibility(View.GONE);
        mSetupView.setVisibility(View.VISIBLE);
        mSetupView.updateDeleteButtonAndDivider();
        mSetupView.registerStartButton(mFab);
        mLastView = mSetupView;
        setLeftRightButtonAppearance();
        setFabAppearance();
        stopClockTicks();
    }

    private void updateTimerState(TimerObj t, String action) {
        updateTimerState(t, action, true);
    }

    /**
     * @param update indicates whether to call updateNextTimesup in TimerReceiver. This is false
     *               only for label changes.
     */
    private void updateTimerState(TimerObj t, String action, boolean update) {
        if (Timers.DELETE_TIMER.equals(action)) {
            mAdapter.deleteTimer(t.mTimerId);
            if (mAdapter.getCount() == 0) {
                mSetupView.reset();
                goToSetUpView();
            }
        } else {
            t.writeToSharedPref(mPrefs);
        }
        final Intent i = new Intent();
        i.setAction(action);
        i.putExtra(Timers.TIMER_INTENT_EXTRA, t.mTimerId);
        i.putExtra(Timers.UPDATE_NEXT_TIMESUP, update);
        // Make sure the receiver is getting the intent ASAP.
        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        getActivity().sendBroadcast(i);
    }

    private void setTimerViewFabIcon(TimerObj timer) {
        final DeskClock deskClock = (DeskClock) getActivity();
        if (deskClock == null || timer == null || mFab == null) {
            return;
        }

        if (deskClock.getSelectedTab() != DeskClock.TIMER_TAB_INDEX) {
            return;
        }

        final Resources r = deskClock.getResources();
        switch (timer.mState) {
            case TimerObj.STATE_RUNNING:
                mFab.setVisibility(View.VISIBLE);
                mFab.setContentDescription(r.getString(R.string.timer_stop));
                mFab.setImageResource(R.drawable.ic_fab_pause);
                break;
            case TimerObj.STATE_STOPPED:
            case TimerObj.STATE_RESTART:
            // It is possible for a Timer from an older version of Clock to be in STATE_DELETED and
            // still exist in the list
            case TimerObj.STATE_DELETED:
                mFab.setVisibility(View.VISIBLE);
                mFab.setContentDescription(r.getString(R.string.timer_start));
                mFab.setImageResource(R.drawable.ic_fab_play);
                break;
            case TimerObj.STATE_TIMESUP: // time-up but didn't stopped, continue negative ticking
                mFab.setVisibility(View.VISIBLE);
                mFab.setContentDescription(r.getString(R.string.timer_stop));
                mFab.setImageResource(R.drawable.ic_fab_stop);
                break;
            default:
        }
    }

    private Animator getRotateFromAnimator(View view) {
        final Animator animator = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 0.0f);
        animator.setDuration(ROTATE_ANIM_DURATION_MILIS);
        animator.setInterpolator(DECELERATE_INTERPOLATOR);
        return animator;
    }

    private Animator getRotateToAnimator(View view) {
        final Animator animator = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.0f, 1.0f);
        animator.setDuration(ROTATE_ANIM_DURATION_MILIS);
        animator.setInterpolator(ACCELERATE_INTERPOLATOR);
        return animator;
    }

    private Animator getScaleFooterButtonsAnimator(final boolean show) {
        final AnimatorSet animatorSet = new AnimatorSet();
        final Animator leftButtonAnimator = AnimatorUtils.getScaleAnimator(
                mLeftButton, show ? 0.0f : 1.0f, show ? 1.0f : 0.0f);
        final Animator rightButtonAnimator = AnimatorUtils.getScaleAnimator(
                mRightButton, show ? 0.0f : 1.0f, show ? 1.0f : 0.0f);
        final float fabStartScale = (show && mFab.getVisibility() == View.INVISIBLE) ? 0.0f : 1.0f;
        final Animator fabAnimator = AnimatorUtils.getScaleAnimator(
                mFab, fabStartScale, show ? 1.0f : 0.0f);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLeftButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                mRightButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                restoreScale(mLeftButton);
                restoreScale(mRightButton);
                restoreScale(mFab);
            }
        });
        // If not show, means transiting from timer view to setup view,
        // when the setup view starts to rotate, the footer buttons are already invisible,
        // so the scaling has to finish before the setup view starts rotating
        animatorSet.setDuration(show ? ROTATE_ANIM_DURATION_MILIS * 2 : ROTATE_ANIM_DURATION_MILIS);
        animatorSet.play(leftButtonAnimator).with(rightButtonAnimator).with(fabAnimator);
        return animatorSet;
    }

    private void restoreScale(View view) {
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
    }

    private Animator createRotateAnimator(AnimatorListenerAdapter adapter, boolean toSetup) {
        final AnimatorSet animatorSet = new AnimatorSet();
        final Animator rotateFrom = getRotateFromAnimator(toSetup ? mTimerView : mSetupView);
        rotateFrom.addListener(adapter);
        final Animator rotateTo = getRotateToAnimator(toSetup ? mSetupView : mTimerView);
        final Animator expandFooterButton = getScaleFooterButtonsAnimator(!toSetup);
        animatorSet.play(rotateFrom).before(rotateTo).with(expandFooterButton);
        return animatorSet;
    }

    @Override
    public void onFabClick(View view) {
        if (mLastView != mTimerView) {
            // Timer is at Setup View, so fab is "play", rotate from setup view to timer view
            final AnimatorListenerAdapter adapter = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    final int timerLength = mSetupView.getTime();
                    final TimerObj timerObj = new TimerObj(timerLength * DateUtils.SECOND_IN_MILLIS,
                        getActivity());
                    timerObj.setState(TimerObj.STATE_RUNNING);
                    Events.sendTimerEvent(R.string.action_create, R.string.label_deskclock);

                    updateTimerState(timerObj, Timers.START_TIMER);
                    Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);

                    // Go to the newly created timer view
                    mAdapter.addTimer(timerObj);
                    mViewPager.setCurrentItem(0);
                    highlightPageIndicator(0);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mSetupView.reset(); // Make sure the setup is cleared for next time
                    mSetupView.setScaleX(1.0f); // Reset the scale for setup view
                    goToPagerView();
                }
            };
            createRotateAnimator(adapter, false).start();
        } else {
            // Timer is at view pager, so fab is "play" or "pause" or "square that means reset"
            final TimerObj t = getCurrentTimer();
            switch (t.mState) {
                case TimerObj.STATE_RUNNING:
                    // Stop timer and save the remaining time of the timer
                    t.setState(TimerObj.STATE_STOPPED);
                    t.mView.pause();
                    t.updateTimeLeft(true);
                    updateTimerState(t, Timers.STOP_TIMER);
                    Events.sendTimerEvent(R.string.action_stop, R.string.label_deskclock);
                    break;
                case TimerObj.STATE_STOPPED:
                case TimerObj.STATE_RESTART:
                // It is possible for a Timer from an older version of Clock to be in STATE_DELETED and
                // still exist in the list
                case TimerObj.STATE_DELETED:
                    // Reset the remaining time and continue timer
                    t.setState(TimerObj.STATE_RUNNING);
                    t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                    t.mView.start();
                    updateTimerState(t, Timers.START_TIMER);
                    Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
                    break;
                case TimerObj.STATE_TIMESUP:
                    if (t.mDeleteAfterUse) {
                        cancelTimerNotification(t.mTimerId);
                        // Tell receiver the timer was deleted.
                        // It will stop all activity related to the
                        // timer
                        t.setState(TimerObj.STATE_DELETED);
                        updateTimerState(t, Timers.DELETE_TIMER);
                        Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
                    } else {
                        t.setState(TimerObj.STATE_RESTART);
                        t.mOriginalLength = t.mSetupLength;
                        t.mTimeLeft = t.mSetupLength;
                        t.mView.stop();
                        t.mView.setTime(t.mTimeLeft, false);
                        t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
                        updateTimerState(t, Timers.RESET_TIMER);
                        cancelTimerNotification(t.mTimerId);
                        Events.sendTimerEvent(R.string.action_reset, R.string.label_deskclock);
                    }
                    break;
            }
            setTimerViewFabIcon(t);
        }
    }


    private TimerObj getCurrentTimer() {
        if (mViewPager == null) {
            return null;
        }
        final int currPage = mViewPager.getCurrentItem();
        if (currPage < mAdapter.getCount()) {
            TimerObj o = mAdapter.getTimerAt(currPage);
            return o;
        } else {
            return null;
        }
    }

    @Override
    public void setFabAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        if (mFab == null) {
            return;
        }

        if (activity.getSelectedTab() != DeskClock.TIMER_TAB_INDEX) {
            mFab.setVisibility(View.VISIBLE);
            return;
        }

        if (mLastView == mTimerView) {
            setTimerViewFabIcon(getCurrentTimer());
        } else if (mSetupView != null) {
            mSetupView.registerStartButton(mFab);
            mFab.setImageResource(R.drawable.ic_fab_play);
            mFab.setContentDescription(getString(R.string.timer_start));
        }
    }

    @Override
    public void setLeftRightButtonAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        if (mLeftButton == null || mRightButton == null ||
                activity.getSelectedTab() != DeskClock.TIMER_TAB_INDEX) {
            return;
        }

        mLeftButton.setEnabled(true);
        mRightButton.setEnabled(true);
        mLeftButton.setVisibility(mLastView != mTimerView ? View.GONE : View.VISIBLE);
        mRightButton.setVisibility(mLastView != mTimerView ? View.GONE : View.VISIBLE);
        mLeftButton.setImageResource(R.drawable.ic_delete);
        mLeftButton.setContentDescription(getString(R.string.timer_delete));
        mRightButton.setImageResource(R.drawable.ic_add_timer);
        mRightButton.setContentDescription(getString(R.string.timer_add_timer));
    }

    @Override
    public void onRightButtonClick(View view) {
        // Respond to add another timer
        final AnimatorListenerAdapter adapter = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mSetupView.reset();
                mTimerView.setScaleX(1.0f); // Reset the scale for timer view
                goToSetUpView();
            }
        };
        createRotateAnimator(adapter, true).start();
    }

    @Override
    public void onLeftButtonClick(View view) {
        // Respond to delete timer
        final TimerObj timer = getCurrentTimer();
        if (timer == null) {
            return; // Prevent NPE if user click delete faster than the fade animation
        }
        if (timer.mState == TimerObj.STATE_TIMESUP) {
            mNotificationManager.cancel(timer.mTimerId);
        }
        if (mAdapter.getCount() == 1) {
            final AnimatorListenerAdapter adapter = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mTimerView.setScaleX(1.0f); // Reset the scale for timer view
                    deleteTimer(timer);
                }
            };
            createRotateAnimator(adapter, true).start();
        } else {
            if (USE_TRANSITION_FRAMEWORK) {
                TransitionManager.beginDelayedTransition(mContentView, mDeleteTransition);
            }
            deleteTimer(timer);
        }
    }

    private void deleteTimer(TimerObj timer) {
        // Tell receiver the timer was deleted, it will stop all activity related to the
        // timer
        timer.setState(TimerObj.STATE_DELETED);
        updateTimerState(timer, Timers.DELETE_TIMER);
        Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
        highlightPageIndicator(mViewPager.getCurrentItem());
        // When deleting a negative timer (hidden fab), since deleting will not trigger
        // onResume(), in order to ensure the fab showing correctly, we need to manually
        // set fab appearance here.
        setFabAppearance();
    }

    private void highlightPageIndicator(int position) {
        final int count = mAdapter.getCount();
        if (count <= PAGINATION_DOTS_COUNT) {
            for (int i = 0; i < PAGINATION_DOTS_COUNT; i++) {
                if (count < 2 || i >= count) {
                    mPageIndicators[i].setVisibility(View.GONE);
                } else {
                    paintIndicator(i, position == i ? R.drawable.ic_swipe_circle_light :
                            R.drawable.ic_swipe_circle_dark);
                }
            }
        } else {
            /**
             * If there are more than 4 timers, the top and/or bottom dot might need to show a
             * half fade, to indicate there are more timers in that direction.
             */
            final int aboveCount = position; // How many timers are above the current timer
            final int belowCount = count - position - 1; // How many timers are below
            if (aboveCount < PAGINATION_DOTS_COUNT - 1) {
                // There's enough room for the above timers, so top dot need not to fade
                for (int i = 0; i < aboveCount; i++) {
                    paintIndicator(i, R.drawable.ic_swipe_circle_dark);
                }
                paintIndicator(position, R.drawable.ic_swipe_circle_light);
                for (int i = position + 1; i < PAGINATION_DOTS_COUNT - 1 ; i++) {
                    paintIndicator(i, R.drawable.ic_swipe_circle_dark);
                }
                paintIndicator(PAGINATION_DOTS_COUNT - 1, R.drawable.ic_swipe_circle_bottom);
            } else {
                // There's not enough room for the above timers, top dot needs to fade
                paintIndicator(0, R.drawable.ic_swipe_circle_top);
                for (int i = 1; i < PAGINATION_DOTS_COUNT - 2; i++) {
                    paintIndicator(i, R.drawable.ic_swipe_circle_dark);
                }
                // Determine which resource to use for the "second indicator" from the bottom.
                paintIndicator(PAGINATION_DOTS_COUNT - 2, belowCount == 0 ?
                        R.drawable.ic_swipe_circle_dark : R.drawable.ic_swipe_circle_light);
                final int lastDotRes;
                if (belowCount == 0) {
                    // The current timer is the last one
                    lastDotRes = R.drawable.ic_swipe_circle_light;
                } else if (belowCount == 1) {
                    // There's only one timer below the current
                    lastDotRes = R.drawable.ic_swipe_circle_dark;
                } else {
                    // There are more than one timer below, bottom dot needs to fade
                    lastDotRes = R.drawable.ic_swipe_circle_bottom;
                }
                paintIndicator(PAGINATION_DOTS_COUNT - 1, lastDotRes);
            }
        }
    }

    private void paintIndicator(int position, int res) {
        mPageIndicators[position].setVisibility(View.VISIBLE);
        mPageIndicators[position].setImageResource(res);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(mPrefs)) {
            if (key.equals(Timers.REFRESH_UI_WITH_LATEST_DATA) && prefs.getBoolean(key, false)) {
                // Clear the flag forcing a refresh of the adapter to reflect external changes.
                mPrefs.edit().putBoolean(key, false).apply();
                mAdapter.populateTimersFromPref();
                mViewPager.setAdapter(mAdapter);
                if (mViewState != null) {
                    final int currPage = mViewState.getInt(CURR_PAGE);
                    mViewPager.setCurrentItem(currPage);
                    highlightPageIndicator(currPage);
                } else {
                    highlightPageIndicator(0);
                }
                setFabAppearance();
            }
        }
    }

    public void setLabel(TimerObj timer, String label) {
        timer.mLabel = label;
        updateTimerState(timer, Timers.TIMER_UPDATE, false);
        // Make sure the new label is visible.
        mAdapter.populateTimersFromPref();
    }

    public void onPlusOneButtonPressed(TimerObj t) {
        switch (t.mState) {
            case TimerObj.STATE_RUNNING:
                t.addTime(TimerObj.MINUTE_IN_MILLIS);
                long timeLeft = t.updateTimeLeft(false);
                t.mView.setTime(timeLeft, false);
                t.mView.setLength(timeLeft);
                mAdapter.notifyDataSetChanged();
                updateTimerState(t, Timers.TIMER_UPDATE);

                Events.sendTimerEvent(R.string.action_add_minute, R.string.label_deskclock);
                break;
            case TimerObj.STATE_STOPPED:
                t.setState(TimerObj.STATE_RESTART);
                t.mTimeLeft = t.mSetupLength;
                t.mOriginalLength = t.mSetupLength;
                t.mView.stop();
                t.mView.setTime(t.mTimeLeft, false);
                t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
                updateTimerState(t, Timers.RESET_TIMER);

                Events.sendTimerEvent(R.string.action_reset, R.string.label_deskclock);
                break;
            case TimerObj.STATE_TIMESUP:
                // +1 min when the time is up will restart the timer with 1 minute left.
                t.setState(TimerObj.STATE_RUNNING);
                t.mStartTime = Utils.getTimeNow();
                t.mTimeLeft = t.mOriginalLength = TimerObj.MINUTE_IN_MILLIS;
                t.mView.setTime(t.mTimeLeft, false);
                t.mView.set(t.mOriginalLength, t.mTimeLeft, true);
                t.mView.start();
                updateTimerState(t, Timers.RESET_TIMER);
                Events.sendTimerEvent(R.string.action_add_minute, R.string.label_deskclock);

                updateTimerState(t, Timers.START_TIMER);
                cancelTimerNotification(t.mTimerId);
                break;
        }
        // This will change status of the timer, so update fab
        setFabAppearance();
    }

    private void cancelTimerNotification(int timerId) {
        mNotificationManager.cancel(timerId);
    }
}
