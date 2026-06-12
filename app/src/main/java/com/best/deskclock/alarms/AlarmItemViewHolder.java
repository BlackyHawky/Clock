/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.util.TypedValue;

import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.databinding.AlarmItemBinding;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * ViewHolder for alarm items.
 */
public class AlarmItemViewHolder extends RecyclerView.ViewHolder {

    public static final float CLOCK_ENABLED_ALPHA = 1f;
    public static final float CLOCK_DISABLED_ALPHA = 0.6f;
    public static final int ALPHA_ANIMATION_DURATION = 300;
    public static final String SKELETON = "EEE MMM d";
    public static final String SKELETON_WITH_YEAR = "EEE MMM d yyyy";

    public final AlarmItemBinding mBinding;

    private final Calendar mLocalCalendar = Calendar.getInstance();
    public final SharedPreferences mPrefs;
    private final AlarmAdapter mAdapter;
    private AlarmItemHolder mItemHolder;
    private final Typeface mGeneralTypeface;
    private final Typeface mGeneralBoldTypeface;
    private final Locale mLocale;
    private final String mDatePattern;
    private final String mDatePatternWithYear;
    public int mItemPosition = 0;
    public int mTotalCount = 0;

    public AlarmItemViewHolder(AlarmItemBinding binding, AlarmAdapter alarmAdapter, SharedPreferences prefs, Typeface generalTypeface,
                               Typeface generalBoldTypeface, Locale locale, String datePattern, String datePatternWithYear) {

        super(binding.getRoot());

        final Context context = itemView.getContext();

        mBinding = binding;
        mAdapter = alarmAdapter;
        mPrefs = prefs;
        mGeneralTypeface = generalTypeface;
        mGeneralBoldTypeface = generalBoldTypeface;
        mLocale = locale;
        mDatePattern = datePattern;
        mDatePatternWithYear = datePatternWithYear;

        itemView.setOnClickListener(v ->
            mItemHolder.getAlarmTimeClickHandler().displayBottomSheetDialog(mItemHolder.item, false)
        );

        // Clock handler
        mBinding.digitalClock.setOnClickListener(v -> mItemHolder.getAlarmTimeClickHandler().onClockClicked(mItemHolder.item));
        mBinding.digitalClock.setOnLongClickListener(v -> {
            mItemHolder.getAlarmTimeClickHandler().onClockLongClicked(mItemHolder.item);
            return true;
        });

        // Upcoming date font
        mBinding.upcomingDate.setTypeface(mGeneralTypeface);

        // Preemptive dismiss button handler
        mBinding.preemptiveDismissButton.setBackground(ThemeUtils.pillRippleDrawable(context, Color.TRANSPARENT));
        mBinding.preemptiveDismissButton.setTypeface(mGeneralBoldTypeface);
        mBinding.preemptiveDismissButton.setOnClickListener(v -> {
            final AlarmInstance alarmInstance = mItemHolder.getAlarmInstance();
            if (alarmInstance != null) {
                mItemHolder.getAlarmTimeClickHandler().dismissAlarmInstance(mItemHolder, alarmInstance);
            }
        });
    }

    public AlarmItemHolder getItemHolder() {
        return mItemHolder;
    }

    public void updateAlarmFont(Typeface alarmTypeface) {
        mBinding.digitalClock.setTypeface(alarmTypeface);
    }

    public void bind(final AlarmItemHolder itemHolder) {
        this.mItemHolder = itemHolder;
        final Alarm alarm = itemHolder.item;
        final AlarmInstance alarmInstance = itemHolder.getAlarmInstance();
        final Context context = itemView.getContext();

        bindExpressiveCardBackground();
        bindAlarmLabel(context, alarm);
        bindClock(alarm);
        bindOnOffSwitch(alarm);
        bindRepeatText(context, alarm, alarmInstance);
        bindUpcomingDate(alarm, alarmInstance);
        bindPreemptiveDismissButton(context, alarm, alarmInstance);
        bindAlphaAnimation(alarm);

        itemView.setContentDescription(mBinding.digitalClock.getText() + " " + alarm.getLabelOrDefault(context));
    }

    private void bindExpressiveCardBackground() {
        if (mAdapter == null) {
            return;
        }

        int position = getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        Drawable.ConstantState bgState;

        if (mAdapter.isUseExpressiveBackground()) {
            // Phone in portrait mode
            int totalCount = mAdapter.getItemCount();
            this.mItemPosition = position;
            this.mTotalCount = totalCount;

            if (totalCount <= 1) {
                bgState = mAdapter.getBgSingle();
            } else if (position == 0) {
                bgState = mAdapter.getBgTop();
            } else if (position == totalCount - 1) {
                bgState = mAdapter.getBgBottom();
            } else {
                bgState = mAdapter.getBgMiddle();
            }
        } else {
            // Tablet / Landscape
            bgState = mAdapter.getBgStandard();
        }

        if (bgState != null) {
            itemView.setBackground(bgState.newDrawable());
        }
    }

    private void bindAlarmLabel(Context context, Alarm alarm) {
        if (alarm.label == null || alarm.label.isEmpty()) {
            mBinding.alarmLabel.setVisibility(GONE);
            return;
        }

        Typeface typeface = alarm.enabled ? mGeneralBoldTypeface : mGeneralTypeface;

        mBinding.alarmLabel.setTypeface(typeface);
        mBinding.alarmLabel.setText(alarm.label);
        mBinding.alarmLabel.setVisibility(VISIBLE);
        mBinding.alarmLabel.setContentDescription(context.getString(R.string.label_description) + " " + alarm.label);
    }

    private void bindOnOffSwitch(Alarm alarm) {
        if (RingtoneUtils.RINGTONE_SILENT.equals(alarm.alert)) {
            mBinding.onOffButton.setThumbIconResource(R.drawable.ic_ringtone_silent_filled);
        } else {
            mBinding.onOffButton.setThumbIconResource(R.drawable.alarm_switch_thumb_icon);
        }

        mBinding.onOffButton.setOnCheckedChangeListener(null);
        mBinding.onOffButton.setChecked(alarm.enabled);
        mBinding.onOffButton.setOnCheckedChangeListener((compoundButton, checked) ->
            mItemHolder.getAlarmTimeClickHandler().setAlarmEnabled(mItemHolder.item, checked));
    }

    private void bindClock(Alarm alarm) {
        mBinding.digitalClock.setTime(alarm.hour, alarm.minutes);
    }

    private void bindRepeatText(Context context, Alarm alarm, AlarmInstance alarmInstance) {
        if (alarmInstance != null
            && alarm.canPreemptivelyDismiss(context)
            && alarm.instanceState == AlarmInstance.SNOOZE_STATE) {
            mBinding.daysOfWeek.setTypeface(mGeneralBoldTypeface);
            mBinding.daysOfWeek.setText(context.getString(R.string.alarm_alert_snooze_until,
                AlarmUtils.getAlarmText(context, alarmInstance, false)));
        } else if (alarmInstance != null && alarm.daysOfWeek.isRepeating()) {
            setRepeatingDaysDescription(context, alarm, alarmInstance);
        } else if (alarm.isSpecifiedDate()) {
            setSpecifiedDateDescription(context, alarm);
        } else {
            setNonRepeatingDefaultDescription(context, alarm);
        }
    }

    private void bindUpcomingDate(Alarm alarm, AlarmInstance alarmInstance) {
        if (alarmInstance == null || !alarm.enabled || !alarm.daysOfWeek.isRepeating()) {
            mBinding.upcomingDate.setVisibility(GONE);
            mBinding.digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
            return;
        }

        Calendar nextAlarmTime = alarm.getNextAlarmTimeCalendar(alarmInstance);

        long diffInMillis = nextAlarmTime.getTimeInMillis() - System.currentTimeMillis();
        long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        if (diffInDays < 6) {
            mBinding.upcomingDate.setVisibility(GONE);
            mBinding.digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
            return;
        }

        mLocalCalendar.setTimeInMillis(System.currentTimeMillis());
        boolean isDifferentYear = mLocalCalendar.get(Calendar.YEAR) != nextAlarmTime.get(Calendar.YEAR);
        String formattedDate = DateFormat.format(isDifferentYear ? mDatePatternWithYear : mDatePattern, nextAlarmTime).toString();
        mBinding.upcomingDate.setText(FormattedTextUtils.capitalizeFirstLetter(formattedDate, mLocale));
        mBinding.upcomingDate.setVisibility(VISIBLE);

        boolean hasLabel = alarm.label != null && !alarm.label.isEmpty();
        mBinding.digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, hasLabel ? 32 : 48);
    }

    private void bindPreemptiveDismissButton(Context context, Alarm alarm, AlarmInstance alarmInstance) {
        final boolean canBind = alarm.canPreemptivelyDismiss(context) && alarmInstance != null;

        if (!canBind) {
            mBinding.preemptiveDismissButton.setVisibility(GONE);
            return;
        }

        final String dismissText = alarm.isDeleteAfterUse()
            ? context.getString(R.string.alarm_alert_dismiss_and_delete_text_button)
            : context.getString(R.string.alarm_alert_dismiss_text);

        mBinding.preemptiveDismissButton.setText(dismissText);
        mBinding.preemptiveDismissButton.setVisibility(VISIBLE);
    }

    private void bindAlphaAnimation(Alarm alarm) {
        float targetAlpha = alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA;

        mBinding.alarmLabel.animate().cancel();
        mBinding.digitalClock.animate().cancel();
        mBinding.daysOfWeek.animate().cancel();

        if (mBinding.digitalClock.getAlpha() == targetAlpha) {
            return;
        }

        if (!itemView.isAttachedToWindow()) {
            mBinding.alarmLabel.setAlpha(targetAlpha);
            mBinding.digitalClock.setAlpha(targetAlpha);
            mBinding.daysOfWeek.setAlpha(targetAlpha);
            return;
        }

        mBinding.alarmLabel.animate().alpha(targetAlpha).setDuration(ALPHA_ANIMATION_DURATION).start();
        mBinding.digitalClock.animate().alpha(targetAlpha).setDuration(ALPHA_ANIMATION_DURATION).start();
        mBinding.daysOfWeek.animate().alpha(targetAlpha).setDuration(ALPHA_ANIMATION_DURATION).start();
    }

    // ********************
    // ** HELPER METHODS **
    // ********************

    public void updateBackground() {
        bindExpressiveCardBackground();
    }

    private void setRepeatingDaysDescription(Context context, Alarm alarm, AlarmInstance alarmInstance) {
        Weekdays.Order weekdayOrder = SettingsDAO.getWeekdayOrder(mPrefs);
        String contentDesc = alarm.daysOfWeek.toAccessibilityString(context, weekdayOrder);
        CharSequence styledDaysText;

        if (isPauseEffectivelyActive(alarm, alarmInstance)) {
            String dateRangeStr = AlarmUtils.formatPauseDateRange(context, alarm.pauseStartDate, alarm.pauseEndDate);
            String pauseText = context.getString(R.string.pause_alarm_range, dateRangeStr);

            styledDaysText = pauseText;
            contentDesc = pauseText;
        } else if (alarm.enabled) {
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

        setDaysOfWeekText(styledDaysText);
        mBinding.daysOfWeek.setContentDescription(contentDesc);
    }

    private boolean isPauseEffectivelyActive(Alarm alarm, AlarmInstance nextInstance) {
        if (!alarm.enabled || !alarm.isPauseSet() || nextInstance == null) {
            return false;
        }

        // Check if the pause is not already in the past
        if (AlarmUtils.isPauseExpired(alarm.pauseEndDate)) {
            return false;
        }

        // Check if the instance date is scheduled after the end of the pause
        return nextInstance.getAlarmTime().getTimeInMillis() > alarm.pauseEndDate;
    }

    private void setNonRepeatingDefaultDescription(Context context, Alarm alarm) {
        mLocalCalendar.setTimeInMillis(System.currentTimeMillis());

        if (alarm.isTomorrow(mLocalCalendar)) {
            setDaysOfWeekText(context.getString(R.string.alarm_tomorrow));
        } else {
            setDaysOfWeekText(context.getString(R.string.alarm_today));
        }
    }

    private void setSpecifiedDateDescription(Context context, Alarm alarm) {
        mLocalCalendar.setTimeInMillis(System.currentTimeMillis());

        if (Alarm.isSpecifiedDateTomorrow(alarm.year, alarm.month, alarm.day)) {
            setDaysOfWeekText(context.getString(R.string.alarm_tomorrow));
        } else if (alarm.isDateInThePast()) {
            setDaysOfWeekText(getTodayOrTomorrowBasedOnTime(context, alarm, mLocalCalendar));
        } else {
            setDaysOfWeekText(context.getString(R.string.alarm_scheduled_for, AlarmUtils.formatAlarmDate(alarm)));
        }
    }

    private void setDaysOfWeekText(CharSequence text) {
        mBinding.daysOfWeek.setTypeface(mGeneralTypeface);
        mBinding.daysOfWeek.setText(text);
    }

    private String getTodayOrTomorrowBasedOnTime(Context context, Alarm alarm, Calendar now) {
        // Used when the date has passed, the new alarm will be scheduled either the same day
        // or the next day depending on the time.
        // The text is therefore updated accordingly.
        return context.getString(alarm.isTimeBeforeOrEqual(now) ? R.string.alarm_tomorrow : R.string.alarm_today);
    }

}
