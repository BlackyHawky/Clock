/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.settings.AboutFragment.KEY_ABOUT_TITLE;
import static com.best.deskclock.settings.AlarmSettingsFragment.KEY_ALARM_VOLUME_SETTING;
import static com.best.deskclock.settings.AlarmSettingsFragment.KEY_SHAKE_INTENSITY;
import static com.best.deskclock.settings.ScreensaverSettingsActivity.ScreensaverSettingsFragment.KEY_SCREENSAVER_BRIGHTNESS;

import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.Objects;

public abstract class ScreenFragment extends PreferenceFragmentCompat {

    AppBarLayout mAppBarLayout;
    CollapsingToolbarLayout mCollapsingToolbarLayout;
    RecyclerView mRecyclerView;
    LinearLayoutManager mLinearLayoutManager;

    /**
     * This method should be implemented by subclasses of ScreenFragment to provide the title
     * for the fragment's collapsing toolbar.
     * <p>
     * The title returned by this method will be displayed in the collapsing toolbar layout
     * and will be correctly translated when changing the language in settings.
     *
     * @return The title of the fragment to be displayed in the collapsing toolbar.
     */
    protected abstract String getFragmentTitle() ;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getPreferenceManager().setStorageDeviceProtected();
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.add(0, Menu.NONE, 0, R.string.about_title)
                .setIcon(R.drawable.ic_about)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            Fragment existingFragment =
                    requireActivity().getSupportFragmentManager().findFragmentByTag(AboutFragment.class.getSimpleName());

            if (existingFragment == null) {
                FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
                if (DataModel.getDataModel().isFadeTransitionsEnabled()) {
                    fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                            R.anim.fade_in, R.anim.fade_out);
                } else {
                    fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right);
                }
                fragmentTransaction.replace(R.id.content_frame, new AboutFragment())
                        .addToBackStack(null)
                        .commit();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCollapsingToolbarLayout = requireActivity().findViewById(R.id.collapsing_toolbar);
        mAppBarLayout = requireActivity().findViewById(R.id.app_bar);
        mAppBarLayout.setExpanded(true, true);

        mRecyclerView = getListView();
        if (mRecyclerView != null) {
            int bottomPadding = ThemeUtils.convertDpToPixels(20, requireContext());
            int topPadding = ThemeUtils.convertDpToPixels(10, requireContext());
            mRecyclerView.setPadding(0, topPadding, 0, bottomPadding);
            mRecyclerView.setVerticalScrollBarEnabled(false);
            mLinearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mCollapsingToolbarLayout.setTitle(getFragmentTitle());
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        // this must be overridden, but is useless, because it's called during onCreate
        // so there is no possibility of calling setStorageDeviceProtected before this is called...
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        super.setPreferenceScreen(preferenceScreen);

        final boolean isCardBackgroundDisplayed = DataModel.getDataModel().isCardBackgroundDisplayed();
        final boolean isCardBorderDisplayed = DataModel.getDataModel().isCardBorderDisplayed();

        if (preferenceScreen == null) return;
        int count = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            final Preference pref = preferenceScreen.getPreference(i);
            if (pref instanceof PreferenceCategory) {
                final int subPrefCount = ((PreferenceCategory) pref).getPreferenceCount();
                for (int j = 0; j < subPrefCount; j++) {
                    if (Objects.equals(((PreferenceCategory) pref).getPreference(j).getKey(), KEY_ALARM_VOLUME_SETTING)
                            || Objects.equals(((PreferenceCategory) pref).getPreference(j).getKey(), KEY_SCREENSAVER_BRIGHTNESS)
                            || Objects.equals(((PreferenceCategory) pref).getPreference(j).getKey(), KEY_SHAKE_INTENSITY)) {
                        if (isCardBackgroundDisplayed && isCardBorderDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_seekbar_layout_bordered);
                        } else if (isCardBackgroundDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_seekbar_layout);
                        } else if (isCardBorderDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_seekbar_layout_transparent_bordered);
                        } else {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_seekbar_layout_transparent);
                        }
                    } else {
                        if (isCardBackgroundDisplayed && isCardBorderDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_layout_bordered);
                        } else if (isCardBackgroundDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_layout);
                        } else if (isCardBorderDisplayed) {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_layout_transparent_bordered);
                        } else {
                            ((PreferenceCategory) pref).getPreference(j)
                                    .setLayoutResource(R.layout.settings_preference_layout_transparent);
                        }
                    }
                }
            } else if (Objects.equals(pref.getKey(), KEY_ABOUT_TITLE)) {
                pref.setLayoutResource(R.layout.settings_about_title);
            } else {
                if (isCardBackgroundDisplayed && isCardBorderDisplayed) {
                    pref.setLayoutResource(R.layout.settings_preference_layout_bordered);
                } else if (isCardBackgroundDisplayed) {
                    pref.setLayoutResource(R.layout.settings_preference_layout);
                } else if (isCardBorderDisplayed) {
                    pref.setLayoutResource(R.layout.settings_preference_layout_transparent_bordered);
                } else {
                    pref.setLayoutResource(R.layout.settings_preference_layout_transparent);
                }
            }
        }
    }

    /**
     * Initiates a fragment transaction with custom animations to replace the current fragment.
     * The new fragment is added to the back stack, allowing for back navigation, and custom
     * slide-in and slide-out animations are applied to transition between fragments.
     *
     * @param fragment The new fragment to be displayed.
     */
    protected void animateAndShowFragment(Fragment fragment) {
        final boolean isFadeTransitionsEnabled = DataModel.getDataModel().isFadeTransitionsEnabled();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        if (isFadeTransitionsEnabled) {
            fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                    R.anim.fade_in, R.anim.fade_out);
        } else {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right);
        }
        fragmentTransaction.replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

}
