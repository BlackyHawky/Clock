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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.android.deskclock.R;


public class CountingTimerView extends View {
    private static final String TWO_DIGITS = "%02d";
    private static final String ONE_DIGIT = "%01d";
    private static final String NEG_TWO_DIGITS = "-%02d";
    private static final String NEG_ONE_DIGIT = "-%01d";
    private static final float TEXT_SIZE_TO_WIDTH_RATIO = 0.7f;


    private String mHours, mMinutes, mSeconds, mHunderdths;
    private final String mHoursLabel, mMinutesLabel, mSecondsLabel;
    private float mHoursWidth, mMinutesWidth, mSecondsWidth, mHundredthsWidth;
    private float mHoursLabelWidth, mMinutesLabelWidth, mSecondsLabelWidth, mHundredthsSepWidth;

    private boolean mShowTimeStr = true;
    private final Typeface mRobotoThin, mRobotoBold, mRobotoLabel;
    private final Paint mPaintBig = new Paint();
    private final Paint mPaintBigThin = new Paint();
    private final Paint mPaintMed = new Paint();
    private final Paint mPaintLabel = new Paint();
    private float mHalfTextHeight = 0;
    private float mTotalTextWidth;
    private static final String HUNDREDTH_SEPERATOR = ".";
    private boolean mRemeasureText = true;

    Runnable mBlinkThread = new Runnable() {
        @Override
        public void run() {
            mShowTimeStr = !mShowTimeStr;
            CountingTimerView.this.setVisibility(mShowTimeStr ? View.VISIBLE : View.INVISIBLE);
            CountingTimerView.this.invalidate();
            mRemeasureText = true;
            postDelayed(mBlinkThread, 500);
        }

    };


    public CountingTimerView(Context context) {
        this(context, null);
    }

    public CountingTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRobotoThin = Typeface.createFromAsset(context.getAssets(),"fonts/Roboto-Thin.ttf");
        mRobotoBold = Typeface.create("sans-serif", Typeface.BOLD);
        mRobotoLabel= Typeface.create("sans-serif-condensed", Typeface.BOLD);
        Resources r = context.getResources();
        mHoursLabel = r.getString(R.string.hours_label).toUpperCase();
        mMinutesLabel = r.getString(R.string.minutes_label).toUpperCase();
        mSecondsLabel = r.getString(R.string.seconds_label).toUpperCase();

        mPaintBig.setAntiAlias(true);
        mPaintBig.setStyle(Paint.Style.STROKE);
        mPaintBig.setColor(r.getColor(R.color.clock_white));
        mPaintBig.setTextAlign(Paint.Align.LEFT);
        mPaintBig.setTypeface(mRobotoBold);
        mPaintBig.setTextSize(r.getDimension(R.dimen.big_font_size));
        mHalfTextHeight = 60;

        mPaintBigThin.setAntiAlias(true);
        mPaintBigThin.setStyle(Paint.Style.STROKE);
        mPaintBigThin.setColor(r.getColor(R.color.clock_white));
        mPaintBigThin.setTextAlign(Paint.Align.LEFT);
        mPaintBigThin.setTypeface(mRobotoThin);
        mPaintBigThin.setTextSize(r.getDimension(R.dimen.big_font_size));

        mPaintMed.setAntiAlias(true);
        mPaintMed.setStyle(Paint.Style.STROKE);
        mPaintMed.setColor(r.getColor(R.color.clock_white));
        mPaintMed.setTextAlign(Paint.Align.LEFT);
        mPaintMed.setTypeface(mRobotoThin);
        mPaintMed.setTextSize(r.getDimension(R.dimen.small_font_size));

        mPaintLabel.setAntiAlias(true);
        mPaintLabel.setStyle(Paint.Style.STROKE);
        mPaintLabel.setColor(r.getColor(R.color.clock_white));
        mPaintLabel.setTextAlign(Paint.Align.LEFT);
        mPaintLabel.setTypeface(mRobotoLabel);
        mPaintLabel.setTextSize(r.getDimension(R.dimen.label_font_size));

    }

    public void setTime(long time, boolean showHundredths) {
        boolean neg = false;
        String format = null;
        if (time < 0) {
            time = -time;
            neg = true;
        }
        long hundreds, seconds, minutes, hours;
        seconds = time / 1000;
        hundreds = (time - seconds * 1000) / 10;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 99) {
            hours = 0;
        }
        // time may less than a second below zero, since we do not show fractions of seconds
        // when counting down, do not show the minus sign.
        if (hours ==0 && minutes == 0 && seconds == 0) {
            neg = false;
        }
        // TODO: must build to account for localization
        if (!showHundredths) {
            if (!neg && hundreds != 0) {
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
            if (hundreds < 10 || hundreds > 90) {
                update = true;
            }
        }

        if (hours >= 10) {
            format = neg ? NEG_TWO_DIGITS : TWO_DIGITS;
            mHours = String.format(format, hours);
        } else if (hours > 0) {
            format = neg ? NEG_ONE_DIGIT : ONE_DIGIT;
            mHours = String.format(format, hours);
        } else {
            mHours = null;
        }

        if (minutes >= 10 || hours > 0) {
            format = (neg && hours == 0) ? NEG_TWO_DIGITS : TWO_DIGITS;
            mMinutes = String.format(format, minutes);
        } else {
            format = (neg && hours == 0) ? NEG_ONE_DIGIT : ONE_DIGIT;
            mMinutes = String.format(format, minutes);
        }

        mSeconds = String.format(TWO_DIGITS, seconds);
        if (showHundredths) {
            mHunderdths = String.format(TWO_DIGITS, hundreds);
        } else {
            mHunderdths = null;
        }
        mRemeasureText = true;
        invalidate();
    }
    private void setTotalTextWidth() {
        mTotalTextWidth = 0;
        if (mHours != null) {
            mHoursWidth = mPaintBig.measureText(mHours);
            mTotalTextWidth += mHoursWidth;
            mHoursLabelWidth = mPaintLabel.measureText(mHoursLabel);
            mTotalTextWidth += mHoursLabelWidth;
        }
        if (mMinutes != null) {
            mMinutesWidth =  mPaintBig.measureText(mMinutes);
            mTotalTextWidth += mMinutesWidth;
            mMinutesLabelWidth = mPaintLabel.measureText(mMinutesLabel);
            mTotalTextWidth += mMinutesLabelWidth;
        }
        if (mSeconds != null) {
            mSecondsWidth = mPaintBigThin.measureText(mSeconds);
            mTotalTextWidth += mSecondsWidth;
            mSecondsLabelWidth = mPaintLabel.measureText(mSecondsLabel);
            mTotalTextWidth += mSecondsLabelWidth;
        }
        if (mHunderdths != null) {
            mHundredthsWidth = mPaintMed.measureText(mHunderdths);
            mTotalTextWidth += mHundredthsWidth;
            mHundredthsSepWidth = mPaintLabel.measureText(HUNDREDTH_SEPERATOR);
            mTotalTextWidth += mHundredthsSepWidth;
        }

        // This is a hack: if the text is too wide, reduce all the paint text sizes
        int width = getWidth();
        if (width != 0) {
            float ratio = mTotalTextWidth / width;
            if (ratio > TEXT_SIZE_TO_WIDTH_RATIO) {
                float sizeRatio = (TEXT_SIZE_TO_WIDTH_RATIO / ratio);
                mPaintBig.setTextSize( mPaintBig.getTextSize() * sizeRatio);
                mPaintBigThin.setTextSize( mPaintBigThin.getTextSize() * sizeRatio);
                mPaintMed.setTextSize( mPaintMed.getTextSize() * sizeRatio);
                mPaintLabel.setTextSize( mPaintLabel.getTextSize() * sizeRatio);
                mTotalTextWidth *= sizeRatio;
                mMinutesWidth *= sizeRatio;
                mHoursWidth *= sizeRatio;
                mSecondsWidth *= sizeRatio;
                mHundredthsWidth *= sizeRatio;
                mHoursLabelWidth *= sizeRatio;
                mMinutesLabelWidth *= sizeRatio;
                mSecondsLabelWidth *= sizeRatio;
                mHundredthsSepWidth *= sizeRatio;
            }
        }
    }

    public void setTime(String hours, String minutes, String seconds, String hundreds) {
            mHours = hours;
            mMinutes = minutes;
            mSeconds = seconds;
            mHunderdths = hundreds;
            mRemeasureText = true;
            invalidate();
    }

    public void blinkTimeStr(boolean blink) {
        if (blink) {
            removeCallbacks(mBlinkThread);
            postDelayed(mBlinkThread, 1000);
        } else {
            removeCallbacks(mBlinkThread);
            mShowTimeStr = true;
            this.setVisibility(View.VISIBLE);
        }
    }

    public String getTimeString() {
        if (mHours == null) {
            return String.format("%s:%s.%s",mMinutes, mSeconds,  mHunderdths);
        }
        return String.format("%s:%s:%s.%s",mHours, mMinutes, mSeconds,  mHunderdths);
    }

    @Override
    public void onDraw(Canvas canvas) {

        int width = getWidth();
        if (mRemeasureText && width != 0) {
            setTotalTextWidth();
            mRemeasureText = false;
        }

        int xCenter = width / 2;
        int yCenter = getHeight() / 2;

        float textXstart = xCenter - mTotalTextWidth / 2;
        float textYstart = yCenter + mHalfTextHeight;
        if (mHours != null) {
            canvas.drawText(mHours, textXstart, textYstart, mPaintBig);
            textXstart += mHoursWidth;
            canvas.drawText(mHoursLabel, textXstart, textYstart, mPaintLabel);
            textXstart += mHoursLabelWidth;
        }
        if (mMinutes != null) {
            canvas.drawText(mMinutes, textXstart, textYstart, mPaintBig);
            textXstart += mMinutesWidth;
            canvas.drawText(mMinutesLabel, textXstart, textYstart, mPaintLabel);
            textXstart += mMinutesLabelWidth;
        }
        if (mSeconds != null) {
            canvas.drawText(mSeconds, textXstart, textYstart, mPaintBigThin);
            textXstart += mSecondsWidth;
            canvas.drawText(mSecondsLabel, textXstart, textYstart, mPaintLabel);
            textXstart += mSecondsLabelWidth;
        }
        if (mHunderdths != null) {
            canvas.drawText(HUNDREDTH_SEPERATOR, textXstart, textYstart, mPaintLabel);
            textXstart += mHundredthsSepWidth;
            canvas.drawText(mHunderdths, textXstart, textYstart, mPaintMed);
        }
    }
}
