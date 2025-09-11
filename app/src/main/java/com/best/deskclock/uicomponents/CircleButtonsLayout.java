/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.best.deskclock.R;
import com.best.deskclock.utils.ThemeUtils;

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

        final float strokeSize = ThemeUtils.convertDpToPixels(6, context);
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
        View mCircleView = findViewById(R.id.timer_time);
        View mResetAddButton = findViewById(R.id.reset);

        final int frameWidth = mCircleView.getMeasuredWidth();
        final int frameHeight = mCircleView.getMeasuredHeight();
        final int minBound = Math.min(frameWidth, frameHeight);
        final int circleDiam = (int) (minBound - mDiamOffset);

        if (mResetAddButton != null) {
            final MarginLayoutParams resetParams = (MarginLayoutParams) mResetAddButton
                    .getLayoutParams();
            resetParams.bottomMargin = circleDiam / 8;
            if (minBound == frameWidth) {
                resetParams.bottomMargin += (frameHeight - frameWidth) / 2;
            }
        }

    }
}