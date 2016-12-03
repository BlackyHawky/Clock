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

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static android.view.View.LAYOUT_DIRECTION_RTL;
import static com.android.deskclock.uidata.UiDataModel.Tab;

/**
 * All tab data is accessed via this model.
 */
final class TabModel {

    private final SharedPreferences mPrefs;

    /** The listeners to notify when the selected tab is changed. */
    private final List<TabListener> mTabListeners = new ArrayList<>();

    /** The listeners to notify when the vertical scroll state of the selected tab is changed. */
    private final List<TabScrollListener> mTabScrollListeners = new ArrayList<>();

    /** The scrolled-to-top state of each tab. */
    private final boolean[] mTabScrolledToTop = new boolean[Tab.values().length];

    /** An enumerated value indicating the currently selected tab. */
    private Tab mSelectedTab;

    TabModel(SharedPreferences prefs) {
        mPrefs = prefs;
        Arrays.fill(mTabScrolledToTop, true);
    }

    //
    // Selected tab
    //

    /**
     * @param tabListener to be notified when the selected tab changes
     */
    void addTabListener(TabListener tabListener) {
        mTabListeners.add(tabListener);
    }

    /**
     * @param tabListener to no longer be notified when the selected tab changes
     */
    void removeTabListener(TabListener tabListener) {
        mTabListeners.remove(tabListener);
    }

    /**
     * @return the number of tabs
     */
    int getTabCount() {
        return Tab.values().length;
    }

    /**
     * @param ordinal the ordinal (left-to-right index) of the tab
     * @return the tab at the given {@code ordinal}
     */
    Tab getTab(int ordinal) {
        return Tab.values()[ordinal];
    }

    /**
     * @param position the position of the tab in the user interface
     * @return the tab at the given {@code ordinal}
     */
    Tab getTabAt(int position) {
        final int ordinal;
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == LAYOUT_DIRECTION_RTL) {
            ordinal = getTabCount() - position - 1;
        } else {
            ordinal = position;
        }
        return getTab(ordinal);
    }

    /**
     * @return an enumerated value indicating the currently selected primary tab
     */
    Tab getSelectedTab() {
        if (mSelectedTab == null) {
            mSelectedTab = TabDAO.getSelectedTab(mPrefs);
        }
        return mSelectedTab;
    }

    /**
     * @param tab an enumerated value indicating the newly selected primary tab
     */
    void setSelectedTab(Tab tab) {
        final Tab oldSelectedTab = getSelectedTab();
        if (oldSelectedTab != tab) {
            mSelectedTab = tab;
            TabDAO.setSelectedTab(mPrefs, tab);

            // Notify of the tab change.
            for (TabListener tl : mTabListeners) {
                tl.selectedTabChanged(oldSelectedTab, tab);
            }

            // Notify of the vertical scroll position change if there is one.
            final boolean tabScrolledToTop = isTabScrolledToTop(tab);
            if (isTabScrolledToTop(oldSelectedTab) != tabScrolledToTop) {
                for (TabScrollListener tsl : mTabScrollListeners) {
                    tsl.selectedTabScrollToTopChanged(tab, tabScrolledToTop);
                }
            }
        }
    }

    //
    // Tab scrolling
    //

    /**
     * @param tabScrollListener to be notified when the scroll position of the selected tab changes
     */
    void addTabScrollListener(TabScrollListener tabScrollListener) {
        mTabScrollListeners.add(tabScrollListener);
    }

    /**
     * @param tabScrollListener to be notified when the scroll position of the selected tab changes
     */
    void removeTabScrollListener(TabScrollListener tabScrollListener) {
        mTabScrollListeners.remove(tabScrollListener);
    }

    /**
     * Updates the scrolling state in the {@link UiDataModel} for this tab.
     *
     * @param tab an enumerated value indicating the tab reporting its vertical scroll position
     * @param scrolledToTop {@code true} iff the vertical scroll position of this tab is at the top
     */
    void setTabScrolledToTop(Tab tab, boolean scrolledToTop) {
        if (isTabScrolledToTop(tab) != scrolledToTop) {
            mTabScrolledToTop[tab.ordinal()] = scrolledToTop;
            if (tab == getSelectedTab()) {
                for (TabScrollListener tsl : mTabScrollListeners) {
                    tsl.selectedTabScrollToTopChanged(tab, scrolledToTop);
                }
            }
        }
    }

    /**
     * @param tab identifies the tab
     * @return {@code true} iff the content in the given {@code tab} is currently scrolled to top
     */
    boolean isTabScrolledToTop(Tab tab) {
        return mTabScrolledToTop[tab.ordinal()];
    }
}