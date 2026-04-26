/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.Context;
import android.view.View;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerStringFormatter;
import com.best.deskclock.dialogfragment.LabelDialogFragment;
import com.best.deskclock.dialogfragment.TimerAddTimeButtonDialogFragment;
import com.best.deskclock.dialogfragment.TimerSetNewDurationDialogFragment;
import com.best.deskclock.events.Events;

/**
 * Click handler for a timer item.
 */
public record TimerClickHandler(TimerFragment mTimerFragment) {

    public void onEditLabelClicked(Timer timer) {
        Events.sendAlarmEvent(R.string.action_set_label, R.string.label_deskclock);
        final LabelDialogFragment fragment = LabelDialogFragment.newInstance(timer);
        LabelDialogFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
    }

    public void onEditAddTimeButtonLongClicked(Timer timer) {
        Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, R.string.label_deskclock);
        final TimerAddTimeButtonDialogFragment fragment = TimerAddTimeButtonDialogFragment.newInstance(timer);
        TimerAddTimeButtonDialogFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
    }

    public void onDurationClicked(Timer timer) {
        Events.sendTimerEvent(R.string.action_set_new_timer_duration, R.string.label_deskclock);
        final TimerSetNewDurationDialogFragment fragment = TimerSetNewDurationDialogFragment.newInstance(timer);
        TimerSetNewDurationDialogFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
    }

    public void onDeleteTimerClicked(Timer timer) {
        mTimerFragment.confirmAndDeleteTimer(timer);
    }

    public void onPlayPauseClicked(Timer timer) {
        if (timer.isPaused() || timer.isReset()) {
            DataModel.getDataModel().startTimer(timer);
        } else if (timer.isRunning()) {
            DataModel.getDataModel().pauseTimer(timer);
        } else if (timer.isExpired()) {
            DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_deskclock);
        } else if (timer.isMissed()) {
            DataModel.getDataModel().resetMissedTimers(R.string.label_deskclock);
        }
    }

    public void onResetClicked(Timer timer) {
        DataModel.getDataModel().resetOrDeleteTimer(timer, R.string.label_deskclock);
    }

    public void onAddTimeClicked(Timer timer, View v) {
        DataModel.getDataModel().addCustomTimeToTimer(timer);
        Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, R.string.label_deskclock);

        Context context = mTimerFragment.requireContext();

        final long currentTime = timer.getRemainingTime();
        final String buttonTime = timer.getButtonTime();

        if (currentTime > 0) {
            v.announceForAccessibility(TimerStringFormatter.formatString(
                context, R.string.timer_accessibility_custom_time_added, buttonTime, currentTime, true));
        }
    }

}
