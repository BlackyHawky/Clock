package com.best.deskclock.timer;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.best.deskclock.data.Timer;
import com.google.android.material.color.MaterialColors;

/**
 * Custom view that draws timer progress as a horizontal bar.
 */
public final class TimerBarView extends View {

    private static final float NEAR_COMPLETE_THRESHOLD = 0.99f;

    private final Paint mCompletedPaint = new Paint();
    private final Paint mRemainingPaint = new Paint();

    private final Path mCompletedPath = new Path();
    private final Path mRemainingPath = new Path();

    private final float[] mRadiiAll = new float[8];
    private final float[] mRadiiLeft = new float[8];
    private final float[] mRadiiRight = new float[8];

    private final float mBarHeight;

    private Timer mTimer;

    public TimerBarView(Context context) {
        this(context, null);
    }

    public TimerBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mBarHeight = dpToPx(6, getResources().getDisplayMetrics());

        final int remainingColor = MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK);
        final int completedColor = MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK);

        mCompletedPaint.setAntiAlias(true);
        mCompletedPaint.setStyle(Paint.Style.FILL);
        mCompletedPaint.setColor(completedColor);

        mRemainingPaint.setAntiAlias(true);
        mRemainingPaint.setStyle(Paint.Style.FILL);
        mRemainingPaint.setColor(remainingColor);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (mTimer == null) {
            return;
        }

        final int width = getWidth();
        final int height = getHeight();

        final float top = (height - mBarHeight) / 2f;
        final float bottom = top + mBarHeight;
        final float radius = mBarHeight / 2f;

        // Prepare radii arrays
        prepareRadii(radius);

        mCompletedPath.reset();
        mRemainingPath.reset();

        if (mTimer.isReset()) {
            // Rounded to the right only
            mRemainingPath.addRoundRect(0, top, width, bottom, mRadiiRight, Path.Direction.CW);
            canvas.drawPath(mRemainingPath, mRemainingPaint);
            return;
        }

        if (mTimer.isExpired()) {
            // Rounded at both ends
            mCompletedPath.addRoundRect(0, top, width, bottom, mRadiiAll, Path.Direction.CW);
            canvas.drawPath(mCompletedPath, mCompletedPaint);
            return;
        }

        final float totalLength = mTimer.getTotalLength();
        final float completedPercent = totalLength > 0
                ? Math.min(1f, (float) mTimer.getElapsedTime() / totalLength)
                : 0f;

        final float completedWidth = completedPercent * width;

        // If almost finished: complete rounding
        if (completedPercent > NEAR_COMPLETE_THRESHOLD) {
            mCompletedPath.addRoundRect(0, top, width, bottom, mRadiiAll, Path.Direction.CW);
            canvas.drawPath(mCompletedPath, mCompletedPaint);
        } else {
            // Completed part: rounded to the left only
            mCompletedPath.addRoundRect(0, top, completedWidth, bottom, mRadiiLeft, Path.Direction.CW);
            canvas.drawPath(mCompletedPath, mCompletedPaint);

            // Remaining part: rounded to the right only
            mRemainingPath.addRoundRect(completedWidth, top, width, bottom, mRadiiRight, Path.Direction.CW);
            canvas.drawPath(mRemainingPath, mRemainingPaint);
        }

        if (mTimer.isRunning()) {
            postInvalidateOnAnimation();
        }
    }

    private void prepareRadii(float radius) {
        // All rounded
        for (int i = 0; i < 8; i++) {
            mRadiiAll[i] = radius;
        }

        // Left rounded
        mRadiiLeft[0] = radius; mRadiiLeft[1] = radius;
        mRadiiLeft[6] = radius; mRadiiLeft[7] = radius;
        mRadiiLeft[2] = mRadiiLeft[3] = mRadiiLeft[4] = mRadiiLeft[5] = 0f;

        // Right rounded
        mRadiiRight[2] = radius; mRadiiRight[3] = radius;
        mRadiiRight[4] = radius; mRadiiRight[5] = radius;
        mRadiiRight[0] = mRadiiRight[1] = mRadiiRight[6] = mRadiiRight[7] = 0f;
    }

    void update(Timer timer) {
        if (mTimer != timer) {
            mTimer = timer;
            postInvalidateOnAnimation();
        }
    }

}


