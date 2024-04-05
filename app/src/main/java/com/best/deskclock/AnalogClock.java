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

package com.best.deskclock;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

import com.best.deskclock.alarms.AlarmActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * This widget display an analog clock with two hands for hours and minutes.
 */
public class AnalogClock extends FrameLayout {

    private final ImageView mHourHand;
    private final ImageView mMinuteHand;
    private final ImageView mSecondHand;
    private final String mDescFormat;
    private Calendar mTime;
    private TimeZone mTimeZone;
    private boolean mEnableSeconds = true;
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

    public AnalogClock(Context context) {
        this(context, null);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTime = Calendar.getInstance();
        mDescFormat = ((SimpleDateFormat) DateFormat.getTimeFormat(context)).toLocalizedPattern();

        // Get color from textColorPrimary attribute
        final TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        final int color = context instanceof AlarmActivity
                ? context.getColor(R.color.md_theme_outline)
                : context.getColor(typedValue.resourceId);

        // Must call mutate on these instances, otherwise the drawables will blur, because they're
        // sharing their size characteristics with the (smaller) world cities analog clocks.
        final ImageView dial = new AppCompatImageView(context);
        dial.setImageResource(R.drawable.clock_analog_dial);
        dial.getDrawable().mutate();
        dial.setColorFilter(color);
        addView(dial);

        mHourHand = new AppCompatImageView(context);
        mHourHand.setImageResource(R.drawable.clock_analog_hour);
        mHourHand.getDrawable().mutate();
        mHourHand.setColorFilter(color);
        addView(mHourHand);

        mMinuteHand = new AppCompatImageView(context);
        mMinuteHand.setImageResource(R.drawable.clock_analog_minute);
        mMinuteHand.getDrawable().mutate();
        mMinuteHand.setColorFilter(color);
        addView(mMinuteHand);

        mSecondHand = new AppCompatImageView(context);
        mSecondHand.setImageResource(R.drawable.clock_analog_second);
        mSecondHand.getDrawable().mutate();
        mSecondHand.setColorFilter(context.getColor(R.color.md_theme_primary));
        addView(mSecondHand);

        if (context.getClass().getSimpleName().equalsIgnoreCase(ScreensaverActivity.class.getSimpleName())) {
            dial.setColorFilter(Color.WHITE);
            mHourHand.setColorFilter(Color.WHITE);
            mMinuteHand.setColorFilter(Color.WHITE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getContext().registerReceiver(mIntentReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getContext().registerReceiver(mIntentReceiver, filter);
        }

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

    private void onTimeChanged() {
        mTime.setTimeInMillis(System.currentTimeMillis());
        final float hourAngle = mTime.get(Calendar.HOUR) * 30f;
        mHourHand.setRotation(hourAngle);
        final float minuteAngle = mTime.get(Calendar.MINUTE) * 6f;
        mMinuteHand.setRotation(minuteAngle);
        if (mEnableSeconds) {
            final float secondAngle = mTime.get(Calendar.SECOND) * 6f;
            mSecondHand.setRotation(secondAngle);
        }
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
            mSecondHand.setVisibility(VISIBLE);
            mClockTick.run();
        } else {
            mSecondHand.setVisibility(GONE);
        }
    }
}
