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

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Activity scoped singleton that manages action bar menus. Each menu item is controlled by a
 * {@link MenuItemController} instance.
 */
public final class OptionsMenuManager {

    private final List<MenuItemController> mControllers = new ArrayList<>();

    /**
     * Add one or more {@link MenuItemController} to the actionbar menu.
     * <p/>
     * This should be called in {@link Activity#onCreate(Bundle)}.
     */
    public OptionsMenuManager addMenuItemController(MenuItemController... controllers) {
        Collections.addAll(mControllers, controllers);
        return this;
    }

    /**
     * Inflates {@link Menu} for the activity.
     * <p/>
     * This method should be called during {@link Activity#onCreateOptionsMenu(Menu)}.
     */
    public void onCreateOptionsMenu(Menu menu) {
        for (MenuItemController controller : mControllers) {
            controller.onCreateOptionsItem(menu);
        }
    }

    /**
     * Prepares the popup to displays all required menu items.
     * <p/>
     * This method should be called during {@link Activity#onPrepareOptionsMenu(Menu)} (Menu)}.
     */
    public void onPrepareOptionsMenu(Menu menu) {
        for (MenuItemController controller : mControllers) {
            final MenuItem menuItem = menu.findItem(controller.getId());
            if (menuItem != null) {
                controller.onPrepareOptionsItem(menuItem);
            }
        }
    }

    /**
     * Handles click action for a menu item.
     * <p/>
     * This method should be called during {@link Activity#onOptionsItemSelected(MenuItem)}.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        for (MenuItemController controller : mControllers) {
            if (controller.getId() == itemId
                    && controller.onOptionsItemSelected(item)) {
                return true;
            }
        }
        return false;
    }
}
