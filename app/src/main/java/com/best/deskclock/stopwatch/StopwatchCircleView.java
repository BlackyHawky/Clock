/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.stopwatch;

import android.content.Context;
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
import com.best.deskclock.utils.ThemeUtils;
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

    private final Paint mCompletedPaint = new Paint();
    private final Paint mRemainingPaint = new Paint();
    private final Paint mCapPaint = new Paint();
    private final Paint mMarkerPaint = new Paint();

    private final RectF mArcRect = new RectF();

    public StopwatchCircleView(Context context) {
        this(context, null);
    }

    public StopwatchCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final float dotDiameter = ThemeUtils.convertDpToPixels(12, context);
        int strokeSize = ThemeUtils.convertDpToPixels(8, context);
        int markerStrokeSize = ThemeUtils.convertDpToPixels(ThemeUtils.isTablet() ? 4 : 3, context);

        mRadiusOffset = ThemeUtils.calculateRadiusOffset(strokeSize, dotDiameter, markerStrokeSize);

        int remainingArcColor = MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK);
        int completedArcColor = MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK);

        mCompletedPaint.setAntiAlias(true);
        mCompletedPaint.setStyle(Paint.Style.STROKE);
        mCompletedPaint.setStrokeWidth(strokeSize);
        mCompletedPaint.setColor(completedArcColor);

        mCapPaint.setAntiAlias(true);
        mCapPaint.setStyle(Paint.Style.STROKE);
        mCapPaint.setStrokeCap(Paint.Cap.ROUND);
        mCapPaint.setStrokeWidth(strokeSize);
        mCapPaint.setColor(completedArcColor);

        mRemainingPaint.setAntiAlias(true);
        mRemainingPaint.setStyle(Paint.Style.STROKE);
        mRemainingPaint.setStrokeWidth(strokeSize);
        mRemainingPaint.setColor(remainingArcColor);

        mMarkerPaint.setAntiAlias(true);
        mMarkerPaint.setStyle(Paint.Style.STROKE);
        mMarkerPaint.setStrokeCap(Paint.Cap.ROUND);
        mMarkerPaint.setStrokeWidth(markerStrokeSize);
        mMarkerPaint.setColor(remainingArcColor);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        // Compute the size and location of the circle to be drawn.
        final int xCenter = getWidth() / 2;
        final int yCenter = getHeight() / 2;
        final float radius = Math.min(xCenter, yCenter) - mRadiusOffset;

        final List<Lap> laps = getLaps();

        // If a reference lap does not exist or should not be drawn, draw a simple circle.
        if (laps.isEmpty() || !DataModel.getDataModel().canAddMoreLaps()) {
            canvas.drawCircle(xCenter, yCenter, radius, mRemainingPaint);
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
        final float completedPercent = Math.min((float) currentLapTime / (float) firstLapTime, 1.0f);
        final float remainingPercent = 1 - (completedPercent > 1 ? 1 : completedPercent);

        // Draw an arc to indicate the amount of reference lap that remains.
        canvas.drawArc(mArcRect, 270 + (1 - remainingPercent) * 360,
                remainingPercent * 360, false, mRemainingPaint);

        // Draw an arc to indicate the amount of reference lap completed.
        canvas.drawArc(mArcRect, 270, completedPercent * 360, false, mCompletedPaint);

        // Simulate a rounded at the end of the arc by drawing a point
        if (completedPercent > 0f && completedPercent < 1f) {
            float endAngleDeg = 270 + completedPercent * 360;
            double endAngleRad = Math.toRadians(endAngleDeg);

            float endX = xCenter + radius * (float) Math.cos(endAngleRad);
            float endY = yCenter + radius * (float) Math.sin(endAngleRad);

            canvas.drawPoint(endX, endY, mCapPaint);
        }

        // Starting on lap 2, a marker can be drawn indicating where the prior lap ended.
        if (lapCount > 1) {
            final float angleFactor = 360f / firstLapTime;
            float markerAngleDeg = 270 + ((float) priorLap.getLapTime() * angleFactor);
            double markerAngleRad = Math.toRadians(markerAngleDeg);

            float markerLength = ThemeUtils.convertDpToPixels(14, getContext());

            float startX = xCenter + (radius - markerLength / 2) * (float) Math.cos(markerAngleRad);
            float startY = yCenter + (radius - markerLength / 2) * (float) Math.sin(markerAngleRad);
            float endX = xCenter + (radius + markerLength / 2) * (float) Math.cos(markerAngleRad);
            float endY = yCenter + (radius + markerLength / 2) * (float) Math.sin(markerAngleRad);

            canvas.drawLine(startX, startY, endX, endY, mMarkerPaint);
        }

        // Only redraw continuously while the timer is running
        if (stopwatch.isRunning()) {
            postInvalidateOnAnimation();
        }
    }

    /**
     * Start the animation if it is not currently running.
     */
    void update() {
        postInvalidateOnAnimation();
    }

    private Stopwatch getStopwatch() {
        return DataModel.getDataModel().getStopwatch();
    }

    private List<Lap> getLaps() {
        return DataModel.getDataModel().getLaps();
    }
}
