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

import android.text.Layout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import static java.lang.Integer.MAX_VALUE;

/**
 * A TextView which automatically re-sizes its text to fit within its boundaries.
 */
public final class TextSizeHelper {

    // The text view whose size this class controls.
    private final TextView mTextView;

    // Text paint used for measuring.
    private final TextPaint mMeasurePaint = new TextPaint();

    // The maximum size the text is allowed to be (in pixels).
    private float mMaxTextSize;

    // The maximum width the text is allowed to be (in pixels).
    private int mWidthConstraint = MAX_VALUE;

    // The maximum height the text is allowed to be (in pixels).
    private int mHeightConstraint = MAX_VALUE;

    // When {@code true} calls to {@link #requestLayout()} should be ignored.
    private boolean mIgnoreRequestLayout;

    public TextSizeHelper(TextView view) {
        mTextView = view;
        mMaxTextSize = view.getTextSize();
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthConstraint = MAX_VALUE;
        if (View.MeasureSpec.getMode(widthMeasureSpec) != View.MeasureSpec.UNSPECIFIED) {
            widthConstraint = View.MeasureSpec.getSize(widthMeasureSpec)
                    - mTextView.getCompoundPaddingLeft() - mTextView.getCompoundPaddingRight();
        }

        int heightConstraint = MAX_VALUE;
        if (View.MeasureSpec.getMode(heightMeasureSpec) != View.MeasureSpec.UNSPECIFIED) {
            heightConstraint = View.MeasureSpec.getSize(heightMeasureSpec)
                    - mTextView.getCompoundPaddingTop() - mTextView.getCompoundPaddingBottom();
        }

        if (mTextView.isLayoutRequested() || mWidthConstraint != widthConstraint
                || mHeightConstraint != heightConstraint) {
            mWidthConstraint = widthConstraint;
            mHeightConstraint = heightConstraint;

            adjustTextSize();
        }
    }

    public void onTextChanged(int lengthBefore, int lengthAfter) {
        // The length of the text has changed, request layout to recalculate the current text
        // size. This is necessary to workaround an optimization in TextView#checkForRelayout()
        // which will avoid re-layout when the view has a fixed layout width.
        if (lengthBefore != lengthAfter) {
            mTextView.requestLayout();
        }
    }

    public boolean shouldIgnoreRequestLayout() {
        return mIgnoreRequestLayout;
    }

    private void adjustTextSize() {
        final CharSequence text = mTextView.getText();
        float textSize = mMaxTextSize;
        if (text.length() > 0 && (mWidthConstraint < MAX_VALUE || mHeightConstraint < MAX_VALUE)) {
            mMeasurePaint.set(mTextView.getPaint());

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

        if (mTextView.getTextSize() != textSize) {
            mIgnoreRequestLayout = true;
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            mIgnoreRequestLayout = false;
        }
    }
}