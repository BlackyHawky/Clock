/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.best.deskclock.R;

/**
 * This class adjusts the location of the reset button.
 */
public class CircleButtonsLayout extends FrameLayout {

    private final float mDiamOffset;

    @SuppressWarnings("unused")
    public CircleButtonsLayout(Context context) {
        this(context, null);
    }

    public CircleButtonsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        final float strokeSize = dpToPx(6, getResources().getDisplayMetrics());
        mDiamOffset = strokeSize * 2;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We must call onMeasure both before and after re-measuring our views because the circle
        // may not always be drawn here yet. The first onMeasure will force the circle to be drawn,
        // and the second will force our re-measurements to take effect.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        remeasureViews();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void remeasureViews() {
        View circleView = findViewById(R.id.timer_circle_view);
        View resetAddButton = findViewById(R.id.reset_button);

        final int frameWidth = circleView.getMeasuredWidth();
        final int frameHeight = circleView.getMeasuredHeight();
        final int minBound = Math.min(frameWidth, frameHeight);
        final int circleDiam = (int) (minBound - mDiamOffset);

        if (resetAddButton != null) {
            final MarginLayoutParams resetParams = (MarginLayoutParams) resetAddButton.getLayoutParams();
            resetParams.bottomMargin = circleDiam / 8;
            if (minBound == frameWidth) {
                resetParams.bottomMargin += (frameHeight - frameWidth) / 2;
            }
        }

    }
}
