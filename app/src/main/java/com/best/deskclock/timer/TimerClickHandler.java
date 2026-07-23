/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.Context;
import android.view.View;

import androidx.core.view.ViewCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerStringFormatter;
import com.best.deskclock.events.Events;
import com.best.deskclock.utils.LogUtils;

/**
 * Click handler for a timer item.
 */
public record TimerClickHandler(TimerFragment mTimerFragment) {

    public static final String TAG = "TimerClickHandler";
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger(TAG);

    public void onPlayPauseClicked(Timer timer) {
        if (timer.isPaused() || timer.isReset()) {
            Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
            DataModel.getDataModel().startTimer(timer);
        } else if (timer.isRunning()) {
            Events.sendTimerEvent(R.string.action_pause, R.string.label_deskclock);
            DataModel.getDataModel().pauseTimer(timer);
        } else if (timer.isExpired()) {
            final boolean isDeleteAfterUse = timer.getDeleteAfterUse();

            Events.sendTimerEvent(isDeleteAfterUse ? R.string.action_delete : R.string.action_reset, R.string.label_deskclock);

            if (isDeleteAfterUse) {
                DataModel.getDataModel().removeTimer(timer, R.string.label_deskclock);
            } else {
                DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_deskclock);
            }
        } else if (timer.isMissed()) {
            Events.sendTimerEvent(R.string.action_reset, R.string.label_deskclock);
            DataModel.getDataModel().resetOrDeleteMissedTimers(R.string.label_deskclock);
        }
    }

    public void onCircleClicked(Timer timer) {
        if (timer.isPaused() || timer.isReset()) {
            Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
            DataModel.getDataModel().startTimer(timer);
        } else if (timer.isRunning()) {
            Events.sendTimerEvent(R.string.action_pause, R.string.label_deskclock);
            DataModel.getDataModel().pauseTimer(timer);
        }
    }

    public void onResetClicked(Timer timer) {
        Events.sendTimerEvent(R.string.action_reset, R.string.label_deskclock);
        DataModel.getDataModel().resetTimer(timer, R.string.label_deskclock);
    }

    public void onAddTimeClicked(Timer timer, View v) {
        Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, R.string.label_deskclock);
        DataModel.getDataModel().addCustomTimeToTimer(timer);

        Context context = mTimerFragment.requireContext();

        final long currentTime = timer.getRemainingTime();
        final String buttonTime = timer.getButtonTime();

        if (currentTime > 0) {
            ViewCompat.setStateDescription(v, TimerStringFormatter.formatString(
                context, R.string.timer_accessibility_custom_time_added, buttonTime, currentTime, true)
            );
        }
    }

    public void displayBottomSheetDialog(Timer timer) {
        TimerEditBottomSheetFragment fragment = TimerEditBottomSheetFragment.newInstance(timer.getId(), mTimerFragment.getTag());

        TimerEditBottomSheetFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
        LOGGER.v("Opening BottomSheet to edit timer: " + timer.getId());
    }

}
