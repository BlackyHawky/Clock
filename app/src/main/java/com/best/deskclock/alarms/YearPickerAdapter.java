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

/**
 * Adapter for a 3-column year picker grid, similar to MaterialDatePicker's year selector.
 */
public class YearPickerAdapter extends RecyclerView.Adapter<YearPickerAdapter.YearViewHolder> {

    public interface OnYearSelectedListener {
        void onYearSelected(int year);
    }

    private final int mStartYear;
    private final int mEndYear;
    private final int mSelectedYear;
    private final OnYearSelectedListener mListener;
    private final Typeface mTypeface;
    @ColorInt private final int mSelectedColor;
    @ColorInt private final int mSelectedTextColor;
    @ColorInt private final int mNormalTextColor;

    public YearPickerAdapter(int startYear, int endYear, int selectedYear,
                              @NonNull OnYearSelectedListener listener,
                              @ColorInt int selectedColor,
                              @ColorInt int selectedTextColor,
                              @ColorInt int normalTextColor,
                              Typeface typeface) {
        mStartYear = startYear;
        mEndYear = endYear;
        mSelectedYear = selectedYear;
        mListener = listener;
        mSelectedColor = selectedColor;
        mSelectedTextColor = selectedTextColor;
        mNormalTextColor = normalTextColor;
        mTypeface = typeface;
    }

    @Override
    public int getItemCount() {
        return mEndYear - mStartYear + 1;
    }

    @NonNull
    @Override
    public YearViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.year_picker_item, parent, false);
        return new YearViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull YearViewHolder holder, int position) {
        int year = mStartYear + position;
        holder.yearText.setText(String.valueOf(year));
        holder.yearText.setTypeface(mTypeface);

        boolean isSelected = (year == mSelectedYear);

        if (isSelected) {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            float cornerRadius = 8 * holder.itemView.getContext().getResources().getDisplayMetrics().density;
            bg.setCornerRadius(cornerRadius);
            bg.setColor(mSelectedColor);
            holder.yearText.setBackground(bg);
            holder.yearText.setTextColor(mSelectedTextColor);
        } else {
            holder.yearText.setBackground(null);
            holder.yearText.setTextColor(mNormalTextColor);
        }

        holder.yearText.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onYearSelected(year);
            }
        });
    }

    static class YearViewHolder extends RecyclerView.ViewHolder {
        final TextView yearText;

        YearViewHolder(@NonNull View itemView) {
            super(itemView);
            yearText = itemView.findViewById(R.id.year_text);
        }
    }
}
