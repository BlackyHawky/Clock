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
     * An enumerated value indicating the currently selected tab.
     */
    private Tab mSelectedTab;

    TabModel(SharedPreferences prefs) {
        mPrefs = prefs;
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
        }
    }

}
