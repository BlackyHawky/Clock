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

package com.android.deskclock.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextClock;

/**
 *  Wrapper around TextClock that automatically re-sizes itself to fit within the given bounds.
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
        if (mTextSizeHelper == null || !mTextSizeHelper.shouldIgnoreRequestLayout()) {
            if (!mSuppressLayout) {
                super.requestLayout();
            }
        }
    }
}