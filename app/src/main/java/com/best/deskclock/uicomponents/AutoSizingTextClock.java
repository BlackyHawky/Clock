/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextClock;

/**
 * Wrapper around TextClock that automatically re-sizes itself to fit within the given bounds.
 */
public class AutoSizingTextClock extends TextClock {

    private final TextSizeHelper mTextSizeHelper;
    private boolean mSuppressLayout = false;

    public AutoSizingTextClock(Context context) {
        this(context, null);
    }

    public AutoSizingTextClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoSizingTextClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTextSizeHelper = new TextSizeHelper(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mTextSizeHelper.onMeasure(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
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
    public void setText(CharSequence text, BufferType type) {
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
}
