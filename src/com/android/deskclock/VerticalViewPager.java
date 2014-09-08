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

package com.android.deskclock;

import android.content.Context;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

public class VerticalViewPager extends ViewPager {
    // TODO Remove the hack of using a parent view pager
    private ViewPager mParentViewPager;
    private float mLastMotionX;
    private float mLastMotionY;
    private float mTouchSlop;
    private boolean mVerticalDrag;
    private boolean mHorizontalDrag;

    // Vertical transit page transformer
    private final ViewPager.PageTransformer mPageTransformer = new ViewPager.PageTransformer() {
        @Override
        public void transformPage(View view, float position) {
            final int pageWidth = view.getWidth();
            final int pageHeight = view.getHeight();
            if (position < -1) {
                // This page is way off-screen to the left.
                view.setAlpha(0);
            } else if (position <= 1) {
                view.setAlpha(1);
                // Counteract the default slide transition
                view.setTranslationX(pageWidth * -position);
                // set Y position to swipe in from top
                float yPosition = position * pageHeight;
                view.setTranslationY(yPosition);
            } else {
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    };

    public VerticalViewPager(Context context) {
        super(context, null);
    }

    public VerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        init();
    }

    private void init() {
        // Make page transit vertical
        setPageTransformer(true, mPageTransformer);
        // Get rid of the overscroll drawing that happens on the left and right (the ripple)
        setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            initializeParent();
            final float x = ev.getX();
            final float y = ev.getY();
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    mLastMotionX = x;
                    mLastMotionY = y;
                    if (!mParentViewPager.onTouchEvent(ev))
                        return false;
                    return verticalDrag(ev);
                }
                case MotionEvent.ACTION_MOVE: {
                    final float xDiff = Math.abs(x - mLastMotionX);
                    final float yDiff = Math.abs(y - mLastMotionY);
                    if (!mHorizontalDrag && !mVerticalDrag) {
                        if (xDiff > mTouchSlop && xDiff > yDiff) { // Swiping left and right
                            mHorizontalDrag = true;
                        } else if (yDiff > mTouchSlop && yDiff > xDiff) { //Swiping up and down
                            mVerticalDrag = true;
                        }
                    }
                    if (mHorizontalDrag) {
                        return mParentViewPager.onTouchEvent(ev);
                    } else if (mVerticalDrag) {
                        return verticalDrag(ev);
                    }
                }
                case MotionEvent.ACTION_UP: {
                    if (mHorizontalDrag) {
                        mHorizontalDrag = false;
                        return mParentViewPager.onTouchEvent(ev);
                    }
                    if (mVerticalDrag) {
                        mVerticalDrag = false;
                        return verticalDrag(ev);
                    }
                }
            }
            // Set both flags to false in case user lifted finger in the parent view pager
            mHorizontalDrag = false;
            mVerticalDrag = false;
        } catch (Exception e) {
            // The mParentViewPager shouldn't be null, but just in case. If this happens,
            // app should not crash, instead just ignore the user swipe input
            // TODO: handle the exception gracefully
        }
        return false;
    }

    private void initializeParent() {
        if (mParentViewPager == null) {
            // This vertical view pager is nested in the frame layout inside the timer tab
            // (fragment), which is nested inside the horizontal view pager. Therefore,
            // it needs 3 layers to get all the way to the horizontal view pager.
            final ViewParent parent = getParent().getParent().getParent();
            if (parent instanceof ViewPager) {
                mParentViewPager = (ViewPager) parent;
            }
        }
    }

    private boolean verticalDrag(MotionEvent ev) {
        final float x = ev.getX();
        final float y = ev.getY();
        ev.setLocation(y, x);
        return super.onTouchEvent(ev);
    }
}
