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

package com.best.deskclock;

import static android.app.AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.best.deskclock.uidata.UiDataModel.Tab.CLOCKS;
import static java.util.Calendar.DAY_OF_WEEK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.data.City;
import com.best.deskclock.data.CityListener;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.Events;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.worldclock.CitySelectionActivity;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Fragment that shows the clock (analog or digital), the next alarm info and the world clock.
 */
public final class ClockFragment extends DeskClockFragment {

    // Updates dates in the UI on every quarter-hour.
    private final Runnable mQuarterHourUpdater = new QuarterHourRunnable();

    // Updates the UI in response to changes to the scheduled alarm.
    private BroadcastReceiver mAlarmChangeReceiver;

    private TextClock mDigitalClock;
    private AnalogClock mAnalogClock;
    private View mClockFrame;
    private SelectedCitiesAdapter mCityAdapter;
    private RecyclerView mCityList;
    private String mDateFormat;
    private String mDateFormatForAccessibility;

    public static boolean mIsPortrait;
    public static boolean mShowHomeClock;

    /**
     * The public no-arg constructor required by all fragments.
     */
    public ClockFragment() {
        super(CLOCKS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlarmChangeReceiver = new AlarmChangedBroadcastReceiver();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
        super.onCreateView(inflater, container, icicle);

        final View fragmentView = inflater.inflate(R.layout.clock_fragment, container, false);
        final ScrollPositionWatcher scrollPositionWatcher = new ScrollPositionWatcher();

        mIsPortrait = Utils.isPortrait(getContext());

        mShowHomeClock = DataModel.getDataModel().getShowHomeClock();

        mDateFormat = getContext().getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getContext().getString(R.string.full_wday_month_day_no_year);

        mCityAdapter = new SelectedCitiesAdapter(getContext(), mDateFormat, mDateFormatForAccessibility);
        DataModel.getDataModel().addCityListener(mCityAdapter);

        mCityList = fragmentView.findViewById(R.id.cities);
        mCityList.setLayoutManager(new LinearLayoutManager(getContext()));
        mCityList.setAdapter(mCityAdapter);
        mCityList.setItemAnimator(null);
        mCityList.addOnScrollListener(scrollPositionWatcher);
        mCityList.setOnTouchListener(new CityListOnLongClickListener(getContext()));
        fragmentView.setOnLongClickListener(new StartScreenSaverListener());

        // On tablet landscape, the clock frame will be a distinct view.
        // Otherwise, it'll be added on as a header to the main listview.
        mClockFrame = fragmentView.findViewById(R.id.main_clock_left_panel);
        if (mClockFrame != null) {
            mDigitalClock = mClockFrame.findViewById(R.id.digital_clock);
            mAnalogClock = mClockFrame.findViewById(R.id.analog_clock);
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

        mDateFormat = getContext().getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getContext().getString(R.string.full_wday_month_day_no_year);

        // Watch for system events that effect clock time or format.
        if (mAlarmChangeReceiver != null) {
            final IntentFilter filter = new IntentFilter(ACTION_NEXT_ALARM_CLOCK_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(mAlarmChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                activity.registerReceiver(mAlarmChangeReceiver, filter);
            }
        }

        // Resume can be invoked after changing the clock style or seconds display.
        if (mDigitalClock != null && mAnalogClock != null) {
            Utils.setClockStyle(mDigitalClock, mAnalogClock);
            Utils.setClockSecondsEnabled(mDigitalClock, mAnalogClock);
        }

        final View view = getView();
        if (view != null && view.findViewById(R.id.main_clock_left_panel) != null) {
            // Center the main clock frame by hiding the world clocks when none are selected.
            mCityList.setVisibility(mCityAdapter.getItemCount() == 0 ? GONE : VISIBLE);
        }

        // force refresh so clock style changes apply immediately
        mCityAdapter.citiesChanged();

        refreshAlarm();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mAlarmChangeReceiver != null) {
            getContext().unregisterReceiver(mAlarmChangeReceiver);
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
        startActivity(new Intent(getContext(), CitySelectionActivity.class));
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        fab.setVisibility(VISIBLE);
        fab.setImageResource(R.drawable.ic_fab_public);
        fab.setContentDescription(getContext().getResources().getString(R.string.button_cities));
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        left.setVisibility(INVISIBLE);
        right.setVisibility(INVISIBLE);
    }


    /**
     * Refresh the next alarm time.
     */
    private void refreshAlarm() {
        if (mClockFrame != null) {
            Utils.refreshAlarm(getContext(), mClockFrame);
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

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }
    }

    /**
     * This adapter lists all of the selected world clocks. Optionally, it also includes a clock at
     * the top for the home timezone if "Automatic home clock" is turned on in settings and the
     * current time at home does not match the current time in the timezone of the current location.
     * If the phone is in portrait mode it will also include the main clock at the top.
     */
    private static final class SelectedCitiesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements CityListener {

        private final static int MAIN_CLOCK = R.layout.main_clock_frame;
        private final static int WORLD_CLOCK = R.layout.world_clock_item;

        private final LayoutInflater mInflater;
        private final Context mContext;
        private final String mDateFormat;
        private final String mDateFormatForAccessibility;

        private SelectedCitiesAdapter(Context context, String dateFormat, String dateFormatForAccessibility) {
            mContext = context;
            mDateFormat = dateFormat;
            mDateFormatForAccessibility = dateFormatForAccessibility;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 && mIsPortrait) {
                return MAIN_CLOCK;
            }
            return WORLD_CLOCK;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(viewType, parent, false);
            if (viewType == WORLD_CLOCK) {
                return new CityViewHolder(view);
            } else if (viewType == MAIN_CLOCK) {
                return new MainClockViewHolder(view);
            }
            throw new IllegalArgumentException("View type not recognized");
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            final int viewType = getItemViewType(position);
            // Retrieve the city to bind.
            if (viewType == WORLD_CLOCK) {
                final City city;
                // If showing home clock, put it at the top
                if (mShowHomeClock && position == (mIsPortrait ? 1 : 0)) {
                    city = getHomeCity();
                } else {
                    final int positionAdjuster = (mIsPortrait ? 1 : 0) + (mShowHomeClock ? 1 : 0);
                    city = getCities().get(position - positionAdjuster);
                }
                ((CityViewHolder) holder).bind(mContext, city);
            } else if (viewType == MAIN_CLOCK) {
                ((MainClockViewHolder) holder).bind(mContext, mDateFormat,
                        mDateFormatForAccessibility);
            } else {
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
        public void citiesChanged() {
            notifyDataSetChanged();
        }

        private static final class CityViewHolder extends RecyclerView.ViewHolder {

            private final TextView mName;
            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;
            private final TextView mHoursAhead;

            private CityViewHolder(View itemView) {
                super(itemView);

                mName = itemView.findViewById(R.id.city_name);
                mDigitalClock = itemView.findViewById(R.id.digital_clock);
                mAnalogClock = itemView.findViewById(R.id.analog_clock);
                mHoursAhead = itemView.findViewById(R.id.hours_ahead);
            }

            private void bind(Context context, City city) {
                final String cityTimeZoneId = city.getTimeZone().getID();
                // Configure the digital clock or analog clock depending on the user preference.
                if (DataModel.getDataModel().getClockStyle() == DataModel.ClockStyle.ANALOG) {
                    mAnalogClock.getLayoutParams().height = Utils.toPixel(Utils.isTablet(context) ? 150 : 80, context);
                    mAnalogClock.getLayoutParams().width = Utils.toPixel(Utils.isTablet(context) ? 150 : 80, context);
                    mDigitalClock.setVisibility(GONE);
                    mAnalogClock.setVisibility(VISIBLE);
                    mAnalogClock.setTimeZone(cityTimeZoneId);
                    mAnalogClock.enableSeconds(false);
                } else {
                    mAnalogClock.setVisibility(GONE);
                    mDigitalClock.setVisibility(VISIBLE);
                    mDigitalClock.setTimeZone(cityTimeZoneId);
                    mDigitalClock.setFormat12Hour(Utils.get12ModeFormat(mDigitalClock.getContext(), 0.3f, false));
                    mDigitalClock.setFormat24Hour(Utils.get24ModeFormat(mDigitalClock.getContext(), false));
                }

                itemView.setBackground(Utils.cardBackground(context));

                // Supply margins dynamically.
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                final int marginLeft = Utils.toPixel(10, context);
                final int marginRight = Utils.toPixel(10, context);
                final int marginBottom = DataModel.getDataModel().getSelectedCities().size() > 1 || mShowHomeClock
                        ? Utils.toPixel(10, context)
                        : Utils.toPixel(0, context);
                params.setMargins(marginLeft, 0, marginRight, marginBottom);
                itemView.setLayoutParams(params);

                // Bind the city name.
                mName.setText(city.getName());

                // Compute if the city week day matches the weekday of the current timezone.
                final Calendar localCal = Calendar.getInstance(TimeZone.getDefault());
                final Calendar cityCal = Calendar.getInstance(city.getTimeZone());
                final boolean displayDayOfWeek = localCal.get(DAY_OF_WEEK) != cityCal.get(DAY_OF_WEEK);

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
                final boolean isAhead = hoursDifferent > 0 || (hoursDifferent == 0 && minutesDifferent > 0);
                final boolean displayDifference = hoursDifferent != 0 || displayMinutes;

                mHoursAhead.setVisibility(displayDifference ? VISIBLE : GONE);
                final String timeString = Utils.createHoursDifferentString(
                        context, displayMinutes, isAhead, hoursDifferent, minutesDifferent);
                mHoursAhead.setText(displayDayOfWeek
                        ? (context.getString(isAhead
                            ? R.string.world_hours_tomorrow
                            : R.string.world_hours_yesterday, timeString))
                        : timeString);

                if (!mIsPortrait) {
                    LinearLayout.LayoutParams textViewLayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    mName.setLayoutParams(textViewLayoutParams);
                    mName.setGravity(Gravity.CENTER_HORIZONTAL);
                    mHoursAhead.setLayoutParams(textViewLayoutParams);
                    mHoursAhead.setGravity(Gravity.CENTER_HORIZONTAL);
                }
            }
        }

        private static final class MainClockViewHolder extends RecyclerView.ViewHolder {

            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;

            private MainClockViewHolder(View itemView) {
                super(itemView);

                mDigitalClock = itemView.findViewById(R.id.digital_clock);
                mAnalogClock = itemView.findViewById(R.id.analog_clock);
                Utils.setClockIconTypeface(itemView);
            }

            private void bind(Context context, String dateFormat, String dateFormatForAccessibility) {
                Utils.refreshAlarm(context, itemView);
                Utils.updateDate(dateFormat, dateFormatForAccessibility, itemView);
                Utils.setClockStyle(mDigitalClock, mAnalogClock);
                Utils.setClockSecondsEnabled(mDigitalClock, mAnalogClock);
            }
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
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            setTabScrolledToTop(Utils.isScrolledToTop(mCityList));
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                   int oldLeft, int oldTop, int oldRight, int oldBottom) {
            setTabScrolledToTop(Utils.isScrolledToTop(mCityList));
        }
    }
}
