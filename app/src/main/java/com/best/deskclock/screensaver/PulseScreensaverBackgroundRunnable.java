// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.screensaver;

import static com.best.deskclock.utils.AnimatorUtils.getAlphaAnimator;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.Utils;

/**
 * Runnable responsible for animating the screensaver background to prevent burn-in.
 *
 * <p>This animation performs a subtle fade-out / fade-in.
 * This creates a gentle "breathing" effect that periodically refreshes the pixels.</p>
 *
 * <p>The animation is triggered periodically using UiDataModel's half-minute callbacks,
 * keeping it synchronized with the clock movement animation.</p>
 */
public final class PulseScreensaverBackgroundRunnable implements Runnable {

    /**
     * The duration over which the fade in/out animations occur.
     */
    private static final long FADE_TIME = 3000L;

    /**
     * Accelerate the hide animation.
     */
    private final Interpolator mAcceleration = new AccelerateInterpolator();

    /**
     * Decelerate the show animation.
     */
    private final Interpolator mDeceleration = new DecelerateInterpolator();

    private final View mBackgroundView;
    private Animator mActiveAnimator;

    public PulseScreensaverBackgroundRunnable(View backgroundView) {
        mBackgroundView = backgroundView;
    }

    public void start() {
        stop();
        UiDataModel.getUiDataModel().addHalfMinuteCallback(this, -FADE_TIME);
    }

    public void stop() {
        UiDataModel.getUiDataModel().removePeriodicCallback(this);
        if (mActiveAnimator != null) {
            mActiveAnimator.end();
            mActiveAnimator = null;
        }
    }

    @Override
    public void run() {
        Utils.enforceMainLooper();

        // Fade out + slight zoom out
        AnimatorSet hide = new AnimatorSet();
        hide.setDuration(FADE_TIME);
        hide.setInterpolator(mAcceleration);
        hide.play(getAlphaAnimator(mBackgroundView, 1f, 0f));

        // Fade in + zoom in
        AnimatorSet show = new AnimatorSet();
        show.setDuration(FADE_TIME);
        show.setInterpolator(mDeceleration);
        show.play(getAlphaAnimator(mBackgroundView, 0f, 1f));

        AnimatorSet all = new AnimatorSet();
        all.play(show).after(hide);

        mActiveAnimator = all;
        mActiveAnimator.start();
    }
}
