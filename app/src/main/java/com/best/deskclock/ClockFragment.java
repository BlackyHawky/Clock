/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static android.app.Activity.OVERRIDE_TRANSITION_OPEN;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLACK_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_CITIES_MANUALLY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CITY_NOTE;
import static com.best.deskclock.uidata.UiDataModel.Tab.CLOCKS;
import static com.best.deskclock.utils.AlarmUtils.ACTION_NEXT_ALARM_CHANGED_BY_CLOCK;
import static java.util.Calendar.DAY_OF_WEEK;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
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
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.data.City;
import com.best.deskclock.data.CityListener;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.worldclock.CitySelectionActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

    private Context mContext;
    private SharedPreferences mPrefs;
    private final List<City> mMutableCities = new ArrayList<>();
    private View mClockFrame;
    private View mEmptyCityViewRightPanel;
    private SelectedCitiesAdapter mCityAdapter;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private boolean mIsPortrait;
    private boolean mShowHomeClock;

    /**
     * The public no-arg constructor required by all fragments.
     */
    public ClockFragment() {
        super(CLOCKS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = requireContext();
        mPrefs = getDefaultSharedPreferences(mContext);
        mShowHomeClock = SettingsDAO.getShowHomeClock(mContext, mPrefs);
        mIsPortrait = ThemeUtils.isPortrait();
        mDateFormat = mContext.getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = mContext.getString(R.string.full_wday_month_day_no_year);

        mMutableCities.clear();
        mMutableCities.addAll(DataModel.getDataModel().getSelectedCities());

        mCityAdapter = new SelectedCitiesAdapter(mContext, mDateFormat, mDateFormatForAccessibility,
                mMutableCities, mShowHomeClock, mIsPortrait);
        DataModel.getDataModel().addCityListener(mCityAdapter);

        mAlarmChangeReceiver = new AlarmChangedBroadcastReceiver();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle icicle) {
        super.onCreateView(inflater, container, icicle);

        final View fragmentView = inflater.inflate(R.layout.clock_fragment, container, false);

        // On landscape mode, the clock frame will be a distinct view.
        // Otherwise, it'll be added on as a header to the main listview.
        mClockFrame = fragmentView.findViewById(R.id.main_clock_left_panel);
        if (mClockFrame != null) {
            DataModel.ClockStyle clockStyle = SettingsDAO.getClockStyle(mPrefs);
            TextClock digitalClock = mClockFrame.findViewById(R.id.digital_clock);
            AnalogClock analogClock = mClockFrame.findViewById(R.id.analog_clock);
            boolean showSeconds = SettingsDAO.areClockSecondsDisplayed(mPrefs);

            mClockFrame.setPadding(0, 0, 0, 0);

            ClockUtils.setClockIconTypeface(mClockFrame);
            ClockUtils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
            ClockUtils.setClockStyle(clockStyle, digitalClock, analogClock);
            ClockUtils.setClockSecondsEnabled(clockStyle, digitalClock, analogClock, showSeconds);
        }

        mEmptyCityViewRightPanel = fragmentView.findViewById(R.id.empty_city_view_right_panel);

        final RecyclerView cityList = fragmentView.findViewById(R.id.cities);
        cityList.setAdapter(mCityAdapter);
        cityList.setLayoutManager(new LinearLayoutManager(mContext));
        // Due to the ViewPager and the location of FAB, set a bottom padding to prevent
        // the city list from being hidden by the FAB (e.g. when scrolling down).
        cityList.setPadding(0, 0, 0, ThemeUtils.convertDpToPixels(
                ThemeUtils.isTablet() && mIsPortrait ? 106 : mIsPortrait ? 91 : 0, mContext));

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {

                int position = viewHolder.getBindingAdapterPosition();

                boolean isMainClock = position == 0 && mIsPortrait;
                boolean isHomeClock = mShowHomeClock && position == (mIsPortrait ? 1 : 0);
                if (isMainClock || isHomeClock) {
                    return 0;
                }

                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();

                int offset = (mIsPortrait ? 1 : 0) + (mShowHomeClock ? 1 : 0);
                if (from < offset || to < offset) {
                    return false;
                }

                int fromIndex = from - offset;
                int toIndex = to - offset;

                Collections.swap(mMutableCities, fromIndex, toIndex);

                mCityAdapter.notifyItemMoved(from, to);

                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                // Saving the new order
                DataModel.getDataModel().updateSelectedCitiesOrder(mMutableCities);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {

                // Draw a shadow under the timer card when it's dragging
                viewHolder.itemView.setTranslationZ(
                        (float) ThemeUtils.convertDpToPixels(isCurrentlyActive ? 6 : 0, mContext));

                // Calculation of upper and lower limits for drag
                int position = viewHolder.getBindingAdapterPosition();
                int offset = (mIsPortrait ? 1 : 0) + (mShowHomeClock ? 1 : 0);

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && position >= offset) {
                    // Upper limit
                    RecyclerView.ViewHolder firstHolder = recyclerView.findViewHolderForAdapterPosition(offset);
                    float minY = firstHolder != null
                            ? firstHolder.itemView.getTop()
                            : 0; // Fallback

                    // Bottom limit
                    int lastIndex = mCityAdapter.getItemCount() - 1;
                    RecyclerView.ViewHolder lastHolder = recyclerView.findViewHolderForAdapterPosition(lastIndex);

                    float maxY = lastHolder != null
                            ? lastHolder.itemView.getBottom()
                            : recyclerView.getHeight() - recyclerView.getPaddingBottom();

                    // Calculation of the projection
                    View movingView = viewHolder.itemView;
                    float currentTop = movingView.getTop();
                    float currentBottom = movingView.getBottom();

                    float projectedTop = currentTop + dY;
                    float projectedBottom = currentBottom + dY;

                    // Adjustment
                    float newDY = dY;

                    if (projectedTop < minY) {
                        newDY = minY - currentTop;
                    } else if (projectedBottom > maxY) {
                        newDY = maxY - currentBottom;
                    }

                    super.onChildDraw(c, recyclerView, viewHolder, dX, newDY, actionState, isCurrentlyActive);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }
        });

        if (SettingsDAO.getCitySorting(mPrefs).equals(SORT_CITIES_MANUALLY)) {
            itemTouchHelper.attachToRecyclerView(cityList);
        } else {
            itemTouchHelper.attachToRecyclerView(null);
        }

        // Schedule a runnable to update the date every quarter hour.
        UiDataModel.getUiDataModel().addQuarterHourCallback(mQuarterHourUpdater, 100);

        return fragmentView;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onResume() {
        super.onResume();

        // Watch for system events that effect clock time or format.
        if (mAlarmChangeReceiver != null) {
            final IntentFilter filter = new IntentFilter(ACTION_NEXT_ALARM_CHANGED_BY_CLOCK);
            if (SdkUtils.isAtLeastAndroid13()) {
                mContext.registerReceiver(mAlarmChangeReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                mContext.registerReceiver(mAlarmChangeReceiver, filter);
            }
        }

        refreshAlarm();

        if (mEmptyCityViewRightPanel != null) {
            if (!mShowHomeClock && mCityAdapter.getCities().isEmpty()) {
                mEmptyCityViewRightPanel.setVisibility(VISIBLE);

                ViewGroup.LayoutParams params = mEmptyCityViewRightPanel.getLayoutParams();
                int screenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
                params.height = (int) (screenHeight / 2f);
                mEmptyCityViewRightPanel.setLayoutParams(params);
            } else {
                mEmptyCityViewRightPanel.setVisibility(GONE);
            }
        }
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
    public void onFabClick() {
        startActivity(new Intent(mContext, CitySelectionActivity.class));
        if (SettingsDAO.isFadeTransitionsEnabled(mPrefs)) {
            if (SdkUtils.isAtLeastAndroid14()) {
                requireActivity().overrideActivityTransition(OVERRIDE_TRANSITION_OPEN,
                        R.anim.fade_in, R.anim.fade_out);
            } else {
                requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        } else {
            if (SdkUtils.isAtLeastAndroid14()) {
                requireActivity().overrideActivityTransition(OVERRIDE_TRANSITION_OPEN,
                        R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
            } else {
                requireActivity().overridePendingTransition(
                        R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
            }
        }
    }

    @Override
    public void onFabLongClick(@NonNull ImageView fab) {
        fab.setHapticFeedbackEnabled(false);
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
     * Updates the note associated with a specific city by delegating to the adapter.
     * <p>
     * This method is typically called by the hosting activity after the user saves a city note
     * via the dialog. It ensures that the adapter updates the displayed note and persists it as needed.
     *
     * @param cityId the unique identifier of the city whose note is being updated
     * @param note   the new note text to associate with the city
     */
    public void setCityNote(String cityId, String note) {
        mCityAdapter.setCityNote(cityId, note);
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
        private final SharedPreferences mPrefs;
        private final String mDateFormat;
        private final String mDateFormatForAccessibility;
        private final List<City> mCities;
        private final boolean mIsPortrait;
        private final boolean mShowHomeClock;

        public SelectedCitiesAdapter(Context context, String dateFormat,
                                     String dateFormatForAccessibility, List<City> cities,
                                     boolean showHomeClock, boolean isPortrait) {

            mContext = context;
            mPrefs = getDefaultSharedPreferences(context);
            mDateFormat = dateFormat;
            mDateFormatForAccessibility = dateFormatForAccessibility;
            mInflater = LayoutInflater.from(context);
            mCities = cities;
            mShowHomeClock = showHomeClock;
            mIsPortrait = isPortrait;
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
                return new CityViewHolder(view, this);
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
                ((CityViewHolder) holder).bind(mContext, mCities, city, mShowHomeClock, mIsPortrait);
            } else if (viewType == MAIN_CLOCK) {
                ((MainClockViewHolder) holder).bind(mContext, mDateFormat, mDateFormatForAccessibility,
                        mCities, mShowHomeClock, mIsPortrait);
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
            return mCities;
        }

        private void refreshAlarm() {
            if (mIsPortrait && getItemCount() > 0) {
                notifyItemChanged(0);
            }
        }

        private int getCityPositionById(String cityId) {
            final int positionAdjuster = (mIsPortrait ? 1 : 0) + (mShowHomeClock ? 1 : 0);

            for (int i = 0; i < mCities.size(); i++) {
                if (mCities.get(i).getId().equals(cityId)) {
                    return i + positionAdjuster;
                }
            }

            return RecyclerView.NO_POSITION;
        }

        public void setCityNote(String cityId, String note) {
            SharedPreferences.Editor editor = mPrefs.edit();
            String key = KEY_CITY_NOTE + cityId;

            if (note.trim().isEmpty()) {
                editor.remove(key);
            } else {
                editor.putString(key, note);
            }

            editor.apply();

            int position = getCityPositionById(cityId);
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position);
            }
        }

        @Nullable
        public String getCityNote(String cityId) {
            return mPrefs.getString(KEY_CITY_NOTE + cityId, null);
        }

        @Override
        public void citiesChanged() {
            List<City> newCities = DataModel.getDataModel().getSelectedCities();

            if (!mCities.equals(newCities)) {
                mCities.clear();
                mCities.addAll(newCities);
            }

            notifyDataSetChanged();
        }

        private static final class CityViewHolder extends RecyclerView.ViewHolder {

            private final SharedPreferences mPrefs;
            private final SelectedCitiesAdapter mAdapter;
            private final TextView mName;
            private final DataModel.ClockStyle mClockStyle;
            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;
            private final TextView mHoursAhead;
            private final boolean mIsTablet;

            private CityViewHolder(View itemView, SelectedCitiesAdapter adapter) {
                super(itemView);

                mPrefs = getDefaultSharedPreferences(itemView.getContext());
                mAdapter = adapter;
                mClockStyle = SettingsDAO.getClockStyle(mPrefs);
                mName = itemView.findViewById(R.id.city_name);
                mDigitalClock = itemView.findViewById(R.id.digital_clock);
                mAnalogClock = itemView.findViewById(R.id.analog_clock);
                mHoursAhead = itemView.findViewById(R.id.hours_ahead);
                mIsTablet = ThemeUtils.isTablet();
            }

            private void bind(Context context, List<City> cities, City city, boolean showHomeClock, boolean isPortrait) {
                final String cityTimeZoneId = city.getTimeZone().getID();
                final boolean isPhoneInLandscapeMode = !mIsTablet && !isPortrait;
                final boolean isAnalogClock = mClockStyle == DataModel.ClockStyle.ANALOG
                        || mClockStyle == DataModel.ClockStyle.ANALOG_MATERIAL;
                int paddingVertical = ThemeUtils.convertDpToPixels(isAnalogClock ? 12 : 18, context);

                itemView.setBackground(ThemeUtils.cardBackground(context));
                itemView.setPadding(itemView.getPaddingLeft(), paddingVertical, itemView.getPaddingRight(), paddingVertical);

                // Configure the digital clock or analog clock depending on the user preference.
                if (isAnalogClock) {
                    mDigitalClock.setVisibility(GONE);
                    mAnalogClock.getLayoutParams().height = ThemeUtils.convertDpToPixels(mIsTablet ? 150 : 80, context);
                    mAnalogClock.getLayoutParams().width = ThemeUtils.convertDpToPixels(mIsTablet ? 150 : 80, context);
                    mAnalogClock.setVisibility(VISIBLE);
                    mAnalogClock.setTimeZone(cityTimeZoneId);
                    mAnalogClock.enableSeconds(false);
                } else {
                    mAnalogClock.setVisibility(GONE);
                    mDigitalClock.setBackground(ThemeUtils.pillBackground(
                            context, com.google.android.material.R.attr.colorSecondary));
                    if (SettingsDAO.getAccentColor(mPrefs).equals(BLACK_ACCENT_COLOR)) {
                        mDigitalClock.setTextColor(Color.WHITE);
                    }
                    mDigitalClock.setTimeZone(cityTimeZoneId);
                    mDigitalClock.setFormat12Hour(
                            ClockUtils.get12ModeFormat(mDigitalClock.getContext(), 0.3f, false));
                    mDigitalClock.setFormat24Hour(
                            ClockUtils.get24ModeFormat(mDigitalClock.getContext(), false));
                    mDigitalClock.setVisibility(VISIBLE);
                }

                // Due to the ViewPager and the location of FAB, set margins to prevent
                // the city list from being hidden by the FAB (e.g. when scrolling down).
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                final int leftMargin = ThemeUtils.convertDpToPixels(isPhoneInLandscapeMode ? 0 : 10, context);
                final int rightMargin =  ThemeUtils.convertDpToPixels(isPhoneInLandscapeMode ? 90 : 10, context);
                final int bottomMargin = ThemeUtils.convertDpToPixels(cities.size() > 1 || showHomeClock
                        ? 8
                        : 0, context);
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
                mName.setSelected(true);

                TextView cityNoteView = itemView.findViewById(R.id.city_note);
                String note = mAdapter.getCityNote(city.getId());

                if (SettingsDAO.isCityNoteEnabled(mPrefs)) {
                    if (note != null && !note.trim().isEmpty()) {
                        cityNoteView.setVisibility(View.VISIBLE);
                        cityNoteView.setText(note.trim());
                    } else {
                        cityNoteView.setVisibility(View.GONE);
                    }

                    itemView.setOnClickListener(v -> {
                        LabelDialogFragment labelDialogFragment = LabelDialogFragment.newInstance(
                                city.getId(),
                                city.getName(),
                                note,
                                CLOCKS.name());

                        LabelDialogFragment.show(
                                ((AppCompatActivity) context).getSupportFragmentManager(),
                                labelDialogFragment);
                    });
                } else {
                    cityNoteView.setVisibility(View.GONE);
                }
            }
        }

        private static final class MainClockViewHolder extends RecyclerView.ViewHolder {

            private final View mMainClockContainer;
            private final View mEmptyCityView;
            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;
            private final DataModel.ClockStyle mClockStyle;
            private final boolean mAreClockSecondsDisplayed;

            private MainClockViewHolder(View itemView) {
                super(itemView);

                final SharedPreferences prefs = getDefaultSharedPreferences(itemView.getContext());
                mMainClockContainer = itemView.findViewById(R.id.main_clock_container);
                mEmptyCityView = itemView.findViewById(R.id.cities_empty_view);
                mDigitalClock = itemView.findViewById(R.id.digital_clock);
                mAnalogClock = itemView.findViewById(R.id.analog_clock);
                mClockStyle = SettingsDAO.getClockStyle(prefs);
                mAreClockSecondsDisplayed = SettingsDAO.areClockSecondsDisplayed(prefs);
                ClockUtils.setClockIconTypeface(itemView);
            }

            private void bind(Context context, String dateFormat, String dateFormatForAccessibility,
                              List<City> selectedCities, boolean showHomeClock, boolean isPortrait) {

                ViewGroup.LayoutParams mainClockparams = mMainClockContainer.getLayoutParams();

                if (isPortrait) {
                    if (selectedCities.isEmpty() && !showHomeClock) {
                        mainClockparams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                        mMainClockContainer.setPadding(0, 0, 0, 0);

                        mEmptyCityView.setVisibility(View.VISIBLE);
                    } else {
                        mainClockparams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        mMainClockContainer.setPadding(0, 0, 0, ThemeUtils.convertDpToPixels(20, context));
                        mEmptyCityView.setVisibility(View.GONE);
                    }

                    mMainClockContainer.setLayoutParams(mainClockparams);
                } else {
                    mEmptyCityView.setVisibility(View.GONE);
                }

                AlarmUtils.refreshAlarm(context, itemView);
                ClockUtils.updateDate(dateFormat, dateFormatForAccessibility, itemView);
                ClockUtils.setClockStyle(mClockStyle, mDigitalClock, mAnalogClock);
                ClockUtils.setClockSecondsEnabled(mClockStyle, mDigitalClock, mAnalogClock, mAreClockSecondsDisplayed);
            }
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

}
