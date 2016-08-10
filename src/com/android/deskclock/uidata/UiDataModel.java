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

package com.android.deskclock.uidata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.SparseArray;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.ClockFragment;
import com.android.deskclock.R;
import com.android.deskclock.stopwatch.StopwatchFragment;
import com.android.deskclock.timer.TimerFragment;

import java.util.Locale;

import static com.android.deskclock.Utils.enforceMainLooper;

/**
 * All application-wide user interface data is accessible through this singleton.
 */
public final class UiDataModel {

    /** Identifies each of the primary tabs within the application. */
    public enum Tab {
        ALARMS(AlarmClockFragment.class, R.drawable.ic_tab_alarm, R.string.menu_alarm),
        CLOCKS(ClockFragment.class, R.drawable.ic_tab_clock, R.string.menu_clock),
        TIMERS(TimerFragment.class, R.drawable.ic_tab_timer, R.string.menu_timer),
        STOPWATCH(StopwatchFragment.class, R.drawable.ic_tab_stopwatch, R.string.menu_stopwatch);

        private final String mFragmentClassName;
        private final @DrawableRes int mIconId;
        private final @StringRes int mContentDescriptionId;

        Tab(Class fragmentClass, @DrawableRes int iconId, @StringRes int contentDescriptionId) {
            mFragmentClassName = fragmentClass.getName();
            mIconId = iconId;
            mContentDescriptionId = contentDescriptionId;
        }

        public String getFragmentClassName() { return mFragmentClassName; }
        public int getIconId() { return mIconId; }
        public int getContentDescriptionId() { return mContentDescriptionId; }
    }

    /** The single instance of this data model that exists for the life of the application. */
    private static final UiDataModel sUiDataModel = new UiDataModel();

    public static UiDataModel getUiDataModel() {
        return sUiDataModel;
    }

    /** Clears data structures containing data that is locale-sensitive. */
    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();

    /**
     * Caches formatted numbers in the current locale padded with zeroes to requested lengths.
     * The first level of the cache maps length to the second level of the cache.
     * The second level of the cache maps an integer to a formatted String in the current locale.
     */
    private final SparseArray<SparseArray<String>> mNumberFormatCache = new SparseArray<>(3);

    private Context mContext;

    /** The model from which tab data are fetched. */
    private TabModel mTabModel;

    /** The model from which colors are fetched. */
    private ColorModel mColorModel;

    /** The model from which timed callbacks originate. */
    private PeriodicCallbackModel mPeriodicCallbackModel;

    private UiDataModel() {}

    /**
     * The context may be set precisely once during the application life.
     */
    public void setContext(Context context) {
        if (mContext != null) {
            throw new IllegalStateException("context has already been set");
        }
        mContext = context.getApplicationContext();

        mPeriodicCallbackModel = new PeriodicCallbackModel(mContext);
        mColorModel = new ColorModel(mPeriodicCallbackModel);
        mTabModel = new TabModel(mContext);

        // Clear caches affected by locale when locale changes.
        final IntentFilter localeBroadcastFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mLocaleChangedReceiver, localeBroadcastFilter);
    }

    /**
     * To display the alarm clock in this font, use the character {@link R.string#clock_emoji}.
     *
     * @return a special font containing a glyph that draws an alarm clock
     */
    public Typeface getAlarmIconTypeface() {
        return Typeface.createFromAsset(mContext.getAssets(), "fonts/clock.ttf");
    }

    //
    // Number Formatting
    //

    /**
     * This method is intended to be used when formatting numbers occurs in a hotspot such as the
     * update loop of a timer or stopwatch. It returns cached results when possible in order to
     * provide speed and limit garbage to be collected by the virtual machine.
     *
     * @param value a positive integer to format as a String
     * @return the {@code value} formatted as a String in the current locale
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public String getFormattedNumber(int value) {
        final int length = (int) Math.log10(value);
        return getFormattedNumber(false, value, length == 0 ? 1 : length);
    }

    /**
     * This method is intended to be used when formatting numbers occurs in a hotspot such as the
     * update loop of a timer or stopwatch. It returns cached results when possible in order to
     * provide speed and limit garbage to be collected by the virtual machine.
     *
     * @param value a positive integer to format as a String
     * @param length the length of the String; zeroes are padded to match this length
     * @return the {@code value} formatted as a String in the current locale and padded to the
     *      requested {@code length}
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public String getFormattedNumber(int value, int length) {
        return getFormattedNumber(false, value, length);
    }

    /**
     * This method is intended to be used when formatting numbers occurs in a hotspot such as the
     * update loop of a timer or stopwatch. It returns cached results when possible in order to
     * provide speed and limit garbage to be collected by the virtual machine.
     *
     * @param negative force a minus sign (-) onto the display, even if {@code value} is {@code 0}
     * @param value a positive integer to format as a String
     * @param length the length of the String; zeroes are padded to match this length. If
     *      {@code negative} is {@code true} the return value will contain a minus sign and a total
     *      length of {@code length + 1}.
     * @return the {@code value} formatted as a String in the current locale and padded to the
     *      requested {@code length}
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public String getFormattedNumber(boolean negative, int value, int length) {
        if (value < 0) {
            throw new IllegalArgumentException("value may not be negative: " + value);
        }

        // Look up the value cache using the length; -ve and +ve values are cached separately.
        final int lengthCacheKey = negative ? -length : length;
        SparseArray<String> valueCache = mNumberFormatCache.get(lengthCacheKey);
        if (valueCache == null) {
            valueCache = new SparseArray<>((int) Math.pow(10, length));
            mNumberFormatCache.put(lengthCacheKey, valueCache);
        }

        // Look up the cached formatted value using the value.
        String formatted = valueCache.get(value);
        if (formatted == null) {
            final String sign = negative ? "−" : "";
            formatted = String.format(Locale.getDefault(), sign + "%0" + length + "d", value);
            valueCache.put(value, formatted);
        }

        return formatted;
    }

    //
    // Colors
    //

    /**
     * @param colorListener to be notified when the app's color changes
     */
    public void addOnAppColorChangeListener(OnAppColorChangeListener colorListener) {
        enforceMainLooper();
        mColorModel.addOnAppColorChangeListener(colorListener);
    }

    /**
     * @param colorListener to be notified when the app's color changes
     */
    public void removeOnAppColorChangeListener(OnAppColorChangeListener colorListener) {
        enforceMainLooper();
        mColorModel.removeOnAppColorChangeListener(colorListener);
    }

    /**
     * @return the color of the application window background
     */
    public @ColorInt int getWindowBackgroundColor() {
        enforceMainLooper();
        return mColorModel.getAppColor();
    }

    //
    // Animations
    //

    /**
     * @return the duration in milliseconds of short animations
     */
    public long getShortAnimationDuration() {
        enforceMainLooper();
        return mContext.getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    //
    // Tabs
    //

    /**
     * @param tabListener to be notified when the selected tab changes
     */
    public void addTabListener(TabListener tabListener) {
        enforceMainLooper();
        mTabModel.addTabListener(tabListener);
    }

    /**
     * @param tabListener to no longer be notified when the selected tab changes
     */
    public void removeTabListener(TabListener tabListener) {
        enforceMainLooper();
        mTabModel.removeTabListener(tabListener);
    }

    /**
     * @return the number of tabs
     */
    public int getTabCount() {
        enforceMainLooper();
        return mTabModel.getTabCount();
    }

    /**
     * @param index the index of the tab
     * @return the tab at the given {@code index}
     */
    public Tab getTab(int index) {
        enforceMainLooper();
        return mTabModel.getTab(index);
    }

    /**
     * @return the index of the currently selected primary tab
     */
    public int getSelectedTabIndex() {
        enforceMainLooper();
        return mTabModel.getSelectedTabIndex();
    }

    /**
     * @param index the index of the tab to select
     */
    public void setSelectedTabIndex(int index) {
        enforceMainLooper();
        mTabModel.setSelectedTabIndex(index);
    }

    /**
     * @return an enumerated value indicating the currently selected primary tab
     */
    public Tab getSelectedTab() {
        enforceMainLooper();
        return mTabModel.getSelectedTab();
    }

    /**
     * @param tab an enumerated value indicating the newly selected primary tab
     */
    public void setSelectedTab(Tab tab) {
        enforceMainLooper();
        mTabModel.setSelectedTab(tab);
    }

    /**
     * @param tabScrollListener to be notified when the scroll position of the selected tab changes
     */
    public void addTabScrollListener(TabScrollListener tabScrollListener) {
        enforceMainLooper();
        mTabModel.addTabScrollListener(tabScrollListener);
    }

    /**
     * @param tabScrollListener to be notified when the scroll position of the selected tab changes
     */
    public void removeTabScrollListener(TabScrollListener tabScrollListener) {
        enforceMainLooper();
        mTabModel.removeTabScrollListener(tabScrollListener);
    }

    /**
     * Updates the scrolling state in the {@link UiDataModel} for this tab.
     *
     * @param tab an enumerated value indicating the tab reporting its vertical scroll position
     * @param scrolledToTop {@code true} iff the vertical scroll position of the tab is at the top
     */
    public void setTabScrolledToTop(Tab tab, boolean scrolledToTop) {
        enforceMainLooper();
        mTabModel.setTabScrolledToTop(tab, scrolledToTop);
    }

    /**
     * @return {@code true} iff the content in the selected tab is currently scrolled to the top
     */
    public boolean isSelectedTabScrolledToTop() {
        enforceMainLooper();
        return mTabModel.isTabScrolledToTop(getSelectedTab());
    }

    /**
     * This method converts the given {@code ltrTabIndex} which assumes Left-To-Right layout of the
     * tabs into an index that respects the system layout, which may be Left-To-Right or
     * Right-To-Left.
     *
     * @param ltrTabIndex the tab index assuming left-to-right layout direction
     * @return the tab index in the current layout direction
     */
    public int getTabLayoutIndex(int ltrTabIndex) {
        enforceMainLooper();
        return mTabModel.getTabLayoutIndex(ltrTabIndex);
    }

    //
    // Timed Callbacks
    //

    /**
     * @param runnable to be called every minute
     * @param offset an offset applied to the minute to control when the callback occurs
     */
    public void addMinuteCallback(Runnable runnable, long offset) {
        enforceMainLooper();
        mPeriodicCallbackModel.addMinuteCallback(runnable, offset);
    }

    /**
     * @param runnable to be called every quarter-hour
     * @param offset an offset applied to the quarter-hour to control when the callback occurs
     */
    public void addQuarterHourCallback(Runnable runnable, long offset) {
        enforceMainLooper();
        mPeriodicCallbackModel.addQuarterHourCallback(runnable, offset);
    }

    /**
     * @param runnable to be called every hour
     * @param offset an offset applied to the hour to control when the callback occurs
     */
    public void addHourCallback(Runnable runnable, long offset) {
        enforceMainLooper();
        mPeriodicCallbackModel.addHourCallback(runnable, offset);
    }

    /**
     * @param runnable to be called every midnight
     * @param offset an offset applied to the midnight to control when the callback occurs
     */
    public void addMidnightCallback(Runnable runnable, long offset) {
        enforceMainLooper();
        mPeriodicCallbackModel.addMidnightCallback(runnable, offset);
    }

    /**
     * @param runnable to no longer be called periodically
     */
    public void removePeriodicCallback(Runnable runnable) {
        enforceMainLooper();
        mPeriodicCallbackModel.removePeriodicCallback(runnable);
    }

    /**
     * Cached information that is locale-sensitive must be cleared in response to locale changes.
     */
    private final class LocaleChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mNumberFormatCache.clear();
        }
    }
}