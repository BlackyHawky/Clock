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

import android.widget.TextView;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * A controller which will format a provided time in millis to display as a timer.
 */
public final class TimerTextController {

    private final TextView mTextView;

    public TimerTextController(TextView textView) {
        mTextView = textView;
    }

    public void setTimeString(long remainingTime) {
        boolean isNegative = false;
        if (remainingTime < 0) {
            remainingTime = -remainingTime;
            isNegative = true;
        }

        int hours = (int) (remainingTime / HOUR_IN_MILLIS);
        int remainder = (int) (remainingTime % HOUR_IN_MILLIS);

        int minutes = (int) (remainder / MINUTE_IN_MILLIS);
        remainder = (int) (remainder % MINUTE_IN_MILLIS);

        int seconds = (int) (remainder / SECOND_IN_MILLIS);
        remainder = (int) (remainder % SECOND_IN_MILLIS);

        // Round up to the next second
        if (!isNegative && remainder != 0) {
            seconds++;
            if (seconds == 60) {
                seconds = 0;
                minutes++;
                if (minutes == 60) {
                    minutes = 0;
                    hours++;
                }
            }
        }

        String time = Utils.getTimeString(mTextView.getContext(), hours, minutes, seconds);
        if (isNegative && !(hours == 0 && minutes == 0 && seconds == 0)) {
            time = "\u2212" + time;
        }

        mTextView.setText(time);
    }
}
