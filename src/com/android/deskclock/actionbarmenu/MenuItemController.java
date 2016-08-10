/*
 * Copyright (C) 2016 The Android Open Source Project
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
     * Returns the menu item resource id that the controller manages.
     */
    int getId();

    /**
     * Create the menu item.
     */
    void onCreateOptionsItem(Menu menu);

    /**
     * Called immediately before the {@link MenuItem} is shown.
     *
     * @param item the {@link MenuItem} created by the controller
     */
    void onPrepareOptionsItem(MenuItem item);

    /**
     * Attempts to handle the click action.
     *
     * @param item the {@link MenuItem} that was selected
     * @return {@code true} if the action is handled by this controller
     */
    boolean onOptionsItemSelected(MenuItem item);
}
