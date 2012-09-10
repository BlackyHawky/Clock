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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class TimerSetupView extends LinearLayout implements Button.OnClickListener{

    private static final int INPUT_SIZE = 5;

    private final Button mNumbers [] = new Button [10];
    private final int mInput [] = new int [INPUT_SIZE];
    private int mInputPointer = -1;
    private String mLabels [];
    private Button mLeft, mRight;
    private TextView mEnteredTime;
    private final Context mContext;

    public TimerSetupView(Context context) {
        this(context, null);
    }

    public TimerSetupView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater layoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.time_setup_view, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLabels = mContext.getResources().getStringArray(R.array.key_pad_numbers);


        View v1 = findViewById(R.id.first);
        View v2 = findViewById(R.id.second);
        View v3 = findViewById(R.id.third);
        View v4 = findViewById(R.id.fourth);
        mEnteredTime = (TextView)findViewById(R.id.entered_time);

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

        for (int i = 0; i < 10; i++) {
            mNumbers[i].setOnClickListener(this);
            mNumbers [i].setText(mLabels[i]);
            mNumbers [i].setTag(R.id.numbers_key,new Integer(i));
        }
        updateTime();
    }


    @Override
    public void onClick(View v) {

        Integer val = (Integer) v.getTag(R.id.numbers_key);
        // A number was pressed
        if (val != null) {
            // pressing "0" as the first digit does nothing
            if (mInputPointer == -1 && val == 0) {
                return;
            }
            if (mInputPointer < INPUT_SIZE - 1) {
                mInputPointer++;
                mInput [mInputPointer] = val;
                updateTime();
            }
            return;
        }

        // other keys
        switch (v.getId()) {
            case R.id.delete:
                if (mInputPointer >= 0) {
                    mInput [mInputPointer] = 0;
                    mInputPointer --;
                    updateTime();
                }
                break;
            default:
                break;
        }
    }

    private void updateTime() {
        mEnteredTime.setText(String.format("%d:%d%d:%d%d", mInput[0], mInput[1], mInput[2],
                mInput[3], mInput[4]));
    }

    public void reset() {
        for (int i = 0; i < INPUT_SIZE; i ++) {
            mInput[i] = 0;
        }
        mInputPointer = -1;
        updateTime();
    }
}
