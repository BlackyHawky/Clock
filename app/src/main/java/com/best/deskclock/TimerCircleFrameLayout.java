/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * A container that frames a timer circle of some sort. The circle is allowed to grow naturally
 * according to its layout constraints up to the allowable size.
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
     * @param widthMeasureSpec  horizontal space requirements as imposed by the parent
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
        final int maxSize = Utils.toPixel(240, getContext());
        final int size = Math.min(smallestDimension, maxSize);

        // Set the size of this container.
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(size + paddingLeft + paddingRight,
                MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(size + paddingTop + paddingBottom,
                MeasureSpec.EXACTLY);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
