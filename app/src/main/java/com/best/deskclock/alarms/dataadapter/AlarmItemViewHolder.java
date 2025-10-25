/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms.dataadapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.ItemAnimator;
import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmTimeClickHandler;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uicomponents.TextTime;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Abstract ViewHolder for alarm time items.
 */
public abstract class AlarmItemViewHolder extends ItemAdapter.ItemViewHolder<AlarmItemHolder>
        implements ItemAnimator.OnAnimateChangeListener {

    public static final float CLOCK_ENABLED_ALPHA = 1f;
    public static final float CLOCK_DISABLED_ALPHA = 0.63f;

    public static final float ANIM_STANDARD_DELAY_MULTIPLIER = 1f / 6f;
    public static final float ANIM_LONG_DURATION_MULTIPLIER = 2f / 3f;
    public static final float ANIM_SHORT_DURATION_MULTIPLIER = 1f / 4f;
    public static final float ANIM_SHORT_DELAY_INCREMENT_MULTIPLIER =
            1f - ANIM_LONG_DURATION_MULTIPLIER - ANIM_SHORT_DURATION_MULTIPLIER;
    public static final float ANIM_LONG_DELAY_INCREMENT_MULTIPLIER =
            1f - ANIM_STANDARD_DELAY_MULTIPLIER - ANIM_SHORT_DURATION_MULTIPLIER;

    public final SharedPreferences mPrefs;
    public final ImageView arrow;
    public final TextTime clock;
    public final CompoundButton onOff;
    public final TextView daysOfWeek;
    public final TextView preemptiveDismissButton;
    public final View bottomPaddingView;

    public float annotationsAlpha = CLOCK_ENABLED_ALPHA;

    public AlarmItemViewHolder(View itemView) {
        super(itemView);

        final Context context = itemView.getContext();

        mPrefs = getDefaultSharedPreferences(context);
        arrow = itemView.findViewById(R.id.arrow);
        clock = itemView.findViewById(R.id.digital_clock);
        onOff = itemView.findViewById(R.id.onoff);
        daysOfWeek = itemView.findViewById(R.id.days_of_week);
        preemptiveDismissButton = itemView.findViewById(R.id.preemptive_dismiss_button);
        bottomPaddingView = itemView.findViewById(R.id.bottom_padding_view);

        int rippleColor = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorControlHighlight, Color.BLACK);
        RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(rippleColor),
                ThemeUtils.cardBackground(context), null);
        itemView.setBackground(rippleDrawable);

        // Clock handler
        clock.setOnClickListener(v -> getAlarmTimeClickHandler().onClockClicked(getItemHolder().item));
        clock.setOnLongClickListener(v -> {
            getAlarmTimeClickHandler().onClockLongClicked(getItemHolder().item);
            return true;
        });

        // On/Off button handler
        onOff.setOnCheckedChangeListener((compoundButton, checked) ->
                getItemHolder().getAlarmTimeClickHandler().setAlarmEnabled(getItemHolder().item, checked));

        // Preemptive dismiss button handler
        preemptiveDismissButton.setOnClickListener(v -> {
            final AlarmInstance alarmInstance = getItemHolder().getAlarmInstance();
            if (alarmInstance != null) {
                getItemHolder().getAlarmTimeClickHandler().dismissAlarmInstance(getItemHolder().item, alarmInstance);
            }
        });
    }

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        final Alarm alarm = itemHolder.item;
        final AlarmInstance alarmInstance = itemHolder.getAlarmInstance();
        final Context context = itemView.getContext();

        bindClock(alarm);
        bindOnOffSwitch(alarm);
        bindRepeatText(context, alarm, alarmInstance);
        bindPreemptiveDismissButton(context, alarm, alarmInstance);
        bindAnnotations(alarm);

        itemView.setContentDescription(clock.getText() + " " + alarm.getLabelOrDefault(context));
    }

    private void bindOnOffSwitch(Alarm alarm) {
        if (onOff.isChecked() != alarm.enabled) {
            onOff.setChecked(alarm.enabled);
        }
    }

    private void bindClock(Alarm alarm) {
        clock.setTime(alarm.hour, alarm.minutes);
        clock.setTypeface(alarm.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }

    private void bindRepeatText(Context context, Alarm alarm, AlarmInstance alarmInstance) {
        if (alarm.daysOfWeek.isRepeating()) {
            setRepeatingDaysDescription(context, alarm, alarmInstance);
        } else if (alarm.isSpecifiedDate()) {
            setSpecifiedDateDescription(context, alarm);
        } else {
            setNonRepeatingDefaultDescription(context, alarm);
        }
    }

    private void setRepeatingDaysDescription(Context context, Alarm alarm, AlarmInstance alarmInstance) {
        Weekdays.Order weekdayOrder = SettingsDAO.getWeekdayOrder(mPrefs);
        String contentDesc = alarm.daysOfWeek.toAccessibilityString(context, weekdayOrder);
        CharSequence styledDaysText;

        if (alarm.enabled) {
            int nextAlarmDay = alarm.getNextAlarmDayOfWeek(alarmInstance);

            if (alarm.daysOfWeek.isAllDaysSelected()) {
                if (alarm.isRepeatDayStyleEnabled(mPrefs)) {
                    styledDaysText = alarm.daysOfWeek.toStyledString(context, weekdayOrder, false, nextAlarmDay);
                } else {
                    styledDaysText = alarm.daysOfWeek.toString(context, weekdayOrder);
                }
            } else {
                styledDaysText = alarm.daysOfWeek.toStyledString(context, weekdayOrder, false, nextAlarmDay);
            }
        } else {
            styledDaysText = alarm.daysOfWeek.toString(context, weekdayOrder);
        }

        daysOfWeek.setText(styledDaysText);
        daysOfWeek.setContentDescription(contentDesc);
    }

    private void setNonRepeatingDefaultDescription(Context context, Alarm alarm) {
        if (Alarm.isTomorrow(alarm, Calendar.getInstance())) {
            daysOfWeek.setText(context.getString(R.string.alarm_tomorrow));
        } else {
            daysOfWeek.setText(context.getString(R.string.alarm_today));
        }
    }

    private void setSpecifiedDateDescription(Context context, Alarm alarm) {
        Calendar calendar = Calendar.getInstance();

        if (Alarm.isSpecifiedDateTomorrow(alarm.year, alarm.month, alarm.day)) {
            daysOfWeek.setText(context.getString(R.string.alarm_tomorrow));
        } else if (alarm.isDateInThePast()) {
            daysOfWeek.setText(getTodayOrTomorrowBasedOnTime(context, alarm, calendar));
        } else {
            daysOfWeek.setText(context.getString(R.string.alarm_scheduled_for, formatAlarmDate(alarm)));
        }
    }

    private String getTodayOrTomorrowBasedOnTime(Context context, Alarm alarm, Calendar now) {
        // Used when the date has passed, the new alarm will be scheduled either the same day
        // or the next day depending on the time.
        // The text is therefore updated accordingly.
        return context.getString(alarm.isTimeBeforeOrEqual(now)
                ? R.string.alarm_tomorrow
                : R.string.alarm_today);
    }

    private void bindPreemptiveDismissButton(Context context, Alarm alarm, AlarmInstance alarmInstance) {
        final boolean canBind = alarm.canPreemptivelyDismiss(context) && alarmInstance != null;

        if (canBind) {
            preemptiveDismissButton.setVisibility(VISIBLE);
            final String dismissText = alarm.instanceState == AlarmInstance.SNOOZE_STATE
                    ? context.getString(R.string.alarm_alert_snooze_until,
                    AlarmUtils.getAlarmText(context, alarmInstance, false))
                    : alarm.deleteAfterUse && !alarm.daysOfWeek.isRepeating()
                    ? context.getString(R.string.alarm_alert_dismiss_and_delete_text)
                    : context.getString(R.string.alarm_alert_dismiss_text);
            preemptiveDismissButton.setText(dismissText);

            if (!getItemHolder().isExpanded()) {
                bottomPaddingView.setVisibility(GONE);
            }
        } else {
            preemptiveDismissButton.setVisibility(GONE);

            if (!getItemHolder().isExpanded()) {
                bottomPaddingView.setVisibility(VISIBLE);
            }
        }
    }

    private void bindAnnotations(Alarm alarm) {
        annotationsAlpha = alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA;

        ObjectAnimator clockAlphaAnimator = ObjectAnimator.ofFloat(clock,
                View.ALPHA, clock.getAlpha(), annotationsAlpha).setDuration(300);
        ObjectAnimator daysOfWeekAlphaAnimator = ObjectAnimator.ofFloat(daysOfWeek,
                View.ALPHA, daysOfWeek.getAlpha(), annotationsAlpha).setDuration(300);
        ObjectAnimator preemptiveDismissButtonAlphaAnimator = ObjectAnimator.ofFloat(preemptiveDismissButton,
                View.ALPHA, preemptiveDismissButton.getAlpha(), annotationsAlpha).setDuration(300);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(daysOfWeekAlphaAnimator, clockAlphaAnimator,
                preemptiveDismissButtonAlphaAnimator);

        animatorSet.start();
    }

    protected void setChangingViewsAlpha(float alpha) {
        daysOfWeek.setAlpha(alpha);
        preemptiveDismissButton.setAlpha(alpha);
    }

    protected String formatAlarmDate(Alarm alarm) {
        Calendar calendar = Calendar.getInstance();
        boolean isCurrentYear = alarm.year == calendar.get(Calendar.YEAR);

        calendar.set(alarm.year, alarm.month, alarm.day);

        String pattern = DateFormat.getBestDateTimePattern(
                Locale.getDefault(), isCurrentYear ? "MMMMd" : "yyyyMMMMd");
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(calendar.getTime());
    }

    protected Animator getBoundsAnimator(View from, View to, long duration) {
        final Animator animator = AnimatorUtils
                .getBoundsAnimator(from, from, to)
                .setDuration(duration);

        animator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        return animator;
    }

    protected AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return getItemHolder().getAlarmTimeClickHandler();
    }
}
