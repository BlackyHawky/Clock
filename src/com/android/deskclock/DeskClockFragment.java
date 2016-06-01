/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock;

import android.app.Fragment;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.widget.ImageButton;

import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.uidata.UiDataModel.Tab;

import static com.android.deskclock.FabContainer.UpdateType.FAB_AND_BUTTONS_IMMEDIATE;

public abstract class DeskClockFragment extends Fragment implements FabContainer, FabController {

    /** The tab associated with this fragment. */
    private final Tab mTab;

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

    @Override
    public void onLeftButtonClick(@NonNull ImageButton left) {
        // Do nothing here, only in derived classes
    }

    @Override
    public void onRightButtonClick(@NonNull ImageButton right) {
        // Do nothing here, only in derived classes
    }

    /**
     * @param color the newly installed app window color
     */
    protected void onAppColorChanged(@ColorInt int color) {
        // Do nothing here, only in derived classes
    }

    /**
     * Requests that the parent activity update the fab and buttons.
     *
     * @param updateType the manner in which the fab container should be updated
     */
    @Override
    public final void updateFab(FabContainer.UpdateType updateType) {
        final FabContainer parentFabContainer = (FabContainer) getActivity();
        if (parentFabContainer != null) {
            parentFabContainer.updateFab(updateType);
        }
    }

    @Override
    public void onMorphFabButtons(@NonNull ImageButton left, @NonNull ImageButton right) {
        // Pass through to onUpdateFabButtons because there is no spec for morphing button icon.
        onUpdateFabButtons(left, right);
    }

    /**
     * @return {@code true} iff the currently selected tab displays this fragment
     */
    public final boolean isTabSelected() {
        return UiDataModel.getUiDataModel().getSelectedTab() == mTab;
    }

    /**
     * Updates the scrolling state in the {@link UiDataModel} for this tab.
     *
     * @param scrolledToTop {@code true} iff the vertical scroll position of this tab is at the top
     */
    public final void setTabScrolledToTop(boolean scrolledToTop) {
        UiDataModel.getUiDataModel().setTabScrolledToTop(mTab, scrolledToTop);
    }
}