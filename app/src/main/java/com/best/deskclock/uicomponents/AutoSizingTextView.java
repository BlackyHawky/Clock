/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * A TextView which automatically re-sizes its text to fit within its boundaries.
 */
public class AutoSizingTextView extends AppCompatTextView {

    private final TextSizeHelper mTextSizeHelper;

    public AutoSizingTextView(Context context) {
        this(context, null);
    }

    public AutoSizingTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AutoSizingTextView(Context context, AttributeSet attrs, int defStyleAttr) {
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
            mTextSizeHelper.onTextChanged(lengthBefore, lengthAfter);
        } else {
            requestLayout();
        }
    }

    @Override
    public void requestLayout() {
        if (mTextSizeHelper == null || mTextSizeHelper.shouldIgnoreRequestLayout()) {
            super.requestLayout();
        }
    }
}
