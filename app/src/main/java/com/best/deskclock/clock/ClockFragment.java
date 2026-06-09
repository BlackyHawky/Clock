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
import static com.best.deskclock.settings.PreferencesDefaultValues.BLACK_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_CITIES_MANUALLY;
import static com.best.deskclock.settings.PreferencesKeys.*;
import static com.best.deskclock.uidata.UiDataModel.Tab.CLOCKS;
import static com.best.deskclock.utils.AlarmUtils.ACTION_NEXT_ALARM_CHANGED_BY_CLOCK;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.Typeface;
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

import com.best.deskclock.R;
import com.best.deskclock.base.DeskClockFragment;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.ClockFragmentBinding;
import com.best.deskclock.dialogfragment.LabelDialogFragment;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uicomponents.AutoSizingTextClock;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.worldclock.CitySelectionActivity;
import com.google.android.material.appbar.AppBarLayout;

import java.util.List;

/**
 * Fragment that shows the clock (analog or digital), the next alarm info and the world clock.
 */
public final class ClockFragment extends DeskClockFragment {

    private ClockFragmentBinding mBinding;

    // Updates dates in the UI on every quarter-hour.
    private final Runnable mQuarterHourUpdater = new QuarterHourRunnable();

    // Updates the UI in response to changes to the scheduled alarm.
    private BroadcastReceiver mAlarmChangeReceiver;
    private SharedPreferences mPrefs;
    private final ClockSettings mSettings = new ClockSettings();
    private DisplayMetrics mDisplayMetrics;
    private List<City> mSelectedCities;
    private boolean mIsDigitalClock;
    private boolean mAreSettingsChanged = false;
    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefListener = (prefs, key) -> {
        if (key != null) {
            switch (key) {
                case KEY_CLOCK_STYLE, KEY_CLOCK_DIAL, KEY_CLOCK_DIAL_MATERIAL, KEY_ANALOG_CLOCK_SIZE, KEY_DISPLAY_CLOCK_SECONDS,
                     KEY_CLOCK_SECOND_HAND, KEY_DIGITAL_CLOCK_FONT, KEY_DIGITAL_CLOCK_FONT_SIZE, KEY_DISPLAY_TEXT_UPPERCASE,
                     KEY_SORT_CITIES, KEY_ENABLE_CITY_NOTE, KEY_AUTO_HOME_CLOCK, KEY_HOME_TIME_ZONE -> {

                    mAreSettingsChanged = true;

                    if (isResumed()) {
                        applySettingsChanges();
                    }
                }
            }
        }
    };

    private SelectedCitiesAdapter mCityAdapter;
    private ItemTouchHelper mItemTouchHelper;
    private CityItemTouchHelper mTouchHelperCallback;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private boolean mIsPortrait;
    private boolean mIsTablet;
    private boolean mHasBlackAccentColor;

    /**
     * The public no-arg constructor required by all fragments.
     */
    public ClockFragment() {
        super(CLOCKS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getDefaultSharedPreferences(requireContext());
        mDisplayMetrics = getResources().getDisplayMetrics();
        mHasBlackAccentColor = SettingsDAO.getAccentColor(mPrefs).equals(BLACK_ACCENT_COLOR);
        mIsPortrait = ThemeUtils.isPortrait();
        mIsTablet = ThemeUtils.isTablet();
        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
        mSelectedCities = DataModel.getDataModel().getSelectedCities();
        mAlarmChangeReceiver = new AlarmChangedBroadcastReceiver();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mBinding = ClockFragmentBinding.inflate(inflater, container, false);

        ClockUtils.applyBoldDateTypeface(mBinding.mainClockFrame.mainClockContainer);
        ClockUtils.setClockIconTypeface(mBinding.mainClockFrame.mainClockContainer);
        AlarmUtils.applyBoldNextAlarmTypeface(mBinding.mainClockFrame.mainClockContainer);

        mBinding.cityRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        mBinding.cityRecyclerView.addItemDecoration(new CitySpacingItemDecoration(requireContext(), mDisplayMetrics, mIsPortrait, mIsTablet));

        if (mIsPortrait) {
            mBinding.cityRecyclerView.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                                 oldLeft, oldTop, oldRight, oldBottom) ->
                v.post(() -> {
                    if (mBinding == null || mBinding.clockAppBarLayout == null) {
                        return;
                    }

                    ViewGroup.LayoutParams rawParams = mBinding.mainClockFrame.getRoot().getLayoutParams();

                    if (rawParams instanceof AppBarLayout.LayoutParams layoutParams) {
                        int coordinatorHeight = mBinding.getRoot().getHeight();
                        int appBarHeight = mBinding.clockAppBarLayout.getHeight();
                        int stableAvailableHeight = coordinatorHeight - appBarHeight;

                        int totalContentHeight = mBinding.cityRecyclerView.computeVerticalScrollRange();

                        boolean canScroll = totalContentHeight > stableAvailableHeight;

                        int currentFlags = layoutParams.getScrollFlags();
                        int targetFlags = canScroll ?
                            (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS) : 0;

                        if (currentFlags != targetFlags) {
                            layoutParams.setScrollFlags(targetFlags);
                            mBinding.mainClockFrame.getRoot().setLayoutParams(layoutParams);

                            if (!canScroll && mBinding.clockAppBarLayout != null) {
                                mBinding.clockAppBarLayout.setExpanded(true, true);
                            }
                        }
                    }
                })
            );
        }

        // Schedule a runnable to update the date every quarter-hour.
        UiDataModel.getUiDataModel().addQuarterHourCallback(mQuarterHourUpdater, 100);

        refreshAlarm();

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        refreshSettings();
        updateMainClock();

        String fontPath = SettingsDAO.getGeneralFont(mPrefs);
        Typeface regularTypeface = ThemeUtils.loadFont(fontPath);
        Typeface boldTypeface = ThemeUtils.boldTypeface(fontPath);

        mCityAdapter = new SelectedCitiesAdapter(
            requireContext(), mPrefs, mSelectedCities, mHasBlackAccentColor, regularTypeface, boldTypeface, mSettings);

        mBinding.cityRecyclerView.setAdapter(mCityAdapter);

        DataModel.getDataModel().addCityListener(mCityAdapter);

        mTouchHelperCallback = new CityItemTouchHelper(mCityAdapter, mSettings.showHomeClock);
        mItemTouchHelper = new ItemTouchHelper(mTouchHelperCallback);
        updateDragAndDrop();

        updateEmptyStateVisibility();

        getParentFragmentManager().setFragmentResultListener(LabelDialogFragment.REQUEST_CITY_NOTE, getViewLifecycleOwner(),
            (requestKey, bundle) -> {
                String cityId = bundle.getString(LabelDialogFragment.RESULT_CITY_ID);
                String note = bundle.getString(LabelDialogFragment.RESULT_CITY_NOTE);

                if (cityId != null && note != null) {
                    mCityAdapter.setCityNote(cityId, note);
                }
            });

        mPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onStart() {
        super.onStart();

        // Watch for system events that effect clock time or format.
        if (mAlarmChangeReceiver != null) {
            final IntentFilter filter = new IntentFilter(ACTION_NEXT_ALARM_CHANGED_BY_CLOCK);
            if (SdkUtils.isAtLeastAndroid13()) {
                requireContext().registerReceiver(mAlarmChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                requireContext().registerReceiver(mAlarmChangeReceiver, filter);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isSystem24Hour = DataModel.getDataModel().is24HourFormat();

        if (mAreSettingsChanged || mSettings.is24HourFormat != isSystem24Hour) {
            applySettingsChanges();
        }

        updateEmptyStateVisibility();

        if (getView() != null) {
            getView().post(() -> {
                if (!isAdded()) {
                    return;
                }

                refreshAlarm();
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mAlarmChangeReceiver != null) {
            requireContext().unregisterReceiver(mAlarmChangeReceiver);
        }
    }

    @Override
    public void onDestroyView() {
        UiDataModel.getUiDataModel().removePeriodicCallback(mQuarterHourUpdater);
        DataModel.getDataModel().removeCityListener(mCityAdapter);

        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);

        mBinding.cityRecyclerView.setAdapter(null);

        mAreSettingsChanged = false;

        mBinding = null;

        super.onDestroyView();
    }

    @Override
    public void onFabClick() {
        startActivity(new Intent(requireContext(), CitySelectionActivity.class));

        if (SettingsDAO.isFadeTransitionsEnabled(mPrefs)) {
            if (SdkUtils.isAtLeastAndroid14()) {
                requireActivity().overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
            } else {
                requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        } else {
            if (SdkUtils.isAtLeastAndroid14()) {
                requireActivity().overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN, R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
            } else {
                requireActivity().overridePendingTransition(R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
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
        fab.setContentDescription(getString(R.string.button_cities));
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
        if (mBinding != null) {
            AlarmUtils.refreshAlarm(mBinding.mainClockFrame.mainClockContainer, false, mSettings.isTextUppercase);
        }
    }

    private void updateEmptyStateVisibility() {
        if ( mBinding == null || mCityAdapter == null) {
            return;
        }

        final boolean isEmpty = !mSettings.showHomeClock && mCityAdapter.getCities().isEmpty();

        if (mBinding.citiesEmptyView != null) {
            mBinding.citiesEmptyView.setVisibility(isEmpty ? VISIBLE : GONE);
        } else if (mBinding.emptyCityViewRightPanel != null) {
            mBinding.emptyCityViewRightPanel.setVisibility(isEmpty ? VISIBLE : GONE);
        }
    }

    private void refreshSettings() {
        mSettings.clockStyle = SettingsDAO.getClockStyle(mPrefs);
        mSettings.is24HourFormat = DataModel.getDataModel().is24HourFormat();
        mSettings.showSeconds = SettingsDAO.areClockSecondsDisplayed(mPrefs);
        mSettings.isTextUppercase = SettingsDAO.isTextUppercaseDisplayed(mPrefs);

        mSettings.digitalClockTypeface = ThemeUtils.loadFont(SettingsDAO.getDigitalClockFont(mPrefs));
        mSettings.digitalClockFontSize = SettingsDAO.getDigitalClockFontSize(mPrefs);
        mSettings.analogClockSizePercent = SettingsDAO.getAnalogClockSize(mPrefs);

        mSettings.showHomeClock = SettingsDAO.getShowHomeClock(requireContext(), mPrefs);
        mSettings.isCityNoteEnabled = SettingsDAO.isCityNoteEnabled(mPrefs);
        mSettings.citySorting = SettingsDAO.getCitySorting(mPrefs);

        mIsDigitalClock = mSettings.clockStyle == DataModel.ClockStyle.DIGITAL;
    }

    private void updateMainClock() {
        if (mBinding == null) {
            return;
        }

        AnalogClock analogClock = mBinding.mainClockFrame.analogClock;
        AutoSizingTextClock digitalClock = mBinding.mainClockFrame.digitalClock;

        ClockUtils.setClockStyle(mSettings.clockStyle, digitalClock, analogClock);

        if (mIsDigitalClock) {
            digitalClock.setTypeface(mSettings.digitalClockTypeface);
            ClockUtils.setDigitalClockTimeFormat(digitalClock, 0.4f, mSettings.showSeconds, false, true, false);
            digitalClock.applyUserPreferredTextSizeSp(mSettings.digitalClockFontSize);
        } else {
            ClockUtils.adjustAnalogClockSize(analogClock, mSettings.analogClockSizePercent);
            ClockUtils.setAnalogClockSecondsEnabled(mSettings.clockStyle, analogClock, mSettings.showSeconds);
        }

        ClockUtils.updateDate(
            mDateFormat, mDateFormatForAccessibility, mBinding.mainClockFrame.mainClockContainer, mSettings.isTextUppercase);
        AlarmUtils.refreshAlarm(mBinding.mainClockFrame.mainClockContainer, false, mSettings.isTextUppercase);
    }

    private void updateDragAndDrop() {
        if (mSettings.citySorting.equals(SORT_CITIES_MANUALLY)) {
            mItemTouchHelper.attachToRecyclerView(mBinding.cityRecyclerView);
        } else {
            mItemTouchHelper.attachToRecyclerView(null);
        }
    }

    private void applySettingsChanges() {
        refreshSettings();

        updateMainClock();

        if (mBinding != null) {
            ClockUtils.refreshAnalogClockStyle(mBinding.mainClockFrame.analogClock);
        }

        if (mCityAdapter != null) {
            mCityAdapter.updateSettings(mSettings);
        }

        if (mTouchHelperCallback != null) {
            mTouchHelperCallback.setShowHomeClock(mSettings.showHomeClock);
            updateDragAndDrop();
        }

        mAreSettingsChanged = false;
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

        private final boolean mIsRTL;

        public CitySpacingItemDecoration(Context context, DisplayMetrics displayMetrics, boolean isPortrait, boolean isTablet) {
            boolean isPhoneInLandscapeMode = !isTablet && !isPortrait;

            this.leftMargin = (int) dpToPx(isPhoneInLandscapeMode ? 0 : 10, displayMetrics);
            this.rightMargin = (int) dpToPx(isPhoneInLandscapeMode ? 90 : 10, displayMetrics);
            this.spacing = (int) dpToPx(2, displayMetrics);
            this.bottomMargin = (int) dpToPx(10, displayMetrics);
            this.mIsRTL = ThemeUtils.isRTL(context);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {

            int position = parent.getChildAdapterPosition(view);
            RecyclerView.Adapter<?> adapter = parent.getAdapter();

            if (position == RecyclerView.NO_POSITION || adapter == null) {
                return;
            }

            // Side margins
            outRect.left = mIsRTL ? rightMargin : leftMargin;
            outRect.right = mIsRTL ? leftMargin : rightMargin;

            int itemCount = adapter.getItemCount();

            if (position == itemCount - 1) {
                // Bottom margin for the very last city
                outRect.bottom = bottomMargin;
            } else {
                // Bottom margin if it is a city in the middle of the list
                outRect.bottom = (itemCount > 1) ? spacing : 0;
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
