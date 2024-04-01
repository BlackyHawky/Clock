/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.best.deskclock.widget;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.AppCompatTextView;

import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;

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
    /**
     * UTC does not have DST rules and will not alter the {@link #mHour} and {@link #mMinute}.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private CharSequence mFormat12;
    private CharSequence mFormat24;
    private CharSequence mFormat;

    private boolean mAttached;

    private int mHour;
    private int mMinute;

    private final ContentObserver mFormatChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            chooseFormat();
            updateTime();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            chooseFormat();
            updateTime();
        }
    };

    @SuppressWarnings("UnusedDeclaration")
    public TextTime(Context context) {
        this(context, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    public TextTime(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextTime(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setFormat12Hour(Utils.get12ModeFormat(context, 0.45f, false));
        setFormat24Hour(Utils.get24ModeFormat(context, false));

        chooseFormat();
    }

    @SuppressWarnings("UnusedDeclaration")
    public CharSequence getFormat12Hour() {
        return mFormat12;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setFormat12Hour(CharSequence format) {
        mFormat12 = format;

        chooseFormat();
        updateTime();
    }

    @SuppressWarnings("UnusedDeclaration")
    public CharSequence getFormat24Hour() {
        return mFormat24;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setFormat24Hour(CharSequence format) {
        mFormat24 = format;

        chooseFormat();
        updateTime();
    }

    private void chooseFormat() {
        final boolean format24Requested = DataModel.getDataModel().is24HourFormat();
        if (format24Requested) {
            mFormat = mFormat24 == null ? DEFAULT_FORMAT_24_HOUR : mFormat24;
        } else {
            mFormat = mFormat12 == null ? DEFAULT_FORMAT_12_HOUR : mFormat12;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            registerObserver();
            updateTime();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            unregisterObserver();
            mAttached = false;
        }
    }

    private void registerObserver() {
        final ContentResolver resolver = getContext().getContentResolver();
        resolver.registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);
    }

    private void unregisterObserver() {
        final ContentResolver resolver = getContext().getContentResolver();
        resolver.unregisterContentObserver(mFormatChangeObserver);
    }

    public void setTime(int hour, int minute) {
        mHour = hour;
        mMinute = minute;
        updateTime();
    }

    private void updateTime() {
        // Format the time relative to UTC to ensure hour and minute are not adjusted for DST.
        final Calendar calendar = DataModel.getDataModel().getCalendar();
        calendar.setTimeZone(UTC);
        calendar.set(HOUR_OF_DAY, mHour);
        calendar.set(MINUTE, mMinute);
        final CharSequence text = DateFormat.format(mFormat, calendar);
        setText(text);
        // Strip away the spans from text so talkback is not confused
        setContentDescription(text.toString());
    }
}
