/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.Interpolator;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

public class AnimatorUtils {

    public static final Interpolator INTERPOLATOR_FAST_OUT_SLOW_IN = new FastOutSlowInInterpolator();

    @SuppressWarnings("unchecked")
    public static final TypeEvaluator<Integer> ARGB_EVALUATOR = new ArgbEvaluator();

    public static ValueAnimator getScaleAnimator(View view, float... values) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, values),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, values));
    }

    public static ValueAnimator getAlphaAnimator(View view, float... values) {
        return ObjectAnimator.ofFloat(view, View.ALPHA, values);
    }

}
