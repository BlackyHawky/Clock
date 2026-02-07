// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.uicomponents;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Property;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;

import com.best.deskclock.R;

public class PillView extends View {

    public static final Property<PillView, Float> PILL_CENTER_X =
            new Property<>(Float.class, "pillCenterX") {
                @Override
                public Float get(PillView view) {
                    return view.getPillCenterX();
                }

                @Override
                public void set(PillView view, Float value) {
                    view.setPillCenterX(value);
                }
            };

    public static final Property<PillView, Float> PILL_WIDTH =
            new Property<>(Float.class, "pillWidth") {
                @Override
                public Float get(PillView view) {
                    return view.getPillWidth();
                }

                @Override
                public void set(PillView view, Float value) {
                    view.setPillWidth(value);
                }
            };

    public static final Property<PillView, Integer> FILL_COLOR =
            new Property<>(Integer.class, "pillFillColor") {
                @Override
                public Integer get(PillView view) {
                    return view.getFillColor();
                }

                @Override
                public void set(PillView view, Integer value) {
                    view.setFillColor(value);
                }
            };

    private final Paint mPillPaint = new Paint();
    private final RectF mRect = new RectF();
    private final int mGravity;
    private float mCenterX;
    private float mCenterY;
    private float mWidth;
    private float mHeight;

    public PillView(Context context) {
        this(context, null);
    }

    public PillView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * @noinspection resource
     */
    public PillView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PillView, defStyleAttr, 0);
        try {
            mGravity = a.getInt(R.styleable.PillView_android_gravity, Gravity.NO_GRAVITY);
            mCenterX = a.getDimension(R.styleable.PillView_pillCenterX, 0.0f);
            mCenterY = a.getDimension(R.styleable.PillView_pillCenterY, 0.0f);
            mWidth = a.getDimension(R.styleable.PillView_pillWidth, 0.0f);
            mHeight = a.getDimension(R.styleable.PillView_pillHeight, 0.0f);
            mPillPaint.setColor(a.getColor(R.styleable.PillView_pillFillColor, Color.WHITE));
        } finally {
            a.recycle();
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mGravity != Gravity.NO_GRAVITY) {
            applyGravity(mGravity, layoutDirection);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mGravity != Gravity.NO_GRAVITY) {
            applyGravity(mGravity, getLayoutDirection());
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        mRect.set(
                mCenterX - mWidth / 2,
                mCenterY - mHeight / 2,
                mCenterX + mWidth / 2,
                mCenterY + mHeight / 2
        );
        canvas.drawRoundRect(mRect, mHeight / 2, mHeight / 2, mPillPaint);
    }

    public float getPillWidth() {
        return mWidth;
    }

    public void setPillWidth(float width) {
        if (mWidth != width) {
            mWidth = width;
            invalidate();
        }
    }

    public void setPillHeight(float height) {
        if (mHeight != height) {
            mHeight = height;
            invalidate();
        }
    }

    public float getPillCenterX() {
        return mCenterX;
    }

    public void setPillCenterX(float centerX) {
        if (mCenterX != centerX) {
            mCenterX = centerX;
            invalidate();
        }
    }

    public final int getFillColor() {
        return mPillPaint.getColor();
    }

    public PillView setFillColor(int color) {
        if (mPillPaint.getColor() != color) {
            mPillPaint.setColor(color);
            invalidate();
        }
        return this;
    }

    @SuppressLint("RtlHardcoded")
    private void applyGravity(int gravity, int layoutDirection) {
        final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);

        float oldCenterX = mCenterX;
        float oldCenterY = mCenterY;

        // Horizontal
        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT -> mCenterX = 0.0f;
            case Gravity.CENTER_HORIZONTAL, Gravity.FILL_HORIZONTAL -> mCenterX = getWidth() / 2f;
            case Gravity.RIGHT -> mCenterX = getWidth();
        }

        // Vertical
        switch (absoluteGravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP -> mCenterY = 0.0f;
            case Gravity.CENTER_VERTICAL, Gravity.FILL_VERTICAL -> mCenterY = getHeight() / 2f;
            case Gravity.BOTTOM -> mCenterY = getHeight();
        }

        if ((absoluteGravity & Gravity.FILL_HORIZONTAL) == Gravity.FILL_HORIZONTAL) {
            mWidth = getWidth();
        }

        if ((absoluteGravity & Gravity.FILL_VERTICAL) == Gravity.FILL_VERTICAL) {
            mHeight = getHeight();
        }

        if (mCenterX != oldCenterX || mCenterY != oldCenterY) {
            invalidate();
        }
    }
}
