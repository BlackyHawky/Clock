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

import java.util.ArrayList;
import java.util.Iterator;

public class Timers {
    // Logging shared by TimerReceiver and TimerAlertFullScreen
    public static final boolean LOGGING = true;

    // Private actions processed by the receiver
    public static final String START_TIMER = "start_timer";
    public static final String STOP_TIMER = "stop_timer";
    public static final String DELETE_TIMER = "delete_timer";
    public static final String RESET_TIMER = "reset_timer";
    public static final String TIMES_UP = "times_up";
    public static final String TIMER_DONE = "timer_done";
    public static final String TIMER_UPDATE = "timer_update";

    public static final String TIMER_INTENT_EXTRA = "timer.intent.extra";

    public static final String UPDATE_NEXT_TIMESUP = "timer_update_next_timesup";

    public static final String NOTIF_IN_USE_SHOW = "notif_in_use_show";
    public static final String NOTIF_IN_USE_CANCEL = "notif_in_use_cancel";
    public static final String NOTIF_APP_OPEN = "notif_app_open";
    public static final String NOTIF_TIMES_UP_STOP = "notif_times_up_stop";
    public static final String NOTIF_TIMES_UP_PLUS_ONE = "notif_times_up_plus_one";
    public static final String NOTIF_TIMES_UP_SHOW = "notif_times_up_show";
    public static final String NOTIF_TIMES_UP_CANCEL = "notif_times_up_cancel";
    public static final String FIRST_LAUNCH_FROM_API_CALL = "first_launch_from_api_call";
    public static final String SCROLL_TO_TIMER_ID = "scroll_to_timer_id";

    public static final String TIMESUP_MODE = "times_up";

    /**
     * Key to a shared preference that forces Timer user interfaces to refresh in order to reflect
     * changes in the database.
     */
    public static final String REFRESH_UI_WITH_LATEST_DATA = "refresh_ui_with_latest_data";

    public static TimerObj findTimer(ArrayList<TimerObj> timers, int timerId) {
        for (TimerObj t : timers) {
            if (t.mTimerId == timerId) {
                return t;
            }
        }
        return null;
    }

    public static TimerObj findExpiredTimer(ArrayList<TimerObj> timers) {
        for (TimerObj t : timers) {
            if (t.mState == TimerObj.STATE_TIMESUP) {
                return t;
            }
        }
        return null;
    }

    public static ArrayList<TimerObj> timersInUse(ArrayList<TimerObj> timers) {
        final ArrayList<TimerObj> result = new ArrayList<>(timers);
        for (Iterator<TimerObj> it = result.iterator(); it.hasNext();) {
            if (!it.next().isInUse()) {
                it.remove();
            }
        }
        return result;
    }

    public static ArrayList<TimerObj> timersInTimesUp(ArrayList<TimerObj> timers) {
        final ArrayList<TimerObj> result = new ArrayList<>(timers);
        for (Iterator<TimerObj> it = result.iterator(); it.hasNext();) {
            if (it.next().mState != TimerObj.STATE_TIMESUP) {
                it.remove();
            }
        }
        return result;
    }
}
