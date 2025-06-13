/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLACK_ACCENT_COLOR;
import static com.best.deskclock.uidata.UiDataModel.Tab.CLOCKS;
import static com.best.deskclock.utils.AlarmUtils.ACTION_NEXT_ALARM_CHANGED_BY_CLOCK;
import static java.util.Calendar.DAY_OF_WEEK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.data.City;
import com.best.deskclock.data.CityListener;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.AnalogClock;
import com.best.deskclock.worldclock.CitySelectionActivity;
import com.google.android.material.card.MaterialCardView;

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
    private Context mContext;

    public static SharedPreferences mPrefs;
    public static DataModel.ClockStyle mClockStyle;
    public static boolean mAreClockSecondsDisplayed;
    public static boolean mIsPortrait;
    public static boolean mIsLandscape;
    public static boolean mIsTablet;
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle icicle) {
        super.onCreateView(inflater, container, icicle);

        final View fragmentView = inflater.inflate(R.layout.clock_fragment, container, false);
        final ScrollPositionWatcher scrollPositionWatcher = new ScrollPositionWatcher();

        mContext = requireContext();
        mPrefs = getDefaultSharedPreferences(mContext);
        mClockStyle = SettingsDAO.getClockStyle(mPrefs);
        mAreClockSecondsDisplayed = SettingsDAO.areClockSecondsDisplayed(mPrefs);
        mIsPortrait = ThemeUtils.isPortrait();
        mIsLandscape = ThemeUtils.isLandscape();
        mIsTablet = ThemeUtils.isTablet();
        mShowHomeClock = SettingsDAO.getShowHomeClock(mContext, mPrefs);
        mDateFormat = mContext.getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = mContext.getString(R.string.full_wday_month_day_no_year);

        mCityAdapter = new SelectedCitiesAdapter(mContext, mDateFormat, mDateFormatForAccessibility);
        DataModel.getDataModel().addCityListener(mCityAdapter);

        mCityList = fragmentView.findViewById(R.id.cities);
        mCityList.setLayoutManager(new LinearLayoutManager(mContext));
        mCityList.setAdapter(mCityAdapter);
        mCityList.setItemAnimator(null);
        mCityList.addOnScrollListener(scrollPositionWatcher);
        // Due to the ViewPager and the location of FAB, set a bottom padding to prevent
        // the city list from being hidden by the FAB (e.g. when scrolling down).
        mCityList.setPadding(0, 0, 0, ThemeUtils.convertDpToPixels(
                mIsTablet && mIsPortrait ? 106 : mIsPortrait ? 91 : 0, mContext));

        // On landscape mode, the clock frame will be a distinct view.
        // Otherwise, it'll be added on as a header to the main listview.
        mClockFrame = fragmentView.findViewById(R.id.main_clock_left_panel);
        if (mClockFrame != null) {
            mClockFrame.setPadding(0, 0, 0, 0);
            mDigitalClock = mClockFrame.findViewById(R.id.digital_clock);
            mAnalogClock = mClockFrame.findViewById(R.id.analog_clock);
            ClockUtils.setClockIconTypeface(mClockFrame);
            ClockUtils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
            ClockUtils.setClockStyle(mClockStyle, mDigitalClock, mAnalogClock);
            ClockUtils.setClockSecondsEnabled(mClockStyle, mDigitalClock, mAnalogClock, mAreClockSecondsDisplayed);
        }

        // Schedule a runnable to update the date every quarter hour.
        UiDataModel.getUiDataModel().addQuarterHourCallback(mQuarterHourUpdater, 100);

        return fragmentView;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onResume() {
        super.onResume();

        final Activity activity = requireActivity();

        mDateFormat = mContext.getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = mContext.getString(R.string.full_wday_month_day_no_year);

        // Watch for system events that effect clock time or format.
        if (mAlarmChangeReceiver != null) {
            final IntentFilter filter = new IntentFilter(ACTION_NEXT_ALARM_CHANGED_BY_CLOCK);
            if (SdkUtils.isAtLeastAndroid13()) {
                activity.registerReceiver(mAlarmChangeReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                activity.registerReceiver(mAlarmChangeReceiver, filter);
            }
        }

        // Resume can be invoked after changing the clock style or seconds display.
        if (mDigitalClock != null && mAnalogClock != null) {
            ClockUtils.setClockStyle(mClockStyle, mDigitalClock, mAnalogClock);
            ClockUtils.setClockSecondsEnabled(mClockStyle, mDigitalClock, mAnalogClock, mAreClockSecondsDisplayed);
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
            mContext.unregisterReceiver(mAlarmChangeReceiver);
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
        startActivity(new Intent(mContext, CitySelectionActivity.class));
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        fab.setVisibility(VISIBLE);
        fab.setImageResource(R.drawable.ic_fab_public);
        fab.setContentDescription(mContext.getResources().getString(R.string.button_cities));
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
            AlarmUtils.refreshAlarm(getContext(), mClockFrame);
        } else {
            mCityAdapter.refreshAlarm();
        }
    }

    /**
     * This adapter lists all of the selected world clocks. Optionally, it also includes a clock at
     * the top for the home timezone if "Automatic home clock" is turned on in settings and the
     * current time at home does not match the current time in the timezone of the current location.
     * If the phone is in portrait mode it will also include the main clock at the top.
     */
    public static final class SelectedCitiesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements CityListener {

        private final static int MAIN_CLOCK = R.layout.main_clock_frame;
        private final static int WORLD_CLOCK = R.layout.world_clock_item;

        private final LayoutInflater mInflater;
        private final Context mContext;
        private final String mDateFormat;
        private final String mDateFormatForAccessibility;

        public SelectedCitiesAdapter(Context context, String dateFormat, String dateFormatForAccessibility) {
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
            private final MaterialCardView mDigitalClockContainer;
            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;
            private final TextView mHoursAhead;

            private CityViewHolder(View itemView) {
                super(itemView);

                mName = itemView.findViewById(R.id.city_name);
                mDigitalClockContainer = itemView.findViewById(R.id.digital_clock_container);
                mDigitalClock = itemView.findViewById(R.id.digital_clock);
                mAnalogClock = itemView.findViewById(R.id.analog_clock);
                mHoursAhead = itemView.findViewById(R.id.hours_ahead);
            }

            private void bind(Context context, City city) {
                final String cityTimeZoneId = city.getTimeZone().getID();
                final boolean isPhoneInLandscapeMode = !mIsTablet && mIsLandscape;
                final boolean isAnalogClock = mClockStyle == DataModel.ClockStyle.ANALOG
                        || mClockStyle == DataModel.ClockStyle.ANALOG_MATERIAL;
                int paddingVertical = ThemeUtils.convertDpToPixels(isAnalogClock ? 12 : 18, context);

                itemView.setBackground(ThemeUtils.cardBackground(context));
                itemView.setPadding(itemView.getPaddingLeft(), paddingVertical, itemView.getPaddingRight(), paddingVertical);

                // Configure the digital clock or analog clock depending on the user preference.
                if (isAnalogClock) {
                    mAnalogClock.getLayoutParams().height = ThemeUtils.convertDpToPixels(mIsTablet ? 150 : 80, context);
                    mAnalogClock.getLayoutParams().width = ThemeUtils.convertDpToPixels(mIsTablet ? 150 : 80, context);
                    mDigitalClockContainer.setVisibility(GONE);
                    mAnalogClock.setVisibility(VISIBLE);
                    mAnalogClock.setTimeZone(cityTimeZoneId);
                    mAnalogClock.enableSeconds(false);
                } else {
                    mAnalogClock.setVisibility(GONE);
                    mDigitalClockContainer.setVisibility(VISIBLE);

                    if (SettingsDAO.getAccentColor(mPrefs).equals(BLACK_ACCENT_COLOR)) {
                        mDigitalClock.setTextColor(Color.WHITE);
                    }
                    mDigitalClock.setTimeZone(cityTimeZoneId);
                    mDigitalClock.setFormat12Hour(
                            ClockUtils.get12ModeFormat(mDigitalClock.getContext(), 0.3f, false));
                    mDigitalClock.setFormat24Hour(
                            ClockUtils.get24ModeFormat(mDigitalClock.getContext(), false));
                }

                // Due to the ViewPager and the location of FAB, set margins to prevent
                // the city list from being hidden by the FAB (e.g. when scrolling down).
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                final int leftMargin = ThemeUtils.convertDpToPixels(isPhoneInLandscapeMode ? 0 : 10, context);
                final int rightMargin =  ThemeUtils.convertDpToPixels(isPhoneInLandscapeMode ? 90 : 10, context);
                final int bottomMargin = ThemeUtils.convertDpToPixels(
                        DataModel.getDataModel().getSelectedCities().size() > 1 || mShowHomeClock ? 8 : 0, context);
                params.setMargins(leftMargin, 0, rightMargin, bottomMargin);
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
                final String timeString = createHoursDifferentString(
                        context, displayMinutes, isAhead, hoursDifferent, minutesDifferent);
                mHoursAhead.setText(displayDayOfWeek
                        ? (context.getString(isAhead
                            ? R.string.world_hours_tomorrow
                            : R.string.world_hours_yesterday, timeString))
                        : timeString);

                // Allow text scrolling by clicking on the item (all other attributes are indicated
                // in the "world_clock_city_container.xml" file)
                itemView.setOnClickListener(v -> mName.setSelected(true));
            }
        }

        /**
         * @param context          to obtain strings.
         * @param displayMinutes   whether or not minutes should be included
         * @param isAhead          {@code true} if the time should be marked 'ahead', else 'behind'
         * @param hoursDifferent   the number of hours the time is ahead/behind
         * @param minutesDifferent the number of minutes the time is ahead/behind
         * @return String describing the hours/minutes ahead or behind
         */
        public static String createHoursDifferentString(Context context, boolean displayMinutes,
                                                        boolean isAhead, int hoursDifferent, int minutesDifferent) {

            String timeString;
            if (displayMinutes && hoursDifferent != 0) {
                // Both minutes and hours
                final String hoursShortQuantityString = Utils.getNumberFormattedQuantityString(context, R.plurals.hours_short, Math.abs(hoursDifferent));
                final String minsShortQuantityString = Utils.getNumberFormattedQuantityString(context, R.plurals.minutes_short, Math.abs(minutesDifferent));
                final @StringRes int stringType = isAhead ? R.string.world_hours_minutes_ahead : R.string.world_hours_minutes_behind;
                timeString = context.getString(stringType, hoursShortQuantityString, minsShortQuantityString);
            } else {
                // Minutes alone or hours alone
                final String hoursQuantityString = Utils.getNumberFormattedQuantityString(context, R.plurals.hours, Math.abs(hoursDifferent));
                final String minutesQuantityString = Utils.getNumberFormattedQuantityString(context, R.plurals.minutes, Math.abs(minutesDifferent));
                final @StringRes int stringType = isAhead ? R.string.world_time_ahead : R.string.world_time_behind;
                timeString = context.getString(stringType, displayMinutes ? minutesQuantityString : hoursQuantityString);
            }
            return timeString;
        }

        private static final class MainClockViewHolder extends RecyclerView.ViewHolder {

            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;

            private MainClockViewHolder(View itemView) {
                super(itemView);

                mDigitalClock = itemView.findViewById(R.id.digital_clock);
                mAnalogClock = itemView.findViewById(R.id.analog_clock);
                ClockUtils.setClockIconTypeface(itemView);
            }

            private void bind(Context context, String dateFormat, String dateFormatForAccessibility) {
                AlarmUtils.refreshAlarm(context, itemView);
                ClockUtils.updateDate(dateFormat, dateFormatForAccessibility, itemView);
                ClockUtils.setClockStyle(mClockStyle, mDigitalClock, mAnalogClock);
                ClockUtils.setClockSecondsEnabled(mClockStyle, mDigitalClock, mAnalogClock, mAreClockSecondsDisplayed);
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
