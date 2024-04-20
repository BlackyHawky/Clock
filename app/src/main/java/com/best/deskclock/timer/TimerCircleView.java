/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.Timer;

/**
 * Custom view that draws timer progress as a circle.
 */
public final class TimerCircleView extends View {

    /**
     * An amount to subtract from the true radius to account for drawing thicknesses.
     */
    private final float mRadiusOffset;

    /**
     * The color indicating the remaining portion of the timer.
     */
    private final int mRemainderColor;

    /**
     * The color indicating the completed portion of the timer.
     */
    private final int mCompletedColor;

    /**
     * The size of the stroke that paints the timer circle.
     */
    private final float mStrokeSize;

    private final Paint mPaint = new Paint();
    private final RectF mArcRect = new RectF();

    private Timer mTimer;

    public TimerCircleView(Context context) {
        this(context, null);
    }

    public TimerCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final float dotDiameter = Utils.toPixel(10, context);

        mStrokeSize = Utils.toPixel(6, context);
        mRadiusOffset = Utils.calculateRadiusOffset(mStrokeSize, dotDiameter, 0);

        mRemainderColor = context.getColor(R.color.md_theme_onSurfaceVariant);
        mCompletedColor = context.getColor(R.color.md_theme_inversePrimary);

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(mCompletedColor);
        paint.setStyle(Paint.Style.FILL);
    }

    void update(Timer timer) {
        if (mTimer != timer) {
            mTimer = timer;
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mTimer == null) {
            return;
        }

        // Compute the size and location of the circle to be drawn.
        final int xCenter = getWidth() / 2;
        final int yCenter = getHeight() / 2;
        final float radius = Math.min(xCenter, yCenter) - mRadiusOffset;

        // Reset old painting state.
        mPaint.setColor(mRemainderColor);
        mPaint.setStrokeWidth(mStrokeSize);

        // If the timer is reset, draw a simple white circle.
        final float redPercent;
        if (mTimer.isReset()) {
            // Draw a complete white circle; no red arc required.
            canvas.drawCircle(xCenter, yCenter, radius, mPaint);
        } else if (mTimer.isExpired()) {
            mPaint.setColor(mCompletedColor);

            // Draw a complete white circle; no red arc required.
            canvas.drawCircle(xCenter, yCenter, radius, mPaint);
        } else {
            // Draw a combination of red and white arcs to create a circle.
            mArcRect.top = yCenter - radius;
            mArcRect.bottom = yCenter + radius;
            mArcRect.left = xCenter - radius;
            mArcRect.right = xCenter + radius;
            redPercent = Math.min(1, (float) mTimer.getElapsedTime() / (float) mTimer.getTotalLength());
            final float whitePercent = 1 - redPercent;

            // Draw a white arc to indicate the amount of timer that remains.
            canvas.drawArc(mArcRect, 270, whitePercent * 360, false, mPaint);

            // Draw a red arc to indicate the amount of timer completed.
            mPaint.setColor(mCompletedColor);
            canvas.drawArc(mArcRect, 270, -redPercent * 360, false, mPaint);
        }

        if (mTimer.isRunning()) {
            postInvalidateOnAnimation();
        }
    }
}
