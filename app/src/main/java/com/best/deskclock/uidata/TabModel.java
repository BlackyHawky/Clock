/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uidata;

import static android.view.View.LAYOUT_DIRECTION_RTL;
import static com.best.deskclock.uidata.UiDataModel.Tab;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.best.deskclock.data.SettingsDAO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * All tab data is accessed via this model.
 */
final class TabModel {

    private final SharedPreferences mPrefs;

    /**
     * The listeners to notify when the selected tab is changed.
     */
    private final List<TabListener> mTabListeners = new ArrayList<>();

    /**
     * The listeners to notify when the vertical scroll state of the selected tab is changed.
     */
    private final List<TabScrollListener> mTabScrollListeners = new ArrayList<>();

    /**
     * The scrolled-to-top state of each tab.
     */
    private final boolean[] mTabScrolledToTop = new boolean[Tab.values().length];

    /**
     * An enumerated value indicating the currently selected tab.
     */
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
            int tabIndex = SettingsDAO.getTabToDisplay(mPrefs);
            if (tabIndex == -1) {
                TabDAO.setSelectedTab(mPrefs, tab);
            }

            // Notify of the tab change.
            for (TabListener tl : mTabListeners) {
                tl.selectedTabChanged(tab);
            }

            // Notify of the vertical scroll position change if there is one.
            final boolean tabScrolledToTop = isTabScrolledToTop(tab);
            if (isTabScrolledToTop(oldSelectedTab) != tabScrolledToTop) {
                for (TabScrollListener tsl : mTabScrollListeners) {
                    tsl.selectedTabScrollToTopChanged(tabScrolledToTop);
                }
            }
        }
    }

    //
    // Tab scrolling
    //

    /**
     * Updates the scrolling state in the {@link UiDataModel} for this tab.
     *
     * @param tab           an enumerated value indicating the tab reporting its vertical scroll position
     * @param scrolledToTop {@code true} iff the vertical scroll position of this tab is at the top
     */
    void setTabScrolledToTop(Tab tab, boolean scrolledToTop) {
        if (isTabScrolledToTop(tab) != scrolledToTop) {
            mTabScrolledToTop[tab.ordinal()] = scrolledToTop;
            if (tab == getSelectedTab()) {
                for (TabScrollListener tsl : mTabScrollListeners) {
                    tsl.selectedTabScrollToTopChanged(scrolledToTop);
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
