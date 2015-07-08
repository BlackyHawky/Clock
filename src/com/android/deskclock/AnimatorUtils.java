/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AnimatorUtils {

    public static final long ANIM_DURATION_SHORT = 266L;  // 8/30 frames long

    public static final Interpolator DECELERATE_ACCELERATE_INTERPOLATOR = new Interpolator() {
        @Override
        public float getInterpolation(float x) {
            return 0.5f + 4.0f * (x - 0.5f) * (x - 0.5f) * (x - 0.5f);
        }
    };

    public static final Property<View, Integer> BACKGROUND_ALPHA =
            new Property<View, Integer>(Integer.class, "background.alpha") {
        @Override
        public Integer get(View view) {
            return view.getBackground().getAlpha();
        }

        @Override
        public void set(View view, Integer value) {
            view.getBackground().setAlpha(value);
        }
    };

    public static final Property<ImageView, Integer> DRAWABLE_ALPHA =
            new Property<ImageView, Integer>(Integer.class, "drawable.alpha") {
        @Override
        public Integer get(ImageView view) {
            return view.getDrawable().getAlpha();
        }

        @Override
        public void set(ImageView view, Integer value) {
            view.getDrawable().setAlpha(value);
        }
    };

    public static final Property<ImageView, Integer> DRAWABLE_TINT =
            new Property<ImageView, Integer>(Integer.class, "drawable.tint") {
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

    public static final ArgbEvaluator ARGB_EVALUATOR = new ArgbEvaluator();

    private static Method sAnimateValue;
    private static boolean sTryAnimateValue = true;

    public static void setAnimatedFraction(ValueAnimator animator, float fraction) {
        if (Utils.isLMR1OrLater()) {
            animator.setCurrentFraction(fraction);
            return;
        }

        if (sTryAnimateValue) {
            // try to set the animated fraction directly so that it isn't affected by the
            // internal animator scale or time (b/17938711)
            try {
                if (sAnimateValue == null) {
                    sAnimateValue = ValueAnimator.class
                            .getDeclaredMethod("animateValue", float.class);
                    sAnimateValue.setAccessible(true);
                }

                sAnimateValue.invoke(animator, fraction);
                return;
            } catch (NoSuchMethodException | InvocationTargetException
                    | IllegalAccessException e) {
                // something went wrong, don't try that again
                LogUtils.e("Unable to use animateValue directly", e);
                sTryAnimateValue = false;
            }
        }

        // if that doesn't work then just fall back to setting the current play time
        animator.setCurrentPlayTime(Math.round(fraction * animator.getDuration()));
    }

    public static void reverse(ValueAnimator... animators) {
        for (ValueAnimator animator : animators) {
            final float fraction = animator.getAnimatedFraction();
            if (fraction > 0.0f) {
                animator.reverse();
                setAnimatedFraction(animator, 1.0f - fraction);
            }
        }
    }

    public static void cancel(ValueAnimator... animators) {
        for (ValueAnimator animator : animators) {
            animator.cancel();
        }
    }

    public static ValueAnimator getScaleAnimator(View view, float... values) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, values),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, values));
    }
}
