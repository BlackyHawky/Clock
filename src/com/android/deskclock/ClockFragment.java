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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.worldclock.CitySelectionActivity;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static java.util.Calendar.DAY_OF_WEEK;

/**
 * Fragment that shows the clock (analog or digital), the next alarm info and the world clock.
 */
public final class ClockFragment extends DeskClockFragment {

    // Updates the UI in response to system setting changes that alter time values and time display.
    private final BroadcastReceiver mBroadcastReceiver = new SystemBroadcastReceiver();

    // Updates dates in the UI on every quarter-hour.
    private final Runnable mQuarterHourUpdater = new QuarterHourRunnable();

    // Detects changes to the next scheduled alarm pre-L.
    private ContentObserver mAlarmObserver;

    private Handler mHandler;

    private TextClock mDigitalClock;
    private View mAnalogClock, mClockFrame;
    private SelectedCitiesAdapter mCityAdapter;
    private ListView mCityList;
    private String mDateFormat;
    private String mDateFormatForAccessibility;

    /** The public no-arg constructor required by all fragments. */
    public ClockFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        mAlarmObserver = Utils.isPreL() ? new AlarmObserverPreL(mHandler) : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
        super.onCreateView(inflater, container, icicle);

        final OnTouchListener startScreenSaverListener = new StartScreenSaverListener();
        final View footerView = inflater.inflate(R.layout.blank_footer_view, mCityList, false);
        final View fragmentView = inflater.inflate(R.layout.clock_fragment, container, false);

        mCityAdapter = new SelectedCitiesAdapter(getActivity());

        mCityList = (ListView) fragmentView.findViewById(R.id.cities);
        mCityList.setDivider(null);
        mCityList.setAdapter(mCityAdapter);
        mCityList.addFooterView(footerView, null, false);
        mCityList.setOnTouchListener(startScreenSaverListener);

        // On tablet landscape, the clock frame will be a distinct view. Otherwise, it'll be added
        // on as a header to the main listview.
        mClockFrame = fragmentView.findViewById(R.id.main_clock_left_pane);
        if (mClockFrame == null) {
            mClockFrame = inflater.inflate(R.layout.main_clock_frame, mCityList, false);
            mCityList.addHeaderView(mClockFrame, null, false);
            final View hairline = mClockFrame.findViewById(R.id.hairline);
            hairline.setVisibility(mCityAdapter.getCount() == 0 ? GONE : VISIBLE);
        } else {
            final View hairline = mClockFrame.findViewById(R.id.hairline);
            hairline.setVisibility(GONE);
            // The main clock frame needs its own touch listener for night mode now.
            fragmentView.setOnTouchListener(startScreenSaverListener);
        }

        mDigitalClock = (TextClock) mClockFrame.findViewById(R.id.digital_clock);
        mAnalogClock = mClockFrame.findViewById(R.id.analog_clock);
        return fragmentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Utils.setTimeFormat(getActivity(), mDigitalClock);
    }

    @Override
    public void onResume() {
        super.onResume();

        final Activity activity = getActivity();
        setFabAppearance();
        setLeftRightButtonAppearance();

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        // Schedule a runnable to update the date every quarter hour.
        Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);

        // Watch for system events that effect clock time or format.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
        activity.registerReceiver(mBroadcastReceiver, filter);

        // Resume can be invoked after changing the clock style.
        Utils.setClockStyle(mDigitalClock, mAnalogClock);

        final View view = getView();
        if (view != null && view.findViewById(R.id.main_clock_left_pane) != null) {
            // Center the main clock frame by hiding the world clocks when none are selected.
            mCityList.setVisibility(mCityAdapter.getCount() == 0 ? GONE : VISIBLE);
        }

        refreshDates();
        refreshAlarm();

        if (mAlarmObserver != null) {
            final Uri uri = Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED);
            activity.getContentResolver().registerContentObserver(uri, false, mAlarmObserver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.cancelQuarterHourUpdater(mHandler, mQuarterHourUpdater);

        final Activity activity = getActivity();
        activity.unregisterReceiver(mBroadcastReceiver);
        if (mAlarmObserver != null) {
            activity.getContentResolver().unregisterContentObserver(mAlarmObserver);
        }
    }

    @Override
    public void onFabClick(View view) {
        startActivity(new Intent(getActivity(), CitySelectionActivity.class));
    }

    @Override
    public void setFabAppearance() {
        if (mFab == null || getSelectedTab() != DeskClock.CLOCK_TAB_INDEX) {
            return;
        }

        mFab.setVisibility(VISIBLE);
        mFab.setImageResource(R.drawable.ic_language_white_24dp);
        mFab.setContentDescription(getString(R.string.button_cities));
    }

    @Override
    public void setLeftRightButtonAppearance() {
        if (getSelectedTab() != DeskClock.CLOCK_TAB_INDEX) {
            return;
        }

        if (mLeftButton != null) {
            mLeftButton.setVisibility(INVISIBLE);
        }

        if (mRightButton != null) {
            mRightButton.setVisibility(INVISIBLE);
        }
    }

    /**
     * Refresh the displayed dates in response to a change that may have changed them.
     */
    private void refreshDates() {
        // Refresh the date in the main clock.
        Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);

        // Refresh the day-of-week in each world clock.
        mCityAdapter.notifyDataSetChanged();
    }

    /**
     * Refresh the next alarm time.
     */
    private void refreshAlarm() {
        Utils.refreshAlarm(getActivity(), mClockFrame);
    }

    /**
     * Long pressing over the main clock or any world clock item starts the screen saver.
     */
    private final class StartScreenSaverListener implements OnTouchListener, Runnable {

        private float mTouchSlop = -1;
        private int mLongPressTimeout = -1;
        private float mLastTouchX, mLastTouchY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mTouchSlop == -1) {
                mTouchSlop = ViewConfiguration.get(getActivity()).getScaledTouchSlop();
                mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
            }

            switch (event.getAction()) {
                case (MotionEvent.ACTION_DOWN):
                    // Create and post a runnable to start the screen saver in the future.
                    mHandler.postDelayed(this, mLongPressTimeout);
                    mLastTouchX = event.getX();
                    mLastTouchY = event.getY();
                    return true;

                case (MotionEvent.ACTION_MOVE):
                    final float xDiff = Math.abs(event.getX() - mLastTouchX);
                    final float yDiff = Math.abs(event.getY() - mLastTouchY);
                    if (xDiff >= mTouchSlop || yDiff >= mTouchSlop) {
                        mHandler.removeCallbacks(this);
                    }
                    break;
                default:
                    mHandler.removeCallbacks(this);
            }
            return false;
        }

        @Override
        public void run() {
            startActivity(new Intent(getActivity(), ScreensaverActivity.class));
        }
    }

    /**
     * This runnable executes at every quarter-hour (e.g. 1:00, 1:15, 1:30, 1:45, etc...) and
     * updates the dates displayed within the UI. Quarter-hour increments were chosen to accommodate
     * the "weirdest" timezones (e.g. Nepal is UTC/GMT +05:45).
     */
    private final class QuarterHourRunnable implements Runnable {
        @Override
        public void run() {
            refreshDates();

            // Schedule the next quarter-hour callback.
            Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);
        }
    }

    /**
     * Prior to L, a ContentObserver was used to monitor changes to the next scheduled alarm.
     * In L and beyond this is accomplished via a system broadcast of
     * {@link AlarmManager#ACTION_NEXT_ALARM_CLOCK_CHANGED}.
     */
    private final class AlarmObserverPreL extends ContentObserver {
        public AlarmObserverPreL(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Utils.refreshAlarm(getActivity(), mClockFrame);
        }
    }

    /**
     * Handle system broadcasts that influence the display of this fragment. Since this fragment
     * displays time-related information, ACTION_TIME_CHANGED and ACTION_TIMEZONE_CHANGED both
     * alter the actual time values displayed. ACTION_NEXT_ALARM_CLOCK_CHANGED indicates the time at
     * which the next alarm will fire has changed.
     */
    private final class SystemBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_TIME_CHANGED:
                case Intent.ACTION_TIMEZONE_CHANGED:
                    refreshDates();

                case AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED:
                    refreshAlarm();
            }
        }
    }

    /**
     * This adapter lists all of the selected world clocks. Optionally, it also includes a clock at
     * the top for the home timezone if "Automatic home clock" is turned on in settings and the
     * current time at home does not match the current time in the timezone of the current location.
     */
    private static final class SelectedCitiesAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;
        private final Context mContext;

        public SelectedCitiesAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            final int homeClockCount = getShowHomeClock() ? 1 : 0;
            final int worldClockCount = getCities().size();
            return homeClockCount + worldClockCount;
        }

        @Override
        public Object getItem(int position) {
            if (getShowHomeClock()) {
                return position == 0 ? getHomeCity() : getCities().get(position - 1);
            }

            return getCities().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            // Retrieve the city to bind.
            final City city = (City) getItem(position);

            // Inflate a new view for the city, if necessary.
            if (view == null) {
                view = mInflater.inflate(R.layout.world_clock_list_item, parent, false);
            }

            final View clock = view.findViewById(R.id.city_left);

            // Configure the digital clock or analog clock depending on the user preference.
            final TextClock digitalClock = (TextClock) clock.findViewById(R.id.digital_clock);
            final AnalogClock analogClock = (AnalogClock) clock.findViewById(R.id.analog_clock);
            if (DataModel.getDataModel().getClockStyle() == DataModel.ClockStyle.ANALOG) {
                digitalClock.setVisibility(GONE);
                analogClock.setVisibility(VISIBLE);
                analogClock.setTimeZone(city.getTimeZoneId());
                analogClock.enableSeconds(false);
            } else {
                digitalClock.setVisibility(VISIBLE);
                analogClock.setVisibility(GONE);
                digitalClock.setTimeZone(city.getTimeZoneId());
                Utils.setTimeFormat(mContext, digitalClock);
            }

            // Bind the city name.
            final TextView name = (TextView) clock.findViewById(R.id.city_name);
            name.setText(city.getName());

            // Compute if the city week day matches the weekday of the current timezone.
            final Calendar localCal = Calendar.getInstance(TimeZone.getDefault());
            final Calendar cityCal = Calendar.getInstance(city.getTimeZone());
            final boolean displayDayOfWeek = localCal.get(DAY_OF_WEEK) != cityCal.get(DAY_OF_WEEK);

            // Bind the week day display.
            final TextView dayOfWeek = (TextView) clock.findViewById(R.id.city_day);
            dayOfWeek.setVisibility(displayDayOfWeek ? VISIBLE : GONE);
            if (displayDayOfWeek) {
                final Locale locale = Locale.getDefault();
                final String weekday = cityCal.getDisplayName(DAY_OF_WEEK, Calendar.SHORT, locale);
                dayOfWeek.setText(mContext.getString(R.string.world_day_of_week_label, weekday));
            }

            return view;
        }

        /**
         * @return {@code false} to prevent the cities from responding to touch
         */
        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        private City getHomeCity() {
            return DataModel.getDataModel().getHomeCity();
        }

        private List<City> getCities() {
            return DataModel.getDataModel().getSelectedCities();
        }

        private boolean getShowHomeClock() {
            return DataModel.getDataModel().getShowHomeClock();
        }
    }
}
