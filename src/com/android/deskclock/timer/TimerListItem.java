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
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.deskclock.CircleTimerView;
import com.android.deskclock.R;


public class TimerListItem extends LinearLayout {

    CountingTimerView mTimerText;
    CircleTimerView mCircleView;

    long mTimerLength;

    public TimerListItem(Context context) {
        this(context, null);
    }

    public TimerListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater layoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.timer_list_item, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTimerText = (CountingTimerView)findViewById(R.id.timer_time_text);
        mCircleView = (CircleTimerView)findViewById(R.id.timer_time);
        mCircleView.setTimerMode(true);
    }

    public void set(long timerLength, long timeLeft, boolean drawRed) {
        if (mCircleView == null) {
            mCircleView = (CircleTimerView)findViewById(R.id.timer_time);
            mCircleView.setTimerMode(true);
        }
        mTimerLength = timerLength;
        mCircleView.setIntervalTime(mTimerLength);
        mCircleView.setPassedTime(timerLength - timeLeft, drawRed);
        invalidate();
    }

    public void start() {
        mCircleView.startIntervalAnimation();
        mTimerText.blinkTimeStr(false);
    }

    public void pause() {
        mCircleView.pauseIntervalAnimation();
        mTimerText.blinkTimeStr(true);
    }

    public void stop() {
        mCircleView.stopIntervalAnimation();
        mTimerText.blinkTimeStr(false);
    }

    public void setLength(long timerLength) {
        mTimerLength = timerLength;
        mCircleView.setIntervalTime(mTimerLength);
        mCircleView.invalidate();
    }

    public void setBlink(boolean blink) {
        mTimerText.blinkTimeStr(blink);
    }

    public void setTime(long time, boolean forceUpdate) {
        if (mTimerText == null) {
            mTimerText = (CountingTimerView)findViewById(R.id.timer_time_text);
        }
        mTimerText.setTime(time, false, forceUpdate);
    }
}
