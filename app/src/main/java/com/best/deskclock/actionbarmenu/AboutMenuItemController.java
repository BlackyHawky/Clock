// SPDX-License-Identifier: GPL-3.0-only
package com.best.deskclock.actionbarmenu;

import static android.view.Menu.NONE;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.best.deskclock.R;
import com.best.deskclock.settings.AboutActivity;

/**
 * {@link AboutMenuItemController} for about menu.
 */
public final class AboutMenuItemController implements MenuItemController {

    private static final int ABOUT_MENU_RES_ID = R.id.menu_item_about;

    private final Activity mActivity;

    public AboutMenuItemController(Activity activity) {
        mActivity = activity;
    }

    @Override
    public int getId() {
        return ABOUT_MENU_RES_ID;
    }

    @Override
    public void onCreateOptionsItem(Menu menu) {
        menu.add(NONE, ABOUT_MENU_RES_ID, NONE, R.string.about_title)
                .setIcon(R.drawable.ic_about)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public void onPrepareOptionsItem(MenuItem item) {
    }

    @Override
    public boolean onOptionsItemSelected() {
        final Intent aboutIntent = new Intent(mActivity, AboutActivity.class);
        mActivity.startActivity(aboutIntent);
        return true;
    }
}
