/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

public class AnimatorUtils {

    public static final Interpolator DECELERATE_ACCELERATE_INTERPOLATOR = x -> 0.5f + 4.0f * (x - 0.5f) * (x - 0.5f) * (x - 0.5f);

    public static final Interpolator INTERPOLATOR_FAST_OUT_SLOW_IN = new FastOutSlowInInterpolator();

    public static final Property<View, Integer> BACKGROUND_ALPHA = new Property<>(Integer.class, "background.alpha") {
                @Override
                public Integer get(View view) {
                    Drawable background = view.getBackground();
                    if (background instanceof LayerDrawable
                            && ((LayerDrawable) background).getNumberOfLayers() > 0) {
                        background = ((LayerDrawable) background).getDrawable(0);
                    }
                    return background.getAlpha();
                }

                @Override
                public void set(View view, Integer value) {
                    setBackgroundAlpha(view, value);
                }
            };

    public static final Property<ImageView, Integer> DRAWABLE_ALPHA = new Property<>(Integer.class, "drawable.alpha") {
                @Override
                public Integer get(ImageView view) {
                    return view.getDrawable().getAlpha();
                }

                @Override
                public void set(ImageView view, Integer value) {
                    view.getDrawable().setAlpha(value);
                }
            };

    public static final Property<ImageView, Integer> DRAWABLE_TINT = new Property<>(Integer.class, "drawable.tint") {
                @Override
                public Integer get(ImageView view) {
                    return null;
                }

                @Override
                public void set(ImageView view, Integer value) {
                    // Ensure the drawable is wrapped using DrawableCompat.
                    final Drawable drawable = view.getDrawable();
                    final Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
                    if (wrappedDrawable != drawable) {
                        view.setImageDrawable(wrappedDrawable);
                    }
                    // Set the new tint value via DrawableCompat.
                    DrawableCompat.setTint(wrappedDrawable, value);
                }
            };

    @SuppressWarnings("unchecked")
    public static final TypeEvaluator<Integer> ARGB_EVALUATOR = new ArgbEvaluator();
    public static final Property<View, Integer> VIEW_LEFT = new Property<>(Integer.class, "left") {
                @Override
                public Integer get(View view) {
                    return view.getLeft();
                }

                @Override
                public void set(View view, Integer left) {
                    view.setLeft(left);
                }
            };

    public static final Property<View, Integer> VIEW_TOP = new Property<>(Integer.class, "top") {
                @Override
                public Integer get(View view) {
                    return view.getTop();
                }

                @Override
                public void set(View view, Integer top) {
                    view.setTop(top);
                }
            };

    public static final Property<View, Integer> VIEW_BOTTOM = new Property<>(Integer.class, "bottom") {
        @Override
        public Integer get(View view) {
            return view.getBottom();
        }

        @Override
        public void set(View view, Integer bottom) {
            view.setBottom(bottom);
        }
    };

    public static final Property<View, Integer> VIEW_RIGHT = new Property<>(Integer.class, "right") {
        @Override
        public Integer get(View view) {
            return view.getRight();
        }

        @Override
        public void set(View view, Integer right) {
            view.setRight(right);
        }
    };

    /**
     * Sets the alpha of the top layer's drawable (of the background) only, if the background is a
     * layer drawable, to ensure that the other layers (i.e., the selectable item background, and
     * therefore the touch feedback RippleDrawable) are not affected.
     *
     * @param view  the affected view
     * @param value the alpha value (0-255)
     */
    public static void setBackgroundAlpha(View view, Integer value) {
        Drawable background = view.getBackground();
        if (background instanceof LayerDrawable && ((LayerDrawable) background).getNumberOfLayers() > 0) {
            background = ((LayerDrawable) background).getDrawable(0);
        }
        background.setAlpha(value);
    }

    public static void reverse(ValueAnimator... animators) {
        for (ValueAnimator animator : animators) {
            final float fraction = animator.getAnimatedFraction();
            if (fraction > 0.0f) {
                animator.reverse();
                animator.setCurrentFraction(1.0f - fraction);
            }
        }
    }

    public static ValueAnimator getScaleAnimator(View view, float... values) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, values),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, values));
    }

    public static ValueAnimator getAlphaAnimator(View view, float... values) {
        return ObjectAnimator.ofFloat(view, View.ALPHA, values);
    }

    /**
     * @param target the view to be morphed
     * @param from   the bounds of the {@code target} before animating
     * @param to     the bounds of the {@code target} after animating
     * @return an animator that morphs the {@code target} between the {@code from} bounds and the
     * {@code to} bounds. Note that it is the *content* bounds that matter here, so padding
     * insets contributed by the background are subtracted from the views when computing the
     * {@code target} bounds.
     */
    public static Animator getBoundsAnimator(View target, View from, View to) {
        // Fetch the content insets for the views. Content bounds are what matter, not total bounds.
        final Rect targetInsets = new Rect();
        target.getBackground().getPadding(targetInsets);
        final Rect fromInsets = new Rect();
        from.getBackground().getPadding(fromInsets);
        final Rect toInsets = new Rect();
        to.getBackground().getPadding(toInsets);

        // Before animating, the content bounds of target must match the content bounds of from.
        final int startLeft = from.getLeft() - fromInsets.left + targetInsets.left;
        final int startTop = from.getTop() - fromInsets.top + targetInsets.top;
        final int startRight = from.getRight() - fromInsets.right + targetInsets.right;
        final int startBottom = from.getBottom() - fromInsets.bottom + targetInsets.bottom;

        // After animating, the content bounds of target must match the content bounds of to.
        final int endLeft = to.getLeft() - toInsets.left + targetInsets.left;
        final int endTop = to.getTop() - toInsets.top + targetInsets.top;
        final int endRight = to.getRight() - toInsets.right + targetInsets.right;
        final int endBottom = to.getBottom() - toInsets.bottom + targetInsets.bottom;

        return getBoundsAnimator(target, startLeft, startTop, startRight, startBottom, endLeft,
                endTop, endRight, endBottom);
    }

    /**
     * Returns an animator that animates the bounds of a single view.
     */
    public static Animator getBoundsAnimator(View view, int fromLeft, int fromTop, int fromRight,
                                             int fromBottom, int toLeft, int toTop, int toRight, int toBottom) {

        view.setLeft(fromLeft);
        view.setTop(fromTop);
        view.setRight(fromRight);
        view.setBottom(fromBottom);

        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofInt(VIEW_LEFT, toLeft),
                PropertyValuesHolder.ofInt(VIEW_TOP, toTop),
                PropertyValuesHolder.ofInt(VIEW_RIGHT, toRight),
                PropertyValuesHolder.ofInt(VIEW_BOTTOM, toBottom));
    }

    public static void startDrawableAnimation(ImageView view) {
        final Drawable d = view.getDrawable();
        if (d instanceof Animatable) {
            ((Animatable) d).start();
        }
    }
}
