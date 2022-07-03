/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.best.deskclock.timer;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.legacy.app.FragmentCompat;
import androidx.viewpager.widget.PagerAdapter;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerListener;

import java.util.List;
import java.util.Map;

/**
 * This adapter produces a {@link TimerItemFragment} for each timer.
 */
class TimerPagerAdapter extends PagerAdapter implements TimerListener {

    private final FragmentManager mFragmentManager;

    /**
     * Maps each timer id to the corresponding {@link TimerItemFragment} that draws it.
     */
    private final Map<Integer, TimerItemFragment> mFragments = new ArrayMap<>();

    /**
     * The current fragment transaction in play or {@code null}.
     */
    private FragmentTransaction mCurrentTransaction;

    /**
     * The {@link TimerItemFragment} that is current visible on screen.
     */
    private Fragment mCurrentPrimaryItem;

    public TimerPagerAdapter(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    private static void setItemVisible(Fragment item, boolean visible) {
        FragmentCompat.setMenuVisibility(item, visible);
        FragmentCompat.setUserVisibleHint(item, visible);
    }

    @Override
    public int getCount() {
        return getTimers().size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return ((Fragment) object).getView() == view;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        final TimerItemFragment fragment = (TimerItemFragment) object;
        final Timer timer = fragment.getTimer();

        final int position = getTimers().indexOf(timer);
        return position == -1 ? POSITION_NONE : position;
    }

    @NonNull
    @Override
    @SuppressLint("CommitTransaction")
    public Fragment instantiateItem(@NonNull ViewGroup container, int position) {
        if (mCurrentTransaction == null) {
            mCurrentTransaction = mFragmentManager.beginTransaction();
        }

        final Timer timer = getTimers().get(position);

        // Search for the existing fragment by tag.
        final String tag = getClass().getSimpleName() + timer.getId();
        TimerItemFragment fragment = (TimerItemFragment) mFragmentManager.findFragmentByTag(tag);

        if (fragment != null) {
            // Reattach the existing fragment.
            mCurrentTransaction.attach(fragment);
        } else {
            // Create and add a new fragment.
            fragment = TimerItemFragment.newInstance(timer);
            mCurrentTransaction.add(container.getId(), fragment, tag);
        }

        if (fragment != mCurrentPrimaryItem) {
            setItemVisible(fragment, false);
        }

        mFragments.put(timer.getId(), fragment);

        return fragment;
    }

    @Override
    @SuppressLint("CommitTransaction")
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        final TimerItemFragment fragment = (TimerItemFragment) object;

        if (mCurrentTransaction == null) {
            mCurrentTransaction = mFragmentManager.beginTransaction();
        }

        mFragments.remove(fragment.getTimerId());
        mCurrentTransaction.remove(fragment);
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        final Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                setItemVisible(mCurrentPrimaryItem, false);
            }

            mCurrentPrimaryItem = fragment;

            if (mCurrentPrimaryItem != null) {
                setItemVisible(mCurrentPrimaryItem, true);
            }
        }
    }

    @Override
    public void finishUpdate(@NonNull ViewGroup container) {
        if (mCurrentTransaction != null) {
            mCurrentTransaction.commitAllowingStateLoss();
            mCurrentTransaction = null;

            if (!mFragmentManager.isDestroyed()) {
                mFragmentManager.executePendingTransactions();
            }
        }
    }

    @Override
    public void timerAdded(Timer timer) {
        notifyDataSetChanged();
    }

    @Override
    public void timerRemoved(Timer timer) {
        notifyDataSetChanged();
    }

    @Override
    public void timerUpdated(Timer before, Timer after) {
        final TimerItemFragment timerItemFragment = mFragments.get(after.getId());
        if (timerItemFragment != null) {
            timerItemFragment.updateTime();
        }
    }

    /**
     * @return {@code true} if at least one timer is in a state requiring continuous updates
     */
    boolean updateTime() {
        boolean continuousUpdates = false;
        for (TimerItemFragment fragment : mFragments.values()) {
            continuousUpdates |= fragment.updateTime();
        }
        return continuousUpdates;
    }

    Timer getTimer(int index) {
        return getTimers().get(index);
    }

    private List<Timer> getTimers() {
        return DataModel.getDataModel().getTimers();
    }
}
