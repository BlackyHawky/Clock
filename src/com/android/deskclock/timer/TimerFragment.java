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
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.deskclock.CircleButtonsLayout;
import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClock.OnTapListener;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.R;
import com.android.deskclock.TimerSetupView;
import com.android.deskclock.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import com.android.deskclock.widget.sgv.SgvAnimationHelper.AnimationIn;
import com.android.deskclock.widget.sgv.SgvAnimationHelper.AnimationOut;
import com.android.deskclock.widget.sgv.StaggeredGridView;
import com.android.deskclock.widget.sgv.GridAdapter;

public class TimerFragment extends DeskClockFragment
        implements OnClickListener, OnSharedPreferenceChangeListener {

    private static final String TAG = "TimerFragment";
    private static final String KEY_SETUP_SELECTED = "_setup_selected";
    private static final String KEY_ENTRY_STATE = "entry_state";
    public static final String GOTO_SETUP_VIEW = "deskclock.timers.gotosetup";

    private Bundle mViewState = null;
    private StaggeredGridView mTimersList;
    private View mTimersListPage;
    private int mColumnCount;
    private Button mCancel, mStart;
    private View mSeperator;
    private ImageButton mAddTimer;
    private View mTimerFooter;
    private TimerSetupView mTimerSetup;
    private TimersListAdapter mAdapter;
    private boolean mTicking = false;
    private SharedPreferences mPrefs;
    private NotificationManager mNotificationManager;
    private OnEmptyListListener mOnEmptyListListener;
    private View mLastVisibleView = null;  // used to decide if to set the view or animate to it.

    public TimerFragment() {
    }

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
        public void onEmptyList();
        public void onListChanged();
    }

    TimersListAdapter createAdapter(Context context, SharedPreferences prefs) {
        if (mOnEmptyListListener == null) {
            return new TimersListAdapter(context, prefs);
        } else {
            return new TimesUpListAdapter(context, prefs);
        }
    }

    class TimersListAdapter extends GridAdapter {

        ArrayList<TimerObj> mTimers = new ArrayList<TimerObj> ();
        Context mContext;
        SharedPreferences mmPrefs;

        public TimersListAdapter(Context context, SharedPreferences prefs) {
            mContext = context;
            mmPrefs = prefs;
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

        public void deleteTimer(int id) {
            for (int i = 0; i < mTimers.size(); i++) {
                TimerObj t = mTimers.get(i);

                if (t.mTimerId == id) {
                    if (t.mView != null) {
                        ((TimerListItem) t.mView).stop();
                    }
                    t.deleteFromSharedPref(mmPrefs);
                    mTimers.remove(i);
                    if (mTimers.size() == 1 && mColumnCount > 1) {
                        // If we're going from two timers to one (in the same row), we don't want to
                        // animate the translation because we're changing the layout params span
                        // from 1 to 2, and the animation doesn't handle that very well. So instead,
                        // just fade out and in.
                        mTimersList.setAnimationMode(AnimationIn.FADE, AnimationOut.FADE);
                    } else {
                        mTimersList.setAnimationMode(
                                AnimationIn.FLY_IN_NEW_VIEWS, AnimationOut.FADE);
                    }
                    notifyDataSetChanged();
                    return;
                }
            }
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
            TimerListItem v = new TimerListItem (mContext); // TODO: Need to recycle convertView.

            final TimerObj o = (TimerObj)getItem(position);
            o.mView = v;
            long timeLeft =  o.updateTimeLeft(false);
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
            case TimerObj.STATE_DONE:
                v.done();
                break;
            default:
                break;
            }

            // Timer text serves as a virtual start/stop button.
            final CountingTimerView countingTimerView = (CountingTimerView)
                    v.findViewById(R.id.timer_time_text);
            countingTimerView.registerVirtualButtonAction(new Runnable() {
                @Override
                public void run() {
                    TimerFragment.this.onClickHelper(
                            new ClickAction(ClickAction.ACTION_STOP, o));
                }
            });

            ImageButton delete = (ImageButton)v.findViewById(R.id.timer_delete);
            delete.setOnClickListener(TimerFragment.this);
            delete.setTag(new ClickAction(ClickAction.ACTION_DELETE, o));
            ImageButton leftButton = (ImageButton)v. findViewById(R.id.timer_plus_one);
            leftButton.setOnClickListener(TimerFragment.this);
            leftButton.setTag(new ClickAction(ClickAction.ACTION_PLUS_ONE, o));
            TextView stop = (TextView)v. findViewById(R.id.timer_stop);
            stop.setTag(new ClickAction(ClickAction.ACTION_STOP, o));
            TimerFragment.this.setTimerButtons(o);

            v.setBackgroundColor(getResources().getColor(R.color.blackish));
            countingTimerView.registerStopTextView(stop);
            CircleButtonsLayout circleLayout =
                    (CircleButtonsLayout)v.findViewById(R.id.timer_circle);
            circleLayout.setCircleTimerViewIds(
                    R.id.timer_time, R.id.timer_plus_one, R.id.timer_delete, R.id.timer_stop,
                    R.dimen.plusone_reset_button_padding, R.dimen.delete_button_padding,
                    R.id.timer_label, R.id.timer_label_text);

            FrameLayout label = (FrameLayout)v. findViewById(R.id.timer_label);
            ImageButton labelIcon = (ImageButton)v. findViewById(R.id.timer_label_icon);
            TextView labelText = (TextView)v. findViewById(R.id.timer_label_text);
            if (o.mLabel.equals("")) {
                labelText.setVisibility(View.GONE);
                labelIcon.setVisibility(View.VISIBLE);
            } else {
                labelText.setText(o.mLabel);
                labelText.setVisibility(View.VISIBLE);
                labelIcon.setVisibility(View.GONE);
            }
            if (getActivity() instanceof DeskClock) {
                label.setOnTouchListener(new OnTapListener(getActivity(), labelText) {
                    @Override
                    protected void processClick(View v) {
                        onLabelPressed(o);
                    }
                });
            } else {
                labelIcon.setVisibility(View.INVISIBLE);
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
            TimerObj.putTimersInSharedPrefs(mmPrefs, mTimers);
        }

        public void onRestoreInstanceState(Bundle outState) {
            TimerObj.getTimersFromSharedPrefs(mmPrefs, mTimers);
            sort();
        }

        public void saveGlobalState() {
            TimerObj.putTimersInSharedPrefs(mmPrefs, mTimers);
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

    class TimesUpListAdapter extends TimersListAdapter {

        public TimesUpListAdapter(Context context, SharedPreferences prefs) {
            super(context, prefs);
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
            TimerObj.getTimersFromSharedPrefs(mmPrefs, mTimers, TimerObj.STATE_TIMESUP);

            if (getCount() == 0) {
                mOnEmptyListListener.onEmptyList();
            } else {
                Collections.sort(mTimers, new Comparator<TimerObj>() {
                    @Override
                    public int compare(TimerObj o1, TimerObj o2) {
                        return (o1.mTimeLeft <  o2.mTimeLeft) ? -1 : 1;
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
            for (int i = 0; i < mAdapter.getCount(); i ++) {
                TimerObj t = mAdapter.getItem(i);
                if (t.mState == TimerObj.STATE_RUNNING || t.mState == TimerObj.STATE_TIMESUP) {
                    long timeLeft = t.updateTimeLeft(false);
                    if (t.mView != null) {
                        ((TimerListItem)(t.mView)).setTime(timeLeft, false);
                        // Update button every 1/2 second
                        if (toggle) {
                            ImageButton leftButton = (ImageButton)
                                  t.mView.findViewById(R.id.timer_plus_one);
                            leftButton.setEnabled(canAddMinute(t));
                        }
                    }
                }
                if (t.mTimeLeft <= 0 && t.mState != TimerObj.STATE_DONE
                        && t.mState != TimerObj.STATE_RESTART) {
                    t.mState = TimerObj.STATE_TIMESUP;
                    TimerFragment.this.setTimerButtons(t);
                    if (t.mView != null) {
                        ((TimerListItem)(t.mView)).timesUp();
                    }
                }

                // The blinking
                if (toggle && t.mView != null) {
                    if (t.mState == TimerObj.STATE_TIMESUP) {
                        ((TimerListItem)(t.mView)).setCircleBlink(mVisible);
                    }
                    if (t.mState == TimerObj.STATE_STOPPED) {
                        ((TimerListItem)(t.mView)).setTextBlink(mVisible);
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
        View v = inflater.inflate(R.layout.timer_fragment, container, false);

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

        mTimersList = (StaggeredGridView) v.findViewById(R.id.timers_list);
        // For tablets in landscape, the count will be 2. All else will be 1.
        mColumnCount = getResources().getInteger(R.integer.timer_column_count);
        mTimersList.setColumnCount(mColumnCount);
        // Set this to true; otherwise adding new views to the end of the list won't cause
        // everything above it to be filled in correctly.
        mTimersList.setGuardAgainstJaggedEdges(true);

        mTimersListPage = v.findViewById(R.id.timers_list_page);
        mTimerSetup = (TimerSetupView)v.findViewById(R.id.timer_setup);
        mSeperator = v.findViewById(R.id.timer_button_sep);
        mCancel = (Button)v.findViewById(R.id.timer_cancel);
        mCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapter.getCount() != 0) {
                    gotoTimersView();
                }
            }
        });
        mStart = (Button)v.findViewById(R.id.timer_start);
        mStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // New timer create if timer length is not zero
                // Create a new timer object to track the timer and
                // switch to the timers view.
                int timerLength = mTimerSetup.getTime();
                if (timerLength == 0) {
                    return;
                }
                TimerObj t = new TimerObj(timerLength * 1000);
                t.mState = TimerObj.STATE_RUNNING;
                mAdapter.addTimer(t);
                updateTimersState(t, Timers.START_TIMER);
                gotoTimersView();
                mTimerSetup.reset(); // Make sure the setup is cleared for next time

                mTimersList.setFirstPositionAndOffsets(
                        mAdapter.findTimerPositionById(t.mTimerId), 0);
            }

        });
        mTimerSetup.registerStartButton(mStart);
        mAddTimer = (ImageButton)v.findViewById(R.id.timer_add_timer);
        mAddTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimerSetup.reset();
                gotoSetupView();
            }

        });

        // Put it on the right for landscape, left for portrait.
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) mAddTimer.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutParams.gravity = Gravity.END;
        } else {
            layoutParams.gravity = Gravity.CENTER;
        }
        mAddTimer.setLayoutParams(layoutParams);

        mTimerFooter = v.findViewById(R.id.timer_footer);
        mTimerFooter.setVisibility(mOnEmptyListListener == null ? View.VISIBLE : View.GONE);
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

        mAdapter = createAdapter(getActivity(), mPrefs);
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
            footerView.setBackgroundResource(R.color.blackish);
            mAdapter.setFooterView(footerView);
        }

        if (mPrefs.getBoolean(Timers.FROM_NOTIFICATION, false)) {
            // Clear the flag set in the notification because the adapter was just
            // created and is thus in sync with the database
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(Timers.FROM_NOTIFICATION, false);
            editor.apply();
        }
        if (mPrefs.getBoolean(Timers.FROM_ALERT, false)) {
            // Clear the flag set in the alert because the adapter was just
            // created and is thus in sync with the database
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(Timers.FROM_ALERT, false);
            editor.apply();
        }

        mTimersList.setAdapter(mAdapter);
        if (mAdapter.getCount() == 0) {
            mCancel.setVisibility(View.GONE);
            mSeperator.setVisibility(View.GONE);
        }
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
    }

    @Override
    public void onPause() {
        if (getActivity() instanceof DeskClock) {
            ((DeskClock)getActivity()).unregisterPageChangedListener(this);
        }
        super.onPause();
        stopClockTicks();
        if (mAdapter != null) {
            mAdapter.saveGlobalState ();
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
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            mAdapter.onSaveInstanceState (outState);
        }
        if (mTimerSetup != null) {
            saveViewState(outState);
        } else if (mViewState != null) {
            outState.putAll(mViewState);
        }
    }

    private void saveViewState(Bundle outState) {
        outState.putBoolean(KEY_SETUP_SELECTED, mTimerSetup.getVisibility() == View.VISIBLE);
        mTimerSetup.saveEntryState(outState, KEY_ENTRY_STATE);
    }

    public void setPage() {
        boolean switchToSetupView;
        if (mViewState != null) {
            switchToSetupView = mViewState.getBoolean(KEY_SETUP_SELECTED, false);
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

    public void stopAllTimesUpTimers() {
        boolean notifyChange = false;
        //  To avoid race conditions where a timer was dismissed and it is still in the timers list
        // and can be picked again, create a temporary list of timers to be removed first and
        // then removed them one by one
        LinkedList<TimerObj> timesupTimers = new LinkedList<TimerObj>();
        for (int i = 0; i  < mAdapter.getCount(); i ++) {
            TimerObj timerObj = mAdapter.getItem(i);
            if (timerObj.mState == TimerObj.STATE_TIMESUP) {
                timesupTimers.addFirst(timerObj);
                notifyChange = true;
            }
        }

        while (timesupTimers.size() > 0) {
            onStopButtonPressed(timesupTimers.remove());
        }

        if (notifyChange) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(Timers.FROM_ALERT, true);
            editor.apply();
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
        if (mAdapter.getCount() == 0) {
            mCancel.setVisibility(View.GONE);
            mSeperator.setVisibility(View.GONE);
        } else {
            mSeperator.setVisibility(View.VISIBLE);
            mCancel.setVisibility(View.VISIBLE);
        }
        mTimerSetup.updateStartButton();
        mTimerSetup.updateDeleteButton();
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
                t.mState = TimerObj.STATE_DELETED;
                updateTimersState(t, Timers.DELETE_TIMER);
                break;
            case ClickAction.ACTION_PLUS_ONE:
                onPlusOneButtonPressed(clickAction.mTimer);
                setTimerButtons(clickAction.mTimer);
                break;
            case ClickAction.ACTION_STOP:
                onStopButtonPressed(clickAction.mTimer);
                setTimerButtons(clickAction.mTimer);
                break;
            default:
                break;
        }
    }

    private void onPlusOneButtonPressed(TimerObj t) {
        switch(t.mState) {
            case TimerObj.STATE_RUNNING:
                 t.addTime(TimerObj.MINUTE_IN_MILLIS);
                 long timeLeft = t.updateTimeLeft(false);
                 ((TimerListItem)(t.mView)).setTime(timeLeft, false);
                 ((TimerListItem)(t.mView)).setLength(timeLeft);
                 mAdapter.notifyDataSetChanged();
                 updateTimersState(t, Timers.TIMER_UPDATE);
                break;
            case TimerObj.STATE_TIMESUP:
                // +1 min when the time is up will restart the timer with 1 minute left.
                t.mState = TimerObj.STATE_RUNNING;
                t.mStartTime = Utils.getTimeNow();
                t.mTimeLeft = t. mOriginalLength = TimerObj.MINUTE_IN_MILLIS;
                ((TimerListItem)t.mView).setTime(t.mTimeLeft, false);
                ((TimerListItem)t.mView).set(t.mOriginalLength, t.mTimeLeft, true);
                ((TimerListItem) t.mView).start();
                updateTimersState(t, Timers.TIMER_RESET);
                updateTimersState(t, Timers.START_TIMER);
                updateTimesUpMode(t);
                cancelTimerNotification(t.mTimerId);
                break;
            case TimerObj.STATE_STOPPED:
            case TimerObj.STATE_DONE:
                t.mState = TimerObj.STATE_RESTART;
                t.mTimeLeft = t. mOriginalLength = t.mSetupLength;
                ((TimerListItem)t.mView).stop();
                ((TimerListItem)t.mView).setTime(t.mTimeLeft, false);
                ((TimerListItem)t.mView).set(t.mOriginalLength, t.mTimeLeft, false);
                updateTimersState(t, Timers.TIMER_RESET);
                break;
            default:
                break;
        }
    }

    private void onStopButtonPressed(TimerObj t) {
        switch(t.mState) {
            case TimerObj.STATE_RUNNING:
                // Stop timer and save the remaining time of the timer
                t.mState = TimerObj.STATE_STOPPED;
                ((TimerListItem) t.mView).pause();
                t.updateTimeLeft(true);
                updateTimersState(t, Timers.TIMER_STOP);
                break;
            case TimerObj.STATE_STOPPED:
                // Reset the remaining time and continue timer
                t.mState = TimerObj.STATE_RUNNING;
                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                ((TimerListItem) t.mView).start();
                updateTimersState(t, Timers.START_TIMER);
                break;
            case TimerObj.STATE_TIMESUP:
                if (t.mDeleteAfterUse) {
                    cancelTimerNotification(t.mTimerId);
                    // Tell receiver the timer was deleted.
                    // It will stop all activity related to the
                    // timer
                    t.mState = TimerObj.STATE_DELETED;
                    updateTimersState(t, Timers.DELETE_TIMER);
                } else {
                    t.mState = TimerObj.STATE_DONE;
                    // Used in a context where the timer could be off-screen and without a view
                    if (t.mView != null) {
                        ((TimerListItem) t.mView).done();
                    }
                    updateTimersState(t, Timers.TIMER_DONE);
                    cancelTimerNotification(t.mTimerId);
                    updateTimesUpMode(t);
                }
                break;
            case TimerObj.STATE_DONE:
                break;
            case TimerObj.STATE_RESTART:
                t.mState = TimerObj.STATE_RUNNING;
                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                ((TimerListItem) t.mView).start();
                updateTimersState(t, Timers.START_TIMER);
                break;
            default:
                break;
        }
    }

    private void deleteTimer(TimerObj t) {
        mAdapter.deleteTimer(t.mTimerId);
        mTimersList.setSelectionToTop();
        if (mAdapter.getCount() == 0) {
            if (mOnEmptyListListener == null) {
                mTimerSetup.reset();
                gotoSetupView();
            } else {
                mOnEmptyListListener.onEmptyList();
            }
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

    public void setLabel(TimerObj timer, String label) {
        mAdapter.getItem(mAdapter.findTimerPositionById(timer.mTimerId)).mLabel = label;
        updateTimersState(timer, Timers.TIMER_UPDATE);
        // Make sure the new label is visible.
        mAdapter.notifyDataSetChanged();
    }

    private void setTimerButtons(TimerObj t) {
        Context a = getActivity();
        if (a == null || t == null || t.mView == null) {
            return;
        }
        ImageButton leftButton = (ImageButton) t.mView.findViewById(R.id.timer_plus_one);
        CountingTimerView countingTimerView = (CountingTimerView)
                t.mView.findViewById(R.id.timer_time_text);
        TextView stop = (TextView) t.mView.findViewById(R.id.timer_stop);
        ImageButton delete = (ImageButton) t.mView.findViewById(R.id.timer_delete);
        // Make sure the delete button is visible in case the view is recycled.
        delete.setVisibility(View.VISIBLE);

        Resources r = a.getResources();
        switch (t.mState) {
            case TimerObj.STATE_RUNNING:
                // left button is +1m
                leftButton.setVisibility(View.VISIBLE);
                leftButton.setContentDescription(r.getString(R.string.timer_plus_one));
                leftButton.setImageResource(R.drawable.ic_plusone);
                leftButton.setEnabled(canAddMinute(t));
                stop.setVisibility(View.VISIBLE);
                stop.setContentDescription(r.getString(R.string.timer_stop));
                stop.setText(R.string.timer_stop);
                stop.setTextColor(getResources().getColor(R.color.clock_white));
                countingTimerView.setVirtualButtonEnabled(true);
                break;
            case TimerObj.STATE_STOPPED:
                // left button is reset
                leftButton.setVisibility(View.VISIBLE);
                leftButton.setContentDescription(r.getString(R.string.timer_reset));
                leftButton.setImageResource(R.drawable.ic_reset);
                leftButton.setEnabled(true);
                stop.setVisibility(View.VISIBLE);
                stop.setContentDescription(r.getString(R.string.timer_start));
                stop.setText(R.string.timer_start);
                stop.setTextColor(getResources().getColor(R.color.clock_white));
                countingTimerView.setVirtualButtonEnabled(true);
                break;
            case TimerObj.STATE_TIMESUP:
                // left button is +1m
                leftButton.setVisibility(View.VISIBLE);
                leftButton.setContentDescription(r.getString(R.string.timer_plus_one));
                leftButton.setImageResource(R.drawable.ic_plusone);
                leftButton.setEnabled(true);
                stop.setVisibility(View.VISIBLE);
                stop.setContentDescription(r.getString(R.string.timer_stop));
                // If the timer is deleted after use , show "done" instead of "stop" on the button
                // and hide the delete button since pressing done will delete the timer
                stop.setText(t.mDeleteAfterUse ? R.string.timer_done : R.string.timer_stop);
                stop.setTextColor(getResources().getColor(R.color.clock_white));
                delete.setVisibility(t.mDeleteAfterUse ? View.INVISIBLE : View.VISIBLE);
                countingTimerView.setVirtualButtonEnabled(true);
                break;
            case TimerObj.STATE_DONE:
                // left button is reset
                leftButton.setVisibility(View.VISIBLE);
                leftButton.setContentDescription(r.getString(R.string.timer_reset));
                leftButton.setImageResource(R.drawable.ic_reset);
                leftButton.setEnabled(true);
                stop.setVisibility(View.INVISIBLE);
                countingTimerView.setVirtualButtonEnabled(false);
                break;
            case TimerObj.STATE_RESTART:
                leftButton.setVisibility(View.INVISIBLE);
                leftButton.setEnabled(true);
                stop.setVisibility(View.VISIBLE);
                stop.setContentDescription(r.getString(R.string.timer_start));
                stop.setText(R.string.timer_start);
                stop.setTextColor(getResources().getColor(R.color.clock_white));
                countingTimerView.setVirtualButtonEnabled(true);
                break;
            default:
                break;
        }
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

    private boolean canAddMinute(TimerObj t) {
        return TimerObj.MAX_TIMER_LENGTH - t.mTimeLeft > TimerObj.MINUTE_IN_MILLIS ? true : false;
    }

    private void updateTimersState(TimerObj t, String action) {
        if (Timers.DELETE_TIMER.equals(action)) {
            deleteTimer(t);
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
        mAdapter = createAdapter(getActivity(), mPrefs);
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
            if ((key.equals(Timers.FROM_ALERT) && prefs.getBoolean(Timers.FROM_ALERT, false))
                    || (key.equals(Timers.FROM_NOTIFICATION)
                    && prefs.getBoolean(Timers.FROM_NOTIFICATION, false))) {
                // The data-changed flag was set in the alert or notification so the adapter needs
                // to re-sync with the database
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(key, false);
                editor.apply();
                mAdapter = createAdapter(getActivity(), mPrefs);
                mAdapter.onRestoreInstanceState(null);
                mTimersList.setAdapter(mAdapter);
            }
        }
    }
}
