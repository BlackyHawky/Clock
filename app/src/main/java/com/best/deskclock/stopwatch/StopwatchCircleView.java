/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.stopwatch;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Lap;
import com.best.deskclock.data.Stopwatch;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.List;

/**
 * Custom view that draws a reference lap as a circle when one exists.
 */
public final class StopwatchCircleView extends View {

    /**
     * An amount to subtract from the true radius to account for drawing thicknesses.
     */
    private final float mRadiusOffset;

    /**
     * Used to scale the width of the marker to make it similarly visible on all screens.
     */
    private final float mScreenDensity;

    /**
     * The color indicating the remaining portion of the current lap.
     */
    private final int mRemainderColor;

    /**
     * The color indicating the completed portion of the lap.
     */
    private final int mCompletedColor;

    /**
     * The size of the stroke that paints the lap circle.
     */
    private final float mStrokeSize;

    /**
     * The size of the stroke that paints the marker for the end of the prior lap.
     */
    private final float mMarkerStrokeSize;

    private final Paint mPaint = new Paint();
    private final RectF mArcRect = new RectF();

    public StopwatchCircleView(Context context) {
        this(context, null);
    }

    public StopwatchCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources resources = context.getResources();
        final float dotDiameter = Utils.toPixel(12, context);

        mScreenDensity = resources.getDisplayMetrics().density;
        mStrokeSize = Utils.toPixel(6, context);
        mMarkerStrokeSize = Utils.toPixel(12, context);
        mRadiusOffset = Utils.calculateRadiusOffset(mStrokeSize, dotDiameter, mMarkerStrokeSize);

        mRemainderColor = MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK);
        mCompletedColor = MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK);

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(mCompletedColor);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Start the animation if it is not currently running.
     */
    void update() {
        postInvalidateOnAnimation();
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        // Compute the size and location of the circle to be drawn.
        final int xCenter = getWidth() / 2;
        final int yCenter = getHeight() / 2;
        final float radius = Math.min(xCenter, yCenter) - mRadiusOffset;

        // Reset old painting state.
        mPaint.setColor(mRemainderColor);
        mPaint.setStrokeWidth(mStrokeSize);

        final List<Lap> laps = getLaps();

        // If a reference lap does not exist or should not be drawn, draw a simple white circle.
        if (laps.isEmpty() || !DataModel.getDataModel().canAddMoreLaps()) {
            // Draw a complete white circle; no red arc required.
            canvas.drawCircle(xCenter, yCenter, radius, mPaint);

            // No need to continue animating the plain white circle.
            return;
        }

        // The first lap is the reference lap to which all future laps are compared.
        final Stopwatch stopwatch = getStopwatch();
        final int lapCount = laps.size();
        final Lap firstLap = laps.get(lapCount - 1);
        final Lap priorLap = laps.get(0);
        final long firstLapTime = firstLap.getLapTime();
        final long currentLapTime = stopwatch.getTotalTime() - priorLap.getAccumulatedTime();

        // Draw a combination of red and white arcs to create a circle.
        mArcRect.top = yCenter - radius;
        mArcRect.bottom = yCenter + radius;
        mArcRect.left = xCenter - radius;
        mArcRect.right = xCenter + radius;
        final float redPercent = (float) currentLapTime / (float) firstLapTime;
        final float whitePercent = 1 - (redPercent > 1 ? 1 : redPercent);

        // Draw a white arc to indicate the amount of reference lap that remains.
        canvas.drawArc(mArcRect, 270 + (1 - whitePercent) * 360, whitePercent * 360, false, mPaint);

        // Draw a red arc to indicate the amount of reference lap completed.
        mPaint.setColor(mCompletedColor);
        canvas.drawArc(mArcRect, 270, redPercent * 360, false, mPaint);

        // Starting on lap 2, a marker can be drawn indicating where the prior lap ended.
        if (lapCount > 1) {
            mPaint.setColor(mRemainderColor);
            mPaint.setStrokeWidth(mMarkerStrokeSize);
            final float markerAngle = (float) priorLap.getLapTime() / (float) firstLapTime * 360;
            final float startAngle = 270 + markerAngle;
            final float sweepAngle = mScreenDensity * (float) (360 / (radius * Math.PI));
            canvas.drawArc(mArcRect, startAngle, sweepAngle, false, mPaint);
        }

        // If the stopwatch is not running it does not require continuous updates.
        if (stopwatch.isRunning()) {
            postInvalidateOnAnimation();
        }
    }

    private Stopwatch getStopwatch() {
        return DataModel.getDataModel().getStopwatch();
    }

    private List<Lap> getLaps() {
        return DataModel.getDataModel().getLaps();
    }
}
