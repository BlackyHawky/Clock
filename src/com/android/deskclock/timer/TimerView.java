/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.deskclock.R;


public class TimerView extends LinearLayout {

    private TextView mHoursOnes, mMinutesOnes, mSeconds;
    private TextView mHoursTens, mMinutesTens;
    private final Typeface mRobotoThin;
    private Typeface mOriginalHoursTypeface;
    private final int mWhiteColor, mGrayColor;

    public TimerView(Context context) {
        this(context, null);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRobotoThin = Typeface.createFromAsset(context.getAssets(),"fonts/Roboto-Thin.ttf");
        mWhiteColor = context.getResources().getColor(R.color.clock_white);
        mGrayColor = context.getResources().getColor(R.color.clock_gray);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHoursTens = (TextView)findViewById(R.id.hours_tens);
        mMinutesTens = (TextView)findViewById(R.id.minutes_tens);
        mHoursOnes = (TextView)findViewById(R.id.hours_ones);
        mMinutesOnes = (TextView)findViewById(R.id.minutes_ones);
        mSeconds = (TextView)findViewById(R.id.seconds);
        if (mHoursOnes != null) {
            mOriginalHoursTypeface = mHoursOnes.getTypeface();
        }
        // Set the lowest time unit with thin font (excluding hundredths)
        if (mSeconds != null) {
            mSeconds.setTypeface(mRobotoThin);
        } else  {
            if (mMinutesTens != null) {
                mMinutesTens.setTypeface(mRobotoThin);
            }
            if (mMinutesOnes != null) {
                mMinutesOnes.setTypeface(mRobotoThin);
            }
        }
    }


    public void setTime(String hoursTensDigit, String hoursOnesDigit, String minutesTensDigit,
            String minutesOnesDigit, String seconds, String hundreds) {
        if (mHoursTens != null && hoursTensDigit != null) {
            mHoursTens.setText(hoursTensDigit);
            if (hoursTensDigit == "-") {
                mHoursTens.setTypeface(mRobotoThin);
                mHoursTens.setTextColor(mGrayColor);
            } else {
                mHoursTens.setTypeface(mOriginalHoursTypeface);
                mHoursTens.setTextColor(mWhiteColor);
            }
        }
        if (mHoursOnes != null && hoursOnesDigit != null) {
            mHoursOnes.setText(hoursOnesDigit);
            if (hoursOnesDigit == "-") {
                mHoursOnes.setTypeface(mRobotoThin);
                mHoursOnes.setTextColor(mGrayColor);
            } else {
                mHoursOnes.setTypeface(mOriginalHoursTypeface);
                mHoursOnes.setTextColor(mWhiteColor);
            }
        }
        if (mMinutesTens != null && minutesTensDigit != null) {
            mMinutesTens.setText(minutesTensDigit);
            if (minutesTensDigit == "-") {
                mMinutesTens.setTextColor(mGrayColor);
            } else {
                mMinutesTens.setTextColor(mWhiteColor);
            }
        }
        if (mMinutesOnes != null && minutesOnesDigit != null) {
            mMinutesOnes.setText(minutesOnesDigit);
            if (minutesOnesDigit == "-") {
                mMinutesOnes.setTextColor(mGrayColor);
            } else {
                mMinutesOnes.setTextColor(mWhiteColor);
            }
        }

        if (mSeconds != null && seconds != null) {
            mSeconds.setText(seconds);
        }
    }
}
