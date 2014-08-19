/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.text.format.DateFormat;

import com.android.deskclock.provider.Alarm;

import java.util.Calendar;

public class TimePickerFragment extends DialogFragment {

    private Alarm mAlarm;
    private OnTimeSetListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int hour, minute;
        if (mAlarm == null) {
            final Calendar c = Calendar.getInstance();
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
        } else {
            hour = mAlarm.hour;
            minute = mAlarm.minutes;
        }

        return new TimePickerDialog(getActivity(), R.style.TimePickerTheme, mListener, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (getTargetFragment() instanceof OnTimeSetListener) {
            setOnTimeSetListener((OnTimeSetListener) getTargetFragment());
        }
    }

    public void setOnTimeSetListener(OnTimeSetListener listener) {
        mListener = listener;
    }

    public void setAlarm(Alarm alarm) {
        mAlarm = alarm;
    }
}
