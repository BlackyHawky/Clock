/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.deskclock.timer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v13.app.FragmentCompat;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

/**
 * Forked from support lib's {@link FragmentStatePagerAdapter}, with some minor
 * changes that couldn't be accomplished through subclassing: we need to override the
 * onDataSetChanged method using the private member mFragments which cannot be accessed outside.
 *
 * This class is used for TimerFragment's vertical view pager only. It removed the save/restore
 * functionality, because all the fragments needs to be destroyed whenever TimerFragment's
 * onPause, in order to bind the newly created timer view with the new pager fragment.
 */
public abstract class FragmentStatePagerAdapter2 extends PagerAdapter {

    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;

    private SparseArrayCompat<Fragment> mFragments = new SparseArrayCompat<Fragment>();
    private Fragment mCurrentPrimaryItem = null;


    public FragmentStatePagerAdapter2(FragmentManager fm) {
        mFragmentManager = fm;
    }

    /**
     * Return the Fragment associated with a specified position.
     */
    public abstract Fragment getItem(int position);

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // If we already have this item instantiated, there is nothing
        // to do.  This can happen when we are restoring the entire pager
        // from its saved state, where the fragment manager has already
        // taken care of restoring the fragments we previously had instantiated.
        final Fragment existing = mFragments.get(position);
        if (existing != null) {
            return existing;
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        Fragment fragment = getItem(position);
        if (fragment != mCurrentPrimaryItem) {
            setItemVisible(fragment, false);
        }
        mFragments.put(position, fragment);
        mCurTransaction.add(container.getId(), fragment);

        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        mFragments.delete(position);

        mCurTransaction.remove(fragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                setItemVisible(mCurrentPrimaryItem, false);
            }
            if (fragment != null) {
                setItemVisible(fragment, true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment) object).getView() == view;
    }

    public void setItemVisible(Fragment item, boolean visible) {
        FragmentCompat.setMenuVisibility(item, visible);
        FragmentCompat.setUserVisibleHint(item, visible);
    }

    @Override
    public void notifyDataSetChanged() {
        // update positions in mFragments
        SparseArrayCompat<Fragment> newFragments =
                new SparseArrayCompat<Fragment>(mFragments.size());
        for (int i = 0; i < mFragments.size(); i++) {
            final int oldPos = mFragments.keyAt(i);
            final Fragment f = mFragments.valueAt(i);
            final int newPos = getItemPosition(f);

            if (newPos != POSITION_NONE) {
                final int pos = (newPos >= 0) ? newPos : oldPos;
                newFragments.put(pos, f);
            }
        }
        mFragments = newFragments;

        super.notifyDataSetChanged();
    }

    public Fragment getFragmentAt(int position) {
        return mFragments.valueAt(position);
    }

}
