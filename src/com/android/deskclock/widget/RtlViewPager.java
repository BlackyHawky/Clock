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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

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
                // Do nothing
            }

            @Override
            public void onPageSelected(int position) {
                if (mListener != null) {
                    mListener.onPageSelected(getRtlAwareIndex(position));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Do nothing
            }
        });
    }

    @Override
    public int getCurrentItem() {
        return getRtlAwareIndex(super.getCurrentItem());
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(getRtlAwareIndex(item));
    }

    @Override
    public void setOnPageChangeListener(OnPageChangeListener unused) {
        throw new UnsupportedOperationException("Use setOnRTLPageChangeListener instead");
    }

    /**
     * Get a "RTL friendly" index. If the locale is LTR, the index is returned as is.
     * Otherwise it's transformed so view pager can render views using the new index for RTL. For
     * example, the second view will be rendered to the left of first view.
     *
     * @param index The logical index.
     */
    public int getRtlAwareIndex(int index) {
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                View.LAYOUT_DIRECTION_RTL) {
            return getAdapter().getCount() - index - 1;
        }
        return index;
    }

    /**
     * Sets a {@link OnPageChangeListener}. The listener will be called when a page is selected.
     */
    public void setOnRTLPageChangeListener(OnPageChangeListener listener) {
        mListener = listener;
    }
}
