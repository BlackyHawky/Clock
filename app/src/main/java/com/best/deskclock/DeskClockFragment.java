/*
 * Copyright (C) 2012 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import android.view.KeyEvent;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.uidata.UiDataModel.Tab;

public abstract class DeskClockFragment extends Fragment implements FabContainer, FabController {

    /**
     * The tab associated with this fragment.
     */
    private final Tab mTab;

    /**
     * The container that houses the fab and its left and right buttons.
     */
    private FabContainer mFabContainer;

    public DeskClockFragment(Tab tab) {
        mTab = tab;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update the fab and buttons in case their state changed while the fragment was paused.
        if (isTabSelected()) {
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // By default return false so event continues to propagate
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // By default return false so event continues to propagate
        return false;
    }

    /**
     * Called before onUpdateFab when the fab should be animated.
     * @param fab the fab component to be configured based on current state
     */
    public void onMorphFab(@NonNull ImageView fab) {
    }

    /**
     * @param fabContainer the container that houses the fab and its left and right buttons
     */
    public final void setFabContainer(FabContainer fabContainer) {
        mFabContainer = fabContainer;
    }

    /**
     * Requests that the parent activity update the fab and buttons.
     *
     * @param updateTypes the manner in which the fab container should be updated
     */
    @Override
    public final void updateFab(@UpdateFabFlag int updateTypes) {
        if (mFabContainer != null) {
            mFabContainer.updateFab(updateTypes);
        }
    }

    /**
     * @return {@code true} if the currently selected tab displays this fragment
     */
    public final boolean isTabSelected() {
        return UiDataModel.getUiDataModel().getSelectedTab() == mTab;
    }

    /**
     * Select the tab that displays this fragment.
     */
    public final void selectTab() {
        UiDataModel.getUiDataModel().setSelectedTab(mTab);
    }

}
