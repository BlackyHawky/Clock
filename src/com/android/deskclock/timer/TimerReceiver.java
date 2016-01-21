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

package com.android.deskclock.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.deskclock.LogUtils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;

/**
 * This broadcast receiver exists to handle timer expiry scheduled in 4.2.1 and prior. It must exist
 * for at least one release cycle before removal to honor these old scheduled timers after upgrading
 * beyond 4.2.1. After 4.2.1, all timer expiration is directed to TimerService.
 */
public class TimerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.e("TimerReceiver", "Received legacy timer broadcast: %s", intent.getAction());

        if ("times_up".equals(intent.getAction())) {
            final int timerId = intent.getIntExtra("timer.intent.extra", -1);
            final Timer timer = DataModel.getDataModel().getTimer(timerId);
            context.startService(TimerService.createTimerExpiredIntent(context, timer));
        }
    }
}