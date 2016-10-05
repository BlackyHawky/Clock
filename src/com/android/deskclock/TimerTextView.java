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

package com.android.deskclock;

import android.content.Context;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * A TextView which automatically resizes its font to fit within its boundaries without ellipsizing.
 * This allows for timer times to fit within the boundaries of their containers while still keeping
 * the entire time readable.
 */
public class TimerTextView extends TextView {

    private boolean mInitialized;
    private final RectF mTextRect = new RectF();
    private final RectF mMeasureRect = new RectF();
    private final SparseArray<Float> mSizeCache = new SparseArray<>(14);
    private final TextPaint mPaint = new TextPaint(getPaint());
    private final float mMaxTextSize = getTextSize();
    private final float m2SP = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 2,
            getResources().getDisplayMetrics());

    public TimerTextView(Context context) {
        super(context);
    }

    public TimerTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimerTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (lengthBefore != lengthAfter) {
            adjustTextSize();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mInitialized = true;
        if (w != oldw || h != oldh) {
            mSizeCache.clear();
            adjustTextSize();
        }
    }

    private void adjustTextSize() {
        if (!mInitialized) {
            return;
        }
        mMeasureRect.right = getMeasuredWidth() - getCompoundPaddingLeft()
                - getCompoundPaddingRight();
        mMeasureRect.bottom = getMeasuredHeight() - getCompoundPaddingBottom()
                - getCompoundPaddingTop();
        super.setTextSize(TypedValue.COMPLEX_UNIT_PX, computeTextSize());
    }

    private float computeTextSize() {
        final boolean negative = getText().charAt(0) == '-';
        final int key = getText().toString().length() * (negative ? -1 : 1);
        Float size = mSizeCache.get(key);
        if (size != null) {
            return size;
        }
        size = mMaxTextSize;
        while (!textFits(size)) {
            size -= m2SP;
        }
        mSizeCache.put(key, size);
        return size;
    }

    private boolean textFits(float size) {
        mPaint.setTextSize(size);
        final String text = getText().toString();
        mTextRect.right = mPaint.measureText(text);
        mTextRect.bottom = mPaint.getFontSpacing();
        return mMeasureRect.contains(mTextRect);
    }
}