/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.stopwatch;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.android.deskclock.HandleDeskClockApiCalls;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;

/**
 * This service exists solely to allow the stopwatch notification to alter the state of the
 * stopwatch without disturbing the notification shade. If an activity were used instead (even one
 * that is not displayed) the notification manager implicitly closes the notification shade which
 * clashes with the use case of starting/pausing/lapping/resetting the stopwatch without
 * disturbing the notification shade.
 */
public final class StopwatchService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case HandleDeskClockApiCalls.ACTION_START_STOPWATCH: {
                DataModel.getDataModel().startStopwatch();
                Events.sendStopwatchEvent(R.string.action_start, R.string.label_notification);
                break;
            }
            case HandleDeskClockApiCalls.ACTION_PAUSE_STOPWATCH: {
                DataModel.getDataModel().pauseStopwatch();
                Events.sendStopwatchEvent(R.string.action_pause, R.string.label_notification);
                break;
            }
            case HandleDeskClockApiCalls.ACTION_LAP_STOPWATCH: {
                DataModel.getDataModel().addLap();
                Events.sendStopwatchEvent(R.string.action_lap, R.string.label_notification);
                break;
            }
            case HandleDeskClockApiCalls.ACTION_RESET_STOPWATCH: {
                DataModel.getDataModel().clearLaps();
                DataModel.getDataModel().resetStopwatch();
                Events.sendStopwatchEvent(R.string.action_reset, R.string.label_notification);
                break;
            }
        }

        return Service.START_NOT_STICKY;
    }
}