/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uidata;

import com.best.deskclock.uidata.UiDataModel.Tab;

/**
 * The interface through which interested parties are notified of changes to the selected tab.
 */
public interface TabListener {

    /**
     * @param newSelectedTab an enumerated value indicating the newly selected tab
     */
    void selectedTabChanged(Tab newSelectedTab);
}
