/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static com.best.deskclock.uidata.UiDataModel.Tab.STOPWATCH;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.best.deskclock.events.Events;
import com.best.deskclock.stopwatch.StopwatchService;
import com.best.deskclock.uidata.UiDataModel;

public class HandleShortcuts extends Activity {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("HandleShortcuts");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        try {
            final String action = intent.getAction();
            switch (action) {
                case StopwatchService.ACTION_PAUSE_STOPWATCH -> {
                    Events.sendStopwatchEvent(R.string.action_pause, R.string.label_shortcut);

                    // Open DeskClock positioned on the stopwatch tab.
                    UiDataModel.getUiDataModel().setSelectedTab(STOPWATCH);
                    startActivity(new Intent(this, DeskClock.class)
                            .setAction(StopwatchService.ACTION_PAUSE_STOPWATCH));
                    setResult(RESULT_OK);
                }
                case StopwatchService.ACTION_START_STOPWATCH -> {
                    Events.sendStopwatchEvent(R.string.action_start, R.string.label_shortcut);

                    // Open DeskClock positioned on the stopwatch tab.
                    UiDataModel.getUiDataModel().setSelectedTab(STOPWATCH);
                    startActivity(new Intent(this, DeskClock.class)
                            .setAction(StopwatchService.ACTION_START_STOPWATCH));
                    setResult(RESULT_OK);
                }
                default -> throw new IllegalArgumentException("Unsupported action: " + action);
            }
        } catch (Exception e) {
            LOGGER.e("Error handling intent: " + intent, e);
            setResult(RESULT_CANCELED);
        } finally {
            finish();
        }
    }
}
