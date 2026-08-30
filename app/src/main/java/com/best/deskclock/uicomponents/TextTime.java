/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Based on {@link android.widget.TextClock}, This widget displays a constant time of day using
 * format specifiers. {@link android.widget.TextClock} doesn't support a non-ticking clock.
 */
public class TextTime extends AppCompatTextView {

    @VisibleForTesting()
    static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";
    @VisibleForTesting()
    static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";

    private CharSequence mFormat12;
    private CharSequence mFormat24;
    private CharSequence mFormat;
    private boolean mIs24HourMode;
    private int mHour;
    private int mMinute;

    public TextTime(@NonNull Context context) {
        this(context, null);
    }

    public TextTime(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextTime(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void configure(boolean is24HourMode, CharSequence format12, CharSequence format24) {
        mIs24HourMode = is24HourMode;
        mFormat12 = format12;
        mFormat24 = format24;

        chooseFormat();
        updateTime();
    }

    private void chooseFormat() {
        if (mIs24HourMode) {
            mFormat = mFormat24 == null ? DEFAULT_FORMAT_24_HOUR : mFormat24;
        } else {
            mFormat = mFormat12 == null ? DEFAULT_FORMAT_12_HOUR : mFormat12;
        }
    }

    public void setTime(int hour, int minute) {
        if (isInEditMode()) {
            return;
        }

        mHour = hour;
        mMinute = minute;
        updateTime();
    }

    private void updateTime() {
        if (isInEditMode()) {
            return;
        }

        // Format the time relative to UTC to ensure hour and minute are not adjusted for DST.
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(HOUR_OF_DAY, mHour);
        calendar.set(MINUTE, mMinute);
        final CharSequence text = DateFormat.format(mFormat, calendar);
        setText(text);
        // Strip away the spans from text so talkback is not confused
        setContentDescription(text.toString());
    }
}
