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
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.uidata.UiDataModel.Tab;

public abstract class DeskClockFragment extends Fragment {

    /** The tab associated with this fragment. */
    private final Tab mTab;

    protected ImageView mFab;
    protected ImageButton mLeftButton;
    protected ImageButton mRightButton;

    public DeskClockFragment(Tab tab) {
        mTab = tab;
    }

    public void onFabClick(View view) {
        // Do nothing here, only in derived classes
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final DeskClock deskClock = (DeskClock) getActivity();
        mFab = deskClock.getFab();
        mLeftButton = deskClock.getLeftButton();
        mRightButton = deskClock.getRightButton();
    }

    public void setFabAppearance() {
        // Do nothing here, only in derived classes
    }

    public void setLeftRightButtonAppearance() {
        // Do nothing here, only in derived classes
    }

    public void onLeftButtonClick(View view) {
        // Do nothing here, only in derived classes
    }

    public void onRightButtonClick(View view) {
        // Do nothing here, only in derived classes
    }

    public boolean isTabSelected() {
        return UiDataModel.getUiDataModel().getSelectedTab() == mTab;
    }
}