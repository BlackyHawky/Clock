/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.format.DateFormat;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.CombinedDays;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.databinding.AlarmItemBinding;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * ViewHolder for alarm items.
 */
public class AlarmItemViewHolder extends RecyclerView.ViewHolder {

    public static final float CLOCK_ENABLED_ALPHA = 1f;
    public static final float CLOCK_DISABLED_ALPHA = 0.6f;
    public static final int ALPHA_ANIMATION_DURATION = 300;

    public final AlarmItemBinding mBinding;

    private final Calendar mLocalCalendar = Calendar.getInstance();
    private final Context mContext;
    private final AlarmAdapter mAdapter;
    private AlarmItemHolder mItemHolder;

    public int mItemPosition = 0;
    public int mTotalCount = 0;

    public AlarmItemViewHolder(@NonNull AlarmItemBinding binding, @NonNull AlarmAdapter alarmAdapter) {

        super(binding.getRoot());

        mContext = binding.getRoot().getContext();
        mBinding = binding;
        mAdapter = alarmAdapter;

        UiConfig.Fonts currentFonts = mAdapter.getFonts();
        UiConfig.Screen screen = mAdapter.getScreen();
        UiConfig.Haptics haptics = mAdapter.getHaptics();

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
        mBinding.upcomingDate.setTypeface(currentFonts.general());

        // Preemptive dismiss button handler
        mBinding.preemptiveDismissButton.setBackground(ThemeUtils.pillRippleDrawable(mContext, screen.metrics(), Color.TRANSPARENT));
        mBinding.preemptiveDismissButton.setTypeface(currentFonts.bold());
        mBinding.preemptiveDismissButton.setOnClickListener(v -> {
            final AlarmInstance alarmInstance = mItemHolder.getAlarmInstance();
            if (alarmInstance != null) {
                Utils.performHapticFeedback(v, haptics.isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mItemHolder.getAlarmTimeClickHandler().dismissAlarmInstance(mItemHolder, alarmInstance);
            }
        });
    }

    public AlarmItemHolder getItemHolder() {
        return mItemHolder;
    }

    public void bind(@NonNull final AlarmItemHolder itemHolder) {
        this.mItemHolder = itemHolder;
        final Alarm alarm = itemHolder.item;
        final AlarmInstance alarmInstance = itemHolder.getAlarmInstance();

        bindExpressiveCardBackground();
        bindAlarmLabel(mContext, alarm);
        bindClock(alarm);
        bindOnOffSwitch(alarm);
        bindRepeatText(alarm, alarmInstance);
        bindUpcomingDate(alarm, alarmInstance);
        bindPreemptiveDismissButton(alarm, alarmInstance);
        bindAlphaAnimation(alarm);

        itemView.setContentDescription(mBinding.digitalClock.getText() + " " + alarm.getLabelOrDefault(mContext));
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

    private void bindAlarmLabel(@NonNull Context context, @NonNull Alarm alarm) {
        if (alarm.label == null || alarm.label.isEmpty()) {
            mBinding.alarmLabel.setVisibility(GONE);
            return;
        }

        Typeface typeface = alarm.enabled ? mAdapter.getFonts().bold() : mAdapter.getFonts().general();

        mBinding.alarmLabel.setTypeface(typeface);
        mBinding.alarmLabel.setText(alarm.label);
        mBinding.alarmLabel.setVisibility(VISIBLE);
        mBinding.alarmLabel.setContentDescription(context.getString(R.string.label_description) + " " + alarm.label);
    }

    private void bindOnOffSwitch(@NonNull Alarm alarm) {
        if (RingtoneUtils.RINGTONE_SILENT.equals(alarm.alert)) {
            mBinding.onOffButton.setThumbIconResource(R.drawable.ic_ringtone_silent_filled);
        } else {
            mBinding.onOffButton.setThumbIconResource(R.drawable.alarm_switch_thumb_icon);
        }

        mBinding.onOffButton.setOnCheckedChangeListener(null);
        mBinding.onOffButton.setChecked(alarm.enabled);
        mBinding.onOffButton.setOnCheckedChangeListener((compoundButton, checked) -> {
            mItemHolder.getAlarmTimeClickHandler().setAlarmEnabled(mItemHolder.item, checked);
            if (checked) {
                Utils.performHapticFeedback(
                    compoundButton, mAdapter.getHaptics().isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                compoundButton.postDelayed(() ->
                    Utils.performHapticFeedback(
                        compoundButton, mAdapter.getHaptics().isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY), 50);
            } else {
                Utils.performHapticFeedback(
                    compoundButton, mAdapter.getHaptics().isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
        });
    }

    private void bindClock(@NonNull Alarm alarm) {
        boolean is24HourMode = mAdapter.is24HourFormat();
        CharSequence format12 = mAdapter.getFormat12();
        CharSequence format24 = mAdapter.getFormat24();
        Typeface alarmFont = mAdapter.getFonts().alarmClockFont() != null ? mAdapter.getFonts().alarmClockFont() : mAdapter.getFonts().bold();

        mBinding.digitalClock.configure(is24HourMode, format12, format24);

        mBinding.digitalClock.setTypeface(alarmFont);

        mBinding.digitalClock.setTime(alarm.hour, alarm.minutes);
    }

    private void bindRepeatText(@NonNull Alarm alarm, @Nullable AlarmInstance alarmInstance) {
        if (alarmInstance != null
            && mAdapter.getStateProvider().canPreemptivelyDismiss(alarm)
            && alarm.instanceState == AlarmInstance.SNOOZE_STATE) {
            mBinding.daysOfWeek.setTypeface(mAdapter.getFonts().bold());
            mBinding.daysOfWeek.setText(mContext.getString(R.string.alarm_alert_snooze_until,
                AlarmUtils.getAlarmText(mContext, alarmInstance, false)));
        } else if (alarmInstance != null && alarm.daysOfWeek.isRepeating()) {
            setRepeatingDaysDescription(alarm, alarmInstance);
        } else if (alarm.combinedDays != null && alarm.combinedDays.hasSelectedDates()) {
            setCombinedDaysDateDescription(alarm);
        } else if (alarm.isSpecifiedDate()) {
            setSpecifiedDateDescription(alarm);
        } else {
            setNonRepeatingDefaultDescription(alarm);
        }
    }

    private void bindUpcomingDate(@NonNull Alarm alarm, @Nullable AlarmInstance alarmInstance) {
        if (alarmInstance == null || !alarm.enabled || !alarm.daysOfWeek.isRepeating()
            || (alarm.combinedDays != null && !alarm.combinedDays.isEmpty())) {
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
        String formattedDate = DateFormat.format(isDifferentYear
            ? mAdapter.getDateFormat().patternWithYear()
            : mAdapter.getDateFormat().pattern(), nextAlarmTime).toString();
        mBinding.upcomingDate.setText(FormattedTextUtils.capitalizeFirstLetter(formattedDate, mAdapter.getDateFormat().locale()));
        mBinding.upcomingDate.setVisibility(VISIBLE);

        boolean hasLabel = alarm.label != null && !alarm.label.isEmpty();
        mBinding.digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, hasLabel ? 32 : 48);
    }

    private void bindPreemptiveDismissButton(@NonNull Alarm alarm, @Nullable AlarmInstance alarmInstance) {
        if (AlarmVisualCache.isDismissed(alarm.id) && !mAdapter.getStateProvider().isDismissButtonDisplayed()) {
            mBinding.preemptiveDismissButton.setVisibility(GONE);
            return;
        }

        final boolean canBind = mAdapter.getStateProvider().canPreemptivelyDismiss(alarm) && alarmInstance != null;

        if (!canBind) {
            mBinding.preemptiveDismissButton.setVisibility(GONE);
            return;
        }

        final String dismissText = alarm.isDeleteAfterUse()
            ? mContext.getString(R.string.alarm_alert_dismiss_and_delete_text_button)
            : mContext.getString(R.string.alarm_alert_dismiss_text);

        mBinding.preemptiveDismissButton.setText(dismissText);
        mBinding.preemptiveDismissButton.setVisibility(VISIBLE);
    }

    private void bindAlphaAnimation(@NonNull Alarm alarm) {
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

    private void setRepeatingDaysDescription(@NonNull Alarm alarm, @NonNull AlarmInstance alarmInstance) {
        Weekdays.Order weekdayOrder = mAdapter.getWeekdayOrder();
        String contentDesc = alarm.daysOfWeek.toAccessibilityString(mContext, weekdayOrder);
        CharSequence styledDaysText;

        if (isPauseEffectivelyActive(alarm, alarmInstance)) {
            String dateRangeStr = AlarmUtils.formatPauseDateRange(mContext, alarm.pauseStartDate, alarm.pauseEndDate);
            String pauseText = mContext.getString(R.string.pause_alarm_range, dateRangeStr);

            styledDaysText = pauseText;
            contentDesc = pauseText;
        } else if (alarm.enabled) {
            int nextAlarmDay = alarm.getNextAlarmDayOfWeek(alarmInstance);

            if (alarm.combinedDays != null && !alarm.combinedDays.isEmpty()) {
                styledDaysText = alarm.daysOfWeek.toString(mContext, weekdayOrder);
            } else if (alarm.daysOfWeek.isAllDaysSelected()) {
                if (mAdapter.getStateProvider().isRepeatDayStyleEnabled(alarm.id)) {
                    styledDaysText = alarm.daysOfWeek.toStyledString(mContext, weekdayOrder, false, nextAlarmDay);
                } else {
                    styledDaysText = alarm.daysOfWeek.toString(mContext, weekdayOrder);
                }
            } else {
                styledDaysText = alarm.daysOfWeek.toStyledString(mContext, weekdayOrder, false, nextAlarmDay);
            }
        } else {
            styledDaysText = alarm.daysOfWeek.toString(mContext, weekdayOrder);
        }

        // Append combined days info (next date + selected/deselected counts)
        if (alarm.combinedDays != null && !alarm.combinedDays.isEmpty()) {
            styledDaysText = buildCombinedDaysDisplay(alarm, styledDaysText);
            contentDesc = styledDaysText.toString();
        }

        setDaysOfWeekText(styledDaysText);
        mBinding.daysOfWeek.setContentDescription(contentDesc);
    }

    @NonNull
    private CharSequence buildCombinedDaysDisplay(@NonNull Alarm alarm, @NonNull CharSequence base) {
        final String suffix = getCombinedDaysCountSuffix(alarm);
        if (suffix.isEmpty()) {
            return base;
        }

        final SpannableStringBuilder ssb = new SpannableStringBuilder(base);
        if (ssb.length() > 0) {
            ssb.append(", ");
        }
        ssb.append(getCombinedDaysNextDateText(alarm));
        ssb.append(" ").append(suffix);
        return ssb;
    }

    @NonNull
    private String getCombinedDaysCountSuffix(@NonNull Alarm alarm) {
        final int selected = getRemainingSelectedDateCount(alarm);
        final int excluded = getRemainingExcludedDateCount(alarm);
        if (selected == 0 && excluded == 0) {
            return "";
        }

        final StringBuilder sb = new StringBuilder("(");
        if (selected > 0) {
            sb.append("+").append(selected);
        }
        if (selected > 0 && excluded > 0) {
            sb.append(", ");
        }
        if (excluded > 0) {
            sb.append("-").append(excluded);
        }
        return sb.append(")").toString();
    }

    /**
     * Counts the added dates that have not passed yet. Dismissed (transiently skipped) dates
     * are still counted so that dismissing an occurrence does not visually remove an added date
     * from the main view; only dates that are actually in the past are excluded.
     */
    private static int getRemainingSelectedDateCount(@NonNull Alarm alarm) {
        if (alarm.combinedDays == null) {
            return 0;
        }
        final Calendar now = Calendar.getInstance();
        int count = 0;
        for (String key : alarm.combinedDays.getSelectedDates()) {
            try {
                final int[] parts = CombinedDays.parseDateKey(key);
                final Calendar date = Calendar.getInstance();
                date.set(parts[0], parts[1], parts[2], alarm.hour, alarm.minutes, 0);
                date.set(Calendar.MILLISECOND, 0);
                if (!date.before(now)) {
                    count++;
                }
            } catch (IllegalArgumentException e) {
                // Skip malformed date key
            }
        }
        return count;
    }

    /**
     * Counts the excluded dates that have not passed yet. Past excluded dates are no longer
     * relevant, so they do not count toward the "-N" part of the display string.
     */
    private static int getRemainingExcludedDateCount(@NonNull Alarm alarm) {
        if (alarm.combinedDays == null) {
            return 0;
        }
        final Calendar now = Calendar.getInstance();
        int count = 0;
        for (String key : alarm.combinedDays.getDeselectedDates()) {
            try {
                final int[] parts = CombinedDays.parseDateKey(key);
                final Calendar date = Calendar.getInstance();
                date.set(parts[0], parts[1], parts[2], alarm.hour, alarm.minutes, 0);
                date.set(Calendar.MILLISECOND, 0);
                if (!date.before(now)) {
                    count++;
                }
            } catch (IllegalArgumentException e) {
                // Skip malformed date key
            }
        }
        return count;
    }

    @NonNull
    private CharSequence getCombinedDaysNextDateText(@NonNull Alarm alarm) {
        final Calendar nextTime = alarm.getNextAlarmTime(Calendar.getInstance());
        final int accentColor = MaterialColors.getColor(mBinding.daysOfWeek,
            com.google.android.material.R.attr.colorTertiary, Color.BLACK);

        final String dayText;
        if (Alarm.isDateToday(nextTime)) {
            dayText = mContext.getString(R.string.alarm_today);
        } else if (Alarm.isDateTomorrow(nextTime)) {
            dayText = mContext.getString(R.string.alarm_tomorrow);
        } else {
            final String datePattern = DateFormat.getBestDateTimePattern(mAdapter.getDateFormat().locale(), "MMM d");
            final String dateStr = FormattedTextUtils.capitalizeFirstLetter(
                DateFormat.format(datePattern, nextTime).toString(), mAdapter.getDateFormat().locale());
            final String weekdayPattern = DateFormat.getBestDateTimePattern(mAdapter.getDateFormat().locale(), "EEE");
            final String weekdayStr = FormattedTextUtils.capitalizeFirstLetter(
                DateFormat.format(weekdayPattern, nextTime).toString(), mAdapter.getDateFormat().locale());
            dayText = dateStr + ", " + weekdayStr;
        }

        final SpannableStringBuilder ssb = new SpannableStringBuilder(dayText);
        ssb.setSpan(new ForegroundColorSpan(accentColor), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    private void setCombinedDaysDateDescription(@NonNull Alarm alarm) {
        final CharSequence text = buildCombinedDaysDisplay(alarm, "");
        setDaysOfWeekText(text);
        mBinding.daysOfWeek.setContentDescription(text.toString());
    }

    private boolean isPauseEffectivelyActive(@NonNull Alarm alarm, @Nullable AlarmInstance nextInstance) {
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

    private void setNonRepeatingDefaultDescription(@NonNull Alarm alarm) {
        mLocalCalendar.setTimeInMillis(System.currentTimeMillis());

        if (alarm.isTomorrow(mLocalCalendar)) {
            setDaysOfWeekText(mContext.getString(R.string.alarm_tomorrow));
        } else {
            setDaysOfWeekText(mContext.getString(R.string.alarm_today));
        }
    }

    private void setSpecifiedDateDescription(@NonNull Alarm alarm) {
        mLocalCalendar.setTimeInMillis(System.currentTimeMillis());

        if (Alarm.isSpecifiedDateTomorrow(alarm.year, alarm.month, alarm.day)) {
            setDaysOfWeekText(mContext.getString(R.string.alarm_tomorrow));
        } else if (alarm.isDateInThePast()) {
            setDaysOfWeekText(getTodayOrTomorrowBasedOnTime(alarm, mLocalCalendar));
        } else {
            setDaysOfWeekText(mContext.getString(R.string.alarm_scheduled_for, AlarmUtils.formatAlarmDate(alarm)));
        }
    }

    private void setDaysOfWeekText(@NonNull CharSequence text) {
        mBinding.daysOfWeek.setTypeface(mAdapter.getFonts().general());
        mBinding.daysOfWeek.setText(text);
    }

    @NonNull
    private String getTodayOrTomorrowBasedOnTime(@NonNull Alarm alarm, @NonNull Calendar now) {
        // Used when the date has passed, the new alarm will be scheduled either the same day
        // or the next day depending on the time.
        // The text is therefore updated accordingly.
        return mContext.getString(alarm.isTimeBeforeOrEqual(now) ? R.string.alarm_tomorrow : R.string.alarm_today);
    }

}
