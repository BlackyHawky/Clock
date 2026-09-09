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
import android.os.SystemClock;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * ViewHolder for alarm items.
 */
public class AlarmItemViewHolder extends RecyclerView.ViewHolder {

    public static final float CLOCK_ENABLED_ALPHA = 1f;
    public static final float CLOCK_DISABLED_ALPHA = 0.6f;
    public static final int ALPHA_ANIMATION_DURATION = 300;
    private static long mLastClickTime = 0;


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

        itemView.setOnClickListener(v -> {
            long currentTime = SystemClock.elapsedRealtime();

            if (currentTime - mLastClickTime >= Utils.MIN_CLICK_INTERVAL) {
                mLastClickTime = currentTime;
                mItemHolder.getAlarmTimeClickHandler().displayBottomSheetDialog(mItemHolder.item, false);
            }
        });

        // Clock handler
        mBinding.digitalClock.setOnClickListener(v -> {
            long currentTime = SystemClock.elapsedRealtime();

            if (currentTime - mLastClickTime >= Utils.MIN_CLICK_INTERVAL) {
                mLastClickTime = currentTime;
                mItemHolder.getAlarmTimeClickHandler().onClockClicked(mItemHolder.item);
            }
        });
        mBinding.digitalClock.setOnLongClickListener(v -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            mItemHolder.getAlarmTimeClickHandler().onClockLongClicked(mItemHolder.item);
            return true;
        });

        // Globe icon handler
        mBinding.globeIcon.setOnClickListener(v ->
            mItemHolder.getAlarmTimeClickHandler().onGlobeClicked(mItemHolder.item, mBinding.globeIcon, screen.metrics()));

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
        bindLockAlarm(alarm);
        bindGlobeIcon(alarm);
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

    private void bindLockAlarm(@NonNull Alarm alarm) {
        mBinding.lockIcon.setVisibility(alarm.lock ? VISIBLE : GONE);
    }

    private void bindGlobeIcon(@NonNull Alarm alarm) {
        TimeZone alarmTimeZone = alarm.getTimeZone();
        TimeZone defaultTimeZone = TimeZone.getDefault();

        long now = System.currentTimeMillis();
        int alarmOffset = alarmTimeZone.getOffset(now);
        int defaultOffset = defaultTimeZone.getOffset(now);

        mBinding.globeIcon.setVisibility(alarmOffset != defaultOffset ? VISIBLE : GONE);
    }

    private void bindRepeatText(@NonNull Alarm alarm, @Nullable AlarmInstance alarmInstance) {
        // Check if the alarm was recently dismissed to bypass the database synchronization delay.
        final boolean isRecentlyDismissed = AlarmVisualCache.isDismissed(alarm.id);
        final AlarmInstance recentlySnoozedInstance = AlarmVisualCache.getSnoozedAlarm(alarm.id);

        // Optimistic Snooze (Bypass database delay)
        if (recentlySnoozedInstance != null && !isRecentlyDismissed) {
            mBinding.daysOfWeek.setTypeface(mAdapter.getFonts().bold());
            mBinding.daysOfWeek.setText(mContext.getString(R.string.alarm_alert_snooze_until,
                AlarmUtils.getFormattedTime(mContext, recentlySnoozedInstance.getAlarmTime())));

        // Optimistic Dismiss
        } else if (isRecentlyDismissed) {
            if (alarmInstance != null && alarm.daysOfWeek.isRepeating()) {
                setRepeatingDaysDescription(alarm, alarmInstance);
            } else if (alarm.isSpecifiedDate()) {
                setSpecifiedDateDescription(alarm);
            } else {
                mLocalCalendar.setTimeZone(alarm.getTimeZone());
                mLocalCalendar.setTimeInMillis(System.currentTimeMillis());
                if (alarm.isTomorrow(mLocalCalendar)) {
                    setDaysOfWeekText(mContext.getString(R.string.alarm_tomorrow));
                } else {
                    setDaysOfWeekText(mContext.getString(R.string.alarm_today));
                }
            }

        // Standard fallbacks
        } else if (alarmInstance != null
            && mAdapter.getStateProvider().canPreemptivelyDismiss(alarm)
            && alarm.instanceState == AlarmInstance.SNOOZE_STATE) {
            mBinding.daysOfWeek.setTypeface(mAdapter.getFonts().bold());
            mBinding.daysOfWeek.setText(mContext.getString(R.string.alarm_alert_snooze_until,
                AlarmUtils.getFormattedTime(mContext, alarmInstance.getAlarmTime())));
        } else if (alarmInstance != null && alarm.daysOfWeek.isRepeating()) {
            setRepeatingDaysDescription(alarm, alarmInstance);
        } else if (alarm.isSpecifiedDate()) {
            setSpecifiedDateDescription(alarm);
        } else {
            setNonRepeatingDefaultDescription(alarm);
        }
    }

    private void bindUpcomingDate(@NonNull Alarm alarm, @Nullable AlarmInstance alarmInstance) {
        if (alarmInstance == null || !alarm.enabled || !alarm.daysOfWeek.isRepeating()) {
            mBinding.upcomingDate.setVisibility(GONE);
            mBinding.digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
            return;
        }

        Calendar nextAlarmTime = alarm.getNextAlarmTimeCalendar(alarmInstance);

        mLocalCalendar.setTimeZone(alarm.getTimeZone());
        mLocalCalendar.setTimeInMillis(System.currentTimeMillis());

        Calendar today = (Calendar) mLocalCalendar.clone();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar targetDay = (Calendar) nextAlarmTime.clone();
        targetDay.set(Calendar.HOUR_OF_DAY, 0);
        targetDay.set(Calendar.MINUTE, 0);
        targetDay.set(Calendar.SECOND, 0);
        targetDay.set(Calendar.MILLISECOND, 0);

        long diffInMillis = targetDay.getTimeInMillis() - today.getTimeInMillis();
        long diffInDays = Math.round((double) diffInMillis / (24 * 60 * 60 * 1000));

        if (diffInDays < 6) {
            mBinding.upcomingDate.setVisibility(GONE);
            mBinding.digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
            return;
        }

        boolean isDifferentYear = mLocalCalendar.get(Calendar.YEAR) != nextAlarmTime.get(Calendar.YEAR);
        SimpleDateFormat simpleDateFormat = mAdapter.getDateFormat(alarm.getTimeZone(), isDifferentYear);
        String formattedDate = simpleDateFormat.format(nextAlarmTime.getTime());

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

            if (alarm.daysOfWeek.isAllDaysSelected()) {
                if (mAdapter.getStateProvider().isRepeatDayStyleEnabled(alarm.id)) {
                    styledDaysText = alarm.daysOfWeek.toStyledString(mContext, weekdayOrder, false, nextAlarmDay);
                } else {
                    styledDaysText = alarm.daysOfWeek.toString(mContext, weekdayOrder);
                }
            } else {
                styledDaysText = alarm.daysOfWeek.toStyledString(mContext, weekdayOrder, false, nextAlarmDay);
            }

            // Append combined days info if there are deselected dates
            if (alarm.combinedDays != null && alarm.combinedDays.hasDeselectedDates()) {
                int count = alarm.combinedDays.getDeselectedDateCount();
                String excludeInfo = mContext.getString(R.string.dates_excluded_count, count);
                styledDaysText = styledDaysText + " (" + excludeInfo + ")";
                contentDesc = contentDesc + " (" + excludeInfo + ")";
            }
        } else {
            styledDaysText = alarm.daysOfWeek.toString(mContext, weekdayOrder);

            // Append combined days info if there are deselected dates
            if (alarm.combinedDays != null && alarm.combinedDays.hasDeselectedDates()) {
                int count = alarm.combinedDays.getDeselectedDateCount();
                String excludeInfo = mContext.getString(R.string.dates_excluded_count, count);
                styledDaysText = styledDaysText + " (" + excludeInfo + ")";
                contentDesc = contentDesc + " (" + excludeInfo + ")";
            }
        }

        setDaysOfWeekText(styledDaysText);
        mBinding.daysOfWeek.setContentDescription(contentDesc);
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
        mLocalCalendar.setTimeZone(alarm.getTimeZone());
        mLocalCalendar.setTimeInMillis(System.currentTimeMillis());

        if (alarm.isTomorrow(mLocalCalendar)) {
            setDaysOfWeekText(mContext.getString(R.string.alarm_tomorrow));
        } else {
            setDaysOfWeekText(mContext.getString(R.string.alarm_today));
        }
    }

    private void setSpecifiedDateDescription(@NonNull Alarm alarm) {
        mLocalCalendar.setTimeZone(alarm.getTimeZone());
        mLocalCalendar.setTimeInMillis(System.currentTimeMillis());

        if (alarm.isSpecifiedDateTomorrow()) {
            setDaysOfWeekText(mContext.getString(R.string.alarm_tomorrow));
        } else if (alarm.isDateInThePast()) {
            setDaysOfWeekText(getTodayOrTomorrowBasedOnTime(alarm, mLocalCalendar));
        } else {
            setDaysOfWeekText(mContext.getString(R.string.alarm_scheduled_for, AlarmUtils.formatAlarmDate(mContext, alarm)));
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

    public static long getLastClickTime() {
        return mLastClickTime;
    }

}
