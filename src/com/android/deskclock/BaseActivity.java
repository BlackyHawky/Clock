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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

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
     * Duration in millis to animate changes to the background color.
     */
    private static final long BACKGROUND_COLOR_ANIMATION_DURATION = 3000L;

    /**
     * {@link BroadcastReceiver} to update the background color whenever the system time changes.
     */
    private BroadcastReceiver mOnTimeChangedReceiver;

    /**
     * {@link ColorDrawable} used to draw the window's background.
     */
    private ColorDrawable mBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int currentColor = Utils.getCurrentHourColor();
        final int backgroundColor = savedInstanceState == null ? currentColor
                : savedInstanceState.getInt(KEY_BACKGROUND_COLOR, currentColor);
        setBackgroundColor(backgroundColor, false /* animate */);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register mOnTimeChangedReceiver to update current background color periodically.
        if (mOnTimeChangedReceiver == null) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            registerReceiver(mOnTimeChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    setBackgroundColor(Utils.getCurrentHourColor(), true /* animate */);
                }
            }, filter);
        }

        // Ensure the background color is up-to-date.
        setBackgroundColor(Utils.getCurrentHourColor(), true /* animate */);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop updating the background color when not active.
        if (mOnTimeChangedReceiver != null) {
            unregisterReceiver(mOnTimeChangedReceiver);
            mOnTimeChangedReceiver = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the background color so we can animate the change when the activity is restored.
        if (mBackground != null) {
            outState.putInt(KEY_BACKGROUND_COLOR, mBackground.getColor());
        }
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
