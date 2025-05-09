/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms.dataadapter;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
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

    private final SharedPreferences mPrefs;
    private final ImageView editLabelIcon;
    private final TextView editLabel;
    private final LinearLayout repeatDays;
    private final CompoundButton[] dayButtons = new CompoundButton[7];
    private final TextView ringtone;
    private final CheckBox dismissAlarmWhenRingtoneEnds;
    private final CheckBox alarmSnoozeActions;
    private final CheckBox vibrate;
    private final CheckBox flash;
    private final CheckBox deleteOccasionalAlarmAfterUse;
    private final Chip delete;
    private final Chip duplicate;

    private final boolean mHasVibrator;
    private final boolean mHasFlash;

    private ExpandedAlarmViewHolder(View itemView, boolean hasVibrator, boolean hasFlash) {
        super(itemView);

        final Context context = itemView.getContext();
        mPrefs = getDefaultSharedPreferences(context);
        mHasVibrator = hasVibrator;
        mHasFlash = hasFlash;

        editLabelIcon = itemView.findViewById(R.id.edit_label_icon);
        editLabel = itemView.findViewById(R.id.edit_label);
        repeatDays = itemView.findViewById(R.id.repeat_days);
        ringtone = itemView.findViewById(R.id.choose_ringtone);
        dismissAlarmWhenRingtoneEnds = itemView.findViewById(R.id.dismiss_alarm_when_ringtone_ends_onoff);
        alarmSnoozeActions = itemView.findViewById(R.id.alarm_snooze_actions_onoff);
        vibrate = itemView.findViewById(R.id.vibrate_onoff);
        flash = itemView.findViewById(R.id.flash_onoff);
        deleteOccasionalAlarmAfterUse = itemView.findViewById(R.id.delete_occasional_alarm_after_use);
        delete = itemView.findViewById(R.id.delete);
        duplicate = itemView.findViewById(R.id.duplicate);

        // Collapse handler
        itemView.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_collapse_implied, R.string.label_deskclock);
            getItemHolder().collapse();
        });

        // Arrow handler
        arrow.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_collapse, R.string.label_deskclock);
            getItemHolder().collapse();
        });

        // Edit label handler
        editLabel.setOnClickListener(view -> getAlarmTimeClickHandler().onEditLabelClicked(getItemHolder().item));

        // Build button for each day.
        final LayoutInflater inflater = LayoutInflater.from(context);
        final List<Integer> weekdays = SettingsDAO.getWeekdayOrder(mPrefs).getCalendarDays();
        for (int i = 0; i < 7; i++) {
            final View dayButtonFrame = inflater.inflate(R.layout.day_button, repeatDays, false);
            final CompoundButton dayButton = dayButtonFrame.findViewById(R.id.day_button_box);
            final int weekday = weekdays.get(i);
            dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));
            repeatDays.addView(dayButtonFrame);
            dayButtons[i] = dayButton;
        }

        // Day buttons handler
        for (int i = 0; i < dayButtons.length; i++) {
            final int buttonIndex = i;
            dayButtons[i].setOnClickListener(view -> {
                final boolean isChecked = ((CompoundButton) view).isChecked();
                getAlarmTimeClickHandler().setDayOfWeekEnabled(getItemHolder().item,
                        isChecked, buttonIndex);
            });
        }

        // Ringtone editor handler
        ringtone.setOnClickListener(v -> getAlarmTimeClickHandler().onRingtoneClicked(context, getItemHolder().item));

        // Dismiss alarm when ringtone ends checkbox handler
        dismissAlarmWhenRingtoneEnds.setOnClickListener(v ->
                getAlarmTimeClickHandler().setDismissAlarmWhenRingtoneEndsEnabled(
                        getItemHolder().item, ((CheckBox) v).isChecked())
        );

        // Alarm snooze actions checkbox handler
        alarmSnoozeActions.setOnClickListener(v -> getAlarmTimeClickHandler().setAlarmSnoozeActionsEnabled(
                getItemHolder().item, ((CheckBox) v).isChecked())
        );

        // Vibrator checkbox handler
        vibrate.setOnClickListener(v ->
                getAlarmTimeClickHandler().setAlarmVibrationEnabled(getItemHolder().item, ((CheckBox) v).isChecked()));

        // Flash checkbox handler
        flash.setOnClickListener(v ->
                getAlarmTimeClickHandler().setAlarmFlashEnabled(getItemHolder().item, ((CheckBox) v).isChecked()));

        // Delete Occasional Alarm After Use checkbox handler
        deleteOccasionalAlarmAfterUse.setOnClickListener(v ->
                getAlarmTimeClickHandler().deleteOccasionalAlarmAfterUse(getItemHolder().item, ((CheckBox) v).isChecked()));

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

        itemView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        super.onBindItemView(itemHolder);

        final Alarm alarm = itemHolder.item;
        final Context context = itemView.getContext();
        bindEditLabel(context, alarm);
        bindDaysOfWeekButtons(alarm, context);
        bindRingtone(context, alarm);
        bindDismissAlarmWhenRingtoneEnds(alarm);
        bindAlarmSnoozeActions(alarm);
        bindVibrator(alarm);
        bindFlash(alarm);
        bindDeleteOccasionalAlarmAfterUse(alarm);
        bindEditLabelAnnotations(alarm);

        // If this view is bound without coming from a CollapsedAlarmViewHolder (e.g.
        // when calling expand() before this alarm was visible in it's collapsed state),
        // the animation listeners won't do the showing and therefore lead to unwanted
        // half-visible state
        arrow.setVisibility(VISIBLE);
        clock.setVisibility(VISIBLE);
        onOff.setVisibility(VISIBLE);
        daysOfWeek.setVisibility(VISIBLE);
        editLabelIcon.setAlpha(1f);
        // Necessary so that the hint keeps its own alpha value
        // and to avoid flickering when turning the alarm on/off
        final boolean labelIsEmpty = alarm.label == null || alarm.label.isEmpty();
        editLabel.setAlpha(labelIsEmpty || alarm.enabled ? 1f : editLabel.getAlpha());
        repeatDays.setAlpha(1f);
        ringtone.setAlpha(1f);
        dismissAlarmWhenRingtoneEnds.setAlpha(1f);
        alarmSnoozeActions.setAlpha(1f);
        preemptiveDismissButton.setAlpha(1f);
        vibrate.setAlpha(1f);
        flash.setAlpha(1f);
        deleteOccasionalAlarmAfterUse.setAlpha(1f);
        delete.setAlpha(1f);
        duplicate.setAlpha(1f);
    }

    private void bindEditLabel(Context context, Alarm alarm) {
        final boolean alarmLabelIsEmpty = alarm.label == null || alarm.label.isEmpty();

        editLabel.setText(alarm.label);
        editLabel.setTypeface(alarmLabelIsEmpty || !alarm.enabled ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);
        editLabel.setContentDescription(alarmLabelIsEmpty
                ? context.getString(R.string.no_label_specified)
                : context.getString(R.string.label_description) + " " + alarm.label);
    }

    private void bindDaysOfWeekButtons(Alarm alarm, Context context) {
        final List<Integer> weekdays = SettingsDAO.getWeekdayOrder(mPrefs).getCalendarDays();
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
        final int timeoutMinutes = SettingsDAO.getAlarmTimeout(mPrefs);
        if (timeoutMinutes == -2) {
            dismissAlarmWhenRingtoneEnds.setVisibility(GONE);
        } else {
            dismissAlarmWhenRingtoneEnds.setVisibility(VISIBLE);
            dismissAlarmWhenRingtoneEnds.setChecked(alarm.dismissAlarmWhenRingtoneEnds);
        }
    }

    private void bindAlarmSnoozeActions(Alarm alarm) {
        final int snoozeMinutes = SettingsDAO.getSnoozeLength(mPrefs);
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

    private void bindFlash(Alarm alarm) {
        if (mHasFlash) {
            flash.setVisibility(VISIBLE);
            flash.setChecked(alarm.flash);
        } else {
            flash.setVisibility(GONE);
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

    private void bindEditLabelAnnotations(Alarm alarm) {
        final boolean labelIsEmpty = alarm.label == null || alarm.label.isEmpty();
        final float labelAlpha = labelIsEmpty ? 1f : editLabel.getAlpha();
        annotationsAlpha = alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA;

        if (!labelIsEmpty) {
            ObjectAnimator editLabelAlphaAnimator = ObjectAnimator.ofFloat(editLabel,
                    View.ALPHA, labelAlpha, annotationsAlpha).setDuration(300);

            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(editLabelAlphaAnimator);

            animatorSet.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    // Prevent the alarm item from collapsing while this animation is running
                    // to avoid display bugs
                    itemView.setOnClickListener(null);
                    arrow.setOnClickListener(null);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);

                    itemView.setOnClickListener(v -> {
                        Events.sendAlarmEvent(R.string.action_collapse_implied, R.string.label_deskclock);
                        getItemHolder().collapse();
                    });

                    arrow.setOnClickListener(v -> {
                        Events.sendAlarmEvent(R.string.action_collapse, R.string.label_deskclock);
                        getItemHolder().collapse();
                    });
                }
            });

            animatorSet.start();
        }
    }

    @Override
    public Animator onAnimateChange(final ViewHolder oldHolder, ViewHolder newHolder, long duration) {
        if (!(oldHolder instanceof AlarmItemViewHolder) || !(newHolder instanceof AlarmItemViewHolder)) {
            return null;
        }

        final boolean isExpanding = this == newHolder;

        AnimatorUtils.setBackgroundAlpha(itemView, isExpanding ? 0 : 255);

        setChangingViewsAlpha(isExpanding ? 0f : annotationsAlpha);

        final Animator changeAnimatorSet = isExpanding
                ? createExpandingAnimator((AlarmItemViewHolder) oldHolder, duration)
                : createCollapsingAnimator((AlarmItemViewHolder) newHolder, duration);

        changeAnimatorSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                AnimatorUtils.setBackgroundAlpha(itemView, 255);
                arrow.setTranslationY(0f);
                setChangingViewsAlpha(annotationsAlpha);
                arrow.jumpDrawablesToCurrentState();
                arrow.setVisibility(isExpanding ? VISIBLE : INVISIBLE);
                clock.setVisibility(isExpanding ? VISIBLE : INVISIBLE);
                onOff.setVisibility(isExpanding ? VISIBLE : INVISIBLE);
                daysOfWeek.setVisibility(isExpanding ? VISIBLE : INVISIBLE);
            }
        });

        return changeAnimatorSet;
    }

    private Animator createCollapsingAnimator(AlarmItemViewHolder newHolder, long duration) {
        final Animator backgroundAnimator = ObjectAnimator.ofPropertyValuesHolder(itemView,
                PropertyValuesHolder.ofInt(AnimatorUtils.BACKGROUND_ALPHA, 255, 0));
        backgroundAnimator.setDuration(duration);

        final Animator boundsAnimator = getBoundsAnimator(itemView, newHolder.itemView, duration);
        final Animator switchAnimator = getBoundsAnimator(onOff, newHolder.onOff, duration);
        final Animator clockAnimator = getBoundsAnimator(clock, newHolder.clock, duration);
        final Animator ellipseAnimator = getBoundsAnimator(daysOfWeek, newHolder.daysOfWeek, duration);

        final long shortDuration = (long) (duration * ANIM_SHORT_DURATION_MULTIPLIER);

        final Animator editLabelIconAnimation = ObjectAnimator.ofFloat(editLabelIcon, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator editLabelAnimation = ObjectAnimator.ofFloat(editLabel, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator repeatDaysAnimation = ObjectAnimator.ofFloat(repeatDays, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator ringtoneAnimation = ObjectAnimator.ofFloat(ringtone, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator dismissAlarmWhenRingtoneEndsAnimation = ObjectAnimator.ofFloat(
                dismissAlarmWhenRingtoneEnds, View.ALPHA, 0f).setDuration(shortDuration);

        final Animator alarmSnoozeActionsAnimation = ObjectAnimator.ofFloat(alarmSnoozeActions, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator vibrateAnimation = ObjectAnimator.ofFloat(vibrate, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator flashAnimation = ObjectAnimator.ofFloat(flash, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator deleteOccasionalAlarmAfterUseAnimation = ObjectAnimator.ofFloat(
                deleteOccasionalAlarmAfterUse, View.ALPHA, 0f).setDuration(shortDuration);

        final Animator dismissAnimation = ObjectAnimator.ofFloat(preemptiveDismissButton,
                View.ALPHA, 0f).setDuration(shortDuration);

        final Animator deleteAnimation = ObjectAnimator.ofFloat(delete, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator duplicateAnimation = ObjectAnimator.ofFloat(duplicate, View.ALPHA, 0f)
                .setDuration(shortDuration);

        // Set the staggered delays; use the first portion (duration * (1 - 1/4 - 1/6)) of the time,
        // so that the final animation, with a duration of 1/4 the total duration, finishes exactly
        // before the collapsed holder begins expanding.
        long startDelay = 0L;
        final int numberOfItems = countNumberOfItems();
        final long delayIncrement = (long) (duration * ANIM_LONG_DELAY_INCREMENT_MULTIPLIER) / (numberOfItems - 1);
        final boolean vibrateVisible = vibrate.getVisibility() == VISIBLE;
        final boolean flashVisible = flash.getVisibility() == VISIBLE;
        final boolean deleteOccasionalAlarmAfterUseVisible = deleteOccasionalAlarmAfterUse.getVisibility() == VISIBLE;
        final boolean dismissAlarmWhenRingtoneEndsVisible = dismissAlarmWhenRingtoneEnds.getVisibility() == VISIBLE;
        final boolean alarmSnoozeActionsVisible = alarmSnoozeActions.getVisibility() == VISIBLE;
        final boolean preemptiveDismissButtonVisible = preemptiveDismissButton.getVisibility() == VISIBLE;

        editLabelIconAnimation.setStartDelay(startDelay);

        editLabelAnimation.setStartDelay(startDelay);

        duplicateAnimation.setStartDelay(startDelay);

        deleteAnimation.setStartDelay(startDelay);

        if (preemptiveDismissButtonVisible) {
            startDelay += delayIncrement;
            dismissAnimation.setStartDelay(startDelay);
        }

        if (deleteOccasionalAlarmAfterUseVisible) {
            startDelay += delayIncrement;
            deleteOccasionalAlarmAfterUseAnimation.setStartDelay(startDelay);
        }

        if (flashVisible) {
            startDelay += delayIncrement;
            flashAnimation.setStartDelay(startDelay);
        }

        if (vibrateVisible) {
            startDelay += delayIncrement;
            vibrateAnimation.setStartDelay(startDelay);
        }

        if (alarmSnoozeActionsVisible) {
            startDelay += delayIncrement;
            alarmSnoozeActionsAnimation.setStartDelay(startDelay);
        }

        if (dismissAlarmWhenRingtoneEndsVisible) {
            startDelay += delayIncrement;
            dismissAlarmWhenRingtoneEndsAnimation.setStartDelay(startDelay);
        }

        ringtoneAnimation.setStartDelay(startDelay);

        repeatDaysAnimation.setStartDelay(startDelay);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(backgroundAnimator, boundsAnimator, repeatDaysAnimation,
                editLabelAnimation, editLabelIconAnimation, flashAnimation,
                dismissAlarmWhenRingtoneEndsAnimation, deleteOccasionalAlarmAfterUseAnimation,
                vibrateAnimation, ringtoneAnimation, deleteAnimation, duplicateAnimation,
                dismissAnimation, alarmSnoozeActionsAnimation, switchAnimator, clockAnimator,
                ellipseAnimator);

        animatorSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                newHolder.clock.setVisibility(INVISIBLE);
                newHolder.onOff.setVisibility(INVISIBLE);
                newHolder.arrow.setVisibility(INVISIBLE);
                newHolder.daysOfWeek.setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                newHolder.clock.setVisibility(VISIBLE);
                newHolder.onOff.setVisibility(VISIBLE);
                newHolder.arrow.setVisibility(VISIBLE);
                newHolder.daysOfWeek.setVisibility(VISIBLE);
            }
        });

        return animatorSet;
    }

    private Animator createExpandingAnimator(AlarmItemViewHolder oldHolder, long duration) {
        arrow.setVisibility(INVISIBLE);
        clock.setVisibility(INVISIBLE);
        onOff.setVisibility(INVISIBLE);
        daysOfWeek.setVisibility(INVISIBLE);
        ringtone.setAlpha(0f);
        preemptiveDismissButton.setAlpha(0f);
        dismissAlarmWhenRingtoneEnds.setAlpha(0f);
        alarmSnoozeActions.setAlpha(0f);
        vibrate.setAlpha(0f);
        flash.setAlpha(0f);
        deleteOccasionalAlarmAfterUse.setAlpha(0f);
        delete.setAlpha(0f);
        duplicate.setAlpha(0f);
        setChangingViewsAlpha(0f);

        final View newView = itemView;

        final Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(newView, oldHolder.itemView, newView);
        boundsAnimator.setDuration(duration);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final Animator backgroundAnimator = ObjectAnimator.ofPropertyValuesHolder(newView,
                PropertyValuesHolder.ofInt(AnimatorUtils.BACKGROUND_ALPHA, 0, 255));
        backgroundAnimator.setDuration(duration);

        final long longDuration = (long) (duration * ANIM_LONG_DURATION_MULTIPLIER);

        final Animator editLabelAnimation = ObjectAnimator.ofFloat(editLabel, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator editLabelIconAnimation = ObjectAnimator.ofFloat(editLabelIcon, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator repeatDaysAnimation = ObjectAnimator.ofFloat(repeatDays, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator ringtoneAnimation = ObjectAnimator.ofFloat(ringtone, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator dismissAlarmWhenRingtoneEndsAnimation = ObjectAnimator.ofFloat(
                dismissAlarmWhenRingtoneEnds, View.ALPHA, 1f).setDuration(longDuration);

        final Animator alarmSnoozeActionsAnimation = ObjectAnimator.ofFloat(
                alarmSnoozeActions, View.ALPHA, 1f).setDuration(longDuration);

        final Animator vibrateAnimation = ObjectAnimator.ofFloat(vibrate, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator flashAnimation = ObjectAnimator.ofFloat(flash, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator deleteOccasionalAlarmAfterUseAnimation = ObjectAnimator.ofFloat(
                deleteOccasionalAlarmAfterUse, View.ALPHA, 1f).setDuration(longDuration);

        final Animator dismissAnimation = ObjectAnimator.ofFloat(preemptiveDismissButton, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator deleteAnimation = ObjectAnimator.ofFloat(delete, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator duplicateAnimation = ObjectAnimator.ofFloat(duplicate, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator arrowAnimation = ObjectAnimator.ofFloat(arrow, View.TRANSLATION_Y, 0f)
                .setDuration(duration);

        arrowAnimation.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        // Set the stagger delays; delay the first by the amount of time it takes for the collapse
        // to complete, then stagger the expansion with the remaining time.
        long startDelay = (long) (duration * ANIM_STANDARD_DELAY_MULTIPLIER);
        final int numberOfItems = countNumberOfItems();
        final long delayIncrement = (long) (duration * ANIM_SHORT_DELAY_INCREMENT_MULTIPLIER) / (numberOfItems - 1);
        final boolean vibrateVisible = vibrate.getVisibility() == VISIBLE;
        final boolean flashVisible = flash.getVisibility() == VISIBLE;
        final boolean deleteOccasionalAlarmAfterUseVisible = deleteOccasionalAlarmAfterUse.getVisibility() == VISIBLE;
        final boolean dismissAlarmWhenRingtoneEndsVisible = dismissAlarmWhenRingtoneEnds.getVisibility() == VISIBLE;
        final boolean alarmSnoozeActionsVisible = alarmSnoozeActions.getVisibility() == VISIBLE;
        final boolean preemptiveDismissButtonVisible = preemptiveDismissButton.getVisibility() == VISIBLE;

        editLabelIconAnimation.setStartDelay(startDelay);

        editLabelAnimation.setStartDelay(startDelay);

        repeatDaysAnimation.setStartDelay(startDelay);

        ringtoneAnimation.setStartDelay(startDelay);

        if (dismissAlarmWhenRingtoneEndsVisible) {
            dismissAlarmWhenRingtoneEndsAnimation.setStartDelay(startDelay);
            startDelay += delayIncrement;
        }

        if (alarmSnoozeActionsVisible) {
            alarmSnoozeActionsAnimation.setStartDelay(startDelay);
            startDelay += delayIncrement;
        }
        if (vibrateVisible) {
            vibrateAnimation.setStartDelay(startDelay);
            startDelay += delayIncrement;
        }

        if (flashVisible) {
            flashAnimation.setStartDelay(startDelay);
            startDelay += delayIncrement;
        }

        if (deleteOccasionalAlarmAfterUseVisible) {
            deleteOccasionalAlarmAfterUseAnimation.setStartDelay(startDelay);
            startDelay += delayIncrement;
        }

        if (preemptiveDismissButtonVisible) {
            dismissAnimation.setStartDelay(startDelay);
            startDelay += delayIncrement;
        }

        deleteAnimation.setStartDelay(startDelay);

        duplicateAnimation.setStartDelay(startDelay);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(backgroundAnimator, boundsAnimator, repeatDaysAnimation,
                editLabelAnimation, editLabelIconAnimation, flashAnimation, vibrateAnimation,
                dismissAlarmWhenRingtoneEndsAnimation, deleteOccasionalAlarmAfterUseAnimation,
                ringtoneAnimation, deleteAnimation, duplicateAnimation, dismissAnimation,
                alarmSnoozeActionsAnimation, arrowAnimation);

        animatorSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                AnimatorUtils.startDrawableAnimation(arrow);
                // Allow text scrolling (all other attributes are indicated in the "alarm_time_expanded.xml" file)
                ringtone.setSelected(true);
            }
        });

        return animatorSet;
    }

    private int countNumberOfItems() {
        // Always between 4 and 10 items.
        int numberOfItems = 4;

        if (preemptiveDismissButton.getVisibility() == VISIBLE) {
            numberOfItems++;
        }

        if (dismissAlarmWhenRingtoneEnds.getVisibility() == VISIBLE) {
            numberOfItems++;
        }

        if (alarmSnoozeActions.getVisibility() == VISIBLE) {
            numberOfItems++;
        }

        if (vibrate.getVisibility() == VISIBLE) {
            numberOfItems++;
        }

        if (flash.getVisibility() == VISIBLE) {
            numberOfItems++;
        }

        if (deleteOccasionalAlarmAfterUse.getVisibility() == VISIBLE) {
            numberOfItems++;
        }

        return numberOfItems;
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {

        private final LayoutInflater mLayoutInflater;
        private final boolean mHasVibrator;
        private final boolean mHasFlash;

        public Factory(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            mHasVibrator = Utils.hasVibrator(context);
            mHasFlash = AlarmUtils.hasBackFlash(context);
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            final View itemView = mLayoutInflater.inflate(viewType, parent, false);
            return new ExpandedAlarmViewHolder(itemView, mHasVibrator, mHasFlash);
        }
    }
}
