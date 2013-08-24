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
 * limitations under the License
 */

package com.android.deskclock.timer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.deskclock.R;


public class TimerView extends LinearLayout {

    private TextView mHoursOnes, mMinutesOnes;
    private TextView mHoursTens, mMinutesTens;
    private TextView mSeconds;
    private final Typeface mAndroidClockMonoThin;
    private Typeface mOriginalHoursTypeface;
    private Typeface mOriginalMinutesTypeface;
    private final int mWhiteColor, mGrayColor;

    @SuppressWarnings("unused")
    public TimerView(Context context) {
        this(context, null);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAndroidClockMonoThin =
                Typeface.createFromAsset(context.getAssets(), "fonts/AndroidClockMono-Thin.ttf");

        Resources resources = context.getResources();
        mWhiteColor = resources.getColor(R.color.clock_white);
        mGrayColor = resources.getColor(R.color.clock_gray);
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
        if (mMinutesOnes != null) {
            mOriginalMinutesTypeface = mMinutesOnes.getTypeface();
        }
    }


    public void setTime(int hoursTensDigit, int hoursOnesDigit, int minutesTensDigit,
            int minutesOnesDigit, int seconds) {
        if (mHoursTens != null) {
            // Hide digit
            if (hoursTensDigit == -2) {
                mHoursTens.setVisibility(View.INVISIBLE);
            } else if (hoursTensDigit == -1) {
                mHoursTens.setText("-");
                mHoursTens.setTypeface(mAndroidClockMonoThin);
                mHoursTens.setTextColor(mGrayColor);
                mHoursTens.setVisibility(View.VISIBLE);
            } else {
                mHoursTens.setText(String.format("%d",hoursTensDigit));
                mHoursTens.setTypeface(mOriginalHoursTypeface);
                mHoursTens.setTextColor(mWhiteColor);
                mHoursTens.setVisibility(View.VISIBLE);
            }
        }
        if (mHoursOnes != null) {
            if (hoursOnesDigit == -1) {
                mHoursOnes.setText("-");
                mHoursOnes.setTypeface(mAndroidClockMonoThin);
                mHoursOnes.setTextColor(mGrayColor);
            } else {
                mHoursOnes.setText(String.format("%d", hoursOnesDigit));
                mHoursOnes.setTypeface(mOriginalHoursTypeface);
                mHoursOnes.setTextColor(mWhiteColor);
            }
        }

        if (mMinutesTens != null) {
            if (minutesTensDigit == -1) {
                mMinutesTens.setText("-");
                mMinutesTens.setTypeface(mAndroidClockMonoThin);
                mMinutesTens.setTextColor(mGrayColor);
            } else {
                mMinutesTens.setText(String.format("%d", minutesTensDigit));
                mMinutesTens.setTypeface(mOriginalMinutesTypeface);
                mMinutesTens.setTextColor(mWhiteColor);
            }
        }
        if (mMinutesOnes != null) {
            if (minutesOnesDigit == -1) {
                mMinutesOnes.setText("-");
                mMinutesOnes.setTypeface(mAndroidClockMonoThin);
                mMinutesOnes.setTextColor(mGrayColor);
            } else {
                mMinutesOnes.setText(String.format("%d", minutesOnesDigit));
                mMinutesOnes.setTypeface(mOriginalMinutesTypeface);
                mMinutesOnes.setTextColor(mWhiteColor);
            }
        }

        if (mSeconds != null) {
            mSeconds.setText(String.format("%02d", seconds));
        }
    }
}
