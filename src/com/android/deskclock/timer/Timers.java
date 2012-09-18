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

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Timers {
    // Private actions processed by the receiver
    public static final String START_TIMER = "start_timer";
    public static final String DELETE_TIMER = "delete_timer";
    public static final String TIMES_UP = "times_up";
    public static final String TIMER_RESET = "timer_reset";
    public static final String TIMER_STOP = "timer_stop";
    public static final String TIMER_DONE = "timer_done";
    public static final String TIMER_UPDATE = "timer_update";

    public static final String TIMER_INTENT_EXTRA = "timer.intent.extra";

    public static TimerObj findTimer(ArrayList<TimerObj> timers, int timerId) {
        Iterator<TimerObj> i = timers.iterator();
        while(i.hasNext()) {
            TimerObj t = i.next();
            if (t.mTimerId == timerId) {
                return t;
            }
        }
        return null;
    }
    public static TimerObj findExpiredTimer(ArrayList<TimerObj> timers) {
        Iterator<TimerObj> i = timers.iterator();
        while(i.hasNext()) {
            TimerObj t = i.next();
            if (t.mState == TimerObj.STATE_TIMESUP) {
                return t;
            }
        }
        return null;
    }
}
