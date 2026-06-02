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
import com.google.android.material.progressindicator.CircularProgressIndicator;

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
        CircularProgressIndicator progressIndicator = findViewById(R.id.circular_progress_indicator);
        View resetOrEditButton = findViewById(R.id.reset_or_edit_button);

        final int frameWidth = getMeasuredWidth();
        final int frameHeight = getMeasuredHeight();
        final int minBound = Math.min(frameWidth, frameHeight);

        if (minBound <= 0) {
            return;
        }

        final int circleDiam = (int) (minBound - mDiamOffset);

        if (progressIndicator != null) {
            if (progressIndicator.getIndicatorSize() != circleDiam) {
                progressIndicator.setIndicatorSize(circleDiam);
            }
        }

        if (resetOrEditButton != null) {
            final MarginLayoutParams params = (MarginLayoutParams) resetOrEditButton.getLayoutParams();
            params.bottomMargin = circleDiam / 8;
            if (minBound == frameWidth) {
                params.bottomMargin += (frameHeight - frameWidth) / 2;
            }

            resetOrEditButton.setLayoutParams(params);
        }

    }
}
