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

package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * This widget display an analog clock with two hands for hours and minutes.
 */
public class AnalogClock extends View {

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                final String tz = intent.getStringExtra("time-zone");
                mTime = Calendar.getInstance(TimeZone.getTimeZone(tz));
            }
            onTimeChanged();
        }
    };

    private final Runnable mClockTick = new Runnable() {
        @Override
        public void run() {
            onTimeChanged();

            if (mEnableSeconds) {
                final long now = System.currentTimeMillis();
                final long delay = SECOND_IN_MILLIS - now % SECOND_IN_MILLIS;
                postDelayed(this, delay);
            }
        }
    };

    private final Drawable mDial;
    private final Drawable mHourHand;
    private final Drawable mMinuteHand;
    private final Drawable mSecondHand;

    private Calendar mTime;
    private String mDescFormat;
    private TimeZone mTimeZone;
    private boolean mEnableSeconds = true;

    public AnalogClock(Context context) {
        this(context, null /* attrs */);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0 /* defStyleAttr */);
    }

    public AnalogClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTime = Calendar.getInstance();
        mDescFormat = ((SimpleDateFormat) DateFormat.getTimeFormat(context)).toLocalizedPattern();

        mDial = initDrawable(context, R.drawable.clock_analog_dial);
        mHourHand = initDrawable(context, R.drawable.clock_analog_hour);
        mMinuteHand = initDrawable(context, R.drawable.clock_analog_minute);
        mSecondHand = initDrawable(context, R.drawable.clock_analog_second);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(mIntentReceiver, filter);

        // Refresh the calendar instance since the time zone may have changed while the receiver
        // wasn't registered.
        mTime = Calendar.getInstance(mTimeZone != null ? mTimeZone : TimeZone.getDefault());
        onTimeChanged();

        // Tick every second.
        if (mEnableSeconds) {
            mClockTick.run();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        getContext().unregisterReceiver(mIntentReceiver);
        removeCallbacks(mClockTick);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int minWidth = Math.max(mDial.getIntrinsicWidth(), getSuggestedMinimumWidth());
        final int minHeight = Math.max(mDial.getIntrinsicHeight(), getSuggestedMinimumHeight());
        setMeasuredDimension(getDefaultSize(minWidth, widthMeasureSpec),
                getDefaultSize(minHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int w = getWidth();
        final int h = getHeight();

        final int saveCount = canvas.save();

        // Center the canvas at the mid-point.
        canvas.translate(w / 2, h / 2);

        // Scale down the clock if necessary.
        final float scale = Math.min((float) w / mDial.getIntrinsicWidth(),
                (float) h / mDial.getIntrinsicHeight());
        if (scale < 1f) {
            canvas.scale(scale, scale, 0f, 0f);
        }

        mDial.draw(canvas);

        final float hourAngle = mTime.get(Calendar.HOUR) * 30f;
        canvas.rotate(hourAngle, 0f, 0f);
        mHourHand.draw(canvas);

        final float minuteAngle = mTime.get(Calendar.MINUTE) * 6f;
        canvas.rotate(minuteAngle - hourAngle, 0f, 0f);
        mMinuteHand.draw(canvas);

        if (mEnableSeconds) {
            final float secondAngle = mTime.get(Calendar.SECOND) * 6f;
            canvas.rotate(secondAngle - minuteAngle, 0f, 0f);
            mSecondHand.draw(canvas);
        }

        canvas.restoreToCount(saveCount);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return mDial == who
                || mHourHand == who
                || mMinuteHand == who
                || mSecondHand == who
                || super.verifyDrawable(who);
    }

    private Drawable initDrawable(Context context, @DrawableRes int id) {
        final Drawable d = Utils.getVectorDrawable(context, id);

        // Center the drawable using its bounds.
        final int midX = d.getIntrinsicWidth() / 2;
        final int midY = d.getIntrinsicHeight() / 2;
        d.setBounds(-midX, -midY, midX, midY);

        // Register callback to support non-bitmap drawables.
        d.setCallback(this);

        return d;
    }

    private void onTimeChanged() {
        mTime.setTimeInMillis(System.currentTimeMillis());
        setContentDescription(DateFormat.format(mDescFormat, mTime));
        invalidate();
    }

    public void setTimeZone(String id) {
        mTimeZone = TimeZone.getTimeZone(id);
        mTime.setTimeZone(mTimeZone);
        onTimeChanged();
    }

    public void enableSeconds(boolean enable) {
        mEnableSeconds = enable;
        if (mEnableSeconds) {
            mClockTick.run();
        }
    }
}
