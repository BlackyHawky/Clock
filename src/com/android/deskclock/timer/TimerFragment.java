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
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.R;
import com.android.deskclock.TimerSetupView;
import com.android.deskclock.ToastMaster;
import com.android.deskclock.Utils;
import com.android.deskclock.VerticalViewPager;

public class TimerFragment extends DeskClockFragment implements OnSharedPreferenceChangeListener {
    public static final long ANIMATION_TIME_MILLIS = DateUtils.SECOND_IN_MILLIS / 3;

    private static final String KEY_SETUP_SELECTED = "_setup_selected";
    private static final String KEY_ENTRY_STATE = "entry_state";
    private static final int MAX_PAGE_COUNT = 4;
    private static final String CURR_PAGE = "_currPage";
    private static final Handler HANDLER = new Handler();
    private static final Interpolator REVEAL_INTERPOLATOR =
            new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);

    private boolean mTicking = false;
    private TimerSetupView mSetupView;
    private VerticalViewPager mViewPager;
    private TimerFragmentAdapter mAdapter;
    private ImageButton mFab;
    private ImageButton mLeftButton;
    private ImageButton mRightButton;
    private ImageButton mCancel;
    private ViewGroup mContentView;
    private View mTimerView;
    private View mLastView;
    private ImageView[] mPageIndicators = new ImageView[MAX_PAGE_COUNT];
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
                if (t.mTimeLeft <= 0 && t.mState != TimerObj.STATE_DONE
                        && t.mState != TimerObj.STATE_RESTART) {
                    t.mState = TimerObj.STATE_TIMESUP;
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
                    revealAnimation(getActivity(), v, Utils.getNextHourColor());
                    HANDLER.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            goToPagerView();
                        }
                    }, ANIMATION_TIME_MILLIS);
                }
            }
        });
        mDeleteTransition = new AutoTransition();
        mDeleteTransition.setDuration(ANIMATION_TIME_MILLIS / 2);
        mDeleteTransition.setInterpolator(new AccelerateDecelerateInterpolator());

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

        // Clear the flag set in the notification and alert because the adapter was just
        // created and is thus in sync with the database
        final SharedPreferences.Editor editor = mPrefs.edit();
        if (mPrefs.getBoolean(Timers.FROM_NOTIFICATION, false)) {
            editor.putBoolean(Timers.FROM_NOTIFICATION, false);
        }
        if (mPrefs.getBoolean(Timers.FROM_ALERT, false)) {
            editor.putBoolean(Timers.FROM_ALERT, false);
        }
        editor.apply();

        mCancel.setVisibility(mAdapter.getCount() == 0 ? View.INVISIBLE : View.VISIBLE);

        boolean goToSetUpView;
        // Process extras that were sent to the app and were intended for the timer fragment
        final Intent newIntent = getActivity().getIntent();
        if (newIntent != null && newIntent.getBooleanExtra(
                TimerFullScreenFragment.GOTO_SETUP_VIEW, false)) {
            goToSetUpView = true;
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
        if (mAdapter != null) {
            mAdapter.saveTimersToSharedPrefs();
        }
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
        setLeftRightButtonAppearance(mLeftButton, mRightButton);
        setFabAppearance(mFab);
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
        setLeftRightButtonAppearance(mLeftButton, mRightButton);
        setFabAppearance(mFab);
        stopClockTicks();
    }

    private void updateTimerState(TimerObj t, String action) {
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
        // Make sure the receiver is getting the intent ASAP.
        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        getActivity().sendBroadcast(i);
    }

    private void setTimerViewFabIcon(TimerObj timer) {
        final Context context = getActivity();
        if (context == null || timer == null || mFab == null) {
            return;
        }
        final Resources r = context.getResources();
        switch (timer.mState) {
            case TimerObj.STATE_RUNNING:
                mFab.setVisibility(View.VISIBLE);
                mFab.setContentDescription(r.getString(R.string.timer_stop));
                mFab.setImageResource(R.drawable.ic_fab_pause);
                break;
            case TimerObj.STATE_STOPPED:
            case TimerObj.STATE_RESTART:
                mFab.setVisibility(View.VISIBLE);
                mFab.setContentDescription(r.getString(R.string.timer_start));
                mFab.setImageResource(R.drawable.ic_fab_play);
                break;
            case TimerObj.STATE_DONE: // time-up then stopped
                mFab.setVisibility(View.INVISIBLE);
                break;
            case TimerObj.STATE_TIMESUP: // time-up but didn't stopped, continue negative ticking
                mFab.setVisibility(View.VISIBLE);
                mFab.setContentDescription(r.getString(R.string.timer_reset));
                // TODO: request UX to provide a real reset asset instead of the stop asset.
                mFab.setImageResource(R.drawable.ic_fab_stop);
                break;
            default:
        }
    }

    @Override
    public void onFabClick(View view) {
        if (mLastView != mTimerView) {
            final Activity activity = getActivity();
            revealAnimation(activity, mFab, activity.getResources().getColor(R.color.hot_pink));
            HANDLER.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Timer is at Setup View, so fab is "play"
                    final int timerLength = mSetupView.getTime();
                    if (timerLength == 0) {
                        return;
                    }

                    final TimerObj timerObj = new TimerObj(timerLength * DateUtils.SECOND_IN_MILLIS);
                    timerObj.mState = TimerObj.STATE_RUNNING;
                    updateTimerState(timerObj, Timers.START_TIMER);

                    // Go to the newly created timer view
                    mAdapter.addTimer(timerObj);
                    goToPagerView();
                    mViewPager.setCurrentItem(mAdapter.getCount() - 1);
                    mSetupView.reset(); // Make sure the setup is cleared for next time
                }
            }, ANIMATION_TIME_MILLIS);
        } else {
            // Timer is at view pager, so fab is "play" or "pause" or "square that means reset"
            final TimerObj t = getCurrentTimer();
            switch (t.mState) {
                case TimerObj.STATE_RUNNING:
                    // Stop timer and save the remaining time of the timer
                    t.mState = TimerObj.STATE_STOPPED;
                    t.mView.pause();
                    t.updateTimeLeft(true);
                    updateTimerState(t, Timers.TIMER_STOP);
                    break;
                case TimerObj.STATE_STOPPED:
                case TimerObj.STATE_RESTART:
                    // Reset the remaining time and continue timer
                    t.mState = TimerObj.STATE_RUNNING;
                    t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                    t.mView.start();
                    updateTimerState(t, Timers.START_TIMER);
                    break;
                case TimerObj.STATE_TIMESUP:
                    if (t.mDeleteAfterUse) {
                        cancelTimerNotification(t.mTimerId);
                        // Tell receiver the timer was deleted.
                        // It will stop all activity related to the
                        // timer
                        t.mState = TimerObj.STATE_DELETED;
                        updateTimerState(t, Timers.DELETE_TIMER);
                    } else {
                        t.mState = TimerObj.STATE_RESTART;
                        t.mOriginalLength = t.mSetupLength;
                        t.mTimeLeft = t.mSetupLength;
                        t.mView.stop();
                        t.mView.setTime(t.mTimeLeft, false);
                        t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
                        updateTimerState(t, Timers.TIMER_RESET);
                        cancelTimerNotification(t.mTimerId);
                    }
                    break;
            }
            setTimerViewFabIcon(t);
        }
    }


    public static void revealAnimation(final Activity activity, final View centerView, int color) {

        final View decorView = activity.getWindow().getDecorView();
        final ViewGroupOverlay overlay = (ViewGroupOverlay) decorView.getOverlay();

        // Create a transient view for performing the reveal animation.
        final View revealView = new View(activity);
        revealView.setRight(decorView.getWidth());
        revealView.setBottom(decorView.getHeight());
        revealView.setBackgroundColor(color);
        overlay.add(revealView);

        final int[] clearLocation = new int[2];
        centerView.getLocationInWindow(clearLocation);
        clearLocation[0] += centerView.getWidth() / 2;
        clearLocation[1] += centerView.getHeight() / 2;
        final int revealCenterX = clearLocation[0] - revealView.getLeft();
        final int revealCenterY = clearLocation[1] - revealView.getTop();

        final int xMax = Math.max(revealCenterX, decorView.getWidth() - revealCenterX);
        final int yMax = Math.max(revealCenterY, decorView.getHeight() - revealCenterY);
        final float revealRadius = (float) Math.sqrt(Math.pow(xMax, 2.0) + Math.pow(yMax, 2.0));

        final Animator revealAnimator = ViewAnimationUtils.createCircularReveal(
                revealView, revealCenterX, revealCenterY, 0.0f, revealRadius);
        revealAnimator.setInterpolator(REVEAL_INTERPOLATOR);

        float endAlpha = activity instanceof TimerAlertFullScreen ? 1.0f : 0.0f;
        final ValueAnimator fadeAnimator = ObjectAnimator.ofFloat(revealView, View.ALPHA, endAlpha);
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.remove(revealView);
            }
        });

        final AnimatorSet alertAnimator = new AnimatorSet();
        alertAnimator.setDuration(ANIMATION_TIME_MILLIS);
        alertAnimator.play(revealAnimator).before(fadeAnimator);
        alertAnimator.start();
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
    public void setFabAppearance(ImageButton fab) {
        mFab = fab;
        if (mFab != null) {
            if (atTimerTab()) {
                if (mLastView == mTimerView) {
                    setTimerViewFabIcon(getCurrentTimer());
                } else if (mSetupView != null) {
                    mSetupView.registerStartButton(mFab);
                    mFab.setImageResource(R.drawable.ic_fab_play);
                    mFab.setContentDescription(getString(R.string.timer_start));
                }
            } else {
                mFab.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean atTimerTab() {
        if (getActivity() instanceof DeskClock) {
            final DeskClock deskClockActivity = (DeskClock) getActivity();
            return deskClockActivity.getSelectedTab() == DeskClock.TIMER_TAB_INDEX;
        } else {
            return false;
        }
    }

    @Override
    public void setLeftRightButtonAppearance(ImageButton left, ImageButton right) {
        mLeftButton = left;
        mRightButton = right;
        if (mLeftButton != null && mRightButton != null && atTimerTab()) {
            mLeftButton.setEnabled(true);
            mRightButton.setEnabled(true);
            mLeftButton.setVisibility(mLastView != mTimerView ? View.GONE : View.VISIBLE);
            mRightButton.setVisibility(mLastView != mTimerView ? View.GONE : View.VISIBLE);
            mLeftButton.setImageResource(R.drawable.ic_add_timer);
            mLeftButton.setContentDescription(getString(R.string.timer_add_timer));
            mRightButton.setImageResource(R.drawable.ic_delete);
            mRightButton.setContentDescription(getString(R.string.timer_delete));
        }
    }

    @Override
    public void onLeftButtonClick(View view) {
        // Respond to another timer
        if (mAdapter.getCount() == MAX_PAGE_COUNT) {
            final Toast toast = Toast.makeText(getActivity(), R.string.timers_max_count_reached,
                    Toast.LENGTH_SHORT);
            ToastMaster.setToast(toast);
            toast.show();
        } else {
            revealAnimation(getActivity(), view, Utils.getNextHourColor());
            HANDLER.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSetupView.reset();
                    goToSetUpView();
                }
            }, ANIMATION_TIME_MILLIS);
        }
    }

    @Override
    public void onRightButtonClick(View view) {
        // Respond to delete
        final TimerObj timer = getCurrentTimer();
        if (timer.mState == TimerObj.STATE_TIMESUP) {
            mNotificationManager.cancel(timer.mTimerId);
        }
        if (mAdapter.getCount() == 1) {
            final Activity activity = getActivity();
            revealAnimation(activity, view, activity.getResources().getColor(R.color.clock_white));
            HANDLER.postDelayed(new Runnable() {
                @Override
                public void run() {
                    deleteTimer(timer);
                }
            }, ANIMATION_TIME_MILLIS);
        } else {
            TransitionManager.beginDelayedTransition(mContentView, mDeleteTransition);
            deleteTimer(timer);
        }
    }

    private void deleteTimer(TimerObj timer) {
        // Tell receiver the timer was deleted, it will stop all activity related to the
        // timer
        timer.mState = TimerObj.STATE_DELETED;
        updateTimerState(timer, Timers.DELETE_TIMER);
        highlightPageIndicator(mViewPager.getCurrentItem());
        // When deleting a negative timer (hidden fab), since deleting will not trigger
        // onResume(), in order to ensure the fab showing correctly, we need to manually
        // set fab appearance here.
        setFabAppearance(mFab);
    }

    private void highlightPageIndicator(int position) {
        for (int i = 0; i < MAX_PAGE_COUNT; i++) {
            final int count = mAdapter.getCount();
            if (count < 2 || i >= count) {
                mPageIndicators[i].setVisibility(View.GONE);
            } else {
                mPageIndicators[i].setVisibility(View.VISIBLE);
                mPageIndicators[i].setImageResource(position == i ?
                        R.drawable.ic_swipe_circle_light : R.drawable.ic_swipe_circle_dark);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(mPrefs)) {
            if ((key.equals(Timers.FROM_ALERT) && prefs.getBoolean(Timers.FROM_ALERT, false))
                    || (key.equals(Timers.FROM_NOTIFICATION)
                    && prefs.getBoolean(Timers.FROM_NOTIFICATION, false))) {
                // The data-changed flag was set in the alert or notification so the adapter needs
                // to re-sync with the database
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(key, false);
                editor.apply();
                mAdapter.populateTimersFromPref();
                mViewPager.setAdapter(mAdapter);
                if (mViewState != null) {
                    final int currPage = mViewState.getInt(CURR_PAGE);
                    mViewPager.setCurrentItem(currPage);
                    highlightPageIndicator(currPage);
                } else {
                    highlightPageIndicator(0);
                }
                setFabAppearance(mFab);
                return;
            }
        }
    }

    public void setLabel(TimerObj timer, String label) {
        timer.mLabel = label;
        updateTimerState(timer, Timers.TIMER_UPDATE);
        // Make sure the new label is visible.
        mAdapter.notifyDataSetChanged();
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
                break;
            case TimerObj.STATE_STOPPED:
            case TimerObj.STATE_DONE:
                t.mState = TimerObj.STATE_RESTART;
                t.mTimeLeft = t.mSetupLength;
                t.mOriginalLength = t.mSetupLength;
                t.mView.stop();
                t.mView.setTime(t.mTimeLeft, false);
                t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
                updateTimerState(t, Timers.TIMER_RESET);
                break;
            case TimerObj.STATE_TIMESUP:
                // +1 min when the time is up will restart the timer with 1 minute left.
                t.mState = TimerObj.STATE_RUNNING;
                t.mStartTime = Utils.getTimeNow();
                t.mTimeLeft = t.mOriginalLength = TimerObj.MINUTE_IN_MILLIS;
                t.mView.setTime(t.mTimeLeft, false);
                t.mView.set(t.mOriginalLength, t.mTimeLeft, true);
                t.mView.start();
                updateTimerState(t, Timers.TIMER_RESET);
                updateTimerState(t, Timers.START_TIMER);
                cancelTimerNotification(t.mTimerId);
                break;
        }
        // This will change status of the timer, so update fab
        setFabAppearance(mFab);
    }

    private void cancelTimerNotification(int timerId) {
        mNotificationManager.cancel(timerId);
    }
}
