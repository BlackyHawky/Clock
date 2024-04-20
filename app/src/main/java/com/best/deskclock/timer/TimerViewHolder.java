/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerStringFormatter;
import com.best.deskclock.events.Events;

public class TimerViewHolder extends RecyclerView.ViewHolder {

    private int mTimerId;
    private final TimerItem mTimerItem;
    private final TimerClickHandler mTimerClickHandler;

    public TimerViewHolder(View view, TimerClickHandler timerClickHandler) {
        super(view);

        view.setBackground(Utils.cardBackground(view.getContext()));

        mTimerItem = (TimerItem) view;
        mTimerClickHandler = timerClickHandler;

        setLayoutParams(view);

        view.findViewById(R.id.reset).setOnClickListener(v -> {
            DataModel.getDataModel().resetOrDeleteTimer(getTimer(), R.string.label_deskclock);
            Utils.vibrationTime(v.getContext(), 10);
        });

        // Must use getTimer() because old timer is no longer accurate.
        View.OnClickListener mAddListener = v -> {
            final Timer timer = getTimer();
            DataModel.getDataModel().addTimerMinute(timer);
            Utils.vibrationTime(v.getContext(), 10);
            Events.sendTimerEvent(R.string.action_add_minute, R.string.label_deskclock);

            final Context context = v.getContext();
            // Must use getTimer() because old timer is no longer accurate.
            final long currentTime = getTimer().getRemainingTime();
            if (currentTime > 0) {
                v.announceForAccessibility(TimerStringFormatter.formatString(context,
                        R.string.timer_accessibility_one_minute_added, currentTime, true));
            }
        };
        view.findViewById(R.id.add_one_min).setOnClickListener(mAddListener);
        view.findViewById(R.id.timer_label).setOnClickListener(v -> mTimerClickHandler.onEditLabelClicked(getTimer()));
        View.OnClickListener mPlayPauseListener = v -> {
            Utils.vibrationTime(v.getContext(), 50);
            final Timer clickedTimer = getTimer();
            if (clickedTimer.isPaused() || clickedTimer.isReset()) {
                DataModel.getDataModel().startTimer(clickedTimer);
            } else if (clickedTimer.isRunning()) {
                DataModel.getDataModel().pauseTimer(clickedTimer);
            } else if (clickedTimer.isExpired() || clickedTimer.isMissed()) {
                DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_deskclock);
            }
        };
        view.findViewById(R.id.play_pause).setOnClickListener(mPlayPauseListener);
        view.findViewById(R.id.close).setOnClickListener(v -> {
            DataModel.getDataModel().removeTimer(getTimer());
            Utils.vibrationTime(v.getContext(), 10);
        });
    }

    public void onBind(int timerId) {
        mTimerId = timerId;
        updateTime();
    }

    private void setLayoutParams(View view) {
        if (Utils.isTablet(view.getContext()) && Utils.isLandscape(view.getContext())) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            view.setLayoutParams(lp);
        }
    }

    /**
     * @return {@code true} if the timer is in a state that requires continuous updates
     */
    boolean updateTime() {
        final TimerItem view = mTimerItem;
        if (view != null) {
            final Timer timer = getTimer();
            view.update(timer);
            return !timer.isReset();
        }

        return false;
    }

    int getTimerId() {
        return mTimerId;
    }

    Timer getTimer() {
        return DataModel.getDataModel().getTimer(getTimerId());
    }

}
