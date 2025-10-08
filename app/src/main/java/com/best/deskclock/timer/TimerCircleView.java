/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.utils.ThemeUtils;

import com.google.android.material.color.MaterialColors;

/**
 * Custom view that draws timer progress as a circle.
 */
public final class TimerCircleView extends View {

    private static final float NEAR_COMPLETE_THRESHOLD = 0.99f;

    private final Paint mCompletedPaint = new Paint();
    private final Paint mRemainingPaint = new Paint();

    /**
     * An amount to subtract from the true radius to account for drawing thicknesses.
     */
    private final float mRadiusOffset;

    private final RectF mArcRect = new RectF();

    private Timer mTimer;

    public TimerCircleView(Context context) {
        this(context, null);
    }

    public TimerCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final float dotDiameter = ThemeUtils.convertDpToPixels(10, context);

        final float mStrokeSize = ThemeUtils.convertDpToPixels(isSingleTimer() ? 8 : 6, context);
        mRadiusOffset = ThemeUtils.calculateRadiusOffset(mStrokeSize, dotDiameter, 0);

        final int remainingArcColor = MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK);
        final int completedArcColor = MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK);

        mCompletedPaint.setAntiAlias(true);
        mCompletedPaint.setStyle(Paint.Style.STROKE);
        mCompletedPaint.setStrokeCap(Paint.Cap.ROUND);
        mCompletedPaint.setStrokeWidth(mStrokeSize);
        mCompletedPaint.setColor(completedArcColor);

        mRemainingPaint.setAntiAlias(true);
        mRemainingPaint.setStyle(Paint.Style.STROKE);
        mRemainingPaint.setStrokeCap(Paint.Cap.ROUND);
        mRemainingPaint.setStrokeWidth(mStrokeSize);
        mRemainingPaint.setColor(remainingArcColor);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        if (mTimer == null) {
            return;
        }

        // Compute the size and location of the circle to be drawn.
        final int xCenter = getWidth() / 2;
        final int yCenter = getHeight() / 2;
        final float radius = Math.min(xCenter, yCenter) - mRadiusOffset;

        float gapSize = ThemeUtils.convertDpToPixels(isSingleTimer() ? 14 : 12, getContext());
        float gapAngle = (float) Math.toDegrees(gapSize / radius);

        mArcRect.set(
                xCenter - radius,
                yCenter - radius,
                xCenter + radius,
                yCenter + radius
        );

        if (mTimer.isReset()) {
            canvas.drawCircle(xCenter, yCenter, radius, mRemainingPaint);
            return;
        }

        if (mTimer.isExpired()) {
            canvas.drawCircle(xCenter, yCenter, radius, mCompletedPaint);
            return;
        }

        final float totalLength = mTimer.getTotalLength();
        final float completedPercent = totalLength > 0
                ? Math.min(1f, (float) mTimer.getElapsedTime() / totalLength)
                : 0f;

        final float totalAngle = 360f - 2 * gapAngle;
        final float completedAngle = completedPercent * totalAngle;
        final float remainingAngle = (1f - completedPercent) * totalAngle;

        final float startCompletedAngle = 270f - gapAngle / 2f;
        final float startRemainingAngle = startCompletedAngle - completedAngle - gapAngle;

        // When the timer is very close to finishing (e.g. > 99.5%),
        // drawing the gap can cause small visual glitches (like a flickering remainder arc).
        // To keep the transition smooth, we draw a full 360° arc just before the timer expires.
        if (completedPercent > NEAR_COMPLETE_THRESHOLD) {
            canvas.drawArc(mArcRect, 0, 360, false, mCompletedPaint);
        } else {
            // Normal drawing with gap between completed arc and remaining arc
            canvas.drawArc(mArcRect, startCompletedAngle, -completedAngle, false, mCompletedPaint);
            canvas.drawArc(mArcRect, startRemainingAngle, -remainingAngle, false, mRemainingPaint);
        }

        // Only redraw continuously while the timer is running
        if (mTimer.isRunning()) {
            postInvalidateOnAnimation();
        }
    }

    void update(Timer timer) {
        if (mTimer != timer) {
            mTimer = timer;
            postInvalidateOnAnimation();
        }
    }

    private boolean isSingleTimer() {
        return DataModel.getDataModel().getTimers().size() == 1;
    }

}
