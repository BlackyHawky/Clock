/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uidata;

import static com.best.deskclock.uidata.UiDataModel.Tab;

import android.content.SharedPreferences;

/**
 * This class encapsulates the storage of tab data in {@link SharedPreferences}.
 */
final class TabDAO {

    /**
     * Key to a preference that stores the ordinal of the selected tab.
     */
    private static final String KEY_SELECTED_TAB = "selected_tab";

    private TabDAO() {
    }

    /**
     * @return an enumerated value indicating the currently selected primary tab
     */
    static Tab getSelectedTab(SharedPreferences prefs) {
        final int ordinal = prefs.getInt(KEY_SELECTED_TAB, Tab.CLOCKS.ordinal());
        return Tab.values()[ordinal];
    }

    /**
     * @param tab an enumerated value indicating the newly selected primary tab
     */
    static void setSelectedTab(SharedPreferences prefs, Tab tab) {
        prefs.edit().putInt(KEY_SELECTED_TAB, tab.ordinal()).apply();
    }
}
