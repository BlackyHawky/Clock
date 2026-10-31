/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.base;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_GENERAL_FONT;
import static com.best.deskclock.utils.NotificationUtils.EXTRA_UPDATE_ALARM_NOTIFICATIONS;
import static com.best.deskclock.utils.WidgetUtils.EXTRA_UPDATE_WIDGETS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.MenuProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.CollapsingToolbarBaseLayoutBinding;
import com.best.deskclock.settings.AboutFragment;
import com.best.deskclock.settings.BackupAndRestoreManager;
import com.best.deskclock.settings.PermissionsManagementActivity;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.ColorPreferenceDialogFragment;
import com.best.deskclock.settings.custompreference.CustomAboutTitlePreference;
import com.best.deskclock.settings.custompreference.CustomListPreferenceDialogFragment;
import com.best.deskclock.settings.custompreference.CustomMultiSelectListPreferenceDialogFragment;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.color.MaterialColors;

import java.util.Locale;

public abstract class BaseSettingsScreenFragment extends PreferenceFragmentCompat {

    private static final String KEY_PENDING_FILE_PREF_KEY = "pending_file_pref_key";
    private static final String KEY_PENDING_FILE_PATH = "pending_file_path";
    private static final String KEY_PENDING_FILE_IS_FONT = "pending_file_is_font";

    private static final String KEY_IS_SEARCH_MODE = "key_is_search_mode";
    private static final String KEY_SEARCH_QUERY = "key_search_query";
    private static final String KEY_IS_SEARCH_FOCUSED = "key_is_search_focused";
    private static final String KEY_WAS_SEARCH_FOCUSED_BEFORE_CLICK = "was_search_focused_before_click";
    private static final String KEY_WAS_APPBAR_EXPANDED = "key_was_appbar_expanded";
    private static final String KEY_SCROLL_POSITION_BEFORE_SEARCH = "key_scroll_position_before_search";
    private static final String KEY_APPBAR_EXPANDED_STATE = "key_appbar_expanded_state";

    protected SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private AppBarLayout.OnOffsetChangedListener mAppBarOffsetListener;

    private Drawable mToolbarSearchIcon;
    protected boolean mIsSearchMode = false;
    private boolean mIsSearchFocused = false;
    private boolean mWasSearchFocusedBeforeClick = false;
    protected boolean mKeepSearchOpenOnPause = false;
    private boolean mSearchClosedOnPause = false;
    private boolean mBlockSearchReset = false;
    private boolean mIsAppBarExpanded = true;
    private boolean mWasAppBarExpandedBeforeSearch = true;
    private boolean mWasAppBarExpandedOnStop = true;
    private String mCurrentSearchQuery = "";
    private int mScrollPositionBeforeSearch = RecyclerView.NO_POSITION;

    private String mPendingFilePrefKey = null;
    private String mPendingFilePath = null;
    private boolean mPendingFileIsFont = false;

    private CollapsingToolbarBaseLayoutBinding mActivityBinding;

    private SharedPreferences mPrefs;
    private DataModel mDataModel;
    private Locale mLocale;
    private UiConfig.CardStyle mCardStyleConfig;
    private DisplayMetrics mDisplayMetrics;
    private Typeface mGeneralTypeface;
    private Typeface mGeneralBoldTypeface;
    private boolean mIsVibrationEnabled;
    private int mAccentStyle;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    protected AlertDialog mActiveDialog = null;

    private int mRecyclerViewPosition = RecyclerView.NO_POSITION;

    private final FragmentManager.FragmentLifecycleCallbacks mDialogLifecycleCallback =
        new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentViewDestroyed(fm, f);

                if (f instanceof DialogFragment) {
                    blockSearchResetForAWhile();
                }
            }
        };

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SdkUtils.isAtLeastAndroid7()) {
            getPreferenceManager().setStorageDeviceProtected();
        }

        mDataModel = DataModel.getDataModel();
        mLocale = Utils.getLocaleFromContext(requireContext());
        mPrefs = getDefaultSharedPreferences(requireContext());
        mDisplayMetrics = getResources().getDisplayMetrics();
        String fontPath = SettingsDAO.getGeneralFont(mPrefs);
        mGeneralTypeface = ThemeUtils.loadFont(fontPath);
        mGeneralBoldTypeface = ThemeUtils.boldTypeface(fontPath);
        mIsVibrationEnabled = SettingsDAO.isVibrationsEnabled(mPrefs);

        mAccentStyle = ThemeUtils.getAccentStyle(requireContext(),
            SettingsDAO.isAutoNightAccentColorEnabled(mPrefs),
            SettingsDAO.getAccentColor(mPrefs),
            SettingsDAO.getNightAccentColor(mPrefs));

        mCardStyleConfig = new UiConfig.CardStyle(
            SettingsDAO.isCardBackgroundDisplayed(mPrefs),
            SettingsDAO.isCardBorderDisplayed(mPrefs),
            SettingsDAO.getDarkMode(mPrefs).equals(AMOLED_DARK_MODE)
        );

        mToolbarSearchIcon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_search);
        if (mToolbarSearchIcon != null) {
            DrawableCompat.setTint(
                mToolbarSearchIcon,
                MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
            );
        }

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(requireActivity().getWindow(), false);

        if (savedInstanceState != null) {
            mPendingFilePrefKey = savedInstanceState.getString(KEY_PENDING_FILE_PREF_KEY);
            mPendingFilePath = savedInstanceState.getString(KEY_PENDING_FILE_PATH);
            mPendingFileIsFont = savedInstanceState.getBoolean(KEY_PENDING_FILE_IS_FONT);

            mIsSearchMode = savedInstanceState.getBoolean(KEY_IS_SEARCH_MODE, false);
            mCurrentSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            mIsSearchFocused = savedInstanceState.getBoolean(KEY_IS_SEARCH_FOCUSED, false);
            mWasSearchFocusedBeforeClick = savedInstanceState.getBoolean(KEY_WAS_SEARCH_FOCUSED_BEFORE_CLICK, false);
            mWasAppBarExpandedBeforeSearch = savedInstanceState.getBoolean(KEY_WAS_APPBAR_EXPANDED, true);
            mWasAppBarExpandedOnStop = savedInstanceState.getBoolean(KEY_APPBAR_EXPANDED_STATE, true);
            mScrollPositionBeforeSearch = savedInstanceState.getInt(KEY_SCROLL_POSITION_BEFORE_SEARCH, RecyclerView.NO_POSITION);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mLinearLayoutManager != null) {
            mWasAppBarExpandedOnStop = mIsAppBarExpanded;
        }

        outState.putString(KEY_PENDING_FILE_PREF_KEY, mPendingFilePrefKey);
        outState.putString(KEY_PENDING_FILE_PATH, mPendingFilePath);
        outState.putBoolean(KEY_PENDING_FILE_IS_FONT, mPendingFileIsFont);

        outState.putBoolean(KEY_IS_SEARCH_MODE, mIsSearchMode);
        outState.putString(KEY_SEARCH_QUERY, mCurrentSearchQuery);
        outState.putBoolean(KEY_IS_SEARCH_FOCUSED, mIsSearchFocused);
        outState.putBoolean(KEY_WAS_SEARCH_FOCUSED_BEFORE_CLICK, mWasSearchFocusedBeforeClick);
        outState.putBoolean(KEY_WAS_APPBAR_EXPANDED, mWasAppBarExpandedBeforeSearch);
        outState.putBoolean(KEY_APPBAR_EXPANDED_STATE, mWasAppBarExpandedOnStop);
        outState.putInt(KEY_SCROLL_POSITION_BEFORE_SEARCH, mScrollPositionBeforeSearch);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getParentFragmentManager().registerFragmentLifecycleCallbacks(mDialogLifecycleCallback, false);
        getChildFragmentManager().registerFragmentLifecycleCallbacks(mDialogLifecycleCallback, false);

        CollapsingToolbarBaseActivity activity = (CollapsingToolbarBaseActivity) requireActivity();
        mActivityBinding = activity.getBaseBinding();

        boolean shouldAnimate = mActivityBinding.appBar.isLaidOut();

        if (mIsSearchMode) {
            mActivityBinding.collapsingToolbar.setTitle(" ");
            mActivityBinding.appBar.setExpanded(false, shouldAnimate);
        } else {
            mActivityBinding.appBar.setExpanded(mWasAppBarExpandedOnStop, shouldAnimate);
        }

        // Keep track of the current expansion state of the AppBar
        mAppBarOffsetListener = (appBarLayout, verticalOffset) -> mIsAppBarExpanded = (verticalOffset == 0);
        mActivityBinding.appBar.addOnOffsetChangedListener(mAppBarOffsetListener);

        mRecyclerView = getListView();
        if (mRecyclerView != null) {
            applyWindowInsets();
            mRecyclerView.setClipToPadding(false);
            mRecyclerView.setVerticalScrollBarEnabled(false);
            mRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            mLinearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        }

        requireActivity().addMenuProvider(new MenuProvider() {
            @SuppressLint("AlwaysShowAction")
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                // When the activity is recreated, the old menu is destroyed. If the SearchView was expanded,
                // its destruction triggers the collapse listener (onMenuItemActionCollapse) and text listeners.
                // Nullifying these listeners before clearing the menu prevents the old dying SearchView
                // from resetting the search state variables (mIsSearchMode, etc.) whereas they should instead be restored.
                MenuItem oldSearchItem = menu.findItem(R.id.menu_settings_search);
                if (oldSearchItem != null) {
                    oldSearchItem.setOnActionExpandListener(null);

                    View oldActionView = oldSearchItem.getActionView();
                    if (oldActionView instanceof SearchView oldSearchView) {
                        oldSearchView.setOnQueryTextListener(null);
                        oldSearchView.setOnQueryTextFocusChangeListener(null);
                    }
                }

                menu.clear();

                if (shouldShowSearchMenu()) {
                    MenuItem searchItem = menu.add(0, R.id.menu_settings_search, 0, android.R.string.search_go)
                        .setIcon(mToolbarSearchIcon)
                        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItem.SHOW_AS_ACTION_ALWAYS);

                    configureSearchBar(searchItem);
                }

                menu.add(0, R.id.menu_about, 0, R.string.about_title)
                    .setIcon(R.drawable.ic_about)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                if (mActivityBinding != null) {
                    mActivityBinding.actionBar.post(() -> {
                        if (mActivityBinding != null) {
                            ThemeUtils.applyToolbarTooltips(mActivityBinding.actionBar, mGeneralTypeface, mDisplayMetrics);
                        }
                    });
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_about) {
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

        mKeepSearchOpenOnPause = false;

        if (mIsSearchMode) {
            // Use a space instead of an empty string to properly hide the Toolbar title.
            mActivityBinding.collapsingToolbar.setTitle(" ");
            mActivityBinding.appBar.setExpanded(false, false);
            lockAppBar(true);
        } else {
            lockAppBar(false);
            mActivityBinding.collapsingToolbar.setTitle(getFragmentTitle());

            // This only executes during navigation, because when an activity is recreated,
            // mRecyclerViewPosition will be set to NO_POSITION.
            if (mRecyclerViewPosition != RecyclerView.NO_POSITION) {
                mLinearLayoutManager.scrollToPosition(mRecyclerViewPosition);
                mRecyclerViewPosition = RecyclerView.NO_POSITION;

                if (mIsAppBarExpanded != mWasAppBarExpandedOnStop) {
                    mActivityBinding.appBar.setExpanded(mWasAppBarExpandedOnStop, true);
                }
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            if (getActivity() != null && !getActivity().isChangingConfigurations()) {
                if (!mKeepSearchOpenOnPause) {
                    mSearchClosedOnPause = true;
                    mWasAppBarExpandedOnStop = mWasAppBarExpandedBeforeSearch;
                    mSearchMenuItem.collapseActionView();
                }
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mLinearLayoutManager != null) {
            mRecyclerViewPosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
        }

        if (mSearchClosedOnPause) {
            mSearchClosedOnPause = false;
        } else {
            mWasAppBarExpandedOnStop = mIsAppBarExpanded;
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

        final Drawable.ConstantState bgSingle = ThemeUtils.rippleDrawable(context,
            ThemeUtils.expressiveCardBackground(context, mDisplayMetrics, mCardStyleConfig.isBackgroundDisplayed(),
                mCardStyleConfig.isBorderDisplayed(), mCardStyleConfig.isAmoledDarkMode(), 0, 1)).getConstantState();
        final Drawable.ConstantState bgTop = ThemeUtils.rippleDrawable(context,
            ThemeUtils.expressiveCardBackground(context, mDisplayMetrics, mCardStyleConfig.isBackgroundDisplayed(),
                mCardStyleConfig.isBorderDisplayed(), mCardStyleConfig.isAmoledDarkMode(), 0, 3)).getConstantState();
        final Drawable.ConstantState bgMiddle = ThemeUtils.rippleDrawable(context,
            ThemeUtils.expressiveCardBackground(context, mDisplayMetrics, mCardStyleConfig.isBackgroundDisplayed(),
                mCardStyleConfig.isBorderDisplayed(), mCardStyleConfig.isAmoledDarkMode(), 1, 3)).getConstantState();
        final Drawable.ConstantState bgBottom = ThemeUtils.rippleDrawable(context,
            ThemeUtils.expressiveCardBackground(context, mDisplayMetrics, mCardStyleConfig.isBackgroundDisplayed(),
                mCardStyleConfig.isBorderDisplayed(), mCardStyleConfig.isAmoledDarkMode(), 2, 3)).getConstantState();

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
                        title.setTypeface(mGeneralBoldTypeface);
                        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    }
                    return;
                }

                // Normal preferences
                if (title != null) {
                    title.setTypeface(mGeneralTypeface);
                }
                if (summary != null) {
                    summary.setTypeface(mGeneralTypeface);
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
            private Drawable.ConstantState getCardBackgroundState(@NonNull Preference pref) {
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
    public boolean onPreferenceTreeClick(@NonNull Preference preference) {
        if (!(preference instanceof SwitchPreferenceCompat)) {
            clearSearchFocus();
        }

        return super.onPreferenceTreeClick(preference);
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
        getParentFragmentManager().unregisterFragmentLifecycleCallbacks(mDialogLifecycleCallback);
        getChildFragmentManager().unregisterFragmentLifecycleCallbacks(mDialogLifecycleCallback);

        if (mActivityBinding != null && mAppBarOffsetListener != null) {
            mActivityBinding.appBar.removeOnOffsetChangedListener(mAppBarOffsetListener);
        }

        mAppBarOffsetListener = null;

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
            BackupAndRestoreManager.isRestoringBackupOrIsResettingApp = false;
            BackupAndRestoreManager.appNeedsRestart = false;
        }

        super.onDestroy();
    }

    private void configureSearchBar(@NonNull MenuItem searchItem) {
        mSearchMenuItem = searchItem;

        final SearchView searchView = new SearchView(requireContext());
        searchView.setId(R.id.menu_settings_search);
        mSearchView = searchView;

        searchView.setLayoutParams(new Toolbar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        searchView.setQueryHint(getString(android.R.string.search_go));
        searchView.setIconifiedByDefault(false);
        searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        searchView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        searchView.setBackground(ThemeUtils.pillBackgroundFromAttr(
            requireContext(), getResources().getDisplayMetrics(), com.google.android.material.R.attr.colorSecondaryContainer)
        );

        // Apply custom font to the search text
        TextView searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null) {
            searchText.setTypeface(mGeneralTypeface);
        }

        // Use a rounded icon for the search icon
        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        if (searchIcon != null) {
            searchIcon.setImageResource(R.drawable.ic_search);
        }

        // Hide the bottom bar of the search field
        View searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackground(null);
        }

        searchItem.setActionView(searchView);

        MenuItem.OnActionExpandListener expandListener = new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                mIsSearchMode = true;

                // Save the AppBar state
                mWasAppBarExpandedBeforeSearch = mIsAppBarExpanded;

                if (mLinearLayoutManager != null) {
                    // Save the scroll position
                    mScrollPositionBeforeSearch = mLinearLayoutManager.findFirstVisibleItemPosition();
                }

                // Collapse the AppBar and lock it
                if (mActivityBinding != null) {
                    // Use a space instead of an empty string to properly hide the Toolbar title.
                    mActivityBinding.collapsingToolbar.setTitle(" ");
                    mActivityBinding.appBar.setExpanded(false, true);
                    lockAppBar(true);

                    mActivityBinding.actionBar.post(() ->
                        ThemeUtils.applyToolbarTooltips(mActivityBinding.actionBar, getGeneralTypeface(), mDisplayMetrics));
                }

                // Required if the search icon is quickly clicked after opening the child fragment.
                if (mSearchView != null) {
                    if (!mIsSearchFocused && searchText != null) {
                        searchText.setFocusable(false);
                    }

                    mSearchView.clearFocus();

                    mSearchView.postDelayed(() -> {
                        if (isAdded() && mIsSearchMode && getActivity() != null && getActivity().getWindow() != null) {
                            if (searchText != null) {
                                searchText.setFocusable(true);
                                searchText.setFocusableInTouchMode(true);
                            }

                            mSearchView.requestFocus();
                            Utils.showKeyboard(getActivity().getWindow(), mSearchView);

                        }
                    }, Utils.UI_SETTLE_MEDIUM_DELAY_MS);
                }

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                mIsSearchMode = false;
                mCurrentSearchQuery = "";
                filterPreferences("");

                // Unlock the AppBar and restore its previous state
                if (mActivityBinding != null) {
                    lockAppBar(false);
                    mActivityBinding.collapsingToolbar.setTitle(getFragmentTitle());
                    mActivityBinding.appBar.setExpanded(mWasAppBarExpandedBeforeSearch, true);
                }

                // Restore the scroll position after the RecyclerView has laid out the newly visible items
                if (mRecyclerView != null && mScrollPositionBeforeSearch != RecyclerView.NO_POSITION) {
                    mRecyclerView.postDelayed(() -> {
                        if (mLinearLayoutManager != null) {
                            mLinearLayoutManager.scrollToPositionWithOffset(mScrollPositionBeforeSearch, 0);
                        }
                    }, Utils.UI_SETTLE_SHORT_DELAY_MS);
                }

                return true;
            }
        };

        SearchView.OnQueryTextListener textListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(@NonNull String query) {
                mIsSearchFocused = false;

                searchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(@NonNull String newText) {
                if (mBlockSearchReset && TextUtils.isEmpty(newText)) {
                    return true;
                }
                mCurrentSearchQuery = newText;
                filterPreferences(newText);
                return true;
            }
        };

        View.OnFocusChangeListener focusListener = (view, hasFocus) -> mIsSearchFocused = hasFocus;

        // Restoring state after a device rotation
        if (mIsSearchMode) {
            // Apply filter immediately to avoid UI flashes during rotation
            if (!TextUtils.isEmpty(mCurrentSearchQuery)) {
                filterPreferences(mCurrentSearchQuery);
            }

            // Prevents the keyboard from opening briefly
            if (!mIsSearchFocused && searchText != null) {
                searchText.setFocusable(false);
            }

            searchItem.expandActionView();
            searchView.setQuery(mCurrentSearchQuery, false);

            View targetView = searchText != null ? searchText : searchView;
            targetView.clearFocus();

            searchItem.setOnActionExpandListener(expandListener);
            searchView.setOnQueryTextListener(textListener);
            searchView.setOnQueryTextFocusChangeListener(focusListener);

            mActivityBinding.appBar.setExpanded(false, false);

            lockAppBar(true);

            // Restoring focus
            if (mIsSearchFocused) {
                targetView.postDelayed(() -> {
                    if (isAdded() && getActivity() != null && getActivity().getWindow() != null) {
                        targetView.requestFocus();
                        Utils.showKeyboard(getActivity().getWindow(), targetView);
                    }
                }, Utils.UI_SETTLE_LONG_DELAY_MS);
            } else {
                searchView.clearFocus();

                // Prevents the keyboard from opening briefly
                if (searchText != null) {
                    searchText.setFocusable(true);
                    searchText.setFocusableInTouchMode(true);
                }
            }
        } else {
            searchItem.setOnActionExpandListener(expandListener);
            searchView.setOnQueryTextListener(textListener);
            searchView.setOnQueryTextFocusChangeListener(focusListener);
        }
    }

    /**
     * Refreshes the visibility of all preferences.
     * It automatically applies the search filter if a search is active, otherwise it applies the standard business rules.
     */
    protected void invalidatePreferencesVisibility() {
        if (mIsSearchMode && !TextUtils.isEmpty(mCurrentSearchQuery)) {
            filterPreferences(mCurrentSearchQuery);
        } else {
            filterPreferences("");
        }
    }

    private void filterPreferences(@NonNull String query) {
        PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            return;
        }

        // Normalize the query once
        String normalizedQuery = Utils.removeSpecialCharacters(query).toLowerCase(mLocale);

        filterGroup(screen, normalizedQuery);
    }

    /**
     * Recursively filters preferences using normalized strings (ignoring accents and special chars).
     * @return true if at least one child preference in the group matches the query.
     */
    private boolean filterGroup(@NonNull PreferenceGroup group, @NonNull String query) {
        boolean hasVisibleChild = false;

        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);

            if (!isPreferenceAllowedToBeVisible(pref)) {
                pref.setVisible(false);
                continue;
            }

            if (pref instanceof PreferenceGroup) {
                // It's a category. Recursively filter its children.
                boolean childVisible = filterGroup((PreferenceGroup) pref, query);
                pref.setVisible(childVisible);

                if (childVisible) {
                    hasVisibleChild = true;
                }
            } else {
                // It's a regular preference
                boolean matches = TextUtils.isEmpty(query);

                if (!matches) {
                    String title = pref.getTitle() != null ?
                        Utils.removeSpecialCharacters(pref.getTitle().toString()).toLowerCase(mLocale) : "";
                    String summary = pref.getSummary() != null ?
                        Utils.removeSpecialCharacters(pref.getSummary().toString()).toLowerCase(mLocale) : "";

                    matches = title.contains(query) || summary.contains(query);
                }

                pref.setVisible(matches);

                if (matches) {
                    hasVisibleChild = true;
                }
            }
        }

        return hasVisibleChild;
    }

    /**
     * Clears the focus on the search bar when a preference is clicked, for example.
     */
    protected void clearSearchFocus() {
        mWasSearchFocusedBeforeClick = mIsSearchFocused;
        mIsSearchFocused = false;

        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
    }

    /**
     * Entry point for filtering. Handles null keys automatically.
     * (Do not override this in children anymore).
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected final boolean isPreferenceAllowedToBeVisible(@NonNull Preference pref) {
        String key = pref.getKey();
        if (key == null) {
            return true;
        }

        return isPreferenceVisible(pref, key);
    }

    /**
     * Subclasses should override this method to define business rules.
     * @param key The preference key.
     */
    protected boolean isPreferenceVisible(@NonNull Preference pref, @NonNull String key) {
        return true;
    }

    /**
     * Locks or unlocks the AppBarLayout to prevent the user from expanding it manually
     * (either by dragging it directly or by scrolling the RecyclerView).
     */
    private void lockAppBar(boolean lock) {
        if (mActivityBinding == null) {
            return;
        }

        if (mRecyclerView != null && mRecyclerView.isNestedScrollingEnabled() == !lock) {
            return;
        }

        // Prevent the user from dragging the AppBar itself
        ViewGroup.LayoutParams layoutParams = mActivityBinding.appBar.getLayoutParams();
        if (layoutParams instanceof CoordinatorLayout.LayoutParams coordinatorParams) {
            CoordinatorLayout.Behavior<?> behavior = coordinatorParams.getBehavior();

            // If there's no behavior yet, attach the default one
            if (behavior == null) {
                behavior = new AppBarLayout.Behavior();
                coordinatorParams.setBehavior(behavior);
            }

            if (behavior instanceof AppBarLayout.Behavior appBarBehavior) {
                appBarBehavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                    @Override
                    public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                        // Block dragging if locked
                        return !lock;
                    }
                });
            }
        }

        // Prevent the RecyclerView from pushing/pulling the AppBar when scrolling
        if (mRecyclerView != null) {
            mRecyclerView.setNestedScrollingEnabled(!lock);
        }
    }

    /**
     * Protects the search against abusive deletion by the Android system during interface updates
     * (e.g., when closing a Dialog).
     */
    protected void blockSearchResetForAWhile() {
        if (mBlockSearchReset) {
            return;
        }

        mBlockSearchReset = true;

        boolean shouldRestoreFocus = mWasSearchFocusedBeforeClick;

        if (shouldRestoreFocus) {
            mIsSearchFocused = true;

            if (getActivity() != null && getActivity().getWindow() != null) {
                mSearchView.postDelayed(() -> {
                    if (isAdded() && getActivity() != null) {
                        mSearchView.requestFocus();
                        Utils.showKeyboard(getActivity().getWindow(), mSearchView);
                    }
                }, Utils.UI_SETTLE_SHORT_DELAY_MS);
            }
        }

        mWasSearchFocusedBeforeClick = false;

        if (getView() != null) {
            getView().postDelayed(() -> mBlockSearchReset = false, Utils.UI_SETTLE_MEDIUM_DELAY_MS);
        }
    }

    /**
     * {@code true} if the Search menu should be displayed. {@code false} otherwise.
     */
    protected boolean shouldShowSearchMenu() {
        return true;
    }

    private boolean isCardPreference(@Nullable Preference preference) {
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
    protected void animateAndShowFragment(@NonNull Fragment fragment) {
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

    protected void restoreCustomFileDialogIfNeeded(@NonNull String targetPrefKey, @NonNull Preference pref,
                                                   @NonNull ActivityResultLauncher<Intent> launcher,
                                                   @Nullable OnPreferenceDeleted onPreferenceDeleted) {

        if (mPendingFilePrefKey != null && mPendingFilePrefKey.equals(targetPrefKey)) {
            if (mActiveDialog == null || !mActiveDialog.isShowing()) {
                selectCustomFile(pref, launcher, mPendingFilePath, mPendingFilePrefKey, mPendingFileIsFont, true, onPreferenceDeleted);
            }
        }
    }

    protected void selectCustomFile(@NonNull Preference pref, @NonNull ActivityResultLauncher<Intent> launcher, @Nullable String fontPath,
                                    @NonNull String prefKey, boolean isFontFile, boolean isRestoring,
                                    @Nullable OnPreferenceDeleted onPreferenceDeleted) {

        if (!isRestoring) {
            clearSearchFocus();
        }

        if (fontPath == null) {
            mKeepSearchOpenOnPause = true;
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
                    mKeepSearchOpenOnPause = true;
                    FileUtils.selectFile(launcher, isFontFile);
                },
                null,
                null,
                getString(R.string.delete),
                (d, w) -> {
                    mPendingFilePrefKey = null;

                    if (mIsSearchMode && isAdded()) {
                        blockSearchResetForAWhile();
                    }

                    mPrefs.edit().remove(prefKey).apply();
                    pref.setTitle(isFontFile ? R.string.custom_font_title : R.string.background_image_title);
                    pref.setSummary(null);

                    if (onPreferenceDeleted != null) {
                        onPreferenceDeleted.onDeleted();
                    }

                    final Context appContext = requireContext().getApplicationContext();
                    final int style = getAccentStyle();
                    final Typeface toastFont;

                    if (KEY_GENERAL_FONT.equals(prefKey)) {
                        toastFont = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(mPrefs));
                    } else {
                        toastFont = getGeneralTypeface();
                    }

                    FileUtils.deleteCustomFile(appContext, style, toastFont, fontPath, isFontFile);
                },
                (alertDialog -> alertDialog.setOnDismissListener(d -> {
                    mPendingFilePrefKey = null;

                    if (mIsSearchMode && isAdded()) {
                        blockSearchResetForAWhile();
                    }
                })),
                CustomDialog.SoftInputMode.NONE
            );

            mActiveDialog.show();
        }
    }

    /**
     * @return a dialog to be displayed after a restore or reset.
     */
    protected AlertDialog restartAppDialog(@NonNull Context appContext, boolean isResettingApp) {
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

                Utils.applyAppLanguage(SettingsDAO.getLanguageCode(mPrefs), isResettingApp);

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

    protected final DataModel getDataModel() {
        return mDataModel;
    }

    protected final SharedPreferences getPrefs() {
        return mPrefs;
    }

    protected final Typeface getGeneralTypeface() {
        return mGeneralTypeface;
    }

    protected final boolean isVibrationsEnabled() {
        return mIsVibrationEnabled;
    }

    protected final int getAccentStyle() {
        return mAccentStyle;
    }

    /**
     * Interface for updating preference when pressing the Delete button in the dialog box
     * that appears when a file is selected.
     */
    protected interface OnPreferenceDeleted {
        void onDeleted();
    }

}
