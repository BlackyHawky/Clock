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
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_TIMEOUT_NEVER;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION;

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
import android.media.AudioManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
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
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.Utils;

import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
    private final View emptyView;
    private final TextView scheduleAlarm;
    private final TextView selectedDate;
    private final ImageView addDate;
    private final ImageView removeDate;
    private final TextView ringtone;
    private final CheckBox vibrate;
    private final CheckBox flash;
    private final CheckBox deleteOccasionalAlarmAfterUse;
    private final TextView autoSilenceDurationTitle;
    private final TextView autoSilenceDurationValue;
    private final TextView snoozeDurationTitle;
    private final TextView snoozeDurationValue;
    private final TextView crescendoDurationTitle;
    private final TextView crescendoDurationValue;
    private final TextView alarmVolumeTitle;
    private final TextView alarmVolumeValue;
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
        emptyView = itemView.findViewById(R.id.empty_view);
        scheduleAlarm = itemView.findViewById(R.id.schedule_alarm);
        selectedDate = itemView.findViewById(R.id.selected_date);
        addDate = itemView.findViewById(R.id.add_date);
        removeDate = itemView.findViewById(R.id.remove_date);
        ringtone = itemView.findViewById(R.id.choose_ringtone);
        vibrate = itemView.findViewById(R.id.vibrate_onoff);
        flash = itemView.findViewById(R.id.flash_onoff);
        deleteOccasionalAlarmAfterUse = itemView.findViewById(R.id.delete_occasional_alarm_after_use);
        autoSilenceDurationTitle = itemView.findViewById(R.id.auto_silence_duration_title);
        autoSilenceDurationValue = itemView.findViewById(R.id.auto_silence_duration_value);
        snoozeDurationTitle = itemView.findViewById(R.id.snooze_duration_title);
        snoozeDurationValue = itemView.findViewById(R.id.snooze_duration_value);
        crescendoDurationTitle = itemView.findViewById(R.id.crescendo_duration_title);
        crescendoDurationValue = itemView.findViewById(R.id.crescendo_duration_value);
        alarmVolumeTitle = itemView.findViewById(R.id.alarm_volume_title);
        alarmVolumeValue = itemView.findViewById(R.id.alarm_volume_value);
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
        editLabel.setOnClickListener(view ->
                getAlarmTimeClickHandler().onEditLabelClicked(getItemHolder().item));

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

        // Schedule date handler
        scheduleAlarm.setOnClickListener(v ->
                getAlarmTimeClickHandler().onDateClicked(getItemHolder().item));

        // Selected date handler
        selectedDate.setOnClickListener(v ->
                getAlarmTimeClickHandler().onDateClicked(getItemHolder().item));

        // Add date handler
        addDate.setOnClickListener(v ->
                getAlarmTimeClickHandler().onDateClicked(getItemHolder().item));

        // Remove date handler
        removeDate.setOnClickListener(v ->
                getAlarmTimeClickHandler().onRemoveDateClicked(getItemHolder().item));

        // Ringtone editor handler
        ringtone.setOnClickListener(v ->
                getAlarmTimeClickHandler().onRingtoneClicked(getItemHolder().item));

        // Vibrator checkbox handler
        vibrate.setOnClickListener(v ->
                getAlarmTimeClickHandler().setAlarmVibrationEnabled(
                        getItemHolder().item, ((CheckBox) v).isChecked()));

        // Flash checkbox handler
        flash.setOnClickListener(v ->
                getAlarmTimeClickHandler().setAlarmFlashEnabled(
                        getItemHolder().item, ((CheckBox) v).isChecked()));

        // Delete Occasional Alarm After Use checkbox handler
        deleteOccasionalAlarmAfterUse.setOnClickListener(v ->
                getAlarmTimeClickHandler().deleteOccasionalAlarmAfterUse(
                        getItemHolder().item, ((CheckBox) v).isChecked()));

        // Auto silence handler
        autoSilenceDurationTitle.setOnClickListener(v ->
                getAlarmTimeClickHandler().setAutoSilenceDuration(getItemHolder().item));

        // Auto silence handler
        autoSilenceDurationValue.setOnClickListener(v ->
                getAlarmTimeClickHandler().setAutoSilenceDuration(getItemHolder().item));

        // Snooze duration handler
        snoozeDurationTitle.setOnClickListener(v ->
                getAlarmTimeClickHandler().setSnoozeDuration(getItemHolder().item));

        // Snooze duration handler
        snoozeDurationValue.setOnClickListener(v ->
                getAlarmTimeClickHandler().setSnoozeDuration(getItemHolder().item));

        // Crescendo duration handler
        crescendoDurationTitle.setOnClickListener(v ->
                getAlarmTimeClickHandler().setCrescendoDuration(getItemHolder().item));

        // Crescendo duration handler
        crescendoDurationValue.setOnClickListener(v ->
                getAlarmTimeClickHandler().setCrescendoDuration(getItemHolder().item));

        // Volume handler
        alarmVolumeTitle.setOnClickListener(v ->
                getAlarmTimeClickHandler().setAlarmVolume(getItemHolder().item));

        // Volume handler
        alarmVolumeValue.setOnClickListener(v ->
                getAlarmTimeClickHandler().setAlarmVolume(getItemHolder().item));

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
        bindScheduleAlarm(alarm);
        bindSelectedDate(alarm);
        bindRingtone(context, alarm);
        bindVibrator(alarm);
        bindFlash(alarm);
        bindDeleteOccasionalAlarmAfterUse(alarm);
        bindEditLabelAnnotations(alarm);
        bindAutoSilenceValue(context, alarm);
        bindSnoozeValue(context, alarm);
        bindCrescendoValue(context, alarm);
        bindAlarmVolume(context, alarm);

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
        scheduleAlarm.setAlpha(1f);
        selectedDate.setAlpha(1f);
        addDate.setAlpha(1f);
        removeDate.setAlpha(1f);
        ringtone.setAlpha(1f);
        autoSilenceDurationTitle.setAlpha(1f);
        autoSilenceDurationValue.setAlpha(1f);
        snoozeDurationTitle.setAlpha(1f);
        snoozeDurationValue.setAlpha(1f);
        crescendoDurationTitle.setAlpha(1f);
        crescendoDurationValue.setAlpha(1f);
        alarmVolumeTitle.setAlpha(1f);
        alarmVolumeValue.setAlpha(1f);
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

    private void bindAutoSilenceValue(Context context, Alarm alarm) {
        int autoSilenceDuration = alarm.autoSilenceDuration;

        if (autoSilenceDuration == ALARM_TIMEOUT_NEVER) {
            autoSilenceDurationValue.setText(context.getString(R.string.auto_silence_never));
        } else if (autoSilenceDuration == ALARM_TIMEOUT_END_OF_RINGTONE) {
            autoSilenceDurationValue.setText(context.getString(R.string.auto_silence_end_of_ringtone));
        } else {
            autoSilenceDurationValue.setText(context.getResources().getQuantityString(
                    R.plurals.minutes_short, autoSilenceDuration, autoSilenceDuration));
        }
    }

    private void bindSnoozeValue(Context context, Alarm alarm) {
        int snoozeDuration = alarm.snoozeDuration;

        int h = snoozeDuration / 60;
        int m = snoozeDuration % 60;

        if (h > 0 && m > 0) {
            String hoursString = context.getResources().getQuantityString(R.plurals.hours_short, h, h);
            String minutesString = context.getResources().getQuantityString(R.plurals.minutes_short, m, m);
            snoozeDurationValue.setText(String.format("%s %s", hoursString, minutesString));
        } else if (h > 0) {
            snoozeDurationValue.setText(context.getResources().getQuantityString(R.plurals.hours_short, h, h));
        } else if (snoozeDuration == ALARM_SNOOZE_DURATION_DISABLED) {
            snoozeDurationValue.setText(context.getString(R.string.snooze_duration_none));
        } else {
            snoozeDurationValue.setText(context.getResources().getQuantityString(R.plurals.minutes_short, m, m));
        }
    }

    private void bindCrescendoValue(Context context, Alarm alarm) {
        int crescendoDuration = alarm.crescendoDuration;

        int m = crescendoDuration / 60;
        int s = crescendoDuration % 60;

        if (m > 0 && s > 0) {
            String minutesString = context.getResources().getQuantityString(R.plurals.minutes_short, m, m);
            String secondsString = s + " " + context.getString(R.string.seconds_label);
            crescendoDurationValue.setText(String.format("%s %s", minutesString, secondsString));
        } else if (m > 0) {
            crescendoDurationValue.setText(context.getResources().getQuantityString(R.plurals.minutes_short, m, m));
        } else if (crescendoDuration == DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION) {
            crescendoDurationValue.setText(context.getString(R.string.label_off));
        } else {
            String secondsString = s + " " + context.getString(R.string.seconds_label);
            crescendoDurationValue.setText(secondsString);
        }
    }

    private void bindAlarmVolume(Context context, Alarm alarm) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        final int currentVolume = alarm.alarmVolume;

        if (SettingsDAO.isPerAlarmVolumeEnabled(mPrefs)) {
            alarmVolumeTitle.setVisibility(VISIBLE);
            alarmVolumeValue.setVisibility(VISIBLE);

            int volumePercent = (int) (((float) currentVolume / maxVolume) * 100);
            String formatted = String.format(Locale.getDefault(), "%d%%", volumePercent);
            alarmVolumeValue.setText(formatted);

            Drawable icon = ContextCompat.getDrawable(context, volumePercent < 50
                    ? R.drawable.ic_volume_down
                    : R.drawable.ic_volume_up);

            if (icon != null) {
                alarmVolumeTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            }
        } else {
            alarmVolumeTitle.setVisibility(GONE);
            alarmVolumeValue.setVisibility(GONE);
        }
    }

    private void bindDaysOfWeekButtons(Alarm alarm, Context context) {
        final List<Integer> weekdays = SettingsDAO.getWeekdayOrder(mPrefs).getCalendarDays();
        for (int i = 0; i < weekdays.size(); i++) {
            final CompoundButton dayButton = dayButtons[i];
            if (alarm.daysOfWeek.isBitOn(weekdays.get(i))) {
                dayButton.setChecked(true);
                dayButton.setTextColor(MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorOnSurfaceInverse, Color.BLACK));

                selectedDate.setVisibility(GONE);
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorSurfaceInverse, Color.BLACK));

                selectedDate.setVisibility(VISIBLE);
            }
        }
    }

    private void bindScheduleAlarm(Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            scheduleAlarm.setVisibility(GONE);
        } else {
            scheduleAlarm.setVisibility(VISIBLE);
        }
    }

    private void bindSelectedDate(Alarm alarm) {
        int year = alarm.year;
        int month = alarm.month;
        int dayOfMonth = alarm.day;
        Calendar calendar = Calendar.getInstance();
        boolean isCurrentYear = year == calendar.get(Calendar.YEAR);

        calendar.set(year, month, dayOfMonth);

        String pattern = DateFormat.getBestDateTimePattern(
                Locale.getDefault(), isCurrentYear ? "MMMMd" : "yyyyMMMMd");
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        String formattedDate = dateFormat.format(calendar.getTime());

        if (alarm.daysOfWeek.isRepeating()) {
            repeatDays.setVisibility(VISIBLE);
            emptyView.setVisibility(GONE);
            selectedDate.setVisibility(GONE);
            addDate.setVisibility(GONE);
            removeDate.setVisibility(GONE);
        } else {
            if (alarm.isSpecifiedDate()) {
                if (alarm.isDateInThePast()) {
                    repeatDays.setVisibility(VISIBLE);
                    emptyView.setVisibility(GONE);
                    selectedDate.setVisibility(GONE);
                    addDate.setVisibility(VISIBLE);
                    removeDate.setVisibility(GONE);
                } else {
                    repeatDays.setVisibility(GONE);
                    emptyView.setVisibility(VISIBLE);
                    selectedDate.setText(formattedDate);
                    addDate.setVisibility(GONE);
                    removeDate.setVisibility(VISIBLE);
                }
            } else {
                repeatDays.setVisibility(VISIBLE);
                emptyView.setVisibility(GONE);
                selectedDate.setVisibility(GONE);
                addDate.setVisibility(VISIBLE);
                removeDate.setVisibility(GONE);
            }
        }
    }

    private void bindRingtone(Context context, Alarm alarm) {
        final String title = DataModel.getDataModel().getRingtoneTitle(alarm.alert);
        ringtone.setText(title);

        final String description = context.getString(R.string.ringtone_description);
        ringtone.setContentDescription(description + " " + title);

        final Drawable iconRingtone;
        if (RingtoneUtils.RINGTONE_SILENT.equals(alarm.alert)) {
            iconRingtone = AppCompatResources.getDrawable(context, R.drawable.ic_ringtone_silent);
        } else if (RingtoneUtils.isRandomRingtone(alarm.alert)
                || RingtoneUtils.isRandomCustomRingtone(alarm.alert)) {
            iconRingtone = AppCompatResources.getDrawable(context, R.drawable.ic_random);
        } else {
            iconRingtone = AppCompatResources.getDrawable(context, R.drawable.ic_ringtone);
        }

        ringtone.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRingtone, null, null, null);
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

        final Animator scheduleAlarmAnimation = ObjectAnimator.ofFloat(scheduleAlarm, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator selectedDateAnimation = ObjectAnimator.ofFloat(selectedDate, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator addDateAnimation = ObjectAnimator.ofFloat(addDate, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator removeDateAnimation = ObjectAnimator.ofFloat(removeDate, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator ringtoneAnimation = ObjectAnimator.ofFloat(ringtone, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator vibrateAnimation = ObjectAnimator.ofFloat(vibrate, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator flashAnimation = ObjectAnimator.ofFloat(flash, View.ALPHA, 0f)
                .setDuration(shortDuration);

        final Animator deleteOccasionalAlarmAfterUseAnimation = ObjectAnimator.ofFloat(
                deleteOccasionalAlarmAfterUse, View.ALPHA, 0f).setDuration(shortDuration);

        final Animator silenceAfterDurationTitleAnimation = ObjectAnimator.ofFloat(
                autoSilenceDurationTitle, View.ALPHA, 0f).setDuration(shortDuration);

        final Animator silenceAfterDurationValueAnimation = ObjectAnimator.ofFloat(
                autoSilenceDurationValue, View.ALPHA, 0f).setDuration(shortDuration);

        final Animator snoozeDurationTitleAnimation = ObjectAnimator.ofFloat(
                snoozeDurationTitle, View.ALPHA, 0f).setDuration(shortDuration);

        final Animator snoozeDurationValueAnimation = ObjectAnimator.ofFloat(
                snoozeDurationValue, View.ALPHA, 0f).setDuration(shortDuration);

        final Animator crescendoDurationTitleAnimation = ObjectAnimator.ofFloat(
                crescendoDurationTitle, View.ALPHA, 0f).setDuration(shortDuration);

        final Animator alarmVolumeTitleAnimation = ObjectAnimator.ofFloat(
                alarmVolumeTitle, View.ALPHA, 0f).setDuration(shortDuration);

        final Animator alarmVolumeValueAnimation = ObjectAnimator.ofFloat(
                alarmVolumeValue, View.ALPHA, 0f).setDuration(shortDuration);

        final Animator crescendoDurationValueAnimation = ObjectAnimator.ofFloat(
                crescendoDurationValue, View.ALPHA, 0f).setDuration(shortDuration);

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
        final boolean isAlarmVolumeTitleVisible = alarmVolumeTitle.getVisibility() == VISIBLE;
        final boolean preemptiveDismissButtonVisible = preemptiveDismissButton.getVisibility() == VISIBLE;

        editLabelIconAnimation.setStartDelay(startDelay);

        editLabelAnimation.setStartDelay(startDelay);

        duplicateAnimation.setStartDelay(startDelay);

        deleteAnimation.setStartDelay(startDelay);

        if (preemptiveDismissButtonVisible) {
            startDelay += delayIncrement;
            dismissAnimation.setStartDelay(startDelay);
        }

        if (isAlarmVolumeTitleVisible) {
            startDelay += delayIncrement;
            alarmVolumeTitleAnimation.setStartDelay(startDelay);
            alarmVolumeValueAnimation.setStartDelay(startDelay);
        }

        crescendoDurationTitleAnimation.setStartDelay(startDelay);

        crescendoDurationValueAnimation.setStartDelay(startDelay);

        snoozeDurationTitleAnimation.setStartDelay(startDelay);

        snoozeDurationValueAnimation.setStartDelay(startDelay);

        silenceAfterDurationTitleAnimation.setStartDelay(startDelay);

        silenceAfterDurationTitleAnimation.setStartDelay(startDelay);

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

        scheduleAlarmAnimation.setStartDelay(startDelay);

        selectedDateAnimation.setStartDelay(startDelay);

        addDateAnimation.setStartDelay(startDelay);

        removeDateAnimation.setStartDelay(startDelay);

        ringtoneAnimation.setStartDelay(startDelay);

        repeatDaysAnimation.setStartDelay(startDelay);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(backgroundAnimator, boundsAnimator, repeatDaysAnimation,
                editLabelAnimation, editLabelIconAnimation, flashAnimation,
                deleteOccasionalAlarmAfterUseAnimation, vibrateAnimation, ringtoneAnimation,
                deleteAnimation, duplicateAnimation, dismissAnimation, switchAnimator,
                clockAnimator, ellipseAnimator, scheduleAlarmAnimation, selectedDateAnimation,
                addDateAnimation, removeDateAnimation, snoozeDurationTitleAnimation,
                snoozeDurationValueAnimation, crescendoDurationTitleAnimation,
                crescendoDurationValueAnimation, silenceAfterDurationTitleAnimation,
                silenceAfterDurationValueAnimation, alarmVolumeTitleAnimation,
                alarmVolumeValueAnimation);

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
        scheduleAlarm.setAlpha(0f);
        selectedDate.setAlpha(0f);
        addDate.setAlpha(0f);
        removeDate.setAlpha(0f);
        ringtone.setAlpha(0f);
        preemptiveDismissButton.setAlpha(0f);
        vibrate.setAlpha(0f);
        flash.setAlpha(0f);
        deleteOccasionalAlarmAfterUse.setAlpha(0f);
        autoSilenceDurationTitle.setAlpha(0f);
        autoSilenceDurationValue.setAlpha(0f);
        snoozeDurationTitle.setAlpha(0f);
        snoozeDurationValue.setAlpha(0f);
        crescendoDurationTitle.setAlpha(0f);
        crescendoDurationValue.setAlpha(0f);
        alarmVolumeTitle.setAlpha(0f);
        alarmVolumeValue.setAlpha(0f);
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

        final Animator scheduleAlarmAnimation = ObjectAnimator.ofFloat(scheduleAlarm, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator selectedDateAnimation = ObjectAnimator.ofFloat(selectedDate, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator addDateAnimation = ObjectAnimator.ofFloat(addDate, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator removeDateAnimation = ObjectAnimator.ofFloat(removeDate, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator repeatDaysAnimation = ObjectAnimator.ofFloat(repeatDays, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator ringtoneAnimation = ObjectAnimator.ofFloat(ringtone, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator vibrateAnimation = ObjectAnimator.ofFloat(vibrate, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator flashAnimation = ObjectAnimator.ofFloat(flash, View.ALPHA, 1f)
                .setDuration(longDuration);

        final Animator deleteOccasionalAlarmAfterUseAnimation = ObjectAnimator.ofFloat(
                deleteOccasionalAlarmAfterUse, View.ALPHA, 1f).setDuration(longDuration);

        final Animator silenceAfterDurationTitleAnimation = ObjectAnimator.ofFloat(
                autoSilenceDurationTitle, View.ALPHA, 1f).setDuration(longDuration);

        final Animator silenceAfterDurationValueAnimation = ObjectAnimator.ofFloat(
                autoSilenceDurationValue, View.ALPHA, 1f).setDuration(longDuration);

        final Animator snoozeDurationTitleAnimation = ObjectAnimator.ofFloat(
                snoozeDurationTitle, View.ALPHA, 1f).setDuration(longDuration);

        final Animator snoozeDurationValueAnimation = ObjectAnimator.ofFloat(
                snoozeDurationValue, View.ALPHA, 1f).setDuration(longDuration);

        final Animator crescendoDurationTitleAnimation = ObjectAnimator.ofFloat(
                crescendoDurationTitle, View.ALPHA, 1f).setDuration(longDuration);

        final Animator crescendoDurationValueAnimation = ObjectAnimator.ofFloat(
                crescendoDurationValue, View.ALPHA, 1f).setDuration(longDuration);

        final Animator alarmVolumeTitleAnimation = ObjectAnimator.ofFloat(
                alarmVolumeTitle, View.ALPHA, 1f).setDuration(longDuration);

        final Animator alarmVolumeValueAnimation = ObjectAnimator.ofFloat(
                alarmVolumeValue, View.ALPHA, 1f).setDuration(longDuration);

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
        final boolean isAlarmVolumeTitleVisible = alarmVolumeTitle.getVisibility() == VISIBLE;
        final boolean preemptiveDismissButtonVisible = preemptiveDismissButton.getVisibility() == VISIBLE;

        editLabelIconAnimation.setStartDelay(startDelay);

        editLabelAnimation.setStartDelay(startDelay);

        repeatDaysAnimation.setStartDelay(startDelay);

        scheduleAlarmAnimation.setStartDelay(startDelay);

        selectedDateAnimation.setStartDelay(startDelay);

        addDateAnimation.setStartDelay(startDelay);

        removeDateAnimation.setStartDelay(startDelay);

        ringtoneAnimation.setStartDelay(startDelay);

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

        silenceAfterDurationTitleAnimation.setStartDelay(startDelay);

        silenceAfterDurationValueAnimation.setStartDelay(startDelay);

        snoozeDurationTitleAnimation.setStartDelay(startDelay);

        snoozeDurationValueAnimation.setStartDelay(startDelay);

        crescendoDurationTitleAnimation.setStartDelay(startDelay);

        crescendoDurationValueAnimation.setStartDelay(startDelay);

        if (isAlarmVolumeTitleVisible) {
            alarmVolumeTitleAnimation.setStartDelay(startDelay);
            alarmVolumeValueAnimation.setStartDelay(startDelay);
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
                deleteOccasionalAlarmAfterUseAnimation, ringtoneAnimation, deleteAnimation,
                duplicateAnimation, dismissAnimation, arrowAnimation, scheduleAlarmAnimation,
                selectedDateAnimation, addDateAnimation, removeDateAnimation,
                snoozeDurationTitleAnimation, snoozeDurationValueAnimation,
                crescendoDurationTitleAnimation, crescendoDurationValueAnimation,
                silenceAfterDurationTitleAnimation, silenceAfterDurationValueAnimation,
                alarmVolumeTitleAnimation, alarmVolumeValueAnimation);

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

        if (vibrate.getVisibility() == VISIBLE) {
            numberOfItems++;
        }

        if (flash.getVisibility() == VISIBLE) {
            numberOfItems++;
        }

        if (deleteOccasionalAlarmAfterUse.getVisibility() == VISIBLE) {
            numberOfItems++;
        }

        if (alarmVolumeTitle.getVisibility() == VISIBLE) {
            numberOfItems++;
        }

        if (alarmVolumeValue.getVisibility() == VISIBLE) {
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
