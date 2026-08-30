/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.base;

import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.FabContainer;
import com.best.deskclock.uicomponents.FabController;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.uidata.UiDataModel.Tab;
import com.best.deskclock.utils.ThemeUtils;

import java.util.Locale;

public abstract class DeskClockFragment extends Fragment implements FabContainer, FabController {

    /**
     * The tab associated with this fragment.
     */
    private final Tab mTab;

    /**
     * The container that houses the fab and its left and right buttons.
     */
    private FabContainer mFabContainer;

    private final DataModel mDataModel;
    private final UiDataModel mUiDataModel;
    private SharedPreferences mPrefs;
    private DisplayMetrics mDisplayMetrics;
    private Typeface mGeneralTypeface;
    private Typeface mGeneralBoldTypeface;
    private Locale mLocale;
    private boolean mIsTablet;
    private boolean mIsPortrait;
    private boolean mIsLandscape;
    private boolean mIsRtl;
    private boolean mIsCardBackgroundDisplayed;
    private boolean mIsCardBorderDisplayed;
    private boolean mIsAmoled;
    private boolean mIsVibrationEnabled;

    public DeskClockFragment(Tab tab) {
        mTab = tab;
        mDataModel = DataModel.getDataModel();
        mUiDataModel = UiDataModel.getUiDataModel();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = DeskClockApplication.getDefaultSharedPreferences(requireContext());
        mDisplayMetrics = getResources().getDisplayMetrics();
        String generalFontPath = SettingsDAO.getGeneralFont(mPrefs);
        mGeneralTypeface = ThemeUtils.loadFont(generalFontPath);
        mGeneralBoldTypeface = ThemeUtils.boldTypeface(generalFontPath);
        mLocale = Locale.getDefault();
        mIsTablet = ThemeUtils.isTablet();
        mIsPortrait = ThemeUtils.isPortrait();
        mIsLandscape = ThemeUtils.isLandscape();
        mIsRtl = ThemeUtils.isRTL(requireContext());
        mIsCardBackgroundDisplayed = SettingsDAO.isCardBackgroundDisplayed(mPrefs);
        mIsCardBorderDisplayed = SettingsDAO.isCardBorderDisplayed(mPrefs);
        mIsAmoled = SettingsDAO.getDarkMode(mPrefs).equals(AMOLED_DARK_MODE);
        mIsVibrationEnabled = SettingsDAO.isVibrationsEnabled(mPrefs);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update the fab and buttons in case their state changed while the fragment was paused.
        updateFab(FAB_AND_BUTTONS_IMMEDIATE);
    }

    @Override
    public void onDestroy() {
        mFabContainer = null;

        super.onDestroy();
    }

    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        // By default, return false so event continues to propagate
        return false;
    }

    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        // By default, return false so event continues to propagate
        return false;
    }

    /**
     * Called before onUpdateFab when the fab should be animated.
     *
     * @param fab the fab component to be configured based on current state
     */
    public void onMorphFab(@NonNull ImageView fab) {
    }

    /**
     * @param fabContainer the container that houses the fab and its left and right buttons
     */
    public final void setFabContainer(@Nullable FabContainer fabContainer) {
        mFabContainer = fabContainer;
    }

    /**
     * Requests that the parent activity update the fab and buttons.
     *
     * @param updateTypes the manner in which the fab container should be updated
     */
    @Override
    public final void updateFab(@UpdateFabFlag int updateTypes) {
        boolean isHidingButtons = (updateTypes == BUTTONS_SHRINK_AND_EXPAND || updateTypes == BUTTONS_IMMEDIATE);

        if (mFabContainer != null && (isTabSelected() || isHidingButtons)) {
            mFabContainer.updateFab(updateTypes);
        }
    }

    /**
     * @return {@code true} if the currently selected tab displays this fragment
     */
    public final boolean isTabSelected() {
        return mUiDataModel.getSelectedTab() == mTab;
    }

    /**
     * Select the tab that displays this fragment.
     */
    public final void selectTab() {
        mUiDataModel.setSelectedTab(mTab);
    }

    protected final DataModel getDataModel() {
        return mDataModel;
    }

    protected final UiDataModel getUiDataModel() {
        return mUiDataModel;
    }

    protected final SharedPreferences getPrefs() {
        return mPrefs;
    }

    protected final DisplayMetrics getDisplayMetrics() {
        return mDisplayMetrics;
    }

    protected final Locale getLocale() {
        return mLocale;
    }

    protected final Typeface getGeneralTypeface() {
        return mGeneralTypeface;
    }

    protected final Typeface getGeneralBoldTypeface() {
        return mGeneralBoldTypeface;
    }

    protected final boolean isTablet() {
        return mIsTablet;
    }

    protected final boolean isPortrait() {
        return mIsPortrait;
    }

    protected final boolean isLandscape() {
        return mIsLandscape;
    }

    protected final boolean isRtl() {
        return mIsRtl;
    }

    protected final boolean isVibrationsEnabled() {
        return mIsVibrationEnabled;
    }

    protected UiConfig.Fonts getFontsConfig() {
        return new UiConfig.Fonts(
            mGeneralTypeface,
            mGeneralBoldTypeface,
            null,
            null,
            null,
            null
        );
    }

    protected UiConfig.Screen getScreenConfig() {
        return new UiConfig.Screen(
            mDisplayMetrics,
            mIsTablet,
            mIsPortrait,
            mIsLandscape,
            mIsRtl
        );
    }

    protected UiConfig.CardStyle getCardStyleConfig() {
        return new UiConfig.CardStyle(
            mIsCardBackgroundDisplayed,
            mIsCardBorderDisplayed,
            mIsAmoled
        );
    }

    protected UiConfig.Haptics getHapticsConfig() {
        return new UiConfig.Haptics(mIsVibrationEnabled);
    }

}
