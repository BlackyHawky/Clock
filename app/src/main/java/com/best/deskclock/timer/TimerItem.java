/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.best.deskclock.timer;

import static android.R.attr.state_activated;
import static android.R.attr.state_pressed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;

import com.best.deskclock.R;
import com.best.deskclock.ThemeUtils;
import com.best.deskclock.TimerTextController;
import com.best.deskclock.Utils.ClickAccessibilityDelegate;
import com.best.deskclock.data.Timer;

/**
 * This view is a visual representation of a {@link Timer}.
 */
public class TimerItem extends ConstraintLayout {

    /**
     * Displays the remaining time or time since expiration.
     */
    private TextView mTimerText;

    /**
     * Formats and displays the text in the timer.
     */
    private TimerTextController mTimerTextController;

    /**
     * Displays timer progress as a color circle that changes from white to red.
     */
    private TimerCircleView mCircleView;

    /**
     * A button that either resets the timer or adds time to it, depending on its state.
     */
    private Button mResetAddButton;

    /**
     * Displays the label associated with the timer. Tapping it presents an edit dialog.
     */
    private TextView mLabelView;

    /**
     * The last state of the timer that was rendered; used to avoid expensive operations.
     */
    private Timer.State mLastState;

    public TimerItem(Context context) {
        this(context, null);
    }

    public TimerItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLabelView = findViewById(R.id.timer_label);
        mResetAddButton = findViewById(R.id.reset_add);
        mCircleView = findViewById(R.id.timer_time);
        mTimerText = findViewById(R.id.timer_time_text);
        mTimerTextController = new TimerTextController(mTimerText);

        final Context c = mTimerText.getContext();
        final int colorAccent = ThemeUtils.resolveColor(c, androidx.appcompat.R.attr.colorAccent);
        final int textColorPrimary = ThemeUtils.resolveColor(c, android.R.attr.textColorPrimary);
        mTimerText.setTextColor(new ColorStateList(
                new int[][]{{-state_activated, -state_pressed}, {}},
                new int[]{textColorPrimary, colorAccent}));
    }

    /**
     * Updates this view to display the latest state of the {@code timer}.
     */
    void update(Timer timer) {
        // Update the time.
        mTimerTextController.setTimeString(timer.getRemainingTime());

        // Update the label if it changed.
        final String label = timer.getLabel();
        if (!TextUtils.equals(label, mLabelView.getText())) {
            mLabelView.setText(label);
        }

        // Update visibility of things that may blink.
        final boolean blinkOff = SystemClock.elapsedRealtime() % 1000 < 500;
        if (mCircleView != null) {
            final boolean hideCircle = (timer.isExpired() || timer.isMissed()) && blinkOff;
            mCircleView.setVisibility(hideCircle ? INVISIBLE : VISIBLE);

            if (!hideCircle) {
                // Update the progress of the circle.
                mCircleView.update(timer);
            }
        }
        if (!timer.isPaused() || !blinkOff || mTimerText.isPressed()) {
            mTimerText.setAlpha(1f);
        } else {
            mTimerText.setAlpha(0f);
        }

        // Update some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState) {
            mResetAddButton.setVisibility(View.VISIBLE);
            mLastState = timer.getState();
            final Context context = getContext();
            switch (mLastState) {
                case RESET:
                    mResetAddButton.setVisibility(View.GONE);
                    break;
                case PAUSED: {
                    mResetAddButton.setText(R.string.timer_reset);
                    mResetAddButton.setContentDescription(null);
                    mTimerText.setClickable(true);
                    mTimerText.setActivated(false);
                    mTimerText.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
                    ViewCompat.setAccessibilityDelegate(mTimerText, new ClickAccessibilityDelegate(
                            context.getString(R.string.timer_start), true));
                    break;
                }
                case RUNNING: {
                    final String addTimeDesc = context.getString(R.string.timer_plus_one);
                    mResetAddButton.setText(R.string.timer_add_minute);
                    mResetAddButton.setContentDescription(addTimeDesc);
                    mTimerText.setClickable(true);
                    mTimerText.setActivated(false);
                    mTimerText.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
                    ViewCompat.setAccessibilityDelegate(mTimerText, new ClickAccessibilityDelegate(
                            context.getString(R.string.timer_pause)));
                    break;
                }
                case EXPIRED:
                case MISSED: {
                    final String addTimeDesc = context.getString(R.string.timer_plus_one);
                    mResetAddButton.setText(R.string.timer_add_minute);
                    mResetAddButton.setContentDescription(addTimeDesc);
                    mTimerText.setClickable(false);
                    mTimerText.setActivated(true);
                    mTimerText.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
                    break;
                }
            }
        }
    }
}