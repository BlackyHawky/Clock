/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v13.app.FragmentCompat;
import android.support.v4.view.PagerAdapter;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;

import com.android.deskclock.uidata.UiDataModel;

import java.util.Map;

/**
 * This adapter produces the DeskClockFragments that are the content of the DeskClock tabs. The
 * adapter presents the tabs in LTR and RTL order depending on the text layout direction for the
 * current locale. To prevent issues when switching between LTR and RTL, fragments are registered
 * with the manager using position-independent tags, which is an important departure from
 * FragmentPagerAdapter.
 */
final class FragmentTabPagerAdapter extends PagerAdapter {

    private final DeskClock mDeskClock;

    /** The manager into which fragments are added. */
    private final FragmentManager mFragmentManager;

    /** A fragment cache that can be accessed before {@link #instantiateItem} is called. */
    private final Map<UiDataModel.Tab, DeskClockFragment> mFragmentCache;

    /** The active fragment transaction if one exists. */
    private FragmentTransaction mCurrentTransaction;

    /** The current fragment displayed to the user. */
    private Fragment mCurrentPrimaryItem;

    FragmentTabPagerAdapter(DeskClock deskClock) {
        mDeskClock = deskClock;
        mFragmentCache = new ArrayMap<>(getCount());
        mFragmentManager = deskClock.getFragmentManager();
    }

    @Override
    public int getCount() {
        return UiDataModel.getUiDataModel().getTabCount();
    }

    /**
     * @param position the left-to-right index of the fragment to be returned
     * @return the fragment displayed at the given {@code position}
     */
    DeskClockFragment getDeskClockFragment(int position) {
        // Fetch the tab the UiDataModel reports for the position.
        final UiDataModel.Tab tab = UiDataModel.getUiDataModel().getTabAt(position);

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
        mFragmentCache.put(tab, fragment);
        return fragment;
    }

    @Override
    public void startUpdate(ViewGroup container) {
        if (container.getId() == View.NO_ID) {
            throw new IllegalStateException("ViewPager with adapter " + this + " has no id");
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (mCurrentTransaction == null) {
            mCurrentTransaction = mFragmentManager.beginTransaction();
        }

        // Use the fragment located in the fragment manager if one exists.
        final UiDataModel.Tab tab = UiDataModel.getUiDataModel().getTabAt(position);
        Fragment fragment = mFragmentManager.findFragmentByTag(tab.name());
        if (fragment != null) {
            mCurrentTransaction.attach(fragment);
        } else {
            fragment = getDeskClockFragment(position);
            mCurrentTransaction.add(container.getId(), fragment, tab.name());
        }

        if (fragment != mCurrentPrimaryItem) {
            FragmentCompat.setMenuVisibility(fragment, false);
            FragmentCompat.setUserVisibleHint(fragment, false);
        }

        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (mCurrentTransaction == null) {
            mCurrentTransaction = mFragmentManager.beginTransaction();
        }
        final DeskClockFragment fragment = (DeskClockFragment) object;
        fragment.setFabContainer(null);
        mCurrentTransaction.detach(fragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        final Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                FragmentCompat.setMenuVisibility(mCurrentPrimaryItem, false);
                FragmentCompat.setUserVisibleHint(mCurrentPrimaryItem, false);
            }
            if (fragment != null) {
                FragmentCompat.setMenuVisibility(fragment, true);
                FragmentCompat.setUserVisibleHint(fragment, true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurrentTransaction != null) {
            mCurrentTransaction.commitAllowingStateLoss();
            mCurrentTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment) object).getView() == view;
    }
}