/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.deskclock.timer.TimerView;


public class TimerSetupView extends LinearLayout implements Button.OnClickListener,
        Button.OnLongClickListener{

    protected int mInputSize = 5;

    protected final Button mNumbers [] = new Button [10];
    protected int mInput [] = new int [mInputSize];
    protected int mInputPointer = -1;
    protected Button mLeft, mRight;
    protected ImageButton mStart;
    protected ImageButton mDelete;
    protected TimerView mEnteredTime;
    protected View mDivider;

    private final int mColorAccent;
    private final int mColorHairline;

    private final AnimatorListenerAdapter mHideFabAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (mStart != null) {
                mStart.setScaleX(1.0f);
                mStart.setScaleY(1.0f);
                mStart.setVisibility(View.INVISIBLE);
            }
        }
    };

    private final AnimatorListenerAdapter mShowFabAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
            if (mStart != null) {
                mStart.setVisibility(View.VISIBLE);
            }
        }
    };

    public TimerSetupView(Context context) {
        this(context, null /* attrs */);
    }

    public TimerSetupView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mColorAccent = Utils.obtainStyledColor(context, R.attr.colorAccent, Color.RED);
        mColorHairline = context.getResources().getColor(R.color.hairline);

        LayoutInflater.from(context).inflate(R.layout.time_setup_view, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View v1 = findViewById(R.id.first);
        View v2 = findViewById(R.id.second);
        View v3 = findViewById(R.id.third);
        View v4 = findViewById(R.id.fourth);

        mEnteredTime = (TimerView)findViewById(R.id.timer_time_text);
        mDelete = (ImageButton)findViewById(R.id.delete);
        mDelete.setOnClickListener(this);
        mDelete.setOnLongClickListener(this);
        mDivider = findViewById(R.id.divider);

        mNumbers[1] = (Button)v1.findViewById(R.id.key_left);
        mNumbers[2] = (Button)v1.findViewById(R.id.key_middle);
        mNumbers[3] = (Button)v1.findViewById(R.id.key_right);

        mNumbers[4] = (Button)v2.findViewById(R.id.key_left);
        mNumbers[5] = (Button)v2.findViewById(R.id.key_middle);
        mNumbers[6] = (Button)v2.findViewById(R.id.key_right);

        mNumbers[7] = (Button)v3.findViewById(R.id.key_left);
        mNumbers[8] = (Button)v3.findViewById(R.id.key_middle);
        mNumbers[9] = (Button)v3.findViewById(R.id.key_right);

        mLeft = (Button)v4.findViewById(R.id.key_left);
        mNumbers[0] = (Button)v4.findViewById(R.id.key_middle);
        mRight = (Button)v4.findViewById(R.id.key_right);

        mLeft.setVisibility(INVISIBLE);
        mRight.setVisibility(INVISIBLE);

        for (int i = 0; i < 10; i++) {
            mNumbers[i].setOnClickListener(this);
            mNumbers[i].setText(String.format("%d", i));
            mNumbers[i].setTextColor(Color.WHITE);
            mNumbers[i].setTag(R.id.numbers_key, new Integer(i));
        }
        updateTime();
    }

    public void registerStartButton(ImageButton start) {
        mStart = start;
        initializeStartButtonVisibility();
    }

    private void initializeStartButtonVisibility() {
        if (mStart != null) {
            mStart.setVisibility(isInputHasValue() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void updateStartButton() {
        setFabButtonVisibility(isInputHasValue() /* show or hide */);
    }

    public void updateDeleteButtonAndDivider() {
        final boolean enabled = isInputHasValue();
        if (mDelete != null) {
            mDelete.setEnabled(enabled);
            mDivider.setBackgroundColor(enabled ? mColorAccent : mColorHairline);
        }
    }

    private boolean isInputHasValue() {
        return mInputPointer != -1;
    }

    private void setFabButtonVisibility(boolean show) {
        final int finalVisibility = show ? View.VISIBLE : View.INVISIBLE;
        if (mStart == null || mStart.getVisibility() == finalVisibility) {
            // Fab is not initialized yet or already shown/hidden
            return;
        }

        final Animator scaleAnimator = AnimatorUtils.getScaleAnimator(
                mStart, show ? 0.0f : 1.0f, show ? 1.0f : 0.0f);
        scaleAnimator.setDuration(AnimatorUtils.ANIM_DURATION_SHORT);
        scaleAnimator.addListener(show ? mShowFabAnimatorListener : mHideFabAnimatorListener);
        scaleAnimator.start();
    }

    @Override
    public void onClick(View v) {
        doOnClick(v);
        updateStartButton();
        updateDeleteButtonAndDivider();
    }

    protected void doOnClick(View v) {

        Integer val = (Integer) v.getTag(R.id.numbers_key);
        // A number was pressed
        if (val != null) {
            // pressing "0" as the first digit does nothing
            if (mInputPointer == -1 && val == 0) {
                return;
            }
            if (mInputPointer < mInputSize - 1) {
                for (int i = mInputPointer; i >= 0; i--) {
                    mInput[i+1] = mInput[i];
                }
                mInputPointer++;
                mInput [0] = val;
                // Update so talkback will read the number being deleted
                mDelete.setContentDescription(
                        getResources().getString(R.string.timer_descriptive_delete,
                                Integer.toString(val)));
                updateTime();
            }
            return;
        }

        // other keys
        if (v == mDelete) {
            if (mInputPointer >= 0) {
                for (int i = 0; i < mInputPointer; i++) {
                    mInput[i] = mInput[i + 1];
                }
                mInput[mInputPointer] = 0;
                mInputPointer--;
                updateTime();
            }
            // update so talkback will read either the next number or its original description
            // if there are no more numbers.
            mDelete.setContentDescription(getResources().getString(
                    R.string.timer_descriptive_delete,
                    mInputPointer < 0 ? "" : Integer.toString(mInput[mInputPointer])));
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == mDelete) {
            reset();
            updateStartButton();
            updateDeleteButtonAndDivider();
            return true;
        }
        return false;
    }

    protected void updateTime() {
        mEnteredTime.setTime(mInput[4], mInput[3], mInput[2],
                mInput[1] * 10 + mInput[0]);
    }

    public void reset() {
        for (int i = 0; i < mInputSize; i ++) {
            mInput[i] = 0;
        }
        mInputPointer = -1;
        updateTime();
    }

    public int getTime() {
        return mInput[4] * 3600 + mInput[3] * 600 + mInput[2] * 60 + mInput[1] * 10 + mInput[0];
    }

    public void saveEntryState(Bundle outState, String key) {
        outState.putIntArray(key, mInput);
    }

    public void restoreEntryState(Bundle inState, String key) {
        int[] input = inState.getIntArray(key);
        if (input != null && mInputSize == input.length) {
            for (int i = 0; i < mInputSize; i++) {
                mInput[i] = input[i];
                if (mInput[i] != 0) {
                    mInputPointer = i;
                }
            }
            updateTime();
        }
        initializeStartButtonVisibility();
    }
}
