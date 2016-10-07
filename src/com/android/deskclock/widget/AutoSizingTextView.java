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
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import static java.lang.Integer.MAX_VALUE;

/**
 * A TextView which automatically re-sizes its text to fit within its boundaries.
 */
public class AutoSizingTextView extends TextView {

    /**
     * Text paint used for measuring.
     */
    private final TextPaint mMeasurePaint = new TextPaint();

    /**
     * The maximum size the text is allowed to be (in pixels).
     */
    private float mMaxTextSize;

    /**
     * The maximum width the text is allows to be (in pixels).
     */
    private int mWidthConstraint = MAX_VALUE;
    /**
     * The maximum height the text is allows to be (in pixels).
     */
    private int mHeightConstraint = MAX_VALUE;

    /**
     * When {@code true} calls to {@link #requestLayout()} should be ignored.
     */
    private boolean mIgnoreRequestLayout;

    public AutoSizingTextView(Context context) {
        this(context, null /* attrs */);
    }

    public AutoSizingTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AutoSizingTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMaxTextSize = getTextSize();
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        mMaxTextSize = getTextSize();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthConstraint = MAX_VALUE;
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            widthConstraint = MeasureSpec.getSize(widthMeasureSpec)
                    - getCompoundPaddingLeft() - getCompoundPaddingRight();
        }

        int heightConstraint = MAX_VALUE;
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            heightConstraint = MeasureSpec.getSize(heightMeasureSpec)
                    - getCompoundPaddingTop() - getCompoundPaddingBottom();
        }

        if (mWidthConstraint != widthConstraint || mHeightConstraint != heightConstraint) {
            mWidthConstraint = widthConstraint;
            mHeightConstraint = heightConstraint;

            adjustTextSize();
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void requestLayout() {
        if (!mIgnoreRequestLayout) {
            super.requestLayout();

            mWidthConstraint = MAX_VALUE;
            mHeightConstraint = MAX_VALUE;
        }
    }

    private void adjustTextSize() {
        final CharSequence text = getText();
        float textSize = mMaxTextSize;
        if (text.length() > 0
                && (mWidthConstraint < MAX_VALUE || mHeightConstraint < MAX_VALUE)) {
            mMeasurePaint.set(getPaint());

            float minTextSize = 1f;
            float maxTextSize = mMaxTextSize;
            while (maxTextSize >= minTextSize) {
                final float midTextSize = Math.round((maxTextSize + minTextSize) / 2f);
                mMeasurePaint.setTextSize(midTextSize);

                final float width = Layout.getDesiredWidth(text, mMeasurePaint);
                final float height = mMeasurePaint.getFontMetricsInt(null);
                if (width > mWidthConstraint || height > mHeightConstraint) {
                    maxTextSize = midTextSize - 1f;
                } else {
                    textSize = midTextSize;
                    minTextSize = midTextSize + 1f;
                }
            }
        }

        if (getTextSize() != textSize) {
            mIgnoreRequestLayout = true;
            super.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            mIgnoreRequestLayout = false;
        }
    }
}
