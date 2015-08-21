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

import android.app.Activity;
import android.util.ArrayMap;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.deskclock.R;

/**
 * Activity scoped singleton that manages action bar menus. Each menu item is controlled by a
 * {@link MenuItemController} instance.
 * <p/>
 * This class needs to be instantiated before or during activity's onCreate event.
 */
public final class ActionBarMenuManager {
    // A map of all menu item controllers, keyed by menu item id.
    private final ArrayMap<Integer, MenuItemController> mControllers;

    public ActionBarMenuManager(Activity activity) {
        mControllers = new ArrayMap<>();
    }

    /**
     * Add one or more {@link MenuItemController} to the actionbar menu.
     * <p/>
     * This should be called before activity's onPrepareOptionsMenu event.
     */
    public ActionBarMenuManager addMenuItemController(MenuItemController... menuItemControllers) {
        if (menuItemControllers != null) {
            for (MenuItemController controller : menuItemControllers) {
                mControllers.put(controller.getId(), controller);
            }
        }
        return this;
    }

    /**
     * Inflates {@link Menu} for the activity.
     * <p/>
     * This method should be called during activity's onCreateOptionsMenu method.
     */
    public void createOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menu.size() > 0) {
            throw new IllegalStateException("Menu has already been inflated.");
        }
        inflater.inflate(R.menu.desk_clock_menu, menu);

        final int controllerSize = mControllers.size();
        for (int i = 0; i < controllerSize; i++) {
            final MenuItemController controller = mControllers.valueAt(i);
            if (controller.isEnabled()) {
                controller.setInitialState(menu);
            }
        }
    }

    /**
     * Prepares the popup to displays all required menu items.
     * <p/>
     * This method should be called during activity's onPrepareOptionsMenu method.
     */
    public void prepareShowMenu(Menu menu) {
        final int menuSize = menu.size();
        for (int i = 0; i < menuSize; i++) {
            menu.getItem(i).setVisible(false);
        }
        final int controllerSize = mControllers.size();
        for (int i = 0; i < controllerSize; i++) {
            final MenuItemController controller = mControllers.valueAt(i);
            if (controller.isEnabled()) {
                controller.showMenuItem(menu);
            }
        }
    }

    /**
     * Handles click action for a menu item.
     * <p/>
     * This method should be called during activity's onOptionsItemSelected method.
     */
    public boolean handleMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        return mControllers.get(itemId).handleMenuItemClick(item);
    }
}
