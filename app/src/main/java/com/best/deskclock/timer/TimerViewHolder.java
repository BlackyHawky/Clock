/*
 * Copyright (C) 2023 The LineageOS Project
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

package com.best.deskclock.timer;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.LabelDialogFragment;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerStringFormatter;
import com.best.deskclock.events.Events;

public class TimerViewHolder extends RecyclerView.ViewHolder {

    private int mTimerId;
    private TimerItem mTimerItem;
    private TimerClickHandler mTimerClickHandler;

    public TimerViewHolder(View view, TimerClickHandler timerClickHandler) {
        super(view);
        mTimerItem = (TimerItem) view;
        mTimerClickHandler = timerClickHandler;

        setLayoutParams(view);

        view.findViewById(R.id.reset).setOnClickListener(v ->
                DataModel.getDataModel().resetOrDeleteTimer(getTimer(), R.string.label_deskclock)
        );
        view.findViewById(R.id.add_one_min).setOnClickListener(mAddListener);
        view.findViewById(R.id.timer_label).setOnClickListener(v ->
                mTimerClickHandler.onEditLabelClicked(getTimer()));
        view.findViewById(R.id.play_pause).setOnClickListener(mPlayPauseListener);
        view.findViewById(R.id.close).setOnClickListener(v -> {
            DataModel.getDataModel().removeTimer(getTimer());
        });
    }

    public void onBind(int timerId) {
        mTimerId = timerId;
        updateTime();
    }

    private void setLayoutParams(View view) {
        Resources res = view.getContext().getResources();
        boolean isTablet = res.getBoolean(R.bool.rotateAlarmAlert);
        int orientation = res.getConfiguration().orientation;
        if (isTablet && orientation == Configuration.ORIENTATION_LANDSCAPE) {
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

    private final class ResetListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            DataModel.getDataModel().resetOrDeleteTimer(getTimer(), R.string.label_deskclock);
        }
    }

    private final View.OnClickListener mAddListener = v -> {
        final Timer timer = getTimer();
        DataModel.getDataModel().addTimerMinute(timer);
        Events.sendTimerEvent(R.string.action_add_minute, R.string.label_deskclock);

        final Context context = v.getContext();
        // Must use getTimer() because old timer is no longer accurate.
        final long currentTime = getTimer().getRemainingTime();
        if (currentTime > 0) {
            v.announceForAccessibility(TimerStringFormatter.formatString(
                    context, R.string.timer_accessibility_one_minute_added, currentTime,
                    true));
        }
    };

    private final View.OnClickListener mPlayPauseListener = v -> {
        final Timer clickedTimer = getTimer();
        if (clickedTimer.isPaused() || clickedTimer.isReset()) {
            DataModel.getDataModel().startTimer(clickedTimer);
        } else if (clickedTimer.isRunning()) {
            DataModel.getDataModel().pauseTimer(clickedTimer);
        } else if (clickedTimer.isExpired() || clickedTimer.isMissed()) {
            DataModel.getDataModel().resetOrDeleteTimer(clickedTimer, R.string.label_deskclock);
        }
    };
}
