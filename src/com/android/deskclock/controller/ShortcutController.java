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

package com.android.deskclock.controller;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.UserManager;
import android.provider.AlarmClock;
import android.support.annotation.StringRes;

import com.android.deskclock.DeskClock;
import com.android.deskclock.HandleApiCalls;
import com.android.deskclock.HandleShortcuts;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.ScreensaverActivity;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Lap;
import com.android.deskclock.data.Stopwatch;
import com.android.deskclock.data.StopwatchListener;
import com.android.deskclock.events.Events;
import com.android.deskclock.events.ShortcutEventTracker;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.uidata.UiDataModel;

import java.util.Arrays;
import java.util.Collections;

@TargetApi(Build.VERSION_CODES.N_MR1)
class ShortcutController {

    private final Context mContext;
    private final ComponentName mComponentName;
    private final ShortcutManager mShortcutManager;
    private final UserManager mUserManager;

    ShortcutController(Context context) {
        mContext = context;
        mComponentName = new ComponentName(mContext, DeskClock.class);
        mShortcutManager = mContext.getSystemService(ShortcutManager.class);
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        Controller.getController().addEventTracker(new ShortcutEventTracker(mContext));
        DataModel.getDataModel().addStopwatchListener(new StopwatchWatcher());
    }

    void updateShortcuts() {
        if (!mUserManager.isUserUnlocked()) {
            LogUtils.i("Skipping shortcut update because user is locked.");
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
        final Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                .setClass(mContext, HandleApiCalls.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String setAlarmShortcut = UiDataModel.getUiDataModel()
                .getShortcutId(R.string.category_alarm, R.string.action_create);
        return new ShortcutInfo.Builder(mContext, setAlarmShortcut)
                .setIcon(Icon.createWithResource(mContext, R.drawable.shortcut_new_alarm))
                .setActivity(mComponentName)
                .setShortLabel(mContext.getString(R.string.shortcut_new_alarm_short))
                .setLongLabel(mContext.getString(R.string.shortcut_new_alarm_long))
                .setIntent(intent)
                .setRank(0)
                .build();
    }

    private ShortcutInfo createNewTimerShortcut() {
        final Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER)
                .setClass(mContext, HandleApiCalls.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String setTimerShortcut = UiDataModel.getUiDataModel()
                .getShortcutId(R.string.category_timer, R.string.action_create);
        return new ShortcutInfo.Builder(mContext, setTimerShortcut)
                .setIcon(Icon.createWithResource(mContext, R.drawable.shortcut_new_timer))
                .setActivity(mComponentName)
                .setShortLabel(mContext.getString(R.string.shortcut_new_timer_short))
                .setLongLabel(mContext.getString(R.string.shortcut_new_timer_long))
                .setIntent(intent)
                .setRank(1)
                .build();
    }

    private ShortcutInfo createStopwatchShortcut() {
        final @StringRes int action = DataModel.getDataModel().getStopwatch().isRunning()
                ? R.string.action_pause : R.string.action_start;
        final String shortcutId = UiDataModel.getUiDataModel()
                .getShortcutId(R.string.category_stopwatch, action);
        final ShortcutInfo.Builder shortcut = new ShortcutInfo.Builder(mContext, shortcutId)
                .setIcon(Icon.createWithResource(mContext, R.drawable.shortcut_stopwatch))
                .setActivity(mComponentName)
                .setRank(2);
        final Intent intent;
        if (DataModel.getDataModel().getStopwatch().isRunning()) {
            intent = new Intent(StopwatchService.ACTION_PAUSE_STOPWATCH)
                    .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
            shortcut.setShortLabel(mContext.getString(R.string.shortcut_pause_stopwatch_short))
                    .setLongLabel(mContext.getString(R.string.shortcut_pause_stopwatch_long));
        } else {
            intent = new Intent(StopwatchService.ACTION_START_STOPWATCH)
                    .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
            shortcut.setShortLabel(mContext.getString(R.string.shortcut_start_stopwatch_short))
                    .setLongLabel(mContext.getString(R.string.shortcut_start_stopwatch_long));
        }
        intent.setClass(mContext, HandleShortcuts.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return shortcut
                .setIntent(intent)
                .build();
    }

    private ShortcutInfo createScreensaverShortcut() {
        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClass(mContext, ScreensaverActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String screensaverShortcut = UiDataModel.getUiDataModel()
                .getShortcutId(R.string.category_screensaver, R.string.action_show);
        return new ShortcutInfo.Builder(mContext, screensaverShortcut)
                .setIcon(Icon.createWithResource(mContext, R.drawable.shortcut_screensaver))
                .setActivity(mComponentName)
                .setShortLabel((mContext.getString(R.string.shortcut_start_screensaver_short)))
                .setLongLabel((mContext.getString(R.string.shortcut_start_screensaver_long)))
                .setIntent(intent)
                .setRank(3)
                .build();
    }

    private class StopwatchWatcher implements StopwatchListener {

        @Override
        public void stopwatchUpdated(Stopwatch before, Stopwatch after) {
            if (!mUserManager.isUserUnlocked()) {
                LogUtils.i("Skipping stopwatch shortcut update because user is locked.");
                return;
            }
            try {
                mShortcutManager.updateShortcuts(
                        Collections.singletonList(createStopwatchShortcut()));
            } catch (IllegalStateException e) {
                LogUtils.wtf(e);
            }
        }

        @Override
        public void lapAdded(Lap lap) {
        }
    }
}
