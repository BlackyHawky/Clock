/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.alarms;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.CombinedDays;
import com.best.deskclock.data.Weekdays;

import java.util.Calendar;
import java.util.TimeZone;

/** Displays the active and overridden days for one alarm month. */
final class InlineCalendarAdapter extends RecyclerView.Adapter<InlineCalendarAdapter.DayHolder> {

    interface OnDateToggledListener {
        void onDateToggled(int year, int month, int day);
    }

    private int mYear;
    private int mMonth;
    private final int mFirstDayOfWeek;
    private final int mGridStartDay;
    private int mAlarmHour;
    private int mAlarmMinute;
    private TimeZone mTimeZone;
    private final int mActiveColor;
    private final int mActiveTextColor;
    private final int mInactiveTextColor;
    private final Typeface mTypeface;
    private final OnDateToggledListener mListener;
    private Weekdays mWeekdays;
    private CombinedDays mCombinedDays;
    private int mDaysInMonth;
    private int mOffsetCells;

    InlineCalendarAdapter(int year, int month, @NonNull Weekdays weekdays,
                          @NonNull CombinedDays combinedDays, @NonNull OnDateToggledListener listener,
                          int firstDayOfWeek, int alarmHour, int alarmMinute, @NonNull TimeZone timeZone,
                          int activeColor, int activeTextColor, int inactiveTextColor, Typeface typeface) {
        mYear = year;
        mMonth = month;
        mWeekdays = weekdays;
        mCombinedDays = combinedDays;
        mListener = listener;
        mGridStartDay = firstDayOfWeek;
        mAlarmHour = alarmHour;
        mAlarmMinute = alarmMinute;
        mTimeZone = (TimeZone) timeZone.clone();
        mActiveColor = activeColor;
        mActiveTextColor = activeTextColor;
        mInactiveTextColor = inactiveTextColor;
        mTypeface = typeface;
        mFirstDayOfWeek = firstDayOfWeek;
        calculateMonth();
    }

    int getYear() {
        return mYear;
    }

    int getMonth() {
        return mMonth;
    }

    void setMonth(int year, int month) {
        mYear = year;
        mMonth = month;
        calculateMonth();
        notifyDataSetChanged();
    }

    void setData(@NonNull Weekdays weekdays, @NonNull CombinedDays combinedDays) {
        mWeekdays = weekdays;
        mCombinedDays = combinedDays;
        notifyDataSetChanged();
    }

    void setSchedule(int alarmHour, int alarmMinute, @NonNull TimeZone timeZone) {
        mAlarmHour = alarmHour;
        mAlarmMinute = alarmMinute;
        mTimeZone = (TimeZone) timeZone.clone();
        notifyDataSetChanged();
    }

    private void calculateMonth() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(mYear, mMonth, 1);
        int firstDay = calendar.get(Calendar.DAY_OF_WEEK);
        mDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        mOffsetCells = (firstDay - mGridStartDay + 7) % 7;
    }

    @Override
    public int getItemCount() {
        return mOffsetCells + mDaysInMonth;
    }

    @NonNull
    @Override
    public DayHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DayHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.inline_calendar_day, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DayHolder holder, int position) {
        TextView dayText = holder.dayText;
        if (position < mOffsetCells) {
            dayText.setText("");
            dayText.setBackground(null);
            dayText.setOnClickListener(null);
            dayText.setClickable(false);
            dayText.setContentDescription(null);
            return;
        }

        int day = position - mOffsetCells + 1;
        dayText.setText(String.valueOf(day));
        dayText.setTypeface(mTypeface);

        boolean isPast = isPast(day);
        boolean isToday = isToday(day);
        boolean isActive = !isPast && isActive(day);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        if (isActive) {
            background.setColor(mActiveColor);
            dayText.setTextColor(mActiveTextColor);
            dayText.setAlpha(1f);
        } else {
            background.setColor(Color.TRANSPARENT);
            dayText.setTextColor(mInactiveTextColor);
            dayText.setAlpha(isPast ? 0.4f : 1f);
        }
        if (isToday) {
            background.setStroke(Math.max(1, Math.round(
                1.5f * holder.itemView.getResources().getDisplayMetrics().density)),
                mActiveColor);
        }
        dayText.setBackground(background);
        dayText.setClickable(!isPast);
        dayText.setFocusable(!isPast);
        if (isPast) {
            dayText.setOnClickListener(null);
        } else {
            dayText.setOnClickListener(v -> mListener.onDateToggled(mYear, mMonth, day));
        }
    }

    private boolean isPast(int day) {
        Calendar today = Calendar.getInstance(mTimeZone);
        int comparison = compareDate(mYear, mMonth, day,
            today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        if (comparison < 0) {
            return true;
        }
        return comparison == 0
            && (today.get(Calendar.HOUR_OF_DAY) > mAlarmHour
            || (today.get(Calendar.HOUR_OF_DAY) == mAlarmHour
            && today.get(Calendar.MINUTE) >= mAlarmMinute));
    }

    private boolean isToday(int day) {
        Calendar today = Calendar.getInstance(mTimeZone);
        return compareDate(mYear, mMonth, day, today.get(Calendar.YEAR),
            today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)) == 0;
    }

    private boolean isActive(int day) {
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        date.clear();
        date.set(mYear, mMonth, day);
        int weekday = date.get(Calendar.DAY_OF_WEEK);
        if (mCombinedDays.isDateDismissed(mYear, mMonth, day)) {
            return false;
        }
        if (mWeekdays.isBitOn(weekday)) {
            return !mCombinedDays.isDateDeselected(mYear, mMonth, day);
        }
        return mCombinedDays.isDateSelected(mYear, mMonth, day);
    }

    private static int compareDate(int year, int month, int day, int otherYear, int otherMonth, int otherDay) {
        if (year != otherYear) {
            return Integer.compare(year, otherYear);
        }
        if (month != otherMonth) {
            return Integer.compare(month, otherMonth);
        }
        return Integer.compare(day, otherDay);
    }

    static final class DayHolder extends RecyclerView.ViewHolder {
        final TextView dayText;

        DayHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.day_text);
        }
    }
}
