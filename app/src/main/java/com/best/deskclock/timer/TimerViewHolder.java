/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerStringFormatter;
import com.best.deskclock.events.Events;
import com.best.deskclock.utils.Utils;

import com.google.android.material.color.MaterialColors;

public class TimerViewHolder extends RecyclerView.ViewHolder {

    private int mTimerId;
    private final TimerItem mTimerItem;
    private final TimerClickHandler mTimerClickHandler;

    public TimerViewHolder(View view, TimerClickHandler timerClickHandler) {
        super(view);

        final Context context = view.getContext();

        view.setBackground(Utils.cardBackground(context));

        mTimerItem = (TimerItem) view;
        mTimerClickHandler = timerClickHandler;

        view.findViewById(R.id.reset).setOnClickListener(v -> {
            DataModel.getDataModel().resetOrDeleteTimer(getTimer(), R.string.label_deskclock);
            Utils.setVibrationTime(context, 10);
        });

        view.findViewById(R.id.timer_add_time_button).setOnClickListener(v -> {
            final Timer timer = getTimer();
            DataModel.getDataModel().addCustomTimeToTimer(timer);
            Utils.setVibrationTime(context, 10);
            Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, R.string.label_deskclock);

            // Must use getTimer() because old timer is no longer accurate.
            final long currentTime = getTimer().getRemainingTime();
            final String buttonTime = getTimer().getButtonTime();
            if (currentTime > 0) {
                v.announceForAccessibility(TimerStringFormatter.formatString(context,
                        R.string.timer_accessibility_custom_time_added, buttonTime, currentTime, true));
            }
        });

        view.findViewById(R.id.timer_add_time_button).setOnLongClickListener(v -> {
            mTimerClickHandler.onEditAddTimeButtonLongClicked(getTimer());
            return true;
        });

        view.findViewById(R.id.timer_label).setOnClickListener(v -> mTimerClickHandler.onEditLabelClicked(getTimer()));

        View.OnClickListener mPlayPauseListener = v -> {
            Utils.setVibrationTime(context, 50);
            final Timer clickedTimer = getTimer();
            if (clickedTimer.isPaused() || clickedTimer.isReset()) {
                DataModel.getDataModel().startTimer(clickedTimer);
            } else if (clickedTimer.isRunning()) {
                DataModel.getDataModel().pauseTimer(clickedTimer);
            } else if (clickedTimer.isExpired() || clickedTimer.isMissed()) {
                DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_deskclock);
            }
        };

        // If we click on the circular container when the phones (only) are in landscape mode,
        // indicating a title for the timers is not possible so in this case we click on the time text.
        if (!Utils.isTablet(context) && Utils.isLandscape(context)) {
            view.findViewById(R.id.timer_time_text).setOnClickListener(mPlayPauseListener);
        } else {
            view.findViewById(R.id.circle_container).setOnClickListener(mPlayPauseListener);
            view.findViewById(R.id.circle_container).setOnTouchListener(new Utils.CircleTouchListener());
        }

        view.findViewById(R.id.play_pause).setOnClickListener(mPlayPauseListener);

        view.findViewById(R.id.delete_timer).setOnClickListener(v -> {
            Utils.setVibrationTime(context, 10);

            final boolean isWarningDisplayedBeforeDeletingTimer =
                    DataModel.getDataModel().isWarningDisplayedBeforeDeletingTimer();

            if (isWarningDisplayedBeforeDeletingTimer) {
                final Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_delete);
                assert drawable != null;
                drawable.setTint(MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
                );
                // Get the title of the timer if there is one; otherwise, get the total duration.
                final String dialogMessage;
                if (getTimer().getLabel().isEmpty()) {
                    dialogMessage = context.getString(R.string.warning_dialog_message, getTimer().getTotalDuration());
                } else {
                    dialogMessage = context.getString(R.string.warning_dialog_message, getTimer().getLabel());
                }

                final AlertDialog dialog = new AlertDialog.Builder(context)
                        .setIcon(drawable)
                        .setTitle(R.string.warning_dialog_title)
                        .setMessage(dialogMessage)
                        .setPositiveButton(android.R.string.yes, (dialog1, which) ->
                                DataModel.getDataModel().removeTimer(getTimer()))
                        .setNegativeButton(android.R.string.no, null)
                        .create();
                dialog.show();
            } else {
                DataModel.getDataModel().removeTimer(getTimer());
            }
        });
    }

    public void onBind(int timerId) {
        mTimerId = timerId;
        updateTime();
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
