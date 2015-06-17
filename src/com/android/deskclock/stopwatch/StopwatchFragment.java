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

import android.animation.LayoutTransition;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.deskclock.CircleButtonsLayout;
import com.android.deskclock.CircleTimerView;
import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.HandleDeskClockApiCalls;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;
import com.android.deskclock.timer.CountingTimerView;

import java.util.ArrayList;

public class StopwatchFragment extends DeskClockFragment
        implements OnSharedPreferenceChangeListener {
    private static final boolean DEBUG = false;

    private static final String TAG = "StopwatchFragment";
    private static final int STOPWATCH_REFRESH_INTERVAL_MILLIS = 25;
    // Lower the refresh rate in accessibility mode to give talkback time to catch up
    private static final int STOPWATCH_ACCESSIBILTY_REFRESH_INTERVAL_MILLIS = 500;

    int mState = Stopwatches.STOPWATCH_RESET;

    // Stopwatch views that are accessed by the activity
    private CircleTimerView mTime;
    private CountingTimerView mTimeText;
    private ListView mLapsList;
    private WakeLock mWakeLock;
    private CircleButtonsLayout mCircleLayout;

    // Animation constants and objects
    private LayoutTransition mLayoutTransition;
    private LayoutTransition mCircleLayoutTransition;
    private View mStartSpace;
    private View mEndSpace;
    private View mBottomSpace;
    private boolean mSpacersUsed;

    private AccessibilityManager mAccessibilityManager;

    // Used for calculating the time from the start taking into account the pause times
    long mStartTime = 0;
    long mAccumulatedTime = 0;

    // Lap information
    class Lap {

        Lap (long time, long total) {
            mLapTime = time;
            mTotalTime = total;
        }
        public long mLapTime;
        public long mTotalTime;

        public void updateView() {
            View lapInfo = mLapsList.findViewWithTag(this);
            if (lapInfo != null) {
                mLapsAdapter.setTimeText(lapInfo, this);
            }
        }
    }

    // Adapter for the ListView that shows the lap times.
    class LapsListAdapter extends BaseAdapter {

        private static final int VIEW_TYPE_LAP = 0;
        private static final int VIEW_TYPE_SPACE = 1;
        private static final int VIEW_TYPE_COUNT = 2;

        ArrayList<Lap> mLaps = new ArrayList<>();
        private final LayoutInflater mInflater;
        private final String[] mFormats;
        private final String[] mLapFormatSet;
        // Size of this array must match the size of formats
        private final long[] mThresholds = {
                10 * DateUtils.MINUTE_IN_MILLIS, // < 10 minutes
                DateUtils.HOUR_IN_MILLIS, // < 1 hour
                10 * DateUtils.HOUR_IN_MILLIS, // < 10 hours
                100 * DateUtils.HOUR_IN_MILLIS, // < 100 hours
                1000 * DateUtils.HOUR_IN_MILLIS // < 1000 hours
        };
        private int mLapIndex = 0;
        private int mTotalIndex = 0;
        private String mLapFormat;

        public LapsListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mFormats = context.getResources().getStringArray(R.array.stopwatch_format_set);
            mLapFormatSet = context.getResources().getStringArray(R.array.sw_lap_number_set);
            updateLapFormat();
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return position < mLaps.size() ? VIEW_TYPE_LAP : VIEW_TYPE_SPACE;
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (getCount() == 0) {
                return null;
            }

            // Handle request for the Spacer at the end
            if (getItemViewType(position) == VIEW_TYPE_SPACE) {
                return convertView != null ? convertView
                        : mInflater.inflate(R.layout.stopwatch_spacer, parent, false);
            }

            final View lapInfo = convertView != null ? convertView
                    : mInflater.inflate(R.layout.lap_view, parent, false);
            Lap lap = getItem(position);
            lapInfo.setTag(lap);

            TextView count = (TextView) lapInfo.findViewById(R.id.lap_number);
            count.setText(String.format(mLapFormat, mLaps.size() - position).toUpperCase());
            setTimeText(lapInfo, lap);

            return lapInfo;
        }

        protected void setTimeText(View lapInfo, Lap lap) {
            TextView lapTime = (TextView)lapInfo.findViewById(R.id.lap_time);
            TextView totalTime = (TextView)lapInfo.findViewById(R.id.lap_total);
            lapTime.setText(Stopwatches.formatTimeText(lap.mLapTime, mFormats[mLapIndex]));
            totalTime.setText(Stopwatches.formatTimeText(lap.mTotalTime, mFormats[mTotalIndex]));
        }

        @Override
        public int getCount() {
            // Add 1 for the spacer if list is not empty
            return mLaps.isEmpty() ? 0 : mLaps.size() + 1;
        }

        @Override
        public Lap getItem(int position) {
            if (position >= mLaps.size()) {
                return null;
            }
            return mLaps.get(position);
        }

        private void updateLapFormat() {
            // Note Stopwatches.MAX_LAPS < 100
            mLapFormat = mLapFormatSet[mLaps.size() < 10 ? 0 : 1];
        }

        private void resetTimeFormats() {
            mLapIndex = mTotalIndex = 0;
        }

        /**
         * A lap is printed into two columns: the total time and the lap time. To make this print
         * as pretty as possible, multiple formats were created which minimize the width of the
         * print. As the total or lap time exceed the limit of that format, this code updates
         * the format used for the total and/or lap times.
         *
         * @param lap to measure
         * @return true if this lap exceeded either threshold and a format was updated.
         */
        public boolean updateTimeFormats(Lap lap) {
            boolean formatChanged = false;
            while (mLapIndex + 1 < mThresholds.length && lap.mLapTime >= mThresholds[mLapIndex]) {
                mLapIndex++;
                formatChanged = true;
            }
            while (mTotalIndex + 1 < mThresholds.length &&
                lap.mTotalTime >= mThresholds[mTotalIndex]) {
                mTotalIndex++;
                formatChanged = true;
            }
            return formatChanged;
        }

        public void addLap(Lap l) {
            mLaps.add(0, l);
            // for efficiency caller also calls notifyDataSetChanged()
        }

        public void clearLaps() {
            mLaps.clear();
            updateLapFormat();
            resetTimeFormats();
            notifyDataSetChanged();
        }

        // Helper function used to get the lap data to be stored in the activity's bundle
        public long [] getLapTimes() {
            int size = mLaps.size();
            if (size == 0) {
                return null;
            }
            long [] laps = new long[size];
            for (int i = 0; i < size; i ++) {
                laps[i] = mLaps.get(i).mTotalTime;
            }
            return laps;
        }

        // Helper function to restore adapter's data from the activity's bundle
        public void setLapTimes(long [] laps) {
            if (laps == null || laps.length == 0) {
                return;
            }

            int size = laps.length;
            mLaps.clear();
            for (long lap : laps) {
                mLaps.add(new Lap(lap, 0));
            }
            long totalTime = 0;
            for (int i = size -1; i >= 0; i --) {
                totalTime += laps[i];
                mLaps.get(i).mTotalTime = totalTime;
                updateTimeFormats(mLaps.get(i));
            }
            updateLapFormat();
            showLaps();
            notifyDataSetChanged();
        }
    }

    LapsListAdapter mLapsAdapter;

    public StopwatchFragment() {
    }

    private void toggleStopwatchState() {
        long time = Utils.getTimeNow();
        Context context = getActivity().getApplicationContext();
        Intent intent = new Intent(context, StopwatchService.class);
        intent.putExtra(Stopwatches.MESSAGE_TIME, time);
        intent.putExtra(Stopwatches.SHOW_NOTIF, false);
        switch (mState) {
            case Stopwatches.STOPWATCH_RUNNING:
                // do stop
                long curTime = Utils.getTimeNow();
                mAccumulatedTime += (curTime - mStartTime);
                doStop();
                Events.sendStopwatchEvent(R.string.action_stop, R.string.label_deskclock);

                intent.setAction(HandleDeskClockApiCalls.ACTION_STOP_STOPWATCH);
                context.startService(intent);
                releaseWakeLock();
                break;
            case Stopwatches.STOPWATCH_RESET:
            case Stopwatches.STOPWATCH_STOPPED:
                // do start
                doStart(time);
                Events.sendStopwatchEvent(R.string.action_start, R.string.label_deskclock);

                intent.setAction(HandleDeskClockApiCalls.ACTION_START_STOPWATCH);
                context.startService(intent);
                acquireWakeLock();
                break;
            default:
                LogUtils.wtf("Illegal state " + mState
                        + " while pressing the right stopwatch button");
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup v = (ViewGroup)inflater.inflate(R.layout.stopwatch_fragment, container, false);

        mTime = (CircleTimerView)v.findViewById(R.id.stopwatch_time);
        mTimeText = (CountingTimerView)v.findViewById(R.id.stopwatch_time_text);
        mLapsList = (ListView)v.findViewById(R.id.laps_list);
        mLapsList.setDividerHeight(0);
        mLapsAdapter = new LapsListAdapter(getActivity());
        mLapsList.setAdapter(mLapsAdapter);

        // Timer text serves as a virtual start/stop button.
        mTimeText.registerVirtualButtonAction(new Runnable() {
            @Override
            public void run() {
                toggleStopwatchState();
            }
        });
        mTimeText.setVirtualButtonEnabled(true);

        mCircleLayout = (CircleButtonsLayout)v.findViewById(R.id.stopwatch_circle);
        mCircleLayout.setCircleTimerViewIds(R.id.stopwatch_time, 0 /* stopwatchId */ ,
                0 /* labelId */);

        // Animation setup
        mLayoutTransition = new LayoutTransition();
        mCircleLayoutTransition = new LayoutTransition();

        // The CircleButtonsLayout only needs to undertake location changes
        mCircleLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        mCircleLayoutTransition.disableTransitionType(LayoutTransition.APPEARING);
        mCircleLayoutTransition.disableTransitionType(LayoutTransition.DISAPPEARING);
        mCircleLayoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        mCircleLayoutTransition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        mCircleLayoutTransition.setAnimateParentHierarchy(false);

        // These spacers assist in keeping the size of CircleButtonsLayout constant
        mStartSpace = v.findViewById(R.id.start_space);
        mEndSpace = v.findViewById(R.id.end_space);
        mSpacersUsed = mStartSpace != null || mEndSpace != null;

        // Only applicable on portrait, only visible when there is no lap
        mBottomSpace = v.findViewById(R.id.bottom_space);

        // Listener to invoke extra animation within the laps-list
        mLayoutTransition.addTransitionListener(new LayoutTransition.TransitionListener() {
            @Override
            public void startTransition(LayoutTransition transition, ViewGroup container,
                                        View view, int transitionType) {
                if (view == mLapsList) {
                    if (transitionType == LayoutTransition.DISAPPEARING) {
                        if (DEBUG) LogUtils.v("StopwatchFragment.start laps-list disappearing");
                        boolean shiftX = view.getResources().getConfiguration().orientation
                                == Configuration.ORIENTATION_LANDSCAPE;
                        int first = mLapsList.getFirstVisiblePosition();
                        int last = mLapsList.getLastVisiblePosition();
                        // Ensure index range will not cause a divide by zero
                        if (last < first) {
                            last = first;
                        }
                        long duration = transition.getDuration(LayoutTransition.DISAPPEARING);
                        long offset = duration / (last - first + 1) / 5;
                        for (int visibleIndex = first; visibleIndex <= last; visibleIndex++) {
                            View lapView = mLapsList.getChildAt(visibleIndex - first);
                            if (lapView != null) {
                                float toXValue = shiftX ? 1.0f * (visibleIndex - first + 1) : 0;
                                float toYValue = shiftX ? 0 : 4.0f * (visibleIndex - first + 1);
                                        TranslateAnimation animation = new TranslateAnimation(
                                        Animation.RELATIVE_TO_SELF, 0,
                                        Animation.RELATIVE_TO_SELF, toXValue,
                                        Animation.RELATIVE_TO_SELF, 0,
                                        Animation.RELATIVE_TO_SELF, toYValue);
                                animation.setStartOffset((last - visibleIndex) * offset);
                                animation.setDuration(duration);
                                lapView.startAnimation(animation);
                            }
                        }
                    }
                }
            }

            @Override
            public void endTransition(LayoutTransition transition, ViewGroup container,
                                      View view, int transitionType) {
                if (transitionType == LayoutTransition.DISAPPEARING) {
                    if (DEBUG) LogUtils.v("StopwatchFragment.end laps-list disappearing");
                    int last = mLapsList.getLastVisiblePosition();
                    for (int visibleIndex = mLapsList.getFirstVisiblePosition();
                         visibleIndex <= last; visibleIndex++) {
                        View lapView = mLapsList.getChildAt(visibleIndex);
                        if (lapView != null) {
                            Animation animation = lapView.getAnimation();
                            if (animation != null) {
                                animation.cancel();
                            }
                        }
                    }
                }
            }
        });

        return v;
    }

    /**
     * Make the final display setup.
     *
     * If the fragment is starting with an existing list of laps, shows the laps list and if the
     * spacers around the clock exist, hide them. If there are not laps at the start, hide the laps
     * list and show the clock spacers if they exist.
     */
    @Override
    public void onStart() {
        super.onStart();

        boolean lapsVisible = mLapsAdapter.getCount() > 0;

        mLapsList.setVisibility(lapsVisible ? View.VISIBLE : View.GONE);
        if (mSpacersUsed) {
            showSpacerVisibility(lapsVisible);
        }
        showBottomSpacerVisibility(lapsVisible);

        ((ViewGroup)getView()).setLayoutTransition(mLayoutTransition);
        mCircleLayout.setLayoutTransition(mCircleLayoutTransition);

        mAccessibilityManager = (AccessibilityManager) getActivity().getSystemService(
                Context.ACCESSIBILITY_SERVICE);
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        readFromSharedPref(prefs);
        mTime.readFromSharedPref(prefs, "sw");
        mTime.postInvalidate();

        setFabAppearance();
        setLeftRightButtonAppearance();
        mTimeText.setTime(mAccumulatedTime, true, true);
        if (mState == Stopwatches.STOPWATCH_RUNNING) {
            acquireWakeLock();
            startUpdateThread();
        } else if (mState == Stopwatches.STOPWATCH_STOPPED && mAccumulatedTime != 0) {
            mTimeText.blinkTimeStr(true);
        }
        showLaps();
        ((DeskClock)getActivity()).registerPageChangedListener(this);
        // View was hidden in onPause, make sure it is visible now.
        View v = getView();
        if (v != null) {
            v.setVisibility(View.VISIBLE);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mState == Stopwatches.STOPWATCH_RUNNING) {
            stopUpdateThread();

            // This is called because the lock screen was activated, the window stay
            // active under it and when we unlock the screen, we see the old time for
            // a fraction of a second.
            View v = getView();
            if (v != null) {
                v.setVisibility(View.INVISIBLE);
            }
        }
        // The stopwatch must keep running even if the user closes the app so save stopwatch state
        // in shared prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        writeToSharedPref(prefs);
        mTime.writeToSharedPref(prefs, "sw");
        mTimeText.blinkTimeStr(false);
        ((DeskClock)getActivity()).unregisterPageChangedListener(this);
        releaseWakeLock();
        super.onPause();
    }

    @Override
    public void onPageChanged(int page) {
        if (page == DeskClock.STOPWATCH_TAB_INDEX && mState == Stopwatches.STOPWATCH_RUNNING) {
            acquireWakeLock();
        } else {
            releaseWakeLock();
        }
    }

    private void doStop() {
        if (DEBUG) LogUtils.v("StopwatchFragment.doStop");
        stopUpdateThread();
        mTime.pauseIntervalAnimation();
        mTimeText.setTime(mAccumulatedTime, true, true);
        mTimeText.blinkTimeStr(true);
        updateCurrentLap(mAccumulatedTime);
        mState = Stopwatches.STOPWATCH_STOPPED;
        setFabAppearance();
        setLeftRightButtonAppearance();
    }

    private void doStart(long time) {
        if (DEBUG) LogUtils.v("StopwatchFragment.doStart");
        mStartTime = time;
        startUpdateThread();
        mTimeText.blinkTimeStr(false);
        if (mTime.isAnimating()) {
            mTime.startIntervalAnimation();
        }
        mState = Stopwatches.STOPWATCH_RUNNING;
        setFabAppearance();
        setLeftRightButtonAppearance();
    }

    private void doLap() {
        if (DEBUG) LogUtils.v("StopwatchFragment.doLap");
        showLaps();
        setFabAppearance();
        setLeftRightButtonAppearance();
    }

    private void doReset() {
        if (DEBUG) LogUtils.v("StopwatchFragment.doReset");
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        Utils.clearSwSharedPref(prefs);
        mTime.clearSharedPref(prefs, "sw");
        mAccumulatedTime = 0;
        mLapsAdapter.clearLaps();
        showLaps();
        mTime.stopIntervalAnimation();
        mTime.reset();
        mTimeText.setTime(mAccumulatedTime, true, true);
        mTimeText.blinkTimeStr(false);
        mState = Stopwatches.STOPWATCH_RESET;
        setFabAppearance();
        setLeftRightButtonAppearance();
    }

    private void shareResults() {
        final Context context = getActivity();
        final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                Stopwatches.getShareTitle(context.getApplicationContext()));
        shareIntent.putExtra(Intent.EXTRA_TEXT, Stopwatches.buildShareResults(
                getActivity().getApplicationContext(), mTimeText.getTimeString(),
                getLapShareTimes(mLapsAdapter.getLapTimes())));

        final Intent launchIntent = Intent.createChooser(shareIntent,
                context.getString(R.string.sw_share_button));
        try {
            context.startActivity(launchIntent);
        } catch (ActivityNotFoundException e) {
            LogUtils.e("No compatible receiver is found");
        }
    }

    /** Turn laps as they would be saved in prefs into format for sharing. **/
    private long[] getLapShareTimes(long[] input) {
        if (input == null) {
            return null;
        }

        int numLaps = input.length;
        long[] output = new long[numLaps];
        long prevLapElapsedTime = 0;
        for (int lap_i = numLaps - 1; lap_i >= 0; lap_i--) {
            long lap = input[lap_i];
            LogUtils.v("lap " + lap_i + ": " + lap);
            output[lap_i] = lap - prevLapElapsedTime;
            prevLapElapsedTime = lap;
        }
        return output;
    }

    private boolean reachedMaxLaps() {
        return mLapsAdapter.getCount() >= Stopwatches.MAX_LAPS;
    }

    /***
     * Handle action when user presses the lap button
     * @param time - in hundredth of a second
     */
    private void addLapTime(long time) {
        // The total elapsed time
        final long curTime = time - mStartTime + mAccumulatedTime;
        int size = mLapsAdapter.getCount();
        if (size == 0) {
            // Create and add the first lap
            Lap firstLap = new Lap(curTime, curTime);
            mLapsAdapter.addLap(firstLap);
            // Create the first active lap
            mLapsAdapter.addLap(new Lap(0, curTime));
            // Update the interval on the clock and check the lap and total time formatting
            mTime.setIntervalTime(curTime);
            mLapsAdapter.updateTimeFormats(firstLap);
        } else {
            // Finish active lap
            final long lapTime = curTime - mLapsAdapter.getItem(1).mTotalTime;
            mLapsAdapter.getItem(0).mLapTime = lapTime;
            mLapsAdapter.getItem(0).mTotalTime = curTime;
            // Create a new active lap
            mLapsAdapter.addLap(new Lap(0, curTime));
            // Update marker on clock and check that formatting for the lap number
            mTime.setMarkerTime(lapTime);
            mLapsAdapter.updateLapFormat();
        }
        // Repaint the laps list
        mLapsAdapter.notifyDataSetChanged();

        // Start lap animation starting from the second lap
        mTime.stopIntervalAnimation();
        if (!reachedMaxLaps()) {
            mTime.startIntervalAnimation();
        }
    }

    private void updateCurrentLap(long totalTime) {
        // There are either 0, 2 or more Laps in the list See {@link #addLapTime}
        if (mLapsAdapter.getCount() > 0) {
            Lap curLap = mLapsAdapter.getItem(0);
            curLap.mLapTime = totalTime - mLapsAdapter.getItem(1).mTotalTime;
            curLap.mTotalTime = totalTime;
            // If this lap has caused a change in the format for total and/or lap time, all of
            // the rows need a fresh print. The simplest way to refresh all of the rows is
            // calling notifyDataSetChanged.
            if (mLapsAdapter.updateTimeFormats(curLap)) {
                mLapsAdapter.notifyDataSetChanged();
            } else {
                curLap.updateView();
            }
        }
    }

    /**
     * Show or hide the laps-list
     */
    private void showLaps() {
        if (DEBUG) LogUtils.v(String.format("StopwatchFragment.showLaps: count=%d",
                mLapsAdapter.getCount()));

        boolean lapsVisible = mLapsAdapter.getCount() > 0;

        // Layout change animations will start upon the first add/hide view. Temporarily disable
        // the layout transition animation for the spacers, make the changes, then re-enable
        // the animation for the add/hide laps-list
        if (mSpacersUsed) {
            ViewGroup rootView = (ViewGroup) getView();
            if (rootView != null) {
                rootView.setLayoutTransition(null);

                showSpacerVisibility(lapsVisible);

                rootView.setLayoutTransition(mLayoutTransition);
            }
        }

        showBottomSpacerVisibility(lapsVisible);

        if (lapsVisible) {
            // There are laps - show the laps-list
            // No delay for the CircleButtonsLayout changes - start immediately so that the
            // circle has shifted before the laps-list starts appearing.
            mCircleLayoutTransition.setStartDelay(LayoutTransition.CHANGING, 0);

            mLapsList.setVisibility(View.VISIBLE);
        } else {
            // There are no laps - hide the laps list

            // Delay the CircleButtonsLayout animation until after the laps-list disappears
            long startDelay = mLayoutTransition.getStartDelay(LayoutTransition.DISAPPEARING) +
                    mLayoutTransition.getDuration(LayoutTransition.DISAPPEARING);
            mCircleLayoutTransition.setStartDelay(LayoutTransition.CHANGING, startDelay);
            mLapsList.setVisibility(View.GONE);
        }
    }

    private void showSpacerVisibility(boolean lapsVisible) {
        final int spacersVisibility = lapsVisible ? View.GONE : View.VISIBLE;
        if (mStartSpace != null) {
            mStartSpace.setVisibility(spacersVisibility);
        }
        if (mEndSpace != null) {
            mEndSpace.setVisibility(spacersVisibility);
        }
    }

    private void showBottomSpacerVisibility(boolean lapsVisible) {
        if (mBottomSpace != null) {
            mBottomSpace.setVisibility(lapsVisible ? View.GONE : View.VISIBLE);
        }
    }

    private void startUpdateThread() {
        mTime.post(mTimeUpdateThread);
    }

    private void stopUpdateThread() {
        mTime.removeCallbacks(mTimeUpdateThread);
    }

    Runnable mTimeUpdateThread = new Runnable() {
        @Override
        public void run() {
            long curTime = Utils.getTimeNow();
            long totalTime = mAccumulatedTime + (curTime - mStartTime);
            if (mTime != null) {
                mTimeText.setTime(totalTime, true, true);
            }
            updateCurrentLap(totalTime);
            mTime.postDelayed(mTimeUpdateThread, mAccessibilityManager != null &&
                    mAccessibilityManager.isTouchExplorationEnabled()
                    ? STOPWATCH_ACCESSIBILTY_REFRESH_INTERVAL_MILLIS
                    : STOPWATCH_REFRESH_INTERVAL_MILLIS);
        }
    };

    private void writeToSharedPref(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong (Stopwatches.PREF_START_TIME, mStartTime);
        editor.putLong (Stopwatches.PREF_ACCUM_TIME, mAccumulatedTime);
        editor.putInt (Stopwatches.PREF_STATE, mState);
        if (mLapsAdapter != null) {
            long [] laps = mLapsAdapter.getLapTimes();
            if (laps != null) {
                editor.putInt (Stopwatches.PREF_LAP_NUM, laps.length);
                for (int i = 0; i < laps.length; i++) {
                    String key = Stopwatches.PREF_LAP_TIME + Integer.toString(laps.length - i);
                    editor.putLong (key, laps[i]);
                }
            }
        }
        if (mState == Stopwatches.STOPWATCH_RUNNING) {
            editor.putLong(Stopwatches.NOTIF_CLOCK_BASE, mStartTime-mAccumulatedTime);
            editor.putLong(Stopwatches.NOTIF_CLOCK_ELAPSED, -1);
            editor.putBoolean(Stopwatches.NOTIF_CLOCK_RUNNING, true);
        } else if (mState == Stopwatches.STOPWATCH_STOPPED) {
            editor.putLong(Stopwatches.NOTIF_CLOCK_ELAPSED, mAccumulatedTime);
            editor.putLong(Stopwatches.NOTIF_CLOCK_BASE, -1);
            editor.putBoolean(Stopwatches.NOTIF_CLOCK_RUNNING, false);
        } else if (mState == Stopwatches.STOPWATCH_RESET) {
            editor.remove(Stopwatches.NOTIF_CLOCK_BASE);
            editor.remove(Stopwatches.NOTIF_CLOCK_RUNNING);
            editor.remove(Stopwatches.NOTIF_CLOCK_ELAPSED);
        }
        editor.putBoolean(Stopwatches.PREF_UPDATE_CIRCLE, false);
        editor.apply();
    }

    private void readFromSharedPref(SharedPreferences prefs) {
        mStartTime = prefs.getLong(Stopwatches.PREF_START_TIME, 0);
        mAccumulatedTime = prefs.getLong(Stopwatches.PREF_ACCUM_TIME, 0);
        mState = prefs.getInt(Stopwatches.PREF_STATE, Stopwatches.STOPWATCH_RESET);
        int numLaps = prefs.getInt(Stopwatches.PREF_LAP_NUM, Stopwatches.STOPWATCH_RESET);
        if (mLapsAdapter != null) {
            long[] oldLaps = mLapsAdapter.getLapTimes();
            if (oldLaps == null || oldLaps.length < numLaps) {
                long[] laps = new long[numLaps];
                long prevLapElapsedTime = 0;
                for (int lap_i = 0; lap_i < numLaps; lap_i++) {
                    String key = Stopwatches.PREF_LAP_TIME + Integer.toString(lap_i + 1);
                    long lap = prefs.getLong(key, 0);
                    laps[numLaps - lap_i - 1] = lap - prevLapElapsedTime;
                    prevLapElapsedTime = lap;
                }
                mLapsAdapter.setLapTimes(laps);
            }
        }
        if (prefs.getBoolean(Stopwatches.PREF_UPDATE_CIRCLE, true)) {
            if (mState == Stopwatches.STOPWATCH_STOPPED) {
                doStop();
            } else if (mState == Stopwatches.STOPWATCH_RUNNING) {
                doStart(mStartTime);
            } else if (mState == Stopwatches.STOPWATCH_RESET) {
                doReset();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(PreferenceManager.getDefaultSharedPreferences(getActivity()))) {
            if (! (key.equals(Stopwatches.PREF_LAP_NUM) ||
                    key.startsWith(Stopwatches.PREF_LAP_TIME))) {
                readFromSharedPref(prefs);
                if (prefs.getBoolean(Stopwatches.PREF_UPDATE_CIRCLE, true)) {
                    mTime.readFromSharedPref(prefs, "sw");
                }
            }
        }
    }

    // Used to keeps screen on when stopwatch is running.

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            final PowerManager pm =
                    (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
            mWakeLock.setReferenceCounted(false);
        }
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    public void onFabClick(View view){
        toggleStopwatchState();
    }

    @Override
    public void onLeftButtonClick(View view) {
        final long time = Utils.getTimeNow();
        final Context context = getActivity().getApplicationContext();
        final Intent intent = new Intent(context, StopwatchService.class);
        intent.putExtra(Stopwatches.MESSAGE_TIME, time);
        intent.putExtra(Stopwatches.SHOW_NOTIF, false);
        switch (mState) {
            case Stopwatches.STOPWATCH_RUNNING:
                // Save lap time
                addLapTime(time);
                doLap();
                Events.sendStopwatchEvent(R.string.action_lap, R.string.label_deskclock);

                intent.setAction(HandleDeskClockApiCalls.ACTION_LAP_STOPWATCH);
                context.startService(intent);
                break;
            case Stopwatches.STOPWATCH_STOPPED:
                // do reset
                doReset();
                Events.sendStopwatchEvent(R.string.action_reset, R.string.label_deskclock);

                intent.setAction(HandleDeskClockApiCalls.ACTION_RESET_STOPWATCH);
                context.startService(intent);
                releaseWakeLock();
                break;
            default:
                // Happens in monkey tests
                LogUtils.i("Illegal state " + mState + " while pressing the left stopwatch button");
                break;
        }
    }

    @Override
    public void onRightButtonClick(View view) {
        shareResults();
    }

    @Override
    public void setFabAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        if (mFab == null || activity.getSelectedTab() != DeskClock.STOPWATCH_TAB_INDEX) {
            return;
        }
        if (mState == Stopwatches.STOPWATCH_RUNNING) {
            mFab.setImageResource(R.drawable.ic_fab_pause);
            mFab.setContentDescription(getString(R.string.sw_stop_button));
        } else {
            mFab.setImageResource(R.drawable.ic_fab_play);
            mFab.setContentDescription(getString(R.string.sw_start_button));
        }
        mFab.setVisibility(View.VISIBLE);
    }

    @Override
    public void setLeftRightButtonAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        if (mLeftButton == null || mRightButton == null ||
                activity.getSelectedTab() != DeskClock.STOPWATCH_TAB_INDEX) {
            return;
        }
        mRightButton.setImageResource(R.drawable.ic_share);
        mRightButton.setContentDescription(getString(R.string.sw_share_button));

        switch (mState) {
            case Stopwatches.STOPWATCH_RESET:
                mLeftButton.setImageResource(R.drawable.ic_lap);
                mLeftButton.setContentDescription(getString(R.string.sw_lap_button));
                mLeftButton.setEnabled(false);
                mLeftButton.setVisibility(View.INVISIBLE);
                mRightButton.setVisibility(View.INVISIBLE);
                break;
            case Stopwatches.STOPWATCH_RUNNING:
                mLeftButton.setImageResource(R.drawable.ic_lap);
                mLeftButton.setContentDescription(getString(R.string.sw_lap_button));
                mLeftButton.setEnabled(!reachedMaxLaps());
                mLeftButton.setVisibility(View.VISIBLE);
                mRightButton.setVisibility(View.INVISIBLE);
                break;
            case Stopwatches.STOPWATCH_STOPPED:
                mLeftButton.setImageResource(R.drawable.ic_reset);
                mLeftButton.setContentDescription(getString(R.string.sw_reset_button));
                mLeftButton.setEnabled(true);
                mLeftButton.setVisibility(View.VISIBLE);
                mRightButton.setVisibility(View.VISIBLE);
                break;
        }
    }
}
