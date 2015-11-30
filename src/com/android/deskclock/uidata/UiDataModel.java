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
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.ClockFragment;
import com.android.deskclock.R;
import com.android.deskclock.stopwatch.StopwatchFragment;
import com.android.deskclock.timer.TimerFragment;

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

    private Context mContext;

    /** The model from which tab data are fetched. */
    private TabModel mTabModel;

    private UiDataModel() {}

    /**
     * The context may be set precisely once during the application life.
     */
    public void setContext(Context context) {
        if (mContext != null) {
            throw new IllegalStateException("context has already been set");
        }
        mContext = context.getApplicationContext();

        mTabModel = new TabModel(mContext);
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
}