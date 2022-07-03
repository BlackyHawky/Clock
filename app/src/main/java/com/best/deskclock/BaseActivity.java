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

package com.best.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity class that changes the app window's color based on the current hour.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /**
     * Sets the app window color on each frame of the {@link #mAppColorAnimator}.
     */
    private final AppColorAnimationListener mAppColorAnimationListener
            = new AppColorAnimationListener();

    /**
     * The current animator that is changing the app window color or {@code null}.
     */
    private ValueAnimator mAppColorAnimator;

    /**
     * Draws the app window's color.
     */
    private ColorDrawable mBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final @ColorInt int color = ThemeUtils.resolveColor(this, android.R.attr.colorBackground);
        adjustAppColor(color  /* animate */);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Ensure the app window color is up-to-date.
        final @ColorInt int color = ThemeUtils.resolveColor(this, android.R.attr.colorBackground);
        adjustAppColor(color  /* animate */);
    }

    /**
     * Adjusts the current app window color of this activity; animates the change if desired.
     *
     * @param color the ARGB value to set as the current app window color
     */
    protected void adjustAppColor(@ColorInt int color) {
        // Create and install the drawable that defines the window color.
        if (mBackground == null) {
            mBackground = new ColorDrawable(color);
            // getWindow().setBackgroundDrawable(mBackground);
        }

        // Cancel the current window color animation if one exists.
        if (mAppColorAnimator != null) {
            mAppColorAnimator.cancel();
        }

        final @ColorInt int currentColor = mBackground.getColor();
        if (currentColor != color) {
            setAppColor(color);
        }
    }

    private void setAppColor(@ColorInt int color) {
        mBackground.setColor(color);
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
