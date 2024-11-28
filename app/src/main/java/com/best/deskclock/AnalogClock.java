/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLACK_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLACK_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLUE_GRAY_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLUE_GRAY_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BROWN_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BROWN_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.GREEN_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.GREEN_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.INDIGO_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.INDIGO_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.ORANGE_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.ORANGE_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PINK_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PINK_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PURPLE_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PURPLE_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.RED_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.RED_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.YELLOW_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.YELLOW_NIGHT_ACCENT_COLOR;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

import com.best.deskclock.alarms.AlarmActivity;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.settings.AlarmDisplayPreviewActivity;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

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

        final int alarmClockColor = DataModel.getDataModel().getAlarmClockColor();
        final int alarmSecondsHandColor = DataModel.getDataModel().getAlarmSecondsHandColor();
        final int clockColor = context instanceof AlarmActivity || context instanceof AlarmDisplayPreviewActivity
                ? alarmClockColor
                : MaterialColors.getColor(context, android.R.attr.textColorPrimary, Color.BLACK);

        // Must call mutate on these instances, otherwise the drawables will blur, because they're
        // sharing their size characteristics with the (smaller) world cities analog clocks.
        final ImageView dial = new AppCompatImageView(context);
        dial.setImageResource(R.drawable.clock_analog_dial);
        dial.getDrawable().mutate();
        if (!(context instanceof ScreensaverActivity)) {
            dial.setColorFilter(clockColor);
        }
        addView(dial);

        mHourHand = new AppCompatImageView(context);
        mHourHand.setImageResource(R.drawable.clock_analog_hour);
        mHourHand.getDrawable().mutate();
        if (!(context instanceof ScreensaverActivity)) {
            mHourHand.setColorFilter(clockColor);
        }
        addView(mHourHand);

        mMinuteHand = new AppCompatImageView(context);
        mMinuteHand.setImageResource(R.drawable.clock_analog_minute);
        mMinuteHand.getDrawable().mutate();
        if (!(context instanceof ScreensaverActivity)) {
            mMinuteHand.setColorFilter(clockColor);
        }
        addView(mMinuteHand);

        mSecondHand = new AppCompatImageView(context);
        mSecondHand.setImageResource(R.drawable.clock_analog_second);
        mSecondHand.getDrawable().mutate();
        if (context instanceof AlarmActivity || context instanceof AlarmDisplayPreviewActivity) {
            mSecondHand.setColorFilter(alarmSecondsHandColor);
        } else if (!(context instanceof ScreensaverActivity)) {
            final boolean isAutoNightAccentColorEnabled = DataModel.getDataModel().isAutoNightAccentColorEnabled();
            final String accentColor = DataModel.getDataModel().getAccentColor();
            final String nightAccentColor = DataModel.getDataModel().getNightAccentColor();
            if (isAutoNightAccentColorEnabled) {
                switch (accentColor) {
                    case BLACK_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.blackColorPrimary));
                    case BLUE_GRAY_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.blueGrayColorPrimary));
                    case BROWN_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.brownColorPrimary));
                    case GREEN_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.greenColorPrimary));
                    case INDIGO_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.indigoColorPrimary));
                    case ORANGE_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.orangeColorPrimary));
                    case PINK_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.pinkColorPrimary));
                    case PURPLE_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.purpleColorPrimary));
                    case RED_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.redColorPrimary));
                    case YELLOW_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.yellowColorPrimary));
                    default -> mSecondHand.setColorFilter(context.getColor(R.color.md_theme_primary));
                }
            } else {
                if (Utils.isNight(context.getResources())) {
                    switch (nightAccentColor) {
                        case BLACK_NIGHT_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.blackColorPrimary));
                        case BLUE_GRAY_NIGHT_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.blueGrayColorPrimary));
                        case BROWN_NIGHT_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.brownColorPrimary));
                        case GREEN_NIGHT_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.greenColorPrimary));
                        case INDIGO_NIGHT_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.indigoColorPrimary));
                        case ORANGE_NIGHT_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.orangeColorPrimary));
                        case PINK_NIGHT_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.pinkColorPrimary));
                        case PURPLE_NIGHT_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.purpleColorPrimary));
                        case RED_NIGHT_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.redColorPrimary));
                        case YELLOW_NIGHT_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.yellowColorPrimary));
                        default -> mSecondHand.setColorFilter(context.getColor(R.color.md_theme_primary));
                    }
                } else {
                    switch (accentColor) {
                        case BLACK_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.blackColorPrimary));
                        case BLUE_GRAY_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.blueGrayColorPrimary));
                        case BROWN_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.brownColorPrimary));
                        case GREEN_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.greenColorPrimary));
                        case INDIGO_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.indigoColorPrimary));
                        case ORANGE_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.orangeColorPrimary));
                        case PINK_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.pinkColorPrimary));
                        case PURPLE_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.purpleColorPrimary));
                        case RED_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.redColorPrimary));
                        case YELLOW_ACCENT_COLOR -> mSecondHand.setColorFilter(context.getColor(R.color.yellowColorPrimary));
                        default -> mSecondHand.setColorFilter(context.getColor(R.color.md_theme_primary));
                    }
                }
            }
        }
        addView(mSecondHand);
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

        // To get closer to a mechanical watch, the hour hand will move according to the minute value
        float hourAngle = mTime.get(Calendar.HOUR) * 30f;
        if (mTime.get(Calendar.MINUTE) >= 48) {
            hourAngle = hourAngle + 24f;
        } else if (mTime.get(Calendar.MINUTE) >= 36) {
            hourAngle = hourAngle + 18f;
        } else if (mTime.get(Calendar.MINUTE) >= 24) {
            hourAngle = hourAngle + 12f;
        } else if (mTime.get(Calendar.MINUTE) >= 12) {
            hourAngle = hourAngle + 6f;
        } else if (mTime.get(Calendar.MINUTE) <= 11) {
            hourAngle = hourAngle + 0f;
        }
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
