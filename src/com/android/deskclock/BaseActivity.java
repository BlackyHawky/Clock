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

import android.animation.ObjectAnimator;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;

/**
 * Base activity class that changes with window's background color dynamically based on the
 * current hour.
 */
public class BaseActivity extends AppCompatActivity {

    /**
     * Key used to save/restore the current background color from the saved instance state.
     */
    private static final String KEY_BACKGROUND_COLOR = "background_color";

    /**
     * Frequency to check if the background color needs to be updated.
     */
    private static final long BACKGROUND_COLOR_CHECK_DELAY_MILLIS = DateUtils.MINUTE_IN_MILLIS;

    /**
     * Duration in millis to animate changes to the background color.
     */
    private static final long BACKGROUND_COLOR_ANIMATION_DURATION = 3000L;

    /**
     * {@link Handler} used to post the {@link #mBackgroundColorChanger} runnable.
     */
    private final Handler mHandler = new Handler();

    /**
     * {@link Runnable} posted periodically to update the background color.
     */
    private final Runnable mBackgroundColorChanger = new Runnable() {
        @Override
        public void run() {
            setBackgroundColor(Utils.getCurrentHourColor(), true /* animate */);
            mHandler.postDelayed(this, BACKGROUND_COLOR_CHECK_DELAY_MILLIS);
        }
    };

    /**
     * {@link ColorDrawable} used to draw the window's background.
     */
    private ColorDrawable mBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int backgroundColor = savedInstanceState == null ? Utils.getCurrentHourColor()
                : savedInstanceState.getInt(KEY_BACKGROUND_COLOR, Utils.getCurrentHourColor());
        setBackgroundColor(backgroundColor, false /* animate */);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the current background color periodically.
        mHandler.post(mBackgroundColorChanger);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop updating the background color when not active.
        mHandler.removeCallbacks(mBackgroundColorChanger);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the background color so we can animate the change when the activity is restored.
        outState.putInt(KEY_BACKGROUND_COLOR, mBackground.getColor());
    }

    /**
     * Sets the current background color to the provided value and animates the change if desired.
     *
     * @param color the ARGB value to set as the current background color
     * @param animate {@code true} if the change should be animated
     */
    protected void setBackgroundColor(int color, boolean animate) {
        if (mBackground == null) {
            mBackground = new ColorDrawable(color);
            getWindow().setBackgroundDrawable(mBackground);
        }

        if (mBackground.getColor() != color) {
            if (animate) {
                ObjectAnimator.ofObject(mBackground, "color", AnimatorUtils.ARGB_EVALUATOR, color)
                        .setDuration(BACKGROUND_COLOR_ANIMATION_DURATION)
                        .start();
            } else {
                mBackground.setColor(color);
            }
        }
    }
}
