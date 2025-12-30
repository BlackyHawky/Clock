/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.AppCompatTextView;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.ThemeUtils;

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
    private Context mContext;
    private SharedPreferences mPrefs;

    private boolean mAttached;

    private int mHour;
    private int mMinute;

    private final ContentObserver mFormatChangeObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {

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

    public TextTime(Context context) {
        this(context, null);
    }

    public TextTime(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextTime(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            return;
        }

        mContext = context;
        mPrefs = getDefaultSharedPreferences(context);
        setTimeFormat(0.45f, false);
        chooseFormat();
    }

    public void setFormat12Hour(CharSequence format) {
        mFormat12 = format;

        chooseFormat();
        updateTime();
    }

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

        if (isInEditMode()) {
            return;
        }

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
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);
    }

    private void unregisterObserver() {
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.unregisterContentObserver(mFormatChangeObserver);
    }

    public void setTime(int hour, int minute) {
        if (isInEditMode()) {
            return;
        }

        mHour = hour;
        mMinute = minute;
        updateTime();
    }

    public void setTypeface(boolean isAlarmEnabled) {
        String fontPath = SettingsDAO.getAlarmFont(mPrefs);
        Typeface typeface = isAlarmEnabled
                ? ThemeUtils.boldTypeface(fontPath)
                : ThemeUtils.loadFont(fontPath);

        setTypeface(typeface);
    }

    public void setTimeFormat(float amPmRatio, boolean includeSeconds) {
        CharSequence format12 = ClockUtils.get12ModeFormat(mContext, amPmRatio, includeSeconds,
                true, false, false);
        setFormat12Hour(format12);

        CharSequence format24 = ClockUtils.get24ModeFormat(includeSeconds, false);
        setFormat24Hour(format24);
    }

    private void updateTime() {
        if (isInEditMode()) {
            return;
        }

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
