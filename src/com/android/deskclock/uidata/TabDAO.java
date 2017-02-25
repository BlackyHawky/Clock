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

import static com.android.deskclock.uidata.UiDataModel.Tab;

/**
 * This class encapsulates the storage of tab data in {@link SharedPreferences}.
 */
final class TabDAO {

    /** Key to a preference that stores the ordinal of the selected tab. */
    private static final String KEY_SELECTED_TAB = "selected_tab";

    private TabDAO() {}

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