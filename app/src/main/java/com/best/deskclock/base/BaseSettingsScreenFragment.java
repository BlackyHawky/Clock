/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.base;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.utils.NotificationUtils.EXTRA_UPDATE_ALARM_NOTIFICATIONS;
import static com.best.deskclock.utils.WidgetUtils.EXTRA_UPDATE_WIDGETS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.CollapsingToolbarBaseLayoutBinding;
import com.best.deskclock.settings.AboutFragment;
import com.best.deskclock.settings.PermissionsManagementActivity;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.ColorPreferenceDialogFragment;
import com.best.deskclock.settings.custompreference.CustomAboutTitlePreference;
import com.best.deskclock.settings.custompreference.CustomListPreferenceDialogFragment;
import com.best.deskclock.settings.custompreference.CustomMultiSelectListPreferenceDialogFragment;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.BackupAndRestoreUtils;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

public abstract class BaseSettingsScreenFragment extends PreferenceFragmentCompat {

    private static final String KEY_PENDING_FILE_PREF_KEY = "pending_file_pref_key";
    private static final String KEY_PENDING_FILE_PATH = "pending_file_path";
    private static final String KEY_PENDING_FILE_IS_FONT = "pending_file_is_font";

    private String mPendingFilePrefKey = null;
    private String mPendingFilePath = null;
    private boolean mPendingFileIsFont = false;

    protected static final int MENU_ABOUT = 1;
    protected static final int MENU_BUG_REPORT = 2;
    protected static final int MENU_RESET_SETTINGS = 3;

    private CollapsingToolbarBaseLayoutBinding mActivityBinding;

    public SharedPreferences mPrefs;
    private DisplayMetrics mDisplayMetrics;
    private Typeface mRegularTypeface;
    private Typeface mBoldTypeface;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    public AlertDialog mActiveDialog = null;

    int mRecyclerViewPosition = -1;

    /**
     * This method should be implemented by subclasses of BaseSettingsScreenFragment to provide the title
     * for the fragment's collapsing toolbar.
     * <p>
     * The title returned by this method will be displayed in the collapsing toolbar layout
     * and will be correctly translated when changing the language in settings.
     *
     * @return The title of the fragment to be displayed in the collapsing toolbar.
     */
    protected abstract String getFragmentTitle();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SdkUtils.isAtLeastAndroid7()) {
            getPreferenceManager().setStorageDeviceProtected();
        }

        mPrefs = getDefaultSharedPreferences(requireContext());
        mDisplayMetrics = getResources().getDisplayMetrics();
        String fontPath = SettingsDAO.getGeneralFont(mPrefs);
        mRegularTypeface = ThemeUtils.loadFont(fontPath);
        mBoldTypeface = ThemeUtils.boldTypeface(fontPath);

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(requireActivity().getWindow(), false);

        if (savedInstanceState != null) {
            mPendingFilePrefKey = savedInstanceState.getString(KEY_PENDING_FILE_PREF_KEY);
            mPendingFilePath = savedInstanceState.getString(KEY_PENDING_FILE_PATH);
            mPendingFileIsFont = savedInstanceState.getBoolean(KEY_PENDING_FILE_IS_FONT);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_PENDING_FILE_PREF_KEY, mPendingFilePrefKey);
        outState.putString(KEY_PENDING_FILE_PATH, mPendingFilePath);
        outState.putBoolean(KEY_PENDING_FILE_IS_FONT, mPendingFileIsFont);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CollapsingToolbarBaseActivity activity = (CollapsingToolbarBaseActivity) requireActivity();
        mActivityBinding = activity.getBaseBinding();

        mActivityBinding.appBar.setExpanded(true, true);

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

                if (mActivityBinding != null) {
                    mActivityBinding.actionBar.post(() -> {
                        if (mActivityBinding != null) {
                            ThemeUtils.applyToolbarTooltips(mActivityBinding.actionBar);
                        }
                    });
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == MENU_ABOUT) {
                    Fragment existingFragment =
                        requireActivity().getSupportFragmentManager().findFragmentByTag(AboutFragment.class.getSimpleName());

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

        mActivityBinding.collapsingToolbar.setTitle(getFragmentTitle());

        if (mRecyclerViewPosition != -1) {
            mLinearLayoutManager.scrollToPosition(mRecyclerViewPosition);
            mActivityBinding.appBar.setExpanded(mRecyclerViewPosition == 0, true);
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
        // This must be overridden, but is useless, because it's called during onCreate
        // so there is no possibility of calling setStorageDeviceProtected before this is called...
    }

    /**
     * Overrides the default preference adapter to apply custom Material 3 Expressive styling.
     *
     * <p>This method intercepts the binding of each preference to its view (ViewHolder) to:
     * <ul>
     * <li>Apply custom fonts (regular/bold) and text sizes to titles and summaries.</li>
     * <li>Calculate the exact visual position of a preference within its contiguous block.</li>
     * <li>Apply dynamic rounded corners and a ripple effect based on that position.</li>
     * </ul>
     *
     * @param preferenceScreen The root preference screen containing the preferences.
     * @return A custom {@link PreferenceGroupAdapter} handling the UI updates.
     */
    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    protected RecyclerView.Adapter<?> onCreateAdapter(@NonNull PreferenceScreen preferenceScreen) {
        final Context context = preferenceScreen.getContext();

        final Drawable.ConstantState bgSingle = ThemeUtils.rippleDrawable(
            context, ThemeUtils.expressiveCardBackground(context, 0, 1)).getConstantState();
        final Drawable.ConstantState bgTop = ThemeUtils.rippleDrawable(
            context, ThemeUtils.expressiveCardBackground(context, 0, 3)).getConstantState();
        final Drawable.ConstantState bgMiddle = ThemeUtils.rippleDrawable(
            context, ThemeUtils.expressiveCardBackground(context, 1, 3)).getConstantState();
        final Drawable.ConstantState bgBottom = ThemeUtils.rippleDrawable(
            context, ThemeUtils.expressiveCardBackground(context, 2, 3)).getConstantState();

        return new PreferenceGroupAdapter(preferenceScreen) {
            @Override
            public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    adapterPosition = position;
                }

                super.onBindViewHolder(holder, adapterPosition);

                Preference pref = getItem(adapterPosition);
                if (pref == null) {
                    return;
                }

                TextView title = (TextView) holder.findViewById(android.R.id.title);
                TextView summary = (TextView) holder.findViewById(android.R.id.summary);

                // Categories
                if (pref instanceof PreferenceCategory) {
                    if (title != null) {
                        title.setTypeface(mBoldTypeface);
                        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    }
                    return;
                }

                // Normal preferences
                if (title != null) {
                    title.setTypeface(mRegularTypeface);
                }
                if (summary != null) {
                    summary.setTypeface(mRegularTypeface);
                }

                View cardView = holder.itemView.findViewById(R.id.pref_card_view);

                // Apply the Material Expressive background
                if (cardView != null) {
                    Drawable.ConstantState bgState = getCardBackgroundState(pref);

                    if (bgState != null) {
                        cardView.setBackground(bgState.newDrawable());
                    }
                }
            }

            /**
             * Calculates the preference's position within its group and returns the appropriate background.
             */
            private Drawable.ConstantState getCardBackgroundState(Preference pref) {
                int visibleCount = 1;
                int visibleIndex = 0;

                PreferenceGroup parent = pref.getParent();
                if (parent != null) {
                    int tempCount = 0;
                    boolean foundTarget = false;

                    for (int i = 0; i < parent.getPreferenceCount(); i++) {
                        Preference child = parent.getPreference(i);

                        if (!child.isVisible()) {
                            // Ignore hidden items
                            continue;
                        }

                        if (isCardPreference(child)) {
                            if (child == pref) {
                                foundTarget = true;
                                visibleIndex = tempCount;
                            }
                            tempCount++;
                        } else {
                            if (foundTarget) {
                                break;
                            } else {
                                tempCount = 0;
                            }
                        }
                    }

                    if (foundTarget) {
                        visibleCount = tempCount;
                    }
                }

                if (visibleCount <= 1) {
                    return bgSingle;
                } else if (visibleIndex == 0) {
                    return bgTop;
                } else if (visibleIndex == visibleCount - 1) {
                    return bgBottom;
                } else {
                    return bgMiddle;
                }
            }

        };
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference pref) {
        if (pref instanceof ListPreference customListPref) {
            CustomListPreferenceDialogFragment dialog = CustomListPreferenceDialogFragment.newInstance(customListPref);
            CustomListPreferenceDialogFragment.show(getChildFragmentManager(), dialog);
        } else if (pref instanceof MultiSelectListPreference customMultiSelectListPref) {
            CustomMultiSelectListPreferenceDialogFragment dialog =
                CustomMultiSelectListPreferenceDialogFragment.newInstance(customMultiSelectListPref);
            CustomMultiSelectListPreferenceDialogFragment.show(getChildFragmentManager(), dialog);
        } else if (pref instanceof ColorPickerPreference colorPickerPref) {
            ColorPreferenceDialogFragment dialog = ColorPreferenceDialogFragment.newInstance(colorPickerPref);
            ColorPreferenceDialogFragment.show(getChildFragmentManager(), dialog);
        } else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    @Override
    public void onDestroyView() {
        if (mActiveDialog != null && mActiveDialog.isShowing()) {
            mActiveDialog.dismiss();
            mActiveDialog = null;
        }

        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(null);
        }

        mActivityBinding = null;
        mRecyclerView = null;
        mLinearLayoutManager = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (getActivity() == null || !getActivity().isChangingConfigurations()) {
            BackupAndRestoreUtils.isRestoringBackupOrIsResettingApp = false;
            BackupAndRestoreUtils.appNeedsRestart = false;
        }

        super.onDestroy();
    }

    private boolean isCardPreference(Preference preference) {
        return preference != null
            && !(preference instanceof PreferenceCategory)
            && !(preference instanceof CustomAboutTitlePreference);
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
        InsetsUtils.doOnApplyWindowInsets(mActivityBinding.coordinatorLayout, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

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

    protected void nullifyPreferenceListeners(Preference... preferences) {
        for (Preference pref : preferences) {
            if (pref != null) {
                pref.setOnPreferenceClickListener(null);
                pref.setOnPreferenceChangeListener(null);
            }
        }
    }

    protected void restoreCustomFileDialogIfNeeded(String targetPrefKey, Preference pref, ActivityResultLauncher<Intent> launcher,
                                                   @Nullable OnPreferenceDeleted onPreferenceDeleted) {

        if (mPendingFilePrefKey != null && mPendingFilePrefKey.equals(targetPrefKey)) {
            if (mActiveDialog == null || !mActiveDialog.isShowing()) {
                selectCustomFile(pref, launcher, mPendingFilePath, mPendingFilePrefKey, mPendingFileIsFont, onPreferenceDeleted);
            }
        }
    }

    protected void selectCustomFile(Preference pref, ActivityResultLauncher<Intent> launcher, String fontPath, String prefKey,
                                    boolean isFontFile, @Nullable OnPreferenceDeleted onPreferenceDeleted) {

        if (fontPath == null) {
            FileUtils.selectFile(launcher, isFontFile);
        } else {
            mPendingFilePrefKey = prefKey;
            mPendingFilePath = fontPath;
            mPendingFileIsFont = isFontFile;

            mActiveDialog = CustomDialog.create(
                requireContext(),
                null,
                null,
                getString(isFontFile
                    ? R.string.custom_font_dialog_title
                    : R.string.background_image_dialog_title),
                getString(isFontFile
                    ? R.string.custom_font_dialog_message
                    : R.string.background_image_dialog_message),
                null,
                getString(isFontFile ? R.string.label_new_font : R.string.label_new_image),
                (d, w) -> {
                    mPendingFilePrefKey = null;
                    FileUtils.selectFile(launcher, isFontFile);
                },
                null,
                null,
                getString(R.string.delete),
                (d, w) -> {
                    mPendingFilePrefKey = null;

                    mPrefs.edit().remove(prefKey).apply();
                    pref.setTitle(isFontFile ? R.string.custom_font_title : R.string.background_image_title);
                    pref.setSummary(null);

                    if (onPreferenceDeleted != null) {
                        onPreferenceDeleted.onDeleted();
                    }

                    FileUtils.deleteCustomFile(requireContext().getApplicationContext(), fontPath, isFontFile);
                },
                (alertDialog -> alertDialog.setOnDismissListener(d -> mPendingFilePrefKey = null)),
                CustomDialog.SoftInputMode.NONE
            );

            mActiveDialog.show();
        }
    }

    /**
     * @return a dialog to be displayed after a restore or reset.
     */
    protected AlertDialog restartAppDialog(Context appContext, boolean isResettingApp) {
        final AlertDialog dialog = CustomDialog.create(
            requireActivity(),
            null,
            AppCompatResources.getDrawable(requireContext(), isResettingApp ? R.drawable.ic_reset_settings :R.drawable.ic_backup_restore),
            getString(isResettingApp ? R.string.restart_app_dialog_title_for_reset : R.string.restart_app_dialog_title_for_restore),
            getString(R.string.restart_app_dialog_message),
            null,
            getString(android.R.string.ok),
            (d, w) -> {
                NotificationUtils.clearAllNotifications(appContext);

                Utils.applyAppLanguage(appContext, isResettingApp);

                Intent restartIntent = new Intent(appContext, DeskClock.class);
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                restartIntent.putExtra(EXTRA_UPDATE_WIDGETS, true);
                if (!isResettingApp) {
                    restartIntent.putExtra(EXTRA_UPDATE_ALARM_NOTIFICATIONS, true);
                }
                appContext.startActivity(restartIntent);
                Runtime.getRuntime().exit(0);
            },
            null,
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.NONE
        );

        dialog.setCancelable(false);

        return dialog;
    }

    /**
     * Interface for updating preference when pressing the Delete button in the dialog box
     * that appears when a file is selected.
     */
    protected interface OnPreferenceDeleted {
        void onDeleted();
    }

}
