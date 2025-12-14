/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_TITLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
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
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.io.File;
import java.util.Objects;

public abstract class ScreenFragment extends PreferenceFragmentCompat {

    protected static final int MENU_ABOUT = 1;
    protected static final int MENU_BUG_REPORT = 2;
    protected static final int MENU_RESET_SETTINGS = 3;

    SharedPreferences mPrefs;
    DisplayMetrics mDisplayMetrics;
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
        mDisplayMetrics = getResources().getDisplayMetrics();

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

        final boolean isBackgroundDisplayed = SettingsDAO.isCardBackgroundDisplayed(mPrefs);
        final boolean isBorderDisplayed = SettingsDAO.isCardBorderDisplayed(mPrefs);

        if (preferenceScreen == null) {
            return;
        }

        int count = preferenceScreen.getPreferenceCount();

        for (int i = 0; i < count; i++) {
            final Preference pref = preferenceScreen.getPreference(i);

            if (pref instanceof PreferenceCategory category) {
                category.setLayoutResource(R.layout.settings_preference_category_layout);
                applyLayoutToCategory(category, isBackgroundDisplayed, isBorderDisplayed);
            } else if (Objects.equals(pref.getKey(), KEY_ABOUT_TITLE)) {
                pref.setLayoutResource(R.layout.settings_about_title);
            } else {
                applyLayoutToPreference(pref, isBackgroundDisplayed, isBorderDisplayed);
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

            int padding = (int) dpToPx(10, mDisplayMetrics);
            mRecyclerView.setPadding(0, padding, 0, bars.bottom + padding);
        });
    }

    /**
     * Applies the appropriate layout resource to all preferences inside the given
     * PreferenceCategory, except for custom preference types that manage their own layout.
     *
     * @param category The PreferenceCategory whose child preferences should be styled.
     * @param isBackgroundDisplayed True if the card background should be displayed.
     * @param isBorderDisplayed True if the card border should be displayed.
     */
    private void applyLayoutToCategory(PreferenceCategory category, boolean isBackgroundDisplayed,
                                       boolean isBorderDisplayed) {

        int subCount = category.getPreferenceCount();
        for (int j = 0; j < subCount; j++) {
            Preference subPref = category.getPreference(j);

            // CustomSeekbarPreference and AlarmVolumePreference manage their own layout
            if (subPref instanceof CustomSeekbarPreference || subPref instanceof AlarmVolumePreference) {
                continue;
            }

            applyLayoutToPreference(subPref, isBackgroundDisplayed, isBorderDisplayed);
        }
    }

    /**
     * Applies the correct layout resource to a single Preference based on the current
     * background and border settings.
     *
     * @param pref The Preference to style.
     * @param isBackgroundDisplayed True if the card background should be displayed.
     * @param isBorderDisplayed True if the card border should be displayed.
     */
    private void applyLayoutToPreference(Preference pref, boolean isBackgroundDisplayed,
                                         boolean isBorderDisplayed) {

        if (isBackgroundDisplayed && isBorderDisplayed) {
            pref.setLayoutResource(R.layout.settings_preference_layout_bordered);
        } else if (isBackgroundDisplayed) {
            pref.setLayoutResource(R.layout.settings_preference_layout);
        } else if (isBorderDisplayed) {
            pref.setLayoutResource(R.layout.settings_preference_layout_transparent_bordered);
        } else {
            pref.setLayoutResource(R.layout.settings_preference_layout_transparent);
        }
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

    /**
     * Opens a file picker allowing the user to select either a font file or an image file.
     *
     * @param launcher    The ActivityResultLauncher used to start the document picker.
     * @param isFontFile  True to filter for font files, false to filter for image files.
     */
    protected void selectFile(ActivityResultLauncher<Intent> launcher, boolean isFontFile) {
        final String type = isFontFile ? "*/*" : "image/*";
        final String[] mimeTypes = isFontFile
                ? new String[]{"application/x-font-ttf", "application/x-font-otf", "font/ttf", "font/otf"}
                : new String[]{"image/jpeg", "image/png"};

        launcher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(type)
                .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        );
    }

    /**
     * Deletes a file from storage and removes its associated preference entry.
     *
     * @param path        The absolute path of the file to delete.
     * @param prefKey     The preference key associated with the stored file path.
     * @param isFontFile  True if the deleted file is a font, false if it is an image.
     */
    protected void deleteFile(String path, String prefKey, boolean isFontFile) {
        clearFile(path);

        mPrefs.edit().remove(prefKey).apply();

        Toast.makeText(requireContext(), isFontFile
                ? R.string.custom_font_toast_message_deleted
                : R.string.background_image_toast_message_deleted,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Deletes the file at the given path if it exists and is a regular file.
     *
     * @param path  The absolute path of the file to delete.
     */
    protected void clearFile(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    LogUtils.w("Unable to delete file: " + path);
                }
            }
        }
    }

}
