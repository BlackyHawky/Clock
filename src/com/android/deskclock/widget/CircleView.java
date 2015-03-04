/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.widget;

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

import com.android.deskclock.R;

/**
 * A {@link View} that draws primitive circles.
 */
public class CircleView extends View {

    /**
     * A Property wrapper around the fillColor functionality handled by the
     * {@link #setFillColor(int)} and {@link #getFillColor()} methods.
     */
    public final static Property<CircleView, Integer> FILL_COLOR =
            new Property<CircleView, Integer>(Integer.class, "fillColor") {
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
            new Property<CircleView, Float>(Float.class, "radius") {
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
        this(context, null /* attrs */);
    }

    public CircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0 /* defStyleAttr */);
    }

    public CircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CircleView, defStyleAttr, 0 /* defStyleRes */);

        mGravity = a.getInt(R.styleable.CircleView_android_gravity, Gravity.NO_GRAVITY);
        mCenterX = a.getDimension(R.styleable.CircleView_centerX, 0.0f);
        mCenterY = a.getDimension(R.styleable.CircleView_centerY, 0.0f);
        mRadius = a.getDimension(R.styleable.CircleView_radius, 0.0f);

        mCirclePaint.setColor(a.getColor(R.styleable.CircleView_fillColor, Color.WHITE));

        a.recycle();
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw the circle, duh
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mCirclePaint);
    }

    @Override
    public boolean hasOverlappingRendering() {
        // only if we have a background, which we shouldn't...
        return getBackground() != null && getBackground().getCurrent() != null;
    }

    /**
     * @return the current {@link Gravity} used to align/size the circle
     */
    public final int getGravity() {
        return mGravity;
    }

    /**
     * Describes how to align/size the circle relative to the view's bounds. Defaults to
     * {@link Gravity#NO_GRAVITY}.
     * <p/>
     * Note: using {@link #setCenterX(float)}, {@link #setCenterY(float)}, or
     * {@link #setRadius(float)} will automatically clear any conflicting gravity bits.
     *
     * @param gravity the {@link Gravity} flags to use
     * @return this object, allowing calls to methods in this class to be chained
     * @see R.styleable#CircleView_android_gravity
     */
    public CircleView setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;

            if (gravity != Gravity.NO_GRAVITY && isLayoutDirectionResolved()) {
                applyGravity(gravity, getLayoutDirection());
            }
        }
        return this;
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
            invalidate(mCenterX, mCenterY, mRadius);
        }
        return this;
    }

    /**
     * @return the x-coordinate of the center of the circle
     */
    public final float getCenterX() {
        return mCenterX;
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
            invalidate(oldCenterX, mCenterY, mRadius);
            invalidate(centerX, mCenterY, mRadius);
        }

        // clear the horizontal gravity flags
        mGravity &= ~Gravity.HORIZONTAL_GRAVITY_MASK;

        return this;
    }

    /**
     * @return the y-coordinate of the center of the circle
     */
    public final float getCenterY() {
        return mCenterY;
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
            invalidate(mCenterX, oldCenterY, mRadius);
            invalidate(mCenterX, centerY, mRadius);
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
     * @return this object, allowing calls to methods in this class to be chained
     * @see R.styleable#CircleView_radius
     */
    public CircleView setRadius(float radius) {
        final float oldRadius = mRadius;
        if (oldRadius != radius) {
            mRadius = radius;

            // invalidate the old/new areas
            invalidate(mCenterX, mCenterY, oldRadius);
            if (radius > oldRadius) {
                invalidate(mCenterX, mCenterY, radius);
            }
        }

        // clear the fill gravity flags
        if ((mGravity & Gravity.FILL_HORIZONTAL) == Gravity.FILL_HORIZONTAL) {
            mGravity &= ~Gravity.FILL_HORIZONTAL;
        }
        if ((mGravity & Gravity.FILL_VERTICAL) == Gravity.FILL_VERTICAL) {
            mGravity &= ~Gravity.FILL_VERTICAL;
        }

        return this;
    }

    /**
     * Invalidates the rectangular area that circumscribes the circle defined by {@code centerX},
     * {@code centerY}, and {@code radius}.
     */
    private void invalidate(float centerX, float centerY, float radius) {
        invalidate((int) (centerX - radius - 0.5f), (int) (centerY - radius - 0.5f),
                (int) (centerX + radius + 0.5f), (int) (centerY + radius + 0.5f));
    }

    /**
     * Applies the specified {@code gravity} and {@code layoutDirection}, adjusting the alignment
     * and size of the circle depending on the resolved {@link Gravity} flags. Also invalidates the
     * affected area if necessary.
     *
     * @param gravity the {@link Gravity} the {@link Gravity} flags to use
     * @param layoutDirection the layout direction used to resolve the absolute gravity
     */
    @SuppressLint("RtlHardcoded")
    private void applyGravity(int gravity, int layoutDirection) {
        final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);

        final float oldRadius = mRadius;
        final float oldCenterX = mCenterX;
        final float oldCenterY = mCenterY;

        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
                mCenterX = 0.0f;
                break;
            case Gravity.CENTER_HORIZONTAL:
            case Gravity.FILL_HORIZONTAL:
                mCenterX = getWidth() / 2.0f;
                break;
            case Gravity.RIGHT:
                mCenterX = getWidth();
                break;
        }

        switch (absoluteGravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP:
                mCenterY = 0.0f;
                break;
            case Gravity.CENTER_VERTICAL:
            case Gravity.FILL_VERTICAL:
                mCenterY = getHeight() / 2.0f;
                break;
            case Gravity.BOTTOM:
                mCenterY = getHeight();
                break;
        }

        switch (absoluteGravity & Gravity.FILL) {
            case Gravity.FILL:
                mRadius = Math.min(getWidth(), getHeight()) / 2.0f;
                break;
            case Gravity.FILL_HORIZONTAL:
                mRadius = getWidth() / 2.0f;
                break;
            case Gravity.FILL_VERTICAL:
                mRadius = getHeight() / 2.0f;
                break;
        }

        if (oldCenterX != mCenterX || oldCenterY != mCenterY || oldRadius != mRadius) {
            invalidate(oldCenterX, oldCenterY, oldRadius);
            invalidate(mCenterX, mCenterY, mRadius);
        }
    }
}
