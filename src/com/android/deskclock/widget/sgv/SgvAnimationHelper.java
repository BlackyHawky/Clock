// Copyright 2013 Google Inc. All Rights Reserved.

package com.android.deskclock.widget.sgv;

import android.R.interpolator;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;


import java.util.List;

public class SgvAnimationHelper {

    /**
     * Supported entrance animations for views in the {@link StaggeredGridView}.
     */
    public enum AnimationIn {
        NONE,
        // Fly in all views from the bottom of the screen
        FLY_UP_ALL_VIEWS,
        // New views expand into view from height 0.  Existing views are updated and translated
        // to their new positions if appropriate.
        EXPAND_NEW_VIEWS,
        // New views expand into view from height 0.  Existing views are updated and translated
        // to their new positions if appropriate.  This animation is done for all views
        // simultaneously without a cascade effect.
        EXPAND_NEW_VIEWS_NO_CASCADE,
        // New views are flown in from the bottom.  Existing views are updated and translated
        // to their new positions if appropriate.
        FLY_IN_NEW_VIEWS,
        // New views are slid in from the side.  Existing views are updated and translated
        // to their new positions if appropriate.
        SLIDE_IN_NEW_VIEWS,
        // Fade in all new views
        FADE,
    }

    /**
     * Supported exit animations for views in the {@link StaggeredGridView}.
     */
    public enum AnimationOut {
        NONE,
        // Stale views are faded out of view.  Existing views are then updated and translated
        // to their new positions if appropriate.
        FADE,
        // Stale views are dropped to the bottom of the screen.  Existing views are then updated
        // and translated to their new positions if appropriate.
        FLY_DOWN,
        // Stale views are slid to the side of the screen.  Existing views are then updated
        // and translated to their new positions if appropriate.
        SLIDE,
        // Stale views are collapsed to height 0.  Existing views are then updated
        // and translated to their new positions if appropriate.
        COLLAPSE
    }

    private static Interpolator sDecelerateQuintInterpolator;

    private static final int ANIMATION_LONG_SCREEN_SIZE = 1600;
    private static final int ANIMATION_MED_SCREEN_SIZE = 1200;

    // Duration of an individual animation when the children of the grid are laid out again. These
    // are measured in milliseconds and based on the height of the screen.
    private static final int ANIMATION_SHORT_DURATION = 400;
    private static final int ANIMATION_MED_DURATION = 450;
    private static final int ANIMATION_LONG_DURATION = 500;

    public static final float ANIMATION_ROTATION_DEGREES = 25.0f;

    /**
     * Duration of an individual animation when the children of the grid are laid out again.
     * This is measured in milliseconds.
     */
    private static int sAnimationDuration = ANIMATION_MED_DURATION;

    public static void initialize(Context context) {
        sDecelerateQuintInterpolator = AnimationUtils.loadInterpolator(context,
                interpolator.decelerate_quint);

        final Point size = new Point();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).
                getDefaultDisplay().getSize(size);
        final int screenHeight = size.y;

        if (screenHeight >= ANIMATION_LONG_SCREEN_SIZE) {
            sAnimationDuration = ANIMATION_LONG_DURATION;
        } else if (screenHeight >= ANIMATION_MED_SCREEN_SIZE) {
            sAnimationDuration = ANIMATION_MED_DURATION;
        } else {
            sAnimationDuration = ANIMATION_SHORT_DURATION;
        }
    }

    public static int getDefaultAnimationDuration() {
        return sAnimationDuration;
    }

    public static Interpolator getDefaultAnimationInterpolator() {
        return sDecelerateQuintInterpolator;
    }

    /**
     * Add animations to translate a view's X-translation.  {@link AnimatorListener} can be null.
     */
    private static void addXTranslationAnimators(List<Animator> animators,
            final View view, int startTranslation, final int endTranslation, int animationDelay,
            AnimatorListener listener) {
        // We used to skip the animation if startTranslation == endTranslation,
        // but to add a recycle view listener, we need at least one animation
        view.setTranslationX(startTranslation);
        final ObjectAnimator translateAnimatorX = ObjectAnimator.ofFloat(view,
                View.TRANSLATION_X, startTranslation, endTranslation);
        translateAnimatorX.setInterpolator(sDecelerateQuintInterpolator);
        translateAnimatorX.setDuration(sAnimationDuration);
        translateAnimatorX.setStartDelay(animationDelay);
        if (listener != null) {
            translateAnimatorX.addListener(listener);
        }

        animators.add(translateAnimatorX);
    }

    /**
     * Add animations to translate a view's Y-translation.  {@link AnimatorListener} can be null.
     */
    private static void addYTranslationAnimators(List<Animator> animators,
            final View view, int startTranslation, final int endTranslation, int animationDelay,
            AnimatorListener listener) {
        // We used to skip the animation if startTranslation == endTranslation,
        // but to add a recycle view listener, we need at least one animation
        view.setTranslationY(startTranslation);
        final ObjectAnimator translateAnimatorY = ObjectAnimator.ofFloat(view,
                View.TRANSLATION_Y, startTranslation, endTranslation);
        translateAnimatorY.setInterpolator(sDecelerateQuintInterpolator);
        translateAnimatorY.setDuration(sAnimationDuration);
        translateAnimatorY.setStartDelay(animationDelay);

        if (listener != null) {
            translateAnimatorY.addListener(listener);
        }

        animators.add(translateAnimatorY);
    }

    /**
     * Translate a view to the specified translation values, and animate the translations to 0.
     */
    public static void addXYTranslationAnimators(List<Animator> animators, final View view,
            int xTranslation, int yTranslation, int animationDelay) {
        addXTranslationAnimators(animators, view, xTranslation, 0, animationDelay, null);
        addYTranslationAnimators(animators, view, yTranslation, 0, animationDelay, null);
    }

    /**
     * Translate a view to the specified translation values, while rotating to the specified
     * rotation value.
     */
    public static void addTranslationRotationAnimators(List<Animator> animators, final View view,
            int xTranslation, int yTranslation, float rotation, int animationDelay) {
        addXYTranslationAnimators(animators, view, xTranslation, yTranslation, animationDelay);

        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        view.setRotation(rotation);

        final ObjectAnimator rotateAnimatorY = ObjectAnimator.ofFloat(view,
                View.ROTATION, view.getRotation(), 0.0f);
        rotateAnimatorY.setInterpolator(sDecelerateQuintInterpolator);
        rotateAnimatorY.setDuration(sAnimationDuration);
        rotateAnimatorY.setStartDelay(animationDelay);
        rotateAnimatorY.addListener(new AnimatorListenerAdapter() {
            private boolean mIsCanceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                mIsCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mIsCanceled) {
                    view.setRotation(0);
                }

                view.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        animators.add(rotateAnimatorY);
    }

    /**
     * Expand a view into view by scaling up vertically from 0.
     */
    public static void addExpandInAnimators(List<Animator> animators, final View view,
            int animationDelay) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        view.setScaleY(0);

        final ObjectAnimator scaleAnimatorY = ObjectAnimator.ofFloat(view,
                View.SCALE_Y, view.getScaleY(), 1.0f);
        scaleAnimatorY.setInterpolator(sDecelerateQuintInterpolator);
        scaleAnimatorY.setDuration(sAnimationDuration);
        scaleAnimatorY.setStartDelay(animationDelay);
        scaleAnimatorY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setScaleY(1.0f);
                view.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        animators.add(scaleAnimatorY);
    }

    /**
     * Collapse a view out by scaling it from its current scaled value to 0.
     */
    public static void addCollapseOutAnimators(List<Animator> animators, final View view,
            int animationDelay) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        final ObjectAnimator scaleAnimatorY = ObjectAnimator.ofFloat(view, View.SCALE_Y,
                view.getScaleY(), 0);
        scaleAnimatorY.setInterpolator(sDecelerateQuintInterpolator);
        scaleAnimatorY.setDuration(sAnimationDuration);
        scaleAnimatorY.setStartDelay(animationDelay);
        scaleAnimatorY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setScaleY(0);
                view.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        animators.add(scaleAnimatorY);
    }

    /**
     * Collapse a view out by scaling it from its current scaled value to 0.
     * The animators are expected to run immediately without a start delay.
     */
    public static void addCollapseOutAnimators(List<Animator> animators, final View view) {
        addCollapseOutAnimators(animators, view, 0 /* animation delay */);
    }



    /**
     * Fly a view out by moving it vertically off the bottom of the screen.
     */
    public static void addFlyOutAnimators(List<Animator> animators,
            final View view, int startTranslation, int endTranslation, int animationDelay) {
        addYTranslationAnimators(animators, view, startTranslation, endTranslation,
                animationDelay, null);
    }

    public static void addFlyOutAnimators(List<Animator> animators, final View view,
            int startTranslation, int endTranslation) {
        addFlyOutAnimators(animators, view, startTranslation,
                endTranslation, 0 /* animation delay */);
    }

    public static void addSlideInFromRightAnimators(List<Animator> animators, final View view,
            int startTranslation, int animationDelay) {
        addXTranslationAnimators(animators, view, startTranslation, 0, animationDelay, null);
        addFadeAnimators(animators, view, 0, 1.0f, animationDelay);
    }

    /**
     * Slide a view out of view from the start to end position, fading the view out as it
     * approaches the end position.
     */
    public static void addSlideOutAnimators(List<Animator> animators, final View view,
            int startTranslation, int endTranslation, int animationDelay) {
        addFadeAnimators(animators, view, view.getAlpha(), 0, animationDelay);
        addXTranslationAnimators(animators, view, startTranslation, endTranslation,
                animationDelay, null);
    }

    /**
     * Slide a view out of view from the start to end position, fading the view out as it
     * approaches the end position.  The animators are expected to run immediately without a
     * start delay.
     */
    public static void addSlideOutAnimators(List<Animator> animators, final View view,
            int startTranslation, int endTranslation) {
        addSlideOutAnimators(animators, view, startTranslation,
                endTranslation, 0 /* animation delay */);
    }

    /**
     * Add animations to fade a view from the specified start alpha value to end value.
     */
    public static void addFadeAnimators(List<Animator> animators, final View view,
            float startAlpha, final float endAlpha, int animationDelay) {
        if (startAlpha == endAlpha) {
            return;
        }

        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        view.setAlpha(startAlpha);

        final ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(view, View.ALPHA,
                view.getAlpha(), endAlpha);
        fadeAnimator.setInterpolator(sDecelerateQuintInterpolator);
        fadeAnimator.setDuration(sAnimationDuration);
        fadeAnimator.setStartDelay(animationDelay);
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setAlpha(endAlpha);
                view.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
        animators.add(fadeAnimator);
    }

    /**
     * Add animations to fade a view from the specified start alpha value to end value.
     * The animators are expected to run immediately without a start delay.
     */
    public static void addFadeAnimators(List<Animator> animators, final View view,
            float startAlpha, final float endAlpha) {
        addFadeAnimators(animators, view, startAlpha, endAlpha, 0 /* animation delay */);
    }
}
