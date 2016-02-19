/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.deskclock.actionbarmenu;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * {@link MenuItemController} for handling navigation up button in actionbar. It is a special
 * menu item because it's not inflated through menu.xml, and has its own predefined id.
 */
public final class NavUpMenuItemController extends AbstractMenuItemController {

    private final Activity mActivity;

    public NavUpMenuItemController(Activity activity) {
        mActivity = activity;
    }

    @Override
    public int getId() {
        return android.R.id.home;
    }

    @Override
    public void showMenuItem(Menu menu) {
        // Intentionally left empty, because back button in actionbar is not shown by inflating
        // menu layouts. It's directly controlled by actionbar.
    }

    @Override
    public boolean handleMenuItemClick(MenuItem item) {
        mActivity.finish();
        return true;
    }
}
