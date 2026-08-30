/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.controller;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.provider.AlarmClock;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Stopwatch;
import com.best.deskclock.data.StopwatchListener;
import com.best.deskclock.events.Events;
import com.best.deskclock.events.ShortcutEventTracker;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.stopwatch.StopwatchService;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiresApi(Build.VERSION_CODES.N_MR1)
class ShortcutController {

    private final ComponentName mComponentName;
    private final ShortcutManager mShortcutManager;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final DataModel mDataModel;
    private final UiDataModel mUiDataModel;

    ShortcutController(@NonNull Context context, @NonNull SharedPreferences prefs) {
        mContext = context;
        mPrefs = prefs;
        mComponentName = new ComponentName(context, DeskClock.class);
        mShortcutManager = context.getSystemService(ShortcutManager.class);
        mDataModel = DataModel.getDataModel();
        mUiDataModel = UiDataModel.getUiDataModel();
        Controller.getController().addEventTracker(new ShortcutEventTracker());
        mDataModel.addStopwatchListener(new StopwatchWatcher());
    }

    void updateShortcuts() {
        Context localizedContext = Utils.getLocalizedContext(mContext, SettingsDAO.getLanguageCode(mPrefs));

        if (!DeviceUtils.isUserUnlocked(localizedContext)) {
            return;
        }

        try {
            List<ShortcutInfo> dynamicShortcuts = new ArrayList<>();
            List<String> disabledShortcutIds = new ArrayList<>();

            if (SettingsDAO.isAlarmTabVisible(mPrefs)) {
                dynamicShortcuts.add(createNewAlarmShortcut());
            } else {
                disabledShortcutIds.add(mUiDataModel.getShortcutId(R.string.category_alarm, R.string.action_create));
            }

            if (SettingsDAO.isTimerTabVisible(mPrefs)) {
                dynamicShortcuts.add(createNewTimerShortcut());
            } else {
                disabledShortcutIds.add(mUiDataModel.getShortcutId(R.string.category_timer, R.string.action_create));
            }

            if (SettingsDAO.isStopwatchTabVisible(mPrefs)) {
                dynamicShortcuts.add(createStopwatchShortcut());
            } else {
                disabledShortcutIds.add(mUiDataModel.getShortcutId(R.string.category_stopwatch, R.string.action_start));
                disabledShortcutIds.add(mUiDataModel.getShortcutId(R.string.category_stopwatch, R.string.action_pause));
            }

            dynamicShortcuts.add(createScreensaverShortcut());

            mShortcutManager.setDynamicShortcuts(dynamicShortcuts);

            if (!disabledShortcutIds.isEmpty()) {
                mShortcutManager.disableShortcuts(disabledShortcutIds, localizedContext.getString(R.string.shortcut_disabled));
            }

            List<String> enabledShortcutIds = new ArrayList<>();
            for (ShortcutInfo info : dynamicShortcuts) {
                enabledShortcutIds.add(info.getId());
            }

            mShortcutManager.enableShortcuts(enabledShortcutIds);
        } catch (IllegalStateException e) {
            LogUtils.wtf(e);
        }
    }

    @NonNull
    private ShortcutInfo createNewAlarmShortcut() {
        Context localizedContext = Utils.getLocalizedContext(mContext, SettingsDAO.getLanguageCode(mPrefs));

        final Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
            .setClass(localizedContext, HandleApiCalls.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String setAlarmShortcut = mUiDataModel.getShortcutId(R.string.category_alarm, R.string.action_create);

        return new ShortcutInfo.Builder(localizedContext, setAlarmShortcut)
            .setIcon(Icon.createWithResource(localizedContext, R.drawable.shortcut_new_alarm))
            .setActivity(mComponentName)
            .setShortLabel(localizedContext.getString(R.string.shortcut_new_alarm_short))
            .setLongLabel(localizedContext.getString(R.string.shortcut_new_alarm_long))
            .setIntent(intent)
            .setRank(0)
            .build();
    }

    @NonNull
    private ShortcutInfo createNewTimerShortcut() {
        Context localizedContext = Utils.getLocalizedContext(mContext, SettingsDAO.getLanguageCode(mPrefs));

        final Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER)
            .setClass(localizedContext, HandleApiCalls.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String setTimerShortcut = mUiDataModel.getShortcutId(R.string.category_timer, R.string.action_create);

        return new ShortcutInfo.Builder(localizedContext, setTimerShortcut)
            .setIcon(Icon.createWithResource(localizedContext, R.drawable.shortcut_new_timer))
            .setActivity(mComponentName)
            .setShortLabel(localizedContext.getString(R.string.shortcut_new_timer_short))
            .setLongLabel(localizedContext.getString(R.string.shortcut_new_timer_long))
            .setIntent(intent)
            .setRank(1)
            .build();
    }

    @NonNull
    private ShortcutInfo createStopwatchShortcut() {
        Context localizedContext = Utils.getLocalizedContext(mContext, SettingsDAO.getLanguageCode(mPrefs));

        final @StringRes int action = mDataModel.getStopwatch().isRunning()
            ? R.string.action_pause
            : R.string.action_start;
        final String shortcutId = mUiDataModel.getShortcutId(R.string.category_stopwatch, action);
        final ShortcutInfo.Builder shortcut = new ShortcutInfo.Builder(localizedContext, shortcutId)
            .setIcon(Icon.createWithResource(localizedContext, R.drawable.shortcut_stopwatch))
            .setActivity(mComponentName)
            .setRank(2);
        final Intent intent;

        if (mDataModel.getStopwatch().isRunning()) {
            intent = new Intent(StopwatchService.ACTION_PAUSE_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
            shortcut.setShortLabel(localizedContext.getString(R.string.shortcut_pause_stopwatch_short)).setLongLabel(localizedContext.getString(R.string.shortcut_pause_stopwatch_long));
        } else {
            intent = new Intent(StopwatchService.ACTION_START_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
            shortcut.setShortLabel(localizedContext.getString(R.string.shortcut_start_stopwatch_short)).setLongLabel(localizedContext.getString(R.string.shortcut_start_stopwatch_long));
        }

        intent.setClass(localizedContext, HandleShortcuts.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return shortcut.setIntent(intent).build();
    }

    @NonNull
    private ShortcutInfo createScreensaverShortcut() {
        Context localizedContext = Utils.getLocalizedContext(mContext, SettingsDAO.getLanguageCode(mPrefs));

        final Intent intent = new Intent(Intent.ACTION_MAIN)
            .setClass(localizedContext, ScreensaverActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String screensaverShortcut = mUiDataModel.getShortcutId(R.string.category_screensaver, R.string.action_show);

        return new ShortcutInfo.Builder(localizedContext, screensaverShortcut)
            .setIcon(Icon.createWithResource(localizedContext, R.drawable.shortcut_screensaver))
            .setActivity(mComponentName)
            .setShortLabel((localizedContext.getString(R.string.shortcut_start_screensaver_short)))
            .setLongLabel((localizedContext.getString(R.string.shortcut_start_screensaver_long)))
            .setIntent(intent)
            .setRank(3)
            .build();
    }

    private class StopwatchWatcher implements StopwatchListener {
        @Override
        public void stopwatchUpdated(@NonNull Stopwatch after) {
            if (!DeviceUtils.isUserUnlocked(mContext)) {
                return;
            }

            try {
                mShortcutManager.updateShortcuts(Collections.singletonList(createStopwatchShortcut()));
            } catch (IllegalStateException e) {
                LogUtils.wtf(e);
            }
        }

    }
}
