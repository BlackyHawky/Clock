/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.stopwatch;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Lap;
import com.best.deskclock.utils.Utils;

import java.util.List;

/**
 * A container that frames a timer circle of some sort. The circle is allowed to grow naturally
 * according to its layout constraints up to the allowable size.
 */
public class StopwatchCircleFrameLayout extends FrameLayout {

    public StopwatchCircleFrameLayout(Context context) {
        super(context);
    }

    public StopwatchCircleFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StopwatchCircleFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
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
        final List<Lap> laps = DataModel.getDataModel().getLaps();
        final int maxSize;
        if (Utils.isTablet(getContext())) {
            if (laps.isEmpty()) {
                if (getResources().getDisplayMetrics().densityDpi <= 213) {
                    maxSize = Utils.toPixel(360, getContext());
                } else if (getResources().getDisplayMetrics().densityDpi <= 240) {
                    maxSize = Utils.toPixel(300, getContext());
                } else if (getResources().getDisplayMetrics().densityDpi <= 280) {
                    maxSize = Utils.toPixel(240, getContext());
                } else {
                    maxSize = Utils.toPixel(200, getContext());
                }
            } else {
                if (getResources().getDisplayMetrics().densityDpi <= 213) {
                    maxSize = Utils.toPixel(420, getContext());
                } else if (getResources().getDisplayMetrics().densityDpi <= 240) {
                    maxSize = Utils.toPixel(360, getContext());
                } else if (getResources().getDisplayMetrics().densityDpi <= 280) {
                    maxSize = Utils.toPixel(300, getContext());
                } else {
                    maxSize = Utils.toPixel(240, getContext());
                }
            }
        } else {
            maxSize = Utils.toPixel(240, getContext());
        }
        final int size = Math.min(smallestDimension, maxSize);

        // Set the size of this container.
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(size + paddingLeft + paddingRight,
                MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(size + paddingTop + paddingBottom,
                MeasureSpec.EXACTLY);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
