/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_TITLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SHADOW_OFFSET;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_TITLE_FONT_SIZE_PREF;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_VOLUME_SETTING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_BLUETOOTH_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAXIMUM_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_MAXIMUM_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.Objects;

public abstract class ScreenFragment extends PreferenceFragmentCompat {

    protected static final int MENU_ABOUT = 1;
    protected static final int MENU_BUG_REPORT = 2;
    protected static final int MENU_RESET_SETTINGS = 3;

    SharedPreferences mPrefs;
    CoordinatorLayout mCoordinatorLayout;
    AppBarLayout mAppBarLayout;
    CollapsingToolbarLayout mCollapsingToolbarLayout;
    RecyclerView mRecyclerView;
    LinearLayoutManager mLinearLayoutManager;

    int mRecyclerViewPosition = -1;

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
        if (SdkUtils.isAtLeastAndroid7()) {
            getPreferenceManager().setStorageDeviceProtected();
        }

        mPrefs = getDefaultSharedPreferences(requireContext());

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(requireActivity().getWindow(), false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCoordinatorLayout = requireActivity().findViewById(R.id.coordinator_layout);
        mCollapsingToolbarLayout = requireActivity().findViewById(R.id.collapsing_toolbar);
        mAppBarLayout = requireActivity().findViewById(R.id.app_bar);
        mAppBarLayout.setExpanded(true, true);

        mRecyclerView = getListView();
        if (mRecyclerView != null) {
            applyWindowInsets();
            mRecyclerView.setClipToPadding(false);
            mRecyclerView.setVerticalScrollBarEnabled(false);
            mLinearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        }

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();

                menu.add(0, MENU_ABOUT, 0, R.string.about_title)
                        .setIcon(R.drawable.ic_about)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == MENU_ABOUT) {
                    Fragment existingFragment =
                            requireActivity().getSupportFragmentManager()
                                    .findFragmentByTag(AboutFragment.class.getSimpleName());

                    if (existingFragment == null) {
                        animateAndShowFragment(new AboutFragment());
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    @Override
    public void onResume() {
        super.onResume();

        mCollapsingToolbarLayout.setTitle(getFragmentTitle());

        if (mRecyclerViewPosition != -1) {
            mLinearLayoutManager.scrollToPosition(mRecyclerViewPosition);
            mAppBarLayout.setExpanded(mRecyclerViewPosition == 0, true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mLinearLayoutManager != null) {
            mRecyclerViewPosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
        }
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        // this must be overridden, but is useless, because it's called during onCreate
        // so there is no possibility of calling setStorageDeviceProtected before this is called...
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        super.setPreferenceScreen(preferenceScreen);

        final boolean isCardBackgroundDisplayed = SettingsDAO.isCardBackgroundDisplayed(mPrefs);
        final boolean isCardBorderDisplayed = SettingsDAO.isCardBorderDisplayed(mPrefs);

        if (preferenceScreen == null) return;
        int count = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            final Preference pref = preferenceScreen.getPreference(i);
            if (pref instanceof PreferenceCategory category) {
                category.setLayoutResource(R.layout.settings_preference_category_layout);

                final int subPrefCount = category.getPreferenceCount();
                for (int j = 0; j < subPrefCount; j++) {
                    Preference subPref = category.getPreference(j);
                    if (Objects.equals(subPref.getKey(), KEY_ALARM_VOLUME_SETTING)
                            || Objects.equals(subPref.getKey(), KEY_SCREENSAVER_BRIGHTNESS)
                            || Objects.equals(subPref.getKey(), KEY_BLUETOOTH_VOLUME)
                            || Objects.equals(subPref.getKey(), KEY_SHAKE_INTENSITY)
                            || Objects.equals(subPref.getKey(), KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE)
                            || Objects.equals(subPref.getKey(), KEY_ALARM_TITLE_FONT_SIZE_PREF)
                            || Objects.equals(subPref.getKey(), KEY_ALARM_SHADOW_OFFSET)
                            || Objects.equals(subPref.getKey(), KEY_ALARM_BLUR_INTENSITY)
                            || Objects.equals(subPref.getKey(), KEY_TIMER_SHAKE_INTENSITY)
                            || Objects.equals(subPref.getKey(), KEY_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE)
                            || Objects.equals(subPref.getKey(), KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE)
                            || Objects.equals(subPref.getKey(), KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_MAXIMUM_FONT_SIZE)
                            || Objects.equals(subPref.getKey(), KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE)
                            || Objects.equals(subPref.getKey(), KEY_NEXT_ALARM_WIDGET_MAXIMUM_FONT_SIZE)
                            || Objects.equals(subPref.getKey(), KEY_VERTICAL_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE)) {
                        if (isCardBackgroundDisplayed && isCardBorderDisplayed) {
                            subPref.setLayoutResource(R.layout.settings_preference_seekbar_layout_bordered);
                        } else if (isCardBackgroundDisplayed) {
                            subPref.setLayoutResource(R.layout.settings_preference_seekbar_layout);
                        } else if (isCardBorderDisplayed) {
                            subPref.setLayoutResource(R.layout.settings_preference_seekbar_layout_transparent_bordered);
                        } else {
                            subPref.setLayoutResource(R.layout.settings_preference_seekbar_layout_transparent);
                        }
                    } else {
                        if (isCardBackgroundDisplayed && isCardBorderDisplayed) {
                            subPref.setLayoutResource(R.layout.settings_preference_layout_bordered);
                        } else if (isCardBackgroundDisplayed) {
                            subPref.setLayoutResource(R.layout.settings_preference_layout);
                        } else if (isCardBorderDisplayed) {
                            subPref.setLayoutResource(R.layout.settings_preference_layout_transparent_bordered);
                        } else {
                            subPref.setLayoutResource(R.layout.settings_preference_layout_transparent);
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
     * This method adjusts the space occupied by system elements (such as the status bar,
     * navigation bar or screen notch) and adjust the display of the application interface
     * accordingly.
     * <p>
     * Note: Not applicable for the {@link PermissionsManagementActivity.PermissionsManagementFragment}
     * fragment because it does not have a RecyclerView. Therefore, this fragment has its own method.
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mCoordinatorLayout, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() |
                    WindowInsetsCompat.Type.displayCutout());

            v.setPadding(bars.left, bars.top, bars.right, 0);

            int padding = ThemeUtils.convertDpToPixels(10, requireContext());
            mRecyclerView.setPadding(0, padding, 0, bars.bottom + padding);
        });
    }

    /**
     * Initiates a fragment transaction with custom animations to replace the current fragment.
     * The new fragment is added to the back stack, allowing for back navigation, and custom
     * slide-in/slide-out or fade_in/fade-out animations are applied to transition between fragments.
     *
     * @param fragment The new fragment to be displayed.
     */
    protected void animateAndShowFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();

        if (ThemeUtils.areSystemAnimationsDisabled(requireContext())) {
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_NONE);
        } else if (SettingsDAO.isFadeTransitionsEnabled(mPrefs)) {
            fragmentTransaction.setCustomAnimations(
                    R.anim.fade_in, R.anim.fade_out,
                    R.anim.fade_in, R.anim.fade_out);
        } else {
            fragmentTransaction.setCustomAnimations(
                    R.anim.fragment_slide_from_right, R.anim.fragment_slide_to_left,
                    R.anim.fragment_slide_from_left, R.anim.fragment_slide_to_right);
        }
        fragmentTransaction.replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

}
