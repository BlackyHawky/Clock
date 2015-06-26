/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.deskclock.CircleButtonsLayout;
import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.TimerSetupView;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;
import com.android.deskclock.widget.CircleView;
import com.android.deskclock.widget.sgv.GridAdapter;
import com.android.deskclock.widget.sgv.StaggeredGridView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

// TODO: This class is renamed from TimerFragment to TimerFullScreenFragment with no change. It
// is responsible for the timer list in full screen timer alert and should be deprecated shortly.
public class TimerFullScreenFragment extends DeskClockFragment
        implements OnClickListener, OnSharedPreferenceChangeListener {

    private static final String TAG = "TimerFragment1";
    private static final String KEY_ENTRY_STATE = "entry_state";
    public static final String GOTO_SETUP_VIEW = "deskclock.timers.gotosetup";

    private Bundle mViewState;
    private StaggeredGridView mTimersList;
    private View mTimersListPage;
    private int mColumnCount;
    private ImageButton mFab;
    private TimerSetupView mTimerSetup;
    private TimersListAdapter mAdapter;
    private boolean mTicking = false;
    private SharedPreferences mPrefs;
    private NotificationManager mNotificationManager;
    private OnEmptyListListener mOnEmptyListListener;
    private View mLastVisibleView = null;  // used to decide if to set the view or animate to it.

    class ClickAction {
        public static final int ACTION_STOP = 1;
        public static final int ACTION_PLUS_ONE = 2;
        public static final int ACTION_DELETE = 3;

        public int mAction;
        public TimerObj mTimer;

        public ClickAction(int action, TimerObj t) {
            mAction = action;
            mTimer = t;
        }
    }

    // Container Activity that requests TIMESUP_MODE must implement this interface
    public interface OnEmptyListListener {
        void onEmptyList();
        void onListChanged();
    }

    TimersListAdapter createAdapter(Context context) {
        if (mOnEmptyListListener == null) {
            return new TimersListAdapter(context);
        } else {
            return new TimesUpListAdapter(context);
        }
    }

    private class TimersListAdapter extends GridAdapter {

        protected final ArrayList<TimerObj> mTimers = new ArrayList<>();
        private final LayoutInflater mLayoutInflater;

        public TimersListAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mTimers.size();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public TimerObj getItem(int p) {
            return mTimers.get(p);
        }

        @Override
        public long getItemId(int p) {
            if (p >= 0 && p < mTimers.size()) {
                return mTimers.get(p).mTimerId;
            }
            return 0;
        }

        protected int findTimerPositionById(int id) {
            for (int i = 0; i < mTimers.size(); i++) {
                TimerObj t = mTimers.get(i);
                if (t.mTimerId == id) {
                    return i;
                }
            }
            return -1;
        }

        public void removeTimer(TimerObj timerObj) {
            int position = findTimerPositionById(timerObj.mTimerId);
            if (position >= 0) {
                mTimers.remove(position);
                notifyDataSetChanged();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final TimerListItem v = (TimerListItem) mLayoutInflater.inflate(
                    R.layout.timer_list_item, parent, false /* attachToRoot */);
            final TimerObj o = getItem(position);
            o.mView = v;
            long timeLeft = o.updateTimeLeft(false);
            boolean drawRed = o.mState != TimerObj.STATE_RESTART;
            v.set(o.mOriginalLength, timeLeft, drawRed);
            v.setTime(timeLeft, true);
            switch (o.mState) {
                case TimerObj.STATE_RUNNING:
                    v.start();
                    break;
                case TimerObj.STATE_TIMESUP:
                    v.timesUp();
                    break;
                default:
                    break;
            }

            CircleButtonsLayout circleLayout =
                    (CircleButtonsLayout) v.findViewById(R.id.timer_circle);
            circleLayout.setCircleTimerViewIds(R.id.timer_time, R.id.reset_add, R.id.timer_label);

            ImageButton resetAddButton = (ImageButton) v.findViewById(R.id.reset_add);
            resetAddButton.setTag(new ClickAction(ClickAction.ACTION_PLUS_ONE, o));
            v.setResetAddButton(true, TimerFullScreenFragment.this);

            TextView label = (TextView) v.findViewById(R.id.timer_label);
            label.setText(o.mLabel);
            if (getActivity() instanceof DeskClock) {
                label.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onLabelPressed(o);
                    }
                });
            }  else if (TextUtils.isEmpty(o.mLabel)) {
                label.setVisibility(View.INVISIBLE);
            }

            return v;
        }

        @Override
        public int getItemColumnSpan(Object item, int position) {
            // This returns the width for a specified position. If we only have one item, have it
            // span all columns so that it's centered. Otherwise, all timers should just span one.
            if (getCount() == 1) {
                return mColumnCount;
            } else {
                return 1;
            }
        }

        public void addTimer(TimerObj t) {
            mTimers.add(0, t);
            sort();
        }

        public void onSaveInstanceState(Bundle outState) {
            TimerObj.putTimersInSharedPrefs(mPrefs, mTimers);
        }

        public void onRestoreInstanceState(Bundle outState) {
            TimerObj.getTimersFromSharedPrefs(mPrefs, mTimers);
            sort();
        }

        public void saveGlobalState() {
            TimerObj.putTimersInSharedPrefs(mPrefs, mTimers);
        }

        public void sort() {
            if (getCount() > 0) {
                Collections.sort(mTimers, mTimersCompare);
                notifyDataSetChanged();
            }
        }

        private final Comparator<TimerObj> mTimersCompare = new Comparator<TimerObj>() {
            static final int BUZZING = 0;
            static final int IN_USE = 1;
            static final int NOT_USED = 2;

            protected int getSection(TimerObj timerObj) {
                switch (timerObj.mState) {
                    case TimerObj.STATE_TIMESUP:
                        return BUZZING;
                    case TimerObj.STATE_RUNNING:
                    case TimerObj.STATE_STOPPED:
                        return IN_USE;
                    default:
                        return NOT_USED;
                }
            }

            @Override
            public int compare(TimerObj o1, TimerObj o2) {
                int section1 = getSection(o1);
                int section2 = getSection(o2);
                if (section1 != section2) {
                    return (section1 < section2) ? -1 : 1;
                } else if (section1 == BUZZING || section1 == IN_USE) {
                    return (o1.mTimeLeft < o2.mTimeLeft) ? -1 : 1;
                } else {
                    return (o1.mSetupLength < o2.mSetupLength) ? -1 : 1;
                }
            }
        };
    }

    private class TimesUpListAdapter extends TimersListAdapter {

        public TimesUpListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            // This adapter has a data subset and never updates entire database
            // Individual timers are updated in button handlers.
        }

        @Override
        public void saveGlobalState() {
            // This adapter has a data subset and never updates entire database
            // Individual timers are updated in button handlers.
        }

        @Override
        public void onRestoreInstanceState(Bundle outState) {
            // This adapter loads a subset
            TimerObj.getTimersFromSharedPrefs(mPrefs, mTimers, TimerObj.STATE_TIMESUP);

            if (getCount() == 0) {
                mOnEmptyListListener.onEmptyList();
            } else {
                Collections.sort(mTimers, new Comparator<TimerObj>() {
                    @Override
                    public int compare(TimerObj o1, TimerObj o2) {
                        return (o1.mTimeLeft < o2.mTimeLeft) ? -1 : 1;
                    }
                });
            }
        }
    }

    private final Runnable mClockTick = new Runnable() {
        boolean mVisible = true;
        final static int TIME_PERIOD_MS = 1000;
        final static int SPLIT = TIME_PERIOD_MS / 2;

        @Override
        public void run() {
            // Setup for blinking
            boolean visible = Utils.getTimeNow() % TIME_PERIOD_MS < SPLIT;
            boolean toggle = mVisible != visible;
            mVisible = visible;
            for (int i = 0; i < mAdapter.getCount(); i++) {
                TimerObj t = mAdapter.getItem(i);
                if (t.mState == TimerObj.STATE_RUNNING || t.mState == TimerObj.STATE_TIMESUP) {
                    long timeLeft = t.updateTimeLeft(false);
                    if (t.mView != null) {
                        t.mView.setTime(timeLeft, false);
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
            mTimersList.postDelayed(mClockTick, 20);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Cache instance data and consume in first call to setupPage()
        if (savedInstanceState != null) {
            mViewState = savedInstanceState;
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.timer_full_screen_fragment, container, false);

        // Handle arguments from parent
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(Timers.TIMESUP_MODE)) {
            if (bundle.getBoolean(Timers.TIMESUP_MODE, false)) {
                try {
                    mOnEmptyListListener = (OnEmptyListListener) getActivity();
                } catch (ClassCastException e) {
                    Log.wtf(TAG, getActivity().toString() + " must implement OnEmptyListListener");
                }
            }
        }

        mFab = (ImageButton) v.findViewById(R.id.fab);
        mTimersList = (StaggeredGridView) v.findViewById(R.id.timers_list);
        // For tablets in landscape, the count will be 2. All else will be 1.
        mColumnCount = getResources().getInteger(R.integer.timer_column_count);
        mTimersList.setColumnCount(mColumnCount);
        // Set this to true; otherwise adding new views to the end of the list won't cause
        // everything above it to be filled in correctly.
        mTimersList.setGuardAgainstJaggedEdges(true);

        mTimersListPage = v.findViewById(R.id.timers_list_page);
        mTimerSetup = (TimerSetupView) v.findViewById(R.id.timer_setup);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mNotificationManager = (NotificationManager)
                getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        return v;
    }

    @Override
    public void onDestroyView() {
        mViewState = new Bundle();
        saveViewState(mViewState);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        Intent newIntent = null;

        if (getActivity() instanceof DeskClock) {
            DeskClock activity = (DeskClock) getActivity();
            activity.registerPageChangedListener(this);
            newIntent = activity.getIntent();
        }
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        mAdapter = createAdapter(getActivity());
        mAdapter.onRestoreInstanceState(null);

        LayoutParams params;
        float dividerHeight = getResources().getDimension(R.dimen.timer_divider_height);
        if (getActivity() instanceof DeskClock) {
            // If this is a DeskClock fragment (i.e. not a FullScreenTimerAlert), add a footer to
            // the bottom of the list so that it can scroll underneath the bottom button bar.
            // StaggeredGridView doesn't support a footer view, but GridAdapter does, so this
            // can't happen until the Adapter itself is instantiated.
            View footerView = getActivity().getLayoutInflater().inflate(
                    R.layout.blank_footer_view, mTimersList, false);
            params = footerView.getLayoutParams();
            params.height -= dividerHeight;
            footerView.setLayoutParams(params);
            mAdapter.setFooterView(footerView);
        }

        if (mPrefs.getBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, false)) {
            // Clear the flag indicating the adapter is out of sync with the database.
            mPrefs.edit().putBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, false).apply();
        }

        mTimersList.setAdapter(mAdapter);
        setTimerListPosition(mAdapter.getCount());

        mLastVisibleView = null;   // Force a non animation setting of the view
        setPage();
        // View was hidden in onPause, make sure it is visible now.
        View v = getView();
        if (v != null) {
            getView().setVisibility(View.VISIBLE);
        }

        if (newIntent != null) {
            processIntent(newIntent);
        }

        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Animator revealAnimator = getRevealAnimator(mFab, Color.WHITE);
                revealAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        updateAllTimesUpTimers();
                    }
                });
                revealAnimator.start();
            }
        });
    }

    private void setTimerListPosition(int numTimers) {
        final LayoutParams timerListParams = mTimersList.getLayoutParams();
        if (numTimers <= 1) {
            // Set a fixed height so we can center it
            final Resources resources = getResources();
            timerListParams.height = (int) resources.getDimension(R.dimen.circle_size)
                    + (int) resources.getDimension(R.dimen.timer_list_padding_bottom);

            // Disable scrolling
            mTimersList.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    return (event.getAction() == MotionEvent.ACTION_MOVE);
                }
            });
        } else {
            timerListParams.height = LayoutParams.MATCH_PARENT;
            mTimersList.setOnTouchListener(null);
        }
    }

    private Animator getRevealAnimator(View source, int revealColor) {
        final ViewGroup containerView = (ViewGroup) source.getRootView()
                .findViewById(android.R.id.content);

        final Rect sourceBounds = new Rect(0, 0, source.getHeight(), source.getWidth());
        containerView.offsetDescendantRectToMyCoords(source, sourceBounds);

        final int centerX = sourceBounds.centerX();
        final int centerY = sourceBounds.centerY();

        final int xMax = Math.max(centerX, containerView.getWidth() - centerX);
        final int yMax = Math.max(centerY, containerView.getHeight() - centerY);

        final float startRadius = Math.max(sourceBounds.width(), sourceBounds.height()) / 2.0f;
        final float endRadius = (float) Math.sqrt(xMax * xMax + yMax * yMax);

        final CircleView revealView = new CircleView(source.getContext())
                .setCenterX(centerX)
                .setCenterY(centerY)
                .setFillColor(revealColor);
        containerView.addView(revealView);

        final Animator revealAnimator = ObjectAnimator.ofFloat(
                revealView, CircleView.RADIUS, startRadius, endRadius);
        revealAnimator.setInterpolator(PathInterpolatorCompat.create(0.0f, 0.0f, 0.2f, 1.0f));

        final ValueAnimator fadeAnimator = ObjectAnimator.ofFloat(revealView, View.ALPHA, 0.0f);
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                containerView.removeView(revealView);
            }
        });

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(TimerFragment.ANIMATION_TIME_MILLIS);
        animatorSet.playSequentially(revealAnimator, fadeAnimator);

        return revealAnimator;
    }

    @Override
    public void onPause() {
        if (getActivity() instanceof DeskClock) {
            ((DeskClock) getActivity()).unregisterPageChangedListener(this);
        }
        super.onPause();
        stopClockTicks();
        if (mAdapter != null) {
            mAdapter.saveGlobalState();
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        // This is called because the lock screen was activated, the window stay
        // active under it and when we unlock the screen, we see the old time for
        // a fraction of a second.
        View v = getView();
        if (v != null) {
            v.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onPageChanged(int page) {
        if (page == DeskClock.TIMER_TAB_INDEX && mAdapter != null) {
            mAdapter.sort();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            mAdapter.onSaveInstanceState(outState);
        }
        if (mTimerSetup != null) {
            saveViewState(outState);
        } else if (mViewState != null) {
            outState.putAll(mViewState);
        }
    }

    private void saveViewState(Bundle outState) {
        mTimerSetup.saveEntryState(outState, KEY_ENTRY_STATE);
    }

    public void setPage() {
        boolean switchToSetupView;
        if (mViewState != null) {
            switchToSetupView = false;
            mTimerSetup.restoreEntryState(mViewState, KEY_ENTRY_STATE);
            mViewState = null;
        } else {
            switchToSetupView = mAdapter.getCount() == 0;
        }
        if (switchToSetupView) {
            gotoSetupView();
        } else {
            gotoTimersView();
        }
    }

    private void resetTimer(TimerObj t) {
        t.setState(TimerObj.STATE_RESTART);
        t.mTimeLeft = t.mOriginalLength = t.mSetupLength;
        // when multiple timers are firing, some timers will be off-screen and they will not
        // have Fragment instances unless user scrolls down further. t.mView is null in this case.
        if (t.mView != null) {
            t.mView.stop();
            t.mView.setTime(t.mTimeLeft, false);
            t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
        }
        updateTimersState(t, Timers.RESET_TIMER);
        Events.sendTimerEvent(R.string.action_reset, R.string.label_deskclock);
    }

    public void updateAllTimesUpTimers() {
        boolean notifyChange = false;
        //  To avoid race conditions where a timer was dismissed and it is still in the timers list
        // and can be picked again, create a temporary list of timers to be removed first and
        // then removed them one by one
        LinkedList<TimerObj> timesupTimers = new LinkedList<>();
        for (int i = 0; i < mAdapter.getCount(); i++) {
            TimerObj timerObj = mAdapter.getItem(i);
            if (timerObj.mState == TimerObj.STATE_TIMESUP) {
                timesupTimers.addFirst(timerObj);
                notifyChange = true;
            }
        }

        while (timesupTimers.size() > 0) {
            final TimerObj t = timesupTimers.remove();
            onStopButtonPressed(t);
        }

        if (notifyChange) {
            mPrefs.edit().putBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, true).apply();
        }
    }

    private void gotoSetupView() {
        if (mLastVisibleView == null || mLastVisibleView.getId() == R.id.timer_setup) {
            mTimerSetup.setVisibility(View.VISIBLE);
            mTimerSetup.setScaleX(1f);
            mTimersListPage.setVisibility(View.GONE);
        } else {
            // Animate
            ObjectAnimator a = ObjectAnimator.ofFloat(mTimersListPage, View.SCALE_X, 1f, 0f);
            a.setInterpolator(new AccelerateInterpolator());
            a.setDuration(125);
            a.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mTimersListPage.setVisibility(View.GONE);
                    mTimerSetup.setScaleX(0);
                    mTimerSetup.setVisibility(View.VISIBLE);
                    ObjectAnimator b = ObjectAnimator.ofFloat(mTimerSetup, View.SCALE_X, 0f, 1f);
                    b.setInterpolator(new DecelerateInterpolator());
                    b.setDuration(225);
                    b.start();
                }
            });
            a.start();

        }
        stopClockTicks();
        mTimerSetup.updateDeleteButtonAndDivider();
        mLastVisibleView = mTimerSetup;
    }

    private void gotoTimersView() {
        if (mLastVisibleView == null || mLastVisibleView.getId() == R.id.timers_list_page) {
            mTimerSetup.setVisibility(View.GONE);
            mTimersListPage.setVisibility(View.VISIBLE);
            mTimersListPage.setScaleX(1f);
        } else {
            // Animate
            ObjectAnimator a = ObjectAnimator.ofFloat(mTimerSetup, View.SCALE_X, 1f, 0f);
            a.setInterpolator(new AccelerateInterpolator());
            a.setDuration(125);
            a.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mTimerSetup.setVisibility(View.GONE);
                    mTimersListPage.setScaleX(0);
                    mTimersListPage.setVisibility(View.VISIBLE);
                    ObjectAnimator b =
                            ObjectAnimator.ofFloat(mTimersListPage, View.SCALE_X, 0f, 1f);
                    b.setInterpolator(new DecelerateInterpolator());
                    b.setDuration(225);
                    b.start();
                }
            });
            a.start();
        }
        startClockTicks();
        mLastVisibleView = mTimersListPage;
    }

    @Override
    public void onClick(View v) {
        ClickAction tag = (ClickAction) v.getTag();
        onClickHelper(tag);
    }

    private void onClickHelper(ClickAction clickAction) {
        switch (clickAction.mAction) {
            case ClickAction.ACTION_DELETE:
                final TimerObj t = clickAction.mTimer;
                if (t.mState == TimerObj.STATE_TIMESUP) {
                    cancelTimerNotification(t.mTimerId);
                }
                // Tell receiver the timer was deleted.
                // It will stop all activity related to the
                // timer
                t.setState(TimerObj.STATE_DELETED);
                updateTimersState(t, Timers.DELETE_TIMER);

                Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
                break;
            case ClickAction.ACTION_PLUS_ONE:
                onPlusOneButtonPressed(clickAction.mTimer);
                break;
            case ClickAction.ACTION_STOP:
                onStopButtonPressed(clickAction.mTimer);
                break;
            default:
                break;
        }
    }

    private void onPlusOneButtonPressed(TimerObj t) {
        switch (t.mState) {
            case TimerObj.STATE_RUNNING:
                t.addTime(TimerObj.MINUTE_IN_MILLIS);
                long timeLeft = t.updateTimeLeft(false);
                t.mView.setTime(timeLeft, false);
                t.mView.setLength(timeLeft);
                mAdapter.notifyDataSetChanged();
                updateTimersState(t, Timers.TIMER_UPDATE);

                Events.sendTimerEvent(R.string.action_add_minute, R.string.label_deskclock);
                break;
            case TimerObj.STATE_TIMESUP:
                // +1 min when the time is up will restart the timer with 1 minute left.
                t.setState(TimerObj.STATE_RUNNING);
                t.mStartTime = Utils.getTimeNow();
                t.mTimeLeft = t.mOriginalLength = TimerObj.MINUTE_IN_MILLIS;
                updateTimersState(t, Timers.RESET_TIMER);
                Events.sendTimerEvent(R.string.action_add_minute, R.string.label_deskclock);

                updateTimersState(t, Timers.START_TIMER);
                Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);

                updateTimesUpMode(t);
                cancelTimerNotification(t.mTimerId);
                break;
            case TimerObj.STATE_STOPPED:
                t.setState(TimerObj.STATE_RESTART);
                t.mTimeLeft = t.mOriginalLength = t.mSetupLength;
                t.mView.stop();
                t.mView.setTime(t.mTimeLeft, false);
                t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
                updateTimersState(t, Timers.RESET_TIMER);

                Events.sendTimerEvent(R.string.action_reset, R.string.label_deskclock);
                break;
            default:
                break;
        }
    }

    private void onStopButtonPressed(TimerObj t) {
        switch (t.mState) {
            case TimerObj.STATE_RUNNING:
                // Stop timer and save the remaining time of the timer
                t.setState(TimerObj.STATE_STOPPED);
                t.mView.pause();
                t.updateTimeLeft(true);
                updateTimersState(t, Timers.STOP_TIMER);

                Events.sendTimerEvent(R.string.action_stop, R.string.label_deskclock);
                break;
            case TimerObj.STATE_STOPPED:
                // Reset the remaining time and continue timer
                t.setState(TimerObj.STATE_RUNNING);
                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                t.mView.start();
                updateTimersState(t, Timers.START_TIMER);

                Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
                break;
            case TimerObj.STATE_TIMESUP:
                if (t.mDeleteAfterUse) {
                    cancelTimerNotification(t.mTimerId);
                    // Tell receiver the timer was deleted.
                    // It will stop all activity related to the
                    // timer
                    t.setState(TimerObj.STATE_DELETED);
                    updateTimersState(t, Timers.DELETE_TIMER);
                    Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
                } else {
                    resetTimer(t);
                }
                break;
            case TimerObj.STATE_RESTART:
                t.setState(TimerObj.STATE_RUNNING);
                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                t.mView.start();
                updateTimersState(t, Timers.START_TIMER);
                Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
                break;
            default:
                break;
        }
    }

    private void onLabelPressed(TimerObj t) {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag("label_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        final LabelDialogFragment newFragment =
                LabelDialogFragment.newInstance(t, t.mLabel, getTag());
        newFragment.show(ft, "label_dialog");
    }

    // Starts the ticks that animate the timers.
    private void startClockTicks() {
        mTimersList.postDelayed(mClockTick, 20);
        mTicking = true;
    }

    // Stops the ticks that animate the timers.
    private void stopClockTicks() {
        if (mTicking) {
            mTimersList.removeCallbacks(mClockTick);
            mTicking = false;
        }
    }

    private void updateTimersState(TimerObj t, String action) {
        if (Timers.DELETE_TIMER.equals(action)) {
            LogUtils.e("~~ update timer state");
            t.deleteFromSharedPref(mPrefs);
        } else {
            t.writeToSharedPref(mPrefs);
        }
        Intent i = new Intent();
        i.setAction(action);
        i.putExtra(Timers.TIMER_INTENT_EXTRA, t.mTimerId);
        // Make sure the receiver is getting the intent ASAP.
        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        getActivity().sendBroadcast(i);
    }

    private void cancelTimerNotification(int timerId) {
        mNotificationManager.cancel(timerId);
    }

    private void updateTimesUpMode(TimerObj timerObj) {
        if (mOnEmptyListListener != null && timerObj.mState != TimerObj.STATE_TIMESUP) {
            mAdapter.removeTimer(timerObj);
            if (mAdapter.getCount() == 0) {
                mOnEmptyListListener.onEmptyList();
            } else {
                mOnEmptyListListener.onListChanged();
            }
        }
    }

    public void restartAdapter() {
        mAdapter = createAdapter(getActivity());
        mAdapter.onRestoreInstanceState(null);
    }

    // Process extras that were sent to the app and were intended for the timer
    // fragment
    public void processIntent(Intent intent) {
        // switch to timer setup view
        if (intent.getBooleanExtra(GOTO_SETUP_VIEW, false)) {
            gotoSetupView();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(mPrefs)) {
            if (key.equals(Timers.REFRESH_UI_WITH_LATEST_DATA) && prefs.getBoolean(key, false)) {
                // Clear the flag forcing a fresh of the adapter to reflect changes in the database.
                mPrefs.edit().putBoolean(key, false).apply();
                mAdapter = createAdapter(getActivity());
                mAdapter.onRestoreInstanceState(null);
                mTimersList.setAdapter(mAdapter);
            }
        }
    }

    @Override
    public void onFabClick(View view) {
        if (mLastVisibleView != mTimersListPage) {
            // New timer create if timer length is not zero
            // Create a new timer object to track the timer and
            // switch to the timers view.
            int timerLength = mTimerSetup.getTime();
            if (timerLength == 0) {
                return;
            }
            TimerObj t = new TimerObj(timerLength * DateUtils.SECOND_IN_MILLIS, getActivity());
            t.setState(TimerObj.STATE_RUNNING);
            mAdapter.addTimer(t);
            updateTimersState(t, Timers.START_TIMER);
            Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
            gotoTimersView();
            mTimerSetup.reset(); // Make sure the setup is cleared for next time

            mTimersList.setFirstPositionAndOffsets(
                    mAdapter.findTimerPositionById(t.mTimerId), 0);
        } else {
            mTimerSetup.reset();
            gotoSetupView();
        }
    }
}
