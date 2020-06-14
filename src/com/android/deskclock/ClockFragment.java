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
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.data.City;
import com.android.deskclock.data.CityListener;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.worldclock.CitySelectionActivity;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static android.app.AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.android.deskclock.uidata.UiDataModel.Tab.CLOCKS;
import static java.util.Calendar.DAY_OF_WEEK;

/**
 * Fragment that shows the clock (analog or digital), the next alarm info and the world clock.
 */
public final class ClockFragment extends DeskClockFragment {

    // Updates dates in the UI on every quarter-hour.
    private final Runnable mQuarterHourUpdater = new QuarterHourRunnable();

    // Updates the UI in response to changes to the scheduled alarm.
    private BroadcastReceiver mAlarmChangeReceiver;

    // Detects changes to the next scheduled alarm pre-L.
    private ContentObserver mAlarmObserver;

    private TextClock mDigitalClock;
    private AnalogClock mAnalogClock;
    private View mClockFrame;
    private SelectedCitiesAdapter mCityAdapter;
    private RecyclerView mCityList;
    private String mDateFormat;
    private String mDateFormatForAccessibility;

    /**
     * The public no-arg constructor required by all fragments.
     */
    public ClockFragment() {
        super(CLOCKS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlarmObserver = Utils.isPreL() ? new AlarmObserverPreL() : null;
        mAlarmChangeReceiver = Utils.isLOrLater() ? new AlarmChangedBroadcastReceiver() : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
        super.onCreateView(inflater, container, icicle);

        final View fragmentView = inflater.inflate(R.layout.clock_fragment, container, false);

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        mCityAdapter = new SelectedCitiesAdapter(getActivity(), mDateFormat,
                mDateFormatForAccessibility);

        mCityList = (RecyclerView) fragmentView.findViewById(R.id.cities);
        mCityList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mCityList.setAdapter(mCityAdapter);
        mCityList.setItemAnimator(null);
        DataModel.getDataModel().addCityListener(mCityAdapter);

        final ScrollPositionWatcher scrollPositionWatcher = new ScrollPositionWatcher();
        mCityList.addOnScrollListener(scrollPositionWatcher);

        final Context context = container.getContext();
        mCityList.setOnTouchListener(new CityListOnLongClickListener(context));
        fragmentView.setOnLongClickListener(new StartScreenSaverListener());

        // On tablet landscape, the clock frame will be a distinct view. Otherwise, it'll be added
        // on as a header to the main listview.
        mClockFrame = fragmentView.findViewById(R.id.main_clock_left_pane);
        if (mClockFrame != null) {
            mDigitalClock = (TextClock) mClockFrame.findViewById(R.id.digital_clock);
            mAnalogClock = (AnalogClock) mClockFrame.findViewById(R.id.analog_clock);
            Utils.setClockIconTypeface(mClockFrame);
            Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
            Utils.setClockStyle(mDigitalClock, mAnalogClock);
            Utils.setClockSecondsEnabled(mDigitalClock, mAnalogClock);
        }

        // Schedule a runnable to update the date every quarter hour.
        UiDataModel.getUiDataModel().addQuarterHourCallback(mQuarterHourUpdater, 100);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();

        final Activity activity = getActivity();

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        // Watch for system events that effect clock time or format.
        if (mAlarmChangeReceiver != null) {
            final IntentFilter filter = new IntentFilter(ACTION_NEXT_ALARM_CLOCK_CHANGED);
            activity.registerReceiver(mAlarmChangeReceiver, filter);
        }

        // Resume can be invoked after changing the clock style or seconds display.
        if (mDigitalClock != null && mAnalogClock != null) {
            Utils.setClockStyle(mDigitalClock, mAnalogClock);
            Utils.setClockSecondsEnabled(mDigitalClock, mAnalogClock);
        }

        final View view = getView();
        if (view != null && view.findViewById(R.id.main_clock_left_pane) != null) {
            // Center the main clock frame by hiding the world clocks when none are selected.
            mCityList.setVisibility(mCityAdapter.getItemCount() == 0 ? GONE : VISIBLE);
        }

        refreshAlarm();

        // Alarm observer is null on L or later.
        if (mAlarmObserver != null) {
            @SuppressWarnings("deprecation")
            final Uri uri = Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED);
            activity.getContentResolver().registerContentObserver(uri, false, mAlarmObserver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        final Activity activity = getActivity();
        if (mAlarmChangeReceiver != null) {
            activity.unregisterReceiver(mAlarmChangeReceiver);
        }
        if (mAlarmObserver != null) {
            activity.getContentResolver().unregisterContentObserver(mAlarmObserver);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        UiDataModel.getUiDataModel().removePeriodicCallback(mQuarterHourUpdater);
        DataModel.getDataModel().removeCityListener(mCityAdapter);
    }

    @Override
    public void onFabClick(@NonNull ImageView fab) {
        startActivity(new Intent(getActivity(), CitySelectionActivity.class));
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        fab.setVisibility(VISIBLE);
        fab.setImageResource(R.drawable.ic_public);
        fab.setContentDescription(fab.getResources().getString(R.string.button_cities));
    }

    @Override
    public void onUpdateFabButtons(@NonNull Button left, @NonNull Button right) {
        left.setVisibility(INVISIBLE);
        right.setVisibility(INVISIBLE);
    }

    @Override
    public final int getFabTargetVisibility() {
        return View.VISIBLE;
    }

    /**
     * Refresh the next alarm time.
     */
    private void refreshAlarm() {
        if (mClockFrame != null) {
            Utils.refreshAlarm(getActivity(), mClockFrame);
        } else {
            mCityAdapter.refreshAlarm();
        }
    }

    /**
     * Long pressing over the main clock starts the screen saver.
     */
    private final class StartScreenSaverListener implements View.OnLongClickListener {

        @Override
        public boolean onLongClick(View view) {
            startActivity(new Intent(getActivity(), ScreensaverActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));
            return true;
        }
    }

    /**
     * Long pressing over the city list starts the screen saver.
     */
    private final class CityListOnLongClickListener extends GestureDetector.SimpleOnGestureListener
            implements View.OnTouchListener {

        private final GestureDetector mGestureDetector;

        private CityListOnLongClickListener(Context context) {
            mGestureDetector = new GestureDetector(context, this);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            final View view = getView();
            if (view != null) {
                view.performLongClick();
            }
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
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
            mCityAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Prior to L, a ContentObserver was used to monitor changes to the next scheduled alarm.
     * In L and beyond this is accomplished via a system broadcast of
     * {@link AlarmManager#ACTION_NEXT_ALARM_CLOCK_CHANGED}.
     */
    private final class AlarmObserverPreL extends ContentObserver {
        private AlarmObserverPreL() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            refreshAlarm();
        }
    }

    /**
     * Update the display of the scheduled alarm as it changes.
     */
    private final class AlarmChangedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshAlarm();
        }
    }

    /**
     * Updates the vertical scroll state of this tab in the {@link UiDataModel} as the user scrolls
     * the recyclerview or when the size/position of elements within the recyclerview changes.
     */
    private final class ScrollPositionWatcher extends RecyclerView.OnScrollListener
            implements View.OnLayoutChangeListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            setTabScrolledToTop(Utils.isScrolledToTop(mCityList));
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                int oldLeft, int oldTop, int oldRight, int oldBottom) {
            setTabScrolledToTop(Utils.isScrolledToTop(mCityList));
        }
    }

    /**
     * This adapter lists all of the selected world clocks. Optionally, it also includes a clock at
     * the top for the home timezone if "Automatic home clock" is turned on in settings and the
     * current time at home does not match the current time in the timezone of the current location.
     * If the phone is in portrait mode it will also include the main clock at the top.
     */
    private static final class SelectedCitiesAdapter extends RecyclerView.Adapter
            implements CityListener {

        private final static int MAIN_CLOCK = R.layout.main_clock_frame;
        private final static int WORLD_CLOCK = R.layout.world_clock_item;

        private final LayoutInflater mInflater;
        private final Context mContext;
        private final boolean mIsPortrait;
        private final boolean mShowHomeClock;
        private final String mDateFormat;
        private final String mDateFormatForAccessibility;

        private SelectedCitiesAdapter(Context context, String dateFormat,
                String dateFormatForAccessibility) {
            mContext = context;
            mDateFormat = dateFormat;
            mDateFormatForAccessibility = dateFormatForAccessibility;
            mInflater = LayoutInflater.from(context);
            mIsPortrait = Utils.isPortrait(context);
            mShowHomeClock = DataModel.getDataModel().getShowHomeClock();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 && mIsPortrait) {
                return MAIN_CLOCK;
            }
            return WORLD_CLOCK;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(viewType, parent, false);
            switch (viewType) {
                case WORLD_CLOCK:
                    return new CityViewHolder(view);
                case MAIN_CLOCK:
                    return new MainClockViewHolder(view);
                default:
                    throw new IllegalArgumentException("View type not recognized");
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final int viewType = getItemViewType(position);
            switch (viewType) {
                case WORLD_CLOCK:
                    // Retrieve the city to bind.
                    final City city;
                    // If showing home clock, put it at the top
                    if (mShowHomeClock && position == (mIsPortrait ? 1 : 0)) {
                        city = getHomeCity();
                    } else {
                        final int positionAdjuster = (mIsPortrait ? 1 : 0)
                                + (mShowHomeClock ? 1 : 0);
                        city = getCities().get(position - positionAdjuster);
                    }
                    ((CityViewHolder) holder).bind(mContext, city, position, mIsPortrait);
                    break;
                case MAIN_CLOCK:
                    ((MainClockViewHolder) holder).bind(mContext, mDateFormat,
                            mDateFormatForAccessibility, getItemCount() > 1);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected view type: " + viewType);
            }
        }

        @Override
        public int getItemCount() {
            final int mainClockCount = mIsPortrait ? 1 : 0;
            final int homeClockCount = mShowHomeClock ? 1 : 0;
            final int worldClockCount = getCities().size();
            return mainClockCount + homeClockCount + worldClockCount;
        }

        private City getHomeCity() {
            return DataModel.getDataModel().getHomeCity();
        }

        private List<City> getCities() {
            return DataModel.getDataModel().getSelectedCities();
        }

        private void refreshAlarm() {
            if (mIsPortrait && getItemCount() > 0) {
                notifyItemChanged(0);
            }
        }

        @Override
        public void citiesChanged(List<City> oldCities, List<City> newCities) {
            notifyDataSetChanged();
        }

        private static final class CityViewHolder extends RecyclerView.ViewHolder {

            private final TextView mName;
            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;
            private final TextView mHoursAhead;

            private CityViewHolder(View itemView) {
                super(itemView);

                mName = (TextView) itemView.findViewById(R.id.city_name);
                mDigitalClock = (TextClock) itemView.findViewById(R.id.digital_clock);
                mAnalogClock = (AnalogClock) itemView.findViewById(R.id.analog_clock);
                mHoursAhead = (TextView) itemView.findViewById(R.id.hours_ahead);
            }

            private void bind(Context context, City city, int position, boolean isPortrait) {
                final String cityTimeZoneId = city.getTimeZone().getID();

                // Configure the digital clock or analog clock depending on the user preference.
                if (DataModel.getDataModel().getClockStyle() == DataModel.ClockStyle.ANALOG) {
                    mDigitalClock.setVisibility(GONE);
                    mAnalogClock.setVisibility(VISIBLE);
                    mAnalogClock.setTimeZone(cityTimeZoneId);
                    mAnalogClock.enableSeconds(false);
                } else {
                    mAnalogClock.setVisibility(GONE);
                    mDigitalClock.setVisibility(VISIBLE);
                    mDigitalClock.setTimeZone(cityTimeZoneId);
                    mDigitalClock.setFormat12Hour(Utils.get12ModeFormat(0.3f /* amPmRatio */,
                            false));
                    mDigitalClock.setFormat24Hour(Utils.get24ModeFormat(false));
                }

                // Supply top and bottom padding dynamically.
                final Resources res = context.getResources();
                final int padding = res.getDimensionPixelSize(R.dimen.medium_space_top);
                final int top = position == 0 && !isPortrait ? 0 : padding;
                final int left = itemView.getPaddingLeft();
                final int right = itemView.getPaddingRight();
                final int bottom = itemView.getPaddingBottom();
                itemView.setPadding(left, top, right, bottom);

                // Bind the city name.
                mName.setText(city.getName());

                // Compute if the city week day matches the weekday of the current timezone.
                final Calendar localCal = Calendar.getInstance(TimeZone.getDefault());
                final Calendar cityCal = Calendar.getInstance(city.getTimeZone());
                final boolean displayDayOfWeek =
                        localCal.get(DAY_OF_WEEK) != cityCal.get(DAY_OF_WEEK);

                // Compare offset from UTC time on today's date (daylight savings time, etc.)
                final TimeZone currentTimeZone = TimeZone.getDefault();
                final TimeZone cityTimeZone = TimeZone.getTimeZone(cityTimeZoneId);
                final long currentTimeMillis = System.currentTimeMillis();
                final long currentUtcOffset = currentTimeZone.getOffset(currentTimeMillis);
                final long cityUtcOffset = cityTimeZone.getOffset(currentTimeMillis);
                final long offsetDelta = cityUtcOffset - currentUtcOffset;

                final int hoursDifferent = (int) (offsetDelta / DateUtils.HOUR_IN_MILLIS);
                final int minutesDifferent = (int) (offsetDelta / DateUtils.MINUTE_IN_MILLIS) % 60;
                final boolean displayMinutes = offsetDelta % DateUtils.HOUR_IN_MILLIS != 0;
                final boolean isAhead = hoursDifferent > 0 || (hoursDifferent == 0
                        && minutesDifferent > 0);
                if (!Utils.isLandscape(context)) {
                    // Bind the number of hours ahead or behind, or hide if the time is the same.
                    final boolean displayDifference = hoursDifferent != 0 || displayMinutes;
                    mHoursAhead.setVisibility(displayDifference ? VISIBLE : GONE);
                    final String timeString = Utils.createHoursDifferentString(
                            context, displayMinutes, isAhead, hoursDifferent, minutesDifferent);
                    mHoursAhead.setText(displayDayOfWeek ?
                            (context.getString(isAhead ? R.string.world_hours_tomorrow
                                    : R.string.world_hours_yesterday, timeString))
                            : timeString);
                } else {
                    // Only tomorrow/yesterday should be shown in landscape view.
                    mHoursAhead.setVisibility(displayDayOfWeek ? View.VISIBLE : View.GONE);
                    if (displayDayOfWeek) {
                        mHoursAhead.setText(context.getString(isAhead ? R.string.world_tomorrow
                                : R.string.world_yesterday));
                    }

                }
            }
        }

        private static final class MainClockViewHolder extends RecyclerView.ViewHolder {

            private final View mHairline;
            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;

            private MainClockViewHolder(View itemView) {
                super(itemView);

                mHairline = itemView.findViewById(R.id.hairline);
                mDigitalClock = (TextClock) itemView.findViewById(R.id.digital_clock);
                mAnalogClock = (AnalogClock) itemView.findViewById(R.id.analog_clock);
                Utils.setClockIconTypeface(itemView);
            }

            private void bind(Context context, String dateFormat,
                    String dateFormatForAccessibility, boolean showHairline) {
                Utils.refreshAlarm(context, itemView);

                Utils.updateDate(dateFormat, dateFormatForAccessibility, itemView);
                Utils.setClockStyle(mDigitalClock, mAnalogClock);
                mHairline.setVisibility(showHairline ? VISIBLE : GONE);

                Utils.setClockSecondsEnabled(mDigitalClock, mAnalogClock);
            }
        }
    }
}
