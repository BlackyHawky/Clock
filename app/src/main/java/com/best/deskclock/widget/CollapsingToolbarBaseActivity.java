/*
 * Copyright (C) 2021 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.widget;

import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_AMOLED_DARK_MODE;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.settings.AboutActivity;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.worldclock.CitySelectionActivity;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

/**
 * A base Activity that has a collapsing toolbar layout is used for the activities intending to
 * enable the collapsing toolbar function.
 */
public class CollapsingToolbarBaseActivity extends AppCompatActivity {

    @Nullable
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    @Nullable
    private AppBarLayout mAppBarLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.applyThemeAndAccentColor(this);

        super.setContentView(R.layout.collapsing_toolbar_base_layout);

        final String getDarkMode = DataModel.getDataModel().getDarkMode();
        mCollapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        if (mCollapsingToolbarLayout == null) {
            return;
        }
        if (Utils.isNight(getResources()) && getDarkMode.equals(KEY_AMOLED_DARK_MODE)) {
            mCollapsingToolbarLayout.setBackgroundColor(getColor(android.R.color.black));
            mCollapsingToolbarLayout.setContentScrimColor(getColor(android.R.color.black));
        }

        mAppBarLayout = findViewById(R.id.app_bar);
        disableCollapsingToolbarLayoutScrollingBehavior();

        final Toolbar toolbar = findViewById(R.id.action_bar);
        setSupportActionBar(toolbar);

        final boolean isFadeTransitionsEnabled = DataModel.getDataModel().isFadeTransitionsEnabled();
        if (isFadeTransitionsEnabled) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!(this instanceof RingtonePickerActivity || this instanceof CitySelectionActivity
                || this instanceof AboutActivity)) {

            menu.add(0, Menu.NONE, 0, R.string.about_title)
                    .setIcon(R.drawable.ic_about).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            return true;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            final Intent settingIntent = new Intent(getApplicationContext(), AboutActivity.class);
            startActivity(settingIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setContentView(int layoutResID) {
        final ViewGroup parent = findViewById(R.id.content_frame);
        if (parent != null) {
            parent.removeAllViews();
        }
        LayoutInflater.from(this).inflate(layoutResID, parent);
    }

    @Override
    public void setContentView(View view) {
        final ViewGroup parent = findViewById(R.id.content_frame);
        if (parent != null) {
            parent.addView(view);
        }
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        final ViewGroup parent = findViewById(R.id.content_frame);
        if (parent != null) {
            parent.addView(view, params);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (mCollapsingToolbarLayout != null) {
            mCollapsingToolbarLayout.setTitle(title);
        } else {
            super.setTitle(title);
        }
    }

    @Override
    public void setTitle(int titleId) {
        if (mCollapsingToolbarLayout != null) {
            mCollapsingToolbarLayout.setTitle(getText(titleId));
        } else {
            super.setTitle(titleId);
        }
    }

    @Override
    public boolean onNavigateUp() {
        if (!super.onNavigateUp()) {
            finishAfterTransition();
        }
        return true;
    }

    private void disableCollapsingToolbarLayoutScrollingBehavior() {
        if (mAppBarLayout == null) {
            return;
        }
        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();
        final AppBarLayout.Behavior behavior = new AppBarLayout.Behavior();
        behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                    @Override
                    public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                        return false;
                    }
                });
        params.setBehavior(behavior);
    }
}