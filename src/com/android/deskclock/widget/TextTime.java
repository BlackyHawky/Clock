// Copyright 2013 Google Inc. All Rights Reserved.

package com.android.deskclock.widget;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.deskclock.R;
import com.android.deskclock.Utils;

import java.util.Calendar;

/**
 * Based on {@link android.widget.TextClock}, This widget displays a constant time of day using
 * format specifiers. {@link android.widget.TextClock} Doesn't support a non ticking clock.
 */
public class TextTime extends TextView {
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";

    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";

    private CharSequence mFormat12;
    private CharSequence mFormat24;
    private CharSequence mFormat;
    private String mContentDescriptionFormat;

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

        final TypedArray styledAttributes = context.obtainStyledAttributes(
                attrs, R.styleable.TextTime, defStyle, 0);
        try {
            mFormat12 = styledAttributes.getText(R.styleable.TextTime_format12Hour);
            mFormat24 = styledAttributes.getText(R.styleable.TextTime_format24Hour);
        } finally {
            styledAttributes.recycle();
        }
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
        final boolean format24Requested = DateFormat.is24HourFormat(getContext());
        if (format24Requested) {
            mFormat = mFormat24 == null ? DEFAULT_FORMAT_24_HOUR : mFormat24;
        } else {
            mFormat = mFormat12 == null ? DEFAULT_FORMAT_12_HOUR : mFormat12;
        }
        mContentDescriptionFormat = mFormat.toString();
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

    public void setFormat(Context context, int amPmFontSize) {
        setFormat12Hour(Utils.get12ModeFormat(context, amPmFontSize));
        setFormat24Hour(Utils.get24ModeFormat());
    }

    public void setTime(int hour, int minute) {
        mHour = hour;
        mMinute = minute;
        updateTime();
    }

    private void updateTime() {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, mHour);
        calendar.set(Calendar.MINUTE, mMinute);
        setText(DateFormat.format(mFormat, calendar));
        if (mContentDescriptionFormat != null) {
            setContentDescription(DateFormat.format(mContentDescriptionFormat, calendar));
        } else {
            setContentDescription(DateFormat.format(mFormat, calendar));
        }
    }
}
