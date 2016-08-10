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

package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;

import com.android.deskclock.uidata.OnAppColorChangeListener;
import com.android.deskclock.uidata.UiDataModel;

import static com.android.deskclock.AnimatorUtils.ARGB_EVALUATOR;

/**
 * Base activity class that changes the app window's color based on the current hour.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Key used to save/restore the current app window color from the saved instance state. */
    private static final String KEY_WINDOW_COLOR = "window_color";

    /** Reacts to app window color changes from the model when this activity is forward. */
    private final OnAppColorChangeListener mAppColorChangeListener = new AppColorChangeListener();

    /** Sets the app window color on each frame of the {@link #mAppColorAnimator}. */
    private final AppColorAnimationListener mAppColorAnimationListener
            = new AppColorAnimationListener();

    /** The current animator that is changing the app window color or {@code null}. */
    private ValueAnimator mAppColorAnimator;

    /** Draws the app window's color. */
    private ColorDrawable mBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final @ColorInt int currentColor = UiDataModel.getUiDataModel().getWindowBackgroundColor();
        final @ColorInt int bgColor = savedInstanceState == null ? currentColor
                : savedInstanceState.getInt(KEY_WINDOW_COLOR, currentColor);
        adjustAppColor(bgColor, false /* animate */);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start updating the app window color each time it changes.
        UiDataModel.getUiDataModel().addOnAppColorChangeListener(mAppColorChangeListener);

        // Ensure the app window color is up-to-date.
        final @ColorInt int bgColor = UiDataModel.getUiDataModel().getWindowBackgroundColor();
        adjustAppColor(bgColor, true /* animate */);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop updating the app window color when not active.
        UiDataModel.getUiDataModel().removeOnAppColorChangeListener(mAppColorChangeListener);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the app window color so we can animate the change when the activity is restored.
        if (mBackground != null) {
            outState.putInt(KEY_WINDOW_COLOR, mBackground.getColor());
        }
    }

    /**
     * Adjusts the current app window color of this activity; animates the change if desired.
     *
     * @param color the ARGB value to set as the current app window color
     * @param animate {@code true} if the change should be animated
     */
    protected void adjustAppColor(@ColorInt int color, boolean animate) {
        // Create and install the drawable that defines the window color.
        if (mBackground == null) {
            mBackground = new ColorDrawable(color);
            getWindow().setBackgroundDrawable(mBackground);
        }

        // Cancel the current window color animation if one exists.
        if (mAppColorAnimator != null) {
            mAppColorAnimator.cancel();
        }

        final @ColorInt int currentColor = mBackground.getColor();
        if (currentColor != color) {
            if (animate) {
                mAppColorAnimator = ValueAnimator.ofObject(ARGB_EVALUATOR, currentColor, color)
                        .setDuration(3000L);
                mAppColorAnimator.addUpdateListener(mAppColorAnimationListener);
                mAppColorAnimator.addListener(mAppColorAnimationListener);
                mAppColorAnimator.start();
            } else {
                setAppColor(color);
            }
        }
    }

    /**
     * Subclasses may react to the new app window color as they see fit.
     *
     * @param color the newly installed app window color
     */
    protected void onAppColorChanged(@ColorInt int color) {
        // Do nothing here, only in derived classes
    }

    private void setAppColor(@ColorInt int color) {
        mBackground.setColor(color);

        // Allow the activity and its hierarchy to react to the app window color change.
        onAppColorChanged(color);
    }

    /**
     * Alters the app window color each time the model reports a change.
     */
    private final class AppColorChangeListener implements OnAppColorChangeListener {
        @Override
        public void onAppColorChange(@ColorInt int oldColor, @ColorInt int newColor) {
            adjustAppColor(newColor, true /* animate */);
        }
    }

    /**
     * Sets the app window color to the current color produced by the animator.
     */
    private final class AppColorAnimationListener extends AnimatorListenerAdapter
            implements AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            final @ColorInt int color = (int) valueAnimator.getAnimatedValue();
            setAppColor(color);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mAppColorAnimator == animation) {
                mAppColorAnimator = null;
            }
        }
    }
}