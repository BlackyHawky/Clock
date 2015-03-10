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

package com.android.deskclock;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextClock;

import com.android.deskclock.worldclock.CitiesActivity;
import com.android.deskclock.worldclock.WorldClockAdapter;

/**
 * Fragment that shows  the clock (analog or digital), the next alarm info and the world clock.
 */
public class ClockFragment extends DeskClockFragment implements OnSharedPreferenceChangeListener {

    private static final String BUTTONS_HIDDEN_KEY = "buttons_hidden";
    private static final boolean PRE_L_DEVICE =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    private final static String TAG = "ClockFragment";

    private boolean mButtonsHidden = false;
    private View mDigitalClock, mAnalogClock, mClockFrame, mHairline;
    private WorldClockAdapter mAdapter;
    private ListView mList;
    private SharedPreferences mPrefs;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private String mDefaultClockStyle;
    private String mClockStyle;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean changed = action.equals(Intent.ACTION_TIME_CHANGED)
                    || action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                    || action.equals(Intent.ACTION_LOCALE_CHANGED);
            if (changed) {
                Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
                if (mAdapter != null) {
                    // *CHANGED may modify the need for showing the Home City
                    if (mAdapter.hasHomeCity() != mAdapter.needHomeCity()) {
                        mAdapter.reloadData(context);
                    } else {
                        mAdapter.notifyDataSetChanged();
                    }
                    // Locale change: update digital clock format and
                    // reload the cities list with new localized names
                    if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                        if (mDigitalClock != null) {
                            Utils.setTimeFormat(context,
                                (TextClock) mDigitalClock.findViewById(R.id.digital_clock),
                                context.getResources().getDimensionPixelSize(
                                    R.dimen.main_ampm_font_size));
                        }
                        mAdapter.loadCitiesDb(context);
                        mAdapter.notifyDataSetChanged();
                    }
                }
                Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);
            }
            if (changed || action.equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
                Utils.refreshAlarm(getActivity(), mClockFrame);
            }
        }
    };

    private final Handler mHandler = new Handler();

    /* Register ContentObserver to see alarm changes for pre-L */
    private final ContentObserver mAlarmObserver = PRE_L_DEVICE
            ? new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    Utils.refreshAlarm(ClockFragment.this.getActivity(), mClockFrame);
                }
            }
            : null;

    // Thread that runs on every quarter-hour and refreshes the date.
    private final Runnable mQuarterHourUpdater = new Runnable() {
        @Override
        public void run() {
            // Update the main and world clock dates
            Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
            Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);
        }
    };

    public ClockFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle icicle) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.clock_fragment, container, false);
        if (icicle != null) {
            mButtonsHidden = icicle.getBoolean(BUTTONS_HIDDEN_KEY, false);
        }
        mList = (ListView) v.findViewById(R.id.cities);
        mList.setDivider(null);

        OnTouchListener longPressNightMode = new OnTouchListener() {
            private float mMaxMovementAllowed = -1;
            private int mLongPressTimeout = -1;
            private float mLastTouchX
                    ,
                    mLastTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mMaxMovementAllowed == -1) {
                    mMaxMovementAllowed = ViewConfiguration.get(getActivity()).getScaledTouchSlop();
                    mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
                }

                switch (event.getAction()) {
                    case (MotionEvent.ACTION_DOWN):
                        long time = Utils.getTimeNow();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(getActivity(), ScreensaverActivity.class));
                            }
                        }, mLongPressTimeout);
                        mLastTouchX = event.getX();
                        mLastTouchY = event.getY();
                        return true;
                    case (MotionEvent.ACTION_MOVE):
                        float xDiff = Math.abs(event.getX() - mLastTouchX);
                        float yDiff = Math.abs(event.getY() - mLastTouchY);
                        if (xDiff >= mMaxMovementAllowed || yDiff >= mMaxMovementAllowed) {
                            mHandler.removeCallbacksAndMessages(null);
                        }
                        break;
                    default:
                        mHandler.removeCallbacksAndMessages(null);
                }
                return false;
            }
        };

        // On tablet landscape, the clock frame will be a distinct view. Otherwise, it'll be added
        // on as a header to the main listview.
        mClockFrame = v.findViewById(R.id.main_clock_left_pane);
        mHairline = v.findViewById(R.id.hairline);
        if (mClockFrame == null) {
            mClockFrame = inflater.inflate(R.layout.main_clock_frame, mList, false);
            mHairline = mClockFrame.findViewById(R.id.hairline);
            mHairline.setVisibility(View.VISIBLE);
            mList.addHeaderView(mClockFrame, null, false);
        } else {
            mHairline.setVisibility(View.GONE);
            // The main clock frame needs its own touch listener for night mode now.
            v.setOnTouchListener(longPressNightMode);
        }
        mList.setOnTouchListener(longPressNightMode);

        // If the current layout has a fake overflow menu button, let the parent
        // activity set up its click and touch listeners.
        View menuButton = v.findViewById(R.id.menu_button);
        if (menuButton != null) {
            setupFakeOverflowMenuButton(menuButton);
        }

        mDigitalClock = mClockFrame.findViewById(R.id.digital_clock);
        mAnalogClock = mClockFrame.findViewById(R.id.analog_clock);
        Utils.setTimeFormat(getActivity(),
            (TextClock) mDigitalClock.findViewById(R.id.digital_clock),
            getResources().getDimensionPixelSize(R.dimen.main_ampm_font_size));
        View footerView = inflater.inflate(R.layout.blank_footer_view, mList, false);
        mList.addFooterView(footerView, null, false);
        mAdapter = new WorldClockAdapter(getActivity());
        if (mAdapter.getCount() == 0) {
            mHairline.setVisibility(View.GONE);
        }
        mList.setAdapter(mAdapter);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mDefaultClockStyle = getActivity().getResources().getString(R.string.default_clock_style);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        final DeskClock activity = (DeskClock) getActivity();
        if (activity.getSelectedTab() == DeskClock.CLOCK_TAB_INDEX) {
            setFabAppearance();
            setLeftRightButtonAppearance();
        }

        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);
        // Besides monitoring when quarter-hour changes, monitor other actions that
        // effect clock time
        IntentFilter filter = new IntentFilter();
        filter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        activity.registerReceiver(mIntentReceiver, filter);

        // Resume can invoked after changing the cities list or a change in locale
        if (mAdapter != null) {
            mAdapter.loadCitiesDb(activity);
            mAdapter.reloadData(activity);
        }
        // Resume can invoked after changing the clock style.
        View clockView = Utils.setClockStyle(activity, mDigitalClock, mAnalogClock,
                SettingsActivity.KEY_CLOCK_STYLE);
        mClockStyle = (clockView == mDigitalClock ?
                Utils.CLOCK_TYPE_DIGITAL : Utils.CLOCK_TYPE_ANALOG);

        // Center the main clock frame if cities are empty.
        if (getView().findViewById(R.id.main_clock_left_pane) != null && mAdapter.getCount() == 0) {
            mList.setVisibility(View.GONE);
        } else {
            mList.setVisibility(View.VISIBLE);
        }
        mAdapter.notifyDataSetChanged();

        Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
        Utils.refreshAlarm(activity, mClockFrame);
        if (PRE_L_DEVICE) {
            activity.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED),
                false,
                mAlarmObserver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        Utils.cancelQuarterHourUpdater(mHandler, mQuarterHourUpdater);
        Activity activity = getActivity();
        activity.unregisterReceiver(mIntentReceiver);
        if (PRE_L_DEVICE) {
            activity.getContentResolver().unregisterContentObserver(mAlarmObserver);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(BUTTONS_HIDDEN_KEY, mButtonsHidden);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key == SettingsActivity.KEY_CLOCK_STYLE) {
            mClockStyle = prefs.getString(SettingsActivity.KEY_CLOCK_STYLE, mDefaultClockStyle);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onFabClick(View view) {
        final Activity activity = getActivity();
        startActivity(new Intent(activity, CitiesActivity.class));
    }

    @Override
    public void setFabAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        if (mFab == null || activity.getSelectedTab() != DeskClock.CLOCK_TAB_INDEX) {
            return;
        }
        mFab.setVisibility(View.VISIBLE);
        mFab.setImageResource(R.drawable.ic_globe);
        mFab.setContentDescription(getString(R.string.button_cities));
    }

    @Override
    public void setLeftRightButtonAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        if (mLeftButton == null || mRightButton == null ||
                activity.getSelectedTab() != DeskClock.CLOCK_TAB_INDEX) {
            return;
        }
        mLeftButton.setVisibility(View.INVISIBLE);
        mRightButton.setVisibility(View.INVISIBLE);
    }
}
