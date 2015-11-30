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
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.view.View.LAYOUT_DIRECTION_RTL;
import static com.android.deskclock.uidata.UiDataModel.Tab;

/**
 * All tab data is accessed via this model.
 */
final class TabModel {

    private final Context mContext;

    /** The listeners to notify when the selected tab is changed. */
    private final List<TabListener> mTabListeners = new ArrayList<>();

    /** An enumerated value indicating the currently selected tab. */
    private Tab mSelectedTab;

    TabModel(Context context) {
        mContext = context;
    }

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
     * @param index the index of the tab
     * @return the tab at the given {@code index}
     */
    Tab getTab(int index) {
        return Tab.values()[index];
    }

    /**
     * @return the index of the currently selected primary tab
     */
    int getSelectedTabIndex() {
        return getSelectedTab().ordinal();
    }

    /**
     * @param index the index of the tab to select
     */
    void setSelectedTabIndex(int index) {
        setSelectedTab(Tab.values()[index]);
    }

    /**
     * @return an enumerated value indicating the currently selected primary tab
     */
    Tab getSelectedTab() {
        if (mSelectedTab == null) {
            mSelectedTab = TabDAO.getSelectedTab(mContext);
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
            TabDAO.setSelectedTab(mContext, tab);

            for (TabListener tl : mTabListeners) {
                tl.selectedTabChanged(oldSelectedTab, tab);
            }
        }
    }

    /**
     * @param ltrTabIndex the tab index assuming left-to-right layout direction
     * @return the tab index in the current layout direction
     */
    int getTabLayoutIndex(int ltrTabIndex) {
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == LAYOUT_DIRECTION_RTL) {
            return getTabCount() - ltrTabIndex - 1;
        }
        return ltrTabIndex;
    }
}