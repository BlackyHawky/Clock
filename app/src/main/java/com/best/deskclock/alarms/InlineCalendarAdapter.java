// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.CombinedDays;
import com.best.deskclock.data.Weekdays;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Adapter for the inline calendar grid that shows a month of days.
 * <p>
 * Dates matching selected weekdays are shown as "active" (alarm will fire).
 * Users can tap any date to toggle its override state in {@link CombinedDays}.
 * <p>
 * Active dates have a filled circle background; inactive dates are transparent.
 * Today's date has a stroke outline in addition to the fill if active.
 */
public class InlineCalendarAdapter extends RecyclerView.Adapter<InlineCalendarAdapter.DayViewHolder> {

    /**
     * Callback interface for date toggle events.
     */
    public interface OnDateToggleListener {
        /**
         * Called when a date is toggled on the inline calendar.
         *
         * @param year  the year
         * @param month the month (0-based)
         * @param day   the day of month
         */
        void onDateToggled(int year, int month, int day);
    }

    private int mYear;
    private int mMonth;
    private Weekdays mWeekdays;
    private CombinedDays mCombinedDays;
    private final OnDateToggleListener mListener;
    private final Typeface mTypeface;

    // Alarm time for today-past check
    private int mAlarmHour = -1;
    private int mAlarmMinute = -1;

    // Calendar fields for the current month being displayed
    private int mFirstDayOfWeek; // 1=Sunday, 2=Monday, ..., 7=Saturday (Calendar API)
    private final int mGridStartDay; // preferred first column day, Calendar API (1=Sun..7=Sat)
    private int mDaysInMonth;
    private int mOffsetCells; // empty cells before day 1

    // Today's date for "today" indicator
    private final int mTodayYear;
    private final int mTodayMonth;
    private final int mTodayDay;

    // Color cache
    @ColorInt
    private final int mActiveColor;
    @ColorInt
    private final int mActiveTextColor;
    @ColorInt
    private final int mInactiveTextColor;
    @ColorInt
    private final int mTodayStrokeColor;
    @ColorInt
    private final int mEmptyColor;

    public InlineCalendarAdapter(int year, int month,
                                  @NonNull Weekdays weekdays,
                                  @NonNull CombinedDays combinedDays,
                                  @NonNull OnDateToggleListener listener,
                                  int firstDayOfWeek,
                                  @ColorInt int activeColor,
                                  @ColorInt int activeTextColor,
                                  @ColorInt int inactiveTextColor,
                                  @ColorInt int todayStrokeColor,
                                  @ColorInt int emptyColor,
                                  Typeface typeface) {
        mYear = year;
        mMonth = month;
        mWeekdays = weekdays;
        mCombinedDays = combinedDays;
        mListener = listener;
        mGridStartDay = firstDayOfWeek;
        mActiveColor = activeColor;
        mActiveTextColor = activeTextColor;
        mInactiveTextColor = inactiveTextColor;
        mTodayStrokeColor = todayStrokeColor;
        mEmptyColor = emptyColor;
        mTypeface = typeface;

        // Today (use local timezone so "today" matches user's clock)
        Calendar now = Calendar.getInstance();
        mTodayYear = now.get(Calendar.YEAR);
        mTodayMonth = now.get(Calendar.MONTH);
        mTodayDay = now.get(Calendar.DAY_OF_MONTH);

        recalculateMonth();
    }

    /**
     * Sets the alarm hour and minute for today-past checking.
     */
    public void setAlarmTime(int hour, int minute) {
        mAlarmHour = hour;
        mAlarmMinute = minute;
    }

    /**
     * Updates the displayed month.
     */
    public void setMonth(int year, int month) {
        mYear = year;
        mMonth = month;
        recalculateMonth();
        notifyDataSetChanged();
    }

    /**
     * Returns the currently displayed year.
     */
    public int getYear() {
        return mYear;
    }

    /**
     * Returns the currently displayed month (0-based).
     */
    public int getMonth() {
        return mMonth;
    }

    /**
     * Updates the weekdays and combined days data, then refreshes the display.
     */
    public void setData(@NonNull Weekdays weekdays, @NonNull CombinedDays combinedDays) {
        mWeekdays = weekdays;
        mCombinedDays = combinedDays;
        notifyDataSetChanged();
    }

    private void recalculateMonth() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(mYear, mMonth, 1);
        mFirstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=Sunday, 2=Monday, ...
        mDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Offset empty cells so the first column matches the preferred start day
        // (same rule as the weekday buttons above the calendar).
        // mFirstDayOfWeek is the DAY_OF_WEEK of the 1st; subtract the preferred start day.
        mOffsetCells = (mFirstDayOfWeek - mGridStartDay + 7) % 7;
    }

    /**
     * Returns the total number of items in the adapter (offset + days).
     */
    @Override
    public int getItemCount() {
        return mOffsetCells + mDaysInMonth;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.inline_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        int adapterPosition = holder.getBindingAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition < mOffsetCells) {
            holder.dayText.setText("");
            holder.dayText.setBackground(null);
            holder.dayText.setClickable(false);
            holder.dayText.setFocusable(false);
            return;
        }

        int day = adapterPosition - mOffsetCells + 1;
        holder.dayText.setText(String.valueOf(day));
        holder.dayText.setTypeface(mTypeface);

        boolean isPast = isDatePast(day);
        boolean isActive = !isPast && isDateActive(day);
        boolean isToday = mYear == mTodayYear && mMonth == mTodayMonth && day == mTodayDay;

        GradientDrawable baseBg = new GradientDrawable();
        baseBg.setShape(GradientDrawable.OVAL);

        if (isPast) {
            baseBg.setColor(Color.TRANSPARENT);
            if (isToday) {
                // Keep a soft outline around today so its cell stays recognizable once dimmed.
                float density = holder.itemView.getResources().getDisplayMetrics().density;
                baseBg.setStroke(Math.round((1.5f + 2f) * density), mTodayStrokeColor);
            }
            holder.dayText.setTextColor(mInactiveTextColor);
            holder.dayText.setAlpha(0.4f);
            holder.dayText.setClickable(false);
            holder.dayText.setFocusable(false);
        } else if (isActive) {
            baseBg.setColor(mActiveColor);
            holder.dayText.setTextColor(mActiveTextColor);
            holder.dayText.setAlpha(1f);
            holder.dayText.setClickable(true);
            holder.dayText.setFocusable(true);
        } else {
            baseBg.setColor(Color.TRANSPARENT);
            holder.dayText.setTextColor(mInactiveTextColor);
            holder.dayText.setAlpha(1f);
            holder.dayText.setClickable(true);
            holder.dayText.setFocusable(true);
        }

        holder.dayText.setBackground(baseBg);

        if (!isPast) {
            holder.dayText.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onDateToggled(mYear, mMonth, day);
                }
            });
        } else {
            holder.dayText.setOnClickListener(null);
        }
    }

    /**
     * Determines if a given day of the current month is in the past.
     * Today is considered past if the alarm time has already passed.
     */
    private boolean isDatePast(int day) {
        // "Today" is the local calendar date. Comparing dates as (year,month,day) only is safe
        // across DST transitions and never disagrees with the user's clock near midnight.
        final boolean beforeToday = mYear < mTodayYear
            || (mYear == mTodayYear && mMonth < mTodayMonth)
            || (mYear == mTodayYear && mMonth == mTodayMonth && day < mTodayDay);
        if (beforeToday) {
            return true;
        }

        // Today is considered past once the alarm time itself has passed.
        if (mYear == mTodayYear && mMonth == mTodayMonth && day == mTodayDay
            && mAlarmHour >= 0 && mAlarmMinute >= 0) {
            final Calendar now = Calendar.getInstance();
            return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                >= mAlarmHour * 60 + mAlarmMinute;
        }

        return false;
    }

    /**
     * Determines if a given day of the current month is "active" (alarm will fire).
     * <p>
     * A date is active if:
     * - It matches a selected weekday AND is NOT in the deselected list, OR
     * - It does NOT match any selected weekday AND IS in the selected list
     */
    private boolean isDateActive(int day) {
        // Pure day-of-week arithmetic avoids a Calendar allocation per bound cell.
        int calendarDayOfWeek = ((mFirstDayOfWeek - 1) + (day - 1)) % 7 + 1;

        boolean matchesWeekday = mWeekdays.isBitOn(calendarDayOfWeek);
        boolean isDeselected = mCombinedDays.isDateDeselected(mYear, mMonth, day);
        boolean isSelected = mCombinedDays.isDateSelected(mYear, mMonth, day);

        if (matchesWeekday) {
            // Matches a selected weekday: active unless deselected
            return !isDeselected;
        } else {
            // Doesn't match any weekday: active only if explicitly selected
            return isSelected;
        }
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final TextView dayText;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.day_text);
        }
    }
}
