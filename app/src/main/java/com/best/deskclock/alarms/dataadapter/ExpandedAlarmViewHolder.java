/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms.dataadapter;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmTimeClickHandler;
import com.best.deskclock.bedtime.BedtimeFragment;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.Utils;

import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;

import java.util.List;

/**
 * A ViewHolder containing views for an alarm item in expanded state.
 */
public final class ExpandedAlarmViewHolder extends AlarmItemViewHolder {
    public static final int VIEW_TYPE = R.layout.alarm_time_expanded;

    public final LinearLayout repeatDays;
    public final CheckBox dismissAlarmWhenRingtoneEnds;
    public final CheckBox alarmSnoozeActions;
    public final CheckBox vibrate;
    public final CheckBox deleteOccasionalAlarmAfterUse;
    public final TextView ringtone;
    public final Chip delete;
    public final Chip duplicate;
    private final CompoundButton[] dayButtons = new CompoundButton[7];

    private final boolean mHasVibrator;

    private ExpandedAlarmViewHolder(View itemView, boolean hasVibrator) {
        super(itemView);

        mHasVibrator = hasVibrator;

        repeatDays = itemView.findViewById(R.id.repeat_days_alarm);
        ringtone = itemView.findViewById(R.id.choose_ringtone);
        delete = itemView.findViewById(R.id.delete);
        duplicate = itemView.findViewById(R.id.duplicate);
        dismissAlarmWhenRingtoneEnds = itemView.findViewById(R.id.dismiss_alarm_when_ringtone_ends_onoff);
        alarmSnoozeActions = itemView.findViewById(R.id.alarm_snooze_actions_onoff);
        vibrate = itemView.findViewById(R.id.vibrate_onoff);
        deleteOccasionalAlarmAfterUse = itemView.findViewById(R.id.delete_occasional_alarm_after_use);

        final Context context = itemView.getContext();

        // Build button for each day.
        final LayoutInflater inflater = LayoutInflater.from(context);
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < 7; i++) {
            final View dayButtonFrame = inflater.inflate(R.layout.day_button, repeatDays, false);
            final CompoundButton dayButton = dayButtonFrame.findViewById(R.id.day_button_box);
            final int weekday = weekdays.get(i);
            dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));
            repeatDays.addView(dayButtonFrame);
            dayButtons[i] = dayButton;
        }

        // Collapse handler
        itemView.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_collapse_implied, R.string.label_deskclock);
            getItemHolder().collapse();
        });

        arrow.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_collapse, R.string.label_deskclock);
            getItemHolder().collapse();
        });

        // Dismiss alarm when ringtone ends checkbox handler
        dismissAlarmWhenRingtoneEnds.setOnClickListener(v ->
                getAlarmTimeClickHandler().setDismissAlarmWhenRingtoneEndsEnabled(
                        getItemHolder().item, ((CheckBox) v).isChecked())
        );

        // Alarm snooze actions checkbox handler
        alarmSnoozeActions.setOnClickListener(v ->
                getAlarmTimeClickHandler().setAlarmSnoozeActionsEnabled(
                        getItemHolder().item, ((CheckBox) v).isChecked())
        );

        // Vibrator checkbox handler
        vibrate.setOnClickListener(v ->
                getAlarmTimeClickHandler().setAlarmVibrationEnabled(getItemHolder().item, ((CheckBox) v).isChecked()));

        deleteOccasionalAlarmAfterUse.setOnClickListener(v ->
                getAlarmTimeClickHandler().deleteOccasionalAlarmAfterUse(getItemHolder().item, ((CheckBox) v).isChecked()));

        // Ringtone editor handler
        ringtone.setOnClickListener(view -> getAlarmTimeClickHandler().onRingtoneClicked(context, getItemHolder().item));

        // Delete alarm handler
        delete.setOnClickListener(v -> {
            getAlarmTimeClickHandler().onDeleteClicked(getItemHolder());
            v.announceForAccessibility(context.getString(R.string.alarm_deleted));
        });

        // Duplicate alarm handler
        duplicate.setOnClickListener(v -> {
            getAlarmTimeClickHandler().onDuplicateClicked(getItemHolder());
            v.announceForAccessibility(context.getString(R.string.alarm_created));
        });

        // Day buttons handler
        for (int i = 0; i < dayButtons.length; i++) {
            final int buttonIndex = i;
            dayButtons[i].setOnClickListener(view -> {
                final boolean isChecked = ((CompoundButton) view).isChecked();
                getAlarmTimeClickHandler().setDayOfWeekEnabled(getItemHolder().item, isChecked, buttonIndex);
            });
        }

        itemView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        super.onBindItemView(itemHolder);

        final Alarm alarm = itemHolder.item;
        final Context context = itemView.getContext();
        bindDaysOfWeekButtons(alarm, context);
        bindRingtone(context, alarm);
        bindDismissAlarmWhenRingtoneEnds(alarm);
        bindAlarmSnoozeActions(alarm);
        bindVibrator(alarm);
        bindDeleteOccasionalAlarmAfterUse(alarm);
        bindDuplicateButton();
    }

    private void bindDaysOfWeekButtons(Alarm alarm, Context context) {
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < weekdays.size(); i++) {
            final CompoundButton dayButton = dayButtons[i];
            if (alarm.daysOfWeek.isBitOn(weekdays.get(i))) {
                dayButton.setChecked(true);
                dayButton.setTextColor(MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorOnSurfaceInverse, Color.BLACK));
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorSurfaceInverse, Color.BLACK));
            }
        }
    }

    private void bindRingtone(Context context, Alarm alarm) {
        final String title = DataModel.getDataModel().getRingtoneTitle(alarm.alert);
        ringtone.setText(title);

        final String description = context.getString(R.string.ringtone_description);
        ringtone.setContentDescription(description + " " + title);

        final boolean silent = Utils.RINGTONE_SILENT.equals(alarm.alert);
        final Drawable iconRingtone = silent
                ? AppCompatResources.getDrawable(context, R.drawable.ic_ringtone_silent)
                : AppCompatResources.getDrawable(context, R.drawable.ic_ringtone);
        ringtone.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRingtone, null, null, null);
    }

    private void bindDismissAlarmWhenRingtoneEnds(Alarm alarm) {
        final int timeoutMinutes = DataModel.getDataModel().getAlarmTimeout();
        if (timeoutMinutes == -2) {
            dismissAlarmWhenRingtoneEnds.setVisibility(GONE);
        } else {
            dismissAlarmWhenRingtoneEnds.setVisibility(VISIBLE);
            dismissAlarmWhenRingtoneEnds.setChecked(alarm.dismissAlarmWhenRingtoneEnds);
        }
    }

    private void bindAlarmSnoozeActions(Alarm alarm) {
        final int snoozeMinutes = DataModel.getDataModel().getSnoozeLength();
        if (snoozeMinutes == -1) {
            alarmSnoozeActions.setVisibility(GONE);
        } else {
            alarmSnoozeActions.setVisibility(VISIBLE);
            alarmSnoozeActions.setChecked(alarm.alarmSnoozeActions);
        }
    }

    private void bindVibrator(Alarm alarm) {
        if (mHasVibrator) {
            vibrate.setVisibility(VISIBLE);
            vibrate.setChecked(alarm.vibrate);
        } else {
            vibrate.setVisibility(GONE);
        }
    }

    private void bindDeleteOccasionalAlarmAfterUse(Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            deleteOccasionalAlarmAfterUse.setVisibility(GONE);
        } else {
            deleteOccasionalAlarmAfterUse.setVisibility(VISIBLE);
            deleteOccasionalAlarmAfterUse.setChecked(alarm.deleteAfterUse);
        }
    }

    private void bindDuplicateButton() {
        if (getItemHolder().item.equals(
                Alarm.getAlarmByLabel(itemView.getContext().getContentResolver(), BedtimeFragment.BEDTIME_LABEL))) {
            duplicate.setVisibility(INVISIBLE);
        } else {
            duplicate.setVisibility(VISIBLE);
        }
    }

    private AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return getItemHolder().getAlarmTimeClickHandler();
    }

    @Override
    public Animator onAnimateChange(List<Object> payloads, int fromLeft, int fromTop, int fromRight,
                                    int fromBottom, long duration) {
        /* There are no possible partial animations for expanded view holders. */
        return null;
    }

    @Override
    public Animator onAnimateChange(final ViewHolder oldHolder, ViewHolder newHolder, long duration) {
        if (!(oldHolder instanceof AlarmItemViewHolder) || !(newHolder instanceof AlarmItemViewHolder)) {
            return null;
        }

        final boolean isExpanding = this == newHolder;

        final Animator changeAnimatorSet = isExpanding
                ? createExpandingAnimator((AlarmItemViewHolder) oldHolder, duration)
                : createCollapsingAnimator((AlarmItemViewHolder) newHolder, duration);

        changeAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                arrow.jumpDrawablesToCurrentState();
            }
        });

        return changeAnimatorSet;
    }

    private Animator createCollapsingAnimator(AlarmItemViewHolder newHolder, long duration) {
        final View oldView = itemView;
        final Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(oldView, oldView, newHolder.itemView).setDuration(duration);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(boundsAnimator);
        return animatorSet;
    }

    private Animator createExpandingAnimator(AlarmItemViewHolder oldHolder, long duration) {
        final View newView = itemView;
        final Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(newView, oldHolder.itemView, newView).setDuration(duration);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(boundsAnimator);

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                AnimatorUtils.startDrawableAnimation(arrow);
            }
        });

        return animatorSet;
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {

        private final LayoutInflater mLayoutInflater;
        private final boolean mHasVibrator;

        public Factory(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            mHasVibrator = ((Vibrator) context.getSystemService(VIBRATOR_SERVICE)).hasVibrator();
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            final View itemView = mLayoutInflater.inflate(viewType, parent, false);
            return new ExpandedAlarmViewHolder(itemView, mHasVibrator);
        }
    }
}
