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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import androidx.annotation.IntegerRes;
import androidx.annotation.StringRes;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.ClockFragment;
import com.android.deskclock.R;
import com.android.deskclock.stopwatch.StopwatchFragment;
import com.android.deskclock.timer.TimerFragment;

import java.util.Calendar;

import static com.android.deskclock.Utils.enforceMainLooper;

/**
 * All application-wide user interface data is accessible through this singleton.
 */
public final class UiDataModel {

    /** Identifies each of the primary tabs within the application. */
    public enum Tab {
        ALARMS(AlarmClockFragment.class, R.id.page_alarm, R.string.menu_alarm),
        CLOCKS(ClockFragment.class, R.id.page_clock, R.string.menu_clock),
        TIMERS(TimerFragment.class, R.id.page_timer, R.string.menu_timer),
        STOPWATCH(StopwatchFragment.class, R.id.page_stopwatch, R.string.menu_stopwatch);

        private final String mFragmentClassName;
        private final @IntegerRes int mPageResId;
        private final @StringRes int mLabelResId;

        Tab(Class fragmentClass, @IntegerRes int pageResId, @StringRes int labelResId) {
            mFragmentClassName = fragmentClass.getName();
            mPageResId = pageResId;
            mLabelResId = labelResId;
        }

        public String getFragmentClassName() { return mFragmentClassName; }
        public @IntegerRes int getPageResId() { return mPageResId; }
        public @StringRes int getLabelResId() { return mLabelResId; }
    }

    /** The single instance of this data model that exists for the life of the application. */
    private static final UiDataModel sUiDataModel = new UiDataModel();

    public static UiDataModel getUiDataModel() {
        return sUiDataModel;
    }

    private Context mContext;

    /** The model from which tab data are fetched. */
    private TabModel mTabModel;

    /** The model from which formatted strings are fetched. */
    private FormattedStringModel mFormattedStringModel;

    /** The model from which timed callbacks originate. */
    private PeriodicCallbackModel mPeriodicCallbackModel;

    private UiDataModel() {}

    /**
     * The context may be set precisely once during the application life.
     */
    public void init(Context context, SharedPreferences prefs) {
        if (mContext != context) {
            mContext = context.getApplicationContext();

            mPeriodicCallbackModel = new PeriodicCallbackModel(mContext);
            mFormattedStringModel = new FormattedStringModel(mContext);
            mTabModel = new TabModel(prefs);
        }
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
    // Formatted Strings
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
        enforceMainLooper();
        return mFormattedStringModel.getFormattedNumber(value);
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
        enforceMainLooper();
        return mFormattedStringModel.getFormattedNumber(value, length);
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
        enforceMainLooper();
        return mFormattedStringModel.getFormattedNumber(negative, value, length);
    }

    /**
     * @param calendarDay any of the following values
     *                     <ul>
     *                     <li>{@link Calendar#SUNDAY}</li>
     *                     <li>{@link Calendar#MONDAY}</li>
     *                     <li>{@link Calendar#TUESDAY}</li>
     *                     <li>{@link Calendar#WEDNESDAY}</li>
     *                     <li>{@link Calendar#THURSDAY}</li>
     *                     <li>{@link Calendar#FRIDAY}</li>
     *                     <li>{@link Calendar#SATURDAY}</li>
     *                     </ul>
     * @return single-character version of weekday name; e.g.: 'S', 'M', 'T', 'W', 'T', 'F', 'S'
     */
    public String getShortWeekday(int calendarDay) {
        enforceMainLooper();
        return mFormattedStringModel.getShortWeekday(calendarDay);
    }

    /**
     * @param calendarDay any of the following values
     *                     <ul>
     *                     <li>{@link Calendar#SUNDAY}</li>
     *                     <li>{@link Calendar#MONDAY}</li>
     *                     <li>{@link Calendar#TUESDAY}</li>
     *                     <li>{@link Calendar#WEDNESDAY}</li>
     *                     <li>{@link Calendar#THURSDAY}</li>
     *                     <li>{@link Calendar#FRIDAY}</li>
     *                     <li>{@link Calendar#SATURDAY}</li>
     *                     </ul>
     * @return full weekday name; e.g.: 'Sunday', 'Monday', 'Tuesday', etc.
     */
    public String getLongWeekday(int calendarDay) {
        enforceMainLooper();
        return mFormattedStringModel.getLongWeekday(calendarDay);
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

    /**
     * @return the duration in milliseconds of long animations
     */
    public long getLongAnimationDuration() {
        enforceMainLooper();
        return mContext.getResources().getInteger(android.R.integer.config_longAnimTime);
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
     * @param ordinal the ordinal of the tab
     * @return the tab at the given {@code ordinal}
     */
    public Tab getTab(int ordinal) {
        enforceMainLooper();
        return mTabModel.getTab(ordinal);
    }

    /**
     * @param position the position of the tab in the user interface
     * @return the tab at the given {@code ordinal}
     */
    public Tab getTabAt(int position) {
        enforceMainLooper();
        return mTabModel.getTabAt(position);
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

    //
    // Shortcut Ids
    //

    /**
     * @param category which category of shortcut of which to get the id
     * @param action the desired action to perform
     * @return the id of the shortcut
     */
    public String getShortcutId(@StringRes int category, @StringRes int action) {
        if (category == R.string.category_stopwatch) {
            return mContext.getString(category);
        }
        return mContext.getString(category) + "_" + mContext.getString(action);
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
}
