/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.clock;

import static android.app.Activity.OVERRIDE_TRANSITION_OPEN;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_CITIES_MANUALLY;
import static com.best.deskclock.uidata.UiDataModel.Tab.CLOCKS;
import static com.best.deskclock.utils.AlarmUtils.ACTION_NEXT_ALARM_CHANGED_BY_CLOCK;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.DeskClockFragment;
import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.dialogfragment.LabelDialogFragment;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uicomponents.AutoSizingTextClock;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.worldclock.CitySelectionActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private DisplayMetrics mDisplayMetrics;
    private final List<City> mMutableCities = new ArrayList<>();
    private View mClockFrame;
    private DataModel.ClockStyle mClockStyle;
    private boolean mIsDigitalClock;
    private boolean mShowSeconds;
    private View mEmptyCityViewRightPanel;
    private SelectedCitiesAdapter mCityAdapter;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private boolean mIsPortrait;
    private boolean mIsTablet;
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
        mDisplayMetrics = getResources().getDisplayMetrics();
        mClockStyle = SettingsDAO.getClockStyle(mPrefs);
        mShowHomeClock = SettingsDAO.getShowHomeClock(mContext, mPrefs);
        mShowSeconds = SettingsDAO.areClockSecondsDisplayed(mPrefs);
        mIsDigitalClock = mClockStyle == DataModel.ClockStyle.DIGITAL;
        mIsPortrait = ThemeUtils.isPortrait();
        mIsTablet = ThemeUtils.isTablet();
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
            AnalogClock analogClock = mClockFrame.findViewById(R.id.analog_clock);
            AutoSizingTextClock digitalClock = mClockFrame.findViewById(R.id.digital_clock);

            mClockFrame.setPadding(0, 0, 0, 0);
            ClockUtils.setClockStyle(mClockStyle, digitalClock, analogClock);
            if (mIsDigitalClock) {
                ClockUtils.setDigitalClockFont(digitalClock, SettingsDAO.getDigitalClockFont(mPrefs));
                ClockUtils.setDigitalClockTimeFormat(digitalClock, 0.4f, mShowSeconds,
                        false, true, false);
                digitalClock.applyUserPreferredTextSizeSp(SettingsDAO.getDigitalClockFontSize(mPrefs));
            } else {
                ClockUtils.adjustAnalogClockSize(analogClock, mPrefs, false, true, false);
                ClockUtils.setAnalogClockSecondsEnabled(mClockStyle, analogClock, mShowSeconds);
            }
            ClockUtils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
            ClockUtils.applyBoldDateTypeface(mClockFrame);
            ClockUtils.setClockIconTypeface(mClockFrame);
            AlarmUtils.applyBoldNextAlarmTypeface(mClockFrame);
        }

        mEmptyCityViewRightPanel = fragmentView.findViewById(R.id.empty_city_view_right_panel);

        final RecyclerView cityList = fragmentView.findViewById(R.id.cities);
        cityList.setAdapter(mCityAdapter);
        cityList.setLayoutManager(new LinearLayoutManager(mContext));

        // Due to the ViewPager and the location of FAB, set a bottom padding to prevent
        // the city list from being hidden by the FAB (e.g. when scrolling down).
        cityList.setPadding(0, 0, 0, (int) dpToPx(mIsTablet && mIsPortrait
                ? 110 : mIsPortrait
                ? 90 : 0, mDisplayMetrics));

        cityList.addItemDecoration(new CitySpacingItemDecoration(mContext, mIsPortrait, mIsTablet));

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
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {

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

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                // Draw a shadow under the city card when it's dragging
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    viewHolder.itemView.setTranslationZ(dpToPx(6, mDisplayMetrics));
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                // Remove the shadow under the city card when the drag is complete.
                viewHolder.itemView.setTranslationZ(0f);

                // Saving the new order
                DataModel.getDataModel().updateSelectedCitiesOrder(mMutableCities);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getParentFragmentManager().setFragmentResultListener(
                LabelDialogFragment.REQUEST_CITY_NOTE, getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    String cityId = bundle.getString(LabelDialogFragment.RESULT_CITY_ID);
                    String note = bundle.getString(LabelDialogFragment.RESULT_CITY_NOTE);

                    if (cityId != null && note != null) {
                        mCityAdapter.setCityNote(cityId, note);
                    }
                });
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
                int screenHeight = mDisplayMetrics.heightPixels;
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
            AlarmUtils.refreshAlarm(mClockFrame, false);
        } else {
            mCityAdapter.refreshAlarm();
        }
    }

    /**
     * A custom {@link RecyclerView.ItemDecoration} that applies dynamic spacing
     * to the list of world cities.
     *
     * <p>It calculates and sets specific margins based on the device's orientation
     * (portrait/landscape) and screen type (phone/tablet). It also ensures proper
     * bottom spacing for the last item in the list, while intentionally ignoring
     * the main home clock at the top.</p>
     */
    private static class CitySpacingItemDecoration extends RecyclerView.ItemDecoration {

        private final int leftMargin;
        private final int rightMargin;
        private final int bottomMargin;
        private final int spacing;
        private final int mainClockCount;

        public CitySpacingItemDecoration(Context context, boolean isPortrait, boolean isTablet) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            boolean isPhoneInLandscapeMode = !isTablet && !isPortrait;

            // Determine whether the main clock is present (so that margins are not applied to it)
            this.mainClockCount = isPortrait ? 1 : 0;

            this.leftMargin = (int) dpToPx(isPhoneInLandscapeMode ? 0 : 10, metrics);
            this.rightMargin = (int) dpToPx(isPhoneInLandscapeMode ? 90 : 10, metrics);
            this.spacing = (int) dpToPx(2, metrics);
            this.bottomMargin = (int) dpToPx(10, metrics);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

            boolean isRTL = ThemeUtils.isRTL();
            int position = parent.getChildAdapterPosition(view);
            RecyclerView.Adapter<?> adapter = parent.getAdapter();

            if (position == RecyclerView.NO_POSITION || adapter == null) {
                return;
            }

            // Ignore the main clock
            if (position < mainClockCount) {
                return;
            }

            // Side margins
            outRect.left = isRTL ? rightMargin : leftMargin;
            outRect.right = isRTL ? leftMargin : rightMargin;

            int itemCount = adapter.getItemCount();

            if (position == itemCount - 1) {
                // Bottom margin for the very last city
                outRect.bottom = bottomMargin;
            } else {
                // Bottom margin if it is a city in the middle of the list
                int totalCitiesAndHomeClock = itemCount - mainClockCount;
                outRect.bottom = (totalCitiesAndHomeClock > 1) ? spacing : 0;
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
            int itemCount = mCityAdapter.getItemCount();
            if (itemCount > 0) {
                mCityAdapter.notifyItemRangeChanged(0, itemCount);
            }
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
