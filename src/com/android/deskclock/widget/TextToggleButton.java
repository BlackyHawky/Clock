/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.deskclock.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.android.deskclock.R;
import com.android.deskclock.ZeroTopPaddingTextView;

/**
 * Text based on/off button.
 */
public class TextToggleButton extends LinearLayout implements View.OnClickListener {

    private ZeroTopPaddingTextView mOnText;
    private ZeroTopPaddingTextView mOffText;
    private OnClickListener mListener;
    private boolean mChecked = false;
    private int mColorLit;
    private int mColorDim;
    private int mColorRed;

    public TextToggleButton(Context context) {
        super(context);
        init(context, null, 0);
    }

    public TextToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public TextToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        mColorLit = context.getResources().getColor(R.color.clock_white);
        mColorDim = context.getResources().getColor(R.color.clock_gray);
        mColorRed = context.getResources().getColor(R.color.clock_red);

        mOnText = new ZeroTopPaddingTextView(context, attrs, defStyle);
        mOffText = new ZeroTopPaddingTextView(context, attrs, defStyle);

        mOffText.setPaddingRight(20);
        addView(mOffText);
        addView(mOnText);

        updateColors();
        setClickable(true);
        super.setOnClickListener(this);
    }
    
    public void setOnText(String text) {
        mOnText.setText(text);
    }

    public void setOffText(String text) {
        mOffText.setText(text);
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        updateColors();
    }

    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mListener = l;
    }

    private void updateColors() {
        if (mChecked) {
            mOnText.setTextColor(mColorRed);
            mOffText.setTextColor(mColorDim);
        } else {
            mOnText.setTextColor(mColorDim);
            mOffText.setTextColor(mColorLit);
        }
    }

    @Override
    public void onClick(View view) {
        mChecked = !mChecked;

        updateColors();

        mListener.onClick(view);
    }
}
