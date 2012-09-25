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
 * limitations under the License.
 */

package com.android.deskclock;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

/**
 * A time picker dialog
 */
public class AlarmTimePickerDialog extends Dialog implements android.view.View.OnClickListener{

    private final Context mContext;
    private Button mSet, mCancel;
    private TimePicker mPicker;
    private OnTimeSetListener mOnSetTimeListener;

    public interface OnTimeSetListener {
        void onTimeSet(int hourOfDay, int minute);
    }

    public AlarmTimePickerDialog(Context context) {
        super(context, R.style.TimePickerDialog);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.time_picker_dialog, null);
        mSet = (Button)v.findViewById(R.id.set_button);
        mSet.setOnClickListener(this);
        mCancel = (Button)v.findViewById(R.id.cancel_button);
        mCancel.setOnClickListener(this);
        mPicker = (TimePicker)v.findViewById(R.id.time_picker);
        mPicker.setSetButton(mSet);
        setContentView(v);
    }

    public void setOnTimeSetlListener(OnTimeSetListener listener) {
        mOnSetTimeListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (v == mSet) {
            mOnSetTimeListener.onTimeSet(mPicker.getHours(), mPicker.getMinutes());
        }
        dismiss();
    }

    @Override
    public Bundle onSaveInstanceState () {
        if (mPicker != null) {
            return mPicker.onSaveInstanceState();
        }
        return null;
    }

    @Override
    public void onRestoreInstanceState (Bundle b) {
        if (mPicker != null) {
            mPicker.onRestoreInstanceState(b);
        }
    }
}



