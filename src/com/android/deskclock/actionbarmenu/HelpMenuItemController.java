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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.android.deskclock.R;
import com.android.deskclock.Utils;

/**
 * {@link MenuItemController} for help menu.
 */
public final class HelpMenuItemController extends AbstractMenuItemController {

    private static final int HELP_MENU_RES_ID = R.id.menu_item_help;
    private final Context mContext;

    public HelpMenuItemController(Context context) {
        mContext = context;
    }

    @Override
    public int getId() {
        return HELP_MENU_RES_ID;
    }

    @Override
    public void showMenuItem(Menu menu) {
        MenuItem helpItem = menu.findItem(HELP_MENU_RES_ID);
        Utils.prepareHelpMenuItem(mContext, helpItem);
        helpItem.setVisible(true);
    }

    @Override
    public boolean handleMenuItemClick(MenuItem item) {
        Intent i = item.getIntent();
        if (i != null) {
            try {
                mContext.startActivity(i);
                return true;
            } catch (ActivityNotFoundException e) {
                // No activity found to match the intent - ignore
            }
        }
        return false;
    }
}
