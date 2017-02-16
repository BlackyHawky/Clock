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

import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.android.deskclock.R;
import com.android.deskclock.ScreensaverActivity;
import com.android.deskclock.events.Events;

import static android.view.Menu.NONE;

/**
 * {@link MenuItemController} for controlling night mode display.
 */
public final class NightModeMenuItemController implements MenuItemController {

    private static final int NIGHT_MODE_MENU_RES_ID = R.id.menu_item_night_mode;

    private final Context mContext;

    public NightModeMenuItemController(Context context) {
        mContext = context;
    }

    @Override
    public int getId() {
        return NIGHT_MODE_MENU_RES_ID;
    }

    @Override
    public void onCreateOptionsItem(Menu menu) {
        menu.add(NONE, NIGHT_MODE_MENU_RES_ID, NONE, R.string.menu_item_night_mode)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public void onPrepareOptionsItem(MenuItem item) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mContext.startActivity(new Intent(mContext, ScreensaverActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));
        return true;
    }
}
