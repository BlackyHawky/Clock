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
    private final Weekdays mWeekdays;
    private final CombinedDays mCombinedDays;
    private final OnDateToggleListener mListener;
    private final Typeface mTypeface;

    private static final int COLUMN_COUNT = 7;

    // Calendar fields for the current month being displayed
    private int mFirstDayOfWeek; // 1=Sunday, 2=Monday, ..., 7=Saturday (Calendar API)
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
        mActiveColor = activeColor;
        mActiveTextColor = activeTextColor;
        mInactiveTextColor = inactiveTextColor;
        mTodayStrokeColor = todayStrokeColor;
        mEmptyColor = emptyColor;
        mTypeface = typeface;

        // Today
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        mTodayYear = now.get(Calendar.YEAR);
        mTodayMonth = now.get(Calendar.MONTH);
        mTodayDay = now.get(Calendar.DAY_OF_MONTH);

        recalculateMonth();
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

    private void recalculateMonth() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(mYear, mMonth, 1);
        mFirstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=Sunday, 2=Monday, ...
        mDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Calculate offset: how many empty cells before day 1
        // Convert Calendar's DAY_OF_WEEK (1=Sun) to a Monday-first index (0=Mon, 6=Sun)
        int firstDayMondayBased = (mFirstDayOfWeek + 5) % 7; // Mon=0, Tue=1, ..., Sun=6
        mOffsetCells = firstDayMondayBased;
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
        int adapterPosition = holder.getAdapterPosition();
        if (adapterPosition < mOffsetCells) {
            // Empty cell before day 1
            holder.dayText.setText("");
            holder.dayText.setBackground(null);
            holder.dayText.setClickable(false);
            holder.dayText.setFocusable(false);
            return;
        }

        int day = adapterPosition - mOffsetCells + 1;
        holder.dayText.setText(String.valueOf(day));
        holder.dayText.setTypeface(mTypeface);
        holder.dayText.setClickable(true);
        holder.dayText.setFocusable(true);

        boolean isActive = isDateActive(day);
        boolean isToday = (mYear == mTodayYear && mMonth == mTodayMonth && day == mTodayDay);

        // Background: filled circle for active, nothing for inactive
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);

        if (isActive) {
            bg.setColor(mActiveColor);
            holder.dayText.setTextColor(mActiveTextColor);
        } else {
            bg.setColor(Color.TRANSPARENT);
            holder.dayText.setTextColor(mInactiveTextColor);
        }

        // Today indicator: add a stroke
        if (isToday) {
            bg.setStroke(2, mTodayStrokeColor);
        }

        holder.dayText.setBackground(bg);

        // Click to toggle
        holder.dayText.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onDateToggled(mYear, mMonth, day);
            }
        });
    }

    /**
     * Determines if a given day of the current month is "active" (alarm will fire).
     * <p>
     * A date is active if:
     * - It matches a selected weekday AND is NOT in the deselected list, OR
     * - It does NOT match any selected weekday AND IS in the selected list
     */
    private boolean isDateActive(int day) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(mYear, mMonth, day);
        int calendarDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

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

    /**
     * Returns the number of rows needed for the current month display.
     */
    public int getRowCount() {
        return (int) Math.ceil((double) (mOffsetCells + mDaysInMonth) / COLUMN_COUNT);
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final TextView dayText;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.day_text);
        }
    }
}
