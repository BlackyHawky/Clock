/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.CustomListPreference;
import com.best.deskclock.settings.custompreference.CustomPreference;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.io.File;

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
        Toolbar toolbar = requireActivity().findViewById(R.id.action_bar);

        mRecyclerView = getListView();
        if (mRecyclerView != null) {
            applyWindowInsets();
            mRecyclerView.setClipToPadding(false);
            mRecyclerView.setVerticalScrollBarEnabled(false);
            mLinearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        }

        requireActivity().addMenuProvider(new MenuProvider() {
            @SuppressLint("AlwaysShowAction")
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();

                menu.add(0, MENU_ABOUT, 0, R.string.about_title)
                        .setIcon(R.drawable.ic_about)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                toolbar.post(() -> ThemeUtils.applyToolbarTooltips(toolbar));
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
    public void onDisplayPreferenceDialog(@NonNull Preference pref) {
        if (pref instanceof CustomListPreference customListPref) {
            CustomListPreferenceDialogFragment dialog =
                    CustomListPreferenceDialogFragment.newInstance(customListPref);
            CustomListPreferenceDialogFragment.show(getChildFragmentManager(), dialog);
        } else if (pref instanceof ColorPickerPreference colorPickerPref) {
            ColorPreferenceDialogFragment dialog =
                    ColorPreferenceDialogFragment.newInstance(colorPickerPref);
            dialog.show(getChildFragmentManager(), "color_dialog");
        } else {
            super.onDisplayPreferenceDialog(pref);
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
     * Initiates a fragment transaction with custom animations to replace the current fragment.
     * The new fragment is added to the back stack, allowing for back navigation, and custom
     * slide-in/slide-out or fade_in/fade-out animations are applied to transition between fragments.
     *
     * @param fragment The new fragment to be displayed.
     */
    protected void animateAndShowFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction =
                requireActivity().getSupportFragmentManager().beginTransaction();

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

    protected void selectCustomFile(CustomPreference pref, ActivityResultLauncher<Intent> launcher,
                                    String fontPath, String prefKey, boolean isFontFile,
                                    @Nullable OnPreferenceDeleted onPreferenceDeleted) {

        if (fontPath == null) {
            selectFile(launcher, isFontFile);
        } else {
            CustomDialog.create(
                    requireContext(),
                    null,
                    null,
                    getString(isFontFile
                            ? R.string.custom_font_dialog_title
                            : R.string.background_image_dialog_title),
                    getString(isFontFile
                            ? R.string.custom_font_title_variant
                            : R.string.background_image_title_variant),
                    null,
                    getString(isFontFile ? R.string.label_new_font : R.string.label_new_image),
                    (d, w) -> selectFile(launcher, isFontFile),
                    null,
                    null,
                    getString(R.string.delete),
                    (d, w) -> {
                        mPrefs.edit().remove(prefKey).apply();
                        pref.setTitle(isFontFile ? R.string.custom_font_title : R.string.background_image_title);
                        if (onPreferenceDeleted != null) {
                            onPreferenceDeleted.onDeleted(pref);
                        }
                        deleteFile(fontPath, prefKey, isFontFile);
                    },
                    null,
                    CustomDialog.SoftInputMode.NONE
            ).show();
        }
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

        CustomToast.show(requireContext(), isFontFile
                ? R.string.custom_font_toast_message_deleted
                : R.string.background_image_toast_message_deleted);
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

    /**
     * Interface for updating preference when pressing the Delete button in the dialog box
     * that appears when a file is selected.
     */
    protected interface OnPreferenceDeleted {
        void onDeleted(CustomPreference pref);
    }

}
