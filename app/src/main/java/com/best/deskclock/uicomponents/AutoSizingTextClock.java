/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;

/**
 * Wrapper around TextClock that automatically re-sizes itself to fit within the given bounds.
 */
public class AutoSizingTextClock extends TextClock {

    private final TextSizeHelper mTextSizeHelper;
    private boolean mSuppressLayout = false;

    public AutoSizingTextClock(@NonNull Context context) {
        this(context, null);
    }

    public AutoSizingTextClock(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoSizingTextClock(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTextSizeHelper = new TextSizeHelper(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mTextSizeHelper.onMeasure(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onTextChanged(@NonNull CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (mTextSizeHelper != null) {
            if (lengthBefore != lengthAfter) {
                mSuppressLayout = false;
            }
            mTextSizeHelper.onTextChanged(lengthBefore, lengthAfter);
        } else {
            requestLayout();
        }
    }

    @Override
    public void setText(@NonNull CharSequence text, @NonNull BufferType type) {
        mSuppressLayout = true;
        super.setText(text, type);
        mSuppressLayout = false;
    }

    @Override
    public void requestLayout() {
        if (mTextSizeHelper == null || mTextSizeHelper.shouldIgnoreRequestLayout()) {
            if (!mSuppressLayout) {
                super.requestLayout();
            }
        }
    }

    public void applyUserPreferredTextSizeSp(float sizeSp) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        mTextSizeHelper.setMaxTextSize(getTextSize());
    }

    /**
     * Freezes the TextClock at a specific time (useful for previewing).
     */
    public void setStaticTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        String formattedTime = DateFormat.getTimeFormat(getContext()).format(calendar.getTime());

        // Wrap the time in single quotes to force TextClock to display it as literal text, ignoring any format patterns.
        String escapedTime = "'" + formattedTime.replace("'", "''") + "'";

        setFormat12Hour(escapedTime);
        setFormat24Hour(escapedTime);
    }

}
