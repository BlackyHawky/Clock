/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright (C) 2020 The LineageOS Project
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

import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.deskclock.uidata.UiDataModel;

import java.util.Map;

/**
 * This class produces the DeskClockFragments that are the content of the DeskClock tabs.
 * It presents the tabs in LTR and RTL order depending on the text layout direction for the
 * current locale. To prevent issues when switching between LTR and RTL, fragments are registered
 * with the manager using position-independent tags, which is an important departure from
 * FragmentPagerAdapter.
 */
public final class FragmentUtils {

    private final DeskClock mDeskClock;

    /** The manager into which fragments are added. */
    private final FragmentManager mFragmentManager;

    /** A fragment cache that can be accessed before {@link #instantiateItem} is called. */
    private final Map<UiDataModel.Tab, DeskClockFragment> mFragmentCache;

    /** The current fragment displayed to the user. */
    private DeskClockFragment mCurrentPrimaryItem;

    FragmentUtils(DeskClock deskClock) {
        mDeskClock = deskClock;
        mFragmentCache = new ArrayMap<>(getCount());
        mFragmentManager = deskClock.getSupportFragmentManager();
    }

    private int getCount() {
        return UiDataModel.getUiDataModel().getTabCount();
    }

    public DeskClockFragment getDeskClockFragment(UiDataModel.Tab tab) {
        // First check the local cache for the fragment.
        DeskClockFragment fragment = mFragmentCache.get(tab);
        if (fragment != null) {
            return fragment;
        }

        // Next check the fragment manager; relevant when app is rebuilt after locale changes
        // because this adapter will be new and mFragmentCache will be empty, but the fragment
        // manager will retain the Fragments built on original application launch.
        fragment = (DeskClockFragment) mFragmentManager.findFragmentByTag(tab.name());
        if (fragment != null) {
            fragment.setFabContainer(mDeskClock);
            mFragmentCache.put(tab, fragment);
            return fragment;
        }

        // Otherwise, build the fragment from scratch.
        final String fragmentClassName = tab.getFragmentClassName();
        fragment = (DeskClockFragment) Fragment.instantiate(mDeskClock, fragmentClassName);
        fragment.setFabContainer(mDeskClock);

        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, fragment, tab.name());
        transaction.commit();

        mFragmentCache.put(tab, fragment);
        return fragment;
    }

    public void hideAllFragments() {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        for (UiDataModel.Tab tab : UiDataModel.Tab.values()) {
            Fragment fragment = mFragmentManager.findFragmentByTag(tab.name());
            if (fragment != null) {
                transaction.hide(fragment);
            }
        }

        transaction.commit();
    }

    public void showFragment(UiDataModel.Tab tab) {
        hideAllFragments();
        Fragment fragment = getDeskClockFragment(tab);
        mFragmentManager.beginTransaction().show(fragment).commit();
        mCurrentPrimaryItem = (DeskClockFragment) fragment;
    }

    public DeskClockFragment getCurrentFragment() {
        return mCurrentPrimaryItem;
    }
}