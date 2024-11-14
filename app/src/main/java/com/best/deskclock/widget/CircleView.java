/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Property;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;

import com.best.deskclock.R;

/**
 * A {@link View} that draws primitive circles.
 */
public class CircleView extends View {

    /**
     * A Property wrapper around the fillColor functionality handled by the
     * {@link #setFillColor(int)} and {@link #getFillColor()} methods.
     */
    public final static Property<CircleView, Integer> FILL_COLOR =
            new Property<>(Integer.class, "fillColor") {
                @Override
                public Integer get(CircleView view) {
                    return view.getFillColor();
                }

                @Override
                public void set(CircleView view, Integer value) {
                    view.setFillColor(value);
                }
            };

    /**
     * A Property wrapper around the radius functionality handled by the
     * {@link #setRadius(float)} and {@link #getRadius()} methods.
     */
    public final static Property<CircleView, Float> RADIUS =
            new Property<>(Float.class, "radius") {
                @Override
                public Float get(CircleView view) {
                    return view.getRadius();
                }

                @Override
                public void set(CircleView view, Float value) {
                    view.setRadius(value);
                }
            };

    /**
     * The {@link Paint} used to draw the circle.
     */
    private final Paint mCirclePaint = new Paint();

    private int mGravity;
    private float mCenterX;
    private float mCenterY;
    private float mRadius;

    public CircleView(Context context) {
        this(context, null);
    }

    public CircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleView, defStyleAttr, 0)) {

            mGravity = a.getInt(R.styleable.CircleView_android_gravity, Gravity.NO_GRAVITY);
            mCenterX = a.getDimension(R.styleable.CircleView_centerX, 0.0f);
            mCenterY = a.getDimension(R.styleable.CircleView_centerY, 0.0f);
            mRadius = a.getDimension(R.styleable.CircleView_radius, 0.0f);

            mCirclePaint.setColor(a.getColor(R.styleable.CircleView_fillColor, Color.WHITE));
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

        // draw the circle, duh
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mCirclePaint);
    }

    @Override
    public boolean hasOverlappingRendering() {
        // only if we have a background, which we shouldn't...
        return getBackground() != null;
    }

    /**
     * @return the ARGB color used to fill the circle
     */
    public final int getFillColor() {
        return mCirclePaint.getColor();
    }

    /**
     * Sets the ARGB color used to fill the circle and invalidates only the affected area.
     *
     * @param color the ARGB color to use
     * @return this object, allowing calls to methods in this class to be chained
     * @see R.styleable#CircleView_fillColor
     */
    public CircleView setFillColor(int color) {
        if (mCirclePaint.getColor() != color) {
            mCirclePaint.setColor(color);

            // invalidate the current area
            invalidate();
        }
        return this;
    }

    /**
     * Sets the x-coordinate for the center of the circle and invalidates only the affected area.
     *
     * @param centerX the x-coordinate to use, relative to the view's bounds
     * @return this object, allowing calls to methods in this class to be chained
     * @see R.styleable#CircleView_centerX
     */
    public CircleView setCenterX(float centerX) {
        final float oldCenterX = mCenterX;
        if (oldCenterX != centerX) {
            mCenterX = centerX;

            // invalidate the old/new areas
            invalidate();
        }

        // clear the horizontal gravity flags
        mGravity &= ~Gravity.HORIZONTAL_GRAVITY_MASK;

        return this;
    }

    /**
     * Sets the y-coordinate for the center of the circle and invalidates only the affected area.
     *
     * @param centerY the y-coordinate to use, relative to the view's bounds
     * @return this object, allowing calls to methods in this class to be chained
     * @see R.styleable#CircleView_centerY
     */
    public CircleView setCenterY(float centerY) {
        final float oldCenterY = mCenterY;
        if (oldCenterY != centerY) {
            mCenterY = centerY;

            // invalidate the old/new areas
            invalidate();
        }

        // clear the vertical gravity flags
        mGravity &= ~Gravity.VERTICAL_GRAVITY_MASK;

        return this;
    }

    /**
     * @return the radius of the circle
     */
    public final float getRadius() {
        return mRadius;
    }

    /**
     * Sets the radius of the circle and invalidates only the affected area.
     *
     * @param radius the radius to use
     * @see R.styleable#CircleView_radius
     */
    public void setRadius(float radius) {
        final float oldRadius = mRadius;
        if (oldRadius != radius) {
            mRadius = radius;

            // invalidate the old/new areas
            invalidate();
        }

        // clear the fill gravity flags
        if ((mGravity & Gravity.FILL_HORIZONTAL) == Gravity.FILL_HORIZONTAL) {
            mGravity &= ~Gravity.FILL_HORIZONTAL;
        }
        if ((mGravity & Gravity.FILL_VERTICAL) == Gravity.FILL_VERTICAL) {
            mGravity &= ~Gravity.FILL_VERTICAL;
        }
    }

    /**
     * Applies the specified {@code gravity} and {@code layoutDirection}, adjusting the alignment
     * and size of the circle depending on the resolved {@link Gravity} flags. Also invalidates the
     * affected area if necessary.
     *
     * @param gravity         the {@link Gravity} the {@link Gravity} flags to use
     * @param layoutDirection the layout direction used to resolve the absolute gravity
     */
    @SuppressLint("RtlHardcoded")
    private void applyGravity(int gravity, int layoutDirection) {
        final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);

        final float oldRadius = mRadius;
        final float oldCenterX = mCenterX;
        final float oldCenterY = mCenterY;

        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT -> mCenterX = 0.0f;
            case Gravity.CENTER_HORIZONTAL, Gravity.FILL_HORIZONTAL -> mCenterX = getWidth() / 2.0f;
            case Gravity.RIGHT -> mCenterX = getWidth();
        }

        switch (absoluteGravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP -> mCenterY = 0.0f;
            case Gravity.CENTER_VERTICAL, Gravity.FILL_VERTICAL -> mCenterY = getHeight() / 2.0f;
            case Gravity.BOTTOM -> mCenterY = getHeight();
        }

        switch (absoluteGravity & Gravity.FILL) {
            case Gravity.FILL -> mRadius = Math.min(getWidth(), getHeight()) / 2.0f;
            case Gravity.FILL_HORIZONTAL -> mRadius = getWidth() / 2.0f;
            case Gravity.FILL_VERTICAL -> mRadius = getHeight() / 2.0f;
        }

        if (oldCenterX != mCenterX || oldCenterY != mCenterY || oldRadius != mRadius) {
            invalidate();
        }
    }
}
