/*
 * Copyright (C) 2021 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.best.deskclock.BaseActivity;
import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.CollapsingToolbarBaseLayoutBinding;
import com.best.deskclock.settings.SettingsActivity;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.appbar.AppBarLayout;

/**
 * A base Activity that has a collapsing toolbar layout is used for the activities intending to
 * enable the collapsing toolbar function.
 */
public abstract class CollapsingToolbarBaseActivity extends BaseActivity {

    protected CollapsingToolbarBaseLayoutBinding mBaseBinding;

    /**
     * This method should be implemented by subclasses of CollapsingToolbarBaseActivity
     * to provide the title for the activity's collapsing toolbar.
     * <p>
     * The title returned by this method will be displayed in the collapsing toolbar layout
     * and will be correctly translated when changing the language in settings.
     *
     * @return The title of the activity to be displayed in the collapsing toolbar.
     */
    protected abstract String getActivityTitle();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        final SharedPreferences prefs = getDefaultSharedPreferences(this);
        boolean isFadeTransitionEnabled = SettingsDAO.isFadeTransitionsEnabled(prefs);

        if (isFadeTransitionEnabled) {
            if (SdkUtils.isAtLeastAndroid14()) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
            } else {
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        } else {
            if (SdkUtils.isAtLeastAndroid14()) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
            } else {
                overridePendingTransition(R.anim.activity_slide_from_right, R.anim.activity_slide_to_left);
            }
        }

        super.onCreate(savedInstanceState);

        mBaseBinding = CollapsingToolbarBaseLayoutBinding.inflate(getLayoutInflater());

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ThemeUtils.allowDisplayCutout(getWindow());

        super.setContentView(mBaseBinding.getRoot());

        final String getDarkMode = SettingsDAO.getDarkMode(prefs);

        final Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));
        mBaseBinding.collapsingToolbar.setExpandedTitleTypeface(typeface);
        mBaseBinding.collapsingToolbar.setCollapsedTitleTypeface(typeface);

        if (ThemeUtils.isNight(getResources()) && getDarkMode.equals(AMOLED_DARK_MODE)) {
            mBaseBinding.collapsingToolbar.setBackgroundColor(getColor(android.R.color.black));
            mBaseBinding.collapsingToolbar.setContentScrimColor(getColor(android.R.color.black));
        }

        disableCollapsingToolbarLayoutScrollingBehavior();

        setSupportActionBar(mBaseBinding.actionBar);

        applyWindowInsets();

        // Exclude SettingsActivity as this is handled in SettingsFragment.
        if (!(this instanceof SettingsActivity)) {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finish();
                    if (isFadeTransitionEnabled) {
                        if (SdkUtils.isAtLeastAndroid14()) {
                            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.fade_in, R.anim.fade_out);
                        } else {
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        }
                    } else {
                        if (SdkUtils.isAtLeastAndroid14()) {
                            overrideActivityTransition(
                                OVERRIDE_TRANSITION_CLOSE, R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
                        } else {
                            overridePendingTransition(R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mBaseBinding.collapsingToolbar.setTitle(getActivityTitle());
    }

    @Override
    public void setContentView(int layoutResID) {
        mBaseBinding.contentFrame.removeAllViews();
        getLayoutInflater().inflate(layoutResID, mBaseBinding.contentFrame);
    }

    @Override
    public void setContentView(View view) {
        mBaseBinding.contentFrame.addView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        mBaseBinding.contentFrame.addView(view, params);
    }

    @Override
    public void setTitle(CharSequence title) {
        mBaseBinding.collapsingToolbar.setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        mBaseBinding.collapsingToolbar.setTitle(getText(titleId));
    }

    @Override
    public boolean onNavigateUp() {
        if (!super.onNavigateUp()) {
            finishAfterTransition();
        }
        return true;
    }

    public CollapsingToolbarBaseLayoutBinding getBaseBinding() {
        return mBaseBinding;
    }

    private void disableCollapsingToolbarLayoutScrollingBehavior() {
        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mBaseBinding.appBar.getLayoutParams();
        final AppBarLayout.Behavior behavior = new AppBarLayout.Behavior();
        behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
            @Override
            public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                return false;
            }
        });

        params.setBehavior(behavior);
    }


    /**
     * This method adjusts the spacing of the Toolbar and content to take into account system insets,
     * so that they are not obscured by system elements (status bar, navigation bar or cutout).
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mBaseBinding.appBar, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            v.setPadding(bars.left, bars.top, bars.right, 0);
        });
    }

}
