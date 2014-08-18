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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * Gets animators on view.
 */
public class AnimatorUtil {

    public static final long ANIM_DURATION_SHORT = 266;  // 8/30 frames long

    public static final float SCALE_ZERO = 0.0f;
    public static final float SCALE_FULL = 1.0f;

    private static final Interpolator ACCELERATE_DECELERATE_INTERPOLATOR =
            new AccelerateDecelerateInterpolator();

    /**
     * Switch to hardware layer type when animation starts.
     * And set it back when animation ends.
     */
    private static void setLayerTypeListener(final View view, ObjectAnimator animator) {
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
    }

    public static ObjectAnimator getScaleXAnimator(View view,
            float start, float end, long duration) {
        final ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.SCALE_X, start, end);
        animator.setDuration(duration);
        animator.setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);
        setLayerTypeListener(view, animator);
        return animator;
    }

    public static ObjectAnimator getScaleYAnimator(View view,
            float start, float end, long duration) {
        final ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.SCALE_Y, start, end);
        animator.setDuration(duration);
        animator.setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);
        setLayerTypeListener(view, animator);
        return animator;
    }
}
