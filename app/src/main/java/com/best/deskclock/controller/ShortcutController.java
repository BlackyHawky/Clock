/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.controller;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.provider.AlarmClock;

import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

import com.best.deskclock.DeskClock;
import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.HandleApiCalls;
import com.best.deskclock.HandleShortcuts;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Stopwatch;
import com.best.deskclock.data.StopwatchListener;
import com.best.deskclock.events.Events;
import com.best.deskclock.events.ShortcutEventTracker;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.stopwatch.StopwatchService;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.LogUtils;

import java.util.Arrays;
import java.util.Collections;

@RequiresApi(Build.VERSION_CODES.N_MR1)
class ShortcutController {

    private final ComponentName mComponentName;
    private final ShortcutManager mShortcutManager;

    ShortcutController() {
        Context appContext = DeskClockApplication.getAppContext();
        mComponentName = new ComponentName(appContext, DeskClock.class);
        mShortcutManager = appContext.getSystemService(ShortcutManager.class);
        Controller.getController().addEventTracker(new ShortcutEventTracker());
        DataModel.getDataModel().addStopwatchListener(new StopwatchWatcher());
    }

    void updateShortcuts() {
        Context appContext = DeskClockApplication.getAppContext();

        if (!DeviceUtils.isUserUnlocked(appContext)) {
            return;
        }
        try {
            final ShortcutInfo alarm = createNewAlarmShortcut();
            final ShortcutInfo timer = createNewTimerShortcut();
            final ShortcutInfo stopwatch = createStopwatchShortcut();
            final ShortcutInfo screensaver = createScreensaverShortcut();
            mShortcutManager.setDynamicShortcuts(
                    Arrays.asList(alarm, timer, stopwatch, screensaver));
        } catch (IllegalStateException e) {
            LogUtils.wtf(e);
        }
    }

    private ShortcutInfo createNewAlarmShortcut() {
        Context appContext = DeskClockApplication.getAppContext();

        final Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                .setClass(appContext, HandleApiCalls.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String setAlarmShortcut = UiDataModel.getUiDataModel()
                .getShortcutId(R.string.category_alarm, R.string.action_create);
        return new ShortcutInfo.Builder(appContext, setAlarmShortcut)
                .setIcon(Icon.createWithResource(appContext, R.drawable.shortcut_new_alarm))
                .setActivity(mComponentName)
                .setShortLabel(appContext.getString(R.string.shortcut_new_alarm_short))
                .setLongLabel(appContext.getString(R.string.shortcut_new_alarm_long))
                .setIntent(intent)
                .setRank(0)
                .build();
    }

    private ShortcutInfo createNewTimerShortcut() {
        Context appContext = DeskClockApplication.getAppContext();

        final Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER)
                .setClass(appContext, HandleApiCalls.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String setTimerShortcut = UiDataModel.getUiDataModel()
                .getShortcutId(R.string.category_timer, R.string.action_create);
        return new ShortcutInfo.Builder(appContext, setTimerShortcut)
                .setIcon(Icon.createWithResource(appContext, R.drawable.shortcut_new_timer))
                .setActivity(mComponentName)
                .setShortLabel(appContext.getString(R.string.shortcut_new_timer_short))
                .setLongLabel(appContext.getString(R.string.shortcut_new_timer_long))
                .setIntent(intent)
                .setRank(1)
                .build();
    }

    private ShortcutInfo createStopwatchShortcut() {
        Context appContext = DeskClockApplication.getAppContext();

        final @StringRes int action = DataModel.getDataModel().getStopwatch().isRunning()
                ? R.string.action_pause : R.string.action_start;
        final String shortcutId = UiDataModel.getUiDataModel()
                .getShortcutId(R.string.category_stopwatch, action);
        final ShortcutInfo.Builder shortcut = new ShortcutInfo.Builder(appContext, shortcutId)
                .setIcon(Icon.createWithResource(appContext, R.drawable.shortcut_stopwatch))
                .setActivity(mComponentName)
                .setRank(2);
        final Intent intent;
        if (DataModel.getDataModel().getStopwatch().isRunning()) {
            intent = new Intent(StopwatchService.ACTION_PAUSE_STOPWATCH)
                    .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
            shortcut.setShortLabel(appContext.getString(R.string.shortcut_pause_stopwatch_short))
                    .setLongLabel(appContext.getString(R.string.shortcut_pause_stopwatch_long));
        } else {
            intent = new Intent(StopwatchService.ACTION_START_STOPWATCH)
                    .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
            shortcut.setShortLabel(appContext.getString(R.string.shortcut_start_stopwatch_short))
                    .setLongLabel(appContext.getString(R.string.shortcut_start_stopwatch_long));
        }

        intent.setClass(appContext, HandleShortcuts.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return shortcut
                .setIntent(intent)
                .build();
    }

    private ShortcutInfo createScreensaverShortcut() {
        Context appContext = DeskClockApplication.getAppContext();

        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClass(appContext, ScreensaverActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String screensaverShortcut = UiDataModel.getUiDataModel()
                .getShortcutId(R.string.category_screensaver, R.string.action_show);
        return new ShortcutInfo.Builder(appContext, screensaverShortcut)
                .setIcon(Icon.createWithResource(appContext, R.drawable.shortcut_screensaver))
                .setActivity(mComponentName)
                .setShortLabel((appContext.getString(R.string.shortcut_start_screensaver_short)))
                .setLongLabel((appContext.getString(R.string.shortcut_start_screensaver_long)))
                .setIntent(intent)
                .setRank(3)
                .build();
    }

    private class StopwatchWatcher implements StopwatchListener {

        @Override
        public void stopwatchUpdated(Stopwatch after) {
            Context context = DeskClockApplication.getAppContext();

            if (!DeviceUtils.isUserUnlocked(context)) {
                return;
            }

            try {
                mShortcutManager.updateShortcuts(
                        Collections.singletonList(createStopwatchShortcut()));
            } catch (IllegalStateException e) {
                LogUtils.wtf(e);
            }
        }

    }
}
