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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.deskclock.events.Events;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.uidata.UiDataModel;

import static com.android.deskclock.uidata.UiDataModel.Tab.STOPWATCH;

public class HandleShortcuts extends Activity {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("HandleShortcuts");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        try {
            final String action = intent.getAction();
            switch (action) {
                case StopwatchService.ACTION_PAUSE_STOPWATCH:
                    Events.sendStopwatchEvent(R.string.action_pause, R.string.label_shortcut);

                    // Open DeskClock positioned on the stopwatch tab.
                    UiDataModel.getUiDataModel().setSelectedTab(STOPWATCH);
                    startActivity(new Intent(this, DeskClock.class)
                            .setAction(StopwatchService.ACTION_PAUSE_STOPWATCH));
                    setResult(RESULT_OK);
                    break;
                case StopwatchService.ACTION_START_STOPWATCH:
                    Events.sendStopwatchEvent(R.string.action_start, R.string.label_shortcut);

                    // Open DeskClock positioned on the stopwatch tab.
                    UiDataModel.getUiDataModel().setSelectedTab(STOPWATCH);
                    startActivity(new Intent(this, DeskClock.class)
                            .setAction(StopwatchService.ACTION_START_STOPWATCH));
                    setResult(RESULT_OK);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported action: " + action);
            }
        } catch (Exception e) {
            LOGGER.e("Error handling intent: " + intent, e);
            setResult(RESULT_CANCELED);
        } finally {
            finish();
        }
    }
}
