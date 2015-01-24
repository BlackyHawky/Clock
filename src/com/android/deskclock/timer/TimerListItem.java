/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.deskclock.CircleTimerView;
import com.android.deskclock.R;


public class TimerListItem extends LinearLayout {

    CountingTimerView mTimerText;
    CircleTimerView mCircleView;
    ImageView mResetAddButton;

    long mTimerLength;

    public TimerListItem(Context context) {
        this(context, null);
    }

    public TimerListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTimerText = (CountingTimerView) findViewById(R.id.timer_time_text);
        mCircleView = (CircleTimerView) findViewById(R.id.timer_time);
        mResetAddButton = (ImageView) findViewById(R.id.reset_add);
        mCircleView.setTimerMode(true);
    }

    public void set(long timerLength, long timeLeft, boolean drawRed) {
        if (mCircleView == null) {
            mCircleView = (CircleTimerView) findViewById(R.id.timer_time);
            mCircleView.setTimerMode(true);
        }
        mTimerLength = timerLength;
        mCircleView.setIntervalTime(mTimerLength);
        mCircleView.setPassedTime(timerLength - timeLeft, drawRed);
        invalidate();
    }

    public void start() {
        mResetAddButton.setImageResource(R.drawable.ic_plusone);
        mResetAddButton.setContentDescription(getResources().getString(R.string.timer_plus_one));
        mCircleView.startIntervalAnimation();
        mTimerText.setTimeStrTextColor(false, true);
        mTimerText.showTime(true);
        mCircleView.setVisibility(VISIBLE);
    }

    public void pause() {
        mResetAddButton.setImageResource(R.drawable.ic_reset);
        mResetAddButton.setContentDescription(getResources().getString(R.string.timer_reset));
        mCircleView.pauseIntervalAnimation();
        mTimerText.setTimeStrTextColor(false, true);
        mTimerText.showTime(true);
        mCircleView.setVisibility(VISIBLE);
    }

    public void stop() {
        mCircleView.stopIntervalAnimation();
        mTimerText.setTimeStrTextColor(false, true);
        mTimerText.showTime(true);
        mCircleView.setVisibility(VISIBLE);
    }

    public void timesUp() {
        mCircleView.abortIntervalAnimation();
        mTimerText.setTimeStrTextColor(true, true);
    }

    public void done() {
        mCircleView.stopIntervalAnimation();
        mCircleView.setVisibility(VISIBLE);
        mCircleView.invalidate();
        mTimerText.setTimeStrTextColor(true, false);
    }

    public void setLength(long timerLength) {
        mTimerLength = timerLength;
        mCircleView.setIntervalTime(mTimerLength);
        mCircleView.invalidate();
    }

    public void setTextBlink(boolean blink) {
        mTimerText.showTime(!blink);
    }

    public void setCircleBlink(boolean blink) {
        mCircleView.setVisibility(blink ? INVISIBLE : VISIBLE);
    }

    public void setResetAddButton(boolean isRunning, OnClickListener listener) {
        if (mResetAddButton == null) {
            mResetAddButton = (ImageView) findViewById(R.id.reset_add);
        }
        mResetAddButton.setImageResource(isRunning ? R.drawable.ic_plusone :
                R.drawable.ic_reset);
        mResetAddButton.setContentDescription(getResources().getString(
                isRunning ? R.string.timer_plus_one : R.string.timer_reset));
        mResetAddButton.setOnClickListener(listener);
    }

    public void setTime(long time, boolean forceUpdate) {
        if (mTimerText == null) {
            mTimerText = (CountingTimerView) findViewById(R.id.timer_time_text);
        }
        mTimerText.setTime(time, false, forceUpdate);
    }
}
