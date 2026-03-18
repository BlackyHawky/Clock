/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uicomponents.TextTime;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.textview.MaterialTextView;

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
    private static final String SKELETON = "EEE MMM d";


    public final SharedPreferences mPrefs;
    private AlarmItemHolder mItemHolder;
    private final Typeface mGeneralTypeface;
    private final Typeface mGeneralBoldTypeface;
    private final Locale mLocale;
    private final String mDatePattern;
    public int mItemPosition = 0;
    public int mTotalCount = 0;

    private final TextView mAlarmLabel;
    private final TextTime mClock;
    private final CompoundButton mOnOff;
    private final TextView mDaysOfWeek;
    private final TextView mUpcomingDate;
    private final MaterialTextView mPreemptiveDismissButton;

    public AlarmItemViewHolder(View itemView) {
        super(itemView);

        final Context context = itemView.getContext();

        mPrefs = getDefaultSharedPreferences(context);
        String generalFontPath = SettingsDAO.getGeneralFont(mPrefs);
        mGeneralTypeface = ThemeUtils.loadFont(generalFontPath);
        mGeneralBoldTypeface = ThemeUtils.boldTypeface(generalFontPath);
        mLocale = Locale.getDefault();
        mDatePattern = DateFormat.getBestDateTimePattern(mLocale, SKELETON);

        mAlarmLabel = itemView.findViewById(R.id.label);
        mClock = itemView.findViewById(R.id.digital_clock);
        mOnOff = itemView.findViewById(R.id.onoff);
        mDaysOfWeek = itemView.findViewById(R.id.days_of_week);
        mUpcomingDate = itemView.findViewById(R.id.upcoming_date);
        mPreemptiveDismissButton = itemView.findViewById(R.id.preemptive_dismiss_button);

        itemView.setOnClickListener(v ->
                mItemHolder.getAlarmTimeClickHandler().displayBottomSheetDialog(mItemHolder.item));

        // Clock handler
        mClock.setTypeface(ThemeUtils.boldTypeface(SettingsDAO.getAlarmFont(mPrefs)));
        mClock.setOnClickListener(v -> mItemHolder.getAlarmTimeClickHandler().onClockClicked(mItemHolder.item));
        mClock.setOnLongClickListener(v -> {
            mItemHolder.getAlarmTimeClickHandler().onClockLongClicked(mItemHolder.item);
            return true;
        });

        // On/Off button handler
        mOnOff.setOnCheckedChangeListener((compoundButton, checked) ->
                mItemHolder.getAlarmTimeClickHandler().setAlarmEnabled(mItemHolder.item, checked));

        // Upcoming date font
        mUpcomingDate.setTypeface(mGeneralTypeface);

        // Preemptive dismiss button handler
        mPreemptiveDismissButton.setBackground(ThemeUtils.pillRippleDrawable(context, Color.TRANSPARENT));
        mPreemptiveDismissButton.setTypeface(mGeneralBoldTypeface);
        mPreemptiveDismissButton.setOnClickListener(v -> {
            final AlarmInstance alarmInstance = mItemHolder.getAlarmInstance();
            if (alarmInstance != null) {
                mItemHolder.getAlarmTimeClickHandler().dismissAlarmInstance(mItemHolder, alarmInstance);
            }
        });
    }

    public AlarmItemHolder getItemHolder() {
        return mItemHolder;
    }

    public void bind(final AlarmItemHolder itemHolder) {
        this.mItemHolder = itemHolder;
        final Alarm alarm = itemHolder.item;
        final AlarmInstance alarmInstance = itemHolder.getAlarmInstance();
        final Context context = itemView.getContext();

        bindExpressiveCardBackground(context);
        bindAlarmLabel(context, alarm);
        bindClock(alarm);
        bindOnOffSwitch(alarm);
        bindRepeatText(context, alarm, alarmInstance);
        bindUpcomingDate(alarm, alarmInstance);
        bindPreemptiveDismissButton(context, alarm, alarmInstance);
        bindAlphaAnimation(alarm);

        itemView.setContentDescription(mClock.getText() + " " + alarm.getLabelOrDefault(context));
    }

    private void bindExpressiveCardBackground(Context context) {
        Drawable cardBackground;

        if (ThemeUtils.isTablet() || ThemeUtils.isLandscape()) {
            cardBackground = ThemeUtils.cardBackground(context);
        } else {
            int position = getBindingAdapterPosition();
            RecyclerView.Adapter<?> adapter = getBindingAdapter();

            if (position != RecyclerView.NO_POSITION && adapter != null) {
                int totalCount = adapter.getItemCount();

                this.mItemPosition = position;
                this.mTotalCount = totalCount;

                cardBackground = ThemeUtils.expressiveCardBackground(context, position, totalCount);
            } else {
                cardBackground = ThemeUtils.cardBackground(context);
            }
        }

        itemView.setBackground(ThemeUtils.rippleDrawable(context, cardBackground));
    }

    private void bindAlarmLabel(Context context, Alarm alarm) {
        if (alarm.label == null || alarm.label.isEmpty()) {
            mAlarmLabel.setVisibility(GONE);
            return;
        }

        Typeface typeface = alarm.enabled ? mGeneralBoldTypeface : mGeneralTypeface;

        mAlarmLabel.setTypeface(typeface);
        mAlarmLabel.setText(alarm.label);
        mAlarmLabel.setVisibility(VISIBLE);
        mAlarmLabel.setContentDescription(context.getString(R.string.label_description) + " " + alarm.label);
    }

    private void bindOnOffSwitch(Alarm alarm) {
        if (mOnOff.isChecked() != alarm.enabled) {
            mOnOff.setChecked(alarm.enabled);
        }
    }

    private void bindClock(Alarm alarm) {
        mClock.setTime(alarm.hour, alarm.minutes);
    }

    private void bindRepeatText(Context context, Alarm alarm, AlarmInstance alarmInstance) {
        if (alarmInstance != null
                && alarm.canPreemptivelyDismiss(context)
                && alarm.instanceState == AlarmInstance.SNOOZE_STATE) {
            mDaysOfWeek.setTypeface(mGeneralBoldTypeface);
            mDaysOfWeek.setText(context.getString(R.string.alarm_alert_snooze_until,
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
            mUpcomingDate.setVisibility(GONE);
            mClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
            return;
        }

        Calendar nextAlarmTime = alarm.getNextAlarmTimeCalendar(alarmInstance);

        long diffInMillis = nextAlarmTime.getTimeInMillis() - System.currentTimeMillis();
        long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        if (diffInDays < 6) {
            mUpcomingDate.setVisibility(GONE);
            mClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
            return;
        }

        String formattedDate = DateFormat.format(mDatePattern, nextAlarmTime).toString();
        mUpcomingDate.setText(FormattedTextUtils.capitalizeFirstLetter(formattedDate, mLocale));
        mUpcomingDate.setVisibility(VISIBLE);

        boolean hasLabel = alarm.label != null && !alarm.label.isEmpty();
        mClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, hasLabel ? 32 : 48);
    }

    private void bindPreemptiveDismissButton(Context context, Alarm alarm, AlarmInstance alarmInstance) {
        final boolean canBind = alarm.canPreemptivelyDismiss(context) && alarmInstance != null;

        if (!canBind) {
            mPreemptiveDismissButton.setVisibility(GONE);
            return;
        }

        final String dismissText = alarm.isDeleteAfterUse()
                ? context.getString(R.string.alarm_alert_dismiss_and_delete_text_button)
                : context.getString(R.string.alarm_alert_dismiss_text);

        mPreemptiveDismissButton.setText(dismissText);
        mPreemptiveDismissButton.setVisibility(VISIBLE);
    }

    private void bindAlphaAnimation(Alarm alarm) {
        float targetAlpha = alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA;

        mAlarmLabel.animate().cancel();
        mClock.animate().cancel();
        mDaysOfWeek.animate().cancel();

        if (mClock.getAlpha() == targetAlpha) {
            return;
        }

        if (!itemView.isAttachedToWindow()) {
            mAlarmLabel.setAlpha(targetAlpha);
            mClock.setAlpha(targetAlpha);
            mDaysOfWeek.setAlpha(targetAlpha);
            return;
        }

        mAlarmLabel.animate().alpha(targetAlpha).setDuration(ALPHA_ANIMATION_DURATION).start();
        mClock.animate().alpha(targetAlpha).setDuration(ALPHA_ANIMATION_DURATION).start();
        mDaysOfWeek.animate().alpha(targetAlpha).setDuration(ALPHA_ANIMATION_DURATION).start();
    }

    // ********************
    // ** HELPER METHODS **
    // ********************

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

        setDaysOfWeekText(styledDaysText);
        mDaysOfWeek.setContentDescription(contentDesc);
    }

    private void setNonRepeatingDefaultDescription(Context context, Alarm alarm) {
        if (alarm.isTomorrow(Calendar.getInstance())) {
            setDaysOfWeekText(context.getString(R.string.alarm_tomorrow));
        } else {
            setDaysOfWeekText(context.getString(R.string.alarm_today));
        }
    }

    private void setSpecifiedDateDescription(Context context, Alarm alarm) {
        Calendar calendar = Calendar.getInstance();

        if (Alarm.isSpecifiedDateTomorrow(alarm.year, alarm.month, alarm.day)) {
            setDaysOfWeekText(context.getString(R.string.alarm_tomorrow));
        } else if (alarm.isDateInThePast()) {
            setDaysOfWeekText(getTodayOrTomorrowBasedOnTime(context, alarm, calendar));
        } else {
            setDaysOfWeekText(context.getString(R.string.alarm_scheduled_for, AlarmUtils.formatAlarmDate(alarm)));
        }
    }

    private void setDaysOfWeekText(CharSequence text) {
        mDaysOfWeek.setTypeface(mGeneralTypeface);
        mDaysOfWeek.setText(text);
    }

    private String getTodayOrTomorrowBasedOnTime(Context context, Alarm alarm, Calendar now) {
        // Used when the date has passed, the new alarm will be scheduled either the same day
        // or the next day depending on the time.
        // The text is therefore updated accordingly.
        return context.getString(alarm.isTimeBeforeOrEqual(now) ? R.string.alarm_tomorrow : R.string.alarm_today);
    }

}
