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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * A container that frames a timer circle of some sort. The circle is allowed to grow naturally
 * according to its layout constraints up to the {@link R.dimen#max_timer_circle_size largest}
 * allowable size.
 */
public class TimerCircleFrameLayout extends FrameLayout {

    public TimerCircleFrameLayout(Context context) {
        super(context);
    }

    public TimerCircleFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimerCircleFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Note: this method assumes the parent container will specify {@link MeasureSpec#EXACTLY exact}
     * width and height values.
     *
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent
     * @param heightMeasureSpec vertical space requirements as imposed by the parent
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();

        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        // Fetch the exact sizes imposed by the parent container.
        final int width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight;
        final int height = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom;
        final int smallestDimension = Math.min(width, height);

        // Fetch the absolute maximum circle size allowed.
        final int maxSize = getResources().getDimensionPixelSize(R.dimen.max_timer_circle_size);
        final int size = Math.min(smallestDimension, maxSize);

        // Set the size of this container.
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(size + paddingLeft + paddingRight,
                MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(size + paddingTop + paddingBottom,
                MeasureSpec.EXACTLY);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
