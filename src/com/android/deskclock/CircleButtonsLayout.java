package com.android.deskclock;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * This class adjusts the locations of children buttons and text of this view group by adjusting the
 * margins of each item. The left and right buttons are aligned with the bottom of the circle. The
 * stop button and label text are located within the circle with the stop button near the bottom and
 * the label text near the top. The maximum text size for the label text view is also calculated.
 */
public class CircleButtonsLayout extends FrameLayout {

    private int mCircleTimerViewId;
    private int mResetAddButtonId;
    private int mLabelId;
    private float mDiamOffset;
    private View mCircleView;
    private ImageButton mResetAddButton;
    private TextView mLabel;

    @SuppressWarnings("unused")
    public CircleButtonsLayout(Context context) {
        this(context, null);
    }

    public CircleButtonsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCircleTimerViewIds(int circleTimerViewId, int stopButtonId,  int labelId) {
        mCircleTimerViewId = circleTimerViewId;
        mResetAddButtonId = stopButtonId;
        mLabelId = labelId;

        final Resources res = getContext().getResources();
        final float strokeSize = res.getDimension(R.dimen.circletimer_circle_size);
        final float dotStrokeSize = res.getDimension(R.dimen.circletimer_dot_size);
        final float markerStrokeSize = res.getDimension(R.dimen.circletimer_marker_size);
        mDiamOffset = Utils.calculateRadiusOffset(strokeSize, dotStrokeSize, markerStrokeSize) * 2;
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
        if (mCircleView == null) {
            mCircleView = findViewById(mCircleTimerViewId);
            if (mCircleView == null) {
                return;
            }
            mResetAddButton = (ImageButton) findViewById(mResetAddButtonId);
            mLabel = (TextView) findViewById(mLabelId);
        }

        final int frameWidth = mCircleView.getMeasuredWidth();
        final int frameHeight = mCircleView.getMeasuredHeight();
        final int minBound = Math.min(frameWidth, frameHeight);
        final int circleDiam = (int) (minBound - mDiamOffset);

        if (mResetAddButton != null) {
            final MarginLayoutParams resetAddParams = (MarginLayoutParams) mResetAddButton
                    .getLayoutParams();
            resetAddParams.bottomMargin = circleDiam / 6;
            if (minBound == frameWidth) {
                resetAddParams.bottomMargin += (frameHeight - frameWidth) / 2;
            }
        }

        if (mLabel != null) {
            MarginLayoutParams labelParams = (MarginLayoutParams) mLabel.getLayoutParams();
            labelParams.topMargin = circleDiam/6;
            if (minBound == frameWidth) {
                labelParams.topMargin += (frameHeight-frameWidth)/2;
            }
            /* The following formula has been simplified based on the following:
             * Our goal is to calculate the maximum width for the label frame.
             * We may do this with the following diagram to represent the top half of the circle:
             *                 ___
             *            .     |     .
             *        ._________|         .
             *     .       ^    |            .
             *   /         x    |              \
             *  |_______________|_______________|
             *
             *  where x represents the value we would like to calculate, and the final width of the
             *  label will be w = 2 * x.
             *
             *  We may find x by drawing a right triangle from the center of the circle:
             *                 ___
             *            .     |     .
             *        ._________|         .
             *     .    .       |            .
             *   /          .   | }y           \
             *  |_____________.t|_______________|
             *
             *  where t represents the angle of that triangle, and y is the height of that triangle.
             *
             *  If r = radius of the circle, we know the following trigonometric identities:
             *        cos(t) = y / r
             *  and   sin(t) = x / r
             *     => r * sin(t) = x
             *  and   sin^2(t) = 1 - cos^2(t)
             *     => sin(t) = +/- sqrt(1 - cos^2(t))
             *  (note: because we need the positive value, we may drop the +/-).
             *
             *  To calculate the final width, we may combine our formulas:
             *        w = 2 * x
             *     => w = 2 * r * sin(t)
             *     => w = 2 * r * sqrt(1 - cos^2(t))
             *     => w = 2 * r * sqrt(1 - (y / r)^2)
             *
             *  Simplifying even further, to mitigate the complexity of the final formula:
             *        sqrt(1 - (y / r)^2)
             *     => sqrt(1 - (y^2 / r^2))
             *     => sqrt((r^2 / r^2) - (y^2 / r^2))
             *     => sqrt((r^2 - y^2) / (r^2))
             *     => sqrt(r^2 - y^2) / sqrt(r^2)
             *     => sqrt(r^2 - y^2) / r
             *     => sqrt((r + y)*(r - y)) / r
             *
             * Placing this back in our formula, we end up with, as our final, reduced equation:
             *        w = 2 * r * sqrt(1 - (y / r)^2)
             *     => w = 2 * r * sqrt((r + y)*(r - y)) / r
             *     => w = 2 * sqrt((r + y)*(r - y))
             */
            // Radius of the circle.
            int r = circleDiam / 2;
            // Y value of the top of the label, calculated from the center of the circle.
            int y = frameHeight / 2 - labelParams.topMargin;
            // New maximum width of the label.
            double w = 2 * Math.sqrt((r + y) * (r - y));

            mLabel.setMaxWidth((int) w);
        }
    }
}
