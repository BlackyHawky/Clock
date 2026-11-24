// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.uicomponents;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link TextTime} that shrinks its text when it does not fit in the available width.
 */
public class AutoSizingTextTime extends TextTime {

    private final TextSizeHelper mTextSizeHelper;

    public AutoSizingTextTime(@NonNull Context context) {
        this(context, null);
    }

    public AutoSizingTextTime(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoSizingTextTime(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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

    /**
     * Sets the preferred text size. The text is only made smaller than this when it does not fit.
     */
    public void applyUserPreferredTextSizeSp(float sizeSp) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        mTextSizeHelper.setMaxTextSize(getTextSize());
    }
}
