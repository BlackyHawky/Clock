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
package com.android.deskclock.actionbarmenu;

import android.view.Menu;
import android.view.MenuItem;

/**
 * Interface for handling a single menu item in action bar.
 */
public interface MenuItemController {

    /**
     * Sets whether or not the controller is enabled.
     */
    void setEnabled(boolean enabled);

    /**
     * Returns true if the controller is currently enabled.
     */
    boolean isEnabled();

    /**
     * Returns the menu item id that the controller is responsible for.
     */
    int getId();

    /**
     * Sets the initial state for the menu item.
     */
    void setInitialState(Menu menu);

    /**
     * Find the menu item this controller cares about, and make it visible.
     *
     * @param menu The menu object containing an item that controller can handle.
     */
    void showMenuItem(Menu menu);

    /**
     * Attempts to handle the click action.
     *
     * @param item The menu item being clicked.
     * @return True if the action is handled by this controller, false otherwise.
     */
    boolean handleMenuItemClick(MenuItem item);
}
