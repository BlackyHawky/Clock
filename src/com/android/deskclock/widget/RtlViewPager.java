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

package com.android.deskclock.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

import com.android.deskclock.data.DataModel;
import com.android.deskclock.uidata.UiDataModel;

/**
 * A {@link ViewPager} that's aware of RTL changes when used with FragmentPagerAdapter.
 */
public final class RtlViewPager extends ViewPager {

    /**
     * Callback interface for responding to changing state of the selected page.
     * Positions supplied will always be the logical position in the adapter -
     * that is, the 0 index corresponds to the left-most page in LTR and the
     * right-most page in RTL.
     */
    private OnPageChangeListener mListener;

    public RtlViewPager(Context context) {
        this(context, null /* attrs */);
    }

    public RtlViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float offset, int offsetPixels) {
                if (mListener != null) {
                    position = UiDataModel.getUiDataModel().getTabLayoutIndex(position);
                    mListener.onPageScrolled(position, offset, offsetPixels);
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (mListener != null) {
                    position = UiDataModel.getUiDataModel().getTabLayoutIndex(position);
                    mListener.onPageSelected(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (mListener != null) {
                    mListener.onPageScrollStateChanged(state);
                }
            }
        });
    }

    @Override
    public int getCurrentItem() {
        return UiDataModel.getUiDataModel().getTabLayoutIndex(super.getCurrentItem());
    }

    @Override
    public void setCurrentItem(int item) {
        // Smooth-scroll to the new tab if the app is open; snap to the new tab if it is not.
        final boolean smoothScrolling = DataModel.getDataModel().isApplicationInForeground();
        setCurrentItem(item, smoothScrolling);
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        // Convert the item (which assumes LTR) into the correct index relative to layout direction.
        final int index = UiDataModel.getUiDataModel().getTabLayoutIndex(item);
        super.setCurrentItem(index, smoothScroll);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setOnPageChangeListener(OnPageChangeListener unused) {
        throw new UnsupportedOperationException("Use setOnRTLPageChangeListener instead");
    }

    /**
     * Sets a {@link OnPageChangeListener}. The listener will be called when a page is selected.
     */
    public void setOnRTLPageChangeListener(OnPageChangeListener listener) {
        mListener = listener;
    }
}
