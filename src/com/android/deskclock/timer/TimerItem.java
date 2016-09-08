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

package com.android.deskclock.timer;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;
import android.support.annotation.ColorRes;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.deskclock.R;
import com.android.deskclock.TimerTextView;
import com.android.deskclock.data.Timer;

import static com.android.deskclock.data.Timer.State.RUNNING;

/**
 * This view is a visual representation of a {@link Timer}.
 */
public class TimerItem extends LinearLayout {

    /** Displays the remaining time or time since expiration. */
    private TimerTextView mTimerText;

    /** Displays timer progress as a color circle that changes from white to red. */
    private TimerCircleView mCircleView;

    /** A button that either resets the timer or adds time to it, depending on its state. */
    private Button mResetAddButton;

    /** Displays the label associated with the timer. Tapping it presents an edit dialog. */
    private TextView mLabelView;

    /** The last state of the timer that was rendered; used to avoid expensive operations. */
    private Timer.State mLastState;

    private long mLastTime = Long.MIN_VALUE;

    @ColorRes private final int mWhite = R.color.white;
    @ColorRes private int mPrimaryColor = R.color.color_accent;

    public TimerItem(Context context) {
        this(context, null);
    }

    public TimerItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLabelView = (TextView) findViewById(R.id.timer_label);
        mResetAddButton = (Button) findViewById(R.id.reset_add);
        mCircleView = (TimerCircleView) findViewById(R.id.timer_time);
        mTimerText = (TimerTextView) findViewById(R.id.timer_time_text);
    }

    void setTimeString(long remainingTime) {
        // No updates necessary
        if (Math.abs(mLastTime - remainingTime) < 1000) {
            return;
        }
        mLastTime = remainingTime;

        boolean neg = false;
        if (remainingTime < 0) {
            remainingTime = -remainingTime;
            neg = true;
        }

        int hours = (int) (remainingTime / DateUtils.HOUR_IN_MILLIS);
        int remainder = (int) (remainingTime % DateUtils.HOUR_IN_MILLIS);

        int minutes = (int) (remainder / DateUtils.MINUTE_IN_MILLIS);
        remainder = (int) (remainder % DateUtils.MINUTE_IN_MILLIS);

        int seconds = (int) (remainder / DateUtils.SECOND_IN_MILLIS);
        remainder = (int) (remainder % DateUtils.SECOND_IN_MILLIS);

        final StringBuilder time = new StringBuilder();

        if (!neg && remainder != 0) {
            seconds++;
            if (seconds == 60) {
                seconds = 0;
                minutes++;
                if (minutes == 60) {
                    minutes = 0;
                    hours++;
                }
            }
        }
        if (neg && !(hours == 0 && minutes == 0 && seconds == 0)) {
            time.append('-');
        }

        time.append(getTimeString(hours, minutes, seconds));

        mTimerText.setText(time.toString());
    }

    private String getTimeString(int hours, int minutes, int seconds) {
        final Resources r = getResources();
        if (hours != 0) {
            return r.getString(R.string.hours_minutes_seconds, hours, minutes, seconds);
        }
        if (minutes != 0) {
            return r.getString(R.string.minutes_seconds, minutes, seconds);
        }
        return r.getString(R.string.seconds, seconds);
    }

    /**
     * Updates this view to display the latest state of the {@code timer}.
     */
    void update(Timer timer) {
        // Update the time.
        setTimeString(timer.getRemainingTime());

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
        if (!timer.isPaused() || !blinkOff) {
            mTimerText.setVisibility(VISIBLE);
        } else {
            mTimerText.setVisibility(GONE);
        }

        // Update some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState) {
            mLastState = timer.getState();
            switch (mLastState) {
                case RESET:
                case PAUSED: {
                    mResetAddButton.setText(R.string.timer_reset);
                    mTimerText.setTextColor(getResources().getColor(mWhite));
                    break;
                }
                case RUNNING:
                case EXPIRED:
                case MISSED: {
                    final String addTimeDesc = getResources().getString(R.string.timer_plus_one);
                    mResetAddButton.setText(R.string.timer_add_minute);
                    mResetAddButton.setContentDescription(addTimeDesc);
                    final int color = mLastState == RUNNING ? mWhite : mPrimaryColor;
                    mTimerText.setTextColor(getResources().getColor(color));
                    break;
                }
            }
        }
    }
}