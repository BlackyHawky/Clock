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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DateFormatSymbols;


public class TimePicker extends TimerSetupView implements Button.OnClickListener{

    private TextView mAmPmLabel;
    private String[] mAmpm;
    private int mAmPmState;
    private Button mSetButton;

    private static final int AMPM_NOT_SELECTED = 0;
    private static final int PM_SELECTED = 1;
    private static final int AM_SELECTED = 2;
    private static final int HOURS24_MODE = 3;

    private static final String TIME_PICKER_SAVED_BUFFER_POINTER =
            "timer_picker_saved_buffer_pointer";
    private static final String TIME_PICKER_SAVED_INPUT = "timer_picker_saved_input";
    private static final String TIME_PICKER_SAVED_AMPM = "timer_picker_saved_ampm";

    public TimePicker(Context context) {
        this(context, null);
    }

    public TimePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInputSize = 4;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.time_picker_view;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = mContext.getResources();
        mAmpm = new DateFormatSymbols().getAmPmStrings();

        mLeft.setText(mAmpm[0]);
        mLeft.setOnClickListener(this);
        mRight.setText(mAmpm[1]);
        mRight.setOnClickListener(this);
        mAmPmLabel = (TextView)findViewById(R.id.ampm_label);
        mAmPmState = AMPM_NOT_SELECTED;
        showAmPm();
        updateNumericKeys();
        enableSetButton();
    }


    @Override
    public void onClick(View v) {
        doOnClick(v);
    }

    @Override
    protected void doOnClick(View v) {
        super.doOnClick(v);

        if (v == mLeft) {
            mAmPmState = AM_SELECTED;
            showAmPm();
        } else if (v == mRight) {
            mAmPmState = PM_SELECTED;
            showAmPm();
        } else {
            // enable/disable numeric keys according to the numbers entered already
            updateNumericKeys();
        }
        // enable/disable the "set" key
        enableSetButton();
    }

    @Override
    protected void updateTime() {

        // Put "-" in digits that was not entered
        String hours1 = (mInputPointer < 3) ? "-" :  mLabels[mInput[3]];
        String hours2 = (mInputPointer < 2) ? "-" :  mLabels[mInput[2]];
        String minutes1 = (mInputPointer < 1) ? "-" :  mLabels[mInput[1]];
        String minutes2 = (mInputPointer < 0) ? "-" :  mLabels[mInput[0]];
        mEnteredTime.setTime(hours1, hours2, minutes1, minutes2, null, null);
    }

    private void showAmPm() {
        setLeftRightEnabled(true);
        if (!Alarms.get24HourMode(mContext)) {
            mLeft.setVisibility(View.VISIBLE);
            mRight.setVisibility(View.VISIBLE);
            switch(mAmPmState) {
                case AMPM_NOT_SELECTED:
                    mAmPmLabel.setVisibility(View.INVISIBLE);
                    break;
                case AM_SELECTED:
                    mAmPmLabel.setVisibility(View.VISIBLE);
                    mAmPmLabel.setText(mAmpm[0]);
                    break;
                case PM_SELECTED:
                    mAmPmLabel.setVisibility(View.VISIBLE);
                    mAmPmLabel.setText(mAmpm[1]);
                    break;
                default:
                    break;
            }
        } else {
            mLeft.setVisibility(View.INVISIBLE);
            mRight.setVisibility(View.INVISIBLE);
            mAmPmLabel.setVisibility(View.INVISIBLE);
            mAmPmState = HOURS24_MODE;
        }
    }

    // Enable/disable keys in the numeric key pad according to the data entered
    private void updateNumericKeys() {
        int time = mInput[3] * 1000 + mInput[2] * 100 + mInput[1] * 10 + mInput[0];
        if (Alarms.get24HourMode(mContext)) {
            if (time == 0) {
                setKeyRange(9);
                mNumbers[0].setEnabled(false);
            } else if (time == 1) {
                setKeyRange(9);
            } else if (time <= 5) {
                setKeyRange(9);
            } else if (time <= 9) {
                setKeyRange(5);
            } else if (time >= 10 && time <= 15) {
                setKeyRange(9);
            } else if (time >= 16 && time <= 19) {
                setKeyRange(5);
            } else if (time >= 20 && time <= 25) {
                setKeyRange(9);
            } else if (time >= 30 && time <= 35) {
                setKeyRange(9);
            } else if (time >= 40 && time <= 45) {
                setKeyRange(9);
            } else if (time >= 50 && time <= 55) {
                setKeyRange(9);
            } else if (time >= 60 && time <= 65) {
                setKeyRange(9);
            } else if (time >= 70 && time <= 75) {
                setKeyRange(9);
            } else if (time >= 80 && time <= 85) {
                setKeyRange(9);
            } else if (time >= 90 && time <= 95) {
                setKeyRange(9);
            } else if (time >= 100 && time <= 105) {
                setKeyRange(9);
            } else if (time >= 106 && time <= 109) {
                setKeyRange(-1);
            } else if (time >= 110 && time <= 115) {
                setKeyRange(9);
            } else if (time >= 116 && time <= 119) {
                setKeyRange(-1);
            } else if (time >= 120 && time <= 125) {
                setKeyRange(9);
            } else if (time >= 126 && time <= 129) {
                setKeyRange(-1);
            } else if (time >= 130 && time <= 135) {
                setKeyRange(9);
            } else if (time >= 136 && time <= 139) {
                setKeyRange(-1);
            } else if (time >= 140 && time <= 145) {
                setKeyRange(9);
            } else if (time >= 146 && time <= 149) {
                setKeyRange(-1);
            } else if (time >= 150 && time <= 155) {
                setKeyRange(9);
            } else if (time >= 156 && time <= 159) {
                setKeyRange(-1);
            } else if (time >= 160 && time <= 165) {
                setKeyRange(9);
            } else if (time >= 166 && time <= 169) {
                setKeyRange(-1);
            } else if (time >= 170 && time <= 175) {
                setKeyRange(9);
            } else if (time >= 176 && time <= 179) {
                setKeyRange(-1);
            } else if (time >= 180 && time <= 185) {
                setKeyRange(9);
            } else if (time >= 186 && time <= 189) {
                setKeyRange(-1);
            } else if (time >= 190 && time <= 190) {
                setKeyRange(9);
            } else if (time >= 196 && time <= 199) {
                setKeyRange(-1);
            } else if (time >= 200 && time <= 205) {
                setKeyRange(9);
            } else if (time >= 210 && time <= 215) {
                setKeyRange(9);
            } else if (time >= 220 && time <= 225) {
                setKeyRange(9);
            } else if (time >= 230 && time <= 235) {
                setKeyRange(9);
            } else if (time >= 236) {
                setKeyRange(-1);
            }
        } else {
            if (time == 0) {
                setKeyRange(9);
                mNumbers[0].setEnabled(false);
            } else if (time <= 9) {
                setKeyRange(5);
            } else if (time <= 95) {
                setKeyRange(9);
            } else if (time >= 100 && time <= 105) {
                setKeyRange(9);
            } else if (time >= 106 && time <= 109) {
                setKeyRange(-1);
            } else if (time >= 110 && time <= 115) {
                setKeyRange(9);
            } else if (time >= 116 && time <= 119) {
                setKeyRange(-1);
            } else if (time >= 120 && time <= 125) {
                setKeyRange(9);
            } else if (time >= 126) {
                setKeyRange(-1);
            }
        }
    }

    // enables a range of numeric keys from zero to maxKey. The rest of the keys will be disabled
    private void setKeyRange(int maxKey) {
        for (int i = 0; i < mNumbers.length; i++) {
            mNumbers[i].setEnabled(i <= maxKey);
        }
    }
    // Enable/disable the set key
    private void enableSetButton() {
        if (mSetButton == null) {
            return;
        }
        // If the user entered 3 digits or more , and selected am/pm (if not in 24 hours mode)
        // it is a legal time and the set key should be enabled.
        if (!Alarms.get24HourMode(mContext)) {
            mSetButton.setEnabled(mInputPointer >= 2 && mAmPmState != AMPM_NOT_SELECTED);
        } else {
            int time = mInput[3] * 1000 + mInput[2] * 100 + mInput[1] * 10 + mInput[0];
            mSetButton.setEnabled(!(time < 1000 && time%100 > 59));
        }
    }

    public void setSetButton(Button b) {
        mSetButton = b;
        enableSetButton();
    }

    public int getHours() {
        int hours = mInput[3] * 10 + mInput[2];
        if (hours == 12) {
            switch (mAmPmState) {
                case PM_SELECTED:
                    return 12;
                case AM_SELECTED:
                    return 0;
                case HOURS24_MODE:
                    return hours;
                default:
                    break;
            }
        }
        return hours + (mAmPmState == PM_SELECTED ? 12 : 0);
    }
    public int getMinutes() {
        return mInput[1] * 10 + mInput[0];
    }

    @Override
    public Bundle onSaveInstanceState () {
        Bundle b = new Bundle();
        b.putInt(TIME_PICKER_SAVED_BUFFER_POINTER, mInputPointer);
        b.putIntArray(TIME_PICKER_SAVED_INPUT, mInput);
        b.putInt(TIME_PICKER_SAVED_AMPM, mAmPmState);
        return b;
    }

    @Override
    protected void onRestoreInstanceState (Parcelable state) {
        if (state != null) {
            Bundle b = (Bundle)state;
            mInputPointer = b.getInt(TIME_PICKER_SAVED_BUFFER_POINTER, -1);
            mInput = b.getIntArray(TIME_PICKER_SAVED_INPUT);
            if (mInput == null) {
                mInput = new int [mInputSize];
                mInputPointer = -1;
            }
            mAmPmState = b.getInt(TIME_PICKER_SAVED_AMPM, AMPM_NOT_SELECTED);
        }
        showAmPm();
        updateNumericKeys();
        enableSetButton();
        updateTime();
    }
}
