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

package com.android.deskclock.timer;

import android.os.Parcel;
import android.os.Parcelable;

class Timer implements Parcelable {
    public int mTimerId;             // Unique id
    public long mStartTime;          // With mTimeLeft , used to calculate the correct time
    public long mTimeLeft;           // in the timer.

    // Private actions processed by the receiver
    public static final String START_TIMER = "start_timer";
    public static final String CANCEL_TIMER = "cancel_timer";
    public static final String TIMES_UP = "times_up";
    public static final String TIMER_RESET = "timer_reset";

    public static final String TIMER_INTENT_EXTRA = "timer.intent.extra";

    public static final Parcelable.Creator<Timer> CREATOR = new Parcelable.Creator<Timer>() {
        @Override
        public Timer createFromParcel(Parcel p) {
            return new Timer(p);
        }

        @Override
        public Timer[] newArray(int size) {
            return new Timer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTimerId);
        dest.writeLong(mStartTime);
        dest.writeLong(mTimeLeft);
    }

    public Timer(Parcel p) {
        mTimerId = p.readInt();
        mStartTime = p.readLong();
        mTimeLeft = p.readLong();
    }


    public Timer() {
    }
}
